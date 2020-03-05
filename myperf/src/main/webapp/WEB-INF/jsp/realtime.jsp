<%@page trimDirectiveWhitespaces="true"%>
<%@page import="com.yahoo.dba.perf.myperf.common.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Real Time Tracking</title>
<link rel="stylesheet" href="jquery/css/zTreeStyle/zTreeStyle.css" type="text/css" />
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
</head>
<body>
<div><!-- db list --> 
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

        <select name="session_activity" id="session_activity">
          <option value="processlist">Process List</option>
          <option value="global_status">Global Status</option>
          <option value="global_variables">Global Variable</option>
          <option value="vardiff">Global Variable - Diff</option>
          <option value="mysql_repl_master">Replication - Master</option>          
          <option value="mysql_repl_slave">Replication - Slave</option>          
          <option value="mysql_repl_show">Replication - Topology</option>
          <option value="mysql_repl_mts">Replication - MTS Status</option>
          <option value="innodbStatus">InnoDB Engine Status</option>          
          <option value="mysql_innodb_trx">InnoDB Transactions</option>          
          <option value="mysql_innodb_mutex">InnoDB Mutext</option>          
          <option value="mysql_innodb_locks">InnoDB Locks</option>          
          <option value="mysql_innodb_buffer_pool_status">InnoDB Buffer pool Statistics</option>          
          <option value="mysql_innodb_metrics">InnoDB Metrics</option>          
          <option value="mysql_user_statistics">User Statistics</option>          
          <option value="mysql_user_time">User Time</option>          
          <option value="mysql_client_statistics">Client Statistics</option>          
          <option value="mysql_client_conn_statistics">Connection Statistics</option>          
          <option value="mysql_table_statistics">Table Statistics</option>          
          <option value="mysql_index_statistics">Index Statistics</option>          
        </select>
        <input type="button" id="btn_session_activity" value="&#9658;"/>
   </span>
</div><!-- end of dblist -->

<span id="common_msg"> <!-- common message line -->
<c:if test="${mydbSize==0}">You have not provided any database credential yet. Please use <a href="<%= request.getContextPath() %>/db.htm">DB Credential</a> page to provide access information for databases you are interested in.</c:if>
</span>

