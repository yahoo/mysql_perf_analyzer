/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class ResultListUtil {
	private static Logger logger = Logger.getLogger(ResultListUtil.class.getName());
	/**
	 * Construct a ResultList from SQL query ResultSet with at most maxCount rows.
	 * 
	 * @param rs
	 * @param maxCount If positive number, at most that number of records will be returned 
	 * @return
	 * @throws SQLException 
	 */
	public static ResultList fromSqlResultSet(ResultSet rs, int maxCount) throws SQLException
	{
		logger.fine(new java.util.Date()+": Process results ...");
		ResultList resList = new ResultList();
		if(rs==null)return resList;
		java.sql.ResultSetMetaData meta =  rs.getMetaData();
		int colCnt = meta.getColumnCount();
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.setColumns(new java.util.ArrayList<ColumnInfo>(colCnt));
		for(int i=1;i<=colCnt;i++)
		{
			//for now, we only record name
			ColumnInfo col = new ColumnInfo(meta.getColumnName(i));
			int sqlt = meta.getColumnType(i);
			if(sqlt==java.sql.Types.BIGINT||sqlt==java.sql.Types.DECIMAL||sqlt==java.sql.Types.DOUBLE
			||sqlt==java.sql.Types.FLOAT||sqlt==java.sql.Types.INTEGER||sqlt==java.sql.Types.NUMERIC
			||sqlt==java.sql.Types.TINYINT||sqlt==java.sql.Types.SMALLINT)
				col.setNumberType(true);
			col.setPosition(i);
			desc.getColumns().add(col);
		}
		resList.setColumnDescriptor(desc);
		int rowCnt = 0;
		List<ColumnInfo> cols = desc.getColumns();
		while(rs.next())
		{
			//logger.info(new java.util.Date()+": process "+rowCnt+" rows");
			ResultRow row = new ResultRow();
			row.setColumnDescriptor(desc);
			java.util.ArrayList<String> cols2 = new java.util.ArrayList<String>(colCnt);
			row.setColumns(cols2);
			for(int i=1;i<=colCnt;i++)
			{
				String val = rs.getString(i);
				
				if(cols.get(i-1).isNumberType()&&val!=null &&val.startsWith("."))
					val = "0"+val;//prefix Oracle float number with 0 if starting with "."
				else if(cols.get(i-1).isNumberType()&&val!=null &&val.startsWith("-."))
					val = val.replace("-.", "-0.");//prefix Oracle float number with 0 if starting with "."
					
				cols2.add(val);
			}
			resList.addRow(row);
			rowCnt++;
			if(maxCount>0 && rowCnt>=maxCount)break;
		}
		logger.fine(new java.util.Date()+": Process results done: "+resList.getRows().size());
		return resList;
	}

	/**
	 * This method will flat the rows to columns if Sql is configured that way
	 * @param sql
	 * @param rs
	 * @param maxCount
	 * @return
	 * @throws SQLException
	 */
	public static ResultList flatSqlResultSet(Sql sql, ResultSet rs, int maxCount) throws SQLException
	{
		if(sql.isExpandRow())
			return  expandSqlResultSet(sql, rs);
		else if(sql==null||sql.getFlatKey()==null||sql.getFlatKey().trim().length()==0 ||sql.getFlatValueList().size()==0)
		{
			return fromSqlResultSet(rs,maxCount);
		}
		
		ResultList resList = new ResultList();
		if(rs==null)return resList;
		java.sql.ResultSetMetaData meta =  rs.getMetaData();
		int colCnt = meta.getColumnCount();
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.setColumns(new java.util.ArrayList<ColumnInfo>(colCnt));

		Map<String, Integer> typeMap = new HashMap<String, Integer>();
		for(int i=1;i<=colCnt;i++)
		{
			typeMap.put(meta.getColumnName(i), meta.getColumnType(i));
		}
		Map<String, Integer> flatkeyIdx = new  HashMap<String, Integer>();
		for(int i=0;i<sql.getFlatValueList().size();i++)
		{
			//logger.info("Add "+sql.getFlatValueList().get(i));
			flatkeyIdx.put(sql.getFlatValueList().get(i),i);
		}
		int colIndex=1;
		//first all keys
		for(String k:sql.getKeyList())
		{
			ColumnInfo col = new ColumnInfo(k);
			col.setPosition(colIndex);
			int sqlt = typeMap.get(k);
			if(sqlt==java.sql.Types.BIGINT||sqlt==java.sql.Types.DECIMAL||sqlt==java.sql.Types.DOUBLE
			||sqlt==java.sql.Types.FLOAT||sqlt==java.sql.Types.INTEGER||sqlt==java.sql.Types.NUMERIC
			||sqlt==java.sql.Types.TINYINT||sqlt==java.sql.Types.SMALLINT)
				col.setNumberType(true);
			desc.getColumns().add(col);
			colIndex++;
		}
		//next all metrics
		for(String s: sql.getFlatValueList())
		{
			for(Map.Entry<String, String> e: sql.getMetrics().entrySet())
			{
				ColumnInfo col = new ColumnInfo(sql.getFlatValueAbbrMap().get(s)+e.getValue());
				col.setPosition(colIndex);
				int sqlt = typeMap.get(e.getKey());
				if(sqlt==java.sql.Types.BIGINT||sqlt==java.sql.Types.DECIMAL||sqlt==java.sql.Types.DOUBLE
				||sqlt==java.sql.Types.FLOAT||sqlt==java.sql.Types.INTEGER||sqlt==java.sql.Types.NUMERIC
				||sqlt==java.sql.Types.TINYINT||sqlt==java.sql.Types.SMALLINT)
					col.setNumberType(true);
				desc.getColumns().add(col);
				colIndex++;	
			}
		}
		
		resList.setColumnDescriptor(desc);
		int rowCnt = 0;
		List<ColumnInfo> cols = desc.getColumns();
		String[] prevkeys = new String[sql.getKeyList().size()];
		ResultRow row = null;
		while(rs.next())
		{
			String[] newkeys = new String[sql.getKeyList().size()];
			for(int i=0;i<sql.getKeyList().size();++i)
			{
				newkeys[i] = rs.getString(sql.getKeyList().get(i));
			}
			if(!isSame(prevkeys, newkeys))//start a new row
			{
				row = new ResultRow();
				row.setColumnDescriptor(desc);
				row.setColumns(new java.util.ArrayList<String>(cols.size()));
				//initialize it
				for(int i=0;i<cols.size();i++)
					row.getColumns().add("");
				resList.addRow(row);
				rowCnt++;
				for(int i=0;i<sql.getKeyList().size();++i)
				{
					String val = newkeys[i];
					
					if(cols.get(i).isNumberType()&&val!=null &&val.startsWith("."))
						val = "0"+val;//prefix Oracle float number with 0 if starting with "."						
					else if(cols.get(i).isNumberType()&&val!=null &&val.startsWith("-."))
						val = val.replace("-.", "-0.");//prefix Oracle float number with 0 if starting with "."						
					row.getColumns().set(i,val);
				}
				
			}
			//now we need get the flat record
			String flatVal = rs.getString(sql.getFlatKey());
			//check its index
			int idx = flatkeyIdx.get(flatVal.toUpperCase());
			int mi = 0;
			for(String s:sql.getMetrics().keySet())
			{
				String val = rs.getString(s);
				int colIdx = sql.getKeyList().size()+idx*sql.getMetrics().size()+mi;
				if(cols.get(colIdx).isNumberType()&&val!=null &&val.startsWith("."))
					val = "0"+val;//prefix Oracle float number with 0 if starting with "."
				else if(cols.get(colIdx).isNumberType()&&val!=null &&val.startsWith("-."))
					val = val.replace("-.", "-0.");//prefix Oracle float number with 0 if starting with "."
				
				row.getColumns().set(colIdx,val);
				mi++;				
			}
			prevkeys = newkeys;
			if(maxCount>0 && rowCnt>=maxCount)break;
		}
		return resList;
	}

	/**
	 * Expand a single row to multiple rows. One row per column.
	 * @param sql
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static ResultList expandSqlResultSet(Sql sql, ResultSet rs) throws SQLException
	{
		ResultList resList = new ResultList();
		if(rs==null)return resList;
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("NAME", false, 1);
		desc.addColumn("VALUE", false, 2);
		resList.setColumnDescriptor(desc);
		
		java.sql.ResultSetMetaData meta =  rs.getMetaData();
		int colCnt = meta.getColumnCount();
		
		int rowCnt = 0;
		while(rs.next())//change to allow to expand multiple rows, with a row number separator and a row count suffix
		{
			if(rowCnt>0)
			{
				ResultRow row = new ResultRow();
				row.addColumn("--- ROW "+(rowCnt+1)+"---");
				row.addColumn("");
				resList.addRow(row);
			}
			for(int i=1;i<=colCnt;i++)
			{
				ResultRow row = new ResultRow();
				if(rowCnt>0)
					row.addColumn(meta.getColumnName(i)+"("+(rowCnt+1)+")");
				else
					row.addColumn(meta.getColumnName(i));

				row.addColumn(rs.getString(i));
				resList.addRow(row);
			}
			rowCnt++;			
		}
		return resList;
	}

	
	public static ResultList fakeResultListData()
	{
		ResultList resList = new ResultList();
		ColumnInfo col1 = new ColumnInfo("TEST1");
		col1.setPosition(1);
		ColumnInfo col2 = new ColumnInfo("TEST2");
		col2.setPosition(2);
		java.util.ArrayList<ColumnInfo> colInfoList = new java.util.ArrayList<ColumnInfo>(2);
		colInfoList.add(col1);colInfoList.add(col2);
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.setColumns(colInfoList);
		resList.setColumnDescriptor(desc);
		ResultRow row1 = new ResultRow();
		row1.setColumnDescriptor(desc);
		java.util.ArrayList<String> cols = new java.util.ArrayList<String>();
		cols.add("GOOD");cols.add("BAD");
		row1.setColumns(cols);
		ResultRow row2 = new ResultRow();
		cols = new java.util.ArrayList<String>();
		cols.add("LONG");cols.add("SHORT");
		row2.setColumns(cols);
		resList.getRows().add(row1);
		resList.getRows().add(row2);
		return resList;
	}
	
	public static String toMultiListJSONString(LinkedHashMap<String, ResultList> resultMap, QueryParameters qps, int status, String message)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{\"resp\":{\"status\":").append(status);//start and status
		sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");//message line
		//requests
		if(qps!=null)
		{
			sb.append(",\"request\":{");
			sb.append("\"group\":\"").append(qps.getGroup()).append("\"");
			sb.append(",\"host\":\"").append(qps.getHost()).append("\"");
			for(Map.Entry<String, String> e: qps.getSqlParams().entrySet())
			{
				sb.append(",\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
			sb.append("}");
		}
		//for(String key: resultMap.keySet())
		for(Map.Entry<String, ResultList> e: resultMap.entrySet())
		{
			String key = e.getKey();
			ResultList rs = e.getValue();
			List<ColumnInfo> cols = rs.getColumnDescriptor().getColumns();

			sb.append(",\"").append(key).append("\":{");
			sb.append("\"total\":\"").append(rs.getRows().size()).append("\",");
			//column names
			sb.append("\"columns\":[");
			{
				boolean first = true;
				for(int i=0;i<cols.size();i++)
				{
					if(!first)sb.append(",");
					first = false;
					sb.append("\"").append(cols.get(i).getName()+"\"");
				}
			}
			sb.append("],");
			boolean firstRow = true;
			sb.append("\"results\":[");
			for(ResultRow row:rs.getRows())
			{
				if(!firstRow)sb.append(",");
				int len = row.getColumns().size();
				boolean first = true;
				sb.append("{");
				for(int i=0;i<len;i++)
				{
					if(!first)sb.append(",");
					sb.append("\"").append(escapeJson(cols.get(i).getName())).append("\":");
					if(cols.get(i).isNumberType())
					{
						if(row.getColumns().get(i)==null||row.getColumns().get(i).trim().length()==0)
							sb.append("\"\"");
						else	
							sb.append(escapeJson(row.getColumns().get(i)));
					}
					else
						sb.append("\"").append(escapeJson(row.getColumns().get(i))).append("\"");
					first = false;
				}
				sb.append("}");
				firstRow = false;
			}
			sb.append("]}");
		}
		sb.append("}}");

		return sb.toString();
	}
	public static String toJSONString(ResultList rs, QueryParameters qps, int status, String message)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{\"resp\":{\"status\":").append(status);//start and status
		if(rs!=null && rs.getTotalResponseTime()>0)
		{
		  sb.append(",\"totalTime\":\"").append(rs.getTotalResponseTime()).append("ms\"");
		  sb.append(",\"execTime\":\"").append(rs.getTotalExecutionTime()).append("ms\"");
		  sb.append(",\"fetchTime\":\"").append(rs.getTotalFetchTime()).append("ms\"");
		}
		sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");//message line
		
		//requests
		if(qps!=null)
		{
			sb.append(",\"request\":{");
			sb.append("\"group\":\"").append(qps.getGroup()).append("\"");
			sb.append(",\"host\":\"").append(qps.getHost()).append("\"");
			//sb.append(",\"port\":\"").append(qps.getPort()).append("\"");
			sb.append(",\"sql\":\"").append(qps.getSql()).append("\"");
			for(Map.Entry<String, String> e: qps.getSqlParams().entrySet())
			{
				sb.append(",\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
			sb.append("}\r\n");
		}
		
		if(rs!=null)
		{
			List<ColumnInfo> cols = rs.getColumnDescriptor().getColumns();

			sb.append(",\"results\":{");
			sb.append("\"total\":\"").append(rs.getRows().size()).append("\",");
			//column names
			sb.append("\"columns\":[");
			{
				boolean first = true;
				for(int i=0;i<cols.size();i++)
				{
					if(!first)sb.append(",");
					first = false;
					sb.append("\"").append(cols.get(i).getName().toUpperCase()+"\"");
				}
			}
			sb.append("],\r\n");
			if(rs.getCustomObjects()!=null && rs.getCustomObjects().size()>0)
			{
				for(Map.Entry<String, CustomResultObject> e: rs.getCustomObjects().entrySet())
				{
					sb.append("\"").append(e.getKey()).append("\":");
					sb.append(e.getValue().getValueJsonString());
					sb.append(",\r\n");
				}
			}
			boolean firstRow = true;
			sb.append("\"results\":[");
			for(ResultRow row:rs.getRows())
			{
				if(!firstRow)sb.append(",");
				int len = row.getColumns().size();
				boolean first = true;
				sb.append("{");
				for(int i=0;i<len;i++)
				{
					if(!first)sb.append(",");
					sb.append("\"").append(escapeJson(cols.get(i).getName().toUpperCase())).append("\":");
					if(cols.get(i).isNumberType())
					{
						if(row.getColumns().get(i)==null||row.getColumns().get(i).trim().length()==0)
							sb.append("\"\"");
						else	
							sb.append(escapeJson(row.getColumns().get(i)));
					}
					else
						sb.append("\"").append(escapeJson(row.getColumns().get(i))).append("\"");
					first = false;
				}
				sb.append("}\r\n");
				firstRow = false;
			}
			sb.append("]}");
		}
		sb.append("}}");
		return sb.toString();
	}
	/**
	 * Retrieve distinct values from keyColumn, to be used to regroup the metrics
	 * when forming json output
	 * @param keyColumn
	 * @param rlist
     * @return
	 */
	public static String[] getDistinctKeys(String keyColumn, ResultList rlist)
	  {
		  if(rlist == null || rlist.getRows().size() == 0
				  || keyColumn == null || keyColumn.isEmpty()
				  || rlist.getColumnIndex(keyColumn) <0)
			  return null;
		  
		  int idx = rlist.getColumnIndex(keyColumn);
		  int rows = rlist.getRows().size();
		  Map<String, Integer> keys = new TreeMap<String, Integer>();
		  for(int i=0; i<rows; i++)
			  keys.put(rlist.getRows().get(i).getColumns().get(idx), 1);
		  
		  return keys.keySet().toArray(new String[0]);	  
	  }

	/**
	 * JSON Output metrics with multiple rows, with each row represent different entity such as disk, username, table anme, etc.
	 * @param rs
	 * @param keyColumn
	 * @param groupByColumns columns used to group metrics, such as ["SNAP_ID", "TS"]
	 * @param metricName we only handle a single metric for now
	 * @param qps
	 * @param status
	 * @param message
	 * @return
	 */
	public static String toMetricsJSONStringWithMultiRowsKeys(ResultList rs, String keyColumn, String[] groupByColumns, String metricName, QueryParameters qps, int status, String message)
	{
		String[] keys = getDistinctKeys(keyColumn, rs);
		if(keys == null || keys.length ==0 || groupByColumns == null || groupByColumns.length == 0) //we don't want to bother about the case without multiple key values
			return toJSONString(rs, qps, status, message);
		
		Map<String, String> keyMap = new HashMap<String, String>(keys.length);
		for(int i = 0; i < keys.length; i++)
			keyMap.put(keys[i],  "k"+i);
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\"resp\":{\"status\":").append(status);//start and status
		if(rs!=null && rs.getTotalResponseTime()>0)
		{
		  sb.append(",\"totalTime\":\"").append(rs.getTotalResponseTime()).append("ms\"");
		  sb.append(",\"execTime\":\"").append(rs.getTotalExecutionTime()).append("ms\"");
		  sb.append(",\"fetchTime\":\"").append(rs.getTotalFetchTime()).append("ms\"");
		}
		sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");//message line
		
		//requests
		if(qps!=null)
		{
			sb.append(",\"request\":{");
			sb.append("\"group\":\"").append(qps.getGroup()).append("\"");
			sb.append(",\"host\":\"").append(qps.getHost()).append("\"");
			//sb.append(",\"port\":\"").append(qps.getPort()).append("\"");
			sb.append(",\"sql\":\"").append(qps.getSql()).append("\"");
			for(Map.Entry<String, String> e: qps.getSqlParams().entrySet())
			{
				sb.append(",\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
			sb.append("}\r\n");
		}
		
		if(rs!=null)
		{
			List<ColumnInfo> cols = rs.getColumnDescriptor().getColumns();

			sb.append(",\"results\":{");
			sb.append("\"total\":\"").append(rs.getRows().size()).append("\",");
			//column names
			sb.append("\"columns\":[");
			{
				boolean first = true;
				for(int i=0;i<cols.size();i++)
				{
					//ignore key column
					if(keyColumn.equalsIgnoreCase(cols.get(i).getName()))continue;
					
					if(!first)sb.append(",");
					first = false;
					sb.append("\"").append(cols.get(i).getName()+"\"");
				}
			}
			sb.append("],\r\n");
			//add keys
			sb.append("\"keys\":[");
			for(int i=0; i<keys.length; i++)
			{
				if(i>0)sb.append(",");
				sb.append("{")
				  .append("\"name\":\"").append(keys[i]).append("\", ")
				  .append("\"shortName\":\"").append("k").append(i).append("\"")
				  .append("}");
			}
			sb.append("],\r\n");
			if(rs.getCustomObjects()!=null && rs.getCustomObjects().size()>0)
			{
				for(Map.Entry<String, CustomResultObject> e: rs.getCustomObjects().entrySet())
				{
					sb.append("\"").append(e.getKey()).append("\":");
					sb.append(e.getValue().getValueJsonString());
					sb.append(",\r\n");
				}
			}
			boolean firstRow = true;
			String[] prevGrpKey = new String[groupByColumns.length];
			String[] newGrpKey = new String[groupByColumns.length];
			int keyIdx = rs.getColumnIndex(keyColumn);
			int mtrIdx = rs.getColumnIndex(metricName);
			int[] grpKeyIdx = new int[groupByColumns.length];
			for(int i=0; i<groupByColumns.length; i++)
			{
				prevGrpKey[i] = null;
				newGrpKey[i] = null;
				grpKeyIdx[i] = rs.getColumnIndex(groupByColumns[i]);//don't expect invalid
			}
			sb.append("\"results\":[");
			Map<String, String> valueMap = new TreeMap<String, String>();//to store values temporarily
			for(ResultRow row:rs.getRows())
			{
				for(int i=0; i<groupByColumns.length; i++)
				{
					newGrpKey[i] = row.getColumns().get(grpKeyIdx[i]);
				}
				//if we have a new group by key, we should generate output for previous one
				// and clear the temp storage
				if(valueMap.size() > 0 && isDiff(prevGrpKey, newGrpKey))
				{
					if(!firstRow)sb.append(",");
					firstRow = false;
					sb.append("{");
					for(int i=0; i<groupByColumns.length; i++)
					{
						sb.append("\"").append(groupByColumns[i]).append("\":").append(prevGrpKey[i]).append(",");
					}
					sb.append("\"").append(metricName).append("\":{");
					boolean firstKey = true;
					for(Map.Entry<String, String> e: valueMap.entrySet())
					{
						if(e.getValue()==null)continue;
						String jkey = keyMap.get(e.getKey());
						if(!firstKey)sb.append(",");
						firstKey = false;
						sb.append("\"").append(jkey).append("\":").append(e.getValue());
					}
					sb.append("}");//end metric
					sb.append("}");//end the row
					valueMap.clear();
				} 
				//otherwise, just store it
				valueMap.put(row.getColumns().get(keyIdx), row.getColumns().get(mtrIdx));				
				for(int i=0; i<groupByColumns.length; i++)
				{
					prevGrpKey[i] = newGrpKey[i];
				}
			}
			//todo: output last group
			if(!firstRow)sb.append(",");
			{
				sb.append("{");
				for(int i=0; i<groupByColumns.length; i++)
				{
					sb.append("\"").append(groupByColumns[i]).append("\":").append(newGrpKey[i]).append(",");
				}
				sb.append("\"").append(metricName).append("\":{");
				boolean firstKey = true;
				for(Map.Entry<String, String> e: valueMap.entrySet())
				{
					if(e.getValue()==null)continue;
					String jkey = keyMap.get(e.getKey());
					if(!firstKey)sb.append(",");
					firstKey = false;
					sb.append("\"").append(jkey).append("\":").append(e.getValue());
				}
				sb.append("}");//end metric
				sb.append("}");//end the row		
			}
			sb.append("]}");
		}
		sb.append("}}");
		return sb.toString();
	}
	
	//compare tow arrays if the same
	public static boolean isDiff(String[] arr1, String[] arr2)
	{
		if(arr1 == null && arr2 == null)return false;
		if(arr1 == null || arr2 == null)return true;
		if(arr1.length != arr2.length) return true;
		for(int i=0; i<arr1.length; i++)
		{
			if(arr1[i] != null && !arr1[i].equals(arr2[i]))
				return true;
			if(arr2[i] != null && !arr2[i].equals(arr1[i]))
				return true;
		}
		return false;
	}
	/**
	 * We will filter data by the provided columns. Also make all column names in uppercase.
	 * If no filter provided, only make column names uppercase
	 * @param rs
	 * @param qps
	 * @param status
	 * @param message
	 * @param subset
	 * @return
	 */
	public static String toJSONStringSubset(ResultList rs, QueryParameters qps, int status, String message, String[] subset)
	{
		HashSet<String> ms = new HashSet<String>();
		if(subset!=null)
		for(int i=0;i<subset.length;i++)
			ms.add(subset[i]);
		boolean filter = ms.size()>0;
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\"resp\":{\"status\":").append(status);//start and status
		if(rs!=null && rs.getTotalResponseTime()>0)
		{
		  sb.append(",\"totalTime\":\"").append(rs.getTotalResponseTime()).append("ms\"");
		  sb.append(",\"execTime\":\"").append(rs.getTotalExecutionTime()).append("ms\"");
		  sb.append(",\"fetchTime\":\"").append(rs.getTotalFetchTime()).append("ms\"");
		}
		sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");//message line
		
		//requests
		if(qps!=null)
		{
			sb.append(",\"request\":{");
			sb.append("\"group\":\"").append(qps.getGroup()).append("\"");
			sb.append(",\"host\":\"").append(qps.getHost()).append("\"");
			//sb.append(",\"port\":\"").append(qps.getPort()).append("\"");
			sb.append(",\"sql\":\"").append(qps.getSql()).append("\"");
			for(Map.Entry<String, String> e: qps.getSqlParams().entrySet())
			{
				sb.append(",\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
			sb.append("}\r\n");
		}
		
		if(rs!=null)
		{
			List<ColumnInfo> cols = rs.getColumnDescriptor().getColumns();

			sb.append(",\"results\":{");
			sb.append("\"total\":\"").append(rs.getRows().size()).append("\",");
			//column names
			sb.append("\"columns\":[");
			{
				boolean first = true;
				for(int i=0;i<cols.size();i++)
				{
					if(filter && !ms.contains(cols.get(i).getName()))
					  continue;
					if(!first)sb.append(",");
					first = false;
					sb.append("\"").append(cols.get(i).getName().toUpperCase()+"\"");
				}
			}
			sb.append("],\r\n");
			boolean firstRow = true;
			sb.append("\"results\":[");
			for(ResultRow row:rs.getRows())
			{
				if(!firstRow)sb.append(",");
				int len = row.getColumns().size();
				boolean first = true;
				sb.append("{");
				for(int i=0;i<len;i++)
				{
					if(filter && !ms.contains(cols.get(i).getName()))
					  continue;
					if(!first)sb.append(",");
					sb.append("\"").append(escapeJson(cols.get(i).getName().toUpperCase())).append("\":");
					if(cols.get(i).isNumberType())
					{
						if(row.getColumns().get(i)==null||row.getColumns().get(i).trim().length()==0)
							sb.append("\"\"");
						else	
							sb.append(escapeJson(row.getColumns().get(i)));
					}
					else
						sb.append("\"").append(escapeJson(row.getColumns().get(i))).append("\"");
					first = false;
				}
				sb.append("}\r\n");
				firstRow = false;
			}
			sb.append("]}");
		}
		sb.append("}}");
		return sb.toString();
	}
	public static String toJSONDiffListString(ResultList rsA, ResultList rsB,QueryParameters qpsA, QueryParameters qpsB,int status, String message)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{\"resp\":{\"status\":").append(status);//start and status
		sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");//message line
		
		//requests
		sb.append(",\"request\":{");
		if(qpsA!=null)
		{
			sb.append("\"a_group\":\"").append(qpsA.getGroup()).append("\"");
			sb.append(",\"a_host\":\"").append(qpsA.getHost()).append("\"");
			//sb.append(",\"port\":\"").append(qps.getPort()).append("\"");
			sb.append(",\"a_sql\":\"").append(qpsA.getSql()).append("\"");
			for(Map.Entry<String, String> e: qpsA.getSqlParams().entrySet())
			{
				sb.append(",\"a_").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
			if(qpsB!=null)sb.append(",");
		}
		if(qpsB!=null)
		{
			sb.append("\"b_group\":\"").append(qpsB.getGroup()).append("\"");
			sb.append(",\"b_host\":\"").append(qpsB.getHost()).append("\"");
			//sb.append(",\"port\":\"").append(qps.getPort()).append("\"");
			sb.append(",\"b_sql\":\"").append(qpsB.getSql()).append("\"");
			for(Map.Entry<String, String> e: qpsB.getSqlParams().entrySet())
			{
				sb.append(",\"b_").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
		}
		sb.append("}\r\n");

		int cnt = 0;
		List<ColumnInfo> cols = null;
		if(rsA!=null||rsB!=null)
		{
			if(rsA!=null)
			{
				cnt += rsA.getRows().size();
				cols = rsA.getColumnDescriptor().getColumns();
			}
			if(rsB!=null)
			{
				cnt += rsB.getRows().size();
				if(cols==null)
					cols = rsB.getColumnDescriptor().getColumns();
			}
			
			sb.append(",\"results\":{");
			sb.append("\"total\":\"").append(cnt).append("\",");
			//column names
			sb.append("\"columns\":[\"DB\"");
			{
				for(int i=0;i<cols.size();i++)
				{
					sb.append(",");
					sb.append("\"").append(cols.get(i).getName()+"\"");
				}
			}
			sb.append("],\r\n");
			sb.append("\"results\":[");
		}
		
		boolean firstRow = true;
		if(rsA!=null)
		{
			for(ResultRow row:rsA.getRows())
			{
				if(!firstRow)sb.append(",");
				int len = row.getColumns().size();
				sb.append("{\"DB\":\"A\"");
				for(int i=0;i<len;i++)
				{
					sb.append(",");
					sb.append("\"").append(escapeJson(cols.get(i).getName())).append("\":");
					if(cols.get(i).isNumberType())
					{
						if(row.getColumns().get(i)==null||row.getColumns().get(i).trim().length()==0)
							sb.append("\"\"");
						else	
							sb.append(escapeJson(row.getColumns().get(i)));
					}
					else
						sb.append("\"").append(escapeJson(row.getColumns().get(i))).append("\"");
				}
				sb.append("}\r\n");
				firstRow = false;
			}
		}
		if(rsB!=null)
		{
			for(ResultRow row:rsB.getRows())
			{
				if(!firstRow)sb.append(",");
				int len = row.getColumns().size();
				sb.append("{\"DB\":\"B\"");
				for(int i=0;i<len;i++)
				{
					sb.append(",");
					sb.append("\"").append(escapeJson(cols.get(i).getName())).append("\":");
					if(cols.get(i).isNumberType())
					{
						if(row.getColumns().get(i)==null||row.getColumns().get(i).trim().length()==0)
							sb.append("\"\"");
						else	
							sb.append(escapeJson(row.getColumns().get(i)));
					}
					else
						sb.append("\"").append(escapeJson(row.getColumns().get(i))).append("\"");
				}
				sb.append("}\r\n");
				firstRow = false;
			}
		}
		if(rsA!=null||rsB!=null)
			sb.append("]}");
		sb.append("}}");
		return sb.toString();
	}
	
	/**
	 * Display diff data by key. Metrics will be displayed as {metric}_a and {metric}_b
	 * @param rsA
	 * @param rsB
	 * @param qpsA
	 * @param qpsB
	 * @param status
	 * @param message
	 * @return
	 */
	public static String toJSONDiffFlatString(boolean diffOnly, Sql sqlA,Sql sqlB,ResultList rsA, ResultList rsB,QueryParameters qpsA, QueryParameters qpsB,int status, String message)
	{
		ResultList combined = ResultListMerger.mergeByKey(sqlA, sqlB, rsA, rsB, diffOnly);
		StringBuilder sb = new StringBuilder();
		sb.append("{\"resp\":{\"status\":").append(status);//start and status
		sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");//message line
		
		//requests
		sb.append(",\"request\":{");
		if(qpsA!=null)//parameter from a
		{
			sb.append("\"a_group\":\"").append(qpsA.getGroup()).append("\"");
			sb.append(",\"a_host\":\"").append(qpsA.getHost()).append("\"");
			//sb.append(",\"port\":\"").append(qps.getPort()).append("\"");
			sb.append(",\"a_sql\":\"").append(qpsA.getSql()).append("\"");
			for(Map.Entry<String, String> e: qpsA.getSqlParams().entrySet())
			{
				sb.append(",\"a_").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
			if(qpsB!=null)sb.append(",");
		}
		if(qpsB!=null)//parameter from b
		{
			sb.append("\"b_group\":\"").append(qpsB.getGroup()).append("\"");
			sb.append(",\"b_host\":\"").append(qpsB.getHost()).append("\"");
			//sb.append(",\"port\":\"").append(qps.getPort()).append("\"");
			sb.append(",\"b_sql\":\"").append(qpsB.getSql()).append("\"");
			for(Map.Entry<String, String> e: qpsB.getSqlParams().entrySet())
			{
				sb.append(",\"b_").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
		}
		sb.append("}\r\n");//end of request

		int cnt = combined!=null?combined.getRows().size():0;
		List<ColumnInfo> cols = combined!=null?combined.getColumnDescriptor().getColumns():null;
		
		sb.append(",\"results\":{");
		sb.append("\"total\":\"").append(cnt).append("\",");//end of count
		//column names
		sb.append("\"columns\":[");//start of column names
		if(cols!=null)
		{
			boolean first = true;
			for(int i=0;i<cols.size();i++)
			{
				if(!first)
					sb.append(",");
				else first = false;
				sb.append("\"").append(cols.get(i).getName()+"\"");
			}
		}
		sb.append("],\r\n");//end of column names
		boolean firstRow = true;
		sb.append("\"results\":[");//start of the result list
		if(combined!=null)
		for(ResultRow row:combined.getRows())
		{
			if(!firstRow)sb.append(",");
			int len = row.getColumns().size();
			boolean first = true;
			sb.append("{");//start of one row
			for(int i=0;i<len;i++)
			{
				if(!first)sb.append(",");
				sb.append("\"").append(escapeJson(cols.get(i).getName())).append("\":");
				if(cols.get(i).isNumberType())
				{
					if(row.getColumns().get(i)==null||row.getColumns().get(i).trim().length()==0)
						sb.append("\"\"");
					else
					{
						if(row.getColumns().get(i).startsWith("-."))
							sb.append(row.getColumns().get(i).replace("-.", "-0."));
						else
							sb.append(escapeJson(row.getColumns().get(i)));
					}
				}
				else
					sb.append("\"").append(escapeJson(row.getColumns().get(i))).append("\"");
				first = false;
			}
			sb.append("}\r\n");//end of one row
			firstRow = false;
		}
		sb.append("]");//end of result list
		sb.append("}");//end of outer results
		sb.append("}");//end of resp
		sb.append("}");//end of all

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
		
	private static boolean isSame(String[] s1, String[] s2)
	{
		if(s1==null&&s2==null)return true;
		else if(s1==null&&s2!=null)return false;
		else if(s1!=null && s2==null)return false;
		else if(s1.length!=s2.length)return false;
		for(int i=0;i<s1.length;i++)
		{
			String ss1 = s1[i];
			String ss2 = s2[i];
			if(ss1==null&&ss2==null)continue;
			if(ss1==null&&ss2!=null)return false;
			if(ss1!=null && ss2==null)return false;
			if(!ss1.equalsIgnoreCase(ss2))return false;
		}
		return true;
	}
	/**
	 * Used for text output
	 * @param rList
	 */	
	public static void updateColumnWidth(ResultList rList)	
	{
		if(rList==null||rList.getColumnDescriptor()==null||rList.getRows().size()==0)return;
		List<ColumnInfo> cols = rList.getColumnDescriptor().getColumns();
		for(ColumnInfo col:cols)
		{
			col.setMaxLength(col.getName().length());
		}
		for(ResultRow row: rList.getRows())
		{
			List<String> l = row.getColumns();
			for(int i=0;i<l.size();i++)
			{
				String s = l.get(i);
				if(s!=null && s.length()>cols.get(i).getMaxLength())cols.get(i).setMaxLength(s.length());
			}
		}
	}

	public static String paddingOrTrim(String s, int len)
	{
		StringBuilder sb = new StringBuilder();
		if(s!=null && s.length()>len)
		{
			sb.append(s.substring(0, len));
		}else
		{
			int l = 0;
			if(s!=null){sb.append(s);l=s.length();}
			while(l<len)
			{
				sb.append(' ');l++;
			}
		}
		return sb.toString();
	}
	
	private static class BindVariable
	{
		//<binds><bind name=":1" pos="1" dty="1" dtystr="VARCHAR2(2000)" maxlen="2000" csid="873" len="15">Billing Contact</bind></binds>
		public String name;
		public String pos;
		public String dtystr;
		public String maxLen;
		public String len;
		public String value;
		
		public BindVariable()
		{
			
		}
	}

	
	private static List<BindVariable> parseBinds(String binds, XMLInputFactory inputFactory)
	{
		if(binds==null)return null;
		List<BindVariable> vars = new java.util.ArrayList<BindVariable>();
		//<binds><bind name=":1" pos="1" dty="1" dtystr="VARCHAR2(2000)" maxlen="2000" csid="873" len="15">Billing Contact</bind></binds>
		
		java.io.CharArrayReader in = new java.io.CharArrayReader(binds.toCharArray());
		XMLStreamReader reader = null;
		try
		{
			reader = inputFactory.createXMLStreamReader(in);
			while(reader.hasNext())
			{
				//loop till one sql tag is found
				int evtType = reader.next();
				if(evtType!=XMLStreamConstants.START_ELEMENT)continue;
				String tagName = reader.getLocalName();
				if(!"bind".equals(tagName))continue;
				BindVariable var = new BindVariable();
				int attrCount = reader.getAttributeCount();
				for(int i=0;i<attrCount;i++)
				{
					String attrName = reader.getAttributeLocalName(i);
					String attrValue = reader.getAttributeValue(i);
					if("name".equals(attrName))
						var.name = attrValue;
					else if("pos".equals(attrName))
						var.pos = attrValue;
					else if("dtystr".equalsIgnoreCase(attrName))
						var.dtystr = attrValue;
					else if("maxlen".equalsIgnoreCase(attrName))
					{
						var.maxLen = attrValue;
					}else if("len".equalsIgnoreCase(attrName))
					{
						var.len = attrValue;
					}
				}
				var.value = reader.getElementText();
				vars.add(var);
		    }				
		}catch(Exception ex)
		{
			
		}
		finally
		{
			if(reader!=null)try{reader.close(); reader=null;}catch(Exception iex){}
		}

		return vars;
	}

	public static ResultList paserBindList(ResultList rList)
	{
		if(rList!=null&&rList.getRows().size()>0)
		{
			logger.fine("BindController returns "+rList.getRows().size());
			ResultList newList = new ResultList();
			ColumnDescriptor desc = new ColumnDescriptor();
			for(ColumnInfo col: rList.getColumnDescriptor().getColumns())
			{
				if("binds_xml".equalsIgnoreCase(col.getName()))continue;
				desc.addColumn(col.getName(), col.isNumberType(), desc.getColumns().size());
			}
			desc.addColumn("NAME", false, desc.getColumns().size());
			desc.addColumn("POS", false, desc.getColumns().size());
			desc.addColumn("DTYSTR", false, desc.getColumns().size());
			desc.addColumn("MAXLEN", false, desc.getColumns().size());
			desc.addColumn("LEN", false, desc.getColumns().size());
			desc.addColumn("VALUE", false, desc.getColumns().size());
			newList.setColumnDescriptor(desc);
			
			int idx = rList.getColumnDescriptor().getColumnIndex("BINDS_XML");
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			for(ResultRow row: rList.getRows())
			{
				//logger.info("binds_xml.idx="+idx+", "+ row.getColumns().size());
				List<BindVariable> binds = null;
				try
				{
					String bindXml = row.getColumns().get(idx);
					binds = parseBinds(bindXml, inputFactory);
				}catch(Exception iex){}
				if(binds==null||binds.size()==0)
				{
					ResultRow newRow = new ResultRow();
					newRow.setColumnDescriptor(desc);
					List<String> cols = new java.util.ArrayList<String>(row.getColumns().size()+5);
					for(int i=0;i<row.getColumns().size();i++)
					{
						if(i==idx)continue;
						cols.add(row.getColumns().get(i));
					}
					cols.add(null);cols.add(null);cols.add(null);
					cols.add(null);cols.add(null);cols.add(null);
					newRow.setColumns(cols);
					newList.addRow(newRow);
				}else
				{
					for(BindVariable var:binds)
					{
						ResultRow newRow = new ResultRow();
						newRow.setColumnDescriptor(desc);
						List<String> cols = new java.util.ArrayList<String>(row.getColumns().size()+5);
						for(int i=0;i<row.getColumns().size();i++)
						{
							if(i==idx)continue;
							cols.add(row.getColumns().get(i));
						}
						cols.add(var.name);cols.add(var.pos);cols.add(var.dtystr);
						cols.add(var.maxLen);cols.add(var.len);cols.add(var.value);
						newRow.setColumns(cols);
						newList.addRow(newRow);									
					}
				}
			}
			return newList;
		}
		return null;
	}
	
	public static String toMultiListJSONStringUpper(LinkedHashMap<String, ResultList> resultMap, QueryParameters qps, int status, String message)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{\"resp\":{\"status\":").append(status);//start and status
		sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");//message line
		//requests
		if(qps!=null)
		{
			sb.append(",\"request\":{");
			sb.append("\"group\":\"").append(qps.getGroup()).append("\"");
			sb.append(",\"host\":\"").append(qps.getHost()).append("\"");
			for(Map.Entry<String, String> e: qps.getSqlParams().entrySet())
			{
				sb.append(",\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
			}
			sb.append("}");
		}
		//for(String key: resultMap.keySet())
		for(Map.Entry<String, ResultList> e: resultMap.entrySet())
		{
			String key = e.getKey();
			ResultList rs = e.getValue();
			List<ColumnInfo> cols = rs.getColumnDescriptor().getColumns();

			sb.append(",\"").append(key).append("\":{");
			sb.append("\"total\":\"").append(rs.getRows().size()).append("\",");
			//column names
			sb.append("\"columns\":[");
			{
				boolean first = true;
				for(int i=0;i<cols.size();i++)
				{
					if(!first)sb.append(",");
					first = false;
					sb.append("\"").append(cols.get(i).getName().toUpperCase()+"\"");
				}
			}
			sb.append("],");
			boolean firstRow = true;
			sb.append("\"results\":[");
			for(ResultRow row:rs.getRows())
			{
				if(!firstRow)sb.append(",");
				int len = row.getColumns().size();
				boolean first = true;
				sb.append("{");
				for(int i=0;i<len;i++)
				{
					if(!first)sb.append(",");
					sb.append("\"").append(escapeJson(cols.get(i).getName().toUpperCase())).append("\":");
					if(cols.get(i).isNumberType())
					{
						if(row.getColumns().get(i)==null||row.getColumns().get(i).trim().length()==0)
							sb.append("\"\"");
						else	
							sb.append(escapeJson(row.getColumns().get(i)));
					}
					else
						sb.append("\"").append(escapeJson(row.getColumns().get(i))).append("\"");
					first = false;
				}
				sb.append("}");
				firstRow = false;
			}
			sb.append("]}");
		}
		sb.append("}}");

		return sb.toString();
	}

}
