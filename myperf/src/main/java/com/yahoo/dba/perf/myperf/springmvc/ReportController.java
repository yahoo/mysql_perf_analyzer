/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.yahoo.dba.perf.myperf.common.*;

/**
 * This is the controller used to create and manage reports
 * @author xrao
 *
 */
public class ReportController extends AbstractController{
	private static Logger logger = Logger.getLogger(ReportController.class.getName());
	private String jsonView;
	private String  nosessView;
	private MyPerfContext frameworkContext;

	/**
	 * URL format: /report/{cmd}/{dbgrp}/{dbhost}/{alertType}/{ts}/{random}.html
	 * @param req
	 * @return
	 */
	private Map<String, String> parseURL(HttpServletRequest req)
	{
		  Map<String, String> reqParams = new HashMap<String, String>();
		  String path = req.getServletPath();
		  if(path!=null)
		  {
			  String[] paths = path.split("/");
			  if(paths.length>2)
			  {
				  reqParams.put(Constants.URL_PATH_CMD, paths[2]);
				  if(paths.length>3)
					  reqParams.put(Constants.URL_PATH_DBGROUP, paths[3]); 
				  if(paths.length>4)
					  reqParams.put(Constants.URL_PATH_DBHOST, paths[4]); 
				  if(paths.length>5)
					  reqParams.put(Constants.URL_PATH_ALERT_TYPE, paths[5]); 
				  if(paths.length>6)
					  reqParams.put(Constants.URL_PATH_START_TS, paths[6]); 
			  }
		  }
		  return reqParams;
	}

