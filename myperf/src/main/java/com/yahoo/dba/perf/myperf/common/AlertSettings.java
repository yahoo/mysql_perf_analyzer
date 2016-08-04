/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;


public class AlertSettings {
    
	public static final float CPU_THRESHOLD = 70.0f;
	public static final float IOWAITS_THRESHOLD = 30.0f;
	public static final float LOADAVG_THRESHOLD = 50.0f;
	public static final int THREAD_THRESHOLD = 100;
	public static final int REPLLAG_THRESHOLD = 1800;
	public static final int SLOWQUERY_THRESHOLD = 200;//200/minute
	public static final int ABORTED_CONNECTS_THRESHOLDS = 10;//600/minute
	public static final int DEADLOCKS_THRESHOLDS = 0;//since we use greater than, so any positive detection should be alerted
	public static final int DISK_USED_PCT_THRESHOLD = 90;
	public static final int DEADLOCK_THRESHOLD = 0;
	public static final int SWAPOUT_THRESHOLD = 0;
	
	public static final String[] COMMON_ALERTS = new String[]{"CPU", "IO", "LOADAVG", "THREAD", "REPLLAG", 
		"SLOW", "CONNECT_FAILURE", "DEADLOCKS","DISKUSAGE", "DEADLOCK", "SWAPOUT"};

	private static final HashMap<String, Float> DEFAULT_THRESHOLD = new HashMap<String, Float>();
	private static Logger logger = Logger.getLogger(AlertSettings.class.getName());
	
	static
	{
		DEFAULT_THRESHOLD.put("CPU", CPU_THRESHOLD);
		DEFAULT_THRESHOLD.put("IO", IOWAITS_THRESHOLD);
		DEFAULT_THRESHOLD.put("LOADAVG", LOADAVG_THRESHOLD);
		DEFAULT_THRESHOLD.put("THREAD", new Float(THREAD_THRESHOLD));
		DEFAULT_THRESHOLD.put("REPLLAG", new Float(REPLLAG_THRESHOLD));
		DEFAULT_THRESHOLD.put("SLOW", new Float(SLOWQUERY_THRESHOLD));
		DEFAULT_THRESHOLD.put("CONNECT_FAILURE", new Float(ABORTED_CONNECTS_THRESHOLDS));
		DEFAULT_THRESHOLD.put("DEADLOCKS", new Float(DEADLOCKS_THRESHOLDS));
		DEFAULT_THRESHOLD.put("DISKUSAGE", new Float(DISK_USED_PCT_THRESHOLD));
		DEFAULT_THRESHOLD.put("DEADLOCK", new Float(DEADLOCK_THRESHOLD));
		DEFAULT_THRESHOLD.put("SWAPOUT", new Float(SWAPOUT_THRESHOLD));
	}
	
	static class HostSettings
	{
		private HashMap<String, Float> thresholds = new HashMap<String, Float>();
		private String notificationEmails; //emails to alert if something happens
		
		public Float getThreshold(String alertType)
		{
			if(thresholds.containsKey(alertType))
				return thresholds.get(alertType);
			return null;
		}
		
		public void updateThreshold(String alertType, Float threshold)
		{
			this.thresholds.put(alertType, threshold);
		}

		public String getNotificationEmails() {
			return notificationEmails;
		}

		public void setNotificationEmails(String notificationEmails) {
			this.notificationEmails = notificationEmails;
		}
	}
	static class GroupSettings
	{
		private HashMap<String, Float> groupThresholds = new HashMap<String, Float>();
		private HashMap<String, HostSettings> hostSettings = new HashMap<String, HostSettings>();	
		private String notificationEmails; //emails to alert if something happens
		
		public HostSettings getHostSettings(String dbhost)
		{
			if(this.hostSettings.containsKey(dbhost))
				return this.hostSettings.get(dbhost);
			return null;
		}
		
