/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;
import com.yahoo.dba.perf.myperf.process.AutoScanner;

public class SettingsController extends MyPerfBaseController
{
  private static Logger logger = Logger.getLogger(SettingsController.class.getName());

  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
					HttpServletResponse resp) throws Exception 
  {
	AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
	String task = req.getParameter("task");
	if(!appUser.isAdminUser() && task !=  null && !task.isEmpty())
	{
	  return this.respondFailure("Only admin user can change auto scan configuration or start/stop scanner.", req);
	}
	if("update".equalsIgnoreCase(task))
	{
	  return handleUpdate(req,resp);
	}else if("updatesnmp".equalsIgnoreCase(task))
	{
      return handleUpdateSNMP(req,resp);	  
	}else if("fetchsnmp".equalsIgnoreCase(task))
	{
	      return handleFetchSNMP(req,resp);	  
	}
	else if(("start".equalsIgnoreCase(task)||"restart".equalsIgnoreCase(task))&& appUser.isAdminUser())
	{
	  logger.info("To start/restart scanner");
	  //need reconfig
	  this.frameworkContext.getMyperfConfig().init(this.frameworkContext);

	  AutoScanner scanner = this.frameworkContext.getAutoScanner();
	  if(scanner.isRunning())scanner.stop();
   	  if(this.frameworkContext.getMyperfConfig().isConfigured())
	  {
   		if("start".equalsIgnoreCase(task))
   		{
   		  logger.info("To start/restart metricsdb");
   		  this.frameworkContext.initMetricsDB();
  	      this.frameworkContext.getDbInfoManager().init(this.frameworkContext.getMetricDb()); //note we have moved db def into metrics db
  	      this.frameworkContext.getMetricsDef().getUdmManager().loadSubscriptions(this.frameworkContext);
   		  this.frameworkContext.getMetricDb().loadAlertSetting(this.frameworkContext.getAlertSettings());
   		}	 
  	    scanner.init();//re-init
  	    if(!scanner.isInitialized())
  		    return this.respondFailure("failed to use new configuration to initialize Auto Scanner Scheduler.", req);  	    	
		scanner.start();//TODO catch any exception
		return this.respondSuccess("Auto Scanner Scheduler has been started.", req);
	  }
   	  else
	  {
	    return this.respondFailure("failed to use new configuration.", req);
	  }			
	}
	else if("stop".equalsIgnoreCase(task)&& appUser.isAdminUser())
	{
	  AutoScanner scanner = this.frameworkContext.getAutoScanner();
	  if(scanner.isRunning())scanner.stop();
	  return this.respondSuccess("Auto Scanner Scheduler has been stopped.", req);
	}
			
	ModelAndView mv = new ModelAndView(this.getFormView());
	mv.addObject("config", this.frameworkContext.getMyperfConfig());
	mv.addObject("canupdate", appUser.isAdminUser()?1:0);
	mv.addObject("help_key", "autoscancfg");
	return mv;
 }
	
  private ModelAndView handleFetchSNMP(HttpServletRequest req,
		HttpServletResponse resp) {
	String dbgroup = req.getParameter("group");
	String host = req.getParameter("host");
	SNMPSettings.SNMPSetting snmpSetting = this.frameworkContext.getSnmpSettings().getHostSetting(dbgroup, host);
	ModelAndView mv = new ModelAndView(this.jsonView);
	String community = snmpSetting == null? SNMPSettings.SNMPSetting.DEFAULT_COMMUNITY: snmpSetting.getCommunity();
	String ver = snmpSetting == null? SNMPSettings.SNMPSetting.DEFAULT_VERSION: snmpSetting.getVersion();
	if(community == null || community.isEmpty())community = SNMPSettings.SNMPSetting.DEFAULT_COMMUNITY;
	if(ver == null || ver.isEmpty())ver = SNMPSettings.SNMPSetting.DEFAULT_VERSION;
	String enabled = snmpSetting == null? "yes":snmpSetting.getEnabled();
	StringBuilder sb = new StringBuilder();
	sb.append("{\"status\":0,\"message\":\"OK\", \"community\":\"").append(community)
	  .append("\",\"version\":\"").append(ver)
	  .append("\",\"enabled\":\"").append(enabled).append("\"");
	if("3".equals(ver))
	{
		if(snmpSetting.getUsername()!=null)
			sb.append(",\"username\":\"").append(snmpSetting.getUsername()).append("\"");
		if(snmpSetting.getAuthProtocol()!=null)
			sb.append(",\"authprotocol\":\"").append(snmpSetting.getAuthProtocol()).append("\"");
		if(snmpSetting.getPrivacyProtocol()!=null)
			sb.append(",\"privacyprotocol\":\"").append(snmpSetting.getPrivacyProtocol()).append("\"");
		if(snmpSetting.getContext()!=null)
			sb.append(",\"context\":\"").append(snmpSetting.getContext()).append("\"");
		//logger.info("Ommit pwd: "+snmpSetting.getPassword()+", "+snmpSetting.getPrivacyPassphrase());
	}
	sb.append("}");
    mv.addObject("json_result", sb.toString());
	return mv;
}

