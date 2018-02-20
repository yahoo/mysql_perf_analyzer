/*
 *  Copyright 2015, Yahoo Inc.
 *  Copyrights licensed under the Apache License.
 *  See the accompanying LICENSE file for terms.
 */
//javascripts for UI elements
/*
  Constructor of javascript based table creation manipulation. 
  Instead passing individual parameters, the common parameters are  encapsulated 
  inside a JSOB object, with the following key
  name: a unique name to identify the table, or a list of names to identify multi table from the same retrieval process. Required.
  domid: HTML element (div) id to render the table, if not provided, {name}_tbl
  query: a JSON object with the following content 
        queryURL: the URL to query the data to fill the table, such as "query.html"
        sqlId: the sql query to be used, defined inside sql.xml for now.
        sqlIdField: if sqlId is not provided or dynamic, sqlIdField should be used to pass sqlId
        formAction: GET or POST, default to GET
        paramFields: (the parameter place holder, the element id, the element value) triples of the parameters. Note our SQL definition has 
               place holders like &p_1, &p_2, &p_3, etc:
               name: parameter name, such as p_1, p_2, p-3, cmd, keyword, etc
               value: explicit value
               valueField: if dynamic value, field/html element to extract value
               enc: url encoding method, such as enc:encodeURIComponent or escape
  trigger: the event and event source which triggers the query to retrieve and populate data
        triggerEvent: the event name, if associated with some event, such as "click", "change".
        triggerSource: the component id of the trigger event source, if applicable
  db: the database in concern      
        dbGroupId: the element id of the group id of the database to be queried
        dbHost: the element id of the host name of the database to be queried
    
  handlers: functions to handle message or events
        jquery: if 1, use jquery ui-contextmenu
        statusMessageHandler: the input is (datatable, status, message), status 0 means succeed
        selectHandler: event handler for table row click. the handler will take an object input with the following entries
                htmlRowIndex: table row index (starting from 1)
                row: row index of datatable cell (row index of original data)
                column: column index
                datatable: this JSTable object
        contextMenuHandler: event handler to build context menu and handle context menu selection
                preprocessor: an event handler before popup menu is displayed
                              input (this_obj, selected_row_id, trigger_event)
                A list of the triplets:
                  key: menu entry key
                  label: menu label
                  handler: function to handle the menu entry selection, ithe handler will take an object input with the following entries
                      htmlRowIndex: table row index (starting from 1)
                      key: the menu command
                      row: row index of datatable cell (row index of original data)
                      column: column index
                      datatable: this JSTable object
        hoverHandler: event handler for mouse hover
        
  formatter: column/cell formatter
        commonFormatter: for all cells, if presented
        rowFormatter: if present, attach to createdRow callback, which takes (row, data, dataIndex) as input. 
              All will be wrapped inside an object:       
                      row: table row element
                      data: row data
                      dataIndex: row index
                      datatable: this
        allowHTML: if true, for all columns. Not used for now
        sortable: if y(default), table will be soretable. if no, no sorting.
        columnFormatters: name values pairs of the following
            name: the column name as key, can be in the form of {table name}.{column name}, or just {column name}
            value: the column formatter
            
  diff: if presented, the table will be used to display changes when refreshed
        keyColumns: the columns as keys
        valueColumns: the metrics columns to show diffs
        diffOnly: if true, only row with diff will be displayed
  showRowDataOnClick: if y, when a table row is clicked, it will be shown inside tooltips
  tooltipCallbackOnClick: provide call back function to handle tooltip, takes object of form
         {datatable:this JSTable, src:srcElement, row:rowIdx, col:colIdx} 
  searchable: if y, allow search inside the table (JQuery datatable feature).
*/
function JSTable(defObj)
{
  if(defObj == null) return; //nothing defined
  this.name = defObj.name;  
  //indicate if there are multi table, valid values: 'y', 'n'
  this.multiTable = (typeof defObj.name == 'string')?'n':'y';

  //table UI related
  this.domid = defObj.domid != null? defObj.domid: this.name+"_tbl";

  //how to retrieve the data
  this.query = defObj.query;
  
  //what to trigger data retrieval
  this.trigger = defObj.trigger;
  
  //DB in concern
  this.db = defObj.db;
  
  //event handlers
  this.handlers = defObj.handlers;
  
  //column formatter
  this.formatter = defObj.formatter;  
  
  //table to display changes?
  this.diff = defObj.diff;
  
  //Show tooltips
  this.showRowDataOnClick = defObj.showRowDataOnClick;
  this.tooltipCallbackOnClick = defObj.tooltipCallbackOnClick;
  this.searchable = defObj.searchable;
  
  //place holder for data
  this.dataObj = {};//cache the data
  this.baseDataObj = {}; //for delta calculation, only allow one table for now
  this.startTime = null;//first data gather time
  //place holder for datatables, the key is table name (so allow multi tables)
  this.datatable = {};
  this.columns = {};
  this.columnMapping = {};//for easy to look up column index by name
  
  this.selectedRow = {};
  this.selectedColumn = {};
  this.selectedRecord = {};
}

