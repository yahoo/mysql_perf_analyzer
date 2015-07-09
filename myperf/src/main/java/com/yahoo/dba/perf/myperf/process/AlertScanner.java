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

public class AlertScanner{
	private static Logger logger = Logger.getLogger(AlertScanner.class.getName());
	private MyPerfContext frameworkContext;
	private AppUser appUser;//run as user
	private int threadCount = 4;
	private HashMap<String, Map<String,MetricsBuffer>> buffer ;
	protected java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");


	public AlertScanner()
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
		logger.info(new java.util.Date()+": Starting auto alert scan ...");
		java.util.Date startDate = null;
		java.util.Date endDate = null;
	
		try
		{
			startDate = new java.util.Date();
			scan();
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE,"Exception", ex);
		}
		endDate = new java.util.Date();
		logger.info(new java.util.Date()+": Auto alert scan done and status saved. Total Time in ms: "+(endDate.getTime() - startDate.getTime()));
	}
	public void scan()
	{
		Set<String> clusternames = frameworkContext.getDbInfoManager().getMyDatabases(appUser.getName(), false).getMyDbList();
		logger.info("Start scan alerts");
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
				dbqueue.add(dbinfo);
			}
		}
			
		int mythreadcnt = this.threadCount;
		if(dbqueue.size()<mythreadcnt)mythreadcnt = dbqueue.size();
		Thread th[] = new Thread[mythreadcnt];
		for(int i=0;i<mythreadcnt;i++)
		{
			AlertScannerRunner runner = new 
					AlertScannerRunner(frameworkContext,
						dbqueue,
						appUser);
			th[i] = new Thread(runner);
			th[i].setName("AlertScannerRunner - "+i);
			th[i].start();
		}
		for(int i=0;i<th.length;i++)try{th[i].join();}catch(Exception ex){}
	
		logger.info("Done alert scanner");
		this.frameworkContext.getAutoScanner().getMetricDb().flush();//notify persistent store
	}
	public int getThreadCount() {
		return threadCount;
	}
	public void setThreadCount(int threadCount) {
		if(threadCount<=0)threadCount = 1;
		else this.threadCount = threadCount;
	}
}