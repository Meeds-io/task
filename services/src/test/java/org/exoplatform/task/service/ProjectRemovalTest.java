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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.service.impl.ProjectServiceImpl;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.util.TaskUtil;

/**
 * Removes projects through the real storages on HSQLDB and checks the
 * {@link TaskUtil#TASKS_REMOVED} event the removal broadcasts.
 */
public class ProjectRemovalTest extends AbstractTest {

  private DAOHandler      daoHandler;

  private ListenerService listenerService;

  private ProjectService  projectService;

  private Project         project;

  private Project         child;

  private Task            firstTask;

  private Task            secondTask;

  private Task            childTask;

  /**
   * Creates a project with two tasks, and a child project with one task.
   */
  @Before
  public void setUp() {
    PortalContainer container = PortalContainer.getInstance();
    daoHandler = container.getComponentInstanceOfType(DAOHandler.class);
    listenerService = mock(ListenerService.class);
    projectService = new ProjectServiceImpl(null,
                                            null,
                                            daoHandler,
                                            container.getComponentInstanceOfType(ProjectStorage.class),
                                            listenerService);
    project = newProject("Parent", null);
    child = newProject("Child", project);
    Status status = newStatus(project);
    Status childStatus = newStatus(child);
    firstTask = newTask("First", status);
    secondTask = newTask("Second", status);
    childTask = newTask("Child task", childStatus);
    RequestLifeCycle.restartTransaction();
  }

  /**
   * Deletes what a test left.
   */
  @After
  public void tearDown() {
    deleteAll();
  }

  /**
   * Removing a project with its children deletes every task and broadcasts
   * one event naming both projects and the three tasks, the child's task once
   * the project's deletion has returned.
   */
  @Test
  public void testRemoveProjectWithChildrenBroadcastsEveryRemovedId() throws Exception {
    projectService.removeProject(project.getId(), true);

    TasksRemovedPayload payload = capturePayload(project.getId());
    assertEquals(List.of(project.getId(), child.getId()), payload.projectIds());
    assertEquals(List.of(firstTask.getId(), secondTask.getId(), childTask.getId()), payload.taskIds());
    RequestLifeCycle.restartTransaction();
    assertNull(daoHandler.getProjectHandler().find(child.getId()));
    assertNull(daoHandler.getTaskHandler().find(childTask.getId()));
  }

  /**
   * Removing a project without its children keeps the child and its task, and
   * the event names only the project and its own tasks.
   */
  @Test
  public void testRemoveProjectWithoutChildrenBroadcastsOnlyItsOwnIds() throws Exception {
    projectService.removeProject(project.getId(), false);

    TasksRemovedPayload payload = capturePayload(project.getId());
    assertEquals(List.of(project.getId()), payload.projectIds());
    assertEquals(List.of(firstTask.getId(), secondTask.getId()), payload.taskIds());
    RequestLifeCycle.restartTransaction();
    assertNotNull(daoHandler.getProjectHandler().find(child.getId()));
    assertNotNull(daoHandler.getTaskHandler().find(childTask.getId()));
  }

  /**
   * Captures the data of the {@link TaskUtil#TASKS_REMOVED} event.
   *
   * @param projectId the removed project id, expected as the event source
   * @return the event data
   */
  private TasksRemovedPayload capturePayload(long projectId) throws Exception {
    ArgumentCaptor<Object> data = ArgumentCaptor.forClass(Object.class);
    verify(listenerService).broadcast(eq(TaskUtil.TASKS_REMOVED), eq((Object) projectId), data.capture());
    return (TasksRemovedPayload) data.getValue();
  }

  /**
   * Creates a project.
   *
   * @param name the project name
   * @param parent the parent project, or null
   * @return the created project
   */
  private Project newProject(String name, Project parent) {
    Project created = new Project();
    created.setName(name);
    created.setParent(parent);
    return daoHandler.getProjectHandler().create(created);
  }

  /**
   * Creates the status of a project.
   *
   * @param statusProject the project
   * @return the created status
   */
  private Status newStatus(Project statusProject) {
    Status status = new Status();
    status.setName("To do");
    status.setRank(1);
    status.setProject(statusProject);
    return daoHandler.getStatusHandler().create(status);
  }

  /**
   * Creates a task in a status.
   *
   * @param title the task title
   * @param status the status
   * @return the created task
   */
  private Task newTask(String title, Status status) {
    Task task = new Task();
    task.setTitle(title);
    task.setCreatedBy("root");
    task.setCreatedTime(new Date());
    task.setStatus(status);
    return daoHandler.getTaskHandler().create(task);
  }

}
