/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.snmp;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import com.yahoo.dba.perf.myperf.common.SNMPSettings;

/**
 * A simple SNMP client to poll a single MySQL server for OS performance metrics, such as CPU, memory, disk usage, etc.
 * This SNMP client uses snmp4j open source library. 
 * @author xrao
 *
 */
public class SNMPClient 
{
	//common UC DAVIS oid
	public final static String memTotalSwap =      ".1.3.6.1.4.1.2021.4.3.0";
	public final static String memAvailSwap =      ".1.3.6.1.4.1.2021.4.4.0";
	public final static String memTotalReal =      ".1.3.6.1.4.1.2021.4.5.0";
	public final static String memAvailReal =      ".1.3.6.1.4.1.2021.4.6.0";
	public final static String memTotalSwapTXT =   ".1.3.6.1.4.1.2021.4.7.0";
	public final static String memTotalRealTXT =   ".1.3.6.1.4.1.2021.4.9.0";
	public final static String memTotalFree =      ".1.3.6.1.4.1.2021.4.11.0";
	public final static String memShared =         ".1.3.6.1.4.1.2021.4.13.0";
	public final static String memBuffer =         ".1.3.6.1.4.1.2021.4.14.0";
	public final static String memCached =         ".1.3.6.1.4.1.2021.4.15.0";
	public final static String memUsedSwapTXT =    ".1.3.6.1.4.1.2021.4.16.0";
	public final static String memUsedRealTXT =    ".1.3.6.1.4.1.2021.4.17.0";
	public final static String ssSwapIn =          ".1.3.6.1.4.1.2021.11.3.0";
	public final static String ssSwapOut =         ".1.3.6.1.4.1.2021.11.4.0";
	public final static String ssIOSent =          ".1.3.6.1.4.1.2021.11.5.0";
	public final static String ssIOReceive =       ".1.3.6.1.4.1.2021.11.6.0";
	public final static String ssSysInterrupts =   ".1.3.6.1.4.1.2021.11.7.0";
	public final static String ssSysContext =      ".1.3.6.1.4.1.2021.11.8.0";
	public final static String ssCpuUser =         ".1.3.6.1.4.1.2021.11.9.0";
	public final static String ssCpuSystem =       ".1.3.6.1.4.1.2021.11.10.0";
	public final static String ssCpuIdle =         ".1.3.6.1.4.1.2021.11.11.0";
	public final static String ssCpuRawUser =      ".1.3.6.1.4.1.2021.11.50.0";
	public final static String ssCpuRawNice =      ".1.3.6.1.4.1.2021.11.51.0";
	public final static String ssCpuRawSystem=     ".1.3.6.1.4.1.2021.11.52.0";
	public final static String ssCpuRawIdle =      ".1.3.6.1.4.1.2021.11.53.0";
	public final static String ssCpuRawWait =      ".1.3.6.1.4.1.2021.11.54.0";
	public final static String ssCpuRawKernel =    ".1.3.6.1.4.1.2021.11.55.0";
	public final static String ssCpuRawInterrupt = ".1.3.6.1.4.1.2021.11.56.0";
	public final static String ssIORawSent =       ".1.3.6.1.4.1.2021.11.57.0";
	public final static String ssIORawReceived =   ".1.3.6.1.4.1.2021.11.58.0";
	public final static String ssRawInterrupts =   ".1.3.6.1.4.1.2021.11.59.0";
	public final static String ssRawContexts =     ".1.3.6.1.4.1.2021.11.60.0";
	public final static String ssCpuRawSoftIRQ =   ".1.3.6.1.4.1.2021.11.61.0";
	public final static String ssRawSwapIn =       ".1.3.6.1.4.1.2021.11.62.0";
	public final static String ssRawSwapOut =      ".1.3.6.1.4.1.2021.11.63.0";
	public final static String ssCpuRawSteal =      ".1.3.6.1.4.1.2021.11.64.0";
	public final static String ssCpuRawGuest =      ".1.3.6.1.4.1.2021.11.65.0";
	public final static String ssCpuRawGuestNice =  ".1.3.6.1.4.1.2021.11.66.0";
	public final static String laLoad1m =          ".1.3.6.1.4.1.2021.10.1.3.1";
	public final static String laLoad5m =          ".1.3.6.1.4.1.2021.10.1.3.2";
	public final static String laLoad15m =         ".1.3.6.1.4.1.2021.10.1.3.3";
	public final static String hrSystemUptime =    ".1.3.6.1.2.1.25.1.1.0";
	public final static String hrSystemNumUsers=   ".1.3.6.1.2.1.25.1.5.0";
	public final static String hrSystemProcesses = ".1.3.6.1.2.1.25.1.6.0";
	public final static String tcpAttemptFails =   ".1.3.6.1.2.1.6.7.0";
	public final static String tcpCurrEstab =      ".1.3.6.1.2.1.6.9.0";
		//tcpCurrEstab
	public static OID[] COMMON_SYS_OIDS = null;

