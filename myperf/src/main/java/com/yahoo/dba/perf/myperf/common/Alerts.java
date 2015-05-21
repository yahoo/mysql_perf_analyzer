/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.List;

/**
 * For now, it holds alerts for web notification purpose.
 * @author xrao
 *
 */
public class Alerts implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	public static final int ALERT_CAPACITY = 100;//hold 100 alerts
	
	private List<AlertEntry> entries = null;
	
	public Alerts()
	{
		this.entries = new java.util.ArrayList<AlertEntry>(ALERT_CAPACITY);
	}
	
	/**
	 * Add a new entry. If list full, remove first one
	 * @param entry
	 */
	synchronized public void addAlert(AlertEntry entry)
	{
		if(this.entries.size() >= ALERT_CAPACITY)
			this.entries.remove(0);//remove first one
		this.entries.add(entry);
	}
	
	/**
	 * Retrieve a list of alerts later than given ts, up to max
	 * @param ts
	 * @return
	 */
	synchronized public List<AlertEntry> getEntries(long ts, int max)
	{
		List<AlertEntry> resList = new java.util.ArrayList<AlertEntry>();
		int cnt = 0;
		for(int i=this.entries.size() - 1; i>=0; i--)
		{
			AlertEntry e = this.entries.get(i);
			if(e.getTs() > ts)
			{
				resList.add(e);
				cnt ++;
				if(max > 0 && cnt >= max)break;
			}
		}
		return resList;
	}
}
