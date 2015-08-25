package com.yahoo.dba.perf.myperf.db;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.DBCredential;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultRow;

/**
 * Display replication topology and status
 * @author xrao
 *
 */
public class ReplShowProcessor implements CustomQueryProcessor{

	private static Logger logger = Logger.getLogger(ReplShowProcessor.class.getName());
	
	//a repl server is a host:port pair
	private static class ReplServer
	{
		String host;
		String port;
		boolean probed;
		
		List<ReplServer> repls = new ArrayList<ReplServer>();
		
		ReplServer(String host, String port)
		{
			this.host = host;
			this.port = port;
		}
		
		void addServer(ReplServer server){
			boolean findOne = false;
			for(int i=0;i<repls.size(); i++)
			{
				ReplServer s = repls.get(i);
				if(s.host.equalsIgnoreCase(server.host) && s.port.equals(server.port))
					findOne = true;
			}
			if(!findOne)repls.add(server);
		}
		//slef and children
		Map<String, ReplServer> getAllServers()
		{
			Map<String, ReplServer> childs = new LinkedHashMap<String, ReplServer>();
			childs.put(this.toString(), this); //add self first
			listAllChildren(childs);
			return childs;
		}
		void listAllChildren(Map<String, ReplServer> childs)
		{
			for(ReplServer s: repls)
			{
				if(!childs.containsKey(s.toString()))
				{
					childs.put(s.toString(), s);
					s.listAllChildren(childs);
				}
			}
		}
		@Override
		public String toString() {
			return this.host + ":" + this.port;
		}
		
		
	}
	
	//Cache the entries for short period so that we don't need probe on every request
	//max cache age is configured in MyPerfConfiguration
	private static class ReplTopologyCacheEntry
	{
		long createTime = System.currentTimeMillis();
		private ReplServer rootServer;
		
		ReplTopologyCacheEntry(ReplServer rootServer)
		{
			this.rootServer = rootServer;
		}
		
		ReplServer getServer()
		{
			return rootServer;
		}
		
		//when update with new entries, the client should lock the ReplTopologyCacheEntry
		//before invoke this method
		void update(ReplServer newRoot)
		{
			this.rootServer = newRoot;
			createTime = System.currentTimeMillis();
		}
	}
	//Store repl status per repl server
	private static class ReplStatus
	{
		String hostname;
		String port = "3306";
		String masterFile;
		String masterPosition;
		String masterExecutedGtidSet;
		String masterHost;
		String masterPort;
		String lag;//seconds behind master
		String io;
		String sql;
		String masterLogFile;
		String readMasterLogPos;
		String relayMasterLogFile;
		String execMasterLogPos;
		String executedGtidSet;
		ReplStatus()
		{
			
		}
		
		ReplStatus(String host, String port)
		{
			this.hostname = host;
			this.port = port;
		}
	}
	
	//For all servers in a topology, it will share the same ReplTopologyCacheEntry
	private Map<String, ReplTopologyCacheEntry> topologyCache = new HashMap<String, ReplTopologyCacheEntry>();
	
	@Override
	public void queryMultiple(MyPerfContext context, DBInstanceInfo dbinfo,
			String appUser, DBConnectionWrapper connWrapper,
			QueryParameters qps, Map<String, ResultList> rListMap)
			throws SQLException {
		throw new RuntimeException("Not implmented");
		
	}

