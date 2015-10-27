/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.process;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.yahoo.dba.perf.myperf.common.*;
import com.yahoo.dba.perf.myperf.db.ConnectionFactory;

/**
 * MySQL query profiler
 * @author xrao
 *
 */
public class MyProfiler implements java.io.Serializable
{
  private static final long serialVersionUID = 1L;
  private MyPerfContext frameworkContext;//access common data
  private DBInstanceInfo dbinfo;//target database
  private boolean gatherPlan;//if true, run explain plan first
  private boolean gatherStats;//if true, gather session stats
  private boolean useProfiling;//if true, set profiling as 1
  private String sqlText;//query to be profiled, for now, we only support SELECT
  private String dbUser;
  private long connectionTime = 0L; //time used for connection
  
  private transient Connection connection;
	
  public MyPerfContext getFrameworkContext() 
  {
    return frameworkContext;
  }

  public void setFrameworkContext(MyPerfContext frameworkContext) 
  {
    this.frameworkContext = frameworkContext;
  }

  public DBInstanceInfo getDbinfo() 
  {
    return dbinfo;
  }

  /**
   * The caller should clone the database info, plus the target database (schema) name
   * @param dbinfo
   */
  public void setDbinfo(DBInstanceInfo dbinfo) 
  {
    this.dbinfo = dbinfo;
  }

  public boolean isGatherPlan() 
  {
    return gatherPlan;
  }

  public void setGatherPlan(boolean gatherPlan) 
  {
    this.gatherPlan = gatherPlan;
  }

  public boolean isGatherStats() 
  {
    return gatherStats;
  }

  public void setGatherStats(boolean gatherStats) 
  {
    this.gatherStats = gatherStats;
  }

  public boolean isUseProfiling() 
  {
    return useProfiling;
  }

  public void setUseProfiling(boolean useProfiling) 
  {
    this.useProfiling = useProfiling;
  }

  public String getSqlText() 
  {
    return sqlText;
  }

  public static class UnsafeQueryException extends Exception
  {
	private static final long serialVersionUID = 1L;
	public UnsafeQueryException(String msg)
	{
		super(msg);
	}

  }
  public void setSqlText(String sqlText) 
  throws UnsafeQueryException
  {
	  checkQuerySafety(sqlText); 
    this.sqlText = sqlText;
  }

  private void checkQuerySafety(String sqlText)
		  throws UnsafeQueryException
  {
	  if(sqlText==null||sqlText.isEmpty())
		  throw new UnsafeQueryException("No query text provided");
	  String s = sqlText.trim();
	  //remove any comments
	  while(s.startsWith("/*"))
	  {
		  int idx = s.indexOf("*/");
		  if(idx<0)break;
		  s = s.substring(idx+2);
		  if(s!=null)s = s.trim();
	  }
	  //remove --
	  while(s.startsWith("--"))
	  {
		int idx = s.indexOf("\n");
		if(idx<0)idx = s.indexOf("\r");
		if(idx<0)break;
		  s = s.substring(idx+1);
		  if(s!=null)s = s.trim();
	  }
	  if(s==null||s.isEmpty())
		  throw new UnsafeQueryException("No valid query text provided");
	  if(!s.toLowerCase().startsWith("select") && !s.toLowerCase().startsWith("(select"))
	  {
		  throw new UnsafeQueryException("Only SELECT statement is supported at this moment");		  
	  }
  }
  
  public void destroy()
  { 
    if(this.connection!=null)
      DBUtils.close(connection);
    this.connection = null;
  }
  
  /**
   * We don't want to store username and passwword
   * @param dbuser
   * @param dbpwd
   */
  public void connect(String dbuser, String dbpwd, boolean readOnly)
  throws SQLException
  {
    if(connection!=null && !this.dbUser.equalsIgnoreCase(dbuser))
    {
      DBUtils.close(connection);
    }
    this.dbUser = dbuser;
    //TODO test connection
    java.util.Properties prop = new java.util.Properties();
	prop.put("socketTimeout", "600000");//make it 10 minutes. 1 minute is too short for profiling
	prop.put("interactiveClient", "true");
	long connStartTime = System.currentTimeMillis();
    connection = ConnectionFactory.connect(dbinfo, dbuser, dbpwd, frameworkContext, prop);
	long connEndTime = System.currentTimeMillis();
	this.connectionTime = connEndTime - connStartTime;
    if(readOnly)
    	connection.setReadOnly(true);
  }

