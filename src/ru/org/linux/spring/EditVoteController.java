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
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ServletParameterParser;

@Controller
public class EditVoteController extends ApplicationObjectSupport {
  @RequestMapping(value="/edit-vote.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("msgid", msgid);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Poll poll = Poll.getPollByTopic(db, msgid);
      params.put("poll", poll);

      List<PollVariant> variants = poll.getPollVariants(db, Poll.ORDER_ID);
      params.put("variants", variants);

      return new ModelAndView("edit-vote", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/edit-vote.jsp", method= RequestMethod.POST)
  public ModelAndView editVote(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid,
    @RequestParam("id") int id,
    @RequestParam("title") String title
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      User user = User.getUser(db, tmpl.getNick());
      user.checkCommit();

      Poll poll = new Poll(db, id);

      PreparedStatement pstTitle = db.prepareStatement("UPDATE votenames SET title=? WHERE id=?");
      pstTitle.setInt(2, id);
      pstTitle.setString(1, HTMLFormatter.htmlSpecialChars(title));

      pstTitle.executeUpdate();

      PreparedStatement pstTopic = db.prepareStatement("UPDATE topics SET title=? WHERE id=?");
      pstTopic.setInt(2, msgid);
      pstTopic.setString(1, HTMLFormatter.htmlSpecialChars(title));

      pstTopic.executeUpdate();

      List<PollVariant> variants = poll.getPollVariants(db, Poll.ORDER_ID);
      for (PollVariant var : variants) {
        String label = new ServletParameterParser(request).getString("var" + var.getId());

        if (label == null || label.trim().length() == 0) {
          var.remove(db);
        } else {
          var.updateLabel(db, label);
        }
      }

      for (int i = 1; i <= 3; i++) {
        String label = new ServletParameterParser(request).getString("new" + i);

        if (label != null && label.trim().length() > 0) {
          poll.addNewVariant(db, label);
        }
      }

      logger.info("Отредактирован опрос" + id + " пользователем " + user.getNick());

      db.commit();

      Random random = new Random();

      return new ModelAndView(new RedirectView("view-message.jsp?msgid=" + msgid + "&nocache=" + random.nextInt()));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }  
}
