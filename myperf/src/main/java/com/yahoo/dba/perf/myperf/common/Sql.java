/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.Map;

/**
 * Represent a predefined sql statement.
 * Predefined sql has a handle name and position based place holder like &p_nn, 
 * where nn starts from 1.
 * @author xrao
 *
 */
public class Sql implements java.io.Serializable
{
  private static final long serialVersionUID = 1L;

  //keep those for Oracle expansion
  public final static String GV_PREFIX="g_";
  public final static String GV_INST="&inst_id";
  public final static String DBID="&dbid";
  private boolean useDbid = false;//for AWR, if true, we need lookup dbid
  private boolean useSnapId;//for awr. When it is used, we have to have 
                            //begin_interval_time and end_interval_time in parameter list
  private boolean expandRow;//expand a single row to multiple columns
  private boolean errorInline = false;//if error and without result set, send response back with a column error
  
  private String handle;//name of the handle. Has to be unique
  private String sqlText;//sql text, for default
  private String queryClass;//query is generated from code. the java class name
  private String comments;//comments for the sql if any
  private boolean replace;//if true, use string replace rather than prepared statement. 
                          //Always for MySQL
  private int paramCount = 0;//total parameters, if any. Parameter will start from &p_1 to &p_{paramCount}
  //private java.util.List<String> paramNames = new java.util.ArrayList<String>();//comments or names for the parameters
  private java.util.List<SqlParameter> parameters = new java.util.ArrayList<SqlParameter>();//comments or names for the parameters
  private String queryProcessor;//customized executor

  //When requested metrics are contained in rows, flat function will transpose it to rows.
  private java.util.List<String> keyList = new java.util.ArrayList<String>();//used for flat rows to columns
  private java.util.List<String> valueList = new java.util.ArrayList<String>();//used for flat rows to columns
  private String flatKey;//allow to flat a single key
  private java.util.List<String> flatValueList = new java.util.ArrayList<String>();//create new columns with those value
  private Map<String, String> flatValueAbbrMap = new java.util.LinkedHashMap<String, String>();//create new columns with those value
  private java.util.LinkedHashMap<String, String> metrics = new java.util.LinkedHashMap<String, String>();

  //SQL specific to a version
  private java.util.ArrayList<VSql> vsqls = new java.util.ArrayList<VSql>();
	
  /**
   * SQL with restriction on version
   * @author xrao
   *
   */
  public static class VSql
  {
    private String text;
	private String maxVersion;
	private String minVersion;
		
	public VSql(String text, String minVersion, String maxVersion)
	{
	  this.setText(text);
	  this.setMaxVersion(maxVersion);
	  this.setMinVersion(minVersion);			
	}

	public String getText() 
	{
	  return text;
	}

	public void setText(String text) 
	{
	  this.text = text;
	}

	public String getMaxVersion() 
	{
	  return maxVersion;
	}

	public void setMaxVersion(String maxVersion) 
	{
	  this.maxVersion = maxVersion;
	}

	public String getMinVersion() 
	{
	  return minVersion;
	}

	public void setMinVersion(String minVersion) 
	{
	  this.minVersion = minVersion;
	}				
  }
  
  public Sql(){}

  public String getHandle() 
  {
    return handle;
  }

  public void setHandle(String handle) 
  {
    this.handle = handle;
  }

  public String getSqlText() 
  {
    return sqlText;
  }

  public void setSqlText(String sqlText) 
  {
    this.sqlText = sqlText;
  }

  public String getSqlText(String version)
  {
    if(version==null)return this.getSqlText();
		
	for(VSql s: this.vsqls)
	{
	  if(s.getMinVersion()!=null && version.compareTo(s.getMinVersion())<0)
	  {
		continue;
	  }
	  if(s.getMaxVersion()!=null && version.compareTo(s.getMaxVersion())>0)continue;
	  return s.getText();
	}
		return this.getSqlText();		
  }
	
  public void addVSql(String text, String minVersion, String maxVersion)
  {
    this.vsqls.add(new VSql(text, minVersion,maxVersion));
  }

  public int getParamCount() 
  {
    return paramCount;
  }

  public void setParamCount(int paramCount) 
  {
    this.paramCount = paramCount;
  }

  public java.util.List<SqlParameter> getParameters() 
  {
    return this.parameters;
  }

  /**
   * based on array index, not placeholder index
   * @param i
   * @return
   */
  public SqlParameter getSqlParameter(int index)
  {
	  if(this.parameters.size()>index)
		  return this.parameters.get(index);
	  return null;
  }
  
  /**
   * 1 based index
   * @param name
   * @return
   */
  public int getParamIndex(String name)
  {
    for(int i=0;i<this.parameters.size();i++)
	{
	  if(name.equalsIgnoreCase(this.parameters.get(i).getName()))return i+1;
	}
	return -1;
  }
  
  public void addParameter(String name, String valueType)
  {
	  this.parameters.add(new SqlParameter(name, valueType));
  }
	
  public String getComments() 
  {
	return comments;
  }

  public void setComments(String comments) 
  {
    this.comments = comments;
  }

  public boolean isReplace() 
  {
    return replace;
  }

  public void setReplace(boolean replace) 
  {
    this.replace = replace;
  }

  public java.util.List<String> getKeyList() 
  {
    return keyList;
  }

  public String getFlatKey() 
  {
    return flatKey;
  }

  public void setFlatKey(String flatKey) 
  {
    this.flatKey = flatKey;
  }

  public java.util.List<String> getFlatValueList() 
  {
    return flatValueList;
  }

  public java.util.LinkedHashMap<String, String> getMetrics() 
  {
    return metrics;
  }

  public Map<String, String> getFlatValueAbbrMap() 
  {
    return flatValueAbbrMap;
  }

  public void setFlatValueAbbrMap(Map<String, String> flatValueAbbrMap) 
  {
    this.flatValueAbbrMap = flatValueAbbrMap;
  }

  public boolean isUseSnapId() 
  {
    return useSnapId;
  }

  public void setUseSnapId(boolean useSnapId) 
  {
    this.useSnapId = useSnapId;
  }

  public String getQueryClass() 
  {
	  return queryClass;
  }

  public void setQueryClass(String queryClass) 
  {
    this.queryClass = queryClass;
  }

  public java.util.List<String> getValueList() 
  {
    return valueList;
  }

  public boolean isUseDbid() 
  {
    return useDbid;
  }

  public void setUseDbid(boolean useDbid) 
  {
    this.useDbid = useDbid;
  }

public boolean isExpandRow() {
	return expandRow;
}

public void setExpandRow(boolean expandRow) {
	this.expandRow = expandRow;
}

public boolean isErrorInline() {
	return errorInline;
}

public void setErrorInline(boolean errorInline) {
	this.errorInline = errorInline;
}

public String getQueryProcessor() {
	return queryProcessor;
}

public void setQueryProcessor(String queryProcessor) {
	this.queryProcessor = queryProcessor;
}
}