	@Override
	public ResultList querySingle(MyPerfContext context, DBInstanceInfo dbinfo,
			String appUser, DBConnectionWrapper connWrapper, QueryParameters qps)
			throws SQLException {
		Map<String, ReplStatus> statusResults = new java.util.LinkedHashMap<String, ReplStatus>();
		DBCredential cred = context.getMetaDb().retrieveDBCredential(appUser, dbinfo.getDbGroupName());
		ReplServer replServer = new ReplServer(dbinfo.getHostName(), dbinfo.getPort());
		
		//check cache
		ReplServer startingServer = null;
		synchronized (this.topologyCache)
		{
			ReplTopologyCacheEntry cache = this.topologyCache.get(replServer.toString());
			if(cache != null && cache.createTime + context.getMyperfConfig().getReplTopologyCacheMaxAge() > System.currentTimeMillis())
				startingServer = cache.rootServer;
		}
		
		if(cred != null)
		{
			if(startingServer == null )
			{
				statusResults.put(replServer.toString(), new ReplStatus(dbinfo.getHostName(),dbinfo.getPort() ));
				queryDetail(context, cred, statusResults, replServer, 0);
			}else
			{
				Map<String, ReplServer> allServers = startingServer.getAllServers();
				for(Map.Entry<String, ReplServer> e: allServers.entrySet())
				{
					ReplServer s = e.getValue();
					ReplServer rplServer = new ReplServer(s.host, s.port);
					rplServer.probed = true;
					statusResults.put(rplServer.toString(), new ReplStatus(s.host, s.port));
					queryDetail(context, cred, statusResults, rplServer, 0);
				}
				
			}
		}
		
		ReplServer rootServer = this.buildReplTree(statusResults);
		if(rootServer == null)
		{
			logger.warning("Cannot find replication root for " + replServer);
		}else if(startingServer == null)
		{
			//update cache
			logger.info("Update cache");
			synchronized (this.topologyCache)
			{
				for(Map.Entry<String, ReplStatus> e: statusResults.entrySet())
				{					
					ReplTopologyCacheEntry cache = this.topologyCache.get(e.getKey());
					if(cache == null)
					{
						cache = new ReplTopologyCacheEntry(rootServer);
						this.topologyCache.put(e.getKey(), cache);
					}else
						cache.update(rootServer);
				}
			}
		}
		ResultList rList = new ResultList();
		ColumnDescriptor desc = new ColumnDescriptor();
		rList.setColumnDescriptor(desc);
		int idx = 0;
		desc.addColumn("SERVER", false, idx++);//use host:port
		desc.addColumn("MASTER SERVER", false, idx++);//use host:port
		desc.addColumn("LAG", false, idx++);
		desc.addColumn("IO/SQL", false, idx++);//IO/SQL
		desc.addColumn("MASTER: FILE", false, idx++);//Master file: master position
		desc.addColumn("MASTER LOG FILE", false, idx++);//Master file: master position
		desc.addColumn("READ MASTER LOG POS", false, idx++);
		desc.addColumn("RELAY MASTER LOG FILE", false, idx++);
		desc.addColumn("EXEC MASTER LOG POS", false, idx++);
		desc.addColumn("MASTER EXECUTED GTID", false, idx++);
		desc.addColumn("EXECUTED GTID", false, idx++);
		
		Set<String> probed = new HashSet<String>();
		outputTree(rootServer, statusResults, probed, 0,  rList);
		
		//for (Map.Entry<String, ReplStatus> e: statusResults.entrySet())
		//{
		//	ReplStatus rpl = e.getValue();
		//	addOutputRow(rpl, rList, 0);
		//}
		return rList;
	}

