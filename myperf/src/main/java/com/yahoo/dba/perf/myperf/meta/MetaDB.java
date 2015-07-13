/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.meta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.AppUser;
import com.yahoo.dba.perf.myperf.common.DBCredential;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.MD5Util;

public class MetaDB implements java.io.Serializable{
  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(MetaDB.class.getName());
  private static final String DB_NAME="metaDB";
  private static final String SCHEMA_NAME="METADB";
  private static final String CRED_TABLENAME="DBCREDENTIALS";
  private static final String APPUSER_TABLENAME="APPUSERS";
  private static final String USERACL_TABLENAME="USERACLS"; //user, db group access control
  
  private KeyTool.DesEncrypter keyTool = new KeyTool.DesEncrypter();
  private String dbkey;

  //store  credentials so we no need to go to db everytime
  //the key is owner||dbname
  private Map<String, DBCredential> credCache = java.util.Collections.synchronizedMap(new HashMap<String, DBCredential>());
  
  public static final String DEFAULT_USER = "myperf";
  public MetaDB()
  {
    try
	{
	  //load embedded db driver
	  Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
	}catch(Exception ex)
	{	
	}
  }

 
  public String enc(String str)
  {
	  return keyTool.encrypt(str);
  }
  
  public String dec(String str)
  {
	  return keyTool.decrypt(str);
  }
  /**
   * Check required schema and tables
   * @return
   */
  public boolean init()
  {
    try
    {
      checkAndCreateSchema();
	  checkAndCreateTables();
	  return true;
	}catch(Exception ex)
	{
      logger.log(Level.SEVERE,"Exception", ex);
      return false;
    }
  }

