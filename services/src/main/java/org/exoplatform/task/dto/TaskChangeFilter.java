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
package org.exoplatform.task.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a task listing selects among the changes of its tasks (EXO-91902):
 * the tasks that changed or were completed since a date, assigned to one
 * user, in one status, the completed ones left out on demand. Every field is
 * optional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskChangeFilter {

  /** Only the tasks whose status, project, due date, assignee or completion changed at or after it. */
  private Date    changedSince;

  /** Only the tasks completed at or after it. */
  private Date    completedSince;

  /** Only the tasks assigned to this username. */
  private String  assignee;

  /** Only the tasks in this status. */
  private Long    statusId;

  /** Whether the completed tasks are left out, unless completedSince is given. */
  private boolean hideCompleted;

}
