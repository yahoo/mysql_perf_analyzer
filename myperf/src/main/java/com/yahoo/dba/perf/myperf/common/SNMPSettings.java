/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

/**
 * Store SNMP settings for community, version, etc.
 *
 */
public class SNMPSettings implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(SNMPSettings.class.getName());
    
	public static class SNMPSetting
	{
		public static final String DEFAULT_COMMUNITY = "public";
		public static final String DEFAULT_VERSION = "2c";
		public static final String COMMUNITY = "community";
		public static final String VERSION = "version";		
		public static final String ENABLEED = "enabled";		
		public static final String V3_USERNAME = "username";
		public static final String V3_PASSWORD = "password";
		public static final String V3_AUTHPROTOCOL = "authprotocol";
		public static final String V3_PRIVACYPASSPHRASE = "privacypassphrase";
		public static final String V3_PRIVACYPROTOCOL = "privacyprotocol";
		public static final String V3_CONTEXT = "context";
		
		private String community;
		private String version;
		private String username;//v3
		private String password;//v3
		private String authProtocol;//v3
		private String privacyPassphrase;//v3
		private String privacyProtocol;//v3
		private String context;//v3
		private String enabled = "yes";
		
		public SNMPSetting()
		{
			
		}
		public SNMPSetting(String community, String version)
		{
			this.community = community;
			this.version = version;
		}
		public String getCommunity() {
			return community;
		}
		public void setCommunity(String community) {
			this.community = community;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		
		public SNMPSetting copy()
		{
			return new SNMPSetting(this.community, this.version);
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getAuthProtocol() {
			return authProtocol;
		}
		public void setAuthProtocol(String authProtocol) {
			this.authProtocol = authProtocol;
		}
		public String getPrivacyPassphrase() {
			return privacyPassphrase;
		}
		public void setPrivacyPassphrase(String privacyPassphrase) {
			this.privacyPassphrase = privacyPassphrase;
		}
		public String getPrivacyProtocol() {
			return privacyProtocol;
		}
		public void setPrivacyProtocol(String privacyProtocol) {
			this.privacyProtocol = privacyProtocol;
		}
		public String getContext() {
			return context;
		}
		public void setContext(String context) {
			this.context = context;
		}
		public String getEnabled() {
			return enabled;
		}
		public void setEnabled(String enabled) {
			this.enabled = enabled;
		}
	}
	
	private SNMPSetting siteSetting;//default site setting, if provided
	//group level setting, if any. The key is dbGroupName
	private Map<String, SNMPSetting> groupSettings = new HashMap<String, SNMPSetting>();
	//host level settings, if any. The key is dbGroupName+"|"+hostName
	private Map<String, SNMPSetting> hostSettings = new HashMap<String, SNMPSetting>();
	private String myperfSnmpConfigPath;
	private String rootPath = "myperf_config";
	private String SNMPPATH = "snmp";
	private MyPerfContext frameworkContext;
	
	public SNMPSettings()
	{
		
	}

	public SNMPSetting getSiteSetting() {
		return siteSetting;
	}

	public void setSiteSetting(SNMPSetting siteSetting) {
		this.siteSetting = siteSetting;
	}

	public void setGroupSetting(String dbGroup, SNMPSetting groupSetting) {
		this.groupSettings.put(dbGroup, groupSetting);
	}

	public Map<String, SNMPSetting> getHostSettings() {
		return hostSettings;
	}

	public void setHostSetting(String dbGroupName, String hostName, SNMPSetting hostSetting) {
		this.hostSettings.put(dbGroupName+"|"+hostName, hostSetting);
	}

	/**
	 * 
	 * @param dbGroupHostName dbGroupName+"|"+hostName
	 * @param hostSetting
	 */
	public void setHostSetting(String dbGroupHostName, SNMPSetting hostSetting) {
		this.hostSettings.put(dbGroupHostName, hostSetting);
	}

	public SNMPSetting getGroupSetting(String dbGroupName)
	{
		if(this.groupSettings.containsKey(dbGroupName))
			return this.groupSettings.get(dbGroupName);
		return this.siteSetting;
	}
	
	public SNMPSetting getHostSetting(String dbgroup, String host)
	{
		if((dbgroup == null || dbgroup.isEmpty() ) && (host == null || host.isEmpty()))
			return this.siteSetting;
		if (this.hostSettings.containsKey(dbgroup + "|" + host))
			return this.hostSettings.get(dbgroup + "|" + host);
		else
			return getGroupSetting(dbgroup);
	}
	
	public boolean updateSnmpSetting(String dbgroup, String host, String community, String ver,
			String username, String password, String authprotocol, String privacypassphrase,
			String privacyprotocol, String context, String enabled		
			)
	{
		SNMPSetting setting = new SNMPSetting(community, ver);
		if("3".equals(ver))
		{
			setting.setUsername(username);
			setting.setPassword(password);
			setting.setAuthProtocol(authprotocol);
			setting.setPrivacyPassphrase(privacypassphrase);
			setting.setPrivacyProtocol(privacyprotocol);
			setting.setContext(context);
		}
		setting.setEnabled("no".equalsIgnoreCase(enabled)? "no": "yes");
		if((dbgroup == null || dbgroup.isEmpty() ) && (host == null || host.isEmpty()))
		{
			this.siteSetting = setting;
			logger.info("Add snmp setting for site, version "+setting.getVersion());
		}else if (host == null || host.isEmpty())
		{
			this.setGroupSetting(dbgroup, setting);
			logger.info("Add snmp setting for group" + dbgroup + ", version "+setting.getVersion());
		}else
		{
			this.setHostSetting(dbgroup, host, setting);
			logger.info("Add snmp setting for host (" + dbgroup + ", "+ host+") version "+setting.getVersion());
		}
		return this.store();
	}
	public void init(MyPerfContext ctx)
	{
		logger.info("Read customized SNMP configuration ...");
		this.frameworkContext = ctx;
		File snmproot = new File(new File(rootPath), SNMPPATH);
		if(!snmproot.exists())snmproot.mkdirs();
		myperfSnmpConfigPath = snmproot.getAbsolutePath() + File.separatorChar + "snmp.json";
		this.readConfig();
	}

	synchronized public boolean store()
	{
		File cfgFile = new File(this.myperfSnmpConfigPath);
		if(!cfgFile.exists())
		{
			logger.log(Level.INFO, "There is no customized snmp configuration, create one.");
		}
		Writer pw = null;
		try
		{
			pw = new FileWriter(cfgFile);
			return this.writeToJson(pw);
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Failed to store configurations to file " + this.myperfSnmpConfigPath, ex);
		}finally
		{
			if(pw!=null){try{pw.flush();pw.close();}catch(Exception fex){}}
		}
		return false;
	}
	
	synchronized private boolean readConfig()
	{
		File cfgFile = new File(this.myperfSnmpConfigPath);
		if(!cfgFile.exists())
		{
			logger.info("There is no customized snmp configuration file");
			return true;
		}
		FileInputStream in = null;
		try
		{
			in = new FileInputStream(cfgFile);
			JsonReader jr = javax.json.Json.createReader(in);
			JsonObject jsonObject = jr.readObject();
			jr.close();
			this.setSiteSetting(readFromJson(jsonObject.getJsonObject("site")));
			JsonArray groups = jsonObject.getJsonArray("group");
			if(groups != null)
			{
				int len = groups.size();
				for(int i=0; i<len; i++)
				{
					JsonObject grp = groups.getJsonObject(i);
					SNMPSetting grpSetting = readFromJson(grp);
					String grpName = grp.getString("dbgroup", null);
					if(grpName != null && grpSetting != null)
						this.groupSettings.put(grpName, grpSetting);
				}
			}
			JsonArray hosts = jsonObject.getJsonArray("host");
			if(hosts != null)
			{
				int len = hosts.size();
				for(int i=0; i<len; i++)
				{
					JsonObject host = hosts.getJsonObject(i);
					SNMPSetting hostSetting = readFromJson(host);
					String hostName = host.getString("dbhost", null);
					if(hostName != null && hostSetting != null)
						this.hostSettings.put(hostName, hostSetting);
				}
			}
			return true;
		}catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Failed to read SNMP configuration file "+cfgFile.getAbsolutePath(), ex);
		}finally
		{
			if(in!=null){try{in.close();}catch(Exception fex){};}			
		}
		return false;
	}

	private SNMPSetting readFromJson(JsonObject jobj)
	{
		SNMPSetting snmp = null;
		if(jobj != null)
		{
			String version = jobj.getString(SNMPSetting.VERSION, null);
			String community = jobj.getString(SNMPSetting.COMMUNITY, null);
			String enabled = jobj.getString(SNMPSetting.ENABLEED, "yes");
			if((version != null && !version.isEmpty()) || (community != null && !community.isEmpty()))
			{
				snmp = new SNMPSetting();
				snmp.setVersion(version);
				snmp.setCommunity(community);
				snmp.setEnabled(enabled);
				if("3".equals(version))
				{
					snmp.setUsername(jobj.getString(SNMPSetting.V3_USERNAME, null));
					String pwd = jobj.getString(SNMPSetting.V3_PASSWORD, null);
					if(pwd != null)pwd = frameworkContext.getMetaDb().dec(pwd);
					snmp.setPassword(pwd);
					snmp.setAuthProtocol(jobj.getString(SNMPSetting.V3_AUTHPROTOCOL, null));
					pwd = jobj.getString(SNMPSetting.V3_PRIVACYPASSPHRASE, null);
					if(pwd != null)pwd = frameworkContext.getMetaDb().dec(pwd);
					snmp.setPrivacyPassphrase(pwd);					
					snmp.setPrivacyProtocol(jobj.getString(SNMPSetting.V3_PRIVACYPROTOCOL, null));
					snmp.setContext(jobj.getString(SNMPSetting.V3_CONTEXT, null));
				}
			}
		}
		return snmp;
	}
	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}
	
	private boolean writeToJson(Writer writer)
	{
		javax.json.JsonWriter wr = null;
		try
		{
			JsonObjectBuilder settingBuilder = Json.createObjectBuilder();
			settingBuilder.add("site", toJson(this.siteSetting));
		
			JsonArrayBuilder groupBuilder = Json.createArrayBuilder();
			for(Map.Entry<String, SNMPSetting> e: this.groupSettings.entrySet())
			{
				JsonObjectBuilder grp = toJson(e.getValue());
				grp.add("dbgroup", e.getKey());
				groupBuilder.add(grp);			
			}
			settingBuilder.add("group", groupBuilder);
		
			JsonArrayBuilder hostBuilder = Json.createArrayBuilder();
			for(Map.Entry<String, SNMPSetting> e: this.hostSettings.entrySet())
			{
				JsonObjectBuilder host = toJson(e.getValue());
				host.add("dbhost", e.getKey());
				hostBuilder.add(host);			
			}
			settingBuilder.add("host", hostBuilder);
				
			wr = javax.json.Json.createWriter(writer);
			
			JsonObject jsonObj = settingBuilder.build();
			wr.writeObject(jsonObj);
			wr.close();
			wr = null;
			return true;
		}catch(Exception ex)
		{
			logger.log(Level.INFO, "Failed to store snmp json", ex);
		}finally
		{
			if(wr != null)try{wr.close();}catch(Exception ex){}
		}
		return false;
	}
	
	private JsonObjectBuilder toJson(SNMPSettings.SNMPSetting setting)
	{
		JsonObjectBuilder sb = Json.createObjectBuilder();
		if(setting != null)
		{
			if(setting.getVersion() != null && !setting.getVersion().isEmpty())
				sb.add(SNMPSetting.VERSION, setting.getVersion());
			if(setting.getCommunity() != null && !setting.getCommunity().isEmpty())
				sb.add(SNMPSetting.COMMUNITY, setting.getCommunity());
			if("3".equals(setting.getVersion()))
			{
				if(setting.getUsername() != null && !setting.getUsername().isEmpty())
					sb.add(SNMPSetting.V3_USERNAME, setting.getUsername());
				if(setting.getPassword() != null && !setting.getPassword().isEmpty())
					sb.add(SNMPSetting.V3_PASSWORD, frameworkContext.getMetaDb().enc(setting.getPassword()));
				
				if(setting.getAuthProtocol() != null && !setting.getAuthProtocol().isEmpty())
					sb.add(SNMPSetting.V3_AUTHPROTOCOL, setting.getAuthProtocol());
				if(setting.getPrivacyPassphrase() != null && !setting.getPrivacyPassphrase().isEmpty())
					sb.add(SNMPSetting.V3_PRIVACYPASSPHRASE,frameworkContext.getMetaDb().enc(setting.getPrivacyPassphrase()));				
				if(setting.getPrivacyProtocol() != null && !setting.getPrivacyProtocol().isEmpty())
					sb.add(SNMPSetting.V3_PRIVACYPROTOCOL, setting.getPrivacyProtocol());
				if(setting.getContext() != null && !setting.getContext().isEmpty())
					sb.add(SNMPSetting.V3_CONTEXT, setting.getContext());
			}
			sb.add(SNMPSetting.ENABLEED, setting.getEnabled());
			
		}
		return sb;
	}

	public MyPerfContext getFrameworkContext() {
		return frameworkContext;
	}

	public void setFrameworkContext(MyPerfContext frameworkContext) {
		this.frameworkContext = frameworkContext;
	}
}