  /**
   * Get a new connection
   * @return
   */
  private Connection getConnection()
  {
    Connection conn = null;
	try
	{				
	  conn = DriverManager.getConnection("jdbc:derby:"+DB_NAME+";create=true",SCHEMA_NAME, this.getDbkey());
	  return conn;
	}catch(Exception ex)
    {
				logger.log(Level.SEVERE,"Exception", ex);
	}
    return null;
  }
  private void checkAndCreateSchema() throws SQLException
  {
    java.util.Properties props = new java.util.Properties();
    Connection conn = null; 
    Statement stmt = null;
    ResultSet rs = null;
    boolean findSchema = false;
    try
    {
      conn = DriverManager.getConnection("jdbc:derby:"+DB_NAME+";create=true", props);
	  rs = conn.getMetaData().getSchemas();
	  while(rs.next())
	  {
	    if(SCHEMA_NAME.equalsIgnoreCase(rs.getString("TABLE_SCHEM")))
		{
		  logger.info("Find schema "+SCHEMA_NAME);
		  findSchema = true;break;
		}
	  }
	  if(!findSchema)
	  {
	    stmt = conn.createStatement();
		logger.info("Create schema "+SCHEMA_NAME+".");
	    String sql = "CREATE SCHEMA "+SCHEMA_NAME+" AUTHORIZATION "+this.getDbkey();				
		stmt.execute(sql);
	  }
	}finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(stmt);
	  DBUtils.close(conn);	  
	}
  }  
  
  private void checkAndCreateTables() throws SQLException
  {
    Connection conn = null; 
	try
	{
	  conn = getConnection();
	  checkAndCreateAppUserTable(conn);
	  checkAndCreateDBCredentialTable(conn);
	  checkAndCreateUserAclTable(conn);
	}finally
	{
	  DBUtils.close(conn);
	}		
  }
	
	
  private void checkAndCreateAppUserTable(Connection conn) throws SQLException
  {
    if(!DBUtils.hasTable(conn,SCHEMA_NAME,APPUSER_TABLENAME))
	{
	  Statement stmt = null;
	  PreparedStatement pstmt = null;
	  if(!DBUtils.hasTable(conn,SCHEMA_NAME,APPUSER_TABLENAME))
	  {
		 try
		 {
		   stmt = conn.createStatement();
		   logger.info("create table "+APPUSER_TABLENAME);
		   String sql = "create table "+APPUSER_TABLENAME+"(USERNAME VARCHAR(30) NOT NULL PRIMARY KEY, EMAIL VARCHAR(100), MD5HASH VARCHAR(100), USER_PRIVILEGE SMALLINT, CREATED TIMESTAMP DEFAULT CURRENT TIMESTAMP, VERIFIED SMALLINT DEFAULT 0)";
		   stmt.execute(sql);
		   
		   sql = "create index UK_"+APPUSER_TABLENAME+" on "+APPUSER_TABLENAME+"(USERNAME)";
		   stmt.execute(sql);
		   
		   //insert first admin user
		   pstmt = conn.prepareStatement("insert into "+APPUSER_TABLENAME+" (USERNAME,MD5HASH,USER_PRIVILEGE, VERIFIED) VALUES(?,?,?,?)");
		   pstmt.setString(1, DEFAULT_USER);
		   try{pstmt.setString(2, MD5Util.MD5(DEFAULT_USER + ":change"));}catch(Exception ex){};
		   pstmt.setInt(3,AppUser.PRIV_USER_POWER);
		   pstmt.setInt(4,  1);
		   pstmt.execute();
		   conn.commit();
		 }finally
		 {
			DBUtils.close(stmt);
			DBUtils.close(pstmt);
		 }
	   }
	 }else 
	 {
			Statement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = conn.createStatement();
				String sql = "select * from " + APPUSER_TABLENAME + " where 1=0";
				rs = stmt.executeQuery(sql);
				ResultSetMetaData meta = rs.getMetaData();
				int cnt = meta.getColumnCount();
				boolean hasCol = false;
				for(int i=1;i<=cnt;i++)
				{
					String col = meta.getColumnName(i);
					if("VERIFIED".equalsIgnoreCase(col))
					{
						hasCol = true;
						break;
					}
				}
				DBUtils.close(rs); rs=null;
				if(!hasCol)
				{
					logger.info("Add new column VERIFIED");
					stmt.execute("ALTER TABLE " + APPUSER_TABLENAME + " ADD COLUMN VERIFIED SMALLINT DEFAULT 0");
					stmt.execute("UPDATE " + APPUSER_TABLENAME+" SET VERIFIED=1"); //update existing as 1
				}
			}finally
			{
				if(stmt!=null)try{stmt.close();stmt=null;}catch(Exception ex){}
			}
			
	  }			
  }


  private void checkAndCreateDBCredentialTable(Connection conn) throws SQLException
  {
    if(!DBUtils.hasTable(conn,SCHEMA_NAME,CRED_TABLENAME))
	{
	  Statement stmt = null;
	  try
	  {
	    stmt = conn.createStatement();
		logger.info("create table "+CRED_TABLENAME+".");
		String sql = "create table "+CRED_TABLENAME+"(OWNER VARCHAR(30), DBGROUPNAME VARCHAR(30),USERNAME VARCHAR(60), CREDENTIAL VARCHAR(1024), VERIFIED SMALLINT DEFAULT 0, CREATED TIMESTAMP DEFAULT CURRENT TIMESTAMP)";
		stmt.execute(sql);
		sql = "create unique index UK_"+CRED_TABLENAME+" on "+CRED_TABLENAME+" (OWNER, DBGROUPNAME)";
		stmt.execute(sql);
	  }finally
	  {
	    DBUtils.close(stmt);
	  }
	}else
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = conn.createStatement();
			
			//because of an earlier bug, we forgot to create uniq index/PK on this table
			String  ukSQL = "SELECT * FROM "
							+" SYS.SYSCONGLOMERATES WHERE "
							+" CONGLOMERATENAME = 'UK_" + CRED_TABLENAME + "'";
			logger.info("Check unique constraints on table " + CRED_TABLENAME +" with " + ukSQL);
			rs = stmt.executeQuery(ukSQL);
			boolean hasIndex = false;
			if(rs != null && rs.next())
			{
				logger.info("UK_" + CRED_TABLENAME + " exists, nothing to do.");
				hasIndex = true;
			}
			rs.close(); rs = null;
			
			if(!hasIndex)
			{
				logger.info("UK_" + CRED_TABLENAME + " does not exist, create it.");
				try
				{
					boolean ret = stmt.execute("CREATE UNIQUE INDEX UK_"+CRED_TABLENAME+" ON "+CRED_TABLENAME+" (OWNER, DBGROUPNAME)");
					logger.info("UK_" + CRED_TABLENAME + " created: " + ret);
				}catch(Throwable ex)
				{
					logger.log(Level.WARNING, "Exception when create UK_"+CRED_TABLENAME, ex);
				}
			}
			
			
			String sql = "select * from "+CRED_TABLENAME+" where 1=0";
			rs = stmt.executeQuery(sql);
			ResultSetMetaData meta = rs.getMetaData();
			int cnt = meta.getColumnCount();
			boolean hasCol = false;
			for(int i=1;i<=cnt;i++)
			{
				String col = meta.getColumnName(i);
				if("CREDENTIAL".equalsIgnoreCase(col) && meta.getColumnDisplaySize(i)<1024)
				{
					hasCol = true;
					break;
				}
			}
			DBUtils.close(rs); rs=null;
			if(hasCol)
			{
				logger.info("Add new column credential size");
				stmt.execute("ALTER TABLE "+CRED_TABLENAME+" ALTER COLUMN CREDENTIAL SET DATA TYPE VARCHAR(1024)");
			}
		}finally
		{
			if(stmt!=null)try{stmt.close();stmt=null;}catch(Exception ex){}
		}		
	}
  }

  
  private void checkAndCreateUserAclTable(Connection conn) throws SQLException
  {
    if(!DBUtils.hasTable(conn,SCHEMA_NAME, USERACL_TABLENAME))
	{
	  Statement stmt = null;
	  try
	  {
	    stmt = conn.createStatement();
		logger.info("create table "+USERACL_TABLENAME+".");
		String sql = "create table "+USERACL_TABLENAME+"(USERNAME VARCHAR(30), DBGROUPNAME VARCHAR(30), VISIBLE SMALLINT DEFAULT 0, CREATED TIMESTAMP DEFAULT CURRENT TIMESTAMP, PRIMARY KEY(USERNAME, DBGROUPNAME))";
		stmt.execute(sql);
	  }finally
	  {
	    DBUtils.close(stmt);
	  }
	}
  }

  /**
   * Shut down metadb
   */
  public void destroy()
  {
    try
	{
	  logger.info("Shutdown meta db");
	  DriverManager.getConnection("jdbc:derby:;shutdown=true");
	}catch(SQLException ex)
	{
	  logger.log(Level.WARNING,"Exception", ex);
	  if("XJ015".equalsIgnoreCase(ex.getSQLState()))
	  {
	    logger.info("meta DB has been shutdown.");
	  }
	}
  }
	
  /** 
   * retrieve user info
   * @param username
   * @return
   */
  public AppUser retrieveUserInfo(String username)
  {
    Connection conn = null;
	try
	{
	  conn = getConnection();
	  return this.retrieveUserInfo(conn, username);
	}catch(Exception ex)
	{
	}finally
	{
	  DBUtils.close(conn);
	}
	return null;
  }

  private AppUser retrieveUserInfo(Connection conn, String username)
  {
    String sql = "select * from "+APPUSER_TABLENAME+" where username=?";
	PreparedStatement pstmt = null;
	ResultSet rs = null;
	try
	{
	  pstmt = conn.prepareStatement(sql);
	  pstmt.setString(1, username.toLowerCase());
	  rs = pstmt.executeQuery();
	  if(rs!=null && rs.next())
	  {
	    AppUser user = new AppUser();
		user.setName(rs.getString("USERNAME"));
		user.setMd5Hash(rs.getString("MD5HASH"));
		user.setUserprivilege(rs.getShort("USER_PRIVILEGE"));
		user.setEmail(rs.getString("EMAIL"));
		user.setVerified("1".equals(rs.getString("VERIFIED")));
		return user;
	  }
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	}finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(pstmt);
	}
	return null;
  }


  /**
   * 
   * @param user
   */
  public void upsertAppUser(AppUser user)
  {
    if(user==null)return;
	String sql2 = "update "+APPUSER_TABLENAME+" set MD5HASH=?, USER_PRIVILEGE=?, EMAIL=?, VERIFIED=? where USERNAME=?";
	String sql3 = "insert into "+APPUSER_TABLENAME+" (MD5HASH, USER_PRIVILEGE, EMAIL, VERIFIED,USERNAME) values(?,?,?,?,?)";
		
	Connection conn = null;
	PreparedStatement pstmt = null;
	boolean findOne = false;
	try
	{
	  conn = getConnection();
	  //first, check if we have record
	  findOne = this.retrieveUserInfo(conn, user.getName())!=null;
	  if(findOne)
	    pstmt = conn.prepareStatement(sql2);
	  else 
		pstmt = conn.prepareStatement(sql3);
			
	  int idx = 1;
	  pstmt.setString(idx++, user.getMd5Hash());
	  pstmt.setInt(idx++, user.getUserprivilege());
	  pstmt.setString(idx++, user.getEmail()==null?"NA":user.getEmail());
	  pstmt.setInt(idx++, user.isVerified()?1:0);
	  pstmt.setString(idx++, user.getName().toLowerCase());
	  pstmt.execute();
	  conn.commit();			
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
	  DBUtils.close(conn);
	}				
  }
	
  /**
   * Update user email address
   * @param user
   * @param newEmail
   * @return
   */
  public boolean changeEmail(String user, String newEmail)
  {
    if(user==null)return false;
	String sql2 = "update "+APPUSER_TABLENAME+" set EMAIL=? where USERNAME=?";
		
	Connection conn = null;
	PreparedStatement pstmt = null;
	try
	{
	  conn = getConnection();
	  //first, check if we have record
	  AppUser appUser = this.retrieveUserInfo(conn, user);
	  if(appUser==null)return false;

	  pstmt = conn.prepareStatement(sql2);
	  pstmt.setString(1, newEmail);
	  pstmt.setString(2, appUser.getName().toLowerCase());
	  pstmt.execute();
	  conn.commit();
	  return true;
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
	  DBUtils.close(conn);
	}
  }

  public boolean confirmUser(String user, boolean confirmed)
  {
    if(user==null)return false;
	String sql2 = "update "+APPUSER_TABLENAME+" set VERIFIED=? where USERNAME=?";
		
	Connection conn = null;
	PreparedStatement pstmt = null;
	try
	{
	  conn = getConnection();
	  //first, check if we have record
	  AppUser appUser = this.retrieveUserInfo(conn, user);
	  if(appUser==null)return false;

	  pstmt = conn.prepareStatement(sql2);
	  pstmt.setInt(1, confirmed? 1: 0);
	  pstmt.setString(2, appUser.getName().toLowerCase());
	  pstmt.execute();
	  conn.commit();
	  return true;
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
	  DBUtils.close(conn);
	}
  }

  /**
   * Update user privilege
   * @param user
   * @param newPrivilege
   * @return
   */
  public boolean changePrivilege(String user, int newPrivilege)
  {
    if(user==null)return false;
	String sql2 = "update "+APPUSER_TABLENAME+" set USER_PRIVILEGE=? where USERNAME=?";
		
	Connection conn = null;
	PreparedStatement pstmt = null;
	try
	{
	  conn = getConnection();
	  //first, check if we have record
	  AppUser appUser = this.retrieveUserInfo(conn, user);
	  if(appUser==null)return false;
	  appUser.setUserprivilege(newPrivilege);
	  pstmt = conn.prepareStatement(sql2);
	  pstmt.setInt(1, appUser.getUserprivilege());
	  pstmt.setString(2, appUser.getName().toLowerCase());
	  pstmt.execute();
	  conn.commit();
	  return true;
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
	  DBUtils.close(conn);
	}
  }
  
  /**
   * Update user password hash
   * @param user
   * @param newPwd
   * @return
   */
  public boolean changePasssword(String user, String newPwd)
  {
    if(user==null)return false;
	String sql2 = "update "+APPUSER_TABLENAME+" set MD5HASH=? where USERNAME=?";
		
	Connection conn = null;
	PreparedStatement pstmt = null;
	try
	{
	  conn = getConnection();
	  //first, check if we have record
	  AppUser appUser = this.retrieveUserInfo(conn, user);
	  if(appUser==null)return false;
	  appUser.setPassword(newPwd);
	  appUser.setMd5Hash(appUser.calMd5(newPwd));
	  pstmt = conn.prepareStatement(sql2);
	  pstmt.setString(1, appUser.getMd5Hash());
	  pstmt.setString(2, appUser.getName().toLowerCase());
	  pstmt.execute();
	  conn.commit();
	  return true;
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
	  DBUtils.close(conn);
	}
  }

  public boolean deleteUser(String user)
  {
    if(user == null || DEFAULT_USER.equals(user))return false;
	String[] sql = new String[]{
						"delete from "+APPUSER_TABLENAME+" where USERNAME=?",
						"delete from "+CRED_TABLENAME+" where owner=?",
						"delete from "+USERACL_TABLENAME+" where username=?"
						};

	Connection conn = null;
	PreparedStatement pstmt = null;
	try
	{
	  conn = getConnection();
	  for(int i=0;i<sql.length;i++)
	  {
	    pstmt = conn.prepareStatement(sql[i]);
	    pstmt.setString(1, user);
	    pstmt.execute();
	    conn.commit();
	    DBUtils.close(pstmt); pstmt = null;
	  }	  
	  return true;
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
	  DBUtils.close(conn);
	}
  }

	
  /**
   * List all registered users
   * @return
   */
  public java.util.List<AppUser> retrieveAllUsers()
  {
    String sql = "select * from "+APPUSER_TABLENAME+" order by username";
	Connection conn = null;
	Statement stmt = null;
	ResultSet rs = null;
	java.util.ArrayList<AppUser> userList = new java.util.ArrayList<AppUser>();
	try
	{
	  conn = getConnection();
	  stmt = conn.createStatement();
	  rs = stmt.executeQuery(sql);
	  while(rs!=null && rs.next())
	  {
	    AppUser user = new AppUser();
		user.setName(rs.getString("USERNAME"));
		//user.setMd5Hash(rs.getString("MD5HASH"));
		user.setUserprivilege(rs.getShort("USER_PRIVILEGE"));
		user.setEmail(rs.getString("EMAIL"));
		user.setVerified("1".equals(rs.getString("VERIFIED")));
		userList.add(user);
	  }
	}catch(Exception ex)
    {
	  logger.log(Level.SEVERE,"Exception", ex);
	}finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(stmt);
	  DBUtils.close(conn);
	}			
	return userList;
  }

  /**
   * 
   * @param owner
   * @param dbGroupName
   * @return
   */
  public DBCredential retrieveDBCredential(String owner, String dbGroupName)
  {
	Connection conn = null;
	try
	{
      if(!this.credCache.containsKey(owner + "||" + dbGroupName))
	  {
	    conn = getConnection();
  	    DBCredential cred =  retrieveDBCredential(conn, owner, dbGroupName);
	    if(cred != null)this.credCache.put(owner + "||" +dbGroupName, cred);
	  }  
      DBCredential saved = this.credCache.get(owner + "||" + dbGroupName);
      if(saved != null)//return a decrypted copy
      {
    	  saved = saved.copy();
    	  saved.decryptPassword(keyTool);
      }
      return saved;
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	}
	finally
	{
	  DBUtils.close(conn);
	}
	return null;
  }

  private DBCredential retrieveDBCredential(Connection conn, String owner, String dbGroupName)
  {
    String sql = (owner!=null&&owner.trim().length()>0)?
    		"select * from "+CRED_TABLENAME+" where dbgroupname=?  and owner=?"
    		:"select * from "+CRED_TABLENAME+" where dbgroupname=? and owner is null";
		
	PreparedStatement pstmt = null;
	ResultSet rs = null;
	try
	{
	  pstmt = conn.prepareStatement(sql);
	  pstmt.setString(1, dbGroupName.toLowerCase());
	  if(owner!=null&&owner.trim().length()>0)pstmt.setString(2, owner);
			rs = pstmt.executeQuery();
	  if(rs!=null && rs.next())
	  {
	    DBCredential cred = new DBCredential();
		cred.setAppUser(owner);
		cred.setDbGroupName(rs.getString("DBGROUPNAME"));
		cred.setUsername(rs.getString("USERNAME"));
		cred.setAppUser(rs.getString("OWNER"));
		String credStringOrig = rs.getString("CREDENTIAL");
		String credString = keyTool.decrypt(credStringOrig);
		//we will use two :: as separator
		int verified = rs.getInt("VERIFIED");
		if(verified==1)
		{
		  //credString = credString.substring(0, credString.lastIndexOf("::"));
		  //credString = credString.substring(credString.lastIndexOf("::")+2);
		  //cred.setPassword(credString);
		  cred.setEncrypted(credStringOrig);
		  return cred;
		}else 
		{
		  cred.setPassword(credString);					
		  updateDBCredentialInternal(cred);
		  return retrieveDBCredential(conn, owner, dbGroupName);
		}					
	  }
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	}finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(pstmt);
	}
	return null;
  }

  public void renameDbGroupName(String oldName, String newName)
  {
	  //nothing to change
	  if(newName == null || newName.equalsIgnoreCase(oldName))
		  return;
	  
	  String sql1 = "select * from "+CRED_TABLENAME+" where dbgroupname=?";
	  String sql2 = "insert into "+CRED_TABLENAME+" (owner,dbgroupname,username,credential,verified) values(?,?,?,?,1)";
	  String sql3 = "delete from "+CRED_TABLENAME+" where dbgroupname=?";
	  String sql4 = "select * from "+USERACL_TABLENAME+" where dbgroupname=?";
	  String sql5 = "insert into "+USERACL_TABLENAME+" (username,dbgroupname,visible) values(?,?,?)";
	  String sql6 = "delete from "+USERACL_TABLENAME+" where dbgroupname=?";
	  
	  Connection conn = null;
	  Map<String, String> credMap = new java.util.HashMap<String, String>();
	  Map<String, Integer> aclMap = new java.util.HashMap<String, Integer>();
	  Map<String, String> credUserMap = new java.util.HashMap<String, String>();
	  PreparedStatement stmt = null;
	  ResultSet rs = null;
	  try
	  {
		  conn = getConnection();
		  logger.info("Rename db group: from "+oldName+" to " + newName);
		  stmt = conn.prepareStatement(sql1);
		  stmt.setString(1, oldName.toLowerCase());
		  rs = stmt.executeQuery();
		  while(rs != null && rs.next())
		  {
			  String owner = rs.getString("OWNER");
			  String user = rs.getString("USERNAME");
			  String credString = rs.getString("CREDENTIAL");
			  if(credString != null)
			  {
				  credString = keyTool.decrypt(credString);
				  credString = credString.substring(0, credString.lastIndexOf("::"));
				  credString = credString.substring(credString.lastIndexOf("::")+2);
				  credMap.put(owner, credString);
				  //logger.info("cred for "+owner+" is "+credString);
				  credUserMap.put(owner, user);
			  }
		  }
		  rs.close(); rs = null;
		  stmt.close(); stmt = null;
		  
		  stmt = conn.prepareStatement(sql4);
		  stmt.setString(1, oldName.toLowerCase());
		  rs = stmt.executeQuery();
		  while(rs != null && rs.next())
		  {
			  String user = rs.getString("USERNAME");
			  int visible = rs.getInt("VISIBLE");
			  aclMap.put(user, visible);
		  }
		  rs.close(); rs = null;
		  stmt.close(); stmt = null;
		  
		  logger.info("Update db group credentials: " + credUserMap.size());
		  stmt = conn.prepareStatement(sql2);
		  for(Map.Entry<String, String> entry: credMap.entrySet())
		  {
			  credCache.remove(entry.getKey() + "||" + oldName.toLowerCase());
			  String owner = entry.getKey();
			  String pString = owner == null?"NULL":owner;
				pString += "::"+newName.toLowerCase()
						+"::"+credUserMap.get(owner)
						+"::"+credMap.get(owner)
						+"::"+Math.random();
			  stmt.setString(1, owner);
			  stmt.setString(2, newName.toLowerCase());
			  stmt.setString(3, credUserMap.get(owner));
			  stmt.setString(4, this.keyTool.encrypt(pString));
			  stmt.execute();
		  }
		  stmt.close(); stmt = null;
		  
		  logger.info("Update db acl: " + aclMap.size());
		  stmt = conn.prepareStatement(sql5);
		  for(Map.Entry<String, Integer> entry: aclMap.entrySet())
		  {
			  stmt.setString(1, entry.getKey());
			  stmt.setString(2, newName.toLowerCase());
			  stmt.setInt(3, entry.getValue());
			  stmt.execute();
		  }
		  stmt.close(); stmt = null;
		  
		  stmt = conn.prepareStatement(sql3);
		  stmt.setString(1, oldName.toLowerCase());
		  stmt.execute();
		  stmt.close(); stmt = null;
		  
		  stmt = conn.prepareStatement(sql6);
		  stmt.setString(1, oldName.toLowerCase());
		  stmt.execute();
		  stmt.close(); stmt = null;

	  }catch(Exception ex)
	  {
		  logger.log(Level.SEVERE,"Exception", ex);
		  throw new RuntimeException(ex);
	  }
	  finally
	  {
		  DBUtils.close(rs);
		  DBUtils.close(stmt);
		  DBUtils.close(conn);
	  }
	  
  }
  
  public void removeDbGroup(String grpName)
  {
	  String[] sql = new String[]{
			  "delete from "+ CRED_TABLENAME+" where dbgroupname=?",
			  "delete from "+ USERACL_TABLENAME+" where dbgroupname=?",
	  };
	  Connection conn = null;
	  PreparedStatement stmt = null;
	  try
	  {
		  conn = getConnection();
		  for(int i=0; i<sql.length; i++)
		  {
			  stmt = conn.prepareStatement(sql[i]);
			  stmt.setString(1, grpName.toLowerCase());
			  stmt.execute();
			  stmt.close(); stmt = null;
		  }
	  }catch(Exception ex)
	  {
		  logger.log(Level.SEVERE,"Exception", ex);
		  throw new RuntimeException(ex);
	  }
	  finally
	  {
		  DBUtils.close(stmt);
		  DBUtils.close(conn);
	  }
  
  }
  /**
   * List database groups the specific user has provided passwords
   * @param owner
   * @return
   */
  public List<String> listMyDBs(String owner, boolean restricted)
  {
    Connection conn = null;
	try
	{	
	  conn = getConnection();
	  return listMyDBs(conn, owner, restricted);
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	}
	finally
	{
	  DBUtils.close(conn);
	}
	return null;
  }
  
  /**
   * 
   * @param username
   * @param dbGroups the key is dbgroup name, the value is 1 or 0 (visible or not)
   */
  public void  updateUserAcls(String username, Map<String, Integer> dbGroups)
  {
	  Connection conn = null;
	  PreparedStatement stmt = null;
	  ResultSet rs = null;
	  try
	  {	
		  conn = getConnection();
		  //first we will mask all existing db as not visible
		  String sql = "update " + USERACL_TABLENAME +" set visible=0 where username=?";
		  stmt = conn.prepareStatement(sql);
		  stmt.setString(1, username);
		  stmt.execute();
		  DBUtils.close(stmt); stmt = null;
		  //Now lets grab the entries the user has so far		  
		  sql = "select dbgroupname from " + USERACL_TABLENAME + " where username=?";
		  stmt = conn.prepareStatement(sql);
		  stmt.setString(1, username);
		  rs = stmt.executeQuery();
		  HashSet<String> existingGroups = new HashSet<String>();
		  while(rs != null && rs.next())
			  existingGroups.add(rs.getString(1));
		  DBUtils.close(rs); rs = null;
		  DBUtils.close(stmt); stmt = null;
		  //first, let's update existing ones
		  sql = "update " + USERACL_TABLENAME +" set visible=1 where username=? and dbgroupname=?";
		  stmt = conn.prepareStatement(sql);
 		  for(Map.Entry<String, Integer> e: dbGroups.entrySet())
		  {
			  if(e.getValue() == 0 || !existingGroups.contains(e.getKey()))continue;
			  stmt.setString(1, username);
			  stmt.setString(2, e.getKey());
			  stmt.execute();
		  }
		  DBUtils.close(stmt); stmt = null;

		  //now new entries
		  sql = "insert into " + USERACL_TABLENAME +" (username, dbgroupname, visible) values (?, ?, 1)";
		  stmt = conn.prepareStatement(sql);
 		  for(Map.Entry<String, Integer> e: dbGroups.entrySet())
		  {
			  if(e.getValue() == 0 || existingGroups.contains(e.getKey()))continue;
			  stmt.setString(1, username);
			  stmt.setString(2, e.getKey());
			  stmt.execute();
		  }
		  DBUtils.close(stmt); stmt = null;
		  conn.commit();
	  }catch(Exception ex)
	  {
		  logger.log(Level.SEVERE,"Exception", ex);
	  }
	  finally
	  {
		  DBUtils.close(rs);
		  DBUtils.close(stmt);
		  DBUtils.close(conn);
	  }
  }

  /**
   * Update visibility of a single dbgroup for a user
   * @param username
   * @param dbgroupname
   * @param visible
   */
  public boolean  updateUserAcl(String username, String dbgroupname, boolean visible)
  {
	  Connection conn = null;
	  PreparedStatement stmt = null;
	  ResultSet rs = null;
	  try
	  {	
		  conn = getConnection();
		  //Now lets grab the entries the user has so far		  
		  String sql = "select dbgroupname from " + USERACL_TABLENAME + " where username=? and dbgroupname=?";
		  stmt = conn.prepareStatement(sql);
		  stmt.setString(1, username);
		  stmt.setString(2, dbgroupname);
		  rs = stmt.executeQuery();
		  boolean findOne = false;
		  if(rs != null && rs.next())
			  findOne = true;
		  DBUtils.close(rs); rs = null;
		  DBUtils.close(stmt); stmt = null;
		  //first, let's update existing ones
		  if(findOne)
		  {
		    sql = "update " + USERACL_TABLENAME +" set visible=? where username=? and dbgroupname=?";
		    stmt = conn.prepareStatement(sql);
 		    stmt.setInt(1, visible?1:0);
 		    stmt.setString(2, username);
			stmt.setString(3, dbgroupname);
			stmt.execute();
			DBUtils.close(stmt); stmt = null;
		  }else
		  {
		    //now new entries
		    sql = "insert into " + USERACL_TABLENAME +" (username, dbgroupname, visible) values (?, ?, ?)";
		    stmt = conn.prepareStatement(sql);
 		    stmt.setString(1, username);
			stmt.setString(2, dbgroupname);
 		    stmt.setInt(3, visible?1:0);
			stmt.execute();
		  }		  
		  DBUtils.close(stmt); stmt = null;
		  conn.commit();
		  return true;
	  }catch(Exception ex)
	  {
		  logger.log(Level.SEVERE,"Exception", ex);
		  return false;
	  }
	  finally
	  {
		  DBUtils.close(rs);
		  DBUtils.close(stmt);
		  DBUtils.close(conn);
	  }
  }

  private List<String> listMyDBs(Connection conn, String owner, boolean restricted)
  {
    String sql = "select dbgroupname from " + CRED_TABLENAME + " where owner=? order by dbgroupname";
    if(restricted)
    	sql = "select dbgroupname from " + USERACL_TABLENAME + " where visible=1 and username=? order by dbgroupname";
	PreparedStatement pstmt = null;
	ResultSet rs = null;
	List<String> mydbs = new ArrayList<String>();
	try
	{
	  pstmt = conn.prepareStatement(sql);
	  pstmt.setString(1, owner);
	  rs = pstmt.executeQuery();
	  while(rs!=null && rs.next())
	  {
	    mydbs.add(rs.getString("dbgroupname"));
	  }
	}catch(Exception ex)
	{
	  logger.log(Level.SEVERE,"Exception", ex);
	}finally
	{
	  DBUtils.close(rs);
	  DBUtils.close(pstmt);
	}
	return mydbs;
  }

  /**
   * Store db credential per user/db base
   * @param cred
   */
  public void upsertDBCredential(DBCredential cred)
  {
    if(cred==null)return;
    //bust our cached cred first
    if(this.credCache.containsKey(cred.getAppUser() +"||"+cred.getDbGroupName()))
    	this.credCache.remove(cred.getAppUser() +"||"+cred.getDbGroupName());
	String sql1 = "update "+CRED_TABLENAME+" set username=?,credential=?,verified=1 where owner=? and dbgroupname=?";
    String sql2 = "insert into "+CRED_TABLENAME+" (owner,dbgroupname,username,credential,verified) values(?,?,?,?,1)";
		
	String pString = cred.getAppUser()==null?"NULL":cred.getAppUser();
	pString += "::"+cred.getDbGroupName()
			+"::"+cred.getUsername()
			+"::"+cred.getPassword()
			+"::"+Math.random();
	Connection conn = null;
	PreparedStatement pstmt = null;
	boolean findOne = false;
	try
	{
	  conn = getConnection();
	  //first, check if we have record
	  findOne = this.retrieveDBCredential(conn, cred.getAppUser(), cred.getDbGroupName())!=null;
	  if(findOne)
	  {
	    pstmt = conn.prepareStatement(sql1);
		pstmt.setString(1, cred.getUsername());
		pstmt.setString(2, keyTool.encrypt(pString));
		pstmt.setString(3, cred.getAppUser());
		pstmt.setString(4, cred.getDbGroupName().toLowerCase());
		pstmt.execute();
		conn.commit();
		pstmt.close();pstmt = null;
	  }else
	  {
	    pstmt = conn.prepareStatement(sql2);
		pstmt.setString(1, cred.getAppUser());
		pstmt.setString(2, cred.getDbGroupName().toLowerCase());
		pstmt.setString(3, cred.getUsername());
		pstmt.setString(4, keyTool.encrypt(pString));
		pstmt.execute();
		conn.commit();
		pstmt.close();pstmt = null;				
	  }
	}catch(Exception ex)
	{
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
	  DBUtils.close(conn);
	}			
  }

  public void removeDBCredential(String owner, String dbGroupName)
  {
	if(owner == null || owner.isEmpty())return;
	if(dbGroupName == null || dbGroupName.isEmpty())return;
	
	String sql = "delete from " + CRED_TABLENAME +" where  owner=? and dbgroupname=?";
	Connection conn = null;
	PreparedStatement pstmt = null;
	try
	{
	  conn = getConnection();
	  pstmt = conn.prepareStatement(sql);
	  pstmt.setString(1, owner);
	  pstmt.setString(2, dbGroupName.toLowerCase());
	  pstmt.execute();
	  conn.commit();
	  DBUtils.close(pstmt); pstmt = null;
	  this.credCache.remove(owner+"||"+dbGroupName.toLowerCase());
	}catch(Exception ex)
	{
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
      DBUtils.close(conn);
	}
  }
  
  private void updateDBCredentialInternal(DBCredential cred)
  {
	if(cred==null)return;
	String sql1 = "update "+CRED_TABLENAME+" set username=?,credential=?,verified=1 where owner=? and dbgroupname=?";		
	String pString = cred.getAppUser()==null?"NULL":cred.getAppUser();
	pString += "::"+cred.getDbGroupName()
	  		  +"::"+cred.getUsername()
			  +"::"+cred.getPassword()
			  +"::"+Math.random();
	Connection conn = null;
	PreparedStatement pstmt = null;
	try
	{
	  conn = getConnection();
	  pstmt = conn.prepareStatement(sql1);
	  pstmt.setString(1, cred.getUsername());
	  pstmt.setString(2, keyTool.encrypt(pString));
	  pstmt.setString(3, cred.getAppUser());
	  pstmt.setString(4, cred.getDbGroupName().toLowerCase());
	  pstmt.execute();
	  conn.commit();
	  pstmt.close();pstmt = null;			
	}catch(Exception ex)
	{
	  if(conn!=null)try{conn.rollback();}catch(Exception iex){}
	  throw new RuntimeException(ex);
	}finally
	{
	  DBUtils.close(pstmt);
      DBUtils.close(conn);
	}				
  }

  public String getDbkey() 
  {
    return dbkey;
  }

  public void setDbkey(String dbkey) 
  {
    this.dbkey = dbkey;
  }
}
