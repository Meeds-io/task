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
package io.meeds.task.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

import io.meeds.social.cms.model.ContentLinkSearchResult;

@RunWith(MockitoJUnitRunner.class)
public class TaskContentLinkPluginTest {

  @Mock
  private TaskService           taskService;

  @Mock
  private Identity              identity;

  @InjectMocks
  private TaskContentLinkPlugin plugin;

  /**
   * A user named by id on a space project who left the space no longer gets
   * its tasks in the content link search. Mutant: the view filter removed.
   */
  @Test
  public void testSearchDropsTheTasksOfASpaceProjectTheNamedUserLeft() {
    when(identity.getUserId()).thenReturn("john");
    ProjectDto spaceProject = new ProjectDto();
    spaceProject.setManager(new HashSet<>(List.of("john", "manager:/spaces/team")));
    StatusDto status = new StatusDto();
    status.setProject(spaceProject);
    TaskDto departedTask = mock(TaskDto.class);
    when(departedTask.getId()).thenReturn(1L);
    when(departedTask.getStatus()).thenReturn(status);
    TaskDto ownTask = mock(TaskDto.class);
    when(ownTask.getId()).thenReturn(2L);
    when(ownTask.getTitle()).thenReturn("Own task");
    when(ownTask.getAssignee()).thenReturn("john");
    when(taskService.findTasksByMemberShips(eq("john"), anyList(), eq("task"), anyInt())).thenReturn(List.of(departedTask, ownTask));
    when(taskService.getMentionedUsers(any(Long.class))).thenReturn(Collections.emptySet());

    List<ContentLinkSearchResult> results = plugin.search("task", identity, Locale.ENGLISH, 0, 10);

    assertEquals(1, results.size());
    assertEquals("2", results.get(0).getObjectId());
  }
}
