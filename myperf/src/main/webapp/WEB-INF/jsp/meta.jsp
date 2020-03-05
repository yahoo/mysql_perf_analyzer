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
<title>Meta Data</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<style>
   	#databaseTabView {background: transparent; border:none;}
 	#tableTabView {background: transparent; border:none;}
 	#databaseTabView {background: transparent; border:none;}
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
          </select>&nbsp;
          <input type="button" value="Find" onclick="prepareDBSearch('dbgroup');"/>
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

        <input type="button" id="btn_databases" value="&#9658;"/>
   </span>
</div><!-- end of DB list -->

<span id="common_msg"> <!-- common message line -->
<c:if test="${mydbSize==0}">You have not provided any database credential yet. Please use <a href="<%= request.getContextPath() %>/db.htm">DB Credential</a> page to provide access information for databases you are interested in.</c:if>
</span>

<!-- schema -->
<div id="databases_div">
  <div style="font-weight:bold;">Databases/Schemas</div>
  <table id="mysql_meta_databases_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
</div>
<br />  

<!-- schema action -->
<div>
  Database: <input type="text" name="schema_name" id="schema_name" />&nbsp;
  <select name="meta_activity" id="meta_activity">
    <option value="mysql_meta_table_names">Tables</option>
    <option value="mysql_meta_view_names">Views</option>
    <option value="mysql_meta_schema_pk_check">Table With Missing PK</option>
  </select>&nbsp;
  <input type="button" id="btn_meta_activity" value="&#9658;"/>  
</div>

<!-- database tabs -->
<div id="databaseTabView">
   <ul>
     <li><a href="#mysql_meta_table_names_tbl_div" title="MySQL Tables By Database/schema">Tables</a></li>
     <li><a href="#mysql_meta_view_names_tbl_div" title="MySQL Views By Database/schema">Views</a></li>
     <li><a href="#tableTabView_div" title="MySQL Table Details">Table Details</a></li>
     <li><a href="#view_detail_div" title="MySQL View Details">View Details</a></li>
     <li><a href="#mysql_meta_schema_pk_check_tbl_div" title="MySQL Tables Without PK">Tables Missing PK</a></li>
   </ul>
   <div id="mysql_meta_table_names_tbl_div">
     <table id="mysql_meta_table_names_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
  </div>
  <div id="mysql_meta_view_names_tbl_div">
   <table id="mysql_meta_view_names_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
 </div>
   <!-- table detail view -->
   <div id="tableTabView_div">
     <div>
       Database: <input type="text" name="schema_name2" id="schema_name2" />&nbsp;
       Table: <input type="text" name="table_name" id="table_name" />&nbsp;
       <input type="button" id="btn_table_activity" value="View Table Detail"/>  
     </div>
     
     <!-- table detail tabs -->   
     <div id="tableTabView">
         <ul>
           <li><a href="#mysql_meta_table_columns_tbl_div" title="MySQL table columns">Columns</a></li>
           <li><a href="#mysql_meta_innodb_table_stats_tbl_div" title="MySQL Innodb table stats">Stats</a></li>         
           <li><a href="#mysql_meta_table_indexes_tbl_div" title="MySQL table indexes">Indexes</a></li>         
           <li><a href="#mysql_meta_table_create_tbl_div" title="MySQL table create code">Create Code</a></li>         
           <li><a href="#mysql_meta_table_constraints_tbl_div" title="MySQL table constraints">Constraints</a></li>         
           <li><a href="#mysql_meta_table_triggers_tbl_div" title="MySQL table triggers">Triggers</a></li>         
         </ul>
         <div id="mysql_meta_table_columns_tbl_div">
           <table id="mysql_meta_table_columns_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
         </div>  
         <div id="mysql_meta_innodb_table_stats_tbl_div">
           <table id="mysql_meta_table_stats_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
           <table id="mysql_meta_innodb_table_stats_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
         </div>
         <div id="mysql_meta_table_indexes_tbl_div">
           <table id="mysql_meta_table_indexes_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	       Innodb Index Stats:
           <table id="mysql_meta_innodb_index_stats_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
	     </div>
	     <div id="mysql_meta_table_create_tbl_div">
           <table id="mysql_meta_table_create_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
         </div>
         <div id="mysql_meta_table_constraints_tbl_div">
           <table id="mysql_meta_table_constraints_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
         </div>
         <div id="mysql_meta_table_triggers_tbl_div">
           <table id="mysql_meta_table_triggers_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
         </div>  
     </div>
     <!-- end of table detail tabs -->
   </div>
   <!-- end of table detail -->
   
   <!-- view detail -->
   <div id="view_detail_div">
     <div>
       Database: <input type="text" name="schema_name3" id="schema_name3" />&nbsp;
       View: <input type="text" name="view_name" id="view_name" />&nbsp;
       <input type="button" id="btn_view_activity" value="Show Create View"/>  
     </div>
     <table id="mysql_meta_view_create_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>         
   </div>
   <!-- view_detail_div -->
   <div id="mysql_meta_schema_pk_check_tbl_div">
     <table id="mysql_meta_schema_pk_check_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
   </div>  
