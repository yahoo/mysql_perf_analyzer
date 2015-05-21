/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.yahoo.dba.perf.myperf.springmvc.WebAppUtil;

/**
 * First we move all auth (authentication/authorization) related functions together
 * TODO add retry count
 * @author xrao
 *
 */
public class Auth {

	private MyPerfContext context;

	public Auth()
	{
		
	}
	
	public Auth(MyPerfContext ctx)
	{
		this.context = ctx;
	}
	
	
	/**
	 * Find user by name. Return null if not found
	 * @param name
	 * @return
	 */
	public AppUser findUserByName(String name)
	{
		if(name==null||name.isEmpty())return null;
	    String username = name.trim().toLowerCase();
	    //find the user from the system cache
	    AppUser appUser = this.context.getUserManager().getUser(username);
	    if(appUser != null)return appUser;
		 //find the user from db and add it to system cache
		appUser = this.context.getUserManager().retrieveUserInfoFromMetaDB(username);
		if(appUser!=null)
		      this.context.getUserManager().addUser(appUser);
		return appUser;    
	}
	
	public boolean login(AppUser appUser, HttpServletRequest request)
	{
		if(appUser==null)return false;
		
		//check if login session timed out
  	    HttpSession sess = request.getSession();
  	    if(sess!=null)//if no more session, we cannot login
  	    {
  	    	long stored_server_ts = System.currentTimeMillis();
  	    	if(sess.getAttribute(AppUser.SERVER_TS)!=null)
  	    		stored_server_ts = (Long)sess.getAttribute(AppUser.SERVER_TS);
  	    	int stored_seed = (int)(Math.random()*Integer.MAX_VALUE);
  	    	if(sess.getAttribute(AppUser.RANDOM_SEED)!=null)
  	    		stored_seed = (Integer)sess.getAttribute(AppUser.RANDOM_SEED);	  	    	
	  	    boolean authed = appUser.match(request.getParameter("s"), stored_server_ts, stored_seed);
	  	    if(authed)
	  	    {
	  	      String user = null;
			  try{user = WebAppUtil.findUserFromRequest(request);}catch(Exception ex){};
			  if(!appUser.getName().equalsIgnoreCase(user))
			  {
			    sess.invalidate();
			    sess = request.getSession(true);//get a new session
			  }
		    }
		    sess.setAttribute(AppUser.SESSION_ATTRIBUTE, appUser);
		    sess.removeAttribute(AppUser.SERVER_TS);
		    sess.removeAttribute(AppUser.RANDOM_SEED);
	  	    return authed;
  	    }
		return false;
	}
	public MyPerfContext getContext() {
		return context;
	}

	public void setContext(MyPerfContext context) {
		this.context = context;
	}	
}
