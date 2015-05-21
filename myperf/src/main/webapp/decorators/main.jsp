<%@page trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator"%>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<!DOCTYPE html>
<html lang="en">
<head>
<title>MySQL Database Performance Tool: <decorator:title /></title>
<%@ include file="/includes/style.jsp"%>
<decorator:head />
</head>
<body <decorator:getProperty property="body.onload" writeEntireProperty="true" />>
<%@ include file="/includes/header.jsp"%>
<div style="position:absolute;top:40px;left:10px;z-index:1;"><decorator:body />
<%@ include file="/includes/footer.jsp"%>
</div>
</body>
</html>