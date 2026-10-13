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
package io.meeds.task.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;

/**
 * Pins the space of a task as the space scope sees it: the one space whose
 * group the project's managers name, nothing otherwise.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskSpaceScopeResolverTest {

  private static final long    TASK_ID = 5L;

  @Mock
  private TaskService          taskService;

  @Mock
  private SpaceService         spaceService;

  @InjectMocks
  private TaskSpaceScopeResolver resolver;

  /**
   * Gives the resolver spaces for two groups, one of them gone.
   */
  @Before
  public void setUp() {
    Space alpha = space("12");
    Space beta = space("13");
    lenient().when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenReturn(alpha);
    lenient().when(spaceService.getSpaceByGroupId("/spaces/beta")).thenReturn(beta);
    lenient().when(spaceService.getSpaceByGroupId("/spaces/gone")).thenReturn(null);
  }

  /**
   * The name is the one the task tools' declarations give.
   */
  @Test
  public void nameIsTheOneTheDeclarationsGive() {
    assertEquals("task", resolver.getName());
  }

  /**
   * A project managed through one space group, whatever the role, belongs to
   * that space.
   */
  @Test
  public void spaceOfTaskInSpaceProject() throws Exception {
    givenTaskWithManagers("manager:/spaces/alpha");
    assertEquals(Long.valueOf(12), resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * Two roles of one group are one space group.
   */
  @Test
  public void spaceOfProjectManagedByTwoRolesOfOneGroup() throws Exception {
    givenTaskWithManagers("manager:/spaces/alpha", "member:/spaces/alpha");
    assertEquals(Long.valueOf(12), resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * A project managed through two spaces belongs to none: no first match.
   * Mutant: the first space group wins.
   */
  @Test
  public void noSpaceWhenManagersNameSeveralSpaces() throws Exception {
    givenTaskWithManagers("manager:/spaces/alpha", "manager:/spaces/beta");
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
    verify(spaceService, never()).getSpaceByGroupId(anyString());
  }

  /**
   * A project managed by users only is a personal project. Mutant: a user
   * manager is read as a group and gives a space.
   */
  @Test
  public void noSpaceForPersonalProject() throws Exception {
    givenTaskWithManagers("john");
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
    givenTaskWithManagers("john", "mary");
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
    verify(spaceService, never()).getSpaceByGroupId(anyString());
  }

  /**
   * A project managed through a group that isn't a space's has no space.
   */
  @Test
  public void noSpaceForProjectOfAGroupOutsideSpaces() throws Exception {
    givenTaskWithManagers("manager:/platform/administrators");
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
    verify(spaceService, never()).getSpaceByGroupId(anyString());
  }

  /**
   * A project without managers has no space.
   */
  @Test
  public void noSpaceForProjectWithoutManagers() throws Exception {
    givenTaskWithManagers();
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * A space that no longer exists is no space.
   */
  @Test
  public void noSpaceWhenTheSpaceIsGone() throws Exception {
    givenTaskWithManagers("manager:/spaces/gone");
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * A personal task has no status, and a status has no project.
   */
  @Test
  public void noSpaceForPersonalTask() throws Exception {
    TaskDto task = new TaskDto();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));

    task.setStatus(new StatusDto());
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
    verify(spaceService, never()).getSpaceByGroupId(anyString());
  }

  /**
   * A missing task is no space: the not-found guard. Mutant: the guard is
   * removed and the exception reaches the caller.
   */
  @Test
  public void noSpaceForMissingTask() throws Exception {
    when(taskService.getTask(TASK_ID)).thenThrow(new EntityNotFoundException(TASK_ID, TaskDto.class));
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * A task service that returns nothing is no space.
   */
  @Test
  public void noSpaceWhenTaskServiceReturnsNothing() {
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * An id that isn't decimal digits is never looked up.
   */
  @Test
  public void noSpaceForNonNumericId() throws Exception {
    assertNull(resolver.spaceOf("abc"));
    assertNull(resolver.spaceOf("-3"));
    assertNull(resolver.spaceOf(""));
    assertNull(resolver.spaceOf(null));
    verify(taskService, never()).getTask(anyLong());
  }

  /**
   * A task service that fails is no space, never an exception.
   */
  @Test
  public void noSpaceWhenTheTaskLookupFails() throws Exception {
    when(taskService.getTask(TASK_ID)).thenThrow(new IllegalStateException("boom"));
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * A space service that fails is no space, never an exception.
   */
  @Test
  public void noSpaceWhenTheSpaceLookupFails() throws Exception {
    givenTaskWithManagers("manager:/spaces/alpha");
    when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenThrow(new IllegalStateException("boom"));
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * A space whose id isn't numeric is no space.
   */
  @Test
  public void noSpaceWhenSpaceIdIsNotNumeric() throws Exception {
    givenTaskWithManagers("manager:/spaces/alpha");
    Space malformed = space("x1");
    when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenReturn(malformed);
    assertNull(resolver.spaceOf(String.valueOf(TASK_ID)));
  }

  /**
   * @param managers the manager permissions of the project the task is in
   */
  private void givenTaskWithManagers(String... managers) throws EntityNotFoundException {
    Set<String> set = new LinkedHashSet<>();
    for (String manager : managers) {
      set.add(manager);
    }
    ProjectDto project = new ProjectDto();
    project.setId(7L);
    project.setManager(set);
    StatusDto status = new StatusDto();
    status.setProject(project);
    TaskDto task = new TaskDto();
    task.setId(TASK_ID);
    task.setStatus(status);
    when(taskService.getTask(TASK_ID)).thenReturn(task);
  }

  /**
   * @param id the space id
   * @return a space with that id
   */
  private static Space space(String id) {
    Space space = mock(Space.class);
    when(space.getId()).thenReturn(id);
    return space;
  }

}
