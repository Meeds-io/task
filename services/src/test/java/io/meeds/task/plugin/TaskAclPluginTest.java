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
package io.meeds.task.plugin;

import static io.meeds.portal.plugin.AclPlugin.VIEW_PERMISSION_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

/**
 * Pins that the task VIEW permission the platform asks for a person (to show
 * them a favorite, a link or a comment) is judged on that person, whoever is
 * signed in.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskAclPluginTest {

  private static final long                 TASK_ID = 11L;

  private static MockedStatic<CommonsUtils> commonsUtils;

  @Mock
  private TaskService                       taskService;

  @Mock
  private UserACL                           userAcl;

  @InjectMocks
  private TaskAclPlugin                     plugin;

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
   * Serves a task assigned to {@code assignee}, with no project, coworker or
   * mention.
   *
   * @throws Exception never, the task service is mocked
   */
  @Before
  public void setUp() throws Exception {
    commonsUtils.when(() -> CommonsUtils.getService(UserACL.class)).thenReturn(userAcl);
    lenient().when(userAcl.getAdminGroups()).thenReturn("/platform/administrators");
    lenient().when(taskService.getCoworker(anyLong())).thenReturn(Collections.emptySet());
    lenient().when(taskService.getMentionedUsers(anyLong())).thenReturn(Collections.emptySet());
    TaskDto task = new TaskDto();
    task.setId(TASK_ID);
    task.setAssignee("assignee");
    when(taskService.getTask(TASK_ID)).thenReturn(task);
  }

  /**
   * Leaves no signed-in user behind for the next test.
   */
  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  /**
   * An outsider is refused while the assignee is signed in.
   */
  @Test
  public void refusesAnOutsiderWhileTheAssigneeIsSignedIn() {
    ConversationState.setCurrent(new ConversationState(new Identity("assignee")));
    assertFalse(plugin.hasPermission(String.valueOf(TASK_ID), VIEW_PERMISSION_TYPE, new Identity("outsider")));
  }

  /**
   * The assignee is allowed while an outsider is signed in, and while nobody
   * is.
   */
  @Test
  public void allowsTheAssigneeWhoeverIsSignedIn() {
    ConversationState.setCurrent(new ConversationState(new Identity("outsider")));
    assertTrue(plugin.hasPermission(String.valueOf(TASK_ID), VIEW_PERMISSION_TYPE, new Identity("assignee")));
    ConversationState.setCurrent(null);
    assertTrue(plugin.hasPermission(String.valueOf(TASK_ID), VIEW_PERMISSION_TYPE, new Identity("assignee")));
    assertFalse(plugin.hasPermission(String.valueOf(TASK_ID), VIEW_PERMISSION_TYPE, new Identity("outsider")));
  }

}