		public Float getGroupDefaultThreshold(String alertType)
		{
			if(this.groupThresholds.containsKey(alertType))
				return this.groupThresholds.get(alertType);
			else
				return null;
		}
		public void updateDefault(String alertType, Float threshold)
		{
			this.groupThresholds.put(alertType, threshold);
		}
		public void updateHostThreshold(String dbhost, String alertType, Float threshold)
		{
			if(!this.hostSettings.containsKey(dbhost))
				this.hostSettings.put(dbhost, new HostSettings());
			this.hostSettings.get(dbhost).updateThreshold(alertType, threshold);
		}

		public void updateHostNotificationEmails(String dbhost, String emails)
		{
			if(!this.hostSettings.containsKey(dbhost))
				this.hostSettings.put(dbhost, new HostSettings());
			this.hostSettings.get(dbhost).setNotificationEmails(emails);
		}

		public String getNotificationEmails() {
			return notificationEmails;
		}

		public void setNotificationEmails(String notificationEmails) {
			this.notificationEmails = notificationEmails;
		}
	}
	
	private HashMap<String, GroupSettings> groupSettings = new HashMap<String, GroupSettings>();
	private MyPerfContext context;
	private List<AlertDefinition> alertDefinitions = new ArrayList<AlertDefinition>();
	
	public AlertSettings()
	{
		
	}
	
	synchronized public float getAlertThreshold(String dbgroup, String dbhost, String alertType)
	{
		alertType = alertType.toUpperCase();
		//do we have group?
		if(!this.groupSettings.containsKey(dbgroup))
		{
			if(DEFAULT_THRESHOLD.containsKey(alertType))return DEFAULT_THRESHOLD.get(alertType);
			else return Float.MAX_VALUE;
		}
		GroupSettings grpSetting = groupSettings.get(dbgroup);
		HostSettings hostSetting = grpSetting.getHostSettings(dbhost);
		Float threshold = null;
		if(hostSetting != null)
			threshold = hostSetting.getThreshold(alertType);
		if(threshold == null)
			threshold = grpSetting.getGroupDefaultThreshold(alertType);
		if(threshold == null && DEFAULT_THRESHOLD.containsKey(alertType))return DEFAULT_THRESHOLD.get(alertType);
		if(threshold == null)
			threshold = Float.MAX_VALUE;
		return threshold;
			
	}

	synchronized public String getNotificationEmails(String dbgroup, String dbhost)
	{
		//do we have group?
		if(!this.groupSettings.containsKey(dbgroup))
			return this.context.getMyperfConfig().getAlertNotificationEmails();
		
		GroupSettings grpSetting = groupSettings.get(dbgroup);
		HostSettings hostSetting = grpSetting.getHostSettings(dbhost);
		String emails = null;
		if(hostSetting != null)
			emails = hostSetting.getNotificationEmails();
		if(emails == null || emails.isEmpty())
			emails = grpSetting.getNotificationEmails();
		if(emails != null && !emails.isEmpty())
			return emails;
		return this.context.getMyperfConfig().getAlertNotificationEmails();
	}
	synchronized public String getNotificationEmails(DBInstanceInfo dbinfo)
    {
		  return getNotificationEmails(dbinfo.getDbGroupName(), dbinfo.getHostName());  
    }

	synchronized public Float getAlertThreshold(DBInstanceInfo dbinfo, String alertType)
    {
		  return getAlertThreshold(dbinfo.getDbGroupName(), dbinfo.getHostName(), alertType);  
    }
	
