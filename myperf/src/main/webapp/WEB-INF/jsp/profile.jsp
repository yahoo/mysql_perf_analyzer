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
<title>MySQL profiler</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
 <style>
 cssError {color:red;font-size:10px;}
 #dbinfo_tbl{border:0px;border-spacing:5px;}
 #dbinfo_tbl tr {border:0px;}
 #dbinfo_tbl th {border:0px;}
 #dbinfo_tbl td {border:0px;} 	
 </style>
</head>
<body>
<div style="margin-left:48px;">
  <span>
        <span>DB Group</span>
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
  <! --end of db selection -->

  <div>
    <span id="common_msg"></span>  
     <table border="0"  style="margin-top:12px;margin-bottom:12px;width:400px;">
         <tr>
         <td align="left" valign="top" style="border-right: 1px;border-left:0px; border-top:0px;border-bottom:0px;">
           <input type="checkbox" value="y" id="plan" name="plan" checked/>&nbsp;Explain Plan<br />
           <input type="checkbox" value="y" id="st" name="st" />&nbsp;Session Status<br />   
           <input type="checkbox" value="y" id="pf" name="pf" />&nbsp;Profile
         </td>
         <td align="right" valign="middle" style="border-left: 1px;border-right:0px; border-top:0px;border-bottom:0px;"> 
           Database &nbsp; <input type="text" name="dbname" id="dbname" />
         </td>
        </tr>
     </table>
     <div>
       <span>Query Text:</span><br />
         <textarea row="10" cols="50" id="sqltext" name="sqltext" style="width:700px;height:100px;margin-top:5px; margin-bottom:5px;"></textarea>
     </div>
     <input type="button" id="btn_profile" name="btn_profile" value="Submit"/>
  </div>
</div>        

<div id="profiletabs" class="clearTabView"> <!-- tab pane to display real time info in tabular formats -->
    <ul>
        <li><a href="#plan_tbl_div" title="Query Explain Plan">Plan</a></li>
        <li><a href="#stats_tbl_div" title="Session Stats">Session Stats</a></li>
        <li><a href="#profile_tbl_div" title="MySQL Profile">Profile Info</a></li>
    </ul>
    <div id="plan_tbl_div" class="datatableContainer">
      <table id="plan_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
    </div>
    <div id="stats_tbl_div" class="datatableContainer">
      <table id="stats_tbl" cellpadding="0" cellspacing="0" border="0" class="display" style="width:auto;"></table>
    </div>
    <div id="profile_tbl_div" class="datatableContainer">
      <table id="profile_tbl" cellpadding="0" cellspacing="0" border="0" class="display" style="width:auto;"></table>
    </div>
</div>
<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>
<script language="javascript">
$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host', true);
    //querySchemaName();
  }
);

$('#profiletabs').tabs();

$('#host').change(function(){querySchemaName();});

var schemas = [];

function querySchemaName()
{
  if(mydomval("host") == null || mydomval("host") =="")
    return;
  var mydata = "group="+encodeURIComponent(mydomval("dbgroup"))
               +"&host="+encodeURIComponent(mydomval("host"))
               +"&sql=mysql_show_databases"
               +"&seed="+Math.random();
               
  $.ajax({
       url: "query.html",
       data: mydata,
       type: 'GET',
       dataType: 'json',
       success: function(json)
       {
         if(json == null || json.resp == null || json.resp.results == null)
           return;
         var res = json.resp.results.results;
		 if(res.length>0)
		 {
		   schemas.length=0;
		   for(var dbi=0;dbi<res.length;dbi++)
		   {
		     schemas[dbi] =res[dbi].SCHEMA_NAME;		     
		   }//option loop
		     $("#dbname").autocomplete({source: schemas, minLength:0}) 
                    .bind('focus', function(){$(this).autocomplete("search");});
		   
         } //res
       }//sucess
  });//ajax       
}

var planTable = new JSTable({
   	   name: "plan",   	   
   	   query:{
   	     queryURL: "profile.html",
   	     formAction: "POST",
   	     paramFields:[{name:"plan", value:"y"},{name:"dbname", valueField:"dbname"}, 
   	     {name:"sqltext", valueField:"sqltext", enc:encodeURIComponent}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   tooltipCallbackOnClick: planTermCB,
   	   formatter:{commonFormatter:jqueryFormatPreserveSpace, columnFormatters:{"POSSIBLE_KEYS":jqueryFormatText60}}
   	});

var statsTable = new JSTable({
   	   name: "stats",
   	   query:{
   	     queryURL: "profile.html",
   	     formAction: "POST",
   	     paramFields:[{name:"st", value:"y"},{name:"dbname", valueField:"dbname"}, 
   	     {name:"sqltext", valueField:"sqltext", enc:encodeURIComponent}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler},
   	   tooltipCallbackOnClick: statTermCB
   	});


var profileTable = new JSTable({
   	   name: "profile",
   	   query:{
   	     queryURL: "profile.html",
   	     formAction: "POST",
   	     paramFields:[{name:"pf", value:"y"},{name:"dbname", valueField:"dbname"}, 
   	     {name:"sqltext", valueField:"sqltext", enc:encodeURIComponent}]
   	   }, 
   	   db: {dbGroupId: "dbgroup", dbHost: "host"},
   	   handlers: {jquery:1, statusMessageHandler:messagehandler}
   	});


var invokeIndex = 0;
var invokeOrders = ['plan', 'st', 'pf'];
var invokdeTables = [planTable, statsTable, profileTable];

$('#btn_profile').click(function()
{
   invokeIndex = 0;
   startProfile(); 
});

function messagehandler(datatable, status, message)
{
	console.log("recieve message from "+ datatable.name+", status: "+status
	   + ", message: "+message);
	if(status != 0)
	     $('#common_msg').text(message);
	else if(status == 0)
	{
	  $('#common_msg').text('');//clear old message
	  if(invokeIndex > 0)   //invoke remaining tasks
        startProfile(); 
    }
      
}

function startProfile()
{
  for(var i = invokeIndex; i< invokeOrders.length; i++)
  {
    if(mydom(invokeOrders[i]).checked)
    {
      invokeIndex = (invokeIndex + 1) % invokeOrders.length;
      reportStatus(false, 'common_msg', 'In progress ...');      
      invokdeTables[i].sendQuery();
      return;
    }
  }
}  
</script>
</html>