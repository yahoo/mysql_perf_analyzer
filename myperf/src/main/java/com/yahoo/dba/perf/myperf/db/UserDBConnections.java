/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.*;

/**
 * This will be all the connections held by a specific user.  
 * @author xrao
 *
 */
public class UserDBConnections {
  private static Logger logger = Logger.getLogger(UserDBConnections.class.getName());
  private String appUser;//application user name
  private List<DBConnectionWrapper> connections = new java.util.ArrayList<DBConnectionWrapper>();
  private volatile boolean valid = true;  //if this is still valid
  
  private long connectionIdleTimeout = 600000L;
	
  private MyPerfContext frameworkContext;
  
  public UserDBConnections()
  {		
  }

  public String getAppUser() 
  {
    return appUser;
  }

  public void setAppUser(String appUser) 
  {
    this.appUser = appUser;
  }
	
  /**
   * try to get a free connection not used by other session.
   * @param dbGroupName
   * @param hostname
   * @return
   */
  synchronized public DBConnectionWrapper checkoutConnection(String dbGroupName, String hostname)
  {
    for(DBConnectionWrapper conn:this.connections)
	{
	  if(dbGroupName.equalsIgnoreCase(conn.getDb().getDbGroupName())
			  &&hostname.equalsIgnoreCase(conn.getDb().getHostName())&&!conn.isInuse())
	  {
	    conn.setInuse(true);
		return conn;
	  }
	}
	return null;
  }
	
  /**
   * Note: for oracle only
   * @param dbname
   * @param tgtInstance
   * @return
   */
  public DBConnectionWrapper checkoutConnection(String dbname, int tgtInstance)
  {
    synchronized(this)
	{
	  for(DBConnectionWrapper conn:this.connections)
	  {
	    if(dbname.equalsIgnoreCase( conn.getDb().getDbGroupName())&& conn.getInstance_number()==tgtInstance)
		{
		  conn.setInuse(true);
		  return conn;
		}
	  }
	  //if we reach here without credential, stop here
	  return null;
	}		
  }
	
  /**
   * Because it is possible to wait for too long to create a new connection, we relax this method from
   * synchronized and move the synchronized to required blocks.
   * @param dbinfo
   * @param cred
   * @return
   */
  public DBConnectionWrapper checkoutConnection(DBInstanceInfo dbinfo, DBCredential cred)
  {
    synchronized(this)
	{
	  for(DBConnectionWrapper conn:this.connections)
	  {
		if(dbinfo.getDbGroupName().equalsIgnoreCase(conn.getDb().getDbGroupName())
				&&dbinfo.getHostName().equalsIgnoreCase(conn.getDb().getHostName())&&!conn.isInuse())
		{
		  conn.setInuse(true);
		  return conn;
		}
	  }
	  //if we reach here without credential, stop here
	  if(cred==null)return null;
	}
		
	//when reach here, we have no db connection available, need create one
	//Because it could take too long to create a new connection to one db, it is better not to
	//block other connections.
	if(cred!=null &&cred.getPassword()==null )
	  throw new RuntimeException("No credential provided fro DB "+dbinfo.getDbGroupName());
	try
	{
      return createConnectionInternal(dbinfo, cred);
	}catch(Exception ex)
	{
		  if(ex.getCause() != null)
		  {
			  Throwable cause = ex.getCause();
			  String msg = cause.getMessage();
			  //suppress log messages for certain error
			  if(cause instanceof java.net.UnknownHostException 
					  || (cause instanceof java.net.ConnectException && msg != null && msg.indexOf("Connection refused") >=0))
			  {
				  logger.log(Level.SEVERE,"Exception cause when connecting to ("+dbinfo+"): msg, no retry.");
				  throw new RuntimeException(Constants.CONN_MSG_NORETRY);
			  }else
				  logger.info("Ex: " + ex.getCause().getClass().getName()+", " + ex.getCause().getMessage());
		  }
		  logger.log(Level.SEVERE,"Exception when connecting to ("+dbinfo+")", ex);
	  throw new RuntimeException(ex.getMessage());
	}
  }

