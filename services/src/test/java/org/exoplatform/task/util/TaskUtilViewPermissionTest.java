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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

/**
 * Pins that {@link TaskUtil#hasViewPermission(TaskService, TaskDto, Identity)}
 * judges only the identity it is given, whoever is signed in, and answers
 * without an error when nobody is signed in.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskUtilViewPermissionTest {

  private static final long                TASK_ID       = 7L;

  private static final String              PROJECT_GROUP = "/spaces/forms";

  private static MockedStatic<CommonsUtils> commonsUtils;

  @Mock
  private TaskService                      taskService;

  @Mock
  private UserACL                          userAcl;

  private TaskDto                          task;

  /**
   * Routes the platform-admin lookup to a mocked {@link UserACL}.
   */
  @BeforeClass
  public static void initClassContext() {
    commonsUtils = mockStatic(CommonsUtils.class);
  }

  /**
   * Releases the static mock.
   */
  @AfterClass
  public static void cleanClassContext() {
    commonsUtils.close();
  }

  /**
   * Builds a task assigned to {@code assignee}, created by {@code creator},
   * with {@code coworker} as coworker, {@code mentioned} mentioned, in a
   * project whose participants are the members of {@link #PROJECT_GROUP}.
   */
  @Before
  public void setUp() {
    commonsUtils.when(() -> CommonsUtils.getService(UserACL.class)).thenReturn(userAcl);
    lenient().when(userAcl.getAdminGroups()).thenReturn("/platform/administrators");
    lenient().when(taskService.getCoworker(anyLong())).thenReturn(Set.of("coworker"));
    lenient().when(taskService.getMentionedUsers(anyLong())).thenReturn(Set.of("mentioned"));

    ProjectDto project = new ProjectDto();
    project.setParticipator(Set.of("*:" + PROJECT_GROUP));
    project.setManager(Collections.emptySet());
    StatusDto status = new StatusDto();
    status.setProject(project);

    task = new TaskDto();
    task.setId(TASK_ID);
    task.setAssignee("assignee");
    task.setCreatedBy("creator");
    task.setStatus(status);
  }

  /**
   * Leaves no signed-in user behind for the next test.
   */
  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  /**
   * Someone who may not see the task is refused while someone who may see it
   * is signed in.
   */
  @Test
  public void refusesAnOutsiderWhileAViewerIsSignedIn() {
    signIn(new Identity("assignee"));
    assertFalse(TaskUtil.hasViewPermission(taskService, task, new Identity("outsider")));
  }

  /**
   * Each kind of person who may see the task is allowed while someone who may
   * not see it is signed in.
   */
  @Test
  public void allowsEachViewerWhileAnOutsiderIsSignedIn() {
    signIn(new Identity("outsider"));
    assertTrue("assignee", TaskUtil.hasViewPermission(taskService, task, new Identity("assignee")));
    assertTrue("coworker", TaskUtil.hasViewPermission(taskService, task, new Identity("coworker")));
    assertTrue("mentioned space member",
               TaskUtil.hasViewPermission(taskService, task, new Identity("mentioned", Set.of(new MembershipEntry(PROJECT_GROUP, "member")))));
    assertTrue("creator", TaskUtil.hasViewPermission(taskService, task, new Identity("creator")));
    assertTrue("project member", TaskUtil.hasViewPermission(taskService, task, projectMember()));
  }

  /**
   * Someone mentioned in a comment who is not a member of the space of the
   * task's project may not see it, whoever is signed in.
   */
  @Test
  public void refusesAMentionedPersonOutsideTheSpace() {
    signIn(new Identity("assignee"));
    assertFalse(TaskUtil.hasViewPermission(taskService, task, new Identity("mentioned")));
    assertFalse(TaskUtil.hasViewPermission(taskService,
                                           task,
                                           new Identity("mentioned", Set.of(new MembershipEntry("/spaces/other", "member")))));
  }

  /**
   * A member of the space who is mentioned but not on the board, that is
   * neither a manager nor a participator of the project, may see the task,
   * read-only: the mention gives the view, not the edit.
   */
  @Test
  public void allowsAMentionedSpaceMemberWhoIsNotOnTheBoardReadOnly() {
    boardOfManagersAndOneParticipant();
    Identity mentionedMember = new Identity("mentioned", Set.of(new MembershipEntry(PROJECT_GROUP, "member")));
    assertTrue(TaskUtil.hasViewPermission(taskService, task, mentionedMember));
    assertFalse(TaskUtil.hasEditPermission(taskService, task, mentionedMember));
  }

  /**
   * A member of the space who is neither mentioned nor on the board may not
   * see the task.
   */
  @Test
  public void refusesASpaceMemberWhoIsNeitherMentionedNorOnTheBoard() {
    boardOfManagersAndOneParticipant();
    assertFalse(TaskUtil.hasViewPermission(taskService,
                                           task,
                                           new Identity("member", Set.of(new MembershipEntry(PROJECT_GROUP, "member")))));
  }

  /**
   * The space of the project is read from its participators as well as from
   * its managers: a project whose managers are users and whose participators
   * are a space still refuses a mentioned person outside that space.
   */
  @Test
  public void readsTheSpaceFromTheParticipatorsToo() {
    task.getStatus().getProject().setManager(Set.of("owner"));
    assertFalse(TaskUtil.hasViewPermission(taskService, task, new Identity("mentioned")));
  }

  /**
   * On a personal task, with no project and so no space, a mention is enough.
   */
  @Test
  public void allowsAMentionedPersonOnAPersonalTask() {
    task.setStatus(null);
    assertTrue(TaskUtil.hasViewPermission(taskService, task, new Identity("mentioned")));
    assertFalse(TaskUtil.hasEditPermission(taskService, task, new Identity("mentioned")));
    assertFalse(TaskUtil.hasViewPermission(taskService, task, new Identity("outsider")));
  }

  /**
   * In a project that names no space group, a mention is enough.
   */
  @Test
  public void allowsAMentionedPersonInAProjectWithNoSpace() {
    ProjectDto project = task.getStatus().getProject();
    project.setManager(Set.of("owner", "member:/platform/users"));
    project.setParticipator(Set.of("participant"));
    assertTrue(TaskUtil.hasViewPermission(taskService, task, new Identity("mentioned")));
    assertFalse(TaskUtil.hasViewPermission(taskService, task, new Identity("outsider")));
  }

  /**
   * With nobody signed in, the check answers both ways without an error.
   */
  @Test
  public void answersWithNobodySignedIn() {
    ConversationState.setCurrent(null);
    assertFalse(TaskUtil.hasViewPermission(taskService, task, new Identity("outsider")));
    assertTrue(TaskUtil.hasViewPermission(taskService, task, new Identity("coworker")));
    assertTrue(TaskUtil.hasViewPermission(taskService, task, projectMember()));
  }

  /**
   * The checks made for the signed-in user answer exactly as the checks made
   * for a given person, for each kind of person: in particular a person who
   * is only the task's creator may see and edit it either way.
   */
  @Test
  public void signedInChecksAgreeWithTheChecksForAGivenPerson() {
    for (Identity person : new Identity[] { new Identity("assignee"), new Identity("coworker"),
        new Identity("mentioned"), new Identity("creator"), projectMember(), new Identity("outsider") }) {
      signIn(person);
      String who = person.getUserId();
      assertEquals(who, TaskUtil.hasEditPermission(taskService, task, person), TaskUtil.hasEditPermission(taskService, task));
      assertEquals(who, TaskUtil.hasViewPermission(taskService, task, person), TaskUtil.hasViewPermission(taskService, task));
    }
    signIn(new Identity("creator"));
    assertTrue(TaskUtil.hasEditPermission(taskService, task));
    assertTrue(TaskUtil.hasViewPermission(taskService, task));
  }

  /**
   * With nobody signed in, the checks made for the signed-in user refuse,
   * without an error.
   */
  @Test
  public void signedInChecksRefuseWithNobodySignedIn() {
    ConversationState.setCurrent(null);
    assertFalse(TaskUtil.hasEditPermission(taskService, task));
    assertFalse(TaskUtil.hasViewPermission(taskService, task));
    assertFalse(TaskUtil.hasDeletePermission(task));
  }

  /**
   * No identity means no view, whoever is signed in.
   */
  @Test
  public void refusesANullIdentity() {
    signIn(new Identity("assignee"));
    assertFalse(TaskUtil.hasViewPermission(taskService, task, null));
  }

  /**
   * Makes {@code identity} the signed-in user of the current thread.
   *
   * @param identity the signed-in user
   */
  private void signIn(Identity identity) {
    ConversationState.setCurrent(new ConversationState(identity));
  }

  /**
   * Makes the task's project a board of the space's managers and of one
   * participant, so that a mere member of the space is not on it.
   */
  private void boardOfManagersAndOneParticipant() {
    ProjectDto project = task.getStatus().getProject();
    project.setManager(Set.of("manager:" + PROJECT_GROUP));
    project.setParticipator(Set.of("participant"));
  }

  /**
   * Builds a member of the task's project space who is none of the task's own
   * people.
   *
   * @return the member's identity
   */
  private Identity projectMember() {
    return new Identity("member", Set.of(new MembershipEntry(PROJECT_GROUP, "member")));
  }
}
