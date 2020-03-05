<%@page trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Settings</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<style>
  #scanner_cfg_tbl td{padding:10px;}
  #alert_info_tbl td{padding:10px;}
  #alert_info_tbl th {text-align:left;}
  .snmpv3 {display:none;}
</style>
</head>
<body>
<%
  com.yahoo.dba.perf.myperf.common.AppUser u = com.yahoo.dba.perf.myperf.common.AppUser.class.cast(
  session.getAttribute(com.yahoo.dba.perf.myperf.common.AppUser.SESSION_ATTRIBUTE));
%>
<div style="margin-left:48px;" id="page_main">
  <span class="cssError" id="common_msg">${message}</span>
  <div id="settingsTab" class="clearTabView">
    <ul>
      <li><a href="#scanner_div" title="Configure metrics storage, scheduling and retention">Metrics Gathering</a></li>
      <li><a href="#alerts_div" title="Customize alert thresholds, blackouts, etc">Alert Settings</a></li>
      <li><a href="#snmp_div" title="Customize SNMP community and version settings">SNMP Settings</a></li>
    </ul>
    <div id="scanner_div"><!-- metrics gathering configuration -->
<% if(u.isAdminUser()){ %>   
        <div id="scanner_act_div">
         <c:if test="${scanner_running}">         
           <input type="button" id="btn_start" value="Restart Scanner" />
            &nbsp;&nbsp;<input id="btn_stop" type="button" value="Stop Scanner"/>
        </c:if>
         <c:if test="${!scanner_running}">         
           <input type="button" id="btn_start" value="Start Scanner" />
            &nbsp;&nbsp;<input id="btn_stop" type="button" value="Stop Scanner" disabled />
        </c:if>
        </div>
<% } %>
      <fieldset style="width:600px;">
        <legend>Metrics Scan Configurations</legend>
	    <table border="0" id="scanner_cfg_tbl">
        <tr>
          <td>Admin Email: </td>
          <td><input type="text" id="adminemail" name="adminemail" value="${config.adminEmail}" required autofocus/></td>
        </tr>
        <tr>
          <td>User to be used for auto scan task: </td>
          <td><input type="text" id="username" name="username" value="${config.metricsScannerUser}" required /></td>
        </tr>
        <tr>
          <td>Metrics Scan Interval: </td>
          <td><select name="runtimeScanIntervalSeconds" id="runtimeScanIntervalSeconds">
              <option value="1" ${config.scannerIntervalSeconds==1?"selected":""}>Each second</option>
              <option value="5" ${config.scannerIntervalSeconds==5?"selected":""}>Each 5 seconds</option>
              <option value="10" ${config.scannerIntervalSeconds==10?"selected":""}>Each 10 seconds</option>
              <option value="15" ${config.scannerIntervalSeconds==15?"selected":""}>Each 15 seconds</option>
              <option value="30" ${config.scannerIntervalSeconds==30?"selected":""}>Each 30 seconds</option>
              <option value="60" ${config.scannerIntervalSeconds==60?"selected":""}>Each minute</option>
              <option value="300" ${config.scannerIntervalSeconds==300?"selected":""}>Each 5 minutes</option>
              <option value="600" ${config.scannerIntervalSeconds==600?"selected":""}>Each 10 minutes</option>
              <option value="1800" ${config.scannerIntervalSeconds==1800?"selected":""}>Each half hour</option>
              <option value="3600" ${config.scannerIntervalSeconds==3600?"selected":""}>Each hour</option>
           </select>
          </td>
        </tr>     
        <tr>
          <td>User Alert Scan Interval: </td>
          <td><select name="alertScanIntervalSeconds" id="alertScanIntervalSeconds">
              <option value="60" ${config.alertScanIntervalSeconds==60?"selected":""}>Each minute</option>
              <option value="300" ${config.alertScanIntervalSeconds==300?"selected":""}>Each 5 minutes</option>
              <option value="600" ${config.alertScanIntervalSeconds==600?"selected":""}>Each 10 minutes</option>
              <option value="1800" ${config.alertScanIntervalSeconds==1800?"selected":""}>Each half hour</option>
              <option value="3600" ${config.alertScanIntervalSeconds==3600?"selected":""}>Each hour</option>
           </select>
          </td>
        </tr>     
        <tr>
          <td>Days to Retain: </td>
            <td><input type="text" id="runtimeRecordRententionCount" name="runtimeRecordRententionCount" value="${config.recordRententionDays}" required/></td>
        </tr>     
        <tr>
          <td>Number of Threads: </td>
          <td><input type="text" id="threadCount" name="threadCount" value="${config.scannerThreadCount}" required/></td>
        </tr>
        <tr>
          <td>Alert Notification Emails: </td>
          <td><input type="text" id="notificationEmails" name="notificationEmails" value="${config.alertNotificationEmails}"/></td>
        </tr>
        <tr>
          <td><label for="hipchatUrl" title="hipchat room integration URL, for example, in the form of https://my_company.hipchat.com/v2/room/my_room_number/notification?">Alert Hipchat URL: </label></td>
          <td><input type="text" id="hipchatUrl" name="hipchatUrl" value="${config.hipchatUrl}"/></td>
        </tr>
        <tr>
          <td><lable for="hipchatAuthToken" title="hipchat room integration authtoken">Alert Hipchat AuthToken: </label></td>
          <td><input type="password" id="hipchatAuthToken" name="hipchatAuthToken"/></td>
        </tr>
        <tr>
          <td>Reuse Monitor User Connections? </td>
          <td><input type="checkbox" id="reuseMonUserConnction" name="reuseMonUserConnction" value="y" ${config.reuseMonUserConnction?"checked":""}  /></td>
        </tr>
        <tr>
          <td>Metrics Store: </td>
          <td><select name="metricsDbType" id="metricsDbType">
                <option value="derby" ${config.metricsDbType!="mysql"?"selected":""}>Build In (Derby)</option>
                <option value="mysql" ${config.metricsDbType=="mysql"?"selected":""}>MySQL</option>
              </select></td>
        </tr>
        <tr><td colspan="2">If MySQL database is used to store metrics, make sure you have the database schema created and the MySQL user to have all privileges on the database schema.</td></tr> 
        <tr>
           <td>MySQL Host: </td>
           <td><input type="text" id="metricsDbHost" name="metricsDbHost" value="${config.metricsDbHost}"/></td>
        </tr>     
        <tr>
          <td>MySQL Port: </td>
          <td><input type="text" id="metricsDbPort" name="metricsDbPort" value="${config.metricsDbPort}"/></td>
        </tr>     
        <tr>
          <td>MySQL DB (schema): </td>
          <td><input type="text" id="metricsDbName" name="metricsDbName" value="${config.metricsDbName}"/></td>
        </tr>     
        <tr>
          <td>MySQL User: </td>
          <td><input type="text" id="metricsDbUserName" name="metricsDbUserName" value="${config.metricsDbUserName}"/></td>
        </tr>     
        <tr>
          <td>MySQL Password: </td>
          <td><input type="password" id="metricsDbPassword" name="metricsDbPassword" /></td>
        </tr>
