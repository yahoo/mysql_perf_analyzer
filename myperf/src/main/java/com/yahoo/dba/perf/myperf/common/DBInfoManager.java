/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.meta.MetaDB;
import com.yahoo.dba.perf.myperf.metrics.MetricsDbBase;


/**
 * Manage database information
 * @author xrao
 *
 */
public class DBInfoManager implements java.io.Serializable
{
  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(DBInfoManager.class.getName());
  private MetaDB projectDb;//note we store user cred here
  private MetricsDbBase metricsDb;//db def
	
  private Map<String, DBGroupInfo> groups = new java.util.TreeMap<String, DBGroupInfo>();
  private Map<String, MyDatabases> mydbs = new java.util.HashMap<String, MyDatabases>();
  private HashMap<String, java.util.Date> lastScanTime = new HashMap<String, java.util.Date>();
  public DBInfoManager()
  {
  }

  synchronized public Map<String,DBGroupInfo> getClusters() 
  {
    return groups;
  }
	
  synchronized public DBGroupInfo findGroup(String groupName)
  {
    if(groups!=null && groups.containsKey(groupName))
	  return groups.get(groupName);
	return null;
  }
  
  synchronized public void addOrUpdateInstance(DBInstanceInfo dbinfo)
  {
    DBGroupInfo cls = this.findGroup(dbinfo.getDbGroupName());
	if(cls==null)
	{
	  cls = new DBGroupInfo();
	  cls.setGroupName(dbinfo.getDbGroupName());
	  this.groups.put(cls.getGroupName(), cls);
	}
	cls.addOrUpdateInstance(dbinfo);
  }
  
  synchronized public DBInstanceInfo findDB(String groupName, String hostname)
  {
    DBGroupInfo cls = this.findGroup(groupName);
	if(cls==null)return null;
	for(DBInstanceInfo inst: cls.getInstances())
	{
	  if(hostname.equalsIgnoreCase(inst.getHostName()))return inst;
    }
	return null;
  }
	
  synchronized public boolean removeDBHost(String groupName, String hostname)
  {
    DBGroupInfo cls = this.findGroup(groupName);
	if(cls==null)return false;
	for(int i=cls.getInstances().size()-1;i>=0;i--)
	{
	  DBInstanceInfo inst = cls.getInstances().get(i);
	  if(hostname.equalsIgnoreCase(inst.getHostName()))
	  {
		cls.getInstances().remove(i);
		return true;
	  }
	}
	return false;
  }
	
  synchronized public void removeGroup(String groupName)
  {
    DBGroupInfo cls = this.findGroup(groupName);
	if(cls==null)return;
	this.groups.remove(groupName);
	this.projectDb.removeDbGroup(groupName);
  }

  synchronized public java.util.List<String> listGroupNames()
  {
    java.util.ArrayList<String> clist = new java.util.ArrayList<String>(this.groups.size());
	for(DBGroupInfo c: this.groups.values())
	{
	  clist.add(c.getGroupName());
	}
	return clist;
  }

  synchronized public boolean init(MetricsDbBase metricsDb)
  {
	  this.groups.clear();
	  this.mydbs.clear();
	  this.metricsDb = metricsDb;
	  return init();
  }
  synchronized public boolean init()
  {
    logger.info("Loading stored db info");
    mydbs.clear();//refill
	java.util.List<DBInstanceInfo> dbList = null;
	if(this.metricsDb!=null)
	{
		dbList = this.metricsDb.SearchDbInfo(null);
		logger.info("Find "+dbList.size()+" db info");
		for(DBInstanceInfo db: dbList)
		{
			DBGroupInfo cls = findGroup(db.getDbGroupName());
			if(cls==null)
			{
				cls = new DBGroupInfo();
				cls.setGroupName(db.getDbGroupName());
				this.groups.put(db.getDbGroupName(), cls);
			}
			cls.getInstances().add(db);
		}
		return true;
	}else
	{
		logger.warning("Cannot find stored db info because there is no metrics db");		
	}
	return false;
  }

  public MetaDB getMetaDb() 
  {
		return projectDb;
  }

  public void setMetricsDb(MetricsDbBase metricsDb) 
  {
    this.metricsDb = metricsDb;
  }

  public MetricsDbBase getMetricsDb() 
  {
		return this.metricsDb;
  }

  public void setMetaDb(MetaDB projectDb) 
  {
    this.projectDb = projectDb;
  }

  public MyDatabases getMyDatabases(String owner, boolean restricted)
  {
    MyDatabases mydb = null;
	synchronized (this.mydbs)
	{
	  if(this.mydbs.containsKey(owner))
	    return this.mydbs.get(owner);
	  mydb = new MyDatabases();
	  this.mydbs.put(owner, mydb);
	}
	//fill data from meta rep
	mydb.addDbs( this.projectDb.listMyDBs(owner, restricted));
	return mydb;
  }
	
  /**
   * Will list all DBs and the dbs with user specified credentials are listed at the top
   * @param user
   * @return
   */
  public List<String> listDbsByUserInfo(String user, boolean restricted)
  {
    Set<String> mydb = this.getMyDatabases(user, restricted).getMyDbList();
	List<String> alldb = new ArrayList<String>(this.groups.size());
		
	synchronized(this)
	{
	  for(String db:mydb)
	  {
	    if(this.groups.containsKey(db))
		  alldb.add(db);
	  }
	  if(!restricted)
	    for(String key: this.groups.keySet())
	    {
	      if(!mydb.contains(key))alldb.add(key);
	    }
	}
	return alldb;
  }  
	public void updateLastScanTime(String dbgroup, String dbhost, java.util.Date dt)
	{
	  synchronized (this.lastScanTime)
	  {
	    this.lastScanTime.put(dbgroup+":"+dbhost, dt);	  
	  }
	}
	
