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
package io.meeds.task.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A task listed with {@code changed_since} or {@code completed_since}
 * (EXO-91902), with its changes since that date, oldest first, and its
 * change log when it was asked for.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(value = Include.NON_EMPTY)
public class TaskWithChangesModel extends TaskWithChangeLogModel {

  /** The status, project, due date, assignee and completion changes. */
  private List<TaskChange> changes;

  /**
   * @param model the task model
   * @param changeLogs its change log, null when not asked for
   * @param changes its changes since the date
   */
  public TaskWithChangesModel(TaskModel model, List<TaskChangeLog> changeLogs, List<TaskChange> changes) {
    super(model, changeLogs);
    this.changes = changes;
  }

}