JSTable.prototype.getFirstTableName = function()
{
  if(this.multiTable == 'y')
    return this.name[0];
  else
    return this.name;  
}

JSTable.prototype.getColumnIndex = function(name, tblname)
{
  var tbl = tblname;
  if(tbl == null)
    tbl =  this.getFirstTableName();
    
  var mapping = this.columnMapping[tbl];
  if(mapping == null)return -1;
  var idx = mapping[name];
  if(idx == null)
	  return -1;
  return idx;
}

/*
  retrieve data table cell value by column name, using displayed order
*/
JSTable.prototype.getCellValueByColumnName = function(rowIdx, colName, tblname)
{  
  var colIdx = this.getColumnIndex(colName, tblname);
  return this.getCellValueByColumnIndex(rowIdx, colIdx, tblname);
}

/*
  retrieve data table cell value by column index, using displayed order
*/
JSTable.prototype.getCellValueByColumnIndex = function(rowIdx, colIdx, tblname)
{  
  if(colIdx <0)return null;
  var tbl = tblname;
  if(tbl == null)
    tbl =  this.getFirstTableName();
  
  if(this.datatable[tbl] == null)return null;
  return this.datatable[tbl].cell(rowIdx, colIdx).data();

}

/*
 generate URL parameters to retrieve the data
*/
JSTable.prototype.getQuryParameter = function()
{
	if (this.query == null )
		return null;

    var myurlParams = "";
    if(this.db != null && this.db.dbGroupId != null)
      myurlParams +=  "group="+escape(document.getElementById(this.db.dbGroupId).value);
    if(this.db != null && this.db.dbHost != null && document.getElementById(this.db.dbHost) != null)
        myurlParams +=  "&host="+escape(document.getElementById(this.db.dbHost).value);
    if (this.query.sqlId != null && this.query.sqlId != "")
	    myurlParams +=  "&sql="+this.query.sqlId;
	else if (this.query.sqlIdField != null)
	    myurlParams +=  "&sql="+escape(document.getElementById(this.query.sqlIdField).value);
    myurlParams +=  "&respFormat=json";
    //add parameters if any
    if(this.query.paramFields!=null && this.query.paramFields.length>0)
	{
		for(var i=0;i<this.query.paramFields.length;i++)
		{
		  var p = this.query.paramFields[i];
		  myurlParams += "&"+p.name+"=";
		  if(p.value != null)
		    myurlParams += (p.enc != null)? p.enc(p.value):escape(p.value);
		  else if(p.valueField != null)
		    myurlParams += (p.enc != null)?p.enc(document.getElementById(p.valueField).value):escape(document.getElementById(p.valueField).value);
		}
	}
    
    myurlParams +=  "&seed="+Math.random();
    return myurlParams;
}

/*
  Help function to check if any column count or name change
  for the purpose to rebuild table structure
*/
function diffColumns(origColumns, newColumns)
{
  if(origColumns == null)return false;//most likely new table
  if(newColumns == null)return true;
  if(origColumns.length != newColumns.length)return true;
  for(var i = 0; i<origColumns.length; i++)
  {
    if(origColumns[i] != newColumns[i])return true;
  }
  return false;
}

