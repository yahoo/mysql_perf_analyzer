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
<title>SNMP Test Page</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<style>
 .ui-autocomplete {max-height: 200px;overflow-y: auto; overflow-x: hidden;}
</style>
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

        <input type="button" id="btn_session_activity" value="Test Common Entries"/>
        <input type="hidden" id="diff" name="diff" value="1" />
   </span>
</div><!-- end of query form -->

<span id="common_msg"> <!-- common message line -->

<c:if test="${mydbSize==0}">You have not provided any database credential yet. Please use <a href="<%= request.getContextPath() %>/db.htm">DB Credential</a> page to provide access information for databases you are interested in.</c:if>
</span>
<p style="margin-bottom:5px;width:600px;">Use this page to test if you can use SNMP to gather OS level metrics for a specific server. 
 Use <a href="perf.htm?pg=settings">Settings</a> page to disable/enable metrics gathering for individual server, 
 or to configure snmpd versions and access methods.</p>

<div id="snmptabs" class="clearTabView"> <!-- tab pane to display real time info in tabular formats -->
	<ul>
        <li><a href="#snmp_test_tbl_div" title="Show selected SNMP Data">Test Common Entries</a></li>
        <li><a href="#snmp_oid_tbl_div" title="Show data for a single SNMP entry or table">Test OID</a></li>
        <li><a href="#snmp_diff_tbl_div" title="Changes">Changes</a></li>
    </ul>
    <div id="snmp_test_tbl_div"> 
      <table id="snmp_test_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>      
    </div>
    <div id="snmp_oid_tbl_div">
      <input type="hidden" id="oid_type" name="oid_type" />
      OID: <input type="text" id="oid" name="oid" />&nbsp;<span id="oid_label"></span>
      <input type="button" id="btn_snmp_oid" value="Check" /><br />
      Please refer to <a hrep="http://www.net-snmp.org/docs/mibs/host.html">this page</a> for OIDs.
      <table id="snmp_oid_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>      
    </div>
    <div id="snmp_diff_tbl_div">
         <span>
    	   <input type="button" id="btn_snmp_refresh" value="Refresh" title="Click to see changes"/> 
    	   &nbsp;<input type="checkbox" value="1" id="btn_snmp_auto"  title="Check to start auto refresh, uncheck to stop"/> Auto
    	   &nbsp;<select id="sys_snmp_rate">
    	       <option value="5">5 sec</option>
    	       <option value="10">10 sec</option>
    	       <option value="15">15 sec</option>
    	       <option value="30">30 sec</option>
    	       <option value="60">60 sec</option>
    	       <option value="120">120 sec</option>
    	   </select>
    	   &nbsp;<input type="button" id="btn_snmp_reset" value="Restart" title="Click to Restart"/> 
    	 </span>
         <table id="snmp_diff_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>      
    </div>
</div>

<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>
<script language=javascript>

