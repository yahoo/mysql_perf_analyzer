/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.sql.ResultSet;

/**
 * MySQL process list info
 * @author xrao
 *
 */
public class ProcessListEntry implements java.io.Serializable, Comparable<ProcessListEntry>{

	private static final long serialVersionUID = 1L;
	
	private String id;
	private String user;
	private String host;
	private String db;
	private String command;
	private int time;
	private String state;
	private String info;
	
	//note not all MySQL servers have these metrics (percona 5.5.10 or later). For 5.6, we can use performance schema
	private long time_ms = -1L;
	private long rows_read = 0;
	private long rows_examined = 0;
	private long rows_sent = 0;
	
	public ProcessListEntry()
	{
		
	}

	/**
	 * Construct from one row
	 * @param rs
	 */
	public ProcessListEntry(ResultSet rs)
	throws java.sql.SQLException
	{
		this.setId(rs.getString("ID"));
		this.setUser(rs.getString("USER"));
		this.setHost(rs.getString("HOST"));
		this.setDb(rs.getString("DB"));
		this.setCommand(rs.getString("COMMAND"));
		this.setState(rs.getString("STATE"));
		this.setTime(rs.getInt("TIME"));
		this.setInfo(rs.getString("INFO"));
	}

	/**
	 * 
	 * @param rs
	 * @param rowInfo If true, with ROWS_SENT, ROWS_EXAMINED and ROWS_READ
	 * @throws java.sql.SQLException
	 */
	public ProcessListEntry(ResultSet rs, boolean rowInfo, boolean hasRowsRead)
	throws java.sql.SQLException
	{
		this.setId(rs.getString("ID"));
		this.setUser(rs.getString("USER"));
		this.setHost(rs.getString("HOST"));
		this.setDb(rs.getString("DB"));
		this.setCommand(rs.getString("COMMAND"));
		this.setState(rs.getString("STATE"));
		this.setTime(rs.getInt("TIME"));
		this.setInfo(rs.getString("INFO"));
		if(rowInfo)
		{
			this.setRows_examined(rs.getLong("ROWS_EXAMINED"));
			if(hasRowsRead)this.setRows_read(rs.getLong("ROWS_READ"));
			this.setRows_sent(rs.getLong("ROWS_SENT"));
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		if(host==null)this.host = host;
		int idx = host.indexOf(":");//strip :
		if(idx<0)
			this.host = host;
		else
			this.host = host.substring(0, idx)+"";
	}

	public String getDb() {
		return db;
	}

	public void setDb(String db) {
		this.db = db;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public long getTime_ms() {
		return time_ms;
	}

	public void setTime_ms(long time_ms) {
		this.time_ms = time_ms;
	}

	public long getRows_read() {
		return rows_read;
	}

	public void setRows_read(long rows_read) {
		this.rows_read = rows_read;
	}

	public long getRows_examined() {
		return rows_examined;
	}

	public void setRows_examined(long rows_examined) {
		this.rows_examined = rows_examined;
	}

	public long getRows_sent() {
		return rows_sent;
	}

	public void setRows_sent(long rows_sent) {
		this.rows_sent = rows_sent;
	}

	@Override
	public int compareTo(ProcessListEntry obj) {
		if(obj == null)return 1;
		String s = obj.getInfo();
		if((s==null||s.isEmpty()) && (this.info==null ||this.info.isEmpty()))
				return 0;//both are empty
		else if(s==null||s.isEmpty())
				return 1;//the other is empty
		else if(this.info==null || this.info.isEmpty())
			return -1;//we are empty
		
		try
		{
			//nobody empty
			return this.info.compareTo(s);
		}catch(Exception ex)
		{
			ex.printStackTrace();
			System.out.println("info: "+this.info+", comparedTo "+s);
		}
		return -1;
	}
}
