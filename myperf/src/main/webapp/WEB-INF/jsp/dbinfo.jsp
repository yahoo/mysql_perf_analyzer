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
<title>Database Instance Information</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<style>
  .cssError {color:red;font-size:10px;}
   #dbinfo_tbl{border:0px;border-spacing:0px;}
   #dbinfo_tbl tr {border:0px;}
   #dbinfo_tbl th {border:0px;}
   #dbinfo_tbl td {border:0px; padding: 10px;} 	
   #dbinfo_tb_2{border:0px;border-spacing:0px;}
   #dbinfo_tbl_2 tr {border:0px;}
   #dbinfo_tbl_2 th {border:0px;}
   #dbinfo_tbl_2 td {border:0px; padding: 10px;} 	
   #dbinfo_tb_3{border:0px;border-spacing:0px;}
   #dbinfo_tbl_3 tr {border:0px;}
   #dbinfo_tbl_3 th {border:0px;}
   #dbinfo_tbl_3 td {border:0px; padding: 10px;} 
   #dbinfo_tb_4{border:0px;border-spacing:0px;}
   #dbinfo_tbl_4 tr {border:0px;}
   #dbinfo_tbl_4 th {border:0px;}
   #dbinfo_tbl_4 td {border:0px; padding: 10px;} 
   .user_acl{}	
</style>
</head>
<body>
<%
  com.yahoo.dba.perf.myperf.common.AppUser u = com.yahoo.dba.perf.myperf.common.AppUser.class.cast(
  session.getAttribute(com.yahoo.dba.perf.myperf.common.AppUser.SESSION_ATTRIBUTE));
%>
<script language="javascript">
var DBList=[""
    <c:forEach var="cluster" items="${mydbs}">,"${cluster}"</c:forEach>
  ];
var RESTRICTUSERLIST = [""
    <c:forEach var="u" items="${restrictedUsers}">,"${u}"</c:forEach>
  ];     
</script>
<div style="margin-left:48px;">
  <span id="common_msg"></span>
  <div id="mainTabs" class="clearTabView">
    <ul>
<% if(!u.isRestrictedUser()){ %>    
      <li><a href="#dbinfo_add_update" title="Add or update MySQL servers">Add/Update</a></li>
      <li><a href="#dbinfo_remove" title="Remove a group or a servers">Remove/Rename</a></li>
<% } %>      
      <li><a href="#dbinfo_cred" title="Provide MySQL user and credential for information retrieval">Credentials</a></li>
<% if(u.isAdminUser()){ %>
      <li><li><a href="#dbinfo_acl" title="Assign DB Groups to Restricted User">Access Control</a></li>
<% } %>
    </ul>
