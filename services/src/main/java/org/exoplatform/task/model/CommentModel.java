/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.model;

import java.util.Date;
import java.util.List;

import org.exoplatform.services.security.Identity;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.util.TaskUtil;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class CommentModel {
  private final Comment comment;
  private final User author;
  private final String formattedComment;
  private List<CommentModel> subComments;

  public CommentModel(Comment cmt, User author, String formattedComment) {
    this.comment = cmt;
    this.author = author;
    this.formattedComment = formattedComment;
  }

  public User getAuthor() {
    return this.author;
  }

  public Date getCreatedTime() {
    return comment.getCreatedTime();
  }

  public Long getParentCommentId() {
    return comment.getParentComment() == null ? null : comment.getParentComment().getId();
  }

  public String getComment() {
    return comment.getComment();
  }

  public long getId() {
    return comment.getId();
  }

  public Task getTask() {
    return comment.getTask();
  }

  public String getFormattedComment() {
    return formattedComment;
  }

  public boolean canEdit(Identity identity) {
    return TaskUtil.canDeleteComment(identity, comment);
  }

  public void setSubComments(List<CommentModel> subComments) {
    this.subComments = subComments;
  }

  public List<CommentModel> getSubComments() {
    return subComments;
  }

}
