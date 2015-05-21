/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class AlertReport implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	
	private String dbGroupName;
	private String dbHostName;
	private long timestamp;//in yyyymmddhhmmss
	private String alertReason;
	private String alertValue;
	
	private ProcessListSummary processListSummary;
	private java.util.LinkedHashMap<String, String> replicationSummary;
	//user defined alerts, the key is alert name
	private ResultList userDefinedAlertData;
	
	private long reportTimestamp;
	
	private File rootPath;
	
	public AlertReport()
	{
		
	}

	public ProcessListSummary getProcessListSummary() {
		return processListSummary;
	}

	public void setProcessListSummary(ProcessListSummary processListSummary) {
		this.processListSummary = processListSummary;
	}

	public String getDbGroupName() {
		return dbGroupName;
	}

	public void setDbGroupName(String dbGroupName) {
		this.dbGroupName = dbGroupName;
	}

	public String getDbHostName() {
		return dbHostName;
	}

	public void setDbHostName(String dbHostName) {
		this.dbHostName = dbHostName;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public File getRootPath() {
		return rootPath;
	}

	public void setRootPath(File rootPath) {
		this.rootPath = rootPath;
	}
	
	public void saveAsText()
	{
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
		java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));	
		sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));	
		File dest = null;
		PrintWriter pw = null;
		try
		{
			String ts = sdf.format(new Date(this.timestamp));
			File dir = new File(this.rootPath, sdf2.format(new Date(this.timestamp)));
			if(!dir.exists())
				dir.mkdirs();
			dest = new File(dir, this.alertReason+"_"+this.dbGroupName+"_"+this.dbHostName+"_"+ts+".txt");
			pw = new PrintWriter(new FileWriter(dest));
			pw.println("Report Time:        "+sdf.format(new Date(this.reportTimestamp)));
			pw.println("Detect Time:        "+ts);
			pw.println("DB Group:    "+this.dbGroupName);
			pw.println("DB Host:     "+this.dbHostName);
			pw.println("Alert Type:  "+this.alertReason);
			if(alertValue != null)pw.println("Alert Value:  "+this.alertValue);
			pw.println("");
			
			if(this.processListSummary != null)this.processListSummary.saveAsText(pw);
			
			if(this.replicationSummary!=null && this.replicationSummary.size()>0)
			{
				pw.println();
				pw.println("------SLAVE STATUS------");
				for(Map.Entry<String, String> e: this.replicationSummary.entrySet())
				{
					pw.println(String.format("%40s: %s", e.getKey(), e.getValue()));
				}
			}
			
			if(this.userDefinedAlertData != null)
				printList(pw, this.userDefinedAlertData);
			
			pw.flush();
		}catch(Exception ex)
		{
			
		}finally
		{
			if(pw!=null)try{pw.close();}catch(Exception ex){}
		}
		
		
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

	public java.util.LinkedHashMap<String, String> getReplicationSummary() {
		return replicationSummary;
	}

	public void setReplicationSummary(java.util.LinkedHashMap<String, String> replicationSummary) {
		this.replicationSummary = replicationSummary;
	}

	public long getReportTimestamp() {
		return reportTimestamp;
	}

	public void setReportTimestamp(long reportTimestamp) {
		this.reportTimestamp = reportTimestamp;
	}

	public ResultList getUserDefinedAlertData() {
		return userDefinedAlertData;
	}

	public void setUserDefinedAlertData(ResultList userDefinedAlertData) {
		this.userDefinedAlertData = userDefinedAlertData;
	}
	public static void printList(PrintWriter pw, ResultList rList)
	{
		int colcnt =  rList.getColumnDescriptor().getColumns().size();
		int[] colsize = new int[colcnt];
		for(int i=0;i<colcnt;i++)
			colsize[i] = rList.getColumnDescriptor().getColumns().get(i).getName().length();
		for(ResultRow row: rList.getRows())
		{
			List<String> cols = row.getColumns();
			for(int i=0;i<cols.size();i++)
			{
				if(cols.get(i)!=null && cols.get(i).length()> colsize[i])
					colsize[i] =  cols.get(i).length();
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<colcnt;i++)
		{
			sb.append(String.format("%"+colsize[i]+"s ", rList.getColumnDescriptor().getColumns().get(i).getName()));
		}
		pw.println(sb.toString());
		for(ResultRow row: rList.getRows())
		{
			sb.setLength(0);
			List<String> cols = row.getColumns();
			for(int i=0;i<cols.size();i++)
			{
				sb.append(String.format("%"+colsize[i]+"s ", row.getColumns().get(i)));
			}
			pw.println(sb.toString());
		}
			
	}

}
