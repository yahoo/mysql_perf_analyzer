/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

/**
 * Information about the columns from the a query retiurn results
 * @author xrao
 *
 */
public class ColumnDescriptor implements java.io.Serializable{

	private static final long serialVersionUID = 6923318258330634209L;

	private java.util.List<ColumnInfo> columns ;
	
	public ColumnDescriptor()
	{
		columns = new java.util.ArrayList<ColumnInfo>();
	}

	public java.util.List<ColumnInfo> getColumns() {
		return columns;
	}

	public void setColumns(java.util.List<ColumnInfo> columns) {
		this.columns = columns;
	}
	
	public ColumnInfo getColumn(String colName)
	{
		for(int i=this.columns.size()-1;i>=0;i--)
		{
			if(colName.equalsIgnoreCase(this.columns.get(i).getName()))return this.columns.get(i);
		}
		return null;
	}
	public int getColumnIndex(String colName)
	{
		for(int i=this.columns.size()-1;i>=0;i--)
		{
			if(colName.equalsIgnoreCase(this.columns.get(i).getName()))return i;
		}
		return -1;
	}
	
	public void addColumn(String colName, boolean isNumber, int pos)
	{
		if(this.columns==null)this.columns = new  java.util.ArrayList<ColumnInfo>();
		ColumnInfo col = new ColumnInfo();
		col.setName(colName);
		col.setNumberType(isNumber);
		col.setPosition(pos);
		this.columns.add(col);
	}
}
