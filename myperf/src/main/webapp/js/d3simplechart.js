/*
 *  Copyright 2015, Yahoo Inc.
 *  Copyrights licensed under the Apache License.
 *  See the accompanying LICENSE file for terms.
 */
 //our simple d3js simple chart for metrics

var DEFAULT_MARGIN = {top: 20, right: 60, bottom: 20, left: 74}

function getMetricName(m){
  if (m == null)return "";
  var idx = m.lastIndexOf(".");
  if(idx<0)return m;
  return m.substring(idx+1);
}
/*
  Use this to calculate p-th ctpencental, for example 95-th percental
*/
function percental(data, p)
{
  var newData = [];
  var i=0;
  for(i=0;i<data.length;i++)newData[i] = data[i];
  newData.sort(function(a,b){return a-b;});//without provide a function, it is sort alphanetically
  var n = Math.round((p/100)*data.length + 0.5);
  return newData[n-1];//we are zero based
}

/*
 Draw multiple charts based on chartInfo
 Inside chartInfo
   topUrl: top level url path, "metrics/get". Final URL follows this pattrern
     topUrl/dbGroup/dbHost/metrics names/start_datetime/end_datetime/random.html
   dbGroup: 
   dbHost:
   start_datetime:
   end_datetime:
   messageCB: a function to handle message callback
   				input: messageStr, isError
   statusCB: a function to notify the chart status, take input (chart.domid, status)
             status: 0 no data or failed, 1: succeed
   scatterplotObj: also with scattered plot, can be used when there is only two charts with each has one metric. 
     domid: scatterplot container dom id
     width: container width
     height: container height
     color:     
   charts: a list of the following
     domid: div id to draw the chart which include one or more metrics
     label: chart label
     width: chart width
     height: chart height
     margin: chart margin
     colors: colors for each line, if specified
     gridCss: css class for grid
     lineCss: css class for line
     tootipCss: css class for tool tip
     lengendCss: css class for legend (metric lables)
     metrics: a list of the following
       name: metric full name
       label: for display/legend, and display measurement unit info
       inc: if value is incremented, set to 1, otherwise 0
       avg: time interval used to calculate average with inc, sec/min/hour, default to min 
       adj: value adjustment, for example, make bytes to KB/MB 
*/
function simpleCharts(chartInfo)
{
  //right now, asume all required information are provided
  //construct URL first
   var url = chartInfo.topUrl +"/" + chartInfo.dbGroup + "/" + chartInfo.dbHost + "/";
   var first = true;
   for(var i = 0; i < chartInfo.charts.length; i++)
   {
     var chart = chartInfo.charts[i];
     for(var j=0; j<chart.metrics.length; j++)
     {
       if(!first) url += ",";
       first = false;
       url += chart.metrics[j].name;
     }
   }
   url += "/" + escape(chartInfo.start_datetime)
              + "/" + escape(chartInfo.end_datetime)
              +"/"+Math.round(Math.random()*100000000)+".html";
   
   //retrieve data           
   d3.json(url,
     function(error, json){
       if(error)
       {
         console.warn(error);
         return;
       }
       if(json==null||json.resp==null||json.resp.results==null
         ||json.resp.results.results==null||json.resp.results.results<=1)
       {
         //notify error
         if(chartInfo.messageCB != null)chartInfo.messageCB("No data or not enough data available for charts.", true);
          return;
       }
       chartInfo.messageCB("", false);//clear chart message
       //draw chart one by one
       for(var i = 0; i < chartInfo.charts.length; i++)
       {
         var chart = chartInfo.charts[i];
         var ret = drawOneChart(chart, json);
         if (chartInfo.statusCB != null)
           chartInfo.statusCB(chart.domid, ret);
       }
       //if we need scatterplot
       if(chartInfo.scatterplotObj != null && chartInfo.charts.length == 2)
       {
         scatterplot2(chartInfo.scatterplotObj.domid, chartInfo.scatterplotObj.width, chartInfo.scatterplotObj.height, 
         null, json, "scatterplot", 
         [chartInfo.charts[0].metrics[0], chartInfo.charts[1].metrics[0]], chartInfo.scatterplotObj.colors);       
       }
     }//callback function
   ); //d3.json
}