/*
 General handler to process ajax response, for ecxample, display the data in grid
*/
JSTable.prototype.handleSuccess = function (jsonObj)
{
  if(jsonObj == null )
  {
    console.log("No JSON object returned.");
    return;
  }  
  if(jsonObj.resp == null)
  {
    console.log("Wrong json object returned.");
    console.log(jsonObj);
    return;
  }
  
  var my = this;    

  if(jsonObj.resp.status != 0) //bad status
  { 
    console.log("Status: "+jsonObj.resp.status+", message "+jsonObj.resp.message);
    if(my.handlers!=null && my.handlers.statusMessageHandler!=null)
      my.handlers.statusMessageHandler(my, jsonObj.resp.status, jsonObj.resp.message);
    return;
  }
  
  var names = [];
  
  if(my.multiTable == 'y')
    names = my.name;
  else
    names[0] = my.name;

  for(var tbl_i=0; tbl_i < names.length; tbl_i++)
  {
    var tblname = names[tbl_i];    
    var res = jsonObj.resp[tblname];
    if(res == null) res = jsonObj.resp.results;
    
    if(res == null)
    {
      if(names.length == 1)
      {
        console.log("No data");
        return;
      }
      continue;//otherwise
    }  
  
    var isDiff = false;
    //only allow one for diff now
    if(my.diff != null && names.length == 1 
                 && my.diff.keyColumns != null 
                 && my.diff.keyColumns.length > 0
                 && my.diff.valueColumns != null 
                 && my.diff.valueColumns.length > 0) isDiff = true;
                 
    var newDataSet = res.results;
    if (isDiff)
    {
       var deltaData = simpleRecordMDiff(my.diff.keyColumns, my.diff.valueColumns, my.baseDataObj[tblname], newDataSet, my.startTime);
       if (my.baseDataObj[tblname] == null && deltaData != null)
       {
         my.baseDataObj[tblname] = deltaData;
         my.startTime = new Date();
         isDiff = false;
       }
       newDataSet = deltaData;
    }
                 
    //column def and formatter
    var cols = new Array();
    if (!isDiff)//use original data
    {
      for(var i=0;i<res.columns.length;i++)
      {
        cols[i] = new Object();
        cols[i].key = res.columns[i];
        cols[i].data =  res.columns[i];
        cols[i].title =  res.columns[i];
        cols[i].allowHTML = true;
        cols[i].sortable = true;
        if (my.formatter != null)
        {
          var colfmt = null;
          if (my.formatter.columnFormatters != null)
          {
             colfmt = my.formatter.columnFormatters[tblname+'.' + cols[i].key];
             if(colfmt == null)
	           colfmt = my.formatter.columnFormatters[cols[i].key];
          }   
          if (colfmt == null)colfmt = my.formatter.commonFormatter;    
            if(colfmt != null)cols[i].render = colfmt;
        }
      }
    }else //use delta
    {
     //Add key column first
	 for( var i = 0; i<my.diff.keyColumns.length; i++)
	 {
       cols[i] = new Object();
       cols[i].key = my.diff.keyColumns[i];
       cols[i].data = my.diff.keyColumns[i];
       cols[i].title = my.diff.keyColumns[i];
       cols[i].allowHTML = true;
       cols[i].sortable = true;
       if (my.formatter != null)
       {
         var colfmt = null;
         if (my.formatter.columnFormatters != null)
         {
             colfmt = my.formatter.columnFormatters[tblname+'.' + cols[i].key];
             if(colfmt == null)
	           colfmt = my.formatter.columnFormatters[cols[i].key];
         }   
         if (colfmt == null)colfmt = my.formatter.commonFormatter;    
         if(colfmt != null)cols[i].render = colfmt;
       }		  
     }
		
	 //add new metrics data, delta, avg, old data
	 var suffixes = ["/SEC", "_DELTA", "", "_OLD"];
	 for (var si = 0; si < suffixes.length; si++)
	 {
	   for( var i = 0; i<my.diff.valueColumns.length; i++)
	   {
          var col = new Object();
          col.key = my.diff.valueColumns[i]+suffixes[si];
          col.data = col.key;
          col.title = col.key;
          col.allowHTML = true;
          col.sortable = true;
          if (my.formatter != null)
          {
            var colfmt = null;
            if (my.formatter.columnFormatters != null)
              colfmt = my.formatter.columnFormatters[col.key];
            if (colfmt == null)colfmt = my.formatter.commonFormatter;    
            if(colfmt != null)col.render = colfmt;
          }
          cols[cols.length] = col;
		}//for( var i = 0
      }////for (var si = 0				
     }//delta
     
     var reconstructTable = diffColumns(my.columns[tblname], cols);//record if changes
     my.columns[tblname] = cols;
     my.dataObj[tblname] = newDataSet;
     my.columnMapping[tblname] = {};
     for(var i=0; i<cols.length; i++)
       my.columnMapping[tblname][cols[i].key] = i;
       
     var myTableObj = {
       "destroy": true,
       "columns": cols,
       "data": newDataSet,
       "paging": false,
       "searching": (my.searchable == 'y')?true:false,
       "deferRender": true,
       "order": [] //disable initial ordering
     };
     
     if(my.formatter != null)
     {
       if( my.formatter.sortable == 'n')
         myTableObj["ordering"] = false;
       if(my.formatter.rowFormatter != null)
         myTableObj["createdRow"] = function(row, data, dataIndex)
         {
         	my.formatter.rowFormatter({row:row, data:data, dataIndex:dataIndex, datatable: my});
         }  
     }
     
     if(reconstructTable && my.datatable[tblname] != null) 
     {
       my.datatable[tblname].destroy();
       $('#'+tblname+'_tbl').empty();
     }
     my.datatable[tblname] = $('#'+tblname+'_tbl').DataTable(myTableObj);
     
     //context menu
     my.attachContextMenu(tblname);
     
     //install tooltip
     if(my.showRowDataOnClick == 'y' || my.tooltipCallbackOnClick != null)
     {
       $('#' + tblname + '_tbl td').tooltipster({
           trigger: 'click',
           onlyOnce: false,
           position: 'bottom',
           contentAsHTML: true,
           maxWidth: 500,
           functionReady: function(origin, tooltip)
           {
             var a = tooltip[0].offsetTop;
             if(a<0)
	             $(".tooltipster-base").offset({ top: 0});
           }
       });
     }
     
     //getRowData(rowIdx)
     if((my.handlers != null && my.handlers.selectHandler != null) 
     || my.showRowDataOnClick == 'y'
     || my.tooltipCallbackOnClick != null)
     {
       $('#' + tblname + '_tbl tbody').on('click', 'td', function(e)
         {
            var colIdx = my.datatable[tblname].cell(this).index().column;
            var rowIdx = my.datatable[tblname].cell(this).index().row;
            if(my.showRowDataOnClick == 'y')
            {
              //show row content
              $(this).tooltipster('update', '<pre>' + my.getRowData(rowIdx, tblname) +'</pre>');
            }else if(my.tooltipCallbackOnClick != null)
            {
              my.tooltipCallbackOnClick({datatable:my, name:tblname, src:this, row:rowIdx, col:colIdx});
            }  
            //TODO rowIndex
            if(my.handlers != null && my.handlers.selectHandler != null) 
              my.handlers.selectHandler({row:rowIdx, column:colIdx, datatable:my, name:tblname});
         });
     }
   }     
      //TODO response status
   if(my.handlers!=null && my.handlers.statusMessageHandler!=null)
      my.handlers.statusMessageHandler(my, 0, "OK");
}

