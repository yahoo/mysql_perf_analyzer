/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.process;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.*;
import com.yahoo.dba.perf.myperf.metrics.MetricsDbBase;

/**
 * This will automate AWRScanner and LongRunSqlScanner. The result can be accessed from 
 * FrameworkContext
 * @author xrao
 *
 */
public class AutoScanner {
	private static Logger logger = Logger.getLogger(AutoScanner.class.getName());
//	public static final char lineSeparator = '\n';
    private static final String SCHEDULER_METRICS = "METRICS";
    private static final String SCHEDULER_ALERT = "ALERT";
    private static final String SCHEDULER_RETENTION = "RETENTION";
    private static final String SCHEDULER_MONITOR = "MONITOR";
    
	private MyPerfContext context;
	private AppUser appUser;//The scanner will run as this user and use this user's credentials
	                        //If the user changed, the scanner needs to be restarted.
		
	private boolean initialized = false;//if the scanner is initialized
	private String initializationMessage = "OK";//initialization message
	private boolean running = false;//if the scanner is running

	private MetricsDbBase metricDb; //let AutoScanner to handle its initialization
	
	//scheduler	
	private ScheduledExecutorService metricsScheduler;
	private ScheduledExecutorService alertScheduler;
	private ScheduledExecutorService scheduler2;
	private ScheduledExecutorService monitorScheduler; //monitor if scanner hangs

	private MetricsScanTask metricsTask = null;
	private AlertScanTask alertTask = null;
	private MetricsRetentionTask retentionTask = null;
	private GlobalVariableChangeScanTask configScanTask = null;
	private MonitorTask monitorTask = null;

	//store data. the first key is dbgroupname;dbhost, the second key is metrics group name
	private HashMap<String, Map<String,MetricsBuffer>> buffer = new HashMap<String, Map<String,MetricsBuffer>> ();

	private boolean firstScan = true;//we just started
	private long lastScanTime = System.currentTimeMillis();
	private long lastAlertScanTime = System.currentTimeMillis();
	private long maxScanIdleTime = 3600000L;
	
	public AutoScanner(MyPerfContext context)
	{
		this.context = context;	
	}
	
	synchronized public void init()
	{		
		if(!this.initialized)
		{			
			//read and verify configuration. Especially the user
			if(!this.context.getMyperfConfig().isConfigured())
			{
				this.initializationMessage = "Cannot read configuration file.";
				logger.warning(this.initializationMessage+" The auto scanner has yet to configure.");
				return;
			}
		}
		
		String scanuser = context.getMyperfConfig().getMetricsScannerUser().toLowerCase();
		this.appUser = context.getUserManager().getUser(scanuser);
		if(this.appUser == null)
		{
			this.initializationMessage = "Invalid user specified for scanner: " + scanuser;
			logger.warning(this.initializationMessage);
			return;
		}
		this.metricDb = context.getMetricDb();
		
		this.initialized = true;
		this.initializationMessage = "OK";
		
	}
	
	synchronized public boolean isInitialized()
	{
		return this.initialized;
	}
	
	synchronized public String getInitializationMessage()
	{
		return this.initializationMessage;
	}
	
	synchronized public boolean isRunning()
	{
		return this.running;
	}
	
	synchronized public void start()
	{
		if(!this.initialized)return;
		if(!running)
		{
			this.running = true;
			startSchedulers();
		}
	}
	
	private void startMetricScheduler()
	{
		metricsScheduler = Executors.newSingleThreadScheduledExecutor();//we only need a single thread
		
		metricsTask = new MetricsScanTask();
		
		int secOfTheDay = getCurrentSeconds();
		int interval = context.getMyperfConfig().getScannerIntervalSeconds();
		maxScanIdleTime = interval * 3000;
		if(maxScanIdleTime < 300000L) maxScanIdleTime = 300000L;//minimum check time 5 minutes
		logger.info("maximun alowed hange time: "+maxScanIdleTime);
		
		int metricsDelay = (int)Math.ceil(((double)secOfTheDay)/(interval))*interval - secOfTheDay;
		//configDelay2 = 10;
		ScheduledFuture<?> metricsTaskFuture = metricsScheduler.scheduleAtFixedRate(metricsTask, 
				metricsDelay, interval, TimeUnit.SECONDS);
		logger.info("AutoScanner started: runtime scan for each " + interval
				+" seconds with delay of  "+metricsDelay +" seconds, run as user "
				+ context.getMyperfConfig().getMetricsScannerUser());		
	}
	
