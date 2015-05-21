<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>MySQL Perf Analyzer Help - Database Credential</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>Database Credentials</h1>
  <p>The current Performance Analyzer implementation does not provide common database account and credentials to access a target database. 
     A user has to provide database accounts and passwords to access the target databases. The account needs at least the following privileges:
     <ol>
       <li>PROCESS privilege to access INFORMATION_SCHEMA and INNODB information. </li>
       <li>To check replication status, the account needs REPLCATION_CLIENT privileges.</li> 
       <li>To access PERFORMANCE_SCHEMA, at least SELECT on PERFORMANCE_SCHEMA is required.</li>
       <li>To access meta data such as database and table information, "SHOW DATABASES" privilege is required.</a>
       <li>To access table and view information of a given database, "SELECT" privilege on the given database is required.</li>
       <li>To access view definition, "SHOW VIEW" privilege on the given database is required.</li>
    </ol>
</p>
  <p>Fields:
    <ol>
      <li><strong>Database Group Name</strong>: the unique identifier for the target database. You can use <a href="dbinfo.htm">DB Info</a> page, <strong>Search DB</strong> tab to look up this information.</li>
      <li><strong>DB User Name</strong>: the database account, for example, dbsmnp.</li>
      <li><strong>DB Passowrd</strong>: the password of the database account.</li>
      <li><strong>Retype Passowrd</strong>: the password of the database account.</li>
      <li><strong>Test Passowrd</strong>: if checked, upon submit, the analyzer will connect to the target database to verify the password.</li>
    </ol>
  </p>   
</body>
</html>