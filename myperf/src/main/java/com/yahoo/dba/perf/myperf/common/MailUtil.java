/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MailUtil {
	private static Logger logger = Logger.getLogger(MailUtil.class.getName());
	
	/**
	 * A simple mail client to use shell command to send mail
	 * @param receiver
	 * @param subject
	 * @param msg
	 * @return
	 */
	public static boolean sendMail(String receiver, String subject, String msg)
	{
		String mailCommand = "mailx";//or mail, which send long body as attachment
		
		String os = System.getProperty("os.name");
		if(os!=null && os.toUpperCase().contains("WIN"))return false;
		logger.info("Sending email to "+receiver+" regarding "+subject);
		String[] cmd = {mailCommand, "-s", subject, receiver};
		try
		{
			Process p = Runtime.getRuntime().exec(cmd);
			Writer w = new java.io.OutputStreamWriter(p.getOutputStream());
			w.append(msg);
			w.flush();
			w.close();
			p.waitFor();
			logger.info("Mail exitValue="+p.exitValue());
			return true;
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Error when send mail", ex);
		}
		return false;
	}

}
