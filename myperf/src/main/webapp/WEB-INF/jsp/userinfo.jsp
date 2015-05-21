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
<script language=javascript> 
function setupAction()
{
	var v = document.getElementById("a").value;
	if(v=="selfpwd"||v=="selfemail")
	{
		document.getElementById("name").value="${userInfo.name}";
		document.getElementById("name").style.display="inline";
		if(document.getElementById("userlist")!=null)
		{
			document.getElementById("userlist").style.display="none";
		}
		if(v=="selfpwd")
		{
			showHide(["tr_email","tr_password","tr_userprivilege"], "tr_password", "table-row");
			document.getElementById("tr_password2").style.display="table-row";
		}else
			showHide(["tr_email","tr_password","tr_password2","tr_userprivilege"], "tr_email", "table-row");
	}else if(v=="new")
	{
		if(document.getElementById("userlist")!=null)
		{
			document.getElementById("userlist").style.display="none";
		}
		showRow("tr_name",1);showRow("tr_email",1);showRow("tr_password",1);showRow("tr_password2",1);showRow("tr_userprivilege",1);
	}else if(v=="otherpwd"||v=="otheremail"||v=="otherpriv")
	{
		document.getElementById("name").style.display="none";
		document.getElementById("userlist").style.display="inline";
		if(v=="otherpwd")
		{
			showHide(["tr_email","tr_password","tr_userprivilege"], "tr_password", "table-row");
			document.getElementById("tr_password2").style.display="table-row";
		}
		else if(v=="otheremail")
			showHide(["tr_email","tr_password","tr_password2","tr_userprivilege"], "tr_email", "table-row");
		else if(v=="otherpriv")
			showHide(["tr_email","tr_password","tr_password2","tr_userprivilege"], "tr_userprivilege", "table-row");
	}
}	
 function showRow(tr,show)
 	{
 		if(document.getElementById(tr)==null)return;
 		if(show==1)
 			document.getElementById(tr).style.display="table-row";
 	    else 
 	    	document.getElementById(tr).style.display="none";
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
</script>
 <style>
   #usertbl{border:0px;border-spacing:0px;}
   #usertbl tr {border:0px;}
   #usertbl th {border:0px;}
   #usertbl td {border:0px; padding: 10px;} 	
 </style>
</head>
<body>
<div style="margin-left:48px;">
<c:if test="${a==2}">
  <span>${message}</span><br /> 
</c:if>
<c:if test="${a!=2}">
<c:if test="${message!=null&&status==0&&a!=1}">
  <span style="color:blue;font-size:10px;">${message}</span>
</c:if>
<c:if test="${message!=null&&status==-1}">
  <span class="cssError">${message}</span>
</c:if>
  <form method="post" onsubmit="return verify();">
   <fieldset style="width:600px;">
 <c:if test="${userInfo.adminUser}">    
   <legend>User Management:</legend>
 </c:if>  
 <c:if test="${!userInfo.adminUser&&a==0}">
   <legend>Update User Info:</legend>
 </c:if>
 <c:if test="${!userInfo.adminUser&&a==1}">
   <legend>New User Sign Up</legend>
 </c:if>
 <c:if test="${a==1}">
   <input type="hidden" name="a" id="a" value="new" />
 </c:if>  
   <table border="0" id="usertbl">
 <c:if test="${userInfo.adminUser}">
     <tr>
       <td><label for="a">Action</label></td>
       <td><select name="a" id="a" onchange="setupAction();">
             <option value="">---</option>
             <option value="selfpwd">Change password for myself</option>
             <option value="selfemail">Change email for myself</option>
             <option value="new">Add New User</option>
             <option value="confirm">Confirm new user info</option>
             <option value="otherpwd">Change user's password</option>
             <option value="otheremail">Change user's email</option>
             <option value="otherpriv">Change user's privilege</option>
           </select>
        </td>   
    </tr>
 </c:if>
 <c:if test="${!userInfo.adminUser&&a==0}">
     <tr>
       <td><label for="a">Action</label></td>
       <td><select name="a" id="a"   onchange="setupAction();">
             <option value="">---</option>
             <option value="selfpwd">Change Password For Myself</option>
             <option value="selfemail">Change Email For Myself</option>
           </select>
        </td>   
    </tr>
 </c:if>       
     <tr id="tr_name">
       <td><label for="name"  title="A unique identifier for the user to use this tool and store user specific data such as reports and filters.">User Name</label></td>
 <c:if test="${userInfo.adminUser||a>0}"> 
       <td><input type="text" id="name" name="name" value="${userInfo.name}"/>
 <c:if test="${userInfo.adminUser}">
           <select id="userlist" name="userlist">
               <option value="">select user from list</option>
<c:forEach var="u" items="${users}">
              <option value="${u.name}">${u.name}</option>
</c:forEach>               
           </select>
</c:if>
       </td>
 </c:if>
 <c:if test="${!userInfo.adminUser&&a==0}">      
       <td><input type="text" id="name" name="name" value="${userInfo.name}" readonly="true"/></td>
 </c:if>      
     </tr>     
     <tr  id="tr_email">
       <td><label for="email"  title="Optional. It will be used for password reset and other administrative tasks.">Email</label></td>
       <td><input type="text" name="email" id="email" maxlength="100" value="${userInfo.email}"/></td>
     </tr>     
     <tr  id="tr_password">
       <td><label for="password"  title="Password to access this tool">Password</label></td>
       <td><input type="password" name="password" id="password" /></td>
     </tr>     
     <tr  id="tr_password2">
       <td><label id="password">Retype Password</label></td>
       <td><input type="password" name="password2" id="password2" onblur="macthPwd();"/><span id="pwd_chk" style="color:red;"></span></td>
     </tr>
 <c:if test="${userInfo.adminUser}">      
     <tr id="tr_userprivilege">
       <td><label for="userprivilege"  title="User type of this account. Power User can manage other user accounts.">User Type</label></td>
       <td><select id="userprivilege" name="userprivilege">
             <option value="0" <c:if test="${userInfo.userprivilege==0}">selected="true"</c:if> >Standard User</option>
             <option value="1" <c:if test="${userInfo.userprivilege==2}">selected="true"</c:if>>Power User</option>
           </select
       </td>
     </tr>
     <tr id="tr_confirmed" style="display:none">
       <td><label for="userConfirmed">Confirmed?</label></td>
       <td><input id="userConfirmed" name="userConfirmed" type="checkbox"/></td>
     </tr>
 </c:if>    
 <c:if test="${!userInfo.adminUser}">
   <input type="hidden" name="userprivilege" id="userprivilege" value="${userInfo.userprivilege}"/>
 </c:if>
     <tr>
     <td></td><td><input type="submit" align="center" value="Submit" /></td>
     </tr>
   </table>
   </fieldset>
  </form>
</c:if>  
</div>  
<c:if test="${userInfo.adminUser}">
<script language="javascript">
$('#userlist').change(function()
{
  var mydata = "a=show&seed="+Math.random()+"&name="+escape(mydomval("userlist"));
  mydom("name").value = mydomval("userlist");
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
        mydom("tr_confirmed").style.display="table-row";
      } 
    }//success    
 });//ajax  
});
</script>
</c:if>
</body>
</html>