<%@page trimDirectiveWhitespaces="true"%>
<%@page import="com.yahoo.dba.perf.myperf.common.*" %>
<%@ page import="java.util.*" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>DB Status</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<style>
   	#status_tabs {background: transparent; border:none;}
</style>
</head>
<%
    String rptFormat = "formatAlertNoDownload";
    if(session!=null && session.getAttribute(AppUser.SESSION_ATTRIBUTE) != null)
         rptFormat =  "formatAlert";	
%>
<body>
<div>
<span id="common_msg"> <!-- common message line --></span>
<div>Database Status&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <span><select name="dbgroup" id="dbgroup" >
             <option value="all">All</option>
              <c:forEach var="cluster" items="${mydbs}" varStatus="stat">
<c:if test="${cluster==sessionScope.group}">
    			<option value="${cluster}" selected="true" ${stat.index>=mydbSize?"style='color:red;'":""}>${cluster}</option>
</c:if>
<c:if test="${cluster!=sessionScope.group}">              
    			<option value="${cluster}" ${stat.index>=mydbSize?"style='color:red;'":""}>${cluster}</option>
</c:if>    			
  			  </c:forEach>          
          </select>
        </span>
        <input type="button" name="btn_all_status" id="btn_all_status" value="&#9658;" style="font-weight:bold;"/> 
</div>

<div id="status_tabs">
	<ul>
        <li><a href="#all_status_tbl_div" title="Current Health Status">Health Status</a></li>
        <li><a href="#all_alerts_tbl_div" title="Past 24 hours of alerts">24 Hour Alerts</a></li>
    </ul>
      
    <div id="all_status_tbl_div" class="datatableContainer">
        <table id="all_status_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    </div>
    <div id="all_alerts_tbl_div" class="datatableContainer">
         <div>Red color means the issue is still on going.</div>
        <table id="all_alerts_tbl" cellpadding="0" cellspacing="0" border="0"  class="display"></table>
    </div>
</div><!-- tab -->

</div>

<script language=javascript>
  $('#status_tabs').tabs();

   	var statusTable = new JSTable({
   	   name: "all_status",
   	   query:{
   	     queryURL: "status.html",
   	     sqlId: "all_status",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: null},
   	   //tab:[{tabComponent:sessTabview, tabIndex:0}],
   	   formatter:{rowFormatter:jqueryStylingStatus},
   	   handlers: {jquery:1},
   	   showRowDataOnClick:"y"
   	});
   	
   	var alertsTable = new JSTable({
   	   name: "all_alerts",
   	   query:{
   	     queryURL: "alerts.html",
   	     sqlId: "all_alerts",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: null},
   	   //tab:[{tabComponent:sessTabview, tabIndex:0}],
   	   formatter:{rowFormatter:jqueryStylingAlert, columnFormatters:{"DBGROUP": jqueryFormatAlert}},
   	   handlers: {jquery:1},
   	   showRowDataOnClick:"n"
   	});
	
	$('#btn_all_status').click(function(){
	  queryStatus();
	});

	function queryStatus()
    {
		statusTable.sendQuery();
		alertsTable.sendQuery();      
	}
    queryStatus(null, null);
</script>
</body>
</html>