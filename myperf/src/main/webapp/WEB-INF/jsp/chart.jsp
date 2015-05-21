<%@page trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Performance Metrics</title>
<script type="text/javascript" src="js/common.js"></script> 
<jsp:include page="commheader.jsp" flush="true" />
<script src="js/d3.v3.min.js"></script>
<script src="js/d3simplechart.js"></script>
<script language="javascript">
  //if display is block, use block
  function toggleChart(div, display)
  {
    var d = mydom(div);
    var img = mydom("img_"+div);
    var imgdiv = '#img_'+div;
    if(display=='none')//instructed to hide
    {
      d.style.display = 'none';  
      $(imgdiv).text('+');
    }
    else if(d.style.display=='none'||display=='block')
    {
      d.style.display = 'block';
      $(imgdiv).text('-');
    }
    else 
    {
      d.style.display = 'none';  
      $(imgdiv).text('+');
    }
  }
  
  var MetricsList=[
    <c:forEach var="udm" items="${udms}" varStatus="stat">${stat.index>0?",":""} {label:"${udm}", idx:${stat.index}}</c:forEach>
  ];
  var DBList=[""
    <c:forEach var="cluster" items="${mydbs}">
       <c:forEach var="db" items="${dbMap[cluster].instances}">,"${cluster}|${db.hostName}"</c:forEach>
    </c:forEach>  
  ]; 
</script>
<style>
 .warning-highlighted{background-color:red;}
 #db_block table{border:0px;margin-bottom:1px;border-spacing:0px;}
 #db_block td{border:0px;padding-top:0px;padding-bottom:0px;}
 #db_block th{border:0px;padding-bottom:0px;padding-top:0px;}
 .ui-autocomplete {max-height: 200px;overflow-y: auto; overflow-x: hidden;}
 </style>
</head>
<%
    String[] charts = new String[]{"chart_slow_queries","Slow Queries",
                                   "chart_threads", "Threads",
                                   "chart_connections","Connections",
                                   "chart_tmp","Temp Tables",
                                   "chart_queries","Queries",
     	                           "chart_innodb_io", "InnoDB IO",
                                   "chart_row_ops","InnoDB Row Operations",
                                   "chart_locks","InnoDB Row Lock Time",
                                   "chart_buffer_flush","InnoDB Buffer Flushes",
                                   "chart_buffer","InnoDB Buffers",
                                   "chart_app_data", "Data Received/Sent",
                                   "chart_sort", "Sorting Operations"
                                  };    
    String[] os_charts = new String[]{"chart_cpu","OS CPU",
                                   "chart_load_avg","Load Average (5m)",
                                   "chart_mem","Memory",
                                   "chart_disk_io","Disk Reads/Writes (Bytes)",
                                   "chart_disk_iops","Disk IOPS",
                                   "chart_swap", "Swap"
                                  };    
    
    
%>
<body>
<div><!-- query form -->  
  <table id="db_block" style="margin-bottom:6px;">
    <tr><td>Group Name</td><td>DB Host</td><td>Start Date</td><td>Time</td><td>End Date</td><td>Time</td><td>&nbsp;</td></tr>
    <tr><td><select name="dbgroup" id="dbgroup" >
             <option value="">----</option>
              <c:forEach var="cluster" items="${mydbs}" varStatus="stat">
<c:if test="${cluster==sessionScope.group}">
    			<option value="${cluster}" selected="true" ${stat.index>=mydbSize?"style='color:red;'":""}>${cluster}</option>
</c:if>
<c:if test="${cluster!=sessionScope.group}">              
    			<option value="${cluster}" ${stat.index>=mydbSize?"style='color:red;'":""}>${cluster}</option>
</c:if>    			
  			  </c:forEach>          
          </select>&nbsp;<input type="button" value="Find" onclick="prepareDBSearch('dbgroup');"/>
        </td>
        <td><select  name="host" id="host">
<c:if test="${sessionScope.group==null}">
             <option value=""> --- </option>
</c:if>             
<c:if test="${sessionScope.group!=null}">
              <c:forEach var="db" items="${dbMap[sessionScope.group].instances}">
  <c:if test="${db.hostName==sessionScope.host}">
    			<option value="${db.hostName}" selected="true">${db.hostName}</option>
  </c:if>
  <c:if test="${db.hostName!=sessionScope.host}">
    			<option value="${db.hostName}">${db.hostName}</option>
  </c:if>    			
              </c:forEach>