<% if(!u.isRestrictedUser()){ %>    
    <div id="dbinfo_add_update">
	  <p style="width:600px;">Note: You can use one occurrence of the pattern "[nnn-mmm]" inside host name to add multiple servers to the same group, 
	     where nnn and mmm are numbers only. When you add servers to an existing group, no need to provide MySQL user name and password, Connection test
	     is only done on first server in the list. A single MySQL account should be used to access all servers in the same group for information retrieval.</p>
      <input type="hidden" id="dbtype" name="dbtype" value="mysql" />
      <table border="0" id="dbinfo_tbl">
        <tr>
          <td><label for="dbAction">Action</label></td>
          <td>
       		<select name="dbAction" id="dbAction" onchange="showRows(this.value);" title="Add/remove/update databases or groups" >
       		  <option value="2">Add a DB Server</option>
       		  <option value="3">Update a DB Server</option>
       		</select>
          </td>
        </tr>
        <tr id="tr_clusterName">
          <td><label for="dbGroupName">Group Name</label></td>
          <td><input type="text" id="dbGroupName" name="dbGroupName"  autofocus  required title="A unique name to identify your standalone database server or a group of servers"/> &nbsp;
          <input type="button" value="Find" onclick="prepareDBSearch('dbGroupName','hostName', mydomval('dbGroupName'), null, 'port');" title="Click to find database servers by keyword"/></td>
        </tr>
        <tr id="tr_hostName">
          <td><label for="hostName">Host Name</label></td>
          <td><input type="text" name="hostName" id="hostName"  required title="The full qualified host name of a database host"/></td>
        </tr>
        <tr id="tr_port">
          <td><label for="port">Port Number</alabel</td>
          <td><input type="text" name="port" id="port" value="3306" title="The port number the server instance listens to"/></td>
        </tr>     
        <tr id="tr_sid">
          <td><label for="databaseName">Database Name</label></td>
          <td><input type="text" id="databaseName" name="databaseName"  value="information_schema" title="Default MySQL database/schema name"/></td>
        </tr>     
        <tr id="tr_useTunneling">
          <td><label for="useTunneling">SSH Tunneling?</label></td>
          <td><input type="checkbox" id="useTunneling" name="useTunneling" title="Use SSH tunneling if you cannot access the database host directly, for example, from your desktop."/></td>
        </tr>     
        <tr id="tr_localHostName">
          <td><label for="localHostName">Local Host Name</label></td>
          <td><input type="text" name="localHostName" id="localHostName"  title="Tunneling host when using SSH tunneling if you cannot access the database host directly, for example, from your desktop." /></td>
        </tr>     
        <tr id="tr_localPort">
          <td><label for="localPort">Local Port</label></td>
          <td><input type="text" name="localPort" id="localPort"  title="Tunneling port when using SSH tunneling if you cannot access the database host directly, for example, from your desktop."/></td>
        </tr>
        <tr id="tr_storeCredential">
          <td><label for="storeCredential">Add DB Credentail?</label></td>
          <td><input type="checkbox" id="storeCredential" name="storeCredential" value="1" title="Indicate whether you want to save this DB user name and password for future use"/></td>
        </tr>
        <tr id="tr_username">
          <td><label for="username">DB User Name</label></td>
          <td><input type="text" name="username" id="username"  title="Database user name"/></td>
        </tr>
        <tr id="tr_password">
          <td><label for="password">DB Password</label></td>
          <td><input type="password" name="password" id="password" title="Database password" /></td>
        </tr>
        <tr id="tr_password2">
          <td><label for="password">Retype DB Password</label></td>
          <td><input type="password" name="password2" id="password2" onblur="macthPwd('password','password2','pwd_chk');" title="Database password"/>
           <span id="pwd_chk" style="color:red;"></span>
          </td>
        </tr>
        <tr id="tr_testConnection">
          <td><label for="testConnection">Test Connection?</label></td>
          <td><input type="checkbox" id="testConnection" name="testConnection" value="1" title="If checked, connection to the DB will be tested."/></td>
        </tr>
        <tr>
          <td>&nbsp;</td><td><input type="button" align="center" value="Submit" id="btn_add_update" /></td>
        </tr>
      </table>
    </div><!-- end of add/update db -->
    <div id="dbinfo_remove">
<% if(!u.isAdminUser()){ %>
	  <p style="width:600px;">Note: You can only remove the groups or servers you added.</p>
<% } %>	  
      <table border="0" id="dbinfo_tbl_2">
        <tr>
         <td><label for="dbAction_2">Action</label></td>
         <td>
       		<select name="dbAction_2" id="dbAction_2" onchange="showRows(this.value);" title="remove a server or groups" >
       		  <option value="4">Remove a DB Server</option>
       		  <option value="5">Remove a DB Group</option>
       		  <option value="6">Rename a DB Group</option>
       		</select>
         </td>
       </tr>
       <tr id="tr_clusterName_2">
         <td><label for="dbGroupName_2">Group Name</label></td>
         <td><input type="text" id="dbGroupName_2" name="dbGroupName_2"  autofocus  required title="A unique name to identify your standalone database server or a group of servers"/> &nbsp;
           <input type="button" value="Find" onclick="prepareDBSearch('dbGroupName_2','hostName_2', mydomval('dbGroupName_2'));" title="Click to find database servers by keyword"/>
         </td>
       </tr>
       <tr id="tr_newClusterName_2" style="display:none;">
         <td><label for="dbNewGroupName_2">New Group Name</label></td>
         <td><input type="text" id="dbNewGroupName_2" name="dbNewGroupName_2"  required title="A unique name to identify your standalone database server or a group of servers"/>
         </td>
       </tr>
       <tr id="tr_hostName_2">
         <td><label for="hostName_2">Host Name</label></td>
         <td><input type="text" name="hostName_2" id="hostName_2"  required title="The full qualified host name of a database host"/></td>
     </tr>
     <tr>
       <td>&nbsp;</td><td><input type="button" align="center" value="Submit" id="btn_remove" /></td>
     </tr>
   </table>
  </div><!-- end of dbinfo_remove -->
<% } %>
  <div id="dbinfo_cred">
    <p style="width:600px;">Note: To access database information and gathering metrics, you need provide a MySQL account and password with
      the following privileges:
      <ul>
        <li>process</li>
        <li>replication client</li>
        <li>show databases</li>
        <li>show view</li>
        <li>select (to access meta data and explain plan)</li>
      </ul> 
    </p>
    <table border="0" id="dbinfo_tbl_3">
     <tr>
       <td>Database Group Name</td>
       <td><input id="dbGroupNameCred" name="dbGroupNameCred" />
           &nbsp;<input type="button" value="Find" onclick="prepareDBSearch('dbGroupNameCred');" />
       </td>
     </tr>     
     <tr>
       <td>Database User Name</td>
       <td><input type="text" id="username_cred" name="username_cred" required/></td>
     </tr>     
     <tr>
       <td>DB Password</td>
       <td><input type="password" id="pw_cred" name="pw_cred"  required/></td>
     </tr>
     <tr>
       <td>Retype Password</td>
       <td><input type="password" id="pw2_cred" name="pw2_cred" onblur="macthPwd('pw_cred', 'pw2_cred', 'pwd_chk_cred');"  required/><span id="pwd_chk_cred" style="color:red;"></span></td>
     </tr>
     <tr>
       <td>Test Password?</td>
       <td><input type="checkbox" name="test_cred" id="test_cred" checked="true" value="y"/></td>
     </tr>
     <tr>
       <td></td><td><input type="button" id="btn_cred" align="center" value="Submit" /></td>
     </tr>
   </table>    
  </div><!-- cred div-->
