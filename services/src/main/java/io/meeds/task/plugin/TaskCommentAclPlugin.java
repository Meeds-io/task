/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.task.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.TaskUtil;

import io.meeds.portal.plugin.AclPlugin;

import jakarta.annotation.PostConstruct;

@Component
public class TaskCommentAclPlugin implements AclPlugin {

  public static final String OBJECT_TYPE = TaskPermanentLinkPlugin.OBJECT_TYPE;

  @Autowired
  private TaskService        taskService;

  @Autowired
  private CommentService     commentService;

  @Autowired
  private PortalContainer    container;

  @PostConstruct
  public void init() {
    container.getComponentInstanceOfType(UserACL.class).addAclPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasPermission(String objectId, String permissionType, Identity identity) {
    CommentDto comment = commentService.getComment(Long.parseLong(objectId));
    TaskDto task = comment.getTask();
    if (task == null) {
      return false;
    } else {
      return switch (permissionType) {
      case VIEW_PERMISSION_TYPE: {
        yield TaskUtil.hasViewPermission(taskService, task, identity);
      }
      case EDIT_PERMISSION_TYPE: {
        yield TaskUtil.hasEditPermission(taskService, task, identity);
      }
      case DELETE_PERMISSION_TYPE: {
        yield TaskUtil.hasDeletePermission(task, identity);
      }
      default:
        yield false;
      };
    }
  }

}
