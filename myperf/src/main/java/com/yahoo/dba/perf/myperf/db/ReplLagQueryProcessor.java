/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.DBCredential;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultRow;

/**
 * Query slave status and use master info to add master status
 * @author xrao
 *
 */
public class ReplLagQueryProcessor implements CustomQueryProcessor
{
  protected static Logger logger = Logger.getLogger(ReplLagQueryProcessor.class.getName());
  private static String[] SLAVE_LAG_ENTRIES = new String[]{"Seconds_Behind_Master", "Slave_IO_Running", 
	   "Slave_SQL_Running", "Slave_IO_State"};
  private static String[] SLAVE_POS_ENTRIES = new String[]{"Master_Log_File", "Read_Master_Log_Pos", 
	   "Relay_Master_Log_File", "Exec_Master_Log_Pos","Relay_Log_File","Relay_Log_Pos"};
  @Override
  public void queryMultiple(MyPerfContext context, DBInstanceInfo dbinfo, String appUser,
		DBConnectionWrapper connWrapper, QueryParameters qps,
		Map<String, ResultList> rListMap) throws SQLException {
	throw new RuntimeException("Not implmented");
	
  }

  @Override
  public ResultList querySingle(MyPerfContext context,  DBInstanceInfo dbinfo, String appUser,
		DBConnectionWrapper connWrapper, QueryParameters qps)
		throws SQLException {
	  QueryParameters qps2 = new QueryParameters();
	  qps2.setSql("mysql_repl_slave");
	  ResultList rList = null;
	  rList = context.getQueryEngine().executeQueryGeneric(qps2, connWrapper, qps.getMaxRows());
	  String master = null;
	  String port = null;
	  if(rList != null)
	  {
		  java.util.LinkedHashMap<String, String> kvPairs = new java.util.LinkedHashMap<String, String>(rList.getRows().size());
		  for(ResultRow row: rList.getRows())
		  {
			  if(row.getColumns() == null || row.getColumns().size()<2)continue;
			  kvPairs.put(row.getColumns().get(0), row.getColumns().get(1));
		  }
		  master = kvPairs.get("Master_Host");
		  port = kvPairs.get("Master_Port");
		  if(master != null && !master.isEmpty() && port != null && !port.isEmpty())
		  {
			  logger.info("Query master status from ("+master+":"+port+")");
			  DBInstanceInfo dbinfo2 = new DBInstanceInfo();
			  dbinfo2.setHostName(master);
			  dbinfo2.setPort(port);
			  dbinfo2.setDatabaseName("information_schema");
			  dbinfo2.setDbType("mysql");
			  String url = dbinfo2.getConnectionString();
			  
			  DBCredential cred = null;
			  if(appUser !=null )
			  {
			    try
				{		
				  //first, check if the user has his own credential
				  cred = context.getMetaDb().retrieveDBCredential(appUser, dbinfo.getDbGroupName());
				  if(cred!=null)
				  {
					  Connection conn = null;
					  Statement stmt = null;
					  ResultSet rs = null;
					  try
					  {
						  DriverManager.setLoginTimeout(60);
						  conn = DriverManager.getConnection(url, cred.getUsername(), cred.getPassword());
						  if(conn!=null)
						  {
							stmt = conn.createStatement();
							rs = stmt.executeQuery("show master status");
							//TODO rearrange order
							ResultList rList2 = new ResultList();
							rList2.setColumnDescriptor(rList.getColumnDescriptor());
							for(String e: SLAVE_LAG_ENTRIES)
							{
								ResultRow row = new ResultRow();
								row.setColumnDescriptor(rList.getColumnDescriptor());
								row.addColumn(e);
								row.addColumn(kvPairs.get(e));
								rList2.addRow(row);
								kvPairs.remove(e);
							}
							while(rs!=null && rs.next())
							{
								ResultRow row = new ResultRow();
								row.setColumnDescriptor(rList.getColumnDescriptor());
								row.addColumn("Master: File");
								row.addColumn(rs.getString("File"));
								rList2.addRow(row);
								row = new ResultRow();
								row.setColumnDescriptor(rList.getColumnDescriptor());
								row.addColumn("Master: Position");
								row.addColumn(rs.getString("Position"));
								rList2.addRow(row);
								//TODO gtid set
							}
							//now add slave position info
							for(String e: SLAVE_POS_ENTRIES)
							{
								ResultRow row = new ResultRow();
								row.setColumnDescriptor(rList.getColumnDescriptor());
								row.addColumn(e);
								row.addColumn(kvPairs.get(e));
								rList2.addRow(row);
								kvPairs.remove(e);
							}
							//now add remaining entries
							for(Map.Entry<String, String> e: kvPairs.entrySet())
							{
								ResultRow row = new ResultRow();
								row.setColumnDescriptor(rList.getColumnDescriptor());
								row.addColumn(e.getKey());
								row.addColumn(e.getValue());
								rList2.addRow(row);					
							}
							return rList2;
					      }
						  
					  }catch(Exception ex)
					  {
					  }finally
					  {
					    DBUtils.close(rs);
						DBUtils.close(stmt);
						DBUtils.close(conn);
					  }
				   }
				}catch(Exception ex)
				{			
				}
			  }
		  }
	  }
	  
	  return rList;
  }

  @Override
  public boolean isMultiple() {
	return false;
  }

  @Override
  public boolean requireDBConnection() {
	return true;
  }
}
