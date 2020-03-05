<%@page trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Performance Schema</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
</head>
<body>
<div><!-- query form -->  
  <span><span>DB Group</span>
        <span><select name="dbgroup" id="dbgroup" >
             <option value="">----</option>
              <c:forEach var="cluster" items="${mydbs}" varStatus="stat">
<c:if test="${cluster==sessionScope.group}">
    			<option value="${cluster}" selected="true" ${stat.index>=mydbSize?"style='color:red;'":""}>${cluster}</option>
</c:if>
<c:if test="${cluster!=sessionScope.group}">              
    			<option value="${cluster}" ${stat.index>=mydbSize?"style='color:red;'":""}>${cluster}</option>
</c:if>    			
  			  </c:forEach>          
          </select>&nbsp;<input type="button" value="Find" onclick="prepareDBSearch('dbgroup');"/>
        </span>
        <span>DB Host</span><span>
           <select  name="host" id="host">
             <option value="">select host name</option>
<c:if test="${sessionScope.group!=null}">
              <c:forEach var="db" items="${dbMap[sessionScope.group].instances}">              
<c:if test="${db.hostName==sessionScope.host}">
    			<option value="${db.hostName}" selected="true">${db.hostName}</option>
</c:if>
<c:if test="${db.hostName!=sessionScope.host}">
    			<option value="${db.hostName}">${db.hostName}</option>
</c:if>    			
              </c:forEach>
</c:if>        
           </select>
        </span>

        <select name="ps_activity" id="ps_activity">
          <option value="setup">Setup</option>
          <option value="threads">Threads</option>
          <option value="eventsWaits">Events Waits Current</option>
          <option value="eventsWaitsSummary">Events Waits Summary</option>
          <option value="mysql_perf_objects_summary">Table/Index Access Summary</option>
          <option value="mysql_perf_table_io_summary">Table IO Summary</option>
          <option value="mysql_perf_index_io_summary">Index IO Summary</option>
          <option value="mysql_perf_filesum_inst">File Summary</option>
          <option value="mysql_perf_table_lock_summary">Table Lock Summary</option>
          <option value="mysql_perf_mutex">Mutex</option>
          <option value="mysql_perf_rwlock">RW Locks</option>
          <option value="mysql_metadata_lock">Metadata Locks</option>
          <option value="mysql_perf_memory_summary_global_by_event_name">Global Memory</option>
          <option value="mysql_perf_memory_summary_by_thread_by_event_name">Thread Memory</option>          
          <option value="digest">Top Queries</option>
        </select>
        <input type="button" id="btn_ps_activity" value="&#9658;"/>
   </span>
</div><!-- end of query form -->

<span id="common_msg"> <!-- common message line -->
<c:if test="${mydbSize==0}">You have not provided any database credential yet. Please use <a href="<%= request.getContextPath() %>/db.htm">DB Credential</a> page to provide access information for databases you are interested in.</c:if>
</span>

