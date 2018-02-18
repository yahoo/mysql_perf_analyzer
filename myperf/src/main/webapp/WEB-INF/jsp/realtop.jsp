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
<title>MySQL Top</title>
<link rel="stylesheet" href="jquery/css/zTreeStyle/zTreeStyle.css" type="text/css" />
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<style>
caption {font-weight:bold; text-align: left;}
.cat {border-bottom:1px solid black;}
</style>
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
   </span>
</div><!-- end of dblist -->

<span id="common_msg"> <!-- common message line -->
<c:if test="${mydbSize==0}">You have not provided any database credential yet. Please use <a href="<%= request.getContextPath() %>/db.htm">DB Credential</a> page to provide access information for databases you are interested in.</c:if>
</span>

<div id="realtop_div" style="margin:10px;">
<!-- need a grid here -->
 <div><label for="top_frequency">Frequency</label>&nbsp; 
   <select id="top_frequency" name="top_frequency">
     <option value="1">1 sec</option>
     <option value="3">3 sec</option>
     <option value="5" selected>5 sec</option>
     <option value="10">10 sec</option>
     <option value="15">15 sec</option>
     <option value="30">30 sec</option>
     <option value="60">1 min</option>
     <option value="300">5 min</option>
   </select>
   &nbsp;&nbsp;SNMP: <input type="checkbox" id="chk_snmp" name="chk_snmp" value="y" checked /> <!-- not all servers have snmp enabled -->
   &nbsp;&nbsp;<input type="button" id="btn_start" value="Start" />
   &nbsp;&nbsp;<input type="button" id="btn_stop" value="Stop" disabled />
 </div>
 <table id="top_sys_tbl" style="margin:10px;border-spacing:4px;">
   <caption id="cap_sys_tbl">OS</caption>
   <tr><th colspan="3" class="cat">SYSTEM</th>
       <th colspan="3" class="cat">Load AVG</th>
       <th colspan="4" class="cat">CPU (%)</th>
       <th colspan="4" class="cat">MEMORY (KB)</th>
       <th colspan="2" class="cat">SWAP (KB)</th>
       <th colspan="2" class="cat">TCP CONN</th>
   </tr>
   <tr>
       <td class="cat">UPTIME</td>
       <td class="cat">USERS</td>
       <td class="cat">PROC</td>
       
       <td class="cat">1min</td>
       <td class="cat">5min</td>
       <td class="cat">15min</td>
       
       <td class="cat">USER</td>
       <td class="cat">SYS</td>
       <td class="cat">WAIT</td>
       <td class="cat">IDLE</td>
       
       <td class="cat">TOTAL</td>
       <td class="cat">FREE</td>
       <td class="cat">CACHED</td>
       <td class="cat">BUFFER</td>
       
       <td class="cat">TOTAL</td>
       <td class="cat">FREE</td>
       
       <td class="cat">ESTAB</td>
       <td class="cat">FAILS</td>       
   </tr>
   <tr>
       <td id="top_uptime">-</td><td id="top_users">-</td><td id="top_processes">-</td>
       <td id="top_load_avg_1m">-</td><td id="top_load_avg_5m">-</td><td id="top_load_avg_15m">-</td>       
       <td id="top_sys_user">-</td><td id="top_sys_sys">-</td><td id="top_sys_wait">-</td><td id="top_sys_idle">-</td>
       <td id="top_mem_total">-</td><td id="top_mem_free">-</td><td id="top_mem_cached">-</td><td id="top_mem_buffer">-</td>
       <td id="top_swap_total">-</td><td id="top_swap_free">-</td>       
       <td id="top_tcp_estab">-</td><td id="top_tcp_fails">-</td>       
    </tr>
 </table>

 <table id="top_mysql_tbl" style="margin:10px;border-spacing:4px;">
   <caption id="cap_mysql_tbl">MySQL Load</caption>
   <tr>
     <th class="cat">UPTIME</th>
     <th colspan="11" class="cat">COMMAND/SEC</th>
     <th colspan="3" class="cat">REPLICATION</th>
   </tr>
   <tr>
       <td class="cat">UPTIME</td>       
       <td class="cat">QUESTIONS</td>
       <td class="cat">QUERIES</td>
       <td class="cat">SLOW</td>
       <td class="cat">SELECT</td>
       <td class="cat">INSERT</td>
       <td class="cat">UPDATE</td>
       <td class="cat">REPLACE</td>
       <td class="cat">DELETE</td>
       <td class="cat">COMMIT</td>
       <td class="cat">ROLLBACK</td>
       <td class="cat">SET</td>
       
       <td class="cat">SQL</td>
       <td class="cat">IO</td>
       <td class="cat">LAG</td>
       
   </tr>
   <tr>
       <td id="top_mysql_uptime">-</td>
       <td id="top_mysql_questions">-</td><td id="top_mysql_queries">-</td><td id="top_mysql_slow_queries">-</td><td id="top_mysql_select">-</td><td id="top_mysql_insert">-</td><td id="top_mysql_update">-</td><td id="top_mysql_replace">-</td>
           <td id="top_mysql_delete">-</td><td id="top_mysql_commit">-</td><td id="top_mysql_rollback">-</td>
           <td id="top_mysql_set">-</td>
       <td id="top_mysql_repl_sql">-</td>
       <td id="top_mysql_repl_io">-</td>
       <td id="top_mysql_repl_lag">-</td>    
    </tr>
 </table>

 <table id="top_mysql_net_tbl" style="margin:10px;border-spacing:4px;">
   <caption id="cap_mysql_net_tbl">MySQL Connections/Networks</caption>
   <tr>
   	   <th colspan="4" class="cat">CONNECTIONS</th>
       <th colspan="4" class="cat">THREADS</th>
       <th colspan="2" class="cat">NET IO/SEC</th>
   </tr>
   <tr>
       <td class="cat">NEW/SEC</td>
       <td class="cat">MAX USED</td>
       <td class="cat">ABORTED</td>
       <td class="cat">ABORTED CLIENTS</td>
       
       <td class="cat">CONNECTED</td>
       <td class="cat">RUNNING</td>
       <td class="cat">CREATED/SEC</td>
       <td class="cat">CACHED</td>
       
       <td class="cat">SENT KB</td>
       <td class="cat">RECV KB</td>
       
   </tr>
   <tr>
       <td id="top_mysql_connections">-</td><td id="top_mysql_max_used_connections">-</td><td id="top_mysql_aborted_connects">-</td><td id="top_mysql_aborted_clients">-</td>    
       <td id="top_mysql_threads_connected">-</td><td id="top_mysql_threads_running">-</td><td id="top_mysql_threads_created">-</td><td id="top_mysql_threads_cached">-</td>    
       <td id="top_mysql_bytes_sent">-</td><td id="top_mysql_bytes_received">-</td>
    </tr>
 </table>

 <table id="top_innodb_tbl" style="margin:10px;border-spacing:4px;">
   <caption id="cap_innodb_tbl">InnoDB</caption>
   <tr>
     <th colspan="4"  class="cat">ROW OPS/SEC</th>
     <th colspan="6"  class="cat">IO/SEC</th>
     <th colspan="6"  class="cat">BUFFER POOL PAGES</th>
   </tr>
   <tr>
       <td  class="cat">READ</td>
       <td class="cat">INSERTED</td>
       <td class="cat">UPDATED</td>
       <td class="cat">DELETED</td>
       
       <td class="cat">FSYNCS</td>
       <td class="cat">READ KB</td>
       <td class="cat">READ IOPS</td>
       <td class="cat">WRITE KB</td>
       <td class="cat">WRITE IOPS</td>
	   <td class="cat">LOG KB</td>
	   
       <td class="cat">TOTAL</td>
       <td class="cat">DATA</td>
       <td class="cat">DIRTY</td>
       <td class="cat">FREE</td>
       <td class="cat">MISC</td>
       <td class="cat">FLUSHED/SEC</td>
   </tr>
   <tr>
       <td id="top_mysql_innodb_rows_read">-</td><td id="top_mysql_innodb_rows_inserted">-</td><td id="top_mysql_innodb_rows_updated">-</td><td id="top_mysql_innodb_rows_deleted">-</td>
       <td id="top_mysql_innodb_data_fsyncs">-</td><td id="top_mysql_innodb_data_read">-</td><td id="top_mysql_innodb_data_reads">-</td><td id="top_mysql_innodb_data_written">-</td><td id="top_mysql_innodb_data_writes">-</td><td id="top_mysql_innodb_os_log_written">-</td>
       <td id="top_mysql_innodb_pages_total">-</td><td id="top_mysql_innodb_pages_data">-</td><td id="top_mysql_innodb_pages_dirty">-</td><td id="top_mysql_innodb_pages_free">-</td><td id="top_mysql_innodb_pages_misc">-</td><td id="top_mysql_innodb_pages_flushed">-</td>
   </tr>
 </table>
