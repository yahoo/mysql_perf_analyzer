/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import com.yahoo.dba.perf.myperf.common.AppUser;
import com.yahoo.dba.perf.myperf.common.Constants;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.ResultListUtil;

/**
 * Base controller, includes some common functions, for example, to report
 * status and message from web calls.
 */
public abstract class MyPerfBaseController extends AbstractController{
	private static Logger logger = Logger.getLogger(MyPerfBaseController.class.getName());

	//We use protected to allow subclass to access them directly
	protected MyPerfContext frameworkContext;//configuration and resources
	protected String  nosessView;//no session view
	protected String formView;//if it is going to display a form, form view
	protected String jsonView;//if result will be sent back in json format
	private boolean requireLogin = true;//by default, we need login

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
	{
		setupDebug(req);
		
		boolean debug = this.isDebug(req);
		
		if(debug)logger.info("receive url path: "+req.getContextPath()+","+req.getRequestURI()+", "
	                +req.getServletPath()+", parameters: "+req.getQueryString());
	    
		AppUser user =retrieveAppUser(req);
		
		if(this.requireLogin && user == null )//check if user valid when require login
		{
			if(this.nosessView!=null)
			   return new ModelAndView(new RedirectView(nosessView)); 
			return respondFailure("Session expired. Please relogin and try again.", req);
		}else if(this.requireLogin && !user.isVerified())
		{
			//TODO use a message view
			return this.respondFailure("New user request has not been verified by administrator yet. Please contact administrator by email "
		      + this.frameworkContext.getMyperfConfig().getAdminEmail()+".", 
					req);
		}
		
		return handleRequestImpl(req, resp);
	}

	//Create an MV to ack failure
	protected ModelAndView respondFailure(String message, HttpServletRequest req)
	{
		return respondWithStatus(Constants.STATUS_BAD, message, req);
				
	}
	
	//create a simple response MV to ack success
	protected ModelAndView respondSuccess(String message, HttpServletRequest req)
	{
		return respondWithStatus(Constants.STATUS_OK, message, req);
	}
	
	//create a simple response MV to ack with provided status and message
	protected ModelAndView respondWithStatus(int status, String message, HttpServletRequest req)
	{
		ModelAndView mv = new ModelAndView(this.getJsonView());
		mv.addObject("json_result", "{\"resp\":{\"status\": " + status + ", \"message\": \""
		    + ResultListUtil.escapeJson(message)+"\"}}");
		return mv;
	}
	
	//all subclasss need implement this method
	protected abstract ModelAndView handleRequestImpl(HttpServletRequest req,
			HttpServletResponse resp) throws Exception;
	
	public MyPerfContext getFrameworkContext() {
		return frameworkContext;
	}
	public void setFrameworkContext(MyPerfContext frameworkContext) {
		this.frameworkContext = frameworkContext;
	}
	public String getNosessView() {
		return nosessView;
	}
	public void setNosessView(String nosessView) {
		this.nosessView = nosessView;
	}
	public String getFormView() {
		return formView;
	}
	public void setFormView(String formView) {
		this.formView = formView;
	}
	public String getJsonView() {
		return jsonView;
	}
	public void setJsonView(String jsonView) {
		this.jsonView = jsonView;
	}
	public boolean isRequireLogin() {
		return requireLogin;
	}
	public void setRequireLogin(boolean requireLogin) {
		this.requireLogin = requireLogin;
	}

	/**
	 * Whether debug is enabled at session level
	 * @param req
	 * @return
	 */
	protected boolean isDebug(HttpServletRequest req)
	{
		//if system level use debug, return true
		if(frameworkContext.isDebug())return true;
		if(req.getSession()!=null)
		{
		  try
		  {
		    if("1".equals(req.getSession().getAttribute(Constants.SESSION_DEBUG)))
		    		return true;
		  }catch(Exception ex){}
		}

		return false;
	}

	private void setupDebug(HttpServletRequest req)
	{
		String paramString = req.getQueryString();
		if(paramString != null && (paramString.indexOf("debug=y") >=0 || paramString.indexOf("DEBUG=Y") >=0 ))
			req.getSession().setAttribute(Constants.SESSION_DEBUG, "1");
		else if(paramString != null && (paramString.indexOf("debug=n") >=0 || paramString.indexOf("DEBUG=N") >=0 ))
			req.getSession().removeAttribute(Constants.SESSION_DEBUG);
		  
	}
	static protected AppUser retrieveAppUser(HttpServletRequest req)
	{
	    return AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));

	}

	public static String findUserFromRequest(HttpServletRequest req)
	{
	    if(req.getUserPrincipal()!=null)
		  return req.getUserPrincipal().getName();//find from security login JAAS
		if(req.getSession()!=null)
		{
		  try
		  {
		    return retrieveAppUser(req).getName();//find from session
		  }catch(Exception ex){}
		}
		return null;
	}

	/**
	 * Date range in yyyyMMddHHmmss format, for metrics and alert retrieval.
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public static String[] getDateRange(String startDate, String endDate)
	{
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");//metricsDB TS column format
		java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH");//URL request format
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date d1 = null;	
		if(startDate!=null && !startDate.isEmpty())
		{
			try
			{
				if(startDate.indexOf('-')<0)
					d1 = sdf.parse(startDate); 
				else
					d1 = sdf2.parse(startDate);
			}catch(Exception ex)
			{
			}
		}
		//default to one day earlier
		if(d1==null)
		{
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			d1 = c.getTime();			
		}
		String startDateFmt = sdf.format(d1);
	
		Date d2 = null;
		if(endDate!=null && !endDate.isEmpty())
		{
			try
			{
				if(endDate.indexOf('-')<0)
					d2 = sdf.parse(endDate);  
				else
					d2 = sdf2.parse(endDate);
			}catch(Exception ex)
			{
			}
		}
		//default to now
		if(d2==null)
		{
			d2 = new Date();
		}
		String endDateFmt = sdf.format(d2);
		return new String[]{startDateFmt, endDateFmt};
	}	
}
