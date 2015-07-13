/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;

public class DbController extends MyPerfBaseController
{
  private static Logger logger = Logger.getLogger(DbController.class.getName());
  private DBInfoManager dbManager;
  @Override
  public void setFrameworkContext(MyPerfContext frameworkContext) 
  {
    this.frameworkContext = frameworkContext;
	this.dbManager = this.frameworkContext.getDbInfoManager();
  }


  private DBInstanceInfo parseRequest(HttpServletRequest request)
  {
    DBInstanceInfo db = new DBInstanceInfo();
	db.setDbType(request.getParameter("dbtype"));
	String grp = request.getParameter("dbGroupName");
	if(grp != null)grp = grp.trim().toLowerCase();
	db.setDbGroupName(grp);
	String host = request.getParameter("hostName");
	if(host != null)host = host.trim().toLowerCase();
	db.setHostName(host);
	db.setPort(request.getParameter("port"));
	db.setDatabaseName(request.getParameter("databaseName"));
	db.setUseTunneling("1".equals(request.getParameter("useTunneling")));
	if(db.isUseTunneling())
	{
		db.setLocalHostName(request.getParameter("localHostName"));
		db.setLocalPort(request.getParameter("localPort"));
	}
	db.setStoreCredential("1".equals(request.getParameter("storeCredential")));
	db.setUsername(request.getParameter("username"));
	db.setPassword(request.getParameter("password"));
	db.setTestConnection("1".equals(request.getParameter("testConnection")));
	return db;
  }
  
  private String validate(String cmd, DBInstanceInfo db)
  {
	String msg = null;
	if(db==null || cmd == null)
	  msg = "No DB or action specified";
	else if(db.getDbGroupName()==null||db.getDbGroupName().isEmpty())
      msg = "Database group name is not provided";
	else if((db.getHostName()==null||db.getHostName().isEmpty() )
			  &&(cmd.equals(String.valueOf(Constants.DBM_ACTION_ADD_HOST))
			 || cmd.equals(String.valueOf(Constants.DBM_ACTION_UPDATE_HOST))
			 ||cmd.equals(String.valueOf(Constants.DBM_ACTION_REMOVE_HOST))))
	    msg = "Database host name is not provided";
	  else if(cmd.equals(String.valueOf(Constants.DBM_ACTION_ADD_HOST))
				 || cmd.equals(String.valueOf(Constants.DBM_ACTION_UPDATE_HOST)))
	  {
		//port
		int p = 0;
		try{p = Integer.parseInt(db.getPort());}catch(Exception ex){}
		if(p<=0 || p>65555)
		  msg = "Invalid port number: "+db.getPort();	  
	  }
		  
	return msg;
  }
  
