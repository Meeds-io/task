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
package org.exoplatform.task.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.ProjectHandler;
import org.exoplatform.task.dao.StatusHandler;
import org.exoplatform.task.dao.TaskHandler;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.storage.impl.ProjectStorageImpl;

/**
 * Checks when a project removal reports the projects and the descendants'
 * tasks it deleted.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectStorageRemovalTest {

  @Mock
  private DAOHandler     daoHandler;

  @Mock
  private ProjectHandler projectHandler;

  @Mock
  private StatusHandler  statusHandler;

  @Mock
  private TaskHandler    taskHandler;

  private ProjectStorage projectStorage;

  private List<Long>     removedTaskIds;

  private List<Long>     removedProjectIds;

  /**
   * Mocks a project 3 with a child 4 holding the task 41, and no status of its
   * own.
   */
  @Before
  public void setUp() {
    projectStorage = new ProjectStorageImpl(daoHandler);
    removedTaskIds = new ArrayList<>();
    removedProjectIds = new ArrayList<>();
    Project parent = new Project();
    parent.setId(3L);
    Project child = new Project();
    child.setId(4L);
    when(daoHandler.getProjectHandler()).thenReturn(projectHandler);
    when(daoHandler.getStatusHandler()).thenReturn(statusHandler);
    when(daoHandler.getTaskHandler()).thenReturn(taskHandler);
    when(projectHandler.find(3L)).thenReturn(parent);
    when(projectHandler.find(4L)).thenReturn(child);
    when(projectHandler.findSubProjects(parent)).thenReturn(new ListAccessImpl<>(Project.class, List.of(child)));
    when(projectHandler.findSubProjects(child)).thenReturn(new ListAccessImpl<>(Project.class, List.of()));
    when(taskHandler.selectTaskField(any(TaskQuery.class), eq("id"))).thenReturn(List.of(41L));
    when(statusHandler.getStatuses(3L)).thenReturn(List.of());
  }

  /**
   * Once the project's deletion returned, the project, its child and the
   * child's task are reported.
   */
  @Test
  public void testReportsTheProjectsAndTheDescendantsTasksOnceDeleted() throws Exception {
    projectStorage.removeProject(3L, true, removedTaskIds::add, removedProjectIds::add);

    assertEquals(List.of(3L, 4L), removedProjectIds);
    assertEquals(List.of(41L), removedTaskIds);
  }

  /**
   * When the project's deletion fails, neither the projects nor the
   * descendants' tasks, which are deleted with it, are reported.
   */
  @Test
  public void testReportsNothingOfAProjectWhoseDeletionFailed() {
    when(projectHandler.removeProject(3L, true)).thenThrow(new IllegalStateException("project deletion failed"));

    assertThrows(IllegalStateException.class,
                 () -> projectStorage.removeProject(3L, true, removedTaskIds::add, removedProjectIds::add));

    assertEquals(List.of(), removedProjectIds);
    assertEquals(List.of(), removedTaskIds);
  }

}
