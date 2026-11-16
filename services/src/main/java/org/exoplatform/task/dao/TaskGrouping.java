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
package org.exoplatform.task.dao;

/**
 * What the tasks a query selects are counted by (EXO-94360).
 */
public enum TaskGrouping {

  /** The status name, across projects: a personal task, with no status, is in no group. */
  STATUS,

  /** The status of each project: a personal task is in no group. */
  PROJECT_STATUS,

  /** The assignee, a task with none in the group of key null. */
  ASSIGNEE,

  /**
   * The label of the task's own board, those its card shows: a task is in as
   * many groups as it has such labels, and in none without one.
   */
  LABEL
}