</div>

<div id="mysql_processlist_tbl_div">
    <div style="font-weight:bold;">Active Process List</div>
    <table id="mysql_processlist_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
</div><!-- process list -->

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
   	     sqlId: "mysql_active_processlist",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1},
   	   tooltipCallbackOnClick: processlistTermCB,
   	   formatter:{rowFormatter: jqueryStylingProcessList, columnFormatters:{"INFO":jqueryFormatSqlText}}
   	});//TODO formatter

function messagehandler(datatable, status, message)
{
  console.log("recieve message from "+ datatable.name+", status: "+status
	   + ", message: "+message);
  if(status != 0)
    $('#common_msg').text(message);
}


var top_threads_running = false;
var TOP_METRICS = {}; //contains components each with three objects: first, prev, cur
					  //for example, snmp, global_status, user_status, table_status

$('#btn_start').click(function()
{
  if(!top_threads_running)
  {
    top_threads_running = true;
    resetTop();
    triggerTopSNMPStatRefresh();
    triggerTopGlobalStatusRefresh();
    triggerProcesListRefresh();
    triggerTopReplRefresh();
    mydom('btn_stop').disabled = false;
    mydom('btn_start').disabled = true;
    
  }else
    reportStatus(true, "com_message", "Top is running.")
});

$('#btn_stop').click(function()
{
    top_threads_running = false;
    mydom('btn_stop').disabled = true;
    mydom('btn_start').disabled = false;
});