	public static final  Map<String, String> OID_MAP = new LinkedHashMap<String, String>(); //Name to OID
	public static final Map<String, String> OID_NAME_MAP = new LinkedHashMap<String, String>(); //OID to Name
	private static Logger logger = Logger.getLogger(SNMPClient.class.getName());
	
	Snmp snmp = null;
	String address = null;
    private String community;
	private String version;
	//v3 support
	private String username;
	private String password;
	private String authprotocol;
	private String privacypassphrase;
	private String privacyprotocol;
	private String context;
	static
	{
		OID_MAP.put("memTotalSwap",memTotalSwap);
		OID_MAP.put("memAvailSwap", memAvailSwap);
		OID_MAP.put("memTotalReal", memTotalReal);
		OID_MAP.put("memAvailReal", memAvailReal);
		OID_MAP.put("memTotalSwapTXT", memTotalSwapTXT);
		OID_MAP.put("memTotalRealTXT", memTotalRealTXT);
		OID_MAP.put("memTotalFree", memTotalFree);
		OID_MAP.put("memShared", memShared);
		OID_MAP.put("memBuffer",    memBuffer);
		OID_MAP.put("memCached",    memCached);
		OID_MAP.put("memUsedSwapTXT", memUsedSwapTXT);
		OID_MAP.put("memUsedRealTXT", memUsedRealTXT);
		OID_MAP.put("ssSwapIn",     ssSwapIn);
		OID_MAP.put("ssSwapOut",    ssSwapOut);
		OID_MAP.put("ssIOSent",     ssIOSent);
		OID_MAP.put("ssIOReceive",  ssIOReceive);
		OID_MAP.put("ssSysInterrupts",ssSysInterrupts);
		OID_MAP.put("ssSysContext",  ssSysContext);
		OID_MAP.put("ssCpuUser",     ssCpuUser);
		OID_MAP.put("ssCpuSystem",   ssCpuSystem);
		OID_MAP.put("ssCpuIdle",     ssCpuIdle);
		OID_MAP.put("ssCpuRawUser",  ssCpuRawUser);
		OID_MAP.put("ssCpuRawNice",  ssCpuRawNice);
		OID_MAP.put("ssCpuRawSystem",ssCpuRawSystem);
		OID_MAP.put("ssCpuRawIdle",  ssCpuRawIdle);
		OID_MAP.put("ssCpuRawWait",  ssCpuRawWait);
		OID_MAP.put("ssCpuRawKernel", ssCpuRawKernel);
		OID_MAP.put("ssCpuRawInterrupt",ssCpuRawInterrupt);
		OID_MAP.put("ssIORawSent",     ssIORawSent);
		OID_MAP.put("ssIORawReceived" ,ssIORawReceived);
		OID_MAP.put("ssRawInterrupts", ssRawInterrupts);
		OID_MAP.put("ssRawContexts",   ssRawContexts);
		OID_MAP.put("ssCpuRawSoftIRQ", ssCpuRawSoftIRQ);
		OID_MAP.put("ssRawSwapIn",     ssRawSwapIn);
		OID_MAP.put("ssRawSwapOut",    ssRawSwapOut);
		OID_MAP.put("ssCpuRawSteal",    ssCpuRawSteal);
		OID_MAP.put("ssCpuRawGuest",    ssCpuRawGuest);
		OID_MAP.put("ssCpuRawGuestNice",    ssCpuRawGuestNice);
		OID_MAP.put("laLoad1m",  laLoad1m);
		OID_MAP.put("laLoad5m",  laLoad5m);
		OID_MAP.put("laLoad15m",  laLoad15m);
		OID_MAP.put("hrSystemUptime",hrSystemUptime);
		OID_MAP.put("hrSystemNumUsers",hrSystemNumUsers);
		OID_MAP.put("hrSystemProcesses",hrSystemProcesses);
		OID_MAP.put("tcpAttemptFails",tcpAttemptFails);
		OID_MAP.put("tcpCurrEstab",tcpCurrEstab);

		COMMON_SYS_OIDS = new OID[OID_MAP.size()];
		int oidIdx = 0;
		for(Map.Entry<String, String> e: OID_MAP.entrySet())
		{
			OID_NAME_MAP.put(e.getValue(), e.getKey());
			COMMON_SYS_OIDS[oidIdx] = new OID(e.getValue());
			oidIdx ++;
		}
	};

	  public static class SNMPTriple
	  {
		  public String oid;
		  public String name;
		  public String value;
		  
		  public SNMPTriple(String oid, String name, String value)
		  {
			  this.oid = oid;
			  this.name = name;
			  this.value = value;
		  }
	  }

	/**
	 * Constructor
	 * @param host_name target host name. The final  snmp address will in the form of udp:host_name/port, for example udp:127.0.0.1/161
	 */
	public SNMPClient(String host_name)
	{
		address = "udp:"+host_name+"/161";
	}
	
