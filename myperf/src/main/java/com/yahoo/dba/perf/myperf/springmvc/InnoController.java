/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.springmvc;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.yahoo.dba.perf.myperf.common.ColumnDescriptor;
import com.yahoo.dba.perf.myperf.common.Constants;
import com.yahoo.dba.perf.myperf.common.DBInstanceInfo;
import com.yahoo.dba.perf.myperf.common.DBUtils;
import com.yahoo.dba.perf.myperf.common.QueryParameters;
import com.yahoo.dba.perf.myperf.common.ResultList;
import com.yahoo.dba.perf.myperf.common.ResultListUtil;
import com.yahoo.dba.perf.myperf.common.ResultRow;
import com.yahoo.dba.perf.myperf.db.DBConnectionWrapper;

public class InnoController extends MyPerfBaseController
{
  private static Logger logger = Logger.getLogger(InnoController.class.getName());
		
  @Override
  protected ModelAndView handleRequestImpl(HttpServletRequest req,
				HttpServletResponse resp) throws Exception 
  {
	int status = Constants.STATUS_OK;
	String message = "OK";

	String group = req.getParameter("group");
	String host = req.getParameter("host");
	QueryParameters qps = new QueryParameters();
    qps.setGroup(group);
    qps.setHost(host);
    qps.setSql("mysql_innodb_engine_status");
    
	ResultList rList = null;
	LinkedHashMap<String, ResultList> listMap = new LinkedHashMap<String, ResultList>();
	DBInstanceInfo dbinfo = null;
	DBConnectionWrapper connWrapper = null;
	try
	{
	  dbinfo = this.frameworkContext.getDbInfoManager().findDB(group,host).copy();
	  connWrapper = WebAppUtil.getDBConnection(req, this.frameworkContext, dbinfo);
      if(connWrapper==null)
	  {
	    status = Constants.STATUS_BAD;
	    message = "failed to connect to target db ("+dbinfo+")";
	  }else
	  {
		rList = this.frameworkContext.getQueryEngine().executeQueryGeneric(qps, connWrapper, qps.getMaxRows());
	    logger.info("Done query "+qps.getSql() + " with "+(rList!=null?rList.getRows().size():0)+" records.");
	    if(rList!=null && rList.getRows().size()>0)
	    {
	      logger.info(rList.getRows().get(0).getColumns().get(rList.getRows().get(0).getColumns().size()-1));
	      listMap = parse(rList.getRows().get(0).getColumns().get(rList.getRows().get(0).getColumns().size()-1));
	    }
		WebAppUtil.closeDBConnection(req, connWrapper, false, this.getFrameworkContext().getMyperfConfig().isReuseMonUserConnction());		  
	  }
    }catch(Throwable th)
	{
  	  logger.log(Level.SEVERE,"Exception", th);
  	  if(th instanceof SQLException)
  	  {
  	    SQLException sqlEx = SQLException.class.cast(th);
  		String msg = th.getMessage();
  		logger.info(sqlEx.getSQLState()+", "+sqlEx.getErrorCode()+", "+msg);
  		//check if the connection is still good
  		if(!DBUtils.checkConnection(connWrapper.getConnection()))
  		{
  		  WebAppUtil.closeDBConnection(req, connWrapper, true, false);
  		}
  		else
  		  WebAppUtil.closeDBConnection(req, connWrapper, true, false);
  	  }else
  	  {
  	    if(connWrapper!=null)WebAppUtil.closeDBConnection(req, connWrapper, false,this.getFrameworkContext().getMyperfConfig().isReuseMonUserConnction());
  	  }
  	  status = Constants.STATUS_BAD;
  	  message = "Exception: "+th.getMessage();
	}
    finally
	{
	}
	

	ModelAndView mv = new ModelAndView(this.jsonView);
	if(req.getParameter("callback")!=null&&req.getParameter("callback").trim().length()>0)
	   mv.addObject("callback", req.getParameter("callback"));//YUI datasource binding
	mv = new ModelAndView(this.jsonView);
	    mv.addObject("json_result", ResultListUtil.toMultiListJSONStringUpper(listMap, qps, status, message));
	return mv;			
  }

  
  private LinkedHashMap<String, ResultList> parse(String str)
  {
	final int BEFORE_STARTLINE = -1;
	final int STARTLINE = 0;
	final int AFTER_STARTLINE = 1;
	final int SECTION_HEADER_STARTLINE = 2;
	final int SECTION_HEADER_ENDLINE = 3;
	int state = BEFORE_STARTLINE;
	String currentSection = null;
	LinkedHashMap<String, ResultList> listMap = new LinkedHashMap<String, ResultList>();
    java.io.StringReader reader = new java.io.StringReader(str);
    java.io.BufferedReader bufReader = new java.io.BufferedReader(reader);
    String line = null;
    
    Map<String, String> valMap = new java.util.LinkedHashMap<String, String>();
    List<Transaction> txList = new ArrayList<Transaction>();
    List<SemaphoreEntry> semapList = new ArrayList<SemaphoreEntry>();
    List<String> deadLockInfo = new ArrayList<String>();//TODO not parsing the content for now
    Transaction curTx = null;
    SemaphoreEntry curSemap = null;
    try
    {
      while( (line = bufReader.readLine())!=null)
      {
        switch(state)
        {
        case BEFORE_STARTLINE:
              if(line.startsWith("==="))
            	state = STARTLINE;  
              break;
        case STARTLINE:
            if(line.startsWith("==="))
           	    state = AFTER_STARTLINE;  
            else
            {
              int idx = line.indexOf(" INNODB");
              if(idx>0)
              {
                valMap.put("Time", line.substring(0, idx).trim());
              }
            }
            break;
        case AFTER_STARTLINE:
        	if(line.startsWith("-"))
        		state = SECTION_HEADER_STARTLINE;
        	else 
        	{
        	  if(line.startsWith("Per second averages"))
        	  {
                valMap.put("Duration", line.substring(line.indexOf("last")+5).trim());
        	  }
        	}
        	break;
        case SECTION_HEADER_STARTLINE:
        	if(line.startsWith("-"))
        	{
        		state = SECTION_HEADER_ENDLINE;
        	}
        	else
        	{
        	  currentSection = line.trim();	
        	}
        	break;
        case SECTION_HEADER_ENDLINE:
        	if(line.startsWith("-") && !line.startsWith("---TRANSACTION") && !line.startsWith("--Thread") &&!(line.startsWith("-------")&&line.contains("TRX")) &&!"------------------".equals(line))
        	{
        		state = SECTION_HEADER_STARTLINE;	
        		//store previous section
        		if("BACKGROUND THREAD".equalsIgnoreCase(currentSection))
        		{
        		  //dump valMap	
        		  listMap.put("inno_status_summary",createHeader(valMap));
        		  valMap.clear();
        		}else if("FILE I/O".equalsIgnoreCase(currentSection))
        		{
          		  //dump valMap	
          		  listMap.put("inno_status_file_io",createHeader(valMap));
          		  valMap.clear();
          		}else if("BUFFER POOL AND MEMORY".equalsIgnoreCase(currentSection))
        		{
            	  //dump valMap	
            	  listMap.put("inno_status_buffer_pool",createHeader(valMap));
            	  valMap.clear();
            	}else if("ROW OPERATIONS".equalsIgnoreCase(currentSection))
        		{
              	  //dump valMap	
              	  listMap.put("inno_status_row_operations",createHeader(valMap));
              	  valMap.clear();
              	}else if("SEMAPHORES".equalsIgnoreCase(currentSection))
        		{
                  //dump valMap	
                  listMap.put("inno_status_semaphores",createHeader(valMap));
                  valMap.clear();
            	  logger.info("Processing semaphore list: "+semapList.size());
                  listMap.put("inno_status_semap",buildSemapList(semapList));
                  txList.clear();
               }else if("LOG".equalsIgnoreCase(currentSection))
       		   {
                   //dump valMap	
                   listMap.put("inno_status_log",createHeader(valMap));
                   valMap.clear();
                }
               else if("INSERT BUFFER AND ADAPTIVE HASH INDEX".equalsIgnoreCase(currentSection))
       		   {
                   //dump valMap	
                   listMap.put("inno_status_ibuf",createHeader(valMap));
                   valMap.clear();
                }
               else if("LATEST DETECTED DEADLOCK".equalsIgnoreCase(currentSection))
       		   {
            	   logger.info("Processing deadlocks: " + deadLockInfo.size());
            	   listMap.put("inno_status_deadlocks", buildDeadlockList(deadLockInfo));
                }
               else if("TRANSACTIONS".equalsIgnoreCase(currentSection))
       		   {
            	  if(curTx!=null)txList.add(curTx);
            	  curTx = null;
            	  logger.info("Processing tx: "+txList.size());
                  listMap.put("inno_status_txs",buildTransactionList(txList));
                  txList.clear();
                }
        	}
        	else if("BACKGROUND THREAD".equalsIgnoreCase(currentSection))
        	{
        		String vals[] = line.split(":");
        		if(vals.length>1)
        		{
        		  valMap.put(vals[0], vals[1]);	
        		}
        	}else if("LATEST DETECTED DEADLOCK".equalsIgnoreCase(currentSection))
        	{
        		deadLockInfo.add(line);
          	}
        	else if("FILE I/O".equalsIgnoreCase(currentSection))
        	{
        	  this.parseFileIO(valMap, line);	
        	}else if("BUFFER POOL AND MEMORY".equalsIgnoreCase(currentSection))
    		{
    		  this.parseBufferPool(valMap, line);
    		}else if("ROW OPERATIONS".equalsIgnoreCase(currentSection))
    		{
      		  this.parseRowOperation(valMap, line);
      		}else if("SEMAPHORES".equalsIgnoreCase(currentSection))
    		{
      		  boolean parsed = false;
      		  if(line!=null && line.startsWith("--Thread "))
      		  {
      			  //start a new semaphore
      			  curSemap = new SemaphoreEntry();
      			  semapList.add(curSemap);
      		  }
      		  if(curSemap!=null)
      			  parsed = curSemap.parse(line);
      		  if(!parsed)
      			  this.parseSemaphore(valMap, line);
        	}else if("LOG".equalsIgnoreCase(currentSection))
    		{
          	  this.parseLog(valMap, line);
          	}else if("INSERT BUFFER AND ADAPTIVE HASH INDEX".equalsIgnoreCase(currentSection))
    		{
            	  this.parseIBuf(valMap, line);
            }
          	else if("TRANSACTIONS".equalsIgnoreCase(currentSection))
    		{
        	  //TODO skip summary portion
        	  if(curTx==null && !line.startsWith("---TRANSACTION"))
        		  break;
        	  if(line.startsWith("---TRANSACTION"))
        	  {
        		if(curTx!=null)
        		  txList.add(curTx);
        	    curTx = new Transaction();
        	  }
        	  curTx.parseLine(line);
          	}
        	break;
         default:
        	 break;
        }
      }
    }catch(Exception iex)
    {
      logger.log(Level.INFO, "Error during parsing", iex);
    }
    return listMap;
  }
  
