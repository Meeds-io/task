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
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommentDAOImpl extends CommonJPADAO<Comment, Long> implements CommentHandler {

  /**
   * For each of the given tasks that has comments, the number of its
   * comments, replies included, and the creation time of its latest one: one
   * grouped scan, with no subquery run per row.
   */
  private static final String COMMENTS_COUNTS_QUERY = "SELECT c.task.id, count(c), max(c.createdTime) FROM TaskComment c"
      + " WHERE c.task.id IN (:taskIds) GROUP BY c.task.id";

  /**
   * The comments of the given tasks created at one of the given times: the
   * latest comment of each task, with the comments of another task created
   * at the same time as one of them, which the caller leaves out.
   */
  private static final String LATEST_COMMENTS_QUERY = "SELECT c.task.id, c.createdTime, c.author, c.id FROM TaskComment c"
      + " WHERE c.task.id IN (:taskIds) AND c.createdTime IN (:createdTimes)";

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
   * Runs {@link #COMMENTS_COUNTS_QUERY}, then {@link #LATEST_COMMENTS_QUERY}
   * for the authors of the latest comments, keeping for each task the comment
   * created at its latest time, the one created last when several were.
   *
   * @param taskIds the tasks
   * @return the summaries of those that have comments, by task id
   */
  @Override
  public Map<Long, CommentsSummaryDto> getCommentsSummaries(Collection<Long> taskIds) {
    if (CollectionUtils.isEmpty(taskIds)) {
      return Collections.emptyMap();
    }
    TypedQuery<Object[]> countsQuery = getEntityManager().createQuery(COMMENTS_COUNTS_QUERY, Object[].class);
    countsQuery.setParameter("taskIds", taskIds);
    Map<Long, CommentsSummaryDto> summaries = new LinkedHashMap<>();
    for (Object[] row : countsQuery.getResultList()) {
      long taskId = ((Number) row[0]).longValue();
      summaries.put(taskId, new CommentsSummaryDto(taskId, ((Number) row[1]).longValue(), (Date) row[2], null, 0));
    }
    if (summaries.isEmpty()) {
      return summaries;
    }
    TypedQuery<Object[]> latestQuery = getEntityManager().createQuery(LATEST_COMMENTS_QUERY, Object[].class);
    latestQuery.setParameter("taskIds", summaries.keySet());
    latestQuery.setParameter("createdTimes",
                             summaries.values().stream().map(CommentsSummaryDto::getLastCommentDate).distinct().toList());
    for (Object[] row : latestQuery.getResultList()) {
      CommentsSummaryDto summary = summaries.get(((Number) row[0]).longValue());
      long commentId = ((Number) row[3]).longValue();
      if (((Date) row[1]).getTime() == summary.getLastCommentDate().getTime()
          && (summary.getLastCommentAuthor() == null || commentId > summary.getLastCommentId())) {
        summary.setLastCommentAuthor((String) row[2]);
        summary.setLastCommentId(commentId);
      }
    }
    return summaries;
  }

}
