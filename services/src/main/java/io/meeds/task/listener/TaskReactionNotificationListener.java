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
package io.meeds.task.listener;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.integration.notification.NotificationUtils;
import org.exoplatform.task.integration.notification.TaskReactionPlugin;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.ProjectUtil;

import io.meeds.social.reaction.model.Reaction;
import io.meeds.social.reaction.model.ReactionOption;
import io.meeds.social.reaction.service.ReactionService;
import io.meeds.task.plugin.TaskCommentAclPlugin;

import jakarta.annotation.PostConstruct;

/**
 * Notifies the author of a task comment when someone reacts to it. Changing a
 * reaction doesn't notify again, and nobody is notified of its own reaction.
 */
@Component
public class TaskReactionNotificationListener extends Listener<Reaction, String> {

  @Autowired
  private PortalContainer container;

  @Autowired
  private ReactionService reactionService;

  @Autowired
  private TaskService     taskService;

  @Autowired
  private CommentService  commentService;

  @Autowired
  private UserACL         userAcl;

  @PostConstruct
  public void init() {
    container.getComponentInstanceOfType(ListenerService.class)
             .addListener(ReactionService.REACTION_CREATED_EVENT_NAME, this);
  }

  @Override
  public void onEvent(Event<Reaction, String> event) throws Exception {
    Reaction reaction = event.getSource();
    String reactor = event.getData();
    if (reaction == null || StringUtils.isBlank(reactor)) {
      return;
    }
    if (!TaskCommentAclPlugin.OBJECT_TYPE.equals(reaction.getObjectType())) {
      return;
    }
    CommentDto comment;
    TaskDto task;
    try {
      comment = commentService.getComment(Long.parseLong(reaction.getObjectId()));
      task = comment == null || comment.getTask() == null ? null : taskService.getTask(comment.getTask().getId());
    } catch (EntityNotFoundException | NumberFormatException e) {
      return;
    }
    if (task == null) {
      return;
    }
    Set<String> receivers = getReceivers(task, comment, reactor);
    if (receivers.isEmpty()) {
      return;
    }
    NotificationContext ctx = NotificationContextImpl.cloneInstance()
                                                     .append(NotificationUtils.TASK, task)
                                                     .append(NotificationUtils.COMMENT, comment)
                                                     .append(NotificationUtils.CREATOR, reactor)
                                                     .append(NotificationUtils.RECEIVERS, receivers)
                                                     .append(NotificationUtils.REACTION_EMOJI_ARG,
                                                             getEmoji(reaction.getReactionId()));
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(TaskReactionPlugin.ID))).execute(ctx);
  }

  Set<String> getReceivers(TaskDto task, CommentDto comment, String reactor) {
    Set<String> receivers = new HashSet<>();
    receivers.add(comment.getAuthor());
    receivers.removeIf(user -> StringUtils.isBlank(user) || StringUtils.equals(user, reactor));
    if (task.getStatus() != null && task.getStatus().getProject() != null) {
      ProjectDto project = task.getStatus().getProject();
      receivers.removeIf(user -> !ProjectUtil.isProjectParticipant(userAcl, user, project));
    }
    return receivers;
  }

  private String getEmoji(String reactionId) {
    return reactionService.getReactionOptions()
                          .stream()
                          .filter(option -> StringUtils.equals(option.getId(), reactionId))
                          .map(ReactionOption::getEmoji)
                          .findFirst()
                          .orElse(reactionId);
  }

}
