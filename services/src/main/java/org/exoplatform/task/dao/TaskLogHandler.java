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

import java.util.Date;
import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.domain.ChangeLog;

public interface TaskLogHandler extends GenericDAO<ChangeLog, Long> {
  /**
   * @param taskId the task id
   * @return the task's change log, newest first
   */
  ListAccess<ChangeLog> findTaskLogs(Long taskId);

  /**
   * Reads one page of a task's change log by its position, not by an offset:
   * entries added while a reader pages are never read twice nor skipped.
   *
   * @param taskId the task id
   * @param beforeTime the date of the last entry already read, or null to read
   *          from the newest
   * @param beforeId the id of the last entry already read, ignored when
   *          beforeTime is null
   * @param limit the maximum number of entries
   * @return the entries older than the last one read, or dated alike with a
   *         lower id, newest first and, on the same date, highest id first
   */
  List<ChangeLog> findTaskLogsBefore(Long taskId, Date beforeTime, Long beforeId, int limit);
}