<div id="sessiontab" class="clearTabView"> <!-- tab pane to display real time info in tabular formats -->
    <ul>
        <li><a href="#mysql_processlist_tbl_div" title="MySQL process list">Processes</a></li>
        <li><a href="#mysql_global_status_metrics_tbl_div" title="MySQL global status. Use Refresh button to see delta changes.">Global Status</a></li>
        <li><a href="#mysql_global_variables_tbl_div" title="MySQL global variables.">Global Variables</a></li>
        <li><a href="#vardiff_tbl_div" title="MySQL global variables.">Variable Diffs</a></li>
        <li><a href="#repl_tab" title="MySQL Replication Status.">Replication</a></li>
        <li><a href="#inno_status_tbl_tab" title="MySQL InnoDB engine status.">InnoDB Engine Status</a></li>
        <li><a href="#innodbtab" title="MySQL InnoDB statistics.">InnoDB Statistics</a></li>
        <li><a href="#userstatstab" title="MySQL User statistics from Percona and MariaDB, etc.">User Stats</a></li>
        <li><a href="#plan_tbl_div" title="MySQL Explain Plan.">Explain Plan</a></li>
    </ul>
    <div id="mysql_processlist_tbl_div">
      <span><input type="checkbox" value="y" id="chk_proclist_filter" name="chk_proclist_filter" checked/><input type="hidden" id="proclist_sql" name="proclist_sql" value="mysql_active_processlist"/> Only Show Active Processes</span>
      <table id="mysql_processlist_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    </div><!-- process list -->
    
    <div id="mysql_global_status_metrics_tbl_div">
    	 <span>
    	   <input type="button" id="btn_sys_stat" value="Refresh" title="Click to see differences"/> 
    	   &nbsp;<input type="checkbox" value="1" id="btn_sys_stat_auto"  title="Check to start auto refresh, uncheck to stop"/> Auto
    	   &nbsp;<select id="sys_stat_auto_rate">
    	       <option value="5">5 sec</option>
    	       <option value="10">10 sec</option>
    	       <option value="15">15 sec</option>
    	       <option value="30">30 sec</option>
    	       <option value="60">60 sec</option>
    	       <option value="120">120 sec</option>
    	   </select>
    	   &nbsp;Keyword: <input type="text" id="status_keyword" onchange="handleMetricsFilter();" title="Filtered metrics by keyword"/>
    	                  <input type="hidden" id="prev_status_keyword" />
    	   &nbsp;<input type="button" id="btn_sys_stat_reset" value="Restart" title="Click to Restart"/> 
    	 </span>
      <table id="mysql_global_status_metrics_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    </div><!-- global status -->
    
    <div id="mysql_global_variables_tbl_div">
    	 <span>Filter by: <input type="text" id="variable_keyword" name="variable_keyword" />&nbsp;
    	 <input type="button" id="btn_variable" value="Search" title="Search global variable sesstings"/>&nbsp;
    	 <input type="button" id="btn_variable_hist" value="History" title="global variable sessting changess"/>
    	 </span>
      <table id="mysql_global_variables_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    </div><!-- global variable -->
    
    <div id="vardiff_tbl_div">
    	 <span>Compare To: DB Group <input type="text" id="id2"  name="id2" />&nbsp; Host <input type="text" id="host2"  name="host2" /> 
    	 <input type="button" value="Find" onclick="prepareDBSearch('id2','host2',mydomval('dbgroup'));"/>&nbsp; <input type="button" id="btn_vardiff" value="Compare" title="Compare configuration Differences"/></span>
         <table id="vardiff_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    </div><!-- global variable comparison -->
    
    <div id="repl_tab" class="clearTabView">
    	<ul>
    		<li><a href="#mysql_repl_master_tbl_div" title="SHOW MASTER STATUS">Master Status</a></li>
    		<li><a href="#mysql_repl_slave_tbl_div" title="SHOW SLAVE STATUS + SHOW MASTER STATUS FROM MASTER">Slave Status</a></li>
    		<li><a href="#mysql_repl_show_tbl_div" title="Replication topology and status">Replication Topology</a></li>
    		<li><a href="#mysql_repl_mts_tbl_div" title="Multi Thread replication status">MTS Status</a></li>
    	</ul>
    	<div id="mysql_repl_master_tbl_div">
         	<table id="mysql_repl_master_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	    </div><!-- repl master -->

    	<div id="mysql_repl_slave_tbl_div">
        	 <table id="mysql_repl_slave_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    	</div><!-- mysql_repl_slave -->

    	<div id="mysql_repl_show_tbl_div">
        	 <table id="mysql_repl_show_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    	</div><!-- mysql_repl_show -->

    	<div id="mysql_repl_mts_tbl_div">
        	 <table id="mysql_repl_mts_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    	</div><!-- mysql_repl_mts -->

    </div><!-- end of repl tab -->
    
    <div id="inno_status_tbl_tab" class="clearTabView">
      <ul>
        <li><a href="#inno_status_summary_tbl_div" title="MySQL Innodb Engine Status Summary">Summary</a></li>
        <li><a href="#inno_status_file_io_tbl_div" title="MySQL Innodb Engine Status File IO">File IO</a></li>
        <li><a href="#inno_status_buffer_pool_tbl_div" title="MySQL Innodb Engine Status Buffer pool and memory">Buffer Pool and Memory</a></li>
        <li><a href="#inno_status_row_operations_tbl_div" title="MySQL Innodb Engine Status Row Operations">Row Operations</a></li>
        <li><a href="#inno_status_semaphores_tbl_div" title="MySQL Innodb Engine Status Semaphores">Semaphores</a></li>
        <li><a href="#inno_status_txs_tbl_div" title="MySQL Innodb Engine Status Transactions">Transactions</a></li>
        <li><a href="#inno_status_deadlocks_tbl_div" title="MySQL Innodb Engine Status Deadlocks">Deadlocks</a></li>
        <li><a href="#inno_status_log_tbl_div" title="MySQL Innodb Engine Status Logs">Log</a></li>
        <li><a href="#inno_status_ibuf_tbl_div" title="MySQL Innodb Engine Status Insert Buffer">Insert Buffer</a></li>
      </ul>
      <div id="inno_status_summary_tbl_div">
         <table id="inno_status_summary_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="inno_status_file_io_tbl_div">
         <table id="inno_status_file_io_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="inno_status_buffer_pool_tbl_div">
         <table id="inno_status_buffer_pool_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="inno_status_row_operations_tbl_div">
         <table id="inno_status_row_operations_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="inno_status_semaphores_tbl_div">
         <table id="inno_status_semaphores_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	     <br />
	     <table id="inno_status_semap_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	  </div>
      <div id="inno_status_txs_tbl_div">
         <table id="inno_status_txs_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="inno_status_deadlocks_tbl_div">
         <table id="inno_status_deadlocks_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="inno_status_log_tbl_div">
         <table id="inno_status_log_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="inno_status_ibuf_tbl_div">
         <table id="inno_status_ibuf_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
    </div><!-- end iof inno_status_tbl_tab inno db engine status -->
    
    <div id="innodbtab" class="clearTabView">
      <ul>
        <li><a href="#mysql_innodb_trx_tbl_div" title="MySQL InnoDB Transactions.">Transactions</a></li>        
        <li><a href="#mysql_innodb_mutex_tbl_div" title="MySQL InnoDB Mutex.">Mutex</a></li>        
        <li><a href="#mysql_innodb_locks_tbl_div" title="MySQL InnoDB Locks.">Locks</a></li>
        <li><a href="#mysql_innodb_buffer_pool_status_tbl_div" title="MySQL InnoDB Bufffer pool Statistics.">Buffer Pool Statistics</a></li>
        <li><a href="#mysql_innodb_metrics_tbl_div" title="MySQL InnoDB Metrics.">InnoDB Metrics</a></li>        
      </ul>
      <div id="mysql_innodb_trx_tbl_div">
         <table id="mysql_innodb_trx_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="mysql_innodb_mutex_tbl_div">
        <span><input type="button" id="btn_innodb_mutex" value="Refresh" title="Click to see differences"/> (Click Refresh to see changes)</span>
         <table id="mysql_innodb_mutex_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
             <div>
               <div id="mutex_notes_handle" class="box"  onclick="toggleDiv('mutex_notes');">+</div>&nbsp;Notes:
               <div id="mutex_notes" style="display: none;">
               MySQL uses two-step approach to get a lock on a mutex. A thread first tries to lock the mutex. 
               If the mutex is locked by other thread, the requesting thread will do a spin wait (just like Oracle latch, to save context switches). 
               If this does not work for a while, it will go to sleep until the mutex is free (wait on condition array/variable). See also InnoDB Engine Status - Semaphores.
               <dl>
                 <li>spin waits: the number of times a thread tried to get a mutext and it wasn't available, so it waited in a spin-wait.</li>
                 <li>rounds: the number of times threads looped in the spin-wait cycle, checking the mutex. innodb_sync_spin_loops variable (default 20) controlls how many rounds per spin-wait.</li>
                 <li>OS wait: the number of times the thread gave up spin-waiting and went to sleep.</li>
               </dl>
              </div>
             </div>
      </div>
      <div id="mysql_innodb_locks_tbl_div">
         <table id="mysql_innodb_locks_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="mysql_innodb_buffer_pool_status_tbl_div">
         <table id="mysql_innodb_buffer_pool_status_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="mysql_innodb_metrics_tbl_div">
      	     <span><input type="button" id="btn_innodb_metrics" value="Refresh" title="Click to see differences"/> (Click Refresh to see changes)</span>
         <table id="mysql_innodb_metrics_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div><!-- innodb_metrics -->  
    </div><!-- innodbtab -->
    
    <div id="userstatstab" class="clearTabView">
      <ul>
        <li><a href="#mysql_user_statistics_tbl_div" title="User Statistics">User Statistics</a></li>        
        <li><a href="#mysql_user_time_tbl_div" title="User Time">User Time</a></li>        
        <li><a href="#mysql_client_statistics_tbl_div" title="Client Statistics">Client Statistics</a></li>        
        <li><a href="#mysql_client_conn_statistics_tbl_div" title="Client Connection Statistics">Connection Statistics</a></li>
        <li><a href="#mysql_table_statistics_tbl_div" title="User Statistics">Table Statistics</a></li>        
        <li><a href="#mysql_index_statistics_tbl_div" title="Index Statistics">Index Statistics</a></li>        
      </ul>
      <div id="mysql_user_statistics_tbl_div">
         <table id="mysql_user_statistics_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="mysql_user_time_tbl_div">
	     <span><input type="button" id="btn_mysql_user_time" value="Refresh" title="Click to see changes"/> (Click Refresh to see changes)</span>
         <table id="mysql_user_time_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	  </div>
	  <div id="mysql_client_statistics_tbl_div">
         <table id="mysql_client_statistics_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
	  <div id="mysql_client_conn_statistics_tbl_div">
		 <span><input type="button" id="btn_mysql_client_conn_statistics" value="Refresh" title="Click to see changes"/> (Click Refresh to see changes)</span>
	     <table id="mysql_client_conn_statistics_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      </div>
      <div id="mysql_table_statistics_tbl_div">
		 <span><input type="button" id="btn_mysql_table_statistics" value="Refresh" title="Click to see changes"/> (Click Refresh to see changes)</span>
	     <table id="mysql_table_statistics_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	  </div>
	  <div id="mysql_index_statistics_tbl_div">
	     <span><input type="button" id="btn_mysql_index_statistics" value="Refresh" title="Click to see changes"/> Schema/DB: <input type="text" name="index_stats_schema" id="index_stats_schema" title="use % as wildcard" value="%"/>&nbsp;Table: <input type="text" name="index_stats_table" id="index_stats_table" title="use % as wildcard" value="%"/></span>
	     <table id="mysql_index_statistics_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	  </div>
    </div><!-- userstatstab -->
    
    <div id="plan_tbl_div">
      <div>
        <div>Database &nbsp; <input type="text" name="plan_dbname" id="plan_dbname" /></div>
        <div style="margin-top:5px;"><span>Query Text:</span><br />
             <textarea row="10" cols="50" id="plan_sql" name="sqltext" style="width:700px;height:100px;margin-top:5px;margin-bottom:5px;"></textarea> 
        </div>    
        <div><input type="button" id="btn_explain" name="btn_explain" value="Explain Plan"/>&nbsp;<input type="button" id="btn_explain_json" name="btn_explain_json" value="Explain JSON" title="Use JSON format, MySQL5.6 and up" />&nbsp;<span id="explain_status" style="color:red;"></span></div>
      </div>  
	  <table id="plan_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
      
      <div id="json_plan">
         <div id="json_plan_tree" >
              <ul id="json_plan_ztree" class="ztree"></ul>
          </div>
          <script type="text/javascript" src="jquery/js/jquery.ztree.core-3.5.min.js"></script>
          <script language="javascript">
               var zplanTreeObj;
   			   var zplanSetting = {view:{nameIsHTML: true}};
  		  </script>
          <!-- pre id="json_plan_tree"></pre -->
       </div><!-- end of json plan -->
    </div><!-- explain plan -->
