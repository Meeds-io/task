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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How many of the tasks a query selects fall in one group of a grouping:
 * a status, an assignee, a label, or a status of one project (EXO-94360).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskGroupCountDto {

  /**
   * The group's key: the status name, the assignee's username, the label's id,
   * or null for the tasks with no assignee.
   */
  private String key;

  /** The group's name: the status or label name, or the assignee's username. */
  private String name;

  /** The project of the status or the label, null for an assignee. */
  private Long   projectId;

  /** The name of that project, null for an assignee. */
  private String projectName;

  /** The number of the query's tasks in the group. */
  private long   count;
}
