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
package io.meeds.task.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.service.CommentService;

import io.meeds.portal.plugin.AclPlugin;
import io.meeds.social.reaction.plugin.ReactionObjectPlugin;
import io.meeds.social.reaction.service.ReactionService;

import jakarta.annotation.PostConstruct;

/**
 * Makes the comments (and replies) of a task reactable with the Social
 * reactions framework.
 * <p>
 * NOTE: the description of a task is intentionally not reactable yet. To add
 * it later: register a second {@link ReactionObjectPlugin} for the 'task'
 * object type (see {@link TaskAclPlugin#OBJECT_TYPE}: existence check through
 * the {@code TaskService}, view permission through the {@code UserACL}), render
 * {@code <object-reactions object-type="task" :object-id="task.id" />} under the
 * description in {@code TaskDrawer.vue}, and extend
 * {@code TaskReactionNotificationListener} and {@code TaskReactionCleanupListener}
 * (the creator and the assignee of the task being the ones to notify).
 */
@Component
public class TaskCommentReactionObjectPlugin implements ReactionObjectPlugin {

  public static final String OBJECT_TYPE = TaskCommentAclPlugin.OBJECT_TYPE;

  private final ReactionService reactionService;

  private final CommentService commentService;

  private final UserACL userAcl;

  @Autowired
  public TaskCommentReactionObjectPlugin(ReactionService reactionService,
                                         CommentService commentService,
                                         UserACL userAcl) {
    this.reactionService = reactionService;
    this.commentService = commentService;
    this.userAcl = userAcl;
  }

  @PostConstruct
  public void init() {
    reactionService.addObjectPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public MetadataObject getMetadataObject(String objectId) {
    try {
      CommentDto comment = commentService.getComment(Long.parseLong(objectId));
      return comment == null || comment.getTask() == null ? null :
                                                          new MetadataObject(OBJECT_TYPE,
                                                                             objectId,
                                                                             String.valueOf(comment.getTask().getId()));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public boolean hasViewPermission(String objectId, Identity viewer) {
    return userAcl.hasPermission(OBJECT_TYPE, objectId, AclPlugin.VIEW_PERMISSION_TYPE, viewer);
  }

}
