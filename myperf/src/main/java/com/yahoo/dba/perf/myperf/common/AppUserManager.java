/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.Serializable;
import java.util.Map;

import com.yahoo.dba.perf.myperf.meta.MetaDB;

public class AppUserManager implements Serializable{
  private static final long serialVersionUID = 1L;
  private Map<String, AppUser> userMap = new java.util.HashMap<String, AppUser>();
  private MetaDB metaDb; //to access meta db for persistent actions
  
  public AppUserManager(){}
  
  /**
   * get user by id
   * @param name
   * @return
   */
  synchronized public AppUser getUser(String name)
  {
	if(name == null || name.isEmpty()) return null;
    if(this.userMap.containsKey(name))return this.userMap.get(name);
    
	if(this.metaDb != null)
	{
		AppUser appUser = this.metaDb.retrieveUserInfo(name);
		if(appUser != null)this.addUser(appUser);
		return appUser;
	}
	return null;
  }
	
  synchronized public void addUser(AppUser user)
  {
    this.userMap.put(user.getName(), user);
  }
	
  public java.util.Map<String, AppUser> getUserMap()
  {
    return java.util.Collections.unmodifiableMap(this.userMap);
  }

  public MetaDB getMetaDb() {
	return metaDb;
  }

  public void setMetaDb(MetaDB metaDb) {
	this.metaDb = metaDb;
  }
  
  public AppUser retrieveUserInfoFromMetaDB(String username)
  {
    return metaDb.retrieveUserInfo(username);
  }

  public boolean storeNewPassword(String username, String newpwd)
  {
    return this.metaDb.changePasssword(username, newpwd);
  }
  
  public void updatePasswordCache(String username, String passwd)
  {
	AppUser au = this.getUser(username.toLowerCase());
	if(au!=null)
	  au.setMd5Hash(au.calMd5(passwd));
  }
  
  public boolean storeEmail(String username, String email)
  {
    return this.metaDb.changeEmail(username, email);
  }

  public void updateEmailCache(String username, String email)
  {
    AppUser au = this.getUser(username.toLowerCase());
	if(au!=null)
      au.setEmail(email);
  }

  public boolean storePrivilege(String username, int priv)
  {
    return this.metaDb.changePrivilege(username, priv);
  }

  public void updateprivilegeCache(String username, int priv)
  {
    AppUser au = this.getUser(username.toLowerCase());
	if(au!=null)
      au.setUserprivilege(priv==AppUser.PRIV_USER_POWER?AppUser.PRIV_USER_POWER:AppUser.PRIV_USER_STANDARD);
  }

  public void confirmNewUser(String username, boolean confirmed)
  {
	if(this.metaDb.confirmUser(username, confirmed))
	{
		AppUser au = this.getUser(username.toLowerCase());
		if(au!=null)
			au.setVerified(confirmed);
	}
  }
  
  public boolean deleteUser(String username)
  {
	  boolean ret =  this.metaDb.deleteUser(username);
	  if(ret)
		  this.userMap.remove(username);
	  return ret;
  }
  /**
   * Add a new user. Return AppUser object if succeed. Otherwise null.
   * @param userame
   * @param pwd
   * @param priv
   * @param email
   * @param confirmed If a user is added by admin, set it as confirmed
   * @return
   */
  public AppUser addNewUser(String username, String pwd, String priv, String email, boolean confirmed)
  {
    if(this.retrieveUserInfoFromMetaDB(username)!=null)
      return null;	
			
    AppUser newUser = new AppUser();
	newUser.setName(username.trim().toLowerCase());
	newUser.setPassword(pwd);
	newUser.setUserprivilege(AppUser.PRIV_USER_STANDARD);
	newUser.setEmail(email);
	newUser.setVerified(confirmed);
	try
	{
	  int privInt = Integer.parseInt(priv);
	  if(privInt == AppUser.PRIV_USER_POWER || privInt == AppUser.PRIV_USER_RESTRICTED)
		  newUser.setUserprivilege(privInt);
	  //otherwise standard
	}catch(Exception ex){}	
	newUser.setMd5Hash(newUser.calMd5(newUser.getPassword()));
	metaDb.upsertAppUser(newUser);//save to db
	this.addUser(newUser);//add to cache
    return newUser;			
  }
}
