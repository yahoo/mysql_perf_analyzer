/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.process;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.*;
import com.yahoo.dba.perf.myperf.common.SNMPSettings.SNMPSetting;
import com.yahoo.dba.perf.myperf.db.DBConnectionWrapper;
import com.yahoo.dba.perf.myperf.db.InnoDbMutexPostProccessor;
import com.yahoo.dba.perf.myperf.db.UserDBConnections;
import com.yahoo.dba.perf.myperf.snmp.SNMPClient;

/**
 * A thread runner executing the real metrics gathering tasks
 * @author xrao
 *
 */
public class MetricScannerRunner implements Runnable
{
  private static Logger logger = Logger.getLogger(MetricScannerRunner.class.getName());
  private MyPerfContext frameworkContext;
	
  //note those objects are shared, so we need handle synchronization here	
  private LinkedBlockingQueue<DBInstanceInfo> dbqueue;//distributed job
  private AppUser appUser;//to get db credentials
  private UserDBConnections conns;
  private Map<String,Map<String, MetricsBuffer>> buffer ;
  private int snap_id;
  private AtomicBoolean running = new AtomicBoolean();
  
  protected java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");

  public MetricScannerRunner(MyPerfContext frameworkContext,
			LinkedBlockingQueue<DBInstanceInfo> dbqueue,
			AppUser appUser,
			int snap_id)
  {
    this.frameworkContext = frameworkContext;
	this.dbqueue = dbqueue;
	this.appUser = appUser;
	this.snap_id = snap_id;
	sdf.setTimeZone(TimeZone.getTimeZone("UTC"));	  
  }
  
  public void run() 
  {
    scan();
  }

  private void scan()
  {
	running.set(true);
    conns = new UserDBConnections();
	conns.setAppUser(appUser.getName());
	conns.setFrameworkContext(frameworkContext);
	int count = 0;
	while(!dbqueue.isEmpty() && running.get())
	{
	  try
	  {
	    DBInstanceInfo db = this.dbqueue.poll(1, TimeUnit.SECONDS);
	    count++;
		if(db==null || !db.isMetricsEnabled())continue;
		long startTime = System.currentTimeMillis();
		scanHost(db);
		//record total scan time for each server
		this.frameworkContext.getInstanceStatesManager().getStates(db.getDbid())
		    .setLastScanTime(System.currentTimeMillis() - startTime);
	  }catch(Exception ex)
	  {
	    logger.log(Level.WARNING, "Exception: scan loop", ex);
	  }
	}
	logger.info(Thread.currentThread()+" done scan metrics: "+count+" servers.");
  }

  public void shutdown()
  {
	  logger.info("Shutdown");
	  running.set(false);
  }

  private Map<String, Float> getAlertThreshold(DBInstanceInfo dbinfo)
  {
	  HashMap<String, Float> thresholds = new HashMap<String, Float>(AlertSettings.COMMON_ALERTS.length);
	  for(String alertType: AlertSettings.COMMON_ALERTS)
	    thresholds.put(alertType, this.frameworkContext
			  .getAlertSettings()
			  .getAlertThreshold(dbinfo.getDbGroupName(), dbinfo.getHostName(), alertType));  
	  return thresholds;
  }

  private void reportOffline(DBInstanceInfo dbinfo)
  {
	  try
	  {
		  java.util.Date dt =  new java.util.Date();
		  if(dbinfo.isAlertEnabled() && this.frameworkContext.getInstanceStatesManager().getStates(dbinfo.getDbid()).reportOffline(true, dt))
		  {
			  AlertEntry alert = new AlertEntry(dt.getTime(), "OFFLINE", null, dbinfo.getDbGroupName(), dbinfo.getHostName());
			  this.frameworkContext.getAutoScanner().getMetricDb().addNewAlert(Long.parseLong(sdf.format(dt)), dbinfo.getDbid(), "OFFLINE",  "OFFLINE");
			  this.frameworkContext.addAlert(alert);
			  this.frameworkContext.emailAlert(alert);
		  }
	  }catch(Exception ex)
	  {
		  
	  }
  }

  private void checkAndReportDiskUsageAlert(DBInstanceInfo dbinfo, String diskMount, long value)
  {
	  Float threshold = this.frameworkContext.getAlertSettings().getAlertThreshold(dbinfo, "DISKUSAGE");
	  if(threshold == null || (float)value <= threshold )
	  {
		  //logger.info("Disk usage is under control: mount - "+diskMount+", used - "+value+", threshold - "+threshold);
		  return;
	  }
	  try
	  {
		  java.util.Date dt =  new java.util.Date();
		  if(dbinfo.isAlertEnabled() 
				  && this.frameworkContext.getInstanceStatesManager().getStates(
						  dbinfo.getDbid()).reportUserAlert(dt, "DISKUSAGE", diskMount +" "+ String.valueOf(value)+"%"))
		  {
			  AlertEntry alert = new AlertEntry(dt.getTime(), "DISKUSAGE", diskMount+" "+value+"%", dbinfo.getDbGroupName(), dbinfo.getHostName());
			  this.frameworkContext.getMetricDb().addNewAlert(Long.parseLong(sdf.format(dt)), dbinfo.getDbid(), "DISKUSAGE",  diskMount+" "+value+"%");
			  this.frameworkContext.addAlert(alert);
			  this.frameworkContext.emailAlert(alert);
		  }
	  }catch(Exception ex)
	  {
		  
	  }
  }

