MySQL Performance Analyzer
======

MySQL Performance Analyzer is an open source project for MySQL performance monitoring and analysis. 
This repository includes two sub projects: 
* Java web application project myperf
* Java web server jetty wrapper

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

2. Unzip myperfserver-server.zip to the desired installation directory. If you intend to install on Windows host, please review two shell scripts and create Windows equivalent.

3. For a more formal installation, it is recommended to have a MySQL database server to store the metrics.
   Otherwise, use the built-in derby db.
   a. Create a database, for example, named as metrics, with the MySQL database server.
   b. Create a MySQL user (for example, 'metrics'@'my_host' -> here my_host is the machine where you MySQL perf analyzer) with all privileges on above schema.
   c. The above information will be required when you first login to the analyzer to setup metrics gathering.

4. Review script start_myperf.sh to see if you need to modify any command line settings. Usually, port number is the only one you need change
   -j: jettyHome, leave it as it is
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

7. First time Login and Setup
  After startup, point your browser to http://your_host:9092/myperf (or the port number you changed).
  The initial login user and credential are myperf/change.
  
After login, you will be directed to setup page:
    
You can add an email address for notifications. The email uses OS "mailx" command. 
    
Configure the metrics storage database, using the one that you created in the earlier steps.
A metrics scan interval of 1 or 5 minutes should be good enough.    
    
If use built-in derbydb, choose short retention days.
    

After configuration is done, you need to start the scanner ("Start Scanner" button on top of the page).

Everytime you change the configuration, you need to restart the scanner.

If the scanner does not work as expected, restart the analyzer 
./stop_myperf.sh
then 
./start_myperf.sh

8. For each database server you want to monitor, you need to create a MySQL user with the following privileges:
    a. process
    b. replication client
    c. show databases
    d. show view
    e. select on all (if you want to use it to check data dictionary or run explain plans)

9. The analyzer relies on Linux SNMP to gather OS level data. Check snmpd service status.

Known Limitations
------
1. snmpd is based on the Linux specification.
2. Email notification uses Linux's "mailx" command.

License
------
This code licensed under the Apache license. See the LICENSE file for terms.
