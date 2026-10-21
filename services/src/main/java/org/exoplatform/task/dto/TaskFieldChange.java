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
 * One change of a task since a date, rebuilt from its change log
 * (EXO-91902): which field changed, its value before and after when the log
 * tells it, when and by whom.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskFieldChange {

  /** The status of a task: its column in its project. */
  public static final String FIELD_STATUS    = "status";

  /** The project of a task, when it moved to another or out of any. */
  public static final String FIELD_PROJECT   = "project";

  /** The due date of a task. */
  public static final String FIELD_DUE_DATE  = "due_date";

  /** The assignee of a task, a username. */
  public static final String FIELD_ASSIGNEE  = "assignee";

  /** Whether a task is completed, {@code true} or {@code false}. */
  public static final String FIELD_COMPLETED = "completed";

  /** The field that changed, one of the {@code FIELD_*} names. */
  private String             field;

  /**
   * Its value before the change, a status name, a project name, a username
   * or {@code true}/{@code false}; null when not recorded.
   */
  private String             oldValue;

  /** Its value after the change, as {@link #oldValue}; null when not recorded or removed. */
  private String             newValue;

  /** The new due date of a due date change, null when not recorded. */
  private Date               newDate;

  /** When the change was made, in milliseconds. */
  private long               createdTime;

  /** The username of who made the change. */
  private String             author;

}
