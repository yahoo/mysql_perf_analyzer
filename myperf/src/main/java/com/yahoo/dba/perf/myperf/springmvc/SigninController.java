/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import com.yahoo.dba.perf.myperf.common.*;

/**
 * signin.html controller
 * @author xrao
 *
 */
public class SigninController extends AbstractController
{
  private MyPerfContext frameworkContext;
  private static final String DEFAULT_ERROR = "Invalid user name or password.";
  private static Logger logger = Logger.getLogger(SigninController.class.getName());
  private String loginFormView;
  private String loginSuccessView;
  private String setupView;
  
  public MyPerfContext getFrameworkContext() 
  {
    return frameworkContext;
  }

  public void setFrameworkContext(MyPerfContext frameworkContext) 
  {
    this.frameworkContext = frameworkContext;
  }


  @Override
  protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse resp) throws Exception 
  {
	logger.info("receive url path: "+request.getContextPath()+","+request.getRequestURI()+", "+request.getServletPath()+", parameters: "+request.getQueryString());
	boolean failed = false;     
	String message = null;
	String username = request.getParameter("name");
    if(username!=null)
    {
      username = username.trim().toLowerCase();
      //find the user from the system cache
      AppUser appUser = this.frameworkContext.getAuth().findUserByName(username);

      //sign in process
      boolean authed = this.frameworkContext.getAuth().login(appUser, request);

      if(authed)//display
	  {
	    String view = getLoginSuccessView();
	    //if admin user, and setup not done yet, send to setup.
	    if(appUser.isAdminUser() && !frameworkContext.getMyperfConfig().isConfigured())
	    	view = this.getSetupView();
	    else if(!appUser.isAdminUser()  && !appUser.isVerified())
	    {
	    	failed = true;
	    	message = "Your signup has not been confirmed by any administrator user yet.";
	    }
	    if(!failed)
	    {
	    	logger.info(appUser.getName()+" login, redirect to "+view);
	    	return new ModelAndView(new RedirectView(view));
	    }
	  }//if(appUser!=null && appUser.match(request.getParameter("pd"))
	  else
	  {
	    failed = true;
	    message = DEFAULT_ERROR;
	    
	  }
    }//if(username!=null)

    //not authenticated? Try again
    //TODO add retry count
    long server_ts = System.currentTimeMillis();
    int seed = (int)(Math.random()*Integer.MAX_VALUE);
    ModelAndView mv = new ModelAndView(getLoginFormView());
    mv.addObject("name", username);
	if(failed)mv.addObject("message", message);
	mv.addObject("help_key", "start");
	mv.addObject("server_ts", server_ts);
	mv.addObject("ars", seed);//ars: authentication random seed
	mv.addObject("setup", this.frameworkContext.getMyperfConfig().isConfigured()?1:0);
	//add store them in session
	request.getSession(true).setAttribute(AppUser.SERVER_TS, new Long(server_ts));
	request.getSession().setAttribute(AppUser.RANDOM_SEED, new Integer(seed));
	
	return mv;
  }

  public String getLoginFormView() 
  {
	return loginFormView;
  }

  public void setLoginFormView(String loginFormView) 
  {
	this.loginFormView = loginFormView;
  }

  public String getLoginSuccessView() 
  {
	return loginSuccessView;
  }

  public void setLoginSuccessView(String loginSuccessView) 
  {
	this.loginSuccessView = loginSuccessView;
  }

public String getSetupView() {
	return setupView;
}

public void setSetupView(String setupView) {
	this.setupView = setupView;
}
}
