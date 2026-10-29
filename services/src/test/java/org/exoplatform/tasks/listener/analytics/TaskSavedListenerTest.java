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
package org.exoplatform.tasks.listener.analytics;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.Date;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;

/**
 * The operation recorded for a task save is the first field that changed, in
 * the order the listener compares them; a save that changes none of them is
 * recorded under the registration's own operation. These names are the ones
 * the collaborative activity declaration counts, the rank excepted.
 */
public class TaskSavedListenerTest {

  @Test
  public void eachChangedFieldIsRecordedUnderItsOwnOperation() {
    TaskSavedListener listener = listener("taskUpdated");

    assertEquals(TaskSavedListener.TASK_COMPLETED_OPERATION, operationOf(listener, task -> task.setCompleted(true)));
    assertEquals(TaskSavedListener.TASK_DESCRIPTION_CHANGED_OPERATION, operationOf(listener, task -> task.setDescription("new")));
    assertEquals(TaskSavedListener.TASK_TITLE_CHANGED_OPERATION, operationOf(listener, task -> task.setTitle("new")));
    assertEquals(TaskSavedListener.TASK_DUE_DATE_CHANGED_OPERATION, operationOf(listener, task -> task.setDueDate(new Date(2))));
    assertEquals(TaskSavedListener.TASK_PRIORITY_CHANGED_OPERATION, operationOf(listener, task -> task.setPriority(Priority.HIGH)));
    assertEquals(TaskSavedListener.TASK_RANK_CHANGED_OPERATION, operationOf(listener, task -> task.setRank(5)));
    assertEquals(TaskSavedListener.TASK_ASSIGNEE_CHANGED_OPERATION, operationOf(listener, task -> task.setAssignee("mary")));
    assertEquals(TaskSavedListener.TASK_COWORKER_CHANGED_OPERATION, operationOf(listener, task -> task.setCoworker(Set.of("mary"))));
    assertEquals(TaskSavedListener.TASK_PROJECT_CHANGED_OPERATION,
                 operationOf(listener, task -> task.setStatus(status("To Do", 2L))));
    assertEquals(TaskSavedListener.TASK_STATUS_CHANGED_OPERATION,
                 operationOf(listener, task -> task.setStatus(status("Done", 1L))));
    assertEquals(TaskSavedListener.TASK_DATES_CHANGED_OPERATION, operationOf(listener, task -> task.setStartDate(new Date(2))));
    assertEquals("taskUpdated", operationOf(listener, task -> {
    }));
  }

  @Test
  public void aNewTaskIsRecordedUnderTheRegistrationsOperation() {
    assertEquals("taskCreated", listener("taskCreated").computeTaskOperation(null, task()));
  }

  private static String operationOf(TaskSavedListener listener, Consumer<TaskDto> change) {
    TaskDto newTask = task();
    change.accept(newTask);
    return listener.computeTaskOperation(task(), newTask);
  }

  private static TaskDto task() {
    TaskDto task = new TaskDto();
    task.setTitle("title");
    task.setDescription("description");
    task.setDueDate(new Date(1));
    task.setStartDate(new Date(1));
    task.setEndDate(new Date(1));
    task.setPriority(Priority.NORMAL);
    task.setRank(1);
    task.setAssignee("john");
    task.setCoworker(Set.of("john"));
    task.setStatus(status("To Do", 1L));
    return task;
  }

  private static StatusDto status(String name, long projectId) {
    ProjectDto project = new ProjectDto();
    project.setId(projectId);
    StatusDto status = new StatusDto();
    status.setName(name);
    status.setProject(project);
    return status;
  }

  private static TaskSavedListener listener(String operation) {
    ValueParam operationParam = new ValueParam();
    operationParam.setName("operation");
    operationParam.setValue(operation);
    InitParams params = new InitParams();
    params.addParameter(operationParam);
    try (MockedStatic<PortalContainer> portalContainer = mockStatic(PortalContainer.class)) {
      return new TaskSavedListener(params);
    }
  }

}