<% if (u.isAdminUser()) {%>
          <tr>
             <td></td>
             <td><input type="button" id="btn_scanner_config" name="btn_scanner_config" align="center" value="Submit"/></td>
          </tr>
<% } %>
	    </table>
      </fieldset>
    </div><!-- end of metrics gathering -->
    <div id="alerts_div">
      <div><!-- db selection -->
        <table id="db_block" style="margin-bottom:6px;">
          <tr><td>Group Name</td><td>DB Host</td><td>&nbsp;</td></tr>
          <tr><td><select name="dbgroup" id="dbgroup" >
                 <option value="">----</option>
                 <c:forEach var="cluster" items="${mydbs}" varStatus="stat">
    		    	<option value="${cluster}">${cluster}</option>
  			     </c:forEach>          
                 </select>&nbsp;<input type="button" value="Find" onclick="prepareDBSearch('dbgroup');"/>
              </td>
              <td><select  name="host" id="host">
                    <option value=""> --- </option>
                    <c:forEach var="db" items="${dbMap[sessionScope.group].instances}">
    			      <option value="${db.hostName}">${db.hostName}</option>
                    </c:forEach>
                  </select>
              </td>
              <td>&nbsp;&nbsp;<input type="button" id="btn_alert_settings" value="Settings" /></td>
          </tr>
        </table>
      </div><!-- end of db selection -->
      <div id="alert_settings">
        <fieldset style="width:600px;">
          <legend>Metrics And Alert Settings</legend>
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
              <tr><td>Deadlocks(per period): </td><td><input name="alert_settings_deadlock" id="alert_settings_deadlock" size="10" maxlength="4" /></td></tr>
              <tr><td>Swap(out, per period): </td><td><input name="alert_settings_swapout" id="alert_settings_swapout" size="10" /></td></tr>
              <tr><td>Emails(comma separated): </td><td><input name="alert_settings_emails" id="alert_settings_emails" size="60"/></td></tr>
              <tr id="tr_alerts_enabled"><td>Alerts </td><td><input type="checkbox" name="alerts_enabled" id="alerts_enabled" value="1" />&nbsp;check to enable</td></tr>
              <tr id="tr_metrics_enabled"><td>Metrics Gathering </td><td><input type="checkbox" name="metrics_enabled" id="metrics_enabled"/>&nbsp;check to enable</td></tr>
              <tr id="tr_snmp_enabled"><td>SNMP Gathering </td><td><input type="checkbox" name="snmp_enabled" id="snmp_enabled"/>&nbsp;check to enable</td></tr>
              <tr><td>&nbsp;</td><td><input type="button" id="alert_settings_update" name="alert_settings_update" value="Update" /></td></tr>
            </table>
          </fieldset>  
      </div><!-- end of alert_settings_main -->    
    </div><!-- end of alert settings -->

    <div id="snmp_div">
      <div><!-- db selection -->
        <table id="snmp_db_block" style="margin-bottom:6px;">
          <tr><td>Group Name</td><td>DB Host</td><td>&nbsp;</td></tr>
          <tr><td><select name="snmp_dbgroup" id="snmp_dbgroup" >
                 <option value="">----</option>
                 <c:forEach var="cluster" items="${mydbs}" varStatus="stat">
    		    	<option value="${cluster}">${cluster}</option>
  			     </c:forEach>          
                 </select>&nbsp;<input type="button" value="Find" onclick="prepareDBSearch('snmp_dbgroup');"/>
              </td>
              <td><select  name="snmp_host" id="snmp_host">
                    <option value=""> --- </option>
                    <c:forEach var="db" items="${dbMap[sessionScope.group].instances}">
    			      <option value="${db.hostName}">${db.hostName}</option>
                    </c:forEach>
                  </select>
              </td>
              <td>&nbsp;&nbsp;<input type="button" id="btn_snmp_settings" value="Retrieve Settings" /></td>
          </tr>
        </table>
      </div><!-- end of db selection -->
      <div id="snmp_settings">
        <fieldset style="width:600px;">
          <legend>SNMP Community/Version Settings</legend>
            <table id="snmp_info_tbl">
              <tr><td>Enabled? </td><td><input type="checkbox" name="snmp_settings_enabled" id = "snmp_settings_enabled" value="yes"/></td></tr>
              <tr><td>Community: </td><td><input name="snmp_settings_community" id="snmp_settings_community" size="20" /></td></tr>
              <tr><td>Version: </td><td><select name="snmp_settings_version" id="snmp_settings_version">
                <option value="1">1</option>
                <option value="2c" selected>2c</option>
                <option value="3">3</option>                
              </select></td></tr>
              <tr class="snmpv3"><td>User Name(v3): </td><td><input name="snmp_settings_username" id="snmp_settings_username" size="20" /></td></tr>
              <tr class="snmpv3"><td>Password(v3): </td><td><input type="password" name="snmp_settings_password" id="snmp_settings_password" size="20" /></td></tr>
              <tr class="snmpv3"><td>Auth Protocol(v3): </td><td><select name="snmp_settings_authprotocol" id="snmp_settings_authprotocol">
                <option value="">---</option>
                <option value="MD5">MD4</option>
                <option value="SHA">SHA</option>
              </select></td></tr>
              <tr class="snmpv3"><td>Privacy Passphrase(v3): </td><td><input type="password" name="snmp_settings_passphrase" id="snmp_settings_passphrase" size="20" /></td></tr>
              <tr class="snmpv3"><td>Privacy Protocol(v3): </td><td><select name="snmp_settings_privacyprotocol" id="snmp_settings_privacyprotocol">
                <option value="">---</option>
                <option value="DES">DES</option>
              </select></td></tr>
              <tr class="snmpv3"><td>Context: </td><td><input name="snmp_settings_context" id="snmp_settings_context" size="20" /></td></tr>
              <tr><td colspan="2">Notes: the default version is v2c and default community is public. If both group and host are specified, 
                the setting will be applied to the specific host only. If only group name is specified, the setting will be applied to all
                hosts in that group except the ones specified at host level. If none of the group and host is specified, the setting will be applied
                to all groups/hosts except the ones specified at host or group level.
              </td></tr>
              <tr><td>&nbsp;</td><td><input type="button" id="snmp_settings_update" name="snmp_settings_update" value="Update" /></td></tr>
            </table>
          </fieldset>  
      </div><!-- end of alert_settings_main -->    
    </div><!-- end of snmp settings -->

  </div> <!-- end of settingsTab -->   
