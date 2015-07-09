<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>MySQL perf Analyzer Help - User Account</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>User Account</h1>
  <p>Use this page to register a new user, modify information like email or rest password, and remove users. 
  Only a power user can update other user's information. Note while email is used for offline communication,
  the email is handled by Linux shell command "mailx".
  </p>
  <p>In general, user access control is only limited on direct target database accesses, not meta data or metrics.
  If it is desired to limit visibility of database groups by a specific user, the user should be created as "restricted 
  User", and DB info page can be used to manage such type of access control.
  </p>  
</body>
</html>