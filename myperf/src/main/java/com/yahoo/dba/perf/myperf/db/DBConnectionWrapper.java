/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;

/**
 * Wrap a database connection with additional information
 * @author xrao
 *
 */
public class DBConnectionWrapper implements Comparable<DBConnectionWrapper>
{
  private static Logger logger = Logger.getLogger(DBConnectionWrapper.class.getName());
  private DBInstanceInfo db;
  private String appUser;//connection owner
  private Connection connection;//actual jdbc database connection
  private long createTime = System.currentTimeMillis();//time this object is created
  private boolean inuse;//some session has checked out this connection if true
  private boolean closed = false;
  private int instance_number;//instance_number retrieved from DB when connected, for Oracle
  private String version;//oracle version
  private Statement currentStatement;//current executing statement, for cancel during forceful close
  private long lastAccessTime = System.currentTimeMillis();
  
  public DBConnectionWrapper()
  {
  }
  
  public DBConnectionWrapper(DBInstanceInfo db)
  {
    this.db = db;
  }
  
  public String getAppUser() 
  {
    return appUser;
  }

  public void setAppUser(String appUser) 
  {
    this.appUser = appUser;
  }
	
  public Connection getConnection() 
  {
    this.lastAccessTime = System.currentTimeMillis();
	return connection;
  }
	
  public void setConnection(Connection connection) 
  {
    this.connection = connection;
    retrieveInfoUponOpen();
  }
  
  public long getCreateTime() 
  {
    return createTime;
  }

  public long getLastAccessTime() 
  {
    return lastAccessTime;
  }
	
  /**
   * If connection last used time is 30 minutes away, treat it as expired
   * @return
   */
	
  public boolean isExpired()
  {
    return System.currentTimeMillis() - this.lastAccessTime>=1800000L;
  }

  public boolean isExpired(long timeMillis)
  {
    return System.currentTimeMillis() - this.lastAccessTime>=timeMillis;
  }

	//make it package level only
  boolean isInuse() 
  {	  
    return inuse;
  }
  //make it package level only
  void setInuse(boolean inuse) 
  {
    this.inuse = inuse;
  }
	
  public boolean isClosed() 
  {
    return closed;
  }

  public void close() 
  {
	try
	{
	  this.connection.close();
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	}
	this.closed = true;
  }

  public int getInstance_number() 
  {
    return instance_number;
  }

  public void setInstance_number(int instance_number) 
  {
    this.instance_number = instance_number;
  }
  
  public Statement getCurrentStatement() 
  {
    return currentStatement;
  }
  
  public void setCurrentStatement(Statement currentStatement) 
  {
    this.currentStatement = currentStatement;
  }
  
  public String getVersion() 
  {
    return version;
  }

  public void setVersion(String version) 
  {
    this.version = version;
  }

  public int compareTo(DBConnectionWrapper conn) 
  {
    if(this.db.getDbGroupName()!=null&&!this.db.getDbGroupName().equalsIgnoreCase(conn.getDb().getDbGroupName()))
	  return this.getDb().getDbGroupName().compareTo(conn.getDb().getDbGroupName());
	else if(this.db.getDbGroupName() == null && conn.getDb().getDbGroupName()!=null)
	  return -1;
	else if(this.db.getHostName()!=null && !this.db.getHostName().equalsIgnoreCase(conn.getDb().getHostName()))
	  return this.db.getHostName().compareTo(conn.getDb().getHostName());
	return 0;
  }

  public DBInstanceInfo getDb() 
  {
    return db;
  }
	
  public void setDb(DBInstanceInfo db) 
  {
    this.db = db;
  }
  
  /**
   * Version and oracle instance info. Called by setConnection
   */
  private void retrieveInfoUponOpen()
  {
    if(this.connection == null)return;
    
    String sql = null;
    if("oracle".equalsIgnoreCase(this.db.getDbType()))
      sql = "select instance_number, version from v$instance";
    else if("mysql".equalsIgnoreCase(this.db.getDbType()))
      sql = "select @@global.version as version";
    else 
      return;
    
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
    	stmt = connection.createStatement();
    	rs = stmt.executeQuery(sql);
    	
    	if(rs!=null && rs.next())
    	{
    	  if("oracle".equalsIgnoreCase(this.db.getDbType()))
    	  {
    	    this.instance_number = rs.getInt(1);
    	    this.version = rs.getString(2);
    	  }
    	  else if("mysql".equalsIgnoreCase(this.db.getDbType()))        	  
    	  {
    	    this.version = rs.getString(1);	  
    	  }
    	}
    }catch(Exception ex)
    {
    	DBUtils.close(rs);
    	DBUtils.close(stmt);
    }
  }
}
