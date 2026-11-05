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
package org.exoplatform.task.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.domain.ChangeLog;

public interface TaskLogHandler extends GenericDAO<ChangeLog, Long> {
  ListAccess<ChangeLog> findTaskLogs(Long taskId);

  /**
   * Reads, in one query, the change log entries of several tasks logged at or
   * after a date.
   *
   * @param taskIds the tasks
   * @param since the earliest date, included
   * @return their entries, in no order
   */
  List<ChangeLog> findTaskLogsSince(Collection<Long> taskIds, Date since);

  /**
   * Reads, in one query, the latest change log entry of each of several tasks
   * logged before a date with one of the given actions: two entries logged at
   * that same latest time both come back.
   *
   * @param taskIds the tasks
   * @param before the date the entries precede, excluded
   * @param actionNames the actions of the entries
   * @return the latest entry of each task, in no order
   */
  List<ChangeLog> findLatestTaskLogsBefore(Collection<Long> taskIds, Date before, Collection<String> actionNames);
}
