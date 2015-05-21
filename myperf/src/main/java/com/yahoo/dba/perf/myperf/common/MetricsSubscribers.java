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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yahoo.dba.perf.myperf.common.AlertSubscribers.Subscription;


/**
 * Records of (group, host, metrics group, metrics subsgroup)
 * @author xrao
 *
 */
public class MetricsSubscribers {
	private static Logger logger = Logger.getLogger(MetricsSubscribers.class.getName());
	public static class Subscription
	{
		//lazy, not getter/setter
		public String group; //server group
		public String host;// server host, can be null
		public String mGroup; //metrics group name
		public String mSubGroup; //metrics subgroup name
		
		public Subscription()
		{
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
			String key = sub.mSubGroup != null?sub.mGroup+"."+sub.mSubGroup: sub.mGroup;
			this.subscriptionsMap.put(key, sub);
		}
		
		public void deleteSubscription(Subscription sub)
		{
			if(sub == null)return;
			for(int i=this.subscriptions.size() - 1; i>=0; i--)
			{
				Subscription sub2 = this.subscriptions.get(i);
				if(sub2.mGroup.equals(sub.mGroup)
					&& ((sub2.mSubGroup == null && sub.mSubGroup == null)	
					|| (sub2.mSubGroup != null && sub2.mSubGroup.equals(sub.mSubGroup)))
				)
				{
					this.subscriptions.remove(i);
					String key = sub.mSubGroup != null?sub.mGroup+"."+sub.mSubGroup: sub.mGroup;
					this.subscriptionsMap.remove(key);
					break;
				}
			}

		}
		public Subscription getSubscription(String mGroup, String mSubGroup)
		{
			String key = mSubGroup != null?mGroup+"."+mSubGroup: mGroup;
			return this.subscriptionsMap.get(key);
		}
		public List<Subscription> getSubscribedUDMs()
		{
			List<Subscription> subs = new ArrayList<Subscription>();
			for(Subscription sub: this.subscriptions)
			{
				if("UDM".equals(sub.mGroup))
					subs.add(sub);
			}
			return subs;
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
		  if(sub.host == null || sub.host.isEmpty())
		  {
			  this.subscriptions.add(sub);
			  String key = sub.mSubGroup != null?sub.mGroup+"."+sub.mSubGroup: sub.mGroup;
			  this.subscriptionsMap.put(key, sub);
		  }
		  else
		  {
			  if(!this.hostsSubscriptions.containsKey(sub.host))
				  this.hostsSubscriptions.put(sub.host, new HostSubscriptions());
			  this.hostsSubscriptions.get(sub.host).addSubscription(sub);
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
					if(sub2.mGroup.equals(sub.mGroup)
						&& ((sub2.mSubGroup == null && sub.mSubGroup == null)	
						|| (sub2.mSubGroup != null && sub2.mSubGroup.equals(sub.mSubGroup)))
					)
					{
						this.subscriptions.remove(i);
						String key = sub.mSubGroup != null?sub.mGroup+"."+sub.mSubGroup: sub.mGroup;
						this.subscriptionsMap.remove(key);
						break;
					}
				}
			}else if(this.hostsSubscriptions.containsKey(sub.host))
			{
				this.hostsSubscriptions.get(sub.host).deleteSubscription(sub);
			}
		}
		public Subscription getSubscription(String host, String mGroup, String mSubGroup)
		{
			Subscription sub = null;
			if(this.hostsSubscriptions.containsKey(host))
				sub = this.hostsSubscriptions.get(host).getSubscription(mGroup, mSubGroup);
			if(sub == null)
			{
				String key = mSubGroup != null?mGroup+"."+mSubGroup: mGroup;
				sub = this.subscriptionsMap.get(key);
			}
			return sub;
		}

		public List<String> getSubscribedUDMs(String host)
		{
			List<String> subs = new ArrayList<String>();
			//for now, it is either at host level, or group level
			Set<String> udmSet = new HashSet<String>();
			for(Subscription sub: this.subscriptions)
			{
				if("UDM".equals(sub.mGroup))
					udmSet.add(sub.mSubGroup);
			}
			
			if(this.hostsSubscriptions.containsKey(host))
			{
				for(Subscription sub: this.hostsSubscriptions.get(host).getSubscribedUDMs())
					udmSet.add(sub.mSubGroup);
			}
			for(String subGrpName: udmSet)
				subs.add(subGrpName);
			return subs;
		}
	}
	
	private Map<String, GroupSubscriptions> groupSubscriptions;//all subscriptions, grouped by server group
	private MyPerfContext context; //framework context to access metrics db
	
	public MetricsSubscribers()
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
	    		List<Subscription>  subs = this.context.getMetricDb().loadMetricsSubscriptions();
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
		if(sub == null || sub.group == null || sub.group.isEmpty())return false;
		
		boolean status = this.context.getMetricDb()!=null?
				this.context.getMetricDb().addMetricsSubscription(sub):false;
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
				this.context.getMetricDb().deleteMetricsSubscription(sub): false;
		if(!status)	return false;		
		synchronized(this.groupSubscriptions)
		{
			String group = sub.group;
			if(this.groupSubscriptions.containsKey(group))
				this.groupSubscriptions.get(group).deleteSubscription(sub);						
		}
		return true;
	}

	
	public Subscription getSubscription(String group, String host, String mGroup, String mSubGroup)
	{
		synchronized(this.groupSubscriptions)
		{
			if(!this.groupSubscriptions.containsKey(group))return null;
			return this.groupSubscriptions.get(group).getSubscription(host, mGroup, mSubGroup);
		}
	}
	
	public List<String> getSubscribedUDMs(String group, String host)
	{
	  synchronized(this.groupSubscriptions)
	  {
		if(this.groupSubscriptions.containsKey(group))
			return this.groupSubscriptions.get(group).getSubscribedUDMs(host);
		return new ArrayList<String>();
	  }
	}
}