	private void outputTree(ReplServer server, Map<String, ReplStatus> statusResults, Set<String> probed, int level, ResultList rList)
	{
		if(server == null) return;
		//always output self
		ReplStatus rpl = statusResults.get(server.toString());
		if(rpl == null)return; //don't expect so
		addOutputRow(rpl, rList, level);
		//if current server probed, or no children stop here
		if(probed.contains(server.toString()) || server.repls.size() == 0)
			return;
		probed.add(server.toString());
		for(ReplServer s: server.repls)
		{
			outputTree(s, statusResults, probed, level + 1, rList);
		}
		
	}
	private void addOutputRow(ReplStatus rpl, ResultList rList, int level)
	{
		ResultRow row = new ResultRow();
		String prefix  = "";
		if(level>0)
		{
			for(int i=0; i<level; i++)
			{
				prefix += "-";
			}
		}
		row.addColumn(prefix + rpl.hostname+":"+rpl.port);
		if(rpl.masterHost != null && !rpl.masterHost.isEmpty())
			row.addColumn(rpl.masterHost+":"+rpl.masterPort);
		else
			row.addColumn(null);
		row.addColumn(rpl.lag);
		if(rpl.io != null || rpl.sql != null)
			row.addColumn(rpl.io+"/"+rpl.sql);
		else row.addColumn(null);
		if(rpl.masterFile!=null)
			row.addColumn(rpl.masterFile+":"+rpl.masterPosition);
		else row.addColumn(null);
		row.addColumn(rpl.masterLogFile);
		row.addColumn(rpl.readMasterLogPos);
		row.addColumn(rpl.relayMasterLogFile);
		row.addColumn(rpl.execMasterLogPos);
		row.addColumn(rpl.masterExecutedGtidSet);
		row.addColumn(rpl.executedGtidSet);
		row.setColumnDescriptor(rList.getColumnDescriptor());
		rList.addRow(row);
		
	}
	/**
	 * Recursive probe. Note repl must be inside replStatus map
	 * @param context
	 * @param cred
	 * @param replStatus
	 * @param repl
	 */
	private void queryDetail(MyPerfContext context, DBCredential cred,
			Map<String, ReplStatus> replStatusMap, ReplServer replServer, int depth)
	{
		logger.info("Probing " + replServer.toString());
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		//store new hosts
		ReplStatus repl = replStatusMap.get(replServer.toString());
		Map<String, String> hosts = new java.util.LinkedHashMap<String, String>();
		boolean toprobe = depth<context.getMyperfConfig().getReplTopologyMaxDepth() && !replServer.probed;
		try
		{
			DBInstanceInfo dbinfo2 = new DBInstanceInfo();
			dbinfo2.setHostName(replServer.host);
			dbinfo2.setPort(replServer.port);
			dbinfo2.setDatabaseName("information_schema");
			dbinfo2.setDbType("mysql");
			String url = dbinfo2.getConnectionString();
			
		    DriverManager.setLoginTimeout(60);
			conn = DriverManager.getConnection(url, cred.getUsername(), cred.getPassword());
			if(conn == null)return;//we might not have permission
			stmt = conn.createStatement();
			//first, probe master status
			rs = stmt.executeQuery("show master status");
			if(rs!=null && rs.next())
			{
				java.sql.ResultSetMetaData meta = rs.getMetaData();
				int col = meta.getColumnCount();
				for(int i=1; i<=col; i++)
				{
					String colName = meta.getColumnLabel(i);
					String val = rs.getString(i);
					if("File".equalsIgnoreCase(colName))
						repl.masterFile = val;
					else if("Position".equalsIgnoreCase(colName))
						repl.masterPosition = val;
					else if("Executed_Gtid_Set".equalsIgnoreCase(colName))
						repl.masterExecutedGtidSet = val;
				}
			}
			DBUtils.close(rs); rs = null;
			//then slave status
			rs = stmt.executeQuery("show slave status");
			if(rs!=null && rs.next())
			{
				java.sql.ResultSetMetaData meta = rs.getMetaData();
				int col = meta.getColumnCount();
				for(int i=1; i<=col; i++)
				{
					String colName = meta.getColumnLabel(i);
					String val = rs.getString(i);
					if("Master_Host".equalsIgnoreCase(colName))
						repl.masterHost =  getHostnameByIp(val);
					else if("Master_Port".equalsIgnoreCase(colName))
						repl.masterPort = val;
					else if("Master_Log_File".equalsIgnoreCase(colName))
						repl.masterLogFile = val;
					else if("Read_Master_Log_Pos".equalsIgnoreCase(colName))
						repl.readMasterLogPos = val;
					else if("Relay_Master_Log_File".equalsIgnoreCase(colName))
						repl.relayMasterLogFile = val;
					else if("Exec_Master_Log_Pos".equalsIgnoreCase(colName))
						repl.execMasterLogPos = val;
					else if("Executed_Gtid_Set".equalsIgnoreCase(colName))
						repl.executedGtidSet = val;
					else if("Seconds_Behind_Master".equalsIgnoreCase(colName))
						repl.lag = val;
					else if("Slave_IO_Running".equalsIgnoreCase(colName))
						repl.io = val;
					else if("Slave_SQL_Running".equalsIgnoreCase(colName))
						repl.sql = val;
				}
				if(toprobe)hosts.put(repl.masterHost, repl.masterPort);
			}
			DBUtils.close(rs); rs = null;
			
			if(toprobe)
			{
				rs = stmt.executeQuery("select host from information_schema.processlist where command like 'Binlog Dump%'");
				while(rs != null && rs.next())
				{
					String host = rs.getString(1);
					if(host != null && host.indexOf(":")>0)
					{
						host = host.substring(0, host.indexOf(':'));
					}
					host = getHostnameByIp(host);
					if(host != null && !host.isEmpty())
						hosts.put(host, "3306"); //we cannot probe port, assume 3306
				}
			
				DBUtils.close(rs); rs = null;
			}
			DBUtils.close(stmt); stmt = null;
			DBUtils.close(conn); conn = null;			
		}catch(Exception ex)
		{
			logger.log(Level.WARNING, "Failed to retrieve repl info from "+repl.hostname+":"+repl.port, ex);
		}finally
		{
			DBUtils.close(rs);
			DBUtils.close(stmt);
			DBUtils.close(conn);
		}
		
		//probe 
		for(Map.Entry<String, String> server: hosts.entrySet())
		{
			if(replStatusMap.containsKey(server.getKey()+":"+server.getValue()))
				continue;
			ReplStatus replChild = new ReplStatus();
			replChild.hostname = server.getKey();
			replChild.port = server.getValue();
			ReplServer childServer = new ReplServer(replChild.hostname, replChild.port);
			replStatusMap.put(childServer.toString(), replChild);
			queryDetail(context, cred, replStatusMap, childServer, depth +1);
		}
		logger.info("End of probing " + replServer.toString());
	}
	
