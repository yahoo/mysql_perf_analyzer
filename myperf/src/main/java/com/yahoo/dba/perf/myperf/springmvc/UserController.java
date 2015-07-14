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
 * Note the usage of model value a:
 *   0 self for user management from login user
 *   1 for self sign up
 *   2 action message after self signup/pwd reset
 *   3 for self reset
 * @author xrao
 *
 */
public class UserController  extends MyPerfBaseController{
  private static Logger logger = Logger.getLogger(UserController.class.getName());
  private static final int STATE_SELF_SIGNUP = 1;
  private static final int STATE_MANAGED_ACTION = 0;
  private static final int STATE_SELF_SIGNUP_POST_ACTION = 2;
  private static final int STATE_SELF_RESET = 3;
  
  private String resetView = "resetpwd";

  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
  {
	String actionSrc = req.getParameter("a");//user action src
    //only "reset" and "new" allowed no session
	if(!"reset".equalsIgnoreCase(actionSrc) && !"new".equals(actionSrc)&&!WebAppUtil.hasValidSession(req))
	{
	  return new ModelAndView(new RedirectView(nosessView)); 
	}

	AppUser appUser = AppUser.class.cast(req.getSession().getAttribute(AppUser.SESSION_ATTRIBUTE));

	//self signup
	if(appUser == null && "new".equalsIgnoreCase(actionSrc))
	{
		String name = req.getParameter("name");
		//form
		if(name == null || name.isEmpty())
			return this.showSelfSignupForm(req);
		//signup
		return this.handleSelfSignup(req);
	}
	
	//password reset, form and action
	if("reset".equalsIgnoreCase(actionSrc))
		return handleReset(req);
	
	//retrieve a user list
	if(appUser != null && appUser.isAdminUser() && "userlist".equalsIgnoreCase(actionSrc))
		return this.retrieveUserList(req);
	
	//retrieve all users with detail
	if(appUser != null && appUser.isAdminUser() && "listalldetails".equalsIgnoreCase(actionSrc))
		return this.listAllUsers(req);
	
	//retrieve new user list
	if(appUser != null && appUser.isAdminUser() && "newuserlist".equalsIgnoreCase(actionSrc))
		return this.retrieveNewUserList(req);
	
	//retrieve a user info
	if(appUser != null && appUser.isAdminUser() && "show".equalsIgnoreCase(actionSrc))
		return this.retrieveUserInfo(req);
	
	//for admin to create a new user
    if(appUser != null && appUser.isAdminUser() && "new".equalsIgnoreCase(actionSrc))
    {
    	return handleCreateUser(req);
    }
    //confirm a new user
    if(appUser != null && appUser.isAdminUser() && "confirm".equalsIgnoreCase(actionSrc))
    {
    	return this.handleConfirmNewUser(req);
    }

    //delete a user
    if(appUser != null && appUser.isAdminUser() && "delete".equalsIgnoreCase(actionSrc))
    {
    	return this.handleDelete(req);
    }
    
    //change privilege
    if(appUser != null && appUser.isAdminUser() && "otherpriv".equalsIgnoreCase(actionSrc))
    {
    	return this.handleChangePrivilege(req);
    }
    
    if(appUser != null && ("selfpwd".equals(actionSrc)
			  ||(appUser.isAdminUser() && "otherpwd".equals(actionSrc))))
    {
    	return this.handleChangePwd(req);
    }
    
    if(appUser != null && ("selfemail".equals(actionSrc)
			  ||(appUser.isAdminUser()&& "otheremail".equals(actionSrc))))
    {
    	return this.handleChangeEmail(req);
    }
    
    //anything else except display form is not valid
	if(appUser == null || (actionSrc != null && !actionSrc.isEmpty()))
	{		
		return new ModelAndView(new RedirectView(nosessView)); 
	}
	
        
	int status = Constants.STATUS_OK;
	String message = "";
    ModelAndView mv = new ModelAndView(this.getFormView());
	mv.addObject("userInfo", appUser);
	mv.addObject("status", status);
	mv.addObject("message", message);
	mv.addObject("a",STATE_MANAGED_ACTION);
	appendUserListToModel(req, mv);
	mv.addObject("help_key", "account");
	return mv;				
  }
  
