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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Group {
  private boolean moderate;
  private boolean imagepost;
  private boolean votepoll;
  private boolean havelink;
  private int section;
  private String linktext;
  private String sectionName;
  private String title;
  private String image;
  private int restrictTopics;
  private int restrictComments;
  private int id;

  private int stat1;
  private int stat2;
  private int stat3;

  private String info;
  private String longInfo;

  public Group(Connection db, int id) throws SQLException, BadGroupException {
    this.id = id;

    ResultSet rs = null;
    Statement st = null;
    try {
      st = db.createStatement();

      rs = st.executeQuery("SELECT sections.moderate, sections.preformat, imagepost, vote, section, havelink, linktext, sections.name as sname, title, image, restrict_topics, restrict_comments,stat1,stat2,stat3,groups.id, groups.info, groups.longinfo FROM groups, sections WHERE groups.id=" + id + " AND groups.section=sections.id");

      if (!rs.next()) {
        throw new BadGroupException("Группа " + id + " не существует");
      }

      init(rs);
    } finally {
      if (st != null) {
        st.close();
      }
      if (rs != null) {
        rs.close();
      }
    }
  }

  private Group(ResultSet rs) throws SQLException {
    init(rs);
  }

  public static List<Group> getGroups(Connection db, Section section) throws SQLException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT sections.moderate, sections.preformat, imagepost, vote, section, havelink, linktext, sections.name as sname, title, image, restrict_topics, restrict_comments, stat1,stat2,stat3,groups.id,groups.info,groups.longinfo FROM groups, sections WHERE sections.id=" + section.getId() + " AND groups.section=sections.id ORDER BY id");

    List<Group> list = new ArrayList<Group>();

    while(rs.next()) {
      Group group = new Group(rs);

      list.add(group);
    }

    return list;
  }

  private void init(ResultSet rs) throws SQLException {
    id = rs.getInt("id");
    moderate = rs.getBoolean("moderate");
    imagepost = rs.getBoolean("imagepost");
    votepoll = rs.getBoolean("vote");
    section = rs.getInt("section");
    havelink = rs.getBoolean("havelink");
    linktext = rs.getString("linktext");
    sectionName = rs.getString("sname");
    title = rs.getString("title");
    image = rs.getString("image");
    restrictTopics = rs.getInt("restrict_topics");
    restrictComments = rs.getInt("restrict_comments");

    stat1 = rs.getInt("stat1");
    stat2 = rs.getInt("stat2");
    stat3 = rs.getInt("stat3");


    info = rs.getString("info");
    longInfo = rs.getString("longinfo");
  }

  public boolean isImagePostAllowed() {
    return imagepost;
  }

  public boolean isPollPostAllowed() {
    return votepoll;
  }

  public int getSectionId() {
    return section;
  }

  public boolean isModerated() {
    return moderate;
  }

  public boolean isLinksAllowed() {
    return havelink;
  }

  public String getDefaultLinkText() {
    return linktext;
  }

  public String getSectionName() {
    return sectionName;
  }

  public String getTitle() {
    return title;
  }

  public String getImage() {
    return image;
  }

  public boolean isTopicsRestricted() {
    return restrictTopics != 0;
  }

  public int getTopicsRestriction() {
    return restrictTopics;
  }

  public boolean isTopicPostingAllowed(User currentUser) {
    if (!isTopicsRestricted()) {
      return true;
    }

    if (currentUser==null) {
      return false;
    }

    if (currentUser.isBlocked()) {
      return false;
    }

    if (restrictTopics==-1) {
      return currentUser.canModerate();
    } else {
      return currentUser.getScore() >= restrictTopics;
    }
  }

  public boolean isCommentsRestricted() {
    return restrictComments != 0;
  }

  public int getCommentsRestriction() {
    return restrictComments;
  }

  public boolean isCommentPostingAllowed(User currentUser) {
    if (!isCommentsRestricted()) {
      return true;
    }

    if (restrictComments==-1) {
      return currentUser.canModerate();
    }

    return currentUser.getScore() >= restrictComments;
  }

  public int getId() {
    return id;
  }

  public int getStat1() {
    return stat1;
  }

  public int getStat2() {
    return stat2;
  }

  public int getStat3() {
    return stat3;
  }

  public String getInfo() {
    return info;
  }

  public String getUrl() {
    return "group.jsp?group="+id;
  }

  public int calcTopicsCount(Connection db, boolean showDeleted) throws SQLException {
    Statement st = null;

    try {
      st = db.createStatement();

      ResultSet rs;
      if (showDeleted) {
        rs = st.executeQuery("SELECT count(topics.id) FROM topics,groups,sections WHERE (topics.moderate OR NOT sections.moderate) AND groups.section=sections.id AND topics.groupid=groups.id AND groups.id=" + id);
      } else {
        rs = st.executeQuery("SELECT count(topics.id) FROM topics,groups,sections WHERE (topics.moderate OR NOT sections.moderate) AND groups.section=sections.id AND topics.groupid=groups.id AND groups.id=" + id + " AND NOT topics.deleted");
      }

      if (rs.next()) {
        return rs.getInt("count");
      } else {
        return 0;
      }
    } finally {
      if (st != null) {
        st.close();
      }
    }
  }

  public String getLongInfo() {
    return longInfo;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public void setLongInfo(String longInfo) {
    this.longInfo = longInfo;
  }
}

