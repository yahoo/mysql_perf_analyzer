<%@page trimDirectiveWhitespaces="true"%>
<%@page import="com.yahoo.dba.perf.myperf.common.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>User Defined Metrics</title>
<jsp:include page="commheader.jsp" flush="true" />
<script type="text/javascript" src="js/common.js"></script> 
<style>
 	.cssError {color:red;font-size:10px;}
 	.cssOk {color:blue;font-size:10px;}
 	.selected {background-color:lightblue;color:#47a;text-decoration:underline;}
 	.unselected {background-color:white;color:#47a;text-decoration:none;}
 	#udm_detail td{white-space: nowrap; border: 0px;}
 	#udm_detail th{white-space: nowrap; border: 0px;}
 	#udm_detail textarea{width: 400px;}
 	#alert_detail td{white-space: nowrap; border: 0px;}
 	#alert_detail th{white-space: nowrap; border: 0px;}
 	#alert_detail textarea{width: 400px;}
 	#udm_tabs {background: transparent; border:none;}
 	#udm_tabs .ui-widget-header {background: transparent; border:none;border-bottom: 1px solid #c0c0c0; 
 	          -moz-border-radius: 0px;-webkit-border-radius: 0px; border-radius: 0ps;}
 	#udm_tabs .ui-tabs-nav .ui-state-default {background: transparent; border:none;}
 	#udm_tabs .ui-tabs-nav .ui-state-active {background: transparent url(img/ui_active_tab.png) no-repeat bottom center; border:none;}
 	#udm_tabs .ui-tabs-nav .ui-state-default a {color: #c0c0c0; outline:none;}
 	#udm_tabs .ui-tabs-nav .ui-state-active a {color: #47a; outline:none;}
 	
 	#subscription_tab {background: transparent; border:none;}
 	
 	.udm_subscription{}
 	.alert_subscription{}
 	.predefined_subscription{}
 	.mhead{font-weight:bold;color:#47a;}
 </style>
 <script language="javascript">
   var DBList=[""
    <c:forEach var="cluster" items="${mydbs}">
       <c:forEach var="db" items="${dbMap[cluster].instances}">,"${cluster}|${db.hostName}"</c:forEach>
    </c:forEach>  
  ];
 </script>
</head>
<body>
<span id="common_msg"></span>
<div style="margin-left:48px;width:800px;" id="udm_tabs">
  <ul>
     <li><a href="#udm" title="Add/View UDMs">UDM</a></li> 
     <li><a href="#alerts" title="Add/View Alerts">Alerts</a></li> 
     <li><a href="#udmdb" title="Assign UDMs to database servers">Subscribers</a></li>
  </ul>
  <div id="udm" style="display:block;">
    <div id="udm_list_div" style="width:200px;float:left;">
 <c:if test="${u.adminUser}">
      <div style="color:#47a;cursor:pointer;" onclick="addNewUDM();"> + New UDM</div>
      <br />
 </c:if>
      <select id="udm_list" onchange="selectUDM();">
        <option value="">---Available UDM---</option>
        <c:forEach var="uname" items="${udms}" varStatus="stat">
          <option value="${uname}">${uname}</option>   
        </c:forEach>
      </select>                  
    </div>
    <div id="udm_detail" style="width:500px;display:${udm.name==null?"none":"block"};float:left;padding-left:20px;">
      <table width="460">
        <tr><td><label for="udm_name">Name</label></td><td><input type="text" name="udm_name" id="udm_name"  required title="Alphanumeric and underscore _ only" pattern="[A-Za-z0-9_]+" max_length=20/></td></tr>
        <!-- tr><td><label for="udm_auto">Auto Collect?</label></td><td><input type="checkbox" name="udm_auto" id="udm_auto"  title="If checked, all database servers will gather this group of metrics automatically. Otherwise, need manually assign it to any database server." /></td></tr -->
        <tr><td><label for="udm_storage">Metrics Storage: </label></td>
            <td><select name="udm_storage" id="udm_storage">
                  <option value="NEW" selected>Create A New Table</option>
                  <option value="SHARED">Store In The Shared Table</option>
                 </select>
            </td>
        </tr>
        <tr><td><label for="udm_source">Data Source: </label></td>
            <td><select name="udm_source" id="udm_source"  title="select from the predefined data source or provide customized SQL">
                  <option value="STATUS">Global Status</option>
                  <option value="SQL" selected>Customized SQL</option>
                 </select>
            </td>
        </tr>
        <tr id="tr_udm_type"><td><label for="udm_type">Data Type</label></td>
            <td><select name="udm_type" id="udm_type" title="How metrics will be extracted?">
                  <option value="column" selected>Single Row, One Column One Metric</option>
                  <option value="row">Multiple Rows, One Row One Metric, Key Value Pairs</option>
                  <option value="key">Multiple Rows, Multiple Metrics Per Row, With a Key Column</option>
               </select>
        </td></tr>
        <tr id="tr_udm_name_column"><td><label for="udm_name_column">Name Column</label></td><td><input type="text" name="udm_name_column" id="udm_name_column" size="20"
              title="For multi row key value pairs, the SQL result set column name to be used to extract and match the metrics names." /></td></tr>
        <tr id="tr_udm_value_column"><td><label for="udm_value_column">Value Column</label></td><td><input type="text" name="udm_value_column" id="udm_value_column" size="20"
              title="For row based, the SQL result set column name to be used to extract metric values." /></td></tr>
        <tr id="tr_udm_key_column"><td><label for="udm_key_column">Key Column</label></td><td><input type="text" name="udm_key_column" id="udm_key_column" size="20"
              title="For case of multi rows with multi metrics per row, a entity key column is required"/></td>
        </tr>
        <tr id="tr_udm_sql"><td><label for="udm_sql">SQL</label></td>
             <td><textarea name="udm_sql" id="udm_sql" rows="10" cols="70" required /></textarea></td>
        </tr>
        <tr><td>Test Database: </td>
            <td><input type="text" id="udm_test_db" name="udm_test_db" placeholder="Pick a database for test"" /></td>
        </tr>
        <tr><td colspan="2">Metrics Mappings</td></tr>
        <tr><td colspan="2">
          Note: Source Name is either the column name or the key value (for example, the value of variable_name in global status).  
          <table width="460" id="udm_metrics_mapping">
            <tr><th>Metric Name</th><th>Source Name</th><th>Incremental?</th><th>Data Type</th></tr>
 <% for (int i=1; i<=10; i++) {%>           
            <tr><td><input type="text" id="udm_m_name_<%=i%>" name="udm_m_name_<%=i%>" /></td>
                <td><input type="text" id="udm_m_col_<%=i%>" name="udm_m_col_<%=i%>" /></td>
                <td><input type="checkbox" id="udm_m_inc_<%=i%>" name="udm_m_inc_<%=i%>" /></td>
                <td><select id="udm_m_data_<%=i%>" name="udm_m_data_<%=i%>">
                      <option value="byte">Byte</option>
                      <option value="short">Short</option>
                      <option value="int">Int</option>
                      <option value="long">Long</option>
                      <option value="float">Float</option>
                      <option value="double">Double</option>
                    </select>
                </td>
            </tr>
<% } %>            
          </table>
 <c:if test="${u.adminUser}">       
          <input type="button" id="btn_udm_add_rows" value="Add Rows" onclick="addMetricsRows();"/>
 </c:if>
        </td>
        </tr>
 <c:if test="${u.adminUser}">
 		<tr>
            <td colspan="2"><input type="button" name="udm_publish" id="udm_publish" value='Test And Publish' onclick="handleUDMAction('publish');"/></td>
        </tr>
 </c:if>
      </table>
    </div>    
  </div><!-- end of udm -->
  
  <div id="alerts"><!-- user defined alerts -->
    <div id="alerts_list_div" style="width:200px;float:left;">
 <c:if test="${u.adminUser}">
      <div style="color:#47a;cursor:pointer;" onclick="addNewAlert();"> + New Alert</div>
      <br />
 </c:if>
      <select id="alert_list" onchange="selectAlert();">
        <option value="">---Available Alert Definitions---</option>
        <c:forEach var="uname" items="${alerts}" varStatus="stat">
          <option value="${uname}">${uname}</option>   
        </c:forEach>
      </select>                  
    </div>
    <div id="alert_detail" style="width:500px;display:${udm.name==null?"none":"block"};float:left;padding-left:20px;">
      <table width="460" id="alert_detail_tbl">
        <tr>
            <td><label for="alert_name" width='20%'>Name</label></td>
            <td><input type="text" name="alert_name" id="alert_name"  required title="Alphanumeric and underscore _ only" pattern="[A-Za-z0-9_]+" max_length=30/></td>
        </tr>
		<tr>
			<td><label for="alert_source">Data Source: </label></td>
            <td><select name="alert_source" id="alert_source"  title="select from the predefined metrics data source or provide customized SQL">
                  <option value="SQL" selected>Customized SQL</option>
                  <option value="GLOBAL_STATUS">GLOBAL_STATUS</option>
                  <option value="METRICS">Metrics</option>
                 </select>
            </td>
        </tr>
        <tr id="tr_alert_sql"><td><label for="alert_sql">SQL</label></td>
             <td>
               <span>Prefix &amp; for parameter place holder and quote text parameters. Up to 5 parameters.</span><br />
               <textarea name="alert_sql" id="alert_sql" rows="10" cols="70" /></textarea>
             </td>
        </tr>
        <tr id="tr_alert_metrics_name" style="display:none;"><td><label for="alert_metrics_name">Metric Name</label></td>
             <td><input type="text" name="alert_metrics_name" id="alert_metrics_name" /></td>
        </tr>
        <tr id="tr_alert_metrics_comparison" style="display:none;"><td><label for="alert_metrics_comparison">Metric Comparison</label></td>
             <td><select name="alert_metrics_comparison" id="alert_metrics_comparison">
                   <option value="GT">Alert If Exceed Threshold</option>
                   <option value="LT">Alert If Below Threshold</option>
                 </select>
             </td>
        </tr>
        <tr id="tr_alert_metrics_value_type" style="display:none;"><td><label for="alert_metrics_value_type">Metric Value Type</label></td>
             <td><select name="alert_metrics_value_type" id="alert_metrics_value_type">
                   <option value="LAST_VALUE">Last Value</option>
                   <option value="DIFF">Changes</option>
                   <option value="DIFF_AVG">Changes Per Second</option>
                 </select>
             </td>
        </tr>
        <tr id="tr_alert_metrics_threshold" style="display:none;"><td><label for="alert_metrics_threshold">Metric Threshold</label></td>
             <td><input type="text" name="alert_metrics_threshold" id="alert_metrics_threshold" /></td>
        </tr>
        
        <tr id="tr_alert_test_database"><td>Test Database: </td>
            <td><input type="text" id="alert_test_db" name="alert_test_db" placeholder="Pick a database for test"" /></td>
        </tr>
        <tr id="tr_alert_sql_params_head"><td colspan="2">SQL Parameters</td></tr>
        <tr id="tr_alert_sql_params"><td colspan="2"> 
          <table width="460" id="alert_sql_parameters">
            <tr><th>Parameter Name</th><th>Default Value</th></tr>
 <% for (int i=1; i<=5; i++) {%>           
            <tr><td><input type="text" id="alert_p_name_<%=i%>" name="alert_p_name_<%=i%>" /></td>
                <td><input type="text" id="alert_p_value_<%=i%>" name="alert_p_value_<%=i%>" /></td>
            </tr>
<% } %>            
          </table>
        </td>
        </tr>
 <c:if test="${u.adminUser}">
 		<tr>
            <td colspan="2"><input type="button" name="alert_publish" id="alert_publish" value='Test And Publish' onclick="handleAlertAction('publish_alert');"/></td>
        </tr>
 </c:if>
      </table>
    </div><!-- detail table -->     
  </div><!-- user defined alerts -->
  
  <div id="udmdb">
    <div id="udmdb_list" style="margin-bottom:10px;">
      <span class="mhead">Server Group:&nbsp;</span>
      <select id="list_udmdb" style="margin-top:10px;">
        <option value="">--- select database server---</option>
          <c:forEach var="cluster" items="${mydbs}" varStatus="stat">
            <option value="${cluster}">${cluster}</option>
  	      </c:forEach>
  	  </select>
  	  &nbsp;Host:&nbsp;
  	  <select id="list_udmdb_host">
          <option value="">select host name</option>
  	  </select>
  	  <br />
  	  <span>Check/uncheck to enable/disable metrics/alerts for the selected server group or host.</span> 
    </div><!-- db host list -->
    <div id="subscription_tab"><!-- UDM, METRICS, ALERT subscriptions-->
      <ul>
        <li><a href="#udmdb_predefined_detail">Predfined Metrics</a></li>
        <li><a href="#udmdb_detail">UDMs</a></li>
        <li><a href="#udmdb_alerts_detail">Alerts</a></li>
      </ul>
      <div id="udmdb_predefined_detail"><!-- Predefined metrics  list -->
        <div class="mhead">Available Predefined Metrics</div>
        <c:forEach var="udm" items="${predefined}">
    	  <div><input type="checkbox" id="dbudm_predefined_${udm.key}" name="dbudm_predefined_${udm.key}" 
    	    value="${udm.key}" class="predefined_subscription" ${udm.value=="y"?"checked disabled":""} auto="${udm.value}"/> ${udm.key}</div>
        </c:forEach>
      </div><!-- end of Predefined metrics  list -->
      <div id="udmdb_detail"><!-- UDM list -->
        <div class="mhead">Available UDMs</div>
        <c:forEach var="udm" items="${udms}" varStatus="stat">
    	  <div><input type="checkbox" id="dbudm_udm_${udm}" name="dbudm_udm_${udm}" value="${udm}" class="udm_subscription"/> ${udm}</div>
        </c:forEach>
      </div><!-- end of UDM list -->
      <div id="udmdb_alerts_detail"><!-- alerts  list -->
        <div class="mhead">Available Alerts</div>
        <c:forEach var="udm" items="${alerts}" varStatus="stat">
    	  <div><input type="checkbox" id="dbudm_alert_${udm}" name="dbudm_alert_${udm}" value="${udm}" class="alert_subscription"/> ${udm}</div>
        </c:forEach>
      </div><!-- end of alerts list -->
   </div><!-- UDM, METRICS, ALERT subscriptions-->   
  </div><!-- udmdb -->
</div>
<script language="javascript">
var udms = new Array();
      <c:forEach var="udm" items="${udms}" varStatus="stat">
    	udms[${stat.index}] = "${udm}";
      </c:forEach>

$('#list_udmdb').change(function()
  {
    query_hostlist_main(mydomval('list_udmdb'), 'list_udmdb_host', true, selectHostInit);
  }
);

function selectHostInit()
{
  selectHost("predefined");
}

$('#list_udmdb_host').on("change", function()
{
  selectHostInit();
});

$("#udm_test_db").autocomplete({source: DBList, minLength:0}) 
                    .bind('focus', function(){$(this).autocomplete("search");});  
$("#alert_test_db").autocomplete({source: DBList, minLength:0}) 
                    .bind('focus', function(){$(this).autocomplete("search");});  

 $("#udm_tabs").tabs();
 
 $('#subscription_tab').tabs(
 {
   activate: function( event, ui ) 
   {
     var idx = $('#subscription_tab').tabs("option", "active");
     if(idx == 0)
       selectHost('predefined');
     else if(idx == 1)
       selectHost('udm');
     else
       selectHost('alert');          
    }
  }  
 );
   
   var DATA_TYPE_MAPPING={"BYTE":0, "SHORT":1, "INT":2, "LONG":3, "FLOAT":4, "DOUBLE":5};
   var UDM_TYPE_MAPPING={"column":0, "row":1, "key":2};
      
   var metricsTableRows = 10;
   function addMetricsRows()
   {
     var mtbl = mydom("udm_metrics_mapping");
     var row = mtbl.insertRow();
     var idx = metricsTableRows + 1;     
     var cell1 = row.insertCell(0);
     var cell2 = row.insertCell(1);
     var cell3 = row.insertCell(2);
     var cell4 = row.insertCell(3);
     cell1.innerHTML = '<input type="text" id="udm_m_name_' + idx + '" name="udm_m_name_' + idx + '" />';
     cell2.innerHTML = '<input type="text" id="udm_m_col_' + idx + '" name="udm_m_col_' + idx + '" />';
     cell3.innerHTML = '<input type="checkbox" id="udm_m_inc_' + idx + '" name="udm_m_inc_' + idx + '" />';
     cell4.innerHTML = '<select id="udm_m_data_' + idx + '" name="udm_m_data_' + idx + '">'
                      + '<option value="byte">Byte</option>'
                      + '<option value="short">Short</option>'
                      + '<option value="int">Int</option>'
                      + '<option value="long">Long</option>'
                      + '<option value="float">Float</option>'
                      + '<option value="double">Double</option>'
                      + '</select>';
      metricsTableRows = metricsTableRows + 1;                
   }
   
   function handleUdmSourceOption(e)
   {
   	var udm_source = mydomval('udm_source');
   	var udm_type = mydomval('udm_type');
   	showByUDMSourceOption(udm_source, udm_type);
   }
   
   function showByUDMSourceOption(udm_source, udm_type)
   {
   	var showme = udm_source == 'SQL'? 'table-row': 'none';
   	var showme2 = udm_source == 'SQL' && udm_type == 'row'? 'table-row': 'none';
   	var showme3 = udm_source == 'SQL' && udm_type == 'key'? 'table-row': 'none';
   	mydom('tr_udm_type').style.display = showme;
   	mydom('tr_udm_name_column').style.display = showme2;
   	mydom('tr_udm_value_column').style.display = showme2;
   	mydom('tr_udm_key_column').style.display = showme3;
   	mydom('tr_udm_sql').style.display = showme;   	   
   }
   
   $('#udm_source').bind('change', handleUdmSourceOption);
   
   function handleUdmTypeOption(e)
   {
   	showByUDMTypeOption( mydomval('udm_type'));
   	
   }
   
   function showByUDMTypeOption(udm_type)
   {
   	var showme2 = udm_type == 'row'? 'table-row': 'none';
   	var showme3 = udm_type == 'key'? 'table-row': 'none';
   	mydom('tr_udm_name_column').style.display = showme2;
   	mydom('tr_udm_value_column').style.display = showme2;
   	mydom('tr_udm_key_column').style.display = showme3;   
   }
   
   $('#udm_type').bind('change', handleUdmTypeOption);
   
   function showUDM()
   {
     document.getElementById("udm").style.display="block";
     document.getElementById("label_udm").className="selected";
     document.getElementById("udmdb").style.display="none";
     document.getElementById("label_udmdb").className="unselected";
     
   }
   function showDB()
   {
     document.getElementById("udm").style.display="none";
     document.getElementById("label_udm").className="unselected";
     document.getElementById("udmdb").style.display="block";
     document.getElementById("label_udmdb").className="selected";
   }
   
   function selectUDM()
   {
     var udm = mydomval('udm_list');
     if(udm == '')
     {
       mydom("udm_detail").style.display="none";
       return;
     }
     mydom("udm_detail").style.display="block";
     var mydata = "cmd=udm_detail&name="+udm
              +"&seed="+Math.random();

     clearUDM();
     $.ajax({
       url: "udm.html",
       data: mydata,
       dataType: 'json',
       success: function(json)
       {
         if(json==null || json.resp==null || json.resp.udm == null)
         {
           alert("UDM "+udm+" Not Found");
           return;
         }
         var res = json.resp.udm;
         mydom("udm_name").value= res.groupName;
         mydom("udm_name").readOnly= true;
         mydom("udm_key_column").value= res.keyColumn != null?res.keyColumn:"";
         mydom("udm_name_column").value= res.nameColumn!=null?res.nameColumn:"";
         mydom("udm_value_column").value= res.valueColumn!=null?res.valueColumn:"";
         mydom("udm_sql").value= res.sql!=null?res.sql:"";
         setSelect("udm_storage", (res.storeInCommonTable == "y")?"SHARED":"NEW");
         setSelect("udm_source", res.source);
         
         var typ = mydom("udm_type");
         var typval = res["type"];
         if(typval != null)typ.selectedIndex = UDM_TYPE_MAPPING[typval];
         
         //metrics
         var ms = res.metrics;
         var cnt = ms!=null?ms.length:0;
         //remove unwanted rows
         var mtbl = mydom("udm_metrics_mapping");
         if(cnt <= 10 && metricsTableRows >10)
         {
			for(var i=metricsTableRows; i>=10; i--)
			  mtbl.deleteRow(-1);
			metricsTableRows = 10;  
         }else if(cnt > metricsTableRows)
         {
           for(var i=metricsTableRows+1; i<=cnt; i++)           
	           addMetricsRows();
	       metricsTableRows = cnt;    
         }
         for(var i=1; i<= metricsTableRows; i++)
         {
         	if(i<=cnt)
         	{
         	  var m = ms[i - 1];
         	  mydom("udm_m_name_"+i).value = m.name;
         	  mydom("udm_m_col_"+i).value = m.sourceName;
         	  mydom("udm_m_inc_"+i).checked = (m.inc != null && m.inc == 'y')?true:false;
         	  mydom("udm_m_data_"+i).selectedIndex = DATA_TYPE_MAPPING[m.dataType];
         	}else
         	{
         	  mydom("udm_m_name_"+i).value = "";
         	  mydom("udm_m_col_"+i).value = "";
         	  mydom("udm_m_inc_"+i).checked = false;
         	  mydom("udm_m_data_"+i).selectedIndex = 0;
         	}
         
         }
         showByUDMTypeOption(res["type"]);
         showByUDMSourceOption(res.source, res["type"]);
         mydom("btn_udm_add_rows").disabled = true;
         mydom("udm_publish").disabled = true;
         
       }//success    
     });
   }

   function selectAlert()
   {
     var udm = mydomval('alert_list');
     if(udm == '')
     {
       mydom("alert_detail").style.display="none";
       return;
     }
	 clearAlert();
     mydom("alert_detail").style.display="block";
     var mydata = "cmd=alert_detail&name="+udm
              +"&seed="+Math.random();

     $.ajax({
       url: "udm.html",
       data: mydata,
       dataType: 'json',
       success: function(json)
       {
         if(json==null || json.resp==null || json.resp.alert == null)
         {
           alert("ALERT "+udm+" Not Found");
           return;
         }
         var res = json.resp.alert;
         mydom("alert_name").value= res.name;
         mydom("alert_name").readOnly= true;
         mydom("alert_metrics_name").value= res.metricName != null?res.metricName:"";
         mydom("alert_metrics_threshold").value= res.defaultThreshold!=null?res.defaultThreshold:"";
         mydom("alert_sql").value= res.sqlText!=null?res.sqlText:"";
         var src = mydom("alert_source");
         var srcval = res.source;
         if(srcval == "SQL")
           src.selectedIndex = 0;
         else if(srcval == "GLOBAL_STATUS")
           src.selectedIndex = 1;
         else
           src.selectedIndex = 2;
         
         showAlertComponentsBySource(srcval);
               
         var comp = mydom("alert_metrics_comparison");
         srcval = res.metricComparison;
         comp.selectedIndex = srcval=="GT"?0:1; 
         var vtype = mydom("alert_metrics_value_type");
         srcval = res.metricValueType;
         if(srcval == 'LAST_VALUE')
           vtype.selectedIndex = 0;
         else if(srcval == 'DIFF')
           vtype.selectedIndex = 1;
         else
           vtype.selectedIndex = 2;
         
         //params
         var params = res.params;
         var cnt = params!=null?params.length:0;
         //remove unwanted rows
         for(var i=1; i<= cnt; i++)
         {
         	  var m = params[i - 1];
         	  mydom("alert_p_name_"+i).value = m.name;
         	  mydom("alert_p_value_"+i).value = m.defaultValue;
         }
       }    
     });
   }
   
   /**
    * typ: udm, alert, predefined
   */
   function selectHost(typ)
   {
	   var group = mydomval("list_udmdb");
	   var host = mydomval("list_udmdb_host");
       var mydata = "cmd=udmdb_detail&typ=" + typ + "&group="+group;
       if(host != null && host != "")
           mydata += "&host="+host;
       mydata += "&seed="+Math.random();
       
         $.ajax({
           url: "udm.html",
           data: mydata,
           dataType: 'json',
           success: function(json)
           {
             //reset first
             $("."+typ+"_subscription").each(function(i, obj)
             {
                 if(! $(this).is(":disabled"))
                   $(this).prop("checked", false);
             });
             if(json==null||json.resp==null
               ||json.resp.results==null||json.resp.results.results==null||json.resp.results.results<1)
             {
               return;
             }
             var res = json.resp.results.results;
             var obj = new Object();
             var i = 0;
             //reset first
             for(i=0;i<res.length;i++)
             {
               var udm = res[i]["UDM"];
               $("#dbudm_"+typ+"_"+udm).prop("checked", true);
             }
           }//success    
       });//ajax
   }//selectHost


function showAlertComponentsBySource(source)
{
  var isSQL = "SQL" == source;
  mydom("tr_alert_sql").style.display= isSQL?"table-row":"none";
  mydom("tr_alert_test_database").style.display= isSQL?"table-row":"none";
  mydom("tr_alert_sql_params").style.display= isSQL?"table-row":"none";
  mydom("tr_alert_sql_params_head").style.display= isSQL?"table-row":"none";  
  mydom("tr_alert_metrics_name").style.display= isSQL?"none":"table-row";
  mydom("tr_alert_metrics_comparison").style.display= isSQL?"none":"table-row";
  mydom("tr_alert_metrics_value_type").style.display= isSQL?"none":"table-row";
  mydom("tr_alert_metrics_threshold").style.display= isSQL?"none":"table-row";
  
}//showAlertComponentsBySource 
  
<c:if test="${u.adminUser}">
$("#alert_source").on("change", function()
{
  showAlertComponentsBySource( mydomval("alert_source"));
});
   function addNewUDM()
   {
     clearUDM();
     mydom("btn_udm_add_rows").disabled = false;
     mydom("udm_publish").disabled = false;
     document.getElementById("udm_detail").style.display="block";
   }
   
   function clearUDM()
   {
     mydom('udm_name').value="";
     mydom('udm_name').readOnly = false;
     mydom('udm_name_column').value="";
     mydom('udm_value_column').value="";
     mydom('udm_key_column').value="";
     $('#udm_sql').text("");
     var udm_metrics_mapping = [];
     for(var i=1; i<=metricsTableRows; i++)
     {
       mydom('udm_m_name_'+i).value="";
       mydom('udm_m_col_'+i).value="";
       mydom('udm_m_inc_'+i).checked = false; 
     }   
   }
   
   function clearAlert()
   {
     mydom('alert_name').value="";
     mydom('alert_name').readOnly = false;
     $('#alert_sql').text("");
     mydom('alert_metrics_name').value="";
     mydom('alert_metrics_threshold').value="";
     for(var i=1; i<=5; i++)
     {
       mydom('alert_p_name_'+i).value="";
       mydom('alert_p_value_'+i).value="";
       
     }
   }
   
   //addNewAlert()
   function addNewAlert()
   {
     clearAlert();
     document.getElementById("alert_detail").style.display="block";
   }
   function handleUDMAction(act)
   {
     var udm_name = mydomval('udm_name');
     //var udm_auto = mydom('udm_auto').checked?'y':'n';
     var udm_auto = 'n';
     var udm_source = mydomval('udm_source');
     var udm_storage = mydomval('udm_storage');
     var udm_type = mydomval('udm_type');
     var udm_name_column = mydomval('udm_name_column');
     var udm_value_column = mydomval('udm_value_column');
     var udm_key_column = mydomval('udm_key_column');
     var udm_sql = mydomval('udm_sql');
     var udm_test_db = mydomval('udm_test_db');
     var udm_metrics_mapping = [];
     for(var i=1; i<=metricsTableRows; i++)
     {
       var m = new Object();
       m.name = mydomval('udm_m_name_'+i);
       m.column = mydomval('udm_m_col_'+i);
       m.inc = mydom('udm_m_inc_'+i).checked?'y':'n';
       m.datatype = mydomval('udm_m_data_'+i);
       if (m.name != null && m.name != '' && m.column != null && m.column != '')
         udm_metrics_mapping[udm_metrics_mapping.length] = m;       
     }   
     
     //TODO validation here
     
     var mydata = 'cmd='+act;
     mydata += '&name=' + escape(udm_name);
     mydata += '&auto=' + escape(udm_auto);
     mydata += '&storage=' + escape(udm_storage);
     mydata += '&source=' + escape(udm_source);
     mydata += '&type=' + escape(udm_type);
     mydata += '&namecol=' + escape(udm_name_column);
     mydata += '&valcol=' + escape(udm_value_column);
     mydata += '&keycol=' + escape(udm_key_column);
     mydata += '&sql=' + escape(udm_sql);
     mydata += '&testdb=' + escape(udm_test_db);
     mydata += '&num='+udm_metrics_mapping.length;
     for(var i=0; i<udm_metrics_mapping.length; i++)
     {
       mydata += '&mname_'+i+'='+udm_metrics_mapping[i].name;
       mydata += '&mcol_'+i+'='+udm_metrics_mapping[i].column;
       mydata += '&minc_'+i+'='+udm_metrics_mapping[i].inc;
       mydata += '&mdata_'+i+'='+udm_metrics_mapping[i].datatype;
     }
     $.ajax({
       url: "udm.html",
       data: mydata,
       type: 'POST',
       dataType: 'json',
       success: function(json)
       {
         console.log(json);
         if(json != null && json.resp != null && json.resp.status == 0)
         {
			var msg = "UDM " + udm_name + " has been added successfully.";
			mydom("udm_list").options[mydom("udm_list").options.length] = new Option(udm_name, udm_name);
			$("#udmdb_detail").append('<div><input type="checkbox" id="dbudm_udm_'+udm_name+'" name="dbudm_udm_'+udm_name+'" value="'+udm_name+'" class="udm_subscription"/>'+udm_name+'</div>');
		    $("#dbudm_udm_"+udm_name).on("change", function()
	          {
		       var udm = $(this).attr("value");
       		   var checked = $(this).is(":checked");
		       handleUdmDbUpdate("udmdb_update", udm, checked);
     		});
			reportStatus(false, "common_msg", msg);
         }else if(json != null && json.resp != null && json.resp.status == -1)
         {
           reportStatus(true, "common_msg", json.resp.message);
         }
       }
     });  
     
   }
   function handleAlertAction(act)
   {
     var alert_name = mydomval('alert_name');
     var alert_source = mydomval('alert_source');
     var alert_metrics_name = mydomval('alert_metrics_name');
     var alert_metrics_comparison = mydomval('alert_metrics_comparison');
     var alert_metrics_value_type = mydomval('alert_metrics_value_type');
     var alert_metrics_threshold = mydomval('alert_metrics_threshold');
     var alert_sql = mydomval('alert_sql');
     var alert_test_db = mydomval('alert_test_db');
     var alert_params = [];
     for(var i=1; i<= 5; i++)
     {
       var m = new Object();
       m.name = mydomval('alert_p_name_'+i);
       m.value = mydomval('alert_p_value_'+i);
       if (m.name != null && m.name != '')
         alert_params[alert_params.length] = m;       
     }   
     
     var mydata = 'cmd='+act;
     mydata += '&name=' + escape(alert_name);
     mydata += '&source=' + escape(alert_source);
     mydata += '&mname=' + escape(alert_metrics_name);
     mydata += '&mcomp=' + escape(alert_metrics_comparison);
     mydata += '&mval=' + escape(alert_metrics_value_type);
     mydata += '&mthreshold=' + escape(alert_metrics_threshold);
     mydata += '&sqlText=' + escape(alert_sql);
     mydata += '&testdb=' + escape(alert_test_db);
     mydata += '&num='+alert_params.length;
     for(var i=0; i<alert_params.length; i++)
     {
       mydata += '&pname_'+i+'='+alert_params[i].name;
       mydata += '&pval_'+i+'='+alert_params[i].value;
     }
     $.ajax({
       url: "udm.html",
       data: mydata,
       type: 'POST',
       dataType: 'json',
       success: function(json)
       {
         console.log(json);
         if(json != null && json.resp != null && json.resp.status == 0)
         {
			var msg = "Alert " + alert_name + " has been added successfully.";
			if(mydom('dbudm_alert_'+alert_name) == null)
			{
  			  mydom("alert_list").options[mydom("alert_list").options.length] = new Option(alert_name, alert_name);
			  $("#udmdb_alerts_detail").append('<div><input type="checkbox" id="dbudm_alert_'+alert_name+'" name="dbudm_alert_'+alert_name+'" value="'+alert_name+'" class="alert_subscription"/>'+alert_name+'</div>');
		      $("#dbudm_alert_"+alert_name).on("change", function()
	            {
		          var udm = $(this).attr("value");
       		      var checked = $(this).is(":checked");
		          handleUdmDbUpdate("alertdb_update", udm, checked);
     		  });
     		}
     		reportStatus(false, "common_msg", msg);     		
         }else if(json != null && json.resp != null && json.resp.status == -1)
         {
            reportStatus(true, "common_msg", json.resp.message);
         }
       }
     });  
     
   }
   
   $(".predefined_subscription").on("change", function()
     {
       var udm = $(this).attr("value");
       var checked = $(this).is(":checked");
       handleUdmDbUpdate("udmdb_update", udm, checked);
     }
   );

   $(".udm_subscription").on("change", function()
     {
       var udm = $(this).attr("value");
       var checked = $(this).is(":checked");
       handleUdmDbUpdate("udmdb_update", "UDM."+udm, checked);
     }
   );

   $(".alert_subscription").on("change", function()
     {
       var udm = $(this).attr("value");
       var checked = $(this).is(":checked");
       handleUdmDbUpdate("alertdb_update", udm,  checked);
     }
   );
   
   //whether to subscribe a UDM or not
   //cmd: alertdb_update, udmdb_update
   //m: UDM, metrics group name (including subgroup), or alert name
   //subscribe: true subscribe, false unsubscribe
   function handleUdmDbUpdate(cmd, m,  subscribe)
   {
     
     var group = mydomval("list_udmdb");
     var host = mydomval("list_udmdb_host");
     var mydata = "cmd="+cmd+"&m="+escape(m) +"&group=" + group;
     if(host != null && host != "")
       mydata += "&host="+host;
	 if(subscribe)
	   mydata += "&subscribe=y";
	 else mydata += "&subscribe=n";      
     mydata += "&seed="+Math.random();
     $.ajax({
       url: "udm.html",
       data: mydata,
       dataType: 'json',
       success: function(json)
       {
         if(json!=null && json.resp!=null)
         {
           if(json.resp.status==0)
             alert("UDMs for "+group + ", "+ host + " have been updated.");
           else
             alert("Failed to update UDMs for "+ group +", "+ host);
         } 
       }
     });
   }
 </c:if>
 </script>
</body>