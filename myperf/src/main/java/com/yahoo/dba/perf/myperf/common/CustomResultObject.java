/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

/**
 * An app specific object to be inserted into ResultList as part of result header
 * @author xrao
 *
 */
public interface CustomResultObject extends java.io.Serializable{
	String getName();//the name/key in json ResultList
	String getValueJsonString();//the value in json ResultList
}
