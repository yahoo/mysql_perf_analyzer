/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MyDatabases implements java.io.Serializable{

	private static final long serialVersionUID = -8586381924495834726L;

	private Set<String> myDbSet = new TreeSet<String>();
	
	synchronized public Set<String> getMyDbList()
	{
		return java.util.Collections.unmodifiableSet(this.myDbSet);
	}
	
	synchronized public void addDb(String name)
	{
		if(!this.myDbSet.contains(name))
			this.myDbSet.add(name);
	}
	synchronized public void addDbs(List<String> names)
	{
		for (String name:names)
		{
			if(!this.myDbSet.contains(name))
				this.myDbSet.add(name);
		}
	}
	
	synchronized public void removeDb(String name)
	{
		if(this.myDbSet.contains(name))
			this.myDbSet.remove(name);		
	}
	
	synchronized public void replaceDb(String oldName, String newName)
	{
		if(!this.myDbSet.contains(oldName))
		{
			this.myDbSet.remove(oldName);
			this.myDbSet.remove(newName);
			
		}
	}
	
	synchronized public int size()
	{
		return this.myDbSet.size();
	}
}