<% if(u.isAdminUser()){ %>  
  <div id="dbinfo_acl">
    <p>Please use this screen to control database group visibility to restricted users. If you don't see the DB Group list, please refresh this page.</p>
    <table border="0" id="dbinfo_tbl_4">
      <tr>
        <td>Restricted User</td>
        <td><input type="text" id="username_acl" name="username_acl" required/></td>
      </tr>
      <tr>
        <td>Visible ?</td>
        <td>
          <dl>
            <c:forEach var="dg" items="${mydbs}">
              <dt><input type="checkbox" id="acl_chk_${dg}" value="${dg}" class="user_acl" />&nbsp;${dg}</dt>
            </c:forEach>
          </dl>  
        </td>
      </tr>
    </table>
  </div><!-- acl div -->
<% } %>
</div><!-- main tab -->
</div><!-- top div -->

<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>

<script language="javascript">
$('#mainTabs').tabs();
$("#dbGroupNameCred").autocomplete({source: DBList, minLength:0}) 
  .bind('focus', function(){$(this).autocomplete("search");}); 
     
$('#dbGroupNameCred').blur(function()
{
  var db = mydomval('dbGroupNameCred');
  if(db == null || db == '')return;
  var mydata = "cmd=retrieve&dbGroupName=" + escape(db) + "&seed="+Math.random();
  $.ajax({
       url: "cred.html",
       data: mydata,
       type: 'GET',
       dataType: 'json',
       success: function(json)
       {
         if(json != null && json.resp != null && json.resp.message != null && json.resp.message != "")
           mydom('username_cred').value = json.resp.message;
       }
     }
  );  
});
<% if(u.isAdminUser()){ %>
$("#username_acl").autocomplete({source: RESTRICTUSERLIST, minLength:0}) 
  .bind('focus', function(){$(this).autocomplete("search");});
  
$('#username_acl').blur(function()
{
  var name = mydomval('username_acl');
  if(name == null || name == '')return;
  var mydata = "t=list_restricted&ct=json&name=" + escape(name) + "&seed="+Math.random();
  //clean old data
  for(var i = 0; i<DBList.length; i++)
  {
    if(mydom("acl_chk_"+DBList[i]))
      mydom("acl_chk_"+DBList[i]).checked = false;
  }
  $.ajax({
       url: "datalist.html",
       data: mydata,
       type: 'GET',
       dataType: 'json',
       success: function(json)
       {
          if(json==null||json.resp==null
               ||json.resp.results==null||json.resp.results.results==null||json.resp.results.results<1)
          {
               return;
          }
          var res = json.resp.results.results;
          var obj = new Object();
          var i = 0;
          //reset first
          for(i=0;i<res.length;i++)
          {
               var dg = res[i]["DBGROUP"];
               $("#acl_chk_"+dg).prop("checked", true);
          }//for
     }//success
  });  //ajax
});//binding 

$(".user_acl").on("change", function()
{
   var dg = $(this).attr("value");
   var checked = $(this).is(":checked");
   handleAclUpdate(dg, checked);
}
);
   
function handleAclUpdate(dg,  subscribe)
{     
  var user = mydomval("username_acl");
  var mydata = "dbAction=7&username="+escape(user) +"&dbGroupName=" + escape(dg);
  if(subscribe)
	   mydata += "&visible=1";
  else mydata += "&visible=0";
  mydata += "&seed="+Math.random();
  $.ajax({
       url: "db.html",
       data: mydata,
       dataType: 'json',
       success: function(json)
       {
         if(json!=null && json.resp!=null)
         {
           if(json.resp.status==0)
             alert("ACL for ("+ user + ", "+ dg + ") has been updated.");
           else
             alert("Failed to update ACL for ("+ user +", "+ db+"): " + json.resp.message);
         } 
       }
     });
 }
<% } %>