	synchronized public boolean updateAlertThreshold(String dbgroup, String dbhost, String alertType, Float threshold, boolean save)
	{
		String alertSettingString = "(" + dbgroup +", "+dbhost +", "+alertType+", " + threshold+", " + save + ")"; 
		float prevValue = getAlertThreshold(dbgroup, dbhost, alertType);
		if(prevValue!=0.0f && Math.abs((prevValue - threshold.floatValue())/prevValue)<0.01f)
		{
			logger.info("Change too small, ignore: " + alertSettingString);
			return false;//ignore if change is less than 1%
		}
		if(prevValue==0.0f && threshold.floatValue()==0.0f)return false;
		
		if(dbgroup==null || dbgroup.isEmpty())return false;
		if(this.context.getDbInfoManager().findGroup(dbgroup)==null)
		{
			logger.info("Unknow DB GROUP, ignore: " + alertSettingString);
			return false;//no such dbgroup
		}
		if(dbhost==null || dbhost.isEmpty())dbhost = "all";
		if(!"all".equals(dbhost) && this.context.getDbInfoManager().findDB(dbgroup, dbhost)==null)
		{
			logger.info("Unknown DB, ignore: " + alertSettingString);
			return false;//no such db
		}
		if(!this.groupSettings.containsKey(dbgroup))
			this.groupSettings.put(dbgroup, new GroupSettings());
		GroupSettings grpSetting  = this.groupSettings.get(dbgroup);
		if("all".equals(dbhost))
		{
			if(save)this.context.getMetricDb().upsertAlertSetting(dbgroup, dbhost, alertType, threshold, null);
			grpSetting.updateDefault(alertType, threshold);
		}else
		{
			if(save)this.context.getMetricDb().upsertAlertSetting(dbgroup, dbhost, alertType, threshold, null);
			grpSetting.updateHostThreshold(dbhost, alertType, threshold);
		}
		return true;
	}
	
	synchronized public boolean updateAlertNotification(String dbgroup, String dbhost, String emails, boolean save)
	{
		String alertSettingString = "(" + dbgroup +", "+dbhost +", "+emails + ", " + save + ")"; 
		String prevValue = getNotificationEmails(dbgroup, dbhost);
		if(prevValue != null && prevValue.equalsIgnoreCase(emails))
		{
			logger.info("No changes, ignore: " + emails);
			return false;//ignore if no changes
		}
		
		if(dbgroup==null || dbgroup.isEmpty())return false;
		if(this.context.getDbInfoManager().findGroup(dbgroup)==null)
		{
			logger.info("Unknow DB GROUP, ignore: " + emails);
			return false;//no such dbgroup
		}
		if(dbhost==null || dbhost.isEmpty())dbhost = "all";
		if(!"all".equals(dbhost) && this.context.getDbInfoManager().findDB(dbgroup, dbhost)==null)
		{
			logger.info("Unknown DB, ignore: " + emails);
			return false;//no such db
		}
		if(!this.groupSettings.containsKey(dbgroup))
			this.groupSettings.put(dbgroup, new GroupSettings());
		GroupSettings grpSetting  = this.groupSettings.get(dbgroup);
		if("all".equals(dbhost))
		{
			if(save)this.context.getMetricDb().upsertAlertNotification(dbgroup, dbhost, emails);
			grpSetting.setNotificationEmails(emails);
		}else
		{
			if(save)this.context.getMetricDb().upsertAlertNotification(dbgroup, dbhost, emails);
			grpSetting.updateHostNotificationEmails(dbhost, emails);
		}
		return true;
	}

	public MyPerfContext getContext() {
		return context;
	}

	public void setContext(MyPerfContext context) {
		this.context = context;
	}

	public List<AlertDefinition> getAlertDefinitions() {
		return alertDefinitions;
	}
	
	public boolean addAlertDefinition(AlertDefinition alertDef)
	{
	  synchronized(this.alertDefinitions)	  
	  {
		  String name  = alertDef.getName();
		  for(int i = this.alertDefinitions.size() - 1; i>=0; i--)
		  {
			if(this.alertDefinitions.get(i).getName().equals(name))
			{
				return false;
			}
		  }
		this.alertDefinitions.add(alertDef);
	  }		
	  return true;
	}
	public boolean removeAlertDefinition(String name)
	{
	  synchronized(this.alertDefinitions)
	  {
		for(int i = this.alertDefinitions.size() - 1; i>=0; i--)
		{
			if(this.alertDefinitions.get(i).getName().equals(name))
			{
				this.alertDefinitions.remove(i);
				return true;
			}
		}
	  }
		return false;
	}

}
