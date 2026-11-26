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
package org.exoplatform.task.dto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

public class ProjectDtoPermissionTest {

  private static final String USER = "john";

  private static ProjectDto project(Set<String> managers, Set<String> participants) {
    ProjectDto project = new ProjectDto();
    project.setManager(managers);
    project.setParticipator(participants);
    return project;
  }

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

  private static ProjectDto spaceProject() {
    return project(set(USER, "manager:/spaces/team"), set("member:/spaces/team"));
  }

  @Test
  public void namedUserStillSpaceMemberKeepsRights() {
    // redactor:/spaces/team matches no group entry: only the named entry can grant
    Identity member = identity("redactor:/spaces/team");
    assertTrue(spaceProject().canView(member));
    assertTrue(spaceProject().canEdit(member));
  }

  @Test
  public void namedParticipantStillSpaceMemberKeepsView() {
    ProjectDto project = project(set("manager:/spaces/team"), set(USER, "member:/spaces/team"));
    assertTrue(project.canView(identity("redactor:/spaces/team")));
  }

  @Test
  public void namedUserNoLongerSpaceMemberHasNoRight() {
    Identity notMember = identity("member:/platform/users");
    assertFalse(spaceProject().canView(notMember));
    assertFalse(spaceProject().canEdit(notMember));
  }

  @Test
  public void namedParticipantNoLongerSpaceMemberHasNoRight() {
    ProjectDto project = project(set("manager:/spaces/team"), set(USER, "member:/spaces/team"));
    assertFalse(project.canView(identity("member:/spaces/other")));
  }

  @Test
  public void namedUserOnProjectOutsideSpaceUnchanged() {
    ProjectDto project = project(set(USER), set("other"));
    Identity noGroup = identity("member:/platform/users");
    assertTrue(project.canView(noGroup));
    assertTrue(project.canEdit(noGroup));
  }

  @Test
  public void groupBasedGrantsUnchanged() {
    ProjectDto project = project(set("manager:/spaces/team"), set("member:/spaces/team"));
    assertTrue(project.canEdit(identity("manager:/spaces/team")));
    assertTrue(project.canView(identity("member:/spaces/team")));
    assertFalse(project.canEdit(identity("member:/spaces/team")));
    assertFalse(project.canView(identity("member:/spaces/other")));
  }

  @Test
  public void projectOutsideSpaceWithSpaceParticipantKeepsNamedManagerRights() {
    ProjectDto project = project(set(USER, "other"), set("member:/spaces/team"));
    Identity notMember = identity("member:/platform/users");
    assertTrue(project.canView(notMember));
    assertTrue(project.canEdit(notMember));
  }

  @Test
  public void isMemberOfProjectSpacesFollowsSpaceMembership() {
    assertTrue(spaceProject().isMemberOfProjectSpaces(identity("redactor:/spaces/team")));
    assertFalse(spaceProject().isMemberOfProjectSpaces(identity("member:/spaces/other")));
    assertFalse(spaceProject().isMemberOfProjectSpaces(null));
    assertTrue(project(set(USER), set()).isMemberOfProjectSpaces(identity("member:/platform/users")));
  }

  @Test
  public void identityWithoutMembershipsGrantsNoGroupRight() {
    Identity noMemberships = org.mockito.Mockito.mock(Identity.class);
    org.mockito.Mockito.when(noMemberships.getUserId()).thenReturn(USER);
    org.mockito.Mockito.when(noMemberships.getMemberships()).thenReturn(null);
    assertFalse(spaceProject().canView(noMemberships));
  }

  @Test
  public void nullManagerSetGrantsNoEdit() {
    ProjectDto project = new ProjectDto();
    project.setManager(null);
    assertFalse(project.canEdit(identity("member:/spaces/team")));
  }
}
