/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * Some common utility functions are consolidated here for reuse purpose
 * @author xrao
 *
 */
public class CommonUtils {
	public static long getMapValueLong(Map<String, String> map, String key, long def)
	{
		if(map==null)return def;
		if(!map.containsKey(key))return def;
		try
		{
			return Long.parseLong(map.get(key));
		}catch(Exception ex)
		{
			return def;
		}
	}
	public static float getMapValueFloat(Map<String, String> map, String key, float def)
	{
		if(map==null)return def;
		if(!map.containsKey(key))return def;
		try
		{
			return Float.parseFloat(map.get(key));
		}catch(Exception ex)
		{
			return def;
		}
	}

	public static String escapeJsonHTML(String str)
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
			//else if(c=='/')
			//	sb.append("\\u002F");
			else if(c=='\0'||c<' ')//edw2p has osuser set as two chars (24, 20)
			{
				//skip it
			}
			else sb.append(c);
		}
		return sb.toString();		
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
				sb.append("\\\"");
			else if(c=='\n')
				sb.append("\\n");
			else if(c=='\r')
				sb.append("\\r");
			else if(c=='\t')
				sb.append("\\t");
			//else if(c=='/')
			//	sb.append("\\u002F");
			else if(c=='\0'||c<' ')
			{
				//skip it
			}
			else sb.append(c);
		}
		return sb.toString();		
	}

	/**
	 * Format timestamp ts to string of format fmt, in UTC, for example, yyyy-MM-dd HH:mm:ss
	 * @param ts
	 * @param fmt
	 * @return If failed, return empty string ""
	 */
	public static String formatTimeFromTimestamp(long ts, String fmt)
	{
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));	
		try
		{
			return sdf.format(new Date(ts));
		}catch(Exception ex)
		{
			
		}
		return "";
	}
}
