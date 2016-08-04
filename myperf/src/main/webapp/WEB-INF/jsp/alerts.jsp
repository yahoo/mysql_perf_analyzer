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
<title>DB Alerts</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<style>
 .warning-highlighted{background-color:red;}
 #db_block table{border:0px;margin-bottom:1px;border-spacing:0px;}
 #db_block td{border:0px;padding-top:0px;padding-bottom:0px;}
 #db_block th{border:0px;padding-bottom:0px;padding-top:0px;}
 .selected {background-color:lightblue;color:#47a;text-decoration:underline;}
 .unselected {background-color:white;color:#47a;text-decoration:none;}
 #alert_info_tbl{border:0px;border-spacing:0px;}
 #alert_info_tbl tr {border:0px;}
 #alert_info_tbl th {border:0px;text-align:left;}
 #alert_info_tbl td {border:0px;} 	
 </style>
</head>
<body>
<%
  com.yahoo.dba.perf.myperf.common.AppUser u = com.yahoo.dba.perf.myperf.common.AppUser.class.cast(
  session.getAttribute(com.yahoo.dba.perf.myperf.common.AppUser.SESSION_ATTRIBUTE));
%>

<div>
<span id="common_msg"> <!-- common message line --></span>
<div>
<div><!-- query form -->
  <table id="db_block" style="margin-bottom:6px;">
    <tr><td>Group Name</td><td>DB Host</td><td>Start Date</td><td>Time</td><td>End Date</td><td>Time</td><td>&nbsp;</td></tr>
    <tr><td><select name="dbgroup" id="dbgroup" >
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
        </td>
        <td><select  name="host" id="host">
             <option value=""> --- </option>
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
        </td>
        <td><input type="hidden"  name="begin_time" id="begin_time" value="${start_date} ${start_hour}:00:00"/>
        <input type="text"  name="begin_time_d" id="begin_time_d" value="${start_date}" class="tcal" style="width:100px;"/></td>
        <td><select id="begin_time_t" name="begin_time_t">
<c:forEach var="h" items="${hours}" varStatus="stat"> 
  <option value="${h}" ${h==start_hour?"selected":""}>${h}</option>
</c:forEach>
          </select></td>
        <td><input type="hidden"  name="end_time" id="end_time" value="${end_date} ${end_hour}:00:00"/>
        <input type="text"  name="end_time_d" id="end_time_d" value="${end_date}" class="tcal"  style="width:100px;"/></td>
        <td><select id="end_time_t" name="end_time_t">
<c:forEach var="h" items="${hours}" varStatus="stat"> 
  <option value="${h}" ${h==end_hour?"selected":""}>${h}</option>
</c:forEach>
          </select></td>
        <td><input type="button" id="btn_all_status" value="&#9658;" />&nbsp;&nbsp;<input type="button" id="btn_alert_settings" value="Settings" /></td>
      </tr>
   </table>       
</div><!-- end of query form -->

<script language="javascript">
  $("#begin_time_d").datepicker({dateFormat: 'yy-mm-dd'});
  $("#end_time_d").datepicker({dateFormat: 'yy-mm-dd'});
</script>
<div id="all_alerts_tbl_div">
  <table id="all_alerts_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
</div>
</div>
<div id="alert_settings" style="position:absolute;top:70px;left:20px;width:600px;background-color:white;border:1px solid silver;display:none;z-index:1000;">
   <div id="alert_settings_title" style="position:relative;padding:8px 28px 8px 8px;min-height:13px;height:13px;font-weight:bold;color:white;background-color:#3961c5;">Alert Settings</div>
   <div id="alert_settings_main" style="font-size:12px;margin-top:10px;padding-left:20px;padding-right:8px;width:580px;height:240px;">
     <span style="font-weight:bold;" id="alert_settings_msg"></span>   
     <div id="alert_settings_tbl">
        <table id="alert_info_tbl">
          <tr><th>Metrics Name: </th><th>Threshold</th></tr>
          <tr><td>CPU(%): </td><td><input name="alert_settings_cpu" id="alert_settings_cpu" size="10" maxlength="2" /></td></tr>
          <tr><td>IO WAIT(%): </td><td><input name="alert_settings_io" id="alert_settings_io" size="10" maxlength="2" /></td></tr>
          <tr><td>Load Average: </td><td><input name="alert_settings_loadavg" id="alert_settings_loadavg" size="10" maxlength="4" /></td></tr>
          <tr><td>Disk Usage(%): </td><td><input name="alert_settings_diskusage" id="alert_settings_diskusage" size="10" maxlength="4" /></td></tr>
          <tr><td>Concurrency(threads_running): </td><td><input name="alert_settings_thread" id="alert_settings_thread" size="10" maxlength="4" /></td></tr>
          <tr><td>Replication Lag(seconds): </td><td><input name="alert_settings_repllag" id="alert_settings_repllag" size="10" maxlength="4" /></td></tr>
          <tr><td>Connection Failures(per second): </td><td><input name="alert_settings_aborted_cc" id="alert_settings_aborted_cc" size="10" maxlength="4" /></td></tr>
          <tr><td>Slow Queries(per minute): </td><td><input name="alert_settings_slow" id="alert_settings_slow" size="10" maxlength="4" /></td></tr>
          <tr><td>Deadlocks (per period): </td><td><input name="alert_settings_deadlock" id="alert_settings_deadlock" size="10" maxlength="4" /></td></tr>
          <tr><td>Swap(out, per period): </td><td><input name="alert_settings_swapout" id="alert_settings_swapout" size="10" /></td></tr>
          <tr><td>Emails(comma separated): </td><td><input name="alert_settings_emails" id="alert_settings_emails" size="60"/></td></tr>
          <tr id="tr_alerts_enabled"><td>Alerts </td><td><input type="checkbox" name="alerts_enabled" id="alerts_enabled" value="1" />&nbsp;check to enable</td></tr>
          <tr id="tr_metrics_enabled"><td>Metrics Gathering </td><td><input type="checkbox" name="metrics_enabled" id="metrics_enabled"/>&nbsp;check to enable</td></tr>
          <tr id="tr_snmp_enabled"><td>SNMP Gathering </td><td><input type="checkbox" name="snmp_enabled" id="snmp_enabled"/>&nbsp;check to enable</td></tr>
        </table>
     </div>
   </div>
   <div style="text-align:right;padding-right:24px;padding-top:4px;padding-bottom:4px;background-color:#EDF5FF;"><span><input type="button" id="alert_settings_update" name="alert_settings_update" value="Update" /></span>&nbsp;&nbsp;&nbsp;&nbsp;<span style="display:inline-box;"><input type="button" style="padding:.4em 1em .45em;" onclick="showHideOne('alert_settings','none');" value="Close" /></span></div>
</div>
<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>

<script language="javascript">
$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host', true);
  }
);