  /**
   * To hold intermediate data
   * @author xrao
   *
   */
  private class ScanData
  {
	  DBInstanceInfo dbinfo = null;
	  DBConnectionWrapper conn = null;//find one connection is enough
	  StateSnapshot stateSnap = new StateSnapshot();
	  int snap_id;
	  long startTimestamp;
	  boolean statusUpdated = false;
	  boolean reportGenerated = false;//now report will generate at the moment the issue is detected
	  boolean hasSnapAlert = false;
	  String snapAlertType = null;
	  String snapAlertValue = null;
	  boolean canQuerySNMP = true;//since we do multi round SNMP, if one failed, no meaning for next
  }
  
  private void scanSnmpDisk(ScanData scanData, MetricsGroup mg) throws Exception
  {
	  if(scanData.dbinfo.isSnmpEnabled() && scanData.canQuerySNMP)
	  {
		SNMPClient  snmpClient = null;
	    try
	    {
	      snmpClient = new SNMPClient(scanData.dbinfo.getHostName());
	      snmpClient.setSnmpSetting(this.frameworkContext.getSnmpSettings()
	    		  .getHostSetting(scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName()));
	      snmpClient.start();	
		  long ts1 = System.currentTimeMillis();  
		  Map<String, List<SNMPClient.SNMPTriple>> diskStats = snmpClient.getMultiDiskData();
		  long ts2 = System.currentTimeMillis();
		  for(Map.Entry<String, List<SNMPClient.SNMPTriple>> entry: diskStats.entrySet())
		  {
 		    Map<String, String> snmpRes = new HashMap<String, String>();
		    for(SNMPClient.SNMPTriple e: entry.getValue())
		    {
			  if(entry.getKey().equals(e.value))continue;
			  try
			  {
				  Long.parseLong(e.value);
				  snmpRes.put(e.name, e.value);
			  }catch(Exception ex)
			  {						  
				  snmpRes.put(e.name, "0");
			  }
		    }
		    storeKVTabularData(scanData, mg, entry.getKey(), snmpRes, (int)(ts2 - ts1 ));
		  }
	    }catch(Throwable iex)
	    {
		  logger.log(Level.WARNING, "Faled to scan snmp for "+scanData.dbinfo.getHostName(), iex);
		  scanData.canQuerySNMP = false;
	    }finally
	    {
	    	if(snmpClient!=null)try{snmpClient.stop();}catch(Exception snmpExc){}
	    }
	  }
	  
  }
  private void scanSnmpStorage(ScanData scanData, MetricsGroup mg) throws Exception
  {
	  if(scanData.dbinfo.isSnmpEnabled() && scanData.canQuerySNMP)
	  {
		SNMPClient  snmpClient = null;
	    try
	    {
	      snmpClient = new SNMPClient(scanData.dbinfo.getHostName());
	      snmpClient.setSnmpSetting(this.frameworkContext.getSnmpSettings()
	    		  .getHostSetting(scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName()));
	      snmpClient.start();	
		  long ts1 = System.currentTimeMillis();  
		  Map<String, List<SNMPClient.SNMPTriple>> diskStats = snmpClient.getStorageData(null);
		  long ts2 = System.currentTimeMillis();
		  for(Map.Entry<String, List<SNMPClient.SNMPTriple>> entry: diskStats.entrySet())
		  {
			String key = entry.getKey();
			//excluding memroy stuff
			if ("Cached memory".equals(key) ||"Memory buffers".equals(key)
					||"Physical memory".equals(key)||"/dev/shm".equals(key)
					||"Swap space".equals(key) ||"Virtual memory".endsWith(key))
				continue;
 		    Map<String, String> snmpRes = new HashMap<String, String>();
 		    long diskUsed = 0L;
 		    long diskSize = 0L;
		    for(SNMPClient.SNMPTriple e: entry.getValue())
		    {
			  if(entry.getKey().equals(e.value))continue;
			  try
			  {
				  long lval = Long.parseLong(e.value);
				  if("HRSTORAGEUSED".equalsIgnoreCase(e.name))
					  diskUsed = lval;
				  else if("HRSTORAGESIZE".equalsIgnoreCase(e.name))
				  {
					  diskSize = lval;
				  }
				  snmpRes.put(e.name, e.value);
			  }catch(Exception ex)
			  {						  
				  snmpRes.put(e.name, "0");
			  }
		    }
		    long used_pct = (diskSize != 0L)? Math.round((diskUsed*100.0)/diskSize) : 0L;
		    snmpRes.put("USED_PCT", String.valueOf(used_pct));
		    storeKVTabularData(scanData, mg, entry.getKey(), snmpRes, (int)(ts2 - ts1 ));
		    checkAndReportDiskUsageAlert(scanData.dbinfo, entry.getKey(), used_pct);
		  }
	    }catch(Throwable iex)
	    {
		  logger.log(Level.WARNING, "Faled to scan snmp for "+scanData.dbinfo.getHostName(), iex);
		  scanData.canQuerySNMP = false;
	    }finally
	    {
	    	if(snmpClient!=null)try{snmpClient.stop();}catch(Exception snmpExc){}
	    }
	  }
	  
  }
  private void scanSnmpNet(ScanData scanData, MetricsGroup mg) throws Exception
  {
	  if(scanData.dbinfo.isSnmpEnabled() && scanData.canQuerySNMP)
	  {
		SNMPClient  snmpClient = null;
	    try
	    {
	      snmpClient = new SNMPClient(scanData.dbinfo.getHostName());
	      snmpClient.setSnmpSetting(this.frameworkContext.getSnmpSettings()
	    		  .getHostSetting(scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName()));
	      snmpClient.start();	
		  long ts1 = System.currentTimeMillis();  
		  Map<String, List<SNMPClient.SNMPTriple>> diskStats = snmpClient.getNetIfData(null);//assume eth0
		  long ts2 = System.currentTimeMillis();
		  if(diskStats!=null && diskStats.size()>0)
		  {
			for(Map.Entry<String, List<SNMPClient.SNMPTriple>> entry: diskStats.entrySet())
			{
			  if(entry.getValue().size() == 0)continue;	
		      Map<String, String> snmpRes = new HashMap<String, String>();
		      for(SNMPClient.SNMPTriple e: entry.getValue())
		      {
			    if(entry.getKey().equals(e.value))continue;
			    try
			    {
				  Long.parseLong(e.value);
				  snmpRes.put(e.name, e.value);
			    }catch(Exception ex)
			    {						  
				  snmpRes.put(e.name, "0");
			    }
		      }
			  storeKVTabularData(scanData, mg, entry.getKey(), snmpRes, (int)(ts2 - ts1 ));
			}
		  }
	    }catch(Throwable iex)
	    {
		  logger.log(Level.WARNING, "Faled to scan snmp for "+scanData.dbinfo.getHostName(), iex);
		  scanData.canQuerySNMP = false;
	    }finally
	    {
	    	if(snmpClient!=null)try{snmpClient.stop();}catch(Exception snmpExc){}
	    }
	  }
	  
  }
  private void scanSnmpSys(ScanData scanData, MetricsGroup mg) throws Exception
  {
	  if(scanData.dbinfo.isSnmpEnabled() && scanData.canQuerySNMP)
	  {
		SNMPClient  snmpClient = null;
	    try
	    {
	      snmpClient = new SNMPClient(scanData.dbinfo.getHostName());
	      snmpClient.setSnmpSetting(this.frameworkContext.getSnmpSettings()
	    		  .getHostSetting(scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName()));
	      snmpClient.start();	
		  long ts1 = System.currentTimeMillis();  
		  Map<String, String> snmpRes = snmpClient.querySysData();				  
		  long ts2 = System.currentTimeMillis();
		  scanData.stateSnap.recordSnmpStats(snmpRes);
		  storeKVTabularData(scanData, mg, null, snmpRes, (int)(ts2 - ts1 ));

		  {
			float loadavg = CommonUtils.getMapValueFloat(snmpRes, "laLoad5m", 0.0f);
			if(!scanData.reportGenerated && loadavg>=this.frameworkContext.getAlertSettings().getAlertThreshold(scanData.dbinfo, "LOADAVG"))
			{
				scanData.hasSnapAlert = true;
				scanData.snapAlertType = "LOADAVG";
				scanData.snapAlertValue = String.valueOf(loadavg);
				 recordAlertReport(scanData.conn, scanData.dbinfo, scanData.startTimestamp,"LOADAVG", String.valueOf(loadavg));
				 scanData.reportGenerated = true;
			}
		  }
		  
	    }catch(Throwable iex)
	    {
		  logger.log(Level.WARNING, "Faled to scan snmp for "+scanData.dbinfo.getHostName(), iex);
		  scanData.canQuerySNMP = false;
	    }
	    finally
	    {
	    	try{if(snmpClient!=null)snmpClient.stop();}catch(Exception snmpExc){}
	    }
	  }

  }