</c:if>        
           </select>
        </td>
        <td><input type="hidden"  name="begin_time" id="begin_time" value="${start_date} ${start_hour}:00:00"/>
        <input type="text"  name="begin_time_d" id="begin_time_d" value="${start_date}" class="tcal" style="width:100px;"/></td>
        <td><select id="begin_time_t" name="begin_time_t">
<c:forEach var="h" items="${hours}" varStatus="stat"> 
  <option value="${h}" ${h==start_hour?"selected":""}>${h}</option>
</c:forEach>
          </select></td>
        <td><input type="hidden"  name="end_time" id="end_time" value="${end_date} ${end_hour}:00:00"/>
        <input type="text"  name="end_time_d" id="end_time_d" value="${end_date}" class="tcal"  style="width:100px;"/></td>
        <td><select id="end_time_t" name="end_time_t">
<c:forEach var="h" items="${hours}" varStatus="stat"> 
  <option value="${h}" ${h==end_hour?"selected":""}>${h}</option>
</c:forEach>
          </select></td>
        <td><select id="chart_type" name="chart_type">
             <option value="globalstatus">Global Status</option>
             <option value="snmp">OS</option>
            </select>
            &nbsp;<input type="button" id="btn_metrics_activity" value="&#9658;" onclick="retrieveMetrics();"/></td>
      </tr>
   </table>       
</div><!-- end of query form -->
<script language="javascript">
  $("#begin_time_d").datepicker({dateFormat: 'yy-mm-dd'});
  $("#end_time_d").datepicker({dateFormat: 'yy-mm-dd'});
</script>

<div id="chart_main" class="clearTabView">
  <ul>
     <li><a href="#globalstatus_charts_div" title="A set of predefined global status metrics">Common Status</a></li>
     <li><a href="#snmp_charts_div" title="OS level metrics from SNMP">OS</a></li>
     <li><a href="#udm_charts_div" title="Display one or two metrics and their correlation">Compare Metrics</a></li>
     <li><a href="#host_metrics_charts_div" title="Display one metric for multiple hosts for comparison">Compare Hosts</a></li>
  </ul>
  <div id="globalstatus_charts_div"> <!-- place holder for all charts -->
    <div id="globalstatus_charts">
      <% for(int i=0;i<charts.length/2;i++){ %>
        <div id="div_<%=charts[2*i] %>">
          <div id="title_<%=charts[2*i] %>">
            <div id="img_<%=charts[2*i] %>" class="box"  onclick="toggleChart('<%= charts[2*i] %>');">+</div>&nbsp;<%=charts[2*i+1] %>
          </div>
          <div id="<%=charts[2*i] %>" style="width:1024px;height:160px;display:none;"></div> 
        </div>
      <% } %>
    </div>
  </div><!-- global status -->

  <div id="snmp_charts_div"> <!-- place holder for all charts -->
    <div id="snmp_charts">
      <% for(int i=0;i<os_charts.length/2;i++){ %>
        <div id="div_<%=os_charts[2*i] %>">
          <div id="title_<%=os_charts[2*i] %>">
            <div id="img_<%=os_charts[2*i] %>" class="box"  onclick="toggleChart('<%= os_charts[2*i] %>');">+</div>&nbsp;<%=os_charts[2*i+1] %>
          </div>
          <div id="<%=os_charts[2*i] %>" style="width:1024px;height:160px;display:none;"></div> 
        </div>
      <% } %>
    </div>
  </div><!-- os -->

  <div id="udm_charts_div">
    <div> <input id="metrics1" name="metrics1" style="width:360px;" placeholder="First Metric" required/> &nbsp;&nbsp;  
        <input id="metrics2" name="metrics2" style="width:360px;" placeholder="Second Metric"/> &nbsp;&nbsp;
        <input type="button" id="btn_udm_activity" value="&#9658;" onclick="retrieveUDMMetrics();"/>
        &nbsp;&nbsp;<input type="button" id="btn_udm_activity_next" value="Next &#9658;" onclick="retrieveNextUDMMetrics();"/>
        &nbsp;&nbsp;Auto: <input type="checkbox" id="btn_udm_activity_next_auto" value="y" title="Auto compare the metrics"/>     
    </div>
    <div id="div_chart1" style="width:1024px;height:160px;"></div> 
    <div id="div_chart2" style="width:1024px;height:160px;"></div> 
    <div id="div_scatterplot" style="width:1024px;height:300px;"></div>         
  </div><!-- udm -->

  <!-- host -->
  <div id="host_metrics_charts_div">
    <div>DB to Compare: <input type="text" id="dbgroup2"  name="dbgroup2" placeholder="DB to Compare" style="width:200px;" />&nbsp;&nbsp;
      <input id="host_metrics" name="host_metrics" style="width:360px;" placeholder="Metric Name"/>
         &nbsp;&nbsp;<input type="button" id="btn_host_metrics_activity" value="&#9658;" onclick="retrieveHostMetricsMetrics();"/>
         &nbsp;&nbsp;<input type="button" id="btn_host_metrics_activity_next" value="Next &#9658;" onclick="retrieveNextHostMetricsMetrics();"/>       
         &nbsp;&nbsp;Auto: <input type="checkbox" id="btn_host_metrics_activity_next_auto" value="y" title="Auto compare the metrics"/>
    </div>   
    <div id="host_metrics_charts" style="display:none">
      <% 
        int HOST_GROUP_COUNT = 20;
        for(int i=1;i<=HOST_GROUP_COUNT; i++){
      %>
        <div id="title_div_host_chart<%= i %>">
          <div id="img_div_host_chart<%= i %>" class="box"  onclick="toggleChart('div_host_chart<%= i %>');">+</div>&nbsp;<span id="div_host_chart_label<%= i %>" style="font-weight:bold;"></span>
        </div>
        <div id="div_host_chart<%= i %>" style="width:1024px;height:160px;"></div> 
      <% }   %>
    </div>  
  </div><!-- host_metrics -->

  <div id="common_msg" style="font:red;"></div>
  <div>Notes: select a database and optionally a date range, then click "&#9658;" button. OS metrics data are gathered from SNMP when available</div>