</div>
<!-- end of database tabs -->

<!-- db search popup -->
<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>
<!-- end of db search popup -->

<script language="javascript">
$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host');
  }
);

//tabs
$('#databaseTabView').tabs();
$('#tableTabView').tabs();

var mysql_meta_databasesTable = new JSTable({
   	   name: "mysql_meta_databases",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_databases",
   	     paramFields:[]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, selectHandler:selectRow_mysql_meta_databases,
   	   contextMenuHandler:[
   	       {key: "tables", label: "Show Tables", handler: contextmenu_mysql_meta_databases},
   	       {key: "missing_pks", label: "Show Tables With No PK", handler: contextmenu_mysql_meta_databases},
   	       {key: "views", label: "Show Views", handler: contextmenu_mysql_meta_databases}
   	     ]}
   	});

function selectRow_mysql_meta_databases(obj)
{
  if(obj == null || obj.datatable == null)return;
  var val = obj.datatable.getCellValueByColumnName(obj.row, 'SCHEMA_NAME');
  mydom('schema_name').value = val != null? val: "";
  //TODO hightlight selected row
}
function contextmenu_mysql_meta_databases(obj)
{
  if(obj == null || obj.datatable == null)return;
  var val = obj.datatable.getCellValueByColumnName(obj.row, 'SCHEMA_NAME');
  mydom('schema_name').value = val != null? val: "";
  if(obj.key == "tables")
	mydom('meta_activity').selectedIndex = 0;
  else if(obj.key == "views")
	mydom('meta_activity').selectedIndex = 1;
  if(obj.key == "missing_pks")
	mydom('meta_activity').selectedIndex = 2;
  mydom('btn_meta_activity').click();
}
 
$('#btn_databases').click(function()
    {
      queryDatabases();
    }
  );   

function queryDatabases()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  
  mydom("schema_name").value="";
  //cleanTable("mysql_meta_table_names");
  //cleanTable("mysql_meta_view_names");
  mysql_meta_databasesTable.sendQuery();
}

