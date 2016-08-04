/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consolidate main configurations here
 * @author xrao
 *
 */
public class MyPerfConfiguration implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(MyPerfConfiguration.class.getName());
			
	//configuration default path
	private static final String STORAGE_DIR = "autoscan";
	private static final String ROOTPATH = "myperf_config";//data will be stored under $rootPath/autoscan		
	public static final String MYPERF_CONFIG_FILE_NAME="myperf_configuration.properties";
	//allow to specify a customized path using system property
	//The path of the configuration file. All necessary directories have to be created with right permission
	//to allow read and modifications.
	public static final String MYPERF_CONFIG_PATH_PROP_NAME = "myperf.configuratioin.path";
	
	//email for admin user, for now, only useful if our simple MailUtil works.
	private String adminEmail;
	//alert notification emails
	private String alertNotificationEmails;

	//the user name used by the scanner to obtain credentials of managed database servers
	//default to our built in user
	private String metricsScannerUser = "myperf"; 
	
	//metrics scan and retention
	
	//The number of seconds between two consecutive metrics scans
	private int scannerIntervalSeconds = 300; 
	private int alertScanIntervalSeconds = 300; 
	//metrics retention days
	private int recordRententionDays = 60;
	//number of threads used by scanner
	private int scannerThreadCount = 4;
	//avoid email or notification bombing
	//TODO hard code to one hour.
	//suppress similar alerts from the same db for at least such time
	private int emailAlertIntervalMinutes = 60;
	private int webAlertIntervalMinutes = 5;
	
	//If set to false, connection to the managed database will not be cached and reused
    private boolean reuseMonUserConnction = true;
    
    //maximum age for replication topology cache, hard code for now to 5 minutes
    private long replTopologyCacheMaxAge = 300000L;
    //how far we want to probe the topology
    private int replTopologyMaxDepth = 3;
    
	//Metrics storage DB
	//Either use embedded derby db or mysql
	private String metricsDbType = "derby";
	//If use mysql for metrics storage, mysql configuration
	
	private String metricsDbHost;//If use mysql
	private int metricsDbPort = 3306;//if use mysql
	private String metricsDbName = "METRICSDB";
	private String metricsDbUserName;
	private String metricsDbPassword;

	//hipchat integration
	private String hipchatUrl; //must be in the format like https://xxx.hipchat.com/v2/room/{roomnumber}/notification?
	private String hipchatAuthToken; //we will append authToken to construct the full url
	  
	//If the app is just installed, it might not have been configured
	private boolean configured = false;
	
	//internal use
	private String myperfConfigPath;

	public MyPerfConfiguration()
	{
		reuseMonUserConnction = true; //default to cache and reuse
	}

	/**
	 * Read configurations. It should be invoked after meta db and user data are loaded,
	 * but before scanner initialized
	 * @param ctx
	 */
	public void init(MyPerfContext ctx)
	{
		logger.info("Read configuration ...");
		configured = false;

		//calculate configuration path first
		//default is $ROOTPATH/$STORAGE_DIR/MYPERF_CONFIG_FILE_NAME
		//if cannot retrieve from system property
		
		File cfgFile = null;
		myperfConfigPath = System.getProperty(MYPERF_CONFIG_PATH_PROP_NAME);
		if (myperfConfigPath == null || myperfConfigPath.isEmpty())
		{
			File cfgDir = new File(new File(ROOTPATH), STORAGE_DIR);
			if(!cfgDir.exists()) //if directories do not exist, create first
				cfgDir.mkdirs();
			cfgFile = new File(cfgDir, MYPERF_CONFIG_FILE_NAME);
			myperfConfigPath = cfgFile.getAbsolutePath();
		}else
			cfgFile = new File(myperfConfigPath);
		
		if(!cfgFile.exists())//not setup yet
		{
		   logger.info("Cannot find configuration file "+cfgFile.getAbsolutePath()
				   +". The system might not havde been configured yet.");
		   return;
		}
		
		//now read configuration back
		logger.info("Read configuration from file " + cfgFile.getAbsolutePath());
		if(readConfig(cfgFile, ctx))
		{
		    configured = true;
		}
		logger.info("Done read configuration from file " + cfgFile.getAbsolutePath()
				+", configured="+configured);
	}
	
	
	synchronized private boolean readConfig(File cfgFile, MyPerfContext ctx)
	{
		FileInputStream in = null;
		try
		{
			in = new FileInputStream(cfgFile);
			java.util.Properties props = new java.util.Properties();
			props.load(in);
			this.metricsScannerUser = props.getProperty("metricsScannerUser");
			if(this.metricsScannerUser == null)return false;
			this.alertNotificationEmails = props.getProperty("alertNotificationEmails");
			this.adminEmail = props.getProperty("adminEmail");

			this.scannerIntervalSeconds = Integer.parseInt(props.getProperty("scannerIntervalSeconds","300"));//default to 5 minutes
			if(this.scannerIntervalSeconds < 1 )this.scannerIntervalSeconds = 300;//minimum 1 second, if not set, use default
			this.alertScanIntervalSeconds = Integer.parseInt(props.getProperty("alertScanIntervalSeconds", "300"));
			if(this.alertScanIntervalSeconds < 60)this.alertScanIntervalSeconds  = 300;//minimum 1 minute
			this.recordRententionDays = Integer.parseInt(props.getProperty("recordRententionDays","60"));
			this.scannerThreadCount =  Integer.parseInt(props.getProperty("scannerThreadCount","4"));
			this.reuseMonUserConnction = !"n".equalsIgnoreCase(props.getProperty("reuseMonUserConnction", "y"));
			
			this.metricsDbType = props.getProperty("metricsDbType", this.metricsDbType);
			if(!"derby".equalsIgnoreCase(this.metricsDbType))//if not derby db
			{
				this.metricsDbHost = props.getProperty("metricsDbHost");
				this.setMetricsDbPort(Integer.parseInt(props.getProperty("metricsDbPort", "3306")));
				this.metricsDbName = props.getProperty("metricsDbName", this.metricsDbName);
				this.setMetricsDbUserName(props.getProperty("metricsDbUserName"));
				this.setMetricsDbPassword(ctx.getMetaDb().dec(props.getProperty("metricsDbPassword")));
			}
			this.hipchatUrl = props.getProperty("hipchatUrl", null);
			String tmpAuthTocken = props.getProperty("hipchatAuthToken", null);
			if(tmpAuthTocken != null && !tmpAuthTocken.isEmpty())
				this.hipchatAuthToken = ctx.getMetaDb().dec(tmpAuthTocken);
			return true;
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Failed to read configuration file "+cfgFile.getAbsolutePath(), ex);
		}finally
		{
			if(in!=null){try{in.close();}catch(Exception fex){};}			
		}
		return false;
	}

	/**
	 * Make the configuration permanent 
	 * @param ctx
	 * @return
	 */
	synchronized public boolean store(MyPerfContext ctx)
	{
		File cfgFile = new File(this.myperfConfigPath);
		PrintWriter pw = null;
		try
		{
			pw = new PrintWriter(new FileWriter(cfgFile));
			pw.println("metricsScannerUser="+ this.metricsScannerUser);
			pw.println("adminEmail="+ this.adminEmail);
			pw.println("alertNotificationEmails="+ this.alertNotificationEmails);
			
			pw.println("scannerIntervalSeconds="+ this.scannerIntervalSeconds);
			pw.println("alertScanIntervalSeconds="+this.alertScanIntervalSeconds);
			pw.println("recordRententionDays="+ this.recordRententionDays);
			pw.println("scannerThreadCount="+ this.scannerThreadCount);
			pw.println("reuseMonUserConnction="+(reuseMonUserConnction?"y":"n"));
			pw.println("metricsDbType="+ this.metricsDbType);
			if(this.metricsDbHost!=null && !this.metricsDbHost.isEmpty())
				pw.println("metricsDbHost="+ this.metricsDbHost);
			if(this.metricsDbPort>0)
				pw.println("metricsDbPort="+ this.metricsDbPort);
			if(this.metricsDbName!=null && !this.metricsDbName.isEmpty())
				pw.println("metricsDbName="+ this.metricsDbName);
			if(this.metricsDbUserName!=null && !this.metricsDbUserName.isEmpty())
				pw.println("metricsDbUserName="+ this.metricsDbUserName);
			if(this.metricsDbPassword!=null && !this.metricsDbPassword.isEmpty())
				pw.println("metricsDbPassword="+ ctx.getMetaDb().enc(this.metricsDbPassword));
			if(this.hipchatUrl!= null && !this.hipchatUrl.isEmpty())
				pw.println("hipchatUrl="+this.hipchatUrl);
			if(this.hipchatAuthToken!= null && !this.hipchatAuthToken.isEmpty())
				pw.println("hipchatAuthToken="+ctx.getMetaDb().enc(this.hipchatAuthToken));
			
			return true;
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Failed to store configurations to file " + this.myperfConfigPath, ex);
		}finally
		{
			if(pw!=null){try{pw.flush();pw.close();}catch(Exception fex){}}
		}
		return false;
	}

	public String getMetricsScannerUser() {
		return metricsScannerUser;
	}

	public void setMetricsScannerUser(String metricsScannerUser) {
		this.metricsScannerUser = metricsScannerUser;
	}

	public String getAdminEmail() {
		return adminEmail;
	}

	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}


	public String getAlertNotificationEmails() {
		return alertNotificationEmails;
	}

	public void setAlertNotificationEmails(String alertNotificationEmails) {
		this.alertNotificationEmails = alertNotificationEmails;
	}

	public int getScannerIntervalSeconds() {
		return scannerIntervalSeconds;
	}

	public void setScannerIntervalSeconds(int scannerIntervalSeconds) {
		this.scannerIntervalSeconds = scannerIntervalSeconds;
	}

	public int getRecordRententionDays() {
		return recordRententionDays;
	}

	public void setRecordRententionDays(int recordRententionDays) {
		this.recordRententionDays = recordRententionDays;
	}

	public int getScannerThreadCount() {
		return scannerThreadCount;
	}

	public void setScannerThreadCount(int scannerThreadCount) {
		this.scannerThreadCount = scannerThreadCount;
	}

	public String getMetricsDbType() {
		return metricsDbType;
	}

	public void setMetricsDbType(String metricsDbType) {
		this.metricsDbType = metricsDbType;
	}

	public String getMetricsDbHost() {
		return metricsDbHost;
	}

	public void setMetricsDbHost(String metricsDbHost) {
		this.metricsDbHost = metricsDbHost;
	}

	public int getMetricsDbPort() {
		return metricsDbPort;
	}

	public void setMetricsDbPort(int metricsDbPort) {
		this.metricsDbPort = metricsDbPort;
	}

	public String getMetricsDbName() {
		return metricsDbName;
	}

	public void setMetricsDbName(String metricsDbName) {
		this.metricsDbName = metricsDbName;
	}

	public String getMetricsDbUserName() {
		return metricsDbUserName;
	}

	public void setMetricsDbUserName(String metricsDbUserName) {
		this.metricsDbUserName = metricsDbUserName;
	}

	public String getMetricsDbPassword() {
		return metricsDbPassword;
	}

	public void setMetricsDbPassword(String metricsDbPassword) {
		this.metricsDbPassword = metricsDbPassword;
	}

	public boolean isConfigured() {
		return configured;
	}

	public void setConfigured(boolean configured) {
		this.configured = configured;
	}

	public int getAlertScanIntervalSeconds() {
		return alertScanIntervalSeconds;
	}

	public void setAlertScanIntervalSeconds(int alertScanIntervalSeconds) {
		this.alertScanIntervalSeconds = alertScanIntervalSeconds;
	}

	public int getEmailAlertIntervalMinutes() {
		return emailAlertIntervalMinutes;
	}

	public void setEmailAlertIntervalMinutes(int emailAlertIntervalMinutes) {
		this.emailAlertIntervalMinutes = emailAlertIntervalMinutes;
	}

	public int getWebAlertIntervalMinutes() {
		return webAlertIntervalMinutes;
	}

	public void setWebAlertIntervalMinutes(int webAlertIntervalMinutes) {
		this.webAlertIntervalMinutes = webAlertIntervalMinutes;
	}

	public boolean isReuseMonUserConnction() {
		return reuseMonUserConnction;
	}

	public void setReuseMonUserConnction(boolean reuseMonUserConnction) {
		this.reuseMonUserConnction = reuseMonUserConnction;
	}

	public long getReplTopologyCacheMaxAge() {
		return replTopologyCacheMaxAge;
	}

	public void setReplTopologyCacheMaxAge(long replTopologyCacheMaxAge) {
		this.replTopologyCacheMaxAge = replTopologyCacheMaxAge;
	}

	public int getReplTopologyMaxDepth() {
		return replTopologyMaxDepth;
	}

	public void setReplTopologyMaxDepth(int replTopologyMaxDepth) {
		this.replTopologyMaxDepth = replTopologyMaxDepth;
	}

	public String getHipchatUrl() {
		return hipchatUrl;
	}

	public void setHipchatUrl(String hipchatUrl) {
		this.hipchatUrl = hipchatUrl;
	}

	public String getHipchatAuthToken() {
		return hipchatAuthToken;
	}

	public void setHipchatAuthToken(String hipchatAuthToken) {
		this.hipchatAuthToken = hipchatAuthToken;
	}

}
