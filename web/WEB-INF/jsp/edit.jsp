<%@ page contentType="text/html; charset=utf-8"%>
<%@ page
    import="java.sql.Connection,ru.org.linux.site.LorDataSource,ru.org.linux.site.Message"
      buffer="200kb" %>
<%@ page import="ru.org.linux.site.Tags" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Редактирование сообщения</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<%
  Connection db = null;
  try {
    db = LorDataSource.getConnection();
    Message message = (Message) request.getAttribute("message");
    Message newMsg = (Message) request.getAttribute("newMsg");

%>
<c:if test="${info!=null}">
  <h1>${info}</h1>
</c:if>
<c:if test="${info==null}">
  <h1>Редактирование</h1>
</c:if>

<div class=messages>
  <lor:message db="<%= db %>" message="${newMsg}" showMenu="false" user="<%= Template.getNick(session) %>"/>
</div>

<form action="edit.jsp" name="edit" method="post">
  <input type="hidden" name="msgid" value="${message.id}">
  Заголовок новости :
  <input type=text name=title size=40 value="<%= newMsg.getTitle()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getTitle()) %>" ><br>

  <br>
  <textarea name="newmsg" cols="70" rows="20"><%= newMsg.getMessage() %></textarea>
  <br><br>
  <% if (message.isHaveLink()) {
  %>
  Текст ссылки:
  <input type=text name=linktext size=60 value="<%= newMsg.getLinktext()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getLinktext()) %>"><br>
  <%
    }
  %>
  <% if (message.isHaveLink()) {
  %>
  Ссылка :
  <input type=text name=url size=70 value="<%= newMsg.getUrl()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getUrl()) %>"><br>
  <% } %>

  <c:if test="${group.moderated}">
  <%
    String result = newMsg.getTags().toString();
  %>
  Теги:
  <input type="text" name="tags" id="tags" value="<%= result %>"><br>
  Популярные теги: <%= Tags.getEditTags(Tags.getTopTags(db)) %> <br>
    </c:if>
  <br>

  <input type="submit" value="отредактировать">
  &nbsp;
  <input type=submit name=preview value="Предпросмотр">
</form>
<%

    // out.print("<-- or msgid is null -->\n");
  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>