	private void startAlertScheduler()
	{
		alertScheduler = Executors.newSingleThreadScheduledExecutor();//we only need a single thread
		
		alertTask = new AlertScanTask();
		
		int secOfTheDay = getCurrentSeconds();
		int interval = context.getMyperfConfig().getScannerIntervalSeconds();
		maxScanIdleTime = interval * 3000;
		if(maxScanIdleTime < 300000L) maxScanIdleTime = 300000L;//minimum check time 5 minutes
		logger.info("maximun alowed hange time: "+maxScanIdleTime);
		
		int alertDelay = (int)Math.ceil(((double)secOfTheDay)/(context.getMyperfConfig().getAlertScanIntervalSeconds()))*context.getMyperfConfig().getAlertScanIntervalSeconds() - secOfTheDay;
		ScheduledFuture<?> alertTaskFuture = alertScheduler.scheduleAtFixedRate(alertTask, 
				alertDelay, context.getMyperfConfig().getAlertScanIntervalSeconds(), TimeUnit.SECONDS);
		logger.info("AlertScanner started: runtime scan for each " + context.getMyperfConfig().getAlertScanIntervalSeconds()
				+" seconds with delay of  " + alertDelay +" seconds, run as user "
				+ context.getMyperfConfig().getMetricsScannerUser());		
	}

	private void startConfigRetentionScheduler()
	{
		scheduler2 = Executors.newSingleThreadScheduledExecutor();//we only need a single thread
		
		retentionTask = new MetricsRetentionTask(this.context,  context.getMyperfConfig().getRecordRententionDays(), null);
		configScanTask = new GlobalVariableChangeScanTask(this.context, this.appUser);
		
		int secOfTheDay = getCurrentSeconds();
		int interval = context.getMyperfConfig().getScannerIntervalSeconds();
		maxScanIdleTime = interval * 3000;
		if(maxScanIdleTime < 300000L) maxScanIdleTime = 300000L;//minimum check time 5 minutes
		logger.info("maximun alowed hange time: "+maxScanIdleTime);
		
		int configDelay = (int)Math.ceil(((double)secOfTheDay)/(720*60))*720*60 - secOfTheDay;
		int configDelay2 = (int)Math.ceil(((double)secOfTheDay)/(1440*60))*1440*60 - secOfTheDay;
		int monitorDelay = (int)Math.ceil(((double)secOfTheDay)/(600))*600 - secOfTheDay; //monitor delay
		ScheduledFuture<?> runtimeTaskFuture2 = scheduler2.scheduleAtFixedRate(retentionTask, 
				configDelay2+60, 24*3600, TimeUnit.SECONDS);//once a day
		ScheduledFuture<?> runtimeTaskFuture3 = scheduler2.scheduleAtFixedRate(configScanTask, 
				configDelay+120, 12*3600, TimeUnit.SECONDS);//twice a day
		logger.info("Rentention Task and configuratiion scan task scheduled.");
		
		
	}

	private void startMonitorScheduler()
	{
		this.monitorScheduler = Executors.newScheduledThreadPool(2);
		
		monitorTask = new MonitorTask();
		
		int secOfTheDay = getCurrentSeconds();
		int interval = context.getMyperfConfig().getScannerIntervalSeconds();
		maxScanIdleTime = interval * 3000;
		if(maxScanIdleTime < 300000L) maxScanIdleTime = 300000L;//minimum check time 5 minutes
		logger.info("maximun alowed hange time: "+maxScanIdleTime);
		
		int monitorDelay = (int)Math.ceil(((double)secOfTheDay)/(600))*600 - secOfTheDay; //monitor delay
		//configDelay2 = 10;
		
		ScheduledFuture<?> monitorTaskFuture = this.monitorScheduler.scheduleAtFixedRate(monitorTask, 
				monitorDelay, 600, TimeUnit.SECONDS);//each 10 minutes
		
	}

	private void startSchedulers()
	{
		this.startMetricScheduler();
		this.startAlertScheduler();
		this.startConfigRetentionScheduler();
		this.startMonitorScheduler();
	}