</div><!-- end of main_div -->  

<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>

<script language="javascript">
$('#settingsTab').tabs();
$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host', true);
  }
);
$('#snmp_dbgroup').change(function()
  {
    query_hostlist_main(mydomval('snmp_dbgroup'), 'snmp_host', true);
  }
);

$('#snmp_settings_version').change(function()
  {
    if(mydomval("snmp_settings_version") == "3")
      $(".snmpv3").css("display", "table-row");
    else
      $(".snmpv3").css("display", "none");
  }
);

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

$('#btn_snmp_settings').click(function()
{
  retrieveSnmpSettings();
});
	
function retrieveSnmpSettings()
{
  var mydata = "group="+escape(mydomval("snmp_dbgroup"))+"&host="+escape(mydomval("snmp_host")) + "&task=fetchsnmp";
    
  $.ajax({
      url: 'settings.html',
      method: 'POST',
      data: mydata,
      success:function(jsonObj)
      {
        if(jsonObj==null || jsonObj.status != 0)
        {
         	reportStatus(true, 'common_msg', 'Failed to retrieve snmp settings.');
        }else
        {
          var snmp_enabled = jsonObj.enabled;
          mydom("snmp_settings_enabled").checked = snmp_enabled=="yes"?true:false;
          mydom("snmp_settings_community").value = jsonObj.community;
          setSelect("snmp_settings_version", jsonObj.version);
          if(jsonObj.version == "3")
          {
            mydom("snmp_settings_username").value = jsonObj.username;
            if(jsonObj.context != null)mydom("snmp_settings_context").value = jsonObj.context;
            setSelect("snmp_settings_authprotocol", jsonObj.authprotocol);
            setSelect("snmp_settings_privacyprotocol", jsonObj.privacyprotocol);
            $('.snmpv3').css("display", "table-row");            
          }else
            $('.snmpv3').css("display", "none");
<% if(!u.isAdminUser()) {%>
               mydom("snmp_settings_update").disabled = true; 
<% } %>                
        //showHideOne("snmp_settings", "block");
        }        
      }
  });
}//retrieveSnmpSettings

