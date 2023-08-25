/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.integration;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentPlugin;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.TaskUtil;

public class TaskAttachmentPlugin extends AttachmentPlugin {

  public static final String TASK_OBJECT_TYPE                 = "task";

  public static final String TASK_NOT_FOUND_EXCEPTION_MESSAGE = "Task with id %s wasn't found";

  private TaskService        taskService;

  private ProjectService     projectService;

  private SpaceService       spaceService;

  public TaskAttachmentPlugin(TaskService taskService, ProjectService projectService, SpaceService spaceService) {
    this.taskService = taskService;
    this.projectService = projectService;
    this.spaceService = spaceService;
  }

  @Override
  public String getObjectType() {
    return TASK_OBJECT_TYPE;
  }

  @Override
  public boolean hasAccessPermission(Identity userIdentity, String entityId) throws ObjectNotFoundException {
    try {
      TaskDto task = taskService.getTask(Long.parseLong(entityId));
      return TaskUtil.hasViewPermission(taskService, task, userIdentity);
    } catch (Exception e) {
      throw new ObjectNotFoundException(String.format(TASK_NOT_FOUND_EXCEPTION_MESSAGE, entityId));
    }
  }

  @Override
  public boolean hasEditPermission(Identity userIdentity, String entityId) throws ObjectNotFoundException {
    try {
      TaskDto task = taskService.getTask(Long.parseLong(entityId));
      return TaskUtil.hasEditPermission(taskService, task, userIdentity);
    } catch (Exception e) {
      throw new ObjectNotFoundException(String.format(TASK_NOT_FOUND_EXCEPTION_MESSAGE, entityId));
    }
  }

  @Override
  public long getAudienceId(String entityId) throws ObjectNotFoundException {
    return getSpaceId(entityId);
  }

  @Override
  public long getSpaceId(String entityId) throws ObjectNotFoundException {
    long spaceId = 0l;
    try {
      TaskDto task = taskService.getTask(Long.parseLong(entityId));
      Space space = getProjectSpace(task.getStatus().getProject().getId());
      if (space != null) {
        spaceId = Long.parseLong(space.getId());
      }
    } catch (Exception e) {
      throw new ObjectNotFoundException(String.format(TASK_NOT_FOUND_EXCEPTION_MESSAGE, entityId));
    }
    return spaceId;
  }

  private Space getProjectSpace(long projectId) {
    Space space = null;
    for (String permission : projectService.getManager(projectId)) {
      int index = permission.indexOf(':');
      if (index > -1) {
        String groupId = permission.substring(index + 1);
        space = spaceService.getSpaceByGroupId(groupId);
      }
    }
    return space;
  }
}