	/**
	 * If no permission, return false;
	 * @param req
	 * @param reqParams
	 * @return
	 */
	private boolean checkPermission(HttpServletRequest req, Map<String, String> pathParams)
	{
		String cmd = pathParams.get(Constants.URL_PATH_CMD);
		AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
		if(appUser == null) return false;//not allow anonymous access
			
		if("delete".equals(cmd))
		{
			//only admin is allowed
			if(!appUser.isAdminUser())return false;
		}
		//check db access
		String dbgrp = pathParams.get(Constants.URL_PATH_DBGROUP);
		DBCredential cred = null;
		try
		{
			cred = WebAppUtil.findDBCredential(this.frameworkContext, dbgrp, appUser);
		}catch(Throwable th){}
		
		return cred != null;//if we have valid credential, allow access
	}
	
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest req,
			HttpServletResponse resp) throws Exception {
		
		logger.info("receive url: "+req.getRequestURI()+", parameter: "+req.getQueryString());

		Map<String, String> pathParams = parseURL(req);
		if(!checkPermission(req, pathParams))
		{
			//if we reach here, something wrong
			ModelAndView mv = new ModelAndView(this.jsonView);
			mv.addObject("json_result", ResultListUtil.toJSONString(null, null, Constants.STATUS_BAD, "Access is denied for the resource you requested."));
			return mv;			
		}
		
		String cmd = pathParams.get(Constants.URL_PATH_CMD);//report command, create, list, get, or del
		 		 
		if("get".equalsIgnoreCase(cmd))
		{
			return download(pathParams, req,resp);
		}else if("list".equalsIgnoreCase(cmd))
		{
			return list(req,resp);
		}else if("del".equalsIgnoreCase(cmd)||"delete".equalsIgnoreCase(cmd))
		{
			return delete(req,resp);
		}
		ModelAndView mv = new ModelAndView(this.jsonView);
		mv.addObject("json_result", ResultListUtil.toJSONString(null, null, Constants.STATUS_BAD, "Requested service is not supported."));
		return mv;			
	}
	
	private ModelAndView delete(HttpServletRequest req,
			HttpServletResponse resp) throws Exception
	{
		int status = 0;
		String message = "OK";
		String fname = req.getParameter("file");//file name
		String id = req.getParameter("rpid");//report id
		ResultList rList = null;
		UserReportManager urm = this.getUserReportManager(req);
		try
		{
			//delete first, the  update the idx file, and re-list
			//get session
			String appUser = WebAppUtil.findUserFromRequest(req);
			if(appUser==null || urm==null)
			{
				status = -1;
				message="Not login or session timed out";
			}else
			{
				urm.deleteFile(fname, id);
				rList = urm.listResultList();
			}
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE,"Exception", ex);
		}
		ModelAndView mv = new ModelAndView(this.jsonView);
		mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, status, message));
		return mv;
	}
	private ModelAndView list(HttpServletRequest req,
			HttpServletResponse resp) throws Exception
	{
		int status = 0;
		String message = "OK";
		ResultList rList = null;
		UserReportManager urm = this.getUserReportManager(req);
		try
		{
			String appUser = WebAppUtil.findUserFromRequest(req);
			if(appUser==null||urm==null)
			{
				status = -1;
				message="Not login or session timed out";
			}else
			{
				rList = urm.listResultList();
			}
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE,"Exception", ex);
		}
		ModelAndView mv = new ModelAndView(this.jsonView);
		mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, status, message));
		return mv;
	}
	
	/**
	 * TS format: yyyy-mm-dd hh:mm:ss
	 * @param ts
	 * @return
	 */
	private String getDateFromTS(String ts)
	{
		if(ts==null||ts.isEmpty())
			return ts;
		String[] parts = ts.split(" ");
		return parts[0].replaceAll("-", "");
	}
	/**
	 * change yyyy-mm-dd hh:mm:ss to yyyymmddhhmmss
	 * @param ts
	 * @return
	 */
	private String getOriginalTS(String ts)
	{
		if(ts==null||ts.isEmpty())return ts;
		return ts.replace(" ", "").replaceAll("-", "").replaceAll(":", "");
	}
	private ModelAndView download(Map<String, String> pathParams, HttpServletRequest req,
			HttpServletResponse resp) throws Exception
	{
		File rootPath = this.frameworkContext.getAlertRootPath();
		String ts = pathParams.get(Constants.URL_PATH_START_TS);
		String dt = this.getDateFromTS(ts);
		File dir = new File(rootPath, dt);
		do
		{
			if(!dir.exists())
				break;
			String origTS = getOriginalTS(ts);
			File f = new File(dir, pathParams.get(Constants.URL_PATH_ALERT_TYPE)
					+"_"+pathParams.get(Constants.URL_PATH_DBGROUP)
					+"_"+pathParams.get(Constants.URL_PATH_DBHOST)
					+"_"+origTS+".txt");
			if(!f.exists())break;
			resp.setContentType("text/plain");  
			resp.setContentLength((int)f.length());  
			//resp.setHeader("Content-Disposition","attachment; filename=\""+f.getName()+"\"");  		
			byte[] buf = new byte[8192];
			int len = 0;
			OutputStream out = resp.getOutputStream();
			InputStream in = null;
			try
			{
				in = new FileInputStream(f);
				while( (len = in.read(buf))>0)
					out.write(buf,0,len);
			}catch(Exception ex)
			{
				
			}finally
			{
				try{if(in!=null)in.close();}catch(Exception iex){}
				out.flush();
			}
			return null;
		}while(false);

		//if we reach here, something wrong
		ModelAndView mv = new ModelAndView(this.jsonView);
		mv.addObject("json_result", ResultListUtil.toJSONString(null, null, Constants.STATUS_BAD, "Cannot find your alert report, try another one."));
		return mv;
	}

	public MyPerfContext getFrameworkContext() {
		return frameworkContext;
	}

	public void setFrameworkContext(MyPerfContext frameworkContext) {
		this.frameworkContext = frameworkContext;
	}

	public String getJsonView() {
		return jsonView;
	}

	public void setJsonView(String jsonView) {
		this.jsonView = jsonView;
	}

	private UserReportManager getUserReportManager(HttpServletRequest req)
	{
		
		String appUser = WebAppUtil.findUserFromRequest(req);
		if(appUser==null)return null;
		HttpSession sess = req.getSession();
		UserReportManager urm = UserReportManager.class.cast(sess.getAttribute("UserReportManager"));
		//if none, we need add one
		if(urm==null)
		{
			urm = new UserReportManager(WebAppUtil.findUserFromRequest(req),null);
			urm.init();
			sess.setAttribute("UserReportManager", urm);
		}
		return urm;
	}
	
	public String getNosessView() {
		return nosessView;
	}

	public void setNosessView(String nosessView) {
		this.nosessView = nosessView;
	}

}
