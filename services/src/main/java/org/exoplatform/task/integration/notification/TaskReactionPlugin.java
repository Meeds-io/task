/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;

/**
 * Notifies the author of a task comment that someone reacted to it
 */
public class TaskReactionPlugin extends AbstractNotificationPlugin {

  public static final String ID = "TaskReactionPlugin";

  public TaskReactionPlugin(InitParams initParams) {
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
    info.with(NotificationUtils.TASKS, String.valueOf(task.getId()));
    info.with(NotificationUtils.REACTION_EMOJI, StringUtils.defaultString(ctx.value(NotificationUtils.REACTION_EMOJI_ARG)));
    info.with(NotificationUtils.COMMENT_TEXT, MentionUtils.substituteUsernames(getPortalOwner(), comment.getComment()));
    return info;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean isValid(NotificationContext ctx) {
    Set<String> receivers = ctx.value(NotificationUtils.RECEIVERS);
    return receivers != null && !receivers.isEmpty();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Set<String> getReceiver(TaskDto task, NotificationContext ctx) {
    return ctx.value(NotificationUtils.RECEIVERS);
  }

}
