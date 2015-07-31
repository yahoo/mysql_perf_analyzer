/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.yahoo.dba.perf.myperf.common.AlertDefinition;
import com.yahoo.dba.perf.myperf.common.AlertSubscribers;
import com.yahoo.dba.perf.myperf.common.AppUser;
import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.Metric;
import com.yahoo.dba.perf.myperf.common.MetricsGroup;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultListUtil;
import com.yahoo.dba.perf.myperf.common.ResultRow;
import com.yahoo.dba.perf.myperf.common.UserDefinedMetrics;
import com.yahoo.dba.perf.myperf.db.DBConnectionWrapper;

public class UdmController extends MyPerfBaseController
{
	  private static Logger logger = Logger.getLogger(VarhistoryController.class.getName());
		
	  @Override
	  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
	  {
		String message = "OK";
		String cmd = req.getParameter("cmd");
		boolean isValid = true;
		UserDefinedMetrics udm = null;
		
		logger.info("receive cmd "+cmd+", url "+req.getQueryString());
		//it a form request
		if((cmd==null || cmd.isEmpty()) &&!WebAppUtil.hasValidSession(req))
		{
		  return new ModelAndView(new RedirectView(nosessView)); 
		}

		//first session check
		boolean isSessionValid = WebAppUtil.hasValidSession(req);
		if(!isSessionValid)
			return this.respondFailure("session timeout. Please logout and re-login.", req);
		
	    AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
		ModelAndView mv = null;
		
		//By default, just display the page
		if(cmd==null || cmd.isEmpty())//or any invalid command
		    message = null;
		if	("test".equals(cmd) || "publish".equals(cmd))
		{
			if(appUser.isAdminUser())
				return this.processNewUDM(req, resp);
			else
				return this.respondFailure("Non admin user is not allowed to add UDM", req);
		}else if("test_alert".equals(cmd) || "publish_alert".equals(cmd))
		{
			if(appUser.isAdminUser())
				return this.processNewAlert(req, resp);
			else
				return this.respondFailure("Non admin user is not allowed to add UDM", req);
		}
		else if("udm_detail".equalsIgnoreCase(cmd))//display
		{
			String name=req.getParameter("name");
			udm = this.frameworkContext.getMetricsDef().getUdmManager().getUDMByName(name);
			if(udm!=null)
			{
			  mv = new ModelAndView(this.jsonView);
			  String res = "{\"resp\":{\"status\": 0, \"message\":\"OK\", \"udm\": "+udm.toJSON(true)+"}}";
			  //logger.info("UDM detail: "+res);
			  mv.addObject("json_result", res);
			  return mv;
			}else
				return this.respondFailure("UDM "+name+" not found", req);
		}else if("alert_detail".equalsIgnoreCase(cmd))//display
		{
			String name=req.getParameter("name");
			AlertDefinition alert = this.frameworkContext.getMetricsDef().getUdmManager().getAlertByName(name);
			if(alert!=null)
			{
			  mv = new ModelAndView(this.jsonView);
			  String res = "{\"resp\":{\"status\": 0, \"message\":\"OK\", \"alert\": "+alert.toJSON(true)+"}}";
			  //logger.info("UDM detail: "+res);
			  mv.addObject("json_result", res);
			  return mv;
			}else
				return this.respondFailure("ALERT "+name+" not found", req);
		}else if("udmdb_detail".equalsIgnoreCase(cmd))//display
			return processUDMDBDetail(req, resp);
		else if("udmdb_update".equalsIgnoreCase(cmd))//display
			return processUDMDBUpdate(req, resp, appUser);
		else if("alertdb_update".equalsIgnoreCase(cmd))//display
			return this.processAlertDBUpdate(req, resp, appUser);
		
		mv = new ModelAndView(this.formView);
		//udm list
		List<String> udms = new ArrayList<String>();
		for(Map.Entry<String, UserDefinedMetrics> e: this.frameworkContext.getMetricsDef().getUdmManager().getUdms().entrySet())
		{
			udms.add(e.getKey());
		}
		mv.addObject("udms", udms);
		//alerts
		List<String> alerts = new ArrayList<String>();
		for(Map.Entry<String, AlertDefinition> e: this.frameworkContext.getMetricsDef().getUdmManager().getAlerts().entrySet())
		{
			alerts.add(e.getKey());
		}
		mv.addObject("alerts", alerts);
		
		//predefined
		Map<String, String> predefined = new TreeMap<String, String>();
		String[] predefinedGroups  = this.frameworkContext.getMetricsDef().getGroupNames();
		for(String gname: predefinedGroups)
		{
			MetricsGroup g = this.frameworkContext.getMetricsDef().getGroupByName(gname);
			List<MetricsGroup> subGroups = g.getSubGroups();
			if(subGroups != null && subGroups.size() >0)
			{
				for(MetricsGroup subG: subGroups)
					predefined.put(gname+"."+subG.getGroupName(), subG.isAuto()?"y":"n");
			}else predefined.put(gname, g.isAuto()?"y":"n");
		}
		mv.addObject("predefined", predefined);
		
		if(WebAppUtil.hasValidSession(req))
		{
			mv.addObject("mydbs", this.frameworkContext.getDbInfoManager()
					.listDbsByUserInfo(WebAppUtil.findUserFromRequest(req), retrieveAppUser(req).isRestrictedUser()));
		    mv.addObject("mydbSize", this.frameworkContext.getDbInfoManager()
		    		.getMyDatabases(WebAppUtil.findUserFromRequest(req), retrieveAppUser(req).isRestrictedUser()).size());
		}
		else
		{
		    mv.addObject("mydbs", this.frameworkContext.getDbInfoManager().getClusters().keySet());
		    mv.addObject("mydbSize", 0);
		}
		mv.addObject("dbMap", this.frameworkContext.getDbInfoManager().getClusters());
		mv.addObject("help_key", "udm");			
		if(!isValid && message!=null)mv.addObject("message", message);
		else if(message!=null)mv.addObject("okmessage", message);
		if(cmd!=null)mv.addObject("cmd", cmd);
		mv.addObject("u", appUser);
		return mv;
	  }
	  
