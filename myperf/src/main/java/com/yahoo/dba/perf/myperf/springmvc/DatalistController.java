/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;

/**
 * Used by UI to retrieve data lists
 * @author xrao
 *
 */
public class DatalistController extends MyPerfBaseController
{
  private String xmlView;
  private String jsonView;
  
  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception {
	String dataType = req.getParameter("t");
    String contentType = req.getParameter("ct");
    ModelAndView mv = null;
    if("json".equalsIgnoreCase(contentType))
    {
      resp.setContentType("application/json");
	  mv = new ModelAndView(jsonView);
    }
    else 
    {
      resp.setContentType("text/xml");
	  mv = new ModelAndView(xmlView);
    }
	ResultList rs = null;
	if("dbhosts".equalsIgnoreCase(dataType))
	  rs = this.retrieveDbHostList(req);
	else if("dbinfo".equalsIgnoreCase(dataType))
	  rs = this.retrieveDbHostInfo(req);
	else if("list_restricted".equalsIgnoreCase(dataType))
		  rs = this.listRestricted(req);		
	if("json".equalsIgnoreCase(contentType))
	{
	  mv.addObject("json_result", ResultListUtil.toJSONString(rs, null, 0, "OK"));
	}else
	{
	  if(rs==null||rs.getRows().size()==0)
	    mv.addObject("status", 0);
	  else
	  {
	    mv.addObject("status", rs.getRows().size());
		mv.addObject("results", rs);
	  }
	}
	return mv;
  }

  public String getXmlView() 
  {
    return xmlView;
  }

  public void setXmlView(String successView) 
  {
    this.xmlView = successView;
  }


  public String getJsonView() 
  {
    return jsonView;
  }

  public void setJsonView(String jsonView) 
  {
    this.jsonView = jsonView;
  }

  private ResultList retrieveDbHostList(HttpServletRequest req)
  {
    String clusterName = req.getParameter("dbid");
	DBGroupInfo cls = this.frameworkContext.getDbInfoManager().findGroup(clusterName);
	if(cls!=null)
	{
	  ResultList rs = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  desc.setColumns(new java.util.ArrayList<ColumnInfo>(1));
	  ColumnInfo col = new ColumnInfo("host");
	  col.setPosition(1);
	  desc.getColumns().add(col);
	  rs.setColumnDescriptor(desc);
	  for(DBInstanceInfo db:cls.getInstances())
	  {
	    ResultRow row = new ResultRow();
		row.setColumnDescriptor(desc);
		row.setColumns(new java.util.ArrayList<String>(1));
		row.getColumns().add(db.getHostName());
		rs.addRow(row);
	  }
	  return rs;
	}
	else
	  return null;
  }

  private ResultList retrieveDbHostInfo(HttpServletRequest req)
  {
    String clusterName = req.getParameter("dbid");
	String hostName = req.getParameter("host");
	DBInstanceInfo db = this.frameworkContext.getDbInfoManager().findDB(clusterName, hostName);
	if(db!=null)
	{
	  ResultList rs = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  desc.setColumns(new java.util.ArrayList<ColumnInfo>(1));
	  ColumnInfo col = new ColumnInfo("host");
	  col.setPosition(1);
	  desc.getColumns().add(col);
	  col = new ColumnInfo("port");
	  col.setPosition(2);
	  desc.getColumns().add(col);
	  col = new ColumnInfo("sid");
	  col.setPosition(3);
	  desc.getColumns().add(col);
	  col = new ColumnInfo("useTunneling");
	  col.setPosition(4);
	  desc.getColumns().add(col);
	  col = new ColumnInfo("localHostName");
	  col.setPosition(5);
	  desc.getColumns().add(col);
	  col = new ColumnInfo("localPort");
	  col.setPosition(6);
	  desc.getColumns().add(col);
	  col = new ColumnInfo("instance");
	  col.setPosition(7);
	  desc.getColumns().add(col);
				
	  rs.setColumnDescriptor(desc);
	  ResultRow row = new ResultRow();
	  row.setColumnDescriptor(desc);
	  row.setColumns(new java.util.ArrayList<String>(1));
	  row.getColumns().add(db.getHostName());
	  row.getColumns().add(String.valueOf(db.getPort()));
	  row.getColumns().add(db.getDatabaseName());
	  row.getColumns().add(db.isUseTunneling()?"1":"0");
	  row.getColumns().add(db.getLocalHostName());
	  row.getColumns().add(db.getLocalPort());
	  row.getColumns().add(String.valueOf(db.getInstance()));
				
	  rs.addRow(row);
	  return rs;
	}
	else
	  return null;
  }
  private ResultList listRestricted(HttpServletRequest req)
  {
    String name = req.getParameter("name");
	ResultList rs = new ResultList();
	ColumnDescriptor desc = new ColumnDescriptor();
	desc.addColumn("DBGROUP", false, 1);
	rs.setColumnDescriptor(desc);
	Set<String> mydbs = this.frameworkContext.getDbInfoManager().getMyDatabases(name, true).getMyDbList();
	
	if(mydbs == null)return rs;
	for(String s: mydbs)
	{
	  ResultRow row = new ResultRow();
	  row.setColumnDescriptor(desc);
  	  row.addColumn(s);			
	  rs.addRow(row);
	}
	return rs;
  }
}
