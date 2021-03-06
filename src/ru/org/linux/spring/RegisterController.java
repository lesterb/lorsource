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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;
import ru.org.linux.util.*;

@SuppressWarnings({"ProhibitedExceptionDeclared"})
@Controller
public class RegisterController extends ApplicationObjectSupport {
  @RequestMapping(value = "/register.jsp", method = RequestMethod.GET)
  public ModelAndView register()  {
    return new ModelAndView("register");
  }

  @RequestMapping(value = "/register.jsp", method = RequestMethod.POST)
  public ModelAndView doRegister(HttpServletRequest request) throws Exception {
    HttpSession session = request.getSession();
    Template tmpl = Template.getTemplate(request);

    Connection db = null;
    try {
      boolean changeMode = "change".equals(request.getParameter("mode"));

      String nick;

      if (changeMode) {
        if (!tmpl.isSessionAuthorized()) {
          throw new AccessViolationException("Not authorized");
        }
        
        nick = tmpl.getNick();
      } else {
        nick = request.getParameter("nick");
        User.checkNick(nick);
      }

      String town = request.getParameter("town");
      String info = request.getParameter("info");
      String name = request.getParameter("name");
      String url = request.getParameter("url");
      String password = request.getParameter("password");
      String password2 = request.getParameter("password2");
      String email = request.getParameter("email");

      if (password != null && password.length() == 0) {
        password = null;
      }

      if (password2 != null && password2.length() == 0) {
        password2 = null;
      }

      if (email == null || "".equals(email)) {
        throw new BadInputException("Не указан e-mail");
      }

      InternetAddress mail = new InternetAddress(email);
      if (url != null && "".equals(url)) {
        url = null;
      }

      if (url != null) {
        url = URLUtil.fixURL(url);
      }

      if (!changeMode) {
        if (password == null) {
          throw new BadInputException("пароль не может быть пустым");
        }

        if (password2 == null || !password.equals(password2)) {
          throw new BadInputException("введенные пароли не совпадают");
        }
      } else {
        if (password2 != null && password != null && !password.equals(password2)) {
          throw new BadInputException("введенные пароли не совпадают");
        }
      }

      if (name != null && "".equals(name)) {
        name = null;
      }
      if (town != null && "".equals(town)) {
        town = null;
      }
      if (info != null && "".equals(info)) {
        info = null;
      }

      if (name != null) {
        name = HTMLFormatter.htmlSpecialChars(name);
      }
      if (town != null) {
        town = HTMLFormatter.htmlSpecialChars(town);
      }
      if (info != null) {
        info = HTMLFormatter.htmlSpecialChars(info);
      }

      if (!changeMode) {
        CaptchaUtils.checkCaptcha(request);

        if (session.getAttribute("register-visited") == null) {
          logger.info("Flood protection (not visited register.jsp) " + request.getRemoteAddr());
          throw new BadInputException("сбой");
        }
      }

      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

      int userid;

      if (changeMode) {
        User user = User.getUser(db, nick);
        userid = user.getId();
        user.checkPassword(request.getParameter("oldpass"));
        user.checkAnonymous();

        PreparedStatement ist = db.prepareStatement("UPDATE users SET  name=?, passwd=?, url=?, email=?, town=? WHERE id=" + userid);
        ist.setString(1, name);
        if (password == null) {
          ist.setString(2, request.getParameter("oldpass"));
        } else {
          ist.setString(2, password);
        }

        if (url != null) {
          ist.setString(3, url);
        } else {
          ist.setString(3, null);
        }

        ist.setString(4, mail.getAddress());

        ist.setString(5, town);

        ist.executeUpdate();

        ist.close();

        if (info != null) {
          user.setUserinfo(db, info);
        }
      } else {
        PreparedStatement pst = db.prepareStatement("SELECT count(*) as c FROM users WHERE nick=?");
        pst.setString(1, nick);
        ResultSet rs = pst.executeQuery();
        rs.next();
        if (rs.getInt("c") != 0) {
          throw new BadInputException("пользователь " + nick + " уже существует");
        }
        rs.close();
        pst.close();

        PreparedStatement pst2 = db.prepareStatement("SELECT count(*) as c FROM users WHERE email=?");
        pst2.setString(1, mail.getAddress());
        rs = pst2.executeQuery();
        rs.next();
        if (rs.getInt("c") != 0) {
          throw new BadInputException("пользователь с таким e-mail уже зарегистрирован");
        }
        rs.close();
        pst2.close();

        Statement st = db.createStatement();
        rs = st.executeQuery("select nextval('s_uid') as userid");
        rs.next();
        userid = rs.getInt("userid");
        rs.close();
        st.close();

        PreparedStatement ist = db.prepareStatement("INSERT INTO users (id, name, nick, passwd, url, email, town, score, max_score,regdate) VALUES (?,?,?,?,?,?,?,50,50,current_timestamp)");
        ist.setInt(1, userid);
        ist.setString(2, name);
        ist.setString(3, nick);
        ist.setString(4, password);
        if (url != null) {
          ist.setString(5, URLUtil.fixURL(url));
        } else {
          ist.setString(5, null);
        }

        ist.setString(6, mail.getAddress());

        ist.setString(7, town);
        ist.executeUpdate();
        ist.close();

        String logmessage = "Зарегистрирован пользователь " + nick + " (id=" + userid + ") " + LorHttpUtils.getRequestIP(request);
        logger.info(logmessage);

        if (info != null) {
          User.setUserinfo(db, userid, info);
        }

        sendEmail(tmpl, nick, password, email);
      }

      db.commit();

      if (changeMode) {
        return new ModelAndView("action-done", Collections.singletonMap("message", "Обновление регистрации прошло успешно"));
      } else {
        return new ModelAndView("action-done", Collections.singletonMap("message", "Добавление пользователя прошло успешно"));
      }
    } catch (BadInputException e) {
      return new ModelAndView("register", Collections.singletonMap("error", e.getMessage()));
    } catch (BadURLException e) {
      return new ModelAndView("register", Collections.singletonMap("error", e.getMessage()));
    } catch (AddressException e) {
      return new ModelAndView("register", Collections.singletonMap("error", "Некорректный e-mail: "+e.getMessage()));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private void sendEmail(Template tmpl, String nick, String password, String email) throws MessagingException {
    StringBuilder text = new StringBuilder();

    text.append("Здравствуйте!\n\n");
    text.append("\tВ форуме по адресу http://www.linux.org.ru/ появилась регистрационная запись,\n");
    text.append("в которой был указал ваш электронный адрес (e-mail).\n\n");
    text.append("При заполнении регистрационной формы было указано следующее имя пользователя: '");
    text.append(nick);
    text.append("'\n\n");
    text.append("Если вы не понимаете, о чем идет речь - просто проигнорируйте это сообщение!\n\n");
    text.append("Если же именно вы решили зарегистрироваться в форуме по адресу http://www.linux.org.ru/,\n");
    text.append("то вам следует подтвердить свою регистрацию и тем самым активировать вашу учетную запись.\n\n");

    String regcode = StringUtil.md5hash(tmpl.getSecret() + ':' + nick + ':' + password);

    text.append("Для активации перейдите по ссылке http://www.linux.org.ru/activate.jsp\n\n");
    text.append("Код активации: ").append(regcode).append("\n\n");
    text.append("Благодарим за регистрацию!\n");

    Properties props = new Properties();
    props.put("mail.smtp.host", "localhost");
    Session mailSession = Session.getDefaultInstance(props, null);

    MimeMessage emailMessage = new MimeMessage(mailSession);
    emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"));

    emailMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(email));
    emailMessage.setSubject("Linux.org.ru registration");
    emailMessage.setSentDate(new Date());
    emailMessage.setText(text.toString(), "UTF-8");

    Transport.send(emailMessage);
  }
}