  private void scanSnmpMysqld(ScanData scanData, MetricsGroup mg) throws Exception
  {
	  if(scanData.dbinfo.isSnmpEnabled() && scanData.canQuerySNMP)
	  {
		SNMPClient  snmpClient = null;
	    try
	    {
	      snmpClient = new SNMPClient(scanData.dbinfo.getHostName());
	      snmpClient.setSnmpSetting(this.frameworkContext.getSnmpSettings()
	    		  .getHostSetting(scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName()));
	      snmpClient.start();	
		  long ts1 = System.currentTimeMillis();  
		  Map<String, String> snmpRes = snmpClient.queryMysqld();				  
		  long ts2 = System.currentTimeMillis();
		  //recordSnmpStats(scanData.stateSnap, snmpRes);
		  storeKVTabularData(scanData, mg, null, snmpRes, (int)(ts2 - ts1 ));		  
	    }catch(Throwable iex)
	    {
		  logger.log(Level.WARNING, "Faled to scan mysqld snmp for "+scanData.dbinfo.getHostName(), iex);
		  scanData.canQuerySNMP = false;
	    }
	    finally
	    {
	    	try{if(snmpClient!=null)snmpClient.stop();}catch(Exception snmpExc){}
	    }
	  }

  }

  private void scanRepl(ScanData scanData, MetricsGroup mg) throws Exception
  {
		long ts1 = System.currentTimeMillis();
		HashMap<String, String> resMap =   this.frameworkContext.getQueryEngine().showSlaveStatusMetrics(scanData.conn);
		long ts2 = System.currentTimeMillis();
		if(resMap!=null && resMap.size()>0)
		{
			scanData.stateSnap.recordRepl(resMap);
			storeKVTabularData(scanData, mg, null, resMap, (int)(ts2 - ts1 ));
		}	  
  }
  private void scanInnoDBMutex(ScanData scanData, MetricsGroup mg) throws Exception
  {
	  QueryParameters qps = new QueryParameters();
	  qps.setSql(mg.getSql());
	  ResultList rs = null;
	  long ts1 = System.currentTimeMillis();
	  try
	  {
		  rs = this.frameworkContext.getQueryEngine().executeQuery(qps, scanData.conn, 2000);
	  }catch(Exception iex)
	  {
		  logger.log(Level.INFO, "failed to retrieve innodb_mutex for "+scanData.dbinfo, iex);
	  }
	  if(rs!=null)
	  {
		  InnoDbMutexPostProccessor mp = new InnoDbMutexPostProccessor();
		  rs = mp.process(rs);
			  
		  long ts2 = System.currentTimeMillis();
		  HashMap<String, String> resMap = new HashMap<String, String>(rs.getRows().size());
		  for(ResultRow row: rs.getRows())
		  {
			  if(row.getColumns()==null||row.getColumns().size()<2)continue;//skip null
			  resMap.put(row.getColumns().get(0), row.getColumns().get(1));
		  }
		  storeKVTabularData(scanData, mg, null, resMap, (int)(ts2 - ts1 ));
	  }			  
  
  }
  
