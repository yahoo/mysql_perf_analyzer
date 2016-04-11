/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.AppUser;
import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.DBGroupInfo;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.InstanceStates;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultListUtil;
import com.yahoo.dba.perf.myperf.common.ResultRow;
import com.yahoo.dba.perf.myperf.common.StateSnapshot;

public class StatusController extends MyPerfBaseController
{
	  @Override
	  protected ModelAndView handleRequestImpl(HttpServletRequest req,
					HttpServletResponse resp) throws Exception 
	  {
		
		AppUser appUser = retrieveAppUser(req);
		HashSet<String> filteredGroups = new HashSet<String>();
		boolean useFilter = false;
		if(appUser != null && appUser.isRestrictedUser())
		{
			useFilter = true;
			Set<String> mydbs = this.frameworkContext.getDbInfoManager()
					.getMyDatabases(appUser.getName(), appUser.isRestrictedUser()).getMyDbList();
			if(mydbs != null)
			{
				for(String s: mydbs)
					filteredGroups.add(s);
			}
		}
	    String dbgroup = req.getParameter("group");
		if(dbgroup==null||dbgroup.trim().length()==0)dbgroup = "all";
        String dbhost = req.getParameter("host");
        
        java.util.Date now = new java.util.Date();
        //the first scan yet to complete
        long oneinterval = (this.frameworkContext.getMyperfConfig().getScannerIntervalSeconds()+1)*1000;
        boolean withinOneScan = (now.getTime() - this.frameworkContext.getStartTime().getTime()) <= oneinterval;
        long twointerval = (2*this.frameworkContext.getMyperfConfig().getScannerIntervalSeconds()+1)*1000;
        
        ResultList rList = new ResultList();
        
        //if("all".equalsIgnoreCase(dbgroup))
        if(dbhost==null || dbhost.isEmpty())//we should have id either as all or a dbgroup
        {
          ColumnDescriptor desc = new ColumnDescriptor();
          desc.addColumn("DBGROUP", false, 1);
          desc.addColumn("HOST", false, 2);
          desc.addColumn("QUERIES /SEC", true, 3);
          desc.addColumn("SYS CPU%", true, 4);
          desc.addColumn("USER CPU%", true, 5);
          desc.addColumn("IOWAIT%", true, 6);
          desc.addColumn("LOAD AVG", true, 7);
          desc.addColumn("REPL LAG", true, 8);
          desc.addColumn("FREE MEM (MB)", true, 9);
          desc.addColumn("SLOW QUERY /MIN", true, 10);
          desc.addColumn("THREADS RUNNING", true, 11);
          desc.addColumn("THREADS", true, 12);
          desc.addColumn("CONNECTIONS /SEC", true, 13);
          desc.addColumn("ABORTED CC /SEC", true, 14); 
          desc.addColumn("DEADLOCKS", false, 15);
          desc.addColumn("STATUS", false, 16);
          desc.addColumn("LAST CHECK TIME", false, 17);
          desc.addColumn("LAST ALERT", false, 18);
          desc.addColumn("SCAN TIME", true, 19);
                   
          rList.setColumnDescriptor(desc);
          java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    	        
    	  java.text.DecimalFormat df = new java.text.DecimalFormat("#.###");
          for(Map.Entry<String, DBGroupInfo> e: this.frameworkContext.getDbInfoManager().getClusters().entrySet())
          {
            for (DBInstanceInfo dbinfo: e.getValue().getInstances())
            {
            	
              String dbgroupName = dbinfo.getDbGroupName();
              String dbHostName = dbinfo.getHostName();
              if(!"all".equals(dbgroup) && !dbgroup.equalsIgnoreCase(dbgroupName))continue;
              if(useFilter && !filteredGroups.contains(dbgroupName)) continue;
              InstanceStates stateSnaps = this.frameworkContext.getInstanceStatesManager().getStates(dbinfo.getDbid());
              if(stateSnaps==null)continue;
              //java.util.Date lastConnectTime = this.frameworkContext.getDbInfoManager().getLastAccessDate(dbgroupName, dbHostName);
              java.util.Date lastConnectTime = stateSnaps.getLastAccessTime();
              String dbstatus = "Green";
              if (lastConnectTime == null && withinOneScan)
                dbstatus = "Unknown";
              else if (lastConnectTime == null)
                dbstatus = "Red";
              else if( now.getTime() - lastConnectTime.getTime() > twointerval)
                dbstatus = "Red";
              else if(now.getTime() - lastConnectTime.getTime() > oneinterval)
                dbstatus = "Yellow";
            
              ResultRow row = new ResultRow();
              row.setColumnDescriptor(desc);
              List<String> vals = new ArrayList<String>();
              vals.add(dbgroupName);
              vals.add(dbHostName);
              
              StateSnapshot[] snaps = stateSnaps.copySnapshots();
              boolean isSnapValid = snaps[0].getTimestamp()>=0L && snaps[1].getTimestamp()>snaps[0].getTimestamp();
              long interval = snaps[1].getTimestamp() - snaps[0].getTimestamp();
              if(isSnapValid)
              {
            	  //sys CPU
            	  double syscpu = 0.0f;
            	  double usercpu = 0.0f;
            	  double iowaits = 0.0f;
            	  double loadAvg = 0.0f;
            	  int activeThreads = 0;
            	  if(snaps[0].getQueries()>=0L && snaps[1].getQueries()>=0L)
            	  {
            		  //double val = ((double)(snaps[1].getQueries() - snaps[0].getQueries())*1000)/(double)interval;
            		  double val = diffAvgNoNegative(snaps[1].getQueries(), snaps[0].getQueries(), 
            				  snaps[1].getTimestamp(), snaps[0].getTimestamp(), snaps[1].getUptime(), true, 1000);
            		  vals.add(df.format(val));
            	  }else
            	  {
            		  vals.add("0.00"); 
            	  }

            	  if(snaps[0].getSyscputime()>=0L && snaps[1].getSyscputime()>=0L)
            	  {
            		  if(snaps[0].getSyscputime() == snaps[1].getSyscputime()) 
            		  {
            			  syscpu = 0.0; vals.add("0.00");
            		  }else
            		  {
            			  double val = ((double)(snaps[1].getSyscputime() - snaps[0].getSyscputime())*100)/(double)(snaps[1].getTotalcputime() - snaps[0].getTotalcputime());
            			  vals.add(df.format(val));
            			  syscpu = val;
            		  }
            	  }else
            	  {
            		  vals.add("0.00");
            	  }
            	  if(snaps[0].getUsercputime()>=0L && snaps[1].getUsercputime()>=0L)
            	  {
            		  if(snaps[0].getUsercputime() == snaps[1].getUsercputime() )
            		  {
            			  usercpu = 0.0; vals.add("0.00");
            			  
            		  }else
            		  {
            			  double val = ((double)(snaps[1].getUsercputime() - snaps[0].getUsercputime())*100)/(double)(snaps[1].getTotalcputime() - snaps[0].getTotalcputime());
            			  vals.add(df.format(val));
            			  usercpu = val;
            		  }
            	  }else
            	  {
            		  vals.add("0.00");
            	  }
            	  if(snaps[0].getIotime()>=0L && snaps[1].getIotime()>=0L)
            	  {
            		  if(snaps[0].getIotime() == snaps[1].getIotime())
            		  {
            			  iowaits = 0.0; vals.add("0.00");
            		  }else
            		  {
            			  double val = ((double)(snaps[1].getIotime() - snaps[0].getIotime())*100)/(double)(snaps[1].getTotalcputime() - snaps[0].getTotalcputime());
            			  vals.add(df.format(val));
            			  iowaits = val;
            		  }
            	  }else
            	  {
            		  vals.add("0.00");
            	  }
            	  if(snaps[1].getLoadAverage()>=0.0f)
            	  {
            		  vals.add(df.format(snaps[1].getLoadAverage()));
            		  loadAvg = snaps[1].getLoadAverage();
            	  }else
            		  vals.add("0.00");
            	
            	  vals.add(String.valueOf(snaps[1].getReplLag()));
            	  if(snaps[1].getAvailableMem()>=0L)
            	  {
            		  long val = snaps[1].getAvailableMem()/1024;
            		  vals.add(String.valueOf(val));
            	  }else
            		  vals.add("0.00");
            	  
            	  if(snaps[0].getSlowQueryCount()>=0L && snaps[1].getSlowQueryCount()>=0L)
            	  {
            		  double val = diffAvgNoNegative(snaps[1].getSlowQueryCount(), snaps[0].getSlowQueryCount(), 
            				  snaps[1].getTimestamp(), snaps[0].getTimestamp(), snaps[1].getUptime(), true, 60000);
            		  vals.add(df.format(val));            		  
            	  }else
            		  vals.add("0.00");
            	  
            	  vals.add(String.valueOf(snaps[1].getActiveThreads()));
            	  
            	  activeThreads = snaps[1].getActiveThreads();
            	  vals.add(String.valueOf(snaps[1].getThreads()));
            	  
            	  if(snaps[0].getConnections()>=0L && snaps[1].getConnections()>=0L)
            	  {
            		  //double val = ((double)(snaps[1].getConnections() - snaps[0].getConnections())*1000)/(double)interval;
            		  double val = diffAvgNoNegative(snaps[1].getConnections(), snaps[0].getConnections(), 
            				  snaps[1].getTimestamp(), snaps[0].getTimestamp(), snaps[1].getUptime(), true, 1000);
            		  vals.add(df.format(val));
            	  }else
            	  {
            		  vals.add("0.00");
            	  }
            	  
            	  if(snaps[0].getAbortedConnectsClients()>=0L && snaps[1].getAbortedConnectsClients()>=0L)
            	  {
            		  //double val = ((double)(snaps[1].getAbortedConnectsClients() - snaps[0].getAbortedConnectsClients())*1000)/(double)interval;
            		  double val = diffAvgNoNegative(snaps[1].getAbortedConnectsClients(), snaps[0].getAbortedConnectsClients(), 
            				  snaps[1].getTimestamp(), snaps[0].getTimestamp(), snaps[1].getUptime(), true, 1000);
            		  vals.add(df.format(val));
            	  }else
            	  {
            		  vals.add("0.00");
            	  }
            	  if(snaps[0].getDeadlocks() >= 0L && snaps[1].getDeadlocks() >= 0L)
            	  {
            		  double val = diffAvgNoNegative(snaps[1].getDeadlocks(), snaps[0].getDeadlocks(), 
            				  snaps[1].getTimestamp(), snaps[0].getTimestamp(), snaps[1].getUptime(), false, 1);
            		  vals.add((long)val + "("+snaps[0].getDeadlocks()+")");
            	  }else
            	  {
            		  vals.add(String.valueOf(snaps[1].getDeadlocks()));
            	  }
            	  //hardcoded threshold now
            	  if(iowaits>this.frameworkContext.getAlertSettings().getAlertThreshold(dbinfo, "IO") 
            			  || (syscpu+usercpu>this.frameworkContext.getAlertSettings().getAlertThreshold(dbinfo, "CPU")) 
            			  ||loadAvg>this.frameworkContext.getAlertSettings().getAlertThreshold(dbinfo, "LOADAVG")
            			  ||activeThreads>this.frameworkContext.getAlertSettings().getAlertThreshold(dbinfo, "THREAD"))
            		  if(!"Red".equals(dbstatus))dbstatus = "Yellow";
              }else
              {
            	  vals.add("0.00");
            	  vals.add("0.00");
            	  vals.add("0.00");
            	  vals.add("0.00");
            	  
            	  if(snaps[1].getTimestamp()>0L)
            		  vals.add(String.valueOf(snaps[1].getReplLag()));
            	  else vals.add("0");
            	  
            	  vals.add("0.00");
            	  vals.add("0.00");
            	  vals.add("0");
            	  vals.add("0");
            	  vals.add("0.00");
            	  vals.add("0.00");
            	  vals.add("0.00");
            	  vals.add("0");
              }
              vals.add(dbstatus);
              if(lastConnectTime!=null)
                  vals.add(sdf.format(lastConnectTime));
                else 
                  vals.add("");
              Date altDt = stateSnaps.getLastAlertTime();
              if(altDt!=null)
              {
            	  String val = stateSnaps.getLastAlertValue();
            	  if(val==null)val = "";
            	  //else if(val.indexOf('.')>0 && val.length()>=val.indexOf('.')+4)
            		//  val = val.substring(0, val.indexOf('.')+4);
            	  String end_dt = stateSnaps.getLastAlertEndTime()!=null?sdf.format(stateSnaps.getLastAlertEndTime()):"";
            	  vals.add(sdf.format(altDt)+"-"+end_dt+"/"+stateSnaps.getLastAlertType()+"/"+val);
              }else vals.add("");
              vals.add(String.valueOf(stateSnaps.getLastScanTime()));
              row.setColumns(vals);
              rList.addRow(row);
            }
          }
        }
        else
        {
          Date d1 = null;
          Date d2 = null;	
    	  Calendar c = Calendar.getInstance();
    	  c.add(Calendar.DATE, -7); //7 days
    	  d1 = c.getTime();			
    	  d2 = new Date();
    	  java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
    	  sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
    	  String startDate = sdf2.format(d1);
    	  String endDate = sdf2.format(d2);
  	      try
  	      {
  	    	//TODO validate DB
  	        rList = this.frameworkContext.getAutoScanner().getMetricDb().retrieveMetricsStatus("mysql_globalstatus".toUpperCase(),
  	    		  this.frameworkContext.getDbInfoManager().findDB(dbgroup, dbhost).getDbid(), Long.parseLong(startDate), Long.parseLong(endDate));	
  	        HashMap<String, Integer> connectCounter = new HashMap<String, Integer>(24*8);
 	        HashMap<String, Integer> slowCounter = new HashMap<String, Integer>(24*8);
 	        	        if(rList!=null && rList.getRows().size()>0)
  	        {
  	          for(ResultRow row: rList.getRows())
  	          {
  	        	String ts = row.getColumns().get(1);
  	        	String h = ts.substring(0, ts.length());
  	        	if(connectCounter.containsKey(h))
  	        	  connectCounter.put(h, connectCounter.get(h)+1);
  	        	else
  	        	  connectCounter.put(h, 1);
  	        	try
  	        	{
  	        	  if(slowCounter.containsKey(h))
  	        		slowCounter.put(h, connectCounter.get(h)+Integer.parseInt(row.getColumns().get(3)));
    	          else
    	        	slowCounter.put(h, Integer.parseInt(row.getColumns().get(3)));
  	        	}catch(Exception ex){}
  	          }
  	        }
  	        
  	      }catch(Exception ex)
  	      {
  	        //logger.log(Level.WARNING, "Failed to retrieve metrics data", ex);
  	      }

        }
		ModelAndView mv = new ModelAndView(jsonView);
		String msg = "OK";
		
		mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, 0, msg));
		return mv;
	  }
	  
	  /**
	   * diff between v1 and v2. If diff is negative and uptime is positive, use v1. Otherwise 0.
	   * If avg is set, divide by (t1 - t2), or uptime*1000 for case v1<v2 , then multiply by adjustment. 
	   * @param v1
	   * @param v2
	   * @param ts1
	   * @param ts2
	   * @param uptime
	   * @param avg
	   * @param adjustment
	   * @return
	   */
	  private double diffAvgNoNegative(long v1, long v2, long ts1, long ts2, long uptime, boolean avg, int adjustment)
	  {
		  long diff = v1 - v2;
		  long interval = ts1 - ts2;
		  if(diff < 0)
		  {
		    diff = uptime>0?v1:0;
		    interval = uptime>0?uptime*1000:0;
		  }
		  if(!avg || diff == 0)return diff;
		  
		  return ((double)(diff * adjustment))/(double)interval;
	  }
	}