  private ResultList buildSemapList(List<SemaphoreEntry> semapList) 
  {
	  ResultList rList = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  int idx = 1;
	  desc.addColumn("THREAD_ID", false, idx++);
	  desc.addColumn("LOCK_TYPE", false, idx++);
	  desc.addColumn("LOCK_NAME", false, idx++);
	  desc.addColumn("MODE", false, idx++);
	  desc.addColumn("LOCATION", false, idx++);
	  desc.addColumn("TIME_SEC", false, idx++);
	  desc.addColumn("HOLDER", false, idx++);
	  desc.addColumn("HODL_MODE", false, idx++);
	  desc.addColumn("WAITED_AT", false, idx++);

	  rList.setColumnDescriptor(desc);
	  for(SemaphoreEntry tx: semapList)
	  {
		if(tx.thread_id==null)continue;
	    ResultRow row = new ResultRow();
	    rList.addRow(row);
	    row.setColumnDescriptor(desc);
	    List<String> cols = new ArrayList<String>(16);
	    row.setColumns(cols);
	    cols.add(tx.thread_id);
	    cols.add(tx.lock_type);
	    cols.add(tx.lock_name);
	    cols.add(tx.request_mode);
	    cols.add(tx.lock_loc);
	    cols.add(tx.waited_time);
	    cols.add(tx.lock_holder);
	    cols.add(tx.hold_mode);
	    cols.add(tx.waited_at);
	  }
	  return rList;
  }