</div><!-- end of all tabls -->

<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>

<script language="javascript">
$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host');
  }
);

function toggleDiv(div, display)
{
    var d = mydom(div);
    var img = mydom(div+"_handle");
    var show = (d.style.display=='none'||display=='block');
    d.style.display = show?'block':'none';
    //img.innerText = show?'-':'+';
    $('#'+div+'_handle').text(show?'-':'+');
}

function messagehandler(datatable, status, message)
{
	if(status != 0)
	  reportStatus(true, "common_msg", message);
	else 
      reportStatus(false, "common_msg", "");
}

var topTabTableMapping = {};//use the value from select list to find tab
var topTabActionMapping = {};//When top tab changes, mapping the select list index
var replStatusTabActionMapping = {};//used to mapping the top action select list
var innoStatusTabActionMapping = {};//used to mapping the top action select list
var userStatusTabActionMapping = {};//used to mapping the top action select list

$('#sessiontab').tabs({
   activate: function( event, ui ) 
   {
     var idx = $('#sessiontab').tabs("option", "active");
     if(topTabActionMapping[idx] != null)
       mydom("session_activity").selectedIndex = topTabActionMapping[idx].index;
   }   
 });  

$('#repl_tab').tabs({
   activate: function( event, ui ) 
   {
     var idx = $('#repl_tab').tabs("option", "active");
     if(replStatusTabActionMapping[idx] != null)
       mydom("session_activity").selectedIndex = replStatusTabActionMapping[idx].index;
   }   
});
 
$('#inno_status_tbl_tab').tabs();
$('#innodbtab').tabs({
   activate: function( event, ui ) 
   {
     var idx = $('#innodbtab').tabs("option", "active");
     if(innoStatusTabActionMapping[idx] != null)
       mydom("session_activity").selectedIndex = innoStatusTabActionMapping[idx].index;
   }   
});
$('#userstatstab').tabs({
   activate: function( event, ui ) 
   {
     var idx = $('#userstatstab').tabs("option", "active");
     if(userStatusTabActionMapping[idx] != null)
       mydom("session_activity").selectedIndex = userStatusTabActionMapping[idx].index;
   }   
});

function jqueryStylingProcessList(obj)
{
    if(obj.datatable == null)return;
    var t = eval(obj.data["TIME"]), u = obj.data["USER"], cmd = obj.data["COMMAND"];    
    if(t>=10 && u!='system user' && u!='event_scheduler'
      && cmd!='Sleep' && cmd!='Binlog Dump' && cmd!='Connect'
      && cmd!='Daemon' && cmd!='Binlog Dump GTID') //mark red if passed 10 sec
      $(obj.row).addClass('hilitecell');
}

