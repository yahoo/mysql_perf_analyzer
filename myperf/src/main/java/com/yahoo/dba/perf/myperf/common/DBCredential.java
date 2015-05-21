/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

/** 
 * Database access user name and password
 * @author xrao
 *
 */
public class DBCredential implements java.io.Serializable{

  private static final long serialVersionUID = 1L;

  private String appUser;//the user stores this db username/password pair
  private String username;//db username
  private String password;//db password
  private String dbGroupName;//the name of the database group
	
  public DBCredential()
  {		
  }

  public String getAppUser() 
  {
    return appUser;
  }

  public void setAppUser(String appUser) 
  {
    this.appUser = appUser;
  }

  public String getUsername() 
  {
    return username;
  }

  public void setUsername(String username) 
  {
    this.username = username;
  }

  public String getPassword() 
  {
    return password;
  }

  public void setPassword(String password) 
  {
    this.password = password;
  }
	
  /**
   * Retrun a copy
   * @return
   */
  public DBCredential copy()
  {
    DBCredential cred = new DBCredential();
	cred.setAppUser(this.appUser);
	cred.setDbGroupName(this.dbGroupName);
	cred.setUsername(this.username);
	cred.setPassword(this.password);
	return cred;
  }

  public String getDbGroupName() 
  {
    return dbGroupName;
  }

  public void setDbGroupName(String dbname) 
  {
		this.dbGroupName = dbname;
  }
	
}