  /**
   * For Oracle
   * When a target DB is not directly accessible, try to use gv view to access through another instance. Find first one available
   * @param cluster
   * @param cred
   * @return
   */
  public DBConnectionWrapper checkoutConnection(DBGroupInfo cluster, DBCredential cred)
  {
    synchronized(this)
	{
	  for(DBConnectionWrapper conn:this.connections)
	  {
		if(cluster.getGroupName().equalsIgnoreCase(conn.getDb().getDbGroupName())&&!conn.isInuse())
		{
		  conn.setInuse(true);
		  return conn;
	    }
	  }
	  //if we reach here without credential, stop here
	  if(cred==null)return null;
	}
		
	//when reach here, we have no db connection available, need create one
	//Because it could take too long to create a new connection to one db, it is better not to
	//block other connections.
	if(cred!=null &&cred.getPassword()==null )
	  throw new RuntimeException("No credential provided fro DB group "+cluster.getGroupName());
		
	//now let's try the cluster one by one
	for(DBInstanceInfo dbinfo:cluster.getInstances())
	{
	  try
	  {
		if(!dbinfo.isConnectionVerified())
		{
		  logger.info("Skip host which is not connection verified yet: "+dbinfo.getHostName());
		  continue;
		}
		return createConnectionInternal(dbinfo, cred);
	  }catch(Exception ex)
	  {
	    logger.log(Level.SEVERE,"Exception when connecting to ("+dbinfo+")", ex);
		throw new RuntimeException(ex.getMessage());
	  }
	}
	return null;
  }

  private DBConnectionWrapper createConnectionInternal(DBInstanceInfo dbinfo, DBCredential cred)
	throws SQLException
  {
	logger.fine("Connecting to "+dbinfo.getConnectionString()+" using user "+cred.getUsername());
	Connection conn = ConnectionFactory.connect(dbinfo, cred, frameworkContext);
	if(conn==null)
	  throw new SQLException("Failed to create connection: null connection");
	conn.setReadOnly(true);//TODO right now, we only allow readonly connection
	//wrap it
	DBConnectionWrapper cw = new DBConnectionWrapper(dbinfo);
	cw.setAppUser(this.appUser);
	cw.setConnection(conn);
	cw.setInuse(true);
	//since we already have the connection, now store it back
	synchronized(this)
	{
	  this.connections.add(cw);
	}
    return cw;
  }
  
  /**
   * Well, the only thing we need do is to set inuse as false
   * @param conn
   */
  public void checkinConnection(DBConnectionWrapper conn)
  {
    if(conn==null)return;
	conn.setCurrentStatement(null);
	conn.setInuse(false);
  }

  /**
   * Well, we need close the connection and remove it
   * @param conn
   */
  public void checkinConnectionOnError(DBConnectionWrapper conn)
  {
    conn.setCurrentStatement(null);
	conn.close();
	synchronized(this)
	{
	  for(int i=0;i<this.connections.size();i++)
	  {
	    if(conn == this.connections.get(i))
		{
		  logger.fine("Remove closed connection ("+conn.getAppUser()+","+conn.getDb());
		  this.connections.remove(i);
		  break;
		}
	  }
	}
  }

  /**
   * Well, we need close the connection and remove it
   * @param conn
   */
  public void checkinConnectionAndClose(DBConnectionWrapper conn)
  {
    conn.setCurrentStatement(null);
	conn.close();
	synchronized (this)
	{
	  for(int i=0;i<this.connections.size();i++)
	  {
	    if(conn == this.connections.get(i))
		{
		  logger.fine("Remove closed connection as requested ("+conn.getAppUser()+","+conn.getDb());
		  this.connections.remove(i);
		  break;
	    }
	  }
    }
  }

  /**
   * Close all connections
   */
  synchronized public void close()
  {
    this.valid = false;
	for(DBConnectionWrapper conn:this.connections)
	{
	  logger.fine("Closing connection to ("+conn.getAppUser()+", "+conn.getDb()+")");
	  try
	  {
	    if(conn.isInuse() && conn.getCurrentStatement()!=null)
		{
		  logger.fine("Cancel pending query, connection  ("+conn.getAppUser()+", "+conn.getDb()+")");
		  conn.getCurrentStatement().cancel();
		}
	  }catch(Exception ex)
	  {
	    logger.log(Level.SEVERE,"Exception", ex);
	  }
 	  conn.close();
	  logger.fine("Closed connection to ("+conn.getAppUser()+", "+conn.getDb()+")");
	}
	this.connections.clear();
  }
	