/*
  dom, w, h, margin, //container, size, and margin, jsondata, title, metrics, colors
  Input:
    chartInfo: information about a single chart, see input information from function simpleCharts,
               one member from  the list charts
    jsondata: retrieved metrics data in json format           
*/
function drawOneChart(chartInfo, jsondata)
{
  if(jsondata==null||jsondata.resp==null
    ||jsondata.resp.results==null||jsondata.resp.results.results==null)
  {
    return 0;
  }
  
  var margin = chartInfo.margin;
  if(margin == null)margin = DEFAULT_MARGIN;

  d3.select("#"+chartInfo.domid).select("svg").remove();//remove old chart

  var parseDate = d3.time.format("%Y%m%d%H%M%S").parse;

  //now extract our data
  var data = [];
  var mattr = [];//metrics attribute
  var times = [], avg = [], ts = [];
  var dlist = jsondata.resp.results.results;//metrics data
  var mdesc = jsondata.resp.results.metrics;//metrics descriptor from data
  var keys = jsondata.resp.results.keys; //some metrics might have multiple keys/entities, such as disk metrics
  var num_keys = keys!=null?keys.length: 0;
  var len = dlist.length;
  console.log("Data size for " + chartInfo.label + " " + len);
  var i=0, j = 0, inc = 0; //inc=1 indicate some data are incremental data
  
  //initialize and exract metrics attribute
  var metrics = chartInfo.metrics;
  for(j = 0; j < metrics.length; j++)
  {
    var m = metrics[j]; //meta data provided by chartInfo will overwrite whatever from data set
    var mname = m.name; //metric name
    var mdata = mdesc != null? mdesc[mname]: null; //metric meta data from data
    if(num_keys == 0)
	    data[j] = [];//for each metric, there will be one row
	else
	{
	   for(var k = 0; k<num_keys; k++)
	    data[j*num_keys + k] = [];    
	}
    mattr[j] = new Object();    
    metrics[j].shortName = metrics[j].name;
    if(mdata != null)
    {
      if(mdata["shortName"] != null)
        metrics[j].shortName = mdata["shortName"];
      if (m.inc == null)
    	mattr[j].inc = eval(mdata["inc"]);
      else
        mattr[j].inc = m.inc;
      
      if (m.avg == null)
    	mattr[j].avg = mdata["avg"];
      else
        mattr[j].avg = m.avg;        	
      if(mattr[j].inc == 1 && mattr[j].avg == null)mattr[j].avg="min";
   
      if(m.adj == null)
		mattr[j].adj = eval(mdata["adj"]);
	  else
	    mattr[j].adj = m.adj;
	  
	  if(m.label == null)  
    	mattr[j].display = getMetricName(mname) + "("+ mdata["display"]+")";
      else
        mattr[j].display = m.label;
      if (mattr[j].display == null && mattr[j].inc == 1)
          mattr[j].display = getMetricName(mname) + "(/"+ mattr[j].avg.toUpperCase() +")";
       else if (mattr[j].display == null)
          mattr[j].display = getMetricName(mname);    
    }else //if not set, treat as inc, avg by one minute
    {
        mattr[j].inc = 1;
        mattr[j].avg = "min";
        mattr[j].adj = 1;
        mattr[j].display = getMetricName(mname) +"(/MIN)";
    }
    if(mattr[j].inc==1)inc = 1;
  }
  
  //reprocess timestamp and data
  for(i=0;i<len;i++)
  {
    var srcrow = dlist[i];//source row, each row has one timestamp
    times[i] = ""+srcrow["TS"];//extract time stamp
    ts[i] = parseDate(times[i]);
    for(j=0;j<metrics.length;j++)
    {
      if(num_keys == 0)
      	data[j][i] = srcrow[metrics[j].shortName];//extract metric data
      else
      {
        for(var k = 0; k <num_keys; k++)
          data[j*num_keys + k][i] = srcrow[metrics[j].shortName][keys[k].shortName];
      }
    }
  }

  //reprocess data
  //if any metric is incremental, we need calculate the diff and shift the other
  if(inc==1)
  {
    //we need get the diff of the data
    for(j=0;j<metrics.length;j++)
    {
      if(mattr[j].inc == 1)
      {
        var avgAdjust = 1;
        if(mattr[j].avg=='sec')avgAdjust = 1000;
        else if(mattr[j].avg=='min')avgAdjust = 60000;
        else if(mattr[j].avg=='hour')avgAdjust = 3600000;
        for(i=0;i<len-1;i++)
        {
          var interval = ts[i+1].getTime() - ts[i].getTime();
          if(num_keys == 0)
          {
            //data[j][i] = data[j][i+1]>=data[j][i]?data[j][i+1]-data[j][i]:data[j][i+1];
            //note directly use raw data can have unwanted spike when there is issue with data collection
            if(data[j][i+1]>=data[j][i])data[j][i] = data[j][i+1]-data[j][i];
            else if(i>0) data[j][i] = data[j][i-1];
            else   data[j][i]  = 0;
              
            if(interval>0)
               data[j][i] = eval((data[j][i]*avgAdjust*mattr[j].adj/interval).toFixed(3));
          }else
          {
            for(var k = 0; k<num_keys; k++)
            {
              var jk = j*num_keys+k;
              //data[jk][i] = data[jk][i+1]>=data[jk][i]?data[jk][i+1]-data[jk][i]:data[jk][i+1];
              if(data[jk][i+1]>=data[jk][i])data[jk][i] = data[jk][i+1]-data[jk][i];
              else if(i>0) data[jk][i] = data[jk][i-1];
              else data[jk][i] = 0;
              
              if(interval>0)
                data[jk][i] = eval((data[jk][i]*avgAdjust*mattr[j].adj/interval).toFixed(3));
            }
          }
        }//for loop
      }//if(mattr[j].inc==1)
      else
      {
        for(i=0;i<len-1;i++)
        {
          if(num_keys == 0)
	        data[j][i] = eval((data[j][i+1]*mattr[j].adj).toFixed(3));//otherwise, shift
	      else
	      {
            for(var k = 0; k<num_keys; k++)
            {
              var jk = j*num_keys+k;
	          data[jk][i] = eval((data[jk][i+1]*mattr[j].adj).toFixed(3));//otherwise, shift
            }	      
	      }
	    }
      }
      if(num_keys == 0)
        data[j].length = data[j].length - 1;
      else
      {
        for(var k = 0; k<num_keys; k++)
        {
          var jk = j*num_keys+k;
          data[jk].length = data[jk].length - 1;
        }
      }
    }
    for(i=0;i<len-1;i++)
    {
      times[i] = times[i+1];
    }
    times.length = times.length - 1;
  }//inc = 1
  else
  {
    for(j=0;j<metrics.length;j++)
    {
      for(i=0;i<data[j].length;i++)
      {
        if(num_keys == 0)
          data[j][i] = eval((data[j][i]*mattr[j].adj).toFixed(3));
        else
        {
          for(var k = 0; k<num_keys; k++)
          {
            var jk = j*num_keys+k;
            data[jk][i] = eval((data[jk][i]*mattr[j].adj).toFixed(3));
          }
        }
      }
    }
    //we still need shift by 1, otherwise we cannot align with inc=1
    for(i=0;i<len-1;i++)
    {
      times[i] = times[i+1];
    }
    times.length = times.length - 1;
    for(j=0;j<metrics.length;j++)
    {
      for(i=0;i<data[j].length;i++)
      {
        if(num_keys == 0)
          data[j][i] = data[j][i+1];
        else
        {
          for(var k = 0; k<num_keys; k++)
          {
            var jk = j*num_keys+k;
            data[jk][i] = data[jk][i+1];
          }
        }
      }
      data[j].length = data[j].length -1;
      for(var k = 0; k<num_keys; k++)
        data[jk].length = data[jk].length -1;
    }
  }
  //calculate average
  for(j=0;j<data.length;j++)
  {
  	var sum = 0;
  	for(i=0;i<data[j].length;i++)
          sum+= data[j][i];
    if(data[j].length>0)
    {
      avg[j] = eval((sum/data[j].length).toFixed(3));
      ret = 1;
     }
     else
       avg[j] = 0;
  }
  
  var width = chartInfo.width - margin.left - margin.right;
  var height = chartInfo.height - margin.top - margin.bottom;
  var parseDate = d3.time.format("%Y%m%d%H%M%S").parse;
  var formatTime = d3.time.format("%Y-%m-%d %H:%M");

  //axis
  var x = d3.time.scale().range([0, width]);
  x.domain(d3.extent(times, function(d) { return parseDate(d); }));
  var yExtents = d3.extent(d3.merge(data), function (d) { return d; });
  
  var y = d3.scale.linear().domain([0,yExtents[1]*1.2]).range([height, 0]);
  if(yExtents[1]==0)
    y = d3.scale.linear().domain([0,1]).range([height, 0]);
  var xAxis = d3.svg.axis().scale(x).orient("bottom").ticks(10).tickFormat(d3.time.format('%H:%M'));
  var yAxis = d3.svg.axis().scale(y).orient("left").tickFormat(d3.format("s"));

  function make_x_axis()
  {
    return d3.svg.axis().scale(x).orient("bottom");
  }
  function make_y_axis()
  {
    return d3.svg.axis().scale(y).orient("left");
  }

  var colors = chartInfo.colors;
  //use default colors
  var nk = num_keys>0?num_keys:1;
  if(colors==null||colors.length==0)
  {
    colors = [];
    var c = d3.scale.category10();
    if(metrics.length * nk > 10)
      c = d3.scale.category20();      
    for(var i=0;i<metrics.length*nk;i++)
      colors[i] = c(i);
  }  

  var svg = d3.select("#"+chartInfo.domid).append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

  var gridCss = chartInfo.gridCss, lineCss = chartInfo.lineCss, tooltipCss = chartInfo.tooltipCss, legendCss = chartInfo.legendCss;
  if(gridCss == null)gridCss ="grid";
  if(lineCss == null)lineCss = "line";
  if(tooltipCss == null)tooltipCss = "tooltip";
  if(legendCss == null)legendCss = "legend";
  if(metrics.length>=1)
  {
    var line = d3.svg.line()
      .x(function(d, i) { return x(parseDate(times[i])); })
      .y(y);
    
    svg.append("g")
      .attr("class",gridCss)
      .attr("transform","translate(0,"+height+")")
      .call(make_x_axis()
            .tickSize(-height, 0,0)
            .tickFormat(""));

    svg.append("g")
      .attr("class",gridCss)
      .call(make_y_axis()
            .tickSize(-width, 0,0)
            .tickFormat(""));

    var lines = svg.selectAll( "q" )//using an non-exist element tag q so that selectAll returns empty selection and path data will be append to svg/g
      .data( data )
      .enter().append("path")
      .attr("class", lineCss)
      .attr("d", line)
      .attr("style", function(d, i) {return "stroke: " + colors[i];});
  }
  
  //tooltip
  var div = d3.select("body").append("div")   
    .attr("class", tooltipCss)         
    .style("opacity", 0);
  
  //axis
  svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height + ")")
      .call(xAxis);

  svg.append("g")
      .attr("class", "y axis")
      .call(yAxis);

  //for mouseover
  for(var i1=0;i1<data.length;i1++)
  {
    svg.selectAll("dot")    
        .data(data[i1])
    .enter().append("circle")                               
        .attr("r", 5)       
        .attr("cx", function(d,i) { var a = x(parseDate(times[i]));return a; })
        .attr("cy", function(d,i) { return y(data[i1][i]); })     
        .attr("fill", function(d, i) {
		  return colors[i1]; 
	     })
	    .style("opacity", 0)
	    .on("mouseover", function(d, i) {
	        d3.select(this).transition()        
                .duration(200)      
                .style("opacity", 1);
            div.transition()        
                .duration(200)      
                .style("opacity", 1);      
            div.html("<u>"+formatTime(parseDate(times[i])) + "</u><br/>"  + d3.format(",")(d))  
                .style("z-index", "1000")     
                .style("left", (d3.event.pageX+20) + "px")     
                .style("top", (d3.event.pageY - 28) + "px");    
            })                  
        .on("mouseout", function(d) {       
	        d3.select(this).transition()        
                .duration(500)      
                .style("opacity", 0);
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });
   }
   //metrics labels
   var legend = svg.append("g")
      .attr("class", legendCss)
	  .attr("height", 100)//.attr("width", 100)
      .attr('transform', 'translate(-20,0)');
  
  legend.selectAll('rect')
      .data(num_keys==0?metrics:keys)
      .enter()
      .append("rect")
	  .attr("x", 25)
      .attr("y", function(d, i){ return i *  20;})
	  .attr("width", 10)
	  .attr("height", 10)
	  .style("fill", function(d, i) { 
        var color = colors[i];
        return color;
      })
      
    legend.selectAll('text')
      .data(num_keys==0?metrics:keys)
      .enter()
      .append("text")
	  .attr("x", 40)
      .attr("y", function(d, i){ return i *  20 + 9;})
	  .text(function(d, i) {
        var text = "";
        if(num_keys == 0)
          text = mattr[i].display;
        else
        {
          var ki = i%num_keys;
          var mi = (i - ki)/num_keys;
          if(mattr.length>=mi)
            text = mattr[mi].display+"-"+keys[ki].name;
          else
            text = keys[ki].name;
        }
        if(num_keys==0)
          text = text +", AVG: "+d3.format(",")(avg[i]) + ", 95%: "+d3.format(",")(percental(data[i], 95));
        return text;
      });
    
    return 1;
};//drawOneChart


