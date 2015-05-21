/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.yahoo.dba.perf.myperf.db.UserDBConnections;

public class LogoutController  extends MyPerfBaseController{

  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
  {
    HttpSession sess = req.getSession();

	//do we have session
	if(sess!=null)
	{
	  UserDBConnections conns = UserDBConnections.class.cast(sess.getAttribute("UserDBConnections"));
	  sess.removeAttribute("UserDBConnections");
	  sess.invalidate();
				
	  new Thread(new LogoutCleaner(conns)).start();//make it async. 
				//TODO Add the thread handle for central process
	}

	ModelAndView mv = new ModelAndView(new RedirectView(this.getNosessView()));
	return mv;
  }
	
  private static class LogoutCleaner implements Runnable
  {
    private UserDBConnections conns;
			
	public LogoutCleaner(UserDBConnections conns)
	{
	  this.conns = conns;
	}

	public void run() 
	{	
	  if(conns!=null){conns.setValid(false);conns.close();}
	}		
  }		
}
