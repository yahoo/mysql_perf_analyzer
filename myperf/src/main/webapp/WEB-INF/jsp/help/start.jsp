<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>MySQL Performance Analyzer Help - Start</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>MySQL Performance Analyzer Help - Start</h1>
  <h2>Account</h2>
  <p>You need have an account to use MySQL Performance Analyzer, which currently uses its own simple user account management system.
    Information about the default power user account can be located inside README_MYPERF.TXT at the installation directory.   
    There are two ways to add a user account:
    <ol>
      <li><a href="user.htm?a=new">Self registration:</a>  Self registration will create a standard user. A power user can change the new user's privilege.</li>
      <li>An new user account can be added by a power user.</li>
    </ol>
  </p>
  <p>
    If this is the first time MySQL performance analyzer is used, the following steps are required:
    <ol>
      <li><a href="autoscanconfig.htm" target="_new">Configure Metric scanner</a>.</li>
      <li><a href="db.htm" target="_new">Onboard MySQL servers</a>.</li>
    </ol>
  </p> 
  <p>
    If this is the first use for any user, please note the followings.
    <ol>
      <li><a href="perf.htm?pg=m" target="_new">Metrics charts</a> can always be accessed if available.</li>
      <li>If the concerned MySQL server is not listed,  <a href="db.htm" target="_new">onboard MySQL servers</a> and notify the admin user to add it for metrics collections.</li>
      <li>For any concerned MySQL server, use <a href="cred.htm" target="_new">credential</a> page to add MySQL user account with right privileges so that the user can access other tools.</li>
    </ol>
  </p> 
  <p>Please send comments, suggestions and feature requests to <i>perf-dba@yahoo-inc.com</i></p>
</body>
</html>