  private ResultList createHeader(Map<String, String> valMap)
  {
    ResultList rList = new ResultList();
    ColumnDescriptor desc = new ColumnDescriptor();
    desc.addColumn("NAME", false, 1);
    desc.addColumn("VALUE", false, 2);
    rList.setColumnDescriptor(desc);
    for(Map.Entry<String, String> e: valMap.entrySet())
    {
      ResultRow row = new ResultRow();
      List<String> vals = new ArrayList<String>(2);
      vals.add(e.getKey());
      vals.add(e.getValue());
      row.setColumnDescriptor(desc);
      row.setColumns(vals);
      rList.addRow(row);
    }
    return rList;
  }
  
  private void parseFileIO(Map<String, String> valMap, String str)
  {
	try
	{
      if(str==null||str.isEmpty())return;
      str = str.trim();
      if(str.startsWith("I/O thread "))
      {
		String vals[] = str.split(":");
		if(vals.length>1)
		{
		  valMap.put(vals[0], vals[1]);	
		}
      }else if(str.charAt(0)>='0'&&str.charAt(0)<='9')
      {
        String[] vals = str.split(",");
        for(String v: vals)
        {
          String v2 = v.trim();
          int idx = v2.indexOf(' ');
          if(idx>0)
          {
            valMap.put(v2.substring(idx+1).trim(),v2.substring(0, idx).trim());
          }
        }
      }else if(str.indexOf(',')>=0)
      {
    	  String[] strs = str.split(",");
    	  for(String s: strs)
    	  {
    		  String[] res = this.splitSpaceDelemitNameValuePair(s);
    		  if(res!=null)
    		  {
    			  String name = res[0];
    			  if(name.indexOf(":")>=0)name = name.substring(0, name.indexOf(":"));
    			  valMap.put(name, res[1]);
    		  }
    	  }
      }
    }catch(Exception ex){}
  }
  private void parseRowOperation(Map<String, String> valMap, String str)
  {
	try
	{
      if(str==null||str.isEmpty())return;
      str = str.trim();
      if(str.indexOf("queries inside InnoDB")>=0||str.indexOf("read views")>=0)
      {
		String vals[] = str.split(",");
		for(String v: vals)
		{
		  v = v.trim();
		  int idx = v.indexOf(' ');
		  if(idx>0)
			  valMap.put(v.substring(idx+1),v.substring(0,idx));
		}
      }else if(str.indexOf("Main thread")>=0)
      {
        String[] vals = str.split(",");
        for(String v: vals)
        {
          String v2 = v.trim();
          int idx = v2.lastIndexOf(' ');
          if(idx>0)
          {
        	String k = v2.substring(0, idx).trim();
        	if(!k.startsWith("Main thread"))
        	  k = "Main thread "+k;	
            valMap.put(k,v2.substring(idx+1).trim());
          }
        }
      }else if(str.indexOf("Number of rows")>=0)
      {
        String[] vals = str.split(",");
        for(String v: vals)
        {
          String v2 = v.trim();
          int idx = v2.lastIndexOf(' ');
          if(idx>0)
          {
        	String  k = v2.substring(0, idx).trim();
        	if(!k.startsWith("Number of rows"))
        		k = "Number of rows "+k;
            valMap.put(k,v2.substring(idx+1).trim());
          }
        }        	  
      }else if(str.charAt(0)>='0'&&str.charAt(0)<='9')
      {
        String[] vals = str.split(",");
        for(String v: vals)
        {
          String v2 = v.trim();
          int idx = v2.indexOf(' ');
          if(idx>0)
          {
          	String  k = v2.substring(idx+1).trim();
            valMap.put(k,v2.substring(0, idx).trim());
          }
        }        	  
        	  
      }else
      {
        logger.warning("Data not parsed: "+str);  	  
      }
    }catch(Exception ex){}
  }
  private void parseSemaphore(Map<String, String> valMap, String str)
  {
	try
	{
      if(str==null||str.isEmpty())return;
      str = str.trim();      
      if(str.startsWith("OS WAIT ARRAY INFO: "))
      {
    	String str2 = str.substring(str.indexOf(':')+1).trim();
		String vals[] = str2.split(",");
		for(String v: vals)
		{
		  v = v.trim();
		  int idx = v.lastIndexOf(' ');
		  if(idx>0)
			  valMap.put("OS WAIT ARRAY INFO: "+v.substring(0,idx), v.substring(idx+1));
		}
      }else if(str.startsWith("Mutex"))
      {
    	String str2 = str.substring(6).trim();
		String vals[] = str2.split(",");
		for(String v: vals)
		{
		  v = v.trim();
		  int idx = v.lastIndexOf(' ');
		  if(idx>0)
			  valMap.put("Mutex "+v.substring(0,idx), v.substring(idx+1));
		}
      }else if(str.startsWith("Spin rounds per wait:"))
      {
    	String str2 = str.substring(str.indexOf(':')+1).trim();
		String vals[] = str2.split(",");
		for(String v: vals)
		{
		  v = v.trim();
		  int idx = v.indexOf(' ');
		  if(idx>0)
			  valMap.put("Spin rounds per wait: "+v.substring(idx+1),v.substring(0,idx));
		}
      }else if(str.indexOf("RW-shared spins")>=0||str.indexOf("RW-excl spins")>=0)
      {
		String vals[] = str.split(";");
		for(String v: vals)
		{
		  v = v.trim();
		  String[] vals2 = v.split(",");
		  for(String v2: vals2)
		  {
		    int idx = v2.lastIndexOf(' ');
			if(idx>0)
			{
			  String k = v2.substring(0, idx);
			  if(v.indexOf("RW-shared")>=0 && k.indexOf("RW-shared")<0)
			    k = "RW-shared "+k;
			  else if(v.indexOf("RW-excl")>=0 && k.indexOf("RW-excl")<0)
				    k = "RW-excl "+k;				  
			  valMap.put(k,v2.substring(idx+1));
		    }
		  }
		}
      }else if(str.startsWith("RW-sx"))
      {
    	String str2 = str.substring(str.indexOf(' ')+1).trim();
		String vals[] = str2.split(",");
		for(String v: vals)
		{
		  v = v.trim();
		  int idx = v.lastIndexOf(' ');
		  if(idx>0)
			  valMap.put("RW-sx "+v.substring(0,idx), v.substring(idx+1));
		}
      }
      else
      {
        logger.warning("Data not parsed: "+str);  	  
      }
    }catch(Exception ex){}
  }