  //Retrieve metrics buffer for a given group or sub group
  private MetricsBuffer retrieveMetricsBuffer(ScanData scanData, MetricsGroup mg, MetricsGroup subGrp)
  {	
	 
    if(!buffer.containsKey(scanData.dbinfo.getDbGroupName()+":"+scanData.dbinfo.getHostName()))
    	return null;
    if(mg.isUdmFlagged())//we have to deal with UDM on the fly
    {
    	String grpKey = "UDM."+mg.getGroupName();
    	if(!buffer.get(scanData.dbinfo.getDbGroupName()+":"+scanData.dbinfo.getHostName()).containsKey(grpKey))
    	{
			MetricsBuffer buf = new MetricsBuffer(mg);//no more cache
			buf.setDbid(scanData.dbinfo.getDbid());
			buffer.get(scanData.dbinfo.getDbGroupName()+":"+scanData.dbinfo.getHostName()).put(grpKey, buf);
    	}
    	return buffer.get(scanData.dbinfo.getDbGroupName()+":"+scanData.dbinfo.getHostName()).get(grpKey);
    }
    else if(subGrp == null)//top level group
      return buffer.get(scanData.dbinfo.getDbGroupName()+":"+scanData.dbinfo.getHostName())
    	      .get(mg.getGroupName());
    else
      return 
    		  buffer.get(scanData.dbinfo.getDbGroupName()+":"+scanData.dbinfo.getHostName())
    	      .get(mg.getGroupName()+"."+subGrp.getGroupName());
    	      
  }

  /**
   * Store data in shared table
   * @param scanData
   * @param mg
   * @param key
   * @param kvPairs
   * @param queryTime
   * @throws SQLException
   */
  private void storeKVTabularDataGeneric(ScanData scanData, MetricsGroup mg, 
		  Map<String, String> kvPairs, int queryTime) throws SQLException
  {
	  if(mg == null || !mg.isStoreInCommonTable())
	  {
		  logger.warning(mg.getGroupName()+" is not defined to store metrics in shared table. Ignore it.");
		  return;
	  }
	  if(kvPairs == null || kvPairs.size() ==0)
	  {
		  return; //no data to store
	  }
	  for(Metric m: mg.getMetrics())
	  {
		String srcName = m.getSourceName().toLowerCase();
	  	if(!kvPairs.containsKey(srcName))continue;
	  	int metricId = this.frameworkContext.getAutoScanner().getMetricDb()
	  			.checkAndAddMetricCode(mg.getMetricFullName(m.getName()));
	  	if(metricId<=0)
	  	{
	  		logger.warning("Cannot find metric id for "+m.getName() + ", " 
	  				+ mg.getMetricFullName(m.getName()));//TODO cache local
	  		break;//stop here
	  	}
	  	try
	  	{	  	
	  		byte[] buf = new byte[28];
	  		java.nio.ByteBuffer buf2 = java.nio.ByteBuffer.wrap(buf);
	  		int pos = 0;
	  		buf2.putInt(pos, scanData.dbinfo.getDbid());pos+=4;
	  		buf2.putInt(pos,metricId); pos+=4;
	  		buf2.putInt(pos,scanData.snap_id); pos+=4;
	  		buf2.putLong(pos, scanData.startTimestamp);pos+=8;
	  		buf2.putDouble(pos, Double.parseDouble(kvPairs.get(srcName)));
	  		this.frameworkContext.getAutoScanner().getMetricDb().putData(mg, null, buf2);					
	  	}catch(Exception ex){}
	  	
	  }
  }
  