function triggerProcesListRefresh()
{
  if(!top_threads_running)return;
  var host = mydomval("host");
  if(host != null && host != "")
    mysql_processlistTable.sendQuery();
    
  setTimeout(triggerProcesListRefresh, mydomval("top_frequency")*1000);  
}

function triggerTopSNMPStatRefresh()
{
  if(!top_threads_running)return;
  var ts = (new Date()).getTime();
  var host = mydomval("host");
  TOP_METRICS.host = host;
  if(host != null && host != "")
  	retrieveTopSNMP(ts);
  else
    setTimeout(triggerTopSNMPStatRefresh, mydomval("top_frequency")*1000);  
}

function triggerTopGlobalStatusRefresh()
{
  if(!top_threads_running)return;
  var ts = (new Date()).getTime();
  var host = mydomval("host");
  TOP_METRICS.host = host;
  if(host != null && host != "")
  	retrieveTopGlobalStatus(ts);
  else
    setTimeout(triggerTopGlobalStatusRefresh, mydomval("top_frequency")*1000);  
}

function triggerTopReplRefresh()
{
  if(!top_threads_running)return;
  var ts = (new Date()).getTime();
  var host = mydomval("host");
  TOP_METRICS.host = host;
  if(host != null && host != "")
  	retrieveTopRepl(ts);
  else
    setTimeout(triggerTopReplRefresh, mydomval("top_frequency")*1000);  
}
					  
//empty existing content of TOP_METRICS
function resetTop()
{
  TOP_METRICS = {}; //reset
  //TODO empty other fields
}

var TOP_SNMP_METRICS_NAMES = ["ssCpuRawWait", "ssCpuRawIdle", "ssCpuRawUser", "ssCpuRawSystem", "ssCpuRawSoftIRQ", "hrSystemUptime",
	"hrSystemNumUsers","hrSystemProcesses","laLoad1m","laLoad5m","laLoad15m","memTotalReal", "memAvailReal",
	"memCached","memBuffer","memTotalSwap","memAvailSwap","tcpAttemptFails","tcpCurrEstab"
     ];
