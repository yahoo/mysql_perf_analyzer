/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * handle reports for a single user
 * @author xrao
 *
 */
public class UserReportManager {
	private static Logger logger = Logger.getLogger(UserReportManager.class.getName());
	public static class UserReport
	{
		public static final char fieldSeparator = '\u0001';
		public static final char pairSeparator = '\u0002';
		public static final char kvSeparator = '\u0003';
		
		private int id;//report id, user specific
		private String type;//report type
		private String dbname;//db/cluster name
		private String dbhost;//db hostname, default to all
		private String created_timestamp;//yyyyMMddHHmmssSSS in java
		private String completed_timestamp;//yyyyMMddHHmmssSSS in java
		private String format;//html or text
		private String filename;//file name will be type dependent
		private java.util.LinkedHashMap<String, String> parameters = new java.util.LinkedHashMap<String, String>();
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getCreated_timestamp() {
			return created_timestamp;
		}
		public void setCreated_timestamp(String created_timestamp) {
			this.created_timestamp = created_timestamp;
		}
		public String getFormat() {
			return format;
		}
		public void setFormat(String format) {
			this.format = format;
		}
		public String getFilename() {
			return filename;
		}
		public void setFilename(String filename) {
			this.filename = filename;
		}
		public String getCompleted_timestamp() {
			return completed_timestamp;
		}
		public void setCompleted_timestamp(String completed_timestamp) {
			this.completed_timestamp = completed_timestamp;
		}
		public java.util.LinkedHashMap<String, String> getParameters() {
			return parameters;
		}
		public void addParameter(String name, String value)
		{
			this.parameters.put(name, value);
		}
		public String getDbname() {
			return dbname;
		}
		public void setDbname(String dbname) {
			this.dbname = dbname;
		}
		public String getDbhost() {
			return dbhost;
		}
		public void setDbhost(String dbhost) {
			this.dbhost = dbhost;
		}
		public String encodeLine()
		{
			StringBuilder sb = new StringBuilder();
			sb.append(this.id).append(fieldSeparator);
			sb.append(this.type).append(fieldSeparator);
			sb.append(this.dbname).append(fieldSeparator);
			sb.append(this.dbhost).append(fieldSeparator);
			sb.append(this.created_timestamp).append(fieldSeparator);
			sb.append(this.format).append(fieldSeparator);
			sb.append(this.filename).append(fieldSeparator);
			boolean first = true;
			for(Map.Entry<String, String> e: this.parameters.entrySet())
			{
				if(!first)sb.append(pairSeparator);
				else first = false;
				sb.append(e.getKey()).append(kvSeparator).append(e.getValue());
			}
			sb.append(fieldSeparator);
			sb.append(this.completed_timestamp);
			return sb.toString();
		}
		
		public static UserReport decodeLine(String line)
		{
			if(line==null||line.trim().length()==0)return null;

			String[] tokens = line.trim().split("\\u0001");
			
			UserReport urp = new UserReport();
			try
			{
				int idx = 0;
				urp.setId(Integer.parseInt(tokens[idx++]));
				urp.setType(tokens[idx++]);
				urp.setDbname(tokens[idx++]);
				urp.setDbhost(tokens[idx++]);
				urp.setCreated_timestamp(tokens[idx++]);
				urp.setFormat(tokens[idx++]);
				urp.setFilename(tokens[idx++]);
				String p = tokens[idx++];
				if(p!=null)
				{
					String[] pairs = p.split("\\u0002");
					if(pairs!=null)
					{
						for(String pair:pairs)
						{
							String[] kv = pair.split("\\u0003");
							if(kv!=null && kv.length==2)
							{
								urp.addParameter(kv[0], kv[1]);
							}
						}
					}
				}
				urp.setCompleted_timestamp(tokens[idx++]);
			}catch(Exception ex)
			{
				logger.log(Level.SEVERE,"Exception", ex);
			}
			return urp;
		}
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
	}
	public static final char lineSeparator = '\n';
	
	private String rootpath = "myperf_reports";//root path to store reports, default to reports under current directory
	private String idxfile = "rep.idx";//index file name
	private String username;
	
	private volatile boolean initiated = false;
	
	private File userroot;

	volatile  int maxId = 0;
	
	public UserReportManager(String username, String rootpath)
	{
		this.username = username;
		if(rootpath!=null&& rootpath.trim().length()>0)
			this.rootpath = rootpath.trim();
		//else use default
	}
	
	public void init()
	{
		if(!initiated)
		{
			File reproot = new File(rootpath);
			if(!reproot.exists())
				reproot.mkdir();
			
			userroot = new File(reproot, username);
			if(!userroot.exists())
				userroot.mkdir();
			this.initiated = true;
		}
	}
	
