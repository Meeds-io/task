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

import static io.meeds.mcp.server.tool.util.McpToolPluginUtils.getInteger;
import static io.meeds.mcp.server.tool.util.McpToolPluginUtils.transformHtmlContent;
import static io.meeds.mcp.server.tool.util.UserToolUtils.toUserModel;
import static io.meeds.mcp.server.util.McpToolUtils.formatDate;
import static io.meeds.mcp.server.util.McpToolUtils.toDate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.dto.ChangeLogEntry;
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
import org.exoplatform.task.util.ProjectUtil;
import org.exoplatform.task.util.UserUtil;

import io.meeds.mcp.server.plugin.McpToolPlugin;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.space.plugin.SpaceAclPlugin;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;
import io.meeds.task.mcp.model.ProjectActivityModel;
import io.meeds.task.mcp.model.ProjectCollectionModel;
import io.meeds.task.mcp.model.ProjectLabel;
import io.meeds.task.mcp.model.ProjectModel;
import io.meeds.task.mcp.model.ProjectStatus;
import io.meeds.task.mcp.model.TaskChangeLog;
import io.meeds.task.mcp.model.TaskCollectionModel;
import io.meeds.task.mcp.model.TaskCommentCollectionModel;
import io.meeds.task.mcp.model.TaskCommentModel;
import io.meeds.task.mcp.model.TaskModel;
import io.meeds.task.mcp.model.TaskWithChangeLogModel;
import io.meeds.task.plugin.ProjectPermanentLinkPlugin;
import io.meeds.task.plugin.TaskAclPlugin;
import io.meeds.task.plugin.TaskPermanentLinkPlugin;

import lombok.SneakyThrows;

@Service
@Profile("mcp-server")
public class TaskMcpTool implements McpToolPlugin {

  private static final int              MAX_TO_LOAD                     = 100;

  private static final String           MSG_TASK_NOT_EDITABLE           =
                                                              "Task with identifier '%s' is not editable for user '%s'";

  private static final String           MSG_TASK_NOT_ACCESSIBLE         =
                                                                "Task with identifier '%s' is not accessible for user '%s'";

  private static final String           MSG_TASK_NOT_FOUND              = "Task with identifier '%s' is not found";

  private static final String           MSG_LABEL_NOT_FOUND             =
                                                            "Label with identifier '%s' is not found. Available project labels are:%n```json%n%s%n```";

  private static final String           MSG_STATUS_NOT_FOUND            =
                                                             "Status with identifier '%s' is not found. Available project statuses are:%n```json%n%s%n```";

  private static final String           MSG_TASK_PROJECT_NOT_ACCESSIBLE =
                                                                        "Task project with identifier '%s' is not accessible for the user '%s'";

  private static final String           MSG_TASK_PROJECT_NOT_EDITABLE   =
                                                                      "Task project with identifier '%s' is not editable by the user '%s'";

  private static final String           MSG_TASK_PROJECT_NOT_FOUND      = "Task project with identifier '%s' is not found";

  private final ProjectService          projectService;

  private final StatusService           statusService;

  private final TaskService             taskService;

  private final CommentService          commentService;

  private final LabelService            labelService;

  private final SpaceService            spaceService;

  private final IdentityManager         identityManager;

  private final ProfilePropertyService  profilePropertyService;

  private final TranslationService      translationService;

  private final ResourceBundleService   resourceBundleService;

  private final UserACL                 userAcl;

  private final UserPortalConfigService portalConfigService;

  private final PermanentLinkService    permanentLinkService;

  public TaskMcpTool(ProjectService projectService,
                     StatusService statusService,
                     TaskService taskService,
                     CommentService commentService,
                     LabelService labelService,
                     SpaceService spaceService,
                     TranslationService translationService,
                     ResourceBundleService resourceBundleService,
                     IdentityManager identityManager,
                     ProfilePropertyService profilePropertyService,
                     UserACL userAcl,
                     UserPortalConfigService portalConfigService,
                     PermanentLinkService permanentLinkService) {
    this.projectService = projectService;
    this.statusService = statusService;
    this.taskService = taskService;
    this.commentService = commentService;
    this.labelService = labelService;
    this.spaceService = spaceService;
    this.translationService = translationService;
    this.resourceBundleService = resourceBundleService;
    this.identityManager = identityManager;
    this.profilePropertyService = profilePropertyService;
    this.userAcl = userAcl;
    this.portalConfigService = portalConfigService;
    this.permanentLinkService = permanentLinkService;
  }

  public ProjectCollectionModel listProjects(Long spaceId,
                                             Integer offset,
                                             Integer limit) {
    List<String> memberships = getMemberships(spaceId);
    List<ProjectModel> projects = getProjectModels(memberships, offset, limit);
    long count = countProjects(memberships);
    return new ProjectCollectionModel(projects,
                                      getInteger(offset, DEFAULT_OFFSET),
                                      getInteger(limit, DEFAULT_LIMIT),
                                      count);
  }

