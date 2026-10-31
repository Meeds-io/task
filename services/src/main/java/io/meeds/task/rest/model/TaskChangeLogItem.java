/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.task.rest.model;

import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.ChangeLogEntry;

/**
 * One entry of a task's change log as the Changes timeline shows it: who, what,
 * on whom, when, and the task's priority for a change of priority; never the
 * task itself.
 *
 * @param id the entry id, the timeline's key
 * @param author the username of who made the change
 * @param authorFullName their full name
 * @param actionName the change's action
 * @param target what or whom the change concerns
 * @param targetFullName the full name of a user target
 * @param isTargetFullNameExternal whether that user is an external one
 * @param createdTime the change's date in milliseconds, 0 when the change has
 *          none
 * @param task the task's priority, read by a change of priority
 */
public record TaskChangeLogItem(long id,
                                String author,
                                String authorFullName,
                                String actionName,
                                String target,
                                String targetFullName,
                                boolean isTargetFullNameExternal,
                                long createdTime,
                                TaskPriority task) {

  /**
   * @param priority the task's priority name, none when it has none
   */
  public record TaskPriority(String priority) {
  }

  /**
   * @param entry an entry of a task's change log
   * @return the entry as the timeline shows it
   */
  public static TaskChangeLogItem of(ChangeLogEntry entry) {
    Task task = entry.getTask();
    return new TaskChangeLogItem(entry.getId(),
                                 entry.getAuthor(),
                                 entry.getAuthorFullName(),
                                 entry.getActionName(),
                                 entry.getTarget(),
                                 entry.getTargetFullName(),
                                 entry.getIsTargetFullNameExternal(),
                                 entry.getCreatedTime(),
                                 new TaskPriority(task == null || task.getPriority() == null ? null : task.getPriority().name()));
  }

}
