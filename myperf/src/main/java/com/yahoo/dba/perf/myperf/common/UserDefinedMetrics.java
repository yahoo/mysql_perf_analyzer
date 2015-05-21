/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;


/**
 * User defined metrics
 * @author xrao
 *
 */
public class UserDefinedMetrics implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(UserDefinedMetrics.class.getName());
	public static class UserDefinedMetricsException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public UserDefinedMetricsException()
		{
			super();
		}
		public UserDefinedMetricsException(String msg)
		{
			super(msg);
		}
	}
	private String name; //UDM name
	private boolean auto;  //auto collect
	private String source; //how metrics are gathered, hook up to predefined, or customized sql
	private String udmType; //column, row, key
	private String nameCol; //for multi row type, column name for metric name 
	private String valueCol; //for multi row type, column name for metric value 
	private String keyCol; //forl muti row, multi metrics per row, column name to extract entity key
	private String sql; //sql statement for customized sql
	private boolean storeInCommonTable = false; //whether we want to store metrics in a common table
								//The advantage is we can add new metrics into the same group
								//The disadvantage is the storage overhead.
	
	private List<Metric> metrics = new ArrayList<Metric>();
	
	private MetricsGroup metricsGroup = new MetricsGroup();//wrap the content into a MetricsGroup
		
	public UserDefinedMetrics()
	{
		metricsGroup.setUdmFlagged(true);
	}
	public UserDefinedMetrics(String name)
	{
		if(name != null)
		{
			this.name = name.trim().toUpperCase();
			this.metricsGroup.setGroupName(this.name);
		}
		metricsGroup.setUdmFlagged(true);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		if(name != null)
		{
			this.name = name.trim().toUpperCase();
			this.metricsGroup.setGroupName(this.name);
		}
	}

	public MetricsGroup getMetricsGroup() {
		return metricsGroup;
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean auto) {
		this.auto = auto;
		this.metricsGroup.setAuto(this.auto);
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
		//TODO
	}
	public String getUdmType() {
		return udmType;
	}
	public void setUdmType(String udmType) {
		this.udmType = udmType;
		if(!"row".equals(this.udmType))
			this.metricsGroup.setMultipleMetricsPerRow(true);
	}
	public String getNameCol() {
		return nameCol;
	}
	public void setNameCol(String nameCol) {
		this.nameCol = nameCol;
		this.metricsGroup.setMetricNameColumn(this.nameCol);
	}
	public String getValueCol() {
		return valueCol;
	}
	public void setValueCol(String valueCol) {
		this.valueCol = valueCol;
		this.metricsGroup.setMetricValueColumn(this.valueCol);
	}
	public String getKeyCol() {
		return keyCol;
	}
	
	/**
	 * This should be invoked after setUdmType
	 * @param keyCol
	 */
	public void setKeyCol(String keyCol) {
		this.keyCol = keyCol;
		if("key".equals(this.udmType))
			this.metricsGroup.setKeyColumn(this.keyCol);
			
	}
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {		
		this.sql = sql;
		if("SQL".equals(this.source))
			this.metricsGroup.setSqlText(this.sql);
	}
	public List<Metric> getMetrics() {
		return metrics;
	}
	public void setMetrics(List<Metric> metrics) {
		this.metrics = metrics;
	}

	public void addMetric(Metric m)
	{
		if(m!=null)
		{
			this.metrics.add(m);
			this.metricsGroup.addMetrics(m.copy());
		}
		
	}
	public void addmetric(String name, String sourceName, boolean inc, MetricDataType dataType)
	{
		if(name != null && !name.isEmpty() )
		{
			Metric m = new Metric(name.trim().toUpperCase(), sourceName, dataType, null, null, inc);
			this.metrics.add(m);
			this.metricsGroup.addMetrics(m.copy());
		}
	}
	
	/**
	 * If invalid, exception will be thrown with error message
	 * @throws UserDefinedMetricsException
	 */
	public void validate() throws UserDefinedMetricsException
	{
		StringBuilder sb = new StringBuilder();
		boolean valid = true;
		if(!Metric.isAlphaNumericUnderscoreNotNull(name, 20))
		{
			valid = false;
			sb.append("udm name "+name+" is not valid. Please use alphanumeric and underscore with max length of 20. ");
		}
		if("SQL".equalsIgnoreCase(source))
		{
			if(sql == null || sql.isEmpty())
			{
				sb.append("Please provide customized SQL. ");
				valid = false;
			}
			else
			{
				sql = sql.trim();
				if(!sql.toLowerCase().startsWith("select") || sql.indexOf(';')>=0)
				{
					sb.append("The SQL statement must start with select and no semi column is allowed. ");
					valid = false;
				}
			}
		}
		if("key".equalsIgnoreCase(this.udmType) && (keyCol == null || keyCol.isEmpty()))
		{
			valid = false;
			sb.append("For case of Multiple Rows, Multiple Metrics Per Row, With a Key Column, please provide a valid key column name. ");
		}
		if("row".equalsIgnoreCase(this.udmType) && (nameCol == null || nameCol.isEmpty() || valueCol.isEmpty() || valueCol.isEmpty()))
		{
			valid = false;
			sb.append("For case of Multiple Rows, One Row One Metric, Key Value Pairs, please provide column names to extract metric names and values.");
		}
		for(Metric m: this.metrics)
		{
			
			if(!Metric.isAlphaNumericUnderscoreNotNull(m.getName(), 32))
			{
				valid = false;
				sb.append("udm metric name "+m.getName()+" is not valid. Please use alphanumeric and underscore with max length of 32. ");
			}
			
		}
		if(!valid)
		{
			throw new UserDefinedMetricsException(sb.toString());
		}
	}
	
	public String toJSON(boolean html)
	{
		StringBuilder mtrStr = new StringBuilder();
		mtrStr.append("{\r\n");
		mtrStr.append("\"groupName\": \"").append(name.toUpperCase()).append("\",\r\n");
		mtrStr.append("\"auto\": \"").append(auto?"y":"n").append("\",\r\n");
		mtrStr.append("\"source\": \"").append(source).append("\",\r\n");
		mtrStr.append("\"type\": \"").append(udmType).append("\",\r\n");
		mtrStr.append("\"storeInCommonTable\": \"").append(this.storeInCommonTable?"y":"n").append("\",\r\n");
		if(this.nameCol!=null && !this.nameCol.isEmpty())mtrStr.append("\"nameColumn\": \"").append(nameCol).append("\",\r\n");
		if(this.valueCol!=null && !this.valueCol.isEmpty())mtrStr.append("\"valueColumn\": \"").append(valueCol).append("\",\r\n");
		if(this.keyCol!=null && !this.keyCol.isEmpty())mtrStr.append("\"keyColumn\": \"").append(keyCol).append("\",\r\n");
		if(this.sql != null && !this.sql.isEmpty())mtrStr.append("\"sql\": \"")
		       .append(html?escapeJsonHTML(sql):escapeJson(sql)).append("\",\r\n");
		mtrStr.append("\"metrics\":[");
		int cnt = this.metrics.size();
		for(int i=0;i<cnt; i++)
		{
			Metric m = metrics.get(i);
			if(i>0)mtrStr.append(",\r\n");
			mtrStr.append("{\"name\": \"").append(m.getName()).append("\",\"sourceName\":\"")
			      .append(m.getSourceName()!=null && !m.getSourceName().isEmpty() ?m.getSourceName():m.getName())
			      .append("\", \"inc\": \"").append(m.isIncremental()?"y":"n").append("\", \"dataType\": \"")
			      .append(m.getDataType()).append("\"}");
		}
        mtrStr.append("]}");
		return mtrStr.toString();
	}
	
	public static String escapeJsonHTML(String str)
	{
		if(str==null)return "";
		StringBuilder sb = new StringBuilder(str.length());
		char[] carray = str.toCharArray();
		for(int i=0;i<carray.length;i++)
		{  
			char c = carray[i];
			if(c=='\"')
				sb.append("\\u0022");
			else if(c=='\n')
				sb.append("\\u000A");
			else if(c=='\\')
				sb.append("\\u005C");
			else if(c=='\r')
				sb.append("\\u000D");
			else if(c=='\t')
				sb.append("\\u0009");
			//else if(c=='/')
			//	sb.append("\\u002F");
			else if(c=='\0'||c<' ')//edw2p has osuser set as two chars (24, 20)
			{
				//skip it
			}
			else sb.append(c);
		}
		return sb.toString();
		
	}

	public static String escapeJson(String str)
	{
		if(str==null)return "";
		StringBuilder sb = new StringBuilder(str.length());
		char[] carray = str.toCharArray();
		for(int i=0;i<carray.length;i++)
		{  
			char c = carray[i];
			if(c=='\"')
				sb.append("\\\"");
			else if(c=='\n')
				sb.append("\\n");
			else if(c=='\r')
				sb.append("\\r");
			else if(c=='\t')
				sb.append("\\t");
			//else if(c=='/')
			//	sb.append("\\u002F");
			else if(c=='\0'||c<' ')
			{
				//skip it
			}
			else sb.append(c);
		}
		return sb.toString();		
	}

	public static UserDefinedMetrics createFromJson(java.io.InputStream in)
	{
		JsonReader jsonReader = null;
		UserDefinedMetrics udm = null;
		try
		{
			jsonReader = javax.json.Json.createReader(in);
			JsonObject jsonObject = jsonReader.readObject();
			jsonReader.close();
			udm = new UserDefinedMetrics(jsonObject.getString("groupName"));
			udm.setAuto("y".equalsIgnoreCase(jsonObject.getString("auto", null)));
			udm.setStoreInCommonTable("y".equalsIgnoreCase(jsonObject.getString("storeInCommonTable", null)));
			udm.setSource(jsonObject.getString("source"));
			udm.setUdmType(jsonObject.getString("type"));
			udm.setNameCol(jsonObject.getString("nameColumn", null));
			udm.setValueCol(jsonObject.getString("valueColumn", null));
			udm.setKeyCol(jsonObject.getString("keyColumn", null));
			udm.setSql(jsonObject.getString("sql", null));
			
			JsonArray metrics = jsonObject.getJsonArray("metrics");
			if(metrics != null )
			{
				int mlen = metrics.size();
				for(int i=0; i<mlen; i++)
				{
					JsonObject mobj = metrics.getJsonObject(i);
					udm.addmetric(mobj.getString("name"), 
							mobj.getString("sourceName"), 
							"y".equalsIgnoreCase(mobj.getString("inc")),
							Metric.strToMetricDataType(mobj.getString("dataType")));
				}
			}
		}catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error to parse UDM", ex);
			//TODO
		}
		return udm;
	}
	public boolean isStoreInCommonTable() {
		return storeInCommonTable;
	}
	public void setStoreInCommonTable(boolean storeInCommonTable) {
		this.storeInCommonTable = storeInCommonTable;
		this.metricsGroup.setStoreInCommonTable(storeInCommonTable);
	}
}
