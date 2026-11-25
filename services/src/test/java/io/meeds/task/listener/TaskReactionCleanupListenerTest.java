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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.util.TaskUtil;

import io.meeds.social.reaction.service.ReactionService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TaskReactionCleanupListenerTest {

  @Mock
  private PortalContainer             container;

  @Mock
  private ReactionService             reactionService;

  @InjectMocks
  private TaskReactionCleanupListener listener;

  @Test
  public void initRegistersTheCommentDeletionEvent() {
    ListenerService listenerService = mock(ListenerService.class);
    when(container.getComponentInstanceOfType(ListenerService.class)).thenReturn(listenerService);

    listener.init();

    verify(listenerService).addListener(TaskUtil.TASK_COMMENT_DELETED, listener);
  }

  @Test
  public void commentDeletionDeletesTheCommentReactions() throws Exception {
    CommentDto comment = new CommentDto();
    comment.setId(5L);

    listener.onEvent(new Event<>(TaskUtil.TASK_COMMENT_DELETED, null, comment));

    verify(reactionService).deleteObjectReactions("taskComment", "5");
  }

  @Test
  public void otherEventsAreIgnored() throws Exception {
    listener.onEvent(new Event<>(TaskUtil.TASK_COMMENT_CREATED, null, new CommentDto()));

    verify(reactionService, never()).deleteObjectReactions(org.mockito.ArgumentMatchers.anyString(),
                                                           org.mockito.ArgumentMatchers.anyString());
  }

}
