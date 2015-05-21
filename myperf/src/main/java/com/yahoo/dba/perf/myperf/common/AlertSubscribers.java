/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;


/**
 * Records of (group, host, alert)
 * @author xrao
 *
 */
public class AlertSubscribers {
	private static Logger logger = Logger.getLogger(AlertSubscribers.class.getName());
	public static class Subscription
	{
		//lazy, not getter/setter
		public String group; //server group
		public String host;// server host, can be null
		public String alertName; //alert name
		public Float threshold; //if required. Null use default
		public Map<String, String> params;
		
		public Subscription()
		{
			params = new HashMap<String, String>();
		}
		//parse and set params
		public void setParams(String paramString)
		{
			if(paramString == null || paramString.isEmpty())return;
			JsonReader jsonReader = null;
			try
			{
				jsonReader = javax.json.Json.createReader(new java.io.ByteArrayInputStream(paramString.getBytes()));
				JsonObject jsonObject = jsonReader.readObject();
				jsonReader.close();
				
				String thresholdString = jsonObject.getString("threshold", null);
				if(thresholdString!=null && !thresholdString.isEmpty())
					try{threshold = Float.parseFloat(thresholdString);}catch(Exception ex){}
				
				JsonArray paramList = jsonObject.getJsonArray("params");
				if(paramList != null )
				{
					int mlen = paramList.size();
					for(int i=0; i<mlen; i++)
					{
						JsonObject mobj = paramList.getJsonObject(i);
						params.put(mobj.getString("name"), 
								mobj.getString("value"));
					}
				}
			}catch(Exception ex)
			{
				logger.log(Level.WARNING, "Error to parse alert subscription params: "+paramString, ex);
			}

		}
		
		public String paramToJSON()
		{
			if((this.params == null || this.params.size()==0) && this.threshold ==null)
				return null;
			StringBuilder mtrStr = new StringBuilder();
			mtrStr.append("{\r\n");
			if(this.threshold != null)
				mtrStr.append("\"threshold\": \"").append(threshold).append("\",\r\n");
			mtrStr.append("\"params\":[");
			int cnt = 0;
			for(Map.Entry<String, String> e: this.params.entrySet())
			{
				if(cnt>0)mtrStr.append(",\r\n");
				mtrStr.append("{\"name\": \"").append(e.getKey()).append("\",\"value\":\"")
					      .append(e.getValue())
					      .append("\"}");
			}
		    mtrStr.append("]}");
			return mtrStr.toString();
		}
	}
	
	public static class HostSubscriptions
	{
		private List<Subscription> subscriptions;//host level subscription
		private Map<String, Subscription> subscriptionsMap; //internal index
		public HostSubscriptions()
		{
			subscriptions = new ArrayList<Subscription>();
			subscriptionsMap = new HashMap<String, Subscription>();
		}
		public void addSubscription(Subscription sub)
		{
			this.subscriptions.add(sub);
			this.subscriptionsMap.put(sub.alertName, sub);
		}
		
		public void deleteSubscription(Subscription sub)
		{
			if(sub == null)return;
			for(int i=this.subscriptions.size() - 1; i>=0; i--)
			{
				Subscription sub2 = this.subscriptions.get(i);
				if(sub2.alertName .equals(sub.alertName))
				{
					this.subscriptions.remove(i);
					this.subscriptionsMap.remove(sub.alertName);
					break;
				}
			}
		}
		
		public Subscription getSubscription(String alertName)
		{
			return this.subscriptionsMap.get(alertName);
		}
		
		public List<Subscription> getSubscriptions()
		{
			return this.subscriptions;
		}
	}
	
	public static class GroupSubscriptions
	{
		private List<Subscription> subscriptions;//group level subscription
		private Map<String, HostSubscriptions> hostsSubscriptions;//key is the hostname
		private Map<String, Subscription> subscriptionsMap; //internal index
		public GroupSubscriptions()
		{
			subscriptions = new ArrayList<Subscription>();
			hostsSubscriptions = new HashMap<String, HostSubscriptions>();
			subscriptionsMap = new HashMap<String, Subscription>();
		}
		
		public void addSubscription(Subscription sub)
		{
		  if(sub == null)return;
		  if(sub.host == null || sub.host.isEmpty())this.subscriptions.add(sub);
		  else
		  {
			  if(!this.hostsSubscriptions.containsKey(sub.host))
				  this.hostsSubscriptions.put(sub.host, new HostSubscriptions());
			  this.hostsSubscriptions.get(sub.host).addSubscription(sub);
			  this.subscriptionsMap.put(sub.alertName, sub);
		  }
		}
		
