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
package io.meeds.task.rest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.task.model.TaskFilterSetting;
import jakarta.servlet.http.HttpServletRequest;
import io.meeds.task.service.TaskFilterSettingService;

@RunWith(MockitoJUnitRunner.class)
public class TaskFilterSettingRestTest {

  private static final String      FILTER_BODY = "{\"projectId\":5,\"tabView\":\"board\",\"groupBy\":\"dueDate\",\"orderBy\":\"title\"}";

  @Mock
  private TaskFilterSettingService taskFilterSettingService;

  private MockMvc                  mockMvc;

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new TaskFilterSettingRest(taskFilterSettingService)).build();
  }

  @Test
  public void getSettingAnswersTheStoredFilter() throws Exception {
    when(taskFilterSettingService.getSetting(anyLong(), anyString(), any())).thenReturn(new TaskFilterSetting(5,
                                                                                                             "board",
                                                                                                             "dueDate",
                                                                                                             "title"));

    mockMvc.perform(get("/filter/settings").param("projectId", "5").param("tabView", "board"))
           .andExpect(status().isOk())
           .andExpect(header().string("Cache-Control", "no-store, private"))
           .andExpect(jsonPath("$.groupBy").value("dueDate"))
           .andExpect(jsonPath("$.orderBy").value("title"));
  }

  @Test
  public void getSettingAnswers403WithTheMessageCode() throws Exception {
    when(taskFilterSettingService.getSetting(anyLong(),
                                             anyString(),
                                             any())).thenThrow(new IllegalAccessException("task.filter.project.notAccessible"));

    mockMvc.perform(get("/filter/settings").param("projectId", "5").param("tabView", "board"))
           .andExpect(status().isForbidden())
           .andExpect(header().string("Cache-Control", "no-store, private"))
           .andExpect(jsonPath("$.message").value("task.filter.project.notAccessible"));
  }

  @Test
  public void getSettingAnswers404WithTheMessageCode() throws Exception {
    when(taskFilterSettingService.getSetting(anyLong(),
                                             anyString(),
                                             any())).thenThrow(new ObjectNotFoundException("task.filter.project.notFound"));

    mockMvc.perform(get("/filter/settings").param("projectId", "5").param("tabView", "board"))
           .andExpect(status().isNotFound())
           .andExpect(header().string("Cache-Control", "no-store, private"))
           .andExpect(jsonPath("$.message").value("task.filter.project.notFound"));
  }

  @Test
  public void saveSettingAnswers400WithTheMessageCode() throws Exception {
    when(taskFilterSettingService.saveSetting(any(),
                                              any())).thenThrow(new IllegalArgumentException("task.filter.groupBy.invalid"));

    mockMvc.perform(put("/filter/settings").contentType(MediaType.APPLICATION_JSON).content(FILTER_BODY))
           .andExpect(status().isBadRequest())
           .andExpect(header().string("Cache-Control", "no-store, private"))
           .andExpect(jsonPath("$.message").value("task.filter.groupBy.invalid"));
  }

  @Test
  public void saveSettingAnswersTheSavedFilter() throws Exception {
    when(taskFilterSettingService.saveSetting(any(), any())).thenReturn(new TaskFilterSetting(5,
                                                                                              "board",
                                                                                              "dueDate",
                                                                                              "title"));

    mockMvc.perform(put("/filter/settings").contentType(MediaType.APPLICATION_JSON).content(FILTER_BODY))
           .andExpect(status().isOk())
           .andExpect(header().string("Cache-Control", "no-store, private"))
           .andExpect(jsonPath("$.projectId").value(5))
           .andExpect(jsonPath("$.tabView").value("board"));

    ArgumentCaptor<TaskFilterSetting> saved = ArgumentCaptor.forClass(TaskFilterSetting.class);
    verify(taskFilterSettingService).saveSetting(saved.capture(), any());
    assertEquals(new TaskFilterSetting(5, "board", "dueDate", "title"), saved.getValue());
  }

  /**
   * The endpoints are reachable anonymously through the platform's filter chain, which permits an
   * anonymous authentication on /rest/ and relies on method security, so {@code @Secured} is the
   * whole guard. A standalone MockMvc setup registers no method-security proxy and cannot kill the
   * mutant that removes the annotation; this assertion can.
   */
  @Test
  public void bothEndpointsAreSecuredForUsers() throws Exception {
    Secured getSetting = TaskFilterSettingRest.class.getMethod("getSetting",
                                                               HttpServletRequest.class,
                                                               long.class,
                                                               String.class)
                                                    .getAnnotation(Secured.class);
    Secured saveSetting = TaskFilterSettingRest.class.getMethod("saveSetting",
                                                                HttpServletRequest.class,
                                                                TaskFilterSetting.class)
                                                     .getAnnotation(Secured.class);

    assertNotNull(getSetting);
    assertArrayEquals(new String[] { "users" }, getSetting.value());
    assertNotNull(saveSetting);
    assertArrayEquals(new String[] { "users" }, saveSetting.value());
  }

}