  private ModelAndView showSelfSignupForm(HttpServletRequest req)
  {
	  ModelAndView mv = new ModelAndView(this.getFormView());
	  mv.addObject("a",STATE_SELF_SIGNUP); //a = 1 means a self signup form
	  mv.addObject("userInfo", new AppUser());
	  mv.addObject("help_key", "account");
	  return mv;
  }

  private ModelAndView handleConfirmNewUser(HttpServletRequest req)
  {
	  AppUserManager um = this.frameworkContext.getUserManager();
	  String name = req.getParameter("name");
	  if(name != null)
		  name = name.trim();
	  um.confirmNewUser(name, true);
	  AppUser newUser = um.getUser(name);
	  int status = Constants.STATUS_OK;
	  String message = null;
	  if(newUser.isVerified())
	  {
		  message = "User "+name+" confirmation has been updated.";
		  MailUtil.sendMail(newUser.getEmail(), "Confrmation of your account with MySQL Perf Analyzer", 
				  "The administrator has confirmed your user account. It is time to use it and enjoy it.");
	  }
	  else
	  {
		  status = Constants.STATUS_BAD;
		  message = "The attempt to confirm user "+name+" is failed.";
	  }
	  ModelAndView mv = new ModelAndView(this.getFormView());
	  mv.addObject("userInfo", retrieveAppUser(req));
	  mv.addObject("status",status);
	  mv.addObject("message", message);
	  mv.addObject("help_key", "account");
	  mv.addObject("a",STATE_MANAGED_ACTION);
	  appendUserListToModel(req, mv);
	  return mv;
  }

  private ModelAndView handleChangePrivilege(HttpServletRequest req)
  {
	  AppUserManager um = this.frameworkContext.getUserManager();
	  String name = req.getParameter("name");	  
	  if(name != null)
		  name = name.trim();
	  AppUser appUser = retrieveAppUser(req);
	  int status = Constants.STATUS_OK;
	  String message = null;
	  if(name == null || name.equalsIgnoreCase(appUser.getName()))
	  {
		  status = Constants.STATUS_BAD;
		  message = "Illegal operation. Cannot change privilege for yourself.";
	  }
	  else
	  {
		  int p =0;
		  String userprivilege = req.getParameter("userprivilege");
		  try{p = Integer.parseInt(userprivilege);}catch(Exception ex){}
	      if(um.storePrivilege(name, p))
	      {
	    	  message = "The privilege for "+name+" has been changed successfully.";
	    	  um.updateprivilegeCache(name, p);
	      }
	      else
	      {
	    	  status = Constants.STATUS_BAD;
			  message = "The attempt to change privilege for "+name+" failed.";
	      }
	  }
	  ModelAndView mv = new ModelAndView(this.getFormView());
	  mv.addObject("userInfo", retrieveAppUser(req));
	  mv.addObject("status",status);
	  mv.addObject("message", message);
	  mv.addObject("help_key", "account");
	  mv.addObject("a",STATE_MANAGED_ACTION);
	  appendUserListToModel(req, mv);
	  return mv;
  }

  private ModelAndView handleDelete(HttpServletRequest req)
  {
	  AppUserManager um = this.frameworkContext.getUserManager();
	  String name = req.getParameter("name");	  
	  if(name != null)
		  name = name.trim();
	  AppUser appUser = retrieveAppUser(req);
	  int status = Constants.STATUS_OK;
	  String message = null;
	  if(name == null || name.equalsIgnoreCase(appUser.getName()))
	  {
		  status = Constants.STATUS_BAD;
		  message = "Illegal operation. Cannot delete self .";
	  }
	  else
	  {
		  boolean ret = um.deleteUser(name);
		  if(ret)
		  {
			  message = "User " + name +" has been removed";			  
		  }else
		  {
			  status = Constants.STATUS_BAD;
			  message = "Failed to delete user " + name;
		  }
	  }
	  ModelAndView mv = new ModelAndView(this.getFormView());
	  mv.addObject("userInfo", retrieveAppUser(req));
	  mv.addObject("status",status);
	  mv.addObject("message", message);
	  mv.addObject("help_key", "account");
	  mv.addObject("a",STATE_MANAGED_ACTION);
	  appendUserListToModel(req, mv);
	  return mv;
  }

