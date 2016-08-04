/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;
import com.yahoo.dba.perf.myperf.process.AutoScanner;

/**
 * Retrieve alerts data from metricsDB
 * @author xrao
 *
 */

public class AlertsController extends MyPerfBaseController
{
  private static Logger logger = Logger.getLogger(AlertsController.class.getName());
	
  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
			HttpServletResponse resp) throws Exception 
  {
	
	String cmd = req.getParameter("cmd");
	if("get_settings".equalsIgnoreCase(cmd))
		return handleRetrieveSettings(req, resp);
	else if("update_settings".equalsIgnoreCase(cmd))
		return handleUpdateSettings(req, resp);
	else if("metrics_enabled_yes".equalsIgnoreCase(cmd)||"metrics_enabled_no".equalsIgnoreCase(cmd))
		return enableMetrics(req, resp);
	else if("alerts_enabled_yes".equalsIgnoreCase(cmd)||"alerts_enabled_no".equalsIgnoreCase(cmd))
		return enableAlerts(req, resp);
	else if("snmp_enabled_yes".equalsIgnoreCase(cmd)||"snmp_enabled_no".equalsIgnoreCase(cmd))
		return enableSNMP(req, resp);
	else if("get_top".equalsIgnoreCase(cmd))
	    return this.handleRetrieveNewAlerts(req, resp);
    int status = Constants.STATUS_OK;
	String message = "OK";
	QueryParameters qps = null;
	ResultList rList = null;

	qps = WebAppUtil.parseRequestParameter(req);

	AppUser appUser = retrieveAppUser(req);
	HashSet<String> filteredGroups = new HashSet<String>();
	boolean useFilter = false;
	if(appUser != null && appUser.isRestrictedUser())
	{
		useFilter = true;
		Set<String> mydbs = this.frameworkContext.getDbInfoManager()
				.getMyDatabases(appUser.getName(), appUser.isRestrictedUser()).getMyDbList();
		if(mydbs != null)
		{
			for(String s: mydbs)
				filteredGroups.add(s);
		}
	}

	//starting and ending dates, and their defaults. All values are in UTC, assume format "yyyy-MM-dd HH"
    String startDate = req.getParameter("start");
	String endDate = req.getParameter("end");
	List<Integer> dbIdList = new ArrayList<Integer>();
	Map<Integer, DBInstanceInfo> dbs = new HashMap<Integer, DBInstanceInfo>();
	do
	{
	
		try
		{
			if(qps.getGroup()!=null && !qps.getGroup().isEmpty() && !"All".equalsIgnoreCase(qps.getGroup()))
			{
				if(qps.getHost()!=null && !qps.getHost().isEmpty())
				{
					DBInstanceInfo dbinfo = this.frameworkContext.getDbInfoManager().findDB(qps.getGroup(), qps.getHost());
					if(dbinfo!=null)
					{
						if(!useFilter || filteredGroups.contains(dbinfo.getDbGroupName()))
						{
							dbIdList.add(dbinfo.getDbid());
							DBInstanceInfo dbinfo2 = new DBInstanceInfo();
							dbinfo2.setDbGroupName(dbinfo.getDbGroupName());
							dbinfo2.setHostName(dbinfo.getHostName());
							dbinfo2.setDbid(dbinfo.getDbid());
							dbs.put(dbinfo2.getDbid(), dbinfo2);
						}
					}
				}
				if(dbIdList.size()==0)
				{
					for(DBInstanceInfo dbinfo:this.frameworkContext.getDbInfoManager().findGroup(qps.getGroup()).getInstances())
					{
						if(!useFilter || filteredGroups.contains(dbinfo.getDbGroupName()))
						{
							dbIdList.add(dbinfo.getDbid());
							DBInstanceInfo dbinfo2 = new DBInstanceInfo();
							dbinfo2.setDbGroupName(dbinfo.getDbGroupName());
							dbinfo2.setHostName(dbinfo.getHostName());
							dbinfo2.setDbid(dbinfo.getDbid());
							dbs.put(dbinfo2.getDbid(), dbinfo2);
						}
					}
				}
			}
			if(dbIdList.size()==0)//if at this stage still empty, it is for all
			{
				for(String grp: this.frameworkContext.getDbInfoManager().listGroupNames())
				for(DBInstanceInfo dbinfo:this.frameworkContext.getDbInfoManager().findGroup(grp).getInstances())
				{
					if(!useFilter || filteredGroups.contains(dbinfo.getDbGroupName()))
					{
						DBInstanceInfo dbinfo2 = new DBInstanceInfo();
						dbinfo2.setDbGroupName(dbinfo.getDbGroupName());
						dbinfo2.setHostName(dbinfo.getHostName());
						dbinfo2.setDbid(dbinfo.getDbid());
						dbs.put(dbinfo2.getDbid(), dbinfo2);
					}
				}
			}
		}catch(Exception ex){}
		//when we reach here, at least we have valid query and can connect to db	
		WebAppUtil.storeLastDbInfoRequest(qps.getGroup(),qps.getHost(), req);
		
		//if we reach here, processing starting and ending dates, and their defaults		
		String[] dateRange = MyPerfBaseController.getDateRange(startDate, endDate);
		
		logger.info("query alerts between "+dateRange[0]+", "+dateRange[1]+")");
	
	
		AutoScanner as = this.frameworkContext.getAutoScanner();
		rList = as.getMetricDb().retrieveAlerts(dateRange[0], dateRange[1], dbs, dbIdList);
	    if(rList==null)
	    {
	    	status = Constants.STATUS_BAD;
	    	message = "No alert data available";
	    }
	    break;//end of the flow
	}while(true);
	
	if(status == Constants.STATUS_BAD)
		return this.respondFailure(message, req);

	ModelAndView mv = null;	
	mv = new ModelAndView(this.jsonView);
	mv.addObject("json_result", ResultListUtil.toJSONString(rList, qps, status, message));
	return mv;			
  }
  
  private ModelAndView handleRetrieveNewAlerts(HttpServletRequest req,
			HttpServletResponse resp)
  {
	  Object tsObj =  req.getSession().getAttribute("LAST_ALERT_REQ_STAMP");
	  long ts = System.currentTimeMillis() - 300000; //5 minutes back
	  if(tsObj != null)ts = Long.parseLong(tsObj.toString());
	  
	  //only want 10
	  List<AlertEntry> alerts = this.frameworkContext.getAlerts().getEntries(ts, 10);
	  if(alerts.size()>0)
	  {
		  //update session ts
		  req.getSession().setAttribute("LAST_ALERT_REQ_STAMP", alerts.get(0).getTs());
		  //now build response
		  ColumnDescriptor desc = new ColumnDescriptor();
		  desc.addColumn("TS", false, 1);
		  desc.addColumn("ALERT", false, 2);
		  desc.addColumn("VALUE", false, 3);
		  desc.addColumn("GROUP", false, 4);
		  desc.addColumn("HOST", false, 5);
		  
		  ResultList rList = new ResultList();
		  rList.setColumnDescriptor(desc);
		  for(AlertEntry e: alerts)
		  {
			  ResultRow row = new ResultRow();
			  row.setColumnDescriptor(desc);
			  row.addColumn(e.getAlertTime());
			  row.addColumn(e.getAlertReason());
			  row.addColumn(e.getAlertValue());
			  row.addColumn(e.getDbGroup());
			  row.addColumn(e.getDbHost());
			  rList.addRow(row);
		  }
		  ModelAndView mv = new ModelAndView(this.jsonView);
		  mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, 0, "OK"));
		  return mv;
	  }else
	  {
		  return this.respondSuccess("No Alerts", req);
	  }
  }
  private ModelAndView handleRetrieveSettings(HttpServletRequest req,
			HttpServletResponse resp)
  {
	  ModelAndView mv = new ModelAndView(this.jsonView);
	  AlertSettings as = this.getFrameworkContext().getAlertSettings();
	  String group = req.getParameter("group");
	  String host = req.getParameter("host");
	  
	  boolean updatable = true;
	  if(group==null || group.isEmpty() || "all".equals(group))
		  updatable = false;
	  else
	  {
		  AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
		  if(!appUser.isAdminUser())
			  updatable = false;
	  }
	  DBInstanceInfo dbinfo = null;
	  if(group !=null && !group.isEmpty() && !"all".equals(group) && host != null && !host.isEmpty())
	    dbinfo = this.getFrameworkContext().getDbInfoManager().findDB(group, host);
	  
	  Float cpu = as.getAlertThreshold(group, host, "CPU");
	  Float io = as.getAlertThreshold(group, host, "IO");
	  Float loadavg = as.getAlertThreshold(group, host, "LOADAVG");
	  Float slow = as.getAlertThreshold(group, host, "SLOW");
	  Float deadlock = as.getAlertThreshold(group, host, "DEADLOCK");
	  Float swapout = as.getAlertThreshold(group, host, "SWAPOUT");
	  Float repllag = as.getAlertThreshold(group, host, "REPLLAG");
	  Float thread = as.getAlertThreshold(group, host, "THREAD");
	  Float diskusage = as.getAlertThreshold(group, host, "DISKUSAGE");
	  Float aborted_cc = as.getAlertThreshold(group, host, "CONNECT_FAILURE");
	  StringBuilder sb = new StringBuilder();
	  sb.append("{");
	  sb.append("\"message\":\"OK\",");
	  sb.append("\"updatable\":\"").append(updatable?"y":"n").append("\",");
	  sb.append("\"cpu\":\"").append(Math.round(cpu)).append("\",");
	  sb.append("\"io\":\"").append(Math.round(io)).append("\",");
	  sb.append("\"loadavg\":\"").append(Math.round(loadavg)).append("\",");
	  sb.append("\"diskusage\":\"").append(Math.round(diskusage)).append("\",");
	  sb.append("\"thread\":\"").append(Math.round(thread)).append("\",");
	  sb.append("\"slow\":\"").append(Math.round(slow)).append("\",");
	  sb.append("\"deadlock\":\"").append(Math.round(deadlock)).append("\",");
	  sb.append("\"swapout\":\"").append(Math.round(swapout)).append("\",");
	  sb.append("\"repllag\":\"").append(Math.round(repllag)).append("\",");
	  sb.append("\"aborted_cc\":\"").append(Math.round(aborted_cc)).append("\",");
	  sb.append("\"emails\":\"").append(as.getNotificationEmails(group, host)).append("\"");
	  
	  if(dbinfo != null)
	  {
		  sb.append(",\"alerts\":\"").append(dbinfo.isAlertEnabled()?"y":"n").append("\"");
		  sb.append(",\"metrics\":\"").append(dbinfo.isMetricsEnabled()?"y":"n").append("\"");
		  sb.append(",\"snmp\":\"").append(dbinfo.isSnmpEnabled()?"y":"n").append("\"");
	  }
	  sb.append("}");
	  
	  mv.addObject("json_result", sb.toString());
	  return mv;
  }
  private ModelAndView handleUpdateSettings(HttpServletRequest req,
			HttpServletResponse resp)
  {
	  String message = null;
	  int status = 0;
	  AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
	  String group = req.getParameter("group");
	  if(!appUser.isAdminUser())
	  {
		 message = "Only admin user can update alert threshold settings.";
	  }else if(group==null || group.isEmpty() || "all".equals(group))
	  {
		  message = "Cannot update system default settings.";
		  status = -1;
	  }else
	  {
	  
		  String host = req.getParameter("host");
		  try
		  {
			  AlertSettings as = this.getFrameworkContext().getAlertSettings();
			  as.updateAlertThreshold(group, host, "CPU", Float.parseFloat(req.getParameter("cpu")), true);
			  as.updateAlertThreshold(group, host, "IO", Float.parseFloat(req.getParameter("io")), true);
			  as.updateAlertThreshold(group, host, "LOADAVG", Float.parseFloat(req.getParameter("loadavg")), true);
			  as.updateAlertThreshold(group, host, "DISKUSAGE", Float.parseFloat(req.getParameter("diskusage")), true);
			  as.updateAlertThreshold(group, host, "THREAD", Float.parseFloat(req.getParameter("thread")), true);
			  as.updateAlertThreshold(group, host, "REPLLAG", Float.parseFloat(req.getParameter("repllag")), true);
			  as.updateAlertThreshold(group, host, "SLOW", Float.parseFloat(req.getParameter("slow")), true);
			  as.updateAlertThreshold(group, host, "DEADLOCK", Float.parseFloat(req.getParameter("deadlock")), true);
			  as.updateAlertThreshold(group, host, "SWAPOUT", Float.parseFloat(req.getParameter("swapout")), true);
			  as.updateAlertThreshold(group, host, "CONNECT_FAILURE", Float.parseFloat(req.getParameter("aborted_cc")), true);
			  as.updateAlertNotification(group, host, req.getParameter("emails"), true);
			  message = "alert settings have been updated. ";
		  }catch(Exception ex)
		  {
			  message = "Error when update alert settings: "+ex.getMessage();
			  status = -1;
		  }
	  }
	  return this.respondWithStatus(status, message, req);
  }

  private ModelAndView enableMetrics(HttpServletRequest req,
			HttpServletResponse resp)
  {
	  String message = null;
	  AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
	  String group = req.getParameter("group");
	  String host = req.getParameter("host");
	  String cmd = req.getParameter("cmd");
	  int status = -1;
	  do
	  {
  	    if(!appUser.isAdminUser())
	    {
		   message = "Only admin user can update metrics settings.";
		   break;
	    }
  	    if(group==null || group.isEmpty() || "all".equals(group))
	    {
		  message = "No database server group is provided.";
		  break;
	    }
  	    if(host == null || host.isEmpty())
		{
		  message = "No database server host name is provided.";
		  break;
		}
	    DBInstanceInfo dbinfo = this.getFrameworkContext().getDbInfoManager().findDB(group, host);
	    if(dbinfo == null)
	    {
		   message = "Cannot find  database server.";
	       break;	
	    }
		try
		{
		   if(this.getFrameworkContext().getMetricDb().enableMetrics(group, host, "metrics_enabled_yes".equalsIgnoreCase(cmd), 
				   appUser.getName(), true))
		   {
			   dbinfo.setMetricsEnabled( "metrics_enabled_yes".equalsIgnoreCase(cmd));
		   }else
		   {
			   message = "Failed to update metrics setting";
			   break;
		   }
		}catch(Exception ex)
		{
			
			message = "Failed to update metrics setting: "+ex.getMessage();
			break;
		}
		message = "Metrics gathering for ("+group+", "+host + ") has been " + ("enable_metrics".equalsIgnoreCase(cmd)? "enabled": "disabled");
		status = 0;
		break;
	  }while(false);
	  return this.respondWithStatus(status, message, req);
  }
  private ModelAndView enableAlerts(HttpServletRequest req,
			HttpServletResponse resp)
  {
	  String message = null;
	  AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
	  String group = req.getParameter("group");
	  String host = req.getParameter("host");
	  String cmd = req.getParameter("cmd");
	  int status = -1;
	  do
	  {
	    if(!appUser.isAdminUser())
	    {
		   message = "Only admin user can update alert settings.";
		   break;
	    }
	    if(group==null || group.isEmpty() || "all".equals(group))
	    {
		  message = "No database server group is provided.";
		  break;
	    }
	    if(host == null || host.isEmpty())
		{
		  message = "No database server host name is provided.";
		  break;
		}
	    DBInstanceInfo dbinfo = this.getFrameworkContext().getDbInfoManager().findDB(group, host);
	    if(dbinfo == null)
	    {
		   message = "Cannot find  database server.";
	       break;	
	    }
		try
		{
		   if(this.getFrameworkContext().getMetricDb().enableAlerts(group, host, "alerts_enabled_yes".equalsIgnoreCase(cmd), 
				   appUser.getName(), true))
		   {
			   dbinfo.setAlertEnabled("alerts_enabled_yes".equalsIgnoreCase(cmd));
		   }else
		   {
			   message = "Failed to update alert setting";
			   break;
		   }
		   
		}catch(Exception ex)
		{
			
			message = "Failed to update alert setting: "+ex.getMessage();
			break;
		}
		message = "Alerts for ("+group+", "+host + ") has been " + ("enable_alerts".equalsIgnoreCase(cmd)? "enabled": "disabled");
		status = 0;
		break;
	  }while(false);
	  return this.respondWithStatus(status, message, req);
  }
  
  private ModelAndView enableSNMP(HttpServletRequest req,
			HttpServletResponse resp)
{
	  String message = null;
	  AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
	  String group = req.getParameter("group");
	  String host = req.getParameter("host");
	  String cmd = req.getParameter("cmd");
	  int status = -1;
	  do
	  {
	    if(!appUser.isAdminUser())
	    {
		   message = "Only admin user can update snmp settings.";
		   break;
	    }
	    if(group==null || group.isEmpty() || "all".equals(group))
	    {
		  message = "No database server group is provided.";
		  break;
	    }
	    if(host == null || host.isEmpty())
		{
		  message = "No database server host name is provided.";
		  break;
		}
	    DBInstanceInfo dbinfo = this.getFrameworkContext().getDbInfoManager().findDB(group, host);
	    if(dbinfo == null)
	    {
		   message = "Cannot find  database server.";
	       break;	
	    }
		try
		{
		   if(this.getFrameworkContext().getMetricDb().enableSnmp(group, host, "snmp_enabled_yes".equalsIgnoreCase(cmd), 
				   appUser.getName(), true))
		   {
			   dbinfo.setSnmpEnabled("snmp_enabled_yes".equalsIgnoreCase(cmd));
		   }else
		   {
			   message = "Failed to update snmp setting";
			   break;
		   }
		   
		}catch(Exception ex)
		{
			
			message = "Failed to update snmp setting: "+ex.getMessage();
			break;
		}
		message = "SNMP gathering for ("+group+", "+host + ") has been " + ("enable_alerts".equalsIgnoreCase(cmd)? "enabled": "disabled");
		status = 0;
		break;
	  }while(false);
	  return this.respondWithStatus(status, message, req);
}
}

