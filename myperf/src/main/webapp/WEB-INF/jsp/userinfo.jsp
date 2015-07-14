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
<title>User Information Form</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<style>
   #usertbl{border:0px;border-spacing:0px;}
   #usertbl tr {border:0px;}
   #usertbl th {border:0px;}
   #usertbl td {border:0px; padding: 10px;}
  <c:if test="${a == 1 || a == 3}">
   .userinfo {display: table-row;}	
  </c:if>
  <c:if test="${a == 0}">  
   .userinfo {display: none;}
 </c:if>  
</style>
</head>
<body>
<div style="margin-left:48px;">
<c:if test="${status == 0}">
  <span class="cssInfo">${message}</span><br />
</c:if>
<c:if test="${status != 0}">
  <span class="cssError">${message}</span><br />
</c:if>

<c:if test="${a!=2}">
  <form method="post" onsubmit="return verify();">
   <fieldset style="width:600px;">
     <c:if test="${userInfo.adminUser}">    
       <legend>User Management:</legend>
     </c:if>  
     <c:if test="${!userInfo.adminUser && a == 0}">
       <legend>Update User Info:</legend>
     </c:if>
     <c:if test="${!userInfo.adminUser && a == 1}">
       <legend>New User Sign Up</legend>
       <input type="hidden" name="a" id="a" value="new" />
     </c:if>
     <c:if test="${!userInfo.adminUser && a == 3}">
       <legend>Reset Password</legend>
       <input type="hidden" name="a" id="a" value="reset" />
     </c:if>

     <table border="0" id="usertbl">
       <c:if test="${userInfo.adminUser}">
         <tr>
           <td><label for="a">Action</label></td>
           <td><select name="a" id="a" onchange="setupAction();">
               <option value="">---</option>
               <option value="selfpwd">Change password - Self</option>
               <option value="selfemail">Change email - Self</option>
               <option value="new">Add New User</option>
               <option value="delete">Remove User</option>
               <option value="confirm">Confirm new user info</option>
               <option value="otherpwd">Change user's password</option>
               <option value="otheremail">Change user's email</option>
               <option value="otherpriv">Change user's privilege</option>
             </select>
           </td>   
         </tr>
       </c:if>
       <c:if test="${!userInfo.adminUser && a==0}">
         <tr>
           <td><label for="a">Action</label></td>
           <td><select name="a" id="a"   onchange="setupAction();">
             <option value="">---</option>
             <option value="selfpwd">Change Password</option>
             <option value="selfemail">Change Email</option>
             </select>
           </td>   
         </tr>
       </c:if>       
       <tr id="tr_name" class="userinfo">
         <td><label for="name"  title="A unique identifier for the user to use this tool and store user specific data such as reports and filters.">User Name</label></td>
         <c:if test="${userInfo.adminUser || a>0}"> 
           <td><input type="text" id="name" name="name"/></td>
         </c:if>
         <c:if test="${!userInfo.adminUser&&a==0}">
           <td><input type="text" id="name" name="name" value="${userInfo.name}" readonly="true"/></td>
         </c:if>
       </tr>     
       <tr  id="tr_email" class="userinfo">
         <td><label for="email"  title="Optional. It will be used for password reset and other administrative tasks.">Email</label></td>
         <td>
           <c:if test="${userInfo.adminUser || a>0}">
             <input type="text" name="email" id="email" maxlength="100" />
           </c:if>
           <c:if test="${!userInfo.adminUser&&a==0}">
             <input type="text" name="email" id="email" maxlength="100" value="${userInfo.email}"/>
           </c:if>  
         </td>
       </tr>
       <c:if test="${a != 3}"> 
         <tr  id="tr_password" class="userinfo">
           <td><label for="password"  title="Password to access this tool">Password</label></td>
            <td><input type="password" name="password" id="password" /></td>
         </tr>     
         <tr  id="tr_password2" class="userinfo">
           <td><label id="password">Retype Password</label></td>
           <td><input type="password" name="password2" id="password2" onblur="macthPwd('password','password2','pwd_chk');"/><span id="pwd_chk" style="color:red;"></span></td>
         </tr>
         <c:if test="${userInfo.adminUser}">      
           <tr id="tr_userprivilege" class="userinfo">
             <td><label for="userprivilege"  title="User type of this account. Power User can manage other user accounts.">User Type</label></td>
             <td><select id="userprivilege" name="userprivilege">
                 <option value="0">Standard User</option>
                 <option value="1">Power User</option>
                 <option value="2">Restricted User</option>
               </select>
             </td>
           </tr>
           <tr id="tr_confirmed" style="display:none">
             <td><label for="userConfirmed">Confirmed?</label></td>
             <td><input id="userConfirmed" name="userConfirmed" type="checkbox"/></td>
           </tr>
         </c:if>
       </c:if>
       <c:if test="${!userInfo.adminUser && a != 3}">
         <input type="hidden" name="userprivilege" id="userprivilege" value="${userInfo.userprivilege}"/>
       </c:if>
       <tr>
         <td></td><td><input type="submit" align="center" value="Submit" /></td>
       </tr>
     </table>
<c:if test="${userInfo.adminUser}">
     <hr />
     <div id="userlist_div">
       <a href="#userdetail_div" id="view_users_link" title="Click to view/hide all user details.">View Existing Users</a>
       <div id="userdetail_div" style="display:none;">
         <table id="myperf_userlist_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
       </div>
     </div>
</c:if>     
   </fieldset>
  </form>