  private ModelAndView handleChangeEmail(HttpServletRequest req)
  {
	  AppUserManager um = this.frameworkContext.getUserManager();
	  String name = req.getParameter("name");	  
	  if(name != null)
		  name = name.trim();
	  String email = req.getParameter("email");
	  if(email != null)
		  email = email.trim();
	  AppUser appUser = retrieveAppUser(req);
	  String actionSrc = req.getParameter("a");
	  int status = Constants.STATUS_OK;
	  String message = null;
	  if(email == null || email.isEmpty() || email.indexOf('@') < 0)
	  {
		  status = Constants.STATUS_BAD;
		  message = "Please provide valid email address..";
	  }
	  else if(um.storeEmail("selfemail".equals(actionSrc)?appUser.getName():name, email))
	  {
		  message = "The email for " +("selfemail".equals(actionSrc)?appUser.getName():name) + " has been changed successfully.";
		  um.updateEmailCache("selfemail".equals(actionSrc)?appUser.getName():name, email);
	  }else
	  {
		  status = Constants.STATUS_BAD;
		  message = "The attempt to change email for " + ("selfemail".equals(actionSrc)?appUser.getName():name) + " failed.";					
	  }
	  ModelAndView mv = new ModelAndView(this.getFormView());
	  mv.addObject("userInfo", retrieveAppUser(req));
	  mv.addObject("status",status);
	  mv.addObject("message", message);
	  mv.addObject("help_key", "account");
	  mv.addObject("a",STATE_MANAGED_ACTION);
	  appendUserListToModel(req, mv);
	  return mv;
  }
  
  private ModelAndView handleChangePwd(HttpServletRequest req)
  {
	  AppUserManager um = this.frameworkContext.getUserManager();
	  String name = req.getParameter("name");	  
	  if(name != null)
		  name = name.trim();
	  String passwd = req.getParameter("password");
	  if(passwd != null)
		  passwd = passwd.trim();
	  AppUser appUser = retrieveAppUser(req);
	  String actionSrc = req.getParameter("a");
	  int status = Constants.STATUS_OK;
	  String message = null;
	  if(passwd == null || passwd.isEmpty())
	  {
		  status = Constants.STATUS_BAD;
		  message = "Empty password is not allowed.";
	  }
	  else if(um.storeNewPassword("selfpwd".equals(actionSrc)?appUser.getName():name, passwd))
	  {
		  um.updatePasswordCache("selfpwd".equals(actionSrc)?appUser.getName():name, passwd);
		  message = "Password for " +("selfpwd".equals(actionSrc)?appUser.getName():name)+ " has been changed successfully.";
	  }else 
	  {
		  status = Constants.STATUS_BAD;
		  message = "The attempt to change password of " + ("selfpwd".equals(actionSrc)?appUser.getName():name) + " failed.";
	  }
	  ModelAndView mv = new ModelAndView(this.getFormView());
	  mv.addObject("userInfo", retrieveAppUser(req));
	  mv.addObject("status",status);
	  mv.addObject("message", message);
	  mv.addObject("help_key", "account");
	  mv.addObject("a",STATE_MANAGED_ACTION);
	  appendUserListToModel(req, mv);
	  return mv;
  }
  private ModelAndView handleSelfSignup(HttpServletRequest req)
  {
	  AppUserManager um = this.frameworkContext.getUserManager();	  
	  ModelAndView mv = new ModelAndView(this.getFormView());
	  int status = Constants.STATUS_OK;
	  String message = "OK";
	  String name = req.getParameter("name");
	  if(name!=null)name = name.trim().toLowerCase();
	  String passwd = req.getParameter("password");
	  String userprivilege = "0"; //standard user
	  String email = req.getParameter("email");

	  do
	  {
		  if(name == null || name.isEmpty())
		  {
			  status = Constants.STATUS_BAD;
			  message = "Please provide username";
			  break;
		  }
		  if(passwd == null || passwd.isEmpty())
		  {
			  status = Constants.STATUS_BAD;
			  message = "Please provide password";
			  break;			  
		  }
		  if(email == null || email.isEmpty() || email.indexOf('@') < 0 )
		  {
			  status = Constants.STATUS_BAD;
			  message = "Please provide valid email address";
			  break;			  
		  }
		  AppUser newUser = um.addNewUser(name, passwd, userprivilege, email, false);
		  if(newUser == null)
		  {
			  status = Constants.STATUS_BAD;
			  message = "Cannot add user "+name+". Usename "+name+" has been taken.";
			  logger.info("Failed to add user "+ name);
		  }
		  else
		  {
			  logger.info("Add/update user "+newUser.getName());
			  MailUtil.sendMail(this.frameworkContext.getMyperfConfig().getAdminEmail(), "New User Request - MySQL Perf Analyzer", 
			    		"There is a pending user request, with user name "+newUser.getName()+", email address "+newUser.getEmail());
			  message = "Your requets has been sent to administrator. If you do not recieve confirmation on time, please contact administrator by email " 
			    		  + this.frameworkContext.getMyperfConfig().getAdminEmail();
			  mv.addObject("a",2);
		      mv.addObject("signinview",this.nosessView);
		  }		  
	  }while(false);
	  
	  AppUser tmpUser = new AppUser();
	  tmpUser.setName(name);
	  tmpUser.setEmail(email);
	  
	  mv.addObject("status", status);
	  mv.addObject("message", message);
	  mv.addObject("userInfo", new AppUser());
	  mv.addObject("tmpUser", tmpUser);
	  mv.addObject("a", status == Constants.STATUS_OK?STATE_SELF_SIGNUP_POST_ACTION:STATE_SELF_SIGNUP);
	  mv.addObject("help_key", "account");
	  return mv;					  
  }
  
