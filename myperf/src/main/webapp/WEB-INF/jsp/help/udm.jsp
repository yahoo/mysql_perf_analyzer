<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>Database Performance Analyzer Help - User Defined Metrics</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>User Defined Metrics</h1>
  <p>This page is used to define simple metrics and assign database associations.</p>
  <p>To add a new UDM or modified an existing one, click link <b>UDM</b> to display available UMDs and the link to add new UDM. 
     The UDM <b>Name</b> should be short and only alpha numeric and underscore are allowed. A <b>SELECT SQ</b>L statement should be provided
     and the <b>Metrics</b> are a comma separated column list from the SQL statement. If the metrics values are accumulated, for example,
     like <b>bytes_sent</b> from global status, check the <b>Value Accumulated</b> check box. It is strongly recommended to pick a database
     to test the validity.</p>
  <p>To associate a UDM to a database or remove it, use <b>Databases</b> link. Click the concerned database, then check any available UDM to
    associate the UDM to the database, or uncheck to remove it. Once selected, the UDM will be added to the scheduled metrics scan and the results
    will be accessible from <a herf="perf.htm?pg=m">Metrics</a> page, link <a href="perf.htm?pg=sp">Scatter Plot And More</a>.</p>
</body>
</html>