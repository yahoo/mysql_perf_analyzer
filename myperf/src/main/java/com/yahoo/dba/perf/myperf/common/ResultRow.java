/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

public class ResultRow implements java.io.Serializable{
	private static final long serialVersionUID = 2131095887594423404L;

	private ColumnDescriptor columnDescriptor;
	private java.util.List<String> columns;//we only need string value for display
	public ResultRow()
	{
		
	}


	public ColumnDescriptor getColumnDescriptor() {
		return columnDescriptor;
	}


	public void setColumnDescriptor(ColumnDescriptor columnDescriptor) {
		this.columnDescriptor = columnDescriptor;
	}


	public java.util.List<String> getColumns() {
		return columns;
	}


	public void setColumns(java.util.List<String> columns) {
		this.columns = columns;
	}
	
	/**
	 * The user can build columns by adding column one by one. The user should not use setColumns when this is used.
	 * This is convenient when there are only a few columns to build
	 * @param s
	 */
	public void addColumn(String s)
	{
		if(this.columns==null)
			this.columns = new java.util.ArrayList<String>();
		this.columns.add(s);
	}
}
