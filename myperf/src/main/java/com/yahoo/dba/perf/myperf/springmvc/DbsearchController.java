/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;

public class DbsearchController  extends MyPerfBaseController
{
  //private String jsonView;
  //private MyPerfContext frameworkContext;
					
  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
							HttpServletResponse resp) throws Exception 
  {
    int status = 0;
	String message = "OK";
	ResultList rList = new ResultList();
	{
	  String keyword = req.getParameter("keyword");
	  List<DBInstanceInfo> dbList = this.frameworkContext.getDbInfoManager().SearchDbInfo(keyword);
							
	  ColumnDescriptor desc = new ColumnDescriptor();
	  rList.setColumnDescriptor(desc);
	  int idx = 0;
	  desc.addColumn("DBTYPE", false, idx++);
	  desc.addColumn("DBGROUPNAME", false, idx++);
	  desc.addColumn("HOSTNAME", false, idx++);
	  desc.addColumn("PORT", false, idx++);
	  desc.addColumn("DATABASENAME", false, idx++);

	  for(DBInstanceInfo urp:dbList)
	  {
	    ResultRow row = new ResultRow();
		List<String> cols = new java.util.ArrayList<String>(8);
		row.setColumnDescriptor(desc);
		row.setColumns(cols);
		cols.add(urp.getDbType());
	    cols.add(urp.getDbGroupName());
		cols.add(urp.getHostName());
		cols.add(urp.getPort());
		cols.add(urp.getDatabaseName());
		rList.addRow(row);
	  }
	}
	
	ModelAndView mv = new ModelAndView(this.jsonView);
	mv.addObject("json_result", ResultListUtil.toJSONString(rList, null, status, message));
	return mv;
  }
				
}
