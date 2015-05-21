/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

/**
 * Result column descriptor
 * @author xrao
 *
 */
public class ColumnInfo implements java.io.Serializable{

	private static final long serialVersionUID = 2467837325706019159L;
	private String name;//name of the column
	private int position;//position of the column
	private boolean numberType;//no need to quote number type	
	private int maxLength = 0;//used for formatting
	
	//support repository
	private String dbDataType;//data type of the database column
	private String javaType;//data type in java
	private String javaName;//java bean member name
	private String srcName;//column name from source Oracle database
	
	public ColumnInfo()
	{
		
	}

	public ColumnInfo(String name)
	{
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public boolean isNumberType() {
		return numberType;
	}

	public void setNumberType(boolean numberType) {
		this.numberType = numberType;
	}

	public ColumnInfo copy()
	{
		ColumnInfo colInfo = new ColumnInfo(this.name);
		colInfo.setNumberType(this.numberType);
		return colInfo;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public String getDbDataType() {
		return dbDataType;
	}

	public void setDbDataType(String dbDataType) {
		this.dbDataType = dbDataType;
	}

	public String getJavaType() {
		return javaType;
	}

	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}

	public String getJavaName() {
		return javaName;
	}

	public void setJavaName(String javaName) {
		this.javaName = javaName;
	}

	public String getSrcName() {
		return srcName;
	}

	public void setSrcName(String srcName) {
		this.srcName = srcName;
	}
}
