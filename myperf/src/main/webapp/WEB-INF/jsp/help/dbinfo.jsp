<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>MySQL PerfPerformance Analyzer Help - Database Info</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>Database Information Page</h1>
  <p>This page is used to add and update a database access information so that a user can use the Analyzer to track and analyze the database performance. 
     Before start to add a new database, it is recommended to first use <strong>Find</strong> button to check if the concerned database 
     has been added to the Analyzer.</p>
  <p>How to use <strong>Add/Update and Remove/Rename</strong> tab?
    <ol>
    <li><strong>Action</strong>:
      <ol type="a">
        <li>Add a DB Server: add one MySQL database.</li>
        <li>Update a DB Server: update information about a server already added.</li>
        <li>Remove a DB Server: remove a DB server.</li>
        <li>Remove a DB Group: remove a group of database servers (a cluster) from the Analyzer.</li>            
        <li>Rename a DB Group: rename a db group.</li>            
      </ol>
    </li>
    <li><strong>Group Name</strong>: a unique name to identify a group (cluster) of database servers. Some conventions can be used here, for example, the database business name. For dev, staging or int
      database, prefix like dev_, stg_ and int_ can be used, too.</li>
    <li><strong>Host Name</strong>: the full database host name.</li>
    <li><strong>Port Number</strong>: the port number of MySQL TCP listener, usually it is 3306.</li>
    <li><strong>SSH Tunneling</strong>: if the database cannot be accessed directly and SSH tunneling has to be used, for example, using PUTTY or openSSH, this box should be checked. 
        It is usually used when the Analyzer runs from a desktop/laptop. When checked, please also provide <strong>Local Host Name</strong> (usually localhost) and <strong>Local Port</strong>.</li>
    <li><strong>Add DB Credential</strong>: provide database user account and password to access the database. The password will be stored in encrypted format.
      <ol type="a">
        <li>The account needs at least PROCESS privilege to access INFORMATION_SCHEMA and INNODB information.</li>
        <li>To check replication status, the account needs REPLCATION_CLIENT privileges.</li> 
        <li>To access PERFORMANCE_SCHEMA, at least SELECT on PERFORMANCE_SCHEMA is required.</li>
        <li>To access meta data such as database and table information, "SHOW DATABASES" privilege is required.</li>
        <li>To access table and view information of a given database, "SELECT" privilege on the given database is required.</li>
        <li>To access view definition, "SHOW VIEW" privilege on the given database is required.</li>
      </ol>
    </li>
    <li><strong>Test Connection?</strong>: if checked, upon submit, the Analyzer will first test if the database account can be used to access the target database.</li>
    <li style="color:read;"><strong>Notes</strong>: For the first time to  add a server into a group, make sure to check both "Add DB Credential" and "Test Connection" boxes.
      If a server is added by other user, you can use Credential tab to add database account for your own usages.
    </li>
   </ol>
  </p>
  <p>How to use <strong>Credentials</strong> tab?
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
  </p>
  <p>How to use <strong>Access Control</strong> tab?</p>
  <p>
    This tab is created for organizations which want to limit database visibilities to individual users. Auto-complete is used to retrieve restricted user name list. 
    Once a user is selected, the analyzer will retrieve existing ACLs. Check desired database group to assign the access, or uncheck to revoke the access. 
    Credential to access the assigned database server will be copied from the assigner at the same time.   
  </p>
</body>
</html>