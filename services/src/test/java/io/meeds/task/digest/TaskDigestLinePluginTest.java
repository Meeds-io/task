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
package io.meeds.task.digest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;

import io.meeds.commons.digest.model.DigestItem;
import io.meeds.commons.digest.model.DigestLine;
import io.meeds.commons.digest.plugin.DigestLineContext;

@RunWith(MockitoJUnitRunner.class)
public class TaskDigestLinePluginTest {

  private static final DigestLineContext CONTEXT = new DigestLineContext("ayoub", Locale.ENGLISH, ZoneId.of("Europe/Paris"));

  @Mock
  private TaskService                    taskService;

  @Mock
  private IdentityManager                identityManager;

  private TaskDigestLinePlugin           plugin;

  @Before
  public void setUp() throws Exception {
    InitParams params = new InitParams();
    ValuesParam pluginIds = new ValuesParam();
    pluginIds.setName("pluginIds");
    pluginIds.setValues(new ArrayList<>(List.of(TaskDigestLinePlugin.TASK_ASSIGN_PLUGIN,
                                                TaskDigestLinePlugin.TASK_COWORKER_PLUGIN,
                                                TaskDigestLinePlugin.TASK_MENTIONED_PLUGIN)));
    params.addParameter(pluginIds);
    // The platform link needs the running portal: only the stored link is
    // exercised here
    plugin = new TaskDigestLinePlugin(params, taskService, identityManager);

    ProjectDto project = new ProjectDto();
    project.setName("Website");
    StatusDto status = new StatusDto();
    status.setProject(project);
    TaskDto task = new TaskDto();
    task.setId(7);
    task.setTitle("Write the release notes");
    task.setStatus(status);
    lenient().when(taskService.getTask(7)).thenReturn(task);
    lenient().when(taskService.getTask(404)).thenThrow(new EntityNotFoundException(404, TaskDto.class));
    Identity john = new Identity(OrganizationIdentityProvider.NAME, "john");
    Profile profile = new Profile(john);
    profile.setProperty(Profile.FULL_NAME, "John Smith");
    john.setProfile(profile);
    lenient().when(identityManager.getOrCreateUserIdentity("john")).thenReturn(john);
  }

  @Test
  public void testAssignedLineHasNoActor() {
    DigestLine line = plugin.buildLine(item(TaskDigestLinePlugin.TASK_ASSIGN_PLUGIN, "taskId", "7", "creator", "john",
                                            "taskUrl", "https://platform/portal/dw/tasks/taskDetail/7"),
                                       CONTEXT);
    assertNotNull(line);
    assertEquals("digest.line.TaskAssignPlugin", line.getLabelKey());
    assertEquals(List.of("Write the release notes", "Website"), line.getArgs());
    assertEquals("https://platform/portal/dw/tasks/taskDetail/7", line.getUrl());
  }

  @Test
  public void testCoworkerAndMentionLinesNameTheActor() {
    DigestLine coworker = plugin.buildLine(item(TaskDigestLinePlugin.TASK_COWORKER_PLUGIN, "taskId", "7", "creator", "john",
                                                "taskUrl", "https://platform/t/7"),
                                           CONTEXT);
    DigestLine mention = plugin.buildLine(item(TaskDigestLinePlugin.TASK_MENTIONED_PLUGIN, "taskId", "7", "creator", "john",
                                               "taskUrl", "https://platform/t/7"),
                                          CONTEXT);
    assertNotNull(coworker);
    assertNotNull(mention);
    assertEquals(List.of("John Smith", "Write the release notes", "Website"), coworker.getArgs());
    assertEquals(List.of("John Smith", "Write the release notes", "Website"), mention.getArgs());
  }

  @Test
  public void testDeletedTaskGivesNoLine() {
    assertNull(plugin.buildLine(item(TaskDigestLinePlugin.TASK_ASSIGN_PLUGIN, "taskId", "404"), CONTEXT));
    assertNull(plugin.buildLine(item(TaskDigestLinePlugin.TASK_ASSIGN_PLUGIN, "taskId", "not a number"), CONTEXT));
    assertNull(plugin.buildLine(item(TaskDigestLinePlugin.TASK_ASSIGN_PLUGIN), CONTEXT));
  }

  @Test
  public void testUnknownTypeGivesNoLine() {
    assertNull(plugin.buildLine(item("TaskCompletedPlugin", "taskId", "7", "taskUrl", "https://platform/t/7"), CONTEXT));
  }

  private static DigestItem item(String pluginId, String... params) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i + 1 < params.length; i += 2) {
      map.put(params[i], params[i + 1]);
    }
    return new DigestItem(1, "ayoub", pluginId, "tasks", Instant.now(), map);
  }

}