var OID_LIST = [
{label:"diskIOTable", value:".1.3.6.1.4.1.2021.13.15.1"},
{label:"hrSystemUptime", value:".1.3.6.1.2.1.25.1.1.0"},
{label:"hrSystemDate", 	value:".1.3.6.1.2.1.25.1.2.0"},
{label:"hrSystemInitialLoadDevice", value:".1.3.6.1.2.1.25.1.3.0"},
{label:"hrSystemInitialLoadParameters",value: ".1.3.6.1.2.1.25.1.4.0"},
{label:"hrSystemNumUsers", value:".1.3.6.1.2.1.25.1.5.0"},
{label:"hrSystemProcesses",value:".1.3.6.1.2.1.25.1.6.0"},
{label:"hrSystemMaxProcesses", value:".1.3.6.1.2.1.25.1.7.0"},
{label:"hrMemorySize", value:".1.3.6.1.2.1.25.2.2.0"},
{label:"hrSWOSIndex",value:".1.3.6.1.2.1.25.4.1.0"},
{label:"hrSWInstalledLastChange",value:".1.3.6.1.2.1.25.6.1.0"},
{label:"hrSWInstalledLastUpdateTime",value:".1.3.6.1.2.1.25.6.2.0"},
{label:"hrStorageTable", value:".1.3.6.1.2.1.25.2.3"},
{label:"hrDeviceTable", value:".1.3.6.1.2.1.25.3.2"},
{label:"hrProcessorTable", value:".1.3.6.1.2.1.25.3.3"},
{label:"hrNetworkTable", value:".1.3.6.1.2.1.25.3.4"},
{label:"hrDiskStorageTable", value:".1.3.6.1.2.1.25.3.6"},
{label:"hrPartitionTable", value:".1.3.6.1.2.1.25.3.7"},
{label:"hrFSTable", value:".1.3.6.1.2.1.25.3.8"},
{label:"hrSWRunTable", value:".1.3.6.1.2.1.25.4.2"},
{label:"hrSWRunPerfTable", value:".1.3.6.1.2.1.25.5.1"},
{label:"hrSWInstalledTable", value:".1.3.6.1.2.1.25.6.3"},
{label:"ifTable", value:".1.3.6.1.2.1.2.1"},
{label:"tcpRtoAlgorithm", value:".1.3.6.1.2.1.6.1.0"},
{label:"tcpRtoMin", value:".1.3.6.1.2.1.6.2.0"},
{label:"tcpRtoMax",	value:".1.3.6.1.2.1.6.3.0"},
{label:"tcpMaxConn",value:".1.3.6.1.2.1.6.4.0"},
{label:"tcpActiveOpens",value:".1.3.6.1.2.1.6.5.0"},
{label:"tcpPassiveOpens",value:".1.3.6.1.2.1.6.6.0"},
{label:"tcpAttemptFails",value:".1.3.6.1.2.1.6.7.0"},
{label:"tcpEstabResets",value:".1.3.6.1.2.1.6.8.0"},
{label:"tcpCurrEstab",value:".1.3.6.1.2.1.6.9.0"},
{label:"tcpInSegs", value:".1.3.6.1.2.1.6.10.0"},
{label:"tcpOutSegs",value:".1.3.6.1.2.1.6.11.0"},
{label:"tcpRetransSegs",value:".1.3.6.1.2.1.6.12.0"},
{label:"tcpInErrs", value:".1.3.6.1.2.1.6.14.0"},
{label:"tcpOutRsts",value:".1.3.6.1.2.1.6.15.0"},
{label:"tcpHCInSegs",value:".1.3.6.1.2.1.6.17.0"},
{label:"tcpHCOutSegs", value:".1.3.6.1.2.1.6.18.0"},
{label:"tcpConnectionTable", value:".1.3.6.1.2.1.6.19"},
{label:"tcpListenerTable", value:".1.3.6.1.2.1.6.20"},
{label:"memIndex",value:".1.3.6.1.4.1.2021.4.1.0"},
{label:"memErrorName", value:".1.3.6.1.4.1.2021.4.2.0"},
{label:"memTotalSwap",value:".1.3.6.1.4.1.2021.4.3.0"},	
{label:"memAvailSwap",value:".1.3.6.1.4.1.2021.4.4.0"},	
{label:"memTotalReal", value:".1.3.6.1.4.1.2021.4.5.0"},
{label:"memAvailReal",value:".1.3.6.1.4.1.2021.4.6.0"},
{label:"memTotalSwapTXT", value:".1.3.6.1.4.1.2021.4.7.0"}, 
{label:"memTotalRealTXT",value:".1.3.6.1.4.1.2021.4.9.0"},	
{label:"memTotalFree",value:".1.3.6.1.4.1.2021.4.11.0"},
{label:"memMinimumSwap",value:".1.3.6.1.4.1.2021.4.12.0"},
{label:"memShared",value:".1.3.6.1.4.1.2021.4.13.0"},	
{label:"memBuffer",value:".1.3.6.1.4.1.2021.4.14.0"},	
{label:"memCached",value:".1.3.6.1.4.1.2021.4.15.0"},	
{label:"memUsedSwapTXT", value:".1.3.6.1.4.1.2021.4.16.0"},	
{label:"memUsedRealTXT",value:".1.3.6.1.4.1.2021.4.17.0"},
{label:"memSwapError", value:".1.3.6.1.4.1.2021.4.100.0"},
{label:"memSwapErrorMsg", value:".1.3.6.1.4.1.2021.4.101.0"},
{label:"ssIndex",value:".1.3.6.1.4.1.2021.11.1.0"}, 
{label:"ssErrorName", value:".1.3.6.1.4.1.2021.11.2.0"},
{label:"ssSwapIn", value:".1.3.6.1.4.1.2021.11.3.0"},
{label:"ssSwapOut",value:".1.3.6.1.4.1.2021.11.4.0"},	
{label:"ssCpuRawUser", value:".1.3.6.1.4.1.2021.11.50.0"},	
{label:"ssCpuRawNice",value:".1.3.6.1.4.1.2021.11.51.0"},	
{label:"ssCpuRawSystem",value:".1.3.6.1.4.1.2021.11.52.0"},
{label:"ssCpuRawIdle", value:".1.3.6.1.4.1.2021.11.53.0"},	
{label:"ssCpuRawWait",value:".1.3.6.1.4.1.2021.11.54.0"},	
{label:"ssCpuRawKernel",value:".1.3.6.1.4.1.2021.11.55.0"},
{label:"ssCpuRawInterrupt",value:".1.3.6.1.4.1.2021.11.56.0"},	
{label:"ssIORawSent",value:".1.3.6.1.4.1.2021.11.57.0"},	
{label:"ssIORawReceived",value:".1.3.6.1.4.1.2021.11.58.0"},
{label:"ssRawInterrupts", value:".1.3.6.1.4.1.2021.11.59.0"},	
{label:"ssRawContexts",value:".1.3.6.1.4.1.2021.11.60.0"},	
{label:"ssCpuRawSoftIRQ",value:".1.3.6.1.4.1.2021.11.61.0"},
{label:"ssRawSwapIn", value:".1.3.6.1.4.1.2021.11.62.0"},	
{label:"ssRawSwapOut", value:".1.3.6.1.4.1.2021.11.63.0"},	
{label:"ssCpuRawSteal",value:".1.3.6.1.4.1.2021.11.64.0"},	
{label:"ssCpuRawGuest",value:".1.3.6.1.4.1.2021.11.65.0"},	
{label:"ssCpuRawGuestNice",value:".1.3.6.1.4.1.2021.11.66.0"},	
{label:"logMatchMaxEntries",value:".1.3.6.1.4.1.2021.16.1.0"},	
{label:"versionIndex", value:".1.3.6.1.4.1.2021.100.1.0"},
{label:"versionTag", value:".1.3.6.1.4.1.2021.100.2.0"},
{label:"versionDate",value:".1.3.6.1.4.1.2021.100.3.0"},
{label:"versionCDate",value:".1.3.6.1.4.1.2021.100.4.0"},	
{label:"versionIdent", value:".1.3.6.1.4.1.2021.100.5.0"},	
{label:"versionConfigureOptions", value:".1.3.6.1.4.1.2021.100.6.0"},	
{label:"versionClearCache",value:".1.3.6.1.4.1.2021.100.10.0"},	
{label:"versionUpdateConfig", value:".1.3.6.1.4.1.2021.100.11.0"},	
{label:"versionRestartAgent",value:".1.3.6.1.4.1.2021.100.12.0"},	
{label:"versionSavePersistentData", value:".1.3.6.1.4.1.2021.100.13.0"},
{label:"versionDoDebugging",value:".1.3.6.1.4.1.2021.100.20.0"},
{label:"snmperrIndex", value:".1.3.6.1.4.1.2021.101.1.0"},	
{label:"snmperrNames", value:".1.3.6.1.4.1.2021.101.2.0"},
{label:"snmperrErrorFlag", value:".1.3.6.1.4.1.2021.101.100.0"},
{label:"snmperrErrMessage",value:".1.3.6.1.4.1.2021.101.101.0"},
{label:"laTable", value:".1.3.6.1.4.1.2021.10"}
];
$('#snmptabs').tabs();