  /**
   * Close connection idled for timeMillis. 5 minutes or more
   * @param timeMillis
   */
  public void closeExpired(long timeMillis)
  {
    if(timeMillis<=0)timeMillis = this.connectionIdleTimeout;
    List<DBConnectionWrapper> idleList = new ArrayList<DBConnectionWrapper>();
	synchronized(this.connections)
	{
	  for(int i=this.connections.size()-1;i>=0;i--)
	  {
	    DBConnectionWrapper conn = this.connections.get(i);
		if(!conn.isInuse() && conn.isExpired(timeMillis))
		{
		  logger.fine("Remove idle connection from pool ("+conn.getAppUser()+","+conn.getDb()+"), created at "+conn.getCreateTime());
		  idleList.add(this.connections.remove(i));
		}
	  }
	}
		
	for(DBConnectionWrapper conn:idleList)
	{
	  conn.close();
	  logger.fine("Closed idle connection ("+conn.getAppUser()+","+conn.getDb()+"), created at "+conn.getCreateTime());
	}
  }

  public void closeIdle(String clusterName, String hostName)
  {
    logger.fine("Close idle connections ("+clusterName+","+hostName+")");
	this.valid = false;
	List<DBConnectionWrapper> idleConnections = new ArrayList<DBConnectionWrapper>();
	//remove from reuse
	synchronized (this)
	{
	  for(int i=this.connections.size()-1;i>=0;i--)
	  {
	    DBConnectionWrapper conn = this.connections.get(i);
		if(!clusterName.equalsIgnoreCase(conn.getDb().getDbGroupName())
				||!hostName.equalsIgnoreCase(conn.getDb().getHostName())||conn.isInuse())continue;
			
		try
		{
		  conn.setCurrentStatement(null);
		  logger.fine("Remove connection from pool ("+conn.getAppUser()+","+conn.getDb()+"), created at "+conn.getCreateTime());
		  idleConnections.add(this.connections.remove(i));
		}catch(Exception ex)
		{
		  logger.log(Level.SEVERE,"Exception", ex);
		}
	  }
	}
	for(DBConnectionWrapper conn:idleConnections)
	{
	  logger.fine("Closing connection to ("+conn.getAppUser()+", "+conn.getDb()+"), created at "+conn.getCreateTime()+", ignore inuse "+conn.isInuse());
	  conn.close();
	  logger.fine("Closed connection to ("+conn.getAppUser()+", "+conn.getDb()+"), created at "+conn.getCreateTime()+", ignore inuse "+conn.isInuse());
	}
  }
	
  synchronized public int getConnectionCount()
  {
    return this.connections.size();
  }

  synchronized public TreeMap<String,Integer> retrieveConnectionMap()
  {
    TreeMap<String, Integer> connMap = new TreeMap<String, Integer>();
	for(int i=this.connections.size()-1;i>=0;i--)
	{
	  DBConnectionWrapper conn = this.connections.get(i);
	  String dbname = conn.getDb().getDbGroupName();
	  String hostname = conn.getDb().getHostName();
	  String key = dbname+":"+hostname;
	  int connCnt = 0;
	  if(connMap.containsKey(key))
	    connCnt = connMap.get(key);
	  connCnt++;
	  connMap.put(key, connCnt);
	}
	return connMap;
  }
 
  public boolean isValid() 
  {	  
    return valid;
  }

  public void setValid(boolean valid) 
  {
    this.valid = valid;
  }

  public long getConnectionIdleTimeout() 
  {
    return connectionIdleTimeout;
  }

  public void setConnectionIdleTimeout(long connectionIdleTimeout) 
  {
    this.connectionIdleTimeout = connectionIdleTimeout;
  }

  public MyPerfContext getFrameworkContext() 
  {
    return frameworkContext;
  }

  public void setFrameworkContext(MyPerfContext frameworkContext) 
  {
    this.frameworkContext = frameworkContext;
  }
}
