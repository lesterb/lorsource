<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.util.Random" %>
<%@ page import="ru.org.linux.site.AccessViolationException" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ page import="ru.org.linux.site.UserErrorException" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page contentType="text/html;charset=koi8-r" language="java" errorPage="/error.jsp" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<title>usermod</title>
<%= tmpl.DocumentHeader() %>
<%
  if (!tmpl.isModeratorSession()) {
    throw new IllegalAccessException("Not authorized");
  }

  String action = tmpl.getParameters().getString("action");
  int id = tmpl.getParameters().getInt("id");

  if (!request.getMethod().equals("POST")) {
    throw new IllegalAccessException("Invalid method");
  }

  Connection db = null;

  try {
    db = tmpl.getConnection("usermod");
    db.setAutoCommit(false);

    Statement st = db.createStatement();

    User user = new User(db, id);

    if (action.equals("block")) {
      if (!user.isBlockable()) {
        throw new AccessViolationException("������������ " + user.getNick() + " ������ �������������");
      }

      st.executeUpdate("UPDATE users SET blocked='t' WHERE id=" + id);
      tmpl.getLogger().notice("usermod.jsp", "User " + user.getNick() + " blocked by " + session.getValue("nick"));
    } else if (action.equals("unblock")) {
      if (!user.isBlockable()) {
        throw new AccessViolationException("������������ " + user.getNick() + " ������ ��������������");
      }

      st.executeUpdate("UPDATE users SET blocked='f' WHERE id=" + id);
      tmpl.getLogger().notice("usermod.jsp", "User " + user.getNick() + " unblocked by " + session.getValue("nick"));
    } else if (action.equals("remove_userpic")) {
      if (user.canModerate()) {
        throw new AccessViolationException("������������ " + user.getNick() + " ������ ������� ��������");
      }

      if (user.getPhoto() == null) {
        throw new AccessViolationException("������������ " + user.getNick() + " �������� �� �����");
      }

      st.executeUpdate("UPDATE users SET photo=null WHERE id=" + id);
      st.executeUpdate("UPDATE users SET score=score-10 WHERE id=" + id);
      tmpl.getLogger().notice("whois.jsp", "Clearing " + user.getNick() + " userpic");
    } else if (action.equals("remove_userinfo")) {
      if (user.canModerate()) {
        throw new AccessViolationException("������������ " + user.getNick() + " ������ ������� ��������");
      }

      tmpl.getObjectConfig().getStorage().updateMessage("userinfo", String.valueOf(id), "");      

      st.executeUpdate("UPDATE users SET score=score-10 WHERE id=" + id);
      tmpl.getLogger().notice("whois.jsp", "Clearing " + user.getNick() + " userinfo");
    } else {
      throw new UserErrorException("Invalid action=" + HTMLFormatter.htmlSpecialChars(action));
    }

    Random random = new Random();

    response.setHeader("Location", tmpl.getMainUrl() + "/whois.jsp?nick=" + URLEncoder.encode(user.getNick()) + "&nocache=" + random.nextInt());
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

    db.commit();
  } finally {
    if (db != null) {
      db.close();
    }
  }

%>