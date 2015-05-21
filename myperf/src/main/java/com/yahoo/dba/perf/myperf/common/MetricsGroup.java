/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MetricsGroup groups related metrics, for example, metrics can be gathered by a single SQL call.
 * It can contains zero or more sub groups for storage purpose. Each sub group, if exists, will
 * be stored inside a table with the name defined in the sub group definition.
 * @author xrao
 *
 */
public class MetricsGroup {
	private String groupName;//the name will be also used for storage table name.
	private boolean auto = true;//if false, we need manually enable this metric group for a specific db server
	
	//A top level MetricsGroup can either contains a list of metrics, or a list of sub metrics group, 
	//but not both. If sub groups are presented, the data will be stored per sub group and the storage
	//table names will be the groupname of the sub groups.
	private List<Metric> metrics;
	private Map<String, Metric> metricsNameMap;
	
	private List<MetricsGroup> subGroups = new ArrayList<MetricsGroup>();
	
	private boolean multipleMetricsPerRow = true;//if db returns one row with all required metrics
	private String keyColumn; //some metrics can be per user based (user stats), per disk based, or per network interface based, etc.
	                          //this will be used to store the key to identify the user, or disk, or network interface
	                          //For this case, no subgroup is allowed.
	private String metricNameColumn;//column name for the metrics name
	private String metricValueColumn;//column name for the metrics value
	private String dbType = "MYSQL";
	//The sql used to retrieve the data, either a sql handler id for predefined sql, 
	//or full sqlText, but not both, should be specified. 
	private String sql;
	private String sqlText;
	private boolean storeInCommonTable = false;//store the metrics in a common shared table
							//better no to be used for subgroup
	
	private String targetTable;//to save into target metric table, with columns like (dbid, metric_id, ts, value)
	
	private MetricsGroup parentGroup;//Since we allow subGroups, each subGroup should have a way to access its parent group 
	private boolean udmFlagged;//flag this is user defined metrics group, not builtin
	
	public MetricsGroup()
	{
		this.metrics = new java.util.ArrayList<Metric>();
		this.metricsNameMap = new java.util.HashMap<String, Metric>();
	}
	public MetricsGroup(String groupName)
	{
		this.groupName = groupName;
		this.metrics = new java.util.ArrayList<Metric>();
		this.metricsNameMap = new java.util.HashMap<String, Metric>();
	}

	/**
	 * Note the returned list is a unmodified list
	 * @return
	 */
	public List<Metric> getMetrics() {
		return java.util.Collections.unmodifiableList(metrics);
	}

	public void addMetrics(Metric metric)
	{
		this.metrics.add(metric);
		this.metricsNameMap.put(metric.getName(), metric);
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}
	public MetricsGroup getSubGroupByName(String name)
	{
		for(MetricsGroup grp: this.subGroups)
		{
			if (grp.getGroupName().equals(name))
				return grp;
		}
		return null;
	}
	private int length;
	
	public void calculateLength()
	{
		int l = 0;
		for(Metric m: this.metrics)
		{
			l += m.getDataType().getLength();
		}
		this.length = l;
	}
	/**
	 * If the content ever changed, need re-calculate it first
	 * @return
	 */
	public int getLength()
	{
		return this.length;
	}

	public String getMetricNameColumn() {
		return metricNameColumn;
	}

	public void setMetricNameColumn(String metricNameColumn) {
		this.metricNameColumn = metricNameColumn;
	}

	public String getMetricValueColumn() {
		return metricValueColumn;
	}

	public void setMetricValueColumn(String metricValueColumn) {
		this.metricValueColumn = metricValueColumn;
	}

	public String getDbType() {
		return dbType;
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public boolean isStoreInCommonTable() {
		return storeInCommonTable;
	}

	public void setStoreInCommonTable(boolean storeInCommonTable) {
		this.storeInCommonTable = storeInCommonTable;
	}

	public String getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(String targetTable) {
		this.targetTable = targetTable;
	}

	public List<MetricsGroup> getSubGroups() {
		return subGroups;
	}

	public void addSubGroups(MetricsGroup subGroup) {
		if(subGroup == null)return;
		subGroup.setParentGroup(this);
		this.subGroups.add(subGroup);
	}

	public String getSqlText() {
		return sqlText;
	}

	public void setSqlText(String sqlText) {
		this.sqlText = sqlText;
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	public boolean isMultipleMetricsPerRow() {
		return multipleMetricsPerRow;
	}

	public void setMultipleMetricsPerRow(boolean multipleMetricsPerRow) {
		this.multipleMetricsPerRow = multipleMetricsPerRow;
	}

	public String getKeyColumn() {
		return keyColumn;
	}

	public void setKeyColumn(String keyColumn) {
		this.keyColumn = keyColumn;
	}

	public MetricsGroup getParentGroup() {
		return parentGroup;
	}

	public void setParentGroup(MetricsGroup parentGroup) {
		this.parentGroup = parentGroup;
	}
	public boolean isUdmFlagged() {
		return udmFlagged;
	}
	public void setUdmFlagged(boolean udmFlagged) {
		this.udmFlagged = udmFlagged;
	}
	
	/** 
	 * Table name of the metrics storage
	 * @return
	 */
	public String getSinkTableName()
	{
		if(this.isStoreInCommonTable())
		{
		  if(this.targetTable != null) return this.targetTable;
		  else return "METRIC_GENERIC";
		}
		else if(this.udmFlagged)
			return "UDM_" + this.groupName.toUpperCase();
		  return (this.dbType+"_"+this.groupName).toUpperCase();
	}

	public String getMetricFullName(String metricName)
	{
	  if(!this.metricsNameMap.containsKey(metricName))
		  return null;
	  String prefix = null;
	  if(this.udmFlagged)prefix = "UDM." + this.groupName;//udm
	  else if (this.parentGroup == null)prefix = this.groupName + ".";//top level
	  else prefix = this.parentGroup.getGroupName() + "." + this.groupName;
	  return prefix+"." + metricName;
	  
	}
	public Metric getMetricByName(String metricName)
	{
		return this.metricsNameMap.get(metricName);
	}
}
