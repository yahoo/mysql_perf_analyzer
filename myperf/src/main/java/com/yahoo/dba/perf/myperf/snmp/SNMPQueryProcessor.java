/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.snmp;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultRow;
import com.yahoo.dba.perf.myperf.db.CustomQueryProcessor;
import com.yahoo.dba.perf.myperf.db.DBConnectionWrapper;
import com.yahoo.dba.perf.myperf.snmp.SNMPClient.SNMPTriple;

/**
 * Query a single category of SNMP data, use parameter p_1 to decide the category
 *   sys
 *   disk
 *   network
 *   all (all of above)
 *   default to all
 * @author xrao
 *
 */
public class SNMPQueryProcessor implements CustomQueryProcessor{
	private static Logger logger = Logger.getLogger(SNMPQueryProcessor.class.getName());
	
	@Override
	public  void  queryMultiple(MyPerfContext context,  DBInstanceInfo dbinfo, String appUser, DBConnectionWrapper connWrapper, QueryParameters qps, Map<String, ResultList> rListMap)
	throws java.sql.SQLException
	{
		throw new RuntimeException("Not implmented");				
	}

	@Override
	public ResultList querySingle(MyPerfContext context, DBInstanceInfo dbinfo, String appUser,
			DBConnectionWrapper connWrapper, QueryParameters qps)
			throws SQLException {
		String cat = "sys";
		if(qps.getSqlParams().containsKey("p_1"))
		{
			cat = qps.getSqlParams().get("p_1");
		}
		SNMPClient client = null;
		try
		{
		  client = new SNMPClient(qps.getHost());
	      client.setSnmpSetting(context.getSnmpSettings()
	    		  .getHostSetting(dbinfo.getDbGroupName(), dbinfo.getHostName()));
		  client.start();
		  if("disk".equalsIgnoreCase(cat))
		  {
			return queryDisk(client, qps);
		  }else if("network".equalsIgnoreCase(cat))
		  {
			return queryNetwork(client, qps);
		  }else if("storage".equalsIgnoreCase(cat))
		  {
			return this.queryStorage(client, qps);
		  }else if("sys".equalsIgnoreCase(cat))
		  {
			return querySystemData(client, qps);
		  }else if("mysqld".equalsIgnoreCase(cat))
		  {
			return queryMysqldData(client, qps);
		  }else if("single".equalsIgnoreCase(cat))
		  {
			return this.querySingleSNMP(client, qps);
		  }else if("table".equalsIgnoreCase(cat))
		  {
			return this.queryTableSNMP(client, qps);
		  }else if("unknown".equalsIgnoreCase(cat))
		  {
			  ResultList res = this.querySingleSNMP(client, qps);
			  if(res == null || res.getRows().size() == 0 
					  ||(res.getRows().size() == 1 && "noSuchObject".equals(res.getRows().get(0).getColumns().get(2))))
			  {
				  res = this.queryTableSNMP(client, qps);
			  }
			  return res;
		  }
		  else
		  {
			  ResultList sysList = querySystemData(client, qps);
			  
			  if(sysList == null)return null; //don't expect no system data
			  try
			  {
   			    ResultList diskList = queryDisk(client, qps);;
			    //ResultList mysqldList  = queryMysqldData(client, qps);
			    if(diskList != null)
			    {
				  for(ResultRow row: diskList.getRows())
				  {
					  row.setColumnDescriptor(sysList.getColumnDescriptor());
					  sysList.addRow(row);
				  }
			    }
			  }catch(Exception ex)
			  {
				  logger.log(Level.INFO, "Failed to query disk data", ex);
				  return sysList;
			  }
			  try
			  {
  			    ResultList netList = queryNetwork(client, qps);
			    if(netList != null)
			    {
				  for(ResultRow row: netList.getRows())
				  {
					  row.setColumnDescriptor(sysList.getColumnDescriptor());
					  sysList.addRow(row);
				  }
			    }
			  }catch(Exception ex)
			  {
				  logger.log(Level.INFO, "Failed to query netif data", ex);
				  return sysList;
			  }
			  try
			  {
  			    ResultList storageList  = queryStorage(client, qps);
			    if(storageList != null)
			    {
				  for(ResultRow row: storageList.getRows())
				  {
					  row.setColumnDescriptor(sysList.getColumnDescriptor());
					  sysList.addRow(row);
				  }
			    }
			  }catch(Exception ex)
			  {
				  logger.log(Level.INFO, "Failed to query storage data", ex);
				  return sysList;
			  }
			  //if(mysqldList != null)
			  //{
			  //	  for(ResultRow row: mysqldList.getRows())
			  //  {
			  //	  row.setColumnDescriptor(sysList.getColumnDescriptor());
			  //	  sysList.addRow(row);
			  // }
			  //}
			  return sysList;
		  }
		}catch(Throwable ex)
		{
		   throw new SQLException(ex);//not a good way, but leave it as is
		}finally
		{
			if(client != null)try{client.stop();}catch(Exception iex){}
		}
	}