/*
  Return row data in key value pair, with one pair one line
*/
JSTable.prototype.getRowData = function(rowIdx, tblname)
{
  var tbl = tblname;
  if(tbl == null)
    tbl =  this.getFirstTableName();
  
  var row = this.datatable[tbl].row(rowIdx);
  var data = row.data();
  var output = "";
  var maxKeyLength = 0;
  for(var i=0; i<this.columns[tbl].length; i++)
  {
    var len = this.columns[tbl][i].key.length;
    if(len > maxKeyLength) maxKeyLength = len;
  }
  
  //rightPadding
  for(var i=0; i<this.columns[tbl].length; i++)
  {
    if(i > 0)output += '\r\n';
    var key =  this.columns[tbl][i].key;
    var val = data[key];
    if(val != null && val.length>100)
      val = breakLongLineFmt(val);
    output += rightPadding(key, maxKeyLength) + ": "+val;
  }
  return output;
}

/*
  Send query to the server to retrieve data
*/
JSTable.prototype.sendQuery = function()
{
  var params = this.getQuryParameter();
  var my = this; //result handler
  var url = this.query.queryURL;
  var formAction = 'GET';
  if(my.query.formAction == 'POST' || my.query.formAction == 'post')
    formAction = 'POST';
    
  $.ajax({
       url: url,
       data: params,
       type: formAction,
       dataType: 'json',
       success: function(jsonObj)
       {
         my.handleSuccess(jsonObj);       
       },
       statusCode: {
         404: function()
         {
            console.log("Received status code 404"); //TODO
         },
         500: function()
         {
            console.log("Received status code 500"); //TODO    
         }
       },
       error: function(x, t, m)
       {
           console.log(x);
           console.log("t="+t+", m="+m );
       }
   });
}

JSTable.prototype.attachContextMenu = function(tblname)
{
  var my = this;
  var menu = [];
  if (my.handlers != null && my.handlers.contextMenuHandler != null)
  {
    for (var  i = 0; i < my.handlers.contextMenuHandler.length; i++)
    {
      var entry = my.handlers.contextMenuHandler[i];//key and label
      menu[menu.length] = {cmd: 'menu_item_' + entry.key, title: entry.label};
    }
  }
    //add clipboard
  if (document.getElementById("my_clipboard")!=null)
    menu[menu.length] = {cmd:"menu_item_table_export", title:"Copy To Clipboard"};    

  if(menu.length == 0)return; //no menu to show
  
  var trigger = 'td';
  $('#' + tblname + '_tbl tbody').contextmenu({
    delegate: trigger,
    menu: menu,
    beforeOpen: function(e, ui)
    {
      my.selectedRow = my.datatable[tblname].cell(e.currentTarget).index().row;
      my.selectedColumn = my.datatable[tblname].cell(e.currentTarget).index().column;      
      ui.menu.zIndex(99999);//otherwise the popup will be popped under
    },
    select: function(e, ui)
    {
      var tgt = ui.cmd;
      //default menu processing
      if(tgt=="menu_item_table_export")
      {
        exportTableRecordSetJQuery(my, tblname);
        return;
      }
      //
      if (my.handlers != null && my.handlers.contextMenuHandler != null)
      {
        for (var  i = 0; i < my.handlers.contextMenuHandler.length; i++)
        {
          var entry = my.handlers.contextMenuHandler[i];//key and label
          if (tgt == "menu_item_" + entry.key)
          {
            if (entry.handler != null)
               entry.handler({key:entry.key, row:my.selectedRow, column:my.selectedColumn, datatable:my, name:tblname, event: e});
            return;
          }
        }//for loop of handlers
      }//if (my.handlers != null
    }//select
  });
}

