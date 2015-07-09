/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.ArrayList;
import java.util.List;

/**
 * an AppUser is the user who is going to use this app. 
 * @author xrao
 *
 */
public class AppUser implements java.io.Serializable{
  private static final long serialVersionUID = 1L;
  public static final int PRIV_USER_STANDARD = 0; //standard user.
  public static final int PRIV_USER_POWER = 1;    //power user, can manage this app.
  public static final int PRIV_USER_RESTRICTED = 2;    //restricted user, can only access database groups assgined by admin user.
  public static final String SESSION_ATTRIBUTE="APP_USER"; //session handle for AppUser
  public static final String SERVER_TS="SERVER_TS"; //server timestamp, use for login
  public static final String RANDOM_SEED="RANDOM_SEED"; //random seed
  

  private String name;//user name
  private String md5Hash;//password hash to login to this site.
  private String password;//not really used
  private int userprivilege = PRIV_USER_STANDARD;//or uset type  
  private String email;
  private boolean verified = false; //for newly registered user
    
  public AppUser(){}
  
  public AppUser(String name)
  {
    this.name = name;
  }
  
  public String getName() 
  {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public String getMd5Hash() {
    return md5Hash;
  }
  
  public void setMd5Hash(String md5Hash) {
    this.md5Hash = md5Hash;
  }
	
  public boolean match(String pwd)
  {
    try
	{
	  return this.md5Hash.equals(MD5Util.MD5(this.name+":"+pwd));
	}catch(Exception ex)
	{
	  return false;
	}
  }

  public boolean match(String hash, long ts, int seed)
  {
	if(hash==null)return false;  
    try
	{
	  return hash.equals(MD5Util.MD5(ts+":"+this.md5Hash+":"+seed));
	}catch(Exception ex)
	{
	  return false;
	}
  }

  public String calMd5(String pwd)
  {
    try
	{
	  return MD5Util.MD5(this.name+":"+pwd);
	}catch(Exception ex)
	{
	  return "0";
	}
  }

  public String getPassword()
  {
	return password;
  }
	
  public void setPassword(String password) 
  {
    this.password = password;
  }
	
  public int getUserprivilege() 
  {
    return userprivilege;
  }

  public void setUserprivilege(int userprivilege) 
  {
    this.userprivilege = userprivilege;
  }
  
  public boolean isAdminUser() 
  {
    return this.userprivilege == PRIV_USER_POWER;
  }

  public boolean isRestrictedUser()
  {
    return this.userprivilege == PRIV_USER_RESTRICTED;
  }
  public String getEmail() 
  {
    return email;
  }
  
  public void setEmail(String email) 
  {
    this.email = email;
  }
  
  public boolean matchPassword(String pd)
  {
    if(pd==null)return false;//we always need password
    return this.calMd5(pd).equals(this.md5Hash);    
  }
  
  private final static String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; 
	
  /**
   * Utility to generate random password
   * @return
   */
  public static String geterateRandomPwd()
  {
    char mypwd[] = new char[8];
    char[] chars = LETTERS.toCharArray();
	for(int i=0;i<8;i++)
	{
	  mypwd[i] = chars[(int)(Math.floor(Math.random()*chars.length))];
	}
	return new String(mypwd);
  }

  public boolean isVerified() {
	return verified;
  }

  public void setVerified(boolean verified) {
	this.verified = verified;
  }

}
