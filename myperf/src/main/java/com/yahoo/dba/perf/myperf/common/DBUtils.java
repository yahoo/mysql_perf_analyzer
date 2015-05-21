/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * JDBC utility functions
 * @author xrao
 *
 */
public class DBUtils {
  private static Logger logger = Logger.getLogger(DBUtils.class.getName());
  //private static final String INSTANCE_INFO_SQL="select INST_NUMBER, INST_NAME from v$active_instances";

  /**
   * Check if the given jdbc connection is still good
   * @param conn
   * @return
   */
  public static boolean checkConnection(Connection conn)
  {
    //String sql = "select sysdate from dual";
    String sql = "select now()";
    Statement stmt = null;
	ResultSet rs = null;
	try
	{
	  stmt = conn.createStatement();
	  stmt.setQueryTimeout(180);
	  rs = stmt.executeQuery(sql);
	  if(rs!=null && rs.next())
		return true;
	  return false;
	}catch(Exception ex)
	{			
	}finally
	{
	  close(rs);
	  close(stmt);
	}
	return false;
  }

  public static void close(Statement stmt)
  {
    if(stmt!=null)try{stmt.close();}catch(Exception ex){}
  }
  
  public static void close(ResultSet rs)
  {
    if(rs!=null)try{rs.close();}catch(Exception ex){}
  }

  public static void close(Connection conn)
  {
    if(conn!=null)try{conn.close();}catch(Exception ex){}
  }
	
  /**
   * Test is we can connect to db with given user name and password.
   * If testRWPermission is true, we will test if we can create/drop/insert/delete 
   * a test table (for metrics db).
   * @param dbinfo
   * @param username
   * @param password
   * @param testRWPermission
   * @return
   */
  public static String testConnection(DBInstanceInfo dbinfo, 
			String username, 
			String password,
			boolean testRWPermission)
  {
    String url = dbinfo.getConnectionString();
	Connection conn = null;
	Statement stmt = null;
	ResultSet rs = null;
	try
	{
	  logger.info("Test connection to ("+dbinfo+"): "+url);
	  DriverManager.setLoginTimeout(60);
	  conn = DriverManager.getConnection(url, username, password);
		//if(!dbinfo.isVirtualHost())//only update for non virtual host
		//  dbinfo.setInstance(getConnectionInstanceNumber(conn));
	  if(conn!=null)
	  {
	    dbinfo.setConnectionVerified(true);
		logger.info("Connection test succeeded to ("+dbinfo+")");
		if(testRWPermission)
		{
			stmt = conn.createStatement();
			//create table
			stmt.execute("create table mtr_test (id int auto_increment, v varchar(30), primary key(id))");
			stmt.execute("insert into mtr_test (v) values('123')");
			rs = stmt.executeQuery("select v from mtr_test");
			String testData = null;
			if(rs != null && rs.next())
			  testData = rs.getString(1);
			if(!"123".equals(testData))
				return "Failed insert/select test. Expect 123, got "+testData+".";
			rs.close(); rs = null;
			stmt.execute("delete from mtr_test where v='123'");
			stmt.execute("drop table mtr_test");
		}
	    return null;
      }
	  else
      {
		  logger.log(Level.SEVERE,"Connection test failed: reason null.");
	      return "Connection test failed: reason null.";    	  
      }
	  
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
      return "Connection test to "+ url+" failed: "+ex.getMessage();
	}finally
	{
		DBUtils.close(rs);
		DBUtils.close(stmt);
		DBUtils.close(conn);
	}
  }

  public static String testConnection(DBInstanceInfo dbinfo, 
			String username, 
			String password)
{
  String url = dbinfo.getConnectionString();
	Connection conn = null;
	Statement stmt = null;
	ResultSet rs = null;
	try
	{
	  logger.info("Test connection to ("+dbinfo+"): "+url);
	  DriverManager.setLoginTimeout(60);
	  conn = DriverManager.getConnection(url, username, password);
	  if(conn!=null)
	  {
	    dbinfo.setConnectionVerified(true);
		logger.info("Connection test succeeded to ("+dbinfo+")");
	    return null;
    }
	else
    {
		logger.log(Level.SEVERE,"Connection test failed: reason null.");
	    return "Connection test failed: reason null.";    	  
    }
	  
	}catch(Exception ex)
	{
	    logger.log(Level.SEVERE,"Exception", ex);
        return "Connection test to "+ url+" failed: "+ex.getMessage();
	}finally
	{
		DBUtils.close(rs);
		DBUtils.close(stmt);
		DBUtils.close(conn);
	}
  }

  public static boolean hasTable(Connection conn, String schemaName, String tableName)
  {
    ResultSet rs = null;
	try
	{
	  rs = conn.getMetaData().getTables(null, schemaName, tableName.toUpperCase(), null);
	  while(rs.next())
	  {
	    if(tableName.equalsIgnoreCase(rs.getString("TABLE_NAME")))return true;
	  }
	}catch(Exception ex)
	{			
	}
	finally
	{
	  if(rs!=null)try{rs.close();rs = null;}catch(Exception iex){}
	}
	return false;
  }
  
  public static DBCredential findDBCredential(MyPerfContext ctx, String dbid, AppUser appUser)
  {
    DBCredential cred = null;
	if(appUser==null)return null;
	try
	{		
	  //first, check if the user has his own credential
	  cred = ctx.getMetaDb().retrieveDBCredential(appUser.getName(), dbid);
	  if(cred!=null && !appUser.getName().equalsIgnoreCase(cred.getAppUser()))
	  {
	    logger.info(appUser.getName()+": get cred for "+cred.getUsername());
	  }
	}catch(Exception ex)
	{
	    logger.log(Level.WARNING, appUser.getName()+": failed to get cred for "+cred.getUsername());
	}
	return cred;
  }

}