  /**
   * Store the data from mapData into persistent storage
   * @param scanData meta data like DB
   * @param mg MetricsGroup
   * @param kvPairs source Data in key value pair
   * @param queryTime time used to gather source data
 * @throws SQLException 
   */
  private void storeKVTabularData(ScanData scanData, MetricsGroup mg, String key, Map<String, String> kvPairs, int queryTime) throws SQLException
  {
    if (mg == null)return;
    if(mg.isStoreInCommonTable())
    {
    	storeKVTabularDataGeneric(scanData,  mg, 
    			  kvPairs,  queryTime);
    	return;
    }
    //into multiple tables?
    //we should not use storeincommontable for subgroup
	if (mg.getSubGroups() != null && mg.getSubGroups().size() >0)  
	{
      for (MetricsGroup subGrp: mg.getSubGroups())
	  {
    	recordUserAlertsFromMetricsData(scanData, subGrp, kvPairs);
    	if(!subGrp.isAuto())
    	{
  		  if(!mg.isAuto())
  		  { //Skip groups requiring manual configuration for now 
  			if(!this.frameworkContext.getMetricsDef().getUdmManager().isMetricsGroupSubscribed(
  					scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName(), mg.getGroupName(), subGrp.getGroupName()))
  				continue;
  		  }
    	}
	    MetricsBuffer mbuf = retrieveMetricsBuffer(scanData,  mg, subGrp);
	    if(mbuf != null)
		{
		  ByteBuffer buf = mbuf.recordOneRowByMetricsMap(kvPairs, scanData.snap_id, scanData.startTimestamp, queryTime);
		  if(buf != null )this.frameworkContext.getMetricDb().putData(subGrp, key, buf);
	    }
	  }
	}else
	{
      recordUserAlertsFromMetricsData(scanData, mg, kvPairs);
	  MetricsBuffer mbuf = retrieveMetricsBuffer(scanData,  mg, null);
	  if(mbuf != null)
	  {
	    ByteBuffer buf = mbuf.recordOneRowByMetricsMap(kvPairs, scanData.snap_id, scanData.startTimestamp, queryTime);
		if (buf != null)this.frameworkContext.getMetricDb().putData(mg, key, buf);
	  }
	}
	//TODO attached UDM
	List<String> attachedUDMs = this.frameworkContext.getMetricsDef().getUdmManager().getUDMsAttachedToBuiltinMetrics(
			mg.getGroupName(), 
			scanData.dbinfo.getDbGroupName(),
			scanData.dbinfo.getHostName());
	if(attachedUDMs == null || attachedUDMs.size() == 0)return;
	for(String s: attachedUDMs)
	{
		UserDefinedMetrics udm = this.frameworkContext.getMetricsDef().getUdmManager().getUDMByName(s);
		if(udm == null || udm.getMetricsGroup() == null)continue;
		MetricsGroup udmG = udm.getMetricsGroup();
		MetricsBuffer mbuf = retrieveMetricsBuffer(scanData,  udmG, null);
		if(mbuf != null)
		{
		  ByteBuffer buf = mbuf.recordOneRowByMetricsMap(kvPairs, scanData.snap_id, scanData.startTimestamp, queryTime);
	      if (buf != null)this.frameworkContext.getMetricDb().putData(udmG, key, buf);
		}		
	}
  }
  
  private void recordUserAlertsFromMetricsData(ScanData scanData, MetricsGroup mg, Map<String, String> kvPairs)
  {
    if(kvPairs == null)return;
    String mgName = "";
    if(mg.getParentGroup()!=null) mgName = mg.getParentGroup()+".";
    else if(mg.isUdmFlagged()) mgName = "UDM.";
    mgName += mg.getGroupName();
    List<AlertSubscribers.Subscription> subs = this.frameworkContext.getMetricsDef().getUdmManager()
    		.getAlertsAttachedToBuiltinMetrics(mgName, scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName());
    if(subs == null | subs.size() == 0)return;
    for(AlertSubscribers.Subscription sub: subs)
    {
    	AlertDefinition def = this.frameworkContext.getMetricsDef().getUdmManager().getAlerts().get(sub.alertName);
    	String mname = def.getMetricName();
    	int idx = mname.indexOf(".");
    	if(idx > 0)
    	{
    		mname = mname.substring(idx+1);
    		String srcName = null;
    		for(Metric m: mg.getMetrics())
    		{
    			if(mname.equalsIgnoreCase(m.getSourceName()))
    			{
    				srcName = m.getSourceName();
    				break;
    			}
    		}
    		if(srcName != null && kvPairs.containsKey(srcName))
    		{
    			Float f = CommonUtils.getMapValueFloat(kvPairs, srcName, 0.0f);
    			scanData.stateSnap.addMetric(def.getName(), f);
    		}
    	}   	
    }
  }
  
