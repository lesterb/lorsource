package ru.org.linux.site;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import ru.org.linux.util.*;

public class CommentView {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  public String printMessage(Comment comment, Template tmpl, Connection db, CommentList comments, boolean showMenu, boolean moderatorMode, String user, boolean expired)
      throws IOException, UtilException, SQLException, UserNotFoundException {
    StringBuffer out=new StringBuffer();

    out.append("\n\n<!-- ").append(comment.getMessageId()).append(" -->\n");

    out.append("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");

    User author = User.getUserCached(db, comment.getUserid());

    if (showMenu) {
      printMenu(out, comment, expired, moderatorMode, author, user, comments, tmpl, db);
    }

    out.append("<tr class=body><td>");
    out.append("<div class=msg>");

    boolean tbl = false;
    if (author.getPhoto()!=null) {
      if (tmpl.getProf().getBoolean("photos")) {
        out.append("<table><tr><td valign=top align=center>");
        tbl=true;

        try {
          ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+"/photos/"+author.getPhoto());
          out.append("<img src=\"/photos/").append(author.getPhoto()).append("\" alt=\"").append(author.getNick()).append(" (����������)\" ").append(info.getCode()).append(" >");
        } catch (BadImageException e) {
          logger.warning(StringUtil.getStackTrace(e));
        }

        out.append("</td><td valign=top>");
      }
    }

    out.append("<h2><a name=").append(comment.getMessageId()).append('>').append(comment.getTitle()).append("</a></h2>");

    out.append(comment.getMessageText());

    out.append("<p>");

    out.append(author.getSignature(moderatorMode, comment.getPostdate()));

    if (!expired && !comment.isDeleted() && showMenu)
      out.append("<p><font size=2>[<a href=\"add_comment.jsp?topic=").append(comment.getTopic()).append("&amp;replyto=").append(comment.getMessageId()).append("\">�������� �� ��� ���������</a>]</font>");

    if (tbl) out.append("</td></tr></table>");
      out.append("</div></td></tr>");
      out.append("</table><p>");

    return out.toString();
  }

  private void printMenu(StringBuffer out, Comment comment, boolean expired, boolean moderatorMode, User author, String user, CommentList comments, Template tmpl, Connection db) throws UtilException, SQLException, UserNotFoundException {
    out.append("<tr class=title><td>");

    if (!comment.isDeleted()) {
      out.append("[<a href=\"/jump-message.jsp?msgid=").append(comment.getTopic()).append("&amp;cid=").append(comment.getMessageId()).append("\">#</a>]");
    }

    if (!expired && !comment.isDeleted()) {
      out.append("[<a href=\"add_comment.jsp?topic=").append(comment.getTopic()).append("&amp;replyto=").append(comment.getMessageId()).append("\">��������</a>]");
    }

    if (!comment.isDeleted() && (moderatorMode || author.getNick().equals(user))) {
      out.append("[<a href=\"delete_comment.jsp?msgid=").append(comment.getMessageId()).append("\">�������</a>]");
    }

    if (moderatorMode) {
      out.append("[<a href=\"sameip.jsp?msgid=").append(comment.getMessageId()).append("\">������ � ����� IP</a>]");
    }

    if (comment.isDeleted()) {
      if (comment.getDeleteInfo() ==null) {
        out.append("<strong>��������� �������</strong>");
      } else {
        out.append("<strong>��������� ������� ").append(comment.getDeleteInfo().getNick()).append(" �� ������� '").append(HTMLFormatter.htmlSpecialChars(comment.getDeleteInfo().getReason())).append("'</strong>");
      }
    }

    out.append("&nbsp;</td></tr>");

    if (comment.getReplyTo() != 0) {
      CommentNode replyNode = comments.getNode(comment.getReplyTo());
      if (replyNode != null) {
        Comment reply = replyNode.getComment();

        out.append("<tr class=title><td>");

        out.append("����� ��: <a href=\"");

        int replyPage = comments.getCommentPage(reply, tmpl);
        if (replyPage > 0) {
          out.append("view-message.jsp?msgid=").append(comment.getTopic()).append("&amp;page=").append(replyPage).append('#').append(comment.getReplyTo());
        } else {
          out.append("view-message.jsp?msgid=").append(comment.getTopic()).append('#').append(comment.getReplyTo());
        }

        out.append("\">");

        User replyAuthor = User.getUserCached(db, reply.getUserid());

        out.append(StringUtil.makeTitle(reply.getTitle())).append("</a> �� ").append(replyAuthor.getNick()).append(' ').append(Template.dateFormat.format(reply.getPostdate()));
      } else {
        logger.warning("Weak reply #" + comment.getReplyTo() + " on comment=" + comment.getMessageId() + " msgid=" + comment.getTopic());
      }
    }
  }
}