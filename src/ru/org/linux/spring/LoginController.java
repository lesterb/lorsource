/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.spring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;

@Controller
public class LoginController {
  private boolean isAjax(HttpServletRequest request) {
    String header = request.getHeader("X-Requested-With");

    return header != null && "XMLHttpRequest".equals(header);
  }

  @RequestMapping(value="/login.jsp", method= RequestMethod.GET)
  public ModelAndView loginForm() throws Exception {
    return new ModelAndView("login-form");
  }

  @RequestMapping(value = "/login.jsp", method = RequestMethod.POST)
  public ModelAndView doLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Connection db = null;
    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();

    boolean ajax = isAjax(request);

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);
      String nick = request.getParameter("nick");
      if (nick == null || "".equals(nick)) {
        return new ModelAndView(ajax ? "login-xml" : "login-form", Collections.singletonMap("error", "Не указан nick"));
      }

      User user = User.getUser(db, nick);

      if (!user.isActivated()) {
        String activation = request.getParameter("activate");

        if (activation == null) {
          return new ModelAndView(ajax ? "login-xml" : "login-form", Collections.singletonMap("error", "Требуется активация"));
        }

        String regcode = user.getActivationCode(tmpl.getSecret());

        if (regcode.equals(activation)) {
          PreparedStatement pst = db.prepareStatement("UPDATE users SET activated='t' WHERE id=?");
          pst.setInt(1, user.getId());
          pst.executeUpdate();

          tmpl.getProf().setBoolean(DefaultProfile.HIDE_ADSENSE, false);
          tmpl.writeProfile(user.getNick());
        } else {
          throw new AccessViolationException("Bad activation code");
        }
      }

      user.checkAnonymous();

      String password = request.getParameter("passwd");
      if (password==null || !user.matchPassword(password)) {
        return new ModelAndView(ajax ? "login-xml" : "login-form", Collections.singletonMap("error", "Неверный пароль"));
      }

      if (session == null) {
        throw new BadInputException("не удалось открыть сессию; созможно отсутствует поддержка Cookie");
      }

      performLogin(response, db, tmpl, session, nick, user);

      db.commit();

      if (ajax) {
        return new ModelAndView("login-xml", Collections.singletonMap("ok", "welcome"));
      } else {
        return new ModelAndView(new RedirectView("/"));
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/activate.jsp", method= RequestMethod.GET)
  public ModelAndView activateForm() throws Exception {
    return new ModelAndView("activate");
  }

  @RequestMapping(value = "/logout.jsp", method = RequestMethod.GET)
  public ModelAndView logout(HttpSession session, HttpServletResponse response) throws Exception {
    if (session != null && session.getValue("login") != null && (Boolean) session.getValue("login")) {
      session.removeValue("login");
      session.removeValue("nick");
      session.removeValue("moderator");
      session.removeAttribute("ACEGI_SECURITY_CONTEXT"); // if any
      Cookie cookie = new Cookie("password", "");
      cookie.setMaxAge(60 * 60 * 24 * 31 * 24);
      cookie.setPath("/");
      response.addCookie(cookie);

      Cookie cookie2 = new Cookie("profile", "");
      cookie2.setMaxAge(60 * 60 * 24 * 31 * 24);
      cookie2.setPath("/");
      response.addCookie(cookie2);

      Cookie cookie3 = new Cookie("ACEGI_SECURITY_HASHED_REMEMBER_ME_COOKIE", "");
      cookie3.setMaxAge(60 * 60 * 24 * 31 * 24);
      cookie3.setPath("/wiki");
      response.addCookie(cookie3);
    }

    return new ModelAndView(new RedirectView("/"));
  }

  private void performLogin(HttpServletResponse response, Connection db, Template tmpl, HttpSession session, String nick, User user) throws SQLException {
    session.putValue("login", Boolean.TRUE);
    session.putValue("nick", nick);
    session.putValue("moderator", user.canModerate());

    createCookies(response, tmpl, session, nick, user);

    User.updateUserLastlogin(db, nick, new Date());
  }

  private void createCookies(HttpServletResponse response, Template tmpl, HttpSession session, String nick, User user) {
    Cookie cookie = new Cookie("password", user.getMD5(tmpl.getSecret()));
    cookie.setMaxAge(60 * 60 * 24 * 31 * 24);
    cookie.setPath("/");
    response.addCookie(cookie);

    Cookie prof = new Cookie("profile", nick);
    prof.setMaxAge(60 * 60 * 24 * 31 * 12);
    prof.setPath("/");
    response.addCookie(prof);

    user.acegiSecurityHack(response, session);
  }
}
