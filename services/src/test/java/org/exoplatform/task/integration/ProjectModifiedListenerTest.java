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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.Event;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.storage.ProjectStorage;

public class ProjectModifiedListenerTest extends AbstractTest {

  @After
  public void tearDown() {
    deleteAll();
  }

  @Test
  public void testOnlyLastModifiedDateIsUpdated() throws Exception {
    ProjectStorage storage = PortalContainer.getInstance().getComponentInstanceOfType(ProjectStorage.class);

    ProjectDto project = new ProjectDto();
    project.setName("Board");
    project.setColor("blue");
    project.setDescription("Initial description");
    project.setManager(new HashSet<>(Set.of("alice", "bob")));
    project.setParticipator(new HashSet<>(Set.of("carol", "dave")));
    project.setDueDate(localDate(1));
    project.setLastModifiedDate(1000L);
    project = storage.createProject(project);
    long projectId = project.getId();
    RequestLifeCycle.restartTransaction();

    // Copy of the board as carried by the event, with its members and settings
    ProjectDto carried = storage.getProject(projectId);
    Assert.assertEquals(Set.of("alice", "bob"), carried.getManager());

    // Current stored state differs from the carried copy
    ProjectDto current = storage.getProject(projectId);
    current.setName("Renamed board");
    current.setColor("red");
    current.setDescription("New description");
    current.setManager(new HashSet<>(Set.of("alice")));
    current.setParticipator(new HashSet<>(Set.of("carol")));
    current.setDueDate(localDate(2));
    storage.updateProject(current);
    RequestLifeCycle.restartTransaction();

    long before = System.currentTimeMillis();
    new ProjectModifiedListener().onEvent(new Event<ProjectService, ProjectDto>("exo.project.projectModified", null, carried));
    RequestLifeCycle.restartTransaction();

    ProjectDto stored = storage.getProject(projectId);
    Assert.assertEquals(Set.of("alice"), stored.getManager());
    Assert.assertEquals(Set.of("carol"), stored.getParticipator());
    Assert.assertEquals("Renamed board", stored.getName());
    Assert.assertEquals("red", stored.getColor());
    Assert.assertEquals("New description", stored.getDescription());
    Assert.assertEquals(current.getDueDate().getTime(), stored.getDueDate().getTime());
    Assert.assertTrue(storedLastModifiedDate(projectId) >= before);
  }

  @Test
  public void testDeletedBoardIsLeftAsIs() throws Exception {
    ProjectStorage storage = PortalContainer.getInstance().getComponentInstanceOfType(ProjectStorage.class);
    ProjectDto project = new ProjectDto();
    project.setName("Removed board");
    project.setManager(new HashSet<>(Set.of("alice")));
    project.setParticipator(new HashSet<>());
    project = storage.createProject(project);
    long projectId = project.getId();
    RequestLifeCycle.restartTransaction();
    storage.removeProject(projectId, false);
    RequestLifeCycle.restartTransaction();

    new ProjectModifiedListener().onEvent(new Event<ProjectService, ProjectDto>("exo.project.projectModified", null, project));
    RequestLifeCycle.restartTransaction();

    Assert.assertNull(storage.getProject(projectId));
  }

  @Test
  public void testNullPayloadIsIgnored() throws Exception {
    ProjectStorage storage = PortalContainer.getInstance().getComponentInstanceOfType(ProjectStorage.class);
    ProjectDto project = new ProjectDto();
    project.setName("Untouched board");
    project.setManager(new HashSet<>(Set.of("alice")));
    project.setParticipator(new HashSet<>());
    project.setLastModifiedDate(1000L);
    long projectId = storage.createProject(project).getId();
    RequestLifeCycle.restartTransaction();

    new ProjectModifiedListener().onEvent(new Event<ProjectService, ProjectDto>("exo.project.projectModified", null, null));
    RequestLifeCycle.restartTransaction();

    Assert.assertEquals(1000L, storedLastModifiedDate(projectId));
  }

  private static Date localDate(int day) {
    return Date.from(LocalDate.of(2030, 3, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private long storedLastModifiedDate(long projectId) {
    EntityManagerService entityManagerService = PortalContainer.getInstance()
                                                               .getComponentInstanceOfType(EntityManagerService.class);
    Long date = entityManagerService.getEntityManager()
                                    .createQuery("SELECT p.lastModifiedDate FROM TaskProject p WHERE p.id = :id", Long.class)
                                    .setParameter("id", projectId)
                                    .getSingleResult();
    return date == null ? 0L : date;
  }

}
