MySQL Performance Analyzer
======

MySQL Performance Analyzer is an open source project for MySQL performance monitoring and analysis. 
This repository includes two sub projects: 
Java web application project myperf
Java web server jetty wrapper

Build
------
MySQL Performance Analyzer is a Java Maven project. 
JDK and Maven 3.0 or later are required to build it. 

Although JDK 8 is specified in pom.xml, this project does not use Java 8 specific features, so the user can modify pom.xml files of the two sub projects to use different JDK version. 
For example, if the build host only has JDK 7, modify the file myperf/pom.xml, change the lines
```
  	<source>1.8</source>
	<target>1.8</target>
```
to
```
	<source>1.7</source>
	<target>1.7</target>
```

The build will create a zip file named as myperfserver-server.zip under directory perfJettyServer/target. To build it, at top level, run
```
  mvn clean package
```

Installation and Usage Instructions
------
1. Requirement: Java JDK 8, or the one specified by in pom.xml if changed during build time.

2. Unzip myperfserver-server.zip to desired installation directory. If intend to install on Windows host, please review two shell scripts and create Windows equivalent.

3. For more formal installation, it is recommended to have a MySQL database server to store the metrics.
   Otherwise, use built in derby db.
   a. Create a database, for example, named as metrics, with the MySQL database server.
   b. Create a MySQL user (for example, 'metrics'@'my_host', here my_host is the machine to install this MySQL perf analyzer) with all privileges on above schema.
   c. Above information will be required when first login to the analyzer to setup metrics gathering.

4. Review script start_myperf.sh to see if you need modify any command line settings. Usually, port number is the only one you need change
   -j: jetteyHome, leave as it is
   -p: http port to be used, 9092 by default
   -w: war (web archive) file, has to be myperf.war
   -k: working directory, if not specified, it will use ./work
   -c: url context, default to /myperf, leave it as is. 
   
   Modify java command path inside start_myperf.sh, if needed.

5. Start up:
   ./start_myperf.sh
   Check nohup.out and logs directory for any error logs.

6. Shutdown:
  ./stop_myperf.sh

7. Fisrt login and setup
  After startup, point your bwoser to http://your_host:9002/myperf (or the port number you changed).
  The initial login user and credential are myperf/change.
  After login, you will be directed to setup page:
    You can add notification email. The email uses OS "mailx" command. 
    Configure the metrics storage database, using the one created earlier.
    1 minute or 5 minutes metrics scan interval is good enough.
    If use builtin derbydb, choose short retention days.
    After configuration is done, you need start the scanner (the "Start Scanner" button on the page top).
    Everytime you change the configuration, you need restart the scanner.
    If the scanner does not work as expected, restart the analyzer (./stop_myperf.sh, then ./start_myperf.sh)

8. For each database server to monitor, you need create a MySQL user with the following privileges
    a. process
    b. replication client
    c. show databases
    d. show view
    e. select on all (if you want to use it to check data dictionary or run explain plan)

9. The analyzer relies on Linux SNMP to gather OS level data. Check snmpd service status.

Known Limitations
------
1. snmpd is based on Linux specification.
2. Email notification uses Linux "mailx" command.

License
------
Code licensed under the Apache license. See LICENSE file for terms.
