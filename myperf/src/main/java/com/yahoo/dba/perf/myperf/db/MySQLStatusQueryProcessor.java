package com.yahoo.dba.perf.myperf.db;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.ColumnInfo;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.MyPerfContext;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultRow;

public class MySQLStatusQueryProcessor implements CustomQueryProcessor
{
	  protected static Logger logger = Logger.getLogger(MySQLStatusQueryProcessor.class.getName());
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
		  qps2.setSql("mysql_global_status_metrics");
		  for(String key: qps.getSqlParams().keySet()){
			  qps2.getSqlParams().put(key, qps.getSqlParams().get(key));
		  }
		  if(!qps2.getSqlParams().containsKey("p_1")) {
			  
			  qps2.getSqlParams().put("p_1", "");
		  }
		  //mysql_global_status_metrics
		  //mysql_show_global_status
		  ResultList rList = null;
		  rList = context.getQueryEngine().executeQueryGeneric(qps2, connWrapper, qps.getMaxRows());
		  if(rList != null)
		  {
			  
			  ResultList newList = new ResultList();
			  ColumnDescriptor desc = rList.getColumnDescriptor();
			  ColumnDescriptor newDesc = new ColumnDescriptor();
			  int idx = 0;
			  int nameIdx = 0;
			  for(ColumnInfo c: desc.getColumns())
			  {
				  if("VARIABLE_NAME".equalsIgnoreCase(c.getName()))nameIdx =idx;
				  if("VALUE".equalsIgnoreCase(c.getName()))
					  newDesc.addColumn("VARIABLE_VALUE", c.isNumberType(), idx++);
				  else 
					  newDesc.addColumn(c.getName().toUpperCase(), c.isNumberType(), idx++);
			  }
			  
			  newList.setColumnDescriptor(newDesc);
			  for(ResultRow row: rList.getRows())
			  {
				  ResultRow newRow = new ResultRow();
				  newRow.setColumnDescriptor(newDesc);
				  int cols = row.getColumns().size();
				  for(int i=0; i<cols; i++)
				  {
					  String v = row.getColumns().get(i);
					  if(i == nameIdx)
					  {
						  newRow.addColumn(v==null?null:v.toUpperCase());
					  }else newRow.addColumn(v);
				  }
				  newList.addRow(newRow);
			  }
			  return newList;
		  }
		  return null;
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
