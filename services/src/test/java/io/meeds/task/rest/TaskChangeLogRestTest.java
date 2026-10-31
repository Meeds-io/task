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
package io.meeds.task.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.annotation.Secured;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;

@RunWith(MockitoJUnitRunner.class)
public class TaskChangeLogRestTest {

  private static final String CACHE_CONTROL = "no-store, private";

  @Mock
  private TaskService         taskService;

  private MockMvc             mockMvc;

  /** Builds the MVC client and signs a reader in. */
  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new TaskChangeLogRest(taskService)).build();
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));
  }

  /** Signs the reader out. */
  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  /**
   * A page reaches the service with its cursor and the signed-in reader, and
   * comes back never to be stored, each entry as the timeline shows it: the
   * task's priority, never the task itself; with no cursor, the first page of
   * 20.
   */
  @Test
  public void getTaskLogsAnswersThePageAfterTheCursor() throws Exception {
    Project project = new Project("board", "", null, null, null);
    Task task = new Task();
    task.setTitle("a private title");
    task.setDescription("a private description");
    task.setPriority(Priority.HIGH);
    task.setStatus(new Status(3L, "Done", 1, project));
    ChangeLogEntry entry = new ChangeLogEntry();
    entry.setId(7);
    entry.setTask(task);
    entry.setAuthor("john");
    entry.setActionName("edit_priority");
    entry.setCreatedTime(1000L);
    when(taskService.getTaskLogsBefore(eq(1L), eq(1000L), eq(8L), eq(5), any())).thenReturn(List.of(entry));

    mockMvc.perform(get("/tasks/1/logs").param("beforeTime", "1000").param("beforeId", "8").param("limit", "5"))
           .andExpect(status().isOk())
           .andExpect(header().string("Cache-Control", CACHE_CONTROL))
           .andExpect(jsonPath("$[0].id").value(7))
           .andExpect(jsonPath("$[0].author").value("john"))
           .andExpect(jsonPath("$[0].actionName").value("edit_priority"))
           .andExpect(jsonPath("$[0].createdTime").value(1000))
           .andExpect(jsonPath("$[0].task.priority").value("HIGH"))
           .andExpect(jsonPath("$[0].task.title").doesNotExist())
           .andExpect(jsonPath("$[0].task.status").doesNotExist());
    ArgumentCaptor<Identity> reader = ArgumentCaptor.forClass(Identity.class);
    verify(taskService).getTaskLogsBefore(eq(1L), eq(1000L), eq(8L), eq(5), reader.capture());
    assertEquals("mary", reader.getValue().getUserId());

    mockMvc.perform(get("/tasks/1/logs")).andExpect(status().isOk());
    verify(taskService).getTaskLogsBefore(eq(1L), isNull(), isNull(), eq(20), any());
  }

  /** Each refusal of the service keeps its status and its code, never stored. */
  @Test
  public void getTaskLogsRefusesWithTheServiceCode() throws Exception {
    when(taskService.getTaskLogsBefore(eq(2L), any(), any(), anyInt(), any())).thenThrow(new EntityNotFoundException(2L, Task.class));
    when(taskService.getTaskLogsBefore(eq(3L), any(), any(), anyInt(), any())).thenThrow(new IllegalAccessException("task.logs.notAccessible"));
    when(taskService.getTaskLogsBefore(eq(4L), any(), any(), anyInt(), any())).thenThrow(new IllegalArgumentException("task.logs.invalidCursor"));

    mockMvc.perform(get("/tasks/2/logs"))
           .andExpect(status().isNotFound())
           .andExpect(header().string("Cache-Control", CACHE_CONTROL))
           .andExpect(jsonPath("$.message").value("task.logs.notFound"));
    mockMvc.perform(get("/tasks/3/logs"))
           .andExpect(status().isForbidden())
           .andExpect(header().string("Cache-Control", CACHE_CONTROL))
           .andExpect(jsonPath("$.message").value("task.logs.notAccessible"));
    mockMvc.perform(get("/tasks/4/logs").param("beforeTime", "1000"))
           .andExpect(status().isBadRequest())
           .andExpect(header().string("Cache-Control", CACHE_CONTROL))
           .andExpect(jsonPath("$.message").value("task.logs.invalidCursor"));
  }

  /** A parameter that is not a number never reaches the service. */
  @Test
  public void getTaskLogsRefusesAParameterThatIsNotANumber() throws Exception {
    mockMvc.perform(get("/tasks/1/logs").param("beforeTime", "abc").param("beforeId", "8")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/tasks/x/logs")).andExpect(status().isBadRequest());
    verifyNoInteractions(taskService);
  }

  /** The read is for signed-in users only. */
  @Test
  public void getTaskLogsIsForUsers() throws Exception {
    Secured secured = TaskChangeLogRest.class.getMethod("getTaskLogs", long.class, Long.class, Long.class, int.class)
                                             .getAnnotation(Secured.class);
    assertEquals(List.of("users"), List.of(secured.value()));
  }

}
