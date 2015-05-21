/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;


import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;
import com.yahoo.dba.perf.myperf.process.MyProfiler;

public class ProfileController extends MyPerfBaseController
{
  private static Logger logger = Logger.getLogger(ProfileController.class.getName());
  
  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
			HttpServletResponse resp) throws Exception 
  {
    int status = Constants.STATUS_OK;
	String message = "OK";

	String group = req.getParameter("group");
	String host = req.getParameter("host");
	boolean explainPlan = "y".equalsIgnoreCase(req.getParameter("plan"));
	boolean sessionStatus = "y".equalsIgnoreCase(req.getParameter("st"));
	boolean profile = "y".equalsIgnoreCase(req.getParameter("pf"));
	String dbuser = req.getParameter("dbuser");
	String dbpwd = req.getParameter("dbpwd");
	String dbname = req.getParameter("dbname");
    String sqltext = req.getParameter("sqltext");
    String format = req.getParameter("format");
    boolean useJson = "json".equalsIgnoreCase(format) && explainPlan;
    if(useJson)
    {
    	sessionStatus = false;
    	profile = false;
    }
    logger.info("profiling "+sqltext);
	ResultList rList = null;
	LinkedHashMap<String, ResultList> listMap = new LinkedHashMap<String, ResultList>();
	String jsonString = null;
	MyProfiler profiler = new MyProfiler();
	DBInstanceInfo dbinfo = null;
	try
	{
	  dbinfo = this.frameworkContext.getDbInfoManager().findDB(group,host).copy();
	  if(dbuser==null||dbuser.isEmpty())
	  {
	    	//use build in credential
		AppUser appUser = null;
		DBCredential cred = null;
		appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));	
		if(appUser==null)
		    throw new RuntimeException("No user found. Session might not be valid.");
		cred = WebAppUtil.findDBCredential(this.frameworkContext, dbinfo.getDbGroupName(), appUser);
		if(cred!=null)
		{
			dbuser = cred.getUsername();
			dbpwd = cred.getPassword();
		}
	  }
	  profiler.setDbinfo(dbinfo);
	  profiler.setFrameworkContext(frameworkContext);
	  if(dbname!=null && !dbname.isEmpty())
	    dbinfo.setDatabaseName(dbname);
	  profiler.setSqlText(sqltext);
	  profiler.connect(dbuser, dbpwd, explainPlan && !profile && !sessionStatus);
	  if(useJson)
	  {
		 jsonString = profiler.runExplainPlanJson(); 
	  }
	  else if(explainPlan)listMap.put("plan", profiler.runExplainPlan());
	  if(profile)listMap.put("profile",profiler.runProfile());
	  if(sessionStatus)listMap.put("stats", profiler.runStats());
	}catch(Exception ex)
	{
	  status = Constants.STATUS_BAD;
	  message = "Error when profiling: "+ex.getMessage();
	  logger.log(Level.WARNING, "Error when profiling", ex);
	}finally
	{
	  if(profiler!=null)profiler.destroy();	
	}
	ModelAndView mv = null;
	mv = new ModelAndView(this.jsonView);
	if(req.getParameter("callback")!=null&&req.getParameter("callback").trim().length()>0)
	  mv.addObject("callback", req.getParameter("callback"));//YUI datasource binding
	if(jsonString==null)
		mv.addObject("json_result", ResultListUtil.toMultiListJSONStringUpper(listMap, null, status, message));
	else
		mv.addObject("json_result", jsonString);
	return mv;			
  }

}