		public void deleteSubscription(Subscription sub)
		{
			if(sub == null)return;
			if(sub.host == null || sub.host.isEmpty())
			{
				for(int i=this.subscriptions.size() - 1; i>=0; i--)
				{
					Subscription sub2 = this.subscriptions.get(i);
					if(sub2.alertName .equals(sub.alertName))
					{
						this.subscriptions.remove(i);
						this.subscriptionsMap.remove(sub.alertName);
						break;
					}
				}
			}else if(this.hostsSubscriptions.containsKey(sub.host))
			{
				this.hostsSubscriptions.get(sub.host).deleteSubscription(sub);
			}
		}
		public Subscription getSubscription(String host, String alertName)
		{
			Subscription sub = null;
			if(this.hostsSubscriptions.containsKey(host))
				sub = this.hostsSubscriptions.get(host).getSubscription(alertName);
			if(sub == null)sub = this.subscriptionsMap.get(alertName);
			return sub;
		}
		public List<Subscription> getSubscription(String host)
		{
			HashSet<String> alertnames = new HashSet<String>();
			List<Subscription> mysubs = new ArrayList<Subscription>();
			if(this.hostsSubscriptions.containsKey(host))
			{
				for(Subscription sub: this.hostsSubscriptions.get(host).getSubscriptions())
				{
					mysubs.add(sub);
					alertnames.add(sub.alertName);
				}
			}
			//group level
			for(Subscription sub: subscriptions)
			{
				if(!alertnames.contains(sub.alertName))
				{
					mysubs.add(sub);
					alertnames.add(sub.alertName);
				}
			}
			return mysubs;
		}
	}
	
	private Map<String, GroupSubscriptions> groupSubscriptions;//all subscriptions, grouped by server group
	private MyPerfContext context; //framework context to access metrics db
	
	public AlertSubscribers()
	{
		groupSubscriptions = new HashMap<String, GroupSubscriptions>();
	}

	public MyPerfContext getContext() {
		return context;
	}

	public void setContext(MyPerfContext context) {
		this.context = context;
	}
	
	/**
	 * Load subscriptions from DB. Return false if failed.
	 * @return
	 */
	public boolean load()
	{
	    if(this.context.getMetricDb() != null)
	    {
	    	try
	    	{
	    		List<Subscription>  subs = this.context.getMetricDb().loadAlertSubscriptions();
	    		synchronized(this.groupSubscriptions)
	    		{
	    			this.groupSubscriptions.clear();//remove old data
	    			for(Subscription sub: subs)
	    			{
	    				String group = sub.group;
	    				if(!this.groupSubscriptions.containsKey(group))
	    				{
	    					this.groupSubscriptions.put(group, new GroupSubscriptions());
	    				}
	    				this.groupSubscriptions.get(group).addSubscription(sub);	    				
	    			}
	    		}
	    		return true;
	    	}catch(Exception ex)
	    	{
	    		logger.log(Level.WARNING, "Failed to load alert subscriptions", ex);
	    		return false;
	    	}
	    }
		return false;
	}
	
	public boolean addSubscription(Subscription sub)
	{
		if(sub == null || sub.group == null || sub.group.isEmpty() 
				||sub.alertName == null || sub.alertName.isEmpty())return false;
		
		boolean status = this.context.getMetricDb()!=null?
				this.context.getMetricDb().upsertAlertSubscription(sub):false;
		if(!status)	return false;
		synchronized(this.groupSubscriptions)
		{
			String group = sub.group;
			if(!this.groupSubscriptions.containsKey(group))
				this.groupSubscriptions.put(group, new GroupSubscriptions());
			this.groupSubscriptions.get(group).addSubscription(sub);			
		}
		return true;
	}
	
	public boolean deleteSubscription(Subscription sub)
	{
		boolean status = this.context.getMetricDb() != null?
				this.context.getMetricDb().deleteAlertSubscription(sub): false;
		if(!status)	return false;		
		synchronized(this.groupSubscriptions)
		{
			String group = sub.group;
			if(this.groupSubscriptions.containsKey(group))
				this.groupSubscriptions.get(group).deleteSubscription(sub);						
		}
		return true;
	}
	
	/**
	 * Check if a server has subscribed an alert
	 * @param group
	 * @param host
	 * @param alertName
	 * @return
	 */
	public Subscription getSubscription(String group, String host, String alertName)
	{
		synchronized(this.groupSubscriptions)
		{
			if(!this.groupSubscriptions.containsKey(group))return null;
			return this.groupSubscriptions.get(group).getSubscription(host, alertName);
		}
	}
	
	public List<Subscription> getSubscriptions(String group, String host)
	{
		synchronized(this.groupSubscriptions)
		{
			if(!this.groupSubscriptions.containsKey(group))return null;
			return this.groupSubscriptions.get(group).getSubscription(host);
		}
	}
}
