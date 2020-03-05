/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.File;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.yahoo.dba.perf.myperf.db.QueryExecutor;
import com.yahoo.dba.perf.myperf.meta.MetaDB;
import com.yahoo.dba.perf.myperf.metrics.DerbyMetricsDb;
import com.yahoo.dba.perf.myperf.metrics.MetricsDbBase;
import com.yahoo.dba.perf.myperf.metrics.MySQLMetricsDb;
import com.yahoo.dba.perf.myperf.process.AutoScanner;

/**
 * MyPerfContext contains configuration data, accessor to common resources,
 * and resource initialization.
 * @author xrao
 *
 */
public class MyPerfContext implements java.io.Serializable, InitializingBean,DisposableBean  {

  private static final long serialVersionUID = 1L;
  //Use this parameter to control debug log at system level at runtime
  //This way, we don't need change and load our log configuration
  private java.util.concurrent.atomic.AtomicBoolean DEBUG = new java.util.concurrent.atomic.AtomicBoolean();

  private MyPerfConfiguration myperfConfig = new MyPerfConfiguration(); //configurations, from settings page
  private SNMPSettings snmpSettings = new SNMPSettings();
  private DBInfoManager dbInfoManager = new DBInfoManager(); //managed database servers
  private SqlManager sqlManager = new SqlManager();//built in SQL definitions
  private MetricsDefManager metricsDef = new MetricsDefManager();//metrics and alert definitions, builtin and user defined
  private MetaDB metaDb = new MetaDB(); //meta data, now only hold users and db credentials
  private MetricsDbBase metricDb; //database to store metrics and managed database server info
  private QueryExecutor queryEngine = new QueryExecutor(); //Run queries using JDBC
  private AppUserManager userManager = new AppUserManager(); //managed registered users
  private Auth auth = new Auth();//user authentication 
  
  private StatDefManager statDefManager = new StatDefManager();//help tool for terminology lookup

  private AutoScanner autoScanner = null; //metrics and alert scanner
  private AlertSettings alertSettings = new AlertSettings(); //alert thresholding settings

  //for each server, we keep two snapshots of selected metrics
  private InstanceStatesManager instanceStatesManager = new InstanceStatesManager();
  
  //location to find built in sql
  private String sqlPath="sql.xml";
	
  //directory where we can save files, reports, etc.
  private String fileReposirtoryPath="myperf_reports";
  private File alertRootPath;
  
  //database connection settings
  private long connectionIdleTime = 600000L;
  private long connectionTimeout = 5000L;//reduced to 5 seconds
  private long connectionReadTimeout = 10000L;
  private int queryTimeout = 300;
  private int queryFetchSize = 5000;

  //log settings
  private String logLevel = "INFO";
  private String logPath = "perf.log";
  private int logFileSize = 50000000;
  private int logFileCount = 5;
  
  private String configRepKey;

  private Map<String, String> jdbcDriver;
  
  private java.util.Date startTime = new java.util.Date();//when this server started

  //metrics list
  private List<String> metricsList;
  private Object metricsListRefreshLock = new Object();

  //hold alerts for notification
  private Alerts alerts = new Alerts();
  
  //attach HipchatIntegration to email alert
  private HipchatIntegration hipchat = new HipchatIntegration();
  
  public MyPerfContext()
  {
    DEBUG.set(false);
  }

  public void enableDebug(boolean enable)
  {
    DEBUG.set(enable); 
  }
  
  public boolean isDebug()
  {
	  return DEBUG.get();
  }
  
  public DBInfoManager getDbInfoManager() 
  {
    return dbInfoManager;
  }

  public SqlManager getSqlManager() 
  {
    return sqlManager;
  }