function retrieveTopSNMP(ts)
{
   if(!mydom("chk_snmp").checked)
   {
     //don't run snmp query, but will keep loop alive
     setTimeout(triggerTopSNMPStatRefresh, mydomval("top_frequency")*1000);
     return;
   }
   var mydata =  "group="+escape(mydomval("dbgroup"));
       mydata += "&host="+ escape(mydomval("host"));
       mydata += "&sql=snmp&p_1=sys&p_2=0&rf=NAME";
       for(var i=0; i<TOP_SNMP_METRICS_NAMES.length; i++)
         mydata += "&rfv="+TOP_SNMP_METRICS_NAMES[i];
   	   mydata += "&seed=" + Math.random();	
   $.ajax({
       url: "query.html",
       data: mydata,
       dataType: 'json',
       success: function(json)
       {
         if(json!=null && json.resp!=null && json.resp.status == 0)
         {
           //dcode our data first
           var res = json.resp.results.results;
           var mydata = {};
           mydata["ts"] = ts;
           for(var i = 0; i< res.length; i++)
             mydata[res[i]["NAME"]] = res[i]["VALUE"];
           if(TOP_METRICS["snmp"] == null)
           {
             TOP_METRICS["snmp"] = {first:mydata, cur:mydata};
           }else
           {
             TOP_METRICS["snmp"].prev = TOP_METRICS["snmp"].cur;
             TOP_METRICS["snmp"].cur = mydata;
             var total = sumDiff(TOP_METRICS["snmp"].cur, TOP_METRICS["snmp"].prev, 
                         ["ssCpuRawWait", "ssCpuRawIdle", "ssCpuRawUser", "ssCpuRawSystem", "ssCpuRawSoftIRQ"]);
             if(total == 0)
             {
               console.log("sum is 0");
             }else
             {
               fillSingleSNMP({domId:"top_sys_user", source:TOP_METRICS, tableName:"snmp", entryName:"ssCpuRawUser", diff:1, adj:100/total, fixedPos: 2});
               fillSingleSNMP({domId:"top_sys_sys", source:TOP_METRICS, tableName:"snmp", entryName:"ssCpuRawSystem", diff:1, adj:100/total, fixedPos: 2});
               fillSingleSNMP({domId:"top_sys_wait", source:TOP_METRICS, tableName:"snmp", entryName:"ssCpuRawWait", diff:1, adj:100/total, fixedPos: 2});
               fillSingleSNMP({domId:"top_sys_idle", source:TOP_METRICS, tableName:"snmp", entryName:"ssCpuRawIdle", diff:1, adj:100/total, fixedPos: 2});
               fillSingleSNMP({domId:"top_uptime", source:TOP_METRICS, tableName:"snmp", entryName:"hrSystemUptime"});
               fillSingleSNMP({domId:"top_users", source:TOP_METRICS, tableName:"snmp", entryName:"hrSystemNumUsers"});
               fillSingleSNMP({domId:"top_processes", source:TOP_METRICS, tableName:"snmp", entryName:"hrSystemProcesses"});
               fillSingleSNMP({domId:"top_load_avg_1m", source:TOP_METRICS, tableName:"snmp", entryName:"laLoad1m"});
               fillSingleSNMP({domId:"top_load_avg_5m", source:TOP_METRICS, tableName:"snmp", entryName:"laLoad5m"});
               fillSingleSNMP({domId:"top_load_avg_15m", source:TOP_METRICS, tableName:"snmp", entryName:"laLoad15m"});
               fillSingleSNMP({domId:"top_mem_total", source:TOP_METRICS, tableName:"snmp", entryName:"memTotalReal",formatNumber:1});
               fillSingleSNMP({domId:"top_mem_free", source:TOP_METRICS, tableName:"snmp", entryName:"memAvailReal",formatNumber:1});
               fillSingleSNMP({domId:"top_mem_cached", source:TOP_METRICS, tableName:"snmp", entryName:"memCached",formatNumber:1});
               fillSingleSNMP({domId:"top_mem_buffer", source:TOP_METRICS, tableName:"snmp", entryName:"memBuffer",formatNumber:1});
               fillSingleSNMP({domId:"top_swap_total", source:TOP_METRICS, tableName:"snmp", entryName:"memTotalSwap",formatNumber:1});
               fillSingleSNMP({domId:"top_swap_free", source:TOP_METRICS, tableName:"snmp", entryName:"memAvailSwap",formatNumber:1});
               fillSingleSNMP({domId:"top_tcp_estab", source:TOP_METRICS, tableName:"snmp", entryName:"tcpCurrEstab",formatNumber:1});
               fillSingleSNMP({domId:"top_tcp_fails", source:TOP_METRICS, tableName:"snmp", entryName:"tcpAttemptFails",diff:1,formatNumber:1});
             }
           }
         }
         setTimeout(triggerTopSNMPStatRefresh, mydomval("top_frequency")*1000);          
       }
     }); 
}

