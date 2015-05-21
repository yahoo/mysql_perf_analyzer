/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.yahoo.dba.perf.myperf.common.UserDefinedMetrics.UserDefinedMetricsException;


/**
 * Customized alert definitions, used by alert scanner.
 * @author xrao
 *
 */
public class AlertDefinition implements java.io.Serializable{
	private static final long serialVersionUID = 7023147712745750436L;
	private static Logger logger = Logger.getLogger(AlertDefinition.class.getName());
	
	public static final String SOURCE_SQL =  "SQL"; //need a sql statement or build in sql
	public static final String SOURCE_GLOBAL_STATUS =  "GLOBAL_STATUS";//from global_status
	public static final String SOURCE_METRICS =  "METRICS";//from metrics def scanned
	public static final String SOURCE_OTHER =  "OTHER";//not defined yet, or future use
	public static final String METRICS_COMPARISION_GREATER_THAN = "GT"; //alert if exceed threshold
	public static final String METRICS_COMPARISION_LESS_THAN = "LT";//alert of below threshold
	public static final String METRICS_VALUE_TYPE_VALUE = "LAST_VALUE";//use value
	public static final String METRICS_VALUE_TYPE_DIFF_AVG = "DIFF_AVG";//use per second change
	public static final String METRICS_VALUE_TYPE_DIFF = "DIFF";//use changes, for example, just detect if something happened
	
	private String name; //an all upper case name, refer to table ALERT, column ALERT_TYPE 
	                     //for length, currently defined as VARCHAR(30), Alphanumerics only
						 //Cannot be an existing name, including built in ones
	private String source = SOURCE_SQL;
	private String sqlId;//if source is SQL, we need either a sqlId for builtin SQLs in sql.xml
						 //or sqlText
	private String sqlText;
	
	private String metricName; //if type is metrics, the full name of the metrics, group.subgroup.metricname
	private float defaultThreshold; //if type is metrics, the default threshold
	private String metricComparison = METRICS_COMPARISION_GREATER_THAN;
	private String metricValueType = METRICS_VALUE_TYPE_VALUE;//use last value, or changes per second

	private Map<String, String> params = new HashMap<String, String>();
	
	public AlertDefinition(String name)
	{
		this.name = name != null?name.trim() : null;
	}

	public String getName() {
		return name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		if(SOURCE_SQL.equalsIgnoreCase(source))
			this.source = SOURCE_SQL;
		else if(SOURCE_METRICS.equalsIgnoreCase(source))
			this.source = SOURCE_METRICS;
		else if(SOURCE_GLOBAL_STATUS.equalsIgnoreCase(source))
			this.source = SOURCE_GLOBAL_STATUS;
		else this.source = SOURCE_OTHER;
	}

	public String getSqlId() {
		return sqlId;
	}

	public void setSqlId(String sqlId) {
		this.sqlId = sqlId;
	}

	public String getSqlText() {
		return sqlText;
	}

	public void setSqlText(String sqlText) {
		this.sqlText = sqlText;
	}

	public float getDefaultThreshold() {
		return defaultThreshold;
	}

