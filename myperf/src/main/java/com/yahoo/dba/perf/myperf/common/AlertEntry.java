/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

/**
 * Place holder for alert information
 * @author xrao
 *
 */
public class AlertEntry implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private long ts; //timestamp
	private String alertReason;//the type of alert
	private String alertValue;//the value triggered alert
	private String dbGroup;//database group
	private String dbHost;//database server host
	public AlertEntry()
	{
		
	}
	
	public AlertEntry(long ts, String alertReason, String alertValue, String dbGroup, String dbHost)
	{
		this.ts = ts;
		this.alertReason = alertReason;
		this.alertValue = alertValue;
		this.dbGroup = dbGroup;
		this.dbHost = dbHost;
	}
	
	public long getTs() {
		return ts;
	}
	public void setTs(long ts) {
		this.ts = ts;
	}
	public String getAlertReason() {
		return alertReason;
	}
	public void setAlertReason(String alertReason) {
		this.alertReason = alertReason;
	}
	public String getAlertValue() {
		return alertValue;
	}
	public void setAlertValue(String alertValue) {
		this.alertValue = alertValue;
	}
	public String getDbGroup() {
		return dbGroup;
	}
	public void setDbGroup(String dbGroup) {
		this.dbGroup = dbGroup;
	}
	public String getDbHost() {
		return dbHost;
	}
	public void setDbHost(String dbHost) {
		this.dbHost = dbHost;
	}
	
	public String getAlertTime()
	{
		return CommonUtils.formatTimeFromTimestamp(ts, "yyyy-MM-dd HH:mm:ss");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ALERT: ").append(this.alertReason);
		if(this.alertValue != null && !this.alertValue.isEmpty())
			sb.append(", ").append(this.alertValue);
		sb.append(", ").append(this.dbGroup).append(", ").append(this.dbHost);
		sb.append(", TIME: ").append(this.getAlertTime());
		return sb.toString();
	}
	
	
}