var mysql_processlistTable = new JSTable({
   	   name: "mysql_processlist",
   	   query:{
   	     queryURL: "query.html",
   	     sqlIdField: "proclist_sql",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler,
   	        contextMenuHandler:[
   	       {key: "show_explain", label: "Explain", handler: contextmenu_mysql_processlist}   	       
   	     ]},
   	   tooltipCallbackOnClick: processlistTermCB,
   	   formatter:{rowFormatter: jqueryStylingProcessList, columnFormatters:{"INFO":jqueryFormatSqlText}}
   	});//TODO formatter

topTabTableMapping["processlist"] = {table:mysql_processlistTable, tab:0};
topTabActionMapping[0] = {table:mysql_processlistTable, index:0};

function contextmenu_mysql_processlist(obj)
{
  if(obj == null || obj.datatable == null)return;
  var dbname = obj.datatable.getCellValueByColumnName(obj.row, 'DB');
  var sql = obj.datatable.getCellValueByColumnName(obj.row, 'INFO');
  var isSel = dbname!=null && dbname!='' && isSelect(sql);
  
  if(!isSel)
  {
    alert("'Explain' is currently only supported for SELECT statement."); 
    return;
  }
  mydom("plan_dbname").value = dbname;
  mydom("plan_sql").value = sql;
  //TODO tab selection
  mydom('btn_explain').click();
  $('#sessiontab').tabs("option", "active", 8);
}//contextmenu_mysql_processlist


