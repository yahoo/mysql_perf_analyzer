<%@page trimDirectiveWhitespaces="true"%>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<div id="simple_dbsearch" style="position:absolute;top:<%= request.getParameter("y") %>px;left:<%= request.getParameter("x") %>px;width:640px;background-color:white;border:1px solid silver;display:none;z-index:1000;">
   <div id="simple_dbsearch_title" style="position:relative;padding:8px 28px 8px 8px;min-height:13px;height:13px;font-weight:bold;color:white;background-color:#3961c5;">Database Name Search</div>
   <div style="margin-top:10px;padding-left:20px;">Keyword: <input type="text" name="dbsearch_keyword" id="dbsearch_keyword" />&nbsp;<input type="button" id="btn_dbsearch" name="btn_dbsearch" value="Search" /></div>
   <div id="simple_dbsearch_mainh" style="font-size:12px;margin-top:10px;padding-left:20px;padding-right:8px;width:600px;height:240px;">
     <span style="font-weight:bold;" id="dbsearch_msg"></span>   
     <div class="scrollContainer" id=" dbsearch_tbl_div">
     	<div id="dbsearch_tbl_div">
     	  <table id="dbsearch_tbl" cellpadding="0" cellspacing="0" border="0" class="display"></table>
     	</div>
     </div>
   </div>
   <div style="text-align:right;padding-right:24px;padding-top:4px;padding-bottom:4px;background-color:#EDF5FF;"><span><a href="db.htm">Add New Databases</a></span>&nbsp;&nbsp;&nbsp;&nbsp;<span style="display:inline-box;"><input type="button" style="padding:.4em 1em .45em;" onclick="hideDBSearch();" value="Close" /></span></div>
</div>
<script language="javascript">
  var target_list_id = null;
  var target_host_name = null;
  var target_port = null;
  function hideDBSearch()
  {
    mydom("simple_dbsearch").style.display="none";
  }
  function prepareDBSearch(targetListId, targetHostName, keyword, actWhenOpen, targetPort)
  {
    target_list_id = targetListId;
	target_host_name = targetHostName;
	target_port = targetPort;
	if(keyword!=null)
	{
      mydom("dbsearch_keyword").value=keyword;
      if(actWhenOpen)
        dbSearchTable.sendQuery();      
    }  
	mydom("simple_dbsearch").style.display = "block";
	mydom("dbsearch_keyword").focus();		
  }
	 
  var dbSearchTable = new JSTable({
   	   name: "dbsearch",
   	   query:{
   	     queryURL: "dbsearch.html",
   	     paramFields:[{name:"cmd", value:"search"}, {name:"keyword", valueField:"dbsearch_keyword"}]
   	   },
	   handlers: {jquery:1,selectHandler: selectRow_dbsearch},
	   searchable: "y"
  });
  
  function selectRow_dbsearch(obj)
  {
    if(obj == null || obj.datatable == null)return;
    var dbname = obj.datatable.getCellValueByColumnName(obj.row, 'DBGROUPNAME');
    var hostname = obj.datatable.getCellValueByColumnName(obj.row, 'HOSTNAME');
    var port = obj.datatable.getCellValueByColumnName(obj.row, 'PORT');
	if(target_list_id!=null && mydom(target_list_id)!=null)
	{	            
	  var l = mydom(target_list_id);
	  if(l.tagName=='INPUT')
	  {
	    l.value = dbname;
	    l.focus();
	    if(target_host_name!=null && mydom(target_host_name)!=null)
	      mydom(target_host_name).value=hostname;
	    if(target_port!=null && mydom(target_port)!=null)
	      mydom(target_port).value=port;
	  }
	  else
	  {
	    for (i=0;i<l.options.length;i++)
		{
	      if(l.options[i].value==dbname)
	      {
	        l.selectedIndex = i;
	        if("fireEvent" in l)
	          l.fireEvent("onchange");
		    else
		    {
			  var evt = document.createEvent("HTMLEvents");
		      evt.initEvent("change",false,true);
	    	  l.dispatchEvent(evt);
	    	}
	        break;
	      }
	    }	
	  }
	}
	hideDBSearch();
  }
  
  $('#btn_dbsearch').click(function()
    {
      dbSearchTable.sendQuery();    
    }
  );
</script>