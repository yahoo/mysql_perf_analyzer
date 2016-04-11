/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

public class Constants 
{
  //Analyzer version
  public static final String VERSION = "2.0";
	
  //Status
  public static final int STATUS_OK = 0;
  public static final int STATUS_BAD = -1;
  
  // DB instance management action
  public static final int DBM_ACTION_ADD_CLUSTER=0;
  public static final int DBM_ACTION_ADD_CLUSTER_USING_VIP=1;
  public static final int DBM_ACTION_ADD_HOST=2;
  public static final int DBM_ACTION_UPDATE_HOST=3;
  public static final int DBM_ACTION_REMOVE_HOST=4;
  public static final int DBM_ACTION_REMOVE_CLUSTER=5;
  public static final int DBM_ACTION_RENAME_CLUSTER=6;
  public static final int DBM_ACTION_ACL=7;
  	
  public static final String URL_PATH_CMD = "CMD";
  public static final String URL_PATH_DBGROUP = "DBGROUP";
  public static final String URL_PATH_DBHOST = "DBHOST";
  public static final String URL_PATH_METRICS = "METRICS";
  public static final String URL_PATH_START_TS = "START_TS";
  public static final String URL_PATH_END_TS = "END_TS";
  public static final String URL_PATH_ALERT_TYPE = "ALERT_TYPE";

  public static final String SESSION_DEBUG="sess_debug";
  
  public static final String CONN_MSG_NORETRY = "CONN_NO_RETRY";

}