private ModelAndView handleUpdateSNMP(HttpServletRequest req,
		HttpServletResponse resp) {
	boolean successful = this.frameworkContext.getSnmpSettings().updateSnmpSetting(
			req.getParameter("group"), 
			req.getParameter("host"), 
			req.getParameter("community"), 
			req.getParameter("version"),
			req.getParameter("username"),
			req.getParameter("password"),
			req.getParameter("authprotocol"),
			req.getParameter("privacypassphrase"),
			req.getParameter("privacyprotocol"),
			req.getParameter("context"),
			req.getParameter("enabled")
			);
	if(successful)
		return this.respondSuccess("SNMP settings have been updated", req);
	else
		return this.respondFailure("Failed to update SNMP settings.", req);
  }

  private ModelAndView handleUpdate(HttpServletRequest req, HttpServletResponse resp)
  {
	//AutoScanner scanner = this.frameworkContext.getAutoScanner();
	MyPerfConfiguration config = this.frameworkContext.getMyperfConfig();
	String message = null;
	try
	{
	  String username = req.getParameter("username");
	  AppUser u = this.frameworkContext.getUserManager().getUser(username.toLowerCase());
	  if(u==null)
	    u = this.frameworkContext.getMetaDb().retrieveUserInfo(username.toLowerCase());
	  if(u==null)
	  {
	    return this.respondFailure("The system cannot find the user "+username, req);
	  }
      if("mysql".equalsIgnoreCase(req.getParameter("metricsDbType")))
	  {
    	  message = this.testMetricsDB(req.getParameter("metricsDbHost"),
    			  req.getParameter("metricsDbPort"), 
    			  req.getParameter("metricsDbName"), 
    			  req.getParameter("metricsDbUserName"),
    			  req.getParameter("metricsDbPassword"), config);
    	  if(message != null && !message.isEmpty())
    		  return this.respondFailure(message, req);
	  }
      config.setMetricsDbUserName(username.toLowerCase());
      config.setAdminEmail(req.getParameter("adminemail"));
	  int retentionDays = 60;
	  try
	  {
		retentionDays = Integer.parseInt(req.getParameter("runtimeRecordRententionCount"));
	  }catch(Exception ex){}
	  if(retentionDays<1)retentionDays = 60;	
	  config.setRecordRententionDays(retentionDays);
	  int scanInterval = 300;
	  try
	  {
	    scanInterval = Integer.parseInt(req.getParameter("runtimeScanIntervalSeconds"));
	  }catch(Exception ex){}
	  if(scanInterval<1)scanInterval = 300;
		config.setScannerIntervalSeconds(scanInterval);
	  int alertScanInterval = 300;
	  try
	  {
		  alertScanInterval = Integer.parseInt(req.getParameter("alertScanIntervalSeconds"));
	  }catch(Exception ex){}
	  if(alertScanInterval < 60)alertScanInterval = 300;
	  config.setAlertScanIntervalSeconds(alertScanInterval);
	  
	  int threadCount = 4;
	  try
	  {
		threadCount = Integer.parseInt(req.getParameter("threadCount"));
	  }catch(Exception ex){}
	  if(threadCount<1)threadCount = 4;
	  config.setScannerThreadCount(threadCount);
	  config.setMetricsDbType(req.getParameter("metricsDbType"));
	  config.setAlertNotificationEmails(req.getParameter("notificationEmails"));
	  config.setReuseMonUserConnction(!"n".equalsIgnoreCase(req.getParameter("reuseMonUserConnction")));
	  if("mysql".equalsIgnoreCase(config.getMetricsDbType()) && dbChanged(req))
	  {
		  config.setMetricsDbHost(req.getParameter("metricsDbHost"));
		  try
		  {
			config.setMetricsDbPort(Integer.parseInt(req.getParameter("metricsDbPort")));
 		  }catch(Exception ex){}
  		  config.setMetricsDbName(req.getParameter("metricsDbName"));
		  config.setMetricsDbUserName(req.getParameter("metricsDbUserName"));
		  String pwd = req.getParameter("metricsDbPassword");
		  if(pwd!=null && !pwd.isEmpty())
			config.setMetricsDbPassword(req.getParameter("metricsDbPassword"));
	  }
	//hipchat
	  {
		  boolean hipchatChanged = false;
		  String hipchatUrl = req.getParameter("hipchatUrl");
		  if(hipchatUrl != null && !hipchatUrl.isEmpty()){
			  hipchatUrl = hipchatUrl.trim();
			  if(!hipchatUrl.endsWith("?"))
				  hipchatUrl += "?";
		  }
		  if((hipchatUrl == null && config.getHipchatUrl() != null)
				  || (hipchatUrl != null && !hipchatUrl.equals(config.getHipchatUrl())))
				  hipchatChanged = true;
		  config.setHipchatUrl(hipchatUrl);
		  //only change authtoken if provided.
		  String hipchatAuthToken = req.getParameter("hipchatAuthToken");
		  if(hipchatAuthToken != null)hipchatAuthToken = hipchatAuthToken.trim();
		  if(hipchatAuthToken != null && !hipchatAuthToken.isEmpty())
		  {
			  if(!hipchatAuthToken.equals(config.getHipchatAuthToken()))
				  hipchatChanged = true;
			  config.setHipchatAuthToken(hipchatAuthToken);
		  }
		  if(hipchatChanged)
		  {
			  logger.info("HipChat configuration changed to " + config.getHipchatUrl());
			  this.frameworkContext.getHipchat().init(this.frameworkContext);
		  }
	  }
	  config.store(this.frameworkContext);		
	  message = "The configuration has been updated. Please (re)start the auto scanner scheduler.";
	  return this.respondSuccess(message, req);
	}catch(Exception ex)
	{
	  logger.log(Level.WARNING, "exception when update the auto scan configuration", ex);
	  message = "There is an exception when update the configuration: "+ex.getMessage();			
	}
	return this.respondFailure(message, req);
  }

  private boolean dbChanged(HttpServletRequest req){
	MyPerfConfiguration config = this.frameworkContext.getMyperfConfig();
	try
	{
		if(!req.getParameter("metricsDbType").equals(config.getMetricsDbType()))
			return true;
		if(!req.getParameter("metricsDbHost").equals(config.getMetricsDbHost()))
			return true;
		if(!req.getParameter("metricsDbPort").equals(config.getMetricsDbPort()))
			return true;
		if(!req.getParameter("metricsDbName").equals(config.getMetricsDbName()))
			return true;
		if(!req.getParameter("metricsDbUserName").equals(config.getMetricsDbUserName()))
			return true;
		
	}catch(Exception ex)
	{
		
	}
	return false;
  }
  private String testMetricsDB(String host, String port, String schema, String user, String pwd, MyPerfConfiguration config)
  {
	int toTest = 0; //test if only there is a change
	if(config == null)toTest = 1;
	else if(!"mysql".equalsIgnoreCase(config.getMetricsDbType()))
		toTest = 2;
	else if(host != null && !host.equalsIgnoreCase(config.getMetricsDbHost()))
		toTest = 3;
	else if(port != null && !port.equalsIgnoreCase(String.valueOf(config.getMetricsDbPort())))
		toTest = 4;
	else if(schema!=null && !schema.equalsIgnoreCase(config.getMetricsDbName()))
		toTest = 5;
	else if(user != null && !user.equalsIgnoreCase(config.getMetricsDbUserName()))
		toTest = 6;
	else if(pwd != null && !pwd.isEmpty() && !pwd.equalsIgnoreCase(config.getMetricsDbPassword()))
		toTest = 7;
	if(toTest == 0)return null;
	logger.info("MetricsDB changed, need restest. Reason: "+toTest);
	DBInstanceInfo dbinfo = new DBInstanceInfo();
	dbinfo.setDbGroupName("test_only");
	dbinfo.setHostName(host);
	dbinfo.setPort(port);
	dbinfo.setDatabaseName(schema);
	return DBUtils.testConnection(dbinfo, user, pwd, true);	
  }
  
}
