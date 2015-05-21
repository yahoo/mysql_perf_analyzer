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
  <h1>Database Realtime Information Page</h1>
  <p>This page display some realtime information collected from INFORMATION_SCHEMA and various "SHOW ..." command.</p>
  <p>
    <li><strong>Tabs</strong>:
      <ol type="a">
        <li>Processes: the results from "SHOW PROCESSLIST". It can be used to check how busy the database is and to identify some long running queries and their status. It is  not very useful for OLTP where most of the queries complete within a second.</li>
        <li>Global Status: the results from global_status table. The refresh button can be used to calculate the differences since the first time the data is displayed, and this feature can be useful to capture important metrics for a period of time to understand how the resources are consumed. To start a new period, use the menu from drop down list to re-query/refresh the data.</li>
        <li>Global Variables: this can be a convenient place to check server configurations.</li>
        <li>Variable Diffs: this can be used to compare configurations of two MySQL servers.</li>
        <li>User Stats: this is only available on Percona server and requires variable userstat_running on. It is supposed to give some time based metrics and resource usages per user base. Note MySQL does not have other time based metrics available.</li>
        <li>Innodb Engine Status: decoded data from "show engine innodb status". </li>
        <li>Innodb Statistics: data from INFORMATION_SCHEMA related to innodb, for example, transactions, mutext, locks and buffer pool statistics. </li>
      </ol>
    </li>
  </p>   
</body>
</html>