	/**
	 * Query disk data, in (name, oid, value). Note name will be prefixed with disk name.
	 * @param qps
	 * @return
	 * @throws Exception
	 */
	private ResultList queryDisk(SNMPClient client, QueryParameters qps) throws Exception
	{
		boolean diff = "1".equalsIgnoreCase(qps.getSqlParams().get("p_2"));
		Map<String, List<SNMPTriple>> snmpData = client.getMultiDiskData();
		if(snmpData == null) return null;
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("NAME", false, 0);
		desc.addColumn("OID", false, 1);
		desc.addColumn("VALUE", false, 2);
		
		ResultList rList = new ResultList();
		rList.setColumnDescriptor(desc);
		
		for(Map.Entry<String, List<SNMPTriple>> e: snmpData.entrySet())
		{
		  String disk = e.getKey();
		  for(SNMPTriple t: e.getValue())
		  {
			  if(diff)
			  {
				  try{BigDecimal bd = new BigDecimal(t.value);}catch(Exception ex){continue;}
			  }
			  ResultRow row = new ResultRow();
			  row.addColumn(disk + "." + t.name);
			  row.addColumn(t.oid);
			  row.addColumn(t.value);
			  row.setColumnDescriptor(desc);
			  rList.addRow(row);
		  }
		}
		
		return rList;
	}

	private ResultList queryNetwork(SNMPClient client, QueryParameters qps) throws Exception
	{
		boolean diff = "1".equalsIgnoreCase(qps.getSqlParams().get("p_2"));
		Map<String, List<SNMPTriple>> snmpData = client.getNetIfData(null);
		if(snmpData == null) return null;
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("NAME", false, 0);
		desc.addColumn("OID", false, 1);
		desc.addColumn("VALUE", false, 2);
		
		ResultList rList = new ResultList();
		rList.setColumnDescriptor(desc);
		
		for(Map.Entry<String, List<SNMPTriple>> e: snmpData.entrySet())
		{
		  String net = e.getKey();
		  for(SNMPTriple t: e.getValue())
		  {
			  if(diff)
			  {
				  try{BigDecimal bd = new BigDecimal(t.value);}catch(Exception ex){continue;}
			  }
			  ResultRow row = new ResultRow();
			  row.addColumn(net + "." + t.name);
			  row.addColumn(t.oid);
			  row.addColumn(t.value);
			  row.setColumnDescriptor(desc);
			  rList.addRow(row);
		  }
		}
		
		return rList;
	}

	private ResultList queryStorage(SNMPClient client, QueryParameters qps) throws Exception
	{
		boolean diff = "1".equalsIgnoreCase(qps.getSqlParams().get("p_2"));
		Map<String, List<SNMPTriple>> snmpData = client.getStorageData(null);
		if(snmpData == null) return null;
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("NAME", false, 0);
		desc.addColumn("OID", false, 1);
		desc.addColumn("VALUE", false, 2);
		
		ResultList rList = new ResultList();
		rList.setColumnDescriptor(desc);
		
		for(Map.Entry<String, List<SNMPTriple>> e: snmpData.entrySet())
		{
		  String net = e.getKey();
		  for(SNMPTriple t: e.getValue())
		  {
			  if(diff)
			  {
				  try{BigDecimal bd = new BigDecimal(t.value);}catch(Exception ex){continue;}
			  }
			  ResultRow row = new ResultRow();
			  row.addColumn(net + "." + t.name);
			  row.addColumn(t.oid);
			  row.addColumn(t.value);
			  row.setColumnDescriptor(desc);
			  rList.addRow(row);
		  }
		}
		
		return rList;
	}