var mysql_global_status_metricsTable = new JSTable({
   	   name: "mysql_global_status_metrics",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_show_global_status_ps",
   	     paramFields:[{name:"p_1", valueField:"status_keyword"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   tooltipCallbackOnClick: statTermCB,
   	   diff: {keyColumns:["VARIABLE_NAME"], valueColumns:["VARIABLE_VALUE"]}
   	});//TODO formatter

topTabTableMapping["global_status"] = {table: mysql_global_status_metricsTable, tab:1};
topTabActionMapping[1] = {table: mysql_global_status_metricsTable, index:1};

var mysql_global_variablesTable = new JSTable({
   	   name: "mysql_global_variables",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_global_variables",
   	     paramFields:[{name:"p_1", valueField:"variable_keyword"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   formatter:{columnFormatters:{"VARIABLE_NAME":jqueryVariableName, "VARIABLE_VALUE":jqueryVariableValue}}
   	});//TODO formatter

topTabTableMapping["global_variables"] = {table: mysql_global_variablesTable, tab:2};
topTabActionMapping[2] = {table: mysql_global_variablesTable, index:2};

var varhistoryTable = new JSTable({
   	   name: "mysql_global_variables",
   	   query:{
   	     queryURL: "varhistory.html",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   formatter:{columnFormatters:{"VARIABLE_NAME":jqueryVariableName, "VARIABLE_VALUE":jqueryVariableValue, 
   	   "COMMENTS":jqueryVariableValue}}
   	});//TODO formatter

$('#btn_variable_hist').click(function()
{
 if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
 if(mysql_global_variablesTable.datatable["mysql_global_variables"] != null)
 {
   mysql_global_variablesTable.datatable["mysql_global_variables"].destroy();
   $('#mysql_global_variables_tbl').empty();
   mysql_global_variablesTable.datatable["mysql_global_variables"] = null;
 }    
 varhistoryTable.sendQuery();   
});    

$('#btn_variable').click(function()
{
 if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
 if(varhistoryTable.datatable["mysql_global_variables"] != null)
 {
   varhistoryTable.datatable["mysql_global_variables"].destroy();
   $('#mysql_global_variables_tbl').empty();
   varhistoryTable.datatable["mysql_global_variables"] = null;
 }    
 mysql_global_variablesTable.sendQuery();   
});    

function handleMetricsFilter()
{
  	var oldV = mydomval("prev_status_keyword");
  	var newV = mydomval("status_keyword");
  	mydom("prev_status_keyword").value = newV;
  	//if new val is  stricter, do nothing
  	if(newV!=null && newV.length>0 && (oldV==null||oldV.length==0||newV.indexOf(oldV)>=0))
  	   return;
  	else
  	  mydom("btn_sys_stat_reset").click();
}
  
var vardiffTable = new JSTable({
   	   name: "vardiff",
   	   query:{
   	     queryURL: "vardiff.html",
   	     sqlId: "mysql_global_variables",
   	     paramFields:[{name:"p_1", valueField:"id2"},{name:"p_2", valueField:"host2"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   formatter:{columnFormatters:{"VARIABLE_NAME":jqueryVariableName, "DB1":jqueryVariableValue, "DB2":jqueryVariableValue}}
   	});//TODO formatter

$('#btn_vardiff').click(function()
{
 if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
 if(!checkDBSelection(mydomval("id2"), host=mydomval("host2")))
    return;
  vardiffTable.sendQuery();
});

topTabTableMapping["vardiff"] = {table: vardiffTable, tab:3};
topTabActionMapping[3] = {table: vardiffTable, index:3};

var mysql_repl_masterTable = new JSTable({
   	   name: "mysql_repl_master",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_repl_master",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter

topTabTableMapping["mysql_repl_master"] = {table: mysql_repl_masterTable, tab:4, subtab:"repl_tab", subidx:0};
topTabActionMapping[4] = {table: mysql_repl_masterTable, index:4};
replStatusTabActionMapping[0] = {table: mysql_repl_masterTable, index:4};

var mysql_repl_slaveTable = new JSTable({
   	   name: "mysql_repl_slave",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_repl_lag",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   formatter:{rowFormatter:jqueryStylingSlaveStatus},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter

topTabTableMapping["mysql_repl_slave"] = {table: mysql_repl_slaveTable, tab:4, subtab:"repl_tab", subidx:1};
replStatusTabActionMapping[1] = {table: mysql_repl_slaveTable, index:5};

var mysql_repl_showTable = new JSTable({
   	   name: "mysql_repl_show",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_repl_show",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter

topTabTableMapping["mysql_repl_show"] = {table: mysql_repl_showTable, tab:4, subtab:"repl_tab", subidx:2};
replStatusTabActionMapping[2] = {table: mysql_repl_showTable, index:6};

var mysql_repl_mtsTable = new JSTable({
   	   name: "mysql_repl_mts",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_repl_mts",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter

topTabTableMapping["mysql_repl_mts"] = {table: mysql_repl_mtsTable, tab:4, subtab:"repl_tab", subidx:3};
replStatusTabActionMapping[3] = {table: mysql_repl_mtsTable, index:7};

function jqueryStylingSlaveStatus(obj)
{
  var name = obj.data["NAME"]
  var value = obj.data["VALUE"]
  if((name=='Slave_IO_Running' && value!='Yes')
	|| (name=='Slave_SQL_Running' && value!='Yes')
	|| (name=='Seconds_Behind_Master' && value >= 60 ))
      $(obj.row).addClass('hilitecell');
}  

var innodbEngineStatusTable = new JSTable({
   	   name: ['inno_status_summary','inno_status_file_io','inno_status_buffer_pool','inno_status_row_operations',
			'inno_status_semaphores','inno_status_semap','inno_status_txs','inno_status_deadlocks',
			'inno_status_log','inno_status_ibuf'
			],
   	   query:{
   	     queryURL: "inno.html",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   formatter:{columnFormatters:{"inno_status_txs.SQL":jqueryFormatSqlText, "inno_status_txs.LOCKS":jqueryFormatSqlText}},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter
topTabTableMapping["innodbStatus"] = {table: innodbEngineStatusTable, tab:5};
topTabActionMapping[5] = {table: innodbEngineStatusTable, index:8};

var mysql_innodb_trxTable = new JSTable({
   	   name: "mysql_innodb_trx",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_innodb_trx",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{columnFormatters:{"SQL":jqueryFormatSqlText, "LOCKS":jqueryFormatSqlText}},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter

topTabTableMapping["mysql_innodb_trx"] = {table: mysql_innodb_trxTable, tab:6, subtab:"innodbtab", subidx:0};
topTabActionMapping[6] = {table: mysql_innodb_trxTable, index:9};
innoStatusTabActionMapping[0] = {table: mysql_innodb_trxTable, index:9};

var mysql_innodb_mutexTable = new JSTable({
   	   name: "mysql_innodb_mutex",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_innodb_mutex",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["NAME"], valueColumns:["VALUE", "COUNT"]}
   	});//TODO formatter

topTabTableMapping["mysql_innodb_mutex"] = {table: mysql_innodb_mutexTable, tab:6, subtab:"innodbtab", subidx:1};
innoStatusTabActionMapping[1] = {table: mysql_innodb_mutexTable, index:10};

var mysql_innodb_locksTable = new JSTable({
   	   name: "mysql_innodb_locks",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_innodb_locks",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter

topTabTableMapping["mysql_innodb_locks"] = {table: mysql_innodb_locksTable, tab:6, subtab:"innodbtab", subidx:2};
innoStatusTabActionMapping[2] = {table: mysql_innodb_locksTable, index:11};

var mysql_innodb_buffer_pool_statusTable = new JSTable({
   	   name: "mysql_innodb_buffer_pool_status",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_innodb_buffer_pool_status",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter

topTabTableMapping["mysql_innodb_buffer_pool_status"] = {table: mysql_innodb_buffer_pool_statusTable, tab:6, subtab:"innodbtab", subidx:3};
innoStatusTabActionMapping[3] = {table: mysql_innodb_buffer_pool_statusTable, index:12};

var mysql_innodb_metricsTable = new JSTable({
   	   name: "mysql_innodb_metrics",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_innodb_metrics",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["NAME"], valueColumns:["COUNT"]}
   	});//TODO formatter

topTabTableMapping["mysql_innodb_metrics"] = {table: mysql_innodb_metricsTable, tab:6, subtab:"innodbtab", subidx:4};
innoStatusTabActionMapping[4] = {table: mysql_innodb_metricsTable, index:13};

var mysql_user_statisticsTable = new JSTable({
   	   name: "mysql_user_statistics",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_user_statistics",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter
topTabTableMapping["mysql_user_statistics"] = {table: mysql_user_statisticsTable, tab:7, subtab:"userstatstab", subidx:0};
topTabActionMapping[7] = {table: mysql_user_statisticsTable, index:14};
userStatusTabActionMapping[0] = {table: mysql_user_statisticsTable, index:14};

var mysql_user_timeTable = new JSTable({
   	   name: "mysql_user_time",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_user_time",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["USER"], valueColumns:["TOTAL_CONNECTIONS","CONNECTED_TIME", "BUSY_TIME", "CPU_TIME"]}
   	});//TODO formatter
topTabTableMapping["mysql_user_time"] = {table: mysql_user_timeTable, tab:7, subtab:"userstatstab", subidx:1};
userStatusTabActionMapping[1] = {table: mysql_user_timeTable, index:15};

var mysql_client_statisticsTable = new JSTable({
   	   name: "mysql_client_statistics",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_client_statistics",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler}
   	});//TODO formatter
topTabTableMapping["mysql_client_statistics"] = {table: mysql_client_statisticsTable, tab:7, subtab:"userstatstab", subidx:2};
userStatusTabActionMapping[2] = {table: mysql_client_statisticsTable, index:16};

var mysql_client_conn_statisticsTable = new JSTable({
   	   name: "mysql_client_conn_statistics",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_client_conn_statistics",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["CLIENT"], valueColumns:["TOTAL_CONNECTIONS", "DENIED_CONNECTIONS", "LOST_CONNECTIONS","CONNECTED_TIME"]}
   	});//TODO formatter
topTabTableMapping["mysql_client_conn_statistics"] = {table: mysql_client_conn_statisticsTable, tab:7, subtab:"userstatstab", subidx:3};
userStatusTabActionMapping[3] = {table: mysql_client_conn_statisticsTable, index:17};

var mysql_table_statisticsTable = new JSTable({
   	   name: "mysql_table_statistics",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_table_statistics",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["TABLE_NAME"], valueColumns:["ROWS_READ","ROWS_CHANGED", "ROWS_CHANGED_X_INDEXES"]}
   	});//TODO formatter
topTabTableMapping["mysql_table_statistics"] = {table: mysql_table_statisticsTable, tab:7, subtab:"userstatstab", subidx:4};
userStatusTabActionMapping[4] = {table: mysql_table_statisticsTable, index:18};

var mysql_index_statisticsTable = new JSTable({
   	   name: "mysql_index_statistics",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_index_statistics",
   	     paramFields:[{name:"p_1", valueField:"index_stats_schema"},{name:"p_2", valueField:"index_stats_table"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
	   formatter:{},
   	   handlers: {jquery:1,selectHandler: selectRow_mysql_index_statistics,statusMessageHandler:messagehandler},
   	   diff: {keyColumns:["INDEX_NAME"], valueColumns:["ROWS_READ"]}
   	});//TODO formatter
topTabTableMapping["mysql_index_statistics"] = {table: mysql_index_statisticsTable, tab:7, subtab:"userstatstab", subidx:5};
userStatusTabActionMapping[5] = {table: mysql_index_statisticsTable, index:19};

function selectRow_mysql_index_statistics(obj)
{
  if(obj == null || obj.datatable == null)return;
  var indexName = obj.datatable.getCellValueByColumnName(obj.row, 'INDEX_NAME');
  if(indexName!=null)
  {
    var idx = indexName.indexOf(".");
	if(idx>0)
	{
	  mydom("index_stats_schema").value = indexName.substring(0,idx);
	  indexName = indexName.substring(idx+1);
	  idx = indexName.indexOf(".");
	  if(idx>0)
	      mydom("index_stats_table").value = indexName.substring(0,idx);
	}
  }	
 }

var planTable = new JSTable({
   	   name: "plan",   	   
   	   query:{
   	     queryURL: "profile.html",
   	     formAction: "POST",
   	     paramFields:[{name:"plan", value:"y"},{name:"dbname", valueField:"plan_dbname"}, 
   	     {name:"sqltext", valueField:"plan_sql", enc:encodeURIComponent}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   tooltipCallbackOnClick: planTermCB,
   	   formatter:{commonFormatter:jqueryFormatPreserveSpace, columnFormatters:{"POSSIBLE_KEYS":jqueryFormatText60}}
   	});
  
//major trigger
$('#btn_session_activity').click(function()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  var act = mydomval('session_activity');
  var mapping = topTabTableMapping[act];
  if(mapping == null)
  {
    alert("Cannot find table entry for "+act); 
    return;
  }
  $('#sessiontab').tabs("option", "active", mapping.tab);
  if(mapping.subtab!=null && mapping.subidx!=null)
    $('#'+mapping.subtab).tabs("option", "active", mapping.subidx);
  if(mapping.table.diff != null)
    mapping.table.baseDataObj[mapping.table.name] = null;
  if(act =='global_variables')
  {
    if(varhistoryTable.datatable["mysql_global_variables"] != null)
    {
      varhistoryTable.datatable["mysql_global_variables"].destroy();
      $('#mysql_global_variables_tbl').empty();
      varhistoryTable.datatable["mysql_global_variables"] = null;
    }  
  }  
  mapping.table.sendQuery();
});
   
$("#chk_proclist_filter").click(function()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  if(mydom("chk_proclist_filter").checked)
    mydom("proclist_sql").value="mysql_active_processlist";
  else 
    mydom("proclist_sql").value="mysql_processlist";  
  mysql_processlistTable.sendQuery();
});

$("#btn_sys_stat_auto").click(function()
{
  triggerAutoSysStatRefresh();
});

function triggerAutoSysStatRefresh()
{
  if(mydom("btn_sys_stat_auto").checked)
  {
    mysql_global_status_metricsTable.sendQuery();
    setTimeout(triggerAutoSysStatRefresh, mydomval("sys_stat_auto_rate")*1000);
   }
}

$("#btn_sys_stat_reset").click(function()
{
  mysql_global_status_metricsTable.baseDataObj['mysql_global_status_metrics'] = null;
  mysql_global_status_metricsTable.sendQuery();
});

$("#btn_sys_stat").click(function()
{
  mysql_global_status_metricsTable.sendQuery();
});

$('#btn_mysql_user_time').click(function()
{
  mysql_user_timeTable.sendQuery();
});
 
$('#btn_mysql_table_statistics').click(function()
{
  mysql_table_statisticsTable.sendQuery();
});

$('#btn_mysql_index_statistics').click(function()
{
  mysql_index_statisticsTable.sendQuery();
});

$('#btn_mysql_client_conn_statistics').click(function()
{
  mysql_client_conn_statisticsTable.sendQuery();
});

$('#btn_innodb_mutex').click(function()
{
  mysql_innodb_mutexTable.sendQuery();
});

$('#btn_innodb_metrics').click(function()
{
  mysql_innodb_metricsTable.sendQuery();
});

$('#btn_explain').click(function()
{
    planTable.sendQuery();
});

$('#btn_explain_json').click(function()
{
    startExplainJSON();
});
	
function startExplainJSON()
{
  var mydata = "group="+mydomval("dbgroup")+"&host="+mydomval("host");
  mydata += "&plan=y";
  mydata += "&dbname="+encodeURIComponent(mydomval("plan_dbname"));
  mydata += "&format=json";
  mydata += "&sqltext="+encodeURIComponent(mydomval("plan_sql"));
  reportStatus(false, "common_msg", "Explain In Progress ...");
  
  $.ajax({
       url: "profile.html",
       data: mydata,
       type: 'POST',
       dataType: 'json',
       success: function(json)
       {
         if(json.resp != null && json.resp.status != 0)
         {
           reportStatus(true, "common_msg", json.resp.message);
           return;
         }
	     mydom("json_plan").style.display="block";
		 var planObj = json;
		 if(zplanTreeObj != null)
		 {
		   zplanTreeObj.destroy();
		   zplanTreeObj = null;
		 } 
		 var treeData = new Object();
		 treeData.label = "Plan Tree";
		 treeData.name = "Plan Tree";
		 treeData.open = true;
		 treeData.children = new Array();
		 var query_block_id = null;
		 traversePlanObj(planObj, treeData, query_block_id);
		 zplanTreeObj = $.fn.zTree.init($("#json_plan_ztree"), zplanSetting, [treeData]);	    
         reportStatus(false, "common_msg", "Explain Done");
       }//success
  });//explain ajax
}//startExplainJSON
    	    	
//build a treeview object from plan json object
function traversePlanObj(curSrcObj, curTgtObj, query_block_id)
{
    
      if(curSrcObj==null||curTgtObj==null)return;//we are done
      var query_block_cost;
      
      for (var key in curSrcObj)
      {
        var label = key;
        if(key=='access_type'||key=='rows'||key=='filtered'|| key=='access_type'||key=='table_name' ||key=='select_id')
        {
        }else if(curTgtObj.label!=null && (curTgtObj.label.indexOf('materialized_from_subquery')>=0 || curTgtObj.label.indexOf('union spec')>=0)&& 
          (key=='using_temporary_table' || key=='dependent'||key=='cacheable'))
        {
        }else if(curTgtObj.label!=null && (curTgtObj.label.indexOf('duplicates_removal')>=0 ||curTgtObj.label.indexOf('grouping_operation')>=0 ||curTgtObj.label.indexOf('ordering_operation')>=0)&& 
          (key=='using_temporary_table' || key=='using_filesort'))
        {
        }else if(curTgtObj.label!=null && (curTgtObj.label.indexOf('union_result')>=0 )&& 
          (key=='using_temporary_table' ||key=='table_name'||key=='access_type'))
        {
        }
        else if(key=='using_filesort' 
          ||key=='attached_condition'
          ||key=='using_index'||key=='using_temporary_table' || key=='dependent'||key=='cacheable'
          ||key=='using_filesort'||key=='using_join_buffer')
        {
          var s = curSrcObj[key];
          label += ": "+ s;
          var child = new Object();
          child.label = label;
          child.name = label;
          curTgtObj.children[curTgtObj.children.length] = child;  
        }else if(key=='key'||key=='used_key_parts'||key=='ref'||key=='key_length')
        {
          if(key=='key')
          {
            var s = curSrcObj['key'];
            if(s!=null && s!='')
            {
              s = s.replace("<","&lt;").replace(">","&gt;");
              s = 'key: '+s + ', key_length: '+curSrcObj['key_length'];
              s  = s + ', used_key_parts: '+ JSON.stringify(curSrcObj['used_key_parts']);
              s =  s + ', ref: '+ nullToEmpty(JSON.stringify(curSrcObj['ref']));              
            }
            label = s;
            var child = new Object();
            child.label = label;
            child.name = label;
            curTgtObj.children[curTgtObj.children.length] = child;  
          }
        }
        else if(key=='possible_keys')
        {
          var child = new Object();
          //child.label = label;          
          curTgtObj.children[curTgtObj.children.length] = child;  
          var val = curSrcObj[key];
          if(val!=null)
          {
            var s =  JSON.stringify(curSrcObj[key]);
            if(s!=null)
            s = s.replace("<","&lt;").replace(">","&gt;");            
            child.label = key+": "+ s;
            child.name = key +": " + s;
          }
        }else if(key=='used_columns')
        {
          var child = new Object();
          //child.label = label;          
          curTgtObj.children[curTgtObj.children.length] = child;  
          var val = curSrcObj[key];
          if(val!=null)
          {
            var s =  JSON.stringify(curSrcObj[key]);
            if(s!=null)
            s = s.replace("<","&lt;").replace(">","&gt;");            
            child.label = key+": "+ s;
            child.name = key +": " + s;
          }
        }else if(key=='cost_info' && query_block_id != null )
        {
          var val = curSrcObj[key];
          query_block_cost = val['query_cost'];
        }else if(key=='cost_info')
        {
          var child = new Object();
          //child.label = label;          
          curTgtObj.children[curTgtObj.children.length] = child;  
          var val = curSrcObj[key];
          if(val!=null)
          {
            child.label = key+": "
              + "read_cost: " + nullToEmpty(val['read_cost'])
              + ", eval_cost: " + nullToEmpty(val['eval_cost'])
              + ", prefix_cost: " + nullToEmpty(val['prefix_cost'])
              + ", data_read_per_join: " + nullToEmpty(val['data_read_per_join']);
            child.name = child.label;
          }
        }
        else if(key=='partitions')
        {
          var child = new Object();
          child.label = label;  
          child.name = label;        
          curTgtObj.children[curTgtObj.children.length] = child;  
          var val = curSrcObj[key];
          if(val!=null)
          {
            child.children = new Array();
            for(var i=0;i<val.length;i++)
            {
              var obj = new Object();
              obj.label = val[i];
              obj.name = val[i];
              child.children[i] = obj;           
            }
          }
        }else if(key=='table')
        {
          var val = curSrcObj[key];
          var child = new Object();
          child.label = 'table: '+ val['table_name'] + ' (access_type: ' + val['access_type'] 
            + ', rows: ' + nullToEmpty(val['rows']) 
            + ', rows_examined_per_scan: ' + nullToEmpty(val['rows_examined_per_scan']) 
            + ', rows_produced_per_join: ' + nullToEmpty(val['rows_produced_per_join']) 
            + ', filtered: ' + nullToEmpty(val['filtered']) + ')'; 
          child.name = child.label;
          if(query_block_id!=null)
          {
            child.label = '<span class="step">'+query_block_id+'</span> '+ child.label;
            child.name = child.label;
            query_block_id = null;
          }
          curTgtObj.children[curTgtObj.children.length] = child;  
		  child.children = new Array();
          if(val!=null)
  		    traversePlanObj(val, child, query_block_id);  		  	          
        }else if(key=='materialized_from_subquery')
        {
          var val = curSrcObj[key];
          var child = new Object();
          child.label = 'materialized_from_subquery: (using_temporary_table:'+val['using_temporary_table']+', dependent: '+nullToEmpty(val['dependent'])+', cacheable: '+nullToEmpty(val['cacheable'])+')'; 
          child.name = child.label;
          curTgtObj.children[curTgtObj.children.length] = child;  
		  child.children = new Array();
          if(val!=null)
  		    traversePlanObj(val, child,query_block_id);  		  	          
        }else if(key=='duplicates_removal'||key=='grouping_operation'||key=='ordering_operation' )
        {
          var val = curSrcObj[key];
          var child = new Object();
          child.label = key+': (';
          if(val['using_temporary_table']!=null)
             child.label += 'using_temporary_table:'+val['using_temporary_table']+', ';
          if(val['using_filesort']!=null) 
             child.label +=  'using_filesort: '+val['using_filesort'];
          if(query_block_cost != null){
            child.label += ", query_cost: " + query_block_cost;
          }          
           child.label += ')'; 
           child.name = child.label;
          if(query_block_id!=null)
          {
            child.label = '<span class="step">'+query_block_id+'</span> '+ child.label;
            child.name = child.label;
            query_block_id = null;
          }
          curTgtObj.children[curTgtObj.children.length] = child;  
		  child.children = new Array();
          if(val!=null)
  		    traversePlanObj(val, child,query_block_id);  		  	          
        }else if(key=='union_result')
        {
          var val = curSrcObj[key];
          var child = new Object();
          var tbl = val['table_name'];
          if(tbl!=null)
            tbl = tbl.replace('<','&lt;').replace('>','&gt;');
          child.label = key+': (table_name: '+tbl+', access_type: '+val['access_type']+', using_temporary_table:'+val['using_temporary_table']+')'; 
          child.name = child.label;
          curTgtObj.children[curTgtObj.children.length] = child;  
		  //child.children = new Array();
          if(val!=null)
          {
            //skip query_specifications
            val = val['query_specifications'];
  		    //traversePlanObj(val, child,query_block_id); 
  		    if(val!=null)
            {
              child.children = new Array();
              for(var i=0;i<val.length;i++)
              {
                var obj = new Object();
                obj.label = "union spec: (dependent: "+nullToEmpty(val[i]['dependent']) + ", cacheable: "+ nullToEmpty(val[i]['cacheable'])+")";
                obj.name = obj.label;
                obj.children = new Array();
                child.children[i] = obj;
                traversePlanObj(val[i], obj,query_block_id);
              }
            }  
  		  }
        }
        else if(key=='query_block'  )
        {
          var val = curSrcObj[key];
          query_block_id = val['select_id'];
          //var child = new Object();
          //child.label = label;          
          //curTgtObj.children[curTgtObj.children.length] = child;  
		  //child.children = new Array();
          if(val!=null)
          {
            //child.label += ': (select_id: '+ val['select_id']+')';
  		    traversePlanObj(val, curTgtObj,query_block_id);  		  	          
  		  }
        }else if(key=='nested_loop')
        {
          var child = new Object();
          child.label = label;
          child.name = child.label;
          if(query_block_id!=null)
          {
            child.label = '<span class="step">'+query_block_id+'</span> '+ child.label;
            child.name = child.label;
            query_block_id = null;
          }
          curTgtObj.children[curTgtObj.children.length] = child;  
          var val = curSrcObj[key];
          if(val!=null)
          {
            child.children = new Array();
            for(var i=0;i<val.length;i++)
            {
              var nextObj = val[i]["table"];
              var obj = new Object();
              obj.label = 'table: '+ nextObj['table_name'] +' (access_type:'+nextObj['access_type']
                           +', rows: '+nullToEmpty(nextObj['rows'])
                           +', rows_examined_per_scan: '+nullToEmpty(nextObj['rows_examined_per_scan'])
                           +', rows_produced_per_join: '+nullToEmpty(nextObj['rows_produced_per_join'])
                           +', filtered: ' + nullToEmpty(nextObj['filtered'])+')'; 
              obj.name = obj.label;
              obj.children = new Array();
              child.children[i] = obj;
              traversePlanObj(nextObj, obj,query_block_id);
            }
          }          
        }else if(key=='query_specifications')
        {
          var child = new Object();
          child.label = label;
          child.name = child.label;
          curTgtObj.children[curTgtObj.children.length] = child;  
          var val = curSrcObj[key];
          if(val!=null)
          {
            child.children = new Array();
            for(var i=0;i<val.length;i++)
            {
              var obj = new Object();
              obj.label = "union spec: (dependent: "+nullToEmpty(val[i]['dependent'])+", cacheable: "+nullToEmpty(val[i]['cacheable'])+")";
              obj.name = obj.label;
              obj.children = new Array();
              child.children[i] = obj;
              traversePlanObj(val[i], obj,query_block_id);
            }
          }          
        }else if(key=='attached_subqueries')
        {
          var child = new Object();
          child.label = label;
          child.name = child.label;
          curTgtObj.children[curTgtObj.children.length] = child;  
          var val = curSrcObj[key];
          if(val!=null)
          {
            child.children = new Array();
            for(var i=0;i<val.length;i++)
            {
              var obj = new Object();
              obj.label = "Subquery: (dependent: "+nullToEmpty(val[i]['dependent'])+", cacheable: "+nullToEmpty(val[i]['cacheable'])+")";
              //if(val[i]['query_block'] != null && val[i]['query_block']['table'] != null)
              //  obj.label = '<span class="step">'+val[i]['query_block']['select_id'] + '</span> '+ obj.label;              
              obj.name = obj.label;
              obj.children = new Array();
              child.children[i] = obj;
              traversePlanObj(val[i], obj,query_block_id);
            }
          }
        }
        else
        {
          reportStatus(true, "com_message", "The version of MySQL might not support EXPLAIN JSON");
        }
      }//traver for loop
}//traverse  function

function nullToEmpty(str)
{
  if(str == null)return "";
  return str;
}
</script>
</body>
</html>