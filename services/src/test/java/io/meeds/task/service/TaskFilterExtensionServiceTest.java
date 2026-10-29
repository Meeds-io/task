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
package io.meeds.task.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;

import io.meeds.task.plugin.TaskFilterPlugin;

@RunWith(MockitoJUnitRunner.class)
public class TaskFilterExtensionServiceTest {

  private static final long          PROJECT_ID = 5L;

  @Mock
  private ProjectService             projectService;

  @Mock
  private ProjectDto                 project;

  @Mock
  private Identity                   viewer;

  @Mock
  private TaskFilterPlugin           plugin;

  @Mock
  private Condition                  condition;

  private TaskFilterExtensionService service;

  @Before
  public void setUp() {
    service = new TaskFilterExtensionService(projectService);
    when(plugin.getId()).thenReturn("forms.fields");
    service.addPlugin(plugin);
  }

  /**
   * No filter restricts nothing and reads no project, whatever the project.
   */
  @Test
  public void testBlankFiltersRestrictNothing() throws Exception {
    assertTrue(service.resolve(PROJECT_ID, null, viewer).isEmpty());
    assertTrue(service.resolve(-2, " ", viewer).isEmpty());
    verifyNoInteractions(projectService);
  }

  /**
   * A viewer who views the project gets the plugin's condition, built from
   * the payload sent under the plugin's id, as its raw JSON.
   */
  @Test
  public void testResolveCallsThePluginOfEachId() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(viewer)).thenReturn(true);
    when(plugin.toCondition(PROJECT_ID, "{\"conditions\":[]}", viewer)).thenReturn(condition);

    List<Condition> conditions = service.resolve(PROJECT_ID, "{\"forms.fields\":{\"conditions\":[]}}", viewer);

    assertEquals(1, conditions.size());
    assertSame(condition, conditions.get(0));
  }

  /**
   * Every payload reaches its plugin as JSON text, a string one quoted, so a
   * plugin can always parse what it receives.
   */
  @Test
  public void testPayloadArrivesAsJsonText() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(viewer)).thenReturn(true);
    when(plugin.toCondition(PROJECT_ID, "\"abc\"", viewer)).thenReturn(condition);
    when(plugin.toCondition(PROJECT_ID, "[1,2]", viewer)).thenReturn(condition);

    assertEquals(List.of(condition), service.resolve(PROJECT_ID, "{\"forms.fields\":\"abc\"}", viewer));
    assertEquals(List.of(condition), service.resolve(PROJECT_ID, "{\"forms.fields\":[1,2]}", viewer));
  }

  /**
   * A plugin answering that its payload restricts nothing adds no condition.
   */
  @Test
  public void testNullConditionIsSkipped() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(viewer)).thenReturn(true);
    when(plugin.toCondition(anyLong(), anyString(), any())).thenReturn(null);

    assertTrue(service.resolve(PROJECT_ID, "{\"forms.fields\":{}}", viewer).isEmpty());
  }

  /**
   * A viewer who does not view the project is refused before any plugin is
   * asked, so a payload can never be evaluated on a project the viewer cannot
   * see; the viewing branch is pinned by
   * {@link #testResolveCallsThePluginOfEachId()}.
   */
  @Test
  public void testProjectNotViewableIsRefusedBeforeThePlugin() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(viewer)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> service.resolve(PROJECT_ID, "{\"forms.fields\":{}}", viewer));
    assertThrows(IllegalAccessException.class, () -> service.resolve(PROJECT_ID, "{\"forms.fields\":{}}", null));
    verify(plugin, never()).toCondition(anyLong(), anyString(), any());
  }

  /**
   * An unknown project is not found, and no plugin is asked.
   */
  @Test
  public void testUnknownProjectIsNotFound() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenThrow(new EntityNotFoundException(PROJECT_ID, ProjectDto.class));

    assertThrows(ObjectNotFoundException.class, () -> service.resolve(PROJECT_ID, "{\"forms.fields\":{}}", viewer));
    verify(plugin, never()).toCondition(anyLong(), anyString(), any());
  }

  /**
   * Filters on a personal list, which has no project, are refused.
   */
  @Test
  public void testFiltersNeedAProject() {
    IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                                                    () -> service.resolve(-2, "{\"forms.fields\":{}}", viewer));
    assertEquals(TaskFilterExtensionService.PROJECT_REQUIRED_CODE, refused.getMessage());
    refused = assertThrows(IllegalArgumentException.class, () -> service.resolve(0, "{\"forms.fields\":{}}", viewer));
    assertEquals(TaskFilterExtensionService.PROJECT_REQUIRED_CODE, refused.getMessage());
  }

  /**
   * A parameter that is not a JSON object, too many filters and an unknown id
   * are each refused with their own code.
   */
  @Test
  public void testInvalidFiltersAreRefused() throws Exception {
    IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                                                    () -> service.resolve(PROJECT_ID, "[1]", viewer));
    assertEquals(TaskFilterExtensionService.INVALID_FILTERS_CODE, refused.getMessage());

    StringBuilder tooMany = new StringBuilder("{");
    for (int i = 0; i <= TaskFilterExtensionService.MAX_FILTERS; i++) {
      tooMany.append(i == 0 ? "" : ",").append("\"f").append(i).append("\":{}");
    }
    String overLimit = tooMany.append('}').toString();
    refused = assertThrows(IllegalArgumentException.class, () -> service.resolve(PROJECT_ID, overLimit, viewer));
    assertEquals(TaskFilterExtensionService.TOO_MANY_FILTERS_CODE, refused.getMessage());

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(viewer)).thenReturn(true);
    refused = assertThrows(IllegalArgumentException.class, () -> service.resolve(PROJECT_ID, "{\"other\":{}}", viewer));
    assertEquals(TaskFilterExtensionService.UNKNOWN_FILTER_CODE, refused.getMessage());
  }

  /**
   * At the limit, the filters are accepted.
   */
  @Test
  public void testFiltersAtTheLimitAreAccepted() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(viewer)).thenReturn(true);
    StringBuilder atLimit = new StringBuilder("{\"forms.fields\":{}");
    for (int i = 1; i < TaskFilterExtensionService.MAX_FILTERS; i++) {
      TaskFilterPlugin other = new NoRestrictionPlugin("f" + i);
      service.addPlugin(other);
      atLimit.append(",\"f").append(i).append("\":{}");
    }
    assertTrue(service.resolve(PROJECT_ID, atLimit.append('}').toString(), viewer).isEmpty());
  }

  /**
   * A second plugin cannot take an id already registered; the same plugin
   * registering again is harmless.
   */
  @Test
  public void testDuplicateIdIsRefused() {
    service.addPlugin(plugin);
    TaskFilterPlugin other = new NoRestrictionPlugin("forms.fields");
    assertThrows(IllegalArgumentException.class, () -> service.addPlugin(other));
    assertThrows(IllegalArgumentException.class, () -> service.addPlugin(new NoRestrictionPlugin(" ")));
    assertThrows(IllegalArgumentException.class, () -> service.addPlugin(null));
  }

  /** A plugin whose payloads restrict nothing. */
  private record NoRestrictionPlugin(String id) implements TaskFilterPlugin {

    @Override
    public String getId() {
      return id;
    }

    @Override
    public Condition toCondition(long projectId, String payload, Identity viewer) {
      return null;
    }
  }

}