$('#btn_cred').click(function()
{
  var mydata = "cmd=update";
      mydata += "&dbGroupName=" + escape(mydomval('dbGroupNameCred'));
      mydata += "&username=" + escape(mydomval('username_cred'));
      mydata += "&pw=" + escape(mydomval('pw_cred'));
  if(mydom('test_cred').checked)
      mydata += "&test_cred=y&seed="+Math.random();
  $.ajax({
       url: "cred.html",
       data: mydata,
       type: 'POST',
       dataType: 'json',
       success: function(json)
       {
         if(json != null && json.resp != null && json.resp.message != null)
         	reportStatus(json.resp.status != 0, 'common_msg', json.resp.message);
       }
     }
  );        
});
<% if(!u.isRestrictedUser()){ %>
$('#btn_add_update').click(function()
{
  addOrUpdateDB(mydomval("dbAction"));
});

$('#btn_remove').click(function()
{
  addOrUpdateDB(mydomval("dbAction_2"));
});
<% } %>
function showRows(val)
{
 	if(val=="4"||val=="5"||val=="6")
 	{
 		if(val=="4")
 			showRow("tr_hostName_2",1);
 		else  
 			showRow("tr_hostName_2",0); 
 		if(val == "6")
 		    showRow("tr_newClusterName_2",1);
 		else
 		    showRow("tr_newClusterName_2",0);    	
 			
 	}else
 	{
 		showRow("tr_hostName",1);
 		showRow("tr_port",1);
 		showRow("tr_sid",1);
 		showRow("tr_useTunneling",1);
 		showRow("tr_localHostName",1);
 		showRow("tr_localPort",1);
 		showRow("tr_storeCredential",1);
 		showRow("tr_username",1);
 		showRow("tr_password",1);
 		showRow("tr_password2",1);
 		showRow("tr_testConnection",1);
 	}
}
 //show = 1: show
 //show = 0: hide
function showRow(tr,show)
{
 if(show==1)
 	mydom(tr).style.display="table-row";
 else 
    mydom(tr).style.display="none";
}

function addOrUpdateDB(dbAction)
{
	if(dbAction == 5)
 	{
 	   if(!confirm("Do you really want to remove a full group of database servers from this application?"))
 	      return;
 	 }
 	  var grpName =  (dbAction >= 4)?mydomval("dbGroupName_2"): mydomval("dbGroupName"); 
	  var hostName = (dbAction >= 4)?mydomval("hostName_2"): mydomval("hostName"); 
 	  var mydata = "dbAction=" + dbAction +"&dbtype=mysql";
		  mydata += "&dbGroupName=" + escape(grpName);
	  if(dbAction == 6)
	     mydata += "&dbNewGroupName=" + escape(mydomval("dbNewGroupName_2"));;
	  if(dbAction < 5)
	      mydata += "&hostName=" + hostName;

	  if(dbAction < 4)
	  {
	  	  mydata += "&port="+mydomval("port");
	  	  mydata += "&databaseName="+mydomval("databaseName");
	  	  if(mydom("useTunneling").checked)
	  	  {
	  	    mydata += "&useTunneling=1";
	  	    mydata += "&localHostName="+mydomval("localHostName");
	  	    mydata += "&localPort="+mydomval("localPort");
	  	  }
	  	  if(mydom("storeCredential").checked)
	  	    mydata += "&storeCredential="+mydomval("storeCredential");
	  	  mydata += "&username="+mydomval("username");
	  	  mydata += "&password="+mydomval("password");
	  	  if(mydom("testConnection").checked)
	  	  	mydata += "&testConnection=1";  	  	  
	  }    
	  $.ajax({
       url: "db.html",
       data: mydata,
       type: 'POST',
       dataType: 'json',
       success: function(json)
       {
         console.log(json);
         var msg = "";
         if(json != null && json.resp != null)
         	reportStatus(json.resp.status != 0, 'common_msg', json.resp.message);
         else
         	reportStatus(true, 'common_msg', "Failed to execute the specified operation.");
       }
     });
 }
</script>
</body>
</html>