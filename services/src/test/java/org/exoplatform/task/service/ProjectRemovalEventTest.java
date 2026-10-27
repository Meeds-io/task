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
package org.exoplatform.task.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.LongConsumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.service.impl.ProjectServiceImpl;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.util.TaskUtil;

/**
 * Checks what {@link TaskUtil#TASKS_REMOVED} names when a project removal fails
 * midway.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectRemovalEventTest {

  private static final long PROJECT_ID = 3L;

  @Mock
  private ProjectStorage    projectStorage;

  @Mock
  private ListenerService   listenerService;

  private ProjectService    projectService;

  /**
   * Creates the service over the mocked storage.
   */
  @Before
  public void setUp() throws Exception {
    projectService = new ProjectServiceImpl(null, null, null, projectStorage, listenerService);
    when(projectStorage.getProject(PROJECT_ID)).thenReturn(new ProjectDto());
  }

  /**
   * A removal that fails after one task's deletion returned still broadcasts
   * that task, and names no project, which was not deleted.
   */
  @Test
  public void testFailedRemovalBroadcastsOnlyTheDeletedTasks() throws Exception {
    doAnswer(invocation -> {
      invocation.<LongConsumer> getArgument(2).accept(11L);
      throw new IllegalStateException("second task deletion failed");
    }).when(projectStorage).removeProject(eq(PROJECT_ID), eq(true), any(), any());

    assertThrows(IllegalStateException.class, () -> projectService.removeProject(PROJECT_ID, true));

    ArgumentCaptor<Object> data = ArgumentCaptor.forClass(Object.class);
    verify(listenerService).broadcast(eq(TaskUtil.TASKS_REMOVED), eq((Object) PROJECT_ID), data.capture());
    TasksRemovedPayload payload = (TasksRemovedPayload) data.getValue();
    assertEquals(List.of(), payload.projectIds());
    assertEquals(List.of(11L), payload.taskIds());
  }

  /**
   * A removal that fails before deleting anything broadcasts nothing.
   */
  @Test
  public void testRemovalFailingBeforeAnyDeletionBroadcastsNothing() throws Exception {
    doAnswer(invocation -> {
      throw new IllegalStateException("first task deletion failed");
    }).when(projectStorage).removeProject(anyLong(), anyBoolean(), any(), any());

    assertThrows(IllegalStateException.class, () -> projectService.removeProject(PROJECT_ID, false));

    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

}