  private ModelAndView processAddUpdate(HttpServletRequest request, HttpServletResponse response, 
		  String cmd, DBInstanceInfo db)
  {
	//only admin/standard user can add new dbs
	AppUser appUser = retrieveAppUser(request);
	if(appUser == null || appUser.isRestrictedUser())
		return this.respondFailure("Restricted user is not allowed to add or modify databases.", request);
	
	int status = Constants.STATUS_OK;
	String message = null;
	  
	//for add or update
	//check if we will add a group of dbs using range format of [nnn-mmm]
	boolean isGroupAdd = false;
	String groupStart = null;
	String groupEnd = null;
	String origHostName = db.getHostName();
	if(cmd.equals(String.valueOf(Constants.DBM_ACTION_ADD_HOST)))
	{
		Pattern pt = Pattern.compile("\\[(\\d+)\\-(\\d+)\\]");
		Matcher mt = pt.matcher(origHostName);
		isGroupAdd = mt.find();
		if(isGroupAdd)
		{
			groupStart = mt.group(1);
			groupEnd = mt.group(2);
		}
	}
		  
	if(db.isTestConnection())
	{
		if(isGroupAdd)//only test first one
			db.setHostName(origHostName.replaceFirst("\\[\\d+\\-\\d+\\]", groupStart));
		
		message = DBUtils.testConnection(db, db.getUsername(), db.getPassword());
		if(message==null||message.isEmpty())
			db.setConnectionVerified(true);
		else
			status  = Constants.STATUS_BAD;
	}
	if(message==null||message.isEmpty())
	{
		String dbNumberStr = groupStart;
		int strLen = groupStart!=null?groupStart.length():0;
		int startNumber = 0;
		int endNumber = 0;
		int dbNumber = 0;
		if(isGroupAdd)
		{
			try
			{
				startNumber = Integer.parseInt(groupStart);
				endNumber = Integer.parseInt(groupEnd);
				dbNumber = startNumber;
			}catch(Exception ex)
			{
				
			}
		}
		int count = 0;
		do
		{
			//store it
			DBInstanceInfo db2 = db.copy();
			if(isGroupAdd)
				db2.setHostName(origHostName.replaceFirst("\\[\\d+\\-\\d+\\]", dbNumberStr));
			this.frameworkContext.getDbInfoManager().upsertDBInfo(db2);
			//now get it back
			DBInstanceInfo dbinfo = this.frameworkContext.getDbInfoManager()
		    		.retrieveDBInfo(db2.getDbGroupName(), db2.getHostName());
			if(dbinfo == null)
			{
				message = "Failed to add or update db server: "+db2.getHostName();
			    status  = Constants.STATUS_BAD;
			} 
			else
			{
				this.dbManager.addOrUpdateInstance(dbinfo);//update cache
				this.frameworkContext.getInstanceStatesManager().addInstanceStates(dbinfo.getDbid());
			}
			count++;
			if(!isGroupAdd)break;
			if(dbNumber>=endNumber||count>=1000)break;
			dbNumber++;
			dbNumberStr = String.format("%0"+strLen+"d", dbNumber);
		}while(true);
		if(status==Constants.STATUS_OK)
		{
			message = "Database server (" +db.getDbGroupName()+", "+origHostName+") has been added or updated, "+count+" servers.";
		}
		//store db user name or password if specified
		//or if this user has none stored
		DBCredential mycred = DBUtils.findDBCredential(this.frameworkContext, db.getDbGroupName(), WebAppUtil.retrieveUserFromRequest(request));
		boolean recordPwd = (mycred == null && db.isTestConnection())? true: false;
		
		if(db.isStoreCredential() || recordPwd)
		{
			DBCredential cred = new DBCredential();
			cred.setAppUser(WebAppUtil.findUserFromRequest(request));
			cred.setDbGroupName(db.getDbGroupName());
			cred.setUsername(db.getUsername());
			cred.setPassword(db.getPassword());
			this.frameworkContext.getMetaDb().upsertDBCredential(cred);
			this.dbManager.getMyDatabases(cred.getAppUser(), retrieveAppUser(request).isRestrictedUser()).addDb(cred.getDbGroupName());
			
			if(db.isTestConnection()) //only store tested credential
			{
				//we will save the cred for default user and the user used for metrics gathering
				//for easy on boarding
				this.frameworkContext.saveManagedDBCredentialForScanner(cred.getUsername(), 
					cred.getDbGroupName(), cred.getUsername(), cred.getPassword());
			}
		 }
	}
	if(message == null)message = "OK";
	return this.respondWithStatus(status, message, request);
  }