</c:if>
</div>  
<c:if test="${a != 2}">
<script language="javascript">
var myusername="${userInfo.name}";
var myemail="${userInfo.email}";
var myprivilege="${userInfo.userprivilege}";
<c:if test="${userInfo.adminUser}">
var ALLUSERLIST=[
    <c:forEach var="u" items="${allUsers}" varStatus="stat">${stat.index>0?",":""} {label:"${u}", idx:${stat.index}}</c:forEach>
  ];
var NEWUSERLIST=[
    <c:forEach var="u" items="${newUsers}" varStatus="stat">${stat.index>0?",":""} {label:"${u}", idx:${stat.index}}</c:forEach>
  ];
  
var myperf_userlistTable = new JSTable({
   	   name: "myperf_userlist",
   	   query:{
   	     queryURL: "user.html",
   	     paramFields:[{name:"a", value:"listalldetails"}]
   	   }, 
   	   handlers: {jquery:1}
   	});

$("#view_users_link").click(function()
{
  if(mydom("userdetail_div").style.display == "none")
  {
    myperf_userlistTable.sendQuery();
    mydom("userdetail_div").style.display = "block";
  }else
    mydom("userdetail_div").style.display = "none";
}
);  
</c:if>
function setupAction()
{
	var v = document.getElementById("a").value;
	if(v =="")
	  showHideRows([], ["tr_name", "tr_password", "tr_password2","tr_email","tr_userprivilege", "tr_confirmed"]);
	if(v=="selfpwd"||v=="selfemail")
	{
		mydom("name").value = myusername;		
		if(v=="selfpwd")
			showHideRows(["tr_name", "tr_password", "tr_password2"],["tr_email","tr_userprivilege", "tr_confirmed"]);
		else
		{
			mydom("email").value = myemail;
			showHideRows(["tr_name", "tr_email"],["tr_password", "tr_password2","tr_userprivilege", "tr_confirmed"]);			
		}
		return;
	}
	
	mydom("name").value = ""; 
	mydom("email").value = ""; 
	mydom("password").value = ""; 
	mydom("password2").value = ""; 
	mydom("userprivilege").selectedIndex = 0;

	if(v=="new")
	{
		showHideRows(["tr_name", "tr_email", "tr_password", "tr_password2","tr_userprivilege"], ["tr_confirmed"]);
		return;			
	}
	else if(v=="otherpwd"||v=="otheremail"||v=="otherpriv" || v =="confirm" || v == "delete")
	{
		if(v != "confirm")
			$("#name").autocomplete({source: ALLUSERLIST, minLength:0}) 
                    .bind('focus', function(){$(this).autocomplete("search");});  
		else
			$("#name").autocomplete({source: NEWUSERLIST, minLength:0}) 
                    .bind('focus', function(){$(this).autocomplete("search");});
                    	
		if(v=="otherpwd")
			showHideRows(["tr_name", "tr_password", "tr_password2"], ["tr_email","tr_confirmed","tr_userprivilege"]);
		else if(v=="otheremail")
			showHideRows(["tr_name", "tr_email"], ["tr_password", "tr_password2","tr_confirmed","tr_userprivilege"]);
		else if(v=="otherpriv")
			showHideRows(["tr_name", "tr_userprivilege"], ["tr_password", "tr_password2","tr_confirmed","tr_email"]);
		else if(v=="confirm")
			showHideRows(["tr_name","tr_email", "tr_confirmed"], ["tr_password", "tr_password2", "tr_userprivilege"]);
		else if(v=="delete")
			showHideRows(["tr_name","tr_email"], ["tr_password", "tr_password2", "tr_userprivilege", "tr_confirmed"]);
	}
}

function showHideRows(showEles, hideEles){
  if(showEles != null){
    for(var i=0; i<showEles.length; i++){
      if(mydom(showEles[i]))
        mydom(showEles[i]).style.display = "table-row";
    }
  }
  
  if(hideEles != null){
    for(var i=0; i<hideEles.length; i++){
      if(mydom(hideEles[i]))
        mydom(hideEles[i]).style.display = "none";
    }
  }
}
	
function verify()
{
    var a= document.getElementById("a").value;
	if(a=="")
	{
		alert("No action specified");
		return false;
	}else if(a=="selfpwd"||a=="new"||a=="otherpwd")
	{
		return macthPwd();
	}
	return true;
}

<c:if test="${userInfo.adminUser}">
$('#name').blur(function()
{
  var name = mydomval("name");
  if(name == "" || name == myusername || mydomval("a") == "new") return;
  var mydata = "a=show&seed="+Math.random()+"&name="+escape(mydomval("name"));  
  $.ajax({
    url: "user.html",
    data: mydata,
    dataType: 'json',
    success: function(jsonObj)
    {
      if(jsonObj==null)return;
      if(jsonObj.resp.results!=null)
      {
        var res = jsonObj.resp.results.results;
        mydom("email").value = res[0].email;
        mydom("userprivilege").selectedIndex = res[0].privilege;
        mydom("userConfirmed").checked = res[0].verified == "1";
        //mydom("tr_confirmed").style.display="table-row";
      } 
    }//success    
 });//ajax  
});

<c:if test="${tmpUser != null && status != 0}">
  mydom("name").value="${tmpUser.name}";
  mydom("email").value="${tmpUser.email}";
  if(mydom("userprivilege"))
  	mydom("userprivilege").selectedIndex=${tmpUser.userprivilege};
</c:if>
</c:if>
</script>
</c:if>
</body>
</html>