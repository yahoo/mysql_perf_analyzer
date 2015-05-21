/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

public class QueryInputValidator {

  /**
   * If has issue, a RuntimeException will be thrown with the validation message
   * @param sqlManager
   * @param qps
   * @return
   */
  public static void validateSql(SqlManager sqlManager, QueryParameters qps)
  {
    Sql sql = sqlManager.getSql(qps.getSql());
	if(sql==null)throw new RuntimeException("Cannot find sql for handle "+qps.getSql());
	int paramCnt = sql.getParamCount();
	for(int i=1;i<=paramCnt;i++)
	{
	  //parameter name uses p_nn convention
	  if(!qps.getSqlParams().containsKey("p_"+i))
	  {
	    throw new RuntimeException("Missing parameter p_"+i);
	  }
	}
  }
  
  public static void validateDiffSqls(SqlManager sqlManager, QueryParameters qpsA, QueryParameters qpsB)
  {
    Sql sqlA = sqlManager.getSql(qpsA.getSql());
	if(sqlA==null)throw new RuntimeException("Cannot find sql for handle "+qpsA.getSql());
	int paramCnt = sqlA.getParamCount();
	for(int i=1;i<=paramCnt;i++)
	{
	  //parameter name uses p_nn convention
	  if(!qpsA.getSqlParams().containsKey("p_"+i))
	  {
	    throw new RuntimeException("SQL A is missing parameter p_"+i);
	  }
	}
	Sql sqlB = sqlManager.getSql(qpsB.getSql());
	if(sqlB==null)throw new RuntimeException("Cannot find sql for handle "+qpsB.getSql());
	paramCnt = sqlB.getParamCount();
	for(int i=1;i<=paramCnt;i++)
	{
	  //parameter name uses p_nn convention
	  if(!qpsB.getSqlParams().containsKey("p_"+i))
	  {
	    throw new RuntimeException("SQL B is missing parameter p_"+i);
	  }
	}
		
	//compare key list
	if(sqlA.getKeyList().size()!=sqlB.getKeyList().size())
	{
	  throw new RuntimeException("SQL A and SQL B do not have same number of key columnss.");			
	}
		
	for(String key:sqlA.getKeyList())
	{
	  if(!sqlB.getKeyList().contains(key))
	    throw new RuntimeException("SQL B does not have key "+key);							
	}
	//compare metrics list
	if(sqlA.getValueList().size()!=sqlB.getValueList().size())
	{
	  throw new RuntimeException("SQL A and SQL B do not have same number of value columns.");	
	}
		
	for(String key:sqlA.getValueList())
	{
	  if(!sqlB.getValueList().contains(key))
	    throw new RuntimeException("SQL B does not have value column "+key);							
	}		
  }
}
