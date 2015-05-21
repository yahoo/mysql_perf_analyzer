/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Handle all supported performance related sql statements.
 * @author xrao
 *
 */
public class SqlManager implements java.io.Serializable
{
  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(SqlManager.class.getName());  
  
  private XMLInputFactory inputFactory = XMLInputFactory.newInstance();//will be used only once at startup time

  //store all predefined sqls. the key is the handle.
  private java.util.Map<String, Sql> sqlMap = new java.util.TreeMap<String, Sql>();
  
  //for now, we use default path "sql.xml" under classes
  private String sqlPath = "sql.xml";
  
  public SqlManager(){}
  
  public java.util.Map<String, Sql> getSqlMap() 
  {
		return sqlMap;
  }

  public Sql getSql(String handle)
  {
    if(this.sqlMap.containsKey(handle))
      return this.sqlMap.get(handle);
	return null;
  }
	
  /**
   * Load all predefined sqls
   * @param in
   * @return
   * @throws XMLStreamException 
   */
  public boolean load(java.io.InputStream in) throws XMLStreamException
  {
    XMLStreamReader reader = null;
	try
	{
	  reader = inputFactory.createXMLStreamReader(new java.io.InputStreamReader(in));
	  while(reader.hasNext())
	  {
	    //loop till one sql tag is found
		int evtType = reader.next();
		if(evtType!=XMLStreamConstants.START_ELEMENT)continue;
		String tagName = reader.getLocalName();
		if(!"sql".equals(tagName))continue;
		int attrCount = reader.getAttributeCount();
		String name = null;
		String paramCount = null;
		String comments = null;
		boolean replace = false;
		boolean useSnapId = false;
		boolean useDbid = false;
		boolean expandRow = false;
		boolean errorInline = false;
		for(int i=0;i<attrCount;i++)
		{
		  String attrName = reader.getAttributeLocalName(i);
		  String attrValue = reader.getAttributeValue(i);
		  if("handle".equals(attrName))
			name = attrValue;
		  else if("paramCount".equals(attrName))
		    paramCount = attrValue;
		  else if("comments".equalsIgnoreCase(attrName))
		    comments = attrValue;
		  else if("replace".equalsIgnoreCase(attrName))
		  {
		    replace = "Y".equalsIgnoreCase(attrValue)||"TRUE".equalsIgnoreCase(attrValue);
		  }else if("useSnapId".equalsIgnoreCase(attrName))
		  {
		    useSnapId = "Y".equalsIgnoreCase(attrValue)||"TRUE".equalsIgnoreCase(attrValue);
		  }else if("useDbid".equalsIgnoreCase(attrName))
		  {
		    useDbid = "Y".equalsIgnoreCase(attrValue)||"TRUE".equalsIgnoreCase(attrValue);
		  }else if("expandRow".equalsIgnoreCase(attrName))
		  {
			  expandRow = "Y".equalsIgnoreCase(attrValue)||"TRUE".equalsIgnoreCase(attrValue);
		  }else if("errorInline".equalsIgnoreCase(attrName))
		  {
			  errorInline = "Y".equalsIgnoreCase(attrValue)||"TRUE".equalsIgnoreCase(attrValue);
		  }
			  
		}
		if(name!=null&&name.trim().length()>0)
		{
		  Sql sql = new Sql();
		  sql.setHandle(name);
		  sql.setComments(comments);
		  sql.setReplace(replace);
		  sql.setUseSnapId(useSnapId);
		  sql.setUseDbid(useDbid);
		  sql.setExpandRow(expandRow);
		  sql.setErrorInline(errorInline);
		  try
		  {
		    sql.setParamCount(Integer.parseInt(paramCount));
		  }catch(Exception ex)
		  {}
		  while(reader.hasNext())
		  {
		    int evtType2 = reader.next();
			if(evtType2==XMLStreamConstants.END_ELEMENT&&"sql".equals(reader.getLocalName()))break;
			if(evtType2!=XMLStreamConstants.START_ELEMENT)continue;						
			String tagName2 = reader.getLocalName();
			if("text".equals(tagName2))
			  sql.setSqlText(reader.getElementText());
			else if("class".equals(tagName2))
			  sql.setQueryClass(reader.getElementText());
			else if("queryProcessor".equals(tagName2))
				  sql.setQueryProcessor(reader.getElementText());
			else if("param".equals(tagName2))
			  sql.addParameter(reader.getAttributeValue(null, "name"), reader.getAttributeValue(null, "dataType"));
			else if("key".equalsIgnoreCase(tagName2))
			  sql.getKeyList().add(reader.getAttributeValue(null, "name").toUpperCase());
			else if("value".equalsIgnoreCase(tagName2))
			  sql.getValueList().add(reader.getAttributeValue(null, "name").toUpperCase());
			else if("flat".equalsIgnoreCase(tagName2))
			{
			  sql.setFlatKey(reader.getAttributeValue(null, "name").toUpperCase());
			  String flatValue = reader.getAttributeValue(null, "value");
			  try
			  {
			    for(String s:flatValue.split(","))
				{
				  sql.getFlatValueList().add(s.toUpperCase());
				}
			  }catch(Exception ex){}
			  flatValue = reader.getAttributeValue(null, "abbr");
			  try
			  {
			    String[] ss = flatValue.split(",");
				for(int i=0;i<ss.length;i++)
				  sql.getFlatValueAbbrMap().put(sql.getFlatValueList().get(i),ss[i]);
			  }catch(Exception ex){}
			}else if("metric".equalsIgnoreCase(tagName2))
			{
			  if(reader.getAttributeValue(null, "suffix")!=null)
			    sql.getMetrics().put(reader.getAttributeValue(null, "name").toUpperCase(), reader.getAttributeValue(null, "suffix"));
			  else sql.getMetrics().put(reader.getAttributeValue(null, "name").toUpperCase(),"");
			}else if("vsql".equalsIgnoreCase(tagName2))
			{
			  String minVersion = reader.getAttributeValue(null, "minVersion");
			  String maxVersion = reader.getAttributeValue(null, "maxVersion");
			  String vtext = reader.getElementText();
			  if(vtext!=null && vtext.trim().length()>0)
			  {
			    sql.addVSql(vtext, minVersion, maxVersion);
			  }
			}
		  }					
		  this.sqlMap.put(sql.getHandle(), sql);
		}
      }
    }
	finally
	{
	  if(reader!=null)try{reader.close(); reader=null;}catch(Exception iex){}
	}

	return false;
  }
	
  public boolean init()
  {
    java.io.InputStream in = null;
		
	try
	{
		in = this.getClass().getClassLoader().getResourceAsStream(sqlPath);
		this.load(in);
		logger.info("Load predefined sql: "+this.sqlMap.size()+"in "+(in==null)+", sqlPath="+this.sqlPath);
		return true;
	 }catch(Exception ex)
	 {
	   logger.log(Level.SEVERE,"Exception", ex);
	 }finally
	 {
	   if(in!=null)try{in.close();}catch(Exception ex){}
	 }
	 return false;
  }

  public String getSqlPath() 
  {
    return sqlPath;
  }

  public void setSqlPath(String sqlPath) 
  {
    this.sqlPath = sqlPath;
  }
}
