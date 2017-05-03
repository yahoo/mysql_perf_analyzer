/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ProcessListSummary implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger  = Logger.getLogger(ProcessListSummary.class.getName());

	private Map<String, Integer> userSummary = new HashMap<String, Integer>();//group by user
	private Map<String, Integer> hostSummary = new HashMap<String, Integer>();//group by host
	private Map<String, Integer> commandSummary = new HashMap<String, Integer>();//group by command, including idle
	private Map<String, Integer> commandHostSummary = new HashMap<String, Integer>();//group by command and host, including idle
	private Map<String, Integer> stateSummary = new HashMap<String, Integer>();//active state

	private List<ProcessListEntry> processList;
	
	private Pattern pt = Pattern.compile("LIMIT\\s+\\d+", Pattern.CASE_INSENSITIVE);
	private Pattern pt2 = Pattern.compile("LIMIT\\s+\\d+\\s*,\\s*\\d+", Pattern.CASE_INSENSITIVE);
	
	private Map<String, ProcessListEntryAggregate> queries = new HashMap<String, ProcessListEntryAggregate>();
	private Map<String, ProcessListEntryAggregate> queriesWithLimitStripped = new HashMap<String, ProcessListEntryAggregate>();//pagenation
	
	private Map<String, ProcessListEntryAggregate> normalizedQueries = new HashMap<String, ProcessListEntryAggregate>();
	
	private String innodbStatus;
	private int totalAccumultaedTime = 0;
	private ResultList lockList;
	private ResultList trxList;
	private ResultList clientList;
	
	private List<ProcessListEntryProcessor> appProcessorList = new ArrayList<ProcessListEntryProcessor>();

	private long reportTimestamp;

	public class KeyValIntPair implements Comparable<KeyValIntPair>
	{
		public String name;
		public int value;
		public KeyValIntPair(String name, int value)
		{
			this.name = name;
			this.value = value;
		}
		@Override
		public int compareTo(KeyValIntPair o) {
			if(o==null)return -1;
			return o.value - value;//reverse order
		}
		
	}
	
	public void addAppProcessor(ProcessListEntryProcessor pr)
	{
		this.appProcessorList.add(pr);
	}
	
	public ProcessListSummary()
	{
	}
	//TODO 
	/**
	 * It should be invoked after processlist set
	 */
	public void summarize()
	{
		if(this.processList==null)return ;//something wrong
		for(ProcessListEntry e: this.processList)
		{
			if("system user".equalsIgnoreCase(e.getUser()))continue;//ignore replication
			//update user
			if(!this.userSummary.containsKey(e.getUser()))
				this.userSummary.put(e.getUser(), 1);
			else
				this.userSummary.put(e.getUser(), this.userSummary.get(e.getUser())+1); 
			
			//update host
			if(!this.hostSummary.containsKey(e.getHost()))
				this.hostSummary.put(e.getHost(), 1);
			else
				this.hostSummary.put(e.getHost(), this.hostSummary.get(e.getHost())+1); 

			//state 
			if(e.getState()!=null)//Sleep command has no state
			{
				if(!this.stateSummary.containsKey(e.getState()))
					this.stateSummary.put(e.getState(), 1);
				else
					this.stateSummary.put(e.getState(), this.stateSummary.get(e.getState())+1); 
			}

			//command
			if(!this.commandSummary.containsKey(e.getCommand()))
				this.commandSummary.put(e.getCommand(), 1);
			else
				this.commandSummary.put(e.getCommand(), this.commandSummary.get(e.getCommand())+1); 

			//command host: TODO
			if(!this.commandHostSummary.containsKey(e.getHost()+"-"+e.getCommand()))
				this.commandHostSummary.put(e.getHost()+"-"+e.getCommand(), 1);
			else
				this.commandHostSummary.put(e.getHost()+"-"+e.getCommand(), this.commandHostSummary.get(e.getHost()+"-"+e.getCommand())+1); 
			
			//now active SQL only
			if("sleep".equalsIgnoreCase(e.getCommand()))continue;
			
			//1. Strip comments
			String s = stripComments(e.getInfo());
			if(s==null||s.isEmpty())continue;
			int t = e.getTime();
			if(t==Integer.MAX_VALUE||t<0)t = 0;//overflow
			
			ProcessListEntryAggregate.updateDataMap(this.queries, s, t);
			totalAccumultaedTime+=t;
			String ns = this.normalize(s);
						
			ProcessListEntryAggregate.updateDataMap(this.normalizedQueries, ns, t);
			
			String s2 = this.stripLimit(s);
						
			ProcessListEntryAggregate.updateDataMap(this.queriesWithLimitStripped, s2, t);
			
			for(ProcessListEntryProcessor pr:this.appProcessorList)
				pr.processEntry(e);
		}
		
	}
	public Map<String, Integer> getUserSummary() {
		return userSummary;
	}

	public void setUserSummary(Map<String, Integer> userSummary) {
		this.userSummary = userSummary;
	}

	public Map<String, Integer> getHostSummary() {
		return hostSummary;
	}

	public void setHostSummary(Map<String, Integer> hostSummary) {
		this.hostSummary = hostSummary;
	}

	public Map<String, Integer> getCommandSummary() {
		return commandSummary;
	}

	public void setCommandSummary(Map<String, Integer> commandSummary) {
		this.commandSummary = commandSummary;
	}

	public Map<String, Integer> getCommandHostSummary() {
		return commandHostSummary;
	}

	public void setCommandHostSummary(Map<String, Integer> commandHostSummary) {
		this.commandHostSummary = commandHostSummary;
	}

	public Map<String, Integer> getStateSummary() {
		return stateSummary;
	}

	public void setStateSummary(Map<String, Integer> stateSummary) {
		this.stateSummary = stateSummary;
	}
	
	/**
	 * Remove leading comments only
	 * @param str
	 * @return
	 */
	private static String stripComments(String str)
	{
		if(str==null)return "";
		String s = str.trim();
		while(s.startsWith("/*"))
		{
			int idx = s.indexOf("*/");
			if(idx<0)return s;//might not be valie
			s = s.substring(idx+2);
			if(s==null)return s;
			else s = s.trim();			
		}
		return s;
	}
	//TODO in case there is a LIMIT in the middle
	private  String stripLimit(String str)
	{
		if(str==null)return "";
		String s = str.trim();
		Matcher mt = pt.matcher(s);
		if(!mt.find())
			return s;
		if(mt.end()==s.length())
		{
			s = s.substring(0, mt.start());
			if(s!=null)s=s.trim();
			return s;
		}
		mt = pt2.matcher(s);
		if(!mt.find())
			return s;
		if(mt.end()==s.length())
		{
			s = s.substring(0, mt.start());
			if(s!=null)s=s.trim();
			return s;
		}
		return s;//don't care middle one
	}
	
	
	/**
	 * Normalize a query string
	 * @param str
	 * @return
	 */
	private String normalize(String str)
	{
		
	    String rpt3 = "(?i)VALUES\\s*\\((.*?)\\)";
	    String rpt4 = "(?i)in\\s*\\([^\\(^\\)]+\\)";
	    String rpt5 = "(?i)limit\\s+\\d+\\s*$" ;
	    String rpt6 = "(?i)limit\\s+\\d+\\s*,\\s*\\d+\\s*$" ;
	    str =  str.replaceAll("=\\s*\\d+", "=?");
	    str =  str.replaceAll("=\\s*'[^']+'", "=?");
	    
	    str = str.replaceAll("\\+\\s*'?\\d+'?", "+?");

	    str = str.replaceAll("\\-\\s*'?\\d+'?", "-?");

	    str = str.replaceAll(">\\s*\\d+", ">?");
	    str = str.replaceAll(">\\s*'[^']+'", ">?");

	    str = str.replaceAll(">=\\s*\\d+", ">=?");
	    str = str.replaceAll(">=\\s*'[^']+'", ">=?");

	    str = str.replaceAll("<\\s*\\d+", "<?");
	    str = str.replaceAll("<\\s*'[^']+'", "<?");

	    str = str.replaceAll("<=\\s*'?\\d+'?", "<=?");
	    str = str.replaceAll("<=\\s*'[^']+'", "<=?");

	    str = str.replaceAll("(?i)BETWEEN\\s+\\d+\\s+AND\\s+\\d+", "BETWEEN ? and ?");
	    str = str.replaceAll("(?i)BETWEEN\\s+'[^']+'\\s+AND\\s+'[^']+'", "BETWEEN ? and ?");

	    //BETWEEN 1391040000 AND 1391126400
	    
	    str = str.replaceAll(rpt4, "IN (?)");
	    str = str.replaceAll(rpt3, "VALUES (?)");
	    str = str.replaceAll(rpt5, "LIMIT ?");
	    str = str.replaceAll(rpt6, "LIMIT ?, ?");
	    //str = str.replaceAll("\\((\\s*'?\\d+'?\\s*,?)+", "(?");
	    //add special treatment for big inlist
	    str = replaceInlist(str);
	    return str;

	}
	
	/**
	 * We might have a case processlist cannot print the full inlist
	 * This should be used after all other normalize operations
	 * @param str
	 * @return
	 */
	private String replaceInlist(String str)
	{
		try
		{
 		   Pattern pt = Pattern.compile("(?i)\\s*IN\\s*\\('?\\d+'?,");
		  Matcher m = pt.matcher(str);
		  if(m.find())
		  {
			int start = m.start();
			//find first close bracket )
			int idx = str.indexOf(')', start);
			if(idx<0 || idx==str.length()-1)
				return str.substring(0, start)+" IN (......)";
			else
				return str.substring(0, start)+" IN (...) "+str.substring(idx+1);
		  }
		}catch(Exception ex){}
		return str;
	}
	public Map<String, ProcessListEntryAggregate> getQueries() {
		return queries;
	}
	public Map<String, ProcessListEntryAggregate> getQueriesWithLimitStripped() {
		return queriesWithLimitStripped;
	}

	public List<ProcessListEntry> getProcessList() {
		return processList;
	}
	public void setProcessList(List<ProcessListEntry> processList) {
		this.processList = processList;
	}
	
	public void saveAsText(PrintWriter pw)
	{
		pw.println("Total Time: "+this.totalAccumultaedTime+" seconds.");

		pw.println();
		pw.println("------ User Summary ------");
		List<KeyValIntPair> tmpList = new ArrayList<KeyValIntPair>(this.userSummary.size());
		for(Map.Entry<String, Integer> e: this.userSummary.entrySet())
		{
			tmpList.add(new KeyValIntPair(e.getKey(), e.getValue()));
		}
		java.util.Collections.sort(tmpList);
		for(KeyValIntPair p: tmpList)
		{
			//pw.println(p.name+":  "+p.value);
			pw.println(String.format("%5d: %s", p.value, p.name));
		}
		tmpList.clear();
		
		int count = 0;
		pw.println();
		pw.println("------ Host Summary ------");
		tmpList = new ArrayList<KeyValIntPair>(this.hostSummary.size());
		for(Map.Entry<String, Integer> e: this.hostSummary.entrySet())
		{
			tmpList.add(new KeyValIntPair(e.getKey(), e.getValue()));
			count++;
			if(count>=20)break;
		}
		java.util.Collections.sort(tmpList);
		for(KeyValIntPair p: tmpList)
			pw.println(String.format("%5d: %s", p.value, p.name));
		tmpList.clear();

		pw.println();
		pw.println("------ Command Summary ------");
		tmpList = new ArrayList<KeyValIntPair>(this.commandSummary.size());
		for(Map.Entry<String, Integer> e: this.commandSummary.entrySet())
		{
			tmpList.add(new KeyValIntPair(e.getKey(), e.getValue()));
		}		
		java.util.Collections.sort(tmpList);
		for(KeyValIntPair p: tmpList)
			pw.println(String.format("%5d: %s", p.value, p.name));
		tmpList.clear();
		
		pw.println();
		pw.println("------ State Summary ------");
		tmpList = new ArrayList<KeyValIntPair>(this.stateSummary.size());
		for(Map.Entry<String, Integer> e: this.stateSummary.entrySet())
		{
			tmpList.add(new KeyValIntPair(e.getKey(), e.getValue()));
		}
		java.util.Collections.sort(tmpList);
		for(KeyValIntPair p: tmpList)
			pw.println(String.format("%5d: %s", p.value, p.name));
		tmpList.clear();

		
		pw.println();
		pw.println("------ Query Summary With LIMIT Stripped------");
		
		ArrayList<ProcessListEntryAggregate> tmpList2 = new ArrayList<ProcessListEntryAggregate>(this.queriesWithLimitStripped.size());
		for(Map.Entry<String, ProcessListEntryAggregate> e: this.queriesWithLimitStripped.entrySet())
		{
			tmpList2.add(e.getValue());
		}
		java.util.Collections.sort(tmpList2, new ProcessListEntryAggregate.SortByCount());
		count = 0;
		for(ProcessListEntryAggregate p: tmpList2)
		{
			pw.println(p.getChecksum()+": "+p.getSql());
			pw.println(String.format("%s: count - %d, time - %d sec, avg - %3f sec, min - %d sec, max -%d sec", 
					p.getChecksum(), p.getCount(), p.getTotal_time_sec(), p.getAverage(), p.getMin_time_sec(), p.getMax_time_sec()));
			count++;
			if(count>20)break;//since we sort them and attach process list, display only top 20
		}
		//tmpList2.clear();
		
		pw.println();
		pw.println("------ Query Time Summary With LIMIT Stripped------");

		java.util.Collections.sort(tmpList2);
		count = 0;
		for(ProcessListEntryAggregate p: tmpList2)
		{
			pw.println(p.getChecksum()+": "+p.getSql());
			pw.println(String.format("%s: count - %d, time - %d sec, avg - %3f sec, min - %d sec, max -%d sec", 
					p.getChecksum(), p.getCount(), p.getTotal_time_sec(), p.getAverage(), p.getMin_time_sec(), p.getMax_time_sec()));
			count++;
			if(count>20)break;//since we sort them and attach process list, display only top 20
		}
		tmpList2.clear();

		pw.println();
		pw.println("------ Normalized Queries------");
		tmpList2 = new ArrayList<ProcessListEntryAggregate>(this.normalizedQueries.size());
		for(Map.Entry<String, ProcessListEntryAggregate> e: this.normalizedQueries.entrySet())
		{
			tmpList2.add(e.getValue());
		}
		java.util.Collections.sort(tmpList2, new ProcessListEntryAggregate.SortByCount());

		count = 0;
		for(ProcessListEntryAggregate p: tmpList2)
		{
			pw.println(p.getChecksum()+": "+p.getSql());
			pw.println(String.format("%s: count - %d, time - %d sec, avg - %3f sec, min - %d sec, max -%d sec", 
					p.getChecksum(), p.getCount(), p.getTotal_time_sec(), p.getAverage(), p.getMin_time_sec(), p.getMax_time_sec()));
			count++;
			if(count>20)break;//since we sort them and attach process list, display only top 20
		}
		//tmpList2.clear();

		pw.println();
		pw.println("------ Time Summary With Normalized Queries------");
		
		java.util.Collections.sort(tmpList2);
		count = 0;
		for(ProcessListEntryAggregate p: tmpList2)
		{
			pw.println(p.getChecksum()+": "+p.getSql());
			pw.println(String.format("%s: count - %d, time - %d sec, avg - %3f sec, min - %d sec, max -%d sec", 
					p.getChecksum(), p.getCount(), p.getTotal_time_sec(), p.getAverage(), p.getMin_time_sec(), p.getMax_time_sec()));
			count++;
			if(count>20)break;//since we sort them and attach process list, display only top 20
		}
		tmpList2.clear();

		//app specific
		for(ProcessListEntryProcessor pr:this.appProcessorList)
			pr.dumpSummary(pw);

		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));	

		pw.println();
		pw.println("------ Active Process List ------");
		int idx = 1;
		for(ProcessListEntry e: this.processList)
		{
			if("Sleep".equalsIgnoreCase(e.getCommand()))continue;//ignore sleep one
			//dump the process list
			pw.println("------ Query "+idx+" ---");
			pw.println(e.getInfo());
			String ts = "";
			try
			{
				ts = sdf.format(new java.util.Date(this.reportTimestamp - e.getTime()*1000));
			}catch(Exception ex){}
			pw.println("Time: "+e.getTime()+", Ts: "+ts+", State: "+e.getState()+", User: "+e.getUser()+", DB: "+e.getDb()+", Host: "+e.getHost());
			if(e.getRows_examined()>0 || e.getRows_read()>0 ||e.getRows_sent()>0)
			{
				pw.println("Rows_examined: "+e.getRows_examined()+", Rows_read: "+e.getRows_read()+", Rows_sent: "+e.getRows_sent());
			}
			idx++;
		}
		
		if(this.innodbStatus!=null)
		{
			pw.println();
			pw.println("------ Innodb Status ------");
			
			 java.io.StringReader reader = new java.io.StringReader(this.innodbStatus);
			 java.io.BufferedReader bufReader = new java.io.BufferedReader(reader);
			 String line = null;
			 boolean skip = false;
			 try
			 {
			      while( (line = bufReader.readLine())!=null)
			      {
			    	  if(line.startsWith("---TRANSACTION ") && line.contains("not started") && !line.contains("estimating"))
			    		  skip = true;
			    	  else if(skip)
			    	  {
			    		  if(line.startsWith("--------")||line.startsWith("---TRANSACTION "))
				    		  skip = false;
			    	  }
			    	  if(!skip)
			    	  pw.println(line);	  
			      }
			 }catch(Exception ex)
			 {
				 logger.log(Level.INFO, "innodb status parsing error", ex);
			 
			 }finally
			 {
				 if(bufReader!=null)try{bufReader.close();}catch(Exception iex){}
			 }
		}
		
		if(this.lockList!=null  && this.lockList.getRows().size()>0)
		{
			pw.println();
			pw.println("------ InnoDB Locks ------");
			AlertReport.printList(pw, this.lockList);			
		}
		if(this.clientList!=null && this.clientList.getRows().size()>0)
		{
			pw.println();
			pw.println("------ Client Statistics ------");
			AlertReport.printList(pw, this.clientList);			
			
		}
		if(this.trxList!=null && this.trxList.getRows().size()>0)
		{
			pw.println();
			pw.println("------ InnoDB TRX LONGER THAN 60 SECONDS ------");
			AlertReport.printList(pw, this.trxList);			
			
		}
	}
	
	
	public Map<String, ProcessListEntryAggregate> getNormalizedQueries() {
		return normalizedQueries;
	}
	public String getInnodbStatus() {
		return innodbStatus;
	}
	public void setInnodbStatus(String innodbStatus) {
		this.innodbStatus = innodbStatus;
	}

	public long getReportTimestamp() {
		return reportTimestamp;
	}

	public void setReportTimestamp(long reportTimestamp) {
		this.reportTimestamp = reportTimestamp;
	}

	public ResultList getLockList() {
		return lockList;
	}

	public void setLockList(ResultList lockList) {
		this.lockList = lockList;
	}

	public ResultList getClientList() {
		return clientList;
	}

	public void setClientList(ResultList clientList) {
		this.clientList = clientList;
	}

	public ResultList getTrxList() {
		return trxList;
	}

	public void setTrxList(ResultList trxList) {
		this.trxList = trxList;
	}
	
}
