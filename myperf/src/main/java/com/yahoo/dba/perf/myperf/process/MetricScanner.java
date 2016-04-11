/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.process;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.*;

public class MetricScanner{
	private static Logger logger = Logger.getLogger(MetricScanner.class.getName());
	private MyPerfContext frameworkContext;
	private AppUser appUser;//run as user
	private int threadCount = 4;
	private int recordCount = 840;
	private HashMap<String, Map<String,MetricsBuffer>> buffer ;
	protected java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");

	//private Map<String, List<MetricsGroup>> builtinMetrics = new HashMap<String, List<MetricsGroup>>();

	public MetricScanner()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	public MyPerfContext getFrameworkContext() {
		return frameworkContext;
	}

	public void setFrameworkContext(MyPerfContext frameworkContext) {
		this.frameworkContext = frameworkContext;
	}

	public AppUser getAppUser() {
		return appUser;
	}

	public void setAppUser(AppUser appUser) {
		this.appUser = appUser;
	}

	
	public void scanAuto()
	{			
		logger.info(new java.util.Date()+": Starting auto metrics scan ...");
		java.util.Date startDate = null;
		java.util.Date endDate = null;
	
		int snap_id = -1;
		try
		{
			startDate = new java.util.Date();
			snap_id = this.frameworkContext.getMetricDb().getNextSnapshotId(Long.parseLong(sdf.format(startDate)));
			if(snap_id>=0)
				scan(snap_id);
			else
			{
				logger.severe("Error to generate valid snap_id, got "+snap_id);
			}
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE,"Exception", ex);
		}
		this.frameworkContext.getMetricDb().updateSnapCompleteTime(snap_id, Long.parseLong(sdf.format(new java.util.Date())));
		frameworkContext.getInstanceStatesManager().saveStatus();
		endDate = new java.util.Date();
		logger.info(new java.util.Date()+": Auto metrics scan done and status saved. Total Time in ms: "+(endDate.getTime() - startDate.getTime()));
	}
	//setup metrics buffer if need
	private void checkAndSetupMetricsBuffer(DBInstanceInfo dbinfo)
	{
		//will setup UDM buffer when need
		if(!buffer.containsKey(dbinfo.getDbGroupName()+":"+dbinfo.getHostName()))
		{
			Map<String, MetricsBuffer> ms = new HashMap<String, MetricsBuffer>();
			buffer.put(dbinfo.getDbGroupName()+":"+dbinfo.getHostName(), ms);
		}
		String[] mgNames =  this.frameworkContext.getMetricsDef().getGroupNames();
		for(String mgName: mgNames)
		{
		  MetricsGroup mg = this.frameworkContext.getMetricsDef().getGroupByName(mgName);
		  if (mg == null)continue;
		  
		  if(buffer.containsKey(dbinfo.getDbGroupName()+":"+dbinfo.getHostName()))					  
		  {
			Map<String, MetricsBuffer> bufs = buffer.get(dbinfo.getDbGroupName()+":"+dbinfo.getHostName());
			if( mg.getSubGroups() !=null && mg.getSubGroups().size() > 0)
			{ //sub groups
			  for (MetricsGroup subGrp: mg.getSubGroups())
			  {
				String subGrpName = mg.getGroupName()+"."+subGrp.getGroupName();   
			    if(!bufs.containsKey(subGrpName))
				{
				  //MetricsBuffer buf = new MetricsBuffer(mg, this.recordCount+20);
				  MetricsBuffer buf = new MetricsBuffer(subGrp);//no more cache
				  buf.setDbid(dbinfo.getDbid());
				  logger.info("add buffer for "+dbinfo+", " + subGrpName);
				  bufs.put(subGrpName, buf);
				}				    
				  
			  }
			}else //top level
			{
			  if(!bufs.containsKey(mg.getGroupName()))
			  {
			 	//MetricsBuffer buf = new MetricsBuffer(mg, this.recordCount+20);
				MetricsBuffer buf = new MetricsBuffer(mg);//no more cache
				buf.setDbid(dbinfo.getDbid());
				logger.warning("add buffer for "+dbinfo+", "+mg.getGroupName());
				bufs.put(mg.getGroupName(), buf);
			  }
			}
		  }
		}		
	}
	
	private MetricScannerRunner[] metricsScannerRunners = null;
	public void scan(int snap_id)
	{
		Set<String> clusternames = frameworkContext.getDbInfoManager().getMyDatabases(appUser.getName(), false).getMyDbList();
		logger.info("Start scan metrics");
		if (this.buffer == null)
		{
			logger.severe("Data buffer was not found. Scan cannot continue.");
			return;
		}
		LinkedBlockingQueue<DBInstanceInfo> dbqueue = new LinkedBlockingQueue<DBInstanceInfo>();
		for(String cl: clusternames)
		{
			DBCredential cred = DBUtils.findDBCredential(frameworkContext, cl, appUser);
			if(cred==null)
			{
				logger.info("No credential for group "+cl+", skip it");
				continue;//log the error
			}
			DBGroupInfo cls = frameworkContext.getDbInfoManager().findGroup(cl);
			if(cls==null)
			{
				logger.info("Group "+cl+" might have been deleted.");
				continue;
			}
			
			for(DBInstanceInfo dbinfo: cls.getInstances())
			{
				checkAndSetupMetricsBuffer(dbinfo);
				dbqueue.add(dbinfo);
			}
		}
			
		int mythreadcnt = this.threadCount;
		if(dbqueue.size()<mythreadcnt)mythreadcnt = dbqueue.size();
		Thread th[] = new Thread[mythreadcnt];
		metricsScannerRunners = new MetricScannerRunner[mythreadcnt];
		for(int i=0;i<mythreadcnt;i++)
		{
			MetricScannerRunner runner = new 
					MetricScannerRunner(frameworkContext,
						dbqueue,
						appUser,
						snap_id);
			runner.setBuffer(buffer);
//			runner.setBuiltinMetrics(builtinMetrics);
			th[i] = new Thread(runner);
			metricsScannerRunners[i] = runner;
			th[i].setName("MetricScannerRunner - "+i);
			th[i].start();
		}
		for(int i=0;i<th.length;i++)try{th[i].join();}catch(Exception ex){}
	
		logger.info("Done gather metrics");
		this.frameworkContext.getAutoScanner().getMetricDb().flush();//notify persistent store
	}
	public void shutdown()
	{
		if(this.metricsScannerRunners != null)
		{
			for(MetricScannerRunner runner: this.metricsScannerRunners)
			{
				if(runner != null)runner.shutdown();
			}
		}
	}
	public int getThreadCount() {
		return threadCount;
	}
	public void setThreadCount(int threadCount) {
		if(threadCount<=0)threadCount = 1;
		else this.threadCount = threadCount;
	}
	public HashMap<String, Map<String,MetricsBuffer>> getBuffer() {
		return buffer;
	}
	public void setBuffer(HashMap<String, Map<String,MetricsBuffer>> buffer) {
		this.buffer = buffer;
	}
	public int getRecordCount() {
		return recordCount;
	}
	public void setRecordCount(int recordCount) {
		this.recordCount = recordCount;
	}
}