<div id="pstabs" class="clearTabView"> <!-- tab pane to display real time info in tabular formats -->
    <ul>
        <li><a href="#setup_tbl_div" title="MySQL Performance schema Setup">Setup</a></li>
        <li><a href="#thread_tab" title="MySQL Performance schema Thread list">Threads</a></li>
        <li><a href="#mysql_perf_events_waits_current_tbl_div" title="MySQL Performance schema Current Events">Events Waits</a></li>
        <li><a href="#mysql_perf_events_waits_summary_tbl_div" title="MySQL Performance schema Events summary">Events Waits Summary</a></li>
        <li><a href="#mysql_table_tab_div" title="Table and file IO">Table/File IO</a></li>
        <li><a href="#mysql_perf_mutex_tbl_div" title="MySQL Performance schema currently locked mutex">Mutex</a></li>
        <li><a href="#mysql_perf_rwlock_tbl_div" title="MySQL Performance schema RW Locks">RW Locks</a></li>
        <li><a href="#mysql_metadata_lock_tbl_div" title="MySQL Metadata Lock">Metadata Locks</a></li>
        <li><a href="#mysql_mem_tab_div" title="MySQL memory allocation">Memory</a></li>
        <li><a href="#top_query" title="MySQL Performance schema top queries">Top Queries</a></li>
    </ul>
    <div id="setup_tbl_div">
          <select name="setup_activity" id="setup_activity">
            <option value="mysql_perf_setup_timers">Timer</option>
            <option value="mysql_perf_setup_instruments">Instruments</option>
            <option value="mysql_perf_setup_consumers">Consumers</option>
            <option value="mysql_perf_setup_threads">Threads</option>
            <option value="mysql_perf_setup_actors">Actors</option>
            <option value="mysql_perf_setup_objects">Objects</option>
          </select>
          <input type="button" id="btn_setup_activity" value="&#9658;"/>
          <div id="ps_setup_tab" class="clearTabView">
            <ul>
              <li><a href="#mysql_perf_setup_timers_tbl_div" title="MySQL Performance schema setup_timer">Timer</a></li>
              <li><a href="#mysql_perf_setup_instruments_tbl_div" title="MySQL Performance schema setup_instruments">Instruments</a></li>
              <li><a href="#mysql_perf_setup_consumers_tbl_div" title="MySQL Performance schema setup_consumers">Consumers</a></li>
              <li><a href="#mysql_perf_setup_threads_tbl_div" title="MySQL Performance schema threads">Threads</a></li>
              <li><a href="#mysql_perf_setup_actors_tbl_div" title="MySQL Performance schema setup_actors">Actors</a></li>
              <li><a href="#mysql_perf_setup_objects_tbl_div" title="MySQL Performance schema setup_objects">Objects</a></li>
            </ul>
            <div id="mysql_perf_setup_timers_tbl_div">
		      <table id="mysql_perf_setup_timers_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
            </div>
            <div id="mysql_perf_setup_instruments_tbl_div">
		      <table id="mysql_perf_setup_instruments_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
            </div>
            <div id="mysql_perf_setup_consumers_tbl_div">
		      <table id="mysql_perf_setup_consumers_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
            </div>
            <div id="mysql_perf_setup_threads_tbl_div">
		      <table id="mysql_perf_setup_threads_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
            </div>
            <div id="mysql_perf_setup_actors_tbl_div">
		      <table id="mysql_perf_setup_actors_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
            </div>
            <div id="mysql_perf_setup_objects_tbl_div">
		      <table id="mysql_perf_setup_objects_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
            </div>            
          </div>  
      </div> <!-- setup -->
      
      <div id="thread_tab" class="clearTabView">
          <ul>
              <li><a href="#mysql_perf_threads_tbl_div" title="MySQL Performance schema Threads">Threads</a></li>
              <li><a href="#thread_waits_summary_tbl_div" title="MySQL Performance schema Thread Wait Summary">Thread Wait Summary</a></li>
              <li><a href="#thread_stmt_summary_tbl_div" title="MySQL Performance schema Thread Statement Summary">Thread Statement Summary</a></li>
              <li><a href="#thread_stage_summary_tbl_div" title="MySQL Performance schema Thread Stage Summary">Thread Stage Summary</a></li>
          </ul>
          <div id="mysql_perf_threads_tbl_div">   
            <table id="mysql_perf_threads_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
          </div>  
          <div id="thread_waits_summary_tbl_div">
             <span>Thread ID: <input type="text" id="thread_id" /> 
                &nbsp;&nbsp; <input type="button" id="btn_mysql_perf_thread_waits_summary" value="Refresh" title="Click to see differences"/>
                &nbsp;&nbsp; <input type="button" id="btn_mysql_perf_thread_waits_summary_reset" value="Reset" title="Click to Restart"/>
             </span>
             <table id="mysql_perf_thread_waits_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
          </div>  
          <div id="thread_stmt_summary_tbl_div">
             <span>Thread ID: <input type="text" id="stmt_thread_id" /> 
                &nbsp;&nbsp; <input type="button" id="btn_mysql_perf_thread_stmt_summary" value="Refresh" title="Click to see differences"/>
                &nbsp;&nbsp; <input type="button" id="btn_mysql_perf_thread_stmt_summary_reset" value="Reset" title="Click to Restart"/>
             </span>
             <table id="mysql_perf_thread_stmt_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
          </div>  
          <div id="thread_stage_summary_tbl_div">
             <span>Thread ID: <input type="text" id="stage_thread_id" /> 
                &nbsp;&nbsp; <input type="button" id="btn_mysql_perf_thread_stage_summary" value="Refresh" title="Click to see differences"/>
                &nbsp;&nbsp; <input type="button" id="btn_mysql_perf_thread_stage_summary_reset" value="Reset" title="Click to Restart"/>
             </span>
             <table id="mysql_perf_thread_stage_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
          </div>  
      </div><!-- thread -->
      
      <div id="mysql_perf_events_waits_current_tbl_div" class="datatableContainer">
        <table id="mysql_perf_events_waits_current_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      
      <div id="mysql_perf_events_waits_summary_tbl_div" class="datatableContainer">
    	 <span><input type="button" id="btn_events_waits_summary" value="Refresh" title="Click to see differences"/> (Click Refresh to see changes)</span>
         <table id="mysql_perf_events_waits_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      
      <div id="mysql_table_tab_div" class="clearTabView">
        <ul>
          <li><a href="#mysql_perf_objects_summary_tbl_div" title="MySQL Performance schema Table/Index Access summary">Table/Index Access Summary</a></li>
          <li><a href="#mysql_perf_table_io_summary_tbl_div" title="MySQL Performance schema Table IO summary">Table IO </a></li>
          <li><a href="#mysql_perf_index_io_summary_tbl_div" title="MySQL Performance schema Index IO summary">Index Usage</a></li>
          <li><a href="#mysql_perf_filesum_inst_tbl_div" title="MySQL Performance schema File summary">File IO</a></li>
          <li><a href="#mysql_perf_table_lock_summary_tbl_div" title="MySQL Performance schema Index IO summary">Table Locks</a></li>        
        </ul>           
        <div id="mysql_perf_objects_summary_tbl_div" class="datatableContainer">
    	 <span><input type="button" id="btn_mysql_perf_objects_summary" value="Refresh" title="Click to see differences"/> (Click Refresh to see changes)</span>
         <table id="mysql_perf_objects_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
        </div>
        <div id="mysql_perf_table_io_summary_tbl_div" class="datatableContainer">
    	 <span><input type="button" id="btn_mysql_perf_table_io_summary" value="Refresh" title="Click to see differences"/> (Click Refresh to see changes)</span>
         <table id="mysql_perf_table_io_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
        </div>
        <div id="mysql_perf_index_io_summary_tbl_div" class="datatableContainer">
    	 <span><input type="button" id="btn_mysql_perf_index_io_summary" value="Refresh" title="Click to see differences"/> (Click Refresh to see changes)</span>
         <table id="mysql_perf_index_io_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	    </div>
	    <div id="mysql_perf_filesum_inst_tbl_div" class="datatableContainer">
         <table id="mysql_perf_filesum_inst_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	    </div>
        <div id="mysql_perf_table_lock_summary_tbl_div" class="datatableContainer">
    	 <span><input type="button" id="btn_mysql_perf_table_lock_summary" value="Refresh" title="Click to see differences"/> (Click Refresh to see changes)</span>
         <table id="mysql_perf_table_lock_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	    </div>
	  </div><!--   mysql_table_tab_div -->
	  
	  <div id="mysql_perf_mutex_tbl_div" class="datatableContainer">
         <table id="mysql_perf_mutex_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	  </div>
	  <div id="mysql_perf_rwlock_tbl_div" class="datatableContainer">
         <table id="mysql_perf_rwlock_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	  </div>

	  <div id="mysql_metadata_lock_tbl_div" class="datatableContainer">
         <table id="mysql_metadata_lock_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	  </div>
      
      <div id="mysql_mem_tab_div" class="clearTabView">
        <ul>
          <li><a href="#mysql_perf_memory_summary_global_by_event_name_tbl_div" title="MySQL Performance Global Memory Allocation">Global</a></li>
          <li><a href="#mysql_perf_memory_summary_by_thread_by_event_name_tbl_div" title="MySQL Performance Thread Memory Allocation">Thread</a></li>
        </ul>           
        <div id="mysql_perf_memory_summary_global_by_event_name_tbl_div" class="datatableContainer">
    	 <span><input type="button" id="btn_mem_global" value="Refresh" title="Click to see differences"/> (Click Refresh to see changes)
                &nbsp;&nbsp; <input type="button" id="btn_mem_global_reset" value="Reset" title="Click to Restart"/>
    	 </span>
         <table id="mysql_perf_memory_summary_global_by_event_name_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
        </div>
        <div id="mysql_perf_memory_summary_by_thread_by_event_name_tbl_div" class="datatableContainer">
    	 <span>Thread ID: <input type="text" id="mem_thread_id" value="%" /> 
                &nbsp;&nbsp; <input type="button" id="btn_mem_thread" value="Refresh" title="Click to see differences"/>
                &nbsp;&nbsp; <input type="button" id="btn_mem_thread_reset" value="Reset" title="Click to Restart"/>
         </span>
         <table id="mysql_perf_memory_summary_by_thread_by_event_name_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
        </div>
      </div><!-- mysql_mem_tab_div -->
      
      <div id="top_query" class="datatableContainer">
		  <span>	
	        <span>
	          <select id="track_top_q" name="track_top_q">
	            <option value="mysql_perf_digests_wait_time">By Wait Time</option>
	            <option value="mysql_perf_digests_avg_wait_time">By Average Wait Time</option>
	            <option value="mysql_perf_digests_lock_time">By Lock Time</option>
	            <option value="mysql_perf_digests_rows_examined">By Rows Examined</option>
	            <option value="mysql_perf_digests_rows_sent">By Rows Sent</option>
	            <option value="mysql_perf_digests_tmp_disk_tables">By Tmp Disk Tables</option>
	            <option value="mysql_perf_digests_tmp_tables">By Tmp Tables</option>
	            <option value="mysql_perf_digests_select_full_join">By Select Full Join</option>
	            <option value="mysql_perf_digests_select_scan">By Select Scan</option>
	            <option value="mysql_perf_digests_sort_merge_passes">By Sort Merge Passes</option>
	            <option value="mysql_perf_digests_sort_rows">By Sort Rows</option>
	            <option value="mysql_perf_digests_no_index">By No Index</option>
	            <option value="mysql_perf_digests_no_good_index">By No Good Index</option>
	          </select>
	        </span>
	        <span>Minutes back to <select id="top_q_time" name="top_q_time" >
	                           <option value="1">1 minute</option>
	                           <option value="5">5 minutes</option>
	                           <option value="30">30 minutes</option>
	                           <option value="60">1 hour</option>
	                           <option value="240">4 hours</option>
	                           <option value="720">half day</option>
	                           <option value="1440">one day</option>
	                           <option value="52560000">All</option>
	                        </select></span>
	        <span>Records to show: <select id="top_q_records" name="top_q_records">
	                          <option value="5">5</option>
	                          <option value="10">10</option>
	                          <option value="20">20</option>
	                          <option value="50">50</option>
	                          <option value="100">100</option>
	                          <option value="200">200</option>
	                          <option value="500">500</option>
	                          <option value="1000">1000</option>
	                        </select></span>
    	    <span><input type="button" id="btn_track_top_q" value="Search"/></span>  
    	  </span>
    	 <div id="digest_tab" class="clearTabView">
    	   <ul>
    	     <li><a href="#mysql_perf_digests_tbl_div" title="Top digest summary">Digest Summary</a></li>
    	     <li><a href="#mysql_perf_digest_single_tbl_div" title="Changes for individual digest">Changes</a></li>
    	   </ul>
    	   <div id="mysql_perf_digests_tbl_div">
             <table id="mysql_perf_digests_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
           </div>
           <div id="mysql_perf_digest_single_tbl_div">
             <span>Schema Name:&nbsp;<input id="digest_schema_name"/>&nbsp;Digest:&nbsp;<input id="digest_digest" size="20" />
             &nbsp;<input type="button" id="btn_digest_refresh" value="Refresh" title="Refresh to see changes" />
             &nbsp;<input type="button" id="btn_digest_reset" value="Reset" title="Reset to retrieve summary data" />
             </span>
             <table id="mysql_perf_digest_single_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
           </div>  
         </div><!-- end of digest tab-->         
	   </div><!-- end of top query -->
        
