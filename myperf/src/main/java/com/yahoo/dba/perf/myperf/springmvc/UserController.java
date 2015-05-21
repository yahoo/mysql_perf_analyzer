/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.yahoo.dba.perf.myperf.common.*;

/**
 * User management
 * @author xrao
 *
 */
public class UserController  extends MyPerfBaseController{
  private static Logger logger = Logger.getLogger(UserController.class.getName());
  private String resetView = "resetpwd";

  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
  {
	AppUserManager um = this.frameworkContext.getUserManager();
    
	String a = req.getParameter("a");//user action src
    //only "reset" and "new" allowed no session
	if(!"reset".equalsIgnoreCase(a) && !"new".equals(a)&&!WebAppUtil.hasValidSession(req))
	{
	  return new ModelAndView(new RedirectView(nosessView)); 
	}

	int status = 0;
	String message = "OK";
	String name = req.getParameter("name");
	if(name!=null)name = name.trim().toLowerCase();
	String passwd = req.getParameter("password");
	String userprivilege = req.getParameter("userprivilege");
	String email = req.getParameter("email");
			
	if("reset".equalsIgnoreCase(a))
	{
	  if(name==null)
	  {
	    //no name provided, display initial form
		ModelAndView mv = new ModelAndView(this.getResetView());
		mv.addObject("status", "1");
		mv.addObject("help_key", "account");
		return mv;				
	  }else
	  {
		AppUser au = um.retrieveUserInfoFromMetaDB(name);
		if(au==null)
		{
		  status = -1;
		  message = "User name "+name+" does not match any user.";
		}
		else if(au.getEmail()!=null && !"NA".equalsIgnoreCase(au.getEmail()))
		{
		  //find user, check against email
		  if(!au.getEmail().equalsIgnoreCase(email))
		  {
		    status = -1;
		    message = "The email "+email+" you provided does not match the record for the user stored inside the system.";
		  }
		  else
		  {
		    String mypwd = AppUser.geterateRandomPwd();
			//reset the password
			if(um.storeNewPassword(au.getName(), mypwd))
			{	
			  um.updatePasswordCache(au.getName(), mypwd);
			  //send email
			  if(MailUtil.sendMail(au.getEmail(), "Performance Framework Access Reset", "Reset to: "+mypwd))
			  {
				status = 0;message = "Password has been reset. Please check your email.";
			  }
			  else
			  {
			    status = 0;message = "Password has been reset to "+mypwd;									
			  }
			}else							
			{
			  status = -1;message = "The system failed to auto reset the password. Please contact application administrator for help.";																
			}
		  }
		}//reset password
		else
		{
		  //password reset not allowed
		  status = -1;message = "Because there is no email stored for this user, the system cannot auto reset the password. Please contact application administrator for help.";						
		}
		ModelAndView mv = new ModelAndView(this.getResetView());
		mv.addObject("status", status);
	    mv.addObject("message", message);
		mv.addObject("signinview",this.nosessView);
		mv.addObject("help_key", "account");
		return mv;				
	  }//password reset handling
	}//password reset handling
	
	//session user
    AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));
	
    if(appUser!=null && appUser.isAdminUser() && a!=null && a.startsWith("other"))
    {
	  name = req.getParameter("userlist");//retrieve a userlist for managemnet
    }
	
    if(appUser!=null && name!=null && name.trim().length()>0)
	{
      if("show".equalsIgnoreCase(a))
	  {
	    AppUser au = um.getMetaDb().retrieveUserInfo(name);
		if(au!=null)
		{
		  ColumnDescriptor desc = new ColumnDescriptor();
		  desc.addColumn("name", false, 0);
		  desc.addColumn("email", false, 1);
		  desc.addColumn("privilege", true, 2);
		  desc.addColumn("verified", true, 3);
		  ResultList rList = new ResultList();
		  rList.setColumnDescriptor(desc);
		  ResultRow row = new ResultRow();
		  ArrayList<String> cols = new ArrayList<String>();
		  cols.add(au.getName());
		  cols.add(au.getEmail());
		  cols.add(String.valueOf(au.getUserprivilege()));
		  cols.add(au.isVerified()?"1":"0");
		  row.setColumns(cols);
		  rList.addRow(row);
		  ModelAndView mv = new ModelAndView(this.jsonView);
		  mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, 0, "OK"));
		  mv.addObject("help_key", "account");
		  return mv;
		}
	  }
      else if("new".equals(a) && appUser.isAdminUser() && !appUser.getName().equalsIgnoreCase(name))
	  {
	    AppUser newUser = um.addNewUser(name, passwd, userprivilege, email, true);
		if(newUser == null)
		{
		  message = "Cannot add user "+name+". Usename "+name+" has been taken.";
		}
		else
		{
		  logger.info("Add/update user "+newUser.getName());
		   message = "User "+name+" has been added.";
		}										
	  }
	  else if("selfpwd".equals(a)
			  ||(appUser.isAdminUser()&& "otherpwd".equals(a)))//change password
	  {
	    if(um.storeNewPassword("selfpwd".equals(a)?appUser.getName():name, passwd))
		{
		  um.updatePasswordCache("selfpwd".equals(a)?appUser.getName():name, passwd);
		  message = "Your password has been changed successfully.";
		}else 
		  message = "The attempt to change your password failed.";
	  }
	  else if("selfemail".equals(a)
			  ||(appUser.isAdminUser()&& "otheremail".equals(a)))//change password
	  {
	    if(um.storeEmail("selfpwd".equals(a)?appUser.getName():name, email))
		{
		  message = "Your email has been changed successfully.";
		  um.updateEmailCache("selfpwd".equals(a)?appUser.getName():name, email);
		}else 
		  message = "The attempt to change your email failed.";					
	  }
	  else if(appUser.isAdminUser() && "otherpriv".equals(a))//change user type, can only be done by admin
	  {
	    int p =0;
		try{p = Integer.parseInt(userprivilege);}catch(Exception ex){}
        if(um.storePrivilege(name, p))
		{
		  message = "The privilege for "+name+" has been changed successfully.";
		  um.updateprivilegeCache(name, p);
		}
        else 
		  message = "The attempt to change privilege for "+name+" failed.";					
	  }else if(appUser.isAdminUser() && "confirm".equals(a))
	  {
		  um.confirmNewUser(name, true);
		  AppUser newUser = um.getUser(name);
		  if(newUser.isVerified())
		  {
			  message = "User "+name+" confirmation has been updated.";
			  MailUtil.sendMail(newUser.getEmail(), "Confrmation of your account with MySQL Perf Analyzer", 
					  "The administrator has confirmed your user account. It is time to use it and enjoy it.");
		  }
		  else
			  message = "The attempt to confirm user "+name+" is failed.";			  
	  }
	} 
	else if("new".equals(a) && name!=null && !name.isEmpty())
	{//self registration
	  //only allow standard user
      status = 0;
	  AppUser newUser = um.addNewUser(name, passwd, "0", email, false);
	  if(newUser == null)
	  {
	    status = -1;
	    message = "Cannot add user "+name+". Usename "+name+" has been taken.";
	  }
	  else
	  {
	    logger.info("Add/update user "+newUser.getName());
	    MailUtil.sendMail(this.frameworkContext.getMyperfConfig().getAdminEmail(), "New User Request - MySQL Perf Analyzer", 
	    		"There is a pending user request, with user name "+newUser.getName()+", email address "+newUser.getEmail());
		message = "Your requets has been sent to administrator. If you do not recieve confirmation on time, please contact administrator by email " 
	    		  + this.frameworkContext.getMyperfConfig().getAdminEmail();
		status = 2;
	  }
	}
	else
	{
	  status = 1;
	}
    
    ModelAndView mv = new ModelAndView(this.getFormView());
	mv.addObject("userInfo", appUser);
	mv.addObject("status", status);
	mv.addObject("message", message);
	if("new".equals(a) && status!=2)
	  mv.addObject("a",1);
	else if("new".equals(a))
	{
	  mv.addObject("a",2);
      mv.addObject("signinview",this.nosessView);
	}
	else mv.addObject("a",0);
	if(appUser!=null && appUser.isAdminUser())
	{
	  List<AppUser> users = this.frameworkContext.getMetaDb().retrieveAllUsers();
	  List<AppUser> users2 = new java.util.ArrayList<AppUser>(users.size());
	  for(AppUser u:users)
	  {
	    if(!appUser.getName().equals(u.getName()))
	    	users2.add(u);
	  }
	  mv.addObject("users", users2);
	}
	mv.addObject("help_key", "account");
	  return mv;				
  }
  
	public String getResetView() {
		return resetView;
	}

	public void setResetView(String resetView) {
		this.resetView = resetView;
	}
}
