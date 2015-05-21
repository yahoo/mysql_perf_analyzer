<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
  <title>MySQL Perf Analyzer Help - Meta Data</title>
<jsp:include page="../commheader.jsp" flush="true" />
</head>
<body>
  <h1>Database Metq Data</h1>
  <p>This page is used to access meta data about databases, tables and views.</p>
  <p>To use it, first select a database server and click "Go" button" to display all databases/schemas. 
     If the desired database/schema is not displayed, make sure the database user has "SHOW DATABASES" privilege.
     If "SHOW DATABASES" privilege was just added, you might need logout the analyzer and relogin to get a new connection to the database. </p>
  <p>To view tables and views, you can click the concerned database/schema to populate the database name or fill it manually. 
     Then select "Tables" Or "Views" to display the tables or views. Because "show table status from schem_name" or access to information_schema.tables
     will cause MySQL to sample the tables with disk reads (controlled by INNODB_STATS_ON_METADATA, default to ON for MySQL5.1 and MySQL 5.5, and innodb_stats_sample_pages or innodb_stats_transient_sample_pages,
     default to 8), 
     here we only use "show tables from schema_name" to display all tables belonging to the same schema.
     </p>
  <p>Once tables or views are retrieved, use context menu (right click) to view table details or view definition.</p>
</body>
</html>