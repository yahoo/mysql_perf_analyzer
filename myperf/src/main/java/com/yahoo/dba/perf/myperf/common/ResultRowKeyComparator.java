/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.Comparator;
import java.util.List;

/**
 * Compare two ResultRows based on keyList. For simplicity, the column descriptors for both rows should be provided
 * @author xrao
 *
 */
public class ResultRowKeyComparator implements Comparator<ResultRow>, java.io.Serializable{
	private static final long serialVersionUID = 2243120460008043846L;
	private List<String> keyList;
	private List<Integer> index1List;
	private List<Integer> index2List;
	
	
	public ResultRowKeyComparator(List<String> keyList, ColumnDescriptor descA, ColumnDescriptor descB)
	{
		this.keyList = keyList;
		this.index1List = new java.util.ArrayList<Integer>(this.keyList.size());
		this.index2List = new java.util.ArrayList<Integer>(this.keyList.size());
		
		for(int i=0;i<keyList.size();i++)
		{
			this.index1List.add( descA.getColumnIndex(keyList.get(i)));
			this.index2List.add( descB.getColumnIndex(keyList.get(i)));
			
		}
	}
	

	public int compare(ResultRow o1, ResultRow o2) {
		if(o1==null && o2==null)return 0;
		if(o1==null)return -1;
		if(o2==null)return 1;
		for(int i=0;i<this.keyList.size();i++)
		{
			String valA = this.index1List.get(i)>=0?o1.getColumns().get(this.index1List.get(i)):null;
			String valB = this.index2List.get(i)>=0?o2.getColumns().get(this.index2List.get(i)):null;
			if(valA==null && valB==null)return 0;
			if(valA==null)return -1;
			if(valB==null)return 1;
			int c = valA.trim().compareTo(valB.trim());
			if(c!=0)return c;
		}
		
		return 0;
	}

}