</div>

<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>
<script language=javascript>
function messagehandler(datatable, status, message)
{
	if(status != 0)
	  reportStatus(true, "common_msg", message);
	else 
      reportStatus(false, "common_msg", "");
}
$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host');
  }
);

$('#pstabs').tabs(
 {
   activate: function( event, ui ) 
   {
     var idx = $('#pstabs').tabs("option", "active");
     if(idx <= 4)
	     mydom('ps_activity').selectedIndex = idx;
	 else if(idx >= 5 && idx <= 8)
	     mydom('ps_activity').selectedIndex =  idx + 4;   
	 else
	     mydom('ps_activity').selectedIndex =  14;   
   }   
 }
);
$('#ps_setup_tab').tabs
(
 {
   activate: function( event, ui ) 
   {
     var idx = $('#ps_setup_tab').tabs("option", "active");
     mydom('setup_activity').selectedIndex = idx;
   }   
 }
);

$('#mysql_table_tab_div').tabs
(
 {
   activate: function( event, ui ) 
   {
     var idx = $('#mysql_table_tab_div').tabs("option", "active");
     mydom('ps_activity').selectedIndex = 4 + idx;
   }   
 }
);
$('#mysql_mem_tab_div').tabs
(
{
   activate: function( event, ui ) 
   {
     var idx = $('#mysql_mem_tab_div').tabs("option", "active");
     mydom('ps_activity').selectedIndex = 12 + idx;
   }   
 }
);

