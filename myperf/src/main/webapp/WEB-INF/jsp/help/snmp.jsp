<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>MySQL Perf Analyzer Help - SNMP Test</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>SNMP Test Page</h1>
  <p>MySQL does not provide a SQL path to access OS level performance metrics. 
     MySQL Perf Analyzer relies SNMP (and UDP) to gather such data, and currently
     only supports Linux server. You can use this page to test if SNMP data
     for any specific server is available. Use <a href="perf.htm?pg=settings">Settings</a>
     to disable or enable SNMP data gathering.
  </p>
</body>
</html>