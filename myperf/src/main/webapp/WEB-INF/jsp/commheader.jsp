<%@page trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<%
  String protocol = System.getProperty("url_protocl", "http");
  if(!"https".equalsIgnoreCase(protocol))protocol="http";
%>
<script src="<%= protocol%>://code.jquery.com/jquery-1.10.2.js"></script>
<script src="<%= protocol%>://code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
<script src="jquery/js/jquery.ui-contextmenu.min.js"></script>
<script src="jquery/datatables/js/jquery.dataTables.min.js"></script>
<script src="jquery/js/jquery.tooltipster.js"></script>
<script src="js/ui.js"></script>
<link rel="stylesheet" href="<%= protocol%>://code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css" />
<link rel="stylesheet" href="jquery/css/tooltipster.css" />
<link rel="stylesheet" type="text/css" href="jquery/datatables/css/jquery.dataTables.css" />
<style>
 .hilitecell {color:red;}
 .hilitecellyellow {color:orange;}
 .hilitecellwarn {color:#FF9900;}
 .hilitecellgreen {color:green;}
 .scrollContainer{height:220px;overflow-y:auto;}
 .cssError {color:red;font-size:10px;}
 .cssInfo {color:blue;font-size:10px;}
 .gobtn {background: url('../img/next_arrow.gif') 90% 50% no-repeat;padding-right:14px;}
 div.box {display: block;float: left;width: 9px;height: 9px;padding: 0;text-decoration: none; border: 1px solid #444;color: #444;text-align: center;line-height: 9px; font-size: 9px;font-weight: bold;}
 
 .clearTabView {background: transparent; border:none;}
 .ui-widget-header {background: transparent; border:none;border-bottom: 1px solid #c0c0c0; 
 	          -moz-border-radius: 0px;-webkit-border-radius: 0px; border-radius: 0ps;}
 .ui-tabs-nav .ui-state-default {background: transparent; border:none;}
 .ui-tabs-nav .ui-state-active {background: transparent url(img/ui_active_tab.png) no-repeat bottom center; border:none;}
 .ui-tabs-nav .ui-state-default a {color: #c0c0c0; outline:none;}
 .ui-tabs-nav .ui-state-active a {color: #47a; outline:none;}  
 
 .tooltipster-default .tooltipster-content{font-size:10px; line-height:12px; } 
 .datatableContainer{overflow-x:auto;}
 table.dataTable{margin-left: 0px;}
 .dataTables_wrapper .dataTables_filter {float: left;}
 .dataTables_filter label {float: left;}
 table.dataTable tbody td{white-space: nowrap;}
 table.dataTable thead th{text-align: left;}
 .ui-contextmenu{min-width:200px; padding-top:10px;padding-bottom:10px;}
 span.step{ background: #000000;color: #ffffff; display:inline-block;font-weight:bold;text-align: center;padding:2px;width: 28px;}  
 
</style>