$('#thread_tab').tabs();

$('#digest_tab').tabs();

var mysql_perf_threadsTable = new JSTable({
   	   name: "mysql_perf_threads",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_threads",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler, contextMenuHandler:[
   	       {key: "show_thread_wait", label: "Thread Wait Summary", handler: contextmenu_mysql_perf_threads},
   	       {key: "show_thread_stmt", label: "Thread Statement Summary", handler: contextmenu_mysql_perf_threads},	       
   	       {key: "show_thread_stage", label: "Thread Stage Summary", handler: contextmenu_mysql_perf_threads}	       
   	     ]},
   	   showRowDataOnClick:"y",
   	   formatter:{columnFormatters:{"PROCESSLIST_INFO":jqueryFormatSqlText}}
   	});//TODO formatter
   	
function contextmenu_mysql_perf_threads(obj)
{
  if(obj == null || obj.datatable == null)return;
  var thid = obj.datatable.getCellValueByColumnName(obj.row, 'THREAD_ID');
  var key = obj.key;
  mydom("thread_id").value = thid;
  mydom("stmt_thread_id").value = thid;
  mydom("stage_thread_id").value = thid;
  if(key =='show_thread_wait')
  {
    mydom('btn_mysql_perf_thread_waits_summary_reset').click();
    $('#thread_tab').tabs("option", "active", 1);
  }else if(key == 'show_thread_stmt')
  {
    mydom('btn_mysql_perf_thread_stmt_summary_reset').click();
    $('#thread_tab').tabs("option", "active", 2);
  }else
  {
    mydom('btn_mysql_perf_thread_stage_summary_reset').click();
    $('#thread_tab').tabs("option", "active", 3);
  }
}//contextmenu_mysql_perf_threads

var mysql_perf_events_waits_currentTable = new JSTable({
   	   name: "mysql_perf_events_waits_current",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_events_waits_current",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   showRowDataOnClick:"y",
   	   formatter:{columnFormatters:{"SQL_TEXT":jqueryFormatSqlText}}
   	});
    	
