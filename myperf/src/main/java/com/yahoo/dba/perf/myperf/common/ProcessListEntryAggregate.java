/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.Map;

/**
 * The default compartor is sorted by total_time_sec
 * @author xrao
 *
 */
public class ProcessListEntryAggregate implements java.io.Serializable, Comparable<ProcessListEntryAggregate>{
	private static final long serialVersionUID = 1L;

	static final char HEXES[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	private String sql;//that will be the key
	private int count = 0;
	private int total_time_sec = 0;//at this moment, sec only
	private int min_time_sec = -1;//minimum time recorded -1 manes not updated yet
	private int max_time_sec = 0;//max time recorded
	
	private String checksum;
	
	public static class SortByCount implements java.util.Comparator<ProcessListEntryAggregate>
	{

		@Override
		public int compare(ProcessListEntryAggregate obj1,
				ProcessListEntryAggregate obj2) {
			if(obj1==null && obj2==null)
				return 0;
			else if(obj1==null)return 1;
			else if(obj2==null)return -1;
			return obj2.getCount() - obj1.getCount();//reverse order
		}
		
	}
	public ProcessListEntryAggregate(String sql)
	{
		this.sql = sql;
	}

	public String getSql() {
		return sql;
	}

	public int getCount() {
		return count;
	}

	public int getTotal_time_sec() {
		return total_time_sec;
	}

	public int getMin_time_sec() {
		return min_time_sec;
	}


	public int getMax_time_sec() {
		return max_time_sec;
	}

	public float getAverage()
	{
		if(this.count==0)return 0;
		else 
			return (float)this.total_time_sec/(float)this.count;
	}
	/**
	 * Record one execution instance time. Later we might want to add more metrics for example, rows_read, etc
	 * @param time_sec
	 */
	public void record(int time_sec)
	{
		this.count++;
		this.total_time_sec+=time_sec;
		if(this.min_time_sec<0)
			this.min_time_sec = time_sec;
		else 
			this.min_time_sec = Math.min(this.min_time_sec, time_sec);
		this.max_time_sec = Math.max(this.max_time_sec, time_sec);
	}

	@Override
	public int compareTo(ProcessListEntryAggregate obj) {
		if(obj==null)return -1;
		return obj.getTotal_time_sec() - this.getTotal_time_sec();
	}
	
	public static void updateDataMap(Map<String, ProcessListEntryAggregate> m, String sql, int time_sec)
	{
		if(m==null)return;
		if(m.containsKey(sql))
		{
			m.get(sql).record(time_sec);
		}else
		{
			ProcessListEntryAggregate e = new ProcessListEntryAggregate(sql);
			e.setChecksum(generateDigest(sql));
			e.record(time_sec);
			m.put(sql, e);
		}
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	/**
	 * Generate a digest for each query string
	 * @param str
	 * @return
	 */
	public static String generateDigest(String str)
	{
		if(str==null||str.isEmpty())return "00000000";
		try
		{
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA1");
			md.update(str.getBytes());
			byte[] digest = md.digest();
			StringBuilder sb = new StringBuilder();
			for(byte b: digest)
			{
				sb.append(HEXES[(b & 0xF0)>>4])
				  .append(HEXES[(b&0x0F)]);
			}
			return sb.toString();
		}catch(Exception ex)
		{
			
		}
		return "00000000";
	}

}
