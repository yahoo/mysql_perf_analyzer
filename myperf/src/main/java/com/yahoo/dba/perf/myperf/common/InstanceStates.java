/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * This will have two 
 * Modify the way repl down is alerted. Now only alert repl status changes.
 * @author xrao
 *
 */
public class InstanceStates implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(InstanceStates.class.getName());
	private java.util.Date lastAccessTime;
	private StateSnapshot prevSnapshot = new StateSnapshot();
	private StateSnapshot currSnapshot = new StateSnapshot();

	public static long REPETA_ALERT_DELAY = 3600000L;//for certain alert, we don't want repeated alerts, so skip for one hour
	
	private String lastAlertType;
	private Date lastAlertTime;
	private String lastAlertValue;
	private Date lastAlertEndTime = null;
	private long lastUpdateTime = -1;
	private long lastReportTime = -1;
	private long lastScanTime = 0L;
	

	//to avoid email bombing
	private long lastEmailAlertTime = -1L;
	private String lastEmailAlertReason  = null;
	private long lastWebAlertTime = -1L;
	private String lastWebAlertReason  = null;
	
	//work in progress reports. We don't want to run limited report runners
	//in case some SQL query hangs because of network or mysql server bugs.
	private int reportsWIP = 0;
	public final static int REPORTS_WIP_THRESHOLDS = 2; //no more than 2
	
	public InstanceStates()
	{
		
	}

	synchronized public java.util.Date getLastAccessTime() {
		return lastAccessTime;
	}

	synchronized public void setLastAccessTime(java.util.Date dt) {
		 lastAccessTime = dt;
	}
	
	synchronized public StateSnapshot[] copySnapshots() 
	{
		StateSnapshot[] res = new StateSnapshot[2];
		res[0] = this.prevSnapshot.copy();
		res[1] = this.currSnapshot.copy();
		return res;
	}

	
	synchronized public void update(StateSnapshot stat, Map<String, Float> thresholds)
	{
		//switch and update
		this.prevSnapshot = this.currSnapshot;
		this.currSnapshot = stat.copy();
		this.lastUpdateTime = stat.getTimestamp();
		//use hard coded threshold for now
        boolean isSnapValid = this.prevSnapshot.getTimestamp()>=0L && this.currSnapshot.getTimestamp()>this.prevSnapshot.getTimestamp();
        long interval = this.currSnapshot.getTimestamp() - this.prevSnapshot.getTimestamp();
    	Date dt = new java.util.Date(stat.getTimestamp());
    	String alertType = null;
    	String alertValue = null;
        if(isSnapValid) //need two snapshots
        {
      	  //sys CPU
      	  double syscpu = 0.0f;
      	  double usercpu = 0.0f;
      	  double iowaits = 0.0f;
      	  double slowCount = 0.0f;
      	  double aborted_cc = 0.0f;//aborted client and connects
      	  long deadlocks = this.currSnapshot.getDeadlocks() - this.prevSnapshot.getDeadlocks();
      	  //note if swapout =  -1, we don't have any record yet
      	  long swapout = this.prevSnapshot.getSwapout()>=0L?
      			  this.currSnapshot.getSwapout() - this.prevSnapshot.getSwapout():0L;
      	  if(this.prevSnapshot.getSyscputime()>=0L && this.currSnapshot.getSyscputime()>=0L)
      	  {
      		  double val = ((double)(this.currSnapshot.getSyscputime() - this.prevSnapshot.getSyscputime())*100)/(double)(this.currSnapshot.getTotalcputime() - this.prevSnapshot.getTotalcputime());
      		  syscpu = val;
      	  }
      	  if(this.prevSnapshot.getUsercputime()>=0L && this.currSnapshot.getUsercputime()>=0L)
      	  {
      		  double val = ((double)(this.currSnapshot.getUsercputime() - this.prevSnapshot.getUsercputime())*100)/(double)(this.currSnapshot.getTotalcputime() - this.prevSnapshot.getTotalcputime());
      		  usercpu = val;
      	  }
      	  if(this.prevSnapshot.getIotime()>=0L && this.currSnapshot.getIotime()>=0L)
      	  {
      		  double val = ((double)(this.currSnapshot.getIotime() - this.prevSnapshot.getIotime())*100)/(double)(this.currSnapshot.getTotalcputime() - this.prevSnapshot.getTotalcputime());
      		  iowaits = val;
      	  }
      	  if(this.prevSnapshot.getSlowQueryCount()>=0L && this.currSnapshot.getSlowQueryCount()>=0L)
      	  {
      		  double val = ((double)(this.currSnapshot.getSlowQueryCount() - this.prevSnapshot.getSlowQueryCount())*60000)/(double)(this.currSnapshot.getTimestamp() - this.prevSnapshot.getTimestamp());
      		  slowCount = val;      		  
      	  }
      	  if(this.prevSnapshot.getAbortedConnectsClients()>=0L && this.currSnapshot.getAbortedConnectsClients()>=0L)
      	  {
      		  double val = ((double)(this.currSnapshot.getAbortedConnectsClients() - this.prevSnapshot.getAbortedConnectsClients())*1000)/(double)(this.currSnapshot.getTimestamp() - this.prevSnapshot.getTimestamp());
      		  aborted_cc = val;      		  
      	  }
      	  if(deadlocks > thresholds.get("DEADLOCK"))//will generate alert if any deadlocks detected
      	  {
      		alertType = "DEADLOCKS";
      		alertValue = String.valueOf(deadlocks);
      	  }
      	  else if(syscpu+usercpu>thresholds.get("CPU"))
    	  {
    		  alertType = "CPU";      		        		  
    		  alertValue = String.format("%.3f",usercpu)+","+String.format("%.3f",syscpu);
    	  }else if(iowaits>thresholds.get("IO"))
      	  {
      		  alertType = "IO";
      		  alertValue = String.format("%.3f",iowaits);
      	  }else if(swapout > thresholds.get("SWAPOUT"))
      	  {
      		  alertType = "SWAPOUT";
      		  alertValue = String.valueOf(swapout);
      		  logger.info("DEBUG_SWAPOUT, "+ (new java.util.Date()) + " value: " + swapout +", prev: " + this.prevSnapshot.getSwapout()+", new: " + this.currSnapshot.getSwapout());
      	  }
    	  else if(slowCount>=thresholds.get("SLOW"))
      	  {
      		  alertType = "SLOW";
      		  alertValue = String.format("%.3f",slowCount);      		  
      	  }else if(aborted_cc >= thresholds.get("CONNECT_FAILURE"))
      	  {
      		  alertType = "CONNECT_FAILURE";
      		  alertValue = String.format("%.3f",aborted_cc);      		  
      		  
      	  }else if(this.prevSnapshot.getReplIo() > this.currSnapshot.getReplIo() 
      			  && this.prevSnapshot.getReplSql() > this.currSnapshot.getReplSql())
      	  {
      		  alertType = "REPLDOWN";
      		  alertValue = "Slave IO and SQL threads down";      		  
      	  }else if(this.prevSnapshot.getReplIo() > this.currSnapshot.getReplIo())
      	  {
      		  alertType = "REPLDOWN";
      		  alertValue = "Slave IO threads down";      		  
      	  }else if(this.prevSnapshot.getReplSql() > this.currSnapshot.getReplSql())
      	  {
      		  alertType = "REPLDOWN";
      		  alertValue = "Slave SQL threads down";      		  
      	  }else if(this.prevSnapshot.getMax_conn_error() >= 0
      			  && this.currSnapshot.getMax_conn_error() > this.prevSnapshot.getMax_conn_error())
      	  {
      		  alertType = "MAXCONNERR";
      		  alertValue = String.valueOf(this.currSnapshot.getMax_conn_error() - this.prevSnapshot.getMax_conn_error());
      	  }
        }
        if(alertType==null && this.currSnapshot.getTimestamp()>0)//one snap is enough
        {
      	  if(this.currSnapshot.getLoadAverage()>thresholds.get("LOADAVG"))
      	  {
      		  alertType = "LOADAVG";      		        		  
      		  alertValue = String.format("%.3f",this.currSnapshot.getLoadAverage());
      	  }else if(this.currSnapshot.getActiveThreads()>thresholds.get("THREAD"))
      	  {
      		  alertType = "THREAD";      		        		  
      		  alertValue = String.valueOf(this.currSnapshot.getActiveThreads());
      	  }else if(this.currSnapshot.getReplLag()>thresholds.get("REPLLAG") 
      	      && this.currSnapshot.getReplLag() - this.prevSnapshot.getReplLag()<3600)
      	  {	  //note since we check either 1 minutes or 5 minutes, it does not make sense
      		  //if repl lag suddenly jumps for more than 1 hr except parallel slave worker bugs
      		  alertType = "REPLLAG";      		  
      		  alertValue = String.valueOf(this.currSnapshot.getReplLag());
      	  }
        }        
        updateAlert(alertType==null, dt, alertType, alertValue, false);
        //updateAlert(alertType==null && (!"REPLDOWN".equals(this.lastAlertType)), dt, alertType, alertValue, false);
	}

	/**
	 * Check if the data gathered could raise any user defined alerts, this should be invoked after
	 * update(StateSnapshot stat, Map<String, Float> thresholds). We need context object to 
	 * get alert definition and subscription overwrites
	 * @param context
	 * @param dbgroup
	 * @param host
	 * @return
	 */
	synchronized public List<AlertEntry> checkAndRaiseUserAlerts(MyPerfContext context, String dbgroup, String host)
	{
		//no valid snap yet
		if(this.currSnapshot == null 
				|| this.currSnapshot.getTimestamp() < 0L)
			return null;
		//no data gathered
		if(this.currSnapshot.getMetricsMap() == null 
				|| this.currSnapshot.getMetricsMap().size() == 0)
			return null;
		List<AlertEntry> alerts = new ArrayList<AlertEntry>();
		//loop entry by entry
		for(Map.Entry<String, Float> entry: this.currSnapshot.getMetricsMap().entrySet())
		{
			String alertName = entry.getKey();
			Float metricValue = entry.getValue();
			AlertDefinition def = context.getMetricsDef().getUdmManager().getAlertByName(alertName);
			if(def == null)continue;
			AlertSubscribers.Subscription subscript = context.getMetricsDef().getUdmManager()
					.getAlertSubscriptions().getSubscription(dbgroup, host, alertName);
			if(subscript == null)continue;
			//since this is metrics based alerts, we only care about threshold
			Float threshold = subscript.threshold;
			if(threshold == null)threshold = def.getDefaultThreshold();
			if(threshold == null)continue; //something wrong with definition
			if(AlertDefinition.METRICS_VALUE_TYPE_VALUE.equals(def.getMetricValueType()))
			{
				//only need current value to compare
				if(AlertDefinition.METRICS_COMPARISION_GREATER_THAN.equalsIgnoreCase(def.getMetricComparison()))
				{
					if(metricValue.floatValue() > threshold.floatValue())
					{
						alerts.add(new AlertEntry(this.currSnapshot.getTimestamp(), alertName, metricValue.toString(), dbgroup, host));
					}
				}else if(AlertDefinition.METRICS_COMPARISION_LESS_THAN.equalsIgnoreCase(def.getMetricComparison()))
				{
					if(metricValue.floatValue() < threshold.floatValue())
					{
						alerts.add(new AlertEntry(this.currSnapshot.getTimestamp(), alertName, metricValue.toString(), dbgroup, host));
					}					
				}
				
			}else
			{
				//need average change per second to compare
				//no previous one to compare
				if(this.prevSnapshot == null || this.prevSnapshot.getTimestamp() <0L)continue;
				if(this.prevSnapshot.getMetricsMap() == null 
						|| this.prevSnapshot.getMetricsMap().size()==0
						|| !this.prevSnapshot.getMetricsMap().containsKey(alertName))continue;
				long interval = this.currSnapshot.getTimestamp() - this.prevSnapshot.getTimestamp();
				if(interval <=0L)continue; //something wrong
				float avg = metricValue.floatValue() - this.prevSnapshot.getMetricsMap().get(alertName).floatValue();
				if(AlertDefinition.METRICS_VALUE_TYPE_DIFF_AVG.equals(def.getMetricValueType()))
					avg = (float)(((metricValue.floatValue() - this.prevSnapshot.getMetricsMap().get(alertName).floatValue())*1000.0)/(float)interval);
				if(AlertDefinition.METRICS_COMPARISION_GREATER_THAN.equalsIgnoreCase(def.getMetricComparison()))
				{
					if(avg > threshold.floatValue())
					{
						alerts.add(new AlertEntry(this.currSnapshot.getTimestamp(), alertName, String.valueOf(avg), dbgroup, host));
					}
				}else if(AlertDefinition.METRICS_COMPARISION_LESS_THAN.equalsIgnoreCase(def.getMetricComparison()))
				{
					if(avg< threshold.floatValue())
					{
						alerts.add(new AlertEntry(this.currSnapshot.getTimestamp(), alertName, String.valueOf(avg), dbgroup, host));
					}					
				}
			}
			
		}
		return alerts;
	}
	
	/**
	 * Report that we cannot connect to the DB
	 * @param offline true: cannot, false: can
	 * @param dt
	 * @return true if status change is logged
	 */
	public boolean reportOffline(boolean offline, Date dt)
	{
		if("OFFLINE".equals(this.lastAlertType) && !offline)
		{
			updateAlert(true, dt, "OFFLINE", "OFFLINE", false);
			return true;
		}
		//else if("OFFLINE".equals(this.lastAlertType) && offline && (this.lastAlertEndTime!=null ||  (dt.getTime() - this.lastAlertTime.getTime() >= 14400000L)))
		//{
		//	updateAlert(false, dt, "OFFLINE", "OFFLINE", true);
		//	return true;
		//}
		else if(!"OFFLINE".equals(this.lastAlertType) && offline)
		{
			updateAlert(false, dt, "OFFLINE", "OFFLINE", false);			
			return true;
		}
		return false;
	}
	
	public boolean reportUserAlert(Date dt, String alertName,  String value)
	{
		if(!alertName.equals(this.lastAlertType))
		{
			updateAlert(false, dt,alertName, value, false);			
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param clear If true alert ends
	 * @param dt
	 * @param alertType
	 * @param alertValue
	 * @param force: if true, update lastAlertTime anyway with new values
	 */
	public void updateAlert(boolean clear, Date dt, String alertType, String alertValue, boolean force)
	{
		if(clear)
		{
			if(this.lastAlertEndTime==null && this.lastAlertTime!=null)
				this.lastAlertEndTime = dt;
		}else //we have an alert
		{
			if(this.lastAlertEndTime!=null)//must be a new alert
			{
				this.lastAlertEndTime = null;//reset end time
				this.lastAlertTime = dt;
			}else if(this.lastAlertTime == null)//never had one
			{
				this.lastAlertTime = dt;
			}else if(this.lastAlertTime != null && !alertType.equalsIgnoreCase(this.lastAlertType))//a new type of alert
			{
				this.lastAlertTime = dt;
			}else if(force)
			{
				this.lastAlertTime = dt;
			}
			// else //continuous, update new value
			
			this.lastAlertType = alertType;
			this.lastAlertValue = alertValue;				
			
		}
	}
	public String getLastAlertType() {
		return lastAlertType;
	}

	public void setLastAlertType(String lastAlertType) {
		this.lastAlertType = lastAlertType;
	}

	public Date getLastAlertTime() {
		return lastAlertTime;
	}

	public void setLastAlertTime(Date lastAlertTime) {
		this.lastAlertTime = lastAlertTime;
	}

	public Date getLastAlertEndTime() {
		return lastAlertEndTime;
	}

	public void setLastAlertEndTime(Date lastAlertEndTime) {
		this.lastAlertEndTime = lastAlertEndTime;
	}

	public String getLastAlertValue() {
		return lastAlertValue;
	}

	public void setLastAlertValue(String lastAlertValue) {
		this.lastAlertValue = lastAlertValue;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public long getLastReportTime() {
		return lastReportTime;
	}

	public void setLastReportTime(long lastReportTime) {
		this.lastReportTime = lastReportTime;
	}

	public long getLastScanTime() {
		return lastScanTime;
	}

	public void setLastScanTime(long lastScanTime) {
		this.lastScanTime = lastScanTime;
	}

	public long getLastEmailAlertTime() {
		return lastEmailAlertTime;
	}

	public void setLastEmailAlertTime(long lastEmailAlertTime) {
		this.lastEmailAlertTime = lastEmailAlertTime;
	}

	public String getLastEmailAlertReason() {
		return lastEmailAlertReason;
	}

	public void setLastEmailAlertReason(String lastEmailAlertReason) {
		this.lastEmailAlertReason = lastEmailAlertReason;
	}
	
	/**
	 * Test if we should send out email notification
	 * @param ts
	 * @param reason
	 * @param emailAlertIntervalMinutes
	 * @return
	 */
	public boolean canSendEmailNotification(long ts, String reason, int emailAlertIntervalMinutes)
	{
		boolean toSend = false;
		if(this.lastEmailAlertTime == -1L)
			toSend = true; //first alert
		else if (!reason.equalsIgnoreCase(this.lastEmailAlertReason))
			toSend = true;//different type of alert
		else if(ts >= this.lastEmailAlertTime + emailAlertIntervalMinutes*60000)
			toSend = true;
		if(toSend)
		{
			this.lastEmailAlertTime = ts;
			this.lastEmailAlertReason = reason;
		}
		return toSend;
	}
	
	/**
	 * Reset if alert is resolved
	 */
	public void resetNotification()
	{
		this.lastEmailAlertReason = null;
		this.lastEmailAlertTime = -1L;
		this.lastWebAlertTime = -1L;
		this.lastWebAlertReason = null;
	}

	public long getLastWebAlertTime() {
		return lastWebAlertTime;
	}

	public void setLastWebAlertTime(long lastWebAlertTime) {
		this.lastWebAlertTime = lastWebAlertTime;
	}

	public String getLastWebAlertReason() {
		return lastWebAlertReason;
	}

	public void setLastWebAlertReason(String lastWebAlertReason) {
		this.lastWebAlertReason = lastWebAlertReason;
	}
	public boolean canSendWebNotification(long ts, String reason, int webAlertIntervalMinutes)
	{
		boolean toSend = false;
		if(this.lastWebAlertTime == -1L)
			toSend = true; //first alert
		else if (!reason.equalsIgnoreCase(this.lastWebAlertReason))
			toSend = true;//different type of alert
		else if(ts >= this.lastWebAlertTime + webAlertIntervalMinutes*60000)
			toSend = true;
		if(toSend)
		{
			this.lastWebAlertTime = ts;
			this.lastWebAlertReason = reason;
		}
		return toSend;
	}
	
	//Check if we can run a new report
	//Return true if we can
	//Otherwise slots full
	synchronized public boolean canRunReport() {
		boolean canRun = false;
		if(this.reportsWIP < REPORTS_WIP_THRESHOLDS)
		{
			this.reportsWIP++;
			canRun = true;
		}
		return canRun;
	}
	
	/**
	 * Free report slot
	 */
	synchronized public void reportDone() {
		if(this.reportsWIP>0)this.reportsWIP--;		
	}
}
