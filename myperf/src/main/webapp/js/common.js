/*
 *  Copyright 2015, Yahoo Inc.
 *  Copyrights licensed under the Apache License.
 *  See the accompanying LICENSE file for terms.
 */
 //so we don't send clear password for login
function auth()
{
    var s1;    
    try
    {
	    s1 = document.getElementById("ts").value +":"
            +md5(document.getElementById("name").value+":"+document.getElementById("pd").value)+":"
            +document.getElementById("ars").value;
    }catch(e){console.log(e); return false;}
    document.getElementById("s").value = md5(s1);
    document.getElementById("pd").value = "";
    return true;
}

//style: none, block, inline-block, etc
function showHideOne(nodeId, showStyle)
{
  document.getElementById(nodeId).style.display=showStyle;
}

function mydom(id)
{
 	return document.getElementById(id);
}
function mydomval(id)
{
 	return document.getElementById(id).value;
}

function showHide(allDivs, showDiv, displayStyle)
{
	if(allDivs && allDivs.length>0)
	{
		for(var i=0;i<allDivs.length;i++)
		{
		  if(allDivs[i]==showDiv)
			mydom(allDivs[i]).style.display = displayStyle;
		  else mydom(allDivs[i]).style.display = "none";
		}
	}
}
function hidePopup()
{
  	document.getElementById("popup").style.display="none";
  	return false;
}
function toggleDiv(div)
{
  var d = mydom(div);
  if(d.style.display=='none')d.style.display = 'block';
  else d.style.display = 'none';  
}

function isEmpty(val)
{
	if(val==null||val=='')return true;
	for(var i=0;i<val.length;i++)
	{
		var c= val.charAt(i);if(c!=' '&&c!='\t'&&c!='\r'&&c!='\n')return false;		
	}
	return true;
}
function removeEmptyLine(val)
{
	if(val==null)return val;
	if(val.indexOf('\n')<0)return val;
	var str = "";
	var n = val.split("\n");
	var isFirst = true;
	for(var i=0;i<n.length;i++)
	{
		if(isEmpty(n[i]))continue;
		if(!isFirst)str += '\n';
		isFirst = false;
		str += n[i];
	}
	return str;
}

function macthPwd(p1, p2, msgField)
{
  if(mydomval(p1).value != mydomval(p2).value)
    $('#'+msgField).text("Password mismatch");
  else 
    $('#'+msgField).text("");
}
	
function setSelect(selectDomId, targetVal)
{
  var src = mydom(selectDomId);
  var len = src.options.length;
  for(var i=0; i<len; i++)
  {
    if(src.options[i].value == targetVal)
    {
      src.selectedIndex = i;
      return;
    }
  }
}