	public java.util.Date getLastAccessDate(String dbgroup, String dbhost)
	{
    synchronized (this.lastScanTime)
	  {       
		 return this.lastScanTime.get(dbgroup+":"+dbhost);
	  }
	  	
	}

	public DBInstanceInfo retrieveDBInfo(String dbGroupName, String hostName)
	{
		if(this.metricsDb!=null)
			return this.metricsDb.retrieveDBInfo(dbGroupName, hostName);
		return null;
	}
	
	public void upsertDBInfo(DBInstanceInfo dbinfo)
	{
		if(this.metricsDb!=null)
			this.metricsDb.upsertDBInfo(dbinfo);
		else 
			throw new RuntimeException("There is no metricsdb defined yet.");
	}
	
	public boolean removeDbGroup(String dbGroupName, String owner, boolean force)
	{
		if(this.metricsDb!=null)
			return this.metricsDb.removeDbGroup(dbGroupName, owner, force);
		return false;
	}
	
	public boolean removeDBInfo(String dbGroupName, String hostName,String owner, boolean force)
	{
		if(this.metricsDb!=null)
			return this.metricsDb.removeDBInfo(dbGroupName, hostName, owner, force);
		return false;
	}
	
	public java.util.List<DBInstanceInfo> SearchDbInfo(String keyword)
	{
		if(this.metricsDb!=null)
			return this.metricsDb.SearchDbInfo(keyword);
		return new java.util.ArrayList<DBInstanceInfo>();
		
	}
	public boolean enableSnmp(String dbGroup, String hostname, boolean enabled,String owner, boolean force)
	{
		if(this.metricsDb!=null)
			return this.metricsDb.enableSnmp(dbGroup, hostname, enabled, owner, force);
		return false;
	}
	public boolean enableMetrics(String dbGroup, String hostname, boolean enabled,String owner, boolean force)
	{
		if(this.metricsDb!=null)
			return this.metricsDb.enableMetrics(dbGroup, hostname, enabled, owner, force);
		return false;
	}
	
	synchronized public void renameDbGroup(String oldname, String newName)
	{
		//nothing to do
		if(!this.groups.containsKey(oldname.toLowerCase()) || newName == null || newName.isEmpty())
			return;
		DBGroupInfo dbgroups = this.groups.get(oldname.toLowerCase());
		this.groups.remove(oldname.toLowerCase());
		this.metricsDb.renameDbGroup(oldname, newName);
		List<DBInstanceInfo> instances = dbgroups.getInstances();
		dbgroups.setGroupName(newName.toLowerCase());
		for(DBInstanceInfo inst: instances)
			inst.setDbGroupName(newName.toLowerCase());
		this.groups.put(newName.toLowerCase(), dbgroups);
		this.projectDb.renameDbGroupName(oldname.toLowerCase(), newName.toLowerCase());
		for(Map.Entry<String, MyDatabases> entry: this.mydbs.entrySet())
		{
			entry.getValue().replaceDb(oldname.toLowerCase(), newName.toLowerCase());
		}
	}
	
	public boolean updateRestricteduserAcl(String username, String dbgroupname, boolean visible)
	{
	  if(this.projectDb.updateUserAcl(username, dbgroupname, visible))
	  {
		  MyDatabases mydb = this.getMyDatabases(username, true);
		  if(mydb != null)
		  {
			  if(visible)
				  mydb.addDb(dbgroupname);
			  else
			  {
				  revokeDBCredential(username, dbgroupname);
				  mydb.removeDb(dbgroupname);
			  }
		  }
		  return true;
	  }
	  return false;
	  
	}
	
	/**
	 * This method will copy db credential when assign a DB to a restricted user or default admin/scanner user.
	 *  Assume the assigner can access the underlying database.
	 * @param restrictedUserName
	 * @param username
	 * @param dbGroup
	 * @param dbuser
	 * @param password
	 */
   public void copyManagedDBCredential(String targetUserName, boolean restrictedUser,
			  String srcUsername, String dbGroup, String dbuser, String password)
	{
	  if(targetUserName == null || targetUserName.isEmpty() || targetUserName.equalsIgnoreCase(srcUsername)) 
		  return;
	  DBCredential cred2 = new DBCredential();
	  cred2.setAppUser(targetUserName);
	  cred2.setDbGroupName(dbGroup);
	  cred2.setUsername(dbuser);
	  cred2.setPassword(password);
	  getMetaDb().upsertDBCredential(cred2);
	  //note we handle the allowed database for restricted user in different way.
	  if(!restrictedUser)
		  getMyDatabases(cred2.getAppUser(), false).addDb(cred2.getDbGroupName());
	}

   /**
    * For now, only used to handle restricted user
    * @param owner
    * @param dbGroupName
    */
   public void revokeDBCredential(String owner, String dbGroupName)
   {
      getMetaDb().removeDBCredential(owner, dbGroupName);  	   
   }
}