  private ModelAndView processRenameCluster(HttpServletRequest request, HttpServletResponse response, 
		  String cmd, DBInstanceInfo db)
  {
	int status = Constants.STATUS_BAD;
	String message = null;
	String grp = request.getParameter("dbGroupName");
	String newGrp = request.getParameter("dbNewGroupName");
	if(newGrp != null)newGrp = newGrp.trim();
	if(newGrp == null || newGrp.isEmpty())
		message = "Please provide new group name.";	
	else if(grp == null || grp.isEmpty())
		message = "Please provide original group name.";
	else if(newGrp.equalsIgnoreCase(grp))
		message = "New Group Name is the same as old name, ignored.";
	else if(this.frameworkContext.getDbInfoManager().findGroup(newGrp) != null)
		message = "Grpup name " + newGrp +" is an existing group name. Please use different name.";
	else
	{
		try
		{
			this.frameworkContext.getDbInfoManager().renameDbGroup(grp, newGrp);
		}catch(Exception ex)
		{
			message = "Failed to rename db group: " + ex.getMessage();
		}
	}
	if(message == null)
	{
		message = "DB GROUP " + grp+" has been renamed. Please refresh your pages to verify.";
		status = Constants.STATUS_OK;
	}
	return this.respondWithStatus(status, message, request);
  }