  private ModelAndView handleCreateUser(HttpServletRequest req)
  {
	  AppUser appUser = retrieveAppUser(req);
	  AppUserManager um = this.frameworkContext.getUserManager();	  
	  ModelAndView mv = new ModelAndView(this.getFormView());
	  int status = Constants.STATUS_OK;
	  String message = "OK";
	  String name = req.getParameter("name");
	  if(name!=null)name = name.trim().toLowerCase();
	  String passwd = req.getParameter("password");
	  String userprivilege = req.getParameter("userprivilege");
	  String email = req.getParameter("email");

	  do
	  {
		  if(name == null || name.isEmpty())
		  {
			  status = Constants.STATUS_BAD;
			  message = "Please provide username";
			  break;
		  }
		  if(passwd == null || passwd.isEmpty())
		  {
			  status = Constants.STATUS_BAD;
			  message = "Please provide password";
			  break;			  
		  }
		  if(email == null || email.isEmpty() || email.indexOf('@') < 0 )
		  {
			  status = Constants.STATUS_BAD;
			  message = "Please provide valid email address";
			  break;			  
		  }
		  AppUser newUser = um.addNewUser(name, passwd, userprivilege, email, true);
		  if(newUser == null)
		  {
			  message = "Cannot add user "+name+". Usename "+name+" has been taken.";
			  logger.info("Failed to add user "+ name);
		  }
		  else
		  {
			  logger.info("Add/update user "+newUser.getName());
			  message = "User "+name+" has been added.";
		  }		  
	  }while(false);
	  
	  AppUser tmpUser = new AppUser();
	  tmpUser.setName(name);
	  tmpUser.setEmail(email);
	  tmpUser.setUserprivilege("1".equals(userprivilege)?1:0);
	  mv.addObject("tmpUser", tmpUser);
	  mv.addObject("userInfo", appUser);
	  mv.addObject("status", status);
	  mv.addObject("message", message);
	  mv.addObject("a",STATE_MANAGED_ACTION);	  
	  mv.addObject("help_key", "account");
	  appendUserListToModel(req, mv);
	  return mv;					  
  }
  
