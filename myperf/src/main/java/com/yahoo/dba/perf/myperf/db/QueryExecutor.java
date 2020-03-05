/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.*;

public class QueryExecutor 
{
  private static Logger logger = Logger.getLogger(QueryExecutor.class.getName());
  //Oracle specific
  public static final String AWR_SNAP_ID_SQL = "select min(snap_id) min_snap_id, max(snap_id) max_snap_id from dba_hist_snapshot where begin_interval_time between TO_TIMESTAMP(?,'yyyy-mm-dd hh24:mi:ss') and TO_TIMESTAMP(?,'yyyy-mm-dd hh24:mi:ss')";
  public static final String AWR_DB_ID_SQL = "select dbid from dba_hist_snapshot where snap_id between ? and ?  and instance_number=? and rownum=1";
  public static final String AWR_DB_ID_SQL_2 = "select dbid from dba_hist_snapshot where instance_number=? and rownum=1";

  private SqlManager sqlManager;
  private MyPerfContext frameworkContext;
	
  public QueryExecutor()
  {}

  /**
   * No fancy replacement. Only place holder is p_nn. Used for MySQL, etc.
   * @param qps  assume it has been validated
   * @param conn
   * @param maxCount
   * @return
   * @throws SQLException
   */
  public  ResultList executeQueryGeneric(QueryParameters qps, DBConnectionWrapper conn, int maxCount)throws SQLException
  {
    PreparedStatement pstmt = null;
	Statement stmt = null;
	ResultSet rs = null;
	String actualSql = null;
	long startTime  = System.currentTimeMillis();
	long execTime  = startTime;
	try
	{
	  if(sqlManager == null)logger.severe("No sqlManager specified");//code error
	  if(qps == null)logger.severe("No QPS specified");//code error
	  Sql sql = sqlManager.getSql(qps.getSql());
	  if(sql == null)logger.severe("No SQL found for handle "+qps.getSql());//code error
	  String sqlText = null;
	  if(sql.getQueryClass()!=null)
	  {
	    sqlText = DynamicQueryFactory.getQuery(sql.getQueryClass(), conn, false);
	  }
	  if(sqlText==null)sqlText = sql.getSqlText(conn.getVersion());
	
	  if(sql.getParamCount()>0)
	  {
	    if(!sql.isReplace())
		{
		  actualSql = sqlText;
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    actualSql = actualSql.replace("&p_"+i, "?");
		  }
		  pstmt = conn.getConnection().prepareStatement(actualSql);
		  pstmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		  pstmt.setFetchSize(maxCount>0&&maxCount<this.frameworkContext.getQueryFetchSize()?maxCount:this.frameworkContext.getQueryFetchSize());
		  conn.setCurrentStatement(pstmt);
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    pstmt.setString(i, qps.getSqlParams().get("p_"+i));
		  }
		  //logger.info("execute query "+qps.getSql()+" with bind variables: "+actualSql);
		  rs = pstmt.executeQuery();
		  execTime = System.currentTimeMillis();
		}
	    else
		{
		  actualSql = sqlText;
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
			//add check here
		    actualSql = actualSql.replace("&p_"+i, escapeSingleQuote(sql.getSqlParameter(i - 1), qps.getSqlParams().get("p_"+i)));
		  }
		  stmt = conn.getConnection().createStatement();
		  stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		  stmt.setFetchSize(maxCount>0&&maxCount<this.frameworkContext.getQueryFetchSize()?maxCount:this.frameworkContext.getQueryFetchSize());
		  conn.setCurrentStatement(stmt);
		  //logger.info("execute query "+qps.getSql()+" with bind variables: "+actualSql);
		  rs = stmt.executeQuery(actualSql);
		  execTime = System.currentTimeMillis();
					
		}
	  }
	  else
	  {
	    actualSql = sqlText;
		stmt = conn.getConnection().createStatement();
		stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		stmt.setFetchSize(maxCount>0&&maxCount<this.frameworkContext.getQueryFetchSize()?maxCount:this.frameworkContext.getQueryFetchSize());
		conn.setCurrentStatement(stmt);
		rs = stmt.executeQuery(actualSql);
		execTime = System.currentTimeMillis();
	  }
	  ResultList rList = ResultListUtil.flatSqlResultSet(sqlManager.getSql(qps.getSql()), rs,maxCount>=0?maxCount:1000);//TODO, the maximum count
	  long endTime = System.currentTimeMillis();
	  if(rList!=null)
	  {
	    rList.setTotalResponseTime(endTime - startTime);	  
	    rList.setTotalExecutionTime(execTime - startTime);	  
	    rList.setTotalFetchTime(endTime - execTime);	  
	  }
	  return rList;
	}
	catch(SQLException sqlEx)
	{
	  logger.info("Failed to execute "+actualSql);
	  throw sqlEx;
	}
	finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(stmt);
	  DBUtils.close(pstmt);
	  conn.setCurrentStatement(null);
	}
  }

  
  public  Map<String, String> executeQueryWithMultiRowMetricsCollapsed(QueryParameters qps, DBConnectionWrapper conn, MetricsBuffer buf)throws SQLException
  {
    PreparedStatement pstmt = null;
	Statement stmt = null;
	ResultSet rs = null;
	String actualSql = null;
	long startTimestamp = System.currentTimeMillis();
	try
	{
	  if(sqlManager==null)logger.severe("No sqlManager specified");
	  if(qps==null)logger.severe("No QPS specified");
	  Sql sql = sqlManager.getSql(qps.getSql());
	  String sqlText = null;
	  if(sql.getQueryClass()!=null)
	  {
		sqlText = DynamicQueryFactory.getQuery(sql.getQueryClass(), conn, false);
	  }
	  if(sqlText==null)sqlText = sql.getSqlText(conn.getVersion());
	  if(sql.getParamCount()>0)
	  {
	    if(!sql.isReplace())
		{
		  actualSql = sqlText;
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    actualSql = actualSql.replace("&p_"+i, "?");
		  }
		  pstmt = conn.getConnection().prepareStatement(actualSql);
		  pstmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		  conn.setCurrentStatement(pstmt);
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    pstmt.setString(i, qps.getSqlParams().get("p_"+i));
		  }
		  //logger.info("execute query "+qps.getSql()+" with bind variables: "+actualSql);
		  rs = pstmt.executeQuery();
		}else
		{
		  actualSql = sqlText;
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    actualSql = actualSql.replace("&p_"+i, escapeSingleQuote(sql.getSqlParameter(i-1), qps.getSqlParams().get("p_"+i)));
		  }
		  stmt = conn.getConnection().createStatement();
		  stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		  conn.setCurrentStatement(stmt);
		  //logger.info("execute query "+qps.getSql()+" with bind variables: "+actualSql);
		  rs = stmt.executeQuery(actualSql);					
		}
	  }else
	  {
	    actualSql = sqlText;
		stmt = conn.getConnection().createStatement();
		stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		conn.setCurrentStatement(stmt);
		rs = stmt.executeQuery(actualSql);
	  }
	  //long endTimestamp = System.currentTimeMillis();
	  //return buf.recordOneRowBymetricsName2(rs, startTimestamp, (int)(endTimestamp - startTimestamp));
	  Map<String, String> metrics = new HashMap<String, String>();
	  while(rs!=null && rs.next())
	  {
		  metrics.put(rs.getString(buf.getMetrics().getMetricNameColumn()).toLowerCase(), rs.getString(buf.getMetrics().getMetricValueColumn()));
	  }
	  return metrics;
	}
	catch(SQLException sqlEx)
	{
	  logger.info("Failed to execute "+actualSql);
	  throw sqlEx;
	}
	finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(stmt);
	  DBUtils.close(pstmt);
	  conn.setCurrentStatement(null);
	}
  }

  /**
   * Retrieve key value pairs data from DB
   * @param qps
   * @param conn
   * @param keyColumn
   * @param valueColumn
   * @param normalizeKey if true, key will be convert to uppercase
   * @return
   * @throws SQLException
   */
  public  Map<String, String> executeQueryWithKeyValuPairs(QueryParameters qps, DBConnectionWrapper conn, String keyColumn, String valueColumn, boolean normalizeKey)throws SQLException
  {
    PreparedStatement pstmt = null;
	Statement stmt = null;
	ResultSet rs = null;
	String actualSql = null;
	//long startTimestamp = System.currentTimeMillis();
	try
	{
	  if(sqlManager==null)logger.severe("No sqlManager specified");
	  if(qps==null)logger.severe("No QPS specified");
	  String sqlText = qps.getSqlText(); //if not predefined
	  Sql sql = null;
	  if(sqlText == null || sqlText.isEmpty())
		  sql = sqlManager.getSql(qps.getSql());

	  if(sql != null && sql.getQueryClass()!=null)
		sqlText = DynamicQueryFactory.getQuery(sql.getQueryClass(), conn, false);

	  if(sqlText == null && sql != null)
		  sqlText = sql.getSqlText(conn.getVersion());
	  if(sql != null && sql.getParamCount()>0)
	  {
	    if(!sql.isReplace())
		{
		  actualSql = sqlText;
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    actualSql = actualSql.replace("&p_"+i, "?");
		  }
		  pstmt = conn.getConnection().prepareStatement(actualSql);
		  pstmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		  conn.setCurrentStatement(pstmt);
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    pstmt.setString(i, qps.getSqlParams().get("p_"+i));
		  }
		  //logger.info("execute query "+qps.getSql()+" with bind variables: "+actualSql);
		  rs = pstmt.executeQuery();
		}else
		{
		  actualSql = sqlText;
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    actualSql = actualSql.replace("&p_"+i, escapeSingleQuote(sql.getSqlParameter(i-1), qps.getSqlParams().get("p_"+i)));
		  }
		  stmt = conn.getConnection().createStatement();
		  stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		  conn.setCurrentStatement(stmt);
		  rs = stmt.executeQuery(actualSql);					
		}
	  }else
	  {
	    actualSql = sqlText;
		stmt = conn.getConnection().createStatement();
		stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		conn.setCurrentStatement(stmt);
		rs = stmt.executeQuery(actualSql);
	  }
	  Map<String, String> metrics = new HashMap<String, String>();
	  if(keyColumn != null && !keyColumn.isEmpty() && valueColumn != null && !valueColumn.isEmpty())
	  {
	    while(rs!=null && rs.next())
	    {
	      String key = rs.getString(keyColumn.toLowerCase());
	      if(normalizeKey)key = key.toUpperCase();
		  metrics.put(key, rs.getString(valueColumn.toLowerCase()));
	    }
	  }else
	  {
		  if(rs !=null)
		  {
			  java.sql.ResultSetMetaData meta = rs.getMetaData();
			  int cnt = meta.getColumnCount();
			  if(rs.next())
			  {
				  for(int i=1; i<=cnt; i++)
					  metrics.put(meta.getColumnLabel(i).toLowerCase(), rs.getString(i));
			  }
		  }
	  }
	  //logger.info("Retrieved: "+metrics.size()+", sql: "+actualSql);
	  return metrics;
	}
	catch(SQLException sqlEx)
	{
	  logger.info("Failed to execute: " + qps.getSql()+": "+actualSql);
	  throw sqlEx;
	}
	finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(stmt);
	  DBUtils.close(pstmt);
	  conn.setCurrentStatement(null);
	}
  }

  /**
   * For some cases like user stats, table stats, metrics are per user or table name based.
   * The keyColumn is used to retrieve the user or table name.,
   * and other columns should be the metrics
   * @param qps
   * @param conn
   * @param keyColumn
   * @return
   * @throws SQLException
   */
  public  Map<String, Map<String, String>> executeQueryWithMultipleColumns(QueryParameters qps, DBConnectionWrapper conn, String keyColumn)throws SQLException
  {
    PreparedStatement pstmt = null;
	Statement stmt = null;
	ResultSet rs = null;
	String actualSql = null;
	//long startTimestamp = System.currentTimeMillis();
	try
	{
	  if(sqlManager==null)logger.severe("No sqlManager specified");
	  if(qps==null)logger.severe("No QPS specified");
	  String sqlText = qps.getSqlText(); //if not predefined
	  Sql sql = null;
	  if(sqlText == null || sqlText.isEmpty())
		  sql = sqlManager.getSql(qps.getSql());

	  if(sql != null && sql.getQueryClass()!=null)
		sqlText = DynamicQueryFactory.getQuery(sql.getQueryClass(), conn, false);

	  if(sqlText == null && sql != null)
		  sqlText = sql.getSqlText(conn.getVersion());

	  if(sql != null && sql.getParamCount()>0)
	  {
	    if(!sql.isReplace())
		{
		  actualSql = sqlText;
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    actualSql = actualSql.replace("&p_"+i, "?");
		  }
		  pstmt = conn.getConnection().prepareStatement(actualSql);
		  pstmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		  conn.setCurrentStatement(pstmt);
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    pstmt.setString(i, qps.getSqlParams().get("p_"+i));
		  }
		  //logger.info("execute query "+qps.getSql()+" with bind variables: "+actualSql);
		  rs = pstmt.executeQuery();
		}else
		{
		  actualSql = sqlText;
		  for(int i=1;i<=sql.getParamCount();i++)
		  {
		    actualSql = actualSql.replace("&p_"+i, escapeSingleQuote(sql.getSqlParameter(i-1), qps.getSqlParams().get("p_"+i)));
		  }
		  stmt = conn.getConnection().createStatement();
		  stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		  conn.setCurrentStatement(stmt);
		  rs = stmt.executeQuery(actualSql);					
		}
	  }else
	  {
	    actualSql = sqlText;
		stmt = conn.getConnection().createStatement();
		stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
		conn.setCurrentStatement(stmt);
		rs = stmt.executeQuery(actualSql);
	  }
	  Map<String, Map<String,String>> metrics = new HashMap<String, Map<String, String>>();
	  int colCnt = 0;
	  if(rs != null)colCnt = rs.getMetaData().getColumnCount();
	  while(rs!=null && rs.next())
	  {
		  String key = rs.getString(keyColumn);
		  Map<String, String> metricsperKey = new HashMap<String, String>(colCnt);
		  for(int i=1; i<=colCnt; i++)
			  metricsperKey.put(rs.getMetaData().getColumnName(i), rs.getString(i));
		  metrics.put(key, metricsperKey);
	  }
	  return metrics;
	}
	catch(SQLException sqlEx)
	{
	  logger.info("Failed to execute "+actualSql);
	  throw sqlEx;
	}
	finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(stmt);
	  DBUtils.close(pstmt);
	  conn.setCurrentStatement(null);
	}
  }


	
	
	/**
	 * Not a right place, but we put the method here anyway.
	 * We only gather info for seconds_behind_master, salve_sql_running, salve_io_running
	 * @param conn
	 * @return
	 */
	public HashMap<String, String> showSlaveStatusMetrics(DBConnectionWrapper conn)
	{
		Statement stmt = null;
		ResultSet rs = null;
		HashMap<String, String> resMap = new HashMap<String, String>(3);
		boolean isSlave = false;
		int secondsBehindMaster = 0;
		int slaveIoRunning = 1;
		int slaveSqlRunning = 1;
		try
		{
			stmt = conn.getConnection().createStatement();
			rs = stmt.executeQuery("show slave status");
			while(rs!=null && rs.next())
			{
				int sec = rs.getInt("Seconds_Behind_Master");
				if(sec>secondsBehindMaster)secondsBehindMaster = sec;
				if(!"Yes".equalsIgnoreCase(rs.getString("Slave_IO_Running")))
					slaveIoRunning = 0;
				if(!"Yes".equalsIgnoreCase(rs.getString("Slave_SQL_Running")))
					slaveSqlRunning = 0;
				isSlave = true;
			}
			
		}catch(Exception ex)
		{
		}finally
		{
			close(rs);
			close(stmt);
		}
		if(isSlave)
		{
			resMap.put("Seconds_Behind_Master", String.valueOf(secondsBehindMaster));
			resMap.put("Slave_IO_Running", String.valueOf(slaveIoRunning));
			resMap.put("Slave_SQL_Running", String.valueOf(slaveSqlRunning));
		}
		return resMap;
	}
	public  ResultList executeQuery(QueryParameters qps, DBConnectionWrapper conn, int maxCount)throws SQLException
	{
		//validate the sql first
		//QueryInputValidator.validateSql(this.sqlManager, qps);
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		String actualSql = null;
		try
		{
			if(qps==null)logger.severe("No QPS specified");
			if(sqlManager==null)logger.severe("No sqlManager specified");
			Sql sql = sqlManager.getSql(qps.getSql());
			String sqlText = null;
			if(sql.getQueryClass()!=null)
			{
				sqlText = DynamicQueryFactory.getQuery(sql.getQueryClass(), conn, false);
			}
			if(sqlText==null)sqlText = sql.getSqlText(conn.getVersion());
			if(sql.getParamCount()>0)
			{
				if(!sql.isReplace())
				{
					actualSql = sqlText;
					for(int i=1;i<=sql.getParamCount();i++)
					{
						actualSql = actualSql.replace("&p_"+i, "?");
					}
					pstmt = conn.getConnection().prepareStatement(actualSql);
					pstmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
					pstmt.setFetchSize(maxCount>0&&maxCount<this.frameworkContext.getQueryFetchSize()?maxCount:this.frameworkContext.getQueryFetchSize());
					conn.setCurrentStatement(pstmt);
					for(int i=1;i<=sql.getParamCount();i++)
					{
						pstmt.setString(i, qps.getSqlParams().get("p_"+i));
					}
					//logger.info("execute query "+qps.getSql()+" with bind variables: "+actualSql);
					rs = pstmt.executeQuery();
				}else
				{
					actualSql = sqlText;
					for(int i=1;i<=sql.getParamCount();i++)
					{
						actualSql = actualSql.replace("&p_"+i, escapeSingleQuote(sql.getSqlParameter(i-1), qps.getSqlParams().get("p_"+i)));
					}
					stmt = conn.getConnection().createStatement();
					stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
					stmt.setFetchSize(maxCount>0&&maxCount<this.frameworkContext.getQueryFetchSize()?maxCount:this.frameworkContext.getQueryFetchSize());
					conn.setCurrentStatement(stmt);
					//logger.info("execute query "+qps.getSql()+" with bind variables: "+actualSql);
					rs = stmt.executeQuery(actualSql);
					
				}
			}else
			{
				actualSql = sqlText;
				stmt = conn.getConnection().createStatement();
				stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
				stmt.setFetchSize(maxCount>0&&maxCount<this.frameworkContext.getQueryFetchSize()?maxCount:this.frameworkContext.getQueryFetchSize());
				conn.setCurrentStatement(stmt);
				rs = stmt.executeQuery(actualSql);
			}
			return ResultListUtil.flatSqlResultSet(sqlManager.getSql(qps.getSql()), rs,maxCount>=0?maxCount:1000);//TODO, the maximum count
		}
		catch(SQLException sqlEx)
		{
			logger.info("Failed to execute "+actualSql);
			throw sqlEx;
		}finally
		{
			close(rs);
			close(stmt);
			close(pstmt);
			conn.setCurrentStatement(null);
		}
	}

	public  ResultList executeQuery(DBConnectionWrapper conn, String sql, Map<String, String> params, int maxCount)throws SQLException
	{
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		String actualSql = null;
		try
		{
			String sqlText = sql;
			if(params != null && params.size() > 0)
			{				
				actualSql = sqlText;
				for(Map.Entry<String, String> param: params.entrySet())
				{
					//note our parameters are prefixed with &
					actualSql = actualSql.replace("&"+param.getKey(), SqlParameter.escapeSingleQuote(param.getValue()));
				}
			}else
				actualSql = sqlText;
			
			stmt = conn.getConnection().createStatement();
			stmt.setQueryTimeout(this.frameworkContext.getQueryTimeout());
			stmt.setFetchSize(maxCount>0&&maxCount<this.frameworkContext.getQueryFetchSize()?maxCount:this.frameworkContext.getQueryFetchSize());
			conn.setCurrentStatement(stmt);
			rs = stmt.executeQuery(actualSql);
			return ResultListUtil.fromSqlResultSet(rs,maxCount>=0?maxCount:1000);
		}
		catch(SQLException sqlEx)
		{
			logger.info("Failed to execute "+actualSql);
			throw sqlEx;
		}finally
		{
			close(rs);
			close(stmt);
			close(pstmt);
			conn.setCurrentStatement(null);
		}
	}

	public SqlManager getSqlManager() {
		return sqlManager;
	}

	public void setSqlManager(SqlManager sqlManager) {
		this.sqlManager = sqlManager;
	}
	
	private void close(ResultSet rs)
	{
		if(rs!=null)try{rs.close();}catch(Exception ex){}
	}
	
	private void close(Statement stmt)
	{
		if(stmt!=null)try{stmt.close();}catch(Exception ex){}
	}

	public MyPerfContext getFrameworkContext() {
		return frameworkContext;
	}

	public void setFrameworkContext(MyPerfContext frameworkContext) {
		this.frameworkContext = frameworkContext;
	}
	
	private static String escapeSingleQuote(SqlParameter param, String str)
	{
		if(str==null)return null;
		if(param!=null)
		{
			boolean valid = param.isValidValue(str);
			if(valid)
				return str;//nothing to espace
			if(param.needEscape())
			{
				return SqlParameter.escapeSingleQuote(str);
			}
		}
		return null;//otherwise count invalid
		/*
		StringBuilder sb = new StringBuilder();
		char[] chs = str.toCharArray();
		for(int i=0;i<chs.length;i++)
		{
			if(chs[i]=='\'')
				sb.append("''");
			else sb.append(chs[i]);
		}
		return sb.toString();
		*/
	}
}
