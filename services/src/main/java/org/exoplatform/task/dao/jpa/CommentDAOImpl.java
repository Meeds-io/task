/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.task.dao.jpa;

import org.apache.commons.collections4.CollectionUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.CommentHandler;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.dto.CommentsSummaryDto;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommentDAOImpl extends CommonJPADAO<Comment, Long> implements CommentHandler {

  /**
   * For each of the given tasks that has comments, its latest comment, a reply
   * or not, with the number of its comments: two comments created at the same
   * time each give a row.
   */
  private static final String COMMENTS_SUMMARIES_QUERY =
                                                       "SELECT new org.exoplatform.task.dto.CommentsSummaryDto(c.task.id,"
                                                           + " (SELECT count(c2) FROM TaskComment c2 WHERE c2.task.id = c.task.id),"
                                                           + " c.createdTime, c.author, c.id)"
                                                           + " FROM TaskComment c WHERE c.task.id IN (:taskIds)"
                                                           + " AND c.createdTime = (SELECT max(c3.createdTime) FROM TaskComment c3"
                                                           + " WHERE c3.task.id = c.task.id)";

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
  public Comment getLastComment(long taskId) {
    TypedQuery<Comment> query = getEntityManager().createNamedQuery("Comment.findLastCommentOfTask", Comment.class);
    query.setParameter("taskId", taskId);
    List<Comment> result = query.getResultList();
    if (CollectionUtils.isEmpty(result)) {
      return null;
    } else {
      return result.get(0);
    }
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

  @Override
  public int countCommentsWithSubs(long taskId) {
    try {
      TypedQuery<Long> query = getEntityManager().createNamedQuery("Comment.countCommentsWithSubs", Long.class);
      query.setParameter("taskId", taskId);
      return query.getSingleResult().intValue();
    } catch (NoResultException e) {
      return 0;
    }
  }

  /**
   * Runs {@link #COMMENTS_SUMMARIES_QUERY} and keeps, for a task whose latest
   * comments share their creation time, the one created last.
   *
   * @param taskIds the tasks
   * @return the summaries of those that have comments, by task id
   */
  @Override
  public Map<Long, CommentsSummaryDto> getCommentsSummaries(Collection<Long> taskIds) {
    if (CollectionUtils.isEmpty(taskIds)) {
      return Collections.emptyMap();
    }
    TypedQuery<CommentsSummaryDto> query = getEntityManager().createQuery(COMMENTS_SUMMARIES_QUERY, CommentsSummaryDto.class);
    query.setParameter("taskIds", taskIds);
    Map<Long, CommentsSummaryDto> summaries = new LinkedHashMap<>();
    query.getResultList()
         .forEach(summary -> summaries.merge(summary.getTaskId(),
                                             summary,
                                             (first, second) -> first.getLastCommentId() >= second.getLastCommentId() ? first : second));
    return summaries;
  }

}
