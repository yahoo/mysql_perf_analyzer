/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.PrintWriter;

/**
 * Allow application specific information summary to take advantage of application convention
 * @author xrao
 *
 */
interface ProcessListEntryProcessor 
{
	void processEntry(ProcessListEntry e);
	void dumpSummary(PrintWriter pw);
}