var TOP_STATUS_METRICS=["UPTIME","QUESTIONS","QUERIES","SLOW_QUERIES",
  "COM_SELECT","COM_INSERT","COM_UPDATE","COM_REPLACE","COM_DELETE","COM_COMMIT","COM_ROLLBACK","COM_SET_OPTION",
  "CONNECTIONS","MAX_USED_CONNECTIONS","ABORTED_CONNECTS","ABORTED_CLIENTS",
  "THREADS_CONNECTED","THREADS_RUNNING","THREADS_CACHED","THREADS_CREATED",
  "INNODB_ROWS_READ","INNODB_ROWS_INSERTED","INNODB_ROWS_UPDATED","INNODB_ROWS_DELETED", "INNODB_DATA_FSYNCS",
  "INNODB_DATA_READ", "INNODB_DATA_WRITTEN","INNODB_DATA_READS", "INNODB_DATA_WRITES",
  "INNODB_BUFFER_POOL_PAGES_TOTAL","INNODB_BUFFER_POOL_PAGES_DATA","INNODB_BUFFER_POOL_PAGES_DIRTY","INNODB_BUFFER_POOL_PAGES_FREE",
  "INNODB_BUFFER_POOL_PAGES_FLUSHED","INNODB_BUFFER_POOL_PAGES_MISC","INNODB_OS_LOG_WRITTEN","BYTES_RECEIVED","BYTES_SENT"
  ];