  private void recordUserAlertsFromGlobalStatus(ScanData scanData, Map<String, String> kvPairs)
  {
    if(kvPairs == null)return;
    List<AlertSubscribers.Subscription> subs = this.frameworkContext.getMetricsDef().getUdmManager()
    		.getAlertsAttachedToGlobalStatus(scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName());
    
    if(subs == null | subs.size() == 0)return;
    for(AlertSubscribers.Subscription sub: subs)
    {
    	AlertDefinition def = this.frameworkContext.getMetricsDef().getUdmManager().getAlerts().get(sub.alertName);
    	String mname = def.getMetricName();
    	if(mname != null && kvPairs.containsKey(mname))
    	{
    		Float f = CommonUtils.getMapValueFloat(kvPairs, mname, 0.0f);
    		scanData.stateSnap.addMetric(def.getName(), f);
    		//logger.info("Global status alert data: "+def.getName()+", metrics: "+mname+", value: " + f);
    	}
    }
  }

  private void scanRegularPredefined(ScanData scanData, MetricsGroup mg) throws Exception
  {
	//logger.info("Scan group "+mg.getGroupName()+" for "+scanData.dbinfo+", udm: "+mg.isUdmFlagged());
	//The idea to use sub group is, we can scan once and update multiple sub groups.  
    QueryParameters qps = new QueryParameters();
    if(mg.isUdmFlagged())
    	qps.setSqlText(mg.getSqlText());
    else
    	qps.setSql(mg.getSql());
	long q_startTime = System.currentTimeMillis();
	if(mg.getKeyColumn() == null || mg.getKeyColumn().isEmpty())
	{
		Map<String, String> kvPairs = null;	
	  if("mysql_global_status".equalsIgnoreCase(mg.getSql()))
	  {
		  qps.setSql("mysql_show_global_status");//hardwire to adapt to mysql-5.7
		  kvPairs = this.frameworkContext.getQueryEngine().executeQueryWithKeyValuPairs(qps, scanData.conn, mg.getMetricNameColumn(), "VALUE", true);
	  }
	  else
		  kvPairs = this.frameworkContext.getQueryEngine().executeQueryWithKeyValuPairs(qps, scanData.conn, mg.getMetricNameColumn(), mg.getMetricValueColumn(), false);
	  storeKVTabularData(scanData, mg, null,kvPairs, (int)(System.currentTimeMillis() - q_startTime));    
  	  if ("STATUS".equalsIgnoreCase(mg.getGroupName()))
  	  {
  		scanData.stateSnap.recordSnapFromMySQLStatus(kvPairs);//built in alerts
		recordUserAlertsFromGlobalStatus(scanData, kvPairs);//user alerts
  	  }
	}else
	{
		Map<String, Map<String, String>> mkvPairs = this.frameworkContext.getQueryEngine().executeQueryWithMultipleColumns(qps, scanData.conn, mg.getKeyColumn());
		for(Map.Entry<String, Map<String, String>> kvEntry: mkvPairs.entrySet())
		{
			storeKVTabularData(scanData, mg, kvEntry.getKey(), kvEntry.getValue(), (int)(System.currentTimeMillis() - q_startTime));
		}
	}
	java.util.Date dt = new java.util.Date(); 
	this.frameworkContext.getDbInfoManager().updateLastScanTime(scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName(), dt);
	this.frameworkContext.getInstanceStatesManager().getStates(scanData.dbinfo.getDbid()).setLastAccessTime(dt);
  }
  
  //for UDM, now we will treat it same as scanRegularPredefined 
  private void scanUDM(ScanData scanData, UserDefinedMetrics udm) throws Exception
  {
    logger.info("Scan UDM "+udm.getName()+" for "+scanData.dbinfo);
    scanRegularPredefined(scanData, udm.getMetricsGroup());
  }
  