function resolveDates()
{
  mydom("begin_time").value = mydomval("begin_time_d") + " " + mydomval("begin_time_t") + ":00:00";
  mydom("end_time").value = mydomval("end_time_d") + " " + mydomval("end_time_t") + ":00:00";
}

var alertsTable = new JSTable({
   	   name: "all_alerts",
   	   query:{
   	     queryURL: "alerts.html",
   	     sqlId: "all_alerts",
   	     paramFields:[{name: "start", valueField: "begin_time"}, {name: "end", valueField: "end_time"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   //tab:[{tabComponent:sessTabview, tabIndex:0}],
   	   formatter:{rowFormatter:jqueryStylingAlert, columnFormatters:{"DBGROUP": jqueryFormatAlert}},
   	   handlers: {jquery:1},
   	   showRowDataOnClick:"y"
});
	
$('#btn_all_status').click(function()
{
  resolveDates();
  alertsTable.sendQuery();
});
	
$('#btn_alert_settings').click(function()
{
  retrieveSettings();
});
	
function retrieveSettings()
{
  var mydata = "group="+escape(mydomval("dbgroup"))+"&host="+escape(mydomval("host")) + "&cmd=get_settings";
    
  $.ajax({
      url: 'alerts.html',
      method: 'POST',
      data: mydata,
      success:function(jsonObj)
      {
        if(jsonObj==null)
        {
         	reportStatus(true, 'common_msg', 'Failed to retrieve alert settings.');
        }else
        {
                if(jsonObj.updatable=='y')
                	mydom("alert_settings_update").disabled = false;
                else
                   mydom("alert_settings_update").disabled = true;
                
                mydom("alert_settings_cpu").value = jsonObj.cpu;  
                mydom("alert_settings_io").value = jsonObj.io;  
                mydom("alert_settings_loadavg").value = jsonObj.loadavg;  
                mydom("alert_settings_diskusage").value = jsonObj.diskusage;  
                mydom("alert_settings_thread").value = jsonObj.thread;  
                mydom("alert_settings_slow").value = jsonObj.slow;  
                mydom("alert_settings_deadlock").value = jsonObj.deadlock;  
                mydom("alert_settings_swapout").value = jsonObj.swapout;  
                mydom("alert_settings_aborted_cc").value = jsonObj.aborted_cc;  
                mydom("alert_settings_repllag").value = jsonObj.repllag;
                mydom("alert_settings_emails").value = jsonObj.emails;
                
                if(jsonObj.alerts == null)
                {
                  mydom("tr_alerts_enabled").style.display="none";
                  mydom("tr_metrics_enabled").style.display="none";
                  mydom("tr_snmp_enabled").style.display="none";
                }else
                {
                  mydom("tr_alerts_enabled").style.display="table-row";
                  mydom("tr_metrics_enabled").style.display="table-row";
                  mydom("tr_snmp_enabled").style.display="table-row";
                  mydom("alerts_enabled").checked = (jsonObj.alerts=='y');
                  mydom("metrics_enabled").checked = (jsonObj.metrics=='y');
                  mydom("snmp_enabled").checked = (jsonObj.snmp=='y');
                }
<% if(!u.isAdminUser()) {%>
               mydom("alerts_enabled").disabled = true;
               mydom("metrics_enabled").disabled = true;
               mydom("snmp_enabled").disabled = true;
               mydom("alert_settings_update").disabled = true; 
<% } %>                
                showHideOne("alert_settings", "block");
        }        
      }
  });
}//retrieveSettings

<% if(u.isAdminUser()) {%>
$('#alert_settings_update').click(function(){
  updateSettings();
});
	
function updateSettings()
{
      var mydata = "group="+escape(mydomval("dbgroup"))+"&host="+escape(mydomval("host")) + "&cmd=update_settings";
   		  mydata += "&cpu="+ mydom("alert_settings_cpu").value;
   		  mydata += "&io="+ mydom("alert_settings_io").value;
   		  mydata += "&loadavg="+ mydom("alert_settings_loadavg").value;
   		  mydata += "&diskusage="+ mydom("alert_settings_diskusage").value;
   		  mydata += "&thread="+ mydom("alert_settings_thread").value;
   		  mydata += "&slow="+ mydom("alert_settings_slow").value;
   		  mydata += "&deadlock="+ mydom("alert_settings_deadlock").value;
   		  mydata += "&swapout="+ mydom("alert_settings_swapout").value;
   		  mydata += "&emails="+escape(mydom("alert_settings_emails").value);
   		  mydata += "&aborted_cc="+ mydom("alert_settings_aborted_cc").value;
   		  mydata += "&repllag="+ mydom("alert_settings_repllag").value;
   		  
      $.ajax({
        url: 'alerts.html',
        method: 'POST',
        data: mydata,
        success:function(jsonObj)
        {
              if(jsonObj==null)
              {
              	reportStatus(true, 'common_msg', 'Failed to update alert settings.');
              }else
              {
                reportStatus((jsonObj.resp.status != 0), 'common_msg', jsonObj.resp.message);
              }
        }
      });
}

$('#alerts_enabled').click(function()
{
  enableDisableFeature({url:"alerts.html", feature:"alerts", field:"alerts_enabled", msgField: "common_msg", group:"dbgroup", host: "host"});
});

$('#metrics_enabled').click(function()
{
  enableDisableFeature({url:"alerts.html", feature:"metrics gathering", field:"metrics_enabled", msgField: "common_msg", group:"dbgroup", host: "host"});
});

$('#snmp_enabled').click(function()
{
  enableDisableFeature({url:"alerts.html", feature:"SNMP gathering", field:"snmp_enabled", msgField: "common_msg", group:"dbgroup", host: "host"});
});

<% } %>
//fill initial screen
{
  resolveDates();
  alertsTable.sendQuery();
}
</script>
</body>
</html>