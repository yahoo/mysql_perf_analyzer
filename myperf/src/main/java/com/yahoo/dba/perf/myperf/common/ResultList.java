/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

/**
 * Store qresultset of a query
 * @author xrao
 *
 */
public class ResultList implements java.io.Serializable {
	private static final long serialVersionUID = 1126296256817182221L;
	private ColumnDescriptor columnDescriptor;//meta data
	private java.util.List<ResultRow> rows;//results
	private long totalResponseTime = 0L;//total time used in milliseconds
	private long totalExecutionTime = 0L;//total DB execution time used in milliseconds
	private long totalFetchTime = 0L;//total Resultset Fetch time used in milliseconds
	private java.util.Map<String, CustomResultObject> customObjects;//The key is the json key name
	
	public ResultList()
	{
		rows = new java.util.ArrayList<ResultRow>();
	}
	public ResultList(int capacity)
	{
		rows = new java.util.ArrayList<ResultRow>(capacity);
	}

	public ColumnDescriptor getColumnDescriptor() {
		return columnDescriptor;
	}

	public void setColumnDescriptor(ColumnDescriptor columnDescriptor) {
		this.columnDescriptor = columnDescriptor;
	}

	public int getColumnIndex(String colName)
	{
		return this.columnDescriptor.getColumnIndex(colName);
	}
	public java.util.List<ResultRow> getRows() {
		return rows;
	}

	public void addRow(ResultRow row)
	{
		this.rows.add(row);
	}
	public long getTotalResponseTime() {
		return totalResponseTime;
	}
	public void setTotalResponseTime(long totalResponseTime) {
		this.totalResponseTime = totalResponseTime;
	}
	public long getTotalExecutionTime() {
		return totalExecutionTime;
	}
	public void setTotalExecutionTime(long totalExecutionTime) {
		this.totalExecutionTime = totalExecutionTime;
	}
	public long getTotalFetchTime() {
		return totalFetchTime;
	}
	public void setTotalFetchTime(long totalFetchTime) {
		this.totalFetchTime = totalFetchTime;
	}
	public java.util.Map<String, CustomResultObject> getCustomObjects() {
		return customObjects;
	}
	public void setCustomObjects(java.util.Map<String, CustomResultObject> customObjects) {
		this.customObjects = customObjects;
	}
	public void addCustomeObject(CustomResultObject obj)
	{
		if(obj==null)return;
		if(this.customObjects==null)this.customObjects = new java.util.HashMap<String, CustomResultObject>();
		this.customObjects.put(obj.getName(), obj);
	}
}
