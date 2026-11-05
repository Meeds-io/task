/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.task.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

/**
 * The view right of a task for a given identity reads that identity's rights,
 * whoever the current conversation's user is: the access control plugins ask
 * it for any user.
 */
public class TaskUtilViewPermissionTest {

  private static final long             TASK_ID = 12L;

  private TaskService                   taskService;

  private TaskDto                       task;

  private MockedStatic<CommonsUtils>    commonsUtils;

  /**
   * A task created by mary in a project nobody here can view, with no
   * assignee, coworker or mention, while the conversation is root's.
   */
  @Before
  public void setUp() {
    taskService = mock(TaskService.class);
    when(taskService.getMentionedUsers(TASK_ID)).thenReturn(Set.of());
    when(taskService.getCoworker(TASK_ID)).thenReturn(Set.of());
    ProjectDto project = mock(ProjectDto.class);
    StatusDto status = mock(StatusDto.class);
    when(status.getProject()).thenReturn(project);
    task = mock(TaskDto.class);
    when(task.getId()).thenReturn(TASK_ID);
    when(task.getStatus()).thenReturn(status);
    when(task.getCreatedBy()).thenReturn("mary");
    UserACL userAcl = mock(UserACL.class);
    when(userAcl.getAdminGroups()).thenReturn("/platform/administrators");
    commonsUtils = mockStatic(CommonsUtils.class);
    commonsUtils.when(() -> CommonsUtils.getService(UserACL.class)).thenReturn(userAcl);
    ConversationState.setCurrent(new ConversationState(identity("root")));
  }

  /**
   * Releases the conversation and the static lookup.
   */
  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
    commonsUtils.close();
  }

  /**
   * The task's creator may view it, a stranger may not, whatever the
   * conversation's user may do. Mutant: the edit right read from the current
   * conversation, the two-argument {@code hasEditPermission}.
   */
  @Test
  public void theViewRightReadsTheGivenIdentityNotTheConversation() {
    assertTrue(TaskUtil.hasViewPermission(taskService, task, identity("mary")));
    assertFalse(TaskUtil.hasViewPermission(taskService, task, identity("john")));
    assertFalse(TaskUtil.hasViewPermission(taskService, task, null));
  }

  /**
   * @param userId a user
   * @return the user's identity, a member of the platform's users only
   */
  private static Identity identity(String userId) {
    return new Identity(userId, List.of(new MembershipEntry("/platform/users", "member")));
  }
}