var mysql_meta_table_namesTable = new JSTable({
   	   name: "mysql_meta_table_names",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_table_names",
   	     paramFields:[{name:"p_1", valueField:"schema_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, 
   	     selectHandler:selectRow_mysql_meta_table_names,
   	     contextMenuHandler:[
   	       {key: "show_svc_table_detail", label: "Show Table Detail", handler: contextmenu_mysql_meta_table_names}   	       
   	     ]
   	   }
   	});
   	
function selectRow_mysql_meta_table_names(obj)
{
  if(obj == null || obj.datatable == null)return;
  var val = obj.datatable.getCellValueByColumnName(obj.row, 'TABLE_NAME');
  if (val == null){ //mysql8
    var schema_name = mydomval("schema_name");
    if ( schema_name != null) schema_name = schema_name.toUpperCase();
    val = obj.datatable.getCellValueByColumnName(obj.row, 'TABLES_IN_' + schema_name);
  }
  mydom('table_name').value = val != null? val: "";
  mydom("schema_name2").value = mydomval("schema_name");
  //TODO hightlight selected row
}

function contextmenu_mysql_meta_table_names(obj)
{
  if(obj == null || obj.datatable == null)return;
  var val = obj.datatable.getCellValueByColumnName(obj.row, 'TABLE_NAME');
  if (val == null){ //mysql8
    var schema_name = mydomval("schema_name");
    if ( schema_name != null) schema_name = schema_name.toUpperCase();
    val = obj.datatable.getCellValueByColumnName(obj.row, 'TABLES_IN_' + schema_name);
  }
  mydom('table_name').value = val != null? val: "";
  mydom("schema_name2").value = mydomval("schema_name");
  //TODO hightlight selected row
  mydom('btn_table_activity').click();
}

$('#btn_meta_activity').click(function()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  if(!checkEmpty(mydomval("schema_name"), 'Please select a database/schema to start ...'))
	return;	  

  var act = mydomval('meta_activity');
  if(act=='mysql_meta_table_names')
  {
     mysql_meta_table_namesTable.sendQuery();
     $('#databaseTabView').tabs("option", "active", 0);     
  }
  else if(act=='mysql_meta_schema_pk_check')
  {
     mysql_meta_schema_pk_checkTable.sendQuery();
     $('#databaseTabView').tabs("option", "active", 4);
  }   
  else
  {
     mysql_meta_view_namesTable.sendQuery();
    $('#databaseTabView').tabs("option", "active", 1);   
  }
});

$("#btn_table_activity").click(function()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  if(!checkEmpty(mydomval("table_name"), 'Please select a schema and a table to start ...'))
	return;	  

  if(!checkEmpty(mydomval("schema_name2"), 'Please select a schema and a table to start ...'))
	return;	  

  //query table detail
  mysql_meta_table_columnsTable.sendQuery();
  mysql_meta_table_statsTable.sendQuery();
  mysql_meta_innodb_table_statsTable.sendQuery();
  mysql_meta_table_indexesTable.sendQuery();
  mysql_meta_innodb_index_statsTable.sendQuery();
  mysql_meta_table_createTable.sendQuery();
  mysql_meta_table_constraintsTable.sendQuery();
  mysql_meta_table_triggersTable.sendQuery();
  $('#databaseTabView').tabs("option", "active", 2);
});

