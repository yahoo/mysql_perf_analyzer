/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.ConfigBlock;
import com.yahoo.dba.perf.myperf.common.ConfigHistory;
import com.yahoo.dba.perf.myperf.common.Constants;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultListUtil;
import com.yahoo.dba.perf.myperf.common.ResultRow;

public class VarhistoryController extends MyPerfBaseController
{
	  private static Logger logger = Logger.getLogger(VarhistoryController.class.getName());
		
	  @Override
	  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
	  {
	    int status = Constants.STATUS_OK;
		String message = "OK";

		logger.info("receive url "+req.getQueryString());
		QueryParameters qps = null;
		DBInstanceInfo dbinfo = null;
		
		//first session check
		boolean isSessionValid = WebAppUtil.hasValidSession(req);
		if(!isSessionValid)
			return this.respondFailure("session timeout. Please logout and re-login.", req);

		qps = WebAppUtil.parseRequestParameter(req);
		qps.setSql("mysql_global_variables");

		dbinfo = this.frameworkContext.getDbInfoManager().findDB(qps.getGroup(), qps.getHost());
		if(dbinfo==null)
			return this.respondFailure("Cannot find  record for DB ("+qps.getGroup()+", "+qps.getHost()+")", req);

		//when we reach here, at least we have valid query and can connect to db	
		WebAppUtil.storeLastDbInfoRequest(qps.getGroup(),qps.getHost(), req);
		ModelAndView mv = null;
		ResultList rList = new ResultList();
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("VARIABLE_NAME", false, 1);
		desc.addColumn("VARIABLE_VALUE", false, 2);
		desc.addColumn("COMMENTS", false, 3);
		rList.setColumnDescriptor(desc);				
		try
		{
			ConfigHistory ch = ConfigHistory.load(new File(new File(this.frameworkContext.getFileReposirtoryPath()), "autoscan"), dbinfo);
			if(ch!=null && ch.getChanges().size()>0)
			{
				{
					ResultRow row = new ResultRow();
					List<String> cols = new ArrayList<String>();
					cols.add("CHANGES");
					cols.add("");
					cols.add(ch.getStartingConfig().getTime()+" - "+ch.getLastCheckedConfig().getTime());
					row.setColumns(cols);
					row.setColumnDescriptor(desc);
					rList.addRow(row);			  

				}
				//list changed in reverse order
				for(int i=ch.getChanges().size()-1;i>=0;i--)
				{
					ConfigBlock cb = ch.getChanges().get(i);
					
					ResultRow row = new ResultRow();
					List<String> cols = new ArrayList<String>();
					cols.add("CHANGE TIME");
					cols.add(cb.getTime());
					cols.add("Timestamp (UTC) when checked");
					row.setColumns(cols);
					row.setColumnDescriptor(desc);
					rList.addRow(row);			  

					HashMap<String, String> changes = new HashMap<String, String>();
					//scan changes with old value
					for(Map.Entry<String, String> e: cb.getVariables().entrySet())
					{
						String key = e.getKey();
						String val = e.getValue();
						if(key.startsWith("+-"))
						{
							changes.put(key.substring(2), val);
						}
					}
					
					for(Map.Entry<String, String> e: cb.getVariables().entrySet())
					{
						String key = e.getKey();
						String v = e.getValue();
						row = new ResultRow();
						cols = new ArrayList<String>();
						if(key.startsWith("+-"))
							continue;//skip
						else if(key.startsWith("+-"))
							cols.add(key.substring(1));//removed
						else
							cols.add(key);
						cols.add(v);
						if(changes.containsKey(key))
							cols.add("Prev Value: "+changes.get(key));
						else if(key.startsWith("-"))
							cols.add("Removed");
						else
							cols.add("");
						row.setColumns(cols);
						row.setColumnDescriptor(desc);
						rList.addRow(row);
					}
					//add an empty line
					row = new ResultRow();
					cols = new ArrayList<String>();
					cols.add("");
					cols.add("");
					cols.add("");
					row.setColumns(cols);
					row.setColumnDescriptor(desc);
					rList.addRow(row);			  

				}
			}
			if(ch !=  null)
			{
				ConfigBlock cb = ch.getStartingConfig();
				
				ResultRow row = new ResultRow();
				List<String> cols = new ArrayList<String>();
				cols.add("FIRST RECORD TIME");
				cols.add(cb.getTime());
				if(ch!=null && ch.getChanges().size()>0)
					cols.add("First Recorded Timestamp (UTC)");
				else
					cols.add("No Changes Since First Check (Timestamp UTC)");
				row.setColumns(cols);
				row.setColumnDescriptor(desc);
				rList.addRow(row);			  

				for(Map.Entry<String, String> e: cb.getVariables().entrySet())
				{
					String key = e.getKey();
					String v = e.getValue();
					row = new ResultRow();
					cols = new ArrayList<String>();
					cols.add(key);
					cols.add(v);
					cols.add("");
					row.setColumns(cols);
					row.setColumnDescriptor(desc);
					rList.addRow(row);
				}
				
			}else
			{
				status = Constants.STATUS_BAD;
				message = "No variable configuration history has been recorded yet.";
			}

		}catch(Throwable ex)
		{
		  logger.log(Level.SEVERE,"Exception", ex);
		  status = Constants.STATUS_BAD;
		  message = "Exception: "+ex.getMessage();
		}
	    
		mv = new ModelAndView(this.jsonView);
		if(req.getParameter("callback")!=null&&req.getParameter("callback").trim().length()>0)
		  mv.addObject("callback", req.getParameter("callback"));//YUI datasource binding
				
		mv.addObject("json_result", ResultListUtil.toJSONString(rList, qps, status, message));
		return mv;
	  }

}
