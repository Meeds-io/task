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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.favorite.model.Favorite;
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
import org.exoplatform.task.model.TaskTextSearchResult;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.LabelService;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.ProjectUtil;
import org.exoplatform.task.util.UserUtil;

import io.meeds.mcp.server.plugin.McpToolPlugin;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.util.McpToolUtils;
import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.space.plugin.SpaceAclPlugin;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;
import io.meeds.task.mcp.model.ProjectActivityModel;
import io.meeds.task.mcp.model.ProjectCollectionModel;
import io.meeds.task.mcp.model.ProjectLabel;
import io.meeds.task.mcp.model.ProjectModel;
import io.meeds.task.mcp.model.ProjectStatisticsModel;
import io.meeds.task.mcp.model.ProjectStatus;
import io.meeds.task.mcp.model.TaskChangeLog;
import io.meeds.task.mcp.model.TaskCollectionModel;
import io.meeds.task.mcp.model.TaskCommentCollectionModel;
import io.meeds.task.mcp.model.TaskCommentModel;
import io.meeds.task.mcp.model.TaskModel;
import io.meeds.task.mcp.model.TaskSearchCollectionModel;
import io.meeds.task.mcp.model.TaskSearchResultModel;
import io.meeds.task.mcp.model.TaskWithChangeLogModel;
import io.meeds.task.mcp.model.TaskWithChangesModel;
import io.meeds.task.mcp.model.TaskChange;
import io.meeds.task.plugin.ProjectPermanentLinkPlugin;
import io.meeds.task.plugin.TaskAclPlugin;
import io.meeds.task.plugin.TaskPermanentLinkPlugin;

import lombok.SneakyThrows;

@Service
@Profile("mcp-server")
public class TaskMcpTool implements McpToolPlugin {

  /** How many projects a scan of the projects of a space reads at a time. */
  static final int                      SPACE_SCAN_CHUNK                = 50;

  private static final int              MAX_TO_LOAD                     = 100;

  private static final String           MSG_TASK_NOT_EDITABLE           =
                                                              "Task with identifier '%s' is not editable for user '%s'";

  private static final String           MSG_NOT_SCOPE_SPACE_MEMBER      =
                                                              "User '%s' isn't a member of the space this call is limited to";

  private static final String           MSG_SCOPE_UNREADABLE            =
                                                              "The space this call is limited to can't be read, so it can't name a user";

  private static final String           MSG_TASK_NOT_ACCESSIBLE         =
                                                                "Task with identifier '%s' is not accessible for user '%s'";

  private static final String           MSG_TASK_NOT_FOUND              = "Task with identifier '%s' is not found";

  private static final String           MSG_SEARCH_NO_WORD              =
                                                           "A text with a word to search is mandatory: numbers are not searched, a task id goes to get_task_by_id";

  private static final String           MSG_LABEL_NOT_FOUND             =
                                                            "Label with identifier '%s' is not found. Available project labels are:%n```json%n%s%n```";

  private static final String           MSG_STATUS_NOT_FOUND            =
                                                             "Status with identifier '%s' is not found. Available project statuses are:%n```json%n%s%n```";

  private static final String           MSG_TASK_PROJECT_NOT_ACCESSIBLE =
                                                                        "Task project with identifier '%s' is not accessible for the user '%s'";

  private static final String           MSG_TASK_PROJECT_NOT_EDITABLE   =
                                                                      "Task project with identifier '%s' is not editable by the user '%s'";

  private static final String           MSG_TASK_PROJECT_NOT_FOUND      = "Task project with identifier '%s' is not found";

  private static final int              ISO_DATE_LENGTH                 = "2026-10-14".length();

  private static final Pattern          ISO_OFFSET_SUFFIX               = Pattern.compile("(Z|[+-]\\d{2}:?\\d{2})$");

  private static final String           ARG_CHANGED_SINCE               = "changed_since";

  private static final String           ARG_COMPLETED_SINCE             = "completed_since";

  private static final String           MSG_OVERDUE_COMPLETED           =
                                                              "overdue_only lists open tasks and completed_since completed ones: "
                                                                  + "pass one of them, not both";

  private static final String           MSG_INVALID_SINCE               =
                                                          "%s must be a date such as 2026-10-14, or a date and time such as "
                                                              + "2026-10-14T09:00:00+02:00, not '%s'";

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

  private final FavoriteService         favoriteService;

  private final TaskSpaceScopeResolver  spaceScopeResolver;

