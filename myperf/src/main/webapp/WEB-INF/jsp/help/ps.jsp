<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>Database Performance Analyzer Help - Performance Schema</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>Performance Schema Page</h1>
  <p>This page is used to display performance schema setup, realtime data and summary data.</p>
  <p>Performance schema was introduced in MySQL 5.5 to simulate Oracle wait events. Performance schema is enabled by default with MySQL 5.6. 
     But for MySQL 5.5, a line "performance_schema" has to be presented inside <b>my.cnf</b> to enable performance schema. The overhead with
     performance schema is within 10%. For MySQL 5.6, if all instruments are enabled, the overhead will be much higher.</p>
  <p>The feature of MySQL 5.5 performance schema is very limited. The basic function is to understand the waits such as kernel mutex, etc.</a>
  <p>MySQL 5.6 has enhanced performance schema significantly. Now it can be used to find out high cost queries, and it can also be used to 
    replace traditional profiling function. But it is still tricky to use it, for example, the tables might be required frequently truncation 
    to hold fresh information.</p>
</body>
</html>