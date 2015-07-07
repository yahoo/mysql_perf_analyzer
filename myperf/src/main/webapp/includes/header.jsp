<%@page trimDirectiveWhitespaces="true"%>
<%@page import="com.yahoo.dba.perf.myperf.common.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<div class="page-header-area">
  <% 
    String pg = request.getParameter("pg");
    if(session==null || session.getAttribute(AppUser.SESSION_ATTRIBUTE)==null){
  %>
	<span>MySQL Performance Analyzer - v<%= Constants.VERSION %>&nbsp;&nbsp;<a href="signin.htm" >Sign In</a>&nbsp;&nbsp;
	<a target="_help" href="help.htm?key=${help_key}">Help</a></span>
  <%}else {
      AppUser u = AppUser.class.cast(session.getAttribute(AppUser.SESSION_ATTRIBUTE));
  %>
 <% if (u == null) { %>
   <!-- no user log in -->
 <% } else { %>
   <!-- user is <%= u.getName() %>, admin <%= u.isAdminUser() %> -->
 <% } %> 
    <style>
		#my_clipboard_div{position:absolute;top:24px;width:640px;height:240px;text-align: left; padding: 10px; display:none;background-color:#eee;border:1px solid black;}
		#my_clipboard{position:absolute;top:24px;width:600px;height:200px;margin:0px;text-align: left;font-family:courier,yukamoondo,serif;white-space:nowrap;background-color:#FFFFCC;border:1px solid black;overflow:scroll;}
		#ctl_my_clipboard_div{position:absolute;top:2px;width:600px;height:24px;}
   </style>
   <div id="menu_header">
     <ul id="menu-topmenu" class="nav">
	   <li id="menu-item-1"><a href="#" title="Vesrion  v<%= Constants.VERSION %>.">MySQL Performance Analyzer&nbsp;&#9660;</a>
	     <ul>
		   <li><a target="_help" href="<%= request.getContextPath() %>/help.htm?key=about">About</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=alerts">Alerts</a></li>
		   <li><a href="<%= request.getContextPath() %>/db.htm">DB Info</a></li>
		   <li><a target="_help" href="<%= request.getContextPath() %>/help.htm?key=${help_key}">Help</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=mt">Meta Data</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=m">Metrics Charts</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=ps">Perf Schema</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=rt">Realtime Tracking</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=settings">Settings</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=snmp">SNMP</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=st">Status/Dashboard</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=top">Top</a></li>
		   <li><a href="<%= request.getContextPath() %>/perf.htm?pg=pf" title="MySQL Profiling">Tuning/Profiling</a></li>
		   <li><a href="<%= request.getContextPath() %>/user.htm">User</a></li>
		   <li><a href="<%= request.getContextPath() %>/udm.htm">User Defined Metrics/Alerts</a></li>
	     </ul>
	   </li>
	   <li id="menu-item-2"><a href="<%= request.getContextPath() %>/perf.htm?pg=top" 
	   		title="Realtime DB and system metrics" style='text-decoration:<%= "top".equals(pg)?"underline":"none"%>;'>Top</a></li>
	   <li id="menu-item-3"><a href="<%= request.getContextPath() %>/perf.htm?pg=rt" 
	   		title="Realtime DB system, session and SQL tracking" style='text-decoration:<%= "rt".equals(pg)?"underline":"none"%>;'>Realtime</a></li>
	   <li id="menu-item-4"><a href="<%= request.getContextPath() %>/perf.htm?pg=m" 
	   		title="Performance related metrics data" style='text-decoration:<%= "m".equals(pg)?"underline":"none"%>;'>Charts</a></li>
	   <li id="menu-item-5"><a href="<%= request.getContextPath() %>/perf.htm?pg=mt" 
	   		title="Meta data" style='text-decoration:<%= "mt".equals(pg)?"underline":"none"%>;'>Meta</a></li>
	   <li id="menu-item-6"><a href="<%= request.getContextPath() %>/perf.htm?pg=pf" 
	   		title="MySQL Profiling" style='text-decoration:<%= "pf".equals(pg)?"underline":"none"%>;'>Profiling</a></li>
	   <li id="menu-item-7"><a href="<%= request.getContextPath() %>/perf.htm?pg=ps" 
	   		style='text-decoration:<%= "ps".equals(pg)?"underline":"none"%>;'>Perf Schema</a></li>
	   <li id="menu-item-8"><a href="<%= request.getContextPath() %>/perf.htm?pg=st" 
	   		style='text-decoration:<%= "st".equals(pg)?"underline":"none"%>;'>Dashboard</a></li>
	   <li id="menu-item-9"><a href="logout.html" 
	   		title="You are logged in as <%= u.getName() %>, click here to sign out.">Sign Out(<%= u.getName() %>)</a></li>
	   <li id="menu-item-10"><a target="_help" href="help.htm?key=${help_key}">Help</a></li>
    </ul>
   </div>	
	<script language="javascript">
	    $('.nav li').hover(
	      function()
	      {
	        $('ul', this).fadeIn();
	      },
	      function()
	      {
	        $('ul', this).fadeOut();
	      }
	    );
	    function hideClipboard()
	    {
	    	document.getElementById("my_clipboard_div").style.display = "none";
	    	return false;
	    }
	    function exportData(data)
	    {
	        if(data!=null)
	        {
	          data = data.replace('<','&lt;');
	          data = data.replace('>','&gt;');
	        }
	    	document.getElementById("my_clipboard").innerHTML = "<pre>"+data+"\r\n</pre>";
	    	document.getElementById("my_clipboard_div").style.zIndex = "10000";
	    	document.getElementById("my_clipboard").style.zIndex = "10001";
	    	document.getElementById("my_clipboard_div").style.display = "block";
	    	ZeroClipboard.clearData();	    	
            ZeroClipboard.setData("text/plain", data );
		}
	</script>
  <% } %>	
