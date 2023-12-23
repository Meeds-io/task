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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentPlugin;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.TaskUtil;


public class TaskCommentAttachmentPlugin extends AttachmentPlugin {

  public static final String TASK_COMMENT_OBJECT_TYPE                 = "taskComment";

  public static final String TASK_COMMENT_NOT_FOUND_EXCEPTION_MESSAGE = "Task comment with id %s wasn't found";

  private TaskService        taskService;

  private CommentService     commentService;

  private ProjectService     projectService;

  private SpaceService       spaceService;

  public TaskCommentAttachmentPlugin(TaskService taskService,
                                     CommentService commentService,
                                     ProjectService projectService,
                                     SpaceService spaceService) {
    this.taskService = taskService;
    this.commentService = commentService;
    this.projectService = projectService;
    this.spaceService = spaceService;
  }

  @Override
  public String getObjectType() {
    return TASK_COMMENT_OBJECT_TYPE;
  }

  @Override
  public boolean hasAccessPermission(Identity userIdentity, String entityId) throws ObjectNotFoundException {
    try {
      CommentDto comment = commentService.getComment(Long.parseLong(entityId));
      return TaskUtil.hasViewPermission(taskService, comment.getTask(), userIdentity);
    } catch (Exception e) {
      throw new ObjectNotFoundException(String.format(TASK_COMMENT_NOT_FOUND_EXCEPTION_MESSAGE, entityId));
    }
  }

  @Override
  public boolean hasEditPermission(Identity userIdentity, String entityId) throws ObjectNotFoundException {
    try {
      CommentDto comment = commentService.getComment(Long.parseLong(entityId));
      return StringUtils.equals(comment.getAuthor(), userIdentity.getUserId());
    } catch (Exception e) {
      throw new ObjectNotFoundException(String.format(TASK_COMMENT_NOT_FOUND_EXCEPTION_MESSAGE, entityId));
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
      CommentDto comment = commentService.getComment(Long.parseLong(entityId));
      Space space = comment == null || comment.getTask().getStatus() == null || comment.getTask().getStatus().getProject() == null ? null : getProjectSpace(comment.getTask().getStatus().getProject().getId());
      if (space != null) {
        spaceId = Long.parseLong(space.getId());
      }
    } catch (Exception e) {
      throw new ObjectNotFoundException(String.format(TASK_COMMENT_NOT_FOUND_EXCEPTION_MESSAGE, entityId));
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
