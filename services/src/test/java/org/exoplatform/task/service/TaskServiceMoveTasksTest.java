/*
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package org.exoplatform.task.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.util.TaskUtil;

/**
 * {@link TaskService#moveTasks} against the real database: the move is one
 * transaction, so a task refused midway rolls back the tasks written before
 * it, and the update events are broadcast once the transaction is committed.
 * The annotation-removed mutant (no {@code atomic = true}) leaves the first
 * task moved in the refusal case; a broadcast inside the transaction finds it
 * still active.
 */
public class TaskServiceMoveTasksTest extends AbstractTest {

  private static final String  JOHN = "john";

  private static final String  MARY = "mary";

  private TaskService          taskService;

  private ProjectService       projectService;

  private StatusService        statusService;

  private ListenerService      listenerService;

  private EntityManagerService entityManagerService;

  private DAOHandler           daoHandler;

  /** One listener for the class, since a registered listener cannot be removed. */
  private static final List<Boolean> TRANSACTION_ACTIVE_AT_BROADCAST = new ArrayList<>();

  private static boolean       listenerRegistered;

  @Before
  public void init() {
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    taskService = container.getComponentInstanceOfType(TaskService.class);
    projectService = container.getComponentInstanceOfType(ProjectService.class);
    statusService = container.getComponentInstanceOfType(StatusService.class);
    listenerService = container.getComponentInstanceOfType(ListenerService.class);
    entityManagerService = container.getComponentInstanceOfType(EntityManagerService.class);
    daoHandler = container.getComponentInstanceOfType(DAOHandler.class);
    TRANSACTION_ACTIVE_AT_BROADCAST.clear();
    if (!listenerRegistered) {
      EntityManagerService service = entityManagerService;
      listenerService.addListener(TaskUtil.TASK_UPDATED, new Listener<>() {
        @Override
        public void onEvent(Event<Object, Object> event) {
          TRANSACTION_ACTIVE_AT_BROADCAST.add(service.getEntityManager().getTransaction().isActive());
        }
      });
      listenerRegistered = true;
    }
  }

  @After
  public void cleanData() {
    deleteAll();
  }

  @Test
  public void moveTasksMovesEveryTaskAndBroadcastsOnceCommitted() throws Exception {
    StatusDto target = defaultStatusOfProjectOf(JOHN);
    TaskDto first = createPersonalTask(JOHN);
    TaskDto second = createPersonalTask(JOHN);

    List<TaskDto> moved = taskService.moveTasks(List.of(first.getId(), second.getId()), target.getId(), new Identity(JOHN));

    assertEquals(2, moved.size());
    RequestLifeCycle.restartTransaction();
    assertEquals(target.getId(), storedStatusId(first.getId()));
    assertEquals(target.getId(), storedStatusId(second.getId()));
    assertEquals(2, TRANSACTION_ACTIVE_AT_BROADCAST.size());
    assertFalse("the update events are broadcast once the move is committed",
                TRANSACTION_ACTIVE_AT_BROADCAST.contains(Boolean.TRUE));
  }

  @Test
  public void moveTasksRefusedMidwayMovesNoneAndBroadcastsNothing() throws Exception {
    StatusDto target = defaultStatusOfProjectOf(JOHN);
    TaskDto johnsTask = createPersonalTask(JOHN);
    TaskDto marysTask = createPersonalTask(MARY);

    assertThrows(IllegalAccessException.class,
                 () -> taskService.moveTasks(List.of(johnsTask.getId(), marysTask.getId()),
                                             target.getId(),
                                             new Identity(JOHN)));

    RequestLifeCycle.restartTransaction();
    assertNull("the task written before the refusal is rolled back", storedStatusId(johnsTask.getId()));
    assertNull(storedStatusId(marysTask.getId()));
    assertTrue(TRANSACTION_ACTIVE_AT_BROADCAST.isEmpty());
  }

  private StatusDto defaultStatusOfProjectOf(String participant) {
    ProjectDto project = new ProjectDto();
    project.setName("Target project");
    Set<String> participants = new HashSet<>();
    participants.add(participant);
    project.setParticipator(participants);
    project.setManager(new HashSet<>());
    project = projectService.createProject(project);
    statusService.createInitialStatuses(project);
    RequestLifeCycle.restartTransaction();
    return statusService.getDefaultStatus(project.getId());
  }

  private TaskDto createPersonalTask(String assignee) {
    TaskDto task = new TaskDto();
    task.setTitle("Task of " + assignee);
    task.setAssignee(assignee);
    task.setCreatedBy(assignee);
    task = taskService.createTask(task);
    RequestLifeCycle.restartTransaction();
    return task;
  }

  private Long storedStatusId(long taskId) {
    Task task = daoHandler.getTaskHandler().find(taskId);
    return task.getStatus() == null ? null : task.getStatus().getId();
  }
}