  /**
   * All the top level beans, sql manager, connection manage, db manager, etc, will be initialized here.
   */
  public void afterPropertiesSet() throws Exception 
  {
    configureLogging();
	Logger logger = Logger.getLogger(this.getClass().getName());		
	logger.info("Setup afterPropertiesSet.");

	logger.info("Loading JDBC Drivers");
	if(this.jdbcDriver!=null)
	{
	   DriverManager.setLoginTimeout(60);
	   logger.info("Set JDBC DriverManager loginTimeout as 60 seconds");
	   for(Map.Entry<String, String> e: jdbcDriver.entrySet())
	   {
		  logger.info("Loading "+e.getKey()+": "+e.getValue());
		  try
		  {
		    Class.forName(e.getValue());	  
			logger.info("Loaded "+e.getKey()+": "+e.getValue());
		  }catch(Throwable ex)
		  {
		    logger.info("Failed to Load "+e.getKey()+": "+e.getValue());			  
		  }
	   }
	}
	
	alertRootPath = new File(new File(this.fileReposirtoryPath), "alerts");
	alertRootPath.mkdirs();
	
	this.sqlManager.setSqlPath(sqlPath);
	this.sqlManager.init();
	
	this.metricsDef.init();
	logger.info("Refreshing metrics list ...");
	refreshMetricsList();
	logger.info("Retrieved metrics list: " + this.metricsList.size());

	this.metaDb.setDbkey(this.configRepKey);
	this.metaDb.init();
	
	this.dbInfoManager.setMetaDb(metaDb);//since we store db info now in metricsdb, 
	        //it can only be initialized after metricsDB initialized 
	
	this.userManager.setMetaDb(metaDb);
	this.auth.setContext(this);	
	this.queryEngine.setSqlManager(this.sqlManager);
	this.queryEngine.setFrameworkContext(this);
    this.statDefManager.init();
    
	logger.info("Initialize AutoScanner ...");
	this.myperfConfig.init(this);
	this.snmpSettings.init(this);
	if(this.myperfConfig.isConfigured())
	{
	  this.initMetricsDB(); //move metrics db creation and initialization away from scanner
      this.alertSettings.setContext(this);
	  if(this.metricDb != null)
	  {
	    this.dbInfoManager.init(this.metricDb);//need metricsDB to update
		this.metricsDef.getUdmManager().loadSubscriptions(this);
    	this.metricDb.loadAlertSetting(this.alertSettings);//load alert setting after DB info is loaded.
	  }
	}
    this.instanceStatesManager.init(this);
	this.hipchat.init(this);
	
	autoScanner = new AutoScanner(this);
	autoScanner.init();//it will update metricsDB
	if(autoScanner.isInitialized())
	{
	  logger.info("Starting AutoScanner ...");
	  autoScanner.start();
    }
	
	logger.info("Done setup afterPropertiesSet.");
  }

  public String getSqlPath() 
  {
    return sqlPath;
  }

  public void setSqlPath(String sqlPath) 
  {
    this.sqlPath = sqlPath;
  }

  public MetaDB getMetaDb() 
  {
    return metaDb;
  }

  public void destroy() throws Exception 
  {
    if(this.autoScanner!=null)this.autoScanner.stop();
    if(this.metricDb != null)this.metricDb.destroy();
 	this.metaDb.destroy();
  }

  public QueryExecutor getQueryEngine() 
  {
    return queryEngine;
  }

  public AppUserManager getUserManager() 
  {
    return userManager;
  }

  public long getConnectionIdleTime() 
  {
    return connectionIdleTime;
  }

  public void setConnectionIdleTime(long connectionIdleTime) 
  {
    if(connectionIdleTime<=0)
	  this.connectionIdleTime = 600000L;
	else
	  this.connectionIdleTime = connectionIdleTime;
  }

  public long getConnectionTimeout() 
  {
    return connectionTimeout;
  }

  public void setConnectionTimeout(long connectionTimeout) 
  {
    this.connectionTimeout = connectionTimeout;
  }

  public long getConnectionReadTimeout() 
  {
    return connectionReadTimeout;
  }

  public void setConnectionReadTimeout(long connectionReadTimeout) 
  {
    this.connectionReadTimeout = connectionReadTimeout;
  }

  public int getQueryTimeout() 
  {
    return queryTimeout;
  }

  public void setQueryTimeout(int queryTimeout) 
  {
    this.queryTimeout = queryTimeout;
  }

  public int getQueryFetchSize() 
  {
    return queryFetchSize;
  }

  public void setQueryFetchSize(int queryFetchSize) 
  {
    this.queryFetchSize = queryFetchSize;
  }

  public String getLogLevel() 
  {
    return logLevel;
  }

  public void setLogLevel(String logLevel) 
  {
    this.logLevel = logLevel;
  }

  public String getLogPath() 
  {
    return logPath;
  }

  public void setLogPath(String logPath) 
  {
    this.logPath = logPath;
  }

