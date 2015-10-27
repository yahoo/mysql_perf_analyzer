/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.Constants;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.QueryInputValidator;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultListUtil;
import com.yahoo.dba.perf.myperf.common.ResultRow;
import com.yahoo.dba.perf.myperf.db.DBConnectionWrapper;

public class VardiffController extends MyPerfBaseController
{
	  private static Logger logger = Logger.getLogger(VardiffController.class.getName());
		
	  @Override
	  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
	  {
	    int status = Constants.STATUS_OK;
		String message = "OK";

		logger.info("receive url "+req.getQueryString());
		QueryParameters qps = null;
		DBInstanceInfo dbinfo = null;
		DBInstanceInfo dbinfo2 = null;
		DBConnectionWrapper connWrapper = null;
		DBConnectionWrapper connWrapper2 = null;
		
		qps = WebAppUtil.parseRequestParameter(req);
		qps.setSql("mysql_global_variables");
		qps.getSqlParams().put("p_1", "");
		String group2 = req.getParameter("p_1");
		String host2 = req.getParameter("p_2");
		//validation input
		String validation = qps.validate();
		if(validation==null||validation.isEmpty())
		{
		    //do we have such query?
			try
			{
			  QueryInputValidator.validateSql(this.frameworkContext.getSqlManager(), qps);
			}
			catch(Exception ex)
			{
			  validation = ex.getMessage();
			}
		}
		if(validation!=null && !validation.isEmpty())
		  return this.respondFailure(validation, req);
		
		dbinfo = this.frameworkContext.getDbInfoManager().findDB(qps.getGroup(), qps.getHost());
		if(dbinfo==null)
	      return this.respondFailure("Cannot find record for DB ("+qps.getGroup()+", "+qps.getHost()+")", req);
	    dbinfo2 = this.frameworkContext.getDbInfoManager().findDB(group2, host2);
		if(dbinfo2==null)
		  return this.respondFailure("Cannot find record for DB ("+group2+", "+host2+")", req);
		  
		try
		{
		  connWrapper = WebAppUtil.getDBConnection(req, this.frameworkContext, dbinfo);
		  if(connWrapper==null)
		  {
		    status = Constants.STATUS_BAD;
		    message = "failed to connect to target db ("+dbinfo+")";
		  }else 
		  {
		    connWrapper2 = WebAppUtil.getDBConnection(req, this.frameworkContext, dbinfo2);
			if(connWrapper2==null) 
			{
			  status = Constants.STATUS_BAD;
			  message = "failed to connect to target db ("+dbinfo2+")";  		
		    }
		  }
		}catch(Throwable th)
		{
		  logger.log(Level.SEVERE,"Exception", th);
		  status = Constants.STATUS_BAD;
		   message = "Failed to get connection to target db ("+dbinfo+"): "+th.getMessage();
		}

		if(status == -1)
		  return this.respondFailure(message, req);

		//when we reach here, at least we have valid query and can connect to db	
		WebAppUtil.storeLastDbInfoRequest(qps.getGroup(),qps.getHost(), req);
		ModelAndView mv = null;
		ResultList rList = null;
		ResultList rList2 = null;
						
		try
		{
		  rList = this.frameworkContext.getQueryEngine().executeQueryGeneric(qps, connWrapper, qps.getMaxRows());
		  rList2 = this.frameworkContext.getQueryEngine().executeQueryGeneric(qps, connWrapper2, qps.getMaxRows());
		  logger.info("Done query "+qps.getSql() + " with "+(rList!=null?rList.getRows().size():0)+" records, "+(rList2!=null?rList2.getRows().size():0)+" records");
		  WebAppUtil.closeDBConnection(req, connWrapper, false, this.getFrameworkContext().getMyperfConfig().isReuseMonUserConnction());
		  WebAppUtil.closeDBConnection(req, connWrapper2, false, this.getFrameworkContext().getMyperfConfig().isReuseMonUserConnction());
		}catch(Throwable ex)
		{
		  logger.log(Level.SEVERE,"Exception", ex);
		  if(ex instanceof SQLException)
		  {
		    SQLException sqlEx = SQLException.class.cast(ex);
			String msg = ex.getMessage();
			logger.info(sqlEx.getSQLState()+", "+sqlEx.getErrorCode()+", "+msg);
			//check if the connection is still good
			if(!DBUtils.checkConnection(connWrapper.getConnection()))
			{
			  WebAppUtil.closeDBConnection(req, connWrapper, true, false);
			}
			else
			  WebAppUtil.closeDBConnection(req, connWrapper, true, false);
			if(!DBUtils.checkConnection(connWrapper2.getConnection()))
			{
			  WebAppUtil.closeDBConnection(req, connWrapper2, true, false);
			}
			else
			  WebAppUtil.closeDBConnection(req, connWrapper2, true, false);
		  }else
		  {
		    WebAppUtil.closeDBConnection(req, connWrapper, false, this.getFrameworkContext().getMyperfConfig().isReuseMonUserConnction());
		    WebAppUtil.closeDBConnection(req, connWrapper2, false, this.getFrameworkContext().getMyperfConfig().isReuseMonUserConnction());
		  }
		  status = Constants.STATUS_BAD;
		  message = "Exception: "+ex.getMessage();
		}
        
		if(status == Constants.STATUS_BAD)
		  return this.respondFailure(message, req);
		
		HashMap<String, String> param1 = new HashMap<String, String>(rList.getRows().size());
		HashMap<String, String> param2 = new HashMap<String, String>(rList2.getRows().size());
		for(ResultRow r: rList.getRows())
		{
		  param1.put(r.getColumns().get(0).toUpperCase(), r.getColumns().get(1));	
		}
		for(ResultRow r: rList2.getRows())
		{
		  param2.put(r.getColumns().get(0).toUpperCase(), r.getColumns().get(1));	
		}
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("VARIABLE_NAME", false, 1);
		desc.addColumn("DB1", false, 2);
		desc.addColumn("DB2", false, 3);
	    
		ResultList fList = new ResultList();
		fList.setColumnDescriptor(desc);
		
		HashSet<String> diffSet = new HashSet<String>();
		for(Map.Entry<String, String> e: param1.entrySet())
		{
		  String k = e.getKey();
		  String v = e.getValue();
		  if(v!=null)v=v.trim();
		  else v = "";
		  String v2 = null;
		  if(param2.containsKey(k))
		    v2 = param2.get(k);
		  if(v2!=null)v2 = v2.trim();
		  else v2="";
		  if(!v.equals(v2))
		  {
		    ResultRow row = new ResultRow();
		    List<String> cols = new ArrayList<String>();
		    cols.add(k);
		    cols.add(v);
		    cols.add(v2);
		    row.setColumns(cols);
		    row.setColumnDescriptor(desc);
		    fList.addRow(row);
		    diffSet.add(k);
		  }
		}

		for(Map.Entry<String, String> e: param2.entrySet())
		{
		  String k = e.getKey();
		  String v = e.getValue();
		  if(v==null||v.isEmpty())continue;
		  if(diffSet.contains(k)||param1.containsKey(k))continue;
		  ResultRow row = new ResultRow();
		  List<String> cols = new ArrayList<String>();
		  cols.add(k);
		  cols.add("");
		  cols.add(v);
		  row.setColumns(cols);
		  row.setColumnDescriptor(desc);
		  fList.addRow(row);			  
		}

		mv = new ModelAndView(this.jsonView);
		if(req.getParameter("callback")!=null&&req.getParameter("callback").trim().length()>0)
		  mv.addObject("callback", req.getParameter("callback"));//YUI datasource binding
				
		mv.addObject("json_result", ResultListUtil.toJSONString(fList, qps, status, message));
		return mv;
	  }

	}
