/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

/**
 * Information about an individual database server
 * @author xrao
 *
 */
public class DBInstanceInfo implements java.io.Serializable{
  private static final long serialVersionUID = 1L;
  private static final int MAX_16BIT_UINT = 65536;
	
  private int instance = 0;//keep it for future to include Oracle
  
  private String dbType = "mysql";//other value: Oracle
  private String hostName;//db host name
  private String port="3306";//tcp port
  private String databaseName = "information_schema";//MySQL database name for default connect
  private String dbGroupName;//database group or cluster name
	                       //For mysql, we can use it to group servers 
                           //used for the same purpose
  
  
  private boolean useTunneling;//when ssh tunneling is used
  private String localHostName = "localhost";//local hostname when use tunneling
  private String localPort;//local port when use tunneling
  
  private String username;
  private String password;
	
  private boolean connectionVerified;//if we use probe all instances, some instance might not 
                                     //be verified for its connections, 
                                     //especially when ssh tunneling is used
	
  private boolean testConnection;//used for form only
  private boolean probeAllInstance;//used for form only, keep it for Oracle
  private boolean storeCredential;//user for form only
	
  private boolean virtualHost;//some system does not allow direct host/sid access, for Oracle
  private String owner;//the user who creates this entry
	
  private int dbid; //primary key, generated. Will be used for metrics
  private boolean snmpEnabled = true;//by default, we assume SNMP is enabled.
  private boolean metricsEnabled = true;
  private boolean alertEnabled = true;
  
  public DBInstanceInfo(){}
	
  public String getHostName() 
  {
    return hostName;
  }

  public void setHostName(String hostName) 
  {
    if(hostName!=null)
	  this.hostName = hostName.toLowerCase().trim();
	else this.hostName = null;
  }

  public String getPort() 
  {
    return port;
  }

  public void setPort(String port) 
  {
	if(port!=null)
	{
		try
		{
			int lp = Integer.parseInt(port);
			if(lp<0)
			{
				lp = MAX_16BIT_UINT + lp;
			}
			this.port = String.valueOf(lp);
		}catch(Exception ex){}
	}
	else this.port = port;
  }

  public short getPortShort()
  {
    int lp = 0;
	try
	{
	  lp = Integer.valueOf(this.port);	  
	}catch(Exception ex)
	{
			
	}
	return (short)lp;
  }

  public boolean isUseTunneling() 
  {
    return useTunneling;
  }

  public void setUseTunneling(boolean useTunneling) 
  {
    this.useTunneling = useTunneling;
  }

  public String getLocalHostName() 
  {
    return localHostName;
  }

  public void setLocalHostName(String localHostName) 
  {
    if(localHostName!=null)
	  this.localHostName = localHostName.trim();
	else this.localHostName = null;
  }

  public String getLocalPort() 
  {
    return localPort;
  }

  public void setLocalPort(String localPort) 
  {
	if(localPort != null)
	{
	  try
	  {
		int lp = Integer.parseInt(localPort);
		if(lp <0 )
		{
			lp = MAX_16BIT_UINT + lp;
		}
		this.localPort = String.valueOf(lp);
		
	  }catch(Exception ex)
	  {
		  
	  }
	}
	else this.localPort = localPort;
  }

  public short getLocalPortShort()
  {
    int lp = 0;
    try
    {
      lp = Integer.valueOf(this.localPort);
	}catch(Exception ex)
    {			
    }
    return (short)lp;
  }
	
  public int getInstance() 
  {
    return instance;
  }

  public void setInstance(int instance) 
  {
    this.instance = instance;
  }


  public String getUsername() 
  {	  
    return username;
  }