/*
 Helper function for simpleRecordMDiff
 get values of key columns
*/
function getKeys(keyColumns, dataRow)
{
  var keys = [];
  for (var i = 0; i < keyColumns.length; i++)
  {
    var v = dataRow[keyColumns[i]];
    keys[keys.length] = v;
  }
  return keys;
}

/*
  Functions to compare two array
 */
function compareArrays(a1, a2)
{
  if (a1 == null && a2 == null) return 0;
  if (a1 == null && a2 != null) return -1;
  if (a1 != null && a2 == null) return 1;
  
  for (var i = 0; i < a1.length; i++)
  {
    if (a2.length < i + 1) return 1;
    if (a1[i] < a2[i]) return -1;
    if (a1[i] > a2[i]) return 1;    
  }
  if(a2.length>a1.length) return -1;
  return 0;
}

/*
  Help function to calculate changes from two data sets
  keyColumns: the column used as key form comparison
  valueColumns: the column names of the metrics to be compared
  oldData: the old data, sorted by keyColumns
  newData: the newData to be compared
  startTime: the time of old data (JS Date) 
*/
function simpleRecordMDiff(keyColumns, valueColumns, oldData, newData, startTime)
{
      if(newData==null||newData.length==0)return null;
      var mydata = newData, myOldData = oldData;
      //sort the data by key first
      mydata.sort(function(a,b)
        {
          var a1 = getKeys(keyColumns, a);
          var b1 = getKeys(keyColumns, b);
          return compareArrays(a1, b1);
        }
      );

	  
      var output = new Array();
      var i=0;//newData index
      var j=0;//oldData index
      var VALLEN = valueColumns.length;
      var KEYLEN = keyColumns.length;
      
      if(oldData == null) //no old data to compare, return the new data
      {
        for(i=0;i<mydata.length;i++)
        {
          var row = new Object();
          var newRow = mydata[i];
          //copy original data
          for (var key in newRow) {
            if (newRow.hasOwnProperty(key)) 
              row[key] = newRow[key];
          }
          
          //for(var ki=0; ki< KEYLEN; ki++)
	      //    row[keyColumns[ki]] = newRow[keyColumns[ki]];
          for(var vk=0;vk<VALLEN;vk++)
          {
            //row[valueColumns[vk]] = newRow[valueColumns[vk]];
            row[valueColumns[vk]+'_OLD'] = newRow[valueColumns[vk]];
          }
          output[output.length] = row;
        }
        return output;
      }
      
      var diffTimeMs = (new Date()).getTime() - startTime.getTime();

      while(i < mydata.length)
      {
        var row = mydata[i];
        var key = getKeys(keyColumns, row);
        var val = new Object();
        for(var vk = 0; vk < VALLEN; vk++)
        {
          val[valueColumns[vk]] = row[valueColumns[vk]];
        }
        i++;
        //look for match in oldData
        var preKey = null;
        while(myOldData != null && j < myOldData.length)
        {
          var tmpKey = getKeys(keyColumns, myOldData[j]);
          var keydiff = compareArrays(tmpKey, key);
          if(keydiff ==0)
          {
            preKey = tmpKey;
            break;
          }
          if(keydiff <0 ) j++;//old key not reach the new key yet
            else break;
        }
        var newRow = new Object();
        for (var ki = 0; ki < KEYLEN; ki++)
        {
        	newRow[keyColumns[ki]] = key[ki];
        }
        for(var vk=0;vk<VALLEN;vk++)
        {
          newRow[valueColumns[vk]] = val[valueColumns[vk]];
        }
        if(preKey==null)
        {
          for(var vk=0;vk<VALLEN;vk++)
            newRow[valueColumns[vk]+'_OLD'] = 0;
        }
        else
        {
          for(var vk=0;vk<VALLEN;vk++)
            newRow[valueColumns[vk]+'_OLD'] = myOldData[j][valueColumns[vk]+'_OLD'];j++;
        }
        var eq = true;
        for(var vk=0;vk<VALLEN;vk++)
        {
          if(newRow[valueColumns[vk]]!=newRow[valueColumns[vk]+'_OLD'])
          {
            eq = false;
            break;
          }
        }
        //for now, we only output !eq
        if(!eq)
        {
          for(var vk=0;vk<VALLEN;vk++)
          {      
            newRow[valueColumns[vk]+'_DELTA'] = eval((newRow[valueColumns[vk]]-newRow[valueColumns[vk]+'_OLD']).toFixed(6));
            newRow[valueColumns[vk]+'/SEC'] = eval((newRow[valueColumns[vk]+'_DELTA']*1000/diffTimeMs).toFixed(6));
          }
          output[output.length] = newRow;          
        }
      }
      return output;
 }

 
 /*
   helper functions to format data for copy-n-paste
*/
function exportTableRecordSetJQuery(jstbl, tblname){exportData(calTableRecordSetJQuery(jstbl, tblname));}
function calTableRecordSetJQuery(jstbl, tblname)
{
  if(jstbl == null || jstbl.datatable == null || jstbl.dataObj == null)
  {
    alert("No data to copy");
    return "";
  }
  var tbl = tblname;
  if(tbl == null)
    tbl =  this.getFirstTableName();

  var keys = jstbl.columns[tbl];
  var data = "";
  var first = true;
  var colSize = new Array();
  var colNames = new Array();
  for(i=0;i<keys.length;i++)
  {
    if(keys[i]!=null && keys[i].key!=null)
    {
      colSize[i] = keys[i].key.length;       
      colNames[i] = keys[i].key;
    }
    else
    {
      colSize[i] = 0;
      colNames[i] = "";
    }
  }
  //note cell(i,j) might not give data for displayed order
  //it is in the order when data are assigned.
  //rows() will provide original order after sorting  
  var rows = jstbl.datatable[tbl].rows()[0];
  var n_rows = rows.length;
  for(j=0;j<n_rows;j++)
  {
    for(i=0;i<colNames.length;i++)
    {
      var val = ""+ jstbl.datatable[tbl].cell(rows[j], i).data();
      if(val!=null && colSize[i]<val.length)colSize[i] = val.length;
    }
  }
  for(i=0;i<keys.length;i++)   
     data += rightPadding(keys[i].key, colSize[i]+3);   
  data = data+"\r\n";
  for(j=0;j<n_rows;j++)
  {
    for(i=0;i<colNames.length;i++)
    {
      var val = ""+jstbl.datatable[tbl].cell(rows[j], i).data();
      data += rightPadding(val, colSize[i]+3);
    }
    data = data+"\r\n";
  }
  return data;
}

