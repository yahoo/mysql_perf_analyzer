/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * store realtime status
 * @author xrao
 *
 */
public class InstanceStatesManager implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(InstanceStatesManager.class.getName());
	public static String OBJ_FILE_NAME="instance_status.ser";
	public static final String STORAGE_DIR = "autoscan";
	private String rootPath = "myperf_reports";//data will be stored under $rootPath/autoscan
	
	private Map<Integer, InstanceStates> statesMap = new HashMap<Integer, InstanceStates>();
	
	public InstanceStatesManager()
	{
		
	}
	
	/**
	 * Make the data persistent
	 */
	public void saveStatus()
	{
		logger.info("Store instance status snapshots");
		File root = new File(new File(this.rootPath), STORAGE_DIR);		
		File objFile = new File(root, OBJ_FILE_NAME);
		storeObject(objFile);
	}
	public void storeObject(File objf)
	{
		ObjectOutputStream outputStream = null;
        
        try {
            
            //Construct the LineNumberReader object
            outputStream = new ObjectOutputStream(new FileOutputStream(objf));
            outputStream.writeObject(this.statesMap);            
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE,"Exception when store instance status object", ex);
        }finally {
            //Close the ObjectOutputStream
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
	
	@SuppressWarnings("unchecked")
	public Map<Integer, InstanceStates> readObject(File objf)
	{
		ObjectInputStream input = null;
		try
		{
			input = new ObjectInputStream(
	                new FileInputStream(objf));
			return (Map<Integer, InstanceStates>)input.readObject();
		}catch(Exception ex)
		{
            logger.log(Level.WARNING,"Exception when read instance status object", ex);
			
		}finally
		{
			if(input!=null)try{input.close();}catch(Exception iex){}
		}
		return null;
	}

	synchronized public InstanceStates getStates(int dbid)
	{
		if(statesMap.containsKey(dbid))
			return statesMap.get(dbid);
		return null;
	}
	
	
	synchronized public void addInstanceStates(int dbid)
	{
		if(statesMap.containsKey(dbid))
			return;
		this.statesMap.put(dbid, new InstanceStates());
	}
	
	synchronized public boolean removeInstanceStates(int dbid)
	{
		if(statesMap.containsKey(dbid))
		{
			statesMap.remove(dbid);
			return true;
		}
		return false;
	}
	
	/**
	 * This should be called during context initialization
	 * @param context
	 */
	public void init(MyPerfContext context)
	{
		logger.info("Initialize InstanceStatesManager");
		File root = new File(new File(this.rootPath), STORAGE_DIR);
		if(!root.exists())root.mkdirs();
		
		File objFile = new File(root, OBJ_FILE_NAME);
		if(objFile.exists())
		{
			logger.info("Load saved status");
			Map<Integer, InstanceStates> savedState = readObject(objFile);
			if(savedState!=null)
			{
				for(Map.Entry<Integer, InstanceStates> e: savedState.entrySet())
					this.statesMap.put(e.getKey(), e.getValue());
			}
		}
		
		for(Map.Entry<String, DBGroupInfo> e: context.getDbInfoManager().getClusters().entrySet())
        {
          for (DBInstanceInfo dbinfo: e.getValue().getInstances())
          {
        	  if(!this.statesMap.containsKey(dbinfo.getDbid()))
        		  this.statesMap.put(dbinfo.getDbid(), new InstanceStates());
          }
        }
		logger.info("Initialized InstanceStatesManager");		
	}

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}
}