  /**
   * Split a space delimit name value pair, with name followed by value. The name could have space too.
   * @param str
   * @return 2 element array. The first is name, the second is value
   */
  private String[] splitSpaceDelemitNameValuePair(String str)
  {
	  if(str==null||str.isEmpty())return null;
	  String str2 = str.trim();
	  int idx = str2.lastIndexOf(' ');//since we trimmed, it cannot be the last one
	  if(idx<=0)return null;
	  String name = str2.substring(0, idx);
	  String val = str2.substring(idx+1);
	  if(name.isEmpty()||val.isEmpty())return null;
	  name = name.trim();
	  try
	  {
		  //check if we can parse
		  BigDecimal bdecimal = new BigDecimal(val); 
		  if(bdecimal!=null)
		  {
			  String[] res = new String[2];
			  res[0] = name;
			  res[1] = val;
			  return res;
		  }
	  }catch(Exception ex)
	  {
		  
	  }
	  return null;
  }

  private String[] splitSpaceDelemitValueNamePair(String str)
  {
	  if(str==null||str.isEmpty())return null;
	  String str2 = str.trim();
	  int idx = str2.indexOf(' ');//since we trimmed, it cannot be the last one
	  if(idx<=0)return null;
	  String val = str2.substring(0, idx);
	  String name = str2.substring(idx+1);
	  if(name.isEmpty()||val.isEmpty())return null;
	  name = name.trim();
	  try
	  {
		  //check if we can parse
		  BigDecimal bdecimal = new BigDecimal(val); 
		  if(bdecimal!=null)
		  {
			  String[] res = new String[2];
			  res[0] = name;
			  res[1] = val;
			  return res;
		  }
	  }catch(Exception ex)
	  {
		  
	  }
	  return null;
  }