  private ModelAndView handleReset(HttpServletRequest req)
  {
	  int status = Constants.STATUS_OK;
	  String message = "OK";
	  String name = req.getParameter("name");
	  if(name!=null)name = name.trim().toLowerCase();
	  String email = req.getParameter("email");

	  if(name == null)
	  {
		  //no name provided, display initial form
		  ModelAndView mv = new ModelAndView(this.getResetView());
		  mv.addObject("status", "1");
		  mv.addObject("a", STATE_SELF_RESET);
		  mv.addObject("help_key", "account");
		  mv.addObject("userInfo", new AppUser());
		  return mv;				
	  }
	  
	  AppUserManager um = this.frameworkContext.getUserManager();
	  AppUser au = um.retrieveUserInfoFromMetaDB(name);
	  do
	  {
		  //not a valid user
		  if(au==null)
		  {
			  status = Constants.STATUS_BAD;
			  message = "User name "+name+" does not match any user.";
			  break;
		  }
		  
		  // no email stored for the account
		  if(au.getEmail() == null || "NA".equalsIgnoreCase(au.getEmail()))
		  {
			  status = Constants.STATUS_BAD;
			  message = "Because there is no email stored for this user, the system cannot auto reset the password. Please contact application administrator for help.";
			  break;
		  }
		 	  
		  //email not match
		  if(!au.getEmail().equalsIgnoreCase(email))
		  {
			  status = Constants.STATUS_BAD;
			  message = "The email "+email+" you provided does not match the record for the user stored inside the system.";
			  break;
		  }

		  String mypwd = AppUser.geterateRandomPwd();
		  //reset the password
		  if(um.storeNewPassword(au.getName(), mypwd))
		  {	
			  um.updatePasswordCache(au.getName(), mypwd);
			  //send email
			  if(MailUtil.sendMail(au.getEmail(), "Performance Framework Access Reset", "Reset to: "+mypwd))
			  {
				  message = "Password has been reset. Please check your email.";
				  break;
			  }
				  
			  message = "Password has been reset to "+mypwd;									
			  break;
		  }
		  status = Constants.STATUS_BAD;
		  message = "The system failed to auto reset the password. Please contact application administrator for help.";																
		  break;			  
	}while(false);
			
	ModelAndView mv = new ModelAndView(this.getResetView());
	mv.addObject("status", status);
	mv.addObject("message", message);
	mv.addObject("a", status == Constants.STATUS_OK?STATE_SELF_SIGNUP_POST_ACTION:STATE_SELF_RESET);
	mv.addObject("signinview",this.nosessView);
	mv.addObject("help_key", "account");
	mv.addObject("userInfo", new AppUser());
	return mv;
  }
  