  private ModelAndView processACL(HttpServletRequest request, HttpServletResponse response, 
		  String cmd, DBInstanceInfo db)
  {
	int status = Constants.STATUS_BAD;
	String message = null;
	String grp = request.getParameter("dbGroupName");
	String username = request.getParameter("username");
	boolean visible = "1".equals(request.getParameter("visible"));
	if(grp != null)grp = grp.trim();
	if(grp == null || grp.isEmpty())
		message = "Please provide db group name.";	
	else if(username == null || username.isEmpty())
		message = "Please provide restricted user name.";
	else if(this.frameworkContext.getUserManager().getUser(username) == null )
		message = "Please provide a valie restricted user name";
	else if(!this.frameworkContext.getUserManager().getUser(username).isRestrictedUser() )
		message = "Please provide a valie restricted user name";
	else
	{
		try
		{
			if(!this.frameworkContext.getDbInfoManager().updateRestricteduserAcl(username, grp, visible))
			{
				message = "Failed, please check log.";
			}else if(visible)
			{
				//copy db credential. revoke is handled by update directly
				AppUser appUser = retrieveAppUser(request);
				DBCredential cred = this.frameworkContext.getMetaDb().retrieveDBCredential(appUser.getName(), grp);
				if(cred != null)
					this.frameworkContext.getDbInfoManager().copyManagedDBCredential(username, true, 
						appUser.getName(), grp, cred.getUsername(), cred.getPassword());
			}
		}catch(Exception ex)
		{
			message = "Failed to update ACL for user: "+ username+", group: " + ex.getMessage();
		}
	}
	if(message == null)
	{
		message = "ACL for user "+ username + ", DB group " + grp+" has been updated.";
		status = Constants.STATUS_OK;
	}
	return this.respondWithStatus(status, message, request);
  }

  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest request,
			HttpServletResponse response) throws Exception 
  {
    //check if just for page display
    String cmd = request.getParameter("dbAction");
    if(cmd==null || cmd.isEmpty())
    {
		ModelAndView mv = new ModelAndView(this.getFormView());
		mv.addObject("help_key", "dbinfo");
		mv.addObject("mydbs", this.frameworkContext.getDbInfoManager()
				.listDbsByUserInfo(WebAppUtil.findUserFromRequest(request), retrieveAppUser(request).isRestrictedUser()));
		
		  AppUser appUser = retrieveAppUser(request);
		  if(appUser == null || !appUser.isAdminUser()) return mv;
		  List<String> restrictedUsers = new ArrayList<String>();
		  List<AppUser> userList = this.frameworkContext.getMetaDb().retrieveAllUsers();
		  for(AppUser u: userList)
		  {
			  if(u.isRestrictedUser())
				  restrictedUsers.add(u.getName());
		  }
		  mv.addObject("restrictedUsers", restrictedUsers);
		return mv;    	
    }
    
	DBInstanceInfo db = parseRequest(request);
	AppUser user = AppUser.class.cast(request.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
	
	logger.info("action: "+cmd+", db "+db.toString()+", by user "+user.getName());
	String message = this.validate(cmd, db);
	if(message != null && !message.isEmpty())
		return this.respondFailure(message, request);
	
	int status = Constants.STATUS_OK;//status 0 meaning job is well done
		
	//for add or update
	if(cmd.equals(String.valueOf(Constants.DBM_ACTION_ADD_HOST))
			|| cmd.equals(String.valueOf(Constants.DBM_ACTION_UPDATE_HOST)))
		return processAddUpdate(request, response, cmd,  db);
	else if(cmd.equals(String.valueOf(Constants.DBM_ACTION_RENAME_CLUSTER)))
		return processRenameCluster(request, response, cmd,  db);
	else if(cmd.equals(String.valueOf(Constants.DBM_ACTION_ACL)))
		return processACL(request, response, cmd,  db);
	else if(cmd.equals(String.valueOf(Constants.DBM_ACTION_REMOVE_CLUSTER)))//remove a group
	{
	  if(this.frameworkContext.getDbInfoManager().removeDbGroup(db.getDbGroupName(),
			  user.getName(), user.isAdminUser()))
	  {
		java.util.List<Integer> dbidList = new java.util.ArrayList<Integer>();
		for(DBInstanceInfo dbinfo: this.frameworkContext.getDbInfoManager().findGroup(db.getDbGroupName()).getInstances())
		{
			dbidList.add(dbinfo.getDbid());
		}
	    this.dbManager.removeGroup(db.getDbGroupName());
	    message = "Database group " + db.getDbGroupName()+" has been removed.";
	    //now purge all metrics
	    int[] dbids = new int[dbidList.size()];
	    for(int dbi = 0; dbi<dbidList.size(); dbi ++)
	    	dbids[dbi] = dbidList.get(dbi);
	    this.frameworkContext.getMetricDb().purgeMetricsForDbInstance(dbids);
	  }else
	  {
	    status = Constants.STATUS_BAD;
	    message = "Failed to remove database group " + db.getDbGroupName()+". Only the owner or administrator can do it.";
	  }
	  //TODO signal to purge all metrics
	  return this.respondWithStatus(status, message, request);
	}
	else if(cmd.equals(String.valueOf(Constants.DBM_ACTION_REMOVE_HOST)))//remove a server
	{
	  if(this.frameworkContext.getDbInfoManager().removeDBInfo(db.getDbGroupName(), db.getHostName(), 
			  user.getName(), user.isAdminUser()))
	  {
		boolean removed = false;  
		DBInstanceInfo dbInfo = this.dbManager.findDB(db.getDbGroupName(), db.getHostName());
		int dbid = dbInfo.getDbid();
		if(dbInfo!=null)
			removed = this.frameworkContext.getInstanceStatesManager().removeInstanceStates(dbInfo.getDbid());
		removed = this.dbManager.removeDBHost(db.getDbGroupName(), db.getHostName());
	    message = "Database server (" + db.getDbGroupName()+", "+ db.getHostName()+") has been removed: "+removed;
	    //purge metrics
	    this.frameworkContext.getMetricDb().purgeMetricsForDbInstance(new int[]{dbid});
	  }else
	  {
	    status = Constants.STATUS_BAD;
	    message = "Failed to remove database server (" + db.getDbGroupName()+", "+ db.getHostName()+"). Only the owner or administrator can do it.";
	  }
	  return this.respondWithStatus(status, message, request);	  
	}

	logger.info("action = "+cmd+", status = "+status+", message = "+message);
	
	ModelAndView mv = new ModelAndView(this.getFormView());
	mv.addObject(status==0?"okmessage":"message", message);
    if(status==0)
      mv.addObject("db", new DBInstanceInfo());//return an empty object
    else
    {	
      mv.addObject("db", db);
    }	
	mv.addObject("help_key", "dbinfo");
	mv.addObject("dbAction", cmd);
	return mv;
  }

}