</div>
<% if(session!=null && session.getAttribute(AppUser.SESSION_ATTRIBUTE)!=null){
%>
<div id="my_clipboard_div">
   <div id="ctl_my_clipboard_div"><button id="btn_clip" class="clip_button" 
   	title="Click to copy to clipboard.">Copy to Clipboard</button>
   		<a href="#" class="rightbtn" onclick="return hideClipboard();">Hide</a>
   </div>
   <div id="my_clipboard"></div>
   <script type="text/javascript" src="jquery/js/ZeroClipboard.js"></script>
   <script type="text/javascript">
      var client = new ZeroClipboard( document.getElementById('btn_clip'));
   </script>
</div>
<script language="javascript">
//add a queue to store messages so that we will not flood the user
//keep only 3 outstanding notifications
var MAX_OUTSTANDING_ALERTS = 3;
var outstanding_alert_count = 0;//showed, but not clicked by the user
var outstanding_alerts = [];//keep at most MAX_OUTSTANDING_ALERTS alerts, 
                            //if we have to add new one, throw old one away.

function pollAlerts()
{
  $.ajax({
    url: "alerts.html",
       data: "cmd=get_top&seed="+Math.random(),
       dataType: 'json',
       success: function(json)
       {
         if(json != null && json.resp != null && json.resp.results != null 
         && json.resp.results.results.length >0)
         {
           var res = json.resp.results.results;
           for(var i=0; i<res.length; i++)
           {
             var e= res[i];
             var msg = e["ALERT"];
             if(e["VALUE"] != null && e["VALUE"] != "")
               msg += "("+e["VALUE"]+")";
             msg += " "+e["GROUP"]+", "+e["HOST"];
             msg += " TIME: "+e["TS"];  
             try
             {
               if(outstanding_alert_count < MAX_OUTSTANDING_ALERTS)
               {
                 outstanding_alert_count++;
                 var notification = new Notification("Alerts", {body: msg});
                 attachNotificationClickHandler(notification);
               }else
               {
                 //throw away old records if queue full
                 if(outstanding_alerts.length >= MAX_OUTSTANDING_ALERTS)
                   outstanding_alerts.shift();
                 outstanding_alerts.push(msg);  
               }
               
             }catch(e)
             {
               console.log(msg);
             }
           }
           
         }//if  
       }//success
  });//ajax
}//func

//handle user click on notification
function attachNotificationClickHandler(notification)
{
   notification["onclick"] = function(e)
   {
     outstanding_alert_count--;
     if(outstanding_alerts.length>0)
     {
       var msg = outstanding_alerts.shift();
       outstanding_alert_count++;
       var notification2 = new Notification("Alerts", {body: msg});
       attachNotificationClickHandler(notification2);
     }
   }
}

Notification.requestPermission(function(status)
{
  if(Notification.permission !== status)
    Notification.permission = status;
});

//set 30 seconds polling interval
setInterval(pollAlerts, 60000);

</script>
<% } %>

