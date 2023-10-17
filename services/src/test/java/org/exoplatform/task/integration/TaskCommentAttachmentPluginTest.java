/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.integration;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.TaskUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskCommentAttachmentPluginTest {

  private static MockedStatic<TaskUtil> TASK_UTIL;

  @Mock
  private TaskService                   taskService;

  @Mock
  private CommentService                commentService;

  @Mock
  private ProjectService                projectService;

  @Mock
  private SpaceService                  spaceService;

  private TaskCommentAttachmentPlugin   taskCommentAttachmentPlugin;

  @BeforeClass
  public static void initClassContext() {
    TASK_UTIL = mockStatic(TaskUtil.class);
  }

  @AfterClass
  public static void cleanClassContext() {
    TASK_UTIL.close();
  }

  @Before
  public void setUp() throws Exception {
    taskCommentAttachmentPlugin = new TaskCommentAttachmentPlugin(taskService, commentService, projectService, spaceService);
  }

  @Test
  public void testHasAccessPermission() throws ObjectNotFoundException {
    long commentId = 2l;
    Identity userIdentity = Mockito.mock(Identity.class);

    CommentDto comment = Mockito.mock(CommentDto.class);
    when(commentService.getComment(commentId)).thenReturn(comment);
    TaskDto task = Mockito.mock(TaskDto.class);
    when(comment.getTask()).thenReturn(task);

    TASK_UTIL.when(() -> TaskUtil.hasViewPermission(taskService, task, userIdentity)).thenReturn(true);
    assertTrue(taskCommentAttachmentPlugin.hasAccessPermission(userIdentity, String.valueOf(commentId)));
  }

  @Test
  public void testHasEditPermission() throws  ObjectNotFoundException {
    long commentId = 2l;
    Identity userIdentity = Mockito.mock(Identity.class);
    String rootId = "root";

    CommentDto comment = Mockito.mock(CommentDto.class);
    when(commentService.getComment(commentId)).thenReturn(comment);
    when(userIdentity.getUserId()).thenReturn(rootId);
    when(comment.getAuthor()).thenReturn(rootId);
    assertTrue(taskCommentAttachmentPlugin.hasEditPermission(userIdentity, String.valueOf(commentId)));
  }

  @Test
  public void testGetSpaceId() throws ObjectNotFoundException {
    long commentId = 2l;
    long projectId = 12l;
    Set<String> permissions = new HashSet<String>();
    String groupId = "space_test";
    permissions.add("spaces:space_test");
    long spaceId = 5l;

    CommentDto comment = Mockito.mock(CommentDto.class);
    when(commentService.getComment(commentId)).thenReturn(comment);

    TaskDto task = Mockito.mock(TaskDto.class);
    when(comment.getTask()).thenReturn(task);

    StatusDto taskStatus = Mockito.mock(StatusDto.class);
    when(task.getStatus()).thenReturn(taskStatus);

    ProjectDto project = Mockito.mock(ProjectDto.class);
    when(taskStatus.getProject()).thenReturn(project);
    when(project.getId()).thenReturn(projectId);

    Space space = Mockito.mock(Space.class);

    when(projectService.getManager(projectId)).thenReturn(permissions);
    when(spaceService.getSpaceByGroupId(groupId)).thenReturn(space);
    assertNotNull(space);
    when(space.getId()).thenReturn(String.valueOf(spaceId));
    assertEquals(taskCommentAttachmentPlugin.getSpaceId(String.valueOf(commentId)), spaceId);
  }
}
