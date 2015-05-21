/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.db;

public interface DynamicQuery 
{
	String getQueryString(DBConnectionWrapper conn, boolean gc);
}
