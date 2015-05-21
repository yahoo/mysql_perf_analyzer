<?xml version="1.0" encoding="UTF-8"?>
<%@page trimDirectiveWhitespaces="true"%>
<%@page contentType="text/xml" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
   Copyright 2015, Yahoo Inc.
   Copyrights licensed under the Apache License.
   See the accompanying LICENSE file for terms.
--%>
<resp status="${status}">
 <c:if test="${results!=null}" > 
  <results>
    <columnInfo>
      <c:forEach var="col" items="${results.columnDescriptor.columns}"><column name="${col.name}" /></c:forEach>
    </columnInfo>
    <rows>
      <c:forEach var="row" items="${results.rows}">
        <row><c:forEach var="col" items="${row.columns}"><value>${col}</value></c:forEach>
        </row>
	  </c:forEach>
    </rows>
  </results>
 </c:if> 
</resp>