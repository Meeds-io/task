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

import org.exoplatform.commons.exception.ObjectNotFoundException;
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
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.util.TaskUtil;

/**
 * {@link TaskService#unassignTasks} against the real database: the user leaves
 * the assignee and the coworkers of each task, the other coworkers stay, the
 * change is one transaction, so a task refused midway rolls back the tasks
 * written before it, and the update events are broadcast once the transaction
 * is committed.
 */
public class TaskServiceUnassignTasksTest extends AbstractTest {

  private static final String  JOHN = "john";

  private static final String  MARY = "mary";

  private static final String  JACK = "jack";

  private TaskService          taskService;

  private DAOHandler           daoHandler;

  /** One listener for the class, since a registered listener cannot be removed. */
  private static final List<Boolean> TRANSACTION_ACTIVE_AT_BROADCAST = new ArrayList<>();

  private static boolean       listenerRegistered;

  @Before
  public void init() {
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    taskService = container.getComponentInstanceOfType(TaskService.class);
    daoHandler = container.getComponentInstanceOfType(DAOHandler.class);
    TRANSACTION_ACTIVE_AT_BROADCAST.clear();
    if (!listenerRegistered) {
      EntityManagerService service = container.getComponentInstanceOfType(EntityManagerService.class);
      container.getComponentInstanceOfType(ListenerService.class).addListener(TaskUtil.TASK_UPDATED, new Listener<>() {
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
  public void unassignTasksRemovesTheUserOnlyAndBroadcastsOnceCommitted() throws Exception {
    TaskDto assigned = createTask(JOHN, JACK);
    TaskDto coworked = createTask(MARY, JOHN, JACK);

    List<TaskDto> updated = taskService.unassignTasks(List.of(assigned.getId(), coworked.getId()), new Identity(JOHN));
    RequestLifeCycle.restartTransaction();

    assertEquals(2, updated.size());
    assertNull(storedAssignee(assigned.getId()));
    assertEquals(Set.of(JACK), storedCoworkers(assigned.getId()));
    assertEquals(MARY, storedAssignee(coworked.getId()));
    assertEquals(Set.of(JACK), storedCoworkers(coworked.getId()));
    assertEquals(2, TRANSACTION_ACTIVE_AT_BROADCAST.size());
    assertFalse("An update event was broadcast before the change was committed",
                TRANSACTION_ACTIVE_AT_BROADCAST.contains(Boolean.TRUE));
  }

  @Test
  public void unassignTasksRefusedMidwayChangesNoneAndBroadcastsNothing() throws Exception {
    TaskDto johnsTask = createTask(JOHN, JACK);
    TaskDto marysTask = createTask(MARY);

    assertThrows(IllegalArgumentException.class,
                 () -> taskService.unassignTasks(List.of(johnsTask.getId(), marysTask.getId()), new Identity(JOHN)));
    RequestLifeCycle.restartTransaction();

    assertEquals(JOHN, storedAssignee(johnsTask.getId()));
    assertEquals(Set.of(JACK), storedCoworkers(johnsTask.getId()));
    assertEquals(MARY, storedAssignee(marysTask.getId()));
    assertTrue(TRANSACTION_ACTIVE_AT_BROADCAST.isEmpty());
  }

  @Test
  public void unassignTasksWithAnUnknownTaskChangesNoneAndBroadcastsNothing() throws Exception {
    TaskDto johnsTask = createTask(JOHN, JACK);
    long unknownTaskId = johnsTask.getId() + 1000;

    assertThrows(ObjectNotFoundException.class,
                 () -> taskService.unassignTasks(List.of(johnsTask.getId(), unknownTaskId), new Identity(JOHN)));
    RequestLifeCycle.restartTransaction();

    assertEquals(JOHN, storedAssignee(johnsTask.getId()));
    assertEquals(Set.of(JACK), storedCoworkers(johnsTask.getId()));
    assertTrue(TRANSACTION_ACTIVE_AT_BROADCAST.isEmpty());
  }

  private TaskDto createTask(String assignee, String... coworkers) {
    TaskDto task = new TaskDto();
    task.setTitle("Task of " + assignee);
    task.setAssignee(assignee);
    task.setCreatedBy(assignee);
    task.setCoworker(new HashSet<>(List.of(coworkers)));
    task = taskService.createTask(task);
    RequestLifeCycle.restartTransaction();
    return task;
  }

  private String storedAssignee(long taskId) {
    return daoHandler.getTaskHandler().find(taskId).getAssignee();
  }

  private Set<String> storedCoworkers(long taskId) {
    Task task = daoHandler.getTaskHandler().getTaskWithCoworkers(taskId);
    return task.getCoworker() == null ? Set.of() : new HashSet<>(task.getCoworker());
  }
}