  public TaskCollectionModel listAssignedTasks(Integer limit) {
    List<TaskModel> taskModels = getAssignedTaskModels(getInteger(limit, DEFAULT_LIMIT));
    long count = countAssignedTaskModels();
    return new TaskCollectionModel(taskModels,
                                   0,
                                   getInteger(limit, DEFAULT_LIMIT),
                                   count);
  }

  public TaskCollectionModel listTasks(Long projectId,
                                       Boolean hideCompletedTasks,
                                       Boolean includeChangeLog,
                                       Integer offset,
                                       Integer limit) throws IllegalAccessException, ObjectNotFoundException {
    List<? extends TaskModel> taskModels = getTaskModels(projectId,
                                                         offset,
                                                         limit,
                                                         hideCompletedTasks != null && hideCompletedTasks.booleanValue(),
                                                         includeChangeLog != null && includeChangeLog.booleanValue());
    long count = countTaskModels(projectId, false);
    return new TaskCollectionModel(taskModels,
                                   getInteger(offset, DEFAULT_OFFSET),
                                   getInteger(limit, DEFAULT_LIMIT),
                                   count);
  }

  public ProjectModel getProjectById(Long projectId) throws ObjectNotFoundException, IllegalAccessException {
    if (projectId == null) {
      throw new IllegalArgumentException("Project identifier is mandatory");
    }
    String currentUsername = getCurrentUserName();
    ProjectDto project = getProject(projectId);
    if (project == null) {
      throw new ObjectNotFoundException(MSG_TASK_PROJECT_NOT_FOUND.formatted(projectId));
    } else if (!project.canView(getCurrentUserAclIdentity())) {
      throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId,
                                                                                 currentUsername));
    }
    return toProjectModel(project);
  }

  public Long getProjectIdByTaskId(Long taskId) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTaskDetailById(taskId);
    return getProjectId(task);
  }

  public TaskModel getTaskById(Long taskId) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTaskDetailById(taskId);
    return toTaskModel(task);
  }

  public TaskCommentCollectionModel listTaskCommentsById(Long taskId,
                                                         Integer offset,
                                                         Integer limit) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTaskDetailById(taskId);
    List<TaskCommentModel> taskComments = getTaskCommentModels(task, offset, limit);
    int count = countTaskComments(task.getId());
    return new TaskCommentCollectionModel(taskComments,
                                          getInteger(offset, DEFAULT_OFFSET),
                                          getInteger(limit, DEFAULT_LIMIT),
                                          count);
  }

  public ProjectModel createProjectInSpace(Long spaceId,
                                           String title,
                                           String description,
                                           String dueDate) throws ObjectNotFoundException, IllegalAccessException {
    Space space = spaceId != null && spaceId > 0 ? spaceService.getSpaceById(spaceId) : null;
    if (space == null) {
      throw new ObjectNotFoundException("Space with Id '%s' doesn't exist".formatted(spaceId));
    } else if (!userAcl.hasPermission(SpaceAclPlugin.OBJECT_TYPE,
                                      String.valueOf(spaceId),
                                      SpaceAclPlugin.REDACT_PERMISSION_TYPE,
                                      getCurrentUserAclIdentity())) {
      throw new IllegalAccessException("User is not allowed to create a Task Project in Space with Id '%s'".formatted(spaceId));
    }
    Set<String> managers = Collections.singleton("%s:%s".formatted(SpaceUtils.MANAGER, space.getGroupId()));
    Set<String> members = Collections.singleton("%s:%s".formatted(SpaceUtils.MEMBER, space.getGroupId()));
    ProjectDto project = ProjectUtil.newProjectInstanceDto(title,
                                                           description,
                                                           managers,
                                                           members);
    if (StringUtils.isNotBlank(dueDate)) {
      project.setDueDate(toDate(dueDate));
    }
    project = projectService.createProject(project);
    statusService.createInitialStatuses(project);
    return toProjectModel(project);
  }

  public TaskModel createTaskInProject(long projectId, // NOSONAR
                                       String title,
                                       String description,
                                       String assignee,
                                       Set<String> coworkers,
                                       String startDate,
                                       String endDate,
                                       String dueDate,
                                       Priority priority,
                                       Long statusId) throws ObjectNotFoundException, IllegalAccessException {
    ProjectDto project = checkCanCreateInProject(projectId);
    Identity aclIdentity = getCurrentUserAclIdentity();
    TaskDto task = new TaskDto();
    task.setTitle(title);
    task.setDescription(description);
    task.setAssignee(assignee);
    task.setCoworker(coworkers);
    task.setStartDate(toDate(startDate));
    task.setEndDate(toDate(endDate));
    task.setDueDate(toDate(dueDate));
    task.setPriority(priority == null ? Priority.NONE : priority);
    task.setCreatedBy(aclIdentity.getUserId());
    task.setCreatedTime(new Date());
    if (statusId == null) {
      try {
        task.setStatus(statusService.getDefaultStatus(projectId));
      } catch (Exception e) {
        List<StatusDto> statuses = statusService.getStatuses(projectId);
        if (CollectionUtils.isEmpty(statuses)) {
          statusService.createInitialStatuses(project);
          RequestLifeCycle.restartTransaction();
        }
        task.setStatus(statusService.getDefaultStatus(projectId));
      }
    } else {
      task.setStatus(statusService.getStatuses(projectId)
                                  .stream()
                                  .filter(s -> s.getId().longValue() == statusId.longValue())
                                  .findFirst()
                                  .orElseThrow(() -> new IllegalStateException("Task Status with id '%s' doesn't exist in project with id '%s'".formatted(statusId,
                                                                                                                                                          projectId))));
    }
    task = taskService.createTask(task);
    return toTaskModel(task);
  }

  public TaskModel createPersonalTask(String title, // NOSONAR
                                      String description,
                                      String assignee,
                                      Set<String> coworkers,
                                      String startDate,
                                      String endDate,
                                      String dueDate,
                                      Priority priority,
                                      Long statusId) {
    Identity aclIdentity = getCurrentUserAclIdentity();
    TaskDto task = new TaskDto();
    task.setTitle(title);
    task.setDescription(description);
    task.setAssignee(assignee);
    task.setCoworker(coworkers);
    task.setStartDate(toDate(startDate));
    task.setEndDate(toDate(endDate));
    task.setDueDate(toDate(dueDate));
    task.setPriority(priority == null ? Priority.NONE : priority);
    task.setCreatedBy(aclIdentity.getUserId());
    task.setCreatedTime(new Date());
    if (statusId != null) {
      task.setStatus(statusService.getStatus(statusId));
    }
    task = taskService.createTask(task);
    return toTaskModel(task);
  }

  @SneakyThrows
  public TaskCommentModel addTaskComment(long taskId, String text) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId,
                                                                         getCurrentUserName()));
    }
    CommentDto taskComment = commentService.addComment(task, getCurrentUserName(), text);
    return toTaskCommentModel(taskComment, taskId, getUrl(task));
  }

  public TaskModel updateTaskTitle(long taskId, String title) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_EDITABLE.formatted(taskId,
                                                                       getCurrentUserName()));
    }
    task.setTitle(title);
    task = taskService.updateTask(task);
    return toTaskModel(task);
  }

  public TaskModel updateTaskDescription(long taskId, String description) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_EDITABLE.formatted(taskId,
                                                                       getCurrentUserName()));
    }
    task.setDescription(description);
    task = taskService.updateTask(task);
    return toTaskModel(task);
  }

  public TaskModel assignTaskToMe(long taskId) throws ObjectNotFoundException, IllegalAccessException {
    return assignTask(taskId, getCurrentUserName());
  }

  public TaskModel assignTask(long taskId, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("Parameter 'username' is mandatory");
    } else {
      username = username.startsWith("@") ? username.substring(1) : username;
    }
    TaskDto task = getTask(taskId);
    Identity userAclIdentity = userAcl.getUserIdentity(username);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, username));
    } else if (userAclIdentity == null) {
      throw new ObjectNotFoundException("User with login '%s' wasn't found");
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), username)) {
      throw new IllegalAccessException(MSG_TASK_NOT_EDITABLE.formatted(taskId, username));
    }
    task.setAssignee(username);
    task = taskService.updateTask(task);
    return toTaskModel(task);
  }

  public TaskModel addTaskCoworker(long taskId, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("Parameter 'username' is mandatory");
    } else {
      username = username.startsWith("@") ? username.substring(1) : username;
    }
    TaskDto task = getTask(taskId);
    Identity userAclIdentity = userAcl.getUserIdentity(username);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, username));
    } else if (userAclIdentity == null) {
      throw new ObjectNotFoundException("User with login '%s' wasn't found");
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), username)) {
      throw new IllegalAccessException(MSG_TASK_NOT_EDITABLE.formatted(taskId, username));
    }
    if (CollectionUtils.isEmpty(task.getCoworker())) {
      task.setCoworker(Collections.singleton(username));
    } else {
      task.setCoworker(new HashSet<>(task.getCoworker()));
      task.getCoworker().add(username);
    }
    task = taskService.updateTask(task);
    return toTaskModel(task);
  }

  @SneakyThrows
  public List<ProjectLabel> listProjectLabels(Long projectId) throws IllegalAccessException {
    ProjectDto project = projectService.getProject(projectId);
    if (project == null || !project.canView(getCurrentUserAclIdentity())) {
      throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId,
                                                                                 getCurrentUserName()));
    }
    return getProjectLabels(projectId);
  }

  @SneakyThrows
  public List<ProjectStatus> listProjectStatuses(Long projectId) throws IllegalAccessException {
    ProjectDto project = projectService.getProject(projectId);
    if (project == null || !project.canView(getCurrentUserAclIdentity())) {
      throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId,
                                                                                 getCurrentUserName()));
    }
    return getProjectStatuses(projectId);
  }

  @SneakyThrows
  public ProjectLabel createProjectLabel(Long projectId, String labelName) throws IllegalAccessException {
    ProjectDto project = projectService.getProject(projectId);
    if (project == null || !project.canEdit(getCurrentUserAclIdentity())) {
      throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_EDITABLE.formatted(projectId,
                                                                               getCurrentUserName()));
    }
    List<LabelDto> labels = labelService.findLabelsByProject(projectId, getCurrentUserAclIdentity(), 0, MAX_TO_LOAD);
    LabelDto label;
    if (CollectionUtils.isNotEmpty(labels)) {
      label = labels.stream()
                    .filter(l -> StringUtils.equals(l.getName(), labelName))
                    .findFirst()
                    .orElse(null);
      if (label != null) {
        return new ProjectLabel(label.getId(), label.getName());
      }
    }
    label = new LabelDto();
    label.setName(labelName);
    label.setProject(project);
    label.setUsername(getCurrentUserName());
    label = labelService.createLabel(label);
    return new ProjectLabel(label.getId(), label.getName());
  }

  @SneakyThrows
  public void addProjectLabelToTask(Long taskId, Long labelId) throws IllegalAccessException, ObjectNotFoundException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, getCurrentUserName()));
    }
    LabelDto label = labelService.getLabel(labelId);
    if (label == null) {
      throwMissingLabel(labelId,
                        getProjectId(task));
    }
    labelService.addTaskToLabel(task, labelId);
  }

  @SneakyThrows
  public void removeProjectLabelFromTask(Long taskId, Long labelId) throws IllegalAccessException, ObjectNotFoundException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, getCurrentUserName()));
    }
    LabelDto label = labelService.getLabel(labelId);
    if (label == null) {
      throwMissingLabel(labelId,
                        getProjectId(task));
    }
    labelService.removeTaskFromLabel(task, labelId);
  }

  @SneakyThrows
  public void updateTaskStatus(Long taskId, Long statusId) throws IllegalAccessException, ObjectNotFoundException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, getCurrentUserName()));
    }
    StatusDto status = statusService.getStatus(statusId);
    if (status == null || status.getProject().getId() != getProjectId(task)) {
      throw new ObjectNotFoundException(MSG_STATUS_NOT_FOUND.formatted(statusId,
                                                                       JsonUtils.toJsonString(getProjectStatuses(getProjectId(task)))));
    }
    task.setStatus(status);
    taskService.updateTask(task);
  }

  public ProjectActivityModel listProjectActivitySince(Long projectId, Integer days) throws IllegalAccessException,
                                                                                     ObjectNotFoundException {
    long count = countTaskModels(projectId, false);
    if (count == 0) {
      return new ProjectActivityModel(Collections.emptyList(), Collections.emptyList());
    } else {
      List<TaskWithChangeLogModel> updatedTasks = new ArrayList<>();
      List<TaskWithChangeLogModel> uncompletedTasks = new ArrayList<>();

      Instant sinceInstant = LocalDate.now()
                                      .minusDays(days)
                                      .atStartOfDay(ZoneId.systemDefault())
                                      .toInstant();

      int offset = 0;
      int limit = 10;
      do {
        List<TaskWithChangeLogModel> taskModels = getTasks(projectId, offset, limit, false).stream()
                                                                                           .filter(t -> Instant.ofEpochMilli(getLastUpdateDate(t).getTime())
                                                                                                               .isAfter(sinceInstant))
                                                                                           .map(t -> toTaskWithChangeLogModel(t,
                                                                                                                              sinceInstant))
                                                                                           .toList();
        updatedTasks.addAll(taskModels);
        if (taskModels.size() < limit) {
          break;
        } else {
          offset += limit;
        }
      } while (offset < count);

      count = countTaskModels(projectId, false);
      offset = 0;
      limit = 10;
      do {
        getTasks(projectId, offset, limit, true).stream()
                                                .filter(t -> Instant.ofEpochMilli(getLastUpdateDate(t).getTime())
                                                                    .isBefore(sinceInstant))
                                                .map(t -> toTaskWithChangeLogModel(t, sinceInstant))
                                                .forEach(uncompletedTasks::add);
        offset += limit;
      } while (offset < count);
      return new ProjectActivityModel(updatedTasks, uncompletedTasks);
    }
  }

  private long countProjects(List<String> memberships) {
    return projectService.countProjects(memberships, null);
  }

  private List<ProjectModel> getProjectModels(List<String> memberships, Integer offset, Integer limit) {
    List<ProjectDto> projects = projectService.findProjects(memberships,
                                                            null,
                                                            new OrderBy.DESC("lastModifiedDate"),
                                                            getInteger(offset, DEFAULT_OFFSET),
                                                            getInteger(limit, DEFAULT_LIMIT));
    if (CollectionUtils.isEmpty(projects)) {
      return Collections.emptyList();
    } else {
      return projects.stream()
                     .map(this::toProjectModel)
                     .toList();
    }
  }

  @SneakyThrows
  private List<? extends TaskModel> getTaskModels(Long projectId,
                                                  Integer offset,
                                                  Integer limit,
                                                  boolean uncompleteOnly,
                                                  boolean includeChangeLog) throws IllegalAccessException,
                                                                            ObjectNotFoundException {
    List<TaskDto> tasks = getTasks(projectId, offset, limit, uncompleteOnly);
    if (CollectionUtils.isEmpty(tasks)) {
      return Collections.emptyList();
    } else if (includeChangeLog) {
      return tasks.stream()
                  .map(this::toTaskWithChangeLogModel)
                  .toList();
    } else {
      return tasks.stream()
                  .map(this::toTaskModel)
                  .toList();
    }
  }

  private List<TaskDto> getTasks(Long projectId,
                                 Integer offset,
                                 Integer limit,
                                 boolean uncompleteOnly) throws IllegalAccessException,
                                                         ObjectNotFoundException {
    Identity aclIdentity = getCurrentUserAclIdentity();
    List<TaskDto> tasks;
    try {
      if (projectId != null && projectId > 0) {
        ProjectDto project = projectService.getProject(projectId);
        if (project == null || !project.canView(aclIdentity)) {
          throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId,
                                                                                     getCurrentUserName()));
        } else {
          TaskQuery taskQuery = new TaskQuery();
          taskQuery.setProjectIds(Collections.singletonList(projectId));
          if (uncompleteOnly) {
            taskQuery.setCompleted(false);
          }
          tasks = taskService.findLastUpdatedTasks(taskQuery,
                                                   getInteger(offset, DEFAULT_OFFSET),
                                                   getInteger(limit, DEFAULT_LIMIT));
        }
      } else {
        TaskQuery taskQuery = new TaskQuery();
        taskQuery.setAccessible(aclIdentity);
        if (uncompleteOnly) {
          taskQuery.setCompleted(false);
        }
        tasks = taskService.findLastUpdatedTasks(taskQuery,
                                                 getInteger(offset, DEFAULT_OFFSET),
                                                 getInteger(limit, DEFAULT_LIMIT));

      }
    } catch (EntityNotFoundException e) {
      throw new ObjectNotFoundException(MSG_TASK_PROJECT_NOT_FOUND.formatted(projectId));
    }
    return tasks;
  }

  private List<TaskCommentModel> getTaskCommentModels(TaskDto task, Integer offset, Integer limit) {
    List<CommentDto> comments = commentService.getCommentsWithSubs(task.getId(),
                                                                   getInteger(offset, DEFAULT_OFFSET),
                                                                   getInteger(limit, DEFAULT_LIMIT));

    if (CollectionUtils.isNotEmpty(comments)) {
      String url = getUrl(task);
      return comments.stream()
                     .map(c -> toTaskCommentModels(c, task.getId(), url))
                     .flatMap(List::stream)
                     .toList();
    } else {
      return Collections.emptyList();
    }
  }

  private List<TaskModel> getAssignedTaskModels(Integer limit) {
    List<TaskDto> tasks = taskService.getUncompletedTasks(getCurrentUserName(), getInteger(limit, DEFAULT_LIMIT));
    if (CollectionUtils.isEmpty(tasks)) {
      return Collections.emptyList();
    } else {
      return tasks.stream()
                  .map(this::toTaskModel)
                  .toList();
    }
  }

  private long countAssignedTaskModels() {
    String currentUserName = getCurrentUserName();
    return taskService.countUncompletedTasks(currentUserName);
  }

  @SneakyThrows
  private long countTaskModels(Long projectId, boolean uncompleteOnly) throws IllegalAccessException {
    if (projectId == null || projectId == 0) {
      TaskQuery taskQuery = new TaskQuery();
      taskQuery.setAccessible(getCurrentUserAclIdentity());
      if (uncompleteOnly) {
        taskQuery.setCompleted(false);
      }
      return taskService.countTasks(taskQuery);
    } else {
      ProjectDto project = projectService.getProject(projectId);
      if (project == null || !project.canView(getCurrentUserAclIdentity())) {
        throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId,
                                                                                   getCurrentUserName()));
      } else {
        TaskQuery taskQuery = new TaskQuery();
        taskQuery.setProjectIds(Collections.singletonList(projectId));
        if (uncompleteOnly) {
          taskQuery.setCompleted(false);
        }
        return taskService.countTasks(taskQuery);
      }
    }
  }

  private int countTaskComments(long taskId) {
    return commentService.countCommentsWithSubs(taskId);
  }

  private TaskDto getTask(Long taskId) {
    try {
      return taskService.getTask(taskId);
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  private ProjectDto getProject(Long projectId) {
    try {
      return projectService.getProject(projectId);
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  private ProjectModel toProjectModel(ProjectDto project) {
    if (project == null) {
      return null;
    }
    org.exoplatform.services.security.Identity aclIdentity = getCurrentUserAclIdentity();
    Locale locale = getCurrentUserLocale();
    Space space = getProjectSpace(project);
    return new ProjectModel(project.getId(),
                            project.getName(),
                            getUrl(project),
                            transformHtmlContent(project.getDescription(), aclIdentity, locale),
                            getProjectStatuses(project.getId()),
                            getProjectLabels(project.getId()),
                            space == null ? null : space.getSpaceId(),
                            formatDate(project.getDueDate()));
  }

  private List<ProjectStatus> getProjectStatuses(Long projectId) {
    List<StatusDto> statuses = statusService.getStatuses(projectId);
    return CollectionUtils.isEmpty(statuses) ? Collections.emptyList() :
                                             statuses.stream()
                                                     .map(this::toProjectStatusModel)
                                                     .toList();
  }

  private ProjectStatus toProjectStatusModel(StatusDto status) {
    if (status == null) {
      return null;
    }
    return new ProjectStatus(status.getId(),
                             status.getName(),
                             status.getRank() == null ? 0 : status.getRank());
  }

  @SneakyThrows
  private TaskWithChangeLogModel toTaskWithChangeLogModel(TaskDto task) {
    return toTaskWithChangeLogModel(task, null);
  }

  @SneakyThrows
  private TaskWithChangeLogModel toTaskWithChangeLogModel(TaskDto task, Instant since) {
    TaskModel taskModel = toTaskModel(task);
    List<TaskChangeLog> taskLogs = taskService.getTaskLogs(task.getId(), 0, MAX_TO_LOAD) // NOSONAR
                                              .stream()
                                              .filter(l -> since == null
                                                           || Instant.ofEpochMilli(l.getCreatedTime())
                                                                     .isAfter(since))
                                              .map(this::getTaskChangeLog)
                                              .toList();
    int commentsCount = commentService.countComments(task.getId());
    if (commentsCount > 0) {
      List<TaskChangeLog> commentTaskLogs = commentService.getComments(task.getId(), 0, commentsCount)
                                                          .stream()
                                                          .filter(c -> since == null
                                                                       || Instant.ofEpochMilli(c.getCreatedTime().getTime())
                                                                                 .isAfter(since))
                                                          .map(c -> new TaskChangeLog("commented",
                                                                                      "commented",
                                                                                      getUserModel(c.getAuthor()),
                                                                                      null,
                                                                                      formatDate(c.getCreatedTime())))
                                                          .toList();
      taskLogs = new ArrayList<>(taskLogs);
      taskLogs.addAll(commentTaskLogs);
      Collections.sort(taskLogs, (l1, l2) -> l1.getActionDate().compareTo(l2.getActionDate()));
    }
    return new TaskWithChangeLogModel(taskModel, taskLogs);
  }

  private TaskModel toTaskModel(TaskDto task) {
    if (task == null) {
      return null;
    }
    org.exoplatform.services.security.Identity aclIdentity = getCurrentUserAclIdentity();
    Locale locale = getCurrentUserLocale();
    StatusDto status = task.getStatus();
    ProjectDto project = status != null && status.getProject() != null ? status.getProject() : null;
    int commentsCount = commentService.countComments(task.getId());
    String assignee = task.getAssignee();
    Space space = project == null ? null : getProjectSpace(project);
    Long projectId = project == null ? null : project.getId();
    return new TaskModel(task.getId(),
                         task.getTitle(),
                         transformHtmlContent(task.getDescription(), aclIdentity, locale),
                         getUrl(task),
                         getUserModel(assignee),
                         CollectionUtils.isEmpty(task.getCoworker()) ? null :
                                                                     task.getCoworker()
                                                                         .stream()
                                                                         .map(this::getUserModel)
                                                                         .toList(),
                         formatDate(task.getStartDate()),
                         formatDate(task.getEndDate()),
                         formatDate(task.getDueDate()),
                         formatDate(task.getCreatedTime()),
                         formatDate(getLastUpdateDate(task)),
                         task.isCompleted(),
                         task.getPriority(),
                         getTaskWorkLoad(task),
                         commentsCount,
                         projectId,
                         project == null ? null : project.getName(),
                         getTaskStatus(status),
                         project == null ? null : getTaskLabels(task, projectId, aclIdentity),
                         space == null ? null : space.getSpaceId());
  }

  private Integer getTaskWorkLoad(TaskDto task) {
    Date startDate = task.getStartDate();
    Date dueDate = task.getDueDate() == null ? task.getEndDate() : task.getDueDate();
    if (startDate == null || dueDate == null) {
      return null;
    } else {
      return 1 + ((int) Duration.between(Instant.ofEpochMilli(startDate.getTime()),
                                         Instant.ofEpochMilli(dueDate.getTime()))
                                .toDays());
    }
  }

  private ProjectStatus getTaskStatus(StatusDto status) {
    if (status == null) {
      return null;
    } else {
      Long id = status.getId();
      String name = status.getName();
      int rank = status.getRank() == null ? 0 : status.getRank();
      ResourceBundle resourceBundle = resourceBundleService.getResourceBundle("locale.portlet.taskManagement",
                                                                              getCurrentUserLocale());
      if (resourceBundle.containsKey("tasks.status.%s".formatted(name))) {
        name = resourceBundle.getString("tasks.status.%s".formatted(name));
      }
      return new ProjectStatus(id, name, rank);
    }
  }

  private TaskChangeLog getTaskChangeLog(ChangeLogEntry changeLog) {
    return new TaskChangeLog(changeLog.getActionName(),
                             getActionLabel(changeLog.getActionName()),
                             getUserModel(changeLog.getAuthor()),
                             getUserModel(changeLog.getTarget()),
                             formatDate(changeLog.getCreatedTime()));
  }

  private String getActionLabel(String actionName) {
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle("locale.portlet.taskManagement",
                                                                            getCurrentUserLocale());
    if (resourceBundle.containsKey("log.%s".formatted(actionName))) {
      return resourceBundle.getString("log.%s".formatted(actionName));
    } else {
      return actionName;
    }
  }

  private List<ProjectLabel> getTaskLabels(TaskDto task,
                                           Long projectId,
                                           org.exoplatform.services.security.Identity aclIdentity) {
    List<LabelDto> labels = labelService.findLabelsByTask(task,
                                                          projectId,
                                                          aclIdentity,
                                                          0,
                                                          MAX_TO_LOAD);
    if (CollectionUtils.isEmpty(labels)) {
      return Collections.emptyList();
    } else {
      return labels.stream()
                   .map(l -> new ProjectLabel(l.getId(), l.getName()))
                   .toList();
    }
  }

  private List<ProjectLabel> getProjectLabels(long projectId) {
    List<LabelDto> labels = labelService.findLabelsByProject(projectId,
                                                             getCurrentUserAclIdentity(),
                                                             0,
                                                             MAX_TO_LOAD);
    if (CollectionUtils.isEmpty(labels)) {
      return Collections.emptyList();
    } else {
      return labels.stream()
                   .map(l -> new ProjectLabel(l.getId(), l.getName()))
                   .toList();
    }
  }

  private List<TaskCommentModel> toTaskCommentModels(CommentDto comment, long taskId, String url) {
    org.exoplatform.services.security.Identity aclIdentity = getCurrentUserAclIdentity();
    Locale locale = getCurrentUserLocale();
    TaskCommentModel taskCommentModel = new TaskCommentModel(comment.getId(),
                                                             taskId,
                                                             comment.getParentComment() == null ? null :
                                                                                                comment.getParentComment()
                                                                                                       .getId(),
                                                             transformHtmlContent(comment.getComment(), aclIdentity, locale),
                                                             url,
                                                             formatDate(comment.getCreatedTime()),
                                                             getUserModel(comment.getAuthor()));
    if (CollectionUtils.isEmpty(comment.getSubComments())) {
      return Collections.singletonList(taskCommentModel);
    } else {
      List<TaskCommentModel> taskCommentModels = new ArrayList<>();
      taskCommentModels.add(taskCommentModel);
      comment.getSubComments()
             .stream()
             .map(c -> toTaskCommentModel(c, taskId, url))
             .forEach(taskCommentModels::add);
      return taskCommentModels;
    }
  }

  private TaskCommentModel toTaskCommentModel(CommentDto comment, long taskId, String url) {
    org.exoplatform.services.security.Identity aclIdentity = getCurrentUserAclIdentity();
    Locale locale = getCurrentUserLocale();
    return new TaskCommentModel(comment.getId(),
                                taskId,
                                comment.getParentComment() == null ? null :
                                                                   comment.getParentComment()
                                                                          .getId(),
                                transformHtmlContent(comment.getComment(), aclIdentity, locale),
                                url,
                                formatDate(comment.getCreatedTime()),
                                getUserModel(comment.getAuthor()));
  }

  private List<String> getMemberships(Long spaceId) {
    Space space = spaceId != null && spaceId > 0 ? spaceService.getSpaceById(spaceId) : null;
    List<String> memberships;
    if (space == null) {
      memberships = UserUtil.getMemberships(getCurrentUserAclIdentity());
    } else {
      memberships = UserUtil.getSpaceMemberships(space.getGroupId());
    }
    return memberships;
  }

  private UserModel getUserModel(String username) {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    Locale locale = getCurrentUserLocale();
    return StringUtils.isBlank(username) ? null :
                                         toUserModel(identityManager,
                                                     profilePropertyService,
                                                     userAcl,
                                                     translationService,
                                                     portalConfigService,
                                                     username,
                                                     getCurrentUserName(),
                                                     locale,
                                                     false);
  }

  @SneakyThrows
  private String getUrl(TaskDto task) {
    return CommonsUtils.getCurrentDomain() +
        permanentLinkService.getLink(new PermanentLinkObject(TaskPermanentLinkPlugin.OBJECT_TYPE,
                                                             String.valueOf(task.getId())));
  }

  @SneakyThrows
  private String getUrl(ProjectDto project) {
    return CommonsUtils.getCurrentDomain() +
        permanentLinkService.getLink(new PermanentLinkObject(ProjectPermanentLinkPlugin.OBJECT_TYPE,
                                                             String.valueOf(project.getId())));
  }

  @SneakyThrows
  private Date getLastUpdateDate(TaskDto task) {
    Date lastUpdateDate = task.getCreatedTime();
    List<ChangeLogEntry> taskLogs = taskService.getTaskLogs(task.getId(), 0, 1);
    if (CollectionUtils.isNotEmpty(taskLogs)) {
      lastUpdateDate = new Date(taskLogs.get(0).getCreatedTime());
    }
    CommentDto lastComment = commentService.getLastComment(task.getId());
    if (lastComment != null && lastComment.getCreatedTime().compareTo(lastUpdateDate) > 0) {
      lastUpdateDate = lastComment.getCreatedTime();
    }
    return lastUpdateDate;
  }

  private ProjectDto checkCanCreateInProject(long projectId) throws ObjectNotFoundException, IllegalAccessException {
    if (projectId == 0) {
      throw new IllegalArgumentException("Parameter 'project_id' is mandatory");
    }
    try {
      ProjectDto project = projectService.getProject(projectId);
      if (project == null) {
        throw new ObjectNotFoundException(MSG_TASK_PROJECT_NOT_FOUND.formatted(projectId));
      } else if (!project.canView(getCurrentUserAclIdentity())) {
        throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId,
                                                                                   getCurrentUserName()));
      } else {
        return project;
      }
    } catch (EntityNotFoundException e) {
      throw new ObjectNotFoundException(MSG_TASK_PROJECT_NOT_FOUND.formatted(projectId));
    }
  }

  private TaskDto getTaskDetailById(Long taskId) throws ObjectNotFoundException, IllegalAccessException {
    if (taskId == null) {
      throw new IllegalArgumentException("Task identifier is mandatory");
    }
    String currentUsername = getCurrentUserName();
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), currentUsername)) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId,
                                                                         currentUsername));
    }
    return task;
  }

  private Space getProjectSpace(ProjectDto project) {
    Space space = null;
    for (String permission : project.getManager()) {
      int index = permission.indexOf(':');
      if (index > -1) {
        String groupId = permission.substring(index + 1);
        space = spaceService.getSpaceByGroupId(groupId);
      }
    }
    return space;
  }

  private long getProjectId(TaskDto task) {
    if (task.getStatus() == null || task.getStatus().getProject() == null) {
      return 0l;
    } else {
      return task.getStatus()
                 .getProject()
                 .getId();
    }
  }

  private void throwMissingLabel(Long labelId, long projectId) throws ObjectNotFoundException {
    try {
      throw new ObjectNotFoundException(MSG_LABEL_NOT_FOUND.formatted(labelId,
                                                                      JsonUtils.toJsonString(getProjectLabels(projectId))));
    } catch (Exception e) {
      throw new ObjectNotFoundException(MSG_LABEL_NOT_FOUND.formatted(labelId, ""));
    }
  }

}