//dom: container id
//w: width, h height
//margin, such as var margin = {top: 20, right: 20, bottom: 30, left: 50}
//jsondata: data in our json format
//title: chart title or subgroup
//metrics: names of the metric we want, in array of 2
//colors:the color for each metrics
function scatterplot2(dom, w, h, margin, //container, size, and margin
jsondata, title, metrics, colors)
{
  if(jsondata==null||jsondata.resp==null
    ||jsondata.resp.results==null||jsondata.resp.results.results==null
    ||metrics == null ||metrics.length<2)
  {
    return;
  }
  if(margin==null)margin = DEFAULT_MARGIN;
  d3.select("#"+dom).select("svg").remove();
  //now extract our data
  var xdata = [], ydata = [], mattr = [];
  var dlist = jsondata.resp.results.results;
  var mdesc = jsondata.resp.results.metrics;//metrics descriptor from data
  var keys = jsondata.resp.results.keys; //some metrics might have multiple keys/entities, such as disk metrics
  var len = dlist.length;
  console.log("Data size for "+title+" "+len);
  var i=0;
  var j = 0;
  var inc = 0;
  
  for(j = 0; j < metrics.length; j++)
  {
    var m = metrics[j]; //meta data provided by chartInfo will overwrite whatever from data set
    var mname = m.name; //metric name
    var mdata = mdesc != null? mdesc[mname]: null; //metriuc meta data from data

    mattr[j] = new Object();
    metrics[j].shortName = metrics[j].name;
    if(mdata != null)
    {
      if(mdata["shortName"]!=null)
        metrics[j].shortName = mdata["shortName"];
      if (m.inc == null)
    	mattr[j].inc = eval(mdata["inc"]);
      else
        mattr[j].inc = m.inc;      
    }else //if not set, treat as inc, avg by one minute
    {
        mattr[j].inc = 1;
    }
    if(mattr[j].inc==1)inc = 1;
  }
  
  for(i=0;i<len;i++)
  {
    var srcrow = dlist[i];
    xdata[i] = srcrow[metrics[0].shortName];//extract metric data
    ydata[i] = srcrow[metrics[1].shortName];//extract metric data
  }
  if(inc == 1)
  {
      for(i=0;i<len-1;i++)
      {
        if(mattr[0].inc == 1)
	        xdata[i] = xdata[i+1]>=xdata[i]?xdata[i+1]-xdata[i]:xdata[i+1];
	    else
	        xdata[i] = xdata[i+1];//otherwise shift
	    if(mattr[1].inc == 1)
          ydata[i] = ydata[i+1]>=ydata[i]?ydata[i+1]-ydata[i]:ydata[i+1];
        else
          ydata[i] = ydata[i+1];  
      }
      xdata.length = xdata.length - 1;
      ydata.length = ydata.length - 1;
  }
  var width = w - margin.left - margin.right;
  var height = h - margin.top - margin.bottom;

  
  var x = d3.scale.linear().domain([0, d3.max(xdata)]).range([0, width]);
  var y = d3.scale.linear().domain([0,d3.max(ydata)]).range([height, 0]);
  if(d3.max(xdata)<=0)
    x = d3.scale.linear().domain([0, 1]).range([0, width]);
  if(d3.max(ydata)<=0)
    y = d3.scale.linear().domain([0,1]).range([height, 0]);
  var xAxis = d3.svg.axis().scale(x).orient("bottom").tickFormat(d3.format("s"));
  var yAxis = d3.svg.axis().scale(y).orient("left").tickFormat(d3.format("s"));

  function make_x_axis()
  {
    return d3.svg.axis().scale(x).orient("bottom");
  }
  function make_y_axis()
  {
    return d3.svg.axis().scale(y).orient("left");
  }

  var svg = d3.select("#"+dom).append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

  svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height + ")")
      .call(xAxis);

  svg.append("g")
      .attr("class", "y axis")
      .call(yAxis);

  var div = null;
  
  div = d3.select("body").append("div")   
    .attr("class", "tooltip")         
    .style("opacity", 0);
        

  svg.append("g").selectAll("scatter-dots")
     .data(ydata)
     .enter().append("svg:circle")
     .attr("cy", function(d){return y(d);})
     .attr("cx", function(d,i){return x(xdata[i]);})
     .attr("r", 3)
     .style("opacity", 0.6)
     .style("stroke", "red")
     .style("fill", "white")
     .on("mouseover", function(d, i) {
	        div.transition()        
                .duration(200)      
                .style("opacity", 1);      
            div.html("("+xdata[i] + ", "  + ydata[i]+")")  
                .style("z-index", "1000")     
                .style("left", (d3.event.pageX+20) + "px")     
                .style("top", (d3.event.pageY - 28) + "px");    
            })                  
        .on("mouseout", function(d) {       
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });     	  
  svg.append("g")
     .attr("class","grid")
     .attr("transform","translate(0,"+height+")")
     .call(make_x_axis()
        .tickSize(-height, 0,0)
        .tickFormat("")
     );

  svg.append("g")
     .attr("class","grid")
     .call(make_y_axis()
        .tickSize(-width, 0,0)
        .tickFormat("")
     );

     
   var legend = svg.append("g")
      .attr("class", "legend")
	  .attr("height", 100)//.attr("width", 100)
      .attr('transform', 'translate(-20,0)');
  
  var m = [];
  m[0] = "X - "+getMetricName(metrics[0].name);
  m[1] = "Y - "+getMetricName(metrics[1].name);
  m[2] = "CORRELATION - "+corr(xdata, ydata);
  legend.selectAll('rect')
      .data(m)
      .enter()
      .append("rect")
	  .attr("x", 25)
      .attr("y", function(d, i){ return i *  20;})
	  .attr("width", 10)
	  .attr("height", 10)
	  .style("fill", function(d, i) { 
        var color = "blue";
        return color;
      })
      
    legend.selectAll('text')
      .data(m)
      .enter()
      .append("text")
	  .attr("x", 40)
      .attr("y", function(d, i){ return i *  20 + 9;})
	  .text(function(d, i) {
        var text = m[i];
        return text;
      });
  
};

//calculate correlation between xdata and ydara
function corr(xdata, ydata)
{
  if(xdata==null ||xdata.lengtgh<=1)return 0;
  var sumx = 0, sumy = 0, sqx = 0, sqy = 0, xy = 0, len = xdata.length;
  for(i=0;i<xdata.length;i++)
  {
    sumx += xdata[i];
    sumy += ydata[i];
    sqx += xdata[i]*xdata[i];
    sqy += ydata[i]*ydata[i];
    xy += xdata[i]*ydata[i];        
  }
  
  var avgx = sumx/len;
  var avgy = sumy/len;
  var sigmax = Math.sqrt((sqx - len*avgx*avgx)/(len-1));
  var sigmay = Math.sqrt((sqy - len*avgy*avgy)/(len-1));
  if(sigmax==0||sigmay==0)return 0;
  var cov = xy - avgx*sumy - avgy*sumx + len*avgx*avgy;
  return (cov/(sigmax*sigmay*(len - 1))).toFixed(3);
}