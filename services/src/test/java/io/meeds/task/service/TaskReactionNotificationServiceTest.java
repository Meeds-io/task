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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.command.NotificationCommand;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.command.NotificationExecutor;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.integration.notification.NotificationUtils;
import org.exoplatform.task.integration.notification.TaskReactionPlugin;
import org.exoplatform.task.service.TaskService;

import io.meeds.social.reaction.model.Reaction;
import io.meeds.social.reaction.model.ReactionOption;
import io.meeds.social.reaction.service.ReactionService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TaskReactionNotificationServiceTest {

  @Mock
  private ReactionService                  reactionService;

  @Mock
  private TaskService                      taskService;

  @Mock
  private CommentService                   commentService;

  @Mock
  private UserACL                          userAcl;

  @InjectMocks
  private TaskReactionNotificationService  service;

  private TaskDto                          task;

  private CommentDto                       comment;

  @Before
  public void setUp() throws Exception {
    task = new TaskDto();
    task.setId(9L);
    task.setCreatedBy("creator");
    task.setAssignee("assignee");
    task.setCoworker(Set.of("author"));
    comment = new CommentDto();
    comment.setId(5L);
    comment.setAuthor("author");
    comment.setTask(task);
    when(commentService.getComment(5L)).thenReturn(comment);
    when(taskService.getTask(9L)).thenReturn(task);
  }

  @Test
  public void onlyTheCommentAuthorIsNotified() {
    assertEquals(Set.of("author"), service.getReceivers(task, comment, "john"));
  }

  @Test
  public void theAuthorOfACommentOnATaskWithNoProjectMustStillWorkOnIt() {
    comment.setAuthor("former");
    assertTrue(service.getReceivers(task, comment, "john").isEmpty());

    task.setCoworker(Set.of("former"));
    assertEquals(Set.of("former"), service.getReceivers(task, comment, "john"));

    task.setCoworker(null);
    task.setWatcher(Set.of("former"));
    assertEquals(Set.of("former"), service.getReceivers(task, comment, "john"));

    task.setWatcher(null);
    comment.setAuthor("assignee");
    assertEquals(Set.of("assignee"), service.getReceivers(task, comment, "john"));
    comment.setAuthor("creator");
    assertEquals(Set.of("creator"), service.getReceivers(task, comment, "john"));
  }

  private ProjectDto projectWithParticipators(String... participators) {
    ProjectDto project = new ProjectDto();
    project.setId(3L);
    project.setParticipator(Set.of(participators));
    StatusDto status = new StatusDto();
    status.setProject(project);
    task.setStatus(status);
    return project;
  }

  @Test
  public void aParticipantAuthorOfATaskOfAProjectIsNotified() {
    projectWithParticipators("author");

    assertEquals(Set.of("author"), service.getReceivers(task, comment, "john"));
  }

  @Test
  public void aNonParticipantAuthorOfATaskOfAProjectIsNotNotified() {
    projectWithParticipators("somebodyElse");
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity("author",
                                                                                                       List.of(new MembershipEntry("/platform/guests",
                                                                                                                                   "member")));
    when(userAcl.getUserIdentity("author")).thenReturn(identity);

    assertTrue(service.getReceivers(task, comment, "john").isEmpty());
  }

  @Test
  public void anAuthorParticipantByGroupMembershipIsNotified() {
    projectWithParticipators("*:/platform/projects");
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity("author",
                                                                                                       List.of(new MembershipEntry("/platform/projects",
                                                                                                                                   "member")));
    when(userAcl.getUserIdentity("author")).thenReturn(identity);

    assertEquals(Set.of("author"), service.getReceivers(task, comment, "john"));
  }

  @Test
  public void aProjectWithoutParticipantsNotifiesNobody() {
    ProjectDto project = projectWithParticipators("author");
    project.setParticipator(null);

    assertTrue(service.getReceivers(task, comment, "john").isEmpty());
  }

  @Test
  public void nobodyIsNotifiedOfItsOwnReaction() {
    assertTrue(service.getReceivers(task, comment, "author").isEmpty());
  }

  @Test
  public void aCommentWithoutAuthorNotifiesNobody() {
    comment.setAuthor(null);

    assertTrue(service.getReceivers(task, comment, "john").isEmpty());
  }

  @Test
  public void reactionsOnOtherObjectsAreIgnored() throws Exception {
    service.notifyReaction(new Reaction(1L, "like", "activity", "5"), "john");
    service.notifyReaction(new Reaction(1L, "like", "task", "9"), "john");

    verify(commentService, never()).getComment(anyLong());
  }

  @Test
  public void unknownCommentsAndTasksAreIgnored() throws Exception {
    service.notifyReaction(new Reaction(1L, "like", "taskComment", "404"), "john");
    service.notifyReaction(new Reaction(1L, "like", "taskComment", "abc"), "john");
    when(taskService.getTask(9L)).thenThrow(new EntityNotFoundException(9L, TaskDto.class));
    service.notifyReaction(new Reaction(1L, "like", "taskComment", "5"), "john");
    service.notifyReaction(null, "john");
    service.notifyReaction(new Reaction(1L, "like", "taskComment", "5"), " ");

    verify(reactionService, never()).getReactionOptions();
  }

  @Test
  public void aReactionOfTheAuthorOnItsOwnCommentDoesNothing() throws Exception {
    service.notifyReaction(new Reaction(1L, "like", "taskComment", "5"), "author");

    verify(reactionService, never()).getReactionOptions();
    verify(taskService).getTask(9L);
    verify(commentService).getComment(any(Long.class));
  }

  @Test
  public void aReactionOnACommentNotifiesItsAuthorWithTheEmojiOfTheOption() throws Exception {
    when(reactionService.getReactionOptions()).thenReturn(List.of(new ReactionOption("love",
                                                                                      "\u2764\uFE0F",
                                                                                      "l",
                                                                                      "l",
                                                                                      "l",
                                                                                      1,
                                                                                      null)));
    NotificationCommand command = mock(NotificationCommand.class);
    NotificationExecutor executor = mock(NotificationExecutor.class);
    NotificationContext ctx = mock(NotificationContext.class, Answers.RETURNS_SELF);
    when(ctx.getNotificationExecutor()).thenReturn(executor);
    when(ctx.makeCommand(PluginKey.key(TaskReactionPlugin.ID))).thenReturn(command);
    when(executor.with(command)).thenReturn(executor);
    try (MockedStatic<NotificationContextImpl> context = mockStatic(NotificationContextImpl.class)) {
      context.when(NotificationContextImpl::cloneInstance).thenReturn(ctx);

      service.notifyReaction(new Reaction(1L, "love", "taskComment", "5"), "john");

      verify(ctx).append(NotificationUtils.TASK, task);
      verify(ctx).append(NotificationUtils.COMMENT, comment);
      verify(ctx).append(NotificationUtils.CREATOR, "john");
      verify(ctx).append(NotificationUtils.RECEIVERS, Set.of("author"));
      verify(ctx).append(NotificationUtils.REACTION_EMOJI_ARG, "\u2764\uFE0F");
      verify(executor).execute(ctx);
    }
  }

  @Test
  public void aFreeEmojiIsItsOwnEmoji() throws Exception {
    NotificationCommand command = mock(NotificationCommand.class);
    NotificationExecutor executor = mock(NotificationExecutor.class);
    NotificationContext ctx = mock(NotificationContext.class, Answers.RETURNS_SELF);
    when(ctx.getNotificationExecutor()).thenReturn(executor);
    when(ctx.makeCommand(PluginKey.key(TaskReactionPlugin.ID))).thenReturn(command);
    when(executor.with(command)).thenReturn(executor);
    try (MockedStatic<NotificationContextImpl> context = mockStatic(NotificationContextImpl.class)) {
      context.when(NotificationContextImpl::cloneInstance).thenReturn(ctx);

      service.notifyReaction(new Reaction(1L, "\uD83D\uDE80", "taskComment", "5"), "john");

      verify(ctx).append(NotificationUtils.REACTION_EMOJI_ARG, "\uD83D\uDE80");
      verify(executor).execute(ctx);
    }
  }

}
