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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.TaskService;

import io.meeds.social.reaction.model.Reaction;
import io.meeds.social.reaction.service.ReactionService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TaskReactionNotificationListenerTest {

  @Mock
  private PortalContainer                  container;

  @Mock
  private ReactionService                  reactionService;

  @Mock
  private TaskService                      taskService;

  @Mock
  private CommentService                   commentService;

  @Mock
  private UserACL                          userAcl;

  @InjectMocks
  private TaskReactionNotificationListener listener;

  private TaskDto                          task;

  private CommentDto                       comment;

  @Before
  public void setUp() throws Exception {
    task = new TaskDto();
    task.setId(9L);
    task.setCreatedBy("creator");
    task.setAssignee("assignee");
    comment = new CommentDto();
    comment.setId(5L);
    comment.setAuthor("author");
    comment.setTask(task);
    when(commentService.getComment(5L)).thenReturn(comment);
    when(taskService.getTask(9L)).thenReturn(task);
  }

  @Test
  public void initRegistersTheReactionCreationEvent() {
    ListenerService listenerService = mock(ListenerService.class);
    when(container.getComponentInstanceOfType(ListenerService.class)).thenReturn(listenerService);

    listener.init();

    verify(listenerService).addListener(ReactionService.REACTION_CREATED_EVENT_NAME, listener);
  }

  @Test
  public void onlyTheCommentAuthorIsNotified() {
    assertEquals(Set.of("author"), listener.getReceivers(task, comment, "john"));
  }

  @Test
  public void nobodyIsNotifiedOfItsOwnReaction() {
    assertTrue(listener.getReceivers(task, comment, "author").isEmpty());
  }

  @Test
  public void aCommentWithoutAuthorNotifiesNobody() {
    comment.setAuthor(null);

    assertTrue(listener.getReceivers(task, comment, "john").isEmpty());
  }

  @Test
  public void reactionsOnOtherObjectsAreIgnored() throws Exception {
    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME,
                                 new Reaction(1L, "like", "activity", "5"),
                                 "john"));
    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME,
                                 new Reaction(1L, "like", "task", "9"),
                                 "john"));

    verify(commentService, never()).getComment(anyLong());
  }

  @Test
  public void unknownCommentsAndTasksAreIgnored() throws Exception {
    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME,
                                 new Reaction(1L, "like", "taskComment", "404"),
                                 "john"));
    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME,
                                 new Reaction(1L, "like", "taskComment", "abc"),
                                 "john"));
    when(taskService.getTask(9L)).thenThrow(new EntityNotFoundException(9L, TaskDto.class));
    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME,
                                 new Reaction(1L, "like", "taskComment", "5"),
                                 "john"));
    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME, null, "john"));
    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME,
                                 new Reaction(1L, "like", "taskComment", "5"),
                                 " "));

    verify(reactionService, never()).getReactionOptions();
  }

  @Test
  public void aReactionOfTheAuthorOnItsOwnCommentDoesNothing() throws Exception {
    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME,
                                 new Reaction(1L, "like", "taskComment", "5"),
                                 "author"));

    verify(reactionService, never()).getReactionOptions();
    verify(taskService).getTask(9L);
    verify(commentService).getComment(any(Long.class));
  }

}
