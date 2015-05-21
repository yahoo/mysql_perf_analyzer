/*
 *  Copyright 2015, Yahoo Inc.
 *  Copyrights licensed under the Apache License.
 *  See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.tools.myperfserver;

import java.io.File;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContainerInitializer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;

public class App 
{
	public static String PID_FILE = "framework.pid";
	private static int pid = -1;
	
	private int port = 9090;
	private String contextPath = "/";
	private String logPath ="logs";
	private String warFile;
	private String workDir = "work";
	private String jettyHome; //it should have directory webapps which has our warFile
	
	private Server server; //jetty server
	private URI serverURI;
	private String shutdownFile = "myserver.shutdown";
	
	public static void main( String[] args ) throws Exception
    {
		//-p --port 9090
		//-r --webcontextroot  webapps
		//-l --logpath logpath
		//-w --war webapp war file name
		
		CommandLineParser parser = new GnuParser();
		Options options = new Options();
		options.addOption( "j", "jettyHome", true, "Jetty home, if not set, check system property jetty.home, then default to current Dir" );
		options.addOption( "p", "port", true, "http server port, default to 9090." );
		options.addOption( "c", "webcontextroot", true, "web app url root context, defaul to /" );
		options.addOption( "l", "logpath", true, "log path, default to current directory." );
		options.addOption( "w", "warfile", true, "war file name, default to myperf.war." );
		options.addOption( "k", "workdir", true, "work directory for jetty, default to current dir." );
		
		System.out.println(new Date()+" Usage: java -classpath ... com.yahoo.dba.tools.myperfserver.App -j jettyhome -p port -c webcontextroot -k workingDir -l logpath -w war_file");

		App myServer = new App();

		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    // validate that block-size has been set
		    if( line.hasOption( "p" ) ) {
		    	try
		    	{
		    		myServer.setPort(Short.parseShort(line.getOptionValue( "p" ) ));
		    	}catch(Exception ex){}
		    }
		    if( line.hasOption( "c" ) ) {
		    	String val = line.getOptionValue( "c" );
		    	if(val!=null && !val.isEmpty())
		    		myServer.setContextPath(val);
		    }
		    if( line.hasOption( "l" ) ) {
		    	String val = line.getOptionValue( "l" );
		    	if(val!=null && !val.isEmpty())
		    		myServer.setLogPath(val);
		    }
		    if( line.hasOption( "w" ) ) {
		    	String val = line.getOptionValue( "w" );
		    	if(val!=null && !val.isEmpty())
		    		myServer.setWarFile(val);
		    }
		    if( line.hasOption( "k" ) ) {
		    	String val = line.getOptionValue( "k" );
		    		myServer.setWorkDir(val);
		    }
		    if( line.hasOption( "j" ) ) {
		    	String val = line.getOptionValue( "j" );
		    	if(val!=null && !val.isEmpty())
		    		myServer.setJettyHome(val);
		    }
		}
		catch( ParseException exp ) {
		    System.out.println( "Unexpected exception:" + exp.getMessage() );
		}
		
		System.setProperty("logPath", myServer.getLogPath());

		PID_FILE = myServer.getWarFile().substring(0, myServer.getWarFile().indexOf('.'))+".pid";
		int historyPid = getHistoryPid();
		if(historyPid>=0)
		{
			System.out.println(new Date()+" *************************** WARNING *********************");
			System.out.println(PID_FILE+" exists. Possibly another instance is still running. PID = "+historyPid);
			System.out.println(new Date()+" *************************** WARNING *********************");
		}
		if(myServer.startServer())
		{
			pid = getPid();
			writePid();
			myServer.waitForInterrupt();
			removePid();
		}else
			System.out.println("Server not started.");
    }

	public App()
	{
		this.jettyHome = System.getProperty("jetty.home", "."); //set default jetty.home
	}
	public boolean startServer() throws Exception
	{
		removeShutdownFile();
		File work = new File(this.getWorkDir());
    	if(!work.exists())
    	{   System.out.println(new Date()+" Create working dir at "+work.getAbsolutePath()); 		
    		work.mkdir();
    	}
    	if(!work.exists())
    	{
        	System.out.println(new Date()+" Invalid working directory "+work.getAbsolutePath()); 
        	return false;
    	}
    	System.out.println(new Date()+" Use working directory "+work.getAbsolutePath());
    	
    	File log = new File(this.getLogPath());
    	if(!log.exists())
    	{   System.out.println(new Date()+" Create log dir at "+log.getAbsolutePath()); 		
    		log.mkdir();
    	}
    	if(!log.exists())
    	{
        	System.out.println(new Date()+" Invalid log directory "+log.getAbsolutePath()); 
        	return false;
    	}
    	System.out.println(new Date()+" Use log directory " + log.getAbsolutePath());
    	
    	System.out.println(new Date()+" Use jetty home " + this.getJettyHome());
    	String webappPath = this.getJettyHome() + File.separatorChar + "webapps"+ File.separatorChar + this.getWarFile();
    	if(!(new File(webappPath).exists()))
    	{
    		System.out.println(new Date()+" Cannot find war file at location "+webappPath);
    		return false;
    	}
    	System.out.println(new Date()+" Use war file " + webappPath);
    	System.out.println(new Date()+" Use port "+this.getPort()+", context " + this.getContextPath() + ".");
    	
    	WebAppContext webapp = new WebAppContext();
    	webapp.setContextPath(this.getContextPath());
    	webapp.setWar(webappPath);
    	webapp.setAttribute("javax.servlet.context.tempdir", work.getAbsolutePath());
    	webapp.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
    	          ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/.*taglibs.*\\.jar$");
    	webapp.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
    	webapp.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
    	webapp.addBean(new ServletContainerInitializersStarter(webapp), true);
    	//webapp.setClassLoader(new URLClassLoader(new URL[0],App.class.getClassLoader()));
    	webapp.addServlet(jspServletHolder(), "*.jsp");

    	//server = new Server(port);
    	server = new Server();
    	ServerConnector connector = connector();
        server.addConnector(connector);
    	
        server.setHandler(webapp);
    	server.start();
    	//server.join();

    	//dump server state
    	System.out.println(server.dump());
    	this.serverURI = getServerUri(connector);
    	return true;
	}
	private ServerConnector connector()
    {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        return connector;
    }
	private URI getServerUri(ServerConnector connector) throws URISyntaxException
    {
        String scheme = "http";
        for (ConnectionFactory connectFactory : connector.getConnectionFactories())
        {
            if (connectFactory.getProtocol().equals("SSL-http"))
            {
                scheme = "https";
            }
        }
        String host = connector.getHost();
        if (host == null)
        {
            host = "localhost";
        }
        int myport = connector.getLocalPort();
        serverURI = new URI(String.format("%s://%s:%d/", scheme, host, myport));
        System.out.println(new Date()+" Server URI: " + serverURI);
        return serverURI;
    }

    public void stop() throws Exception
    {
    	if(server != null)
    		server.stop();
    }
	/**
	 * Get process id of current running starloader
	 * @return OS process id
	 */
	public static int getPid()
	{
		try
		{
			RuntimeMXBean rt = java.lang.management.ManagementFactory.getRuntimeMXBean();
			String jvmName = rt.getName();
			String ss = jvmName.substring(0,jvmName.indexOf('@'));
			return Integer.parseInt(ss);
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return -1;//no valid number
	}
	
	private static void writePid()
	{
		if(pid<0)return;//not supported
		try
		{
			java.io.FileWriter fw = new java.io.FileWriter(PID_FILE);
			fw.write(String.valueOf(pid));
			fw.close();
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private static int getHistoryPid()
	{
		try
		{
			java.io.BufferedReader fr = new java.io.BufferedReader(new java.io.FileReader(PID_FILE));
			String line = fr.readLine();
			fr.close();
			return Integer.parseInt(line);
		}catch(Exception ex)
		{
			
		}
		return -1;
	}
	
	private static void removePid()
	{
		try
		{
			(new java.io.File(PID_FILE)).delete();
		}catch(Exception ex)
		{
			
		}
	}

	private  void removeShutdownFile()
	{
		try
		{
			(new java.io.File(this.shutdownFile)).delete();
		}catch(Exception ex)
		{
			
		}
	}

	private  List<ContainerInitializer> jspInitializers()
    {
        JettyJasperInitializer sci = new JettyJasperInitializer();
        ContainerInitializer initializer = new ContainerInitializer((ServletContainerInitializer) sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
        initializers.add(initializer);
        return initializers;
    }
	private   ServletHolder jspServletHolder()
    {
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.8");
        holderJsp.setInitParameter("compilerSourceVM", "1.8");
        holderJsp.setInitParameter("keepgenerated", "true");
        return holderJsp;
    }
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public String getWarFile() {
		return warFile;
	}

	public void setWarFile(String warFile) {
		this.warFile = warFile;
	}

	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	public String getJettyHome() {
		return jettyHome;
	}

	public void setJettyHome(String jettyHome) {
		this.jettyHome = jettyHome;
	}
	
	public void waitForInterrupt() throws InterruptedException
    {
		while(true)
		{
			//sleep 5 seconds, then check shutdown file
			try
			{
				Thread.sleep(5000);
				if(new File(this.shutdownFile).exists())
				{
					//we are suppose to shutdown
					System.out.println(new Date()+" Receive soft shutdown signal.");
					break;
				}
			}catch(Throwable th)
			{
				System.out.println(new Date()+" Receive shutdown signal.");
				break;
			}
		}
		try //shutdown 
		{
			System.out.println(new Date()+" Shutdown server");
			this.startServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
		removeShutdownFile();
		System.out.println(new Date()+" Wait for shutdown to complete");
        if(server!= null)server.join(); //wait for it to complete
		System.out.println(new Date()+" Shutdown is completed.");
    }
}