<% if (u.isAdminUser()) {%>
// most functions here are only for admin user

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
   		  mydata += "&aborted_cc="+ mydom("alert_settings_aborted_cc").value;
   		  mydata += "&repllag="+ mydom("alert_settings_repllag").value;
   		  mydata += "&emails="+escape(mydom("alert_settings_emails").value);
   		  
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

$('#snmp_settings_update').click(function(){
  updateSnmpSettings();
});
	
function updateSnmpSettings()
{
      var mydata = "group="+escape(mydomval("snmp_dbgroup"))+"&host="+escape(mydomval("snmp_host")) + "&task=updatesnmp";
   		  mydata += "&community="+ escape(mydomval("snmp_settings_community"));
   		  mydata += "&version="+ mydomval("snmp_settings_version");
   		  mydata += mydom("snmp_settings_enabled").checked?"&enabled=yes":"&enabled=no";
   		  if(mydomval("snmp_settings_version") == "3")
   		  {
   		    mydata += "&username="+ escape(mydomval("snmp_settings_username"));
   		    mydata += "&password="+ escape(mydomval("snmp_settings_password"));
   		    mydata += "&authprotocol="+ escape(mydomval("snmp_settings_authprotocol"));
   		    mydata += "&privacypassphrase="+ escape(mydomval("snmp_settings_passphrase"));
   		    mydata += "&privacyprotocol="+ escape(mydomval("snmp_settings_privacyprotocol"));
   		    mydata += "&context="+ escape(mydomval("snmp_settings_context"));
   		  }
      $.ajax({
        url: 'settings.html',
        method: 'POST',
        data: mydata,
        success:function(jsonObj)
        {
              if(jsonObj==null)
              {
              	reportStatus(true, 'common_msg', 'Failed to update snmp settings.');
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

$('#btn_scanner_config').click(function()
{
  updateScanner();
});

function updateScanner()
{
  var username = mydomval("username");
  if(!checkEmpty(username, "Please provide the user to be used for auto scan task"))
    return;
  var mydata = "task=update";
  mydata += "&adminemail="+escape(mydomval("adminemail"));
  mydata += "&username="+escape(username);
  mydata += "&runtimeScanIntervalSeconds=" + escape(mydomval("runtimeScanIntervalSeconds"));
  mydata += "&alertScanIntervalSeconds=" + escape(mydomval("alertScanIntervalSeconds"));
  mydata += "&runtimeRecordRententionCount=" + escape(mydomval("runtimeRecordRententionCount"));
  mydata += "&threadCount="+escape(mydomval("threadCount"));
  mydata += "&notificationEmails=" + escape(mydomval("notificationEmails"));
  if(mydom("reuseMonUserConnction").checked)
    mydata += "&reuseMonUserConnction=y";
  else
    mydata += "&reuseMonUserConnction=n";
  mydata += "&hipchatUrl=" + escape(mydomval("hipchatUrl"));
  mydata += "&hipchatAuthToken=" + escape(mydomval("hipchatAuthToken"));
    
  mydata += "&metricsDbType=" + escape(mydomval("metricsDbType"));
  mydata += "&metricsDbHost=" + escape(mydomval("metricsDbHost"));
  mydata += "&metricsDbPort=" + escape(mydomval("metricsDbPort"));
  mydata += "&metricsDbName=" + escape(mydomval("metricsDbName"));
  mydata += "&metricsDbUserName=" + escape(mydomval("metricsDbUserName"));
  mydata += "&metricsDbPassword=" +  escape(mydomval("metricsDbPassword"));  
  mydom("metricsDbPassword").value = "";//clear pwd
  
  $.ajax({
      url: 'settings.html',
      method: 'POST',
      data: mydata,
      success:function(jsonObj)
      {
        reportStatus((jsonObj.resp.status != 0), "common_msg", jsonObj.resp.message);
      }
   });
   mydata = "";
}

$('#btn_start').click(function()
{
  var mydata = "task=start&seed="+Math.random();
  $.ajax({
      url: 'settings.html',
      method: 'GET',
      data: mydata,
      success:function(jsonObj)
      {        
        reportStatus((jsonObj.resp.status != 0), "common_msg", jsonObj.resp.message);
        if(jsonObj.resp.status == 0)
        {
          mydom('btn_start').value = "Restart Scanner";
          mydom('btn_stop').disabled = false;
        }
      }
   });  
});

$('#btn_stop').click(function()
{
  var mydata = "task=stop&seed="+Math.random();
  $.ajax({
      url: 'settings.html',
      method: 'GET',
      data: mydata,
      success:function(jsonObj)
      { 
        reportStatus((jsonObj.resp.status != 0), "common_msg", jsonObj.resp.message);
        if(jsonObj.resp.status == 0)
          mydom('btn_start').value = "Start Scanner";
      }
   });    
});
<% } %>

retrieveSettings();
</script>
</body>
</html>