	private static int getCurrentSeconds()
	{
		Calendar rightNow = Calendar.getInstance();
		int h = rightNow.get(Calendar.HOUR_OF_DAY);
		int m = rightNow.get(Calendar.MINUTE);
		int s = rightNow.get(Calendar.SECOND);
		return h*3600+60*m+s;
	}
	/**
	 * This will stop scheduler, but not the task already running
	 */
	synchronized public void stop()
	{
		//stop the scheduler
		logger.info("Stopping AutoScanner");
		
		forceStop();
		forceStopScheduler(SCHEDULER_MONITOR);
		this.running = false;
		this.metricDb.destroy();
		logger.info("AutoScanner Stopped.");
	}
	synchronized public void forceStopScheduler(String name)
	{
		ScheduledExecutorService svc = null;
		if(SCHEDULER_METRICS.equals(name))
			svc = this.metricsScheduler;
		else if(SCHEDULER_ALERT.equals(name))
			svc = this.alertScheduler;
		if(SCHEDULER_RETENTION.equals(name))
			svc = this.scheduler2;
		if(SCHEDULER_MONITOR.equals(name))
			svc = this.monitorScheduler;
		if(svc == null) return;
		try
		{
			logger.info("Shutdown scheduler " + name);
			svc.shutdownNow();
			svc.awaitTermination(5, TimeUnit.SECONDS);
		}catch(Exception ex)
		{
			logger.info("AutoScanner failed to shutdown in 5 sec.");			
		}
		
	}
	
	/**
	 * This will stop the scheduler and scanner tasks
	 */
	synchronized public void forceStop()
	{
		
		forceStopScheduler(SCHEDULER_ALERT);
		forceStopScheduler(SCHEDULER_METRICS);
		forceStopScheduler(SCHEDULER_RETENTION);
	}
	
	public MyPerfContext getContext() {
		return context;
	}
		

	public MetricsBuffer getBufferByDB(String dbName, String hostName, String metricGroupName)
	{
		if(this.buffer.containsKey(dbName+":"+hostName))
		{
		  if(this.buffer.get(dbName+":"+hostName).containsKey(metricGroupName))
			  return this.buffer.get(dbName+":"+hostName).get(metricGroupName);
		}
		return null;
	}
	public MetricsBuffer getBufferByDB(DBInstanceInfo db, String metricGroupName)
	{
		return getBufferByDB(db.getDbGroupName(), db.getHostName(), metricGroupName);
	}
	
	public MetricsDbBase getMetricDb() {
		return metricDb;
	}

	public void setMetricDb(MetricsDbBase metricDb) {
		this.metricDb = metricDb;
	}

	private final class MetricsScanTask implements Runnable
	{
		private MetricScanner scanner = null;
		public void run() {
			lastScanTime = System.currentTimeMillis();
			Thread.currentThread().setName("RunetimeScanTask");
			scanner = new MetricScanner();
			scanner.setAppUser(appUser);
			scanner.setFrameworkContext(context);
			scanner.setThreadCount(context.getMyperfConfig().getScannerThreadCount());
			scanner.setBuffer(buffer);
			scanner.setRecordCount(context.getMyperfConfig().getRecordRententionDays());
			try
			{
				scanner.scanAuto();
			}catch(Exception ex)
			{
				logger.log(Level.SEVERE, "Exception during auto metric scan", ex);
			}
		}
		public void shutdown()
		{
			if(this.scanner != null)
				this.scanner.shutdown();
		}
	}

	private final class AlertScanTask implements Runnable
	{
		private AlertScanner scanner = null;
		public void run() {
			lastAlertScanTime = System.currentTimeMillis();
			Thread.currentThread().setName("AlertScanTask");
			scanner = new AlertScanner();
			scanner.setAppUser(appUser);
			scanner.setFrameworkContext(context);
			scanner.setThreadCount(context.getMyperfConfig().getScannerThreadCount());
			try
			{
				scanner.scanAuto();
			}catch(Exception ex)
			{
				logger.log(Level.SEVERE, "Exception during alert scan", ex);
			}
		}
		
	}
	
	private final class MonitorTask implements Runnable
	{
		public void run()
		{
			long currentTime = System.currentTimeMillis();
			if(currentTime - lastScanTime >= maxScanIdleTime)//if last run is 3 times of interval
			{
				logger.severe("Metrics Scanner hangs, shut it down.");
				if(metricsTask != null)
					metricsTask.shutdown();
				forceStopScheduler(SCHEDULER_METRICS);				
				try
				{
					Thread.sleep(60000L);
				}catch(Exception ex)
				{
					
				}
				//restart schedulers
				startMetricScheduler();
			}
			if(currentTime - lastAlertScanTime >= maxScanIdleTime)//if last run is 3 times of interval
			{
				logger.severe("Alert Scanner hangs, shut it down.");
				forceStopScheduler(SCHEDULER_ALERT);
				try
				{
					Thread.sleep(60000L);
				}catch(Exception ex)
				{
					
				}
				//restart schedulers
				startAlertScheduler();
			}
		}
	
	}

}