	/**
	 * Retrieve common system SNMP data
	 * @return
	 * @throws IOException
	 */
	public Map<String, String> querySysData() throws IOException 
	{
		logger.fine("Query snmp for "+address);
		Map<String, String> resMap = null;
		 resMap = new java.util.LinkedHashMap<String, String>();
		 Map<OID, String> res = get(COMMON_SYS_OIDS);
		 if(res!=null)
		 {
			 for(Map.Entry<OID, String> e: res.entrySet())
			 {
				 if("noSuchObject".equalsIgnoreCase(e.getValue()))continue;
				 resMap.put(OID_NAME_MAP.get("."+e.getKey().toString()), e.getValue());
			 }
		 }
		 return resMap;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<SNMPTriple> querySysData3() throws IOException 
	{
		logger.fine("Query snmp system data for "+address);
		List<SNMPTriple> snmpList = new ArrayList<SNMPTriple>();
		 Map<OID, String> res = get(COMMON_SYS_OIDS);
		 if(res!=null)
		 {
			 for(Map.Entry<OID, String> e: res.entrySet())
			 {
				 if("noSuchObject".equalsIgnoreCase(e.getValue()))continue;
				 snmpList.add(new SNMPTriple(e.getKey().toString(), OID_NAME_MAP.get("."+e.getKey().toString()), e.getValue()));
			 }
		 }
		 return snmpList;
	}

	/**
	 * Start the Snmp session. If you forget the listen() method you will not
	 * get any answers because the communication is asynchronous
	 * and the listen() method listens for answers.
	 * @throws IOException
	 */
	public void start() throws IOException 
	{
		TransportMapping transport = new DefaultUdpTransportMapping();
		snmp = new Snmp(transport);
		if("3".equals(this.version))//add v3 support
		{
			USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);  
			SecurityModels.getInstance().addSecurityModel(usm);  
		}
		// Do not forget this line!
		transport.listen();
	}
	
	public void stop() throws IOException
	{
		if(snmp!=null)snmp.close();
		snmp = null;
	}
	/**
	 * Method which takes a single OID and returns the response from the agent as a String.
	 * @param oid
	 * @return
	 * @throws IOException
	 */
	public String getAsString(OID oid) throws IOException {
		ResponseEvent res = getEvent(new OID[] { oid });
		if(res!=null)
			return res.getResponse().get(0).getVariable().toString();
		return null;
	}
	
	private PDU createPDU() {
		if(!"3".equals(this.version))
			return new PDU();
        ScopedPDU pdu = new ScopedPDU();
        if(this.context != null && !this.context.isEmpty())
          pdu.setContextEngineID(new OctetString(this.context));    //if not set, will be SNMP engine id            
        return pdu;  
    }
	
	/**
	 * This method is capable of handling multiple OIDs
	 * @param oids
	 * @return
	 * @throws IOException
	 */
	public Map<OID, String> get(OID oids[]) throws IOException 
	{
		PDU pdu = createPDU();
		for (OID oid : oids) {
			pdu.add(new VariableBinding(oid));
		}
		pdu.setType(PDU.GET);
		ResponseEvent event = snmp.send(pdu, getTarget(), null);
		if(event != null) {
			PDU pdu2 = event.getResponse();
			VariableBinding[] binds = pdu2!=null?event.getResponse().toArray():null;
			if(binds!=null)
			{
				Map<OID, String> res = new LinkedHashMap<OID, String>(binds.length);
				for(VariableBinding b: binds)
					res.put(b.getOid(), b.getVariable().toString());
				return res;
			}else return null;
		}
		throw new RuntimeException("GET timed out");
	}

	public ResponseEvent getEvent(OID oids[]) throws IOException 
	{
		PDU pdu = createPDU();
		for (OID oid : oids) {
			pdu.add(new VariableBinding(oid));
		}
		pdu.setType(PDU.GET);
		ResponseEvent event = snmp.send(pdu, getTarget(), null);
		if(event != null) {
			return event;
		}
		throw new RuntimeException("GET timed out");
	}

	/**
	* This method returns a Target, which contains information about
	* where the data should be fetched and how.
	* @return
	*/
	private Target getTarget() {
		if("3".equals(this.version))return getTargetV3();
		Address targetAddress = GenericAddress.parse(address);
		CommunityTarget target = new CommunityTarget();
		//logger.info("snmp version "+this.version+", community: "+this.community);
		if(this.community == null || this.community.isEmpty())
			target.setCommunity(new OctetString("public"));
		else 
			target.setCommunity(new OctetString(this.community));
		target.setAddress(targetAddress);
		target.setRetries(2);
		target.setTimeout(5000);
		target.setVersion(this.getVersionInt());
		return target;
	}

