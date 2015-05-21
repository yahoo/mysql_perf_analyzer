<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>MySQL Perf Analyzer Help - Metrics</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>Query Profile Page</h1>
  <p>This page can be used for query explain plan and profiling. MySQL does not store query performance metrics. It does not store query execution plan and query text.</p>
  <p>This page requires a MySQL user which can access the schemas (databases). The MySQL user for performance analyzer only requires to access INFORMATION_SCHEMA, PERFORMANCE_SCHEMA and INNODB status.</p>
  <p>EXPLAIN is the main tool for query tuning. From the output, the user should have pretty good idea about the issues, for example, lacking of appropriate indexes, temp table usages, etc.</p>
  <p>Once Session Status and/or Profile box is checked, the analyzer will actually run the query to gather statistics. For MySQL 5.6, profiling can be done by enabling stage level instruments.</p> 
</body>
</html>