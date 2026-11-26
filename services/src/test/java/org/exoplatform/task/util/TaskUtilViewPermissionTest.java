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
package org.exoplatform.task.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

public class TaskUtilViewPermissionTest {

  @After
  public void clearConversationState() {
    ConversationState.setCurrent(null);
  }

  private TaskService taskService() {
    TaskService taskService = mock(TaskService.class);
    when(taskService.getMentionedUsers(9L)).thenReturn(Collections.emptySet());
    when(taskService.getCoworker(9L)).thenReturn(Set.of("coworker"));
    return taskService;
  }

  private TaskDto task() {
    TaskDto task = new TaskDto();
    task.setId(9L);
    task.setAssignee("assignee");
    task.setCreatedBy("creator");
    return task;
  }

  @Test
  public void theViewRightFollowsTheGivenViewerWhateverTheUserOfTheThread() {
    // The thread belongs to somebody who has nothing to do with the task
    ConversationState.setCurrent(new ConversationState(new Identity("stranger")));

    assertTrue(TaskUtil.hasViewPermission(taskService(), task(), new Identity("assignee")));
    assertTrue(TaskUtil.hasViewPermission(taskService(), task(), new Identity("coworker")));
    assertFalse(TaskUtil.hasViewPermission(taskService(), task(), new Identity("stranger")));
  }

  @Test
  public void theViewRightNeedsNoUserOnTheThread() {
    ConversationState.setCurrent(null);

    assertTrue(TaskUtil.hasViewPermission(taskService(), task(), new Identity("creator")));
  }

  @Test
  public void noViewerHasNoViewRight() {
    assertFalse(TaskUtil.hasViewPermission(taskService(), task(), null));
  }

}