  public void setUsername(String username) 
  {
    if(username!=null)
	  this.username = username.trim();
	else this.username = username;
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
    * Make a value copy
	* @return
	*/
  public DBInstanceInfo copy()
  {
    DBInstanceInfo db = new DBInstanceInfo();
    db.setDbType(this.dbType);
    db.setDbGroupName(this.dbGroupName);
    db.setHostName(this.hostName);
    db.setPort(this.port);
    db.setDatabaseName(this.databaseName);
    db.setLocalHostName(this.localHostName);
    db.setUseTunneling(this.useTunneling);
    db.setLocalPort(this.localPort);
    db.setUsername(this.username);
    db.setPassword(this.password);
    db.setInstance(this.instance);
    db.setSnmpEnabled(this.snmpEnabled);
    db.setMetricsEnabled(this.metricsEnabled);
    db.setConnectionVerified(this.connectionVerified);
    db.setAlertEnabled(this.alertEnabled);
    return db;
  }
	
  public String getConnectionString()
  {
	// Add allowPublicKeyRetrieval to support sha256 password
	if("mysql".equalsIgnoreCase(this.dbType))
	{
	  if(!this.useTunneling)
	    return "jdbc:mysql://"+this.hostName+":"+this.port+"/"+this.databaseName+"?useSSL=true&enabledTLSProtocols=TLSv1.2&verifyServerCertificate=false";
	  else
		return "jdbc:mysql://"+this.localHostName+":"+this.localPort+"/"+this.databaseName+"?useSSL=true&enabledTLSProtocols=TLSv1.2&verifyServerCertificate=false";	    
	}
	else if("oracle".equalsIgnoreCase(this.dbType))
	{
	  if(!this.useTunneling)
	  {
	    if(!this.virtualHost)
		  return   "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST="+hostName+")(PORT="+port+"))(CONNECT_DATA=(SERVER=DEDICATED)(SID="+databaseName+")))";
		else 
		  return   "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST="+hostName+")(PORT="+port+"))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME="+databaseName+")))";
	  }
	  else 
	  {
	    if(!this.virtualHost)
		  return "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST="+this.localHostName+")(PORT="+this.localPort+"))(CONNECT_DATA=(SERVER=DEDICATED)(SID="+databaseName+")))";
		else
		  return "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST="+this.localHostName+")(PORT="+this.localPort+"))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME="+databaseName+")))";
	  }
	}
    return null;
  }

  @Override
  public String toString() 
  {
    return "["+this.dbGroupName+","+this.hostName+","+this.dbid+","+this.instance+","
              +this.port+","+this.databaseName+", ssh: "+this.useTunneling+","
    		  +this.localHostName+","+this.localPort+",virtual:"+this.virtualHost
    		  +","+this.username+"]";
  }

  public boolean isTestConnection() 
  {
    return testConnection;
  }

  public void setTestConnection(boolean testConnection) 
  {
    this.testConnection = testConnection;
  }

  public boolean isProbeAllInstance() 
  {
    return probeAllInstance;
  }

  public void setProbeAllInstance(boolean probeAllInstance) 
  {
    this.probeAllInstance = probeAllInstance;
  }

  public boolean isStoreCredential() 
  {
    return storeCredential;
  }

  public void setStoreCredential(boolean storeCredential) 
  {
    this.storeCredential = storeCredential;
  }

  public boolean isConnectionVerified() 
  {
    return connectionVerified;
  }

  public void setConnectionVerified(boolean connectionVerified) 
  {
    this.connectionVerified = connectionVerified;
  }

  public boolean isVirtualHost() 
  {
    return virtualHost;
  }

  public void setVirtualHost(boolean virtualHost) 
  {
    this.virtualHost = virtualHost;
  }

  public String getOwner() 
  {
    return owner;
  }

  public void setOwner(String owner) 
  {
    this.owner = owner;
  }

  public String getDbType() 
  {
    return dbType;
  }

  public void setDbType(String dbType) 
  {
	if(dbType!=null)
	  this.dbType = dbType.trim().toLowerCase();
	else 
	  this.dbType = null;
  }

  public String getDatabaseName() 
  {
    return databaseName;
  }

  public void setDatabaseName(String database) 
  {
    this.databaseName = database;
  }

  public String getDbGroupName() 
  {
    return dbGroupName;
  }

  public void setDbGroupName(String groupName) 
  {
    this.dbGroupName = groupName;
  }

  /**
   * Whether we can query other db server or instance of the samegroup 
   * to retrieve status of this db, for example, Oracle to use
   * gv$ or dba_hist views
   * @return
   */
  public boolean supportClusterQuery()
  {
	if("Oracle".equalsIgnoreCase(this.dbType))return true;
    return false;	  
  }

public int getDbid() {
	return dbid;
}

public void setDbid(int dbid) {
	this.dbid = dbid;
}

public boolean isSnmpEnabled() {
	return snmpEnabled;
}

public void setSnmpEnabled(boolean snmpEnabled) {
	this.snmpEnabled = snmpEnabled;
}

public boolean isMetricsEnabled() {
	return metricsEnabled;
}

public void setMetricsEnabled(boolean metricsEnabled) {
	this.metricsEnabled = metricsEnabled;
}

public boolean isAlertEnabled() {
	return alertEnabled;
}

public void setAlertEnabled(boolean alertEnabled) {
	this.alertEnabled = alertEnabled;
}
}
