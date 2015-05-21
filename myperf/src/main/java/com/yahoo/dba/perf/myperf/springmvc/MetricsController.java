/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;

/**
 * Retrieve metrics data from metricsDB
 * @author xrao
 *
 */
public class MetricsController extends MyPerfBaseController
{
  private static Logger logger = Logger.getLogger(MetricsController.class.getName());
 	
  /**
   * URL path pattern: /metrics/{cmd}/{dbgroup}/{dbhost}/[{metrics}/[{start_ts}/][{end_ts}/]]{random}.html
   * cmd:
   *   get: get one or more metrics for one dbhost. metrics name separated by comma
   * @param req
   * @return
   */
  private Map<String, String> parseURL(HttpServletRequest req)
  {
	  Map<String, String> reqParams = new HashMap<String, String>();
	  String path = req.getServletPath();
	  if(path!=null)
	  {
		  String[] paths = path.split("/");
		  if(paths.length>2)
		  {
			  reqParams.put(Constants.URL_PATH_CMD, paths[2]);
			  if(paths.length>3)
				  reqParams.put(Constants.URL_PATH_DBGROUP, paths[3]); 
			  if(paths.length>4)
				  reqParams.put(Constants.URL_PATH_DBHOST, paths[4]); 
			  if(paths.length>5)
				  reqParams.put(Constants.URL_PATH_METRICS, paths[5]); 
			  if(paths.length>6)
				  reqParams.put(Constants.URL_PATH_START_TS, paths[6]); 
			  if(paths.length>7)
				  reqParams.put(Constants.URL_PATH_END_TS, paths[7]); 
		  }
	  }
	  return reqParams;
  }
  
  /**
   * Retrieve DBInstanceInfo from path. Return null if none found
   * @param pathParameters
   * @return
   */
  private DBInstanceInfo retrieveDBInfoFromPath(Map<String, String> pathParameters)
  {
	DBInstanceInfo dbinfo = null;
	String dbgroup = pathParameters.get(Constants.URL_PATH_DBGROUP);
	if(dbgroup==null || dbgroup.isEmpty())
		return null;
	
	String dbhost = pathParameters.get(Constants.URL_PATH_DBHOST);
	if(dbhost==null || dbhost.isEmpty())
		return null;
	
	try
	{
		dbinfo = this.frameworkContext.getDbInfoManager().findDB(dbgroup, dbhost);
	}catch(Exception ex){}

	return dbinfo;
  }
  

