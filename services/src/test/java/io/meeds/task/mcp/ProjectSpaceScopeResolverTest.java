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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;

/**
 * Pins the space of a project as the space scope sees it: the one space whose
 * group all the project's managers are roles of, nothing otherwise, and never
 * an exception.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectSpaceScopeResolverTest {

  private static final long          PROJECT_ID = 7L;

  @Mock
  private ProjectService             projectService;

  @Mock
  private SpaceService               spaceService;

  private ProjectSpaceScopeResolver  resolver;

  /**
   * Wires the resolver to the real reading of a project's space, over a
   * space service that knows two groups, one of them gone.
   */
  @Before
  public void setUp() {
    TaskSpaceScopeResolver taskResolver = new TaskSpaceScopeResolver();
    ReflectionTestUtils.setField(taskResolver, "spaceService", spaceService);
    resolver = new ProjectSpaceScopeResolver();
    ReflectionTestUtils.setField(resolver, "projectService", projectService);
    ReflectionTestUtils.setField(resolver, "taskSpaceScopeResolver", taskResolver);
    Space alpha = space("12");
    Space beta = space("13");
    lenient().when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenReturn(alpha);
    lenient().when(spaceService.getSpaceByGroupId("/spaces/beta")).thenReturn(beta);
    lenient().when(spaceService.getSpaceByGroupId("/spaces/gone")).thenReturn(null);
  }

  /**
   * The name is the one the project lookup's declaration gives, and the bean
   * is a mcp-server one, as the SPI it implements.
   */
  @Test
  public void nameAndWiringAreTheOnesTheDeclarationGives() {
    assertEquals("project", resolver.getName());
    assertEquals("projectSpaceScopeResolver", ProjectSpaceScopeResolver.class.getAnnotation(Service.class).value());
    assertEquals("mcp-server", ProjectSpaceScopeResolver.class.getAnnotation(Profile.class).value()[0]);
    assertEquals("mcp-server", TaskSpaceScopeResolver.class.getAnnotation(Profile.class).value()[0]);
  }

  /**
   * A project managed through one space group belongs to that space.
   * Mutant: the resolver gives no space.
   */
  @Test
  public void spaceOfProjectManagedThroughOneSpaceGroup() throws Exception {
    givenProjectWithManagers("manager:/spaces/alpha", "member:/spaces/alpha");
    assertEquals(Long.valueOf(12), resolver.spaceOf(String.valueOf(PROJECT_ID)));
  }

  /**
   * A project managed through two spaces, a user, or a group outside the
   * spaces belongs to none: the same reading as the project's tasks.
   * Mutant: the first space group wins, or non-space managers are ignored.
   */
  @Test
  public void noSpaceWhenManagersAreNotOfOneSpaceGroup() throws Exception {
    givenProjectWithManagers("manager:/spaces/alpha", "manager:/spaces/beta");
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
    givenProjectWithManagers("manager:/spaces/alpha", "john");
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
    givenProjectWithManagers("manager:/platform/administrators");
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
    givenProjectWithManagers("john");
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
    givenProjectWithManagers();
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
    verify(spaceService, never()).getSpaceByGroupId(anyString());
  }

  /**
   * A space that no longer exists is no space.
   */
  @Test
  public void noSpaceWhenTheSpaceIsGone() throws Exception {
    givenProjectWithManagers("manager:/spaces/gone");
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
  }

  /**
   * A missing project is no space: the not-found guard.
   * Mutant: the guard is removed and the exception reaches the caller.
   */
  @Test
  public void noSpaceForMissingProject() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenThrow(new EntityNotFoundException(PROJECT_ID, ProjectDto.class));
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
  }

  /**
   * A project service that returns nothing is no space.
   */
  @Test
  public void noSpaceWhenProjectServiceReturnsNothing() {
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
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
    verify(projectService, never()).getProject(anyLong());
  }

  /**
   * A project service that fails is no space, never an exception.
   * Mutant: the runtime guard is removed.
   */
  @Test
  public void noSpaceWhenTheProjectLookupFails() throws Exception {
    when(projectService.getProject(PROJECT_ID)).thenThrow(new IllegalStateException("boom"));
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
  }

  /**
   * A space service that fails is no space, never an exception.
   */
  @Test
  public void noSpaceWhenTheSpaceLookupFails() throws Exception {
    givenProjectWithManagers("manager:/spaces/alpha");
    when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenThrow(new IllegalStateException("boom"));
    assertNull(resolver.spaceOf(String.valueOf(PROJECT_ID)));
  }

  /**
   * @param managers the manager permissions of the project
   */
  private void givenProjectWithManagers(String... managers) throws EntityNotFoundException {
    Set<String> set = new LinkedHashSet<>(Set.of());
    for (String manager : managers) {
      set.add(manager);
    }
    ProjectDto project = new ProjectDto();
    project.setId(PROJECT_ID);
    project.setManager(set);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
  }

  /**
   * @param id a space id
   * @return a space with that id
   */
  private static Space space(String id) {
    Space space = mock(Space.class);
    when(space.getId()).thenReturn(id);
    return space;
  }

}
