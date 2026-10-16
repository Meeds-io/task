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
package io.meeds.task.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;

import io.meeds.task.model.TaskFilterSetting;
import io.meeds.task.storage.TaskFilterSettingStorage;

@RunWith(MockitoJUnitRunner.class)
public class TaskFilterSettingServiceTest {

  private static final String      USERNAME = "john";

  private static final long        PROJECT_ID = 5L;

  @Mock
  private SettingService           settingService;

  @Mock
  private ProjectService           projectService;

  @Mock
  private UserACL                  userAcl;

  @Mock
  private ProjectDto               project;

  @Mock
  private Identity                 identity;

  private TaskFilterSettingService taskFilterSettingService;

  @Before
  public void setUp() {
    taskFilterSettingService = new TaskFilterSettingService(new TaskFilterSettingStorage(settingService),
                                                            projectService,
                                                            userAcl);
  }

  @Test
  public void saveSettingRefusesAnUnknownGroupBy() {
    TaskFilterSetting setting = new TaskFilterSetting(0, "list", "' OR 1=1", "title");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                      () -> taskFilterSettingService.saveSetting(setting, USERNAME));

    assertEquals("task.filter.groupBy.invalid", exception.getMessage());
    verify(settingService, never()).set(any(), any(), anyString(), any());
  }

  @Test
  public void saveSettingRefusesAnUnknownOrderBy() {
    TaskFilterSetting setting = new TaskFilterSetting(0, "list", "dueDate", "createdBy");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                      () -> taskFilterSettingService.saveSetting(setting, USERNAME));

    assertEquals("task.filter.orderBy.invalid", exception.getMessage());
    verify(settingService, never()).set(any(), any(), anyString(), any());
  }

  @Test
  public void saveSettingRefusesAnUnknownTabView() {
    TaskFilterSetting setting = new TaskFilterSetting(0, "../../other", "dueDate", "title");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                      () -> taskFilterSettingService.saveSetting(setting, USERNAME));

    assertEquals("task.filter.tabView.invalid", exception.getMessage());
    verify(settingService, never()).set(any(), any(), anyString(), any());
  }

  @Test
  public void saveSettingRefusesANegativeProjectId() {
    TaskFilterSetting setting = new TaskFilterSetting(-2, "list", "dueDate", "title");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                      () -> taskFilterSettingService.saveSetting(setting, USERNAME));

    assertEquals("task.filter.projectId.invalid", exception.getMessage());
    verify(settingService, never()).set(any(), any(), anyString(), any());
  }

  @Test
  public void saveSettingRefusesAProjectTheUserCannotView() throws Exception {
    TaskFilterSetting setting = new TaskFilterSetting(PROJECT_ID, "board", "dueDate", "title");
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    when(project.canView(identity)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> taskFilterSettingService.saveSetting(setting, USERNAME));

    verify(settingService, never()).set(any(), any(), anyString(), any());
  }

  @Test
  public void saveSettingRefusesAProjectThatDoesNotExist() throws Exception {
    TaskFilterSetting setting = new TaskFilterSetting(PROJECT_ID, "board", "dueDate", "title");
    when(projectService.getProject(PROJECT_ID)).thenThrow(new EntityNotFoundException(PROJECT_ID, ProjectDto.class));

    assertThrows(ObjectNotFoundException.class, () -> taskFilterSettingService.saveSetting(setting, USERNAME));

    verify(settingService, never()).set(any(), any(), anyString(), any());
  }

  @Test
  public void saveSettingRefusesAProjectWhenTheUserHasNoAclIdentity() throws Exception {
    TaskFilterSetting setting = new TaskFilterSetting(PROJECT_ID, "board", "dueDate", "title");
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(userAcl.getUserIdentity(USERNAME)).thenReturn(null);

    assertThrows(IllegalAccessException.class, () -> taskFilterSettingService.saveSetting(setting, USERNAME));

    verify(project, never()).canView(any());
    verify(settingService, never()).set(any(), any(), anyString(), any());
  }

  @Test
  public void saveSettingAnswersTheProjectRefusalBeforeTheValueRefusal() throws Exception {
    TaskFilterSetting setting = new TaskFilterSetting(PROJECT_ID, "board", "' OR 1=1", "title");
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    when(project.canView(identity)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> taskFilterSettingService.saveSetting(setting, USERNAME));

    verify(settingService, never()).set(any(), any(), anyString(), any());
  }

  @Test
  public void saveSettingStoresTheFilterOfAViewableProject() throws Exception {
    TaskFilterSetting setting = new TaskFilterSetting(PROJECT_ID, "board", "dueDate", "title");
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    when(project.canView(identity)).thenReturn(true);

    TaskFilterSetting saved = taskFilterSettingService.saveSetting(setting, USERNAME);

    assertEquals(setting, saved);
    ArgumentCaptor<SettingValue> value = ArgumentCaptor.forClass(SettingValue.class);
    verify(settingService).set(eq(Context.USER.id(USERNAME)),
                               eq(Scope.APPLICATION.id("task-5-board")),
                               eq("filterTask"),
                               value.capture());
    JSONObject stored = new JSONObject(value.getValue().getValue().toString());
    assertEquals("dueDate", stored.getString("groupBy"));
    assertEquals("title", stored.getString("orderBy"));
    assertEquals("5", stored.getString("projectId"));
    assertEquals("board", stored.getString("tabView"));
  }

  @Test
  public void saveSettingStoresTheTasksFilterUnderTheNoProjectScope() throws Exception {
    TaskFilterSetting setting = new TaskFilterSetting(0, "list", "assignee", "priority");

    taskFilterSettingService.saveSetting(setting, USERNAME);

    verify(settingService).set(eq(Context.USER.id(USERNAME)),
                               eq(Scope.APPLICATION.id("task-None-list")),
                               eq("filterTask"),
                               any());
  }

  @Test
  public void getSettingReturnsTheStoredFilter() throws Exception {
    doReturn(storedValue("{\"groupBy\":\"assignee\",\"orderBy\":\"priority\"}")).when(settingService)
                                                                                 .get(Context.USER.id(USERNAME),
                                                                                      Scope.APPLICATION.id("task-None-list"),
                                                                                      "filterTask");

    TaskFilterSetting setting = taskFilterSettingService.getSetting(0, "list", USERNAME);

    assertEquals(new TaskFilterSetting(0, "list", "assignee", "priority"), setting);
  }

  @Test
  public void getSettingDropsAValueWrittenAroundTheEndpoint() throws Exception {
    doReturn(storedValue("{\"groupBy\":\"' OR 1=1\",\"orderBy\":\"drop\"}")).when(settingService)
                                                                            .get(Context.USER.id(USERNAME),
                                                                                 Scope.APPLICATION.id("task-None-list"),
                                                                                 "filterTask");

    TaskFilterSetting setting = taskFilterSettingService.getSetting(0, "list", USERNAME);

    assertEquals(new TaskFilterSetting(0, "list", "", ""), setting);
  }

  @Test
  public void getSettingReturnsAnEmptyFilterWhenNothingIsStored() throws Exception {
    TaskFilterSetting setting = taskFilterSettingService.getSetting(0, "list", USERNAME);

    assertEquals(new TaskFilterSetting(0, "list", "", ""), setting);
  }

  @Test
  public void getSettingAnswersTheProjectRefusalBeforeTheTabViewRefusal() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    when(project.canView(identity)).thenReturn(false);

    assertThrows(IllegalAccessException.class,
                 () -> taskFilterSettingService.getSetting(PROJECT_ID, "../../other", USERNAME));

    verify(settingService, never()).get(any(), any(), anyString());
  }

  @Test
  public void getSettingRefusesAProjectTheUserCannotView() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    when(project.canView(identity)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> taskFilterSettingService.getSetting(PROJECT_ID, "list", USERNAME));

    verify(settingService, never()).get(any(), any(), anyString());
  }

  private SettingValue<String> storedValue(String json) {
    return SettingValue.create(json);
  }

}
