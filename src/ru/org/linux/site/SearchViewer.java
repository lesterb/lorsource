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

package ru.org.linux.site;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.*;
import java.util.Date;

import org.javabb.bbcode.BBCodeProcessor;

import ru.org.linux.util.HTMLFormatter;

public class SearchViewer {
  public static final int SEARCH_TOPICS = 1;
  public static final int SEARCH_ALL = 0;

  public static final int SEARCH_3MONTH = 1;
  public static final int SEARCH_YEAR = 2;

  public static final int SORT_R = 1;
  public static final int SORT_DATE = 2;

  private final String query;
  private int include = SEARCH_ALL;
  private int date = SEARCH_ALL;
  private int section = 0;
  private int sort = SORT_R;

  private String username = "";
  private boolean userTopic = false;

  public SearchViewer(String query) {
    this.query = query;
  }

  public String show(Connection db) throws SQLException, UserErrorException {
    StringBuilder select = new StringBuilder(""+
        "SELECT " +
        "msgs.id, msgs.title, msgs.postdate, topic, msgs.userid, rank(idxFTI, q) as rank, message, bbcode");

    if (include==SEARCH_ALL) {
      select.append(" FROM msgs_and_cmts as msgs, msgbase, plainto_tsquery(?) as q");
    } else {
      select.append(" FROM msgs, msgbase, plainto_tsquery(?) as q");
    }

    if (section!=0 || userTopic) {
      select.append(", topics");
    }

    if (section!=0) {
      select.append(", groups");
    }

    select.append(" WHERE msgs.id = msgbase.id AND idxFTI @@ q");

    if (date==SEARCH_3MONTH) {
      select.append(" AND msgs.postdate>CURRENT_TIMESTAMP-'3 month'::interval");
    } else if (date == SEARCH_YEAR) {
      select.append(" AND msgs.postdate>CURRENT_TIMESTAMP-'1 year'::interval");
    }

    if (section!=0) {
      select.append(" AND section=").append(section);
      select.append(" AND topics.id = topic AND groups.id = topics.groupid");
    }

    if (username.length()>0) {
      try {
        User user = User.getUser(db, username);

        if (userTopic) {
          select.append(" AND topics.userid=").append(user.getId());
          select.append(" AND topics.id = topic");
        } else {
          select.append(" AND msgs.userid=").append(user.getId());
        }
      } catch (UserNotFoundException ex) {
        throw new UserErrorException("User not found: "+username);
      }
    }

    if (sort==SORT_DATE) {
      select.append(" ORDER BY postdate DESC");
    } else {
      select.append(" ORDER BY rank DESC");
    }

    select.append(" LIMIT 100");

    PreparedStatement pst = null;
    try {
      pst = db.prepareStatement(select.toString());

      pst.setString(1, query);

      ResultSet rs = pst.executeQuery();

      return printResults(db, rs);
    } catch (UserNotFoundException ex) {
      throw new RuntimeException(ex);
    } finally {
      if (pst!=null) {
        pst.close();
      }
    }
  }

  private static String printResults(Connection db, ResultSet rs) throws SQLException, UserNotFoundException {
    StringBuilder out = new StringBuilder("<h1>Результаты поиска</h1>");

    out.append("<div class=\"messages\"><div class=\"comment\">");

    while (rs.next()) {
      String title = rs.getString("title");
      int topic = rs.getInt("topic");
      int id = rs.getInt("id");
      String message = rs.getString("message");
      boolean lorcode = rs.getBoolean("bbcode");
      Timestamp postdate = rs.getTimestamp("postdate");
      int userid = rs.getInt("userid");
      User user = User.getUserCached(db, userid);

      String url;

      if (topic==0 || topic==id) {
        url = "view-message.jsp?msgid="+id;
      } else {
        url = "jump-message.jsp?msgid="+topic+"&amp;cid="+id;
      }

      out.append("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");
      out.append("<tr class=body><td>");
      out.append("<div class=msg>");

      out.append("<h2><a href=\"").append(url).append("\">").append(HTMLFormatter.htmlSpecialChars(title)).append("</a></h2>");

      out.append("<p>");

      if (lorcode) {
        BBCodeProcessor proc = new BBCodeProcessor();
        out.append(proc.preparePostText(db, message));
      } else {
        out.append(message);
      }

      out.append("</p>");

      out.append("<div class=sign>");
      out.append(user.getSignature(false, postdate, false, null));
      out.append("</div>");

      out.append("</div></td></tr></table><p>");
    }

    out.append("</div></div>");

    return out.toString();
  }

  public String getVariantID() {
    try {
      return "search?q="+ URLEncoder.encode(query, "koi8-r")+"&include="+include+"&date="+date+"&section="+section+"&sort="+sort+"&username="+URLEncoder.encode(username);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static Date getExpire() {
    return new java.util.Date(new java.util.Date().getTime() + 15*60*1000);
  }

  public void setInclude(int include) {
    this.include = include;
  }

  public void setDate(int date) {
    this.date = date;
  }

  public void setSection(int section) {
    this.section = section;
  }

  public void setSort(int sort) {
    this.sort = sort;
  }

  public void setUser(String username) {
    this.username = username;
  }

  public void setUserTopic(boolean userTopic) {
    this.userTopic = userTopic;
  }
}
