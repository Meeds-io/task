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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.TaskLogHandler;
import org.exoplatform.task.domain.ChangeLog;

import jakarta.persistence.TypedQuery;

public class TaskLogDAOImpl extends CommonJPADAO<ChangeLog, Long> implements TaskLogHandler {

  private static final String LOGS_SINCE_QUERY     = "SELECT log FROM TaskChangeLog log"
      + " WHERE log.task.id IN (:taskIds) AND log.createdTime >= :since";

  private static final String LATEST_BEFORE_QUERY  = "SELECT log FROM TaskChangeLog log"
      + " WHERE log.task.id IN (:taskIds) AND log.actionName IN (:actionNames)"
      + " AND log.createdTime = (SELECT MAX(prior.createdTime) FROM TaskChangeLog prior"
      + " WHERE prior.task.id = log.task.id AND prior.actionName IN (:actionNames) AND prior.createdTime < :before)";

  @Override
  public List<ChangeLog> findTaskLogsSince(Collection<Long> taskIds, Date since) {
    if (CollectionUtils.isEmpty(taskIds)) {
      return Collections.emptyList();
    }
    TypedQuery<ChangeLog> query = getEntityManager().createQuery(LOGS_SINCE_QUERY, ChangeLog.class);
    query.setParameter("taskIds", taskIds);
    query.setParameter("since", since);
    return query.getResultList();
  }

  @Override
  public List<ChangeLog> findLatestTaskLogsBefore(Collection<Long> taskIds, Date before, Collection<String> actionNames) {
    if (CollectionUtils.isEmpty(taskIds) || CollectionUtils.isEmpty(actionNames)) {
      return Collections.emptyList();
    }
    TypedQuery<ChangeLog> query = getEntityManager().createQuery(LATEST_BEFORE_QUERY, ChangeLog.class);
    query.setParameter("taskIds", taskIds);
    query.setParameter("actionNames", actionNames);
    query.setParameter("before", before);
    return query.getResultList();
  }

  @Override
  public ListAccess<ChangeLog> findTaskLogs(Long taskId) {
    TypedQuery<ChangeLog> query = getEntityManager().createNamedQuery("TaskChangeLog.findChangeLogByTaskId", ChangeLog.class);
    TypedQuery<Long> count = getEntityManager().createNamedQuery("TaskChangeLog.countChangeLogByTaskId", Long.class);

    query.setParameter("taskId", taskId);
    count.setParameter("taskId", taskId);

    return new JPAQueryListAccess<ChangeLog>(ChangeLog.class, count, query);
  }
}