function rightPadding(str, len)
{
  if(str==null)str = "";
  if(str.length>len)return str;
  else
  {
    var l = str.length;
    while(l<len)
    {
      str = str+" ";
      l++;
    }
  }
  return str;
}

function query_hostlist_main(dbgrpId, hostField, emptyLine, CB)
{
  var mydata = 't=dbhosts&dbid=' + dbgrpId + '&ct=json&seed='+Math.random();
  $.ajax({
     url: "datalist.html",
     data: mydata,
     dataType: 'json',
     success: function(json)
     {
       if(json != null && json.resp != null && json.resp.results != null 
         && json.resp.results.results.length > 0)
       {
         var res =json.resp.results.results;
         document.getElementById(hostField).options.length = 0; //clean up old entries
         if(emptyLine)
         {
           document.getElementById(hostField).options[0] = new Option("---", "");
         }
         for(var i = 0; i< res.length; i++)
         {
           var val = res[i].HOST;
           document.getElementById(hostField).options[document.getElementById(hostField).options.length] = new Option(val, val);          
         }//for
       } //if res
       if(CB)CB(); //any other callback
     }//success
  }); //ajax
}//function query_hostlist_main

//just try to save some typing
//return true if it is OK, otrherwise false
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

//check if input str is null or empty. 
//If yes, generate alert message and return false
//otherwise, return true
function checkEmpty(str, message)
{
  if(str == null || str == '')
  {
    if(message)
      alert(message);
    return false;
  }
  return true;
}