$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host');
  }
);

$('#oid').autocomplete({
  source:OID_LIST,
  minLength:0,
  select: function( event, ui ) 
  {
    if(ui.item.label != null && ui.item.label.indexOf("Table")>0)
       mydom("oid_type").value = "table";
    else
      mydom("oid_type").value = "single";
    $('#oid_label').text(ui.item.label);
  }
}).bind('focus', function(){mydom("oid_type").value="unknown";$(this).autocomplete("search");});

var snmp_testTable = new JSTable({
   	   name: "snmp_test",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "snmp",
   	     paramFields:[{name:"p_1", value:"all"},{name:"p_2", value:"0"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:statusCB}
   	});

$('#btn_session_activity').click(function()
   {
	  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
        return;
      snmp_testTable.sendQuery();        
   }
);

var snmp_diffTable = new JSTable({
   	   name: "snmp_diff",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "snmp",
   	     paramFields:[{name:"p_1", value:"all"},{name:"p_2", value:"1"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:statusCB},
   	   diff: {keyColumns:["NAME", "OID"], valueColumns:["VALUE"]}   	   
   	});

$('#btn_snmp_refresh').click(function()
   {
	  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
        return;
      snmp_diffTable.sendQuery();        
   }
);

$('#btn_snmp_reset').click(function()
   {
	  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
        return;
      snmp_diffTable.baseDataObj["snmp_diff"] = null; //reset comparison data  
      snmp_diffTable.sendQuery();        
   }
);

$('#btn_snmp_auto').click(function()
  {
    triggerAutoSnmpRefresh();
  }
);

function triggerAutoSnmpRefresh()
{
  if(mydom("btn_snmp_auto").checked)
  {
    snmp_diffTable.sendQuery();
    setTimeout(triggerAutoSnmpRefresh, mydomval("sys_snmp_rate")*1000);
   }
}

function statusCB(datatable, status, message)
{  
   reportStatus((status != 0), "common_msg", (status==0)?"":message);
}

var snmp_oidTable = new JSTable({
   	   name: "snmp_oid",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "snmp",
   	     paramFields:[{name:"p_1", valueField:"oid_type"},{name:"p_2", valueField:"oid"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:statusCB}
   	});

$('#btn_snmp_oid').click(function()
   {
	  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
        return;
      snmp_oidTable.sendQuery();        
   }
);

</script>
</body>
</html>