	synchronized public void addReportEntry(UserReport urp)
	{
		File idxFile = new File(userroot,idxfile);
		FileWriter fw = null;
			
		if(maxId==0)this.list();
		this.maxId++;
		urp.setId(this.maxId);
		try
		{
			fw = new FileWriter(idxFile, true);				
			String line = urp.encodeLine()+lineSeparator;
			fw.write(line);
		}catch(Exception ex)
		{
			
			logger.log(Level.SEVERE,"Exception", ex);
		}finally
		{
			try{if(fw!=null)fw.close();}catch(Exception iex){}
		}
	}
	synchronized public List<UserReport> list()
	{
		List<UserReport> repList = new java.util.ArrayList<UserReport>();
		File idxFile = new File(userroot,idxfile);
		if(idxFile.exists())
		{
			BufferedReader br = null;
			
			try
			{
				br = new BufferedReader(new FileReader(idxFile));
				
				String line = null;
				
				while((line=br.readLine())!=null)
				{
					UserReport rep = UserReport.decodeLine(line);
					if(rep!=null)
					{
						repList.add(rep);
						if(rep.getId()>=this.maxId)maxId = rep.getId();
					}
				}
			}catch(Exception ex)
			{
				logger.log(Level.SEVERE,"Exception", ex);
			}finally
			{
				try{if(br!=null)br.close();}catch(Exception iex){}
			}
		}
		return repList;
	}
	public ResultList listResultList()
	{
		List<UserReport> repList = list();
		
		ResultList rList = new ResultList();
		ColumnDescriptor desc = new ColumnDescriptor();
		rList.setColumnDescriptor(desc);
		int idx = 0;
		desc.addColumn("ID", false, idx++);
		desc.addColumn("TYPE", false, idx++);
		desc.addColumn("DB", false, idx++);
		desc.addColumn("HOST", false, idx++);
		desc.addColumn("CREATED", false, idx++);
		desc.addColumn("COMPLETED", false, idx++);
		desc.addColumn("FORMAT", false, idx++);
		desc.addColumn("PARAMETER", false, idx++);
		desc.addColumn("FILENAME", false, idx++);

		//list in reverse order
		for(int i=repList.size()-1; i>=0;i--)
		{
			UserReport urp = repList.get(i);
			ResultRow row = new ResultRow();
			List<String> cols = new java.util.ArrayList<String>(8);
			row.setColumnDescriptor(desc);
			row.setColumns(cols);
			cols.add(String.valueOf(urp.getId()));
			cols.add(urp.getType());
			cols.add(urp.getDbname());
			cols.add(urp.getDbhost());
			cols.add(urp.getCreated_timestamp());
			cols.add(urp.getCompleted_timestamp());
			cols.add(urp.getFormat());
			cols.add(urp.getParameters().toString());
			cols.add(urp.getFilename());
			rList.addRow(row);
		}
		return rList;
	}
	
	/**
	 * Refresh the idx file, for example, after delete an entry
	 * @param repList
	 */
	public void update(List<UserReport> repList)
	{
		File idxFile = new File(userroot,idxfile);
		FileWriter fw = null;
			
		try
		{
			fw = new FileWriter(idxFile);				
			for(UserReport urp:repList)
			{
				String line = urp.encodeLine()+lineSeparator;
				fw.write(line);
			}
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE,"Exception", ex);
		}finally
		{
			try{if(fw!=null)fw.close();}catch(Exception iex){}
		}
		
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getIdxfile() {
		return idxfile;
	}
	public void setIdxfile(String idxfile) {
		this.idxfile = idxfile;
	}
	public String getRootpath() {
		return rootpath;
	}
	public void setRootpath(String rootpath) {
		this.rootpath = rootpath;
	}
	
	public File getFileForDownload(String fname)//retrieve file for download
	{
		File f = new File(new File(new File(getRootpath()), getUsername()),fname);
		if(!f.exists()||!f.getAbsolutePath().endsWith(fname))//check file and security
			return null;
		return f;
	}
	
	public boolean deleteFile(String fname, String id)
	{
		if(fname==null&&id==null)return false;
		//first check if we have such file or not
		//update idx
		int rpid = -1;
		try{rpid=Integer.parseInt(id);}catch(Exception ex){}
		List<UserReport> repList = this.list();
		boolean findOne = false;
		for(int i=repList.size()-1;i>=0;i--)
		{
			if(repList.get(i).getFilename().equalsIgnoreCase(fname)||rpid==repList.get(i).getId())
			{
				fname = repList.get(i).getFilename();
				repList.remove(i);
				findOne = true;
				break;
			}
		}
		if(!findOne)return false;
		this.update(repList);
		File f = this.getFileForDownload(fname);
		if(f!=null &&!"rep.idx".equalsIgnoreCase(f.getName()))
		{
			f.delete();
			return true;
		}		
		return false;
	}
}