  private void parseLog(Map<String, String> valMap, String str)
  {
	try
	{
      if(str==null||str.isEmpty())return;
      str = str.trim();      
      if(str.indexOf(',')<0)//not comma separated line, so name value pair
      {
    	  String[] res = splitSpaceDelemitNameValuePair(str);
    	  if(res!=null)
    		  valMap.put(res[0], res[1]);  
      }else //comma separated, each substring has value name pair
      {
    	  String[] strs = str.split(",");
    	  for(String s: strs)
    	  {
        	  String[] res = splitSpaceDelemitValueNamePair(s);
        	  if(res!=null)
        		  valMap.put(res[0], res[1]);      		  
    	  }
      }
      
    }catch(Exception ex){}
  }
  
  private void parseIBuf(Map<String, String> valMap, String str)
  {
	try
	{
      if(str==null||str.isEmpty())return;
      str = str.trim();
      if(str.startsWith("Ibuf: "))
      {
    	  String str2 = str.substring(5).trim();
    	  String[] strs = str2.split(",");
    	  for(String s: strs)
    	  {
        	  String[] res = splitSpaceDelemitNameValuePair(s);
        	  if(res!=null)
        		  valMap.put("Ibuf "+res[0], res[1]);      		  
    	  }

      }else if(Character.isDigit(str.charAt(0)))//value name pair
      {
    	  String[] strs = str.split(",");
    	  for(String s: strs)
    	  {
        	  String[] res = splitSpaceDelemitValueNamePair(s);
        	  if(res!=null)
        		  valMap.put(res[0], res[1]);      		  
    	  }    	  
      }else if(str.startsWith("Hash table"))
      {
    	  String[] strs = str.split(",");
    	  for(String s: strs)
    	  {
    		  if(s!=null)s = s.trim();
    		  else continue;
    		  if(s.startsWith("Hash table"))
    		  {
    			  String[] res = splitSpaceDelemitNameValuePair(s);
    			  if(res!=null)
            		  valMap.put(res[0], res[1]); 
    		  }else if(s.startsWith("node heap"))
    		  {
    			  String[] ss = s.split(" ");
    			  if(ss!=null && ss.length>3)
    				  valMap.put("node heap buffer(s)", ss[3]);
    		  }
    	  }
      }
      
    }catch(Exception ex){}
  }