var mysql_perf_events_waits_summaryTable = new JSTable({
   	   name: "mysql_perf_events_waits_summary",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_events_waits_summary",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["EVENT_NAME"], valueColumns:["COUNT_STAR","WAIT_MS"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});

$('#btn_events_waits_summary').click(function()
{
  mysql_perf_events_waits_summaryTable.sendQuery();
});

var mysql_perf_thread_waits_summaryTable = new JSTable({
   	   name: "mysql_perf_thread_waits_summary",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_thread_waits_summary",
   	     paramFields:[{name:"p_1", valueField:"thread_id"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["EVENT_NAME"], valueColumns:["COUNT_STAR","WAIT_MS"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});

$('#btn_mysql_perf_thread_waits_summary').click(function()
{
  var thid = mydomval('thread_id');
  if(thid == null || thid == "")
  {
    alert("Please provide a thread_id to start.");
    mydom('thread_id').focus();
    return;    
  } 
  mysql_perf_thread_waits_summaryTable.sendQuery();
});

$('#btn_mysql_perf_thread_waits_summary_reset').click(function()
{
  var thid = mydomval('thread_id');
  if(thid == null || thid == "")
  {
    alert("Please provide a thread_id to start.");
    mydom('thread_id').focus();
    return;    
  } 
  mysql_perf_thread_waits_summaryTable.baseDataObj['mysql_perf_thread_waits_summary'] = null;
  mysql_perf_thread_waits_summaryTable.sendQuery();
});


var mysql_perf_thread_stmt_summaryTable = new JSTable({
   	   name: "mysql_perf_thread_stmt_summary",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_thread_stmt_summary",
   	     paramFields:[{name:"p_1", valueField:"stmt_thread_id"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["EVENT_NAME"], valueColumns:["COUNT_STAR","WAIT_MS", "LOCK_MS"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});

$('#btn_mysql_perf_thread_stmt_summary').click(function()
{
  var thid = mydomval('stmt_thread_id');
  if(thid == null || thid == "")
  {
    alert("Please provide a thread_id to start.");
    mydom('stmt_thread_id').focus();
    return;    
  } 
  mysql_perf_thread_stmt_summaryTable.sendQuery();
});

$('#btn_mysql_perf_thread_stmt_summary_reset').click(function()
{
  var thid = mydomval('stmt_thread_id');
  if(thid == null || thid == "")
  {
    alert("Please provide a thread_id to start.");
    mydom('stmt_thread_id').focus();
    return;    
  } 
  mysql_perf_thread_stmt_summaryTable.baseDataObj['mysql_perf_thread_stmt_summary'] = null;
  mysql_perf_thread_stmt_summaryTable.sendQuery();
});


var mysql_perf_thread_stage_summaryTable = new JSTable({
   	   name: "mysql_perf_thread_stage_summary",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_thread_stage_summary",
   	     paramFields:[{name:"p_1", valueField:"stage_thread_id"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["EVENT_NAME"], valueColumns:["COUNT_STAR","WAIT_MS"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});

$('#btn_mysql_perf_thread_stage_summary').click(function()
{
  var thid = mydomval('stage_thread_id');
  if(thid == null || thid == "")
  {
    alert("Please provide a thread_id to start.");
    mydom('stmt_thread_id').focus();
    return;    
  } 
  mysql_perf_thread_stage_summaryTable.sendQuery();
});

$('#btn_mysql_perf_thread_stage_summary_reset').click(function()
{
  var thid = mydomval('stage_thread_id');
  if(thid == null || thid == "")
  {
    alert("Please provide a thread_id to start.");
    mydom('stage_thread_id').focus();
    return;    
  } 
  mysql_perf_thread_stage_summaryTable.baseDataObj['mysql_perf_thread_stage_summary'] = null;
  mysql_perf_thread_stage_summaryTable.sendQuery();
});

var mysql_perf_objects_summaryTable = new JSTable({
   	   name: "mysql_perf_objects_summary",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_objects_summary",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   diff: {keyColumns:["NAME"], valueColumns:["COUNT_STAR","WAIT_MS"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});//TODO format number

$('#btn_mysql_perf_objects_summary').click(function()
{
  mysql_perf_objects_summaryTable.sendQuery();
});

var mysql_perf_table_io_summaryTable = new JSTable({
   	   name: "mysql_perf_table_io_summary",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_table_io_summary",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   showRowDataOnClick:"y",
   	   diff: {keyColumns:["NAME"], valueColumns:["COUNT_STAR","WAIT_MS","COUNT_READ", "READ_MS", 
   	   "COUNT_WRITE", "WRITE_MS", "COUNT_FETCH", "FETCH_MS", "COUNT_INSERT", "INSERT_MS", "COUNT_UPDATE", 
   	   "UPDATE_MS", "COUNT_DELETE", "DELETE_MS"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});//TODO format number

$('#btn_mysql_perf_table_io_summary').click(function()
{
  mysql_perf_table_io_summaryTable.sendQuery();
});

var mysql_perf_index_io_summaryTable = new JSTable({
   	   name: "mysql_perf_index_io_summary",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_index_io_summary",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   showRowDataOnClick:"y",
   	   diff: {keyColumns:["NAME"], valueColumns:["COUNT_STAR","WAIT_MS","COUNT_READ", "READ_MS", 
   	   "COUNT_WRITE", "WRITE_MS", "COUNT_FETCH", "FETCH_MS", "COUNT_INSERT", "INSERT_MS", "COUNT_UPDATE", 
   	   "UPDATE_MS", "COUNT_DELETE", "DELETE_MS"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});//TODO format number

$('#btn_mysql_perf_index_io_summary').click(function()
{
  mysql_perf_index_io_summaryTable.sendQuery();
});

var mysql_perf_filesum_instTable = new JSTable({
   	   name: "mysql_perf_filesum_inst",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_filesum_inst",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   showRowDataOnClick:"y"
   	});//TODO format number

var mysql_perf_table_lock_summaryTable = new JSTable({
   	   name: "mysql_perf_table_lock_summary",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_table_lock_summary",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'n',
   	   showRowDataOnClick:"y",
   	   diff: {keyColumns:["NAME"], valueColumns:["COUNT_STAR","WAIT_MS","COUNT_READ", "READ_MS", "COUNT_WRITE", "WRITE_MS",
   	   "COUNT_READ_NORMAL", "R_NOMRAL_MS", "COUNT_READ_WITH_SHARED_LOCKS", "R_SHARED_LOCKS_MS", "COUNT_READ_HIGH_PRIORITY", "R_HIGH_PRIORITY_MS",
   	   "COUNT_READ_NO_INSERT", "R_NO_INSERT_MS", "COUNT_READ_EXTERNAL", "R_EXTERNAL_MS", "COUNT_WRITE_ALLOW_WRITE", "W_ALLOW_W_MS",
   	   "COUNT_WRITE_CONCURRENT_INSERT", "W_CONC_INSERT_MS",  "COUNT_WRITE_LOW_PRIORITY", "W_LOW_PRIORITY_MS",
   	   "COUNT_WRITE_NORMAL","W_NORMAL_MS", "COUNT_WRITE_EXTERNAL","W_EXTERNAL_MS"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});//TODO format number


var mysql_perf_memory_summary_global_by_event_nameTable = new JSTable({
   	   name: "mysql_perf_memory_summary_global_by_event_name",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_memory_summary_global_by_event_name",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'n',
   	   showRowDataOnClick:"y",
   	   diff: {keyColumns:["EVENT_NAME"], valueColumns:["COUNT_ALLOC","COUNT_FREE","SUM_NUMBER_OF_BYTES_ALLOC", "SUM_NUMBER_OF_BYTES_FREE", 
   	   "LOW_COUNT_USED", "CURRENT_COUNT_USED", "HIGH_COUNT_USED", "LOW_NUMBER_OF_BYTES_USED", "CURRENT_NUMBER_OF_BYTES_USED", 
   	   "HIGH_NUMBER_OF_BYTES_USED"]},
   	   formatter:{commonFormatter:jqueryFormatNumber}
   	});

$('#btn_mem_global').click(function()
{
  mysql_perf_memory_summary_global_by_event_nameTable.sendQuery();
});
$('#btn_mem_global_reset').click(function()
{
  mysql_perf_memory_summary_global_by_event_nameTable.baseDataObj['mysql_perf_memory_summary_global_by_event_name'] = null;
  mysql_perf_memory_summary_global_by_event_nameTable.sendQuery();
});
var mysql_perf_memory_summary_by_thread_by_event_nameTable = new JSTable({
   	   name: "mysql_perf_memory_summary_by_thread_by_event_name",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_memory_summary_by_thread_by_event_name",
   	     paramFields:[{name:"p_1", valueField:"mem_thread_id"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'n',
   	   showRowDataOnClick:"y",
   	   diff: {keyColumns:["THREAD_ID", "EVENT_NAME"], valueColumns:["COUNT_ALLOC","COUNT_FREE","SUM_NUMBER_OF_BYTES_ALLOC", "SUM_NUMBER_OF_BYTES_FREE", 
   	   "LOW_COUNT_USED", "CURRENT_COUNT_USED", "HIGH_COUNT_USED", "LOW_NUMBER_OF_BYTES_USED", "CURRENT_NUMBER_OF_BYTES_USED", 
   	   "HIGH_NUMBER_OF_BYTES_USED"]}
   	});

$('#btn_mem_thread').click(function()
{
  mysql_perf_memory_summary_by_thread_by_event_nameTable.sendQuery();
});

$('#btn_mem_thread_reset').click(function()
{
  var thid = mydomval('mem_thread_id');
  if(thid == null || thid == "")
  {
    mydom('mem_thread_id').value = "%";
  } 
  mysql_perf_memory_summary_by_thread_by_event_nameTable.baseDataObj['mysql_perf_memory_summary_by_thread_by_event_name'] = null;
  mysql_perf_memory_summary_by_thread_by_event_nameTable.sendQuery();
});

$('#btn_mysql_perf_table_lock_summary').click(function()
{
  mysql_perf_table_lock_summaryTable.sendQuery();
});

var mysql_perf_mutexTable = new JSTable({
   	   name: "mysql_perf_mutex",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_mutex",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   showRowDataOnClick:"y"
   	});//TODO format number

var mysql_perf_rwlockTable = new JSTable({
   	   name: "mysql_perf_rwlock",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_rwlock",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   showRowDataOnClick:"y"
   	});//TODO format number

var mysql_metadata_lockTable = new JSTable({
   	   name: "mysql_metadata_lock",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_metadata_lock",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   showRowDataOnClick:"y"
   	});//TODO format number


var mysql_perf_digestsTable = new JSTable({
   	   name: "mysql_perf_digests",
   	   query:{
   	     queryURL: "query.html",
   	     sqlIdField: "track_top_q",
   	     paramFields:[{name:"p_1", valueField:"top_q_time"},{name:"p_2", valueField:"top_q_records"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler,contextMenuHandler:[
   	       {key: "digest_diff", label: "See Changes", handler: contextmenu_mysql_perf_digests}	       
   	     ]},
   	   searchable: 'y',
   	   showRowDataOnClick:"y",
   	   formatter:{commonFormatter:jqueryFormatNumber,columnFormatters:{"DIGEST_TEXT":jqueryFormatSqlText}}
   	});//TODO format number

var mysql_perf_digest_singleTable = new JSTable({
   	   name: "mysql_perf_digest_single",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_digest_single",
   	     paramFields:[{name:"p_1", valueField:"digest_schema_name"},{name:"p_2", valueField:"digest_digest"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'n',
   	   showRowDataOnClick:"n",
   	   formatter:{commonFormatter:jqueryFormatNumber},
   	   diff: {keyColumns:["NAME"], valueColumns:["VALUE"]}   	   
   	});//TODO format number
   	
function contextmenu_mysql_perf_digests(obj)
{
  if(obj == null || obj.datatable == null)return;
  var schema_name = obj.datatable.getCellValueByColumnName(obj.row, 'SCHEMA_NAME');
  var digest = obj.datatable.getCellValueByColumnName(obj.row, 'DIGEST');
  mydom('digest_schema_name').value = schema_name;
  mydom('digest_digest').value = digest;
  $('#digest_tab').tabs("option", "active", 1);
  mysql_perf_digest_singleTable.baseDataObj['mysql_perf_digest_single'] = null;
  mysql_perf_digest_singleTable.sendQuery();  
}//contextmenu_mysql_perf_digests
  	
$('#btn_digest_refresh').click(function()
{
  mysql_perf_digest_singleTable.sendQuery();
});

$('#btn_digest_reset').click(function()
{
  mysql_perf_digest_singleTable.baseDataObj['mysql_perf_digest_single'] = null;
  mysql_perf_digest_singleTable.sendQuery();
});


$('#btn_track_top_q').click(function()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  $('#digest_tab').tabs("option", "active", 0);
  mysql_perf_digestsTable.sendQuery();
});

$('#btn_ps_activity').click(function()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  var act = mydomval('ps_activity');
  if(act == 'setup')
  {
    $('#pstabs').tabs("option", "active", 0)
    mydom('setup_activity').selectedIndex=1;
	mydom('btn_setup_activity').click();
	return;
  }
  
  if(act=='threads')
  {
    mysql_perf_threadsTable.sendQuery();
    $('#pstabs').tabs("option", "active", 1);
  }
  else if(act=='eventsWaits')
  {
    mysql_perf_events_waits_currentTable.sendQuery();
    $('#pstabs').tabs("option", "active", 2);
  }
  else if(act=='eventsWaitsSummary')
  {
    mysql_perf_events_waits_summaryTable.baseDataObj['mysql_perf_events_waits_summary'] = null;
    mysql_perf_events_waits_summaryTable.sendQuery();
    $('#pstabs').tabs("option", "active", 3);
  }else if(act=='mysql_perf_objects_summary')
  {
    mysql_perf_objects_summaryTable.baseDataObj['mysql_perf_objects_summary'] = null;
    mysql_perf_objects_summaryTable.sendQuery();
    $('#pstabs').tabs("option", "active", 4);
    $('#mysql_table_tab_div').tabs("option", "active", 0);
  }else if(act=='mysql_perf_table_io_summary')
  {
    mysql_perf_table_io_summaryTable.baseDataObj['mysql_perf_table_io_summary'] = null;
    mysql_perf_table_io_summaryTable.sendQuery();
    $('#pstabs').tabs("option", "active", 4);
    $('#mysql_table_tab_div').tabs("option", "active", 1);
  }else if(act=='mysql_perf_index_io_summary')
  {
    mysql_perf_index_io_summaryTable.baseDataObj['mysql_perf_index_io_summary'] = null;
    mysql_perf_index_io_summaryTable.sendQuery();
    $('#pstabs').tabs("option", "active", 4);
    $('#mysql_table_tab_div').tabs("option", "active", 2);
  }else if(act=='mysql_perf_filesum_inst')
  {
    mysql_perf_filesum_instTable.sendQuery();
    $('#pstabs').tabs("option", "active", 4);
    $('#mysql_table_tab_div').tabs("option", "active", 3);
  }else  if(act=='mysql_perf_table_lock_summary')
  {
    mysql_perf_table_lock_summaryTable.baseDataObj['mysql_perf_table_lock_summary'] = null;
    mysql_perf_table_lock_summaryTable.sendQuery();
    $('#pstabs').tabs("option", "active", 4);    
    $('#mysql_table_tab_div').tabs("option", "active", 4);
  } else if(act=='mysql_perf_mutex')
  {
    mysql_perf_mutexTable.sendQuery();
    $('#pstabs').tabs("option", "active", 5); 
  }else if(act=='mysql_perf_rwlock')
  {
    mysql_perf_rwlockTable.sendQuery();
    $('#pstabs').tabs("option", "active", 6); 
  }else if(act=='mysql_metadata_lock')
  {
    mysql_metadata_lockTable.sendQuery();
    $('#pstabs').tabs("option", "active", 7); 
  }else if(act=='mysql_perf_memory_summary_global_by_event_name')
  {
    mysql_perf_memory_summary_global_by_event_nameTable.baseDataObj['mysql_perf_memory_summary_global_by_event_name'] = null;
    mysql_perf_memory_summary_global_by_event_nameTable.sendQuery();
    $('#pstabs').tabs("option", "active", 8); 
    $('#mysql_mem_tab_div').tabs("option", "active", 0);
  }else if(act=='mysql_perf_memory_summary_by_thread_by_event_name')
  {
    mysql_perf_memory_summary_by_thread_by_event_nameTable.baseDataObj['mysql_perf_memory_summary_by_thread_by_event_name'] = null;
    mysql_perf_memory_summary_by_thread_by_event_nameTable.sendQuery();
    $('#pstabs').tabs("option", "active", 8); 
    $('#mysql_mem_tab_div').tabs("option", "active", 1);
  }
  else if(act=='digest')
  {
  	mysql_perf_digestsTable.sendQuery();
    $('#pstabs').tabs("option", "active", 9);
    $('#digest_tab').tabs("option", "active", 0);
  }
  
});


//	function formatPSTimer(o)
//	{
//	    if((o.column.key.indexOf('_TIME')<0)||o.value==null)
//	      return formatNum(o);
//       var val = eval((o.value/1000000000).toFixed(3));
//       return  Y.DataType.Number.format(val, {thousandsSeparator:","}) + " ms"; 
//	};
		
//	function formatNum(o)
//	{
//		if(o.column.key=='DIGEST_TEXT')
///			return formatTruncDigestText100(o);
//		if(!Y.Lang.isNumber(o.value))				
//			return o.value;
//		return Y.DataType.Number.format(o.value, {thousandsSeparator:","});
//	}

var mysql_perf_setup_timersTable = new JSTable({
   	   name: "mysql_perf_setup_timers",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_setup_timers",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'n',
   	   showRowDataOnClick:"y"
   	});//TODO format number

var mysql_perf_setup_instrumentsTable = new JSTable({
   	   name: "mysql_perf_setup_instruments",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_setup_instruments",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   showRowDataOnClick:"y"
   	});//TODO format number

var mysql_perf_setup_consumersTable = new JSTable({
   	   name: "mysql_perf_setup_consumers",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_setup_consumers",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'n',
   	   showRowDataOnClick:"y"
   	});//TODO format number

var mysql_perf_setup_threadsTable = new JSTable({
   	   name: "mysql_perf_setup_threads",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_setup_threads",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'y',
   	   showRowDataOnClick:"y"
   	});//TODO format number

var mysql_perf_setup_actorsTable = new JSTable({
   	   name: "mysql_perf_setup_actors",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_setup_actors",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'n',
   	   showRowDataOnClick:"y"
   	});//TODO format number

var mysql_perf_setup_objectsTable = new JSTable({
   	   name: "mysql_perf_setup_objects",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_perf_setup_objects",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   searchable: 'n',
   	   showRowDataOnClick:"y"
   	});//TODO format number

$('#btn_setup_activity').click(function()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  
  var act = mydomval('setup_activity');

  if(act=='mysql_perf_setup_timers')
  {
    mysql_perf_setup_timersTable.sendQuery();
    $('#ps_setup_tab').tabs("option", "active", 0);
  }else if(act=='mysql_perf_setup_instruments')
  {
    mysql_perf_setup_instrumentsTable.sendQuery();
    $('#ps_setup_tab').tabs("option", "active", 1);
  }else if(act=='mysql_perf_setup_consumers')
  {
    mysql_perf_setup_consumersTable.sendQuery();
    $('#ps_setup_tab').tabs("option", "active", 2);
  }else if(act=='mysql_perf_setup_threads')
  {
    mysql_perf_setup_threadsTable.sendQuery();
    $('#ps_setup_tab').tabs("option", "active", 3);
  }else if(act=='mysql_perf_setup_actors')
  {
    mysql_perf_setup_actorsTable.sendQuery();
    $('#ps_setup_tab').tabs("option", "active", 4);
  }else if(act=='mysql_perf_setup_objects')
  {
    mysql_perf_setup_objectsTable.sendQuery();
    $('#ps_setup_tab').tabs("option", "active", 5);
  }
});
</script>
</body>
</html>