/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MetricsBuffer {
	private static Logger logger = Logger.getLogger(MetricsBuffer.class.getName());
	private MetricsGroup metrics;//metrics to store
	
	private int recordLength;//note: for each record, first 4 bytes for dbid, 4 bytes forsnap_id, 8 bytes for timestamp, 4 bytes for SQL response time in ms

	private int dbid;//database id, for data persistence purpose
	
	public MetricsBuffer(MetricsGroup mg)
	{
		//this.capacity = capacity;
		this.metrics = mg;
		metrics.calculateLength();
		this.recordLength = 20 + metrics.getLength();
	}
	
	
	
	public MetricsGroup getMetrics()
	{
		return this.metrics;
	}
	
	public int getRecordLength()
	{
		return this.recordLength;
	}

	

	public java.nio.ByteBuffer recordOneRowBymetricsName2(java.sql.ResultSet rs, int snap_id, long timestamp, int sql_time)
	throws java.sql.SQLException
	{
		HashMap<String, String> hs = new HashMap<String, String>();
		while(rs!=null && rs.next())
		{
			hs.put(rs.getString(metrics.getMetricNameColumn()).toUpperCase(), rs.getString(metrics.getMetricValueColumn()));
		}
		if(hs.size()==0)return null;//TODO log error
		
		byte[] buf = new byte[this.recordLength];
		java.nio.ByteBuffer buf2 = java.nio.ByteBuffer.wrap(buf);
		
		int pos = 0;
		buf2.putInt(pos, dbid); pos+=4;
		buf2.putInt(pos, snap_id); pos+=4;
		buf2.putLong(pos, timestamp); pos+=8;
		buf2.putInt(pos, sql_time); pos+=4;
		List<Metric> ms = metrics.getMetrics();
		int len = ms.size();				
		for(int i=0;i<len;i++)
		{
			Metric m = ms.get(i);
			String val = null;
			if(hs.containsKey(m.getSourceName().toUpperCase()))
			  val = hs.get(m.getSourceName().toUpperCase());
			if(m.getDataType()==MetricDataType.BYTE)
			{
				if(val!=null)
					buf2.put(pos, Byte.parseByte(val));
				else buf2.put(pos, (byte)0);
				pos+=1;
			}
			else if(m.getDataType()==MetricDataType.SHORT)
			{
				if(val!=null)
					buf2.putShort(pos, Short.parseShort(val));
				else buf2.putShort(pos, (short)0);
				pos+=2;
			}else if(m.getDataType()==MetricDataType.INT)
			{
				if(val!=null)
				{
					try
					{
						buf2.putInt(pos, Integer.parseInt(val));
					}catch(Exception ex)
					{
						logger.log(Level.WARNING, "Failed to parse "+m.getName()+" with value "+val, ex);
						buf2.putInt(pos, (int)Long.parseLong(val));
					}
				} else buf2.putInt(pos, 0);
				pos+=4;
			}else if(m.getDataType()==MetricDataType.LONG)
			{
				if(val!=null)
					buf2.putLong(pos, Long.parseLong(val));
				else buf2.putLong(pos, 0);
				pos+=8;
			}else if(m.getDataType()==MetricDataType.FLOAT)
			{
				if(val!=null)
					buf2.putFloat(pos, Float.parseFloat(val));
				else buf2.putFloat(pos, 0);
				pos+=4;
			}else if(m.getDataType()==MetricDataType.DOUBLE)
			{
				if(val!=null)
					buf2.putDouble(pos, Double.parseDouble(val));
				else buf2.putDouble(pos, 0);
				pos+=8;
			}
		}
		return buf2;
	}

	public java.nio.ByteBuffer recordOneRowByMetricsMap(java.util.Map<String, String> rs, int snap_id, long timestamp, int sql_time)
	throws java.sql.SQLException
	{
		if(rs.size()==0)return null;//TODO log error
		HashMap<String, String> hs = new HashMap<String, String>();
		for(Map.Entry<String, String> e: rs.entrySet())
		{
			//skip null
			if(e.getKey()==null||e.getValue()==null)continue;
			hs.put(e.getKey().toUpperCase(), e.getValue());
		}
		
		byte[] buf = new byte[this.recordLength];
		java.nio.ByteBuffer buf2 = java.nio.ByteBuffer.wrap(buf);
		
		int pos = 0;
		buf2.putInt(pos, dbid); pos+=4;
		buf2.putInt(pos, snap_id); pos+=4;		
		buf2.putLong(pos, timestamp); pos+=8;
		buf2.putInt(pos, sql_time); pos+=4;
		List<Metric> ms = metrics.getMetrics();
		int len = ms.size();				
		for(int i=0;i<len;i++)
		{
			Metric m = ms.get(i);
			String val = null;
			if(hs.containsKey(m.getSourceName().toUpperCase()))
			  val = hs.get(m.getSourceName().toUpperCase());
			if(m.getDataType()==MetricDataType.BYTE)
			{
				try
				{
					buf2.put(pos, Byte.parseByte(val));
				}catch(Exception ex)
				{
					buf2.put(pos, (byte)0);
				}
				pos+=1;
			}
			else if(m.getDataType()==MetricDataType.SHORT)
			{
				try
				{
					buf2.putShort(pos, Short.parseShort(val));
				}catch(Exception ex)
				{
					buf2.putShort(pos, (short)0);
				}
				pos+=2;
			}else if(m.getDataType()==MetricDataType.INT)
			{
				try
				{
					buf2.putInt(pos, Integer.parseInt(val));
				}catch(Exception ex)
				{
					buf2.putInt(pos, 0);
				}
				pos+=4;
			}else if(m.getDataType()==MetricDataType.LONG)
			{
				try
				{
					buf2.putLong(pos, Long.parseLong(val));
				}catch(Exception ex)
				{
					buf2.putLong(pos, 0);
				}
				pos+=8;
			}else if(m.getDataType()==MetricDataType.FLOAT)
			{
				try
				{
					buf2.putFloat(pos, Float.parseFloat(val));
				}catch(Exception ex)
				{
					buf2.putFloat(pos, 0);
				}
				pos+=4;
			}else if(m.getDataType()==MetricDataType.DOUBLE)
			{
				try
				{
					buf2.putDouble(pos, Double.parseDouble(val));
				}catch(Exception ex)
				{
					buf2.putDouble(pos, 0);
				}
				pos+=8;
			}
		}
		return buf2;
	}

	public static String escapeJson(String str)
	{
		if(str==null)return "";
		StringBuilder sb = new StringBuilder(str.length());
		char[] carray = str.toCharArray();
		for(int i=0;i<carray.length;i++)
		{  
			char c = carray[i];
			if(c=='\"')
				sb.append("\\u0022");
			else if(c=='\n')
				sb.append("\\u000A");
			else if(c=='\\')
				sb.append("\\u005C");
			else if(c=='\r')
				sb.append("\\u000D");
			else if(c=='\t')
				sb.append("\\u0009");
			else if(c=='/')
				sb.append("\\u002F");
			else if(c=='\0'||c<' ')//edw2p has osuser set as two chars (24, 20)
			{
				//skip it
			}
			else sb.append(c);
		}
		return sb.toString();
		
	}

	public int getDbid() {
		return dbid;
	}

	public void setDbid(int dbid) {
		this.dbid = dbid;
	}

}
