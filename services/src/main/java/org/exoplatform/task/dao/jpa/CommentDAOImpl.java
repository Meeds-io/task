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
package org.exoplatform.task.dao.jpa;

import org.apache.commons.collections.CollectionUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.CommentHandler;
import org.exoplatform.task.domain.Comment;

import jakarta.persistence.TypedQuery;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class CommentDAOImpl extends CommonJPADAO<Comment, Long> implements CommentHandler {

  @Override
  public void delete(Comment entity) {
    List<Comment> comments = getSubComments(Collections.singletonList(entity));
    if (CollectionUtils.isNotEmpty(comments)) {
      comments.forEach(super::delete);
    }
    if (find(entity.getId()) != null) {
      super.delete(entity);
    }
  }

  @Override
  public void deleteAll(List<Comment> comments) {
    comments.forEach(this::delete);
  }

  @Override
  @ExoTransactional
  public void deleteAll() {
    List<Comment> comments = findAll();
    comments.forEach(this::delete);
  }

  @Override
  @ExoTransactional
  public ListAccess<Comment> findComments(long taskId) {
    TypedQuery<Comment> query = getEntityManager().createNamedQuery("Comment.findCommentsOfTask", Comment.class);
    TypedQuery<Long> count = getEntityManager().createNamedQuery("Comment.countCommentOfTask", Long.class);

    query.setParameter("taskId", taskId);
    count.setParameter("taskId", taskId);

    return new JPAQueryListAccess<Comment>(Comment.class, count, query);
  }

  @Override
  @ExoTransactional
  public List<Comment> getSubComments(List<Comment> comments) {
    TypedQuery<Comment> query = getEntityManager().createNamedQuery("Comment.findSubCommentsOfComments", Comment.class);
    query.setParameter("comments", comments);
    return query.getResultList();
  }

  @Override
  public Set<String> findMentionedUsersOfTask(long taskId) {
    TypedQuery<String> query = getEntityManager().createNamedQuery("Comment.findMentionedUsersOfTask", String.class);
    query.setParameter("taskId", taskId);
    List<String> tags = query.getResultList();
    return new HashSet<>(tags);
  }
}
