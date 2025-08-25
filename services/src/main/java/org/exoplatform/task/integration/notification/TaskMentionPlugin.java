/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.exoplatform.task.integration.notification;

import java.util.Set;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;

public class TaskMentionPlugin extends AbstractNotificationPlugin {
  public static final String ID = "TaskMentionedPlugin";

  public TaskMentionPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public NotificationInfo makeNotification(NotificationContext ctx) {
    TaskDto task = ctx.value(NotificationUtils.TASK);
    CommentDto comment = ctx.value(NotificationUtils.COMMENT);
    NotificationInfo info = super.makeNotification(ctx);
    info.with(NotificationUtils.COMMENT_TEXT, MentionUtils.substituteUsernames(getPortalOwner(), comment.getComment()));
    // Override the activityId
    String projectId = "project.";
    if (task.getStatus() != null && task.getStatus().getProject() != null) {
      projectId += String.valueOf(task.getStatus().getProject().getId());
    }
    info.with(NotificationUtils.ACTIVITY_ID, projectId);
    return info;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean isValid(NotificationContext ctx) {
    Set<String> mentioned = ctx.value(NotificationUtils.MENTIONED);
    return mentioned != null && !mentioned.isEmpty();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Set<String> getReceiver(TaskDto task, NotificationContext ctx) {
    return ctx.value(NotificationUtils.MENTIONED);
  }
}