var mysql_meta_view_namesTable = new JSTable({
   	   name: "mysql_meta_view_names",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_view_names",
   	     paramFields:[{name:"p_1", valueField:"schema_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, 
   	     selectHandler:selectRow_mysql_meta_view_names,
   	     contextMenuHandler:[
   	       {key: "show_svc_view_detail", label: "Show View Detail", handler: contextmenu_mysql_meta_view_names}   	       
   	     ]
   	   }
   	});

function selectRow_mysql_meta_view_names(obj)
{
  if(obj == null || obj.datatable == null)return;
  var val = obj.datatable.getCellValueByColumnName(obj.row, 'TABLE_NAME');
  if (val == null){ //mysql8
    var schema_name = mydomval("schema_name");
    if ( schema_name != null) schema_name = schema_name.toUpperCase();
    val = obj.datatable.getCellValueByColumnName(obj.row, 'TABLES_IN_' + schema_name);
  }
  mydom('view_name').value = val != null? val: "";
  mydom("schema_name2").value = mydomval("schema_name");
  //TODO hightlight selected row
}

function contextmenu_mysql_meta_view_names(obj)
{
  if(obj == null || obj.datatable == null)return;
  var val = obj.datatable.getCellValueByColumnName(obj.row, 'TABLE_NAME');
  if (val == null){ //mysql8
    var schema_name = mydomval("schema_name");
    if ( schema_name != null) schema_name = schema_name.toUpperCase();
    val = obj.datatable.getCellValueByColumnName(obj.row, 'TABLES_IN_' + schema_name);
  }
  mydom('view_name').value = val != null? val: "";
  mydom("schema_name3").value = mydomval("schema_name");
  //TODO hightlight selected row
  mydom('btn_view_activity').click();
}

var mysql_meta_schema_pk_checkTable = new JSTable({
   	   name: "mysql_meta_schema_pk_check",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_schema_pk_check",
   	     paramFields:[{name:"p_1", valueField:"schema_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, 
   	     selectHandler: selectRow_mysql_meta_table_names,
   	     contextMenuHandler:[
   	       {key: "show_svc_view_detail", label: "Show View Detail", handler: contextmenu_mysql_meta_table_names}   	       
   	     ]
   	   }
   	});

var mysql_meta_table_columnsTable = new JSTable({
   	   name: "mysql_meta_table_columns",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_table_columns",
   	     paramFields:[{name:"p_1", valueField:"schema_name2"}, {name:"p_2", valueField:"table_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1}
   	});

var mysql_meta_table_statsTable = new JSTable({
   	   name: "mysql_meta_table_stats",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_table_stats",
   	     paramFields:[{name:"p_1", valueField:"schema_name2"}, {name:"p_2", valueField:"table_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1},   	   
   	   formatter:{sortable:"n"}
   	});

var mysql_meta_innodb_table_statsTable = new JSTable({
   	   name: "mysql_meta_innodb_table_stats",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_innodb_table_stats",
   	     paramFields:[{name:"p_1", valueField:"schema_name2"}, {name:"p_2", valueField:"table_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1},   	   
   	   formatter:{sortable:"n"}
   	});

var mysql_meta_table_indexesTable = new JSTable({
   	   name: "mysql_meta_table_indexes",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_table_indexes",
   	     paramFields:[{name:"p_1", valueField:"schema_name2"}, {name:"p_2", valueField:"table_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1}
   	});

var mysql_meta_innodb_index_statsTable = new JSTable({
   	   name: "mysql_meta_innodb_index_stats",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_innodb_index_stats",
   	     paramFields:[{name:"p_1", valueField:"schema_name2"}, {name:"p_2", valueField:"table_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1}
   	});

var mysql_meta_table_createTable = new JSTable({
   	   name: "mysql_meta_table_create",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_table_create",
   	     paramFields:[{name:"p_1", valueField:"schema_name2"}, {name:"p_2", valueField:"table_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1},
   	   formatter:{columnFormatters:{"Create Table":jqueryFormatSqlText},sortable:"n"}
   	});
//	mysql_meta_table_createTbl.setColumnFormatter("formatMySqlText");  

var mysql_meta_table_constraintsTable = new JSTable({
   	   name: "mysql_meta_table_constraints",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_table_constraints",
   	     paramFields:[{name:"p_1", valueField:"schema_name2"}, {name:"p_2", valueField:"table_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1}
   	});

var mysql_meta_table_triggersTable = new JSTable({
   	   name: "mysql_meta_table_triggers",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_table_triggers",
   	     paramFields:[{name:"p_1", valueField:"schema_name2"}, {name:"p_2", valueField:"table_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1}
   	});

var mysql_meta_view_createTable = new JSTable({
   	   name: "mysql_meta_view_create",
   	   query:{
   	     queryURL: "query.html",
   	     sqlId: "mysql_meta_view_create",
   	     paramFields:[{name:"p_1", valueField:"schema_name3"}, {name:"p_2", valueField:"view_name"}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1},
   	   formatter:{columnFormatters:{"Create View":jqueryFormatSqlText}, sortable:"n"}
   	});

$('#btn_view_activity').click(function()
{
  if(!checkDBSelection(mydomval("dbgroup"), host=mydomval("host")))
    return;
  if(!checkEmpty(mydomval("view_name"), 'Please select a schema and a view to start ...'))
	return;	  

  if(!checkEmpty(mydomval("schema_name3"), 'Please select a schema and a view to start ...'))
	return;	  
  
  mysql_meta_view_createTable.sendQuery();
  $('#databaseTabView').tabs("option", "active", 3);
  
});
</script>
</body>
</html>