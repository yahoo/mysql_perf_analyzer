<%@page trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>User Sign In Page</title>
<script src="js/md5.js"></script>
<script src="js/common.js"></script>
</head>
<body>
<center>
  <div style="margin-top:40px;margin-bottom:20px;">
  <form method="post" action="signin.htm">
   <fieldset style="width:640px;padding:30px;border: 1px solid #c0c0c0;">
   <table border="0" cellpadding="20">
     <tr>
       <td style="border-right:1px solid black;width:280px;">
         <table border="0">
           <tr>
             <td>User Name:&nbsp;</td>
             <td><input type="text" id="name" name="name" value="${name}" style="width:160px;" autofocus /></td>
           </tr>
           <tr><td colspan="2">&nbsp;</td></tr>     
           <tr>
             <td>Password:&nbsp;</td>
             <td><input type="password"  id="pd" name="pd" style="width:160px;"/>
                 <input type="hidden" id="s" name="s" ts="${server_ts}" ars="${ars}"/>
                 <input type="hidden" id="ts" name="ts" value="${server_ts}"/>
                 <input type="hidden" id="ars" name="ars"   value="${ars}"/>
             </td>
           </tr>
           <tr><td colspan="2">&nbsp;</td></tr>     
           <tr><td colspan="2" align="right"><input type="submit" value="Sign In" onclick="return auth();"/>
<c:if test="${setup == 1}" >
           &nbsp;&nbsp;<a href="user.htm?a=reset">Forget Password?</a>
</c:if>
           </td></tr>
         </table>
       </td>
       <td style="width:280px;">
         <table valign="middle">
<c:if test="${setup == 1}" >         
           <tr><td align="left">No Account Yet?</a></td></tr>
           <tr><td align="left"><a href="user.htm?a=new">Sign Up Here</a></td></tr>
           <tr><td align="left"><a href="perf.htm?pg=st">Status Dash Board</a></td></tr>
</c:if>
<c:if test="${setup != 1}" >
           <tr><td align="left">(Please login to complete setup first, check <a target="_doc" href="https://github.com/yahoo/mysql_perf_analyzer">the simple doc</a> for detail.)</td></tr>
</c:if>
         </table>
       </td>
     </tr>      
   </table>
  </form>
 <c:if test="${message!=null}" > 
  <div style="color:red">${message}</div>
 </c:if> 
 </div>
 </center>
</body>
</html>