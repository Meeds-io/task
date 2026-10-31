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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.dao.condition.AggregateCondition;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dao.condition.LoggedSince;
import org.exoplatform.task.dao.condition.SingleCondition;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.CommentsSummaryDto;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.model.TaskChanges;
import org.exoplatform.task.model.TaskFieldChange;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.LabelService;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.service.impl.TaskServiceImpl;

import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.util.McpToolUtils;
import io.meeds.portal.permlink.service.PermanentLinkService;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import io.meeds.social.space.plugin.SpaceAclPlugin;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.task.mcp.model.ProjectActivityModel;
import io.meeds.task.mcp.model.TaskChange;
import io.meeds.task.mcp.model.TaskChangeLog;
import io.meeds.task.mcp.model.TaskWithChangeLogModel;
import io.meeds.task.mcp.model.TaskWithChangesModel;
import io.meeds.task.mcp.model.ProjectCollectionModel;
import io.meeds.task.mcp.model.ProjectLabel;
import io.meeds.task.mcp.model.ProjectListItemModel;
import io.meeds.task.mcp.model.ProjectModel;
import io.meeds.task.mcp.model.ProjectStatisticsModel;
import io.meeds.task.mcp.model.ProjectStatus;
import io.meeds.task.mcp.model.TaskCollectionModel;
import io.meeds.task.mcp.model.TaskCommentCollectionModel;
import io.meeds.task.mcp.model.TaskListItemModel;
import io.meeds.task.mcp.model.TaskModel;
import io.meeds.task.plugin.TaskAclPlugin;

import lombok.SneakyThrows;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RunWith(MockitoJUnitRunner.class)
public class TaskMcpToolTest {

  private static final String     USER         = "root";

  private static final String     OTHER_USER   = "john";

  private static final long       PROJECT_ID   = 45L;

  private static final long       TASK_ID      = 12L;

  private static final String     TASK_TITLE   = "New title";

  private static final String     COMMENT      = "hello";

  private static final String     PROJECT_NAME = "Project";

  private static final String     LABEL_NAME   = "Feature";

  private static final ZoneId     USER_ZONE    = ZoneId.of("Europe/Paris");

  private static final LocalDate  TODAY        = LocalDate.of(2026, 10, 8);

  private static final JsonMapper JSON         = JsonMapper.builder().build();

  @Mock
  private ProjectService          projectService;

  @Mock
  private StatusService           statusService;

  @Mock
  private TaskService             taskService;

  @Mock
  private CommentService          commentService;

  @Mock
  private LabelService            labelService;

  @Mock
  private SpaceService            spaceService;

  @Mock
  private TranslationService      translationService;

  @Mock
  private ResourceBundleService   resourceBundleService;

  @Mock
  private IdentityManager         identityManager;

  @Mock
  private ProfilePropertyService  profilePropertyService;

  @Mock
  private UserACL                 userAcl;

  @Mock
  private UserPortalConfigService portalConfigService;

  @Mock
  private PermanentLinkService    permanentLinkService;

  @Mock
  private FavoriteService         favoriteService;

  @Mock
  private TaskSpaceScopeResolver  spaceScopeResolver;

  private boolean                 currentSpaceScoped;

  private Long                    currentScopeSpaceId;

  private boolean                 scopeUnreadable;

  @Mock
  private Identity                currentIdentity;

  private TaskMcpTool             tool;

  @Before
  public void setUp() throws Exception {// NOSONAR
    lenient().when(currentIdentity.getUserId()).thenReturn(USER);

    ResourceBundle emptyBundle = new ListResourceBundle() {
      @Override
      protected Object[][] getContents() {
        return new Object[0][0];
      }
    };
    lenient().when(resourceBundleService.getResourceBundle(anyString(), any(Locale.class))).thenReturn(emptyBundle);
    lenient().when(permanentLinkService.getLink(any())).thenReturn("/task-link");

    // the query building is the task service's: the real one narrows the
    // query the tool builds
    TaskServiceImpl queryBuilder = new TaskServiceImpl(null, null, null);
    lenient().doAnswer(invocation -> {
      queryBuilder.addChangeFilter(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(taskService).addChangeFilter(any(), any());
    lenient().when(taskService.getAssignedTasksQuery(anyString(), any()))
             .thenAnswer(invocation -> queryBuilder.getAssignedTasksQuery(invocation.getArgument(0), invocation.getArgument(1)));
    lenient().when(taskService.getTaskChanges(any(), any(), anyBoolean())).thenReturn(new TaskChanges(List.of(), null));

    tool = new TestableTaskMcpTool();
  }

  @Test(expected = IllegalArgumentException.class)
  public void getProjectByIdWhenProjectIdNullShouldThrowException() throws Exception {// NOSONAR
    tool.getProjectById(null);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void getProjectByIdWhenProjectDoesNotExistShouldThrowException() throws Exception {// NOSONAR
    when(projectService.getProject(PROJECT_ID)).thenReturn(null);

    tool.getProjectById(PROJECT_ID);
  }

  @Test(expected = IllegalAccessException.class)
  public void getProjectByIdWhenProjectIsNotViewableShouldThrowException() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(false);

    tool.getProjectById(PROJECT_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void assignTaskWhenUsernameBlankShouldThrowException() throws Exception {// NOSONAR
    tool.assignTask(TASK_ID, " ");
  }

  @Test(expected = ObjectNotFoundException.class)
  public void assignTaskWhenTaskDoesNotExistShouldThrowException() throws Exception {// NOSONAR
    when(taskService.getTask(TASK_ID)).thenReturn(null);

    tool.assignTask(TASK_ID, OTHER_USER);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void assignTaskWhenTargetUserDoesNotExistShouldThrowException() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(userAcl.getUserIdentity("john")).thenReturn(null);

    tool.assignTask(TASK_ID, OTHER_USER);
  }

  @Test
  public void assignTaskShouldStripMentionPrefixAndUpdateTask() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    Identity johnIdentity = mock(Identity.class);

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.getUserIdentity("john")).thenReturn(johnIdentity);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);

    runWithDateFormatMockResult(() -> tool.assignTask(TASK_ID, OTHER_USER));

    verify(task).setAssignee("john");
    verify(taskService).updateTask(task);
  }

  /**
   * A caller who may see a task but not edit it, such as someone only
   * mentioned in it, changes nothing on it: assigning it, adding a coworker,
   * putting on or taking off a label and moving it to another status are all
   * refused, and the task is left unchanged.
   * Mutants: any of these tools guarded by the view right instead of the edit
   * right.
   *
   * @throws Exception never
   */
  @Test
  public void aCallerWhoMayOnlySeeATaskChangesNothingOnIt() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    lenient().when(userAcl.getUserIdentity(OTHER_USER)).thenReturn(mock(Identity.class));
    // the project is visible to everyone, so only the caller's edit right can refuse
    lenient().when(task.getStatus().getProject().canView(any())).thenReturn(true);
    lenient().when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> tool.assignTask(TASK_ID, OTHER_USER));
    assertThrows(IllegalAccessException.class, () -> tool.assignTaskToMe(TASK_ID));
    assertThrows(IllegalAccessException.class, () -> tool.addTaskCoworker(TASK_ID, OTHER_USER));
    assertThrows(IllegalAccessException.class, () -> tool.addProjectLabelToTask(TASK_ID, 99L));
    assertThrows(IllegalAccessException.class, () -> tool.removeProjectLabelFromTask(TASK_ID, 99L));
    assertThrows(IllegalAccessException.class, () -> tool.updateTaskStatus(TASK_ID, 8L));

    verify(taskService, never()).updateTask(any());
    verify(labelService, never()).addTaskToLabel(any(), anyLong());
    verify(labelService, never()).removeTaskFromLabel(any(), anyLong());
  }