  private static String[] BUFFER_INTERNALS = new String[]{"Adaptive hash index","Page hash","Dictionary cache", "File system", "Lock system", "Recovery system"};
  private void parseBufferPool(Map<String, String> valMap, String str)
  {
	try
	{
      if(str==null||str.isEmpty())return;
      String str2 = str.trim();
      if(str2.startsWith("Total memory"))
      {
		String vals[] = str2.split(";");
		for(String v: vals)
		{
		  String v2 = v.trim();
		  int idx = v2.lastIndexOf(' ');
		  if(idx>0)
			  valMap.put(v2.substring(0, idx), v2.substring(idx+1));
		}
      }else if(str.startsWith("Internal hash"))//Skil sub header
      {
      }
      else if(str.startsWith("  ")||str.indexOf('(')>0)//indented      
      {
    	for(String hts: BUFFER_INTERNALS)
    	{
    		if(str.indexOf(hts)>=0)
    		{
    			str2 = str.substring( str.indexOf(hts)+hts.length()).trim();
    			int idx = str2.lastIndexOf(')');
    			if(idx>=0)valMap.put("Internal hash tables - "+hts, str2.substring(0, idx+1));    		
    		}
    	}
      }else if(str2.startsWith("Dictionary memory allocated")
    		  ||str2.startsWith("Buffer pool size")
    		  ||str2.startsWith("Free buffers")            
    		  ||str2.startsWith("Database pages")
    		  ||str2.startsWith("Old database pages")
    		  ||str2.startsWith("Modified db pages")
    		  ||str2.startsWith("Pending reads"))
      {
        int idx = str2.lastIndexOf(' ');
        if(idx>=0)
        {
          valMap.put(str2.substring(0, idx), str2.substring(idx+1));	
        }
      }else if(str2.startsWith("Pending writes"))
      {
    	String str3 = str2.substring(str2.indexOf(':')+1).trim();
  		String vals[] = str3.split(",");
  		for(String v: vals)
  		{
  		  v = v.trim();
  		  if(v.indexOf("flush list")>=0 && v.indexOf("single page")>=0)
  		  {
  		    	  
  		   valMap.put("flush list", v.substring(v.indexOf("flush list")+11, v.indexOf("single page")-1)); 	  
  		   valMap.put("single page", v.substring(v.lastIndexOf(' ')+1)); 	  
  		  }else
  		  {
    		  int idx = v.lastIndexOf(' ');
	        if(idx>=0)
	      	  valMap.put("Pending writes: "+ v.substring(0, idx),v.substring(idx+1));
  		  }
  		}
      }
      else if(str2.indexOf(',')>=0)
      {
  		String vals[] = str2.split(",");
  		for(String v: vals)
  		{
  		  v = v.trim();
  		  if(v.charAt(0)>='0' && v.charAt(0)<='9')
  		  {
  	        int idx = v.lastIndexOf(' ');
  	        if(idx>=0)
  	        {
  	          String k = v.substring(idx+1);
  	          k = "Pages "+k;
  	          valMap.put(k,v.substring(0, idx));	
  	        }
  		  }else if(v.indexOf(':')>=0)
  		  {
    	    int idx = v.lastIndexOf(':');
      	    if(idx>=0)
      	    {
      	      valMap.put(v.substring(0, idx),v.substring(idx+1));
      	    }
  		  }else if(v.startsWith("Buffer pool hit rate"))
  		  {
  			  valMap.put("Buffer pool hit rate", v.substring(21));
  		  }else if(v.startsWith("young-making rate"))
  		  {
  			  int idx = v.lastIndexOf("not");
  			  valMap.put("young-making rate", v.substring(18, idx));
  			  
  		  }else
  		  {
      	    int idx = v.lastIndexOf(' ');
      	    if(idx>=0)
      	    {
      	      String k = v.substring(0, idx);
      	      if(str.startsWith("Pages") && !k.startsWith("Pages"))
      	        k = "Pages "+k;
      	      valMap.put(k,v.substring(idx+1));
      	    }  			  
  		  }
  		}
      }else
      {
        logger.warning("Data not parsed: "+str2);
      }
    }catch(Exception ex){}
  }
  
  private static ResultList buildTransactionList(List<Transaction> txs)
  {
	  ResultList rList = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  int idx = 1;
	  desc.addColumn("ID", false, idx++);
	  desc.addColumn("STATE", false, idx++);
	  desc.addColumn("TIME", false, idx++);
	  desc.addColumn("PROCESS", false, idx++);
	  desc.addColumn("THREAD", false, idx++);
	  desc.addColumn("USER", false, idx++);
	  desc.addColumn("HOST", false, idx++);
	  desc.addColumn("QUERY_ID", false, idx++);
	  //desc.addColumn("ACTION", false, idx++);
	  desc.addColumn("SQL_STATE", false, idx++);
	  desc.addColumn("SQL", false, idx++);
	  desc.addColumn("LOCKS", false, idx++);
	  //TODO lock strcuts
	  rList.setColumnDescriptor(desc);
	  for(Transaction tx: txs)
	  {
		if("not started".equalsIgnoreCase(tx.state) && (tx.sql==null || tx.sql.isEmpty()))
			  continue;//ignore idle one
	    ResultRow row = new ResultRow();
	    rList.addRow(row);
	    row.setColumnDescriptor(desc);
	    List<String> cols = new ArrayList<String>(16);
	    row.setColumns(cols);
	    cols.add(tx.id);
	    cols.add(tx.state);
	    cols.add(tx.txTime);
	    cols.add(tx.processNumber);
	    cols.add(tx.threadId);
	    cols.add(tx.user);
	    cols.add(tx.host);
	    cols.add(tx.queryId);
	    //cols.add(tx.action);
	    cols.add(tx.cmd);
	    cols.add(tx.sql);
	    if(tx.action!=null && !tx.action.isEmpty() && tx.comments!=null)
	    	cols.add(tx.action+"\n"+tx.comments);
	    else if(tx.action!=null && !tx.action.isEmpty() && tx.comments==null)
	    	cols.add(tx.action);
	    else
	    	cols.add(tx.comments);
	  }
	  return rList;
  }

