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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import java.util.ResourceBundle;
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
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.dao.condition.AggregateCondition;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dao.condition.SingleCondition;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.LabelService;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.service.TaskService;

import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.util.McpToolUtils;
import io.meeds.portal.permlink.service.PermanentLinkService;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import io.meeds.social.space.plugin.SpaceAclPlugin;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.task.mcp.model.ProjectActivityModel;
import io.meeds.task.mcp.model.ProjectCollectionModel;
import io.meeds.task.mcp.model.ProjectLabel;
import io.meeds.task.mcp.model.ProjectModel;
import io.meeds.task.mcp.model.ProjectStatisticsModel;
import io.meeds.task.mcp.model.ProjectStatus;
import io.meeds.task.mcp.model.TaskCollectionModel;
import io.meeds.task.mcp.model.TaskCommentCollectionModel;
import io.meeds.task.mcp.model.TaskModel;
import io.meeds.task.plugin.TaskAclPlugin;

import lombok.SneakyThrows;

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
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(userAcl.getUserIdentity("john")).thenReturn(null);

    tool.assignTask(TASK_ID, OTHER_USER);
  }

  @Test
  public void assignTaskShouldStripMentionPrefixAndUpdateTask() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    Identity johnIdentity = mock(Identity.class);

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.getUserIdentity("john")).thenReturn(johnIdentity);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), "john")).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);

    runWithDateFormatMockResult(() -> tool.assignTask(TASK_ID, OTHER_USER));

    verify(task).setAssignee("john");
    verify(taskService).updateTask(task);
  }

  /**
   * In a call limited to a space, a task is assigned to, or gets as a
   * coworker, a member of that space only: a person who can access the task
   * through the project's participants but isn't a member is refused, and so
   * is anyone when the call's identity can't be read. A call that isn't
   * limited keeps the task's access rule alone.
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
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), OTHER_USER)).thenReturn(true);
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

  @Test
  public void assignTaskToMeShouldAssignCurrentUser() throws Exception {// NOSONAR
    TaskDto task = mockTask();

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.getUserIdentity(USER)).thenReturn(currentIdentity);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
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
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), "john")).thenReturn(true);
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
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(labelService.getLabel(99L)).thenReturn(label);

    tool.addProjectLabelToTask(TASK_ID, 99L);

    verify(labelService).addTaskToLabel(task, 99L);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void addProjectLabelToTaskWhenLabelDoesNotExistShouldThrowException() throws Exception {// NOSONAR
    TaskDto task = mockTask();

    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
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
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
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
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
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
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(null, null, false, true, null, null, null));

    assertEquals(Date.from(Instant.parse("2026-10-08T00:00:00+02:00")), findDueDateBefore(query.getValue()));
    assertTrue(selectsOpenTasksOnly(query.getValue()));
    assertTrue("only the tasks the user can access", scopedToTheUser(query.getValue()));
    assertMostOverdueFirst(query.getValue());
    verify(taskService, never()).findLastUpdatedTasks(any(), anyInt(), anyInt());
    verify(taskService).countTasks(query.getValue());
    assertEquals(1L, result.getCount());
    assertEquals(Integer.valueOf(10), result.getTasks().get(0).getDaysOverdue());
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

    TaskCollectionModel result = tool.listTasks(null, true, false, null, null, null, null);

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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(5, true));

    assertEquals(Date.from(Instant.parse("2026-10-08T00:00:00+02:00")), findDueDateBefore(query.getValue()));
    assertTrue("only the tasks the user is assigned to or a coworker of", scopedToTheUser(query.getValue()));
    assertMostOverdueFirst(query.getValue());
    verify(taskService, never()).getUncompletedTasks(anyString(), anyInt());
    assertEquals(1L, result.getCount());
    assertEquals(Integer.valueOf(1), result.getTasks().get(0).getDaysOverdue());
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

    skippingTool.listAssignedTasks(5, true);

    assertEquals(Date.from(Instant.parse("2018-11-04T03:00:00Z")), findDueDateBefore(query.getValue()));
    assertEquals(1, ZonedDateTime.ofInstant(findDueDateBefore(query.getValue()).toInstant(), saoPaulo).getHour());
  }

  @Test
  public void listAssignedTasksShouldReturnCollection() throws Exception {// NOSONAR
    TaskDto task = mockTask();

    when(taskService.getUncompletedTasks(USER, 5)).thenReturn(Collections.singletonList(task));
    when(taskService.countUncompletedTasks(USER)).thenReturn(1L);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(5, null));

    assertEquals(1, result.getTasks().size());
    assertEquals(0, result.getUsedOffset());
    assertEquals(5, result.getUsedLimit());
    assertEquals(1L, result.getCount());
  }

  @Test
  public void listTasksShouldReturnEmptyCollectionWhenNoTasks() throws Exception {// NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(0);

    TaskCollectionModel result = tool.listTasks(null, false, false, null, null, null, null);

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

  @Test
  public void createTaskInProjectShouldUseDefaultStatusAndCreateTask() throws Exception {// NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    StatusDto status = mock(StatusDto.class);
    TaskDto createdTask = mockTask();

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
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
   * that mentions members only, or no one, is added. A call that isn't
   * limited may mention anyone, as before.
   * Mutants: the mention check removed, or applied to the first mention only.
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
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
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

  @Test
  public void listProjectsShouldUseProjectCountAndReturnCollection() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);

    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(10)))
                                                                            .thenReturn(Collections.singletonList(project));
    when(projectService.countProjects(any(), eq(null))).thenReturn(1);

    when(project.getId()).thenReturn(PROJECT_ID);
    when(project.getName()).thenReturn(PROJECT_NAME);
    when(project.getDescription()).thenReturn("");
    when(project.getManager()).thenReturn(Collections.emptySet());
    when(statusService.getStatuses(PROJECT_ID)).thenReturn(Collections.emptyList());
    when(labelService.findLabelsByProject(PROJECT_ID, currentIdentity, 0, 100))
                                                                               .thenReturn(Collections.emptyList());

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(null, null, null));

    assertEquals(1, result.getProjects().size());
    assertEquals(0, result.getUsedOffset());
    assertEquals(10, result.getUsedLimit());
    assertEquals(1L, result.getCount());
    verify(projectService).countProjects(any(), eq(null));
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null));

    assertEquals(List.of(1L, 4L), result.getProjects().stream().map(ProjectModel::getId).toList());
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
    when(projectService.countProjects(any(), eq(null))).thenReturn(2);

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null));

    assertEquals(List.of(1L, 2L), result.getProjects().stream().map(ProjectModel::getId).toList());
    assertEquals(2L, result.getCount());
    verify(spaceScopeResolver, never()).spaceOfProject(any());
    ArgumentCaptor<List<String>> memberships = ArgumentCaptor.forClass(List.class);
    verify(projectService).findProjects(memberships.capture(), eq(null), any(), eq(0), eq(10));
    assertEquals(List.of("member:/spaces/test"), memberships.getValue());
  }

  /**
   * The page is cut from the projects of the space, after the selection, and
   * the offset and limit given back are the ones used.
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 1, 1));

    assertEquals(List.of(3L), result.getProjects().stream().map(ProjectModel::getId).toList());
    assertEquals(3L, result.getCount());
    assertEquals(1, result.getUsedOffset());
    assertEquals(1, result.getUsedLimit());

    ProjectCollectionModel clamped = runWithDateFormatMockResult(() -> tool.listProjects(10L, -4, -2));
    assertEquals(0, clamped.getProjects().size());
    assertEquals(0, clamped.getUsedOffset());
    assertEquals(0, clamped.getUsedLimit());
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 0, 2));

    assertEquals(List.of(1L, 2L), result.getProjects().stream().map(ProjectModel::getId).toList());
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

    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null));

    assertEquals(List.of(100L, 101L), result.getProjects().stream().map(ProjectModel::getId).toList());
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
    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null));
    assertEquals(0, result.getProjects().size());
    assertEquals(0L, result.getCount());

    when(projectService.findProjects(any(), eq(null), any(), eq(0), eq(TaskMcpTool.SPACE_SCAN_CHUNK)))
                                                                                                      .thenReturn(Collections.emptyList());
    result = runWithDateFormatMockResult(() -> tool.listProjects(10L, null, null));
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
      assertEquals(0L, runWithDateFormatMockResult(() -> tool.listProjects(10L, 5, 20)).getCount());
    }
    ProjectCollectionModel result = runWithDateFormatMockResult(() -> tool.listProjects(10L, 5, 20));

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

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(PROJECT_ID, true, false, null, null, null, null));

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

    tool.listTasks(PROJECT_ID, false, false, null, null, null, "  Live Check 2 ");

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

    tool.listTasks(null, false, false, null, null, null, "Live");
    tool.listTasks(null, false, false, null, null, null, "  ");

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

    tool.listTasks(PROJECT_ID, false, false, null, 0, 10, null);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void listTasksWithProjectWhenProjectServiceThrowsEntityNotFoundShouldThrowObjectNotFound() throws Exception { // NOSONAR
    when(projectService.getProject(PROJECT_ID)).thenThrow(new EntityNotFoundException(PROJECT_ID, ProjectDto.class));

    tool.listTasks(PROJECT_ID, false, false, null, 0, 10, null);
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
    when(task.getId()).thenReturn(TASK_ID);
    when(task.getTitle()).thenReturn("Task title");
    when(task.getDescription()).thenReturn("");
    when(task.getCreatedTime()).thenReturn(new Date());
    when(task.getPriority()).thenReturn(null);
    when(taskService.getTaskLogs(eq(TASK_ID), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    when(commentService.getLastComment(TASK_ID)).thenReturn(null);
    when(commentService.countComments(TASK_ID)).thenReturn(0);
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