  /**
   * The person put on a task as assignee or coworker is judged on their own
   * rights, never on the signed-in user's: on a project task, someone the
   * project isn't visible to is refused by both tools and the task is left
   * unchanged, and someone it is visible to is accepted; on a personal task,
   * anyone is accepted.
   * Mutants: the check made on the signed-in user, made on one tool only, or
   * refusing a personal task.
   *
   * @throws Exception never
   */
  @Test
  public void personPutOnATaskIsJudgedOnTheirOwnRights() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    ProjectDto project = task.getStatus().getProject();
    Identity johnIdentity = mock(Identity.class);
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.getUserIdentity(OTHER_USER)).thenReturn(johnIdentity);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    lenient().when(userAcl.getUserIdentity(USER)).thenReturn(currentIdentity);
    lenient().when(project.canView(currentIdentity)).thenReturn(true);
    lenient().when(taskService.updateTask(task)).thenReturn(task);

    when(project.canView(johnIdentity)).thenReturn(false);
    assertThrows(IllegalAccessException.class, () -> tool.assignTask(TASK_ID, OTHER_USER));
    assertThrows(IllegalAccessException.class, () -> tool.addTaskCoworker(TASK_ID, OTHER_USER));
    verify(taskService, never()).updateTask(any());

    when(project.canView(johnIdentity)).thenReturn(true);
    runWithDateFormatMockResult(() -> tool.assignTask(TASK_ID, OTHER_USER));
    runWithDateFormatMockResult(() -> tool.addTaskCoworker(TASK_ID, OTHER_USER));
    verify(task).setAssignee(OTHER_USER);

    TaskDto personalTask = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(personalTask);
    when(taskService.updateTask(personalTask)).thenReturn(personalTask);
    runWithDateFormatMockResult(() -> tool.assignTask(TASK_ID, OTHER_USER));
    verify(personalTask).setAssignee(OTHER_USER);
  }

  /**
   * In a call limited to a space, a task is assigned to, or gets as a
   * coworker, a member of that space only: on a personal task, which accepts
   * anyone, a person who isn't a member is refused, and so is anyone when the
   * call's identity can't be read. A call that isn't limited keeps the task's
   * own rule alone.
   * Mutants: the membership check removed, applied to one tool only, run on
   * an unlimited call, or an unreadable identity taken as unlimited.
   *
   * @throws Exception never
   */
  @Test
  public void peopleOfACallLimitedToASpaceAreMembersOfIt() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    Identity johnIdentity = mock(Identity.class);
    Space space = mock(Space.class);
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.getUserIdentity(OTHER_USER)).thenReturn(johnIdentity);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    lenient().when(taskService.updateTask(task)).thenReturn(task);
    when(spaceService.getSpaceById(12L)).thenReturn(space);

    currentScopeSpaceId = 12L;
    when(spaceService.isMember(space, OTHER_USER)).thenReturn(false);
    assertThrows(IllegalAccessException.class, () -> tool.assignTask(TASK_ID, OTHER_USER));
    assertThrows(IllegalAccessException.class, () -> tool.addTaskCoworker(TASK_ID, OTHER_USER));
    verify(taskService, never()).updateTask(any());

    currentScopeSpaceId = 13L;
    assertThrows(IllegalAccessException.class, () -> tool.assignTask(TASK_ID, OTHER_USER));

    currentScopeSpaceId = null;
    scopeUnreadable = true;
    assertThrows(IllegalAccessException.class, () -> tool.assignTask(TASK_ID, OTHER_USER));
    assertThrows(IllegalAccessException.class, () -> tool.addTaskCoworker(TASK_ID, OTHER_USER));
    verify(taskService, never()).updateTask(any());

    scopeUnreadable = false;
    currentScopeSpaceId = 12L;
    when(spaceService.isMember(space, OTHER_USER)).thenReturn(true);
    runWithDateFormatMockResult(() -> tool.assignTask(TASK_ID, OTHER_USER));
    runWithDateFormatMockResult(() -> tool.addTaskCoworker(TASK_ID, OTHER_USER));
    verify(taskService, times(2)).updateTask(task);

    currentScopeSpaceId = null;
    lenient().when(spaceService.isMember(space, OTHER_USER)).thenReturn(false);
    runWithDateFormatMockResult(() -> tool.assignTask(TASK_ID, OTHER_USER));
    verify(taskService, times(3)).updateTask(task);
  }

  /**
   * In a call limited to a space, a super manager of that space counts as one
   * of its people, as for the platform's space rules: the task is assigned to
   * them, they are added as a coworker and a comment may mention them,
   * though they aren't a member.
   * Mutant: the super-manager clause removed.
   *
   * @throws Exception never
   */
  @Test
  public void aSuperManagerOfTheScopeSpaceCountsAsOneOfItsPeople() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    Identity johnIdentity = mock(Identity.class);
    Space space = mock(Space.class);
    CommentDto comment = mock(CommentDto.class);
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.getUserIdentity(OTHER_USER)).thenReturn(johnIdentity);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    when(spaceService.getSpaceById(12L)).thenReturn(space);
    when(spaceService.isMember(space, OTHER_USER)).thenReturn(false);
    when(spaceService.isSuperManager(space, OTHER_USER)).thenReturn(true);
    String mention = "Please check @" + OTHER_USER;
    when(commentService.addComment(task, USER, mention)).thenReturn(comment);
    when(comment.getId()).thenReturn(22L);
    when(comment.getComment()).thenReturn(mention);
    when(comment.getCreatedTime()).thenReturn(new Date());
    when(comment.getAuthor()).thenReturn(USER);

    currentScopeSpaceId = 12L;
    runWithDateFormatMockResult(() -> tool.assignTask(TASK_ID, OTHER_USER));
    runWithDateFormatMockResult(() -> tool.addTaskCoworker(TASK_ID, OTHER_USER));
    runWithDateFormatMockResult(() -> tool.addTaskComment(TASK_ID, mention));

    verify(taskService, times(2)).updateTask(task);
    verify(commentService).addComment(task, USER, mention);
  }

  @Test
  public void assignTaskToMeShouldAssignCurrentUser() throws Exception {// NOSONAR
    TaskDto task = mockTask();

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.getUserIdentity(USER)).thenReturn(currentIdentity);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);

    runWithDateFormatMockResult(() -> tool.assignTaskToMe(TASK_ID));

    verify(task).setAssignee(USER);
    verify(taskService).updateTask(task);
  }

  @Test
  public void addTaskCoworkerShouldCreateCoworkerSetWhenEmpty() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    Identity johnIdentity = mock(Identity.class);

    when(task.getCoworker()).thenReturn(null);
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.getUserIdentity("john")).thenReturn(johnIdentity);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);

    runWithDateFormatMockResult(() -> tool.addTaskCoworker(TASK_ID, OTHER_USER));

    verify(task).setCoworker(Collections.singleton("john"));
    verify(taskService).updateTask(task);
  }

  @Test(expected = IllegalAccessException.class)
  public void updateTaskTitleWhenCurrentUserCannotEditShouldThrowException() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(false);

    tool.updateTaskTitle(TASK_ID, TASK_TITLE);
  }

  @Test
  public void updateTaskTitleShouldUpdateAndPersistTask() throws Exception {// NOSONAR
    TaskDto task = mockTask();

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    runWithDateFormatMockResult(() -> tool.updateTaskTitle(TASK_ID, TASK_TITLE));
    verify(task).setTitle(TASK_TITLE);
    verify(taskService).updateTask(task);
  }

  @Test
  public void listProjectStatusesShouldMapStatuses() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto todo = mock(StatusDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(statusService.getStatuses(PROJECT_ID)).thenReturn(Collections.singletonList(todo));
    when(todo.getId()).thenReturn(7L);
    when(todo.getName()).thenReturn("todo");
    when(todo.getRank()).thenReturn(2);

    List<ProjectStatus> statuses = tool.listProjectStatuses(PROJECT_ID);

    assertEquals(1, statuses.size());
    assertEquals(7L, statuses.get(0).getId());
    assertEquals("todo", statuses.get(0).getName());
    assertEquals(2, statuses.get(0).getColumnPosition());
  }

  @Test
  public void createProjectLabelWhenLabelAlreadyExistsShouldReturnExistingLabel() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    LabelDto label = mock(LabelDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(true);
    when(labelService.findLabelsByProject(PROJECT_ID, currentIdentity, 0, 100))
                                                                               .thenReturn(Collections.singletonList(label));
    when(label.getId()).thenReturn(99L);
    when(label.getName()).thenReturn("Bug");

    ProjectLabel result = tool.createProjectLabel(PROJECT_ID, "Bug");

    assertEquals(99L, result.getId());
    assertEquals("Bug", result.getName());
    verify(labelService, never()).createLabel(any(LabelDto.class));
  }

  @Test
  public void addProjectLabelToTaskShouldDelegateToLabelService() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    LabelDto label = mock(LabelDto.class);

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(labelService.getLabel(99L)).thenReturn(label);

    tool.addProjectLabelToTask(TASK_ID, 99L);

    verify(labelService).addTaskToLabel(task, 99L);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void addProjectLabelToTaskWhenLabelDoesNotExistShouldThrowException() throws Exception {// NOSONAR
    TaskDto task = mockTask();

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(labelService.getLabel(99L)).thenReturn(null);

    tool.addProjectLabelToTask(TASK_ID, 99L);
  }

  /**
   * A label of the task's project is put on and taken off the task.
   * Mutant: the project comparison inverted.
   *
   * @throws Exception never
   */
  @Test
  public void labelOfTheTaskProjectIsPutOnAndTakenOffTheTask() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    LabelDto label = projectLabel(PROJECT_ID);
    when(labelService.getLabel(99L)).thenReturn(label);

    tool.addProjectLabelToTask(TASK_ID, 99L);
    tool.removeProjectLabelFromTask(TASK_ID, 99L);

    verify(labelService).addTaskToLabel(task, 99L);
    verify(labelService).removeTaskFromLabel(task, 99L);
  }

  /**
   * A label of another project, or a label in no project, is neither put on
   * nor taken off a task of a project: in a call limited to the space of the
   * task's project, the label is the only argument beside the task, and it
   * must not reach a project of another space.
   * Mutants: the check removed, or applied to the add or the remove only.
   *
   * @throws Exception never
   */
  @Test
  public void labelOfAnotherProjectIsRefusedOnATaskOfAProject() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    LabelDto otherProjectLabel = projectLabel(PROJECT_ID + 1);
    LabelDto noProjectLabel = mock(LabelDto.class);
    when(labelService.getLabel(98L)).thenReturn(otherProjectLabel);
    when(labelService.getLabel(97L)).thenReturn(noProjectLabel);

    for (long labelId : new long[] { 98L, 97L }) {
      assertThrows(ObjectNotFoundException.class, () -> tool.addProjectLabelToTask(TASK_ID, labelId));
      assertThrows(ObjectNotFoundException.class, () -> tool.removeProjectLabelFromTask(TASK_ID, labelId));
    }
    verify(labelService, never()).addTaskToLabel(any(), anyLong());
    verify(labelService, never()).removeTaskFromLabel(any(), anyLong());
  }

  @Test
  public void listTaskLabelsShouldReturnAssignedLabels() throws Exception {// NOSONAR
    TaskDto task = mock(TaskDto.class);
    StatusDto status = mock(StatusDto.class);
    ProjectDto project = mock(ProjectDto.class);
    LabelDto label = mock(LabelDto.class);

    when(task.getStatus()).thenReturn(status);
    when(status.getProject()).thenReturn(project);
    when(project.getId()).thenReturn(PROJECT_ID);
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(label.getId()).thenReturn(7L);
    when(label.getName()).thenReturn("urgent");
    when(labelService.findLabelsByTask(task, PROJECT_ID, currentIdentity, 0, 100)).thenReturn(Collections.singletonList(label));

    List<ProjectLabel> result = tool.listTaskLabels(TASK_ID);

    assertEquals(1, result.size());
    assertEquals(7L, result.get(0).getId());
    assertEquals("urgent", result.get(0).getName());
  }

  @Test
  public void updateProjectLabelShouldRenameAndReturn() throws Exception {// NOSONAR
    LabelDto label = mock(LabelDto.class);
    ProjectDto project = mock(ProjectDto.class);

    when(labelService.getLabel(99L)).thenReturn(label);
    when(label.getProject()).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(true);
    when(labelService.updateLabel(label)).thenReturn(label);
    when(label.getId()).thenReturn(99L);
    when(label.getName()).thenReturn("Renamed");

    ProjectLabel result = tool.updateProjectLabel(99L, "Renamed");

    verify(label).setName("Renamed");
    verify(labelService).updateLabel(label);
    assertEquals(99L, result.getId());
    assertEquals("Renamed", result.getName());
  }

  @Test(expected = IllegalAccessException.class)
  public void updateProjectLabelWhenNotEditableShouldThrowException() throws Exception {// NOSONAR
    LabelDto label = mock(LabelDto.class);
    ProjectDto project = mock(ProjectDto.class);

    when(labelService.getLabel(99L)).thenReturn(label);
    when(label.getProject()).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(false);

    tool.updateProjectLabel(99L, "Renamed");
  }

  @Test
  public void deleteProjectLabelShouldDelegateToService() throws Exception {// NOSONAR
    LabelDto label = mock(LabelDto.class);
    ProjectDto project = mock(ProjectDto.class);

    when(labelService.getLabel(99L)).thenReturn(label);
    when(label.getProject()).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(true);

    tool.deleteProjectLabel(99L);

    verify(labelService).removeLabel(99L);
  }

  @Test
  public void updateTaskStatusShouldSetStatusAndPersistTask() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    ProjectDto project = mock(ProjectDto.class);
    StatusDto currentStatus = mock(StatusDto.class);
    StatusDto newStatus = mock(StatusDto.class);

    when(project.getId()).thenReturn(PROJECT_ID);
    when(currentStatus.getProject()).thenReturn(project);
    when(newStatus.getProject()).thenReturn(project);
    when(task.getStatus()).thenReturn(currentStatus);

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(statusService.getStatus(8L)).thenReturn(newStatus);

    tool.updateTaskStatus(TASK_ID, 8L);

    verify(task).setStatus(newStatus);
    verify(taskService).updateTask(task);
  }

  @Test(expected = IllegalAccessException.class)
  public void setTaskDatesWhenCurrentUserCannotEditShouldThrowException() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(false);
    tool.setTaskDates(TASK_ID, "2024-01-01", null);
  }

  @Test
  public void setTaskDatesShouldUpdateAndPersistTask() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    runWithDateFormatMockResult(() -> tool.setTaskDates(TASK_ID, "2024-01-01", "2024-01-05"));
    verify(task).setStartDate(any());
    verify(task).setDueDate(any());
    verify(taskService).updateTask(task);
  }

  @Test
  public void setTaskDatesShouldNotOverwriteOmittedDates() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    // Setting only the start date must not wipe an existing due date
    runWithDateFormatMockResult(() -> tool.setTaskDates(TASK_ID, "2024-01-05", null));
    verify(task).setStartDate(any());
    verify(task, never()).setDueDate(any());
    verify(task, never()).setEndDate(any());
    verify(taskService).updateTask(task);
  }

  @Test
  public void setTaskDatesWithOnlyDueDateShouldUpdateDueNotStartAndNeverEnd() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    // Setting only the due date updates the due date, not the start date, and never the vestigial end date
    runWithDateFormatMockResult(() -> tool.setTaskDates(TASK_ID, null, "2024-01-10"));
    verify(task).setDueDate(any());
    verify(task, never()).setStartDate(any());
    verify(task, never()).setEndDate(any());
    verify(taskService).updateTask(task);
  }

  @Test
  public void completeTaskShouldSetCompletedAndPersist() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    runWithDateFormatMockResult(() -> tool.completeTask(TASK_ID));
    verify(task).setCompleted(true);
    verify(taskService).updateTask(task);
  }

  @Test
  public void reopenTaskShouldSetNotCompletedAndPersist() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    runWithDateFormatMockResult(() -> tool.reopenTask(TASK_ID));
    verify(task).setCompleted(false);
    verify(taskService).updateTask(task);
  }

  @Test
  public void setTaskPriorityShouldSetPriorityAndPersist() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    runWithDateFormatMockResult(() -> tool.setTaskPriority(TASK_ID, Priority.HIGH));
    verify(task).setPriority(Priority.HIGH);
    verify(taskService).updateTask(task);
  }

  @Test
  public void updateProjectShouldUpdateFieldsAndPersist() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(true);
    when(projectService.updateProject(project)).thenReturn(project);
    runWithDateFormatMockResult(() -> tool.updateProject(PROJECT_ID, "Renamed", "desc", "red"));
    verify(project).setName("Renamed");
    verify(project).setDescription("desc");
    verify(project).setColor("red");
    verify(projectService).updateProject(project);
  }

  @Test
  public void addProjectMemberShouldPersistProject() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(true);
    when(project.getParticipator()).thenReturn(new HashSet<>());
    when(projectService.updateProject(project)).thenReturn(project);
    runWithDateFormatMockResult(() -> tool.addProjectMember(PROJECT_ID, "@john"));
    verify(project).setParticipator(any());
    verify(projectService).updateProject(project);
  }

  @Test
  public void createProjectStatusShouldDelegateToStatusService() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto status = mock(StatusDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(true);
    when(statusService.createStatus(project, "Review")).thenReturn(status);
    when(status.getId()).thenReturn(9L);
    when(status.getName()).thenReturn("Review");
    when(status.getRank()).thenReturn(4);
    ProjectStatus result = tool.createProjectStatus(PROJECT_ID, "Review");
    assertEquals(9L, result.getId());
    assertEquals("Review", result.getName());
  }

  @Test
  public void getProjectStatisticsShouldCountByStatus() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto todo = mock(StatusDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(statusService.getStatuses(PROJECT_ID)).thenReturn(Collections.singletonList(todo));
    when(todo.getName()).thenReturn("ToDo");
    when(taskService.countTaskStatusByProject(PROJECT_ID)).thenReturn(Collections.singletonList(new Object[] { "ToDo", 3L }));
    ProjectStatisticsModel stats = tool.getProjectStatistics(PROJECT_ID);
    assertEquals(3L, stats.getTotalUncompletedTasks());
    assertEquals(Long.valueOf(3L), stats.getUncompletedByStatus().get("ToDo"));
  }

  @Test
  public void favoriteTaskShouldCreateFavorite() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    org.exoplatform.social.core.identity.model.Identity identity =
                                                                 mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identity.getId()).thenReturn("1");
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(identity);
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(favoriteService.isFavorite(any())).thenReturn(false);
    tool.favoriteTask(TASK_ID);
    verify(favoriteService).createFavorite(any());
  }

  @Test
  public void unfavoriteProjectShouldDeleteFavorite() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    org.exoplatform.social.core.identity.model.Identity identity =
                                                                 mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identity.getId()).thenReturn("1");
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(identity);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(favoriteService.isFavorite(any())).thenReturn(true);
    tool.unfavoriteProject(PROJECT_ID);
    verify(favoriteService).deleteFavorite(any());
  }

  @Test
  public void deleteTaskCommentByAuthorShouldRemoveComment() throws Exception {// NOSONAR
    CommentDto comment = mock(CommentDto.class);
    TaskDto task = mock(TaskDto.class);
    when(task.getId()).thenReturn(TASK_ID);
    when(comment.getTask()).thenReturn(task);
    when(comment.getAuthor()).thenReturn(USER);
    when(commentService.getComment(55L)).thenReturn(comment);
    tool.deleteTaskComment(55L);
    verify(commentService).removeComment(55L);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void getProjectIdByTaskIdWhenTaskDoesNotExistShouldThrowException() throws Exception {// NOSONAR
    when(taskService.getTask(TASK_ID)).thenThrow(new EntityNotFoundException(TASK_ID, Task.class));

    tool.getProjectIdByTaskId(TASK_ID);
  }

  /**
   * The overdue cut-off is taken in the time zone of the user's profile,
   * and in the server's only when the profile has none. Runs the real
   * {@code getCurrentUserZone}, which the other tests stub. Mutant: the
   * server's zone always used.
   */
  @Test
  public void theOverdueCutOffUsesTheUsersProfileTimeZone() {// NOSONAR
    TaskMcpTool realZoneTool = new TaskMcpTool(projectService,
                                               statusService,
                                               taskService,
                                               commentService,
                                               labelService,
                                               spaceService,
                                               translationService,
                                               resourceBundleService,
                                               identityManager,
                                               profilePropertyService,
                                               userAcl,
                                               portalConfigService,
                                               permanentLinkService,
                                               favoriteService,
                                               spaceScopeResolver);
    ZoneId profileZone = ZoneId.systemDefault().equals(ZoneId.of("Pacific/Kiritimati")) ? ZoneId.of("Pacific/Pago_Pago")
                                                                                       : ZoneId.of("Pacific/Kiritimati");
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getUserTimeZone).thenReturn(TimeZone.getTimeZone(profileZone));
      assertEquals(profileZone, realZoneTool.getCurrentUserZone());

      mocked.when(McpToolUtils::getUserTimeZone).thenReturn(null);
      assertEquals(ZoneId.systemDefault(), realZoneTool.getCurrentUserZone());
    }
  }

  /**
   * A task is overdue from the day after its due date, in the user's time
   * zone: due yesterday is 1 day overdue, due today is not overdue, and a
   * due date late yesterday in UTC that is already today in Paris is not
   * overdue there. A completed task, or one with no due date, never is.
   * Mutant: the server's zone used, or a task due today counted.
   */
  @Test
  public void daysOverdueCountsWholeDaysInTheUsersTimeZone() {// NOSONAR
    assertEquals(Integer.valueOf(1), TaskMcpTool.getDaysOverdue(openTaskDue("2026-10-07T00:00:00+02:00"), USER_ZONE, TODAY));
    assertEquals(Integer.valueOf(10), TaskMcpTool.getDaysOverdue(openTaskDue("2026-09-28T00:00:00+02:00"), USER_ZONE, TODAY));
    assertNull(TaskMcpTool.getDaysOverdue(openTaskDue("2026-10-08T00:00:00+02:00"), USER_ZONE, TODAY));
    assertNull(TaskMcpTool.getDaysOverdue(openTaskDue("2026-10-07T23:30:00Z"), USER_ZONE, TODAY));
    assertEquals(Integer.valueOf(1), TaskMcpTool.getDaysOverdue(openTaskDue("2026-10-07T23:30:00Z"), ZoneOffset.UTC, TODAY));
    TaskDto completed = openTaskDue("2026-09-28T00:00:00+02:00");
    when(completed.isCompleted()).thenReturn(true);
    assertNull(TaskMcpTool.getDaysOverdue(completed, USER_ZONE, TODAY));
    assertNull(TaskMcpTool.getDaysOverdue(mock(TaskDto.class), USER_ZONE, TODAY));
  }

  /**
   * Without a project, overdue_only lists every task the user can access
   * that is open and due before the start of today in their time zone,
   * personal tasks included; the count is the count of that same query, and
   * each task carries its days overdue. Mutants: no due-date condition; the
   * count of every task; the user's scope dropped.
   */
  @Test
  public void listTasksOverdueOnlySelectsOpenTasksDueBeforeTodayInTheUsersZone() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(task.getDueDate()).thenReturn(Date.from(Instant.parse("2026-09-28T00:00:00+02:00")));
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.singletonList(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(null, null, false, true, null, null, null, null, null, null, null, null));

    assertEquals(Date.from(Instant.parse("2026-10-08T00:00:00+02:00")), findDueDateBefore(query.getValue()));
    assertTrue(selectsOpenTasksOnly(query.getValue()));
    assertTrue("only the tasks the user can access", scopedToTheUser(query.getValue()));
    assertMostOverdueFirst(query.getValue());
    verify(taskService, never()).findLastUpdatedTasks(any(), anyInt(), anyInt());
    verify(taskService).countTasks(query.getValue());
    assertEquals(1L, result.getCount());
    assertEquals(Integer.valueOf(10), ((TaskListItemModel) result.getTasks().get(0)).getDaysOverdue());
  }

  /**
   * Without overdue_only, list_tasks counts with the query it lists,
   * hide_completed_tasks included. Mutant: the count of every task.
   */
  @Test
  public void listTasksCountsWhatItLists() throws Exception {// NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(4);

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null);

    assertNull(findDueDateBefore(query.getValue()));
    assertTrue(selectsOpenTasksOnly(query.getValue()));
    verify(taskService).countTasks(query.getValue());
    assertEquals(4L, result.getCount());
  }

  /**
   * overdue_only on list_assigned_tasks lists the overdue tasks the user is
   * assigned to or a coworker of, counted by the same query. Mutant: the
   * plain uncompleted list.
   */
  @Test
  public void listAssignedTasksOverdueOnlyQueriesTheOverdueTasks() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(task.getDueDate()).thenReturn(Date.from(Instant.parse("2026-10-07T00:00:00+02:00")));
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findTasks(query.capture(), eq(0), eq(5))).thenReturn(Collections.singletonList(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, 5, true, null, null, null));

    assertEquals(Date.from(Instant.parse("2026-10-08T00:00:00+02:00")), findDueDateBefore(query.getValue()));
    assertTrue("only the tasks the user is assigned to or a coworker of", scopedToTheUser(query.getValue()));
    assertMostOverdueFirst(query.getValue());
    verify(taskService, never()).getUncompletedTasks(anyString(), anyInt());
    assertEquals(1L, result.getCount());
    assertEquals(Integer.valueOf(1), ((TaskListItemModel) result.getTasks().get(0)).getDaysOverdue());
  }

  /**
   * changed_since on list_tasks of a project narrows the project's tasks to
   * those with a status, project, due date, assignee or completion change
   * at or after the start of that day in the user's time zone, completed
   * tasks included, counted by the same query; each task carries its
   * changes as the task service rebuilds them since that same date; with
   * hide_completed_tasks, only the open ones. Mutants: the filter not
   * passed, or with the date read in UTC; the plain models returned.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksChangedSinceListsTheTasksThatMovedWithTheirOldAndNewStatus() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    TaskDto task = mockTaskWithProject();
    ArgumentCaptor<Date> since = ArgumentCaptor.forClass(Date.class);
    when(taskService.getTaskChanges(eq(task), since.capture(), eq(false)))
        .thenReturn(new TaskChanges(List.of(new TaskFieldChange(TaskFieldChange.FIELD_STATUS,
                                                                 "ToDo",
                                                                 "InProgress",
                                                                 null,
                                                                 Instant.parse("2026-10-06T10:00:00Z").toEpochMilli(),
                                                                 USER)),
                                    null));
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(PROJECT_ID,
                                                                                  true,
                                                                                  false,
                                                                                  null,
                                                                                  null,
                                                                                  null,
                                                                                  null,
                                                                                  "2026-10-01",
                                                                                  null,
                                                                                  null,
                                                                                  null, null));

    List<Condition> conjunction = conjunction(query.getValue());
    assertTrue(conjunction.stream().anyMatch(c -> isSingle(c, SingleCondition.IN, "status.project.id", List.of(PROJECT_ID))));
    assertChangedSince(query.getValue(), Date.from(Instant.parse("2026-09-30T22:00:00Z")));
    assertEquals(Date.from(Instant.parse("2026-09-30T22:00:00Z")), since.getValue());
    assertTrue("hide_completed_tasks keeps the completed tasks out", selectsOpenTasksOnly(query.getValue()));
    verify(taskService).countTasks(query.getValue());
    assertEquals(1L, result.getCount());
    TaskListItemModel model = (TaskListItemModel) result.getTasks().get(0);
    assertEquals(1, model.getChanges().size());
    TaskChange change = model.getChanges().get(0);
    assertEquals(TaskChange.FIELD_STATUS, change.getField());
    assertEquals("ToDo", change.getOldValue());
    assertEquals("InProgress", change.getNewValue());
  }

  /**
   * completed_since selects the completed tasks with a completion entry at
   * or after the date, whatever hide_completed_tasks says, which would
   * otherwise select the open tasks only and so nothing. Mutants: the
   * completed condition dropped; hide_completed_tasks applied too.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksCompletedSinceSelectsTheTasksCompletedSinceThatDate() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listTasks(null, true, false, null, null, null, null, null, "2026-10-05T08:00:00Z", null, null, null);

    LoggedSince loggedSince = findLoggedSince(query.getValue());
    assertEquals(List.of("mark_done"), loggedSince.actionNames());
    assertEquals(Date.from(Instant.parse("2026-10-05T08:00:00Z")), loggedSince.since());
    assertTrue(conjunction(query.getValue()).stream()
                                            .anyMatch(c -> c instanceof SingleCondition<?> single
                                                           && SingleCondition.IS_TRUE.equals(single.getType())
                                                           && "completed".equals(single.getField())));
    assertFalse(selectsOpenTasksOnly(query.getValue()));
    assertTrue("only the tasks the user can access", scopedToTheUser(query.getValue()));
  }

  /**
   * assignee and status_id narrow the accessible tasks: both are members of
   * the query's conjunction, beside the accessibility; a blank assignee
   * adds no condition. Mutants: either filter dropped, the assignee
   * untrimmed, a blank assignee matching nobody.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksNarrowsByAssigneeAndStatus() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listTasks(null, false, false, null, null, null, null, null, null, " john ", 8L, null);
    tool.listTasks(null, false, false, null, null, null, null, null, null, " ", null, null);

    List<Condition> filtered = conjunction(query.getAllValues().get(0));
    assertTrue(filtered.stream()
                       .anyMatch(c -> c instanceof AggregateCondition aggregate && AggregateCondition.OR.equals(aggregate.getType())));
    assertTrue(filtered.stream().anyMatch(c -> isSingle(c, SingleCondition.IN, "assignee", List.of(OTHER_USER))));
    assertTrue(filtered.stream().anyMatch(c -> isSingle(c, SingleCondition.EQ, "status.id", 8L)));
    List<Condition> unfiltered = conjunction(query.getAllValues().get(1));
    assertTrue(unfiltered.stream()
                         .noneMatch(c -> c instanceof SingleCondition<?> single
                                         && ("assignee".equals(single.getField()) || "status.id".equals(single.getField()))));
  }

  /**
   * changed_since on list_assigned_tasks queries the tasks the user is
   * assigned to or a coworker of with a change since the date, completed
   * ones included, the last updated first, counted by the same query, each
   * with its changes. Mutant: the plain uncompleted list.
   *
   * @throws Exception never
   */
  @Test
  public void listAssignedTasksChangedSinceQueriesTheChangedTasks() throws Exception { // NOSONAR
    TaskDto task = mockTask();
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(5))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, 5, null, "2026-10-01", null, null));

    assertTrue("only the tasks the user is assigned to or a coworker of", scopedToTheUser(query.getValue()));
    assertChangedSince(query.getValue(), Date.from(Instant.parse("2026-09-30T22:00:00Z")));
    assertFalse(selectsOpenTasksOnly(query.getValue()));
    verify(taskService, never()).getUncompletedTasks(anyString(), anyInt());
    verify(taskService).countTasks(query.getValue());
    assertEquals(1L, result.getCount());
    assertTrue("a compact item with its changes", ((TaskListItemModel) result.getTasks().get(0)).getChanges() != null);
  }

  /**
   * The change log listed resolves a target as a user for an assignment
   * entry only: a creation or a status entry names a status there, which is
   * never looked up as a user. Mutant: every target looked up.
   *
   * @throws Exception never
   */
  @Test
  public void onlyAnAssignmentEntryHasAUserTarget() throws Exception { // NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTaskLogs(eq(TASK_ID), anyInt(), anyInt())).thenReturn(List.of(log(3, "assign", OTHER_USER, "2026-10-06T10:00:00Z"),
                                                                                      log(2, "edit_status", "Done", "2026-10-05T10:00:00Z"),
                                                                                      log(1, "created", "ToDo", "2026-10-04T10:00:00Z")));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));

    runWithDateFormatMockResult(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null));

    verify(identityManager).getOrCreateUserIdentity(OTHER_USER);
    verify(identityManager, never()).getOrCreateUserIdentity("ToDo");
    verify(identityManager, never()).getOrCreateUserIdentity("Done");
  }

  /**
   * A date is the start of that day in the user's time zone also on a day
   * whose midnight the zone skips: in America/Sao_Paulo on 2018-11-04 the
   * day starts at 01:00 local, 03:00 UTC. Mutant: midnight read as UTC.
   */
  @Test
  public void aSinceDateStartsWhenTheDayStartsInTheUsersZone() { // NOSONAR
    assertEquals(Date.from(Instant.parse("2018-11-04T03:00:00Z")),
                 tool.parseSince("2018-11-04", "changed_since", ZoneId.of("America/Sao_Paulo")));
  }

  /**
   * A change the task service rebuilt is mapped for the user: a default
   * status in the user's language, a due date and the change's date in the
   * user's format, its author as a user. Mutants: the status label skipped;
   * the due date read from the value instead of the date.
   */
  @Test
  public void aChangeIsMappedForTheUser() { // NOSONAR
    ResourceBundle bundle = new ListResourceBundle() {
      @Override
      protected Object[][] getContents() {
        return new Object[][] { { "tasks.status.ToDo", "To do" }, { "tasks.status.InProgress", "In progress" } };
      }
    };
    when(resourceBundleService.getResourceBundle(anyString(), any(Locale.class))).thenReturn(bundle);
    Date due = Date.from(Instant.parse("2026-10-20T00:00:00Z"));

    TaskChange status;
    TaskChange dueDate;
    TaskChange project;
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(() -> McpToolUtils.formatDate(any(Long.class)))
            .thenAnswer(invocation -> Instant.ofEpochMilli(invocation.<Long> getArgument(0)).toString());
      mocked.when(() -> McpToolUtils.formatDate(any(Date.class)))
            .thenAnswer(invocation -> invocation.<Date> getArgument(0).toInstant().toString());
      status = tool.toTaskChange(new TaskFieldChange(TaskFieldChange.FIELD_STATUS, "ToDo", "InProgress", null, 0L, USER));
      dueDate = tool.toTaskChange(new TaskFieldChange(TaskFieldChange.FIELD_DUE_DATE, null, null, due, 1000L, USER));
      project = tool.toTaskChange(new TaskFieldChange(TaskFieldChange.FIELD_PROJECT, null, "ToDo", null, 0L, USER));
    }

    assertEquals("To do", status.getOldValue());
    assertEquals("In progress", status.getNewValue());
    assertEquals("1970-01-01T00:00:00Z", status.getDate());
    assertEquals("2026-10-20T00:00:00Z", dueDate.getNewValue());
    assertEquals("1970-01-01T00:00:01Z", dueDate.getDate());
    assertEquals("a project name is never a status label", "ToDo", project.getNewValue());
  }

  /**
   * With a date and include_change_log, the change log returned is the one
   * the task service returns, mapped; without include_change_log, none.
   * Mutant: the change log not passed to the model.
   *
   * @throws Exception never
   */
  @Test
  public void theChangeLogListedWithADateIsTheServicesOne() throws Exception { // NOSONAR
    TaskDto task = mockTask();
    ChangeLogEntry entry = log(1, "edit_title", "", "2026-10-02T00:00:00Z");
    when(taskService.getTaskChanges(eq(task), any(), eq(true))).thenReturn(new TaskChanges(List.of(), List.of(entry)));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(null,
                                                                                  false,
                                                                                  true,
                                                                                  null,
                                                                                  null,
                                                                                  null,
                                                                                  null,
                                                                                  "2026-09-01",
                                                                                  null,
                                                                                  null,
                                                                                  null, null));

    List<TaskChangeLog> changeLogs = ((TaskWithChangesModel) result.getTasks().get(0)).getChangeLogs();
    assertEquals(1, changeLogs.size());
    assertEquals("edit_title", changeLogs.get(0).getActionId());
  }

  /**
   * overdue_only lists open tasks and completed_since completed ones: asked
   * together, on either tool, the call is refused instead of answering
   * nothing. Mutant: the check dropped.
   */
  @Test
  public void overdueOnlyAndCompletedSinceAreRefusedTogether() throws Exception { // NOSONAR
    assertThrows(IllegalArgumentException.class,
                 () -> tool.listTasks(null, null, false, true, null, null, null, null, "2026-10-01", null, null, null));
    assertThrows(IllegalArgumentException.class, () -> tool.listAssignedTasks(null, 5, true, null, "2026-10-01", null));
    verify(taskService, never()).findTasks(any(), anyInt(), anyInt());
  }

  /**
   * A date is the start of that day in the user's time zone, a date and
   * time with an offset is that instant, and one without is read in the
   * user's time zone; anything else is refused with a message naming the
   * argument and the expected forms. Mutants: a date read in UTC, an offset
   * ignored.
   */
  @Test
  public void sinceArgumentsAreDatesInTheUsersZone() { // NOSONAR
    assertNull(tool.parseSince("  ", "changed_since", USER_ZONE));
    assertEquals(Date.from(Instant.parse("2026-10-13T22:00:00Z")), tool.parseSince(" 2026-10-14 ", "changed_since", USER_ZONE));
    assertEquals(Date.from(Instant.parse("2026-10-14T07:00:00Z")),
                 tool.parseSince("2026-10-14T09:00:00+02:00", "changed_since", USER_ZONE));
    assertEquals(Date.from(Instant.parse("2026-10-14T09:00:00Z")), tool.parseSince("2026-10-14T09:00:00Z", "changed_since", USER_ZONE));
    assertEquals(Date.from(Instant.parse("2026-10-14T07:00:00Z")), tool.parseSince("2026-10-14T09:00", "changed_since", USER_ZONE));
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                                                  () -> tool.parseSince("last week", "completed_since", USER_ZONE));
    assertTrue(error.getMessage(), error.getMessage().startsWith("completed_since must be a date such as 2026-10-14"));
  }

  /**
   * @param id the entry identifier
   * @param actionName its action
   * @param target its target
   * @param date its ISO 8601 instant
   * @return a change log entry by the test user
   */
  private static ChangeLogEntry log(long id, String actionName, String target, String date) {
    ChangeLogEntry entry = new ChangeLogEntry();
    entry.setId(id);
    entry.setAuthor(USER);
    entry.setActionName(actionName);
    entry.setTarget(target);
    entry.setCreatedTime(Instant.parse(date).toEpochMilli());
    return entry;
  }

  /**
   * Asserts the changed_since condition of a query: a status, project, due
   * date or completion entry at or after the date, or an assignee entry at
   * or after it on a task created before it. Mutants it kills: another
   * action list; the creation date condition dropped, inverted or on
   * another date; the two branches joined by a conjunction.
   *
   * @param query a task query
   * @param since the expected date
   */
  private static void assertChangedSince(TaskQuery query, Date since) {
    AggregateCondition changed = conjunction(query).stream()
                                                   .filter(c -> c instanceof AggregateCondition aggregate
                                                                && AggregateCondition.OR.equals(aggregate.getType())
                                                                && aggregate.getConditions()
                                                                            .stream()
                                                                            .anyMatch(m -> m instanceof SingleCondition<?> single
                                                                                           && SingleCondition.LOGGED_SINCE.equals(single.getType())))
                                                   .map(AggregateCondition.class::cast)
                                                   .findFirst()
                                                   .orElseThrow();
    assertEquals(2, changed.getConditions().size());
    SingleCondition<?> others = (SingleCondition<?>) changed.getConditions().get(0);
    assertEquals(new LoggedSince(List.of("edit_status", "edit_project", "remove_project", "edit_duedate", "mark_done"), since),
                 others.getValue());
    AggregateCondition assignee = (AggregateCondition) changed.getConditions().get(1);
    assertEquals(AggregateCondition.AND, assignee.getType());
    assertEquals(new LoggedSince(List.of("assign", "unassign"), since), ((SingleCondition<?>) assignee.getConditions().get(0)).getValue());
    assertTrue(isSingle(assignee.getConditions().get(1), SingleCondition.LT, "createdTime", since));
  }

  /**
   * @param query a task query
   * @return the value of its change log condition, failing when it has none
   */
  private static LoggedSince findLoggedSince(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof SingleCondition<?> single && SingleCondition.LOGGED_SINCE.equals(single.getType())) {
        return (LoggedSince) single.getValue();
      }
    }
    fail("No change log condition in the query");
    return null;
  }

  /**
   * @param query an overdue query
   */
  private void assertMostOverdueFirst(TaskQuery query) {
    List<OrderBy> orderBy = query.getOrderBy();
    assertEquals(2, orderBy.size());
    assertEquals("dueDate", orderBy.get(0).getFieldName());
    assertTrue("oldest due date first", orderBy.get(0).isAscending());
    assertEquals("id", orderBy.get(1).getFieldName());
    assertTrue("ties broken on the identifier", orderBy.get(1).isAscending());
  }

  /**
   * @param dueDate an ISO 8601 instant
   * @return an open task due then
   */
  private TaskDto openTaskDue(String dueDate) {
    TaskDto task = mock(TaskDto.class);
    when(task.getDueDate()).thenReturn(Date.from(Instant.parse(dueDate)));
    return task;
  }

  /**
   * Days overdue are whole calendar days in the user's zone, also across the
   * Europe/Paris zone transitions of 2026-10-25 (a 25-hour day) and
   * 2027-03-28 (a 23-hour day): for a due day and a "today" on each side of
   * a transition, and a due date at the start, the end, and (in the overlap)
   * both occurrences of 02:30 of its day, the result equals a day-by-day
   * count. Mutant: a count of 24-hour periods between the instants.
   */
  @Test
  public void daysOverdueAcrossTimeZoneTransitionsMatchADayByDayCount() {// NOSONAR
    ZoneId paris = ZoneId.of("Europe/Paris");
    for (LocalDate transition : List.of(LocalDate.of(2026, 10, 25), LocalDate.of(2027, 3, 28))) {
      for (int dueOffset = -2; dueOffset <= 2; dueOffset++) {
        for (int todayOffset = -2; todayOffset <= 3; todayOffset++) {
          LocalDate dueDay = transition.plusDays(dueOffset);
          LocalDate today = transition.plusDays(todayOffset);
          List<Instant> dueInstants = new ArrayList<>();
          dueInstants.add(dueDay.atStartOfDay(paris).toInstant());
          dueInstants.add(dueDay.atTime(23, 59).atZone(paris).toInstant());
          ZonedDateTime half = dueDay.atTime(2, 30).atZone(paris);
          dueInstants.add(half.toInstant());
          dueInstants.add(half.withLaterOffsetAtOverlap().toInstant());
          for (Instant due : dueInstants) {
            TaskDto task = mock(TaskDto.class);
            when(task.getDueDate()).thenReturn(Date.from(due));
            long expected = 0;
            for (LocalDate day = dueDay; day.isBefore(today); day = day.plusDays(1)) {
              expected++;
            }
            Integer expectedDays = expected > 0 ? Integer.valueOf((int) expected) : null;
            assertEquals("due " + due + " today " + today, expectedDays, TaskMcpTool.getDaysOverdue(task, paris, today));
          }
        }
      }
    }
  }

  /**
   * The overdue cut-off is the start of today in the user's zone, also when
   * that zone skips midnight: in America/Sao_Paulo on 2018-11-04 the day
   * starts at 01:00 local (03:00 UTC), and the cut-off is that instant, not
   * midnight read as UTC or in the server's zone.
   */
  @Test
  public void theOverdueCutOffIsTheStartOfTheDayWhenTheZoneSkipsMidnight() throws Exception {// NOSONAR
    ZoneId saoPaulo = ZoneId.of("America/Sao_Paulo");
    LocalDate skippedMidnight = LocalDate.of(2018, 11, 4);
    TaskMcpTool skippingTool = new TestableTaskMcpTool() {
      @Override
      protected ZoneId getCurrentUserZone() {
        return saoPaulo;
      }

      @Override
      protected LocalDate getToday(ZoneId zone) {
        return skippedMidnight;
      }
    };
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findTasks(query.capture(), eq(0), eq(5))).thenReturn(Collections.emptyList());

    skippingTool.listAssignedTasks(null, 5, true, null, null, null);

    assertEquals(Date.from(Instant.parse("2018-11-04T03:00:00Z")), findDueDateBefore(query.getValue()));
    assertEquals(1, ZonedDateTime.ofInstant(findDueDateBefore(query.getValue()).toInstant(), saoPaulo).getHour());
  }

  /**
   * Without a filter, list_assigned_tasks lists the open tasks the user is
   * assigned to or a coworker of, the most recently active first, counted by
   * the same query. Mutants: the completed tasks kept; the count of another
   * query.
   *
   * @throws Exception never
   */
  @Test
  public void listAssignedTasksShouldReturnCollection() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(5))).thenReturn(Collections.singletonList(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, 5, null, null, null, null));

    assertTrue("only the tasks the user is assigned to or a coworker of", scopedToTheUser(query.getValue()));
    assertTrue("only the open tasks", selectsOpenTasksOnly(query.getValue()));
    verify(taskService).countTasks(query.getValue());
    verify(taskService, never()).getUncompletedTasks(anyString(), anyInt());
    assertEquals(1, result.getTasks().size());
    assertEquals(0, result.getUsedOffset());
    assertEquals(5, result.getUsedLimit());
    assertEquals(1L, result.getCount());
    assertFalse(result.isHasMore());
  }

  @Test
  public void listTasksShouldReturnEmptyCollectionWhenNoTasks() throws Exception {// NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(0);

    TaskCollectionModel result = tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null);

    assertEquals(0, result.getTasks().size());
    assertEquals(0, result.getUsedOffset());
    assertEquals(10, result.getUsedLimit());
    assertEquals(0L, result.getCount());
  }

  @Test
  public void getProjectIdByTaskIdShouldReturnProjectId() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);

    Long result = tool.getProjectIdByTaskId(TASK_ID);

    assertEquals(Long.valueOf(PROJECT_ID), result);
  }

  @Test
  public void updateTaskDescriptionShouldUpdateAndPersistTask() throws Exception {// NOSONAR
    TaskDto task = mockTask();

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);

    runWithDateFormatMockResult(() -> tool.updateTaskDescription(TASK_ID, "New description"));

    verify(task).setDescription("New description");
    verify(taskService).updateTask(task);
  }

  @Test
  public void createPersonalTaskShouldCreateTaskWithDefaultPriority() throws Exception {// NOSONAR
    TaskDto createdTask = mockTask();
    when(taskService.createTask(any(TaskDto.class))).thenReturn(createdTask);

    TaskModel result = runWithDateFormatMockResult(() -> tool.createPersonalTask(
                                                                                 "Title",
                                                                                 "Description",
                                                                                 USER,
                                                                                 Collections.singleton(OTHER_USER),
                                                                                 null,
                                                                                 null,
                                                                                 null,
                                                                                 null));

    assertEquals(TASK_ID, result.getId());
    verify(taskService).createTask(any(TaskDto.class));
  }

  /**
   * The storage files the task under the project of the status: a status id
   * the tool receives must belong to a project the user can view.
   */
  @Test
  public void createPersonalTaskShouldRefuseAStatusOfAProjectTheUserCannotView() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto status = mock(StatusDto.class);
    when(status.getProject()).thenReturn(project);
    when(project.getId()).thenReturn(PROJECT_ID);
    when(statusService.getStatus(20L)).thenReturn(status);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(false);

    try {
      tool.createPersonalTask("Title", "Description", USER, Collections.emptySet(), null, null, null, 20L);
      fail("A status of a project the user cannot view must be refused");
    } catch (IllegalAccessException e) {
      // Expected
    }
    verify(taskService, never()).createTask(any(TaskDto.class));
  }

  @Test
  public void createPersonalTaskShouldRefuseAnUnknownStatus() throws Exception {// NOSONAR
    when(statusService.getStatus(30L)).thenReturn(null);

    try {
      tool.createPersonalTask("Title", "Description", USER, Collections.emptySet(), null, null, null, 30L);
      fail("An unknown status must be refused");
    } catch (ObjectNotFoundException e) {
      // Expected
    }
    verify(taskService, never()).createTask(any(TaskDto.class));
  }

  @Test
  public void createPersonalTaskShouldCreateTaskInAStatusOfAViewableProject() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto status = mock(StatusDto.class);
    when(status.getProject()).thenReturn(project);
    when(project.getId()).thenReturn(PROJECT_ID);
    when(statusService.getStatus(10L)).thenReturn(status);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(userAcl.getUserIdentity(USER)).thenReturn(currentIdentity);
    TaskDto createdTask = mockTask();
    when(taskService.createTask(any(TaskDto.class))).thenReturn(createdTask);

    TaskModel result = runWithDateFormatMockResult(() -> tool.createPersonalTask("Title",
                                                                                 "Description",
                                                                                 USER,
                                                                                 Collections.emptySet(),
                                                                                 null,
                                                                                 null,
                                                                                 null,
                                                                                 10L));

    assertEquals(TASK_ID, result.getId());
    verify(taskService).createTask(argThat(task -> task.getStatus() == status));
  }

  @Test
  public void createPersonalTaskShouldAssignTheCallerWhenNoAssigneeIsGiven() throws Exception {// NOSONAR
    TaskDto createdTask = mockTask();
    when(taskService.createTask(any(TaskDto.class))).thenReturn(createdTask);
    ArgumentCaptor<TaskDto> created = ArgumentCaptor.forClass(TaskDto.class);

    runWithDateFormatMockResult(() -> tool.createPersonalTask("Title", "Description", null, null, null, null, null, null));
    runWithDateFormatMockResult(() -> tool.createPersonalTask("Title", "Description", " ", null, null, null, null, null));
    runWithDateFormatMockResult(() -> tool.createPersonalTask("Title", "Description", OTHER_USER, null, null, null, null, null));

    verify(taskService, times(3)).createTask(created.capture());
    assertEquals(USER, created.getAllValues().get(0).getAssignee());
    assertEquals(USER, created.getAllValues().get(1).getAssignee());
    assertEquals(OTHER_USER, created.getAllValues().get(2).getAssignee());
  }

  /**
   * A status id that isn't one of the project's statuses is refused and no
   * task is created: the space scope relies on it to keep a creation inside
   * the project.
   * Mutant: the status is taken by id from anywhere.
   */
  @Test
  public void createTaskInProjectRefusesAStatusOfAnotherProject() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto own = mock(StatusDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(own.getId()).thenReturn(3L);
    when(statusService.getStatuses(PROJECT_ID)).thenReturn(Collections.singletonList(own));

    assertThrows(IllegalStateException.class,
                 () -> tool.createTaskInProject(PROJECT_ID, "Task", null, null, null, null, null, null, 99L));
    verify(taskService, never()).createTask(any(TaskDto.class));
  }

  /**
   * A task created in a project gets as assignee and coworkers only people
   * the project is visible to, each judged on their own memberships, never on
   * the signed-in user's: an outsider as assignee, or as one of the
   * coworkers, refuses the creation, and nothing is created. A personal task
   * with no project accepts anyone, but one created in the project of a
   * given status is checked like a task created in that project. A login is
   * read without the leading {@code @} of a mention, as the assignment tools
   * read it.
   * Mutants: the check removed, made on the assignee only, made on the
   * signed-in user, or skipped for a personal task given a status.
   *
   * @throws Exception never
   */
  @Test
  public void createTaskInProjectPutsOnItOnlyPeopleTheProjectIsVisibleTo() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto status = mock(StatusDto.class);
    Identity outsider = mock(Identity.class);
    Identity member = mock(Identity.class);
    TaskDto createdTask = mockTask();
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(userAcl.getUserIdentity("outsider")).thenReturn(outsider);
    when(userAcl.getUserIdentity("member")).thenReturn(member);
    when(project.canView(outsider)).thenReturn(false);
    when(project.canView(member)).thenReturn(true);
    lenient().when(statusService.getDefaultStatus(PROJECT_ID)).thenReturn(status);
    when(taskService.createTask(any(TaskDto.class))).thenReturn(createdTask);

    assertThrows(IllegalAccessException.class,
                 () -> tool.createTaskInProject(PROJECT_ID, "Task", null, "outsider", null, null, null, null, null));
    assertThrows(IllegalAccessException.class,
                 () -> tool.createTaskInProject(PROJECT_ID, "Task", null, "member", Set.of("member", "outsider"),
                                                null, null, null, null));
    verify(taskService, never()).createTask(any(TaskDto.class));

    runWithDateFormatMockResult(() -> tool.createTaskInProject(PROJECT_ID, "Task", null, "member", Set.of("member"),
                                                               null, null, null, null));
    verify(taskService).createTask(any(TaskDto.class));

    runWithDateFormatMockResult(() -> tool.createPersonalTask("Task", null, "outsider", Set.of("outsider"),
                                                              null, null, null, null));
    verify(taskService, times(2)).createTask(any(TaskDto.class));

    StatusDto statusOfTheProject = mock(StatusDto.class);
    when(statusOfTheProject.getProject()).thenReturn(project);
    when(project.getId()).thenReturn(PROJECT_ID);
    when(statusService.getStatus(10L)).thenReturn(statusOfTheProject);
    assertThrows(IllegalAccessException.class,
                 () -> tool.createPersonalTask("Task", null, "member", Set.of("outsider"), null, null, null, 10L));
    assertThrows(IllegalAccessException.class,
                 () -> tool.createPersonalTask("Task", null, "@outsider", null, null, null, null, 10L));
    verify(taskService, times(2)).createTask(any(TaskDto.class));

    runWithDateFormatMockResult(() -> tool.createTaskInProject(PROJECT_ID, "Task", null, "@member", Set.of("@member"),
                                                               null, null, null, null));
    // the first creation above and this one: the @ is gone from both logins
    verify(taskService, times(2)).createTask(argThat(created -> "member".equals(created.getAssignee())
        && Set.of("member").equals(created.getCoworker())));
  }

  @Test
  public void createTaskInProjectShouldUseDefaultStatusAndCreateTask() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto status = mock(StatusDto.class);
    TaskDto createdTask = mockTask();

    Identity johnIdentity = mock(Identity.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(userAcl.getUserIdentity(USER)).thenReturn(currentIdentity);
    when(userAcl.getUserIdentity(OTHER_USER)).thenReturn(johnIdentity);
    when(project.canView(johnIdentity)).thenReturn(true);
    when(statusService.getDefaultStatus(PROJECT_ID)).thenReturn(status);
    when(taskService.createTask(any(TaskDto.class))).thenReturn(createdTask);

    TaskModel result = runWithDateFormatMockResult(() -> tool.createTaskInProject(
                                                                                  PROJECT_ID,
                                                                                  "Task",
                                                                                  "Description",
                                                                                  USER,
                                                                                  new HashSet<>(Collections.singletonList(OTHER_USER)),
                                                                                  null,
                                                                                  null,
                                                                                  Priority.HIGH,
                                                                                  null));

    assertEquals(TASK_ID, result.getId());
    verify(statusService).getDefaultStatus(PROJECT_ID);
    verify(taskService).createTask(any(TaskDto.class));
  }

  @Test
  public void createProjectInSpaceShouldCreateProjectAndStatuses() throws Exception {// NOSONAR
    Space space = mock(Space.class);
    ProjectDto project = mock(ProjectDto.class);

    when(spaceService.getSpaceById(10L)).thenReturn(space);
    when(space.getGroupId()).thenReturn("/spaces/test");
    when(userAcl.hasPermission(eq(SpaceAclPlugin.OBJECT_TYPE),
                               eq("10"),
                               eq(SpaceAclPlugin.REDACT_PERMISSION_TYPE),
                               eq(currentIdentity)))
                                                    .thenReturn(true);

    when(projectService.createProject(any(ProjectDto.class))).thenReturn(project);
    when(project.getId()).thenReturn(PROJECT_ID);
    when(project.getName()).thenReturn(PROJECT_NAME);
    when(project.getDescription()).thenReturn("");
    when(project.getManager()).thenReturn(Collections.emptySet());

    ProjectDto finalProject = project;
    ProjectModel result = runWithDateFormatMockResult(() -> tool.createProjectInSpace(10L, PROJECT_NAME, "", null));

    assertEquals(PROJECT_ID, result.getId());
    verify(projectService).createProject(any(ProjectDto.class));
    verify(statusService).createInitialStatuses(finalProject);
  }

  @Test
  public void addTaskCommentShouldCreateComment() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    CommentDto comment = mock(CommentDto.class);

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(commentService.addComment(task, USER, COMMENT)).thenReturn(comment);
    when(comment.getId()).thenReturn(22L);
    when(comment.getComment()).thenReturn(COMMENT);
    when(comment.getCreatedTime()).thenReturn(new Date());
    when(comment.getAuthor()).thenReturn(USER);

    runWithDateFormatMockResult(() -> tool.addTaskComment(TASK_ID, COMMENT));

    verify(commentService).addComment(task, USER, COMMENT);
  }

  /**
   * In a call limited to a space, a comment that mentions a user who isn't a
   * member of that space is refused, since each mention is notified; one
   * that mentions members only, or no one, is added, the space being loaded
   * once per comment, not per mention. A call that isn't limited may mention
   * anyone, as before. When the call's identity can't be read, a comment that
   * mentions someone is refused, and one that mentions no one is added.
   * Mutants: the mention check removed, applied to the first mention only,
   * or refusing a comment without mentions when the identity can't be read.
   *
   * @throws Exception never
   */
  @Test
  public void aCommentOfACallLimitedToASpaceMentionsItsMembersOnly() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    CommentDto comment = mock(CommentDto.class);
    Space space = mock(Space.class);
    String mentionsAMember = "Please check @mary";
    String mentionsAnOutsider = "Please check @mary and @" + OTHER_USER;
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(commentService.addComment(eq(task), eq(USER), anyString())).thenReturn(comment);
    when(comment.getId()).thenReturn(22L);
    when(comment.getComment()).thenReturn(COMMENT);
    when(comment.getCreatedTime()).thenReturn(new Date());
    when(comment.getAuthor()).thenReturn(USER);
    when(spaceService.getSpaceById(12L)).thenReturn(space);
    when(spaceService.isMember(space, "mary")).thenReturn(true);
    when(spaceService.isMember(space, OTHER_USER)).thenReturn(false);

    currentScopeSpaceId = 12L;
    assertThrows(IllegalAccessException.class, () -> tool.addTaskComment(TASK_ID, mentionsAnOutsider));
    verify(commentService, never()).addComment(any(), any(), any());
    runWithDateFormatMockResult(() -> tool.addTaskComment(TASK_ID, mentionsAMember));
    runWithDateFormatMockResult(() -> tool.addTaskComment(TASK_ID, COMMENT));
    verify(commentService).addComment(task, USER, mentionsAMember);
    verify(commentService).addComment(task, USER, COMMENT);

    currentScopeSpaceId = null;
    runWithDateFormatMockResult(() -> tool.addTaskComment(TASK_ID, mentionsAnOutsider));
    verify(commentService).addComment(task, USER, mentionsAnOutsider);
    verify(spaceService, times(2)).getSpaceById(12L);

    scopeUnreadable = true;
    assertThrows(IllegalAccessException.class, () -> tool.addTaskComment(TASK_ID, mentionsAMember));
    runWithDateFormatMockResult(() -> tool.addTaskComment(TASK_ID, COMMENT));
    verify(commentService, times(2)).addComment(task, USER, COMMENT);
  }

  @Test
  public void listTaskCommentsByIdShouldReturnFlattenedComments() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    CommentDto comment = mock(CommentDto.class);

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(commentService.getCommentsWithSubs(TASK_ID, 0, 10)).thenReturn(Collections.singletonList(comment));
    when(commentService.countCommentsWithSubs(TASK_ID)).thenReturn(1);

    when(comment.getId()).thenReturn(33L);
    when(comment.getComment()).thenReturn("comment");
    when(comment.getCreatedTime()).thenReturn(new Date());
    when(comment.getAuthor()).thenReturn(USER);
    when(comment.getSubComments()).thenReturn(Collections.emptyList());

    TaskCommentCollectionModel result = runWithDateFormatMockResult(() -> tool.listTaskCommentsById(TASK_ID, null, null));

    assertEquals(1, result.getComments().size());
    assertEquals(1L, result.getCount());
  }

  @Test
  public void removeProjectLabelFromTaskShouldDelegateToLabelService() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    LabelDto label = projectLabel(PROJECT_ID);

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(labelService.getLabel(99L)).thenReturn(label);

    tool.removeProjectLabelFromTask(TASK_ID, 99L);

    verify(labelService).removeTaskFromLabel(task, 99L);
  }

  @Test
  public void listProjectActivitySinceWhenNoTasksShouldReturnEmptyLists() throws Exception {// NOSONAR
    when(taskService.countTasks(any())).thenReturn(0);

    ProjectActivityModel result = tool.listProjectActivitySince(null, 7);

    assertEquals(0, result.getLastUpdatedTasks().size());
    assertEquals(0, result.getOtherUncompletedTasks().size());
  }

  @Test
  public void listProjectLabelsShouldReturnProjectLabels() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    LabelDto label = mock(LabelDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(labelService.findLabelsByProject(PROJECT_ID, currentIdentity, 0, 100))
                                                                               .thenReturn(Collections.singletonList(label));
    when(label.getId()).thenReturn(11L);
    when(label.getName()).thenReturn("Bug");

    List<ProjectLabel> result = tool.listProjectLabels(PROJECT_ID);

    assertEquals(1, result.size());
    assertEquals(11L, result.get(0).getId());
    assertEquals("Bug", result.get(0).getName());
  }

  @Test(expected = IllegalAccessException.class)
  public void listProjectLabelsWhenProjectNotViewableShouldThrowException() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(false);

    tool.listProjectLabels(PROJECT_ID);
  }

  @Test
  public void createProjectLabelWhenLabelDoesNotExistShouldCreateLabel() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    LabelDto createdLabel = mock(LabelDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(true);
    when(labelService.findLabelsByProject(PROJECT_ID, currentIdentity, 0, 100))
                                                                               .thenReturn(Collections.emptyList());
    when(labelService.createLabel(any(LabelDto.class))).thenReturn(createdLabel);
    when(createdLabel.getId()).thenReturn(77L);
    when(createdLabel.getName()).thenReturn(LABEL_NAME);

    ProjectLabel result = tool.createProjectLabel(PROJECT_ID, LABEL_NAME);

    assertEquals(77L, result.getId());
    assertEquals(LABEL_NAME, result.getName());
    verify(labelService).createLabel(any(LabelDto.class));
  }

  @Test(expected = IllegalAccessException.class)
  public void createProjectLabelWhenProjectNotEditableShouldThrowException() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canEdit(currentIdentity)).thenReturn(false);

    tool.createProjectLabel(PROJECT_ID, "Bug");
  }

  /**
   * list_projects returns compact items, the project's identifier, name,
   * space and link, counted by the project count: the description, statuses
   * and labels are neither read nor serialized. The count is that of a
   * project query of the memberships in the order of the list, which counts
   * each project once, not that of the named query that counts a project once
   * per manager and participant. Mutants: the full project model listed; the
   * statuses or labels read for each project; the count read from the named
   * query.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsShouldUseProjectCountAndReturnCollection() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);

    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(10)))
                                                                            .thenReturn(Collections.singletonList(project));
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(1);

    when(project.getId()).thenReturn(PROJECT_ID);
    when(project.getName()).thenReturn(PROJECT_NAME);
    lenient().when(project.getDescription()).thenReturn("<p>A long description</p>");
    when(project.getManager()).thenReturn(Collections.emptySet());

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, null));

    assertEquals(1, result.getProjects().size());
    assertEquals(0, result.getUsedOffset());
    assertEquals(10, result.getUsedLimit());
    assertEquals(1L, result.getCount());
    assertFalse(result.isHasMore());
    assertNull(result.getNextOffset());
    ArgumentCaptor<ProjectQuery> counted = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).countProjects(counted.capture());
    verify(projectService, never()).countProjects(any(), any());
    assertNameQuery(counted.getValue(), null);
    ProjectListItemModel item = result.getProjects().get(0);
    assertEquals(PROJECT_ID, item.getId());
    assertEquals(PROJECT_NAME, item.getName());
    assertTrue(item.getUrl().endsWith("/task-link"));
    verify(statusService, never()).getStatuses(anyLong());
    verify(labelService, never()).findLabelsByProject(anyLong(), any(), anyInt(), anyInt());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(result));
    assertEquals(Set.of("project_id", "name", "url"), fieldNames(json.path("projects").path(0)));
    assertFalse("no next_offset on the last page", json.has("next_offset"));
    assertFalse(json.path("has_more").asBoolean());
  }

  /**
   * get_project_by_id keeps the full project: its description, statuses and
   * labels. Mutant: the list item returned.
   *
   * @throws Exception never
   */
  @Test
  public void getProjectByIdReturnsTheFullProject() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(project.getId()).thenReturn(PROJECT_ID);
    when(project.getName()).thenReturn(PROJECT_NAME);
    when(project.getDescription()).thenReturn("A description");
    when(project.getManager()).thenReturn(Collections.emptySet());
    StatusDto status = mock(StatusDto.class);
    when(status.getId()).thenReturn(8L);
    when(status.getName()).thenReturn("ToDo");
    when(statusService.getStatuses(PROJECT_ID)).thenReturn(List.of(status));
    LabelDto label = mock(LabelDto.class);
    when(label.getId()).thenReturn(3L);
    when(label.getName()).thenReturn(LABEL_NAME);
    when(labelService.findLabelsByProject(PROJECT_ID, currentIdentity, 0, 100)).thenReturn(List.of(label));

    ProjectModel model = runWithDateFormatMockResult(() -> tool.getProjectById(PROJECT_ID));

    assertEquals(1, model.getAllowedStatusList().size());
    assertEquals(LABEL_NAME, model.getAllowedLabelList().get(0).getName());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(model));
    assertTrue(json.has("allowed_status_list"));
    assertTrue(json.has("allowed_label_list"));
  }

  /**
   * name reaches the project list and the project count with the same
   * keyword, trimmed, and a blank name is no keyword; in a run limited to a
   * space, the scan of the space's projects reads the keyword too. Mutants:
   * the keyword dropped from the list, from the count or from the scan; the
   * list and the count given different keywords.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsByNamePassesTheSameKeywordToTheListAndTheCount() throws Exception { // NOSONAR
    when(projectService.findProjects(any(), any(), any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(0);

    runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, "  Q3 Roadmap  "));
    verify(projectService).findProjects(any(), eq("Q3 Roadmap"), any(), eq(0), eq(10));
    ArgumentCaptor<ProjectQuery> counted = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService, times(2)).countProjects(counted.capture());
    assertAllWordsQuery(counted.getAllValues().get(0), "%q3%", "%roadmap%");
    assertNameQuery(counted.getAllValues().get(1), AggregateCondition.OR, "%q3%", "%roadmap%");

    runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, " "));
    verify(projectService).findProjects(any(), eq(null), any(), eq(0), eq(10));
    verify(projectService, times(3)).countProjects(counted.capture());
    assertNameQuery(counted.getAllValues().get(counted.getAllValues().size() - 1), null);
    verify(projectService, never()).countProjects(any(), any());

    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, "Q3 Roadmap"));
    verify(projectService).findProjects(any(), eq("Q3 Roadmap"), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK));
  }

  /**
   * A one-word name selects the same projects whether all or any of its words
   * are asked for: when none holds it, the empty result is returned without
   * reading the list or the count by any word, in a plain run as in a run
   * limited to a space. Mutant: the fallback run for one word.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsByOneWordNameNeverFallsBack() throws Exception { // NOSONAR
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(0);

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, "  Roadmap "));
    assertTrue(result.getProjects().isEmpty());
    assertEquals(0L, result.getCount());
    assertFalse(result.isHasMore());
    verify(projectService, times(1)).countProjects(any(ProjectQuery.class));

    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    ProjectCollectionModel ofSpace = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, "Roadmap"));
    assertTrue(ofSpace.getProjects().isEmpty());
    assertEquals(0L, ofSpace.getCount());
    verify(projectService, times(2)).countProjects(any(ProjectQuery.class));
    verify(projectService, never()).findProjects(any(), any(), any(), anyInt(), anyInt());
    verify(projectService, never()).findProjects(any(ProjectQuery.class), anyInt(), anyInt());
  }

  /**
   * When a project's name holds every word of name, the list and the count
   * come from one query: the memberships of the plain list, every word as a
   * case-insensitive part of the name, the most recently modified first; the
   * list by any one word is never read. Mutants: the any-word list or count
   * read although a project holds every word; the words joined by OR; the
   * memberships or the order dropped from the query.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsByNameListsOnlyTheProjectsHoldingAllItsWords() throws Exception { // NOSONAR
    ProjectDto board = listedProject(1L, null);
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(1);
    when(projectService.findProjects(any(ProjectQuery.class), eq(2), eq(5))).thenReturn(List.of(board));

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, 2, 5, " Big Board 50 "));

    assertEquals(List.of(1L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(1L, result.getCount());
    verify(projectService, never()).findProjects(any(), any(), any(), anyInt(), anyInt());
    verify(projectService, never()).countProjects(any(), any());
    ArgumentCaptor<ProjectQuery> listed = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).findProjects(listed.capture(), eq(2), eq(5));
    ArgumentCaptor<ProjectQuery> counted = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).countProjects(counted.capture());
    for (ProjectQuery query : List.of(listed.getValue(), counted.getValue())) {
      assertAllWordsQuery(query, "%big%", "%board%", "%50%");
    }
  }

  /**
   * When no project's name holds every word of name, the list and the count
   * are those of any one word, with the same keyword, offset and limit as
   * before. Mutants: no fallback; the fallback given another keyword, offset
   * or limit.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsByNameFallsBackToAnyWordWhenNoneHoldsThemAll() throws Exception { // NOSONAR
    ProjectDto onboarding = listedProject(4L, null);
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(0, 3);
    when(projectService.findProjects(any(), eq("Big Board 50"), any(), eq(2), eq(5))).thenReturn(List.of(onboarding));

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, 2, 5, " Big Board 50 "));

    assertEquals(List.of(4L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(3L, result.getCount());
    verify(projectService, never()).findProjects(any(ProjectQuery.class), anyInt(), anyInt());
  }

  /**
   * In a run limited to a space, the scan reads the projects holding every
   * word of name when the space has some, and keeps those of the space with
   * the exact has_more; the scan by any one word is never read. Mutants: the
   * any-word scan read although a project of the space holds every word;
   * has_more lost on that path.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsOfASpaceByNameScansTheProjectsHoldingAllItsWords() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    List<ProjectDto> projects = List.of(listedProject(1L, 10L), listedProject(2L, 11L), listedProject(3L, 10L));
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(3);
    when(projectService.findProjects(any(ProjectQuery.class), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK))).thenReturn(projects);

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 0, 1, "Big Board"));

    assertEquals(List.of(1L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertTrue(result.isHasMore());
    assertEquals(Integer.valueOf(1), result.getNextOffset());
    verify(projectService, never()).findProjects(any(), any(), any(), anyInt(), anyInt());
    ArgumentCaptor<ProjectQuery> scanned = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).findProjects(scanned.capture(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK));
    assertAllWordsQuery(scanned.getValue(), "%big%", "%board%");
  }

  /**
   * In a run limited to a space, the scan reads the projects holding any one
   * word of name when no project holds them all, and also when those holding
   * them all are none of the space's. Mutants: no fallback; the fallback
   * only on a zero count, so that a space whose projects hold one word lists
   * nothing.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsOfASpaceByNameFallsBackToAnyWordWhenTheSpaceHasNoneHoldingThemAll() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    ProjectDto onboarding = listedProject(4L, 10L);
    ProjectDto ofOtherSpace = listedProject(2L, 11L);
    when(projectService.findProjects(any(), eq("Big Board"), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK)))
                                                                                                           .thenReturn(List.of(onboarding));
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(0, 1);
    when(projectService.findProjects(any(ProjectQuery.class),
                                     eq(0),
                                     eq(TaskMcpTool.SPACE_SCAN_CHUNK))).thenReturn(List.of(ofOtherSpace));

    ProjectCollectionModel none = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, "Big Board"));
    assertEquals(List.of(4L), none.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(1L, none.getCount());
    verify(projectService, never()).findProjects(any(ProjectQuery.class), anyInt(), anyInt());

    ProjectCollectionModel otherSpace = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, "Big Board"));
    assertEquals(List.of(4L), otherSpace.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(1L, otherSpace.getCount());
    verify(projectService, times(2)).findProjects(any(), eq("Big Board"), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK));
  }

  /**
   * @param query a query of the projects holding all the words of a name
   * @param likes the patterns each word must match, lower-cased
   */
  private void assertAllWordsQuery(ProjectQuery query, String... likes) {
    assertNameQuery(query, AggregateCondition.AND, likes);
  }

  /**
   * @param query a query of the projects of the memberships, by name words
   *          or not, the most recently modified first
   * @param wordsType how the name words are combined, {@link AggregateCondition#AND}
   *          or {@link AggregateCondition#OR}; null when the query has no name
   *          condition
   * @param likes the patterns each word must match, lower-cased
   */
  private void assertNameQuery(ProjectQuery query, String wordsType, String... likes) {
    assertEquals(1, query.getOrderBy().size());
    assertEquals("lastModifiedDate", query.getOrderBy().get(0).getFieldName());
    assertFalse(query.getOrderBy().get(0).isAscending());
    List<Condition> conditions = ((AggregateCondition) query.getCondition()).getConditions();
    assertEquals(wordsType == null ? 1 : 2, conditions.size());
    AggregateCondition membership = (AggregateCondition) conditions.get(0);
    assertEquals(AggregateCondition.OR, membership.getType());
    assertEquals(List.of("manager", "participator"),
                 membership.getConditions().stream().map(c -> ((SingleCondition<?>) c).getField()).toList());
    Object roles = ((SingleCondition<?>) membership.getConditions().get(0)).getValue();
    assertFalse(((List<?>) roles).isEmpty());
    assertEquals(roles, ((SingleCondition<?>) membership.getConditions().get(1)).getValue());
    if (wordsType == null) {
      return;
    }
    AggregateCondition words = (AggregateCondition) conditions.get(1);
    assertEquals(wordsType, words.getType());
    assertEquals(List.of(likes), words.getConditions().stream().map(c -> ((SingleCondition<?>) c).getValue()).toList());
    words.getConditions()
         .forEach(c -> assertEquals(SingleCondition.LIKE, ((SingleCondition<?>) c).getType()));
  }

  /**
   * In a run limited to a space, has_more is exact although the count is only
   * what the scan kept: a full page with one more project kept says more
   * remain, from the next offset, and the page that reaches the end of the
   * scan says none remain. Mutant: has_more read from the page being full.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsOfASpaceInALimitedRunSaysWhetherMoreRemain() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    List<ProjectDto> projects = new ArrayList<>();
    for (long id = 1; id <= 4; id++) {
      projects.add(listedProject(id, 10L));
    }
    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK))).thenReturn(projects);

    ProjectCollectionModel first = runWithDateFormatMockResult(() -> tool.listProjects(10L, 0, 2, null));
    assertTrue(first.isHasMore());
    assertEquals(Integer.valueOf(2), first.getNextOffset());

    ProjectCollectionModel last = runWithDateFormatMockResult(() -> tool.listProjects(10L, 2, 2, null));
    assertEquals(List.of(3L, 4L), last.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertFalse(last.isHasMore());
    assertNull(last.getNextOffset());
  }

  /**
   * list_tasks returns compact items: the task's identifier, title, link,
   * completion, project, status, labels, assignee as a username and a display
   * name, due date, creation date (the full task's), days overdue, priority,
   * comments count with the replies, and the date and author of the latest
   * comment; no description, no coworkers, and no user profile read for the
   * coworkers. With include_change_log, the full task, whose comments count
   * holds the replies too. Mutants: the full model listed; the coworkers
   * looked up; the assignee's display name not read; the creation date
   * dropped; the latest comment dropped; the full task counting the top-level
   * comments only.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksReturnsCompactItemsAndTheFullTaskWithTheChangeLog() throws Exception { // NOSONAR
    TaskDto task = mockTaskWithProject();
    when(task.getAssignee()).thenReturn(OTHER_USER);
    lenient().when(task.getCoworker()).thenReturn(Set.of("mary"));
    lenient().when(task.getDescription()).thenReturn("<p>A long description</p>");
    when(task.getDueDate()).thenReturn(Date.from(Instant.parse("2026-10-01T10:00:00Z")));
    when(task.getPriority()).thenReturn(Priority.HIGH);
    when(commentService.countComments(TASK_ID)).thenReturn(1);
    when(commentService.countCommentsWithSubs(TASK_ID)).thenReturn(3);
    when(commentService.getCommentsSummaries(List.of(TASK_ID))).thenReturn(Map.of(TASK_ID,
                                                                                   new CommentsSummaryDto(TASK_ID,
                                                                                                          3,
                                                                                                          new Date(),
                                                                                                          "paul",
                                                                                                          55L)));
    org.exoplatform.social.core.identity.model.Identity paul = mock(org.exoplatform.social.core.identity.model.Identity.class);
    Profile paulProfile = mock(Profile.class);
    when(paul.getProfile()).thenReturn(paulProfile);
    when(paulProfile.getFullName()).thenReturn("Paul Martin");
    when(identityManager.getOrCreateUserIdentity("paul")).thenReturn(paul);
    org.exoplatform.social.core.identity.model.Identity john = mock(org.exoplatform.social.core.identity.model.Identity.class);
    Profile johnProfile = mock(Profile.class);
    when(john.getProfile()).thenReturn(johnProfile);
    when(johnProfile.getFullName()).thenReturn("John Smith");
    when(identityManager.getOrCreateUserIdentity(OTHER_USER)).thenReturn(john);
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null));

    TaskListItemModel item = (TaskListItemModel) result.getTasks().get(0);
    assertEquals(TASK_ID, item.getId());
    assertEquals("Task " + TASK_ID, item.getTitle());
    assertEquals(Long.valueOf(PROJECT_ID), item.getProjectId());
    assertEquals(PROJECT_NAME, item.getProjectName());
    assertEquals(8L, item.getStatus().getId());
    assertEquals(OTHER_USER, item.getAssignee().getUsername());
    assertEquals("John Smith", item.getAssignee().getDisplayName());
    assertEquals("2024-01-01T00:00:00Z", item.getDueDate());
    assertEquals(Integer.valueOf(7), item.getDaysOverdue());
    assertEquals(Priority.HIGH, item.getPriority());
    assertEquals(3, item.getCommentsCount());
    assertEquals("2024-01-01T00:00:00Z", item.getLastCommentDate());
    assertEquals("paul", item.getLastCommentAuthor().getUsername());
    assertEquals("Paul Martin", item.getLastCommentAuthor().getDisplayName());
    assertNull("no changes without changed_since", item.getChanges());
    verify(identityManager, never()).getOrCreateUserIdentity("mary");
    verify(taskService, never()).getTaskLogs(anyLong(), anyInt(), anyInt());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(item));
    assertEquals(Set.of("task_id",
                        "title",
                        "link",
                        "completed",
                        "project_id",
                        "project_name",
                        "status",
                        "assignee",
                        "due_date",
                        "created_date",
                        "days_overdue",
                        "priority",
                        "comments_count",
                        "last_comment_date",
                        "last_comment_author"),
                 fieldNames(json));
    assertEquals(Set.of("username", "display_name"), fieldNames(json.path("assignee")));
    assertEquals(Set.of("username", "display_name"), fieldNames(json.path("last_comment_author")));

    TaskCollectionModel full = runWithDateFormatMockResult(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null));

    TaskModel fullTask = (TaskModel) full.getTasks().get(0);
    assertTrue(fullTask instanceof TaskWithChangeLogModel);
    assertEquals("A long description", fullTask.getDescription().replaceAll("<[^>]+>", "").trim());
    assertEquals(1, fullTask.getCoworkers().size());
    assertEquals("the full task counts the replies", 3, fullTask.getCommentsCount());
    assertNotNull(item.getCreatedDate());
    assertEquals("the same creation date as the full task", fullTask.getCreatedDate(), item.getCreatedDate());
  }

  /**
   * The full task of a listing with include_change_log has one "commented"
   * entry per comment, each reply included, as its comments count does.
   * Mutant: the top-level comments read without their replies.
   *
   * @throws Exception never
   */
  @Test
  public void theChangeLogOfAFullTaskHoldsTheReplies() throws Exception { // NOSONAR
    TaskDto task = mockTaskWithProject();
    CommentDto comment = new CommentDto();
    comment.setAuthor(OTHER_USER);
    comment.setCreatedTime(Date.from(Instant.parse("2026-10-01T10:00:00Z")));
    CommentDto reply = new CommentDto();
    reply.setAuthor(OTHER_USER);
    reply.setCreatedTime(Date.from(Instant.parse("2026-10-02T10:00:00Z")));
    comment.setSubComments(List.of(reply));
    when(commentService.countComments(TASK_ID)).thenReturn(1);
    when(commentService.countCommentsWithSubs(TASK_ID)).thenReturn(2);
    when(commentService.getCommentsWithSubs(TASK_ID, 0, 1)).thenReturn(List.of(comment));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel full = runWithDateFormatMockResult(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null));

    TaskWithChangeLogModel fullTask = (TaskWithChangeLogModel) full.getTasks().get(0);
    assertEquals(2, fullTask.getCommentsCount());
    assertEquals(2, fullTask.getChangeLogs().stream().filter(l -> "commented".equals(l.getActionId())).count());
  }

  /**
   * The comments of a page of 100 tasks are read in one call, whatever the
   * number of tasks, never task by task; a commented task carries the date of
   * its latest comment, not its own creation or due date, and a task without
   * comments carries neither a latest comment date nor its author. Mutants:
   * the comments counted task by task; the latest comment's date swapped for
   * the task's creation or due date.
   *
   * @throws Exception never
   */
  @Test
  public void aPageOfTasksReadsItsCommentsInOneQuery() throws Exception { // NOSONAR
    List<TaskDto> tasks = new ArrayList<>();
    for (long id = 1; id <= 100; id++) {
      tasks.add(mockTaskWithProject(id, new Date()));
    }
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(100))).thenReturn(tasks);
    when(taskService.countTasks(any())).thenReturn(100);
    List<Long> taskIds = tasks.stream().map(TaskDto::getId).toList();
    when(commentService.getCommentsSummaries(taskIds)).thenReturn(Map.of(1L,
                                                                         new CommentsSummaryDto(1L,
                                                                                                16,
                                                                                                Date.from(Instant.parse("2026-09-01T07:00:00Z")),
                                                                                                OTHER_USER,
                                                                                                9L)));
    when(tasks.get(0).getDueDate()).thenReturn(Date.from(Instant.parse("2026-12-01T00:00:00Z")));

    TaskCollectionModel result;
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getUserTimeZone).thenReturn(TimeZone.getTimeZone("UTC"));
      mocked.when(() -> McpToolUtils.formatDate(any(Date.class)))
            .thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toInstant().toString());
      result = tool.listTasks(null, false, false, null, 0, 100, null, null, null, null, null, null);
    }

    assertEquals(100, result.getTasks().size());
    verify(commentService).getCommentsSummaries(taskIds);
    verify(commentService, never()).countComments(anyLong());
    verify(commentService, never()).countCommentsWithSubs(anyLong());
    TaskListItemModel commented = (TaskListItemModel) result.getTasks().get(0);
    assertEquals(16, commented.getCommentsCount());
    assertEquals("2026-09-01T07:00:00Z", commented.getLastCommentDate());
    assertEquals(OTHER_USER, commented.getLastCommentAuthor().getUsername());
    JsonNode uncommented = JSON.readTree(JSON.writeValueAsString(result.getTasks().get(1)));
    assertFalse(uncommented.has("last_comment_date"));
    assertFalse(uncommented.has("last_comment_author"));
    assertEquals(0, uncommented.path("comments_count").asInt());
  }

  /**
   * A compact item of list_tasks and of list_assigned_tasks carries the
   * task's creation date, the one the full task carries, not its due date, so
   * that the tasks created since a date are found. Mutants: the creation date
   * dropped; the due date given instead.
   *
   * @throws Exception never
   */
  @Test
  public void aCompactItemCarriesTheCreationDateOfTheFullTask() throws Exception { // NOSONAR
    Date createdTime = Date.from(Instant.parse("2026-10-12T08:30:00Z"));
    TaskDto task = mockTaskWithProject(TASK_ID, createdTime);
    when(task.getDueDate()).thenReturn(Date.from(Instant.parse("2026-10-30T10:00:00Z")));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getUserTimeZone).thenReturn(TimeZone.getTimeZone("UTC"));
      mocked.when(() -> McpToolUtils.formatDate(any(Date.class)))
            .thenAnswer(invocation -> ((Date) invocation.getArgument(0)).toInstant().toString());

      TaskListItemModel item = (TaskListItemModel) tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null)
                                                       .getTasks()
                                                       .get(0);
      TaskModel fullTask = (TaskModel) tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null)
                                           .getTasks()
                                           .get(0);
      TaskListItemModel assigned = (TaskListItemModel) tool.listAssignedTasks(null, null, null, null, null, null).getTasks().get(0);

      assertEquals("2026-10-12T08:30:00Z", item.getCreatedDate());
      assertEquals(fullTask.getCreatedDate(), item.getCreatedDate());
      assertEquals("2026-10-12T08:30:00Z", assigned.getCreatedDate());
      assertEquals("2026-10-12T08:30:00Z", JSON.readTree(JSON.writeValueAsString(item)).path("created_date").asText());
    }
  }

  /**
   * The description of an opportunity card, as the editor stores it: a
   * mention, entities and one qualification line per paragraph, list item or
   * line break.
   */
  private static final String QUALIFICATION_HTML  = "<p>Contact: <a class=\"user-suggester\" href=\"/portal/dw/profile/john\">@john</a>"
      + " &amp; Co&nbsp;Ltd</p><ul><li>Users: 500</li><li>Competition: <a href=\"https://example.org\">none</a></li></ul>"
      + "<p>Planning: Q1<br>Budget: 50 k&euro;</p>";

  /**
   * The plain text of {@link #QUALIFICATION_HTML}.
   */
  private static final String QUALIFICATION_TEXT  = "Contact: @john & Co Ltd\nUsers: 500\nCompetition: none\nPlanning: Q1\nBudget: 50 k\u20ac";

  /**
   * The keys of a compact item of {@link #mockTaskWithProject(long, Date)}
   * listed without include_description.
   */
  private static final Set<String> DEFAULT_ITEM_KEYS = Set.of("task_id",
                                                              "title",
                                                              "link",
                                                              "completed",
                                                              "project_id",
                                                              "project_name",
                                                              "status",
                                                              "created_date",
                                                              "comments_count");

  /**
   * Without include_description, null or false, a compact item of list_tasks
   * and of list_assigned_tasks keeps its keys: neither description nor
   * description_truncated, and the description is not even read. Mutants:
   * the description always added; a false truncation flag serialised.
   *
   * @throws Exception never
   */
  @Test
  public void aCompactItemHasNoDescriptionWithoutIncludeDescription() throws Exception { // NOSONAR
    TaskDto task = mockTaskWithProject(TASK_ID, Date.from(Instant.parse("2026-10-12T08:30:00Z")));
    lenient().when(task.getDescription()).thenReturn(QUALIFICATION_HTML);
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    for (Boolean includeDescription : new Boolean[] { null, false }) {
      TaskListItemModel listed = (TaskListItemModel) runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, includeDescription)).getTasks()
                                                                                                                                                                                      .get(0);
      TaskListItemModel assigned = (TaskListItemModel) runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, null, null, null, null, includeDescription)).getTasks()
                                                                                                                                                                  .get(0);

      assertEquals(DEFAULT_ITEM_KEYS, fieldNames(JSON.readTree(JSON.writeValueAsString(listed))));
      assertEquals(DEFAULT_ITEM_KEYS, fieldNames(JSON.readTree(JSON.writeValueAsString(assigned))));
    }
    verify(task, never()).getDescription();
  }

  /**
   * With include_description, a compact item of list_tasks and of
   * list_assigned_tasks carries its description as plain text: the tags and
   * entities removed, a mention and a link kept as their text, unresolved,
   * one line per block. A short one comes whole, without
   * description_truncated; a long one is cut at 500 characters at most, at a
   * word boundary, with description_truncated true. Mutants: the HTML kept;
   * no cut; a cut inside a word; the flag missing or always set.
   *
   * @throws Exception never
   */
  @Test
  public void aCompactItemCarriesItsDescriptionAsCutPlainTextOnRequest() throws Exception { // NOSONAR
    String longText = "lorem ipsum dolor sit amet consectetur ".repeat(20).strip();
    TaskDto shortTask = mockTaskWithProject(TASK_ID, Date.from(Instant.parse("2026-10-12T08:30:00Z")));
    when(shortTask.getDescription()).thenReturn(QUALIFICATION_HTML);
    TaskDto longTask = mockTaskWithProject(TASK_ID + 1, Date.from(Instant.parse("2026-10-12T08:30:00Z")));
    when(longTask.getDescription()).thenReturn("<p>" + longText + "</p>");
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(shortTask, longTask));
    when(taskService.countTasks(any())).thenReturn(2);

    List<?> listed = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, true)).getTasks();
    List<?> assigned = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, null, null, null, null, true)).getTasks();

    for (List<?> items : List.of(listed, assigned)) {
      JsonNode shortItem = JSON.readTree(JSON.writeValueAsString(items.get(0)));
      Set<String> withDescription = new HashSet<>(DEFAULT_ITEM_KEYS);
      withDescription.add("description");
      assertEquals(withDescription, fieldNames(shortItem));
      assertEquals(QUALIFICATION_TEXT, shortItem.path("description").asString());

      JsonNode longItem = JSON.readTree(JSON.writeValueAsString(items.get(1)));
      withDescription.add("description_truncated");
      assertEquals(withDescription, fieldNames(longItem));
      assertTrue(longItem.path("description_truncated").asBoolean());
      String cut = longItem.path("description").asString();
      assertTrue(cut.length() <= 500);
      assertTrue(cut.length() > 450);
      assertTrue(longText.startsWith(cut));
      assertEquals("cut at a word boundary", ' ', longText.charAt(cut.length()));
    }
  }

  /**
   * A limit above the most a list returns is cut to it, on every list tool,
   * and a limit not positive is the default one. Mutants: the comparison
   * inverted or dropped; a negative limit passed on.
   *
   * @throws Exception never
   */
  @Test
  public void aListLimitAboveTheMostIsCutToIt() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(projectService.findProjects(any(), any(), any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

    TaskCollectionModel tasks = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, 101, null, null, null, null, null, null));
    verify(taskService).findLastUpdatedTasks(any(), eq(0), eq(100));
    assertEquals(100, tasks.getUsedLimit());

    runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, 101, null, null, null, null));
    verify(taskService, times(2)).findLastUpdatedTasks(any(), eq(0), eq(100));

    ProjectCollectionModel projects = runWithDateFormatMockResult(() -> tool.listProjects(null, null, 101, null));
    verify(projectService).findProjects(any(), any(), any(), eq(0), eq(100));
    assertEquals(100, projects.getUsedLimit());

    runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, 100, null, null, null, null, null, null));
    verify(taskService, times(3)).findLastUpdatedTasks(any(), eq(0), eq(100));

    assertEquals(100, TaskMcpTool.getLimit(101));
    assertEquals(100, TaskMcpTool.getLimit(100));
    assertEquals(99, TaskMcpTool.getLimit(99));
    assertEquals(10, TaskMcpTool.getLimit(-2));
    assertEquals(10, TaskMcpTool.getLimit(null));
    assertEquals(0, TaskMcpTool.getOffset(-4));
  }

  /**
   * A full page with more tasks after it says has_more, with the offset of
   * the next page; the last page, and an empty one past the end, say none
   * remain and carry no next_offset. Mutants: has_more always true or
   * false; next_offset left on the last page; the page size ignored.
   *
   * @throws Exception never
   */
  @Test
  public void aPageSaysWhetherMoreTasksRemainAndFromWhichOffset() throws Exception { // NOSONAR
    List<TaskDto> ten = new ArrayList<>();
    for (long id = 1; id <= 10; id++) {
      ten.add(mockTaskWithProject(id, new Date()));
    }
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(ten);
    when(taskService.findLastUpdatedTasks(any(), eq(20), eq(10))).thenReturn(ten.subList(0, 5));
    when(taskService.findLastUpdatedTasks(any(), eq(30), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(25);

    TaskCollectionModel first = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, 0, 10, null, null, null, null, null, null));
    assertTrue(first.isHasMore());
    assertEquals(Integer.valueOf(10), first.getNextOffset());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(first));
    assertTrue(json.path("has_more").asBoolean());
    assertEquals(10, json.path("next_offset").asInt());
    assertEquals(25, json.path("count").asInt());
    assertEquals(10, json.path("used_limit").asInt());

    TaskCollectionModel last = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, 20, 10, null, null, null, null, null, null));
    assertFalse(last.isHasMore());
    assertNull(last.getNextOffset());
    assertFalse(JSON.readTree(JSON.writeValueAsString(last)).has("next_offset"));

    TaskCollectionModel past = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, 30, 10, null, null, null, null, null, null));
    assertFalse(past.isHasMore());
    assertNull(past.getNextOffset());

    assertEquals(Integer.valueOf(10), TaskMcpTool.getNextOffset(10, 0, 11));
    assertNull(TaskMcpTool.getNextOffset(10, 0, 10));
    assertNull(TaskMcpTool.getNextOffset(0, 5, 10));
  }

  /**
   * list_assigned_tasks pages with its offset, on the plain list and on a
   * filtered one. Mutant: the offset dropped (the first page read again).
   *
   * @throws Exception never
   */
  @Test
  public void listAssignedTasksOffsetReachesTheQuery() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(taskService.findTasks(any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(20, 10, null, null, null, null));
    verify(taskService).findLastUpdatedTasks(any(), eq(20), eq(10));
    assertEquals(20, result.getUsedOffset());

    runWithDateFormatMockResult(() -> tool.listAssignedTasks(30, 10, true, null, null, null));
    verify(taskService).findTasks(any(), eq(30), eq(10));
  }

  /**
   * list_unscheduled_tasks returns compact items and says when more
   * unscheduled tasks were found than the limit let through, without a
   * next_offset since the tool takes no offset. Mutant: has_more read from
   * the page alone.
   *
   * @throws Exception never
   */
  @Test
  public void listUnscheduledTasksSaysWhenMoreWereFound() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    List<TaskDto> three = List.of(mockTaskWithProject(1L, new Date()),
                                  mockTaskWithProject(2L, new Date()),
                                  mockTaskWithProject(3L, new Date()));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(100))).thenReturn(three);

    TaskCollectionModel more = runWithDateFormatMockResult(() -> tool.listUnscheduledTasks(PROJECT_ID, 2, null));
    assertEquals(2, more.getTasks().size());
    assertTrue(more.getTasks().get(0) instanceof TaskListItemModel);
    assertTrue(more.isHasMore());
    assertNull(more.getNextOffset());

    TaskCollectionModel all = runWithDateFormatMockResult(() -> tool.listUnscheduledTasks(PROJECT_ID, 3, null));
    assertEquals(3, all.getTasks().size());
    assertFalse(all.isHasMore());
  }

  /**
   * @param node a JSON object
   * @return the names of its fields
   */
  private static Set<String> fieldNames(JsonNode node) {
    Set<String> names = new HashSet<>();
    node.propertyNames().forEach(names::add);
    return names;
  }

  /**
   * A project carries its description in the description and its permanent
   * link in the url, not the one in the other's place.
   * Mutant: the description and the link are passed in each other's position.
   */
  @Test
  public void getProjectByIdShouldKeepTheDescriptionAndTheUrlEachInItsOwnField() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(project.getId()).thenReturn(PROJECT_ID);
    when(project.getName()).thenReturn(PROJECT_NAME);
    when(project.getDescription()).thenReturn("Reworks the Shortcuts portlet");
    when(project.getManager()).thenReturn(Collections.emptySet());
    when(statusService.getStatuses(PROJECT_ID)).thenReturn(Collections.emptyList());
    when(labelService.findLabelsByProject(PROJECT_ID, currentIdentity, 0, 100)).thenReturn(Collections.emptyList());
    when(permanentLinkService.getLink(any())).thenReturn("/project-link");

    ProjectModel model = runWithDateFormatMockResult(() -> tool.getProjectById(PROJECT_ID));

    assertTrue(model.getDescription(), model.getDescription().contains("Reworks the Shortcuts portlet"));
    assertTrue(model.getUrl(), model.getUrl().endsWith("/project-link"));
  }

  /**
   * In a run limited to a space, the query gets the user's roles of that space
   * group and nothing of the user's other memberships, and only the projects
   * the space scope resolver gives to that space are returned: the list and
   * the project lookup agree. The scan reached the end, so the count is that
   * of the selection.
   * Mutant: the space is ignored and the user's own memberships are used; or
   * a project of another space or of none is kept.
   */
  @Test
  public void listProjectsOfASpaceInALimitedRunKeepsTheProjectsTheResolverGivesToIt() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    ProjectDto inSpace = listedProject(1L, 10L);
    ProjectDto inOtherSpace = listedProject(2L, 11L);
    ProjectDto inNoSpace = listedProject(3L, null);
    ProjectDto inSpaceToo = listedProject(4L, 10L);
    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK)))
                                                                                                      .thenReturn(List.of(inSpace,
                                                                                                                          inOtherSpace,
                                                                                                                          inNoSpace,
                                                                                                                          inSpaceToo));

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null));

    assertEquals(List.of(1L, 4L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(2L, result.getCount());
    ArgumentCaptor<List<String>> memberships = ArgumentCaptor.forClass(List.class);
    verify(projectService).findProjects(memberships.capture(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK));
    assertEquals(List.of("member:/spaces/test"), memberships.getValue());
  }

  /**
   * An interactive or unscoped caller keeps every project of a space they
   * belong to: the plain paged query is used, the resolver is never asked.
   * Mutant: the resolver filter is applied to every call.
   */
  @Test
  public void listProjectsOfASpaceOutsideALimitedRunIsNotNarrowed() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = false;
    ProjectDto inSpace = listedProject(1L, 10L);
    ProjectDto shared = listedProject(2L, null);
    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(10))).thenReturn(List.of(inSpace, shared));
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(2);

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null));

    assertEquals(List.of(1L, 2L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(2L, result.getCount());
    verify(spaceScopeResolver, never()).spaceOfProject(any());
    ArgumentCaptor<List<String>> memberships = ArgumentCaptor.forClass(List.class);
    verify(projectService).findProjects(memberships.capture(), eq(null), any(), eq(0), eq(10));
    assertEquals(List.of("member:/spaces/test"), memberships.getValue());
  }

  /**
   * The page is cut from the projects of the space, after the selection, and
   * the offset and limit given back are the ones used: a negative offset is
   * none, and a limit not positive the default one.
   * Mutant: the page is cut before the selection, or the offset is ignored.
   */
  @Test
  public void listProjectsOfASpaceInALimitedRunPagesTheSelection() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    ProjectDto other = listedProject(2L, 11L);
    ProjectDto first = listedProject(1L, 10L);
    ProjectDto second = listedProject(3L, 10L);
    ProjectDto third = listedProject(4L, 10L);
    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK)))
                                                                                                      .thenReturn(List.of(other,
                                                                                                                          first,
                                                                                                                          second,
                                                                                                                          third));

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 1, 1, null));

    assertEquals(List.of(3L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(3L, result.getCount());
    assertEquals(1, result.getUsedOffset());
    assertEquals(1, result.getUsedLimit());

    ProjectCollectionModel clamped = runWithDateFormatMockResult(() -> tool.listProjects(10L, -4, -2, null));
    assertEquals(List.of(1L, 3L, 4L), clamped.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(0, clamped.getUsedOffset());
    assertEquals(10, clamped.getUsedLimit());
  }

  /**
   * A scan stops as soon as one more project than the page asks for is kept:
   * the second chunk is never read, projects after that one are never mapped
   * nor asked of the resolver, and the count is the number kept, more than
   * the page.
   * Mutant: the whole chunk or every chunk is read.
   */
  @Test
  public void listProjectsOfASpaceInALimitedRunStopsOnceThePageIsFilled() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    List<ProjectDto> chunk = new ArrayList<>();
    for (long id = 1; id <= TaskMcpTool.SPACE_SCAN_CHUNK; id++) {
      chunk.add(listedProject(id, 10L));
    }
    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK))).thenReturn(chunk);

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 0, 2, null));

    assertEquals(List.of(1L, 2L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(3L, result.getCount());
    verify(projectService, times(1)).findProjects(any(), any(), any(), anyInt(), anyInt());
    verify(spaceScopeResolver, never()).spaceOfProject(chunk.get(3));
  }

  /**
   * A scan reads the next chunk while the page isn't filled, and its count is
   * exact once it reached the end (a chunk shorter than asked).
   * Mutant: only the first chunk is read.
   */
  @Test
  public void listProjectsOfASpaceInALimitedRunReadsSeveralChunks() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    List<ProjectDto> firstChunk = new ArrayList<>();
    for (long id = 1; id <= TaskMcpTool.SPACE_SCAN_CHUNK; id++) {
      firstChunk.add(listedProject(id, 11L));
    }
    ProjectDto mine = listedProject(100L, 10L);
    ProjectDto minetoo = listedProject(101L, 10L);
    ProjectDto theirs = listedProject(102L, 11L);
    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK))).thenReturn(firstChunk);
    when(projectService.findProjects(any(), eq(null), any(), eq(TaskMcpTool.SPACE_SCAN_CHUNK), eq(TaskMcpTool.SPACE_SCAN_CHUNK)))
                                                                                                                                  .thenReturn(List.of(mine,
                                                                                                                                                      minetoo,
                                                                                                                                                      theirs));

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null));

    assertEquals(List.of(100L, 101L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(2L, result.getCount());
    verify(projectService, times(2)).findProjects(any(), any(), any(), anyInt(), anyInt());
  }

  /**
   * A space none of whose reachable projects belong to it lists nothing in a
   * limited run, and a space the user reaches no project of ends the scan at
   * once.
   */
  @Test
  public void listProjectsOfASpaceWithNoProjectOfItsOwnIsEmpty() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    ProjectDto other = listedProject(2L, 11L);
    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK)))
                                                                                                      .thenReturn(List.of(other));
    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null));
    assertEquals(0, result.getProjects().size());
    assertEquals(0L, result.getCount());

    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK)))
                                                                                                      .thenReturn(Collections.emptyList());
    result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null));
    assertEquals(0L, result.getCount());
  }

  /**
   * A call is limited to a space when its acting identity carries a space
   * scope, and also when its identity can't be read: it is then never taken
   * for an unlimited one. Runs the real {@code isSpaceScoped}, which the
   * other tests stub.
   * Mutant: an unreadable identity, a null one or the scope's presence is
   * read the other way.
   */
  @Test
  public void aCallIsLimitedToASpaceWhenItsIdentityCarriesAScopeOrCantBeRead() {// NOSONAR
    TaskMcpTool realTool = new TaskMcpTool(projectService,
                                           statusService,
                                           taskService,
                                           commentService,
                                           labelService,
                                           spaceService,
                                           translationService,
                                           resourceBundleService,
                                           identityManager,
                                           profilePropertyService,
                                           userAcl,
                                           portalConfigService,
                                           permanentLinkService,
                                           favoriteService,
                                           spaceScopeResolver);
    ActingIdentity limited = mock(ActingIdentity.class);
    when(limited.spaceScope()).thenReturn(7L);
    ActingIdentity unlimited = mock(ActingIdentity.class);
    when(unlimited.spaceScope()).thenReturn(null);
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(limited);
      assertTrue(realTool.isSpaceScoped());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(unlimited);
      assertFalse(realTool.isSpaceScoped());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(null);
      assertFalse(realTool.isSpaceScoped());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenThrow(new IllegalStateException("refused"));
      assertTrue(realTool.isSpaceScoped());
    }
  }

  /**
   * The space a call is limited to is the one its acting identity carries,
   * none for an identity without a scope or no identity, and an identity that
   * can't be read fails the read rather than passing for an unlimited call.
   * Runs the real {@code getScopeSpaceId}, which the other tests stub.
   * Mutants: the scope not read, or the read failure swallowed.
   */
  @Test
  public void theScopeSpaceIsTheOneTheActingIdentityCarries() {// NOSONAR
    TaskMcpTool realTool = new TaskMcpTool(projectService,
                                           statusService,
                                           taskService,
                                           commentService,
                                           labelService,
                                           spaceService,
                                           translationService,
                                           resourceBundleService,
                                           identityManager,
                                           profilePropertyService,
                                           userAcl,
                                           portalConfigService,
                                           permanentLinkService,
                                           favoriteService,
                                           spaceScopeResolver);
    ActingIdentity limited = mock(ActingIdentity.class);
    when(limited.spaceScope()).thenReturn(7L);
    ActingIdentity unlimited = mock(ActingIdentity.class);
    when(unlimited.spaceScope()).thenReturn(null);
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(limited);
      assertEquals(Long.valueOf(7L), realTool.getScopeSpaceId());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(unlimited);
      assertNull(realTool.getScopeSpaceId());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(null);
      assertNull(realTool.getScopeSpaceId());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenThrow(new IllegalStateException("refused"));
      assertThrows(IllegalStateException.class, realTool::getScopeSpaceId);
    }
  }

  /**
   * Gives a space 10 whose group the user holds the member role of.
   */
  private void givenSpaceTheUserIsMemberOf() {
    Space space = mock(Space.class);
    lenient().when(space.getGroupId()).thenReturn("/spaces/test");
    lenient().when(space.getId()).thenReturn("10");
    when(spaceService.getSpaceById(10L)).thenReturn(space);
    when(currentIdentity.getMemberships()).thenReturn(List.of(new MembershipEntry("/spaces/test", "member"),
                                                              new MembershipEntry("/spaces/other", "manager")));
  }

  /**
   * @param id a project id
   * @param spaceId the space the resolver gives the project, or null
   * @return a project the tool can render
   */
  private ProjectDto listedProject(long id, Long spaceId) throws Exception {
    ProjectDto project = mock(ProjectDto.class);
    lenient().when(project.getId()).thenReturn(id);
    lenient().when(project.getName()).thenReturn(PROJECT_NAME);
    lenient().when(project.getDescription()).thenReturn("");
    lenient().when(project.getManager()).thenReturn(Collections.emptySet());
    lenient().when(statusService.getStatuses(id)).thenReturn(Collections.emptyList());
    lenient().when(labelService.findLabelsByProject(id, currentIdentity, 0, 100)).thenReturn(Collections.emptyList());
    lenient().when(spaceScopeResolver.spaceOfProject(project)).thenReturn(spaceId);
    return project;
  }

  /**
   * Given a space the user isn't a member of, the list is empty and the
   * project service is not asked: an empty list of memberships would select
   * every project.
   * Mutant: the space memberships aren't intersected with the user's.
   */
  @Test
  public void listProjectsOfASpaceTheUserIsNotMemberOfIsEmpty() throws Exception { // NOSONAR
    Space space = mock(Space.class);
    when(space.getGroupId()).thenReturn("/spaces/test");
    when(spaceService.getSpaceById(10L)).thenReturn(space);
    when(currentIdentity.getMemberships()).thenReturn(List.of(new MembershipEntry("/spaces/other", "member")));

    for (boolean scoped : new boolean[] { false, true }) {
      currentSpaceScoped = scoped;
      assertEquals(0L, runWithDateFormatMockResult(() -> tool.listProjects(10L, 5, 20, null)).getCount());
    }
    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 5, 20, null));

    assertEquals(0, result.getProjects().size());
    assertEquals(0L, result.getCount());
    assertEquals(5, result.getUsedOffset());
    assertEquals(20, result.getUsedLimit());
    verify(projectService, never()).findProjects(any(), any(), any(), anyInt(), anyInt());
    verify(projectService, never()).countProjects(any(), any());
  }

  @Test
  public void listTasksWithProjectShouldLoadProjectTasks() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    TaskDto task = mockTaskWithProject();

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10)))
                                                                .thenReturn(Collections.singletonList(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(PROJECT_ID, true, false, null, null, null, null, null, null, null, null, null));

    assertEquals(1, result.getTasks().size());
    assertEquals(1L, result.getCount());
    verify(projectService, atLeastOnce()).getProject(PROJECT_ID);
    verify(taskService).findLastUpdatedTasks(any(), eq(0), eq(10));
  }

  /**
   * A title narrows the tasks of the project: the query keeps the project
   * condition and adds, beside it in the same conjunction, a title match on
   * the trimmed, lower-cased title, which the store compares with the
   * lower-cased task title. The count runs on that same query.
   * Mutants: the title not passed to the query, passed with its case, or
   * joined to the project by a disjunction.
   */
  @Test
  public void listTasksNarrowsTheProjectByTitle() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listTasks(PROJECT_ID, false, false, null, null, null, "  Live Check 2 ", null, null, null, null, null);

    List<Condition> conjunction = conjunction(query.getValue());
    assertTrue(conjunction.stream().anyMatch(c -> isSingle(c, SingleCondition.IN, "status.project.id", List.of(PROJECT_ID))));
    assertTrue(conjunction.stream().anyMatch(c -> isSingle(c, SingleCondition.LIKE, "title", "%live check 2%")));
    verify(taskService).countTasks(query.getValue());
  }

  /**
   * Without a project, a title narrows the tasks the user can access: the
   * accessibility disjunction and the title match are both members of the
   * query's conjunction, so the title never widens what is listed. A blank
   * title adds no condition.
   * Mutants: the title added as an alternative to the accessibility, or a
   * blank title matching every title.
   */
  @Test
  public void listTasksNarrowsTheAccessibleTasksByTitle() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listTasks(null, false, false, null, null, null, "Live", null, null, null, null, null);
    tool.listTasks(null, false, false, null, null, null, "  ", null, null, null, null, null);

    List<Condition> withTitle = conjunction(query.getAllValues().get(0));
    assertTrue(withTitle.stream()
                        .anyMatch(c -> c instanceof AggregateCondition aggregate && AggregateCondition.OR.equals(aggregate.getType())));
    assertTrue(withTitle.stream().anyMatch(c -> isSingle(c, SingleCondition.LIKE, "title", "%live%")));
    List<Condition> blank = conjunction(query.getAllValues().get(1));
    assertTrue(blank.stream().noneMatch(c -> c instanceof SingleCondition<?> single && SingleCondition.LIKE.equals(single.getType())));
  }

  @Test(expected = IllegalAccessException.class)
  public void listTasksWithProjectWhenProjectNotViewableShouldThrowException() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(false);

    tool.listTasks(PROJECT_ID, false, false, null, 0, 10, null, null, null, null, null, null);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void listTasksWithProjectWhenProjectServiceThrowsEntityNotFoundShouldThrowObjectNotFound() throws Exception { // NOSONAR
    when(projectService.getProject(PROJECT_ID)).thenThrow(new EntityNotFoundException(PROJECT_ID, ProjectDto.class));

    tool.listTasks(PROJECT_ID, false, false, null, 0, 10, null, null, null, null, null, null);
  }

  @Test
  public void listProjectActivitySinceShouldReturnUpdatedAndUncompletedTasks() throws Exception { // NOSONAR
    TaskDto recentlyUpdatedTask = mockTaskWithProject();
    TaskDto oldUncompletedTask = mockTaskWithProject(13L, daysAgo(30));

    when(taskService.countTasks(any())).thenReturn(2);

    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10)))
                                                                .thenReturn(Collections.singletonList(recentlyUpdatedTask))
                                                                .thenReturn(Collections.singletonList(oldUncompletedTask));

    when(taskService.getTaskLogs(eq(TASK_ID), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(taskService.getTaskLogs(eq(13L), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(commentService.getLastComment(anyLong())).thenReturn(null);
    when(commentService.countComments(anyLong())).thenReturn(0);

    ProjectActivityModel result = runWithDateFormatMockResult(() -> tool.listProjectActivitySince(null, 7));

    assertEquals(1, result.getLastUpdatedTasks().size());
    assertEquals(1, result.getOtherUncompletedTasks().size());
  }

  /**
   * @param projectId a project id
   * @return a label of that project
   */
  private LabelDto projectLabel(long projectId) {
    LabelDto label = mock(LabelDto.class);
    ProjectDto project = mock(ProjectDto.class);
    lenient().when(project.getId()).thenReturn(projectId);
    lenient().when(label.getProject()).thenReturn(project);
    return label;
  }

  @SneakyThrows
  private TaskDto mockTask() {
    TaskDto task = mock(TaskDto.class);
    lenient().when(task.getId()).thenReturn(TASK_ID);
    lenient().when(task.getTitle()).thenReturn("Task title");
    lenient().when(task.getDescription()).thenReturn("");
    lenient().when(task.getCreatedTime()).thenReturn(new Date());
    lenient().when(task.getPriority()).thenReturn(null);
    lenient().when(taskService.getTaskLogs(eq(TASK_ID), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    lenient().when(commentService.getLastComment(TASK_ID)).thenReturn(null);
    lenient().when(commentService.countComments(TASK_ID)).thenReturn(0);
    return task;
  }

  @SneakyThrows
  private TaskDto mockTaskWithProject() {
    return mockTaskWithProject(TASK_ID, new Date());
  }

  @SneakyThrows
  private TaskDto mockTaskWithProject(long taskId, Date createdTime) {
    TaskDto task = mock(TaskDto.class);
    ProjectDto project = mock(ProjectDto.class);
    StatusDto status = mock(StatusDto.class);

    lenient().when(task.getId()).thenReturn(taskId);
    lenient().when(task.getTitle()).thenReturn("Task " + taskId);
    lenient().when(task.getDescription()).thenReturn("");
    lenient().when(task.getCreatedTime()).thenReturn(createdTime);
    lenient().when(task.getPriority()).thenReturn(null);
    lenient().when(task.getStatus()).thenReturn(status);

    lenient().when(project.getId()).thenReturn(PROJECT_ID);
    lenient().when(project.getName()).thenReturn(PROJECT_NAME);
    lenient().when(project.getManager()).thenReturn(Collections.emptySet());

    lenient().when(status.getId()).thenReturn(8L);
    lenient().when(status.getName()).thenReturn("todo");
    lenient().when(status.getRank()).thenReturn(1);
    lenient().when(status.getProject()).thenReturn(project);

    lenient().when(labelService.findLabelsByTask(eq(task), eq(PROJECT_ID), eq(currentIdentity), eq(0), eq(100)))
             .thenReturn(Collections.emptyList());

    return task;
  }

  private Date daysAgo(int days) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, -days);
    return calendar.getTime();
  }

  @SneakyThrows
  private <T> T runWithDateFormatMockResult(CheckedSupplier<T, Exception> supplier) {
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getUserTimeZone)
            .thenReturn(TimeZone.getTimeZone("UTC"));
      mocked.when(() -> McpToolUtils.formatDate(any(Date.class)))
            .thenReturn("2024-01-01T00:00:00Z");
      return supplier.get();
    }
  }

  /**
   * A full task whose change log holds an undated entry (recorded before 7.2
   * on an upgraded PostgreSQL, MariaDB, Oracle or SQL Server database) and a comment
   * lists that entry first, with no date, then the rest by date. Mutant: the
   * entries sorted by their date with no rule for a missing one, which throws.
   *
   * @throws Exception never
   */
  @Test
  public void anUndatedChangeIsTheOldestEntryOfTheChangeLog() throws Exception { // NOSONAR
    TaskDto task = mockTaskWithProject();
    CommentDto comment = new CommentDto();
    comment.setAuthor(OTHER_USER);
    comment.setCreatedTime(Date.from(Instant.parse("2026-10-07T10:00:00Z")));
    comment.setSubComments(Collections.emptyList());
    when(commentService.countComments(TASK_ID)).thenReturn(1);
    when(commentService.getCommentsWithSubs(TASK_ID, 0, 1)).thenReturn(List.of(comment));
    when(taskService.getTaskLogs(eq(TASK_ID), anyInt(), anyInt())).thenReturn(List.of(log(2, "edit_status", "Done", "2026-10-05T10:00:00Z"),
                                                                                      undatedLog(1, "edit_title")));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithInstantDates(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null));

    List<TaskChangeLog> changeLogs = ((TaskWithChangeLogModel) result.getTasks().get(0)).getChangeLogs();
    assertEquals(List.of("edit_title", "edit_status", "commented"), changeLogs.stream().map(TaskChangeLog::getActionId).toList());
    assertNull(changeLogs.get(0).getActionDate());
    assertEquals("2026-10-05T10:00:00Z", changeLogs.get(1).getActionDate());
  }

  /**
   * A task whose only change log entry is undated was last updated when it
   * was created, not on 1 January 1970. Mutant: the entry's date read
   * whatever it is.
   *
   * @throws Exception never
   */
  @Test
  public void anUndatedChangeDoesNotDateTheLastUpdate() throws Exception { // NOSONAR
    Date creation = Date.from(Instant.parse("2026-09-01T08:00:00Z"));
    TaskDto task = mockTaskWithProject();
    when(task.getCreatedTime()).thenReturn(creation);
    when(taskService.getTaskLogs(eq(TASK_ID), anyInt(), anyInt())).thenReturn(List.of(undatedLog(1, "edit_title")));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithInstantDates(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null));

    assertEquals("2026-09-01T08:00:00Z", ((TaskWithChangeLogModel) result.getTasks().get(0)).getUpdatedDate());
  }

  /**
   * @param id the entry's id
   * @param actionName its action
   * @return a change log entry with no date, as the storage maps one: 0
   */
  private static ChangeLogEntry undatedLog(long id, String actionName) {
    ChangeLogEntry entry = new ChangeLogEntry();
    entry.setId(id);
    entry.setAuthor(USER);
    entry.setActionName(actionName);
    entry.setCreatedTime(0);
    return entry;
  }

  /**
   * Runs with the user's time zone set to UTC, each date formatted as its
   * instant, and a time in milliseconds formatted by the real
   * {@link McpToolUtils#formatDate(Long)}, which gives no date for 0.
   *
   * @param supplier the call
   * @return its result
   * @param <T> its type
   */
  private <T> T runWithInstantDates(CheckedSupplier<T, Exception> supplier) {
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getUserTimeZone).thenReturn(TimeZone.getTimeZone("UTC"));
      mocked.when(() -> McpToolUtils.formatDate(any(Date.class)))
            .thenAnswer(invocation -> invocation.<Date> getArgument(0).toInstant().toString());
      mocked.when(() -> McpToolUtils.formatDate(any(Long.class))).thenCallRealMethod();
      return supplier.get();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @FunctionalInterface
  interface CheckedSupplier<T, E extends Throwable> {
    T get() throws E;
  }

  private class TestableTaskMcpTool extends TaskMcpTool {

    TestableTaskMcpTool() {
      super(projectService,
            statusService,
            taskService,
            commentService,
            labelService,
            spaceService,
            translationService,
            resourceBundleService,
            identityManager,
            profilePropertyService,
            userAcl,
            portalConfigService,
            permanentLinkService,
            favoriteService,
            spaceScopeResolver);
    }

    @Override
    protected boolean isSpaceScoped() {
      return currentSpaceScoped;
    }

    @Override
    protected Long getScopeSpaceId() {
      if (scopeUnreadable) {
        throw new IllegalStateException("No acting identity");
      }
      return currentScopeSpaceId;
    }

    @Override
    public Identity getCurrentUserAclIdentity() {
      return currentIdentity;
    }

    @Override
    public String getCurrentUserName() {
      return USER;
    }

    @Override
    public Locale getCurrentUserLocale() {
      return Locale.ENGLISH;
    }

    @Override
    protected ZoneId getCurrentUserZone() {
      return USER_ZONE;
    }

    @Override
    protected LocalDate getToday(ZoneId zone) {
      return TODAY;
    }
  }

  /**
   * @param query a task query
   * @return the instant its due-date "before" condition compares with, null
   *         for none
   */
  private static Date findDueDateBefore(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof SingleCondition<?> single && SingleCondition.LT.equals(single.getType())
          && "dueDate".equals(single.getField())) {
        return (Date) single.getValue();
      }
    }
    return null;
  }

  /**
   * @param query a task query
   * @return whether it is scoped to the current user's tasks: an assignee
   *         condition naming them, as the accessible and the
   *         assignee-or-coworker scopes both carry
   */
  private static boolean scopedToTheUser(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof SingleCondition<?> single && "assignee".equals(single.getField())
          && (USER.equals(single.getValue()) || (single.getValue() instanceof List<?> values && values.contains(USER)))) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param query a task query
   * @return the members of its root conjunction
   */
  private static List<Condition> conjunction(TaskQuery query) {
    AggregateCondition root = (AggregateCondition) query.getCondition();
    assertEquals(AggregateCondition.AND, root.getType());
    return root.getConditions();
  }

  /**
   * @param condition a condition
   * @param type the expected type
   * @param field the expected field
   * @param value the expected value
   * @return whether it is a single condition of that type, field and value
   */
  private static boolean isSingle(Condition condition, String type, String field, Object value) {
    return condition instanceof SingleCondition<?> single
           && type.equals(single.getType())
           && field.equals(single.getField())
           && value.equals(single.getValue());
  }

  /**
   * @param query a task query
   * @return whether it selects the open tasks only
   */
  private static boolean selectsOpenTasksOnly(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof SingleCondition<?> single && SingleCondition.IS_FALSE.equals(single.getType())
          && "completed".equals(single.getField())) {
        return true;
      }
    }
    return false;
  }

  @FunctionalInterface
  interface CheckedRunnable<E extends Throwable> {

    void run() throws E;

  }

}