  /**
   * @param projectService reads and lists the projects
   * @param statusService reads and creates the statuses of a project
   * @param taskService reads and creates the tasks
   * @param commentService reads and writes the comments of a task
   * @param labelService reads and writes the labels of a project
   * @param spaceService reads the spaces
   * @param translationService translates the labels given back to the model
   * @param resourceBundleService reads the resource bundles
   * @param identityManager reads the identities of the users
   * @param profilePropertyService reads the profile properties of the users
   * @param userAcl checks the permissions of the current user
   * @param portalConfigService reads the portal configuration
   * @param permanentLinkService builds the permanent links of the objects
   * @param favoriteService reads and writes the favorites
   * @param spaceScopeResolver tells the space of a project, for a run limited
   *          to a space
   */
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
                     PermanentLinkService permanentLinkService,
                     FavoriteService favoriteService,
                     TaskSpaceScopeResolver spaceScopeResolver) {
    this.favoriteService = favoriteService;
    this.spaceScopeResolver = spaceScopeResolver;
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

  /**
   * Lists the task projects of the current user or, given a space, those of
   * that space the current user holds a role of the space group for. For a
   * user who isn't a member of the space, and for a space id that names no
   * space (missing or deleted), the list is empty.
   * <p>
   * In a run limited to a space (a space scope on the call), the list is
   * narrowed to the projects the space scope accepts for that space, those
   * {@link TaskSpaceScopeResolver#spaceOfProject(ProjectDto)} tells it
   * for, so that the list never holds a project the space scope refuses to
   * get. The selection is read in chunks of {@value #SPACE_SCAN_CHUNK}
   * projects, in the order of the plain list, and stops as soon as one more
   * project than the page asks for is kept: it reads at most the projects
   * the user reaches, a chunk at a time, and no more than the page needs
   * when the space has enough of them. The count is the number of projects of
   * the space when the scan reached the end; when it stopped earlier, it is
   * the number kept so far, which is more than the offset and the page, and
   * means that more exist.
   *
   * @param spaceId the space whose projects are listed, null or not positive
   *          for the projects of the current user
   * @param offset the first project returned
   * @param limit the most projects returned
   * @return the projects, with their count
   */
  public ProjectCollectionModel listProjects(Long spaceId,
                                             Integer offset,
                                             Integer limit) {
    boolean spaceGiven = spaceId != null && spaceId > 0;
    Space space = spaceGiven ? spaceService.getSpaceById(spaceId) : null;
    List<String> memberships = getMemberships(space);
    int usedOffset = getInteger(offset, DEFAULT_OFFSET);
    int usedLimit = getInteger(limit, DEFAULT_LIMIT);
    if (memberships.isEmpty() || spaceGiven && space == null) {
      return new ProjectCollectionModel(Collections.emptyList(), usedOffset, usedLimit, 0);
    } else if (space != null && isSpaceScoped()) {
      return listSpaceProjects(space, memberships, Math.max(usedOffset, 0), Math.max(usedLimit, 0));
    }
    List<ProjectModel> projects = getProjectModels(memberships, offset, limit);
    long count = countProjects(memberships);
    return new ProjectCollectionModel(projects, usedOffset, usedLimit, count);
  }

  /**
   * @return the space the current call is limited to, null when it isn't
   * @throws IllegalStateException when the call's identity can't be read
   */
  protected Long getScopeSpaceId() {
    ActingIdentity identity = McpToolUtils.getCurrentActingIdentity();
    return identity == null ? null : identity.spaceScope();
  }

  /**
   * Checks the people a call limited to a space names, as an assignee, a
   * coworker or the mentions of a comment, are members or super managers of
   * that space, the people the platform's space rules count in. The
   * space scope reads the space of the task only; people are checked here,
   * since a project's participants may include people outside its space, and
   * the task's access rule alone would let a call limited to the space reach
   * them. The scope is read once, the people only for a limited call, and the
   * space only when they name someone. A call that isn't limited is not checked;
   * one whose identity can't be read is refused when it names anyone.
   *
   * @param people the logins the call names, read only when it is limited
   * @throws IllegalAccessException when the call is limited to a space one of
   *           them is neither a member nor a super manager of, or its identity
   *           can't be read and it names someone
   */
  private void checkScopeSpaceMembers(Supplier<Collection<String>> people) throws IllegalAccessException {
    Long spaceId;
    try {
      spaceId = getScopeSpaceId();
    } catch (IllegalStateException e) {
      if (people.get().isEmpty()) {
        return;
      }
      throw new IllegalAccessException(MSG_SCOPE_UNREADABLE);
    }
    if (spaceId == null) {
      return;
    }
    Collection<String> usernames = people.get();
    if (usernames.isEmpty()) {
      return;
    }
    Space space = spaceService.getSpaceById(spaceId);
    for (String username : usernames) {
      if (space == null || !(spaceService.isMember(space, username) || spaceService.isSuperManager(space, username))) {
        throw new IllegalAccessException(MSG_NOT_SCOPE_SPACE_MEMBER.formatted(username));
      }
    }
  }

  /**
   * @return true when the current call is limited to a space; also true when
   *         its identity can't be read, which a limited call must never be
   *         taken for the opposite of
   */
  protected boolean isSpaceScoped() {
    try {
      ActingIdentity identity = McpToolUtils.getCurrentActingIdentity();
      return identity != null && identity.spaceScope() != null;
    } catch (IllegalStateException e) {
      return true;
    }
  }

  /**
   * @param space an existing space
   * @param memberships the roles of the space group the current user holds
   * @param offset the first project returned, not negative
   * @param limit the most projects returned, not negative
   * @return the projects of that space among those the memberships reach,
   *         with the count described on
   *         {@link #listProjects(Long, Integer, Integer)}
   */
  private ProjectCollectionModel listSpaceProjects(Space space, List<String> memberships, int offset, int limit) {
    int wanted = offset + limit + 1;
    List<ProjectDto> ofSpace = new ArrayList<>();
    int from = 0;
    boolean finished = false;
    while (!finished && ofSpace.size() < wanted) {
      List<ProjectDto> chunk = projectService.findProjects(memberships,
                                                           null,
                                                           new OrderBy.DESC("lastModifiedDate"),
                                                           from,
                                                           SPACE_SCAN_CHUNK);
      int read = chunk == null ? 0 : chunk.size();
      finished = read < SPACE_SCAN_CHUNK;
      from += read;
      for (int i = 0; i < read && ofSpace.size() < wanted; i++) {
        if (isProjectOfSpace(chunk.get(i), space)) {
          ofSpace.add(chunk.get(i));
        }
      }
    }
    List<ProjectModel> page = ofSpace.stream()
                                     .skip(offset)
                                     .limit(limit)
                                     .map(this::toProjectModel)
                                     .toList();
    return new ProjectCollectionModel(page, offset, limit, ofSpace.size());
  }

  /**
   * @param project a task project
   * @param space a space
   * @return true when the space scope would tell that space for the project
   */
  private boolean isProjectOfSpace(ProjectDto project, Space space) {
    Long projectSpaceId = spaceScopeResolver.spaceOfProject(project);
    return projectSpaceId != null && String.valueOf(projectSpaceId).equals(space.getId());
  }

  /**
   * Lists the open tasks assigned to the current user, or of which they are
   * a coworker; with {@code overdueOnly}, only those whose due date is
   * before today in the user's time zone (EXO-91122). Each task past its
   * due date carries its days overdue. With {@code changedSince}, only the
   * tasks whose status, due date, assignee or completion changed since that
   * date, and with {@code completedSince} only those completed since that
   * date, completed tasks included, each with its changes since that date
   * (EXO-91902). Asking both the overdue tasks and the tasks completed since
   * a date is refused.
   *
   * @param limit the most tasks returned
   * @param overdueOnly whether only the overdue tasks are listed
   * @param changedSince a date or date-time, blank for no condition
   * @param completedSince a date or date-time, blank for no condition
   * @return the tasks, with their count
   */
  public TaskCollectionModel listAssignedTasks(Integer limit,
                                               Boolean overdueOnly,
                                               String changedSince,
                                               String completedSince) {
    ZoneId zone = getCurrentUserZone();
    int max = getInteger(limit, DEFAULT_LIMIT);
    Date changedSinceDate = parseSince(changedSince, ARG_CHANGED_SINCE, zone);
    Date completedSinceDate = parseSince(completedSince, ARG_COMPLETED_SINCE, zone);
    Date since = changedSinceDate == null ? completedSinceDate : changedSinceDate;
    checkNotOverdueAndCompleted(overdueOnly, completedSinceDate);
    List<? extends TaskModel> taskModels;
    long count;
    if (Boolean.TRUE.equals(overdueOnly) || since != null) {
      TaskQuery taskQuery = new TaskQuery();
      taskQuery.setAssigneeOrCoworker(Collections.singletonList(getCurrentUserName()));
      if (Boolean.TRUE.equals(overdueOnly)) {
        setOverdueOnly(taskQuery, zone);
      }
      taskQuery.setChangedSince(changedSinceDate);
      taskQuery.setCompletedSince(completedSinceDate);
      List<TaskDto> tasks = Boolean.TRUE.equals(overdueOnly) ? findOverdueTasks(taskQuery, 0, max) :
                                                             taskService.findLastUpdatedTasks(taskQuery, 0, max);
      taskModels = since == null ? toTaskModels(tasks, zone) : toTaskWithChangesModels(tasks, since, false, zone);
      count = countTasks(taskQuery);
    } else {
      taskModels = toTaskModels(taskService.getUncompletedTasks(getCurrentUserName(), max), zone);
      count = countAssignedTaskModels();
    }
    return new TaskCollectionModel(taskModels, 0, max, count);
  }

  /**
   * Lists the tasks of a project, or, without one, every task the current
   * user can access, personal tasks included; with {@code overdueOnly}, only
   * the open tasks whose due date is before today in the user's time zone
   * (EXO-91122). Each open task past its due date carries its days overdue.
   * With {@code title}, only the tasks whose title contains it, ignoring
   * case, among those the other filters select: the title narrows the
   * project, or the accessible tasks, and never widens them. With
   * {@code changedSince}, only the tasks whose status, due date, assignee or
   * completion changed since that date; with {@code completedSince}, only
   * the tasks completed since that date, whatever
   * {@code hideCompletedTasks} says; with {@code assignee}, only the tasks
   * assigned to that user; with {@code statusId}, only the tasks in that
   * status (EXO-91902). A task listed with {@code changedSince} or
   * {@code completedSince} carries its changes since that date. Asking both
   * the overdue tasks, which are open, and the tasks completed since a date
   * is refused. The count is
   * the count of the tasks the same filters select.
   *
   * @param projectId the project, null for every accessible task
   * @param hideCompletedTasks whether the completed tasks are left out
   * @param includeChangeLog whether each task comes with its change log
   * @param overdueOnly whether only the overdue tasks are listed
   * @param offset the first task returned
   * @param limit the most tasks returned
   * @param title a part of the title of the tasks listed, blank for any
   * @param changedSince a date or date-time, blank for no condition
   * @param completedSince a date or date-time, blank for no condition
   * @param assignee the username of the assignee, blank for any
   * @param statusId the status of the tasks listed, null for any
   * @return the tasks, with their count
   * @throws IllegalAccessException when the user can't view the project
   * @throws ObjectNotFoundException when the project doesn't exist
   * @throws IllegalArgumentException when a date is no date, or when both
   *           {@code overdueOnly} and {@code completedSince} are given
   */
  public TaskCollectionModel listTasks(Long projectId, // NOSONAR the tool's parameters, each one its own
                                       Boolean hideCompletedTasks,
                                       Boolean includeChangeLog,
                                       Boolean overdueOnly,
                                       Integer offset,
                                       Integer limit,
                                       String title,
                                       String changedSince,
                                       String completedSince,
                                       String assignee,
                                       Long statusId) throws IllegalAccessException, ObjectNotFoundException {
    ZoneId zone = getCurrentUserZone();
    LocalDate today = getToday(zone);
    Date changedSinceDate = parseSince(changedSince, ARG_CHANGED_SINCE, zone);
    Date completedSinceDate = parseSince(completedSince, ARG_COMPLETED_SINCE, zone);
    checkNotOverdueAndCompleted(overdueOnly, completedSinceDate);
    TaskQuery taskQuery = getTaskQuery(projectId,
                                       Boolean.TRUE.equals(hideCompletedTasks) && completedSinceDate == null,
                                       Boolean.TRUE.equals(overdueOnly),
                                       zone);
    if (StringUtils.isNotBlank(title)) {
      // TaskQuery#setTitle compares the lower-cased title with the value as given
      taskQuery.setTitle(title.trim().toLowerCase(Locale.ROOT));
    }
    taskQuery.setChangedSince(changedSinceDate);
    taskQuery.setCompletedSince(completedSinceDate);
    if (StringUtils.isNotBlank(assignee)) {
      taskQuery.setAssignee(Collections.singletonList(assignee.trim()));
    }
    taskQuery.setStatusId(statusId);
    List<TaskDto> tasks = Boolean.TRUE.equals(overdueOnly) ?
                                                           findOverdueTasks(taskQuery,
                                                                            getInteger(offset, DEFAULT_OFFSET),
                                                                            getInteger(limit, DEFAULT_LIMIT)) :
                                                           taskService.findLastUpdatedTasks(taskQuery,
                                                                                            getInteger(offset, DEFAULT_OFFSET),
                                                                                            getInteger(limit, DEFAULT_LIMIT));
    Date since = changedSinceDate == null ? completedSinceDate : changedSinceDate;
    List<? extends TaskModel> taskModels;
    if (since != null) {
      taskModels = toTaskWithChangesModels(tasks, since, Boolean.TRUE.equals(includeChangeLog), zone);
    } else if (Boolean.TRUE.equals(includeChangeLog)) {
      taskModels = tasks.stream().map(t -> toTaskWithChangeLogModel(t, null, zone, today)).toList();
    } else {
      taskModels = toTaskModels(tasks, zone);
    }
    return new TaskCollectionModel(taskModels,
                                   getInteger(offset, DEFAULT_OFFSET),
                                   getInteger(limit, DEFAULT_LIMIT),
                                   countTasks(taskQuery));
  }

  /**
   * Searches the tasks the current user can view by a text in their title or
   * description, ignoring case and accents: the tool's adapter of
   * {@link TaskService#searchTasks(String, Long, boolean, int, Identity)},
   * which holds the rules. It reads the tool's arguments, turns the service's
   * refusals into the tool's messages and maps the tasks found to what tells
   * them apart on their board.
   *
   * @param query the text searched: every word in the title, or every word in
   *          the description; the index does not search numbers, so the text
   *          must hold a word with letters
   * @param projectId the project, null for every task the user can view
   * @param includeCompleted whether the completed tasks are returned, after
   *          the open ones
   * @param limit the most tasks returned
   * @return the tasks found, and whether more may match than were returned
   * @throws IllegalAccessException when the user can't view the project
   * @throws ObjectNotFoundException when the project doesn't exist
   */
  public TaskSearchCollectionModel searchTasks(String query,
                                               Long projectId,
                                               Boolean includeCompleted,
                                               Integer limit) throws IllegalAccessException, ObjectNotFoundException {
    TaskTextSearchResult result;
    try {
      result = taskService.searchTasks(query,
                                       projectId,
                                       Boolean.TRUE.equals(includeCompleted),
                                       getInteger(limit, DEFAULT_LIMIT),
                                       getCurrentUserAclIdentity());
    } catch (IllegalArgumentException e) {
      if (TaskService.TEXT_SEARCH_NO_WORD.equals(e.getMessage())) {
        throw new IllegalArgumentException(MSG_SEARCH_NO_WORD);
      }
      throw e;
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(MSG_TASK_PROJECT_NOT_FOUND.formatted(projectId));
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId, getCurrentUserName()));
    }
    List<TaskSearchResultModel> results = result.getTasks().stream().map(this::toTaskSearchResultModel).toList();
    return new TaskSearchCollectionModel(results, result.getLimit(), result.isHasMore());
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

  /**
   * Creates a task inside a project. Dates mirror the Task UI: a "Start Date"
   * and a "Due date" (the vestigial {@code endDate} field is intentionally not
   * exposed here).
   *
   * @param projectId   id of the project to create the task in
   * @param title       task title
   * @param description task description
   * @param assignee    username of the assignee, or blank for the calling user:
   *                      a personal task shows only in its assignee's list, so
   *                      one without an assignee would be listed nowhere
   * @param coworkers   usernames of the coworkers
   * @param startDate   the start date (ISO string), or blank for none
   * @param dueDate     the due date (ISO string), or blank for none
   * @param priority    task priority (defaults to {@link Priority#NONE} when null)
   * @param statusId    id of the target status, or null for the project default status
   * @return the created task model
   * @throws ObjectNotFoundException if the project does not exist
   * @throws IllegalAccessException  if the current user cannot create tasks in the project
   */
  public TaskModel createTaskInProject(long projectId, // NOSONAR
                                       String title,
                                       String description,
                                       String assignee,
                                       Set<String> coworkers,
                                       String startDate,
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

  /**
   * Creates a personal (label-less, project-less) task. Dates mirror the Task
   * UI: a "Start Date" and a "Due date" (the vestigial {@code endDate} field is
   * intentionally not exposed here).
   *
   * @param title       task title
   * @param description task description
   * @param assignee    username of the assignee
   * @param coworkers   usernames of the coworkers
   * @param startDate   the start date (ISO string), or blank for none
   * @param dueDate     the due date (ISO string), or blank for none
   * @param priority    task priority (defaults to {@link Priority#NONE} when null)
   * @param statusId    id of the target status, or null for none; the task is
   *                      then stored in that status's project, which the
   *                      current user must be able to view
   * @return the created task model
   * @throws ObjectNotFoundException if the status does not exist
   * @throws IllegalAccessException  if the current user cannot create tasks in
   *                                   the project of the status
   */
  public TaskModel createPersonalTask(String title, // NOSONAR
                                      String description,
                                      String assignee,
                                      Set<String> coworkers,
                                      String startDate,
                                      String dueDate,
                                      Priority priority,
                                      Long statusId) throws ObjectNotFoundException, IllegalAccessException {
    Identity aclIdentity = getCurrentUserAclIdentity();
    TaskDto task = new TaskDto();
    task.setTitle(title);
    task.setDescription(description);
    task.setAssignee(StringUtils.isBlank(assignee) ? aclIdentity.getUserId() : assignee);
    task.setCoworker(coworkers);
    task.setStartDate(toDate(startDate));
    task.setDueDate(toDate(dueDate));
    task.setPriority(priority == null ? Priority.NONE : priority);
    task.setCreatedBy(aclIdentity.getUserId());
    task.setCreatedTime(new Date());
    if (statusId != null) {
      StatusDto status = statusService.getStatus(statusId);
      if (status == null || status.getProject() == null) {
        throw new ObjectNotFoundException("Task Status with id '%s' doesn't exist".formatted(statusId));
      }
      checkCanCreateInProject(status.getProject().getId());
      task.setStatus(status);
    }
    task = taskService.createTask(task);
    return toTaskModel(task);
  }

  /**
   * Comments on a task the current user can access. Every user the text
   * mentions is notified, so in a call limited to a space each one must be a
   * member or a super manager of that space.
   *
   * @param taskId the task
   * @param text the comment, which may mention users
   * @return the comment
   * @throws ObjectNotFoundException when the task doesn't exist
   * @throws IllegalAccessException when the current user can't access the
   *           task, or, in a call limited to a space, the text mentions a user
   *           who is neither a member nor a super manager of it
   */
  @SneakyThrows
  public TaskCommentModel addTaskComment(long taskId, String text) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId,
                                                                         getCurrentUserName()));
    }
    checkScopeSpaceMembers(() -> MentionUtils.getMentionedUsernames(text));
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

  /**
   * Assigns a task to a user who can access it; in a call limited to a space,
   * the user must also be a member or a super manager of that space.
   *
   * @param taskId the task, which the current user can access
   * @param username the assignee's login, with or without a leading {@code @}
   * @return the updated task
   * @throws ObjectNotFoundException when the task or the user doesn't exist
   * @throws IllegalAccessException when the current user or the assignee
   *           can't access the task, or, in a call limited to a space, the
   *           assignee is neither a member nor a super manager of it
   */
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
    String person = username;
    checkScopeSpaceMembers(() -> List.of(person));
    task.setAssignee(username);
    task = taskService.updateTask(task);
    return toTaskModel(task);
  }

  /**
   * Adds a coworker who can access the task; in a call limited to a space,
   * the coworker must also be a member or a super manager of that space.
   *
   * @param taskId the task, which the current user can access
   * @param username the coworker's login, with or without a leading {@code @}
   * @return the updated task
   * @throws ObjectNotFoundException when the task or the user doesn't exist
   * @throws IllegalAccessException when the current user or the coworker
   *           can't access the task, or, in a call limited to a space, the
   *           coworker is neither a member nor a super manager of it
   */
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
    String person = username;
    checkScopeSpaceMembers(() -> List.of(person));
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

  /**
   * Adds one of the labels of the task's project to the task.
   *
   * @param taskId the task, which the current user can access
   * @param labelId a label of the task's project; for a task in no project,
   *          any existing label
   * @throws IllegalAccessException when the current user can't access the
   *           task
   * @throws ObjectNotFoundException when the task doesn't exist, or the label
   *           doesn't exist or belongs to another project than the task's
   */
  @SneakyThrows
  public void addProjectLabelToTask(Long taskId, Long labelId) throws IllegalAccessException, ObjectNotFoundException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, getCurrentUserName()));
    }
    checkTaskLabel(task, labelId);
    labelService.addTaskToLabel(task, labelId);
  }

  /**
   * Removes one of the labels of the task's project from the task.
   *
   * @param taskId the task, which the current user can access
   * @param labelId a label of the task's project; for a task in no project,
   *          any existing label
   * @throws IllegalAccessException when the current user can't access the
   *           task
   * @throws ObjectNotFoundException when the task doesn't exist, or the label
   *           doesn't exist or belongs to another project than the task's
   */
  @SneakyThrows
  public void removeProjectLabelFromTask(Long taskId, Long labelId) throws IllegalAccessException, ObjectNotFoundException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, getCurrentUserName()));
    }
    checkTaskLabel(task, labelId);
    labelService.removeTaskFromLabel(task, labelId);
  }

  @SneakyThrows
  public List<ProjectLabel> listTaskLabels(long taskId) throws IllegalAccessException, ObjectNotFoundException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, getCurrentUserName()));
    }
    return getTaskLabels(task, getProjectId(task), getCurrentUserAclIdentity());
  }

  @SneakyThrows
  public ProjectLabel updateProjectLabel(long labelId, String name) throws IllegalAccessException, ObjectNotFoundException {
    LabelDto label = getEditableLabel(labelId);
    label.setName(name);
    label = labelService.updateLabel(label);
    return new ProjectLabel(label.getId(), label.getName());
  }

  @SneakyThrows
  public void deleteProjectLabel(long labelId) throws IllegalAccessException, ObjectNotFoundException {
    getEditableLabel(labelId);
    labelService.removeLabel(labelId);
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

  /**
   * Patches a task's start and due dates. This is a partial update: only the
   * dates actually passed (non-blank) are updated, so setting one date (e.g.
   * the due date) never wipes the other one that was set before. These are the
   * two dates surfaced by the Task UI ("Start Date" + "Due date"); the vestigial
   * {@code endDate} field is intentionally not exposed here.
   *
   * @param taskId    id of the task to update
   * @param startDate the new start date (ISO string), or blank to leave it unchanged
   * @param dueDate   the new due date (ISO string), or blank to leave it unchanged
   * @return the updated task model
   * @throws ObjectNotFoundException if the task does not exist
   * @throws IllegalAccessException  if the current user cannot edit the task
   */
  public TaskModel setTaskDates(long taskId,
                                String startDate,
                                String dueDate) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getEditableTask(taskId);
    if (StringUtils.isNotBlank(startDate)) {
      task.setStartDate(toDate(startDate));
    }
    if (StringUtils.isNotBlank(dueDate)) {
      task.setDueDate(toDate(dueDate));
    }
    task = taskService.updateTask(task);
    return toTaskModel(task);
  }

  public TaskModel completeTask(long taskId) throws ObjectNotFoundException, IllegalAccessException {
    return setTaskCompleted(taskId, true);
  }

  public TaskModel reopenTask(long taskId) throws ObjectNotFoundException, IllegalAccessException {
    return setTaskCompleted(taskId, false);
  }

  public TaskModel setTaskPriority(long taskId, Priority priority) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getEditableTask(taskId);
    task.setPriority(priority == null ? Priority.NONE : priority);
    task = taskService.updateTask(task);
    return toTaskModel(task);
  }

  public TaskCollectionModel listUnscheduledTasks(Long projectId,
                                                  Integer limit) throws IllegalAccessException, ObjectNotFoundException {
    int max = getInteger(limit, DEFAULT_LIMIT);
    ZoneId zone = getCurrentUserZone();
    List<TaskModel> unscheduled = toTaskModels(getTasks(projectId, 0, MAX_TO_LOAD, true).stream()
                                                                                        .filter(t -> t.getStartDate() == null
                                                                                            && t.getDueDate() == null)
                                                                                        .limit(max)
                                                                                        .toList(),
                                               zone);
    return new TaskCollectionModel(unscheduled, 0, max, unscheduled.size());
  }

  private TaskModel setTaskCompleted(long taskId, boolean completed) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getEditableTask(taskId);
    task.setCompleted(completed);
    task = taskService.updateTask(task);
    return toTaskModel(task);
  }

  private TaskDto getEditableTask(long taskId) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_EDITABLE.formatted(taskId, getCurrentUserName()));
    }
    return task;
  }

  public ProjectModel updateProject(Long projectId,
                                    String name,
                                    String description,
                                    String color) throws ObjectNotFoundException, IllegalAccessException {
    ProjectDto project = getEditableProject(projectId);
    if (StringUtils.isNotBlank(name)) {
      project.setName(name);
    }
    if (description != null) {
      project.setDescription(description);
    }
    if (StringUtils.isNotBlank(color)) {
      project.setColor(color);
    }
    project = projectService.updateProject(project);
    return toProjectModel(project);
  }

  public ProjectModel addProjectMember(Long projectId, String username) throws ObjectNotFoundException, IllegalAccessException {
    return updateProjectMembership(projectId, username, true, false);
  }

  public ProjectModel removeProjectMember(Long projectId, String username) throws ObjectNotFoundException,
                                                                          IllegalAccessException {
    return updateProjectMembership(projectId, username, false, false);
  }

  public ProjectModel setProjectManager(Long projectId, String username) throws ObjectNotFoundException, IllegalAccessException {
    return updateProjectMembership(projectId, username, true, true);
  }

  public ProjectModel removeProjectManager(Long projectId, String username) throws ObjectNotFoundException,
                                                                           IllegalAccessException {
    return updateProjectMembership(projectId, username, false, true);
  }

  public ProjectStatus createProjectStatus(Long projectId, String name) throws ObjectNotFoundException, IllegalAccessException {
    ProjectDto project = getEditableProject(projectId);
    StatusDto status = statusService.createStatus(project, name);
    return new ProjectStatus(status.getId(), status.getName(), status.getRank());
  }

  @SneakyThrows
  public ProjectStatus renameProjectStatus(Long statusId, String name) throws ObjectNotFoundException, IllegalAccessException {
    StatusDto status = statusService.getStatus(statusId);
    if (status == null) {
      throw new ObjectNotFoundException(MSG_STATUS_NOT_FOUND.formatted(statusId, "[]"));
    }
    // ensure the caller can edit the owning project
    getEditableProject(status.getProject().getId());
    status = statusService.updateStatus(statusId, name);
    return new ProjectStatus(status.getId(), status.getName(), status.getRank());
  }

  private ProjectModel updateProjectMembership(Long projectId,
                                               String username,
                                               boolean add,
                                               boolean manager) throws ObjectNotFoundException, IllegalAccessException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("Parameter 'username' is mandatory");
    }
    username = username.startsWith("@") ? username.substring(1) : username;
    ProjectDto project = getEditableProject(projectId);
    Set<String> members = new HashSet<>(manager ? safeSet(project.getManager()) : safeSet(project.getParticipator()));
    if (add) {
      members.add(username);
    } else {
      members.remove(username);
    }
    if (manager) {
      project.setManager(members);
    } else {
      project.setParticipator(members);
    }
    project = projectService.updateProject(project);
    return toProjectModel(project);
  }

  private Set<String> safeSet(Set<String> set) {
    return set == null ? Collections.emptySet() : set;
  }

  private ProjectDto getEditableProject(Long projectId) throws ObjectNotFoundException, IllegalAccessException {
    ProjectDto project = getProject(projectId);
    if (project == null) {
      throw new ObjectNotFoundException(MSG_TASK_PROJECT_NOT_FOUND.formatted(projectId));
    } else if (!project.canEdit(getCurrentUserAclIdentity())) {
      throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_EDITABLE.formatted(projectId, getCurrentUserName()));
    }
    return project;
  }

  public ProjectStatisticsModel getProjectStatistics(Long projectId) throws ObjectNotFoundException, IllegalAccessException {
    getViewableProject(projectId);
    Map<String, Long> byStatus = new LinkedHashMap<>();
    for (StatusDto status : statusService.getStatuses(projectId)) {
      byStatus.put(status.getName(), 0L);
    }
    long total = 0;
    List<Object[]> rows = taskService.countTaskStatusByProject(projectId);
    if (rows != null) {
      for (Object[] row : rows) {
        String name = (String) row[0];
        long count = ((Number) row[1]).longValue();
        byStatus.put(name, count);
        total += count;
      }
    }
    return new ProjectStatisticsModel(total, byStatus);
  }

  public void favoriteProject(Long projectId) throws ObjectNotFoundException, IllegalAccessException {
    getViewableProject(projectId);
    createFavorite(ProjectPermanentLinkPlugin.OBJECT_TYPE, String.valueOf(projectId));
  }

  public void unfavoriteProject(Long projectId) throws ObjectNotFoundException, IllegalAccessException {
    getViewableProject(projectId);
    removeFavorite(ProjectPermanentLinkPlugin.OBJECT_TYPE, String.valueOf(projectId));
  }

  public void favoriteTask(long taskId) throws ObjectNotFoundException, IllegalAccessException {
    getAccessibleTask(taskId);
    createFavorite(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId));
  }

  public void unfavoriteTask(long taskId) throws ObjectNotFoundException, IllegalAccessException {
    getAccessibleTask(taskId);
    removeFavorite(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId));
  }

  @SneakyThrows
  public void deleteTaskComment(long commentId) throws ObjectNotFoundException, IllegalAccessException {
    CommentDto comment = commentService.getComment(commentId);
    if (comment == null) {
      throw new ObjectNotFoundException("Comment with identifier '%s' is not found".formatted(commentId));
    }
    long taskId = comment.getTask().getId();
    boolean isAuthor = StringUtils.equals(comment.getAuthor(), getCurrentUserName());
    if (!isAuthor && !userAcl.hasEditPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException("Comment '%s' is not deletable by user '%s'".formatted(commentId, getCurrentUserName()));
    }
    commentService.removeComment(commentId);
  }

  @SneakyThrows
  private void createFavorite(String objectType, String objectId) {
    Favorite favorite = new Favorite(objectType, objectId, null, getCurrentUserIdentityId());
    if (!favoriteService.isFavorite(favorite)) {
      favoriteService.createFavorite(favorite);
    }
  }

  @SneakyThrows
  private void removeFavorite(String objectType, String objectId) {
    Favorite favorite = new Favorite(objectType, objectId, null, getCurrentUserIdentityId());
    if (favoriteService.isFavorite(favorite)) {
      favoriteService.deleteFavorite(favorite);
    }
  }

  private long getCurrentUserIdentityId() {
    return Long.parseLong(identityManager.getOrCreateUserIdentity(getCurrentUserName()).getId());
  }

  private ProjectDto getViewableProject(Long projectId) throws ObjectNotFoundException, IllegalAccessException {
    ProjectDto project = getProject(projectId);
    if (project == null) {
      throw new ObjectNotFoundException(MSG_TASK_PROJECT_NOT_FOUND.formatted(projectId));
    } else if (!project.canView(getCurrentUserAclIdentity())) {
      throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId, getCurrentUserName()));
    }
    return project;
  }

  private TaskDto getAccessibleTask(long taskId) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTask(taskId);
    if (task == null) {
      throw new ObjectNotFoundException(MSG_TASK_NOT_FOUND.formatted(taskId));
    } else if (!userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(taskId), getCurrentUserName())) {
      throw new IllegalAccessException(MSG_TASK_NOT_ACCESSIBLE.formatted(taskId, getCurrentUserName()));
    }
    return task;
  }

  public ProjectActivityModel listProjectActivitySince(Long projectId, Integer days) throws IllegalAccessException,
                                                                                     ObjectNotFoundException {
    long count = countTaskModels(projectId, false);
    if (count == 0) {
      return new ProjectActivityModel(Collections.emptyList(), Collections.emptyList());
    } else {
      List<TaskWithChangeLogModel> updatedTasks = new ArrayList<>();
      List<TaskWithChangeLogModel> uncompletedTasks = new ArrayList<>();
      ZoneId zone = getCurrentUserZone();
      LocalDate today = getToday(zone);

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
                                                                                                                              sinceInstant,
                                                                                                                              zone,
                                                                                                                              today))
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
                                                .map(t -> toTaskWithChangeLogModel(t, sinceInstant, zone, today))
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

  /**
   * Builds the query of {@link #listTasks}: a project's tasks, or every task
   * the user can access, completed ones left out on demand, and only the
   * overdue ones on demand.
   *
   * @param projectId the project, null or 0 for every accessible task
   * @param uncompleteOnly whether the completed tasks are left out
   * @param overdueOnly whether only the overdue tasks are selected
   * @param zone the user's time zone
   * @return the query
   * @throws IllegalAccessException when the user can't view the project
   * @throws ObjectNotFoundException when the project doesn't exist
   */
  private TaskQuery getTaskQuery(Long projectId,
                                 boolean uncompleteOnly,
                                 boolean overdueOnly,
                                 ZoneId zone) throws IllegalAccessException, ObjectNotFoundException {
    Identity aclIdentity = getCurrentUserAclIdentity();
    TaskQuery taskQuery = new TaskQuery();
    if (projectId != null && projectId > 0) {
      ProjectDto project;
      try {
        project = projectService.getProject(projectId);
      } catch (EntityNotFoundException e) {
        throw new ObjectNotFoundException(MSG_TASK_PROJECT_NOT_FOUND.formatted(projectId));
      }
      if (project == null || !project.canView(aclIdentity)) {
        throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_ACCESSIBLE.formatted(projectId, getCurrentUserName()));
      }
      taskQuery.setProjectIds(Collections.singletonList(projectId));
    } else {
      taskQuery.setAccessible(aclIdentity);
    }
    if (overdueOnly) {
      setOverdueOnly(taskQuery, zone);
    } else if (uncompleteOnly) {
      taskQuery.setCompleted(false);
    }
    return taskQuery;
  }

  /**
   * @param projectId the project, null or 0 for every accessible task
   * @param offset the first task returned
   * @param limit the most tasks returned
   * @param uncompleteOnly whether the completed tasks are left out
   * @return the tasks
   * @throws IllegalAccessException when the user can't view the project
   * @throws ObjectNotFoundException when the project doesn't exist
   */
  private List<TaskDto> getTasks(Long projectId,
                                 Integer offset,
                                 Integer limit,
                                 boolean uncompleteOnly) throws IllegalAccessException, ObjectNotFoundException {
    return taskService.findLastUpdatedTasks(getTaskQuery(projectId, uncompleteOnly, false, null),
                                            getInteger(offset, DEFAULT_OFFSET),
                                            getInteger(limit, DEFAULT_LIMIT));
  }

  /**
   * @param task a task found by a search
   * @return what tells it apart on its board: its title, link, completion,
   *         project, status and labels
   */
  private TaskSearchResultModel toTaskSearchResultModel(TaskDto task) {
    StatusDto status = task.getStatus();
    ProjectDto project = status == null ? null : status.getProject();
    Long projectId = project == null ? null : project.getId();
    return new TaskSearchResultModel(task.getId(),
                                     task.getTitle(),
                                     getUrl(task),
                                     task.isCompleted(),
                                     projectId,
                                     project == null ? null : project.getName(),
                                     getTaskStatus(status),
                                     project == null ? null : getTaskLabels(task, projectId, getCurrentUserAclIdentity()));
  }

  /**
   * Restricts a query to the overdue tasks: open, with a due date before the
   * start of today in the user's time zone. A task due today is not overdue.
   * The due date is the one the Tasks UI shows, never the end date.
   *
   * @param taskQuery the query to restrict
   * @param zone the user's time zone
   */
  private void setOverdueOnly(TaskQuery taskQuery, ZoneId zone) {
    taskQuery.setOverdueAt(Date.from(getToday(zone).atStartOfDay(zone).toInstant()));
  }

  /**
   * Lists an overdue query's tasks, the most overdue first: by due date
   * ascending, then by identifier so that two pages never overlap.
   *
   * @param taskQuery an overdue query
   * @param offset the first task returned
   * @param limit the most tasks returned
   * @return the tasks of the page
   */
  @SneakyThrows
  private List<TaskDto> findOverdueTasks(TaskQuery taskQuery, int offset, int limit) {
    taskQuery.setOrderBy(List.of(new OrderBy.ASC("dueDate"), new OrderBy.ASC("id")));
    return taskService.findTasks(taskQuery, offset, limit);
  }

  /**
   * @param taskQuery a query
   * @return the count of the tasks it selects
   */
  @SneakyThrows
  private long countTasks(TaskQuery taskQuery) {
    return taskService.countTasks(taskQuery);
  }

  /**
   * @param tasks tasks, possibly null
   * @param zone the user's time zone
   * @return their models, each overdue one with its days overdue
   */
  private List<TaskModel> toTaskModels(List<TaskDto> tasks, ZoneId zone) {
    if (CollectionUtils.isEmpty(tasks)) {
      return Collections.emptyList();
    }
    LocalDate today = getToday(zone);
    return tasks.stream().map(task -> toTaskModel(task, zone, today)).toList();
  }

  /**
   * Tells how many days an open task is past its due date, in the user's
   * time zone: 1 for a task due yesterday, none for a task due today.
   *
   * @param task a task
   * @param zone the user's time zone
   * @param today today in that zone
   * @return the days overdue, null when the task isn't overdue
   */
  static Integer getDaysOverdue(TaskDto task, ZoneId zone, LocalDate today) {
    if (task == null || task.isCompleted() || task.getDueDate() == null) {
      return null;
    }
    LocalDate dueDay = task.getDueDate().toInstant().atZone(zone).toLocalDate();
    long days = ChronoUnit.DAYS.between(dueDay, today);
    return days > 0 ? Math.toIntExact(days) : null;
  }

  /**
   * @return the current user's time zone, the server's when they have none
   */
  protected ZoneId getCurrentUserZone() {
    TimeZone timeZone = McpToolUtils.getUserTimeZone();
    return timeZone == null ? ZoneId.systemDefault() : timeZone.toZoneId();
  }

  /**
   * @param zone a time zone
   * @return today in that zone
   */
  protected LocalDate getToday(ZoneId zone) {
    return LocalDate.now(zone);
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

  private long countAssignedTaskModels() {
    String currentUserName = getCurrentUserName();
    return taskService.countUncompletedTasks(currentUserName);
  }

  /**
   * @param projectId the project, null or 0 for every accessible task
   * @param uncompleteOnly whether the completed tasks are left out
   * @return the count of the tasks {@link #getTaskQuery} selects
   * @throws IllegalAccessException when the user can't view the project
   */
  @SneakyThrows
  private long countTaskModels(Long projectId, boolean uncompleteOnly) throws IllegalAccessException {
    return countTasks(getTaskQuery(projectId, uncompleteOnly, false, null));
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

  /**
   * @param task a task
   * @param since only the change log entries after this instant, null for all
   * @param zone the user's time zone
   * @param today today in that zone
   * @return the task model, with its change log and comments
   */
  @SneakyThrows
  private TaskWithChangeLogModel toTaskWithChangeLogModel(TaskDto task, Instant since, ZoneId zone, LocalDate today) {
    return new TaskWithChangeLogModel(toTaskModel(task, zone, today),
                                      getChangeLogs(task, taskService.getTaskLogs(task.getId(), 0, MAX_TO_LOAD), since)); // NOSONAR
  }

  /**
   * @param task a task
   * @param logs its change log entries, as the task service returns them
   * @param since only the entries and comments after this instant, null for
   *          all
   * @return its change log and comments, oldest first when it has comments
   */
  private List<TaskChangeLog> getChangeLogs(TaskDto task, List<ChangeLogEntry> logs, Instant since) {
    List<TaskChangeLog> taskLogs = logs.stream()
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
    return taskLogs;
  }

  /**
   * @param tasks tasks listed with changed_since or completed_since
   * @param since the date the changes are listed from
   * @param includeChangeLog whether each task also comes with its change log
   * @param zone the user's time zone
   * @return their models, each with its changes since that date
   */
  private List<TaskWithChangesModel> toTaskWithChangesModels(List<TaskDto> tasks,
                                                             Date since,
                                                             boolean includeChangeLog,
                                                             ZoneId zone) {
    if (CollectionUtils.isEmpty(tasks)) {
      return Collections.emptyList();
    }
    LocalDate today = getToday(zone);
    return tasks.stream().map(task -> toTaskWithChangesModel(task, since, includeChangeLog, zone, today)).toList();
  }

  /**
   * @param task a task
   * @param since the date the changes are listed from
   * @param includeChangeLog whether the task also comes with its change log
   * @param zone the user's time zone
   * @param today today in that zone
   * @return its model, with its changes since that date and, on demand, its
   *         whole change log
   */
  @SneakyThrows
  private TaskWithChangesModel toTaskWithChangesModel(TaskDto task,
                                                      Date since,
                                                      boolean includeChangeLog,
                                                      ZoneId zone,
                                                      LocalDate today) {
    // every entry: the task was selected on an entry the newest ones may not hold
    List<ChangeLogEntry> logs = taskService.getTaskLogs(task.getId(), 0, -1); // NOSONAR
    return new TaskWithChangesModel(toTaskModel(task, zone, today),
                                    includeChangeLog ? getChangeLogs(task, logs.subList(0, Math.min(logs.size(), MAX_TO_LOAD)), null) : null,
                                    getTaskChanges(task, logs, since));
  }

  /**
   * Tells the status, project, due date, assignee and completion changes of
   * a task at or after a date, oldest first, from its change log. The log
   * records the new status, project and assignee of each change, so the old
   * value is the new value of the previous change of the same field, and
   * none when the log holds no earlier one: a task's first move leaves its
   * first status unrecorded. The log records no due date: the latest due
   * date change carries the current due date, the earlier ones none. A
   * completion entry is logged on each completion and each reopening, so
   * the latest one carries the current completion and the earlier ones
   * alternate. An assignee change of a task created at or after the date is
   * left out, as {@link TaskQuery#setChangedSince(Date)} leaves it out: a
   * task created with an assignee logs that assignment at its creation.
   *
   * @param task the task
   * @param logs its change log entries, in any order
   * @param since the earliest date of the changes, included
   * @return the changes
   */
  List<TaskChange> getTaskChanges(TaskDto task, List<ChangeLogEntry> logs, Date since) {
    if (CollectionUtils.isEmpty(logs)) {
      return Collections.emptyList();
    }
    List<ChangeLogEntry> chronological = logs.stream()
                                             .sorted(Comparator.comparingLong(ChangeLogEntry::getCreatedTime)
                                                               .thenComparingLong(ChangeLogEntry::getId))
                                             .toList();
    long completions = chronological.stream().filter(l -> TaskQuery.COMPLETION_ACTION.equals(l.getActionName())).count();
    ChangeLogEntry lastDueDateChange = chronological.stream()
                                                    .filter(l -> TaskQuery.DUE_DATE_CHANGE_ACTION.equals(l.getActionName()))
                                                    .reduce((first, second) -> second)
                                                    .orElse(null);
    List<TaskChange> changes = new ArrayList<>();
    String status = null;
    String project = null;
    String assignee = null;
    long completionsLeft = completions;
    boolean createdBefore = task.getCreatedTime() != null && task.getCreatedTime().before(since);
    for (ChangeLogEntry log : chronological) {
      String action = log.getActionName();
      boolean listed = log.getCreatedTime() >= since.getTime();
      String target = StringUtils.trimToNull(log.getTarget());
      String field = null;
      String oldValue = null;
      String newValue = null;
      if ("edit_status".equals(action)) {
        field = TaskChange.FIELD_STATUS;
        oldValue = getStatusLabel(status);
        newValue = getStatusLabel(target);
        status = target;
      } else if ("edit_project".equals(action) || "remove_project".equals(action)) {
        field = TaskChange.FIELD_PROJECT;
        oldValue = project;
        newValue = "edit_project".equals(action) ? target : null;
        project = newValue;
        status = null;
      } else if (("assign".equals(action) || "unassign".equals(action)) && !createdBefore) {
        assignee = "assign".equals(action) ? target : null;
      } else if ("assign".equals(action) || "unassign".equals(action)) {
        field = TaskChange.FIELD_ASSIGNEE;
        oldValue = "assign".equals(action) ? assignee : target;
        newValue = "assign".equals(action) ? target : null;
        assignee = newValue;
      } else if (TaskQuery.DUE_DATE_CHANGE_ACTION.equals(action)) {
        field = TaskChange.FIELD_DUE_DATE;
        newValue = log == lastDueDateChange ? formatDate(task.getDueDate()) : null;
      } else if (TaskQuery.COMPLETION_ACTION.equals(action)) {
        completionsLeft--;
        // the latest entry is the current completion, and each earlier one its opposite
        boolean completed = task.isCompleted() == (completionsLeft % 2 == 0);
        field = TaskChange.FIELD_COMPLETED;
        oldValue = String.valueOf(!completed);
        newValue = String.valueOf(completed);
      }
      if (field != null && listed) {
        changes.add(new TaskChange(field, oldValue, newValue, formatDate(log.getCreatedTime()), getUserModel(log.getAuthor())));
      }
    }
    return changes;
  }

  /**
   * Refuses to list the overdue tasks, which are open, among the tasks
   * completed since a date: the two filters together select nothing.
   *
   * @param overdueOnly whether only the overdue tasks are asked for
   * @param completedSince the completed_since date, null when not given
   * @throws IllegalArgumentException when both are given
   */
  private void checkNotOverdueAndCompleted(Boolean overdueOnly, Date completedSince) {
    if (Boolean.TRUE.equals(overdueOnly) && completedSince != null) {
      throw new IllegalArgumentException(MSG_OVERDUE_COMPLETED);
    }
  }

  /**
   * Parses the date a {@code changed_since} or {@code completed_since}
   * argument holds: a date, read as the start of that day in the user's time
   * zone; a date and time with an offset or {@code Z}; or a date and time
   * without one, read in the user's time zone.
   *
   * @param value the argument, possibly blank
   * @param argName the argument name, for the error message
   * @param zone the user's time zone
   * @return the date, null for a blank argument
   * @throws IllegalArgumentException when the argument is no such date
   */
  Date parseSince(String value, String argName, ZoneId zone) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    String text = value.trim();
    try {
      Instant instant;
      if (text.length() == ISO_DATE_LENGTH) {
        instant = LocalDate.parse(text).atStartOfDay(zone).toInstant();
      } else if (ISO_OFFSET_SUFFIX.matcher(text).find()) {
        instant = OffsetDateTime.parse(text).toInstant();
      } else {
        instant = LocalDateTime.parse(text).atZone(zone).toInstant();
      }
      return Date.from(instant);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(MSG_INVALID_SINCE.formatted(argName, value), e);
    }
  }

  /**
   * @param task a task, possibly null
   * @return its model, with its days overdue in the user's time zone
   */
  private TaskModel toTaskModel(TaskDto task) {
    ZoneId zone = getCurrentUserZone();
    return toTaskModel(task, zone, getToday(zone));
  }

  /**
   * @param task a task, possibly null
   * @param zone the user's time zone
   * @param today today in that zone
   * @return its model, with its days overdue when it is overdue
   */
  private TaskModel toTaskModel(TaskDto task, ZoneId zone, LocalDate today) {
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
                         space == null ? null : space.getSpaceId(),
                         getDaysOverdue(task, zone, today));
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

  /**
   * @param status a status, possibly null
   * @return its model, its name translated for a default status
   */
  private ProjectStatus getTaskStatus(StatusDto status) {
    if (status == null) {
      return null;
    } else {
      Long id = status.getId();
      int rank = status.getRank() == null ? 0 : status.getRank();
      return new ProjectStatus(id, getStatusLabel(status.getName()), rank);
    }
  }

  /**
   * @param name a status name, possibly null
   * @return its label in the user's language for a default status, else the
   *         name itself
   */
  private String getStatusLabel(String name) {
    if (name == null) {
      return null;
    }
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle("locale.portlet.taskManagement",
                                                                            getCurrentUserLocale());
    if (resourceBundle.containsKey("tasks.status.%s".formatted(name))) {
      return resourceBundle.getString("tasks.status.%s".formatted(name));
    }
    return name;
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

  private LabelDto getEditableLabel(long labelId) throws ObjectNotFoundException, IllegalAccessException {
    LabelDto label = labelService.getLabel(labelId);
    if (label == null) {
      throw new ObjectNotFoundException(MSG_LABEL_NOT_FOUND.formatted(labelId, ""));
    }
    ProjectDto project = label.getProject();
    if (project != null) {
      if (!project.canEdit(getCurrentUserAclIdentity())) {
        throw new IllegalAccessException(MSG_TASK_PROJECT_NOT_EDITABLE.formatted(project.getId(), getCurrentUserName()));
      }
    } else if (!StringUtils.equals(label.getUsername(), getCurrentUserName())) {
      throw new IllegalAccessException("Label '%s' is not editable by user '%s'.".formatted(labelId, getCurrentUserName()));
    }
    return label;
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

  /**
   * @param space an existing space, or null
   * @return the memberships whose projects are listed: those of the current
   *         user, or, for a space, those of its group the current user holds
   *         (none when the user isn't a member, which never means "every
   *         project" to the query)
   */
  private List<String> getMemberships(Space space) {
    List<String> userMemberships = UserUtil.getMemberships(getCurrentUserAclIdentity());
    if (space == null) {
      return userMemberships;
    }
    return UserUtil.getSpaceMemberships(space.getGroupId())
                   .stream()
                   .filter(userMemberships::contains)
                   .toList();
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

  /**
   * Checks a label can be put on or taken off a task: it exists and, when the
   * task is in a project, it is one of that project's labels. The label is
   * the only argument of a label change beside the task, so in a call limited
   * to the space of the task's project it can't reach a label of another
   * project, of another space.
   *
   * @param task the task
   * @param labelId the label
   * @throws ObjectNotFoundException when the label doesn't exist or belongs
   *           to another project than the task's, with the project's labels
   */
  private void checkTaskLabel(TaskDto task, Long labelId) throws ObjectNotFoundException {
    long projectId = getProjectId(task);
    LabelDto label = labelService.getLabel(labelId);
    if (label == null
        || (projectId > 0 && (label.getProject() == null || label.getProject().getId() != projectId))) {
      throwMissingLabel(labelId, projectId);
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