	public void setDefaultThreshold(float defaultThreshold) {
		this.defaultThreshold = defaultThreshold;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public String getMetricComparison() {
		return metricComparison;
	}

	public void setMetricComparison(String metricComparison) {
		this.metricComparison = metricComparison;
	}

	public String getMetricValueType() {
		return metricValueType;
	}

	public void setMetricValueType(String metricValueType) {
		this.metricValueType = metricValueType;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void addParam(String name, String defaultValue) {
		this.params.put(name, defaultValue);
	}

	public static AlertDefinition createFromJson(java.io.InputStream in)
	{
		JsonReader jsonReader = null;
		AlertDefinition alert = null;
		try
		{
			jsonReader = javax.json.Json.createReader(in);
			JsonObject jsonObject = jsonReader.readObject();
			jsonReader.close();
			alert = new AlertDefinition(jsonObject.getString("name"));
			alert.setSource(jsonObject.getString("source"));
			alert.setMetricName(jsonObject.getString("metricName", null));
			alert.setMetricValueType(jsonObject.getString("metricValueType", null));
			alert.setMetricComparison(jsonObject.getString("metricComparison", null));
			try{
				alert.setDefaultThreshold(Float.parseFloat(jsonObject.getString("defaultThreshold", null)));
			}catch(Exception ex){}
			
			alert.setSqlId(jsonObject.getString("sqlId", null));
			alert.setSqlText(jsonObject.getString("sqlText", null));
			
			JsonArray params = jsonObject.getJsonArray("params");
			if(params != null )
			{
				int mlen = params.size();
				for(int i=0; i<mlen; i++)
				{
					JsonObject mobj = params.getJsonObject(i);
					alert.addParam(mobj.getString("name"), 
							mobj.getString("defaultValue"));
				}
			}
			return alert;
		}catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error to parse UDM", ex);
		}
		return null;
	}

	public String toJSON(boolean html)
	{
		StringBuilder mtrStr = new StringBuilder();
		mtrStr.append("{\r\n");
		mtrStr.append("\"name\": \"").append(name).append("\",\r\n");
		mtrStr.append("\"source\": \"").append(source).append("\",\r\n");
		if(this.metricName!=null && !this.metricName.isEmpty())
		{
			mtrStr.append("\"metricName\": \"").append(metricName).append("\",\r\n");
			if(this.metricComparison!=null && !this.metricComparison.isEmpty())mtrStr.append("\"metricComparison\": \"").append(metricComparison).append("\",\r\n");
			if(this.metricValueType!=null && !this.metricValueType.isEmpty())mtrStr.append("\"metricValueType\": \"").append(metricValueType).append("\",\r\n");
			mtrStr.append("\"defaultThreshold\": \"").append(defaultThreshold).append("\",\r\n");
		}
		if(this.sqlId!=null && !this.sqlId.isEmpty())mtrStr.append("\"sqlId\": \"").append(sqlId).append("\",\r\n");
		if(this.sqlText != null && !this.sqlText.isEmpty())mtrStr.append("\"sqlText\": \"")
		       .append(html?CommonUtils.escapeJsonHTML(sqlText):CommonUtils.escapeJson(sqlText)).append("\",\r\n");
		mtrStr.append("\"params\":[");
		int cnt = 0;
		for(Map.Entry<String, String> e: this.params.entrySet())
		{
			if(cnt>0)mtrStr.append(",\r\n");
			mtrStr.append("{\"name\": \"").append(e.getKey()).append("\",\"defaultValue\":\"")
			      .append(e.getValue())
			      .append("\"}");
		}
        mtrStr.append("]}");
		return mtrStr.toString();
	}
	

	public void validate() throws UserDefinedMetricsException{
		StringBuilder sb = new StringBuilder();
		boolean valid = true;
		if(!Metric.isAlphaNumericUnderscoreNotNull(name, 30))
		{
			valid = false;
			sb.append("ALERT name "+name+" is not valid. Please use alphanumeric and underscore with max length of 30. ");
		}
		if("SQL".equalsIgnoreCase(source))
		{
			if((sqlText == null || sqlText.isEmpty()) && (sqlId == null || sqlId.isEmpty()))
			{
				sb.append("Please provide SQL text for customized SQL. ");
				valid = false;
			}
			else
			{
				sqlText = sqlText.trim();
				if(!sqlText.toLowerCase().startsWith("select") || sqlText.indexOf(';')>=0)
				{
					sb.append("The SQL statement must start with select and no semi column is allowed. ");
					valid = false;
				}
			}
		}
		for(Map.Entry<String, String> e: this.params.entrySet())
		{
			
			if(!Metric.isAlphaNumericUnderscoreNotNull(e.getKey(), 0))
			{
				valid = false;
				sb.append("Alert param name "+e.getKey()+" is not valid. Please use alphanumeric and underscore.");
			}
			//TODO value
		}
		if(!valid)
		{
			throw new UserDefinedMetricsException(sb.toString());
		}

	}

}