  private  void configureLogging()
  {
	Logger logger = Logger.getLogger("");
	try
	{
	  logger.setLevel(Level.parse(getLogLevel()));
	}catch(Exception ex)
	{
	  logger.setLevel(Level.INFO);			
	}
	try
	{
	  for(Handler h:logger.getHandlers())
	  {
	    if(h instanceof java.util.logging.ConsoleHandler)
		  h.setLevel(Level.SEVERE);
	  }
	  String logRoot = System.getProperty("logPath", ".");
	  
	  java.util.logging.FileHandler fileHandler = new  java.util.logging.FileHandler(
			  logRoot+File.separatorChar+getLogPath(),this.logFileSize, this.logFileCount);
	  fileHandler.setLevel(logger.getLevel());
	  fileHandler.setFormatter(new SimpleFormatter());
	  logger.addHandler(fileHandler);
	}catch(Exception ex)
	{
	  ex.printStackTrace();
	}
  }

  public int getLogFileSize() 
  {
    return logFileSize;
  }

  public void setLogFileSize(int logFileSize) 
  {
    this.logFileSize = logFileSize;
  }

  public int getLogFileCount() 
  {
    return logFileCount;
  }

  public void setLogFileCount(int logFileCount) 
  {
    this.logFileCount = logFileCount;
  }

  public String getConfigRepKey() 
  {
    return configRepKey;
  }

  public void setConfigRepKey(String configRepKey) 
  {
    this.configRepKey = configRepKey;
  }

  public Map<String, String> getJdbcDriver() {
	return jdbcDriver;
  }

  public void setJdbcDriver(Map<String, String> jdbcDriver) {
	this.jdbcDriver = jdbcDriver;
  }
  public StatDefManager getStatDefManager()
  {
    return this.statDefManager;
  }

  public AutoScanner getAutoScanner() 
  {
    return autoScanner;
  }

  public MetricsDbBase getMetricDb() 
  {
	return metricDb;
  }

  public void setMetricDbBase(MetricsDbBase metricDb) 
  {
	this.metricDb = metricDb;
  }

  public java.util.Date getStartTime() 
  {
	return startTime;
  }

  public String getFileReposirtoryPath() {
	return fileReposirtoryPath;
  }

  public void setFileReposirtoryPath(String fileReposirtoryPath) {
	this.fileReposirtoryPath = fileReposirtoryPath;
  }


  public List<String> getMetricsList()
  {
	synchronized(metricsListRefreshLock)
	{
		return java.util.Collections.unmodifiableList(metricsList);
	}
  }
  public void refreshMetricsList()
  {
	synchronized(metricsListRefreshLock)
	{
		if(metricsList==null)
			metricsList = new ArrayList<String>();
		else
			metricsList.clear();
	  //user defined:
	  //udm list
	  for(Map.Entry<String, UserDefinedMetrics> e: 
		  this.getMetricsDef().getUdmManager().getUdms().entrySet())
	  {
		  String key = e.getKey();
		  UserDefinedMetrics udm = e.getValue();
		  for(Metric m: udm.getMetrics())
			  metricsList.add("UDM."+key+"."+m.getName());
	  }
	  
	  String[] mgNames = this.metricsDef.getGroupNames();
	  for(String mgName: mgNames)
	  {
		  MetricsGroup mg = this.metricsDef.getGroupByName(mgName);
		  if (mg == null )continue;
		  if (mg.getSubGroups() != null && mg.getSubGroups().size() >0)
		  {
		    //deal with sub groups
			for (MetricsGroup subGrp: mg.getSubGroups())
			{
			  for(Metric m: subGrp.getMetrics())
		        metricsList.add(mg.getGroupName().toUpperCase()+"." 
		        		+ subGrp.getGroupName() + "."+m.getName());			  	  
			}
		  }else
		  {
		    //deal with top level metrics with name convention: {group name}._.{metric name}
			for(Metric m: mg.getMetrics())
			  metricsList.add(mg.getGroupName().toUpperCase()+"._."+m.getName());
		  }		  
	  }
	  java.util.Collections.sort(metricsList);
	}
  }

  public InstanceStatesManager getInstanceStatesManager() {
	return instanceStatesManager;
  }

  public File getAlertRootPath() {
	return alertRootPath;
  }

  public void setAlertRootPath(File alertRootPath) {
	this.alertRootPath = alertRootPath;
  }

  public Auth getAuth() {
	return auth;
  }

  public MetricsDefManager getMetricsDef() {
	return metricsDef;
  }

  public String getSqlTextForMetricsGroup(String groupName)
  {
	if(groupName == null)return null;
	String sql = null;
	
	MetricsGroup mg = getMetricsDef().getGroupByName(groupName);
	if(mg != null)
	{
		String sqlId = mg.getSql();
		if(sqlId != null)
		{
			Sql sqlObj = getSqlManager().getSql(sqlId);
			if(sqlObj != null)
			sql = sqlObj.getSqlText();
		}
	}
	return sql;
  }

