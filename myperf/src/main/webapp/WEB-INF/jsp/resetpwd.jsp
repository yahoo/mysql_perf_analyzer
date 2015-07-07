<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Reset Password</title>
 <style>
 	.cssError {color:red;font-size:10px;}
 </style>
</head>
<body>
<div style="margin-left:48px;">
<c:if test="${status==0}">
  <span>${message} Please go to <a href="${signinview}">Sign In</a> page to sign in.</span><br /> 
</c:if>
<c:if test="${message!=null&&status==-1}">
  <span class="cssError">${message}</span>
</c:if>
<c:if test="${status!=0}">
  <form method="post">
   <fieldset style="width:600px;">
   <legend>Reset Password:</legend>
   <table border="0">
     <tr id="tr_name">
       <td><label for="name"  title="A unique identifier for the user to use this tool and store user specific data such as reports and filters.">User Name</label></td>
       <td><input type="text" id="name" name="name" />
       </td>
     </tr>  
     <tr  id="tr_email">
       <td><label for="email" title="Optional. It will be used for password reset and other administrative tasks.">Email</label></td>
       <td><input type="text" name="email" id="email" maxlength="100" /></td>
     </tr>
     <tr>
       <td colspan="2">Note: If the server has problem to send out email, please contact administrator to change your password.</td>
     </tr>
     <tr>
     <td></td><td><input type="submit" align="center" value="Submit" /></td>
     </tr>
   </table>
   </fieldset>
  </form>
</c:if>  
</div>  
</body>
</html>