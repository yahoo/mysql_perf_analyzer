/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.DBGroupInfo;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.MetricsGroup;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.UserDefinedMetrics;
import com.yahoo.dba.perf.myperf.metrics.MetricsDbBase;

/**
 * Handle metrics retention 
 * @author xrao
 *
 */
public class MetricsRetentionTask implements Runnable{

  private static Logger logger = Logger.getLogger(MetricsRetentionTask.class.getName());
  
  private int renentionDays;
  private MyPerfContext context;

  private int[] dbidToPurge = null; //set as one time job, reuse code 
  
  /**
   * 
   * @param context
   * @param metricsDB
   * @param renentionDays will be ignored if dbidToPurge is set
   * @param dbidToPurge use to start a one time job to purge data for a specific db, set to -1 otherwise 
   */
  public MetricsRetentionTask(MyPerfContext context, 
		  int renentionDays,
		  int[] dbidToPurge)
  {
    this.renentionDays = renentionDays;
    this.context = context;
    this.dbidToPurge = dbidToPurge;
  }
    
  @Override
  public void run() 
  {
	Thread.currentThread().setName("MetricsRetentionTask");
	logger.info("Starting metrics purge job");
	
	if(this.context.getMetricDb() == null)
	{
		logger.info("MetricsDB has yet to set.");		
		return;
	}
	//get all dbids
    List<Integer> ids = new ArrayList<Integer>();
    if(this.dbidToPurge == null)
    {
      for(Map.Entry<String, DBGroupInfo> e: context.getDbInfoManager().getClusters().entrySet())
      {
        DBGroupInfo g = e.getValue();
        for(DBInstanceInfo i: g.getInstances())
        {
          ids.add(i.getDbid());	  
       }
      } 
    }else
    {
      for(int id : this.dbidToPurge)
    	ids.add(id);
    }
    Calendar c = Calendar.getInstance();
	c.add(Calendar.DATE, -this.renentionDays);
	Date dt = c.getTime();			

    //now get current timestamp
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
	sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    long endDate = Long.parseLong(sdf.format(dt));
    List<MetricsGroup> mgs = new ArrayList<MetricsGroup>();

    String [] groupNames = this.context.getMetricsDef().getGroupNames();
    for(String grpName: groupNames)
    {
      MetricsGroup group = this.context.getMetricsDef().getGroupByName(grpName);
      if(group == null)continue; //not supposed to be so
      if (group.getSubGroups().size() == 0) //no sub group, add self
      {
    	    mgs.add(group);
	  }else
      {
    	  for(MetricsGroup g: group.getSubGroups())
    		  mgs.add(g);
      }
    }
	
    for(Map.Entry<String, UserDefinedMetrics> entry: this.context.getMetricsDef().getUdmManager().getUdms().entrySet())
    {
      MetricsGroup group = entry.getValue().getMetricsGroup();
      if(group == null)continue; //not supposed to be so
      mgs.add(group);
    }

    for(int dbid: ids)
    {
      logger.info("Check and purge db: "+dbid);
      for(MetricsGroup g: mgs)
      {
    	if(this.dbidToPurge != null )
    	  this.context.getMetricDb().purgeAll(g.getSinkTableName(), dbid);
    	else
    	  this.context.getMetricDb().purge(g.getSinkTableName(), dbid, endDate);
      }
    }
    java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyyMMdd");
	sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    purgeAlertReports(Integer.parseInt(sdf2.format(dt)));
	logger.info("Ended metrics purge job");

	if(this.dbidToPurge == null)
		this.context.getMetricDb().purgeAlerts(endDate);
	
	//for now, we only do it once a day.
	//TODO keep consistency with DB add/update/delete
	logger.info("Retention job done.");
  }
  
  /**
   * 
   * @param lastPurgeDate The last date the report will be purged, in YYYYMMDD format
   */
  private void purgeAlertReports(int lastPurgeDate)
  {
	  try
	  {
	    //also remove old alert reports
	    //report root
		File rootPath = this.context.getAlertRootPath();
		if(!rootPath.exists() && !rootPath.isDirectory())
		{
			logger.warning("Failed to find alert root path: "+rootPath.getPath());
			return;
		}
		logger.warning("Start to purge alert reports: "+rootPath.getPath()+", before "+lastPurgeDate);
		
		File[] dirByDates = rootPath.listFiles();
		for(File dir: dirByDates)
		{
			String name = dir.getName();
			logger.info("Trying to delete report for date: "+name);				
			if(name.indexOf(".")>0)
				name = name.substring(0, name.indexOf("."));
			try
			{
				int nameDate = Integer.parseInt(name);
				if(nameDate<lastPurgeDate)
				{
					if(dir.isDirectory())
					{
						for(File ar: dir.listFiles())
						{
							//don't expect subdir
							if(ar.isFile())
								ar.delete();
						}
					}
					dir.delete();
				}
			}catch(Exception ex)
			{
				logger.warning("Failed to delete report for date: "+name);				
			}
		}
	  }catch(Throwable th)
	  {
		  logger.warning("Failed to purge report data: "+th.getMessage());
	  }
  }
}
