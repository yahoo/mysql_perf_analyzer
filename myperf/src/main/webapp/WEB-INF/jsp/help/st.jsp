<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>Dashboard - MySQL Perf Analyzer</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>Performance Dashboard</h1>
  <p>This page displays most recent snapshot of major performance metrics and 
     past 24 hours of alerts (abnormal detections) for all database server groups 
     or a selected group. The results are displayed in colored text, if there are
     any performance issues requiring user attention. For login user, individual
     alerts will present download-able forensic data gathered at the time the 
     abnormality was detected.   
  </p>
</body>
</html>