  private boolean tryConnect(ScanData scanData)
  {
		DBCredential cred = DBUtils.findDBCredential(frameworkContext, scanData.dbinfo.getDbGroupName(), appUser);
		if(cred==null)
		{
		  logger.info("No credential for cluster "+scanData.dbinfo.getDbGroupName()+", skip it");
		  return false;//log the error
		}
		logger.fine("Scan for host ("+scanData.dbinfo+") as user "+cred.getUsername());
	  long connStartTime = System.currentTimeMillis();
	  long connEndTime = -1L;
	  long connTime = -1L;
	  try
	  {
	      scanData.conn = conns.checkoutConnection(scanData.dbinfo, cred);
	      connEndTime = System.currentTimeMillis();
	      connTime = connEndTime - connStartTime;
	  }catch(Throwable iex)
	  {
		  String msg = iex.getMessage();
		  if(msg != null && (msg.indexOf("Access denied") >= 0 || Constants.CONN_MSG_NORETRY.equals(msg)))
		  {
			  logger.log(Level.WARNING, "Failed  attempt to connect to "+scanData.dbinfo, iex);
			  return false;
		  }
	      connEndTime = System.currentTimeMillis();
	      connTime = connEndTime - connStartTime;
	  }
	  if(scanData.conn==null && connTime < this.frameworkContext.getConnectionTimeout())//try again, if last attempt is within connection timeout seconds
	  {
		  try
		  {
			  logger.info("Retry to connect to " + scanData.dbinfo+", last try time "+connTime +"(" + connStartTime+", " + connEndTime+")"
					  +", timeout " + this.frameworkContext.getConnectionTimeout());
			  scanData.conn = conns.checkoutConnection(scanData.dbinfo, cred);
		  }catch(Throwable iex)
		  {
			  logger.log(Level.WARNING, "Failed second attempt to connect to "+scanData.dbinfo, iex);
			  reportOffline(scanData.dbinfo);			  
		  }
		  
	  }else if(scanData.conn==null)
	  {
		  logger.warning("Failed connection to " + scanData.dbinfo+", but will not retry since last try used " + connTime +"ms");
		  reportOffline(scanData.dbinfo);			  
	  }
	  return scanData.conn!=null;
	  
  }
  private boolean scanHost(DBInstanceInfo dbinfo)
  {
    boolean status = false;
	
	ScanData scanData = new ScanData();
	scanData.dbinfo = dbinfo;
	
	try
	{
      if(!tryConnect(scanData))return false;
      
      scanData.snap_id = this.snap_id;
	  scanData.startTimestamp = System.currentTimeMillis();//use a common timestamp
	  scanData.stateSnap.setTimestamp(scanData.startTimestamp);
	  
	  
	  //Now actual scanning
	  String[] mgNames = this.frameworkContext.getMetricsDef().getGroupNames();
	  for(String mgName: mgNames)
	  {		
		  MetricsGroup mg = this.frameworkContext.getMetricsDef().getGroupByName(mgName);
		  if(!mg.isAuto())
		  { //skip groups requiring manual configuration for now 
			if(!this.frameworkContext.getMetricsDef().getUdmManager().isMetricsGroupSubscribed(
					dbinfo.getDbGroupName(), dbinfo.getHostName(), mg.getGroupName(), null))
				continue;
		  }
		  SNMPSetting snmpSetting = this.frameworkContext.getSnmpSettings()
				  .getHostSetting(dbinfo.getDbGroupName(), dbinfo.getHostName()); 
	  
		  String snmpEnabled = snmpSetting != null? snmpSetting.getEnabled():"yes";
		  boolean toScanSnmp = !"no".equalsIgnoreCase(snmpEnabled);
		  if("mysql_snmp_sda".equalsIgnoreCase(mg.getSql()))
		  {
			 if(toScanSnmp) scanSnmpDisk( scanData,  mg);
		  }else if("snmp_storage".equalsIgnoreCase(mg.getSql()))
		  {
			  if(toScanSnmp)  scanSnmpStorage( scanData,  mg);
		  }else if( "snmp_net_eth0".equalsIgnoreCase(mg.getSql()))
		  {
			  if(toScanSnmp)  scanSnmpNet( scanData,  mg);
		  }
		  else if( "mysql_snmp".equalsIgnoreCase(mg.getSql()))//hard wired
		  {
			  if(toScanSnmp) scanSnmpSys( scanData,  mg);
		  }	else if("mysql_snmp_mysqld".equalsIgnoreCase(mg.getSql()))//hard wired
		  {
			  if(toScanSnmp)  scanSnmpMysqld( scanData,  mg);
		  }else if("mysql_repl".equalsIgnoreCase(mg.getSql()))//replication
		  {
			  scanRepl( scanData,  mg);
		  }else if("mysql_innodb_mutex".equalsIgnoreCase(mg.getSql()))//replication
		  {
			  scanInnoDBMutex( scanData,  mg);
		  }
		  else //other predefined metrics
		  {
			  scanRegularPredefined( scanData,  mg) ;
		  }
	  }
	  //UDM
	  List<String> udms = this.frameworkContext.getMetricsDef().getUdmManager().getMetricsSubscriptions()
			  .getSubscribedUDMs(dbinfo.getDbGroupName(), dbinfo.getHostName());
	  if(udms!=null)
	  {
		  for(String s: udms)
		  {
			  UserDefinedMetrics udm = this.frameworkContext.getMetricsDef().getUdmManager().getUDMByName(s);
			  if (udm == null || !"SQL".equals(udm.getSource()))
				  continue;
			  scanUDM(scanData, udm);
		  }
	  }
	  
	  //TODO handle alerts
	  if(!scanData.statusUpdated &&
				(scanData.stateSnap.getConnections()>=0L||scanData.stateSnap.getThreads()>0))//at least we get something from scan
	  {
		  InstanceStates ist = this.frameworkContext.getInstanceStatesManager().getStates(dbinfo.getDbid());
		  if(ist!=null)
		  {
			 java.util.Date lastAlertTime = ist.getLastAlertTime();
			 java.util.Date lastAlertEndTime = ist.getLastAlertEndTime(); 
			 String lastAlertType = ist.getLastAlertType();
			 
			 ist.update(scanData.stateSnap, this.getAlertThreshold(dbinfo));
			  //TODO
			  List<AlertEntry> userDefinedAlerts = ist.checkAndRaiseUserAlerts(this.frameworkContext, scanData.dbinfo.getDbGroupName(), scanData.dbinfo.getHostName());
			  if(userDefinedAlerts != null && userDefinedAlerts.size()>0 && dbinfo.isAlertEnabled())
			  {
				  for(AlertEntry alert: userDefinedAlerts)
				  {
					  if(ist.reportUserAlert(new Date(alert.getTs()), alert.getAlertReason(), alert.getAlertValue()))
					  {	  
					    this.frameworkContext.getMetricDb().addNewAlert(Long.parseLong(sdf.format(alert.getTs())), dbinfo.getDbid(), alert.getAlertReason(),  alert.getAlertValue());
					    this.frameworkContext.addAlert(alert);
					    this.frameworkContext.emailAlert(alert);
					  }
				  }
			  }
			  
			  scanData.statusUpdated = true;
			  //if(lastAlertTime!=null &&( (lastAlertEndTime==null  && ist.getLastAlertEndTime()!=null)
			  //	||(ist.getLastAlertEndTime()==null && ist.getLastAlertTime()!=null && !ist.getLastAlertType().equalsIgnoreCase(lastAlertType))))
			  if((lastAlertTime!=null && lastAlertEndTime == null) //existing an old alert
				&& (ist.getLastAlertEndTime() != null //there is no more open alert
				   || !ist.getLastAlertTime().equals(lastAlertTime) //new alert
				)
			  )
			  {
				 // if(!scanData.replDown)
				  {
					  //mark all old alerts end, up to 
					  this.frameworkContext.getMetricDb().markAlertEnd(Long.parseLong(sdf.format(lastAlertTime.getTime())), dbinfo.getDbid(), Long.parseLong(sdf.format(scanData.stateSnap.getTimestamp())));
					  //mark anything one minute ago, too
					  //this.frameworkContext.getMetricDb().markAlertEnd(Long.parseLong(sdf.format(scanData.stateSnap.getTimestamp())) - 100, dbinfo.getDbid(), Long.parseLong(sdf.format(scanData.stateSnap.getTimestamp())));
					  //reset email/web notification
					  ist.resetNotification();
				  }
			  }else if(scanData.hasSnapAlert)
			  {
				  ist.updateAlert(false, new java.util.Date(scanData.startTimestamp), scanData.snapAlertType, scanData.snapAlertValue, false);
			  }
			  if(ist.getLastAlertTime()!=null && ist.getLastAlertEndTime()==null)
			  {
				  //if IO, and it is not current one, skip it unless certain minutes have passed
				  if((!"IO".equalsIgnoreCase(ist.getLastAlertType()) 
						  && !"SLOW".equalsIgnoreCase(ist.getLastAlertType())  
						  && !"REPLLAG".equalsIgnoreCase(ist.getLastAlertType())
						  && !"REPLDOWN".equalsIgnoreCase(ist.getLastAlertType())
						  && !"MAXCONNERR".equalsIgnoreCase(ist.getLastAlertType())
						  )
					||ist.getLastUpdateTime() == ist.getLastAlertTime().getTime()
					||ist.getLastUpdateTime() - ist.getLastReportTime()>= InstanceStates.REPETA_ALERT_DELAY)
				  {
					  if(!scanData.reportGenerated)
					  {
						  ist.setLastReportTime(System.currentTimeMillis());
						  recordAlertReport(scanData.conn, dbinfo, scanData.startTimestamp,ist.getLastAlertType(), ist.getLastAlertValue());
						  scanData.reportGenerated = true;
					  }
				  }
			  }
			  
		  }
		  
	  }
	}catch(Exception ex)
	{
	  logger.log(Level.WARNING, "exception: "+dbinfo, ex);
	}finally
	{
	  if(scanData.conn!=null){conns.checkinConnectionAndClose(scanData.conn);scanData.conn = null;}
	}
	logger.fine("Done scan for host ("+dbinfo+")");
	if(!scanData.statusUpdated &&
	(scanData.stateSnap.getSyscputime()>=0L||scanData.stateSnap.getThreads()>0))
		this.frameworkContext.getInstanceStatesManager().getStates(dbinfo.getDbid()).update(scanData.stateSnap, this.getAlertThreshold(dbinfo));
	//TODO update with mysql status 
	return status;
  }

