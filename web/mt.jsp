<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement,java.util.logging.Logger"   buffer="60kb" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
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

<%
  Logger logger = Logger.getLogger("ru.org.linux");

  Template tmpl = Template.getTemplate(request);
%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>
<%
  if (!tmpl.isModeratorSession()) {
    throw new AccessViolationException("Not moderator");
  }
  %>
<title>Перенос темы</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
  Connection db = null;

  try {
    int msgid = new ServletParameterParser(request).getInt("msgid");
    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    Message msg = new Message(db, msgid);

    Statement st1 = db.createStatement();

    if ("POST".equals(request.getMethod())) {
      int newgr = new ServletParameterParser(request).getInt("moveto");

      Group newGrp = new Group(db, newgr);
      String url = msg.getUrl();

      PreparedStatement movePst = db.prepareStatement("UPDATE topics SET groupid=? WHERE id=?");
      movePst.setInt(1, newGrp.getId());
      movePst.setInt(2, msgid);

      movePst.executeUpdate();

      if (url != null && !newGrp.isLinksAllowed() && !newGrp.isImagePostAllowed()) {
        String sSql = "UPDATE topics SET linktext=null, url=null WHERE id=" + msgid;

        String title = msg.getGroupTitle();
        String linktext = msg.getLinktext();

        st1.executeUpdate(sSql);

        /* if url is not null, update the topic text */
        PreparedStatement pst1 = db.prepareStatement("UPDATE msgbase SET message=message||? WHERE id=?");

        String link;
        if (msg.isLorcode()) {
          link = "\n[url=" + url + ']' + linktext + "[/url]\n";
        } else {
          link = "<br><a href=\"" + url + "\">" + linktext + "</a>\n<br>\n";
        }

        if (msg.isLorcode()) {
          pst1.setString(1, '\n' + link + "\n[i]Перемещено " + session.getValue("nick") + " из " + title + "[/i]\n");
        } else {
          pst1.setString(1, '\n' + link + "<br><i>Перемещено " + session.getValue("nick") + " из " + title + "</i>\n");
        }

        pst1.setInt(2, msgid);
        pst1.executeUpdate();
      }

      logger.info("topic " + msgid + " moved" +
              " by " + session.getValue("nick") + " from news/forum " + msg.getGroupTitle() + " to forum " + newGrp.getTitle());
      db.commit();
    } else {
%>
перенос сообщения <strong><%= msgid %></strong> в форум:
<%
      ResultSet rs = st1.executeQuery("SELECT id,title FROM groups WHERE section=2 ORDER BY id");
%>
<form method="post" action="mt.jsp">
<input type=hidden name="msgid" value='<%= msgid %>'>
<select name="moveto">
<%
            while (rs.next()) {
                out.println("<option value='"+rs.getInt("id")+"'>"+rs.getString("title")+"</option>");
            }
            rs.close();
            st1.close();
            out.println("</select>\n<input type='submit' name='move' value='move'>\n</form>");
        }
    } finally {
        if (db!=null) {
          db.close();
        }
    }
%>

  <jsp:include page="WEB-INF/jsp/footer.jsp"/>