	private ReplServer findReplRoot(Map<String, ReplStatus> replStatusMap)
	{
		ReplServer root = null;
		Map<String, ReplServer> probed = new HashMap<String, ReplServer>();
		//only need first probe one
		for(Map.Entry<String, ReplStatus> e: replStatusMap.entrySet())
		{
			ReplStatus rpl = e.getValue();
			//no master, self
			if(rpl.masterHost == null || rpl.masterHost.isEmpty())
				return new ReplServer(rpl.hostname, rpl.port);
			//back to the master
			root = new ReplServer(rpl.masterHost, rpl.masterPort);
			probed.put(root.toString(), root);
			break;
		}
		//at this stage, root has to be here
		while(true)
		{
			ReplStatus rpl = replStatusMap.get(root.toString());
			if(rpl.masterHost == null || rpl.masterHost.isEmpty())
				return root;
			ReplServer newRoot = new ReplServer(rpl.masterHost, rpl.masterPort);
			if(probed.containsKey(newRoot.toString()))
				return newRoot;
			root = newRoot;
			probed.put(root.toString(), root);
		}
	}
	
	//probe the map to build a tree like structure, and return the root
	private ReplServer buildReplTree(Map<String, ReplStatus> replStatusMap)
	{
		Map<String, ReplServer> tree = new HashMap<String, ReplServer>();
		for(Map.Entry<String, ReplStatus> e: replStatusMap.entrySet())
		{
			String self = e.getKey();
			ReplStatus repl = e.getValue();
			String master = repl.masterHost;
			if(master!=null && !master.isEmpty())
				master = master + ":" + repl.masterPort;
			ReplServer s = tree.get(self);
			if ( s == null)
			{
				s = new ReplServer(repl.hostname, repl.port);
				tree.put(self, s);
			}
			if(master != null)
			{
				ReplServer ms = tree.get(master);
				if(ms == null)
				{
					ms = new ReplServer(repl.masterHost, repl.masterPort);
					tree.put(master, ms);
				}
				ms.addServer(s);
			}
		}
		
		ReplServer root = findReplRoot(replStatusMap);
		return tree.get(root.toString());
	}
	private static String getHostnameByIp(String ip)
	{
		if(ip==null||ip.isEmpty())return null;
		char c1 = ip.charAt(0);
		char c2 = ip.charAt(ip.length()-1);
		if(c1>='0' && c1<='9' && c2 >= '0' && c2<='9')
		{
			try
			{
				InetAddress inetAddress = InetAddress.getByName(ip);
				return inetAddress.getHostName().toLowerCase();
			}catch(Exception ex)
			{
			}
		}
		return ip.toLowerCase();
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
