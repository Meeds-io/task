/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.task.integration;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

/**
 * A comment's mention notifies only the mentioned users who may read the task
 * (EXO-92405).
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskCommentNotificationListenerTest {

  private static final String              SPACE_GROUP = "/spaces/team";

  private static MockedStatic<CommonsUtils> commonsUtils;

  @Mock
  private TaskService                      taskService;

  @Mock
  private UserACL                          userAcl;

  private TaskDto                          task;

  /**
   * Routes the platform-admin lookup to a mocked {@link UserACL}.
   */
  @BeforeClass
  public static void initClassContext() {
    commonsUtils = mockStatic(CommonsUtils.class);
  }

  /**
   * Releases the static mock.
   */
  @AfterClass
  public static void cleanClassContext() {
    commonsUtils.close();
  }

  /**
   * Builds a task of a project managed by the space's managers, with one
   * participant, which a comment mentions {@code member} and {@code outsider}
   * in.
   */
  @Before
  public void setUp() {
    commonsUtils.when(() -> CommonsUtils.getService(UserACL.class)).thenReturn(userAcl);
    lenient().when(userAcl.getAdminGroups()).thenReturn("/platform/administrators");
    lenient().when(taskService.getCoworker(anyLong())).thenReturn(Set.of());
    lenient().when(taskService.getMentionedUsers(anyLong())).thenReturn(Set.of("member", "outsider"));

    ProjectDto project = new ProjectDto();
    project.setManager(Set.of("manager:" + SPACE_GROUP));
    project.setParticipator(Set.of("participant"));
    StatusDto status = new StatusDto();
    status.setProject(project);
    task = new TaskDto();
    task.setId(7L);
    task.setCreatedBy("creator");
    task.setStatus(status);

    when(userAcl.getUserIdentity("member")).thenReturn(new Identity("member", Set.of(new MembershipEntry(SPACE_GROUP, "member"))));
    when(userAcl.getUserIdentity("outsider")).thenReturn(new Identity("outsider"));
  }

  /**
   * A mentioned member of the space is notified; a mentioned user outside it,
   * or one who no longer exists, is not.
   */
  @Test
  public void notifiesTheMentionedReadersOnly() {
    Set<String> mentioned = new HashSet<>(Set.of("member", "outsider", "gone"));
    TaskCommentNotificationListener.keepMentionedReaders(mentioned, task, taskService, userAcl);
    assertEquals(Set.of("member"), mentioned);
  }

  /**
   * On a personal task, with no space, every mentioned user is notified.
   */
  @Test
  public void notifiesEveryMentionedUserOnAPersonalTask() {
    task.setStatus(null);
    Set<String> mentioned = new HashSet<>(Set.of("member", "outsider"));
    TaskCommentNotificationListener.keepMentionedReaders(mentioned, task, taskService, userAcl);
    assertEquals(Set.of("member", "outsider"), mentioned);
  }
}
