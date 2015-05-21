/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class HelpController extends AbstractController{

		@Override
		protected ModelAndView handleRequestInternal(HttpServletRequest req,
				HttpServletResponse resp) throws Exception {
			
			String key = req.getParameter("key");
			if(key==null||key.trim().length()==0)key = "about";
			ModelAndView mv = new ModelAndView("help/"+key);
			return mv;
			
		}
}