</div>

<jsp:include page="dbsearch.jsp" flush="true" >
  <jsp:param name="x" value="20" />
  <jsp:param name="y" value="40" />  
</jsp:include>
<script language=javascript>
$('#dbgroup').change(function()
  {
    query_hostlist_main(mydomval('dbgroup'), 'host');
  }
);
$('#chart_main').tabs(
{
  activate: function( event, ui ) 
   {
     var idx = $('#chart_main').tabs("option", "active");
     if(idx < 2)
       mydom('chart_type').selectedIndex = idx;
   }  
});

function showChart(chart)
{
   var chartList = {"globalstatus":0, "snmp":1, "udm":2, "host_metrics":3};
   $('#chart_main').tabs("option", "active", chartList[chart]);
}

  var hostMetricsIdx = 0;
  var secMetricsIdx = 0;
  $("#host_metrics").autocomplete({source: MetricsList, minLength:0, select: function(e, ui){hostMetricsIdx = ui.item.idx;}}) 
                    .bind('focus', function(){$(this).autocomplete("search");});  
  $("#metrics1").autocomplete({source: MetricsList, minLength:0}) 
                    .bind('focus', function(){$(this).autocomplete("search");});  
  $("#metrics2").autocomplete({source: MetricsList, minLength:0, select: function(e, ui){secMetricsIdx = ui.item.idx;}}) 
                    .bind('focus', function(){$(this).autocomplete("search");});  
  $("#dbgroup2").autocomplete({source: DBList, minLength:0}) 
                    .bind('focus', function(){$(this).autocomplete("search");});  

/*
  print out error or information message inside uiDom element.
  error: if true, error, otherwise, info only 
  uiDom: dom id
*/
function reportStatus(error, uiDom, message)
{
  if(uiDom != null)
  {
    $('#'+uiDom).removeClass((!error)? "cssError": "cssInfo")
             .addClass((error)? "cssError": "cssInfo");
    $('#'+uiDom).text(message);
  }else
    alert(message);  
}

