/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This records configuration history. It includes:
 *   a starting ConfigBlock with all configuration variables for the first time it is tracked
 *   a series of changes of ConfigBlock
 *   last checked configBlock 
 * @author xrao
 *
 */
public class ConfigHistory implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private static Map<String, Integer> EXCLUDE_ENTRIES = new HashMap<String, Integer>();
	
	private ConfigBlock startingConfig;
	private ConfigBlock lastCheckedConfig;
	private List<ConfigBlock> changes;
	
	static
	{
		//these are really status
		EXCLUDE_ENTRIES.put("GTID_EXECUTED", 1);
		EXCLUDE_ENTRIES.put("GTID_PURGED", 1);
		
	}
	public ConfigHistory()
	{
		changes = new ArrayList<ConfigBlock>();
	}
	public ConfigBlock getStartingConfig() {
		return startingConfig;
	}
	public void setStartingConfig(ConfigBlock startingConfig) {
		this.startingConfig = startingConfig;
	}
	public ConfigBlock getLastCheckedConfig() {
		return lastCheckedConfig;
	}
	public void setLastCheckedConfig(ConfigBlock lastCheckedConfig) {
		this.lastCheckedConfig = lastCheckedConfig;
	}
	public List<ConfigBlock> getChanges() {
		return changes;
	}
	public void setChanges(List<ConfigBlock> changes) {
		this.changes = changes;
	}
	public void addChange(ConfigBlock change)
	{
		this.changes.add(change);
	}

	/**
	 * Read the data from persistence store
	 * @param in
	 * @throws Exception
	 */
	public void read(InputStream in)
			throws Exception
	{
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			ConfigBlock cb = null;
			while((line = br.readLine())!=null)
			{
				if(line.isEmpty())
				{
					cb = null;//end current configuration block
				}else if(line.startsWith("[CHANGES]"))
				{
					cb = new ConfigBlock();
					this.changes.add(cb);
				}else if(line.startsWith("[STARTING]"))
				{
					cb = new ConfigBlock();
					this.startingConfig = cb;
				}else if(line.startsWith("[LAST]"))
				{
					cb = new ConfigBlock();
					this.lastCheckedConfig = cb;
				}else if(line.startsWith("[") && line.endsWith("]"))
				{
					if(cb!=null)cb.setTime(line.substring(1,line.length()-1));
				}else if(line.indexOf('=')>0)
				{
					int idx = line.indexOf('=');
					if(idx<line.length()-1)
						cb.addVariable(line.substring(0, idx), line.substring(idx+1));
					else
						cb.addVariable(line.substring(0, idx),"");
				}
				
			}
		}finally
		{
			if(br!=null)try{br.close();}catch(Exception iex){}
		}
		
	}
	
	/**
	 * Write the data to persistence store
	 * @param out
	 * @throws Exception
	 */
	public void write(OutputStream out)
	throws Exception
	{
		PrintWriter bw = null;
		try
		{
			bw = new PrintWriter(out);
			//let's start with changes and only leave first and last as references at the end
			for(ConfigBlock cb: this.changes)
			{
				bw.println("[CHANGES]");
				bw.println("["+cb.getTime()+"]");
				for(Map.Entry<String, String> e: cb.getVariables().entrySet())
				{
					if(e.getValue()!=null)
						bw.println(e.getKey()+"="+e.getValue());
					else
						bw.println(e.getKey()+"=");
				}
				bw.println();//add an empty line
				
			}
			if(this.startingConfig!=null)
			{
				bw.println("[STARTING]");
				bw.println("["+this.startingConfig.getTime()+"]");
				for(Map.Entry<String, String> e: this.startingConfig.getVariables().entrySet())
				{
					if(e.getValue()!=null)
						bw.println(e.getKey()+"="+e.getValue());
					else
						bw.println(e.getKey()+"=");
				}
				bw.println();//add an empty line
			}
			if(this.lastCheckedConfig!=null)
			{
				bw.println("[LAST]");
				bw.println("["+this.lastCheckedConfig.getTime()+"]");
				for(Map.Entry<String, String> e: this.lastCheckedConfig.getVariables().entrySet())
				{
					if(e.getValue()!=null)
						bw.println(e.getKey()+"="+e.getValue());
					else
						bw.println(e.getKey()+"=");
				}
			}
		}finally
		{
			if(bw!=null)
			{
				try{bw.flush();bw.close();}catch(Exception ex){}
			}
		}
		
	}
	
	public  void store(File root, DBInstanceInfo db)
	{
		File f = new File(root, db.getDbGroupName()+"_"+db.getHostName()+"_"+db.getPort()+".cnf");
		FileOutputStream out = null;
		
		try
		{
			out = new FileOutputStream(f);
			write(out);
		}catch(Exception ex)
		{
			
		}finally
		{
			if(out!=null)try{out.close();}catch(Exception ex){}
		}
	}
	/**
	 * Load config history data for specified db
	 * @param root
	 * @param db
	 */
	public static ConfigHistory load(File root, DBInstanceInfo db)
	{
		File f = new File(root, db.getDbGroupName()+"_"+db.getHostName()+"_"+db.getPort()+".cnf");
		if(!f.exists())return null;
		
		FileInputStream in = null;
		try
		{
			in = new FileInputStream(f);
			ConfigHistory ch = new ConfigHistory();
			ch.read(in);
			return ch;
		}catch(Exception ex)
		{
			
		}finally
		{
			if(in!=null)try{in.close();}catch(Exception ex){}
		}
		return null;
	}
	/**
	 * Update history with last check
	 * @param last 
	 * @return true if updated, false if no change
	 */
	public boolean updateLast(ConfigBlock last)
	{
		if(last == null)return false;
		if(this.startingConfig==null)
		{
			//first time we build info
			this.startingConfig = last;
			return true;
		}
		
		ConfigBlock prev = this.lastCheckedConfig;
		if(prev==null)//no change history yet
			prev = this.startingConfig;
		
		Map<String, String> diffMap = new TreeMap<String, String>();
		Map<String, String> prevMap = prev.getVariables();
		Map<String, String> lastMap = last.getVariables();
		for(Map.Entry<String, String> e: lastMap.entrySet())
		{
			String key = e.getKey();
			String val = e.getValue();
			if(EXCLUDE_ENTRIES.containsKey(key))continue;
			if(val==null)val = "";//treat null as empty
			
			//1. The prev has no record
			if(!prevMap.containsKey(key))
			{
				diffMap.put(key, val);
			}else
			{
				String oldVal = prevMap.get(key);
				if(oldVal==null)oldVal = "";
				if(!val.equals(oldVal))
				{
					diffMap.put(key, val);//record new value
					diffMap.put("+-"+key, oldVal);//record old value with +-
				}
			}
		}
		
		for(Map.Entry<String, String> e: prevMap.entrySet())
		{
			String key = e.getKey();
			String val = e.getValue();
			if(EXCLUDE_ENTRIES.containsKey(key))continue;
			if(!lastMap.containsKey(key))
				diffMap.put("-"+key, val);//use - to record a variable is removed
		}
		
		if(diffMap.size()==0)
			return false;
		
		this.lastCheckedConfig = last;
		ConfigBlock cb = new ConfigBlock();
		cb.setTime(last.getTime());
		cb.setVariables(diffMap);
		this.changes.add(cb);
		return true;//we updated the history
	}
}