  private void recordAlertReport(DBConnectionWrapper conn, DBInstanceInfo dbinfo, long timestamp, String alertType, String alertValue)
  {	  
	  if(!dbinfo.isAlertEnabled())
	  {
		  logger.info("Alert detected, but blacked out: "+dbinfo+", "+alertType+", "+alertValue);
		  return;
	  }
	  
	  this.frameworkContext.getMetricDb().addNewAlert(Long.parseLong(sdf.format(timestamp)), dbinfo.getDbid(), alertType,  alertValue);
	  if(this.frameworkContext.getInstanceStatesManager().getStates(dbinfo.getDbid()).canRunReport())
      {
	      AlertReportRunner arr = new AlertReportRunner(this.frameworkContext, dbinfo, timestamp, appUser );
	      arr.setConnection(conn);
	      arr.setAlertReason( alertType);
	      arr.setAlertValue(alertValue);
	      //move to start of AlertReportRunner
	      //this.frameworkContext.getAlerts().addAlert(new AlertEntry(timestamp, alertType, alertValue, dbinfo.getDbGroupName(), dbinfo.getHostName()));
	  
	      //new Thread(arr).start();//TODO need some control	  
	      arr.run();
      }
  }
  public void setBuffer(Map<String, Map<String, MetricsBuffer>> buffer) 
  {
    this.buffer = buffer;
  }
}
