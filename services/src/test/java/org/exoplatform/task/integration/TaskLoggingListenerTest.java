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
package org.exoplatform.task.integration;

import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskPayload;
import org.exoplatform.task.service.TaskService;

/**
 * The creation entry a task's change log gets (EXO-91902).
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskLoggingListenerTest {

  private static final String USERNAME = "root";

  private static final long   TASK_ID  = 3L;

  @Mock
  private TaskService         taskService;

  private TaskLoggingListener listener;

  /**
   * Sets the current user, the author of the entries.
   */
  @Before
  public void setUp() {
    ConversationState.setCurrent(new ConversationState(new Identity(USERNAME)));
    listener = new TaskLoggingListener(taskService);
  }

  /**
   * Clears the current user.
   */
  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  /**
   * A task's creation entry records the status it is created in, so that its
   * first move tells the status it left; a task created in no status, such
   * as a personal task, gets an empty target, as every creation entry had
   * before. Mutant: the empty target kept for every task.
   *
   * @throws Exception never
   */
  @Test
  public void theCreationEntryRecordsTheStatusTheTaskIsCreatedIn() throws Exception {
    StatusDto status = new StatusDto();
    status.setName("ToDo");
    TaskDto inAColumn = new TaskDto();
    inAColumn.setId(TASK_ID);
    inAColumn.setStatus(status);
    TaskDto personal = new TaskDto();
    personal.setId(TASK_ID + 1);

    listener.onEvent(new Event<>("exo.task.taskCreation", taskService, new TaskPayload(null, inAColumn)));
    listener.onEvent(new Event<>("exo.task.taskCreation", taskService, new TaskPayload(null, personal)));

    verify(taskService).addTaskLog(TASK_ID, USERNAME, "created", "ToDo");
    verify(taskService).addTaskLog(TASK_ID + 1, USERNAME, "created", "");
  }

}
