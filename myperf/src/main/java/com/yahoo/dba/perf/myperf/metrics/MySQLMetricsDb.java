/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.metrics;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.MetricsGroup;

/**
 * persistence store for metrics
 * @author xrao
 *
 */
public class MySQLMetricsDb  extends MetricsDbBase implements java.io.Serializable, Runnable
{
	  private static final long serialVersionUID = 1L;
	  private static Logger logger = Logger.getLogger(MySQLMetricsDb.class.getName());  
	
	  //private java.text.DecimalFormat df = new java.text.DecimalFormat("#.###");
	  //private java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
	  	 
	  public MySQLMetricsDb()
	  {
	  }
	  public String getDbType() 
	  {
	    return "mysql";
	  }
		
	  
	  protected String buildMetricCodeDDL()
	  {
		  return "CREATE TABLE METRIC_CODE (CODE_ID INT NOT NULL AUTO_INCREMENT, NAME VARCHAR(128), PRIMARY KEY(CODE_ID))";
		  
	  }
	  @Override
	  protected String buildMetricCodeIndexDDL() {
		return "create unique index UK_COCE on METRIC_CODE (NAME)";
	  }

	  protected String buildAlertDDL()
	  {
		 return "CREATE TABLE ALERT (DBID INT, TS BIGINT, END_TS BIGINT DEFAULT '0', ALERT_TYPE VARCHAR(30), ALERT_REASON VARCHAR(128), PRIMARY KEY(DBID, TS))";
		  
	  }
	  
	  @Override
	  protected String buildAlertIndexDDL() {
		return "create index IDX_ALERT_TS on ALERT(TS)";
	  }

	  protected String[] buildGenericMetricDDL()
	  {
		  return new String[]{
			"CREATE TABLE METRIC_GENERIC (DBID INT, METRIC_ID INT, SNAP_ID INT, TS BIGINT, VALUE DECIMAL(22,7), PRIMARY KEY(DBID, METRIC_ID, SNAP_ID), KEY idx_metrics_dbid_ts (DBID, SNAP_ID), KEY idx_metrics_ts (SNAP_ID) )"
		  };
		  
	  }
	  