	private Target getTargetV3() {
		//logger.info("Use SNMP v3, "+this.privacyprotocol +"="+this.password+", "+this.privacyprotocol+"="+this.privacypassphrase);
		OID authOID = AuthMD5.ID;
		if("SHA".equals(this.authprotocol))
			authOID = AuthSHA.ID;
		OID privOID = PrivDES.ID;
		if(this.privacyprotocol == null || this.privacyprotocol.isEmpty())
			privOID = null;
		UsmUser user = new UsmUser(new OctetString(this.username),  
				authOID, new OctetString(this.password),  //auth
				privOID, this.privacypassphrase!=null?new OctetString(this.privacypassphrase):null); //enc
		snmp.getUSM().addUser(new OctetString(this.username), user);  
		Address targetAddress = GenericAddress.parse(address);
		UserTarget target = new UserTarget();
		target.setAddress(targetAddress);
		target.setRetries(2);
		target.setTimeout(1500);
		target.setVersion(this.getVersionInt());
		if(privOID != null)
			target.setSecurityLevel(SecurityLevel.AUTH_PRIV);  
		else
			target.setSecurityLevel(SecurityLevel.AUTH_NOPRIV); 
		target.setSecurityName(new OctetString(this.username));
		return target;
	}

    public static final String DISK_TABLE_OID        ="1.3.6.1.4.1.2021.13.15.1.1";
    public static final String DISK_TABLE_DEVICE_OID ="1.3.6.1.4.1.2021.13.15.1.1.2";
    public static final String[] DISK_TABLE_ENTRIES = {"",
    													"diskIOIndex",
    													"diskIODevice",
    													"diskIONRead",
    													"diskIONWritten",
    													"diskIOReads",
    													"diskIOWrites",
    													"",
    													"",
    													"diskIOLA1",
    													"diskIOLA5",
    													"diskIOLA15",
    													"diskIONReadX",
    													"diskIONWrittenX"};
    
    public List<SNMPTriple> getDiskData(String device) throws IOException {
		
		int index = this.getDiskIndex(device);
		if(index<0)
		{
			return new ArrayList<SNMPTriple>();
		}
		logger.fine("Query disk stats for "+index);
		PDU pdu = createPDU();
		for ( int i=1; i< DISK_TABLE_ENTRIES.length; i++) {
			if(DISK_TABLE_ENTRIES[i].length()==0)continue;
			pdu.add(new VariableBinding(new OID("."+DISK_TABLE_OID+"."+i+"."+index)));
		}
		pdu.setType(PDU.GET);
		Map<String, String> res = new HashMap<String, String>(13);
		ResponseEvent event = snmp.send(pdu, getTarget(), null);
		if(event != null) {
			VariableBinding[] binds = event.getResponse().toArray();
			for(VariableBinding b: binds)
				res.put(b.getOid().toString(), b.getVariable().toString());
			//logger.info(res.toString());
		}		
        List<SNMPTriple> resList = new ArrayList<SNMPTriple>(res.size());
        for(int i=1;i<DISK_TABLE_ENTRIES.length; i++) {
			if(DISK_TABLE_ENTRIES[i].length()==0)continue;
			resList.add(new SNMPTriple("."+DISK_TABLE_OID+"."+i+"."+index, DISK_TABLE_ENTRIES[i], res.get(DISK_TABLE_OID+"."+i+"."+index)));
        }
         return resList;
   }
	
   public Map<String, List<SNMPTriple>> getMultiDiskData() throws IOException {
		
		Map<String, List<SNMPTriple>> resMap = new HashMap<String, List<SNMPTriple>>();
		Map<Integer, String> indexes = this.getDiskIndexes();
		if(indexes == null || indexes.size() == 0)
			return  resMap;
		
		logger.fine("Query disk stats");
		PDU pdu = createPDU();
		int reqSize = 0;
		for(Map.Entry<Integer, String> entry: indexes.entrySet())
		{
			for ( int i=1; i< DISK_TABLE_ENTRIES.length; i++) {
				if(DISK_TABLE_ENTRIES[i].length()==0)continue;
				reqSize++;
				pdu.add(new VariableBinding(new OID("."+DISK_TABLE_OID+"."+i+"."+entry.getKey())));
			}
		}
		pdu.setType(PDU.GET);
		Map<String, String> res = new HashMap<String, String>(13);
		ResponseEvent event = snmp.send(pdu, getTarget(), null);
		if(event != null) {
			PDU resp = event.getResponse();
			if(resp == null)
			{
				logger.info(this.address + ": Unexpected snmp response: "+event+", request size " + reqSize );
				return resMap;
			}
			VariableBinding[] binds = resp.toArray();
			for(VariableBinding b: binds)
				res.put(b.getOid().toString(), b.getVariable().toString());
			//logger.info(res.toString());
		}
		for(Map.Entry<Integer, String> entry: indexes.entrySet())
		{
			List<SNMPTriple> resList = new ArrayList<SNMPTriple>(res.size());
			for(int i=1;i<DISK_TABLE_ENTRIES.length; i++) {
				if(DISK_TABLE_ENTRIES[i].length()==0)continue;
				resList.add(new SNMPTriple("."+DISK_TABLE_OID+"."+i+"."+entry.getKey(), DISK_TABLE_ENTRIES[i], 
						res.get(DISK_TABLE_OID+"."+i+"."+entry.getKey())));
			}
			resMap.put(entry.getValue(), resList);
		}
         return resMap;
   }	
	