	 private ModelAndView processUDMDBDetail(HttpServletRequest req,
				HttpServletResponse resp)
	 {
		ModelAndView mv = null;
		String group = req.getParameter("group");
		String host = req.getParameter("host");
		String typ = req.getParameter("typ");
		ResultList rList = null;
		if(typ == null || typ.isEmpty() || "UDM".equalsIgnoreCase(typ))
		{
			List<String> udms = this.frameworkContext.getMetricsDef().getUdmManager()
				.getMetricsSubscriptions().getSubscribedUDMs(group, host);
			if(udms!=null&&udms.size()>0)
			{
				rList = new ResultList();
				ColumnDescriptor desc = new ColumnDescriptor();
				desc.addColumn("UDM", false, 0);
				rList.setColumnDescriptor(desc);
				for(String u:udms)
				{
					ResultRow row = new ResultRow();
					ArrayList<String> cols = new ArrayList<String>();
					cols.add(u);
					row.setColumns(cols);
					rList.addRow(row);
				}
				
			}
		}else if ("ALERT".equalsIgnoreCase(typ))
		{
			List<AlertSubscribers.Subscription> alerts = this.frameworkContext.getMetricsDef().getUdmManager().getAlertSubscriptions().getSubscriptions(group, host);
			if(alerts !=null && alerts.size()>0)
			{
				rList = new ResultList();
				ColumnDescriptor desc = new ColumnDescriptor();
				desc.addColumn("UDM", false, 0);
				rList.setColumnDescriptor(desc);
				for(AlertSubscribers.Subscription u: alerts)
				{
					ResultRow row = new ResultRow();
					ArrayList<String> cols = new ArrayList<String>();
					cols.add(u.alertName);
					row.setColumns(cols);
					rList.addRow(row);
				}				
			}
		}else if ("PREDEFINED".equalsIgnoreCase(typ))
		{
			rList = new ResultList();
			ColumnDescriptor desc = new ColumnDescriptor();
			desc.addColumn("UDM", false, 0);
			rList.setColumnDescriptor(desc);
			
			String[] mgNames = this.frameworkContext.getMetricsDef().getGroupNames();
			for(String mgName: mgNames)
			{		
				MetricsGroup mg = this.frameworkContext.getMetricsDef().getGroupByName(mgName);
				if(!mg.isAuto())
				{ //skip groups requiring manual configuration for now 
					if(this.frameworkContext.getMetricsDef().getUdmManager().isMetricsGroupSubscribed(
							group, host, mg.getGroupName(), null))
					{
						ResultRow row = new ResultRow();
						ArrayList<String> cols = new ArrayList<String>();
						cols.add(mg.getGroupName());
						row.setColumns(cols);
						rList.addRow(row);
					}
				}
				List<MetricsGroup> subGrps = mg.getSubGroups();
				if(subGrps != null && subGrps.size() >0)
				{
					for(MetricsGroup subG: subGrps)
					{
						if(!subG.isAuto() && this.frameworkContext.getMetricsDef().getUdmManager().isMetricsGroupSubscribed(
							group, host, mg.getGroupName(), subG.getGroupName()))
						{
							ResultRow row = new ResultRow();
							ArrayList<String> cols = new ArrayList<String>();
							cols.add(mg.getGroupName()+"."+subG.getGroupName());
							row.setColumns(cols);
							rList.addRow(row);
						}
					}
				}
			}
		}
		mv = new ModelAndView(this.jsonView);
		mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, 0, "OK"));
		return mv;

	 }
	 private ModelAndView processUDMDBUpdate(HttpServletRequest req,
				HttpServletResponse resp, AppUser appUser)
     {
		ModelAndView mv = null;
		String group = req.getParameter("group");
		String host  = req.getParameter("host");
		String m = req.getParameter("m");//metrics
		boolean subscribe = "y".equalsIgnoreCase(req.getParameter("subscribe"));
		
		boolean updated = false;
		//make sure we have this db
		if(appUser.isAdminUser())
		{
			String groupName = null;
			String subGroupName = null;
			int idx = m.indexOf(".");
			if(idx>0)
			{
				groupName = m.substring(0, idx);
				subGroupName = m.substring(idx+1);
			}else
				groupName = m;
			updated = this.frameworkContext.getMetricsDef().getUdmManager().updateUdmDbSubscription(
					 group,  host,  groupName,  subGroupName, subscribe);
		}
		else 
			return this.respondFailure("No admin user is not allowed to update UDM subscription", req);
		ResultList rList = null;
		mv = new ModelAndView(this.jsonView);
		mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, updated?0:-1, updated?"OK":"Failed"));
		return mv;		
	 }
	 
	 private ModelAndView processAlertDBUpdate(HttpServletRequest req,
				HttpServletResponse resp, AppUser appUser)
     {
		ModelAndView mv = null;
		String group = req.getParameter("group");
		String host  = req.getParameter("host");
		String alert = req.getParameter("m");//metrics
		boolean subscribe = "y".equalsIgnoreCase(req.getParameter("subscribe"));
		
		boolean updated = false;
		//make sure we have this db
		if(appUser.isAdminUser())
		{
			updated = this.frameworkContext.getMetricsDef().getUdmManager().updateAlertDbSubscription(
					 group,  host,  alert,  subscribe);
		}
		else 
			return this.respondFailure("No admin user is not allowed to update alert subscription", req);
		ResultList rList = null;
		mv = new ModelAndView(this.jsonView);
		mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, updated?0:-1, updated?"OK":"Failed"));
		return mv;		
	 }

	/**
	 * 
	 * Test UDM, use exception to send out message
	 * @param req
	 * @param udm
	 * @param db
	 * @throws SQLException
	 */
	private void testUDM(HttpServletRequest req, UserDefinedMetrics udm, String db )
	throws Exception
	{
		if(db == null || db.isEmpty())
			throw new Exception("please provide valid database for test");
		
		String[] dbs = db.split("\\|");
		if(dbs == null || dbs.length <2)
			throw new Exception("please provide valid database for test");
		DBInstanceInfo dbinfo = this.frameworkContext.getDbInfoManager().findDB(dbs[0], dbs[1]);
		if(dbinfo == null)
			throw new Exception("please provide valid database for test");
		
		HashSet<String> metricsNameSet = new HashSet<String>();
		for(Metric m: udm.getMetrics())
		{
			metricsNameSet.add(m.getSourceName());
		}
		DBConnectionWrapper connWrapper = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
  			String sql = udm.getSql();
  			MetricsGroup mg = udm.getMetricsGroup();
  			String udmType = udm.getUdmType();
  			String nameCol = udm.getNameCol();
  			String valCol = udm.getValueCol();
  			String keyCol = udm.getKeyCol();
  			boolean isBuiltin = false;
  			if(!"SQL".equals(udm.getSource()))
  			{
  				sql = this.frameworkContext.getSqlTextForMetricsGroup(udm.getSource());
  				mg = this.frameworkContext.getMetricsDef().getGroupByName(udm.getSource());
  				if(mg != null)
  				{
  					if(mg.getKeyColumn() != null)udmType = "key";
  					else if(mg.isMultipleMetricsPerRow())udmType = "column";
  					else udmType = "row";
  	  				nameCol = mg.getMetricNameColumn();
  	  				valCol = mg.getMetricValueColumn();
  	  				keyCol = mg.getKeyColumn();
  				}
  				isBuiltin = true;
  			}
  			if(sql == null || sql.isEmpty())
  			{
  				throw new Exception("please provide valid SQL");
  			}
			connWrapper = WebAppUtil.getDBConnection(req, this.frameworkContext, dbinfo);
  			if(connWrapper==null)
			{
  				throw new Exception("failed to connect to target db ("+dbinfo+")");
			}
  			stmt = connWrapper.getConnection().createStatement();
  			rs = stmt.executeQuery(sql);
  			if(rs != null)
  			{
  				ResultSetMetaData meta = rs.getMetaData();
  				//verify columns
  				int cols = meta.getColumnCount();
  				Map<String,Integer> colMap = new HashMap<String, Integer>(cols);
  				for(int i = 1; i<= cols; i++)
  					colMap.put(meta.getColumnName(i).toUpperCase(), meta.getColumnType(i));
  				if("row".equals(udmType))
  				{
  					if(!colMap.containsKey(udm.getNameCol().toUpperCase()))
  						throw new Exception("Failed to find name column from SQL result: "+udm.getNameCol() + ", returned: "+colMap);
  					if(!colMap.containsKey(udm.getValueCol().toUpperCase()))
  						throw new Exception("Failed to find value column from SQL result: "+udm.getValueCol() + ", returned: "+colMap);
  				}else //check metrics column 
  				{
  					if("key".equals(udmType))
  	  				{
  	  					if(!colMap.containsKey(keyCol.toUpperCase()))
  	  						throw new Exception("Failed to find key column from SQL result: "+udm.getKeyCol());  					
  	  				}
  					for(Metric m: udm.getMetrics())
  					{
  	  					if(!colMap.containsKey(m.getSourceName().toUpperCase()))
  	  						throw new Exception("Failed to find metric column from SQL result: "+m.getSourceName());   						
  					}
  				}
  			}else
  			{
  				throw new Exception("Failed to test SQL.");
  			}
  			while(rs != null && rs.next())
  			{
  				if("row".equals(udmType))
  				{
  					String name = rs.getString(nameCol);
  					if(!metricsNameSet.contains(name))continue;
  					String val = rs.getString(valCol);
  					try
  					{
  						BigDecimal d = new BigDecimal(val==null?"0":val);
  					}catch(Exception ex)
  					{
  						throw new Exception("Expect numeric value for metric from SQL result, got " + val); 
  					}
  				}else 
  				{
  					for(Metric m: udm.getMetrics())
  					{
  	  					String val = rs.getString(m.getSourceName());  						
  	  					try
  	  					{
  	  						BigDecimal d = new BigDecimal(val==null?"0":val);
  	  					}catch(Exception ex)
  	  					{
  	  						throw new Exception("Expect numeric value metric value from SQL result for column "+ m.getShortName() + ", got " + val); 
  	  					}
  					}  					
  				}
  				
  			}
		}
		finally
		{
			DBUtils.close(rs);
			DBUtils.close(stmt);
			WebAppUtil.closeDBConnection(req, connWrapper, true, false);//close it anyway
		}
	}

	private void testAndValidateAlert(HttpServletRequest req, AlertDefinition def, String db )
	throws Exception
	{
		if(def == null)
			throw new Exception("please provide valid alert definition for test");
			
		if(AlertDefinition.SOURCE_SQL.equals(def.getSource()))
		{
			if(def.getSqlText() == null || def.getSqlText().isEmpty())
				throw new Exception("please provide valid SQL text for test");
			if( db == null || db.isEmpty())
				throw new Exception("please provide valid database for test");
			String[] dbs = db.split("\\|");
			if(dbs == null || dbs.length <2)
				throw new Exception("please provide valid database for test");
			DBInstanceInfo dbinfo = this.frameworkContext.getDbInfoManager().findDB(dbs[0], dbs[1]);
			if(dbinfo == null)
				throw new Exception("please provide valid database for test");
			DBConnectionWrapper connWrapper = null;
			Statement stmt = null;
			ResultSet rs = null;
			try
			{
				connWrapper = WebAppUtil.getDBConnection(req, this.frameworkContext, dbinfo);
	  			if(connWrapper==null)
				{
	  				throw new Exception("failed to connect to target db ("+dbinfo+")");
				}
				String sql = def.getSqlText();
				if(def.getParams()!=null && def.getParams().size()>0)
				{
					for(Map.Entry<String, String> e: def.getParams().entrySet())
					{
						sql = sql.replace("&"+e.getKey(), e.getValue());
					}
				}
	  			stmt = connWrapper.getConnection().createStatement();
	  			rs = stmt.executeQuery(sql);
			}finally
			{
				DBUtils.close(rs);
				DBUtils.close(stmt);
				WebAppUtil.closeDBConnection(req, connWrapper, true, false);//close it anyway
			}
		}else if(AlertDefinition.SOURCE_METRICS.equals(def.getSource()))
		{
			String metricsName = def.getMetricName();
			if(metricsName == null || metricsName.isEmpty())
				throw new Exception("please provide valid metrics name in the format of group[.subgroup].metric_name");
			String[] ms = metricsName.split("\\.");
			if(ms.length < 2)
				throw new Exception("please provide valid metrics name in the format of group[.subgroup].metric_name. "+metricsName+" is not valid.");
			MetricsGroup mg = this.frameworkContext.getMetricsDef().getGroupByName(ms[0]);
			if(mg == null)
			{
				//try UDM
				UserDefinedMetrics udm = this.frameworkContext.getMetricsDef().getUdmManager().getUDMByName(ms[0]);
				if(udm == null)
					throw new Exception("please provide valid metrics name in the format of group[.subgroup].metric_name. " + ms[0] +" is not valid metrics group name.");
				else
					mg = udm.getMetricsGroup();
			}
			if(mg == null)
				throw new Exception("please provide valid metrics name in the format of group[.subgroup].metric_name. " + ms[0] +" is not valid metrics group name.");
			
			if(mg.getSubGroups().size() >0)
			{
				if(ms.length <3)
					throw new Exception("please provide valid metrics name in the format of group[.subgroup].metric_name. "+ms[0]+" has sub groups.");
				else
					mg = mg.getSubGroupByName(ms[1]);
				if(mg == null)
					throw new Exception("please provide valid metrics name in the format of group[.subgroup].metric_name. "+ms[1]+" is not a valid sub group in group "+ms[0]);
			}
			boolean findOne = false;
			for(Metric m: mg.getMetrics())
			{
				if(m.getName().equals(ms[ms.length-1]))
				{
					findOne = true;break;
				}
			}
			if(!findOne)
				throw new Exception("please provide valid metrics name in the format of group[.subgroup].metric_name. "+ metricsName+" is not valid.");
		}
		
		
	}

	private ModelAndView processNewUDM(HttpServletRequest req,
			HttpServletResponse resp)
	{
		boolean valid = true;
		String message = "OK";
		String cmd = req.getParameter("cmd");
		String name = req.getParameter("name");
		if(name != null) name = name.trim();
		String auto = req.getParameter("auto");
		String storage = req.getParameter("storage");
		String source = req.getParameter("source");
		String type = req.getParameter("type");
		String namecol = req.getParameter("namecol");
		String valcol = req.getParameter("valcol");
		String keycol = req.getParameter("keycol");
		String sql = req.getParameter("sql");
		String testdb = req.getParameter("testdb");
		String num = req.getParameter("num");
		
		UserDefinedMetrics udm = new UserDefinedMetrics(name);
		udm.setAuto("y".equals(auto));
		udm.setStoreInCommonTable("SHARED".equalsIgnoreCase(storage));
		udm.setSource(source);
		udm.setUdmType(type);
		udm.setNameCol(namecol);
		udm.setValueCol(valcol);
		udm.setKeyCol(keycol);
		udm.setSql(sql);
		
		int cnt = 0;
		try{cnt = Integer.parseInt(num);}catch(Exception ex){}
		for(int i=0;i<cnt; i++)
		{
			
			String mname = req.getParameter("mname_"+i);
			String mcol = req.getParameter("mcol_"+i);
			String minc = req.getParameter("minc_"+i);
			String mdata = req.getParameter("mdata_"+i);
			udm.addmetric(mname, mcol, "y".equalsIgnoreCase(minc), 
					Metric.strToMetricDataType(mdata));			
		}
		
		//now validation
		try
		{
			udm.validate();
		}catch(Exception ex)
		{
			valid = false;
			message = "Validation error: " + ex.getMessage();
		}
				
		if(valid) //now run test and store
		{
			try
			{
				this.testUDM(req, udm, testdb);
				//if reach here, add and store it.
				if("publish".equalsIgnoreCase(cmd))
				{
					this.frameworkContext.getMetricsDef().getUdmManager().addUDM(udm);
					this.frameworkContext.getMetricDb().addNewUDM(udm);
					this.frameworkContext.refreshMetricsList();
				}
			}catch(Exception ex)
			{
				logger.log(Level.WARNING, "Failed to test UDM", ex);
				valid = false;
				message = "Failed on test: "+ex.getMessage();
			}
		}
        
		ModelAndView mv = new ModelAndView(this.jsonView);
		if(req.getParameter("callback")!=null&&req.getParameter("callback").trim().length()>0)
		    mv.addObject("callback", req.getParameter("callback"));//YUI datasource binding
		if(valid)//we response with udm name
		    mv.addObject("json_result", "{\"resp\":{\"status\":0, \"udm\": \""+udm.getName()
		    		+ "\", \"message\":\"" + message + "\"}}");
		else
			mv = this.respondFailure(message, req);
		return mv;
	}
	private ModelAndView processNewAlert(HttpServletRequest req,
			HttpServletResponse resp)
	{
		boolean valid = true;
		String message = "OK";
		String cmd = req.getParameter("cmd");
		String name = req.getParameter("name");
		if(name != null) name = name.trim();
		String source = req.getParameter("source");
		String metricsName = req.getParameter("mname");
		String metricComparison = req.getParameter("mcomp");
		String metricValueType = req.getParameter("mval");
		String defaultThreshold = req.getParameter("mthreshold");
		String testdb = req.getParameter("testdb");
		String num = req.getParameter("num");
		
		AlertDefinition alertDef = new AlertDefinition(name);
		alertDef.setSource(source);
		if(AlertDefinition.SOURCE_METRICS.equals(source) || AlertDefinition.SOURCE_GLOBAL_STATUS.equals(source))
		{
			alertDef.setMetricName(metricsName);
			alertDef.setMetricComparison(metricComparison);
			alertDef.setMetricValueType(metricValueType);
			try{alertDef.setDefaultThreshold(Float.parseFloat(defaultThreshold));}catch(Exception iex){}
		}else
		{
			alertDef.setSqlText(req.getParameter("sqlText"));
			//alertDef.setSqlId(req.getParameter("sqlId"));
		
			int cnt = 0;
			try{cnt = Integer.parseInt(num);}catch(Exception ex){}
			for(int i=0;i<cnt; i++)
			{			
				String pname = req.getParameter("pname_"+i);
				String pval = req.getParameter("pval_"+i);
				alertDef.addParam(pname, pval);			
			}
		}
		//now validation
		try
		{
			alertDef.validate();
		}catch(Exception ex)
		{
			valid = false;
			message = "Validation error: " + ex.getMessage();
		}
				
		if(valid) //now run test and store
		{
			try
			{
				this.testAndValidateAlert(req, alertDef, testdb);
				//if reach here, add and store it.
				if("publish_alert".equalsIgnoreCase(cmd))
				{
					this.frameworkContext.getMetricsDef().getUdmManager().addAlertDefinition(alertDef);
					//this.frameworkContext.getMetricDb().addNewUDM(alertDef);
				}
			}catch(Exception ex)
			{
				logger.log(Level.WARNING, "Failed to test ALERT", ex);
				valid = false;
				message = "Failed on test: "+ex.getMessage();
			}
		}
        
		ModelAndView mv = new ModelAndView(this.jsonView);
		if(valid)//we response with udm name
		    mv.addObject("json_result", "{\"resp\":{\"status\":0, \"alert\": \""+alertDef.getName()
		    		+ "\", \"message\":\"" + message + "\"}}");
		else
			mv = this.respondFailure(message, req);
		return mv;
	}
	
}
