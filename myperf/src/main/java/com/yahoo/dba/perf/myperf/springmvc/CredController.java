/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;

public class CredController extends MyPerfBaseController
{
  private static Logger logger = Logger.getLogger(CredController.class.getName());
	
  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest request,
			HttpServletResponse resp) throws Exception 
  {	
	//at minimum, need a dbGroupName
	if(!WebAppUtil.hasValidSession(request))
	  return this.respondFailure("Session timed out", request);
   		
    String dbGroupName = request.getParameter("dbGroupName");
    if(dbGroupName == null || dbGroupName.isEmpty() 
    		|| this.getFrameworkContext().getDbInfoManager().findGroup(dbGroupName) == null)
      return this.respondFailure("Please provide valid DB group name", request);
    
	String task = request.getParameter("cmd");
	
	if(!"retrieve".equals(task) && !"update".equals(task))
	  return this.respondFailure("Only support retrieve and update tasks", request);

	AppUser appUser = AppUser.class.cast(request.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));

	if("retrieve".equals(task))
	{
      DBCredential cred = WebAppUtil.findDBCredential(this.getFrameworkContext(), dbGroupName, appUser);
	  String mysqlUserName = cred != null? cred.getUsername():"";
	  return this.respondSuccess(mysqlUserName, request);//use message to send mysql account name back
	}
	
	//check for restricted user
	if(appUser.isRestrictedUser())
	{
		Set<String> mydbs = this.frameworkContext.getDbInfoManager().getMyDatabases(appUser.getName(), true).getMyDbList();
		if(mydbs == null || !mydbs.contains(dbGroupName))
			return this.respondFailure("As a restricted user, you have no permission to use this db group yet: " +dbGroupName, request);
	}
	//now the only thing left is update
		
    String username = request.getParameter("username");
    String pw = request.getParameter("pw");
	
    if(username == null || username.isEmpty() 
    		|| pw == null || pw.isEmpty())
      return this.respondFailure("Pleaseb provide valid MySQWL account", request);

	DBGroupInfo dbGroup = this.frameworkContext.getDbInfoManager().findGroup(dbGroupName);; 

	boolean testCred = "y".equals(request.getParameter("test_cred"));
	String testResult = null;
	if(testCred)
	{
	  for(DBInstanceInfo db:dbGroup.getInstances())
	  {
	    testResult = DBUtils.testConnection(db, username, pw);
	    if(testResult == null)
	    {
	      break;
	    }else
	    {
	      //TODO catch the case the first server is offline	
	       return this.respondFailure(testResult, request);
	    }
	  }
	}
	//either test is not required, or passed
    DBCredential cred = new DBCredential();
    cred.setDbGroupName(dbGroupName);
    cred.setUsername(username);
	cred.setPassword(pw);
	cred.setAppUser(appUser.getName());
	this.frameworkContext.getMetaDb().upsertDBCredential(cred);
    this.frameworkContext.getDbInfoManager().getMyDatabases(cred.getAppUser(), appUser.isRestrictedUser()).addDb(cred.getDbGroupName());
	logger.info("Add new credential for "+cred.getDbGroupName()+", app user "+cred.getAppUser());
	if(testCred && testResult == null)
	{
		//we will save the cred for default user and the user used for metrics gathering
		//for easy on boarding
		this.frameworkContext.saveManagedDBCredentialForScanner(cred.getUsername(), 
				cred.getDbGroupName(), cred.getUsername(), cred.getPassword());	
	}
	return this.respondSuccess("MySQL account to access group " + dbGroupName +" has been updated successfully." , request);
  }

}