  public AlertSettings getAlertSettings() {
	return alertSettings;
  }

  public MyPerfConfiguration getMyperfConfig() {
	return myperfConfig;
  }

  public SNMPSettings getSnmpSettings() {
	return snmpSettings;
  }

  public boolean initMetricsDB()
  {
	if(!this.getMyperfConfig().isConfigured())
		return false;
	if(this.metricDb != null)
		this.metricDb.destroy();
	//by default, use derby
	if("derby".equalsIgnoreCase(getMyperfConfig().getMetricsDbType()))
		this.metricDb = new DerbyMetricsDb();
	else //otherwise, use mysql
	{
		this.metricDb = new MySQLMetricsDb();
		this.metricDb.setUsername(getMyperfConfig().getMetricsDbUserName());
		this.metricDb.setPassword(getMyperfConfig().getMetricsDbPassword());
		this.metricDb.setSchemaName(getMyperfConfig().getMetricsDbName());
		this.metricDb.setConnectionString("jdbc:mysql://"
		    + getMyperfConfig().getMetricsDbHost()+":"
			+ getMyperfConfig().getMetricsDbPort()+"/"
		    + getMyperfConfig().getMetricsDbName()
			+ "?useSSL=true&enabledTLSProtocols=TLSv1.2&verifyServerCertificate=false");
	}
	this.metricDb.setFrameworkContext(this);
	this.metricDb.setMetricsGroups(this.getMetricsDef());
	this.metricDb.init();

	return true;
  }

  public Alerts getAlerts() 
  {
	return alerts;
  }

  public void addAlert(AlertEntry alert)
  {
	try
	{
	    if(!this.instanceStatesManager.getStates(
			    this.dbInfoManager.findDB(alert.getDbGroup(), alert.getDbHost()).getDbid())
				.canSendWebNotification(alert.getTs(), alert.getAlertReason(), this.myperfConfig.getWebAlertIntervalMinutes())
		 )
		 return;
	}catch(Exception ex){}
	alerts.addAlert(alert);    
  }
  /**
   * helper method to create an alert report
   * @param reportTimestamp
   * @param detecedTimestamp
   * @param dbGroupName
   * @param dbHost
   * @param alertReason
   * @param alertValue
   * @return
   */
  public AlertReport createAlertReport(long reportTimestamp, 
		  long detecedTimestamp,
		  String dbGroupName,
		  String dbHost,
		  String alertReason,
		  String alertValue
		  )
  {
	  AlertReport ar = new AlertReport();
	  ar.setReportTimestamp(reportTimestamp);
	  ar.setDbGroupName(dbGroupName);
	  ar.setDbHostName(dbHost);
	  ar.setTimestamp(detecedTimestamp);
	  ar.setRootPath(getAlertRootPath());
	  ar.setAlertReason(alertReason);
	  ar.setAlertValue(alertValue);	
	return ar;
  }
  /**
   * Helper method to create an alert report
   * @param reportTimestamp
   * @param alert
   * @return
   */
  public AlertReport createAlertReport(long reportTimestamp, AlertEntry alert)
  {
	  AlertReport ar = new AlertReport();
	  ar.setReportTimestamp(reportTimestamp);
	  ar.setDbGroupName(alert.getDbGroup());
	  ar.setDbHostName(alert.getDbHost());
	  ar.setTimestamp(alert.getTs());
	  ar.setRootPath(getAlertRootPath());
	  ar.setAlertReason(alert.getAlertReason());
	  ar.setAlertValue(alert.getAlertValue());	
	return ar;
  }

  /**
   * helper method to send out alert
   * suppress alerts if similar alert was sent earlier
   * @param alert
   */
  public void emailAlert(AlertEntry alert)
  {
	try
	{
      if(!this.instanceStatesManager.getStates(
		    this.dbInfoManager.findDB(alert.getDbGroup(), alert.getDbHost()).getDbid())
			.canSendEmailNotification(alert.getTs(), alert.getAlertReason(), this.myperfConfig.getEmailAlertIntervalMinutes())
	  )
	  return;
	}catch(Exception ex){}
	  
	//String receiver = getMyperfConfig().getAlertNotificationEmails();
	String receiver = this.alertSettings.getNotificationEmails(alert.getDbGroup(), alert.getDbHost());
	if(receiver!=null && !receiver.isEmpty())
	{
		String subject = this.getAlertEmailSubject(alert);
		String msg = this.getAlertMessage(alert);
		//send alert to hipchat room if enabled
		this.hipchat.sendMessage(msg);
		MailUtil.sendMail(receiver, subject, msg);
	}
		
  }
  /**
   * Helper method to construct alert subject
   * @param alert
   * @return
   */
  private String getAlertEmailSubject(AlertEntry alert)
  {
		return "MySQL Perf Analyzer Alert - "+alert.getAlertReason()+": "
				+ alert.getDbGroup()+" - " + alert.getDbHost();
  }