//This function will break a long line into multiple shorter line
//ov: original value
//maxSize: maximum display size, default to 1024 chars, if 0 or -1, no limit.
//lineSize: max output length for each line, default to 100 chars
function breakLongLineFmt(ov, maxSize, lineSize)
{
  if(ov == null)return null;
  if(lineSize == null || lineSize <=0) lineSize = 100;
  if(maxSize == null ) maxSize = 1024;
  var str = ''; //output
  var i=0;
  var len = 0;

  if( maxSize> 0 && ov.length > maxSize )ov = ov.substring(0,maxSize);
  ov = ov.replace(/</g,'&lt;');
  ov = ov.replace(/>/g,'&gt;'); 
  while(i<ov.length)
  {
	len = ov.length - i;
	var newLine = 0;
	if(len>lineSize)
	{
		var s = ov.substr(i,lineSize);
		var l = s.lastIndexOf('\n');
		if(l<0)l = s.lastIndexOf('\r');
		if(l>=0)newLine = 1;
		if(l<0)l = s.lastIndexOf('\t');
		if(l<0)l = s.lastIndexOf(' ');
		if(l<0)l = s.lastIndexOf(',');
		if(l<0)l = s.lastIndexOf(';');
		if(l>=0)len = l+1;
		else len = lineSize;
	 }
	 str += ov.substr(i,len);
	 i += len;
	 if(i<ov.length && newLine == 0)str+='\n';
   }
   return str;
}

//JQuery DataTable column render function
//data: cell data
//type: filter, display, sort
//row: full data source of the row
//meta: an object containing row (row index), col(column inex), settings (an object)
function jqueryFormatSqlText (data, type, row, meta)
{
  if(type == 'display')
	  return "<pre>"+breakLongLineFmt(data)+"</pre>";
  return data;
} 

function jqueryFormatText60 (data, type, row, meta)
{
  if(type == 'display')
	  return "<pre>"+breakLongLineFmt(data, 1024, 60)+"</pre>";
  return data;
} 

function jqueryVariableName  (data, type, row, meta)
{
  if(type == 'display')
  {
    var h = "http://dev.mysql.com/doc/refman/5.6/en/server-system-variables.html#sysvar_";
    if(data.indexOf("INNODB")==0)
        h = "http://dev.mysql.com/doc/refman/5.6/en/innodb-parameters.html#sysvar_";
    else if(data.indexOf("BINLOG")==0 || data.indexOf("LOG_BIN")==0 || data.indexOf("MAX_BINLOG")==0
      || data=='SYNC_BINLOG' || data=='MASTER_VERIFY_CHECKSUM' || data == 'LOG_SLAVE_UPDATES')
        h = "http://dev.mysql.com/doc/refman/5.6/en/replication-options-binary-log.html#sysvar_";
	else if(data.indexOf("RELAY_LOG")==0 || data.indexOf("SLAVE_")==0 || data.indexOf("SYNC_RELAY_LOG_")==0
      || data=='SYNC_MASTER_INFO' || data=='SQL_SLAVE_SKIP_COUNTER' || data == 'RPL_STOP_SLAVE_TIMEOUT'
      || data=='MASTER_INFO_REPOSITORY'||data=='LOG_SLOW_SLAVE_ATTEMPTS'||data=='INIT_SLAVE')
        h = "http://dev.mysql.com/doc/refman/5.6/en/replication-options-slave.html#sysvar_";
                 
    return "<a href=\"" + h + data.toLowerCase() + "\" target=\"_var\">" + data + "</a>";
  }
  return data;
} 

function jqueryVariableValue (data, type, row, meta)
{
  if(type == 'display' && data != null  && typeof data == 'string' && data.length >100)
  {
    
  	  return "<pre>"+breakLongLineFmt(data, 2048)+"</pre>";  
  }
  return data;
}  

function jqueryFormatPreserveSpace (data, type, row, meta)
{
  if(type == 'display' && data != null && typeof data == 'string')
  {
    var ov = data;
	ov = ov.replace('<','&lt;');
	ov = data.replace('>','&gt;');   
    return ov;
  }
  return data;
}

function jqueryFormatNumber(data, type, row, meta)
{
  if(type == 'display' && $.isNumeric(data))
	  return data.toLocaleString();
  return data;
}

//toolyipster callback
function termCB(obj, terms)
{
  if(obj == null)return;
  
  var mydata = "";
  for(var i=0; i<terms.length; i++)
  {
    if(i != 0) mydata += '&';
    mydata += "name=" + terms[i];
  }
  var src = obj.src;
  $.ajax({
       url: "term.html",
       data: mydata,
       type: 'GET',
       dataType: 'json',
       success: function(json)
       {
	     var msg = "";
  	    if(json == null || json.resp == null || json.resp.message == null)msg = "Error";
  	    else msg = json.resp.message;
  	    $(src).tooltipster('update', msg)
       }
  });    
}