  private void prefixMetricGroupName(String mg, ResultList rs)
  {
	  if(mg==null||"globalstatus".equals(mg)||mg.isEmpty() ||rs==null)return;
	  for(ColumnInfo col: rs.getColumnDescriptor().getColumns())
	  {
		  String name = col.getName();
		  if("TS".equals(name) || "SNAP_ID".equals(name))continue;
		  if(!name.startsWith(mg+"."))
		  {
			  col.setName(mg+"."+name);
		  }
	  }
  }
  private ModelAndView getMetrics( Map<String, String> pathParameters, HttpServletRequest req,
			HttpServletResponse resp) throws Exception 
  {
	  int status = Constants.STATUS_OK;
	  String message = "OK";
	  ResultList rlist = null;
	  boolean useKeyColumn = false;
	  String[] metricsList = null;//used when useKeyColumn is true
	  DBInstanceInfo dbinfo =  retrieveDBInfoFromPath(pathParameters);
	  do
	  {
		if(dbinfo==null)
		{
			status = Constants.STATUS_BAD;	
			message = "There is no record for DB ("+pathParameters.get(Constants.URL_PATH_DBGROUP)+", "+pathParameters.get(Constants.URL_PATH_DBHOST)+"), please add it to the system first.";
			break;
		}
		String m = pathParameters.get(Constants.URL_PATH_METRICS);
		if(m==null)
		{
			status = Constants.STATUS_BAD;
			message = "No valid meteic name is provided.";
			break;
		}
		String startTime = pathParameters.get(Constants.URL_PATH_START_TS);
		String endTime = pathParameters.get(Constants.URL_PATH_END_TS);
		String[] ms = m.split(",");
		String[] dateRange = MyPerfBaseController.getDateRange(startTime, endTime);
		HashMap<String, HashMap<String, ArrayList<String>>> metricsByGroup = new HashMap<String, HashMap<String, ArrayList<String>>>();
		for(String s: ms)
		{   //all name should be in the form of {group}.{subgrp}.{metric name}
			String[] cmp = s.split("\\."); //retrieve name component
			if (cmp.length != 3) continue; //invalid name, ignore
			if (cmp[2] == null || cmp[2].isEmpty())continue;//invalid name, ignore
			String mg = cmp[0];
			String subGrpName = cmp[1];
			String metricName = cmp[2];
			if(mg==null||mg.isEmpty())mg = "STATUS";
			if("UDM".equals(mg))//need special processing for UDM
				mg = "UDM_"+ subGrpName;
			if(!metricsByGroup.containsKey(mg))
				metricsByGroup.put(mg, new HashMap<String, ArrayList<String>>());
			HashMap<String, ArrayList<String>> subGrp = metricsByGroup.get(mg);
			if (!subGrp.containsKey(subGrpName))
				subGrp.put(subGrpName, new ArrayList<String>());
			subGrp.get(subGrpName).add(metricName);
		}
		
		for(String mg: metricsByGroup.keySet())
		{
			//check if metrics has key column
			MetricsGroup mGroup = null;
			if(mg.startsWith("UDM_"))
			{
				try
				{
					mGroup = this.frameworkContext.getMetricsDef().getUdmManager()
							.getUDMByName(mg.substring(mg.indexOf('_') + 1)).getMetricsGroup();
				}catch(Exception ex){}
			}else
				mGroup = this.frameworkContext.getMetricsDef().getGroupByName(mg);
			if(mGroup == null)
			{
				logger.info("Failed to find metris definition for " + mg);
				continue;
			}
			
			if(metricsByGroup.size() == 1 && mGroup != null && mGroup.getKeyColumn() != null && !mGroup.getKeyColumn().isEmpty())
			{
				//if we have only one metric group and it has additional key
				//Now we need retrieve this group with additional key
				metricsList = metricsByGroup.get(mg).get("_").toArray(new String[0]);
				if(metricsList.length<=1)//we only want to handle one metric now.
				{
  				  rlist = retrieveMetricsWithKey(metricsList, mg, null, dbinfo, dateRange[0], dateRange[1], false);
				  useKeyColumn = true;
				  break;
				}
			}
			HashMap<String, ArrayList<String>> subGroups = metricsByGroup.get(mg);
			for(String subGrpName: subGroups.keySet())
			{
				ResultList tmpList = null;
				if(mGroup != null && mGroup.isStoreInCommonTable())
				{
					tmpList = retrieveGenericMetrics(subGroups.get(subGrpName).toArray(new String[0]), mGroup, dbinfo, dateRange[0],dateRange[1]);
				}
				else if(mGroup != null && mGroup.getKeyColumn() != null && !mGroup.getKeyColumn().isEmpty())
					tmpList = retrieveMetricsWithKey(subGroups.get(subGrpName).toArray(new String[0]), mg, subGrpName, dbinfo, dateRange[0], dateRange[1], true);
				else 
					tmpList = retrieveMetrics(subGroups.get(subGrpName).toArray(new String[0]), mg, subGrpName, dbinfo, dateRange[0], dateRange[1]);
				//if(mGroup == null || !mGroup.isStoreInCommonTable())
				{
					String prefix = mg + ".";
					if(subGrpName != null)prefix += subGrpName;
					if(mg.startsWith("UDM_"))
						prefix = "UDM." + subGrpName;
					prefixMetricGroupName(prefix, tmpList);
				}
				if(rlist==null && tmpList != null)
				{
					logger.info("Return list size: "+tmpList.getRows().size());
					rlist = tmpList;
				}
				else if(tmpList!=null)rlist = this.mergeResultList(rlist, tmpList);
				if(rlist != null)
					logger.info("End list size after merge: "+ rlist.getRows().size());
			}
		}
		//when we reach here, at least we have valid query and can connect to db	
		WebAppUtil.storeLastDbInfoRequest(dbinfo.getDbGroupName(),dbinfo.getHostName(), req);
		break;
	  }while(false);
	  ModelAndView mv = null;
	  
	  mv=new ModelAndView(this.jsonView);
	  if(status == Constants.STATUS_BAD)
	  {
		  if(req.getParameter("callback")!=null&&req.getParameter("callback").trim().length()>0)
		    mv.addObject("callback", req.getParameter("callback"));//YUI datasource binding
		  mv.addObject("json_result", ResultListUtil.toJSONString(null, null, status, message));
	  }else
	  {

	    if(rlist !=null && rlist.getCustomObjects()!=null)
	    {
	    	  HashMap<String, String> metricsMapping = new HashMap<String, String>(rlist.getCustomObjects().size());
	    	  int idx = 0;
		      for(CustomResultObject obj: rlist.getCustomObjects().values())
			  {
			  	if(obj instanceof MetricsResultObject)
			  	{
			  		MetricsResultObject mco = MetricsResultObject.class.cast(obj);
			  		for(Metric m: mco.getMetrics())
			  		{
			  		  if(!useKeyColumn)
			  			  m.setShortName("M_"+idx);
			  		  else
			  			  m.setShortName(m.getName().substring(m.getName().lastIndexOf(".")+1));
		    		  metricsMapping.put(m.getName(), m.getShortName());
		    		  idx++;
			  		}
			  	}
			  }

	    	  //fix column def
		      logger.info("short name mapping: "+metricsMapping);
	    	  for(ColumnInfo col: rlist.getColumnDescriptor().getColumns())
	    	  {
	    		  String shortName = metricsMapping.get(col.getName());
	    		  if(shortName != null)
	    			  col.setName(shortName);
	    	  }
	    }
		mv = new ModelAndView(this.jsonView);
		if(req.getParameter("callback")!=null&&req.getParameter("callback").trim().length()>0)
		  mv.addObject("callback", req.getParameter("callback"));//YUI datasource binding
		QueryParameters qps = new QueryParameters();
		qps.setGroup(dbinfo.getDbGroupName());
		qps.setHost(dbinfo.getHostName());
		mv.addObject("json_result", useKeyColumn?ResultListUtil.toMetricsJSONStringWithMultiRowsKeys(rlist, "KEY_COLUMN", new String[]{"SNAP_ID", "TS"},  metricsList[0], qps, status, message)
				:ResultListUtil.toJSONString(rlist, qps , status, message));
	  }
	  return mv;
  }

  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
			HttpServletResponse resp) throws Exception 
  {
	String cmd = null;//enable and disable metrics, otherwise retrieve/get

	Map<String, String> pathParameters = this.parseURL(req);
	if(pathParameters.containsKey(Constants.URL_PATH_CMD))
	{
		cmd = pathParameters.get(Constants.URL_PATH_CMD);
		logger.info("Receive command: "+cmd);
		if("get".equalsIgnoreCase(cmd))
			return getMetrics(pathParameters, req, resp);
	}
	
	//if we reach here, something wrong
	return this.respondFailure("Invalid command. Supported URL: /metrics/{enable, disable,get}/{dbgroup}/{dbhost}/{metrics_separated_by_comma}/[{start_time}]/[{end_time}]/{random_number}.html", req);	
  }

  
  /**
   * Retrieve metrics with key column
   * @param metrics
   * @param metricgroup
   * @param subGroup
   * @param dbinfo
   * @param startDate
   * @param endDate
   * @param agg if true, use aggregation
   * @return
   */
  private ResultList retrieveMetricsWithKey(String[] metrics, String metricgroup, String subGroup, DBInstanceInfo dbinfo, String startDate, String endDate, boolean agg)
  {
	  ResultList rList = null;
	  try
	  {
    	  String tblName = subGroup;
    	  if (tblName == null || tblName.isEmpty() || tblName.equals("_"))tblName = metricgroup;
    	  if(metricgroup.startsWith("UDM_")) tblName = metricgroup;
    	  else tblName = "MYSQL_" + tblName;
    		  
    	  List<Metric> mtrs = this.frameworkContext.getMetricsDef().getMetrics(metricgroup, subGroup, metrics);
    	  long sqlStartTime = System.currentTimeMillis();
    	  rList = this.frameworkContext.getMetricDb().retrieveMetrics(tblName.toUpperCase(), mtrs.toArray(new Metric[0]), true,
    		  dbinfo.getDbid(), Long.parseLong(startDate), Long.parseLong(endDate), agg);
    	  logger.info("Time used to retrieve metrics from MYSQL_"+tblName+": "+(System.currentTimeMillis() - sqlStartTime)+"ms");
    	  //add back metrics definition
    	  MetricsResultObject mrs = new  MetricsResultObject();
    	  String prefix = metricgroup+".";
    	  if(metricgroup.startsWith("UDM_"))
    	      prefix = "UDM." + subGroup + ".";
    	  else if (subGroup == null || subGroup.isEmpty() )
    		  prefix += "_.";
    	  else 
    		  prefix += subGroup+".";
    	  for (Metric m: mtrs)
    		  m.setName(prefix+m.getName());//prefix name with group/sub group name
    	  mrs.setMetrics(mtrs);
    	  rList.addCustomeObject(mrs);
	}catch(Throwable th)//don't want to throw exception back to the user
	{
		logger.log(Level.WARNING, "Failed to retrieve metrics", th);
	}
	  
	return rList;
  }

  private ResultList retrieveMetrics(String[] metrics, String metricgroup, String subGroup, DBInstanceInfo dbinfo, String startDate, String endDate)
  {
	  ResultList rList = null;
	  try
	  {
      {
    	  String tblName = subGroup;
    	  if (tblName == null || tblName.isEmpty() || tblName.equals("_"))tblName = metricgroup;
    	  if(metricgroup.startsWith("UDM_")) tblName = metricgroup;
    	  else tblName = "MYSQL_" + tblName;    		
    	  long sqlStartTime = System.currentTimeMillis();
    	  rList = this.frameworkContext.getMetricDb().retrieveMetrics(tblName.toUpperCase(), metrics, false,
    		  dbinfo.getDbid(), Long.parseLong(startDate), Long.parseLong(endDate));
    	  logger.info("Time used to retrieve metrics from "+tblName+": "+(System.currentTimeMillis() - sqlStartTime)+"ms");
    	  //add back metrics definition
    	  MetricsResultObject mrs = new  MetricsResultObject();
    	  List<Metric> mtrs = this.frameworkContext.getMetricsDef().getMetrics(metricgroup, subGroup, metrics);
    	  String prefix = metricgroup+".";
    	  if(metricgroup.startsWith("UDM_"))
    	      prefix = "UDM." + subGroup+".";
    	  else if (subGroup == null || subGroup.isEmpty() )
    		  prefix += "_.";
    	  else 
    		  prefix += subGroup + ".";
    	  for (Metric m: mtrs)
    		  m.setName(prefix+m.getName());//prefix name with group/sub group name
    	  mrs.setMetrics(mtrs);
    	  rList.addCustomeObject(mrs);
      }
	}catch(Throwable th)//don't want to throw exception back to the user
	{
		logger.log(Level.WARNING, "Failed to retrieve metrics", th);
	}
	  
	return rList;
  }

  private ResultList retrieveGenericMetrics(String[] metrics, MetricsGroup mg, DBInstanceInfo dbinfo, String startDate, String endDate)
  {
	  ResultList rList = null;
	  for(String mname: metrics)
	  {
		  try
		  {
			  String metricFullName = mg.getMetricFullName(mname);
			  if(metricFullName == null)continue;
			  ResultList tmpList = this.frameworkContext.getMetricDb().retrieveUDMMetrics(metricFullName,  
					  dbinfo.getDbid(), Long.parseLong(startDate), Long.parseLong(endDate));
			  if(tmpList == null )continue;
			  Metric m = mg.getMetricByName(mname).copy();
			  m.setName(metricFullName);
			  List<Metric> mtrs = new java.util.ArrayList<Metric>(1);
			  mtrs.add(m);
			  MetricsResultObject mrs = new  MetricsResultObject();
			  mrs.setMetrics(mtrs);
			  tmpList.addCustomeObject(mrs);
			  if(rList==null)
				  rList = tmpList;
			  else rList = this.mergeResultList(rList, tmpList);
		  }catch(Exception ex)
		  {
			  
		  }
	  }
	return rList;
  }

  private ResultList mergeResultList(ResultList rList1, ResultList rList2)
  {
	  
	  ResultList rList = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  int idx = 0;
	  for(ColumnInfo c: rList1.getColumnDescriptor().getColumns())
	  {
		  desc.addColumn(c.getName(), c.isNumberType(), idx++);
	  }

	  for(ColumnInfo c: rList2.getColumnDescriptor().getColumns())
	  {
		  if(!"TS".equalsIgnoreCase(c.getName()) && !"SNAP_ID".equalsIgnoreCase(c.getName()))
 		    desc.addColumn(c.getName(), c.isNumberType(), idx++);
	  }
	  rList.setColumnDescriptor(desc);
	  
	  //we have use the same timestamp and data are sorted, ignore if we have any missing data in one list
	  int idx1 = 0;
	  int idx2 = 0;
	  int n1 = rList1.getRows().size();
	  int n2 = rList2.getRows().size();
	  while(idx1<n1 && idx2<n2)
	  {
		  ResultRow row1 = rList1.getRows().get(idx1);
		  ResultRow row2 = rList2.getRows().get(idx2);
		  if(row1.getColumns().get(0).equals(row2.getColumns().get(0)))//same timestamp, accept it
		  {
			  ResultRow row = new ResultRow();
			  row.setColumnDescriptor(desc);
			  for(int i=0;i<row1.getColumns().size();i++)
				  row.addColumn(row1.getColumns().get(i));
			  for(int i=2;i<row2.getColumns().size();i++)//skip TS and SNAP_ID
				  row.addColumn(row2.getColumns().get(i));
			  rList.addRow(row);
			  idx1++;
			  idx2++;
		  }else if(Long.parseLong(row1.getColumns().get(0)) < Long.parseLong(row2.getColumns().get(0)))
			  idx1++;//advance the first rList
		  else idx2++;//advance the second rList
	  }
	  MetricsResultObject mrs = new  MetricsResultObject();
	  if(rList1.getCustomObjects()!=null)
	  for(CustomResultObject obj: rList1.getCustomObjects().values())
	  {
		if(obj instanceof MetricsResultObject)
		{
			MetricsResultObject mco = MetricsResultObject.class.cast(obj);
			for(Metric m: mco.getMetrics())
			{
				mrs.addMetric(m);
			}
		}
	  }
	  if(rList2.getCustomObjects()!=null)
	  for(CustomResultObject obj: rList2.getCustomObjects().values())
	  {
		if(obj instanceof MetricsResultObject)
		{
			MetricsResultObject mco = MetricsResultObject.class.cast(obj);
			for(Metric m: mco.getMetrics())
			{
				mrs.addMetric(m);
			}
		}
	  }
	  rList.addCustomeObject(mrs);
	  return rList;
  } 
}