function retrieveTopGlobalStatus(ts)
{
   var mydata =  "group="+escape(mydomval("dbgroup"));
       mydata += "&host="+ escape(mydomval("host"));
       mydata += "&sql=mysql_show_global_status_ps&p_1=&rf=VARIABLE_NAME&seed=" + Math.random();
       for(var i=0; i<TOP_STATUS_METRICS.length; i++)
         mydata += "&rfv="+TOP_STATUS_METRICS[i];
   
   $.ajax({
       url: "query.html",
       data: mydata,
       dataType: 'json',
       success: function(json)
       {
         if(json!=null && json.resp!=null && json.resp.status == 0)
         {
           //dcode our data first
           var res = json.resp.results.results;
           var mydata = {};
           mydata["ts"] = ts;
           for(var i = 0; i< res.length; i++)
             mydata[res[i]["VARIABLE_NAME"]] = res[i]["VARIABLE_VALUE"];
           if(TOP_METRICS["global_status"] == null)
           {
             TOP_METRICS["global_status"] = {first:mydata, cur:mydata};
           }else
           {
             TOP_METRICS["global_status"].prev = TOP_METRICS["global_status"].cur;
             TOP_METRICS["global_status"].cur = mydata;
             var total = sumDiff(TOP_METRICS["global_status"].cur, TOP_METRICS["global_status"].prev, 
                         ["UPTIME"]);
             if(total == 0)
             {
               console.log("UPTIME CHANGE is 0");//skip if no change
             }else
             {
               var tsDiff = ts - TOP_METRICS["global_status"].prev.ts;
               var adj = 1000/tsDiff;//use per second metrics
               fillSingleSNMP({domId:"top_mysql_uptime", source:TOP_METRICS, tableName:"global_status", entryName:"UPTIME",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_questions", source:TOP_METRICS, tableName:"global_status", entryName:"QUESTIONS", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_queries", source:TOP_METRICS, tableName:"global_status", entryName:"QUERIES", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_slow_queries", source:TOP_METRICS, tableName:"global_status", entryName:"SLOW_QUERIES", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_select", source:TOP_METRICS, tableName:"global_status", entryName:"COM_SELECT", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_insert", source:TOP_METRICS, tableName:"global_status", entryName:"COM_INSERT", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_update", source:TOP_METRICS, tableName:"global_status", entryName:"COM_UPDATE", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_replace", source:TOP_METRICS, tableName:"global_status", entryName:"COM_REPLACE", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_delete", source:TOP_METRICS, tableName:"global_status", entryName:"COM_DELETE", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_commit", source:TOP_METRICS, tableName:"global_status", entryName:"COM_COMMIT", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_rollback", source:TOP_METRICS, tableName:"global_status", entryName:"COM_ROLLBACK", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_set", source:TOP_METRICS, tableName:"global_status", entryName:"COM_SET_OPTION", diff:1, fixedPos: 2, formatNumber:1, adj:adj});

               fillSingleSNMP({domId:"top_mysql_connections", source:TOP_METRICS, tableName:"global_status", entryName:"CONNECTIONS", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_max_used_connections", source:TOP_METRICS, tableName:"global_status", entryName:"MAX_USED_CONNECTIONS",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_aborted_connects", source:TOP_METRICS, tableName:"global_status", entryName:"ABORTED_CONNECTS", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_aborted_clients", source:TOP_METRICS, tableName:"global_status", entryName:"ABORTED_CLIENTS", diff:1, fixedPos: 2, formatNumber:1, adj:adj});

               fillSingleSNMP({domId:"top_mysql_threads_connected", source:TOP_METRICS, tableName:"global_status", entryName:"THREADS_CONNECTED",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_threads_running", source:TOP_METRICS, tableName:"global_status", entryName:"THREADS_RUNNING",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_threads_cached", source:TOP_METRICS, tableName:"global_status", entryName:"THREADS_CACHED",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_threads_created", source:TOP_METRICS, tableName:"global_status", entryName:"THREADS_CREATED", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_bytes_received", source:TOP_METRICS, tableName:"global_status", entryName:"BYTES_RECEIVED", diff:1, fixedPos: 2, formatNumber:1, adj:adj/1024});
               fillSingleSNMP({domId:"top_mysql_bytes_sent", source:TOP_METRICS, tableName:"global_status", entryName:"BYTES_SENT", diff:1, fixedPos: 2, formatNumber:1, adj:adj/1024});

               fillSingleSNMP({domId:"top_mysql_innodb_rows_read", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_ROWS_READ", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_innodb_rows_inserted", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_ROWS_INSERTED", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_innodb_rows_updated", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_ROWS_UPDATED", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_innodb_rows_deleted", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_ROWS_DELETED", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_innodb_data_fsyncs", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_DATA_FSYNCS", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_innodb_data_read", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_DATA_READ", diff:1, fixedPos: 2, formatNumber:1, adj:adj/1024});
               fillSingleSNMP({domId:"top_mysql_innodb_data_written", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_DATA_WRITTEN", diff:1, fixedPos: 2, formatNumber:1, adj:adj/1024});
               fillSingleSNMP({domId:"top_mysql_innodb_data_reads", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_DATA_READS", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_innodb_data_writes", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_DATA_WRITES", diff:1, fixedPos: 2, formatNumber:1, adj:adj});
               fillSingleSNMP({domId:"top_mysql_innodb_os_log_written", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_OS_LOG_WRITTEN", diff:1, fixedPos: 2, formatNumber:1, adj:adj/1024});

               fillSingleSNMP({domId:"top_mysql_innodb_pages_total", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_BUFFER_POOL_PAGES_TOTAL",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_innodb_pages_data", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_BUFFER_POOL_PAGES_DATA",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_innodb_pages_dirty", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_BUFFER_POOL_PAGES_DIRTY",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_innodb_pages_free", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_BUFFER_POOL_PAGES_FREE",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_innodb_pages_misc", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_BUFFER_POOL_PAGES_MISC",formatNumber:1});
               fillSingleSNMP({domId:"top_mysql_innodb_pages_flushed", source:TOP_METRICS, tableName:"global_status", entryName:"INNODB_BUFFER_POOL_PAGES_FLUSHED", diff:1, fixedPos: 2, formatNumber:1, adj:adj});

               //fillSingleSNMP({domId:"top_uptime", source:TOP_METRICS, tableName:"snmp", entryName:"hrSystemUptime"});
               //fillSingleSNMP({domId:"top_mem_total", source:TOP_METRICS, tableName:"snmp", entryName:"memTotalReal",formatNumber:1});
             }
           }
         }
         setTimeout(triggerTopGlobalStatusRefresh, mydomval("top_frequency")*1000);          
       }
     }); 
}


var TOP_REPL_METRICS_NAMES = ["Slave_IO_Running", "Slave_SQL_Running", "Seconds_Behind_Master"];
function retrieveTopRepl(ts)
{
   if(!mydom("chk_snmp").checked)
   {
     //don't run snmp query, but will keep loop alive
     setTimeout(triggerTopSNMPStatRefresh, mydomval("top_frequency")*1000);
     return;
   }
   var mydata =  "group="+escape(mydomval("dbgroup"));
       mydata += "&host="+ escape(mydomval("host"));
       mydata += "&sql=mysql_repl_slave&rf=NAME";
       for(var i=0; i<TOP_REPL_METRICS_NAMES.length; i++)
         mydata += "&rfv="+TOP_REPL_METRICS_NAMES[i];
   	   mydata += "&seed=" + Math.random();	
   $.ajax({   
       url: "query.html",
       data: mydata,
       dataType: 'json',
       success: function(json)
       {
         if(json!=null && json.resp!=null && json.resp.status == 0)
         {
           //dcode our data first
           var res = json.resp.results.results;
           var mydata = {};
           mydata["ts"] = ts;
           for(var i = 0; i< res.length; i++)
             mydata[res[i]["NAME"]] = res[i]["VALUE"];
           if(TOP_METRICS["repl"] == null)
           {
             TOP_METRICS["repl"] = {first:mydata, cur:mydata};
           }else
           {
             TOP_METRICS["repl"].prev = TOP_METRICS["repl"].cur;
             TOP_METRICS["repl"].cur = mydata;
           }  
           fillSingleSNMP({domId:"top_mysql_repl_sql", source:TOP_METRICS, tableName:"repl", entryName:"Slave_SQL_Running"});
           fillSingleSNMP({domId:"top_mysql_repl_io", source:TOP_METRICS, tableName:"repl", entryName:"Slave_IO_Running"});
           fillSingleSNMP({domId:"top_mysql_repl_lag", source:TOP_METRICS, tableName:"repl", entryName:"Seconds_Behind_Master"});             
         }//if
         setTimeout(triggerTopReplRefresh, mydomval("top_frequency")*1000);          
       }//success
     }); //ajax
}//retrieveTopRepl

/*
  Fill the output of a single snmp entry
  obj
    source: TOP_METRICS
    tableName: such as snmp
    entryName: such as ssCpuRawUser
    diff: 1 use diff, 0 use current
    adj: If calculate percentage, we need some adjustment
    fixedPos: when format float value. If text data, don't set
    formatNumber: 1 to use toLocaleString
    domId: domId to fill the value
*/
function fillSingleSNMP(obj)
{
  var tbl = obj.source[obj.tableName];
  var val = null;
  if(obj.diff == 1 && obj.adj != null)
    val = sumDiff(tbl.cur, tbl.prev, [obj.entryName]) * obj.adj;
  else if(obj.diff == 1)
    val = sumDiff(tbl.cur, tbl.prev, [obj.entryName]);  
  else if(obj.adj != null)
    val = tbl.cur[obj.entryName]*obj.adj;
  else
    val = tbl.cur[obj.entryName];
  if(obj.fixedPos != null)
    val = val.toFixed(obj.fixedPos);
  if(obj.formatNumber == 1)
    val = eval(val).toLocaleString();
  if(val == null)val = "NA";
  $('#'+obj.domId).text(val);  
}
/*
 calculate the sum of the difference of specfified field
*/
function sumDiff(firstObj, secondObj, fields)
{
  var sum = 0;
  for(var i = 0; i<fields.length; i++)
    sum += eval(firstObj[fields[i]]) - eval(secondObj[fields[i]]);
  return sum;  
}
</script>
</body>
</html>