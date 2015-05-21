<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>MySQL perf Analyzer Help - Settings</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>Settings For Metrics Collection Scanner, Metrics Storage and Alerts</h1>
  <p>A power user can use this page to configure how metrics are collected. At minimum, a user name  (of the analyzer) has to be provided. 
  The analyzer will use the credentials (MySQL server account information for concerned MySQL server) from the selected user to query 
  MySQL server for global status and other user defined metrics. If the purpose is to watch a large number of MySQL servers, it is recommended 
  to pick a MySQL database as metrics storage.
  </p>
  <p>
    Scanning on dead hosts/MySQL servers can cause unnecessary delays because of network timeout or UDP wait time. 
    To avoid such delays, you can either use this page to disable metrics and SMNMP gatherings, or remove the host
    from management.
  </p>   
</body>
</html>