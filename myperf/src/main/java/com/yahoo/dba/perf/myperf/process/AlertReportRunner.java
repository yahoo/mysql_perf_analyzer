/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.process;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.yahoo.dba.perf.myperf.common.AlertEntry;
import com.yahoo.dba.perf.myperf.common.AlertReport;
import com.yahoo.dba.perf.myperf.common.AppUser;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.ProcessListEntry;
import com.yahoo.dba.perf.myperf.common.ProcessListSummary;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultListUtil;
import com.yahoo.dba.perf.myperf.db.DBConnectionWrapper;

public class AlertReportRunner implements Runnable{
	private static Logger logger = Logger.getLogger(AlertReportRunner.class.getName());
	private MyPerfContext context;
	private DBInstanceInfo dbInfo;
	private long timestamp;
	private AppUser appUser;
	private DBConnectionWrapper connection;
	private String alertReason;
	private String alertValue;
	
	
	public AlertReportRunner(MyPerfContext context, DBInstanceInfo dbInfo, long timestamp, AppUser appUser)
	{
		this.context = context;
		this.dbInfo = dbInfo;
		this.timestamp = timestamp;
		this.appUser = appUser;
	}
	
	private boolean hasRowInfo(ResultSet rs) throws SQLException
	{
		ResultSetMetaData md = rs.getMetaData();
		int clSize = md.getColumnCount();
		for(int i=1;i<=clSize; i++)
		{
			String name = md.getColumnName(i);
			if("ROWS_READ".equalsIgnoreCase(name)
					||"ROWS_EXAMINED".equalsIgnoreCase(name)
					||"ROWS_SENT".equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	private boolean hasRowsRead(ResultSet rs) throws SQLException
	{
		ResultSetMetaData md = rs.getMetaData();
		int clSize = md.getColumnCount();
		for(int i=1;i<=clSize; i++)
		{
			String name = md.getColumnName(i);
			if("ROWS_READ".equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	@Override
	public void run() {
		AlertEntry alertEntry = new AlertEntry(timestamp, alertReason, alertValue, dbInfo.getDbGroupName(), dbInfo.getHostName());
		this.context.getAlerts().addAlert(alertEntry);
		
		ResultSet rs = null;
		Statement stmt = null;
		long reportTimestamp = System.currentTimeMillis();
		List<ProcessListEntry> prList = new ArrayList<ProcessListEntry>();
		java.util.LinkedHashMap<String, String> repMap = new java.util.LinkedHashMap<String, String>();
		String innodbStatus = null;
		ResultList rList = null;
		ResultList trxList = null;
		ResultList clientList = null;
		try
		{
			stmt = connection.getConnection().createStatement();
			stmt.setFetchSize(5000);
			//stmt.setMaxRows(5000);
			//rs = stmt.executeQuery("select * from information_schema.processlist limit 5000");
			rs = stmt.executeQuery("select * from information_schema.processlist"); //remove limit to handle the case of very large connections
			boolean hasRowInfo = rs!=null && hasRowInfo(rs);
			boolean hasRowRead = rs!=null && hasRowsRead(rs);
			while(rs!=null && rs.next())
			{
				ProcessListEntry ps = new ProcessListEntry(rs, hasRowInfo, hasRowRead);
				prList.add(ps);
			}
			if("REPLLAG".equalsIgnoreCase(this.alertReason)||"REPLDOWN".equalsIgnoreCase(this.alertReason))
			{
				DBUtils.close(rs);
				rs = stmt.executeQuery("show slave status");
				int i=0;
				int cnt = rs.getMetaData().getColumnCount();
				while(rs!=null && rs.next())
				{
					String suffix = "";
					if(i>0)suffix =  "_"+i;
					for(int k=0;k<cnt;k++)
						repMap.put(rs.getMetaData().getColumnName(k+1)+suffix, rs.getString(k+1));
					i++;
				}
			}
			
			//if ("CPU".equalsIgnoreCase(alertReason) ||"THREAD".equalsIgnoreCase(alertReason)||"REPLLAG".equalsIgnoreCase(this.alertReason))
			{
				DBUtils.close(rs);
				rs = stmt.executeQuery("show engine innodb status");
				if(rs!=null && rs.next())
				{
					innodbStatus = rs.getString("Status");
				}
				
				DBUtils.close(rs); rs = null;
				rs = stmt.executeQuery("select * from information_schema.innodb_locks");
				if(rs!=null)
				{
					rList = ResultListUtil.fromSqlResultSet(rs, 0);
					
				}
				DBUtils.close(rs); rs = null;
				rs = stmt.executeQuery("select * from information_schema.INNODB_TRX where to_seconds(now()) - to_seconds(trx_started) > 60");
				if(rs!=null)
				{
					trxList = ResultListUtil.fromSqlResultSet(rs, 0);
					
				}
				DBUtils.close(rs); rs = null;
			}

//TODO we need two snapshot to get useful data			
			if("CONNECT_FAILURE".equalsIgnoreCase(alertReason))
			{
				//dump summary of processlist if possible
				String sql = "select user, host, command, count(*) conns from information_schema.processlist group by user, host, command";
				DBUtils.close(rs);
				rs = stmt.executeQuery(sql);
				if(rs!=null)
				{
					clientList = ResultListUtil.fromSqlResultSet(rs, 0);					
				}
				
				DBUtils.close(rs);
			}
		}catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error when retrieve alert detail", ex);
		}finally
		{
			DBUtils.close(rs);
			DBUtils.close(stmt);
			//release pending report count
			this.context.getInstanceStatesManager().getStates(this.dbInfo.getDbid()).reportDone();
		}
		if(prList.size()>0||repMap.size()>0)
		{
			java.util.Collections.sort(prList);//sort by sql text
			ProcessListSummary prSum = new ProcessListSummary();
			prSum.setProcessList(prList);
			prSum.setInnodbStatus(innodbStatus);
			prSum.setLockList(rList);
			prSum.setTrxList(trxList);
			//prSum.setClientList(clientList);
			prSum.setReportTimestamp(reportTimestamp);
			prSum.summarize();
			
			AlertReport ar = this.context.createAlertReport(reportTimestamp, alertEntry);
			ar.setProcessListSummary(prSum);
			if(repMap.size()>0)
				ar.setReplicationSummary(repMap);
			ar.saveAsText();
		}
		if(this.dbInfo.isAlertEnabled())
			this.context.emailAlert(alertEntry);
	}
	
	public DBConnectionWrapper getConnection() {
		return connection;
	}
	public void setConnection(DBConnectionWrapper connection) {
		this.connection = connection;
	}
	public String getAlertReason() {
		return alertReason;
	}
	public void setAlertReason(String alertReason) {
		this.alertReason = alertReason;
	}
	public String getAlertValue() {
		return alertValue;
	}
	public void setAlertValue(String alertValue) {
		this.alertValue = alertValue;
	}

	
}
