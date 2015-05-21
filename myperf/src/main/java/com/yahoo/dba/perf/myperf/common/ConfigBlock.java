/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.Map;
import java.util.TreeMap;

/**
 * A config block includes the time the data was gathered 
 * and the configuration data in name=value pair 
 * The data will be persisted in blocks, starting with time in square bracket,
 * followed by a list of lines of name value pair.
 * The time is in yyyy-MM-dd HH:mm:ss (UTC) format.
 * @author xrao
 *
 */
public class ConfigBlock implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private String time;
	private Map<String, String> variables;//well, in MySQL, there are called variables
	
	public ConfigBlock()
	{
		this.variables = new TreeMap<String, String>();
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public Map<String, String> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, String> variables) {
		this.variables = variables;
	}
	
	public void addVariable(String name, String value)
	{
		this.variables.put(name, value);
	}
}
