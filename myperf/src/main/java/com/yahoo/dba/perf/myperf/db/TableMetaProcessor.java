/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.db;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultRow;
import com.yahoo.dba.perf.myperf.common.Sql;

public class TableMetaProcessor implements CustomQueryProcessor{
	private static Logger logger = Logger.getLogger(TableMetaProcessor.class.getName());
	private final static String[] TBL_QUERIES = new String[]{"mysql_meta_table_constraints", "mysql_meta_table_triggers", 
		"mysql_meta_table_create", "mysql_meta_table_indexes",
		"mysql_meta_innodb_index_stats","mysql_meta_innodb_table_stats",
		"mysql_meta_table_stats","mysql_meta_table_columns"};
	
	@Override
	public  void  queryMultiple(MyPerfContext context,  DBInstanceInfo dbinfo, String appUser,
			DBConnectionWrapper connWrapper, QueryParameters qps, Map<String, ResultList> rListMap)
	throws java.sql.SQLException
	{
		for(int i=0;i<TBL_QUERIES.length;i++)
		{
			QueryParameters qps2 = new QueryParameters();
			qps2.setSql(TBL_QUERIES[i]);
			qps2.getSqlParams().put("p_1", qps.getSqlParams().get("p_1"));
			qps2.getSqlParams().put("p_2", qps.getSqlParams().get("p_2"));
			
    		ResultList rList = null;
    		try
    		{
    			rList = context.getQueryEngine().executeQueryGeneric(qps2, connWrapper, qps.getMaxRows());
    		}catch(Throwable th)
    		{
    			logger.log(Level.WARNING, "Error when retrieve meta data", th);
    			if(th instanceof SQLException)
    	    	{
    	    		//check if the connection is still good
    	    		if(!DBUtils.checkConnection(connWrapper.getConnection()))
    	    		{
    	    			throw SQLException.class.cast(th);
    	    		}
    	    	}
    			
    			Sql sql = context.getSqlManager().getSql(qps2.getSql());
    			if(sql!=null && sql.isErrorInline())
    			{
    				rList = new ResultList();
    				ColumnDescriptor desc = new ColumnDescriptor();
    				desc.addColumn("Status", false,0);
    				desc.addColumn("Message", false,1);
    				rList.setColumnDescriptor(desc);
    				ResultRow row = new ResultRow();
    				row.setColumnDescriptor(desc);
    				row.addColumn("Error");
    				row.addColumn(th.getMessage());
    				rList.addRow(row);
    			}
    		}
    		rListMap.put(TBL_QUERIES[i], rList);
		}
				
	}

	@Override
	public ResultList querySingle(MyPerfContext context,  DBInstanceInfo dbinfo, String appUser,
			DBConnectionWrapper connWrapper, QueryParameters qps)
			throws SQLException {
		throw new RuntimeException("Not implmented");
	}

	@Override
	public boolean isMultiple() {
		return true;
	}

	@Override
	public boolean requireDBConnection() {
		return true;
	}
}