	private ResultList querySystemData(SNMPClient client, QueryParameters qps) throws Exception
	{
		boolean diff = "1".equalsIgnoreCase(qps.getSqlParams().get("p_2"));
		List<SNMPTriple> snmpData = client.querySysData3();
		if(snmpData == null) return null;
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("NAME", false, 0);
		desc.addColumn("OID", false, 1);
		desc.addColumn("VALUE", false, 2);
		
		ResultList rList = new ResultList();
		rList.setColumnDescriptor(desc);
		
		for(SNMPTriple t: snmpData)
		{
			  if(diff)
			  {
				  try{BigDecimal bd = new BigDecimal(t.value);}catch(Exception ex){continue;}
			  }
	          ResultRow row = new ResultRow();
			  row.addColumn(t.name);
			  row.addColumn(t.oid);
			  row.addColumn(t.value);
			  row.setColumnDescriptor(desc);
			  rList.addRow(row);	 
		}
		
		return rList;
	}
	private ResultList queryMysqldData(SNMPClient client, QueryParameters qps) throws Exception
	{
		boolean diff = "1".equalsIgnoreCase(qps.getSqlParams().get("p_2"));
		List<SNMPTriple> snmpData = null;
		try{snmpData = client.getProcessData("mysqld");}catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Failed to retrieve mysqld perf data", ex);
		}
		if(snmpData == null) return null;
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("NAME", false, 0);
		desc.addColumn("OID", false, 1);
		desc.addColumn("VALUE", false, 2);
		
		ResultList rList = new ResultList();
		rList.setColumnDescriptor(desc);
		
		for(SNMPTriple t: snmpData)
		{
			  if(diff)
			  {
				  try{BigDecimal bd = new BigDecimal(t.value);}catch(Exception ex){continue;}
			  }
	          ResultRow row = new ResultRow();
			  row.addColumn(t.name);
			  row.addColumn(t.oid);
			  row.addColumn(t.value);
			  row.setColumnDescriptor(desc);
			  rList.addRow(row);	 
		}
		
		return rList;
	}

	private ResultList querySingleSNMP(SNMPClient client, QueryParameters qps) throws Exception
	{
		//p2 will be oid
		String oid = qps.getSqlParams().get("p_2");
		List<SNMPTriple> snmpData = client.querySingleSNMPEntryByOID(oid);
		if(snmpData == null) return null;
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("NAME", false, 0);
		desc.addColumn("OID", false, 1);
		desc.addColumn("VALUE", false, 2);
		
		ResultList rList = new ResultList();
		rList.setColumnDescriptor(desc);
		
		for(SNMPTriple t: snmpData)
		{
	        ResultRow row = new ResultRow();
			row.addColumn(t.name);
			row.addColumn(t.oid);
			row.addColumn(t.value);
			row.setColumnDescriptor(desc);
			rList.addRow(row);	 
		}
		
		return rList;
	}

	private ResultList queryTableSNMP(SNMPClient client, QueryParameters qps) throws Exception
	{
		//p2 will be oid
		String oid = qps.getSqlParams().get("p_2");
		List<SNMPTriple> snmpData = client.querySingleSNMPTableByOID(oid);
		if(snmpData == null) return null;
		ColumnDescriptor desc = new ColumnDescriptor();
		desc.addColumn("NAME", false, 0);
		desc.addColumn("OID", false, 1);
		desc.addColumn("VALUE", false, 2);
		
		ResultList rList = new ResultList();
		rList.setColumnDescriptor(desc);
		
		for(SNMPTriple t: snmpData)
		{
	        ResultRow row = new ResultRow();
			row.addColumn(t.name);
			row.addColumn(t.oid);
			row.addColumn(t.value);
			row.setColumnDescriptor(desc);
			rList.addRow(row);	 
		}
		
		return rList;
	}

	@Override
	public boolean isMultiple() {
		return false;
	}

	@Override
	public boolean requireDBConnection() {
		return false;
	}

}