function checkDBSelection(dbid, host)
{
  if(dbid == null || dbid == ''
	  || host == null || host == '') 
  {
	    alert('Please select a db host ...');
		return false;
  }
  return true;	
}
  //main function
   
  function retrieveMetrics()
  { 
    if(!checkDBSelection(mydomval('dbgroup'), mydomval('host')))
      return;
        
    resolveDates();
    var m = mydomval("chart_type"); 
    var src = "db", w = 986, h = 160;
    if(m=='globalstatus')
    {
      showChart(m);
      simpleCharts({
        topUrl:"metrics/get", dbGroup:mydomval("dbgroup"), dbHost:mydomval("host"), start_datetime:mydomval("begin_time"),end_datetime:mydomval("end_time"),
        messageCB:function(str, isError){reportStatus(isError, "common_msg", str);},
        statusCB:function(domid,ret){toggleChart(domid,ret==1?"block":"none");},
        charts:[
          {domid:"chart_slow_queries", label:"Slow Queries", width:w, height:h, colors:["red"],
           metrics:[
             {name:"STATUS.STATUS_COMMON.SLOW_QUERIES", label:"SLOW_QUERIES/MIN", inc:1}
           ]
          },
          {domid:"chart_threads", label:"Threads", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_COMMON.THREADS_CONNECTED", label:"THREADS_CONNECTED", inc:0},
             {name:"STATUS.STATUS_COMMON.THREADS_RUNNING", label:"THREADS_RUNNING", inc:0}
           ]
          },
          {domid:"chart_connections", label:"Connections", width:w, height:h, colors:["blue"],
           metrics:[
             {name:"STATUS.STATUS_COMMON.CONNECTIONS", label:"CONNECTIONS/MIN", inc:1}
           ]
          },
          {domid:"chart_tmp", label:"Temp Tables", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_COMMON.CREATED_TMP_DISK_TABLES", label:"CREATED_TMP_DISK_TABLES/MIN", inc:1},
             {name:"STATUS.STATUS_COMMON.CREATED_TMP_FILES", label:"CREATED_TMP_FILES/MIN", inc:1}
           ]
          },
          {domid:"chart_queries", label:"Queries", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_COMMON.QUERIES", label:"QUERIES/MIN", inc:1},
             {name:"STATUS.STATUS_COMMAND.COM_SELECT", label:"SELECT/MIN", inc:1},
             {name:"STATUS.STATUS_COMMAND.COM_INSERT", label:"INSERT/MIN", inc:1},
             {name:"STATUS.STATUS_COMMAND.COM_UPDATE", label:"UPDATE/MIN", inc:1},
             {name:"STATUS.STATUS_COMMAND.COM_DELETE", label:"DELETE/MIN", inc:1}
           ]
          },
          {domid:"chart_app_data", label:"Application Data", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_COMMON.BYTES_RECEIVED", label:"BYTES_RECEIVED/SEC", inc:1, avg:"sec"},
             {name:"STATUS.STATUS_COMMON.BYTES_SENT", label:"BYTES_SENT/SEC", inc:1, avg:"sec"}
           ]
          },
          {domid:"chart_sort", label:"Sorting Operations", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_COMMON.SORT_MERGE_PASSES", label:"SORT_MERGE_PASSES/MIN", inc:1},
             {name:"STATUS.STATUS_COMMON.SORT_SCAN", label:"SORT_SCAN/MIN", inc:1},
             {name:"STATUS.STATUS_COMMON.SORT_RANGE", label:"SORT_RANGE/MIN", inc:1}
           ]
          }
        ]
      });
      
      simpleCharts({
        topUrl:"metrics/get", dbGroup:mydomval("dbgroup"), dbHost:mydomval("host"), start_datetime:mydomval("begin_time"),end_datetime:mydomval("end_time"),
        messageCB:function(str, isError){reportStatus(isError, "common_msg", str);},
        statusCB:function(domid,ret){toggleChart(domid,ret==1?"block":"none");},
        charts:[          
          {domid:"chart_innodb_io", label:"InnoDB IO", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_INNODB.DATA_READ", label:"DATA_READ(Bytes/s)", inc:1, avg:"sec"},
             {name:"STATUS.STATUS_INNODB.DATA_WRITTEN", label:"DATA_WRITTEN(Bytes/s)", inc:1, avg:"sec"},
             {name:"STATUS.STATUS_INNODB.OS_LOG_WRITTEN", label:"OS_LOG_WRITTEN(Bytes/s)", inc:1, avg:"sec"}
           ]
          },
          {domid:"chart_row_ops", label:"InnoDB Row Operations", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_INNODB.ROWS_READ", label:"READ/SEC", inc:1, avg:"sec"},
             {name:"STATUS.STATUS_INNODB.ROWS_INSERTED", label:"INSERTED/SEC", inc:1, avg:"sec"},
             {name:"STATUS.STATUS_INNODB.ROWS_UPDATED", label:"UPDATED/SEC", inc:1, avg:"sec"},
             {name:"STATUS.STATUS_INNODB.ROWS_DELETED", label:"DELETED/SEC", inc:1, avg:"sec"}
           ]
          },
          {domid:"chart_locks", label:"InnoDB Lock Time", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_INNODB.ROW_LOCK_TIME", label:"ROW_LOCK_TIME(MSEC/SEC)", inc:1, avg:"sec"}
           ]
          },
          {domid:"chart_buffer_flush", label:"InnoDB Buffer Flushes", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_INNODB.BUFFER_POOL_PAGES_FLUSHED", label:"BUFFER_POOL_PAGES_FLUSHED/MIN", inc:1}
           ]
          },
          {domid:"chart_buffer", label:"InnoDB Buffer", width:w, height:h,
           metrics:[
             {name:"STATUS.STATUS_INNODB.BUFFER_POOL_PAGES_TOTAL", label:"PAGES_TOTAL", inc:0},
             {name:"STATUS.STATUS_INNODB.BUFFER_POOL_PAGES_DATA", label:"PAGES_DATA", inc:0},
           ]
          }
        ]
      });
      //document.getElementById("globalstatus_charts").style.display="block";    
    }//globalstatus
    else //snmp or os
    {
       showChart("snmp");

      simpleCharts({
        topUrl:"metrics/get", dbGroup:mydomval("dbgroup"), dbHost:mydomval("host"), start_datetime:mydomval("begin_time"),end_datetime:mydomval("end_time"),
		messageCB:function(str, isError){reportStatus(isError, "common_msg", str);},
        statusCB:function(domid,ret){toggleChart(domid,ret==1?"block":"none");},
        charts:[          
          {domid:"chart_cpu", label:"OS CPU", width:w, height:h,
           metrics:[
             {name:"SNMP._.SSCPURAWUSER", label:"User CPU(SEC/SEC)", inc:1, avg:"sec"},
             {name:"SNMP._.SSCPURAWSYSTEM", label:"Sys CPU(SEC/SEC)", inc:1, avg:"sec"},
             {name:"SNMP._.SSCPURAWWAIT", label:"IO Waits(SEC/SEC)", inc:1, avg:"sec"}
           ]
          },
          {domid:"chart_load_avg", label:"Load AVerage", width:w, height:h,
           metrics:[
             {name:"SNMP._.LALOAD5M", label:"LOAD AVERAGE", inc:0}
           ]
          },
          {domid:"chart_mem", label:"OS Memory", width:w, height:h,
           metrics:[
             {name:"SNMP._.MEMTOTALREAL", label:"TOTAL", inc:0, adj:1024},
             {name:"SNMP._.MEMBUFFER", label:"BUFFER", inc:0, adj:1024},
             {name:"SNMP._.MEMCACHED", label:"CACHED", inc:0, adj:1024},
             {name:"SNMP._.MEMAVAILREAL", label:"FREE", inc:0, adj:1024},
           ]
          },
          {domid:"chart_swap", label:"Swap", width:w, height:h,
           metrics:[
             {name:"SNMP._.SSRAWSWAPIN", label:"IN/SEC", inc:1, avg:"sec"},
             {name:"SNMP._.SSRAWSWAPOUT", label:"OUT/SEC", inc:1, avg:"sec"}
           ]
          },
          {domid:"chart_disk_io", label:"Disk Reads/Writes", width:w, height:h,
           metrics:[
             {name:"SNMP_DISK_SDA._.DISKIONREADX", label:"READS (Bytes/sec)", inc:1, avg:"sec"},
             {name:"SNMP_DISK_SDA._.DISKIONWRITTENX", label:"WRITES (Bytes/sec)", inc:1, avg:"sec"}
           ]
          },
          {domid:"chart_disk_iops", label:"Disk IOPS", width:w, height:h,
           metrics:[
             {name:"SNMP_DISK_SDA._.DISKIOREADS", label:"READS", inc:1, avg:"sec"},
             {name:"SNMP_DISK_SDA._.DISKIOWRITES", label:"WRITES", inc:1, avg:"sec"}
           ]
          }
        ]
      });
      //document.getElementById("snmp_charts").style.display="block";
    }//snmp or os
  }
  
  
  function retrieveNextUDMMetrics()
  {
    if(secMetricsIdx == MetricsList.length - 1)secMetricsIdx = 0;
  	else secMetricsIdx = secMetricsIdx + 1;
  	mydom("metrics2").value = MetricsList[secMetricsIdx].label;
  	  	  
  	retrieveUDMMetrics();
  }
  
  function retrieveUDMMetrics()
  {
    if(!checkDBSelection(mydomval('dbgroup'), mydomval('host')))
      return;

    var single = false;
    var m = "globalstatus", src = "db", w = 986, h = 300;
    resolveDates();
   
    if(mydomval("metrics1") == null 
      ||mydomval("metrics1") == "")
    {  
    	reportStatus(true, "common_msg", "Please select one metric to start");
    	mydom('metrics1').focus();
    	return;
	}
    if(document.getElementById("metrics2").value == null 
      ||document.getElementById("metrics2").value == "")
      single = true;

    var charts = null;
    if (single )
    {
      document.getElementById("div_chart1").style.display="block";
      document.getElementById("div_chart2").style.display="none";
	  document.getElementById("div_scatterplot").style.display="none";
      charts =   [{domid:"div_chart1", width:w, height:160, 
                    metrics:[{name:mydomval("metrics1")}]}
                 ];
    }
    else
    {
      document.getElementById("div_chart1").style.display="block";
      document.getElementById("div_chart2").style.display="block";
	  document.getElementById("div_scatterplot").style.display="block";
      charts =   [{domid:"div_chart1", width:w, height:160, 
                    metrics:[{name:mydomval("metrics1")}]},
                  {domid:"div_chart2", width:w, height:160, 
                    metrics:[{name:mydomval("metrics2")}]}
                 ];
    }

      simpleCharts({
        topUrl:"metrics/get", dbGroup:mydomval("dbgroup"), dbHost:mydomval("host"), start_datetime:mydomval("begin_time"),end_datetime:mydomval("end_time"),
        messageCB:function(str, isError){reportStatus(isError, "common_msg", str);},        
        statusCB:function(domid,ret){},
        charts:charts,
        scatterplotObj:{domid:"div_scatterplot", width:w, height:h, colors:["red"]}
      });
  }

  
  function retrieveNextHostMetricsMetrics()
  {
    //if(mydom("host_metrics").selectedIndex==mydom("host_metrics").options.length-1)
    //  mydom("host_metrics").selectedIndex = 0;
  	//else
  	//  mydom("host_metrics").selectedIndex = mydom("host_metrics").selectedIndex +1;
  	if(hostMetricsIdx == MetricsList.length - 1)hostMetricsIdx = 0;
  	else hostMetricsIdx = hostMetricsIdx + 1;
  	mydom("host_metrics").value = MetricsList[hostMetricsIdx].label;
  	retrieveHostMetricsMetrics();
  }
  //end of function

  function retrieveHostMetricsMetrics()
  {
    if(!checkDBSelection(mydomval('dbgroup'), mydomval('host')))
      return;

    var single = false;
    var w = 986;
    resolveDates();
    var dbgrp = mydomval("dbgroup");
    var dbhost = mydomval("host");
    var dbToCmp = mydomval("dbgroup2");    
    var dbgrp2 = null;
    var dbhost2 = null;
    if(dbToCmp != null && dbToCmp.indexOf("|")>0)
    {
      dbgrp2 = dbToCmp.substring(0, dbToCmp.indexOf("|"));
      dbhost2 = dbToCmp.substring(dbToCmp.indexOf("|")+1);
    }
    
    if(dbgrp == null || dbgrp == "" || dbhost == null || dbhost == "")
    {
       $("#common_msg").text("Please select ond database to start");
       mydom("dbgroup").focus();
       return;
    }
    
    if(mydomval("host_metrics") == null || mydomval("host_metrics") == "")
    {
       $("#common_msg").text("Please select one metrics to start");
       mydom("host_metrics").focus();
       return;
    }
    
	showHideOne("host_metrics_charts", "block");
	
    if(dbgrp2==null||dbgrp2==""||dbhost2==null||dbhost2=="")
    {
      //when no comparison is selected, we will use the ones from host list, up to 20
      var startIdx = mydom("host").selectedIndex;
      var hostLength = mydom("host").options.length;
      var endIdx = <%= HOST_GROUP_COUNT %> + startIdx;
      if(hostLength <= <%= HOST_GROUP_COUNT %>)
      {
        startIdx = 0;
        endIdx = hostLength;
      }else if(hostLength - startIdx < <%= HOST_GROUP_COUNT %>)
      {
      	startIdx = hostLength - <%= HOST_GROUP_COUNT %>;
      	endIdx = hostLength;
      }
      for(var ci=1; ci<= <%= HOST_GROUP_COUNT %>; ci++)
      {
      	if (ci <= endIdx - startIdx)
      	{
      	  //draw
      	  //div_host_chart_label_
      	  dbhost2 = mydom("host").options[startIdx + ci -1].value;
      	  $("#div_host_chart_label"+ci).text( dbhost2);
      	  showHideOne("title_div_host_chart"+ci, "block");
 	      plotSingleWithDB("div_host_chart"+ci, dbgrp, dbhost2, "host_metrics", w, 160);      	  
          //showHideOne("div_host_chart"+ci, "block");
		  //toggleChart("div_host_chart"+ci,"block");
      	}
      	else  //hide
      	{
      	  showHideOne("title_div_host_chart"+ci, "none");      		
		  toggleChart("div_host_chart"+ci,"none");
      	}
      }
    }else
    {
      $("#div_host_chart_label1").text(dbhost);
      showHideOne("title_div_host_chart1", "block");
 	  plotSingleWithDB("div_host_chart1", dbgrp, dbhost, "host_metrics", w, 160);
      $("#div_host_chart_label2").text( dbhost2);
	  //toggleChart("div_host_chart1","block");
      showHideOne("title_div_host_chart2", "block");
 	  plotSingleWithDB("div_host_chart2", dbgrp2, dbhost2, "host_metrics", w, 160);
	  //toggleChart("div_host_chart2","block");

 	  for(var ci=3;ci<=<%= HOST_GROUP_COUNT %>; ci++)
 	  {
 	      showHideOne("title_div_host_chart"+ci, "none");      		
   	      toggleChart("div_host_chart"+ci,"none");
 	  }
 	}
  }
  //end of function

  function plotSingleWithDB(div, dbgrp, dbhost, srcDiv, w, h)
  {
	  simpleCharts({
            topUrl:"metrics/get", dbGroup:dbgrp, dbHost:dbhost, start_datetime:mydomval("begin_time"),end_datetime:mydomval("end_time"),
            messageCB:function(str, isError){reportStatus(isError, "common_msg", str);},
            statusCB:function(domid,ret){toggleChart(domid,ret==1?"block":"none");},
            charts:[
              {domid:div, width:w, height:h,
               metrics:[
                 {name:mydomval(srcDiv)}
               ]
              }
            ]
      });
  }

  function resolveDates()
   {
   		mydom("begin_time").value = mydomval("begin_time_d") + " " + mydomval("begin_time_t")+":00:00";
   		mydom("end_time").value = mydomval("end_time_d") + " " + mydomval("end_time_t")+":00:00";
   }
  
  $('#btn_udm_activity_next_auto').click(function()
  {
    triggerAutoMetricsCompare();
  });  

  //btn_host_metrics_activity_next_auto
  $('#btn_host_metrics_activity_next_auto').click(function()
  {
    triggerAutoHostMetricsCompare();
  });
    
  function triggerAutoMetricsCompare()
  {
   	  if(mydom("btn_udm_activity_next_auto").checked)
   	  {
   	  	retrieveNextUDMMetrics();
   	  	setTimeout(triggerAutoMetricsCompare, 3000);
   	  }
  }

  function triggerAutoHostMetricsCompare()
  {
   	  if(mydom("btn_host_metrics_activity_next_auto").checked)
   	  {
   	  	retrieveNextHostMetricsMetrics();
   	  	setTimeout(triggerAutoHostMetricsCompare, 3000);
   	  }
  }
</script>
</body>
</html>