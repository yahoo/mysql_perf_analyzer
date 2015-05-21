/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.List;

/**
 * Information about a group of DB, such as a DB cluster
 * @author xrao
 *
 */
public class DBGroupInfo implements java.io.Serializable 
{
  private static final long serialVersionUID = 1L;
  private List<DBInstanceInfo> instances = new java.util.ArrayList<DBInstanceInfo>();
  private String groupName;
	
  public DBGroupInfo()
  {	
  }

  public List<DBInstanceInfo> getInstances() 
  {
    return instances;
  }
  public DBInstanceInfo getInstance(int instanceNumber) 
  {
    for(DBInstanceInfo inst:instances)
	{
	  if(instanceNumber==inst.getInstance())return inst;
	}
	return null;
  }

  /**
   * If all hosts can be accessed directly
   * @return
   */
  public boolean canAccessDirectly()
  {
    for(DBInstanceInfo inst:instances)
	{
	  if(!inst.isConnectionVerified())return false;
	}
	return true;
  }
	
  public DBInstanceInfo findInstanceByHost(String hostname)
  {
	for(DBInstanceInfo dbinfo: this.instances)
	{
	  if(hostname.equalsIgnoreCase(dbinfo.getHostName()))
		return dbinfo;
	}
	return null;
  }
	
  public void addOrUpdateInstance(DBInstanceInfo dbinfo)
  {
    DBInstanceInfo db = findInstanceByHost(dbinfo.getHostName());
	if(db==null)
	  this.instances.add(dbinfo);
	else
	{
	  db.setPort(dbinfo.getPort());
	  db.setDatabaseName(dbinfo.getDatabaseName());
	  db.setUseTunneling(dbinfo.isUseTunneling());
	  db.setLocalHostName(dbinfo.getLocalHostName());
	  db.setLocalPort(dbinfo.getLocalPort());
	  db.setInstance(dbinfo.getInstance());
	  db.setConnectionVerified(dbinfo.isConnectionVerified());
	}
  }
	
  public String getGroupName() 
  {
    return groupName;
  }

  public void setGroupName(String groupName) 
  {	
	this.groupName = groupName;
  }	
}
