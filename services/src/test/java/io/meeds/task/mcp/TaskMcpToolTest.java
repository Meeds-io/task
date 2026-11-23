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
import static org.junit.Assert.assertSame;
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
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.LongStream;

import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.dao.condition.Conditions;
import org.exoplatform.task.dao.condition.PredicateCondition;
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
import org.exoplatform.task.dto.ProjectTaskCountDto;
import org.exoplatform.task.dto.TaskGroupCountDto;
import org.exoplatform.task.dao.TaskGrouping;
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
import io.meeds.mcp.server.tool.util.McpToolPluginUtils;
import io.meeds.mcp.server.util.McpToolUtils;
import io.meeds.portal.permlink.service.PermanentLinkService;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import io.meeds.social.space.plugin.SpaceAclPlugin;
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
import io.meeds.task.mcp.model.ProjectTaskCountModel;
import io.meeds.task.mcp.model.TaskGroupCountModel;
import io.meeds.task.mcp.model.StatusTaskCountModel;
import io.meeds.task.mcp.model.TaskCollectionModel;
import io.meeds.task.mcp.model.TaskCommentCollectionModel;
import io.meeds.task.mcp.model.TaskCommentModel;
import io.meeds.task.mcp.model.TaskListItemModel;
import io.meeds.task.mcp.model.TaskModel;
import io.meeds.task.mcp.util.TaskDescriptionText;
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
  private static final long       TARGET_PROJECT_ID = 46L;

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
  private ResourceBundleService   resourceBundleService;

  @Mock
  private IdentityManager         identityManager;

  @Mock
  private UserACL                 userAcl;

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
    assertThrows(IllegalAccessException.class, () -> tool.updateTaskStatus(TASK_ID, 8L, null));

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
    // the reason given is the project the person can't see, not an edit right
    assertTrue(assertThrows(IllegalAccessException.class,
                            () -> tool.assignTask(TASK_ID, OTHER_USER)).getMessage().contains("project"));
    assertTrue(assertThrows(IllegalAccessException.class,
                            () -> tool.addTaskCoworker(TASK_ID, OTHER_USER)).getMessage().contains("project"));
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

    tool.updateTaskStatus(TASK_ID, 8L, null);

    verify(task).setStatus(newStatus);
    verify(taskService).updateTask(task);
  }

  /**
   * A task moves to a column given by its name as the board shows it: its
   * stored name or its label in the user's language, case, accents and spaces
   * aside (EXO-94959). A name no column has is refused with the board's
   * column names, one two columns have with both and their ids, and the
   * status is given by its id or its name, never both nor neither; a task on
   * no board has no column; the definition asks for the column the user
   * named, so a name no column has (« Doing ») is refused with the columns,
   * and nothing moves. Mutants: the label not compared; the accents
   * compared; the second match ignored; both arguments accepted; the refusal
   * of a name no column has dropped.
   *
   * @throws Exception never
   */
  @Test
  public void aTaskMovesToAColumnGivenByItsName() throws Exception { // NOSONAR
    ResourceBundle bundle = new ListResourceBundle() {
      @Override
      protected Object[][] getContents() {
        return new Object[][] { { "tasks.status.ToDo", "To Do" }, { "tasks.status.InProgress", "In Progress" },
            { "tasks.status.RequestInProgress", "In Progress" } };
      }
    };
    when(resourceBundleService.getResourceBundle(anyString(), any(Locale.class))).thenReturn(bundle);
    TaskDto task = mockTaskWithProject();
    ProjectDto project = task.getStatus().getProject();
    StatusDto toDo = boardStatus(1L, "ToDo", project);
    StatusDto inProgress = boardStatus(2L, "InProgress", project);
    StatusDto validation = boardStatus(3L, "Validation régie", project);
    when(statusService.getStatuses(PROJECT_ID)).thenReturn(List.of(toDo, inProgress, validation));
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);

    tool.updateTaskStatus(TASK_ID, null, "in progress");
    verify(task).setStatus(inProgress);
    tool.updateTaskStatus(TASK_ID, null, "  VALIDATION   regie ");
    verify(task).setStatus(validation);
    tool.updateTaskStatus(TASK_ID, null, "todo");
    verify(task).setStatus(toDo);
    verify(taskService, times(3)).updateTask(task);

    ObjectNotFoundException unknown = assertThrows(ObjectNotFoundException.class,
                                                   () -> tool.updateTaskStatus(TASK_ID, null, "Doing"));
    assertTrue(unknown.getMessage(), unknown.getMessage().contains("\"To Do\", \"In Progress\", \"Validation régie\""));
    verify(taskService, times(3)).updateTask(task);
    assertThrows(IllegalArgumentException.class, () -> tool.updateTaskStatus(TASK_ID, 2L, "In Progress"));
    assertThrows(IllegalArgumentException.class, () -> tool.updateTaskStatus(TASK_ID, null, " "));

    StatusDto requestInProgress = boardStatus(4L, "RequestInProgress", project);
    when(statusService.getStatuses(PROJECT_ID)).thenReturn(List.of(inProgress, requestInProgress));
    IllegalArgumentException ambiguous = assertThrows(IllegalArgumentException.class,
                                                      () -> tool.updateTaskStatus(TASK_ID, null, "In Progress"));
    String compact = ambiguous.getMessage().replaceAll("\\s", "");
    assertTrue(ambiguous.getMessage(), compact.contains("\"status_id\":2") && compact.contains("\"status_id\":4"));

    when(task.getStatus()).thenReturn(null);
    assertThrows(ObjectNotFoundException.class, () -> tool.updateTaskStatus(TASK_ID, null, "To Do"));
    verify(taskService, times(3)).updateTask(task);
  }

  /**
   * @param id a status id
   * @param name its stored name
   * @param project its project
   * @return the status
   */
  private static StatusDto boardStatus(long id, String name, ProjectDto project) {
    StatusDto status = mock(StatusDto.class);
    lenient().when(status.getId()).thenReturn(id);
    lenient().when(status.getName()).thenReturn(name);
    lenient().when(status.getRank()).thenReturn((int) id);
    lenient().when(status.getProject()).thenReturn(project);
    return status;
  }

  @Test
  public void moveTasksToProjectShouldMoveEveryTaskToTheDefaultStatus() throws Exception {// NOSONAR
    long otherTaskId = 13L;
    TaskDto task = mockTaskWithProject();
    TaskDto otherTask = mockTaskWithProject(otherTaskId, new Date());
    ProjectDto target = mockViewableProject(TARGET_PROJECT_ID);
    StatusDto defaultStatus = mockStatusOf(target, 21L);
    when(statusService.getDefaultStatus(TARGET_PROJECT_ID)).thenReturn(defaultStatus);
    mockEditableTask(TASK_ID, task);
    mockEditableTask(otherTaskId, otherTask);
    when(taskService.moveTasks(List.of(TASK_ID, otherTaskId), 21L, currentIdentity)).thenReturn(List.of(task, otherTask));

    List<TaskModel> moved = tool.moveTasksToProject(List.of(TASK_ID, otherTaskId), TARGET_PROJECT_ID, null);

    verify(taskService).moveTasks(List.of(TASK_ID, otherTaskId), 21L, currentIdentity);
    verify(taskService, never()).updateTask(any());
    assertEquals(2, moved.size());
    assertEquals(TASK_ID, moved.get(0).getId());
    assertEquals(otherTaskId, moved.get(1).getId());
    verify(statusService, never()).createInitialStatuses(any());
  }

  @Test
  public void moveTasksToProjectWithStatusShouldUseThatStatus() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    ProjectDto target = mockViewableProject(TARGET_PROJECT_ID);
    StatusDto status = mockStatusOf(target, 22L);
    when(statusService.getStatus(22L)).thenReturn(status);
    mockEditableTask(TASK_ID, task);
    when(taskService.moveTasks(List.of(TASK_ID), 22L, currentIdentity)).thenReturn(List.of(task));

    tool.moveTasksToProject(List.of(TASK_ID), TARGET_PROJECT_ID, 22L);

    verify(taskService).moveTasks(List.of(TASK_ID), 22L, currentIdentity);
    verify(statusService, never()).getDefaultStatus(anyLong());
  }

  @Test
  public void moveTasksToProjectShouldMoveADuplicatedTaskIdOnce() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    ProjectDto target = mockViewableProject(TARGET_PROJECT_ID);
    StatusDto defaultStatus = mockStatusOf(target, 21L);
    when(statusService.getDefaultStatus(TARGET_PROJECT_ID)).thenReturn(defaultStatus);
    mockEditableTask(TASK_ID, task);
    when(taskService.moveTasks(List.of(TASK_ID), 21L, currentIdentity)).thenReturn(List.of(task));

    List<TaskModel> moved = tool.moveTasksToProject(List.of(TASK_ID, TASK_ID), TARGET_PROJECT_ID, null);

    assertEquals(1, moved.size());
    verify(taskService).moveTasks(List.of(TASK_ID), 21L, currentIdentity);
  }

  @Test
  public void moveTasksToProjectWhenTheProjectHasNoStatusShouldCreateTheInitialOnes() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    ProjectDto target = mockViewableProject(TARGET_PROJECT_ID);
    StatusDto createdStatus = mockStatusOf(target, 23L);
    when(statusService.getDefaultStatus(TARGET_PROJECT_ID)).thenThrow(new NoResultException("no status")).thenReturn(createdStatus);
    mockEditableTask(TASK_ID, task);
    when(taskService.moveTasks(List.of(TASK_ID), 23L, currentIdentity)).thenReturn(List.of(task));

    tool.moveTasksToProject(List.of(TASK_ID), TARGET_PROJECT_ID, null);

    verify(statusService).createInitialStatuses(target);
    verify(taskService).moveTasks(List.of(TASK_ID), 23L, currentIdentity);
  }

  @Test
  public void moveTasksToProjectWhenTheStatusIsOfAnotherProjectShouldMoveNothing() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    mockViewableProject(TARGET_PROJECT_ID);
    ProjectDto otherProject = mock(ProjectDto.class);
    when(otherProject.getId()).thenReturn(PROJECT_ID);
    StatusDto status = mockStatusOf(otherProject, 8L);
    when(statusService.getStatus(8L)).thenReturn(status);
    when(statusService.getStatuses(TARGET_PROJECT_ID)).thenReturn(Collections.emptyList());
    lenient().when(taskService.getTask(TASK_ID)).thenReturn(task);
    lenient().when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    lenient().when(taskService.moveTasks(any(), anyLong(), any())).thenReturn(List.of(task));

    try {
      tool.moveTasksToProject(List.of(TASK_ID), TARGET_PROJECT_ID, 8L);
      fail("a status of another project must be refused");
    } catch (ObjectNotFoundException e) {
      verify(taskService, never()).moveTasks(any(), anyLong(), any());
    }
  }

  @Test
  public void moveTasksToProjectWhenOneTaskIsNotEditableShouldMoveNone() throws Exception {// NOSONAR
    long otherTaskId = 13L;
    TaskDto task = mockTaskWithProject();
    TaskDto otherTask = mockTaskWithProject(otherTaskId, new Date());
    ProjectDto target = mockViewableProject(TARGET_PROJECT_ID);
    StatusDto defaultStatus = mockStatusOf(target, 21L);
    lenient().when(statusService.getDefaultStatus(TARGET_PROJECT_ID)).thenReturn(defaultStatus);
    lenient().when(taskService.moveTasks(any(), anyLong(), any())).thenReturn(List.of(task));
    mockEditableTask(TASK_ID, task);
    when(taskService.getTask(otherTaskId)).thenReturn(otherTask);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(otherTaskId), USER)).thenReturn(false);

    try {
      tool.moveTasksToProject(List.of(TASK_ID, otherTaskId), TARGET_PROJECT_ID, null);
      fail("a task the user cannot edit must be refused");
    } catch (IllegalAccessException e) {
      verify(taskService, never()).moveTasks(any(), anyLong(), any());
      verify(statusService, never()).getDefaultStatus(anyLong());
      verify(statusService, never()).createInitialStatuses(any());
    }
  }

  @Test(expected = ObjectNotFoundException.class)
  public void moveTasksToProjectWhenATaskDoesNotExistShouldThrowException() throws Exception {// NOSONAR
    mockViewableProject(TARGET_PROJECT_ID);
    when(taskService.getTask(TASK_ID)).thenReturn(null);

    tool.moveTasksToProject(List.of(TASK_ID), TARGET_PROJECT_ID, null);
  }

  @Test
  public void moveTasksToProjectWhenTheProjectIsNotViewableShouldMoveNothing() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    ProjectDto target = mock(ProjectDto.class);
    lenient().when(target.getId()).thenReturn(TARGET_PROJECT_ID);
    when(projectService.getProject(TARGET_PROJECT_ID)).thenReturn(target);
    when(target.canView(currentIdentity)).thenReturn(false);
    StatusDto defaultStatus = mockStatusOf(target, 21L);
    lenient().when(statusService.getDefaultStatus(TARGET_PROJECT_ID)).thenReturn(defaultStatus);
    lenient().when(taskService.getTask(TASK_ID)).thenReturn(task);
    lenient().when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    lenient().when(taskService.moveTasks(any(), anyLong(), any())).thenReturn(List.of(task));

    try {
      tool.moveTasksToProject(List.of(TASK_ID), TARGET_PROJECT_ID, null);
      fail("a project the user cannot view must be refused");
    } catch (IllegalAccessException e) {
      verify(taskService, never()).moveTasks(any(), anyLong(), any());
    }
  }

  @Test(expected = ObjectNotFoundException.class)
  public void moveTasksToProjectWhenTheProjectDoesNotExistShouldThrowException() throws Exception {// NOSONAR
    when(projectService.getProject(TARGET_PROJECT_ID)).thenReturn(null);

    tool.moveTasksToProject(List.of(TASK_ID), TARGET_PROJECT_ID, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void moveTasksToProjectWhenNoTaskIdShouldThrowException() throws Exception {// NOSONAR
    tool.moveTasksToProject(Collections.emptyList(), TARGET_PROJECT_ID, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void moveTasksToProjectWhenATaskIdIsNullShouldThrowException() throws Exception {// NOSONAR
    tool.moveTasksToProject(Collections.singletonList(null), TARGET_PROJECT_ID, null);
  }

  @Test
  public void unassignMeFromTasksShouldUnassignEveryTaskInOneServiceCall() throws Exception {// NOSONAR
    long otherTaskId = 13L;
    TaskDto task = mockTaskWithProject();
    TaskDto otherTask = mockTaskWithProject(otherTaskId, new Date());
    when(taskService.unassignTasks(List.of(TASK_ID, otherTaskId), currentIdentity)).thenReturn(List.of(task, otherTask));

    List<TaskModel> updated = tool.unassignMeFromTasks(List.of(TASK_ID, otherTaskId));

    verify(taskService).unassignTasks(List.of(TASK_ID, otherTaskId), currentIdentity);
    verify(taskService, never()).updateTask(any());
    assertEquals(2, updated.size());
    assertEquals(TASK_ID, updated.get(0).getId());
    assertEquals(otherTaskId, updated.get(1).getId());
  }

  @Test
  public void unassignMeFromTasksShouldUnassignADuplicatedTaskIdOnce() throws Exception {// NOSONAR
    TaskDto task = mockTaskWithProject();
    when(taskService.unassignTasks(List.of(TASK_ID), currentIdentity)).thenReturn(List.of(task));

    tool.unassignMeFromTasks(List.of(TASK_ID, TASK_ID));

    verify(taskService).unassignTasks(List.of(TASK_ID), currentIdentity);
  }

  @Test
  public void unassignMeFromTasksWhenNoTaskIdShouldUnassignNothing() throws Exception {// NOSONAR
    assertThrows(IllegalArgumentException.class, () -> tool.unassignMeFromTasks(Collections.emptyList()));
    verify(taskService, never()).unassignTasks(any(), any());
  }

  @Test
  public void unassignMeFromTasksWhenATaskIdIsNullShouldUnassignNothing() throws Exception {// NOSONAR
    assertThrows(IllegalArgumentException.class, () -> tool.unassignMeFromTasks(Collections.singletonList(null)));
    verify(taskService, never()).unassignTasks(any(), any());
  }

  @Test
  public void unassignMeFromTasksWhenMoreThanAHundredTasksShouldUnassignNothing() throws Exception {// NOSONAR
    List<Long> taskIds = new ArrayList<>();
    for (long id = 1; id <= 101; id++) {
      taskIds.add(id);
    }
    assertThrows(IllegalArgumentException.class, () -> tool.unassignMeFromTasks(taskIds));
    verify(taskService, never()).unassignTasks(any(), any());
  }

  /**
   * Unassigning overwrites the assignee and the coworkers, so the tool is
   * declared as the single-task assignment is, and asks for an approval; a
   * second call is refused, so it is not idempotent.
   */
  @Test
  public void unassignMeFromTasksDefinitionShouldBeDeclaredDestructiveWithApproval() throws Exception {
    JSONObject definition = toolDefinition("unassign_me_from_tasks");
    assertTrue(definition.getBoolean("require_approval"));
    JSONObject annotations = definition.getJSONObject("annotations");
    assertTrue(annotations.getBoolean("destructiveHint"));
    assertFalse(annotations.getBoolean("readOnlyHint"));
    assertFalse(annotations.getBoolean("idempotentHint"));
    assertEquals(100, definition.getJSONObject("input_schema").getJSONObject("properties").getJSONObject("task_ids").getInt("maxItems"));
  }

  /**
   * The hints are passed on to the MCP clients, which confirm a call by them:
   * a bulk move overwrites each task's status and project, so it is declared
   * as the single-task status change is, and asks for an approval.
   */
  @Test
  public void moveTasksToProjectDefinitionShouldBeDeclaredDestructiveWithApproval() throws Exception {
    JSONObject definition = toolDefinition("move_tasks_to_project");
    assertTrue(definition.getBoolean("require_approval"));
    JSONObject annotations = definition.getJSONObject("annotations");
    assertTrue(annotations.getBoolean("destructiveHint"));
    assertFalse(annotations.getBoolean("readOnlyHint"));
    assertEquals(100, definition.getJSONObject("input_schema").getJSONObject("properties").getJSONObject("task_ids").getInt("maxItems"));
  }

  private JSONObject toolDefinition(String name) throws Exception {
    try (InputStream stream = getClass().getResourceAsStream("/ai-tool-definitions.json")) {
      JSONArray tools = new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getJSONArray("tools");
      for (int i = 0; i < tools.length(); i++) {
        if (name.equals(tools.getJSONObject(i).getString("name"))) {
          return tools.getJSONObject(i);
        }
      }
    }
    fail("Tool " + name + " is not declared");
    return null;
  }

  /**
   * Without the cap the tool goes on to read the target project, which this
   * test leaves unstubbed: the mutant dies on that read, the evidence that
   * the refusal no longer comes first.
   */
  @Test
  public void moveTasksToProjectWhenMoreThanAHundredTasksShouldRefuseBeforeReadingAnything() throws Exception {// NOSONAR
    List<Long> taskIds = new ArrayList<>();
    for (long i = 1; i <= 101; i++) {
      taskIds.add(i);
    }

    try {
      tool.moveTasksToProject(taskIds, TARGET_PROJECT_ID, null);
      fail("more than a hundred tasks must be refused");
    } catch (IllegalArgumentException e) {
      verify(projectService, never()).getProject(anyLong());
      verify(taskService, never()).getTask(anyLong());
    }
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
                                               resourceBundleService,
                                               identityManager,
                                               userAcl,
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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(null, null, false, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

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

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, 5, true, null, null, null, null, null, null));

    assertEquals(Date.from(Instant.parse("2026-10-08T00:00:00+02:00")), findDueDateBefore(query.getValue()));
    assertTrue("only the tasks the user is assigned to or a coworker of", scopedToTheUser(query.getValue()));
    assertMostOverdueFirst(query.getValue());
    verify(taskService, never()).getUncompletedTasks(anyString(), anyInt());
    assertEquals(1L, result.getCount());
    assertEquals(Integer.valueOf(1), ((TaskListItemModel) result.getTasks().get(0)).getDaysOverdue());
  }

  /**
   * list_assigned_tasks tells each task's relation to the user and splits
   * its count the same way: a task assigned to the user is assignee, also
   * when the user is a coworker of it, a task of another assignee the user
   * is a coworker of is coworker; assigned_count counts the listed query
   * restricted to the user as assignee, its filters kept, overdue_only
   * included, and coworker_count the rest of count. list_tasks carries
   * neither field. Mutants: relation read from the coworkers, so a task with
   * the user as both is coworker; coworker_count equal to count; the
   * assigned count without the listing's filters; the fields on list_tasks.
   *
   * @throws Exception never
   */
  @Test
  public void listAssignedTasksTellsEachRelationAndSplitsTheCount() throws Exception { // NOSONAR
    TaskDto assigned = mockTask();
    when(assigned.getAssignee()).thenReturn(USER);
    TaskDto coworked = mockTask();
    when(coworked.getAssignee()).thenReturn("mary");
    lenient().when(coworked.getCoworker()).thenReturn(Set.of(USER));
    TaskDto both = mockTask();
    when(both.getAssignee()).thenReturn(USER);
    lenient().when(both.getCoworker()).thenReturn(Set.of(USER, "mary"));
    when(both.getDueDate()).thenReturn(Date.from(Instant.parse("2026-10-07T00:00:00+02:00")));
    ArgumentCaptor<TaskQuery> listed = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(listed.capture(), eq(0), eq(10))).thenReturn(List.of(assigned, coworked, both));
    ArgumentCaptor<TaskQuery> overdue = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findTasks(overdue.capture(), eq(0), eq(10))).thenReturn(List.of(both));
    when(taskService.countTasks(any())).thenAnswer(invocation -> {
      TaskQuery counted = invocation.getArgument(0);
      return ((AggregateCondition) counted.getCondition()).getConditions()
                                                          .stream()
                                                          .anyMatch(c -> c instanceof SingleCondition<?> single
                                                              && "assignee".equals(single.getField())) ? 2 : 3;
    });

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, null, null, null, null, null, null, null, null));

    assertEquals(List.of(TaskMcpTool.RELATION_ASSIGNEE, TaskMcpTool.RELATION_COWORKER, TaskMcpTool.RELATION_ASSIGNEE),
                 result.getTasks().stream().map(task -> ((TaskListItemModel) task).getRelation()).toList());
    assertEquals(3L, result.getCount());
    assertEquals(Long.valueOf(2), result.getAssignedCount());
    assertEquals(Long.valueOf(1), result.getCoworkerCount());
    ArgumentCaptor<TaskQuery> counted = ArgumentCaptor.forClass(TaskQuery.class);
    verify(taskService, times(2)).countTasks(counted.capture());
    assertRestrictedToTheAssignee(listed.getValue(), counted.getAllValues());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(result));
    assertEquals(2, json.path("assigned_count").asInt());
    assertEquals(1, json.path("coworker_count").asInt());
    assertEquals("coworker", json.path("tasks").path(1).path("relation").asString());

    runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, null, true, null, null, null, null, null, null));
    verify(taskService, times(4)).countTasks(counted.capture());
    List<TaskQuery> overdueCounts = counted.getAllValues().subList(counted.getAllValues().size() - 2, counted.getAllValues().size());
    assertRestrictedToTheAssignee(overdue.getValue(), overdueCounts);
    assertEquals(findDueDateBefore(overdue.getValue()), findDueDateBefore(overdueCounts.get(1)));

    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(assigned));
    TaskCollectionModel tasks = runWithDateFormatMockResult(() -> tool.listTasks(null,
                                                                                true,
                                                                                false,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null, null, null, null, null, null));
    JsonNode tasksJson = JSON.readTree(JSON.writeValueAsString(tasks));
    assertFalse(tasksJson.has("assigned_count"));
    assertFalse(tasksJson.has("coworker_count"));
    assertFalse(tasksJson.path("tasks").path(0).has("relation"));
  }

  /**
   * @param condition a query condition
   * @return its type, field and value, those of its conditions for an
   *         aggregate, its class for another kind, so that a condition and
   *         its clone read the same
   */
  private static String describe(Condition condition) {
    if (condition instanceof AggregateCondition aggregate) {
      return aggregate.getType() + aggregate.getConditions().stream().map(TaskMcpToolTest::describe).toList();
    }
    if (condition instanceof SingleCondition<?> single) {
      return single.getType() + " " + single.getField() + " " + single.getValue();
    }
    return condition.getClass().getSimpleName();
  }

  /**
   * @param listed the query a listing read its tasks with
   * @param counts the two queries it counted with, in their order
   */
  private static void assertRestrictedToTheAssignee(TaskQuery listed, List<TaskQuery> counts) {
    assertSame("count reads the listed query", listed, counts.get(0));
    List<Condition> listedConditions = ((AggregateCondition) listed.getCondition()).getConditions();
    List<Condition> assignedConditions = ((AggregateCondition) counts.get(1).getCondition()).getConditions();
    assertEquals(listedConditions.size() + 1, assignedConditions.size());
    assertEquals(listedConditions.stream().map(TaskMcpToolTest::describe).toList(),
                 assignedConditions.subList(0, listedConditions.size()).stream().map(TaskMcpToolTest::describe).toList());
    SingleCondition<?> assignee = (SingleCondition<?>) assignedConditions.get(listedConditions.size());
    assertEquals("assignee", assignee.getField());
    assertEquals(List.of(USER), assignee.getValue());
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
                                                                                  null, null, null, null, null, null, null, null, null, null, null, null));

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

    tool.listTasks(null, true, false, null, null, null, null, null, "2026-10-05T08:00:00Z", null, null, null, null, null, null, null, null, null, null, null, null, null);

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

    tool.listTasks(null, false, false, null, null, null, null, null, null, " john ", 8L, null, null, null, null, null, null, null, null, null, null, null);
    tool.listTasks(null, false, false, null, null, null, null, null, null, " ", null, null, null, null, null, null, null, null, null, null, null, null);

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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, 5, null, "2026-10-01", null, null, null, null, null));

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

    runWithDateFormatMockResult(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

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
                                                                                  null, null, null, null, null, null, null, null, null, null, null, null));

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
                 () -> tool.listTasks(null, null, false, true, null, null, null, null, "2026-10-01", null, null, null, null, null, null, null, null, null, null, null, null, null));
    assertThrows(IllegalArgumentException.class, () -> tool.listAssignedTasks(null, 5, true, null, "2026-10-01", null, null, null, null));
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

    skippingTool.listAssignedTasks(null, 5, true, null, null, null, null, null, null);

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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, 5, null, null, null, null, null, null, null));

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

    TaskCollectionModel result = tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

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

    runWithDateFormatMockResult(() -> tool.updateTaskDescription(TASK_ID, "New description", null));

    verify(task).setDescription("New description");
    verify(taskService).updateTask(task);
  }

  /**
   * A description rewritten from its plain text would drop the content link
   * and the bold text the stored one holds (EXO-94958, LIVE-94958 Q3): it is
   * refused, naming what it drops and how to keep it, and nothing is saved; a
   * rewrite that keeps that markup is saved, and so is the plain text with
   * keep_formatting false, for a user who asked for it. Mutants: the check
   * dropped; keep_formatting false not honoured; the bold not compared; an
   * empty bold element counted.
   *
   * @throws Exception never
   */
  @Test
  public void aDescriptionRewriteThatDropsItsLinksOrBoldIsRefused() throws Exception { // NOSONAR
    String stored = "<div><strong>zz-94958 fixture:</strong> review on the board <content-link contenteditable=\"false\" "
        + "style=\"display:none\">/project:152</content-link> - checked twice.</div>";
    TaskDto task = mockTask();
    when(task.getDescription()).thenReturn(stored);
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    String plain = "zz-94958 fixture: review on the board L5 rig board - checked twice.\nChecked on the rig";

    IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                                                    () -> tool.updateTaskDescription(TASK_ID, plain, null));
    assertTrue(refused.getMessage(), refused.getMessage().contains("drops the description's links and bold text"));
    assertTrue(refused.getMessage(), refused.getMessage().contains("description_format html"));
    assertThrows(IllegalArgumentException.class, () -> tool.updateTaskDescription(TASK_ID, plain, true));
    IllegalArgumentException noBold = assertThrows(IllegalArgumentException.class,
                                                   () -> tool.updateTaskDescription(TASK_ID,
                                                                                    stored.replace("<strong>", "").replace("</strong>", ""),
                                                                                    null));
    assertTrue(noBold.getMessage(), noBold.getMessage().contains("drops the description's bold text"));
    verify(taskService, never()).updateTask(task);

    String kept = stored + "<div>Checked on the rig</div>";
    runWithDateFormatMockResult(() -> tool.updateTaskDescription(TASK_ID, kept, null));
    verify(task).setDescription(kept);
    runWithDateFormatMockResult(() -> tool.updateTaskDescription(TASK_ID, plain, false));
    verify(task).setDescription(plain);
    verify(taskService, times(2)).updateTask(task);
    assertEquals("an empty bold element shows nothing bold", List.of(),
                 TaskDescriptionText.droppedMarkup("<p>a <b></b> b</p>", "a b"));
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

  /**
   * A task read names each person on it, its assignee and its coworkers, by
   * username and display name only, a disabled one too, and gives its
   * description as plain text unless its markup is asked for; the comments
   * name their authors the same way (EXO-94958). Mutants: the full profile
   * mapped back for the coworkers or the authors; the markup by default; an
   * unknown format accepted.
   *
   * @throws Exception never
   */
  @Test
  public void aTaskReadNamesPeopleOnlyAndGivesThePlainDescription() throws Exception { // NOSONAR
    TaskDto task = mockTaskWithProject();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(task.getAssignee()).thenReturn(OTHER_USER);
    when(task.getCoworker()).thenReturn(new LinkedHashSet<>(List.of("mary", "paul", "lucy")));
    when(task.getDescription()).thenReturn("<p>Ship <b>it</b> today</p><p>Second line</p>");
    mockPerson(OTHER_USER, "John Smith", true);
    mockPerson("mary", "Mary Jones", true);
    mockPerson("paul", "Paul Martin", true);
    mockPerson("lucy", "Lucy Gone", false);

    TaskModel read = runWithDateFormatMockResult(() -> tool.getTaskById(TASK_ID, null));

    JsonNode json = JSON.readTree(JSON.writeValueAsString(read));
    assertEquals(Set.of("username", "display_name"), fieldNames(json.path("assignee")));
    assertEquals("John Smith", json.path("assignee").path("display_name").asString());
    assertEquals(3, json.path("coworkers").size());
    json.path("coworkers").forEach(coworker -> assertEquals(Set.of("username", "display_name"), fieldNames(coworker)));
    assertEquals("a disabled coworker is still named", "Lucy Gone", json.path("coworkers").get(2).path("display_name").asString());
    assertNoProfile(json.toString());
    assertEquals("Ship it today\nSecond line", read.getDescription());
    assertEquals(read.getDescription(), runWithDateFormatMockResult(() -> tool.getTaskById(TASK_ID, "TEXT")).getDescription());

    TaskModel html = runWithDateFormatMockResult(() -> tool.getTaskById(TASK_ID, "html"));
    assertEquals("<p>Ship <b>it</b> today</p><p>Second line</p>", html.getDescription());
    assertThrows(IllegalArgumentException.class, () -> tool.getTaskById(TASK_ID, "markdown"));

    List<CommentDto> comments = new ArrayList<>();
    for (String author : List.of(OTHER_USER, "mary", "paul", "lucy", OTHER_USER)) {
      CommentDto comment = mock(CommentDto.class);
      when(comment.getId()).thenReturn((long) comments.size() + 1);
      when(comment.getComment()).thenReturn("Comment by " + author);
      when(comment.getCreatedTime()).thenReturn(new Date());
      when(comment.getAuthor()).thenReturn(author);
      when(comment.getSubComments()).thenReturn(Collections.emptyList());
      comments.add(comment);
    }
    when(commentService.getCommentsWithSubs(TASK_ID, 0, 10)).thenReturn(comments);
    when(commentService.countCommentsWithSubs(TASK_ID)).thenReturn(5);

    TaskCommentCollectionModel commentList = runWithDateFormatMockResult(() -> tool.listTaskCommentsById(TASK_ID, null, null));

    JsonNode commentsJson = JSON.readTree(JSON.writeValueAsString(commentList));
    assertEquals(5, commentsJson.path("comments").size());
    commentsJson.path("comments")
                .forEach(comment -> assertEquals(Set.of("username", "display_name"), fieldNames(comment.path("author"))));
    assertEquals("Mary Jones", commentsJson.path("comments").get(1).path("author").path("display_name").asString());
    assertNoProfile(commentsJson.toString());
  }

  /**
   * A task read's plain text is cut from the description as the user may
   * read it, the platform's rendering for that user, never from the stored
   * markup: a content link the user may not open reads as the rendering says
   * (EXO-94958); its markup is the stored one, whose link carries no
   * linked object's title, never the rendering for the user, whose titles
   * an edit would store for every later reader. Mutants: the plain text cut
   * from the stored description; the markup served as the user's rendering.
   *
   * @throws Exception never
   */
  @Test
  public void aTaskReadsPlainTextIsCutFromWhatTheUserMayReadAndItsMarkupIsTheStoredOne() throws Exception { // NOSONAR
    TaskDto task = mockTaskWithProject();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    String stored = "<p>See <content-link contenteditable=\"false\" style=\"display: none;\">/notes:12</content-link></p>";
    when(task.getDescription()).thenReturn(stored);

    TaskModel read;
    TaskModel markup;
    try (MockedStatic<McpToolPluginUtils> transformed = mockStatic(McpToolPluginUtils.class, CALLS_REAL_METHODS)) {
      transformed.when(() -> McpToolPluginUtils.transformHtmlContent(eq(stored), any(), any()))
                 .thenReturn("<p>See <a data-object=\"notes:12\" class=\"content-link\">Secret Q4 plan</a></p>");
      read = runWithDateFormatMockResult(() -> tool.getTaskById(TASK_ID, null));
      markup = runWithDateFormatMockResult(() -> tool.getTaskById(TASK_ID, "html"));
    }

    assertEquals("the plain text is the user's own rendering", "See Secret Q4 plan", read.getDescription());
    assertEquals(stored, markup.getDescription());
    assertFalse("the markup carries no linked object's title", markup.getDescription().contains("Secret Q4 plan"));
  }

  /**
   * A task's comments, their replies and the answer to a new comment are
   * read as plain text (EXO-95049): no tag or entity is left, a link reads as
   * its text, and the text is cut from the platform's rendering for the
   * reader, so a link to an object the reader may not view reads as that
   * rendering says, never as the stored markup. Mutants: a comment left as
   * the rendering's markup; a reply left as markup; the plain text cut from
   * the stored comment instead of the rendering.
   *
   * @throws Exception never
   */
  @Test
  public void aTasksCommentsReadAsPlainTextWithTheirLinksAsText() throws Exception { // NOSONAR
    TaskDto task = mockTaskWithProject();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    String storedComment = "<div><p>See <content-link contenteditable=\"false\">/notes:12</content-link> &amp; <b>ship</b></p></div>";
    String storedReply = "<p>Done&nbsp;<content-link contenteditable=\"false\">/notes:13</content-link></p>";
    CommentDto comment = new CommentDto();
    comment.setId(41L);
    comment.setAuthor(OTHER_USER);
    comment.setComment(storedComment);
    comment.setCreatedTime(new Date());
    CommentDto reply = new CommentDto();
    reply.setId(42L);
    reply.setAuthor(USER);
    reply.setComment(storedReply);
    reply.setCreatedTime(new Date());
    reply.setParentComment(comment);
    comment.setSubComments(List.of(reply));
    when(commentService.getCommentsWithSubs(TASK_ID, 0, 10)).thenReturn(List.of(comment));
    when(commentService.countCommentsWithSubs(TASK_ID)).thenReturn(2);
    String storedNew = "<p>New <i>one</i></p>";
    CommentDto added = new CommentDto();
    added.setId(43L);
    added.setAuthor(USER);
    added.setComment(storedNew);
    added.setCreatedTime(new Date());
    when(commentService.addComment(task, USER, storedNew)).thenReturn(added);

    TaskCommentCollectionModel read;
    TaskCommentModel answer;
    try (MockedStatic<McpToolPluginUtils> transformed = mockStatic(McpToolPluginUtils.class, CALLS_REAL_METHODS)) {
      transformed.when(() -> McpToolPluginUtils.transformHtmlContent(eq(storedComment), any(), any()))
                 .thenReturn("<div><p>See <a data-object=\"notes:12\" class=\"content-link\">Q4 plan</a> &amp; <b>ship</b></p></div>");
      transformed.when(() -> McpToolPluginUtils.transformHtmlContent(eq(storedReply), any(), any()))
                 .thenReturn("<p>Done&nbsp;<span class=\"content-link\">Restricted</span></p>");
      read = runWithDateFormatMockResult(() -> tool.listTaskCommentsById(TASK_ID, null, null));
      answer = runWithDateFormatMockResult(() -> tool.addTaskComment(TASK_ID, storedNew));
    }

    assertEquals(2, read.getComments().size());
    assertEquals("the link reads as its text in the reader's rendering", "See Q4 plan & ship", read.getComments().get(0).getText());
    assertEquals("a reply is plain text too", "Done Restricted", read.getComments().get(1).getText());
    assertEquals(Long.valueOf(41L), read.getComments().get(1).getParentCommentId());
    assertEquals("New one", answer.getText());
    String json = JSON.writeValueAsString(read);
    for (String markup : List.of("<", "&amp;", "&nbsp;", "/notes:")) {
      assertFalse(markup + " in " + json, json.contains(markup));
    }
  }

  /**
   * @param username a user's name
   * @param fullName its display name
   * @param enabled whether the user is enabled
   */
  private void mockPerson(String username, String fullName, boolean enabled) {
    org.exoplatform.social.core.identity.model.Identity identity = mock(org.exoplatform.social.core.identity.model.Identity.class);
    Profile profile = mock(Profile.class);
    lenient().when(identity.getProfile()).thenReturn(profile);
    lenient().when(identity.isEnable()).thenReturn(enabled);
    lenient().when(profile.getFullName()).thenReturn(fullName);
    lenient().when(identityManager.getOrCreateUserIdentity(username)).thenReturn(identity);
  }

  /**
   * @param json an answer's JSON
   */
  private static void assertNoProfile(String json) {
    for (String profileField : List.of("about_me", "avatar_url", "banner_url", "profile_page_url", "experiences", "login_id")) {
      assertFalse(profileField + " in " + json, json.contains(profileField));
    }
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
    assertFalse(result.isHasMore());
    assertEquals(0, result.getUncompletedCount());
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, null, null));

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

    runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, "  Q3 Roadmap  ", null));
    verify(projectService).findProjects(any(), eq("Q3 Roadmap"), any(), eq(0), eq(10));
    ArgumentCaptor<ProjectQuery> counted = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService, times(2)).countProjects(counted.capture());
    assertAllWordsQuery(counted.getAllValues().get(0), "%q3%", "%roadmap%");
    assertNameQuery(counted.getAllValues().get(1), AggregateCondition.OR, "%q3%", "%roadmap%");

    runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, " ", null));
    verify(projectService).findProjects(any(), eq(null), any(), eq(0), eq(10));
    verify(projectService, times(3)).countProjects(counted.capture());
    assertNameQuery(counted.getAllValues().get(counted.getAllValues().size() - 1), null);
    verify(projectService, never()).countProjects(any(), any());

    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, "Q3 Roadmap", null));
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, "  Roadmap ", null));
    assertTrue(result.getProjects().isEmpty());
    assertEquals(0L, result.getCount());
    assertFalse(result.isHasMore());
    verify(projectService, times(1)).countProjects(any(ProjectQuery.class));

    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    ProjectCollectionModel ofSpace = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, "Roadmap", null));
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, 2, 5, " Big Board 50 ", null));

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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, 2, 5, " Big Board 50 ", null));

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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 0, 1, "Big Board", null));

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

    ProjectCollectionModel none = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, "Big Board", null));
    assertEquals(List.of(4L), none.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(1L, none.getCount());
    verify(projectService, never()).findProjects(any(ProjectQuery.class), anyInt(), anyInt());

    ProjectCollectionModel otherSpace = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, "Big Board", null));
    assertEquals(List.of(4L), otherSpace.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(1L, otherSpace.getCount());
    verify(projectService, times(2)).findProjects(any(), eq("Big Board"), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK));
  }

  /**
   * With managed_only, the list and the count read one query: the user's
   * memberships, the user's name included, against the managers only, the
   * most recently modified first; the selection of the managers and the
   * participants is never read. managed_only false, like no managed_only,
   * keeps that selection. Mutants: the participants read with managed_only;
   * the list read from the selection of both roles; managed_only taken as
   * true by default.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsManagedOnlyListsAndCountsTheProjectsTheUserManages() throws Exception { // NOSONAR
    ProjectDto managedProject = listedProject(1L, null);
    when(projectService.findProjects(any(ProjectQuery.class), eq(0), eq(10))).thenReturn(List.of(managedProject));
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(1);

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, null, true));

    assertEquals(List.of(1L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(1L, result.getCount());
    assertFalse(result.isHasMore());
    ArgumentCaptor<ProjectQuery> listed = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).findProjects(listed.capture(), eq(0), eq(10));
    ArgumentCaptor<ProjectQuery> counted = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).countProjects(counted.capture());
    for (ProjectQuery query : List.of(listed.getValue(), counted.getValue())) {
      assertManagedQuery(query, null);
      assertTrue(((List<?>) managerCondition(query).getValue()).contains(USER));
    }
    verify(projectService, never()).findProjects(any(), any(), any(), anyInt(), anyInt());

    runWithDateFormatMockResult(() -> tool.listProjects(null, null, null, null, false));
    verify(projectService).findProjects(any(), eq(null), any(), eq(0), eq(10));
    verify(projectService, times(2)).countProjects(counted.capture());
    assertNameQuery(counted.getAllValues().get(counted.getAllValues().size() - 1), null);
  }

  /**
   * With managed_only and a name, the query of every word and, when no
   * managed project holds them all, the list and the count of any one word
   * read the managers only, with the same offset and limit. Mutants: the
   * participants read on the all-words path or on the any-word fallback; the
   * fallback read from the selection of both roles.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsManagedOnlyByNameReadsTheManagersOnBothPaths() throws Exception { // NOSONAR
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(0, 2);
    ProjectDto onboarding = listedProject(4L, null);
    when(projectService.findProjects(any(ProjectQuery.class), eq(2), eq(5))).thenReturn(List.of(onboarding));

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, 2, 5, " Big Board 50 ", true));

    assertEquals(List.of(4L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(2L, result.getCount());
    ArgumentCaptor<ProjectQuery> counted = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService, times(2)).countProjects(counted.capture());
    assertManagedQuery(counted.getAllValues().get(0), AggregateCondition.AND, "%big%", "%board%", "%50%");
    assertManagedQuery(counted.getAllValues().get(1), AggregateCondition.OR, "%big%", "%board%", "%50%");
    ArgumentCaptor<ProjectQuery> listed = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).findProjects(listed.capture(), eq(2), eq(5));
    assertManagedQuery(listed.getValue(), AggregateCondition.OR, "%big%", "%board%", "%50%");
    verify(projectService, never()).findProjects(any(), any(), any(), anyInt(), anyInt());
  }

  /**
   * In a run limited to a space, managed_only scans the projects whose
   * managers hold one of the space roles the user has, and keeps those of the
   * space. Mutant: the scan read from the selection of both roles.
   *
   * @throws Exception never
   */
  @Test
  public void listProjectsOfASpaceManagedOnlyScansTheManagedProjects() throws Exception { // NOSONAR
    givenSpaceTheUserIsMemberOf();
    currentSpaceScoped = true;
    List<ProjectDto> projects = List.of(listedProject(1L, 10L), listedProject(2L, 11L));
    when(projectService.findProjects(any(ProjectQuery.class), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK))).thenReturn(projects);

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null, true));

    assertEquals(List.of(1L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    ArgumentCaptor<ProjectQuery> scanned = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).findProjects(scanned.capture(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK));
    assertManagedQuery(scanned.getValue(), null);
    verify(projectService, never()).findProjects(any(), any(), any(), anyInt(), anyInt());
  }

  /**
   * @param query a query of the projects the memberships manage, by name
   *          words or not, the most recently modified first
   * @param wordsType how the name words are combined, or null when the query
   *          has no name condition
   * @param likes the patterns each word must match, lower-cased
   */
  private void assertManagedQuery(ProjectQuery query, String wordsType, String... likes) {
    assertEquals(1, query.getOrderBy().size());
    assertEquals("lastModifiedDate", query.getOrderBy().get(0).getFieldName());
    assertFalse(query.getOrderBy().get(0).isAscending());
    List<Condition> conditions = ((AggregateCondition) query.getCondition()).getConditions();
    assertEquals(wordsType == null ? 1 : 2, conditions.size());
    SingleCondition<?> managers = managerCondition(query);
    assertEquals(SingleCondition.IN, managers.getType());
    assertFalse(((List<?>) managers.getValue()).isEmpty());
    if (wordsType == null) {
      return;
    }
    AggregateCondition words = (AggregateCondition) conditions.get(1);
    assertEquals(wordsType, words.getType());
    assertEquals(List.of(likes), words.getConditions().stream().map(c -> ((SingleCondition<?>) c).getValue()).toList());
  }

  /**
   * @param query a query of projects by their roles
   * @return its first condition, which must be one on the managers alone
   */
  private SingleCondition<?> managerCondition(ProjectQuery query) {
    Condition first = ((AggregateCondition) query.getCondition()).getConditions().get(0);
    assertTrue("a condition on the managers alone, not " + first, first instanceof SingleCondition);
    SingleCondition<?> managers = (SingleCondition<?>) first;
    assertEquals("manager", managers.getField());
    return managers;
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

    ProjectCollectionModel first = runWithDateFormatMockResult(() -> tool.listProjects(10L, 0, 2, null, null));
    assertTrue(first.isHasMore());
    assertEquals(Integer.valueOf(2), first.getNextOffset());

    ProjectCollectionModel last = runWithDateFormatMockResult(() -> tool.listProjects(10L, 2, 2, null, null));
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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

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

    TaskCollectionModel full = runWithDateFormatMockResult(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

    TaskModel fullTask = (TaskModel) full.getTasks().get(0);
    assertTrue(fullTask instanceof TaskWithChangeLogModel);
    assertEquals("A long description", fullTask.getDescription().replaceAll("<[^>]+>", "").trim());
    assertEquals(1, fullTask.getCoworkers().size());
    assertEquals("the full task counts the replies", 3, fullTask.getCommentsCount());
    assertNotNull(item.getCreatedDate());
    assertEquals("the same creation date as the full task", fullTask.getCreatedDate(), item.getCreatedDate());
  }

  /**
   * A listed task carries its start date beside its due date, so that a
   * gantt of the listing draws it as a bar from one to the other
   * (EXO-93760); a task without a start date carries none (the key set of
   * {@link #listTasksReturnsCompactItemsAndTheFullTaskWithTheChangeLog}).
   * Mutant: the list item built without the start date.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGivesEachTaskItsStartDate() throws Exception { // NOSONAR
    TaskDto task = mockTaskWithProject();
    when(task.getStartDate()).thenReturn(Date.from(Instant.parse("2026-11-02T08:00:00Z")));
    when(task.getDueDate()).thenReturn(Date.from(Instant.parse("2026-11-06T17:00:00Z")));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result;
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getUserTimeZone).thenReturn(TimeZone.getTimeZone("UTC"));
      mocked.when(() -> McpToolUtils.formatDate(any(Date.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Date.class).toInstant().toString());
      result = tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    TaskListItemModel item = (TaskListItemModel) result.getTasks().get(0);
    assertEquals("2026-11-02T08:00:00Z", item.getStartDate());
    assertEquals("2026-11-06T17:00:00Z", item.getDueDate());
    assertEquals("2026-11-02T08:00:00Z", JSON.readTree(JSON.writeValueAsString(item)).path("start_date").asText());
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

    TaskCollectionModel full = runWithDateFormatMockResult(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

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
      result = tool.listTasks(null, false, false, null, 0, 100, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
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

      TaskListItemModel item = (TaskListItemModel) tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
                                                       .getTasks()
                                                       .get(0);
      TaskModel fullTask = (TaskModel) tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
                                           .getTasks()
                                           .get(0);
      TaskListItemModel assigned = (TaskListItemModel) tool.listAssignedTasks(null, null, null, null, null, null, null, null, null).getTasks().get(0);

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
   * and of list_assigned_tasks keeps its keys, relation besides for the
   * latter: neither description nor description_truncated, and the
   * description is not even read. Mutants:
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
      TaskListItemModel listed = (TaskListItemModel) runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, includeDescription, null, null, null, null, null, null, null, null, null, null)).getTasks()
                                                                                                                                                                                      .get(0);
      TaskListItemModel assigned = (TaskListItemModel) runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, null, null, null, null, includeDescription, null, null, null)).getTasks()
                                                                                                                                                                  .get(0);

      assertEquals(DEFAULT_ITEM_KEYS, fieldNames(JSON.readTree(JSON.writeValueAsString(listed))));
      assertEquals(withRelation(DEFAULT_ITEM_KEYS), fieldNames(JSON.readTree(JSON.writeValueAsString(assigned))));
    }
    verify(task, never()).getDescription();
  }

  /**
   * @param keys the keys of a compact item
   * @return those keys and relation, which an item of list_assigned_tasks
   *         carries besides
   */
  private static Set<String> withRelation(Set<String> keys) {
    Set<String> withRelation = new HashSet<>(keys);
    withRelation.add("relation");
    return withRelation;
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

    List<?> listed = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null, null)).getTasks();
    List<?> assigned = runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, null, null, null, null, true, null, null, null)).getTasks();

    for (List<?> items : List.of(listed, assigned)) {
      JsonNode shortItem = JSON.readTree(JSON.writeValueAsString(items.get(0)));
      Set<String> withDescription = items == assigned ? withRelation(DEFAULT_ITEM_KEYS) : new HashSet<>(DEFAULT_ITEM_KEYS);
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

    TaskCollectionModel tasks = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, 101, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    verify(taskService).findLastUpdatedTasks(any(), eq(0), eq(100));
    assertEquals(100, tasks.getUsedLimit());

    runWithDateFormatMockResult(() -> tool.listAssignedTasks(null, 101, null, null, null, null, null, null, null));
    verify(taskService, times(2)).findLastUpdatedTasks(any(), eq(0), eq(100));

    ProjectCollectionModel projects = runWithDateFormatMockResult(() -> tool.listProjects(null, null, 101, null, null));
    verify(projectService).findProjects(any(), any(), any(), eq(0), eq(100));
    assertEquals(100, projects.getUsedLimit());

    runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, null, 100, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
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

    TaskCollectionModel first = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, 0, 10, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    assertTrue(first.isHasMore());
    assertEquals(Integer.valueOf(10), first.getNextOffset());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(first));
    assertTrue(json.path("has_more").asBoolean());
    assertEquals(10, json.path("next_offset").asInt());
    assertEquals(25, json.path("count").asInt());
    assertEquals(10, json.path("used_limit").asInt());

    TaskCollectionModel last = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, 20, 10, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    assertFalse(last.isHasMore());
    assertNull(last.getNextOffset());
    assertFalse(JSON.readTree(JSON.writeValueAsString(last)).has("next_offset"));

    TaskCollectionModel past = runWithDateFormatMockResult(() -> tool.listTasks(null, false, false, null, 30, 10, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(20, 10, null, null, null, null, null, null, null));
    verify(taskService).findLastUpdatedTasks(any(), eq(20), eq(10));
    assertEquals(20, result.getUsedOffset());

    runWithDateFormatMockResult(() -> tool.listAssignedTasks(30, 10, true, null, null, null, null, null, null));
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null, null));

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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null, null));

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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 1, 1, null, null));

    assertEquals(List.of(3L), result.getProjects().stream().map(ProjectListItemModel::getId).toList());
    assertEquals(3L, result.getCount());
    assertEquals(1, result.getUsedOffset());
    assertEquals(1, result.getUsedLimit());

    ProjectCollectionModel clamped = runWithDateFormatMockResult(() -> tool.listProjects(10L, -4, -2, null, null));
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 0, 2, null, null));

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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null, null));

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
    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null, null));
    assertEquals(0, result.getProjects().size());
    assertEquals(0L, result.getCount());

    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK)))
                                                                                                      .thenReturn(Collections.emptyList());
    result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null, null, null));
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
                                           resourceBundleService,
                                           identityManager,
                                           userAcl,
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
                                           resourceBundleService,
                                           identityManager,
                                           userAcl,
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
      assertEquals(0L, runWithDateFormatMockResult(() -> tool.listProjects(10L, 5, 20, null, null)).getCount());
    }
    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 5, 20, null, null));

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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(PROJECT_ID, true, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

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

    tool.listTasks(PROJECT_ID, false, false, null, null, null, "  Live Check 2 ", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

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

    tool.listTasks(null, false, false, null, null, null, "Live", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    tool.listTasks(null, false, false, null, null, null, "  ", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

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

    tool.listTasks(PROJECT_ID, false, false, null, 0, 10, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void listTasksWithProjectWhenProjectServiceThrowsEntityNotFoundShouldThrowObjectNotFound() throws Exception { // NOSONAR
    when(projectService.getProject(PROJECT_ID)).thenThrow(new EntityNotFoundException(PROJECT_ID, ProjectDto.class));

    tool.listTasks(PROJECT_ID, false, false, null, 0, 10, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  @Test
  public void listProjectActivitySinceShouldReturnUpdatedAndUncompletedTasks() throws Exception { // NOSONAR
    TaskDto recentlyUpdatedTask = mockTaskWithProject(TASK_ID, Date.from(TODAY.minusDays(1).atTime(12, 0).atZone(USER_ZONE).toInstant()));
    TaskDto oldUncompletedTask = mockTaskWithProject(13L, Date.from(TODAY.minusDays(30).atTime(12, 0).atZone(USER_ZONE).toInstant()));

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
   * The period of list_project_activity_since starts at midnight of the
   * user's day, in the user's zone, never the server's (EXO-94298): a task
   * updated at 01:00 on the period's first day in Kiritimati (UTC+14), which
   * is still the day before in UTC and in Europe, is in it. Mutants: the
   * server's zone; the server's day.
   */
  @Test
  public void listProjectActivitySinceStartsAtTheUsersMidnight() throws Exception { // NOSONAR
    ZoneId kiritimati = ZoneId.of("Pacific/Kiritimati");
    TaskMcpTool farEastTool = new TestableTaskMcpTool() {
      @Override
      protected ZoneId getCurrentUserZone() {
        return kiritimati;
      }

      @Override
      protected LocalDate getToday(ZoneId zone) {
        return TODAY;
      }
    };
    TaskDto firstHourTask = mockTaskWithProject(TASK_ID, Date.from(TODAY.minusDays(7).atTime(1, 0).atZone(kiritimati).toInstant()));
    TaskDto dayBeforeTask = mockTaskWithProject(13L, Date.from(TODAY.minusDays(8).atTime(23, 0).atZone(kiritimati).toInstant()));
    when(taskService.countTasks(any())).thenReturn(2);
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(firstHourTask, dayBeforeTask));
    when(taskService.getTaskLogs(anyLong(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(commentService.getLastComment(anyLong())).thenReturn(null);
    when(commentService.countComments(anyLong())).thenReturn(0);

    ProjectActivityModel result = runWithDateFormatMockResult(() -> farEastTool.listProjectActivitySince(null, 7));

    assertEquals(List.of(TASK_ID), result.getLastUpdatedTasks().stream().map(TaskWithChangeLogModel::getId).toList());
  }

  /**
   * On a day whose midnight a daylight-saving switch skips (São Paulo, 4
   * November 2018), the period starts at the day's first instant, 01:00, and
   * a task updated at that very instant is in it with the change and the
   * comment made then, not the comment made a minute earlier, and a task
   * updated a minute earlier is not in it. Mutants: a strict comparison, on
   * the task, its change log or its comments; the server's zone.
   */
  @Test
  public void listProjectActivitySinceStartsAtTheFirstInstantOfASkippedMidnight() throws Exception { // NOSONAR
    ZoneId saoPaulo = ZoneId.of("America/Sao_Paulo");
    LocalDate weekAfterTheSwitch = LocalDate.of(2018, 11, 11);
    TaskMcpTool saoPauloTool = new TestableTaskMcpTool() {
      @Override
      protected ZoneId getCurrentUserZone() {
        return saoPaulo;
      }

      @Override
      protected LocalDate getToday(ZoneId zone) {
        return weekAfterTheSwitch;
      }
    };
    Instant firstInstant = Instant.parse("2018-11-04T03:00:00Z");
    TaskDto atFirstInstant = mockTaskWithProject(TASK_ID, Date.from(firstInstant));
    TaskDto minuteBefore = mockTaskWithProject(13L, Date.from(firstInstant.minusSeconds(60)));
    when(taskService.countTasks(any())).thenReturn(2);
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(List.of(atFirstInstant, minuteBefore));
    ChangeLogEntry changedAtFirstInstant = undatedLog(1L, "editTitle");
    changedAtFirstInstant.setCreatedTime(firstInstant.toEpochMilli());
    when(taskService.getTaskLogs(eq(TASK_ID), anyInt(), anyInt())).thenReturn(List.of(changedAtFirstInstant));
    when(taskService.getTaskLogs(eq(13L), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(commentService.getLastComment(anyLong())).thenReturn(null);
    when(commentService.countComments(TASK_ID)).thenReturn(2);
    when(commentService.countComments(13L)).thenReturn(0);
    when(commentService.getCommentsWithSubs(TASK_ID, 0, 2)).thenReturn(List.of(commentAt(firstInstant),
                                                                              commentAt(firstInstant.minusSeconds(60))));

    ProjectActivityModel result = runWithDateFormatMockResult(() -> saoPauloTool.listProjectActivitySince(null, 7));

    assertEquals(List.of(TASK_ID), result.getLastUpdatedTasks().stream().map(TaskWithChangeLogModel::getId).toList());
    // the change and the comment made at the first instant, not the comment a minute earlier
    List<String> actions = result.getLastUpdatedTasks().get(0).getChangeLogs().stream().map(TaskChangeLog::getActionId).toList();
    assertEquals(2, actions.size());
    assertEquals(Set.of("editTitle", "commented"), Set.copyOf(actions));
  }

  /**
   * list_project_activity_since returns at most 20 tasks: of 25 updated in the
   * period, the 20 most recently active, no older one, and has_more; only
   * those 20 are read in full. Mutants: the cap at 21; the tasks read in full
   * before the cap.
   */
  @Test
  public void listProjectActivitySinceListsAtMostTwentyUpdatedTasks() throws Exception { // NOSONAR
    mockProjectActivity(25, 0, 0);

    ProjectActivityModel result = runWithDateFormatMockResult(() -> tool.listProjectActivitySince(null, 7));

    assertEquals(LongStream.range(100, 120).boxed().toList(),
                 result.getLastUpdatedTasks().stream().map(TaskWithChangeLogModel::getId).toList());
    assertEquals(List.of(), result.getOtherUncompletedTasks());
    assertTrue(result.isHasMore());
    verify(commentService, times(20)).countComments(anyLong());
  }

  /**
   * Below the cap, the older uncompleted tasks fill the room the updated ones
   * leave, the most recently active first, never a completed one: 15
   * updated and 10 older give 15 and 5, and has_more. The read of all tasks
   * stops at the first page the updated ones don't fill. Mutants: the older
   * tasks past the room; the completed tasks read among the older ones; the
   * read of all tasks past that page.
   */
  @Test
  public void listProjectActivitySinceFillsTheRestWithOlderUncompletedTasks() throws Exception { // NOSONAR
    mockProjectActivity(15, 10, 3);

    ProjectActivityModel result = runWithDateFormatMockResult(() -> tool.listProjectActivitySince(null, 7));

    assertEquals(15, result.getLastUpdatedTasks().size());
    assertEquals(LongStream.range(200, 205).boxed().toList(),
                 result.getOtherUncompletedTasks().stream().map(TaskWithChangeLogModel::getId).toList());
    assertTrue(result.isHasMore());
    verify(taskService, times(2)).findLastUpdatedTasks(argThat(query -> !isUncompletedOnly(query)), anyInt(), anyInt());
  }

  /**
   * A project of exactly 20 such tasks fits: 15 updated and 5 older are all
   * returned, without has_more, whatever completed tasks the project holds.
   * Mutants: has_more set at the cap rather than past it; the completed tasks
   * read among the older ones.
   */
  @Test
  public void listProjectActivitySinceHasNoMoreWhenTheTasksFitTheCap() throws Exception { // NOSONAR
    mockProjectActivity(15, 5, 3);

    ProjectActivityModel result = runWithDateFormatMockResult(() -> tool.listProjectActivitySince(null, 7));

    assertEquals(15, result.getLastUpdatedTasks().size());
    assertEquals(LongStream.range(200, 205).boxed().toList(),
                 result.getOtherUncompletedTasks().stream().map(TaskWithChangeLogModel::getId).toList());
    assertFalse(result.isHasMore());
  }

  /**
   * Twenty updated tasks fill the cap: one older uncompleted task left out
   * sets has_more, which none does not. Mutant: the older tasks not read once
   * the updated ones fill the cap.
   */
  @Test
  public void listProjectActivitySinceHasMoreWhenAnOlderTaskIsLeftOut() throws Exception { // NOSONAR
    mockProjectActivity(20, 1, 0);

    ProjectActivityModel result = runWithDateFormatMockResult(() -> tool.listProjectActivitySince(null, 7));

    assertEquals(20, result.getLastUpdatedTasks().size());
    assertEquals(List.of(), result.getOtherUncompletedTasks());
    assertTrue(result.isHasMore());
  }

  /**
   * Twenty updated tasks and no older one: all are returned, without
   * has_more.
   */
  @Test
  public void listProjectActivitySinceHasNoMoreWhenTwentyUpdatedTasksAreAll() throws Exception { // NOSONAR
    mockProjectActivity(20, 0, 0);

    ProjectActivityModel result = runWithDateFormatMockResult(() -> tool.listProjectActivitySince(null, 7));

    assertEquals(20, result.getLastUpdatedTasks().size());
    assertFalse(result.isHasMore());
  }

  /**
   * uncompleted_count is the count of the project's uncompleted tasks, not of
   * all its tasks. Mutant: the count of all tasks.
   */
  @Test
  public void listProjectActivitySinceCountsTheUncompletedTasks() throws Exception { // NOSONAR
    mockProjectActivity(3, 2, 4);

    ProjectActivityModel result = runWithDateFormatMockResult(() -> tool.listProjectActivitySince(null, 7));

    assertEquals(5, result.getUncompletedCount());
    assertFalse(result.isHasMore());
  }

  /**
   * Stubs a project for list_project_activity_since: uncompleted tasks updated
   * yesterday (ids from 100), then completed ones updated 20 days ago (ids
   * from 300) and uncompleted ones updated 30 days ago (ids from 200), in the
   * last-activity order. The read of all tasks pages through every one, the
   * read of the uncompleted ones leaves the completed out, and each count
   * counts what its read pages through.
   *
   * @param updated the uncompleted tasks updated in the period
   * @param older the uncompleted tasks updated before it
   * @param completed the completed tasks updated before it
   */
  @SneakyThrows
  private void mockProjectActivity(int updated, int older, int completed) {
    List<TaskDto> uncompletedTasks = new ArrayList<>();
    List<TaskDto> allTasks = new ArrayList<>();
    Date yesterday = Date.from(TODAY.minusDays(1).atTime(12, 0).atZone(USER_ZONE).toInstant());
    Date twentyDaysAgo = Date.from(TODAY.minusDays(20).atTime(12, 0).atZone(USER_ZONE).toInstant());
    Date monthAgo = Date.from(TODAY.minusDays(30).atTime(12, 0).atZone(USER_ZONE).toInstant());
    for (int i = 0; i < updated; i++) {
      uncompletedTasks.add(mockTaskWithProject(100L + i, yesterday));
    }
    allTasks.addAll(uncompletedTasks);
    for (int i = 0; i < completed; i++) {
      allTasks.add(mockTaskWithProject(300L + i, twentyDaysAgo));
    }
    for (int i = 0; i < older; i++) {
      TaskDto task = mockTaskWithProject(200L + i, monthAgo);
      uncompletedTasks.add(task);
      allTasks.add(task);
    }
    when(taskService.countTasks(any())).thenAnswer(invocation -> isUncompletedOnly(invocation.getArgument(0)) ? uncompletedTasks.size()
                                                                                                                : allTasks.size());
    when(taskService.findLastUpdatedTasks(any(), anyInt(), anyInt())).thenAnswer(invocation -> {
      List<TaskDto> tasks = isUncompletedOnly(invocation.getArgument(0)) ? uncompletedTasks : allTasks;
      int offset = invocation.getArgument(1);
      int limit = invocation.getArgument(2);
      return tasks.subList(Math.min(offset, tasks.size()), Math.min(offset + limit, tasks.size()));
    });
    when(taskService.getTaskLogs(anyLong(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(commentService.getLastComment(anyLong())).thenReturn(null);
    when(commentService.countComments(anyLong())).thenReturn(0);
  }

  /**
   * @param query a task query
   * @return true when it leaves the completed tasks out
   */
  private static boolean isUncompletedOnly(TaskQuery query) {
    return query != null && restrictsField(query.getCondition(), "completed");
  }

  /**
   * @param condition a query condition, null for none
   * @param field a task field
   * @return true when it, or one of its conditions, is on that field
   */
  private static boolean restrictsField(Condition condition, String field) {
    if (condition instanceof AggregateCondition aggregate) {
      return aggregate.getConditions().stream().anyMatch(c -> restrictsField(c, field));
    }
    return condition instanceof SingleCondition<?> single && field.equals(single.getField());
  }

  /**
   * @param createdTime when the comment was posted
   * @return a comment of another user, posted then, with no reply
   */
  private static CommentDto commentAt(Instant createdTime) {
    CommentDto comment = new CommentDto();
    comment.setAuthor(OTHER_USER);
    comment.setCreatedTime(Date.from(createdTime));
    comment.setSubComments(Collections.emptyList());
    return comment;
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
  private ProjectDto mockViewableProject(long projectId) {
    ProjectDto project = mock(ProjectDto.class);
    lenient().when(project.getId()).thenReturn(projectId);
    lenient().when(project.getName()).thenReturn("Target project");
    lenient().when(project.getManager()).thenReturn(Collections.emptySet());
    when(projectService.getProject(projectId)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    return project;
  }

  private StatusDto mockStatusOf(ProjectDto project, long statusId) {
    StatusDto status = mock(StatusDto.class);
    lenient().when(status.getId()).thenReturn(statusId);
    lenient().when(status.getName()).thenReturn("status " + statusId);
    lenient().when(status.getRank()).thenReturn(1);
    lenient().when(status.getProject()).thenReturn(project);
    return status;
  }

  @SneakyThrows
  private void mockEditableTask(long taskId, TaskDto task) {
    when(taskService.getTask(taskId)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), USER)).thenReturn(true);
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

    TaskCollectionModel result = runWithInstantDates(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

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

    TaskCollectionModel result = runWithInstantDates(() -> tool.listTasks(null, false, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

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

  /**
   * without_due_date narrows the accessible tasks to those with no due date:
   * an is-null condition on the due date, the date the Tasks UI shows, beside
   * the accessibility; without it, no due-date condition at all. Mutants: the
   * condition dropped, set on the end date, or inverted to not-null.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksWithoutDueDateSelectsTheTasksWithNoDueDate() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null);
    tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, false, null, null, null, null, null, null, null, null, null);

    List<Condition> undated = conjunction(query.getAllValues().get(0));
    assertTrue(undated.stream()
                      .anyMatch(c -> c instanceof SingleCondition<?> single && SingleCondition.IS_NULL.equals(single.getType())
                                     && "dueDate".equals(single.getField())));
    assertTrue("only the tasks the user can access", scopedToTheUser(query.getAllValues().get(0)));
    assertTrue(selectsOpenTasksOnly(query.getAllValues().get(0)));
    assertFalse(hasDueDateCondition(query.getAllValues().get(1)));
  }

  /**
   * A due range of days covers both whole days in the user's time zone: from
   * the start of its first day, included, to the start of the day after its
   * last day, excluded, so that a task due late on the last day is selected
   * and none of the next day. A date-time upper bound is that instant,
   * included. Mutants: the upper bound at the start of the last day, an
   * inclusive bound on the next day, the server's zone instead of the user's.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksDueRangeCoversWholeDaysInTheUserZone() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, "2026-11-09", "2026-11-15", null, null, null, null, null, null, null);
    tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, "2026-11-15T18:00:00+01:00", null, null, null, null, null, null, null);

    List<Condition> days = conjunction(query.getAllValues().get(0));
    assertTrue(days.stream().anyMatch(c -> isSingle(c, SingleCondition.GTE, "dueDate", Date.from(Instant.parse("2026-11-08T23:00:00Z")))));
    assertTrue(days.stream().anyMatch(c -> isSingle(c, SingleCondition.LT, "dueDate", Date.from(Instant.parse("2026-11-15T23:00:00Z")))));
    assertTrue(days.stream()
                   .noneMatch(c -> c instanceof SingleCondition<?> single && SingleCondition.LTE.equals(single.getType())));
    List<Condition> instant = conjunction(query.getAllValues().get(1));
    assertTrue(instant.stream().anyMatch(c -> isSingle(c, SingleCondition.LTE, "dueDate", Date.from(Instant.parse("2026-11-15T17:00:00Z")))));
    assertTrue(instant.stream().noneMatch(c -> c instanceof SingleCondition<?> single && SingleCondition.GTE.equals(single.getType())));
  }

  /**
   * The tasks with no due date are never overdue nor due in a range: asking
   * both is refused with a message saying which to pass, before any query.
   * A due_to that is no date is refused naming due_to. Mutants: either check
   * dropped, or the range check reading due_from only.
   */
  @Test
  public void listTasksRefusesNoDueDateWithARangeOrOverdue() { // NOSONAR
    IllegalArgumentException range = assertThrows(IllegalArgumentException.class,
                                                  () -> tool.listTasks(null, null, false, null, null, null, null, null, null, null, null, null, true, null, "2026-11-15", null, null, null, null, null, null, null));
    assertTrue(range.getMessage().contains("without_due_date"));
    assertTrue(range.getMessage().contains("due_from/due_to"));
    IllegalArgumentException overdue = assertThrows(IllegalArgumentException.class,
                                                    () -> tool.listTasks(null, null, false, true, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null));
    assertTrue(overdue.getMessage().contains("overdue_only"));
    IllegalArgumentException noDate = assertThrows(IllegalArgumentException.class,
                                                   () -> tool.listTasks(null, null, false, null, null, null, null, null, null, null, null, null, null, null, "2026-13-45", null, null, null, null, null, null, null));
    assertTrue(noDate.getMessage().startsWith("due_to"));
    verify(taskService, never()).findLastUpdatedTasks(any(), anyInt(), anyInt());
  }

  /**
   * A priority narrows the listing of the accessible tasks: the listed, the
   * counted and the board-counted query carry the priority equality beside
   * the user's scope, and the boards' open count does not, being every open
   * task of the board the user can access (EXO-94284); the boards' overdue
   * tasks, which a priority does not narrow, are not counted (EXO-94935).
   * Mutants: the priority not applied, applied to the open count too, or
   * another value compared; the overdue tasks counted beside a priority.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksWithPriorityListsAndCountsTheTasksOfThatPriority() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> listed = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(listed.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(42);
    ArgumentCaptor<TaskQuery> counted = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByProject(counted.capture())).thenReturn(List.of(new ProjectTaskCountDto(8L, "Alpha", 40L)),
                                                                        List.of(new ProjectTaskCountDto(8L, "Alpha", 90L)));

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, true, Priority.HIGH, null, null, null, null, null);

    TaskQuery query = listed.getValue();
    assertTrue(conjunction(query).stream().anyMatch(c -> isSingle(c, SingleCondition.EQ, "priority", Priority.HIGH)));
    assertTrue(scopedToTheUser(query));
    verify(taskService).countTasks(query);
    assertEquals(42L, result.getCount());
    assertSame(query, counted.getAllValues().get(0));
    assertFalse("the open count is every open task of the board", hasPriorityCondition(counted.getAllValues().get(1)));
    assertEquals("no overdue count beside a priority", 2, counted.getAllValues().size());
    assertEquals(List.of(new ProjectTaskCountModel(8L, "Alpha", 40L, 90L, List.of(), null)), result.getProjects());
  }

  /**
   * Priority NONE selects, among the tasks assigned to the user, those of
   * priority None or saved with none, ANDed onto the assignee-or-coworker
   * scope; without a priority, no priority condition (EXO-94284). Mutants:
   * the NONE branch reduced to the equality or to the null check, the
   * condition always or never applied.
   *
   * @throws Exception never
   */
  @Test
  public void listAssignedTasksWithPriorityNoneSelectsTheTasksWithoutPriority() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listAssignedTasks(null, null, null, null, null, null, null, null, Priority.NONE);

    TaskQuery none = query.getValue();
    assertTrue(scopedToTheUser(none));
    assertTrue(selectsOpenTasksOnly(none));
    assertTrue(conjunction(none).stream()
                                .anyMatch(c -> c instanceof AggregateCondition or && AggregateCondition.OR.equals(or.getType())
                                    && or.getConditions().size() == 2
                                    && isSingle(or.getConditions().get(0), SingleCondition.EQ, "priority", Priority.NONE)
                                    && or.getConditions().get(1) instanceof SingleCondition<?> isNull
                                    && SingleCondition.IS_NULL.equals(isNull.getType()) && "priority".equals(isNull.getField())));
    verify(taskService).countTasks(none);

    tool.listAssignedTasks(null, null, null, null, null, null, null, null, null);
    assertFalse(hasPriorityCondition(query.getValue()));
  }

  /**
   * group_by_project counts board by board with the listing's own query, then
   * the open tasks of every board the user can access, with the same
   * accessibility, then the overdue ones among them: the boards with the most
   * selected tasks first, ties by name ignoring case, each with its open and
   * overdue counts, 0 for a board with none overdue, project_count counting
   * them all and open_project_count the boards holding open tasks, a board
   * with none selected included (EXO-94360). Mutants: the counts read from
   * another query than the listing's, the open count taken from the selected
   * count, the open query restricted to the listed boards or keeping the
   * completed tasks, the order reversed or by id; the overdue count read from
   * the open count, its query not overdue or not scoped to the user.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupedByProjectCountsEachBoardBesideItsOpenTasks() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> listed = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(listed.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    ArgumentCaptor<TaskQuery> counted = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByProject(counted.capture())).thenReturn(List.of(new ProjectTaskCountDto(7L, "Beta", 1L),
                                                                                new ProjectTaskCountDto(9L, "zeta", 3L),
                                                                                new ProjectTaskCountDto(8L, "Alpha", 3L)),
                                                                        List.of(new ProjectTaskCountDto(7L, "Beta", 5L),
                                                                                new ProjectTaskCountDto(8L, "Alpha", 3L),
                                                                                new ProjectTaskCountDto(9L, "zeta", 4L),
                                                                                new ProjectTaskCountDto(10L, "Gamma", 2L)),
                                                                        List.of(new ProjectTaskCountDto(8L, "Alpha", 2L),
                                                                                new ProjectTaskCountDto(9L, "zeta", 1L)));

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null);

    assertSame(listed.getValue(), counted.getAllValues().get(0));
    TaskQuery open = counted.getAllValues().get(1);
    assertFalse("the open tasks are counted on every board",
                conjunction(open).stream().anyMatch(c -> c instanceof SingleCondition<?> in && "status.project.id".equals(in.getField())));
    assertTrue(selectsOpenTasksOnly(open));
    assertTrue(scopedToTheUser(open));
    TaskQuery overdue = counted.getAllValues().get(2);
    assertTrue(scopedToTheUser(overdue));
    assertTrue(selectsOpenTasksOnly(overdue));
    assertTrue("the overdue tasks are those due before today",
               conjunction(overdue).stream().anyMatch(c -> c instanceof SingleCondition<?> lt && "dueDate".equals(lt.getField())
                   && SingleCondition.LT.equals(lt.getType())));
    assertFalse("the overdue tasks are counted on every board",
                conjunction(overdue).stream().anyMatch(c -> c instanceof SingleCondition<?> in && "status.project.id".equals(in.getField())));
    assertEquals(List.of(new ProjectTaskCountModel(8L, "Alpha", 3L, 3L, List.of(), 2L),
                         new ProjectTaskCountModel(9L, "zeta", 3L, 4L, List.of(), 1L),
                         new ProjectTaskCountModel(7L, "Beta", 1L, 5L, List.of(), 0L)),
                 result.getProjects());
    assertEquals(Integer.valueOf(3), result.getProjectCount());
    assertEquals(Integer.valueOf(4), result.getOpenProjectCount());
    assertNull("the managed totals come with managed_only only", result.getOverdueCount());
    assertNull(result.getIdleProjectCount());

    // a listing narrowed by its due date counts the open tasks of every board
    // still, and no overdue ones, which it does not count (EXO-94935)
    TaskCollectionModel narrowed = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, true, null, null, true, null, null, null, null, null, null);
    assertEquals(5, counted.getAllValues().size());
    assertFalse("the open count is every open task of the board the user can access", hasDueDateCondition(counted.getAllValues().get(4)));
    assertNull(narrowed.getProjects().get(0).getOverdueCount());
  }

  /**
   * « Unassigned tasks per board » (EXO-94935): the grouped listing keeps
   * open and completed tasks both, so it splits them as numbers, in its
   * summary and on each board row, from one more count of its own query
   * restricted to the open tasks; its board rows carry no overdue_count,
   * which counts a board's overdue tasks whatever the listing's filters, so
   * not the unassigned ones; with overdue_only, the count and the summary
   * are the overdue unassigned tasks, with no split and still no board's
   * overdue_count. Mutants: the overdue counted beside unassigned_only; the
   * split dropped; the split counted from the boards' open tasks rather than
   * the listing's query; the split counted on an overdue listing.
   *
   * @throws Exception never
   */
  @Test
  public void unassignedTasksPerBoardSayWhatTheyCountWithNoBoardsOverdue() throws Exception { // NOSONAR
    lenient().when(taskService.findLastUpdatedTasks(any(), eq(0), eq(1))).thenReturn(Collections.emptyList());
    lenient().when(taskService.findTasks(any(), eq(0), eq(1))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(28);
    ArgumentCaptor<TaskQuery> counted = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByProject(counted.capture())).thenReturn(List.of(new ProjectTaskCountDto(8L, "Oualid Opportunités (copy)", 28L)),
                                                                        List.of(new ProjectTaskCountDto(8L, "Oualid Opportunités (copy)", 40L)),
                                                                        List.of(new ProjectTaskCountDto(8L, "Oualid Opportunités (copy)", 19L)));

    TaskCollectionModel unassigned = runWithDateFormatMockResult(() -> tool.listTasks(null, null, false, null, 0, 1, null, null, null, null, null, null, null, null, null, null, null, "project", null, null, true, true));

    JsonNode json = JSON.readTree(JSON.writeValueAsString(unassigned));
    JsonNode row = json.path("projects").path(0);
    assertEquals(28, row.path("count").asInt());
    assertEquals(row.toString(), 19, row.path("selected_open_count").asInt());
    assertEquals(row.toString(), 9, row.path("selected_completed_count").asInt());
    assertEquals(40, row.path("open_count").asInt());
    assertTrue(row.toString(), row.path("overdue_count").isMissingNode());
    assertEquals(19, json.path("selected_open_count").asInt());
    assertEquals(9, json.path("selected_completed_count").asInt());
    assertEquals("28 tasks with no assignee: 19 open, 9 completed, in 1 board" + TaskMcpTool.COUNTS_ONLY_SUMMARY, unassigned.getSummary());
    assertEquals(3, counted.getAllValues().size());
    TaskQuery listed = counted.getAllValues().get(0);
    TaskQuery split = counted.getAllValues().get(2);
    assertFalse(selectsOpenTasksOnly(listed));
    assertTrue("the split counts the listing's open tasks", selectsOpenTasksOnly(split));
    assertEquals("the split is the listing's own query, its open tasks only", leafConditions(listed).size() + 1, leafConditions(split).size());

    TaskCollectionModel overdueUnassigned = runWithDateFormatMockResult(() -> tool.listTasks(null, null, false, true, 0, 1, null, null, null, null, null, null, null, null, null, null, null, "project", null, null, true, true));

    assertNull(overdueUnassigned.getProjects().get(0).getOverdueCount());
    assertNull("an overdue listing selects open tasks only", overdueUnassigned.getSelectedOpenCount());
    assertNull(overdueUnassigned.getProjects().get(0).getSelectedOpenCount());
    assertTrue(overdueUnassigned.getSummary(), overdueUnassigned.getSummary().startsWith("28 overdue open tasks with no assignee"));
    assertEquals(5, counted.getAllValues().size());
  }

  /**
   * Without group_by_project, the answer carries no board counts, no summary
   * and no grouped count runs; with it and no board holding a selected task,
   * the counts are empty and the open and overdue tasks are still counted,
   * the open ones for open_project_count, no row carrying an overdue_count
   * (EXO-94360). Mutant: the counts always computed.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksCountsBoardsOnlyWhenAsked() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    TaskCollectionModel plain = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null);
    assertNull(plain.getProjects());
    assertNull(plain.getProjectCount());
    assertNull(plain.getSummary());
    assertNull(plain.getOpenProjectCount());
    verify(taskService, never()).countTasksByProject(any());

    when(taskService.countTasksByProject(any())).thenReturn(List.of());
    TaskCollectionModel none = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null);
    assertEquals(List.of(), none.getProjects());
    assertEquals(Integer.valueOf(0), none.getProjectCount());
    assertEquals(Integer.valueOf(0), none.getOpenProjectCount());
    verify(taskService, times(3)).countTasksByProject(any());
  }

  /**
   * The board counts list {@value TaskMcpTool#MAX_PROJECT_COUNTS} boards at
   * most, those with the most selected tasks, while project_count counts
   * every board and projects_not_listed the boards left out, and the
   * personal tasks are the count less the tasks of every board, the board
   * left out included (EXO-94360). Mutants: the cap dropped, or applied
   * before the sort; the boards left out not counted; the personal tasks
   * counted from the listed boards only.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupedByProjectListsTheBusiestBoardsFirst() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    List<ProjectTaskCountDto> counts = new ArrayList<>();
    for (long id = 1; id <= TaskMcpTool.MAX_PROJECT_COUNTS + 1; id++) {
      counts.add(new ProjectTaskCountDto(id, "Board " + id, id));
    }
    ArgumentCaptor<TaskQuery> counted = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByProject(counted.capture())).thenReturn(counts, List.of());
    long onBoards = counts.stream().mapToLong(ProjectTaskCountDto::getCount).sum();
    when(taskService.countTasks(any())).thenReturn((int) onBoards + 7);

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, true, null, null, true, null, null, null, null, null, null);

    assertEquals(TaskMcpTool.MAX_PROJECT_COUNTS, result.getProjects().size());
    assertEquals(Integer.valueOf(1), result.getProjectsNotListed());
    assertEquals("the board left out holds 1 task, not a personal one", Long.valueOf(7L), result.getPersonalCount());
    assertEquals(TaskMcpTool.MAX_PROJECT_COUNTS + 1L, result.getProjects().get(0).getProjectId());
    assertEquals(2L, result.getProjects().get(TaskMcpTool.MAX_PROJECT_COUNTS - 1).getProjectId());
    assertEquals(0L, result.getProjects().get(0).getOpenCount());
    assertEquals(Integer.valueOf(TaskMcpTool.MAX_PROJECT_COUNTS + 1), result.getProjectCount());
  }

  /**
   * A grouped call with a limit of 1 is made for the counts: it lists no
   * task (EXO-95037) and says no next page remains, though more tasks match; a
   * grouped call reading a real page, and an ungrouped call with a limit of
   * 1, keep has_more and next_offset (EXO-94360). Mutants: has_more left on
   * the counting call; has_more dropped on every grouped call; the rule
   * applied without group_by_project.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupedCountingCallSaysNoNextPage() throws Exception { // NOSONAR
    List<TaskDto> ten = new ArrayList<>();
    for (long id = 1; id <= 10; id++) {
      ten.add(mockTaskWithProject(id, new Date()));
    }
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(1))).thenReturn(ten.subList(0, 1));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(ten);
    when(taskService.countTasks(any())).thenReturn(25);
    when(taskService.countTasksByProject(any())).thenReturn(List.of(new ProjectTaskCountDto(8L, "Alpha", 25L)));

    TaskCollectionModel counting = runWithDateFormatMockResult(() -> tool.listTasks(null, true, false, null, 0, 1, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null));
    assertFalse(counting.isHasMore());
    assertNull(counting.getNextOffset());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(counting));
    assertFalse(json.path("has_more").asBoolean());
    assertFalse(json.has("next_offset"));
    assertEquals(25, json.path("count").asInt());

    TaskCollectionModel page = runWithDateFormatMockResult(() -> tool.listTasks(null, true, false, null, 0, 10, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null));
    assertTrue(page.isHasMore());
    assertEquals(Integer.valueOf(10), page.getNextOffset());

    TaskCollectionModel plain = runWithDateFormatMockResult(() -> tool.listTasks(null, true, false, null, 0, 1, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    assertTrue(plain.isHasMore());
    assertEquals(Integer.valueOf(1), plain.getNextOffset());
  }

  /**
   * A grouped call with a limit of 1, made for the counts, lists no task and
   * its summary says so (EXO-95037): a single task read as a sample of a
   * board was presented as that board's tasks. A grouped call reading a page
   * lists its tasks, and an ungrouped call with a limit of 1 lists its one
   * task, neither saying « counts only ». Mutants: the sample task kept; the
   * summary left as is; the rule applied to an ungrouped call or a grouped
   * page.
   *
   * @throws Exception never
   */
  @Test
  public void aGroupedCountingCallListsNoTaskAndSaysSo() throws Exception { // NOSONAR
    List<TaskDto> ten = new ArrayList<>();
    for (long id = 1; id <= 10; id++) {
      ten.add(mockTaskWithProject(id, new Date()));
    }
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(1))).thenReturn(ten.subList(0, 1));
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(ten);
    when(taskService.countTasks(any())).thenReturn(25);
    when(taskService.countTasksByProject(any())).thenReturn(List.of(new ProjectTaskCountDto(8L, "Alpha", 25L)));

    TaskCollectionModel counting = runWithDateFormatMockResult(() -> tool.listTasks(null, true, false, null, 0, 1, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null));
    assertTrue(counting.getTasks().isEmpty());
    assertTrue(counting.getSummary(), counting.getSummary().endsWith(TaskMcpTool.COUNTS_ONLY_SUMMARY));
    JsonNode json = JSON.readTree(JSON.writeValueAsString(counting));
    assertEquals(0, json.path("tasks").size());
    assertEquals(25, json.path("count").asInt());

    TaskCollectionModel page = runWithDateFormatMockResult(() -> tool.listTasks(null, true, false, null, 0, 10, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null));
    assertEquals(10, page.getTasks().size());
    assertFalse(page.getSummary(), page.getSummary().contains(TaskMcpTool.COUNTS_ONLY_SUMMARY));

    TaskCollectionModel plain = runWithDateFormatMockResult(() -> tool.listTasks(null, true, false, null, 0, 1, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    assertEquals(1, plain.getTasks().size());
    assertNull(plain.getSummary());
  }

  /**
   * The grouped answer counts the boards holding open tasks the user can
   * access, the boards left out of the list and the selected personal tasks,
   * which are on no board: the count less the tasks of every board, those
   * left out of the list included; its summary states them, under the JSON
   * names the definition gives (EXO-94360). Mutants: the personal tasks not
   * subtracted; the open boards counted from the selected ones.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupedCountsOpenBoardsAndPersonalTasks() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(60);
    when(taskService.countTasksByProject(any())).thenReturn(List.of(new ProjectTaskCountDto(8L, "Alpha", 30L),
                                                                    new ProjectTaskCountDto(9L, "Beta", 20L)),
                                                            List.of(new ProjectTaskCountDto(8L, "Alpha", 50L),
                                                                    new ProjectTaskCountDto(9L, "Beta", 20L),
                                                                    new ProjectTaskCountDto(10L, "Gamma", 5L)));

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null, null, null, null, null);

    assertEquals(Long.valueOf(10L), result.getPersonalCount());
    assertEquals(Integer.valueOf(3), result.getOpenProjectCount());
    assertEquals(Integer.valueOf(0), result.getProjectsNotListed());
    assertEquals(50L, result.getProjects().get(0).getOpenCount());
    assertEquals("60 open tasks: 50 in 2 of 3 boards, 10 personal", result.getSummary());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(result));
    assertEquals(3, json.path("open_project_count").asInt());
    assertEquals(0, json.path("projects_not_listed").asInt());
    assertEquals(10, json.path("personal_count").asInt());
    assertEquals(result.getSummary(), json.path("summary").asText());
  }

  /**
   * group_by project splits each listed board by status, from the listing's
   * own query in the engine's column order, under by_status, a default
   * status under its label in the user's language (EXO-94360). Mutants:
   * by_status read from the open tasks' query; a board's statuses given to
   * another board; the stored name given instead of the label.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupByProjectSplitsEachBoardByStatus() throws Exception { // NOSONAR
    stubStatusLabels();
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(4);
    ArgumentCaptor<TaskQuery> boards = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByProject(boards.capture())).thenReturn(List.of(new ProjectTaskCountDto(8L, "Alpha", 3L),
                                                                               new ProjectTaskCountDto(9L, "Beta", 1L)),
                                                                       List.of(new ProjectTaskCountDto(8L, "Alpha", 5L),
                                                                               new ProjectTaskCountDto(9L, "Beta", 2L)),
                                                                       List.of(new ProjectTaskCountDto(8L, "Alpha", 6L)),
                                                                       List.of(new ProjectTaskCountDto(8L, "Alpha", 2L)));
    ArgumentCaptor<TaskQuery> statuses = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByGroup(statuses.capture(), eq(TaskGrouping.PROJECT_STATUS)))
                                                                                      .thenReturn(List.of(new TaskGroupCountDto("21", "ToDo", 8L, "Alpha", 2L),
                                                                                                          new TaskGroupCountDto("22", "Done", 8L, "Alpha", 1L),
                                                                                                          new TaskGroupCountDto("31", "To do", 9L, "Beta", 1L)));

    TaskCollectionModel result = tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "project", null, null, null, null);

    assertEquals(List.of(new StatusTaskCountModel(21L, "To Do", 2L), new StatusTaskCountModel(22L, "Done", 1L)),
                 result.getProjects().get(0).getByStatus());
    assertEquals(List.of(new StatusTaskCountModel(31L, "To do", 1L)), result.getProjects().get(1).getByStatus());
    assertSame(boards.getAllValues().get(0), statuses.getValue());
    assertNull(result.getGroupBy());
    assertEquals("4 tasks: 2 open, 2 completed, in 2 boards", result.getSummary());
    assertEquals(Long.valueOf(2L), result.getProjects().get(0).getSelectedOpenCount());
    assertEquals(Long.valueOf(1L), result.getProjects().get(0).getSelectedCompletedCount());
    assertEquals("a board with none open counts its selected tasks as completed", Long.valueOf(1L), result.getProjects().get(1).getSelectedCompletedCount());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(result));
    assertEquals(21L, json.path("projects").path(0).path("by_status").path(0).path("status_id").asLong());
    assertEquals("Done", json.path("projects").path(0).path("by_status").path(1).path("status").asText());
  }

  /**
   * group_by assignee counts each assignee beside the open tasks of that
   * assignee the user can access, the most first then by name, the tasks
   * with no assignee under « unassigned »; it tells the grouping, the
   * groups' count and none left out, and a call of limit 1 says no next page
   * remains (EXO-94360). Mutants: the open count read from the selected
   * groups; the order by name dropped; the null group's name left null.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupByAssigneeCountsEachAssignee() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(1))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(13);
    List<TaskGroupCountDto> selected = List.of(new TaskGroupCountDto("john", "john", null, null, 5L),
                                               new TaskGroupCountDto(null, null, null, null, 3L),
                                               new TaskGroupCountDto("ann", "ann", null, null, 5L));
    List<TaskGroupCountDto> open = List.of(new TaskGroupCountDto("john", "john", null, null, 7L),
                                           new TaskGroupCountDto(null, null, null, null, 4L));
    // the listing selects completed tasks too; the open tasks' query only open ones
    when(taskService.countTasksByGroup(any(), eq(TaskGrouping.ASSIGNEE))).thenAnswer(invocation -> selectsOpenTasksOnly(invocation.getArgument(0)) ? open
                                                                                                                                                 : selected);

    TaskCollectionModel result = tool.listTasks(null, false, false, null, null, 1, null, null, null, null, null, null, null, null, null, null, null, " Assignee ", null, null, null, null);

    assertEquals(List.of(new TaskGroupCountModel("ann", "ann", null, null, 5L, 0L),
                         new TaskGroupCountModel("john", "john", null, null, 5L, 7L),
                         new TaskGroupCountModel(null, "unassigned", null, null, 3L, 4L)),
                 result.getGroups());
    assertEquals("assignee", result.getGroupBy());
    assertEquals(Integer.valueOf(3), result.getGroupCount());
    assertEquals(Integer.valueOf(0), result.getGroupsNotListed());
    assertNull(result.getProjects());
    assertNull(result.getPersonalCount());
    assertFalse(result.isHasMore());
    assertEquals("13 open and completed tasks, by assignee: ann 5, john 5, unassigned 3" + TaskMcpTool.COUNTS_ONLY_SUMMARY, result.getSummary());
    verify(taskService, never()).countTasksByProject(any());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(result));
    assertEquals("assignee", json.path("group_by").asText());
    assertEquals(7, json.path("groups").path(1).path("open_count").asInt());
    assertEquals(3, json.path("group_count").asInt());
    assertEquals(0, json.path("groups_not_listed").asInt());
    assertFalse(json.path("groups").path(0).has("project_id"));
  }

  /**
   * group_by status counts each status name across boards as the user reads
   * it, a default status in the user's language, the statuses read the same
   * merged into one group; the personal tasks, in no status, counted apart; past the cap, the groups left out
   * are counted and the summary names five groups and the number of the
   * others (EXO-94360). Mutants: the personal tasks not counted; the cap
   * dropped; the summary naming every group.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupByStatusCountsPersonalTasksAndCapsTheGroups() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(13, 300);
    stubStatusLabels();
    when(taskService.countTasksByGroup(any(), eq(TaskGrouping.STATUS))).thenReturn(List.of(new TaskGroupCountDto("ToDo", "ToDo", null, null, 7L),
                                                                                           new TaskGroupCountDto("To Do", "To Do", null, null, 1L),
                                                                                           new TaskGroupCountDto("Done", "Done", null, null, 2L)),
                                                                                   Collections.emptyList());
    TaskCollectionModel result = tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "status", null, null, null, null);

    assertEquals(Long.valueOf(3L), result.getPersonalCount());
    assertEquals("To Do", result.getGroups().get(0).getKey());
    assertEquals(Integer.valueOf(2), result.getGroupCount());
    assertEquals("13 open and completed tasks, by status: To Do 8, Done 2; 3 personal, in no status", result.getSummary());

    List<TaskGroupCountDto> many = new ArrayList<>();
    for (int i = 0; i < 102; i++) {
      many.add(new TaskGroupCountDto("s" + i, String.format("s%03d", i), null, null, 2L));
    }
    when(taskService.countTasksByGroup(any(), eq(TaskGrouping.STATUS))).thenReturn(many, Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(204);
    TaskCollectionModel capped = tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "status", null, null, null, null);

    assertEquals(TaskMcpTool.MAX_PROJECT_COUNTS, capped.getGroups().size());
    assertEquals(Integer.valueOf(102), capped.getGroupCount());
    assertEquals(Integer.valueOf(2), capped.getGroupsNotListed());
    assertEquals("204 open and completed tasks, by status: s000 2, s001 2, s002 2, s003 2, s004 2, and 97 more", capped.getSummary());
  }

  /**
   * group_by label counts each label with its board, and the tasks with no
   * label under « no label », counted on their own on the selected and on the
   * open tasks; the summary says a task is counted under each of its labels
   * (EXO-94360); groups of one count are ordered by name, not by key. Mutants:
   * the no-label group dropped; its open count read from the selected tasks;
   * the order by name dropped.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupByLabelCountsTheTasksWithNoLabel() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    // the listing, its tasks with no label, the open tasks with no label
    when(taskService.countTasks(any())).thenAnswer(invocation -> {
      TaskQuery query = invocation.getArgument(0);
      if (conjunction(query).stream().noneMatch(PredicateCondition.class::isInstance)) {
        return 10;
      }
      return selectsOpenTasksOnly(query) ? 9 : 6;
    });
    when(taskService.countTasksByGroup(any(), eq(TaskGrouping.LABEL))).thenReturn(List.of(new TaskGroupCountDto("5", "urgent", 8L, "Alpha", 4L),
                                                                                          new TaskGroupCountDto("10", "zeta", 8L, "Alpha", 3L),
                                                                                          new TaskGroupCountDto("9", "alpha", 8L, "Alpha", 3L)),
                                                                                  List.of(new TaskGroupCountDto("5", "urgent", 8L, "Alpha", 6L)));

    TaskCollectionModel result = tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "label", null, null, null, null);

    assertEquals(List.of(new TaskGroupCountModel(null, "no label", null, null, 6L, 9L),
                         new TaskGroupCountModel("5", "urgent", 8L, "Alpha", 4L, 6L),
                         new TaskGroupCountModel("9", "alpha", 8L, "Alpha", 3L, 0L),
                         new TaskGroupCountModel("10", "zeta", 8L, "Alpha", 3L, 0L)),
                 result.getGroups());
    assertEquals("10 open and completed tasks, by label: no label 6, urgent (Alpha) 4, alpha (Alpha) 3, zeta (Alpha) 3; a task is counted under each of its labels",
                 result.getSummary());
  }

  /**
   * group_by due_period counts the five periods in their order, each on the
   * listing's query and on the open tasks' query narrowed to its due dates
   * in the user's time zone: before today, today to Sunday, the next week,
   * later, and no due date (EXO-94360). Mutants: a period's bound moved; the
   * open count read from the listing's query; the no-due-date period read
   * as a range.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupByDuePeriodCountsTheFivePeriods() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    ArgumentCaptor<TaskQuery> counted = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasks(counted.capture())).thenReturn(20, 3, 5, 4, 4, 2, 2, 6, 8, 5, 9);

    TaskCollectionModel result = tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "due_period", null, null, null, null);

    assertEquals(List.of(new TaskGroupCountModel("overdue", "due before today", null, null, 3L, 5L),
                         new TaskGroupCountModel("this_week", "due this week", null, null, 4L, 4L),
                         new TaskGroupCountModel("next_week", "due next week", null, null, 2L, 2L),
                         new TaskGroupCountModel("later", "due later", null, null, 6L, 8L),
                         new TaskGroupCountModel("none", "with no due date", null, null, 5L, 9L)),
                 result.getGroups());
    assertEquals(Integer.valueOf(5), result.getGroupCount());
    assertEquals("20 open and completed tasks: 3 due before today, 4 due this week, 2 due next week, 6 due later, 5 with no due date",
                 result.getSummary());
    // TODAY, 8 October 2026, is a Thursday in Paris, at UTC+2
    Date today = Date.from(Instant.parse("2026-10-07T22:00:00Z"));
    Date nextWeek = Date.from(Instant.parse("2026-10-11T22:00:00Z"));
    Date weekAfter = Date.from(Instant.parse("2026-10-18T22:00:00Z"));
    List<TaskQuery> queries = counted.getAllValues();
    assertEquals(11, queries.size());
    TaskQuery listing = queries.get(0);
    for (int open = 0; open < 2; open++) {
      TaskQuery overdue = queries.get(1 + open);
      TaskQuery thisWeek = queries.get(3 + open);
      TaskQuery next = queries.get(5 + open);
      TaskQuery later = queries.get(7 + open);
      TaskQuery none = queries.get(9 + open);
      assertTrue(conjunction(overdue).stream().anyMatch(c -> isSingle(c, SingleCondition.LT, "dueDate", today)));
      assertTrue(conjunction(thisWeek).stream().anyMatch(c -> isSingle(c, SingleCondition.GTE, "dueDate", today)));
      assertTrue(conjunction(thisWeek).stream().anyMatch(c -> isSingle(c, SingleCondition.LT, "dueDate", nextWeek)));
      assertTrue(conjunction(next).stream().anyMatch(c -> isSingle(c, SingleCondition.GTE, "dueDate", nextWeek)));
      assertTrue(conjunction(next).stream().anyMatch(c -> isSingle(c, SingleCondition.LT, "dueDate", weekAfter)));
      assertTrue(conjunction(later).stream().anyMatch(c -> isSingle(c, SingleCondition.GTE, "dueDate", weekAfter)));
      assertFalse(conjunction(later).stream().anyMatch(c -> isSingle(c, SingleCondition.LT, "dueDate", weekAfter)));
      assertTrue(conjunction(none).stream().anyMatch(c -> c instanceof SingleCondition<?> single && "dueDate".equals(single.getField())
          && SingleCondition.IS_NULL.equals(single.getType())));
      for (TaskQuery period : List.of(overdue, thisWeek, next, later, none)) {
        assertEquals(open == 1, selectsOpenTasksOnly(period));
      }
    }
    assertFalse(hasDueDateCondition(listing));
  }

  /**
   * A week runs from Monday to Sunday: the next week starts on the Monday
   * after the day, seven days later on a Monday, the next day on a Sunday
   * (EXO-94360). Mutant: a Monday taken as the start of the next week.
   */
  @Test
  public void nextWeekStartsOnTheMondayAfterTheDay() {
    assertEquals(LocalDate.of(2026, 10, 12), TaskMcpTool.getStartOfNextWeek(LocalDate.of(2026, 10, 8)));
    assertEquals(LocalDate.of(2026, 10, 19), TaskMcpTool.getStartOfNextWeek(LocalDate.of(2026, 10, 12)));
    assertEquals(LocalDate.of(2026, 10, 12), TaskMcpTool.getStartOfNextWeek(LocalDate.of(2026, 10, 11)));
  }

  /**
   * managed_only narrows the listing and the open tasks to the boards whose
   * managers hold one of the user's memberships, the user's name included, as
   * list_projects managed_only reads them: a condition of the query's own
   * conjunction, beside a project given; without managed_only, none. The
   * grouped answer counts the boards the user manages with the managers'
   * query list_projects counts, each board once, and the summary names them
   * among those, the board holding no task apart (EXO-94360). Mutants: the
   * condition left out of the open or overdue tasks' query; the condition
   * given the participants; managed_only taken as true by default; the
   * managed boards not counted, or counted through the participants or the
   * count by memberships, or counted with a project, whose counts reach that
   * board only; the boards with no open task and the overdue totals given
   * without managed_only, with a project or beside another grouping.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksManagedOnlyNarrowsToTheBoardsTheUserManages() throws Exception { // NOSONAR
    ProjectDto viewable = mock(ProjectDto.class);
    when(viewable.canView(any())).thenReturn(true);
    when(projectService.getProject(5L)).thenReturn(viewable);
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    ArgumentCaptor<TaskQuery> counted = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasks(counted.capture())).thenReturn(4);
    ArgumentCaptor<TaskQuery> boards = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByProject(boards.capture())).thenReturn(List.of(new ProjectTaskCountDto(1L, "One", 4L)));
    // two managed boards, one of them holding no task
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(2);

    TaskCollectionModel all = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null, true, null, null, null);
    tool.listTasks(5L, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null, null);
    // grouped on one board: its counts reach that board only, so no total of the managed boards
    TaskCollectionModel oneBoard = tool.listTasks(5L, true, false, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null, true, null, null, null);
    tool.listTasks(null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null);

    assertTrue(narrowedToManagedBoards(counted.getAllValues().get(0)));
    assertTrue(narrowedToManagedBoards(boards.getAllValues().get(0)));
    assertTrue(narrowedToManagedBoards(boards.getAllValues().get(1)));
    assertTrue(selectsOpenTasksOnly(boards.getAllValues().get(1)));
    assertTrue(narrowedToManagedBoards(boards.getAllValues().get(2)));
    assertEquals("4 open tasks in 1 of the 2 boards the user manages; 1 has no open task; 4 overdue in 1 board", all.getSummary());
    assertEquals(Integer.valueOf(2), all.getManagedProjectCount());
    assertEquals(Integer.valueOf(1), all.getIdleProjectCount());
    assertEquals(Long.valueOf(4), all.getOverdueCount());
    assertEquals(Integer.valueOf(1), all.getOverdueProjectCount());
    JsonNode json = JSON.readTree(JSON.writeValueAsString(all));
    assertEquals(2, json.path("managed_project_count").asInt());
    assertEquals(4, json.path("projects").path(0).path("overdue_count").asInt());
    ArgumentCaptor<ProjectQuery> managed = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).countProjects(managed.capture());
    assertManagedQuery(managed.getValue(), null);
    assertTrue(((List<?>) managerCondition(managed.getValue()).getValue()).contains(USER));
    verify(projectService, never()).countProjects(any(), any());
    TaskQuery project = counted.getAllValues().get(1);
    assertTrue(narrowedToManagedBoards(project));
    assertEquals(List.of(5L), project.getProjectIds());
    assertFalse(narrowedToManagedBoards(counted.getAllValues().get(counted.getAllValues().size() - 1)));
    assertNull(oneBoard.getManagedProjectCount());
    assertNull(oneBoard.getIdleProjectCount());
    assertNull(oneBoard.getOverdueCount());
    assertNull(oneBoard.getOverdueProjectCount());
    TaskCollectionModel byAssignee = tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "assignee", true, null, null, null);
    assertEquals(Integer.valueOf(2), byAssignee.getManagedProjectCount());
    assertNull("the overdue tasks are counted board by board only", byAssignee.getOverdueCount());
    assertTrue(byAssignee.getSummary(), byAssignee.getSummary().startsWith("4 open tasks on the 2 boards the user manages"));
    assertEquals("4 open tasks in 1 of 1 board the user manages", oneBoard.getSummary());
    verify(projectService, never()).findProjects(any(ProjectQuery.class), anyInt(), anyInt());
  }

  /**
   * @param query a task query
   * @return whether its own conjunction, not an alternative of its
   *         accessibility, keeps the boards whose managers hold the user's
   *         name
   */
  private static boolean narrowedToManagedBoards(TaskQuery query) {
    return conjunction(query).stream()
                             .anyMatch(c -> c instanceof SingleCondition<?> single && Conditions.TASK_MANAGER.equals(single.getField())
                                 && SingleCondition.IN.equals(single.getType()) && ((List<?>) single.getValue()).contains(USER));
  }

  /**
   * Gives the default statuses their label: « ToDo » reads « To Do ».
   */
  private void stubStatusLabels() {
    ResourceBundle bundle = new ListResourceBundle() {
      @Override
      protected Object[][] getContents() {
        return new Object[][] { { "tasks.status.ToDo", "To Do" } };
      }
    };
    when(resourceBundleService.getResourceBundle(anyString(), any(Locale.class))).thenReturn(bundle);
  }

  /**
   * group_by takes one of its five groupings, ignoring case and blanks; with
   * group_by_project true, only project; no group_by keeps group_by_project's
   * answer, and neither gives an answer with no counts (EXO-94360). Mutants:
   * an unknown grouping let through; group_by_project true with another
   * grouping let through.
   */
  @Test
  public void groupByTakesOneGroupingOnly() {
    assertEquals("project", TaskMcpTool.getGrouping(null, true));
    assertEquals("project", TaskMcpTool.getGrouping(" ", true));
    assertNull(TaskMcpTool.getGrouping(null, null));
    assertNull(TaskMcpTool.getGrouping("", false));
    assertEquals("due_period", TaskMcpTool.getGrouping(" DUE_PERIOD ", null));
    assertEquals("project", TaskMcpTool.getGrouping("project", true));
    IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> TaskMcpTool.getGrouping("week", null));
    assertEquals("group_by must be one of project, assignee, status, label, due_period, not 'week'", unknown.getMessage());
    assertThrows(IllegalArgumentException.class, () -> TaskMcpTool.getGrouping("label", true));
  }

  /**
   * The summary names the filters the call applied: overdue and open before
   * « tasks », the others after it in the tool's order; it counts the boards
   * among the boards holding open tasks only when the listing selects open
   * tasks only, which hide_completed_tasks with completed_since does not, and
   * says when the list leaves boards out (EXO-94360). With managed_only, it
   * counts them among the boards the user manages, and names those holding
   * no open task. Mutants: « overdue » always written; « open » or « of » the
   * open boards when completed tasks are included; a filter left out; the
   * singular lost; « 0 in 0 boards » written when every task is personal;
   * the managed boards' count dropped or read from the open boards; the
   * boards with no open task not named, named at 0, beside completed tasks,
   * or always plural.
   */
  @Test
  public void groupedSummaryNamesTheFiltersApplied() { // NOSONAR
    assertEquals("260 overdue open tasks in 29 of 86 boards",
                 TaskMcpTool.getGroupedSummary(grouped(260, 29, 86, 0, 0),
                                               new TaskMcpTool.ListingFilters(true, false, false, null, null, null, null, null, null, null, null, false, false, false, false)));
    assertEquals("41 open and completed tasks in 12 boards",
                 TaskMcpTool.getGroupedSummary(grouped(41, 12, 86, 0, 0),
                                               new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, false, false, false, false)));
    assertEquals("41 open tasks in 12 of 86 boards the user manages",
                 TaskMcpTool.getGroupedSummary(grouped(41, 12, 86, 0, 0),
                                               new TaskMcpTool.ListingFilters(false, true, false, null, null, null, null, null, null, null, null, true, false, false, false)));
    TaskCollectionModel managed = grouped(1630, 86, 86, 0, 0);
    managed.setManagedProjectCount(105);
    assertEquals("1630 open tasks in 86 of the 105 boards the user manages; 19 have no open task",
                 TaskMcpTool.getGroupedSummary(managed,
                                               new TaskMcpTool.ListingFilters(false, true, false, null, null, null, null, null, null, null, null, true, false, false, false)));
    TaskCollectionModel overdueManaged = grouped(264, 30, 104, 0, 0);
    overdueManaged.setManagedProjectCount(105);
    assertEquals("264 overdue open tasks in 30 of the 105 boards the user manages; 1 has no open task",
                 TaskMcpTool.getGroupedSummary(overdueManaged,
                                               new TaskMcpTool.ListingFilters(true, false, false, null, null, null, null, null, null, null, null, true, false, false, false)));
    TaskCollectionModel noneIdle = grouped(41, 12, 12, 0, 0);
    noneIdle.setManagedProjectCount(12);
    assertEquals("no « 0 have no open task » when every board the user manages holds open tasks",
                 "41 open tasks in 12 of the 12 boards the user manages",
                 TaskMcpTool.getGroupedSummary(noneIdle,
                                               new TaskMcpTool.ListingFilters(false, true, false, null, null, null, null, null, null, null, null, true, false, false, false)));
    TaskCollectionModel everyManaged = grouped(5, 1, 1, 0, 0);
    everyManaged.setManagedProjectCount(1);
    assertEquals("5 open and completed tasks in 1 of the 1 board the user manages",
                 TaskMcpTool.getGroupedSummary(everyManaged,
                                               new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, true, false, false, false)));
    TaskCollectionModel completedManaged = grouped(12, 3, 86, 0, 0);
    completedManaged.setManagedProjectCount(105);
    assertEquals("the boards with no open task are named only beside open tasks",
                 "12 tasks completed since 2026-11-01 in 3 of the 105 boards the user manages",
                 TaskMcpTool.getGroupedSummary(completedManaged,
                                               new TaskMcpTool.ListingFilters(false, true, false, null, null, null, null, null, null, null, "2026-11-01", true, false, false, false)));
    assertEquals("the managed boards are named only with managed_only",
                 "5 open and completed tasks in 1 board",
                 TaskMcpTool.getGroupedSummary(everyManaged,
                                               new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, false, false, false, false)));
    assertEquals("1 open or completed task in 1 board",
                 TaskMcpTool.getGroupedSummary(grouped(1, 1, 5, 0, 0),
                                               new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, false, false, false, false)));
    assertEquals("7 open tasks of priority high whose title contains \"Report\" assigned to john in status 4 due from 2026-11-01 to 2026-11-30"
        + " changed since 2026-11-02 in 3 of 9 boards",
                 TaskMcpTool.getGroupedSummary(grouped(7, 3, 9, 0, 0),
                                               new TaskMcpTool.ListingFilters(false,
                                                                              true,
                                                                              false,
                                                                              "2026-11-01",
                                                                              "2026-11-30",
                                                                              Priority.HIGH,
                                                                              "john",
                                                                              4L,
                                                                              " Report ",
                                                                              "2026-11-02",
                                                                              null, false, false, false, false)));
    assertEquals("2 open tasks with no priority with no due date in 1 of 1 board",
                 TaskMcpTool.getGroupedSummary(grouped(2, 1, 1, 0, 0),
                                               new TaskMcpTool.ListingFilters(false, true, true, null, null, Priority.NONE, null, null, null, null, null, false, false, false, false)));
    assertEquals("3 tasks due until 2026-11-30 completed since 2026-11-01 in 2 boards",
                 TaskMcpTool.getGroupedSummary(grouped(3, 2, 9, 0, 0),
                                               new TaskMcpTool.ListingFilters(false, true, false, null, "2026-11-30", null, null, null, null, null, "2026-11-01", false, false, false, false)));
    assertEquals("3 overdue open tasks, all personal, on no board",
                 TaskMcpTool.getGroupedSummary(grouped(3, 0, 4, 3, 0),
                                               new TaskMcpTool.ListingFilters(true, false, false, null, null, null, null, null, null, null, null, false, false, false, false)));
    assertEquals("150 open and completed tasks in 101 boards; the 100 boards with the most are listed",
                 TaskMcpTool.getGroupedSummary(grouped(150, 101, 120, 0, 1),
                                               new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, false, false, false, false)));
  }

  /**
   * A listing grouped by assignee, status, label or due period on the boards
   * the user manages says how many boards those are, the singular for one,
   * and only « the boards » when their count is missing (EXO-94360).
   * Mutants: the count dropped; the singular lost.
   */
  @Test
  public void groupsSummaryCountsTheBoardsTheUserManages() {
    TaskCollectionModel collection = new TaskCollectionModel(Collections.emptyList(), 0, 1, 13L, false, null);
    collection.setGroupBy("assignee");
    collection.setGroups(List.of(new TaskGroupCountModel("ann", "ann", null, null, 13L, 9L)));
    collection.setGroupCount(1);
    TaskMcpTool.ListingFilters managed = new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, true, false, false, false);

    collection.setManagedProjectCount(105);
    assertEquals("13 open and completed tasks on the 105 boards the user manages, by assignee: ann 13", TaskMcpTool.getGroupsSummary(collection, managed));
    collection.setManagedProjectCount(1);
    assertEquals("13 open and completed tasks on the 1 board the user manages, by assignee: ann 13", TaskMcpTool.getGroupsSummary(collection, managed));
    collection.setManagedProjectCount(null);
    assertEquals("13 open and completed tasks on the boards the user manages, by assignee: ann 13", TaskMcpTool.getGroupsSummary(collection, managed));
  }

  /**
   * On the boards the user manages, the overdue tasks are counted on every
   * board, those past the {@value TaskMcpTool#MAX_PROJECT_COUNTS} listed
   * included, from the query giving each board's overdue_count, so the total
   * is the listed boards' sum whenever none is left out; the boards with no
   * open task are the managed ones less the open ones (EXO-94360). Mutants:
   * the total summed over the listed boards only; the overdue boards read
   * from the open ones; the boards with no open task counted from the
   * boards holding selected tasks.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksManagedOnlyTotalsTheOverdueTasksOfEveryBoard() throws Exception { // NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(1))).thenReturn(Collections.emptyList());
    List<ProjectTaskCountDto> selected = new ArrayList<>();
    List<ProjectTaskCountDto> open = new ArrayList<>();
    List<ProjectTaskCountDto> overdue = new ArrayList<>();
    for (long id = 1; id <= TaskMcpTool.MAX_PROJECT_COUNTS + 1; id++) {
      selected.add(new ProjectTaskCountDto(id, "Board " + id, id + 1));
      open.add(new ProjectTaskCountDto(id, "Board " + id, id + 1));
      if (id % 2 == 1) {
        overdue.add(new ProjectTaskCountDto(id, "Board " + id, 1L));
      }
    }
    open.add(new ProjectTaskCountDto(500L, "Open only", 3L));
    when(taskService.countTasksByProject(any())).thenReturn(selected, open, overdue);
    when(taskService.countTasks(any())).thenReturn(9999);
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(120);

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, 1, null, null, null, null, null, null, null, null, null, null, null, "project", true, null, null, null);

    assertEquals(Integer.valueOf(1), result.getProjectsNotListed());
    long listedOverdue = result.getProjects().stream().mapToLong(ProjectTaskCountModel::getOverdueCount).sum();
    assertEquals("board 1, the fewest tasks, is left out with its overdue task", overdue.size() - 1L, listedOverdue);
    assertEquals(Long.valueOf(overdue.size()), result.getOverdueCount());
    assertEquals(Integer.valueOf(overdue.size()), result.getOverdueProjectCount());
    assertEquals(Integer.valueOf(120 - open.size()), result.getIdleProjectCount());
  }

  /**
   * The summary of a grouping by board on the boards the user manages ends
   * with their overdue tasks and the boards holding them, the singular for
   * one board, « none overdue » for none, and no such clause on a call that
   * selects the overdue tasks only, whose count already says it, nor beside
   * a count another filter narrows, which those overdue tasks ignore
   * (EXO-94360). Mutants: the clause dropped, always plural, written on an
   * overdue call or beside a narrowed count.
   */
  @Test
  public void groupedSummaryOnManagedBoardsEndsWithTheirOverdueTasks() {
    TaskMcpTool.ListingFilters open = new TaskMcpTool.ListingFilters(false, true, false, null, null, null, null, null, null, null, null, true, false, false, false);
    TaskCollectionModel managed = grouped(1630, 86, 86, 0, 0);
    managed.setManagedProjectCount(105);
    managed.setOverdueCount(264L);
    managed.setOverdueProjectCount(30);
    assertEquals("1630 open tasks in 86 of the 105 boards the user manages; 19 have no open task; 264 overdue in 30 boards",
                 TaskMcpTool.getGroupedSummary(managed, open));
    managed.setOverdueCount(2L);
    managed.setOverdueProjectCount(1);
    assertEquals("1630 open tasks in 86 of the 105 boards the user manages; 19 have no open task; 2 overdue in 1 board",
                 TaskMcpTool.getGroupedSummary(managed, open));
    managed.setOverdueCount(0L);
    managed.setOverdueProjectCount(0);
    assertEquals("1630 open tasks in 86 of the 105 boards the user manages; 19 have no open task; none overdue",
                 TaskMcpTool.getGroupedSummary(managed, open));
    assertEquals("the overdue tasks of every managed board are not stated beside a narrowed count",
                 "12 open tasks of priority high in 5 of the 105 boards the user manages; 19 have no open task",
                 TaskMcpTool.getGroupedSummary(narrowed(),
                                               new TaskMcpTool.ListingFilters(false, true, false, null, null, Priority.HIGH, null, null, null, null, null, true, false, false, false)));
    assertEquals("40 open tasks with no due date in 5 of the 105 boards the user manages; 19 have no open task",
                 TaskMcpTool.getGroupedSummary(withCount(narrowed(), 40),
                                               new TaskMcpTool.ListingFilters(false, true, true, null, null, null, null, null, null, null, null, true, false, false, false)));
    TaskCollectionModel overdueCall = grouped(264, 30, 86, 0, 0);
    overdueCall.setManagedProjectCount(105);
    overdueCall.setOverdueCount(264L);
    overdueCall.setOverdueProjectCount(30);
    assertEquals("264 overdue open tasks in 30 of the 105 boards the user manages; 19 have no open task",
                 TaskMcpTool.getGroupedSummary(overdueCall,
                                               new TaskMcpTool.ListingFilters(true, false, false, null, null, null, null, null, null, null, null, true, false, false, false)));
  }

  /**
   * A board line states its own counts before its split by status, and a
   * listing its totals before the lists they sum up, though the mapper, as
   * the one serializing the tool results, writes other keys in alphabetical
   * order: a board's count is read before a status's count, and a status
   * before its own count (EXO-94360). Mutants: any of the orders dropped.
   */
  @Test
  public void countsAreWrittenBeforeTheListsTheySumUp() {
    ProjectTaskCountModel board = new ProjectTaskCountModel(8L, "Alpha", 32L, 40L, List.of(new StatusTaskCountModel(1L, "To Do", 8L)), 3L);
    JsonNode row = JSON.readTree(JSON.writeValueAsString(board));
    assertEquals(List.of("project_id", "project_name", "count", "open_count", "overdue_count", "by_status"), orderedNames(row));
    assertEquals(List.of("status_id", "status", "count"), orderedNames(row.path("by_status").path(0)));

    TaskCollectionModel collection = new TaskCollectionModel(List.of(), 0, 1, 1630L, false, null);
    collection.setManagedProjectCount(105);
    collection.setProjectCount(86);
    collection.setOpenProjectCount(86);
    collection.setIdleProjectCount(19);
    collection.setOverdueCount(264L);
    collection.setOverdueProjectCount(30);
    collection.setPersonalCount(0L);
    collection.setProjectsNotListed(0);
    collection.setSummary("1630 open tasks");
    collection.setProjects(List.of(board));
    assertEquals(List.of("count",
                         "managed_project_count",
                         "project_count",
                         "open_project_count",
                         "idle_project_count",
                         "overdue_count",
                         "overdue_project_count",
                         "personal_count",
                         "projects_not_listed",
                         "summary",
                         "projects",
                         "tasks",
                         "used_offset",
                         "used_limit",
                         "has_more"),
                 orderedNames(JSON.readTree(JSON.writeValueAsString(collection))));
  }

  /**
   * @return a grouped answer on 5 of the 105 boards the user manages, 86 of
   *         which hold open tasks, 264 overdue in 30 of them
   */
  private static TaskCollectionModel narrowed() {
    TaskCollectionModel collection = grouped(12, 5, 86, 0, 0);
    collection.setManagedProjectCount(105);
    collection.setOverdueCount(264L);
    collection.setOverdueProjectCount(30);
    return collection;
  }

  /**
   * @param collection a grouped answer
   * @param count the count of the tasks it selects
   * @return the answer with that count
   */
  private static TaskCollectionModel withCount(TaskCollectionModel collection, long count) {
    collection.setCount(count);
    return collection;
  }

  /**
   * @param node a JSON object
   * @return the names of its fields, in their order
   */
  private static List<String> orderedNames(JsonNode node) {
    List<String> names = new ArrayList<>();
    node.propertyNames().forEach(names::add);
    return names;
  }

  /**
   * @param count the count of the tasks selected
   * @param boards the boards holding them
   * @param openBoards the boards holding open tasks
   * @param personal the personal tasks among them
   * @param notListed the boards left out of the list
   * @return a grouped answer with those counts, listing every other board
   */
  private static TaskCollectionModel grouped(long count, int boards, int openBoards, long personal, int notListed) {
    TaskCollectionModel collection = new TaskCollectionModel(Collections.emptyList(), 0, 1, count, false, null);
    List<ProjectTaskCountModel> listed = new ArrayList<>();
    for (long id = 1; id <= boards - notListed; id++) {
      listed.add(new ProjectTaskCountModel(id, "Board " + id, 1L, 1L));
    }
    collection.setProjects(listed);
    collection.setProjectCount(boards);
    collection.setOpenProjectCount(openBoards);
    collection.setPersonalCount(personal);
    collection.setProjectsNotListed(notListed);
    return collection;
  }

  /**
   * A due range on list_assigned_tasks narrows the user's open tasks to those
   * due in it, both whole days in the user's time zone; without one, no
   * due-date condition. Mutants: the range ignored, or the completed tasks
   * let in.
   */
  @Test
  public void listAssignedTasksDueRangeNarrowsTheOpenTasks() { // NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listAssignedTasks(null, null, null, null, null, null, "2026-11-09", "2026-11-15", null);
    tool.listAssignedTasks(null, null, null, null, null, null, null, null, null);

    TaskQuery ranged = query.getAllValues().get(0);
    List<Condition> conjunction = conjunction(ranged);
    assertTrue(conjunction.stream().anyMatch(c -> isSingle(c, SingleCondition.GTE, "dueDate", Date.from(Instant.parse("2026-11-08T23:00:00Z")))));
    assertTrue(conjunction.stream().anyMatch(c -> isSingle(c, SingleCondition.LT, "dueDate", Date.from(Instant.parse("2026-11-15T23:00:00Z")))));
    assertTrue(selectsOpenTasksOnly(ranged));
    assertTrue(scopedToTheUser(ranged));
    assertFalse(hasDueDateCondition(query.getAllValues().get(1)));
  }

  /**
   * @param query a task query
   * @return whether any of its conditions reads the due date
   */
  private static boolean hasDueDateCondition(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof SingleCondition<?> single && "dueDate".equals(single.getField())) {
        return true;
      }
    }
    return false;
  }

  private class TestableTaskMcpTool extends TaskMcpTool {

    TestableTaskMcpTool() {
      super(projectService,
            statusService,
            taskService,
            commentService,
            labelService,
            spaceService,
            resourceBundleService,
            identityManager,
            userAcl,
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
   * @return whether any of its conditions, at any depth, reads the priority
   */
  private static boolean hasPriorityCondition(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof SingleCondition<?> single && "priority".equals(single.getField())) {
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
   * @return the single conditions of the query, its aggregates flattened
   */
  private static List<Condition> leafConditions(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    List<Condition> leaves = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      if (conditions.get(i) instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (conditions.get(i) != null) {
        leaves.add(conditions.get(i));
      }
    }
    return leaves;
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

  /**
   * completed_only lists the completed tasks alone, whatever their
   * completion date: the listing's query selects completed tasks and never
   * open ones; without it, the query has no completed-only condition
   * (EXO-94538). Mutants: the filter dropped, the filter inverted to the open
   * tasks, the filter applied whatever completed_only says.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksWithCompletedOnlySelectsTheCompletedTasksAlone() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(10))).thenReturn(Collections.emptyList());

    tool.listTasks(null, null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null);
    tool.listTasks(null, null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, null, null);

    assertTrue(selectsCompletedTasksOnly(query.getAllValues().get(0)));
    assertFalse(selectsOpenTasksOnly(query.getAllValues().get(0)));
    assertFalse(selectsCompletedTasksOnly(query.getAllValues().get(1)));
  }

  /**
   * Grouped by project, completed_only counts each board's completed tasks
   * with the listing's own query, beside its open tasks, which the open
   * count still reads; the summary says the tasks are completed, so that
   * « completed tasks per project » takes one call (EXO-94538). Mutants: the
   * filter dropped, the open count reading the completed tasks, the summary
   * word dropped.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupedWithCompletedOnlyCountsEachBoardsCompletedTasks() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> listed = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(listed.capture(), eq(0), eq(1))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(14);
    ArgumentCaptor<TaskQuery> counted = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByProject(counted.capture())).thenReturn(List.of(new ProjectTaskCountDto(7L, "Beta", 4L),
                                                                                new ProjectTaskCountDto(8L, "Alpha", 10L)),
                                                                        List.of(new ProjectTaskCountDto(7L, "Beta", 5L),
                                                                                new ProjectTaskCountDto(8L, "Alpha", 3L)),
                                                                        Collections.emptyList());

    TaskCollectionModel result = tool.listTasks(null, null, false, null, null, 1, null, null, null, null, null, null, null, null, null, true, null, null, null, true, null, null);

    assertSame(listed.getValue(), counted.getAllValues().get(0));
    assertTrue(selectsCompletedTasksOnly(counted.getAllValues().get(0)));
    assertTrue(selectsOpenTasksOnly(counted.getAllValues().get(1)));
    assertFalse(selectsCompletedTasksOnly(counted.getAllValues().get(1)));
    assertEquals(List.of(8L, 7L), result.getProjects().stream().map(ProjectTaskCountModel::getProjectId).toList());
    assertEquals(10L, result.getProjects().get(0).getCount());
    assertTrue(result.getSummary(), result.getSummary().startsWith("14 completed tasks in 2 boards"));
  }

  /**
   * completed_only with overdue_only, which lists open tasks, or with
   * hide_completed_tasks, which leaves the completed tasks out, selects
   * nothing: each pair is refused with a message naming both arguments, and
   * no task is read; completed_only false refuses neither, nor does
   * hide_completed_tasks with completed_since, which ignores it (EXO-94538).
   * Mutants: either check dropped, the check run whatever completed_only
   * says, the completed_since exception dropped.
   *
   * @throws Exception never
   */
  @Test
  public void completedOnlyIsRefusedWithOverdueOnlyOrHiddenCompletedTasks() throws Exception { // NOSONAR
    IllegalArgumentException overdue =
                                      assertThrows(IllegalArgumentException.class,
                                                   () -> tool.listTasks(null, null, false, true, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null));
    assertTrue(overdue.getMessage(), overdue.getMessage().contains("overdue_only") && overdue.getMessage().contains("completed_only"));
    IllegalArgumentException hidden =
                                     assertThrows(IllegalArgumentException.class,
                                                  () -> tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null));
    assertTrue(hidden.getMessage(), hidden.getMessage().contains("hide_completed_tasks") && hidden.getMessage().contains("completed_only"));
    verify(taskService, never()).findLastUpdatedTasks(any(), anyInt(), anyInt());
    verify(taskService, never()).findTasks(any(), anyInt(), anyInt());

    when(taskService.findLastUpdatedTasks(any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    tool.listTasks(null, true, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, null, null);
    verify(taskService).findLastUpdatedTasks(any(), anyInt(), anyInt());
    runWithDateFormatMockResult(() -> tool.listTasks(null, true, false, null, null, null, null, null, "2026-11-01", null, null, null, null, null, null, null, null, null, null, true, null, null));
    verify(taskService, times(2)).findLastUpdatedTasks(any(), anyInt(), anyInt());
  }

  /**
   * A grouped summary of completed_only says « completed », once even with
   * completed_since, whose own words say it; on the boards the user manages
   * it names no idle board and no overdue tasks, which would read as counts
   * of completed tasks (EXO-94538). Mutants: the word dropped or doubled,
   * completed_only left out of the narrowing filters.
   */
  @Test
  public void groupedSummaryOfCompletedOnlySaysCompleted() { // NOSONAR
    assertEquals("14 completed tasks in 2 boards",
                 TaskMcpTool.getGroupedSummary(grouped(14, 2, 86, 0, 0),
                                               new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, false, true, false, false)));
    assertEquals("14 tasks completed since 2026-11-01 in 2 boards",
                 TaskMcpTool.getGroupedSummary(grouped(14, 2, 86, 0, 0),
                                               new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, "2026-11-01", false, true, false, false)));
    TaskCollectionModel managed = grouped(14, 2, 86, 0, 0);
    managed.setManagedProjectCount(105);
    managed.setOverdueCount(264L);
    managed.setOverdueProjectCount(30);
    assertEquals("14 completed tasks in 2 of the 105 boards the user manages",
                 TaskMcpTool.getGroupedSummary(managed,
                                               new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, true, true, false, false)));
  }

  /**
   * unassigned_only selects the tasks with no assignee, whatever their
   * coworkers, in every column: the open state is left as asked, so a card in
   * a Done column still counts; without it, no such condition (EXO-94785).
   * Mutants: the filter dropped, made unconditional, or requiring no coworker
   * either.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksWithUnassignedOnlySelectsTheTasksWithNoAssigneeWhateverTheirCoworkers() throws Exception { // NOSONAR
    ProjectDto board = mock(ProjectDto.class);
    when(board.canView(any())).thenReturn(true);
    when(projectService.getProject(152L)).thenReturn(board);
    ArgumentCaptor<TaskQuery> query = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(query.capture(), eq(0), eq(100))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(32);

    TaskCollectionModel result = tool.listTasks(152L, null, false, null, null, 100, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, null);
    tool.listTasks(152L, null, false, null, null, 100, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, null);

    assertEquals(32, result.getCount());
    List<PredicateCondition<?>> unassigned = predicates(query.getAllValues().get(0));
    assertEquals(1, unassigned.size());
    assertFalse(selectsOpenTasksOnly(query.getAllValues().get(0)));
    CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
    CriteriaQuery<?> criteria = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
    @SuppressWarnings("unchecked")
    Root<Task> root = mock(Root.class, RETURNS_DEEP_STUBS);
    buildPredicate(unassigned.get(0), cb, criteria, root);
    verify(cb).isNull(any());
    verify(criteria, never()).subquery(any(Class.class));
    verify(criteria, never()).subquery(any(jakarta.persistence.metamodel.EntityType.class));
    assertTrue(predicates(query.getAllValues().get(1)).isEmpty());
  }

  /**
   * unassigned_only with an assignee selects nothing, so the pair is refused
   * before any read, naming both arguments (EXO-94785). Mutant: the refusal
   * dropped, or made whatever the assignee.
   *
   * @throws Exception never
   */
  @Test
  public void unassignedOnlyIsRefusedWithAnAssignee() throws Exception { // NOSONAR
    IllegalArgumentException refused =
                                     assertThrows(IllegalArgumentException.class,
                                                  () -> tool.listTasks(null, null, false, null, null, null, null, null, null, "john", null, null, null, null, null, null, null, null, null, null, true, null));
    assertTrue(refused.getMessage(), refused.getMessage().contains("assignee") && refused.getMessage().contains("unassigned_only"));
    verify(taskService, never()).findLastUpdatedTasks(any(), anyInt(), anyInt());

    when(taskService.findLastUpdatedTasks(any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    tool.listTasks(null, null, false, null, null, null, null, null, null, " ", null, null, null, null, null, null, null, null, null, null, true, null);
    tool.listTasks(null, null, false, null, null, null, null, null, null, "john", null, null, null, null, null, null, null, null, null, null, false, null);
    verify(taskService, times(2)).findLastUpdatedTasks(any(), anyInt(), anyInt());
  }

  /**
   * A grouped summary of unassigned_only says « with no assignee », grouped
   * by project or by any other grouping, and counts it among the narrowing
   * filters, so that the boards the user manages name no overdue tasks
   * beside it (EXO-94785). Mutants: the words dropped, unassigned_only left
   * out of the narrowing filters.
   */
  @Test
  public void groupedSummaryOfUnassignedOnlySaysWithNoAssignee() {
    TaskMcpTool.ListingFilters unassigned = new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, false, false, true, false);
    TaskCollectionModel byProject = new TaskCollectionModel();
    byProject.setCount(32);
    byProject.setProjectCount(1);
    byProject.setOpenProjectCount(1);
    byProject.setProjects(List.of());
    assertEquals("32 open and completed tasks with no assignee in 1 board", TaskMcpTool.getGroupedSummary(byProject, unassigned));

    TaskCollectionModel byStatus = new TaskCollectionModel();
    byStatus.setCount(32);
    byStatus.setGroupBy("status");
    byStatus.setGroupCount(1);
    byStatus.setGroups(List.of(new TaskGroupCountModel("Done", "Done", null, null, 8L, 0L)));
    assertEquals("32 open and completed tasks with no assignee, by status: Done 8", TaskMcpTool.getGroupsSummary(byStatus, unassigned));

    TaskMcpTool.ListingFilters managedUnassigned = new TaskMcpTool.ListingFilters(false, false, false, null, null, null, null, null, null, null, null, true, false, true, false);
    TaskCollectionModel managed = new TaskCollectionModel();
    managed.setCount(5);
    managed.setProjectCount(2);
    managed.setOpenProjectCount(2);
    managed.setManagedProjectCount(3);
    managed.setOverdueCount(4L);
    managed.setOverdueProjectCount(1);
    managed.setProjects(List.of());
    assertEquals("5 open and completed tasks with no assignee in 2 of the 3 boards the user manages", TaskMcpTool.getGroupedSummary(managed, managedUnassigned));
  }

  /**
   * boards_only leaves the personal tasks out of the listing and of the open
   * counts beside a grouping by assignee, so that one call counts each
   * person's open tasks across the user's boards; without it, neither query
   * carries the condition (EXO-94747). Mutants: the condition dropped from
   * the listing or from the open counts, inverted, or made unconditional.
   *
   * @throws Exception never
   */
  @Test
  public void listTasksGroupedByAssigneeWithBoardsOnlyLeavesThePersonalTasksOut() throws Exception { // NOSONAR
    ArgumentCaptor<TaskQuery> listed = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.findLastUpdatedTasks(listed.capture(), eq(0), eq(1))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(7);
    ArgumentCaptor<TaskQuery> grouped = ArgumentCaptor.forClass(TaskQuery.class);
    when(taskService.countTasksByGroup(grouped.capture(), eq(TaskGrouping.ASSIGNEE))).thenReturn(List.of(new TaskGroupCountDto("john", "John", null, null, 5L),
                                                                                                         new TaskGroupCountDto(null, null, null, null, 2L)));

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, 1, null, null, null, null, null, null, null, null, null, null, null, "assignee", null, null, null, true);
    tool.listTasks(null, true, false, null, null, 1, null, null, null, null, null, null, null, null, null, null, null, "assignee", null, null, null, false);

    assertTrue(selectsOnBoardsOnly(listed.getAllValues().get(0)));
    assertSame(listed.getAllValues().get(0), grouped.getAllValues().get(0));
    assertTrue(selectsOpenTasksOnly(grouped.getAllValues().get(1)));
    assertTrue(selectsOnBoardsOnly(grouped.getAllValues().get(1)));
    assertFalse(selectsOnBoardsOnly(listed.getAllValues().get(1)));
    assertFalse(selectsOnBoardsOnly(grouped.getAllValues().get(3)));
    assertEquals(List.of("John", "unassigned"), result.getGroups().stream().map(TaskGroupCountModel::getName).toList());
    assertEquals("7 open tasks on boards, by assignee: John 5, unassigned 2" + TaskMcpTool.COUNTS_ONLY_SUMMARY, result.getSummary());
  }

  /**
   * @param query a task query
   * @return whether one of its conditions, at any depth, selects the tasks
   *         with a status, those on a board
   */
  private static boolean selectsOnBoardsOnly(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof SingleCondition<?> single && SingleCondition.NOT_NULL.equals(single.getType())
          && "status".equals(single.getField())) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param query a task query
   * @return its predicate conditions, at any depth
   */
  private static List<PredicateCondition<?>> predicates(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    List<PredicateCondition<?>> predicates = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof PredicateCondition<?> predicate) {
        predicates.add(predicate);
      }
    }
    return predicates;
  }

  /**
   * Builds a predicate condition of tasks on mocked criteria objects.
   *
   * @param condition the condition, on tasks
   * @param cb the criteria builder
   * @param criteria the criteria query
   * @param root the root
   */
  @SuppressWarnings("unchecked")
  private static void buildPredicate(PredicateCondition<?> condition, CriteriaBuilder cb, CriteriaQuery<?> criteria, Root<Task> root) {
    ((PredicateCondition<Task>) condition).getBuilder().build(cb, criteria, root);
  }

  /**
   * @param query a task query
   * @return whether it selects the completed tasks only
   */
  private static boolean selectsCompletedTasksOnly(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    conditions.add(query.getCondition());
    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);
      if (condition instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      } else if (condition instanceof SingleCondition<?> single && SingleCondition.IS_TRUE.equals(single.getType())
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
