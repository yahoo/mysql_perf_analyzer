<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="com.yahoo.dba.perf.myperf.ui.*" %> 
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
<title>Test Page</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
</head>
<body>
<div><!-- query form -->  
<form method="get" action="query.html">
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
  			  </select>         
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
</form>  
</div><!-- end of query form -->
<span id="common_msg"> <!-- common message line -->
<c:if test="${mydbSize==0}">You have not provided any database credential yet. Please use <a href="<%= request.getContextPath() %>/db.htm">DB Credential</a> page to provide access information for databases you are interested in.</c:if>
</span>

<div>
        Database: <input type="text" name="keyword" id="keyword" />&nbsp;
        <select name="meta_activity" id="meta_activity">
          <option value="mysql_processlist">process list</option>
          <option value="mysql_global_status_metrics">global status</option>
        </select>&nbsp;
        <input type="button" id="btn_meta_activity" value="&#9658;"/>  
</div>
<div id="databaseTabView" class="clearTabView">
   <ul>
     <li><a href="#mysql_processlist_tbl">Process List</a></li>
     <li><a href="#mysql_global_status_metrics_tbl">global status</a></li>
   </ul>
   <div id="mysql_processlist_tbl_div">
     <table id="mysql_processlist_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
   </div>
   <div id="mysql_global_status_metrics_tbl_div">  
     <table id="mysql_global_status_metrics_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
   </div>
</div> <!-- databases tab-->

<script language=javascript>
$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host');
  }
);
$('#databaseTabView').tabs();

var firstTable = new JSTable({
   	   name: "mysql_processlist",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_processlist",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   tab:[{tabComponent:databaseTabView, tabIndex:0}],
   	   handlers: {jquery:1}
   	});
   	
var secondTable = new JSTable({
   	   name: "mysql_global_status_metrics",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_global_status_metrics",
   	     paramFields:[{name:"p_1", valueField:"keyword"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   diff: {keyColumns:["VARIABLE_NAME"], valueColumns:["VARIABLE_VALUE"]},
   	   tab:[{tabComponent:databaseTabView, tabIndex:1}],
   	   handlers: {jquery:1}
   	});

$('#btn_meta_activity').click(function()
{
  queryDatabases();
});

function queryDatabases()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
	  
	  firstTable.sendQuery();
	  secondTable.sendQuery();
}
</script>
</body>
</html>