  /**
   * Helper method to format alert time
   * @param alert
   * @return
   */
  private String getAlertEmailTime(AlertEntry alert)
  {
		long mytime = alert.getTs();
		if(mytime<=0L)mytime = System.currentTimeMillis();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));	
		try
		{
			return sdf.format(new Date(mytime));
		}catch(Exception ex)
		{
			
		}
		return "";
  }
	
  /**
   * Helper method to construct alert email message
   * @param alert
   * @return
   */
  private String getAlertMessage(AlertEntry alert)
  {
		String newline = "\n";//mailx does not like \r
		StringBuilder sb = new StringBuilder();
		sb.append("Alert Reason: ").append(alert.getAlertReason()).append(" @ ").append(alert.getAlertValue()).append(newline);
		sb.append("Time        : ").append(getAlertEmailTime(alert)).append(newline);
		sb.append("DB Group    : ").append(alert.getDbGroup()).append(newline);
		sb.append("DB Host     : ").append(alert.getDbHost()).append(newline);
		return sb.toString();
  }

  public void saveManagedDBCredentialForScanner(String username, String dbGroup, String dbuser, String password)
  {
		AppUser defaultUser = getUserManager().getUser(MetaDB.DEFAULT_USER);
		AppUser metricsUser = getUserManager().getUser(getMyperfConfig().getMetricsScannerUser());
		if(!MetaDB.DEFAULT_USER.equals(username) && //save one db search
				DBUtils.findDBCredential(this, dbGroup, defaultUser) == null)
		{
			this.dbInfoManager.copyManagedDBCredential(defaultUser.getName(), 
					false, username, dbGroup, dbuser, password);
		}
		
		if(metricsUser != null && !MetaDB.DEFAULT_USER.equals(metricsUser.getName())
				&& !username.equals(metricsUser.getName()) //save one db search
				&& DBUtils.findDBCredential(this, dbGroup, metricsUser) == null)
		{
			this.dbInfoManager.copyManagedDBCredential(metricsUser.getName(), 
					false, username, dbGroup, dbuser, password);
		}

  }

  /**
   * Keep as reference. Remove later
   * @param username
   * @param dbGroup
   * @param dbuser
   * @param password
   */
  public void saveManagedDBCredentialForScanner2(String username, String dbGroup, String dbuser, String password)
  {
		AppUser defaultUser = getUserManager().getUser(MetaDB.DEFAULT_USER);
		AppUser metricsUser = getUserManager().getUser(getMyperfConfig().getMetricsScannerUser());
		if(!MetaDB.DEFAULT_USER.equals(username) && //save one db search
				DBUtils.findDBCredential(this, dbGroup, defaultUser) == null)
		{
			DBCredential cred2 = new DBCredential();
			cred2.setAppUser(defaultUser.getName());
			cred2.setDbGroupName(dbGroup);
			cred2.setUsername(dbuser);
			cred2.setPassword(password);
			getMetaDb().upsertDBCredential(cred2);
			this.dbInfoManager.getMyDatabases(cred2.getAppUser(), false).addDb(cred2.getDbGroupName());
		}
		
		if(metricsUser != null && !MetaDB.DEFAULT_USER.equals(metricsUser.getName())
				&& !username.equals(metricsUser.getName()) //save one db search
				&& DBUtils.findDBCredential(this, dbGroup, metricsUser) == null)
		{
			DBCredential cred2 = new DBCredential();
			cred2.setAppUser(metricsUser.getName());
			cred2.setDbGroupName(dbGroup);
			cred2.setUsername(dbuser);
			cred2.setPassword(password);
			getMetaDb().upsertDBCredential(cred2);
			this.dbInfoManager.getMyDatabases(cred2.getAppUser(), false).addDb(cred2.getDbGroupName());				
		}

  }
  
  public HipchatIntegration getHipchat()
  {
	  return this.hipchat;
  }
}
