/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.*;

/**
 * Terminology lookup
 * @author xrao
 *
 */
public class TermController extends MyPerfBaseController
{
  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
  {
	
	String[] names = req.getParameterValues("name");
	ModelAndView mv = new ModelAndView(jsonView);
	String res = "";
	for(String name: names)
	{
		if(name==null||name.isEmpty())continue;
		if(!name.startsWith("plan."))
		{			
			String desc = this.frameworkContext.getStatDefManager().getStatDescription(name);
			if(desc==null)desc = "no description found.";
			if(name!=null && name.startsWith("mysql_status"))
				name = name.substring(13);
			if(res.length()>0)
				res += "<br />";
			res += "<b>"+name+"</b>: "+desc;
		}else
		{
			String[] names2 = name.split(";");
			for(String name2: names2)
			{
				if(name2==null||"plan.extra.".equals(name2)||"plan.select_type.".equals(name2)
						||"plan.join_type.".equals(name2))continue;
				name2 = name2.trim();
				if(name.startsWith("plan.extra.") && !name2.startsWith("plan.extra."))
					name2 = "plan.extra."+name2;
				if(name2.indexOf('(')>=0)
					name2 = name2.substring(0, name2.indexOf('('));
				else if(name2.startsWith("plan.extra.Cost"))
					name2 = "plan.extra.Cost";
				String desc = this.frameworkContext.getStatDefManager().getStatDescription(name2);
				if(desc==null)desc = "no description found.";
				if(res.length()>0)
					res += "<br />";
				res += "<b>"+name2.substring(5)+"</b>: "+desc;
				
			}
		}
	}
	
	mv.addObject("json_result", ResultListUtil.toJSONString(null, null, 0, res));
	return mv;
  }
	
}
