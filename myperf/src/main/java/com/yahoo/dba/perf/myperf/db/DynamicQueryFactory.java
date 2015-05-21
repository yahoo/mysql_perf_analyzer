/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.db;

import java.util.HashMap;
import java.util.Map;

public class DynamicQueryFactory 
{
  private static Map<String, DynamicQuery> queryMap = new HashMap<String, DynamicQuery>();
	
	
  static
  {
    //queryMap.put("com.yahoo.dba.perf.framework.process.SharedCursorQuery", new SharedCursorQuery());
	//queryMap.put("com.yahoo.dba.perf.framework.process.InstanceCacheTransferSummary", new InstanceCacheTransferSummary());
	//queryMap.put("com.yahoo.dba.perf.framework.process.InstanceCacheTransferSummaryAll", new InstanceCacheTransferSummaryAll());
  }
  public static String getQuery(String queryClass, DBConnectionWrapper conn, boolean gc)
  {
    if(queryMap.containsKey(queryClass))
	  return queryMap.get(queryClass).getQueryString(conn, gc);
	return null;
  }
}
