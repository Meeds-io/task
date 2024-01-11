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

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.TaskLogHandler;
import org.exoplatform.task.domain.ChangeLog;

import jakarta.persistence.TypedQuery;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class TaskLogDAOImpl extends CommonJPADAO<ChangeLog, Long> implements TaskLogHandler {
  @Override
  public ListAccess<ChangeLog> findTaskLogs(Long taskId) {
    TypedQuery<ChangeLog> query = getEntityManager().createNamedQuery("TaskChangeLog.findChangeLogByTaskId", ChangeLog.class);
    TypedQuery<Long> count = getEntityManager().createNamedQuery("TaskChangeLog.countChangeLogByTaskId", Long.class);

    query.setParameter("taskId", taskId);
    count.setParameter("taskId", taskId);

    return new JPAQueryListAccess<ChangeLog>(ChangeLog.class, count, query);
  }
}