  private ModelAndView retrieveUserInfo(HttpServletRequest req)
  {
	  AppUserManager um = this.frameworkContext.getUserManager();
	  String name = req.getParameter("name");
	  AppUser appUser = retrieveAppUser(req);
	  if(appUser == null )
		  name = null;
	  else if(!appUser.isAdminUser())
		  name = appUser.getName();
	  
	  AppUser au = um.getMetaDb().retrieveUserInfo(name);
	  ResultList rList = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  desc.addColumn("name", false, 0);
	  desc.addColumn("email", false, 1);
	  desc.addColumn("privilege", true, 2);
	  desc.addColumn("verified", true, 3);
	  rList.setColumnDescriptor(desc);
	  if(au!=null)
	  {
		  ResultRow row = new ResultRow();
		  ArrayList<String> cols = new ArrayList<String>();
		  cols.add(au.getName());
		  cols.add(au.getEmail());
		  cols.add(String.valueOf(au.getUserprivilege()));
		  cols.add(au.isVerified()?"1":"0");
		  row.setColumns(cols);
		  rList.addRow(row);
	  }
	  ModelAndView mv = new ModelAndView(this.jsonView);
	  mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, 0, "OK"));
	  return mv;
  }

  private ModelAndView retrieveUserList(HttpServletRequest req)
  {
	  List<AppUser> users = this.frameworkContext.getMetaDb().retrieveAllUsers();
	  AppUser appUser = retrieveAppUser(req);

	  ResultList rs = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  desc.addColumn("USERNAME", false, 1);
	  rs.setColumnDescriptor(desc);

	  for(AppUser u : users)
	  {
		  if(appUser.getName().equals(u.getName()))
			  continue;
		  ResultRow row = new ResultRow();
		  row.setColumnDescriptor(desc);
		  row.addColumn(u.getName());
		  rs.addRow(row);
	  }
	  ModelAndView mv = new ModelAndView(this.jsonView);
	  mv.addObject("json_result", ResultListUtil.toJSONString(rs, null, 0, "OK"));
	  return mv;
  }

  private ModelAndView retrieveNewUserList(HttpServletRequest req)
  {
	  List<AppUser> users = this.frameworkContext.getMetaDb().retrieveAllUsers();
	  AppUser appUser = retrieveAppUser(req);

	  ResultList rs = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  desc.addColumn("USERNAME", false, 1);
	  rs.setColumnDescriptor(desc);

	  for(AppUser u : users)
	  {
		  if(appUser.getName().equals(u.getName())
				  || u.isVerified())
			  continue;
		  ResultRow row = new ResultRow();
		  row.setColumnDescriptor(desc);
		  row.addColumn(u.getName());
		  rs.addRow(row);
	  }
	  ModelAndView mv = new ModelAndView(this.jsonView);
	  mv.addObject("json_result", ResultListUtil.toJSONString(rs, null, 0, "OK"));
	  return mv;
  }

  private ModelAndView listAllUsers(HttpServletRequest req)
  {
	  List<AppUser> users = this.frameworkContext.getMetaDb().retrieveAllUsers();
	  AppUser appUser = retrieveAppUser(req);

	  ResultList rs = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  desc.addColumn("USERNAME", false, 1);
	  desc.addColumn("EMAIL", false, 2);
	  desc.addColumn("USERTYPE", false, 3);
	  desc.addColumn("CONFIRMED", false, 4);
	  rs.setColumnDescriptor(desc);

	  for(AppUser u : users)
	  {
		  ResultRow row = new ResultRow();
		  row.setColumnDescriptor(desc);
		  row.addColumn(u.getName());
		  row.addColumn(u.getEmail());
		  String userType = "Standard User";
		  if(u.isAdminUser())
			  userType = "Power User";
		  else if(u.isRestrictedUser())
			  userType = "Restricted User";
		  row.addColumn(userType);
		  row.addColumn(u.isVerified()?"Yes":"No");
		  rs.addRow(row);
	  }
	  ModelAndView mv = new ModelAndView(this.jsonView);
	  mv.addObject("json_result", ResultListUtil.toJSONString(rs, null, 0, "OK"));
	  return mv;
  }

  private void appendUserListToModel(HttpServletRequest req, ModelAndView mv)
  {
	  AppUser appUser = retrieveAppUser(req);
	  if(appUser == null || !appUser.isAdminUser()) return;
	  List<String> allUsers = new ArrayList<String>();
	  List<String> newUsers = new ArrayList<String>();
	  List<AppUser> userList = this.frameworkContext.getMetaDb().retrieveAllUsers();
	  for(AppUser u: userList)
	  {
		  if(!u.getName().equalsIgnoreCase(appUser.getName()))
			  allUsers.add(u.getName());
		  if(!u.isVerified())
			  newUsers.add(u.getName()); 
	  }
	  mv.addObject("allUsers", allUsers);
	  mv.addObject("newUsers", newUsers);
	  mv.addObject("mydbs", this.frameworkContext.getDbInfoManager()
				.listDbsByUserInfo(WebAppUtil.findUserFromRequest(req), retrieveAppUser(req).isRestrictedUser()));
	  logger.info("Total user list: "+userList.size()+", all: "+allUsers.size()+", new: "+newUsers.size());
  }
	public String getResetView() {
		return resetView;
	}

	public void setResetView(String resetView) {
		this.resetView = resetView;
	}
}
