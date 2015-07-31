/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.yahoo.dba.perf.myperf.common.AppUser;
import com.yahoo.dba.perf.myperf.common.DBCredential;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.db.DBConnectionWrapper;
import com.yahoo.dba.perf.myperf.db.UserDBConnections;

public class WebAppUtil 
{
  private static Logger logger = Logger.getLogger(WebAppUtil.class.getName());
  public static final String DBGROUP = "group";
  public static final String DBHOST = "host";
	
  public static QueryParameters parseRequestParameter(HttpServletRequest req)
  {
    QueryParameters qps = new QueryParameters();
	//identify target db
    qps.setGroup(req.getParameter(DBGROUP));
	qps.setHost(req.getParameter(DBHOST));
	
	qps.setSql(req.getParameter("sql"));//sql handler  
	//qps.setRespFormat(req.getParameter("respFormat"));//don't care now
	try
	{
	  qps.setMaxRows(Integer.parseInt(req.getParameter("rows")));//records to show
	}catch(Exception ex){}
		
	Enumeration<String> enu = req.getParameterNames();
	while(enu.hasMoreElements())
	{
	  String pname = enu.nextElement().toString();
  	  if(pname.startsWith("p_"))
	  {
	    qps.getSqlParams().put(pname, req.getParameter(pname));
	  }
	}
	return qps;
  }

  public static void storeLastDbInfoRequest(String dbgroup, String host, HttpServletRequest req)
  {
    if(req.getSession()!=null)
	{
	  try
	  {
	    if(dbgroup==null)
	      req.getSession().removeAttribute(DBGROUP);
		else 
		  req.getSession().setAttribute(DBGROUP, dbgroup);
		if(host==null)
		  req.getSession().removeAttribute(DBHOST);
		else 
		  req.getSession().setAttribute(DBHOST, host);
				
	  }catch(Exception ex){}
	}		
  }
		
  /**
   * Use session attribute: APP_USER
   * @param req
   * @return
   */
  public static String findUserFromRequest(HttpServletRequest req)
  {
    if(req.getUserPrincipal()!=null)
	  return req.getUserPrincipal().getName();//find from security login JAAS
	if(req.getSession()!=null)
	{
	  try
	  {
	    return AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE)).getName();//find from session
	  }catch(Exception ex){}
	}
	return null;
  }
	
  public static AppUser retrieveUserFromRequest(HttpServletRequest req)
  {
	if(req.getSession()!=null)
	{
	  try
	  {
	    return AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));//find from session
	  }catch(Exception ex){}
	}
	return null;
  }

  public static boolean hasValidSession(HttpServletRequest req)
  {
	  AppUser appUser = retrieveUserFromRequest(req);
    return appUser !=null && appUser.isVerified();
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
	    logger.info(appUser.getName()+" get cred for "+cred.getAppUser());
	  }
	}catch(Exception ex)
	{			
	}
	return cred;
  }
	
	
  public static DBConnectionWrapper getDBConnection(HttpServletRequest req, MyPerfContext ctx, DBInstanceInfo dbinfo)
  {
	try
	{
	  //get session
	  HttpSession sess = req.getSession();
	  //get connection manager
	  UserDBConnections conns = UserDBConnections.class.cast(sess.getAttribute("UserDBConnections"));
	  //if none, we need add one
	  if(conns==null)
	  {
	    conns = new UserDBConnections();
	    conns.setFrameworkContext(ctx);
	    conns.setConnectionIdleTimeout(ctx.getConnectionIdleTime());
	    conns.setAppUser(WebAppUtil.findUserFromRequest(req));//we don't have validated user, too
	    sess.setAttribute("UserDBConnections", conns);
	  }
	  conns.closeExpired(ctx.getConnectionIdleTime());
	  //first try to get connection without cred, meaning from saved
	  DBConnectionWrapper connWrapper = conns.checkoutConnection(dbinfo, null);		
	  if(connWrapper!=null)
	  {
		  //we will waste one query to check connectivity to avoid annoying error message
		  if(DBUtils.checkConnection(connWrapper.getConnection()))
			  return connWrapper;
		  		  
		   closeDBConnection(req, connWrapper, true, false);
		   connWrapper = null;		 
	  }
			
	  //now get credential
	  AppUser appUser = null;
	  DBCredential cred = null;
	  appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));	
	  if(appUser==null)
	    throw new RuntimeException("No user found. Session might not be valid.");
	  cred = WebAppUtil.findDBCredential(ctx, dbinfo.getDbGroupName(), appUser);
	  if(cred==null||cred.getPassword()==null)
	    throw new RuntimeException("No valid credential provided for DB "+dbinfo.getDbGroupName());
	  if(dbinfo.isConnectionVerified()||!dbinfo.supportClusterQuery())
	  {
		  connWrapper = conns.checkoutConnection(dbinfo, cred);
		  if(!DBUtils.checkConnection(connWrapper.getConnection()))
		  {
			  closeDBConnection(req, connWrapper, true, false);
			  connWrapper = conns.checkoutConnection(dbinfo, cred);
		  }
	  }
	
	  if(connWrapper == null && dbinfo.supportClusterQuery())
	    connWrapper = conns.checkoutConnection(ctx.getDbInfoManager().findGroup(dbinfo.getDbGroupName()), cred);
	  
	  if(connWrapper==null)
	    throw new RuntimeException("failed to connect to target db ("+dbinfo+")");
	  return connWrapper;
	}catch(Throwable th)
	{
	  if(th instanceof RuntimeException)throw RuntimeException.class.cast(th);
	  throw new RuntimeException(th);
	}
  }


  public static void  closeDBConnection(HttpServletRequest req, DBConnectionWrapper conn, boolean hasError, boolean reuse)
  {
    try
	{
	  //get session
	  HttpSession sess = req.getSession();
	  //get connection manager
	  UserDBConnections conns = UserDBConnections.class.cast(sess.getAttribute("UserDBConnections"));
	  //if none, we need add one
	  if(conns==null)
	  {
	    //something wrong. 
	    logger.info("Cannot find connection manager");
	  }
	  else
	  {
	    if(hasError)conns.checkinConnectionOnError(conn);
		else if(reuse)conns.checkinConnection(conn);
		else conns.checkinConnectionAndClose(conn);
	  }
	}catch(Throwable th)
	{
	  if(th instanceof RuntimeException)throw RuntimeException.class.cast(th);
	  throw new RuntimeException(th);
	}
  }
	
}
