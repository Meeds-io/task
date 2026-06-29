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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
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
    tool.setTaskDates(TASK_ID, "2024-01-01", null, null);
  }

  @Test
  public void setTaskDatesShouldUpdateAndPersistTask() throws Exception {// NOSONAR
    TaskDto task = mockTask();
    when(taskService.getTask(TASK_ID)).thenReturn(task);
    when(userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(TASK_ID), USER)).thenReturn(true);
    when(taskService.updateTask(task)).thenReturn(task);
    runWithDateFormatMockResult(() -> tool.setTaskDates(TASK_ID, "2024-01-01", null, "2024-01-05"));
    verify(task).setStartDate(any());
    verify(task).setDueDate(any());
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

  @Test
  public void listAssignedTasksShouldReturnCollection() throws Exception {// NOSONAR
    TaskDto task = mockTask();

    when(taskService.getUncompletedTasks(USER, 5)).thenReturn(Collections.singletonList(task));
    when(taskService.countUncompletedTasks(USER)).thenReturn(1L);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listAssignedTasks(5));

    assertEquals(1, result.getTasks().size());
    assertEquals(0, result.getUsedOffset());
    assertEquals(5, result.getUsedLimit());
    assertEquals(1L, result.getCount());
  }

  @Test
  public void listTasksShouldReturnEmptyCollectionWhenNoTasks() throws Exception {// NOSONAR
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10))).thenReturn(Collections.emptyList());
    when(taskService.countTasks(any())).thenReturn(0);

    TaskCollectionModel result = tool.listTasks(null, false, false, null, null);

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
                                                                                 null,
                                                                                 null));

    assertEquals(TASK_ID, result.getId());
    verify(taskService).createTask(any(TaskDto.class));
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
    LabelDto label = mock(LabelDto.class);

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

  @Test
  public void listTasksWithProjectShouldLoadProjectTasks() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);
    TaskDto task = mockTaskWithProject();

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(true);
    when(taskService.findLastUpdatedTasks(any(), eq(0), eq(10)))
                                                                .thenReturn(Collections.singletonList(task));
    when(taskService.countTasks(any())).thenReturn(1);

    TaskCollectionModel result = runWithDateFormatMockResult(() -> tool.listTasks(PROJECT_ID, true, false, null, null));

    assertEquals(1, result.getTasks().size());
    assertEquals(1L, result.getCount());
    verify(projectService, atLeastOnce()).getProject(PROJECT_ID);
    verify(taskService).findLastUpdatedTasks(any(), eq(0), eq(10));
  }

  @Test(expected = IllegalAccessException.class)
  public void listTasksWithProjectWhenProjectNotViewableShouldThrowException() throws Exception { // NOSONAR
    ProjectDto project = mock(ProjectDto.class);

    when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    when(project.canView(currentIdentity)).thenReturn(false);

    tool.listTasks(PROJECT_ID, false, false, 0, 10);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void listTasksWithProjectWhenProjectServiceThrowsEntityNotFoundShouldThrowObjectNotFound() throws Exception { // NOSONAR
    when(projectService.getProject(PROJECT_ID)).thenThrow(new EntityNotFoundException(PROJECT_ID, ProjectDto.class));

    tool.listTasks(PROJECT_ID, false, false, 0, 10);
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
            favoriteService);
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
  }

  @FunctionalInterface
  interface CheckedRunnable<E extends Throwable> {

    void run() throws E;

  }

}
