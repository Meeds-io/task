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
package io.meeds.task.service;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.portal.config.UserACL;
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

/**
 * Decides who is notified of a reaction on a task comment, and notifies them.
 * The author of the comment is the only receiver, provided that the author
 * still sees the task and isn't the one who reacted. Changing a reaction
 * doesn't notify again (the listener only handles the creation).
 */
@Service
public class TaskReactionNotificationService {

  private final ReactionService reactionService;

  private final TaskService     taskService;

  private final CommentService  commentService;

  private final UserACL         userAcl;

  @Autowired
  public TaskReactionNotificationService(ReactionService reactionService,
                                         TaskService taskService,
                                         CommentService commentService,
                                         UserACL userAcl) {
    this.reactionService = reactionService;
    this.taskService = taskService;
    this.commentService = commentService;
    this.userAcl = userAcl;
  }

  /**
   * Notifies the author of a task comment of a reaction to it. Reactions on
   * other objects, missing comments and tasks, and the reaction of the author
   * on its own comment are ignored.
   *
   * @param reaction the created {@link Reaction}
   * @param reactor user name of the one who reacted
   */
  public void notifyReaction(Reaction reaction, String reactor) {
    if (reaction == null || StringUtils.isBlank(reactor) || !TaskCommentAclPlugin.OBJECT_TYPE.equals(reaction.getObjectType())) {
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

  /**
   * @param task the {@link TaskDto} of the comment
   * @param comment the commented {@link CommentDto}
   * @param reactor user name of the one who reacted
   * @return the user names to notify: the author of the comment, unless it is
   *         the reactor or no longer sees the task (a participant of the
   *         project, or someone who works on a task without project)
   */
  public Set<String> getReceivers(TaskDto task, CommentDto comment, String reactor) {
    Set<String> receivers = new HashSet<>();
    receivers.add(comment.getAuthor());
    receivers.removeIf(user -> StringUtils.isBlank(user) || StringUtils.equals(user, reactor));
    if (task.getStatus() != null && task.getStatus().getProject() != null) {
      ProjectDto project = task.getStatus().getProject();
      receivers.removeIf(user -> !ProjectUtil.isProjectParticipant(userAcl, user, project));
    } else {
      // A task with no project is only seen by those who work on it
      receivers.removeIf(user -> !isTaskParticipant(task, user));
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

  private boolean isTaskParticipant(TaskDto task, String user) {
    return StringUtils.equals(task.getCreatedBy(), user)
           || StringUtils.equals(task.getAssignee(), user)
           || task.getCoworker() != null && task.getCoworker().contains(user)
           || task.getWatcher() != null && task.getWatcher().contains(user);
  }

}
