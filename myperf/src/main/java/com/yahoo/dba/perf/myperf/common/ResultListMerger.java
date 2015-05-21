/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.List;
import java.util.logging.Logger;


public class ResultListMerger {
	private static Logger logger = Logger.getLogger(ResultListMerger.class.getName());

	public static ResultList mergeByKey(Sql sqlA, Sql sqlB, ResultList listA, ResultList listB, boolean diffOnly)
	{
		logger.info("Merge diffonly: "+diffOnly);
		if(listA==null)return listB;
		if(listB==null)return listA;
		
		//we will use A to construct column descriptor
		ColumnDescriptor desc = new ColumnDescriptor();
		ColumnDescriptor cmpSortDesc = new ColumnDescriptor();
		//ColumnDescriptor cmpDesc = new ColumnDescriptor();
		ColumnDescriptor descA = listA.getColumnDescriptor();
		ColumnDescriptor descB = listB.getColumnDescriptor();
		//1. Add all keys first
		int pos = 0;
		List<String> keyList = sqlA.getKeyList();
		List<Integer> keyAIdx = new java.util.ArrayList<Integer>(keyList.size());
		List<Integer> keyBIdx = new java.util.ArrayList<Integer>(keyList.size());
		List<String> compKeyList = new java.util.ArrayList<String>(keyList.size()+1);
		for(int i=0; i<keyList.size();i++)
		{
			ColumnInfo col = descA.getColumn(keyList.get(i)).copy();
			desc.addColumn(col.getName(), col.isNumberType(), pos++);
			keyAIdx.add(descA.getColumnIndex(col.getName()));
			keyBIdx.add(descB.getColumnIndex(col.getName()));
			cmpSortDesc.addColumn(col.getName(), col.isNumberType(), cmpSortDesc.getColumns().size());
			//cmpDesc.addColumn(col.getName(), col.isNumberType(), cmpDesc.getColumns().size());
			compKeyList.add(keyList.get(i));
		}
		compKeyList.add("#COMP");
		cmpSortDesc.addColumn("#COMP", false,cmpSortDesc.getColumns().size());
		//2. add all value
		List<String> valList = sqlA.getValueList();
		List<Integer> valAIdx = new java.util.ArrayList<Integer>(valList.size());
		List<Integer> valBIdx = new java.util.ArrayList<Integer>(valList.size());
		for(int i=0; i<valList.size();i++)
		{
			ColumnInfo col = descA.getColumn(valList.get(i)).copy();
			ColumnInfo colA = col.copy();
			colA.setName(col.getName()+"_A");
			colA.setPosition(pos++);
			desc.getColumns().add(colA);
			ColumnInfo colB = col.copy();
			colB.setName(col.getName()+"_B");
			colB.setPosition(pos++);
			desc.getColumns().add(colB);
			valAIdx.add(descA.getColumnIndex(col.getName()));
			valBIdx.add(descB.getColumnIndex(col.getName()));
			cmpSortDesc.addColumn(col.getName(), col.isNumberType(), cmpSortDesc.getColumns().size());
			//cmpDesc.addColumn(col.getName(), col.isNumberType(), cmpDesc.getColumns().size());
		}
		
		List<ResultRow> listRows = new java.util.ArrayList<ResultRow>(listA.getRows().size()+listB.getRows().size());
		for(ResultRow row: listA.getRows())
		{
			ResultRow newRow = new ResultRow();
			List<String> cols = new java.util.ArrayList<String>(row.getColumns().size()+1);
			//add key first
			for(int i=0;i<keyAIdx.size();i++)
			{
				int idx = keyAIdx.get(i);
				if(idx>=0)
					cols.add(row.getColumns().get(keyAIdx.get(i)));
				else cols.add(null);
			}
			cols.add("A");
			for(int i=0;i<valAIdx.size();i++)
			{
				int idx = valAIdx.get(i);
				if(idx>=0)
					cols.add(row.getColumns().get(valAIdx.get(i)));
				else cols.add(null);
			}
			newRow.setColumns(cols);
			listRows.add(newRow);
		}
		
		for(ResultRow row: listB.getRows())
		{
			ResultRow newRow = new ResultRow();
			List<String> cols = new java.util.ArrayList<String>(row.getColumns().size()+1);
			//add key first
			for(int i=0;i<keyBIdx.size();i++)
			{
				int idx = keyBIdx.get(i);
				if(idx>=0)
					cols.add(row.getColumns().get(keyBIdx.get(i)));
				else cols.add(null);
			}
			cols.add("B");
			for(int i=0;i<valBIdx.size();i++)
			{
				int idx = valBIdx.get(i);
				if(idx>=0)
					cols.add(row.getColumns().get(valBIdx.get(i)));
				else cols.add(null);
			}
			newRow.setColumns(cols);
			listRows.add(newRow);
		}

		//sort new list
		ResultRowKeyComparator sortcomp = new ResultRowKeyComparator(compKeyList,cmpSortDesc,cmpSortDesc );
		ResultRowKeyComparator comp = new ResultRowKeyComparator(keyList,cmpSortDesc,cmpSortDesc );
		ResultRowKeyComparator valComp = new ResultRowKeyComparator(valList,cmpSortDesc,cmpSortDesc );
		java.util.Collections.sort(listRows, sortcomp);
		
		//now start to contruct the final list
		ResultList finalList = new ResultList(Math.max(listA.getRows().size(), listB.getRows().size()));
		finalList.setColumnDescriptor(desc);
		int combinedSize = listRows.size();
		int keySize = keyList.size();
		if(combinedSize==0)return finalList;
		ResultRow curRow = listRows.get(0);
		String curComp = curRow.getColumns().get(keySize);
		int idx = 1;
		while(idx<=combinedSize)
		{
			ResultRow nextRow = idx<combinedSize?listRows.get(idx):null;	
			String nextComp = nextRow!=null?nextRow.getColumns().get(keySize):null;
			//System.out.println("comp: "+curComp+", "+nextComp);
			if(nextComp==null || curComp==nextComp||comp.compare(curRow, nextRow)!=0)//from same group, or next row does not have the same key
			{
				ResultRow row = new ResultRow();
				row.setColumnDescriptor(desc);
				List<String> cols = new java.util.ArrayList<String>(curRow.getColumns().size()-1);
				for(int i=0;i<curRow.getColumns().size();i++)
				{
					if(i==keySize)continue;
					else if(i<keySize)
						cols.add(curRow.getColumns().get(i));
					else if("A".equals(curComp))
					{
						cols.add(curRow.getColumns().get(i));
						cols.add(null);						
					}else if("B".equals(curComp))
					{
						cols.add(null);						
						cols.add(curRow.getColumns().get(i));						
					}
					
				}
				row.setColumns(cols);
				finalList.addRow(row);
				curComp = nextComp;
				curRow = nextRow;
				idx++;				
			}else //from diff group, with the same key
			{
				if(!diffOnly || valComp.compare(curRow, nextRow)!=0)
				{
					ResultRow row = new ResultRow();
					row.setColumnDescriptor(desc);
					List<String> cols = new java.util.ArrayList<String>(curRow.getColumns().size()-1);
					for(int i=0;i<curRow.getColumns().size();i++)
					{
						if(i==keySize)continue;
						else if(i<keySize)
							cols.add(curRow.getColumns().get(i));
						else if("A".equals(curComp))
						{
							cols.add(curRow.getColumns().get(i));
							cols.add(nextRow.getColumns().get(i));						
						}else if("B".equals(curComp))
						{
							cols.add(nextRow.getColumns().get(i));						
							cols.add(curRow.getColumns().get(i));						
						}
					
					}
					row.setColumns(cols);
					finalList.addRow(row);
				}
				if(idx==combinedSize-1)break;//at the end
				curRow = listRows.get(idx+1);//pre fetch one row
				curComp = curRow.getColumns().get(keySize);
				idx = idx + 2;
			}
		}
		
		return finalList;
	}
}
