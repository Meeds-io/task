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

import static org.exoplatform.task.util.TaskUtil.TASK_COMMENT_DELETED;
import static org.exoplatform.task.util.TaskUtil.TASK_DELETED;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.service.TaskPayload;

import io.meeds.task.plugin.TaskCommentAclPlugin;
import io.meeds.social.reaction.service.ReactionService;

import jakarta.annotation.PostConstruct;

/**
 * Deletes the reactions of a task comment once deleted
 */
@Component
public class TaskReactionCleanupListener extends Listener<Object, Object> {

  private final PortalContainer container;

  private final ReactionService reactionService;

  @Autowired
  public TaskReactionCleanupListener(PortalContainer container,
                                     ReactionService reactionService) {
    this.container = container;
    this.reactionService = reactionService;
  }

  @PostConstruct
  public void init() {
    ListenerService listenerService = container.getComponentInstanceOfType(ListenerService.class);
    listenerService.addListener(TASK_COMMENT_DELETED, this);
    listenerService.addListener(TASK_DELETED, this);
  }

  @Override
  public void onEvent(Event<Object, Object> event) throws Exception {
    if (TASK_COMMENT_DELETED.equals(event.getEventName()) && event.getData() instanceof CommentDto comment) {
      // The replies go with their comment by cascade, with no event of their own: the comment carries them
      reactionService.deleteObjectReactions(TaskCommentAclPlugin.OBJECT_TYPE, String.valueOf(comment.getId()));
      if (comment.getSubComments() != null) {
        comment.getSubComments()
               .forEach(reply -> reactionService.deleteObjectReactions(TaskCommentAclPlugin.OBJECT_TYPE,
                                                                       String.valueOf(reply.getId())));
      }
    } else if (TASK_DELETED.equals(event.getEventName()) && event.getData() instanceof TaskPayload payload
               && payload.after() != null) {
      // The comments of a deleted task, and their replies, go by cascade as well
      reactionService.deleteObjectReactionsByParent(TaskCommentAclPlugin.OBJECT_TYPE, String.valueOf(payload.after().getId()));
    }
  }

}
