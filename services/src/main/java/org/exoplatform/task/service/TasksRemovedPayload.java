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
package org.exoplatform.task.service;

import java.util.List;

/**
 * The data of the {@link org.exoplatform.task.util.TaskUtil#TASKS_REMOVED}
 * event: the projects and the tasks a project removal deleted, the event
 * source being the id of the removed project.
 *
 * @param projectIds the removed project and, when its children were removed
 *          with it, every descendant; empty when the removal failed before the
 *          project was deleted
 * @param taskIds the tasks whose deletion committed, those of the descendants
 *          included once the project was deleted
 */
public record TasksRemovedPayload(List<Long> projectIds, List<Long> taskIds) {

  /**
   * Copies the lists, so that the payload does not change after the event.
   *
   * @param projectIds the removed project ids
   * @param taskIds the removed task ids
   */
  public TasksRemovedPayload {
    projectIds = projectIds == null ? List.of() : List.copyOf(projectIds);
    taskIds = taskIds == null ? List.of() : List.copyOf(taskIds);
  }

}