	  protected Connection createConnection(boolean autocommit)
	  {
	    Connection conn = null;
		try
		{	
		    conn = DriverManager.getConnection(this.getConnectionString(), this.getUsername(), this.getPassword());	  
		    if(!autocommit)conn.setAutoCommit(false);
		  return conn;
		}catch(Exception ex)
	    {
			logger.log(Level.SEVERE,"Exception when connect to "+this.getConnectionString()+" as "+this.getUsername(), ex);
		}

	    return conn;
	  }
	  @Override
	  protected void storeGenericMetric(String s, List<MetricsData> q2, Connection conn) throws SQLException
	  {
		  Statement stmt = null;
		  MetricsGroup mg = this.metricsGroups.get(s);
		  StringBuilder sb = new StringBuilder();
		  String targetTable = mg!=null?mg.getTargetTable():null;
		  if(targetTable==null || targetTable.isEmpty())
      		targetTable = "METRIC_GENERIC";

		  sb.append("INSERT INTO "+targetTable+" (DBID, METRIC_ID, SNAP_ID, TS, VALUE) VALUES" );
	      try	      
	      {
			  stmt = conn.createStatement();
	    	  int cnt = 0;
	    	  int batchSize = 100;//hard coded for now
	    	  for(MetricsData dbuf:q2)
	    	  {
	    		  ByteBuffer buf = dbuf.data;
	    		  int idx = 1;
	    		  int pos = 0;
	    		  if(cnt>0)sb.append(",");
	    		  sb.append("(");
	    		  sb.append(buf.getInt(pos));pos+=4;
	    		  sb.append(",").append(buf.getInt(pos));idx++;pos+=4;        
	    		  sb.append(",").append(buf.getInt(pos));idx++;pos+=4;        
	    		  sb.append(",").append(sdf.format(new java.util.Date(buf.getLong(pos))));idx++;pos+=8;//TODO sdf thread safety
	    		  sb.append(",").append(df.format(buf.getDouble(pos))).append(")");idx++;
	    		  cnt++;
	    		  if(cnt>=batchSize)
	    		  {
	    			  //logger.info("Batch statement: "+sb.toString());
	    			  stmt.execute(sb.toString());
	    			  cnt=0;
	    			  //conn.commit();
	    			  sb.setLength(0);
	    			  sb.append("INSERT INTO "+targetTable+" (DBID, METRIC_ID, TS, VALUE) VALUES" );
	    		  }
	    	  }//for loop
	    	  if(cnt>0)
	    	  {
    			  //logger.info("Batch statement: "+sb.toString());
    			  stmt.execute(sb.toString());
	    		  //conn.commit();
	    		  sb.setLength(0);
	    	  }
	    	  stmt.close(); stmt = null;
	      }finally
	      {
	    	   DBUtils.close(stmt);
	      }
		  	  
	  }
	@Override
	protected String[] buildHostDDL() {
		return new String[] {
			  	"create table DBINFOS(DBID INT NOT NULL AUTO_INCREMENT, DBTYPE VARCHAR(30), DBGROUPNAME VARCHAR(30),INSTANCE SMALLINT, HOSTNAME VARCHAR(100),PORT SMALLINT, DATABASE_NAME VARCHAR(60),USE_SSHTUNNEL SMALLINT, LOCAL_HOSTNAME VARCHAR(100), LOCAL_PORT SMALLINT, CREATED TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONNECTION_VERIFIED SMALLINT DEFAULT 0,VIRTUAL_HOST SMALLINT DEFAULT 0,SNMP_ENABLED SMALLINT DEFAULT 1, METRICS_ENABLED SMALLINT DEFAULT 1, ALERT_ENABLED SMALLINT DEFAULT 1, OWNER VARCHAR(30), PRIMARY KEY(DBID), UNIQUE KEY UK_DBINFOS (DBGROUPNAME, HOSTNAME))"
		};
	}
	@Override
	protected String[] buildAlertSettingDDL() {
		return new String[] {
				"create table " + MetricsDbBase.ALERTSETTING_TABLENAME +"(DBGROUPNAME VARCHAR(30), HOSTNAME VARCHAR(100), ALERT_TYPE VARCHAR(30), THRESHOLD DECIMAL(22,7), RESERVED VARCHAR(255), CREATED TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(DBGROUPNAME, HOSTNAME, ALERT_TYPE))"
		};
	}
	@Override
	protected String[] buildSnapshotDDL() {
		return new String[]{"create table SNAPSHOTS(SNAP_ID INT NOT NULL AUTO_INCREMENT, START_TS BIGINT, END_TS BIGINT, FAILED_HOSTS INT, SUCCEED_HOSTS INT, CREATED TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(SNAP_ID),  KEY IDX_SNAPSHOTS_TS (START_TS))"};
	}
	@Override
	protected String[] buildAlertSubScriptionDDL() {
	    return new String[]{"CREATE TABLE ALERT_SUBSCRIPT ("
	         + "ID INT NOT NULL AUTO_INCREMENT, DBGROUP VARCHAR(30) NOT NULL, HOSTNAME VARCHAR(100), ALERT_NAME VARCHAR(30) NOT NULL, PARAMS VARCHAR(512), PRIMARY KEY(ID), UNIQUE KEY (DBGROUP, HOSTNAME, ALERT_NAME), KEY (ALERT_NAME))"};
	}
	@Override
	protected String[] buildMetricsSubscrptionDDL() {
	    return new String[]{"CREATE TABLE METRICS_SUBSCRIPT ("
	     + "ID INT NOT NULL AUTO_INCREMENT, DBGROUP VARCHAR(30) NOT NULL, HOSTNAME VARCHAR(100), MGROUP VARCHAR(30) NOT NULL,  MSUBGROUP VARCHAR(30), PRIMARY KEY(ID), UNIQUE KEY (DBGROUP, HOSTNAME, MGROUP, MSUBGROUP), KEY (MGROUP, MSUBGROUP))"};
	}
	@Override
	protected boolean isLimitSupport() {
		return true;
	}
	@Override
	protected String[] buildAlertNotificationDDL() {
	    return new String[]{"CREATE TABLE ALERT_NOTIFICATION ("
		         + "ID INT NOT NULL AUTO_INCREMENT, DBGROUP VARCHAR(30) NOT NULL, HOSTNAME VARCHAR(100), EMAILS VARCHAR(1000) NOT NULL,  PRIMARY KEY(ID), UNIQUE KEY (DBGROUP, HOSTNAME))"};
	}

}
