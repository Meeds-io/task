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
package org.exoplatform.task.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

/**
 * A user named by id on a space project's managers or participants keeps the
 * notifications and the task view only while a member of the space.
 */
public class NamedUserSpaceMembershipTest {

  private static final String USER = "john";

  private static Set<String> set(String... values) {
    Set<String> set = new HashSet<>();
    Collections.addAll(set, values);
    return set;
  }

  private static Identity identity(String... memberships) {
    Set<MembershipEntry> entries = new HashSet<>();
    for (String membership : memberships) {
      entries.add(MembershipEntry.parse(membership));
    }
    return new Identity(USER, entries);
  }

  private static ProjectDto spaceProjectNamingTheUser() {
    ProjectDto project = new ProjectDto();
    project.setManager(set("manager:/spaces/team"));
    project.setParticipator(set(USER));
    return project;
  }

  private static TaskDto taskOf(ProjectDto project) {
    StatusDto status = new StatusDto();
    status.setProject(project);
    TaskDto task = new TaskDto();
    task.setId(7L);
    task.setStatus(status);
    return task;
  }

  @Test
  public void notificationsReachANamedParticipantOnlyWhileSpaceMember() {
    UserACL userAcl = mock(UserACL.class);
    when(userAcl.getUserIdentity(USER)).thenReturn(identity("redactor:/spaces/team"));
    assertTrue(ProjectUtil.isProjectParticipant(userAcl, USER, spaceProjectNamingTheUser()));

    when(userAcl.getUserIdentity(USER)).thenReturn(identity("member:/platform/users"));
    assertFalse(ProjectUtil.isProjectParticipant(userAcl, USER, spaceProjectNamingTheUser()));
  }

  @Test
  public void notificationsReachANamedParticipantOfAProjectOutsideSpaces() {
    UserACL userAcl = mock(UserACL.class);
    when(userAcl.getUserIdentity(USER)).thenReturn(identity("member:/platform/users"));
    ProjectDto project = new ProjectDto();
    project.setManager(set("someone"));
    project.setParticipator(set(USER));
    assertTrue(ProjectUtil.isProjectParticipant(userAcl, USER, project));
  }

  @Test
  public void notificationsSkipAnUnknownNamedParticipant() {
    UserACL userAcl = mock(UserACL.class);
    assertFalse(ProjectUtil.isProjectParticipant(userAcl, USER, spaceProjectNamingTheUser()));
  }

  @Test
  public void taskViewOfANamedUserFollowsSpaceMembership() {
    TaskService taskService = mock(TaskService.class);
    TaskDto task = taskOf(spaceProjectNamingTheUser());
    ProjectDto namedManager = task.getStatus().getProject();
    namedManager.setManager(set(USER, "manager:/spaces/team"));

    assertTrue(TaskUtil.canViewTask(taskService, task, identity("redactor:/spaces/team")));
    assertFalse(TaskUtil.canViewTask(taskService, task, identity("member:/platform/users")));
    assertFalse(TaskUtil.canViewTask(taskService, task, null));
  }

  @Test
  public void taskViewKeptThroughAssignmentOrMentionAfterLeavingTheSpace() {
    TaskService taskService = mock(TaskService.class);
    Identity left = identity("member:/platform/users");
    TaskDto assigned = taskOf(spaceProjectNamingTheUser());
    assigned.setAssignee(USER);
    assertTrue(TaskUtil.canViewTask(taskService, assigned, left));

    TaskDto mentioned = taskOf(spaceProjectNamingTheUser());
    when(taskService.getMentionedUsers(mentioned.getId())).thenReturn(set(USER));
    assertTrue(TaskUtil.canViewTask(taskService, mentioned, left));
  }
}