function planTermCB(obj)
{
  if(obj == null)return;
  var my = obj.datatable, src = obj.src, rowIdx = obj.row, colIdx = obj.col;
  var selectType = my.getCellValueByColumnName(rowIdx, "SELECT_TYPE");
  var joinType =   my.getCellValueByColumnName(rowIdx, "TYPE");
  var extra = my.getCellValueByColumnName(rowIdx, "EXTRA");
  
  var terms = [
             "plan.select_type."+encodeURIComponent(selectType),
             "plan.join_type."+encodeURIComponent(joinType),
             "plan.extra."+encodeURIComponent(extra)
             ];
  
  termCB(obj, terms);
}

function statTermCB(obj)
{
  if(obj == null)return;
  var my = obj.datatable, src = obj.src, rowIdx = obj.row, colIdx = obj.col;
  var stats = my.getCellValueByColumnName(rowIdx, "VARIABLE_NAME");
  
  var terms = ["mysql_status_"+encodeURIComponent(stats)];
  termCB(obj, terms);
}

function processlistTermCB(obj)
{
  if(obj == null)return;
  var my = obj.datatable, src = obj.src, rowIdx = obj.row, colIdx = obj.col;
  var stats = my.getCellValueByColumnName(rowIdx, "STATE");
  
  var terms = [encodeURIComponent(stats)];
  termCB(obj, terms);
}

//check if a sql statement is select statement.
//used to validate explian plan input.
function isSelect(sql)
{
  if(sql==null||sql=="")return false;
  var sql2 = sql.trim();
  while(sql2.indexOf("/*")==0)//remove comments
  {
    var cidx = sql2.indexOf("*/");
    if(cidx<0)return false;//might be bad sql
	sql2 = sql2.substr(cidx+2);
	sql2 = sql2.trim();
  }
  while(sql2.indexOf("--")==0)//remove comments
  {
    var cidx = sql2.indexOf("\n");
    if(cidx<0)cidx = sql2.indexOf("\r");
    if(cidx<0)return false;//might be bad sql
    sql2 = sql2.substr(cidx+1);
	sql2 = sql2.trim();
  }
  //trim and turn to lower case
  sql2 = sql2.toLowerCase().trim();
  //console.log("sql2: "+sql2+", "+sql2.indexOf("select"));
  return sql2.indexOf("select")==0;//ony if it starts with select
}


function jqueryStylingStatus(obj)
{
    if(obj.datatable == null)return;
    var st = obj.data["STATUS"];
    if(st=='Red')
      $(obj.row).addClass('hilitecell'); 
    else if (st=='Yellow')
      $(obj.row).addClass('hilitecellyellow'); 
    else if (st=='Green')
      $('td', obj.row).eq(obj.datatable.getColumnIndex("STATUS")).addClass('hilitecellgreen');  
}  
function jqueryStylingAlert(obj)
{
    if(obj.datatable == null)return;
    var st = obj.data["END_TS"];
    if(st == null || st == '') //mark red if not ended
      $(obj.row).addClass('hilitecell');
}
  
function jqueryFormatAlert(data, type, row, meta)
{
    if(type == 'display' && meta.col == 0)//only the first column
    {
      var host = row['HOST'];
      var ts = row['TS'];
      var typ = row['ALERT_TYPE'];
      if(typ != 'OFFLINE' && typ != 'DISKUSAGE')
				return "<a href='report/get/" + data + "/" + host +"/" + typ +"/" + escape(ts)
				+ "/"+Math.round(Math.random()*100000000)+".html' target='_report' title='Click to view detail'>"
				+ "<img src='img/save.gif' alt='View' style='vertical-align: bottom;width:12px;height:12px;border:0px;'></a>&nbsp;" + data;
	   else
	     return data;	     
	}
	return data;     	
}

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

/*
  obj fields:
     feature: desc of the feature
     field: checkbox dom id
     url: url to call
     group: dbgroup
     host: db host
     msgField: dom id to display message
*/
function enableDisableFeature(obj)
{
  if(mydom(obj.field).checked 
    && !confirm("Are you sure you want to enable " + obj.feature + " for this server?"))
    return;
  else if(!mydom(obj.field).checked 
    && !confirm("Are you sure you want to disable " + obj.feature + " for this server?"))
    return;
  var cmd = mydom(obj.field).checked?(obj.field+"_yes"):(obj.field + "_no");
  var mydata = "group="+escape(mydomval(obj.group))+"&host="+escape(mydomval(obj.host)) 
               + "&cmd="+cmd 
               + "&seed=" + Math.random();
      $.ajax({
        url: obj.url,
        method: 'GET',
        data: mydata,
        success:function(jsonObj)
        {
              if(jsonObj==null)
              {
              	reportStatus(true, obj.msgField, 'Failed to update " + obj.feature + " settings.');
              }else
              {
                reportStatus((jsonObj.resp.status != 0), obj.msgField, jsonObj.resp.message);
              }
        }
      });

}