	private int getDiskIndex(String device) throws IOException {
		
        TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());
        
        logger.fine("Query "+this.address+" for disk data: "+device);
         @SuppressWarnings("unchecked")
         List<TableEvent> events = tUtils.getTable(getTarget(), new OID[]{new OID("."+DISK_TABLE_DEVICE_OID)}, null, null);

         for (TableEvent event : events) {
           if(event.isError()) {
          	 logger.warning(this.address + ": SNMP event error: "+event.getErrorMessage());
          	 continue;
                //throw new RuntimeException(event.getErrorMessage());
           }
           for(VariableBinding vb: event.getColumns()) {
        	   String key = vb.getOid().toString();
        	   String value = vb.getVariable().toString();
        	   if(value!=null && value.equals(device))
        	   {
        	       logger.fine("Find device OID entry: "+key);
        	         int index = -1;
        	         String[] strs = key.split("\\.");
        	         try
        	         {
        	        	 index = Integer.parseInt(strs[strs.length-1]);
        	         }catch(Exception ex){}
        	         return index;
        	   }
           }
         }
         return -1;
   }

	private Map<Integer, String> getDiskIndexes() throws IOException {
		Map<Integer, String> diskIndexes = new HashMap<Integer, String>();
        TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());
        
        logger.fine("Query "+this.address+" for disk oids");
         @SuppressWarnings("unchecked")
         List<TableEvent> events = tUtils.getTable(getTarget(), new OID[]{new OID("."+DISK_TABLE_DEVICE_OID)}, null, null);

         for (TableEvent event : events) {
           if(event.isError()) {
          	 logger.warning(this.address + ": SNMP event error: "+event.getErrorMessage());
          	 continue;
                //throw new RuntimeException(event.getErrorMessage());
           }
           
           for(VariableBinding vb: event.getColumns()) {
        	   String key = vb.getOid().toString();
        	   String value = vb.getVariable().toString();
        	   if(value == null || value.isEmpty() || value.startsWith("dm-"))continue;//ignore dm disk
        	   if(value.startsWith("ram") || value.startsWith("loop") )continue;//ignore dm disk
        	   char c = value.charAt(value.length()-1);
        	   if(c>='0' && c<='9' )
        	   {
        		   if(value.startsWith("sd"))
        		   {
        			   if(value.length()>2)
        			   {
        				   char d = value.charAt(2);
        				   if(d>='a' && d<='z')continue;
        			   }
        		   }
        	   }
        	   logger.fine("Find device OID entry: "+key);
        	   int index = -1;
        	   String[] strs = key.split("\\.");
        	   try
        	   {
        		   index = Integer.parseInt(strs[strs.length-1]);
        	       diskIndexes.put(index,  value); 	 
        	   }catch(Exception ex){}
        	}
         }
         return diskIndexes;
   }

    public static final String IF_TABLE_OID         = "1.3.6.1.2.1.31.1.1.1";
    public static final String IF_TABLE_DEVICE_OID  = "1.3.6.1.2.1.31.1.1.1.1";
    public static final String[] IF_TABLE_ENTRIES = {"",
    													"ifName",
    													"ifInMulticastPkts",
    													"ifInBroadcastPkts",
    													"ifOutMulticastPkts",
    													"ifOutBroadcastPkts",
    													"ifHCInOctets",
    													"ifHCInUcastPkts",
    													"ifHCInMulticastPkts",
    													"ifHCInBroadcastPkts",
    													"ifHCOutOctets",
    													"ifHCOutUcastPkts",
    													"ifHCOutMulticastPkts",
    													"ifHCOutBroadcastPkts",
    													"ifLinkUpDownTrapEnable",
    													"ifHighSpeed",
    													"ifPromiscuousMode",
    													"ifConnectorPresent",
    													"ifAlias",
    													"ifCounterDiscontinuityTime"};

  private Map<Integer, String> getNetIfIndexes(String device) throws IOException {
	    Map<Integer, String> ifMaps = new HashMap<Integer, String> ();
		
        TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());
        
        logger.fine("Query "+this.address+" for network interface, excluding lo");
         @SuppressWarnings("unchecked")
         List<TableEvent> events = tUtils.getTable(getTarget(), new OID[]{new OID("."+IF_TABLE_DEVICE_OID)}, null, null);

         for (TableEvent event : events) {
           if(event.isError()) {
          	 logger.warning(this.address + ": SNMP event error: "+event.getErrorMessage());
          	 continue;
                //throw new RuntimeException(event.getErrorMessage());
           }
           for(VariableBinding vb: event.getColumns()) {
        	   String key = vb.getOid().toString();
        	   String value = vb.getVariable().toString();
        	   if(device!=null && !device.isEmpty() && !value.equalsIgnoreCase(device))
        		   continue;
        	   if(value!=null && !value.equalsIgnoreCase("lo"))
        	   {
        	       logger.fine("Find device OID entry: "+key);
        	         int index = -1;
        	         String[] strs = key.split("\\.");
        	         try
        	         {
        	        	 index = Integer.parseInt(strs[strs.length-1]);
        	        	 ifMaps.put(index, value);
        	         }catch(Exception ex){}
        	   }
           }
         }
         return ifMaps;
   }

   public Map<String, List<SNMPTriple>> getNetIfData(String device) throws IOException {
		
	    Map<Integer, String> ifMaps = new HashMap<Integer, String> ();
		Map<String, List<SNMPTriple>> resMap = new HashMap<String, List<SNMPTriple>>();
		Map<String, String> res = new HashMap<String, String>();
        TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());
        
        logger.fine("Query "+this.address+" for network interface, excluding lo");
         @SuppressWarnings("unchecked")
         List<TableEvent> events = tUtils.getTable(getTarget(), new OID[]{new OID("."+IF_TABLE_OID)}, null, null);

         for (TableEvent event : events) {
           if(event.isError()) {
          	 logger.warning(this.address + ": SNMP event error: "+event.getErrorMessage()+", already returned: "+ifMaps);
          	 continue;
                //throw new RuntimeException(event.getErrorMessage());
           }
           for(VariableBinding vb: event.getColumns()) {
        	   String key = vb.getOid().toString();
        	   String value = vb.getVariable().toString();
        	   res.put(key, value);
        	   if(key.startsWith(IF_TABLE_DEVICE_OID+"."))
        	   {
        	     if(device!=null && !device.isEmpty() && !value.equalsIgnoreCase(device))
        		   continue;
        	     if(value!=null && !value.equalsIgnoreCase("lo"))
        	     {
        	       logger.fine("Find device OID entry: "+key);
        	         int index = -1;
        	         String[] strs = key.split("\\.");
        	         try
        	         {
        	        	 index = Integer.parseInt(strs[strs.length-1]);
        	        	 ifMaps.put(index, value);
        	         }catch(Exception ex){}
        	     }
             }
           }//for var
         }//for event
		
		for(Map.Entry<Integer, String> entry: ifMaps.entrySet())
		{
			int index = entry.getKey();
			String ifName = entry.getValue();
			//ignore the case with no incoming and no outgoing traffic
			if("0".equals(res.get(IF_TABLE_OID+".6."+index)) && "0".equals(res.get(IF_TABLE_OID+".10."+index)))continue;
			resMap.put(ifName, new ArrayList<SNMPTriple>(IF_TABLE_ENTRIES.length));
			for(int i=1;i<IF_TABLE_ENTRIES.length; i++) {
			    if(IF_TABLE_ENTRIES[i].length()==0)continue;
			    resMap.get(ifName).add(new SNMPTriple("."+IF_TABLE_OID+"."+i+"."+index, IF_TABLE_ENTRIES[i], res.get(IF_TABLE_OID+"."+i+"."+index)));
			}
		}
         return resMap;
   }

   public Map<String, List<SNMPTriple>> getNetIfData3(String device) throws IOException {
		
		Map<String, List<SNMPTriple>> resMap = new HashMap<String, List<SNMPTriple>>();
		Map<Integer, String> indexMap = this.getNetIfIndexes(device);
		
		if(indexMap == null || indexMap.size() ==0)
		{
			
			logger.warning("Cannot find network interfaces ");
			return resMap;
		}
		logger.fine("Query net if stats for network");
		PDU pdu = createPDU();
		for(Map.Entry<Integer, String> entry: indexMap.entrySet())
		for ( int i=1; i< IF_TABLE_ENTRIES.length; i++) {
			if(IF_TABLE_ENTRIES[i].length()==0)continue;
			pdu.add(new VariableBinding(new OID("."+IF_TABLE_OID+"."+i+"."+entry.getKey())));
		}
		pdu.setType(PDU.GET);
		Map<String, String> res = new HashMap<String, String>(IF_TABLE_ENTRIES.length*indexMap.size());
		ResponseEvent event = snmp.send(pdu, getTarget(), null);
		if(event != null) {
			VariableBinding[] binds = event.getResponse().toArray();
			for(VariableBinding b: binds)
				res.put(b.getOid().toString(), b.getVariable().toString());
			//logger.info(res.toString());
		}
		for(Map.Entry<Integer, String> entry: indexMap.entrySet())
		{
			int index = entry.getKey();
			String ifName = entry.getValue();
			//ignore the case with no incoming and no outgoing traffic
			if("0".equals(res.get(IF_TABLE_OID+".6."+index)) && "0".equals(res.get(IF_TABLE_OID+".10."+index)))continue;
			resMap.put(ifName, new ArrayList<SNMPTriple>(IF_TABLE_ENTRIES.length));
			for(int i=1;i<IF_TABLE_ENTRIES.length; i++) {
			    if(IF_TABLE_ENTRIES[i].length()==0)continue;
			    resMap.get(ifName).add(new SNMPTriple("."+IF_TABLE_OID+"."+i+"."+index, IF_TABLE_ENTRIES[i], res.get(IF_TABLE_OID+"."+i+"."+index)));
			}
		}
         return resMap;
   }

   public static final String STORAGE_TABLE_OID         = "1.3.6.1.2.1.25.2.3.1";
   public static final String STORAGE_TABLE_DEVICE_OID  = "1.3.6.1.2.1.25.2.3.1.1";
   public static final String[] STORAGE_TABLE_ENTRIES = {"",
   													"hrStorageIndex",
   													"hrStorageType",
   													"hrStorageDescr",
   													"hrStorageAllocationUnits",
   													"hrStorageSize",
   													"hrStorageUsed",
   													"hrStorageAllocationFailures"};
   public Map<String, List<SNMPTriple>> getStorageData(String device) throws IOException {
	   List<SNMPTriple> resList = querySingleSNMPTableByOID("."+STORAGE_TABLE_OID);
	   List<Integer> idxList = new ArrayList<Integer>();
	   Map<String, String> tmpMap = new HashMap<String, String>();
	   for(SNMPTriple e: resList)
	   {
		   tmpMap.put(e.oid, e.value);
		   if(e.oid.startsWith(STORAGE_TABLE_DEVICE_OID))
		   {
			   try
			   {
			     int idx = Integer.parseInt(e.oid.substring(STORAGE_TABLE_DEVICE_OID.length() + 1));
			     idxList.add(idx);
			   }catch(Exception ex){}
		   }
	   }
	   Map<String, List<SNMPTriple>> resMap = new HashMap<String, List<SNMPTriple>>();
	   for(int idx: idxList)
	   {
		   String name = tmpMap.get(STORAGE_TABLE_OID+"." + 3 +"." + idx);
		   if(device != null && !name.equalsIgnoreCase(device))continue;
		   List<SNMPTriple> entryList = new ArrayList<SNMPTriple>();
		   for(int i = 1; i<STORAGE_TABLE_ENTRIES.length; i++)
		   {
			   entryList.add(new SNMPTriple("."+STORAGE_TABLE_OID+"."+i+"."+idx, STORAGE_TABLE_ENTRIES[i], tmpMap.get(STORAGE_TABLE_OID+"."+i+"."+idx)));			   
		   }
		   resMap.put(name, entryList);
	   }
	   return resMap;
   }
	
   /**
    * For test SNMP purpose. Used to check if individual SNMP entry is supported
    * @param oid
    * @return
    * @throws IOException 
    */
   public List<SNMPTriple> querySingleSNMPEntryByOID(String oid) throws IOException
   {
	   if(oid == null || oid.isEmpty())return null;
	   if(!oid.startsWith("."))oid = "."+oid;
	   List<SNMPTriple> snmpList = new ArrayList<SNMPTriple>();
		 Map<OID, String> res = get(new OID[]{new OID(oid)});
		 if(res!=null)
		 {
			 for(Map.Entry<OID, String> e: res.entrySet())
			 {
				 //if("noSuchObject".equalsIgnoreCase(e.getValue()))continue;
				 snmpList.add(new SNMPTriple(e.getKey().toString(), "", e.getValue()));
			 }
		 }
		 return snmpList;
   }
   
   public List<SNMPTriple> querySingleSNMPTableByOID(String oid) throws IOException
   {
	   if(oid == null || oid.isEmpty())return null;
	   if(!oid.startsWith("."))oid = "."+oid;
       TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());
       List<TableEvent> events = tUtils.getTable(getTarget(), new OID[]{new OID(oid)}, null, null);

	   List<SNMPTriple> snmpList = new ArrayList<SNMPTriple>();
       
       for (TableEvent event : events) {
         if(event.isError()) {
        	 logger.warning(this.address + ": SNMP event error: "+event.getErrorMessage());
        	 continue;
              //throw new RuntimeException(event.getErrorMessage());
         }
         for(VariableBinding vb: event.getColumns()) {
      	   String key = vb.getOid().toString();
      	   String value = vb.getVariable().toString();
      	 snmpList.add(new SNMPTriple(key, "", value));
         }
       }
	   return snmpList;
   }
   
   public static final String PROCESS_TABLE_OID = "1.3.6.1.2.1.25.4.2.1"; //hrSWRunTable
   //1.3.6.1.2.1.25.4.2.1.2.
   /**
    * Query index for given process name. Note the parameter only provides 128 characters,
    * so it could be difficult for us to differentiate each other if multi processes with same name exist.
    * So we will return this list and use the sum from all processes for our metrics
    * @param process
    * @return
    * @throws IOException
    */
   private List<Integer> getProcessIndexes(String process) throws IOException {
	   List<Integer> indexes = new ArrayList<Integer> ();
       if(process == null || process.isEmpty())return indexes;
		
       TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());
       logger.fine("Query "+this.address+" for process " + process);
        @SuppressWarnings("unchecked")
        List<TableEvent> events = tUtils.getTable(getTarget(), new OID[]{new OID("."+PROCESS_TABLE_OID)}, null, null);

        for (TableEvent event : events) {
          if(event.isError()) {
         	 logger.warning(this.address + ": SNMP event error: "+event.getErrorMessage());
         	 continue;
               //throw new RuntimeException(event.getErrorMessage());
          }
          for(VariableBinding vb: event.getColumns()) {
       	   String key = vb.getOid().toString();
       	   String value = vb.getVariable().toString();
       	   if(process!=null && !process.isEmpty() && !value.equalsIgnoreCase(process))
       		   continue;
       	   if(value!=null)
       	   {
       	       logger.fine("Find process OID entry: "+key);
       	       int index = -1;
       	       String[] strs = key.split("\\.");
       	       try
       	       {
       	    	   index = Integer.parseInt(strs[strs.length-1]);
       	    	   indexes.add(index);
       	       }catch(Exception ex){}
       	   }
          }
        }
        return indexes;
  }

   public static final String PROCESS_PERF_TABLE_OID  = "1.3.6.1.2.1.25.5.1.1";//hrSWRunPerfTable
   public static final String[] PROCESS_PERF_TABLE_ENTRIES = {"",
   													"hrSWRunPerfCPU",
   													"hrSWRunPerfMem"};
   public List<SNMPTriple> getProcessData(String processName) throws IOException {
		List<SNMPTriple> resList = new ArrayList<SNMPTriple>();
		List<Integer> prIndexes = this.getProcessIndexes(processName);
		if(prIndexes == null || prIndexes.size() == 0)
			return  resList;
		
		logger.fine("Query process stats");
		PDU pdu = createPDU();
		for(Integer idx: prIndexes)
		{
			for ( int i=1; i< PROCESS_PERF_TABLE_ENTRIES.length; i++) {
				if(PROCESS_PERF_TABLE_ENTRIES[i].length()==0)continue;
				pdu.add(new VariableBinding(new OID("."+PROCESS_PERF_TABLE_OID+"."+i+"."+idx)));
				//logger.info("Adding " + "."+PROCESS_PERF_TABLE_OID+"."+i+"."+idx);
			}
		}
		pdu.setType(PDU.GET);
		Map<String, String> res = new HashMap<String, String>(prIndexes.size()*2);
		ResponseEvent event = snmp.send(pdu, getTarget(), null);
		if(event != null) {
			VariableBinding[] binds = event.getResponse().toArray();
			for(VariableBinding b: binds)
			{
				res.put(b.getOid().toString(), b.getVariable().toString());
				//logger.info(b.getOid().toString() +", "+ b.getVariable().toString());
			}
		}
		//logger.info("result: "+res);
		for(int i=1;i<PROCESS_PERF_TABLE_ENTRIES.length; i++) {
			if(PROCESS_PERF_TABLE_ENTRIES[i].length()==0)continue;
			BigDecimal data = new BigDecimal(0);
			for(Integer idx: prIndexes)
			{
				data = data.add(new BigDecimal(res.get(PROCESS_PERF_TABLE_OID+"."+i+"."+idx)));
			}
			resList.add(new SNMPTriple("", PROCESS_PERF_TABLE_ENTRIES[i], data.toString()));
		}
        return resList;
   }

   public Map<String, String> queryMysqld() throws IOException 
	{
		logger.fine("Query mysqld for "+address);
		Map<String, String> resMap = null;
		 resMap = new java.util.LinkedHashMap<String, String>();
		 List<SNMPTriple> res = this.getProcessData("mysqld");
		 if(res!=null)
		 {
			 for(SNMPTriple e: res)
			 {
				 if("noSuchObject".equalsIgnoreCase(e.value))continue;
				 resMap.put(e.name, e.value);
			 }
		 }
		 return resMap;
	}

	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
    
	public int getVersionInt()
	{
		if("1".equals(this.version))
			return SnmpConstants.version1;
		else if("3".equals(this.version))
			return SnmpConstants.version3;
		else 
			return SnmpConstants.version2c;
	}
	public void setSnmpSetting(SNMPSettings.SNMPSetting setting)
	{
	  if(setting == null)return;
	  this.setCommunity(setting.getCommunity());
	  this.setVersion(setting.getVersion());
	  if("3".equals(this.version))
	  {
		  this.username = setting.getUsername();
		  this.password = setting.getPassword();
		  this.authprotocol = setting.getAuthProtocol();
		  this.privacypassphrase = setting.getPrivacyPassphrase();
		  this.privacyprotocol = setting.getPrivacyProtocol();
		  this.context = setting.getContext();
	  }
	}
}