  private static ResultList buildDeadlockList(List<String> infoList)
  {
	  ResultList rList = new ResultList();
	  ColumnDescriptor desc = new ColumnDescriptor();
	  int idx = 1;
	  desc.addColumn("INFO", false, idx++);
	  //TODO lock strcuts
	  rList.setColumnDescriptor(desc);
	  for(String tx: infoList)
	  {
	    ResultRow row = new ResultRow();
	    rList.addRow(row);
	    row.setColumnDescriptor(desc);
	    List<String> cols = new ArrayList<String>(1);
	    row.setColumns(cols);
	    cols.add(tx);
	  }
	  return rList;
  }

  /**
   * Used to parse transaction record
   * @author xrao
   *
   */
  private static class Transaction
  {
    String id;//TX ID
    String state;//transaction status: ACTIVE or not started
    String txTime; 
    String processNumber;
    String action;
    String lockStructs;//number of lock structs
    String lockStructsStatus;//number of lock structs
    String undoLogEntries;
    String heapSize;
    String threadId;
    String queryId;
    String host;
    String user;
    String cmd;
    String sql;
    String comments;
    
    static final int TX_BEFORE = -1;
    static final int TX_START =0;
    static final int TX_ACTION = 1;
    static final int TX_THREAD = 2;
    static final int TX_SQL_START = 3;
    static final int TX_COMMENT = 4;
    private int txstate = -1;
    Transaction()
    {
    }
    
    void parseLine(String str)
    {
      if((str.startsWith("------") && str.contains("TRX"))
    		  ||str.startsWith("Trx read view will not see trx with id")
    		  ||str.startsWith("TABLE LOCK table ")
    		  ||str.startsWith("RECORD LOCK space "))
        txstate = TX_COMMENT;
      else if(str.startsWith("MySQL thread id"))
    	txstate = TX_THREAD;
      try
      {
      switch(txstate)
      {
      case TX_BEFORE:
    	  if(str.startsWith("---TRANSACTION "))
    	  {
    	    txstate = TX_START;
    	    id = str.substring(str.indexOf(' ')+1, str.indexOf(','));
    	    String str2 = str.substring(str.indexOf(',')+1).trim();
    	    if(str2.startsWith("not started"))
    	    {
    	      state = "not started";
    	      if(str2.indexOf(',')>=0)
    	    	  str2 = str2.substring(str2.indexOf(',')+1).trim();  
    	      else str2 = null;
    	    }
    	    else if(str2.startsWith("ACTIVE"))
    	    {
    	      state = "ACTIVE";
    	      if(str2.indexOf(',')>0)
    	      {
    	    	txTime = str2.substring(str2.indexOf(' ')+1, str2.indexOf(','));  
    	    	str2 = str2.substring(str2.indexOf(',')+1).trim();
    	      }
    	      else
    	      {  
    	    	int idx1 = str2.indexOf(' ');
    	    	int idx2 = str2.indexOf(' ', idx1+1);
    	    	if(idx2>0)
    	    		idx2 = str2.indexOf(' ', idx2+1);
    	    	if(idx2>0)
    	    	{
    	    	  txTime = str2.substring(idx1+1, idx2);
    	    	  str2 = str2.substring(idx2).trim();
    	    	}
    	    	else
    	    	{
    	    	  txTime = str2.substring(idx1+1);
    	    	  str2 = null;
    	    	}
    	      }
    	    }else
    	    {
      	      if(str2.indexOf(',')>=0)
      	      {
    	    	state = str2.substring(0, str2.indexOf(',')).trim();
    	    	str2.substring(str2.indexOf(',')+1).trim();  
      	      }else
      	      {
      	    	state = str2;
     	        str2 = null;
      	      }
    	    }
    	    if(str2!=null)
    	    {
    	      if(str2.startsWith("process "))
    	      {
    	    	if(str2.indexOf(',')>0)
    	    		str2 = str2.substring(0, str2.indexOf(','));
    	        this.processNumber = str2.substring(str2.lastIndexOf(' ')+1);
    	      }
    	      else 
    	    	this.action = str2;
    	    }
    	  }
    	  break;
      case TX_START:
    	  this.action = str.trim();
    	  txstate = TX_ACTION;
    	  break;
      case TX_ACTION:
    	  //TODO
    	  txstate = TX_THREAD;
    	  break;
      case TX_THREAD:
    	  String str2 = str.substring(0, str.indexOf(','));
    	  this.threadId = str2.substring(str2.lastIndexOf(' '));
    	  str2 = str.substring(str.indexOf("query id")).trim();
    	  this.queryId = str2.substring(8, str2.indexOf(' ',10));
    	  str2 = str2.substring(str2.indexOf(' ', 10)+1).trim();
    	  if(str2.startsWith("Slave "))
    	  {
        	 txstate = TX_SQL_START;
        	 this.cmd = str2;
    		 break; 
    	  }
    	  else if(str2.startsWith("update")||str2.startsWith("Updating")||str2.startsWith("init"))
    	  {
        	 txstate = TX_SQL_START;
        	 this.cmd = str2;
    		 break; 
    	  }
    	  try
    	  {
    		  this.host = str2.substring(0,str2.indexOf(' '));
    		  str2 = str2.substring(str2.indexOf(' ')+1).trim();
    		  if(str2.charAt(0)>='0'&&str2.charAt(0)<='9' && str2.indexOf('.')>=0)
    		  {
    			  //ip address
    			  this.host += " "+str2.substring(0, str2.indexOf(' '));
    			  str2 = str2.substring(str2.indexOf(' ')+1).trim();    		
    		  }
    		  if(str2.indexOf(' ')>0)
    		  {
    			  this.user = str2.substring(0, str2.indexOf(' '));
    			  this.cmd = str2.substring(str2.indexOf(' ')+1);
    		  }else
    			  this.user = str2;
    	  }catch(Exception iex)
    	  {
    		  logger.log(Level.INFO, str2+", "+str, iex);
    	  }
    	  txstate = TX_SQL_START;
    	  break;
      case TX_SQL_START:
    	  if(this.sql==null)
    	    this.sql = str;
    	  else
    		this.sql += "\n"+str;
          break;
      case TX_COMMENT:
    	  if(this.comments==null)
      	    this.comments = str;
      	  else
      		this.comments += "\n"+str;
            break;          
      default:
    	  break;
      }
      }catch(Exception ex)
      {
        logger.log(Level.INFO, str, ex);	  
      }
    }
  }
  