  public String getDbUser() 
  {
	return dbUser;
  }

  public void setDbUser(String dbUser) 
  {
	this.dbUser = dbUser;
  }
  
  public ResultList runExplainPlan()
  throws SQLException
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("EXPLAIN "+this.sqlText);
      ResultList rList =  ResultListUtil.fromSqlResultSet(rs, 1000);
      //now add query cost
      DBUtils.close(rs);
      rs = stmt.executeQuery("show status like 'Last_query_cost'");
      if(rs!=null && rs.next())
      {
    	  String cost = rs.getString("Value");
    	  if(cost!=null)
    	  {
    		  //append as last row
    		  ResultRow row = new ResultRow();
    		  int cols = rList.getColumnDescriptor().getColumns().size();
    		  for(int i=0;i<cols-1;i++)
    			  row.addColumn("");
    		  row.addColumn("Cost: "+cost);
    		  rList.addRow(row);
    	  }
      }
      return rList;
    }finally
    {
      DBUtils.close(rs);
      DBUtils.close(stmt);
    }
  }
  
  /**
   * TODO check version
   * @return
   * @throws SQLException
   */
  public String runExplainPlanJson()
		  throws SQLException
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
	  stmt = connection.createStatement();
	  rs = stmt.executeQuery("EXPLAIN FORMAT=JSON "+this.sqlText);
	  if(rs!=null && rs.next())
	  {
	    return rs.getString(1);
	  }
	  DBUtils.close(rs);
	}finally
    {
	  DBUtils.close(rs);
	  DBUtils.close(stmt);
	}
    return null;
   }
  
  public ResultList runProfile()
  throws SQLException
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = connection.createStatement();
      stmt.execute("set profiling=1");
      rs = stmt.executeQuery(this.sqlText);
      int count = 0;
      while(rs!=null && rs.next())
      {
    	  count++;
      }
      DBUtils.close(rs);
      //TODO we need use the count
      rs = stmt.executeQuery("show profile all");
      //while(rs!=null && rs.next())
      //{
      //	  System.out.println(rs.getString(1));
      //}
      ResultList rList = ResultListUtil.fromSqlResultSet(rs, 1000);
      stmt.execute("set profiling=0");
      //reprocess and remove empty/all zero columns
      int[] bitmap =  new int[rList.getColumnDescriptor().getColumns().size()];
      for(int i=0;i<bitmap.length;i++)bitmap[i] = 0;
      
	  for(int i=0;i<bitmap.length;i++)
	  {
		  for(ResultRow row:rList.getRows())
		  {
			List<String> cols = row.getColumns();
			String val = cols.get(i);
			if(val!=null && !val.isEmpty() && !"0".equals(val.trim()))
			{
				bitmap[i] = 1;break;
			}
    	  }
      }
	  ColumnDescriptor desc = new ColumnDescriptor();
	  List<ColumnInfo> colInfos = rList.getColumnDescriptor().getColumns();
	  int idx = 0;
	  for(int i=0;i<bitmap.length;i++)
	  {
		  if(bitmap[i]==1)
			  desc.addColumn(colInfos.get(i).getName(), colInfos.get(i).isNumberType(), idx++);
	  }
	  ResultList rlist2 = new ResultList();
	  rlist2.setColumnDescriptor(desc);
	  for(ResultRow row:rList.getRows())
	  {
		  ResultRow newRow = new ResultRow();
		  newRow.setColumnDescriptor(desc);
		  
		  List<String> cols = row.getColumns();
		  for(int i=0;i<bitmap.length;i++)
		  {
			  if(bitmap[i]==1)
			   newRow.addColumn(cols.get(i));
		  }
		  rlist2.addRow(newRow);
	  }
      return rlist2;
    }finally
    {
      DBUtils.close(rs);
      DBUtils.close(stmt);
    }
  }

  public ResultList runStats()
  throws SQLException
  {
    Statement stmt = null;
    ResultSet rs = null;
    HashMap<String, BigDecimal> startStat = new HashMap<String, BigDecimal>();
    HashMap<String, BigDecimal> endStat = new HashMap<String, BigDecimal>();
    
    try
    {
      stmt = connection.createStatement();
      stmt.setFetchSize(1000);
      stmt.execute("set long_query_time=0.00");//log query running for more than 0 millisconds
      //rs = stmt.executeQuery("select variable_name, variable_value from information_schema.session_status where variable_value!='0' and variable_value regexp '[0-9\\.]' limit 1000");
      rs = stmt.executeQuery("show status");
      while(rs!=null && rs.next())
      {
    	String key =   rs.getString(1);
    	String val = rs.getString(2);
    	if(val==null||val.isEmpty()||"0".equals(val))continue;
    	try{startStat.put(key.toUpperCase(), new BigDecimal(val));}catch(Exception ex){}
      }
      DBUtils.close(rs);
      long execStart = System.currentTimeMillis();
      rs = stmt.executeQuery(this.sqlText);
      long execEnd = System.currentTimeMillis();
      int count = 0;
      while(rs!=null && rs.next())
      {
    	  count++;
      }
      long fetchEnd = System.currentTimeMillis();
      DBUtils.close(rs);
      //rs = stmt.executeQuery("select variable_name, variable_value from information_schema.session_status where variable_value!='0' and variable_value regexp '[0-9\\.]' limit 1000");
      rs = stmt.executeQuery("show status");
      while(rs!=null && rs.next())
      {
    	String key =   rs.getString(1);
    	String val = rs.getString(2);
    	if(val==null||val.isEmpty()||"0".equals(val))continue;
    	try{endStat.put(key.toUpperCase(), new BigDecimal(val));}catch(Exception ex){}
      }
      DBUtils.close(rs);
      
      TreeMap<String, BigDecimal> res = new TreeMap<String, BigDecimal>();
      for(Map.Entry<String, BigDecimal> e: endStat.entrySet())
      {
        String key  = e.getKey();
        if(key.toLowerCase().startsWith("innodb") || key.toLowerCase().startsWith("uptime"))
        	continue;//skip non session related innodb stats and uptime
        BigDecimal val = e.getValue();
        if(key.toLowerCase().startsWith("last_query"))
        {
        	//skip the negate
        }
        else if(startStat.containsKey(key))
        {
        	if(val.equals(startStat.get(key)))continue;//skip the one with no change
        	val = val.add(startStat.get(key).negate());
        }
        res.put(key, val);
      }

      res.put("_RETURNED_ROWS", new BigDecimal(count));
      res.put("_TIME_EXEC_MS", new BigDecimal(execEnd - execStart));
      res.put("_TIME_FETCH_MS", new BigDecimal(fetchEnd - execEnd));
      res.put("_TIME_CONN_MS", new BigDecimal(this.connectionTime));
      
      ResultList rList = new ResultList();
      ColumnDescriptor desc = new ColumnDescriptor();
      desc.addColumn("VARIABLE_NAME", false, 1);
      desc.addColumn("VARIABLE_VALUE", true, 2);
      rList.setColumnDescriptor(desc);
      
      for(Map.Entry<String, BigDecimal> e: res.entrySet())
      {
        ResultRow row = new ResultRow();
        row.setColumnDescriptor(desc);
        List<String> vals = new ArrayList<String>(2);
        vals.add(e.getKey());
        vals.add(e.getValue().toPlainString());
        row.setColumns(vals);
        rList.addRow(row);
      }
      //TODO add row count and time
      return rList;
    }finally
    {
      DBUtils.close(rs);
      DBUtils.close(stmt);
    }
  }

}
