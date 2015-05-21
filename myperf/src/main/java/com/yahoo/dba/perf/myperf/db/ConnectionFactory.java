/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map.Entry;

import com.yahoo.dba.perf.myperf.common.*;


/**
 * ConnectionFactory is used to create connections
 * @author xrao
 *
 */
public class ConnectionFactory 
{
  /**
   * The purpose is to create JDBC connection to DBInstanceInfo db,
   * using credential from cred. Using ConnectionFactory so that
   * we can later cover multiple types of database.
   * @param db
   * @param cred
   * @param context
   * @return
   * @throws java.sql.SQLException
   */
  public static Connection connect(DBInstanceInfo db, DBCredential cred, MyPerfContext context)
  throws java.sql.SQLException
  {
	java.util.Properties info = new java.util.Properties();
	info.put ("user", cred.getUsername());
	info.put ("password",cred.getPassword());
	if("oracle".equalsIgnoreCase(db.getDbType()))
	{
	  info.put("oracle.net.CONNECT_TIMEOUT", String.valueOf(context.getConnectionTimeout()));
	  info.put("oracle.net.READ_TIMEOUT", String.valueOf(context.getConnectionReadTimeout()));
	  info.put("oracle.jdbc.ReadTimeout", String.valueOf(context.getConnectionReadTimeout()));
	}
	else if("mysql".equalsIgnoreCase(db.getDbType()))		
	{
	  info.put("connectTimeout", String.valueOf(context.getConnectionTimeout()));
	  info.put("socketTimeout", String.valueOf(context.getConnectionReadTimeout()));
	}
	return DriverManager.getConnection(db.getConnectionString(), info);
  }
  
  public static Connection connect(DBInstanceInfo db, String username, String password, MyPerfContext context)
  throws java.sql.SQLException
  {
	java.util.Properties info = new java.util.Properties();
	info.put ("user", username);
	info.put ("password",password);
	if("oracle".equalsIgnoreCase(db.getDbType()))
	{
	  info.put("oracle.net.CONNECT_TIMEOUT", String.valueOf(context.getConnectionTimeout()));
	  info.put("oracle.net.READ_TIMEOUT", String.valueOf(context.getConnectionReadTimeout()));
	  info.put("oracle.jdbc.ReadTimeout", String.valueOf(context.getConnectionReadTimeout()));
	}
	else if("mysql".equalsIgnoreCase(db.getDbType()))		
	{
	  info.put("connectTimeout", String.valueOf(context.getConnectionTimeout()));
	  info.put("socketTimeout", String.valueOf(context.getConnectionReadTimeout()));
	}
	return DriverManager.getConnection(db.getConnectionString(), info);
  }  

  /**
   * Allow the user to provide customized properties to overwrite defaults
   * @param db
   * @param username
   * @param password
   * @param context
   * @param myinfo
   * @return
   * @throws java.sql.SQLException
   */
  public static Connection connect(DBInstanceInfo db, String username, String password, MyPerfContext context, java.util.Properties myinfo)
  throws java.sql.SQLException
  {
	java.util.Properties info = new java.util.Properties();
	info.put ("user", username);
	info.put ("password",password);
	if("oracle".equalsIgnoreCase(db.getDbType()))
	{
	  info.put("oracle.net.CONNECT_TIMEOUT", String.valueOf(context.getConnectionTimeout()));
	  info.put("oracle.net.READ_TIMEOUT", String.valueOf(context.getConnectionReadTimeout()));
	  info.put("oracle.jdbc.ReadTimeout", String.valueOf(context.getConnectionReadTimeout()));
	}
	else if("mysql".equalsIgnoreCase(db.getDbType()))		
	{
	  if(myinfo.getProperty("connectTimeout")!=null )
		  info.put("connectTimeout",myinfo.getProperty("connectTimeout"));
	  else 
		  info.put("connectTimeout", String.valueOf(context.getConnectionTimeout()));
	  if(myinfo.getProperty("socketTimeout")!=null )
		  info.put("socketTimeout", myinfo.getProperty("socketTimeout"));
	  else
		  info.put("socketTimeout", String.valueOf(context.getConnectionReadTimeout()));
	}
	if(myinfo!=null && myinfo.size()>0)
	{
		for(Entry<Object, Object> e: myinfo.entrySet())
		{
			if("connectTimeout".equalsIgnoreCase(e.getKey().toString()) || "socketTimeout".equalsIgnoreCase(e.getKey().toString()))
				continue;
			info.put(e.getKey(), e.getValue());
		}
	}
	return DriverManager.getConnection(db.getConnectionString(), info);
  }  

}