  public static class SemaphoreEntry
  {
	  String thread_id;
	  String waited_at;
	  String waited_time;
	  String request_mode;
	  String lock_type;
	  String lock_name;
	  String lock_loc;
	  String lock_holder;
	  String hold_mode;
	  String lock_var;
	  String lock_word;
	  String readers;
	  String waiters_flag;
	  String last_read_locked;
	  String last_write_locked;

	  public SemaphoreEntry()
	  {
		  
	  }
	  
	  public boolean parse(String line)
	  {
		  //--Thread 140455387199232 has waited at btr/btr0cur.c line 501 for 0.0000 seconds the semaphore:
		  if(line==null||line.isEmpty())return false;
		  if(this.thread_id==null && !line.startsWith("--Thread "))return false;
		  try
		  {
		  if(line.startsWith("--Thread "))
		  {
			  Pattern pt = Pattern.compile("\\-\\-Thread\\s+(\\d+)\\s+has\\s+waited\\s+at\\s+(.+)\\s+for\\s+(.+)\\s+seconds\\s+the\\s+semaphore:");
			  Matcher mt = pt.matcher(line);
			  if(mt.find())
			  {
				  this.thread_id = mt.group(1);
				  this.waited_at = mt.group(2);
				  this.waited_time = mt.group(3);
			  }else
			  {
				  logger.info("Failed to parse semaphore line: "+line);
			  }			  
		  }else if(this.lock_name==null)
		  {
			  //S-lock on RW-latch at 0x7fc0e40a46f0 '&new_index->lock'
			  //Mutex at 0x7fc43c785ee0 '&ibuf_mutex', lock var 1
			  if(line.startsWith("Mutex "))
			  {
				  this.lock_type = "Mutex";
				  Pattern pt = Pattern.compile("Mutex\\s+at\\s+(0x[a-z0-9]+)\\s+'(.+)'\\s*,\\s*lock\\s+var\\s+(\\d+)");
				  Matcher mt = pt.matcher(line);
				  if(mt.find())
				  {
					  this.lock_loc = mt.group(1);
					  this.lock_name = mt.group(2);
					  this.lock_var = mt.group(3);
				  }else
				  {
					  logger.info("Failed to parse semaphore Mutex line: "+line);
				  }
				  
			  }else
			  {
				  Pattern pt = Pattern.compile("([A-Z]+)\\-lock\\s+on\\s+([a-zA-Z\\-]+)\\s+at\\s+(0x[a-z0-9]+)\\s+'(.+)'");
				  Matcher mt = pt.matcher(line);
				  if(mt.find())
				  {
					  this.request_mode = mt.group(1);
					  this.lock_type = mt.group(2);
					  this.lock_loc = mt.group(3);
					  this.lock_name = mt.group(4);
				  }else
				  {
					  logger.info("Failed to parse semaphore lock line: "+line);
				  }				  
			  }
		  }else if(line.contains("thread id") && line.contains("mode"))
		  {//a writer (thread id 139924314879744) has reserved it in mode  exclusive
			  Pattern pt = Pattern.compile("thread\\s+id\\s+(\\d+).+mode\\s+(.+)");
			  Matcher mt = pt.matcher(line);
			  if(mt.find())
			  {
				  this.lock_holder = mt.group(1);
				  this.hold_mode = mt.group(2);
			  }else
			  {
				  logger.info("Failed to parse semaphore holder line: "+line);
			  }				  			  
		  }else
			  return false;
		  //ignore other line for now
		  }catch(Exception ex)
		  {
			  logger.log(Level.INFO, "Failed to parse semophore: "+line, ex);
		  }
		  return true;//we parsed it
	  }
  }
}
