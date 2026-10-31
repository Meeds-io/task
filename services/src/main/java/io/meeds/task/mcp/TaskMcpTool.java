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
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.CommentsSummaryDto;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.model.TaskChangeFilter;
import org.exoplatform.task.model.TaskChanges;
import org.exoplatform.task.model.TaskFieldChange;
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
import io.meeds.task.mcp.model.ProjectListItemModel;
import io.meeds.task.mcp.model.ProjectModel;
import io.meeds.task.mcp.model.ProjectStatisticsModel;
import io.meeds.task.mcp.model.ProjectStatus;
import io.meeds.task.mcp.model.TaskAssigneeModel;
import io.meeds.task.mcp.model.TaskChangeLog;
import io.meeds.task.mcp.model.TaskCollectionModel;
import io.meeds.task.mcp.model.TaskCommentCollectionModel;
import io.meeds.task.mcp.model.TaskCommentModel;
import io.meeds.task.mcp.model.TaskListItemModel;
import io.meeds.task.mcp.util.TaskDescriptionText;
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

  /**
   * The most items a list tool returns at a time, whatever limit it is given:
   * the "max 100" the tool definitions state, kept apart from the search
   * window of {@link TaskService} so that retuning one never changes the
   * other.
   */
  private static final int              MAX_LIMIT                       = 100;

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
   * means that more exist; {@code has_more} is exact in both cases.
   * <p>
   * With {@code name}, only the projects whose name holds all its words,
   * each one ignoring case, among those the other arguments select; when
   * none of them does and {@code name} holds more than one word, those whose
   * name holds any one of its words. Each
   * project comes as a list item, without its description, statuses or
   * labels, which {@link #getProjectById(Long)} returns.
   *
   * @param spaceId the space whose projects are listed, null or not positive
   *          for the projects of the current user
   * @param offset the first project returned
   * @param limit the most projects returned, {@value #MAX_LIMIT} at most
   * @param name words of the name of the projects listed, blank for any
   * @return the projects, with their count and whether more remain
   */
  public ProjectCollectionModel listProjects(Long spaceId,
                                             Integer offset,
                                             Integer limit,
                                             String name) {
    boolean spaceGiven = spaceId != null && spaceId > 0;
    Space space = spaceGiven ? spaceService.getSpaceById(spaceId) : null;
    List<String> memberships = getMemberships(space);
    int usedOffset = getOffset(offset);
    int usedLimit = getLimit(limit);
    String keyword = StringUtils.isBlank(name) ? null : name.trim();
    if (memberships.isEmpty() || spaceGiven && space == null) {
      return toProjectCollection(Collections.emptyList(), usedOffset, usedLimit, 0);
    } else if (space != null && isSpaceScoped()) {
      return listSpaceProjects(space, memberships, keyword, usedOffset, usedLimit);
    }
    ProjectQuery allWords = getAllWordsQuery(memberships, keyword);
    if (allWords != null) {
      int allWordsCount = projectService.countProjects(allWords);
      if (allWordsCount > 0) {
        List<ProjectListItemModel> projects = toProjectListItems(projectService.findProjects(allWords, usedOffset, usedLimit));
        return toProjectCollection(projects, usedOffset, usedLimit, allWordsCount);
      } else if (!hasSeveralWords(keyword)) {
        return toProjectCollection(Collections.emptyList(), usedOffset, usedLimit, 0);
      }
    }
    List<ProjectListItemModel> projects = getProjectListItems(memberships, keyword, usedOffset, usedLimit);
    long count = projectService.countProjects(getAnyWordQuery(memberships, keyword));
    return toProjectCollection(projects, usedOffset, usedLimit, count);
  }

  /**
   * Builds the query of the projects the memberships reach whose name holds
   * any one word of the keyword, the most recently modified first: the
   * selection {@link ProjectService#findProjects(List, String, OrderBy, int, int)}
   * lists with that order, so that its count, distinct like the list, counts
   * each project once whatever the number of its managers and participants.
   *
   * @param memberships the memberships whose projects are counted
   * @param keyword words of the name of the projects, null for any
   * @return the query
   */
  private ProjectQuery getAnyWordQuery(List<String> memberships, String keyword) {
    ProjectQuery query = new ProjectQuery();
    query.setMembership(memberships);
    query.setKeyword(keyword);
    query.setOrderBy(List.of(new OrderBy.DESC("lastModifiedDate")));
    return query;
  }

  /**
   * @param keyword words of the name of the projects, not null
   * @return true when the keyword holds more than one word, so that the
   *         projects holding any one of them may be more than those holding
   *         all of them
   */
  private static boolean hasSeveralWords(String keyword) {
    return StringUtils.split(keyword, ' ').length > 1;
  }

  /**
   * Builds the query of the projects the memberships reach whose name holds
   * every word of the keyword, the most recently modified first: the same
   * memberships and order as the list by any word of the keyword.
   *
   * @param memberships the memberships whose projects are listed
   * @param keyword words of the name of the projects, null for any
   * @return the query, or null when there is no keyword
   */
  private ProjectQuery getAllWordsQuery(List<String> memberships, String keyword) {
    if (keyword == null) {
      return null;
    }
    ProjectQuery query = new ProjectQuery();
    query.setMembership(memberships);
    query.setNameWords(keyword);
    query.setOrderBy(List.of(new OrderBy.DESC("lastModifiedDate")));
    return query;
  }

  /**
   * @param limit the limit a tool call gives, possibly null, zero or negative
   * @return the limit used: {@value McpToolPlugin#DEFAULT_LIMIT} when none
   *         or not positive, {@value #MAX_LIMIT} at most
   */
  static int getLimit(Integer limit) {
    int value = getInteger(limit, DEFAULT_LIMIT);
    return value < 1 ? DEFAULT_LIMIT : Math.min(value, MAX_LIMIT);
  }

  /**
   * @param offset the offset a tool call gives, possibly null or negative
   * @return the offset used, never negative
   */
  static int getOffset(Integer offset) {
    return Math.max(getInteger(offset, DEFAULT_OFFSET), 0);
  }

  /**
   * @param projects the projects of a page
   * @param offset the offset of the page
   * @param limit the limit of the page
   * @param count the count of the projects the listing selects
   * @return the page, saying whether more remain after it and from which
   *         offset
   */
  private ProjectCollectionModel toProjectCollection(List<ProjectListItemModel> projects,
                                                     int offset,
                                                     int limit,
                                                     long count) {
    Integer nextOffset = getNextOffset(projects.size(), offset, count);
    return new ProjectCollectionModel(projects, offset, limit, count, nextOffset != null, nextOffset);
  }

  /**
   * @param tasks the tasks of a page
   * @param offset the offset of the page
   * @param limit the limit of the page
   * @param count the count of the tasks the listing selects
   * @return the page, saying whether more remain after it and from which
   *         offset
   */
  private TaskCollectionModel toTaskCollection(List<?> tasks, int offset, int limit, long count) {
    Integer nextOffset = getNextOffset(tasks.size(), offset, count);
    return new TaskCollectionModel(tasks, offset, limit, count, nextOffset != null, nextOffset);
  }

  /**
   * @param pageSize the items of a page
   * @param offset the offset of the page
   * @param count the count of the items the listing selects
   * @return the offset of the next page when items remain after this one,
   *         null otherwise, an empty page included so that no caller loops on
   *         the same offset
   */
  static Integer getNextOffset(int pageSize, int offset, long count) {
    int next = offset + pageSize;
    return pageSize > 0 && next < count ? next : null;
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
   * Lists the projects of the space whose name holds every word of the
   * keyword; when the space has none and the keyword holds more than one
   * word, those whose name holds any one of its words. Each selection is read
   * by {@link #scanSpaceProjects}.
   *
   * @param space an existing space
   * @param memberships the roles of the space group the current user holds
   * @param keyword words of the name of the projects, null for any
   * @param offset the first project returned, not negative
   * @param limit the most projects returned, positive
   * @return the projects of that space among those the memberships reach,
   *         with the count described on
   *         {@link #listProjects(Long, Integer, Integer, String)}
   */
  private ProjectCollectionModel listSpaceProjects(Space space,
                                                   List<String> memberships,
                                                   String keyword,
                                                   int offset,
                                                   int limit) {
    ProjectQuery allWords = getAllWordsQuery(memberships, keyword);
    if (allWords != null) {
      List<ProjectDto> ofSpace = Collections.emptyList();
      if (projectService.countProjects(allWords) > 0) {
        ofSpace = scanSpaceProjects(space, (from, size) -> projectService.findProjects(allWords, from, size), offset + limit + 1);
      }
      if (!ofSpace.isEmpty() || !hasSeveralWords(keyword)) {
        return toSpaceProjectCollection(ofSpace, offset, limit);
      }
    }
    List<ProjectDto> ofSpace = scanSpaceProjects(space,
                                                 (from, size) -> projectService.findProjects(memberships,
                                                                                             keyword,
                                                                                             new OrderBy.DESC("lastModifiedDate"),
                                                                                             from,
                                                                                             size),
                                                 offset + limit + 1);
    return toSpaceProjectCollection(ofSpace, offset, limit);
  }

  /**
   * Scans a selection of projects in chunks of {@value #SPACE_SCAN_CHUNK},
   * in its order, and keeps those of the space. The scan stops once the
   * wanted number of projects is kept, so that one more kept project than the
   * offset and the page tells exactly that more remain.
   *
   * @param space an existing space
   * @param reader reads the projects of the selection from an offset, at
   *          most a given number of them
   * @param wanted the number of kept projects that stops the scan
   * @return the projects of that space, in the order of the selection, at
   *         most the wanted number
   */
  private List<ProjectDto> scanSpaceProjects(Space space,
                                             BiFunction<Integer, Integer, List<ProjectDto>> reader,
                                             int wanted) {
    List<ProjectDto> ofSpace = new ArrayList<>();
    int from = 0;
    boolean finished = false;
    while (!finished && ofSpace.size() < wanted) {
      List<ProjectDto> chunk = reader.apply(from, SPACE_SCAN_CHUNK);
      int read = chunk == null ? 0 : chunk.size();
      finished = read < SPACE_SCAN_CHUNK;
      from += read;
      for (int i = 0; i < read && ofSpace.size() < wanted; i++) {
        if (isProjectOfSpace(chunk.get(i), space)) {
          ofSpace.add(chunk.get(i));
        }
      }
    }
    return ofSpace;
  }

  /**
   * @param ofSpace the projects of a space a scan kept
   * @param offset the first project returned, not negative
   * @param limit the most projects returned, positive
   * @return the page of those projects, their count being the number kept
   */
  private ProjectCollectionModel toSpaceProjectCollection(List<ProjectDto> ofSpace, int offset, int limit) {
    List<ProjectListItemModel> page = ofSpace.stream()
                                             .skip(offset)
                                             .limit(limit)
                                             .map(this::toProjectListItem)
                                             .toList();
    return toProjectCollection(page, offset, limit, ofSpace.size());
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
   * a date is refused. Without any of these filters, the open tasks, the most
   * recently active first. Each task comes as a list item, without its
   * description or coworkers, which {@link #getTaskById(Long)} returns; with
   * {@code includeDescription}, with its description as cut plain text.
   *
   * @param offset the first task returned
   * @param limit the most tasks returned, {@value #MAX_LIMIT} at most
   * @param overdueOnly whether only the overdue tasks are listed
   * @param changedSince a date or date-time, blank for no condition
   * @param completedSince a date or date-time, blank for no condition
   * @param includeDescription whether each task carries its description as
   *          plain text, cut at {@value TaskDescriptionText#MAX_LENGTH}
   *          characters
   * @return the tasks, with their count and whether more remain
   */
  public TaskCollectionModel listAssignedTasks(Integer offset,
                                               Integer limit,
                                               Boolean overdueOnly,
                                               String changedSince,
                                               String completedSince,
                                               Boolean includeDescription) {
    ZoneId zone = getCurrentUserZone();
    int usedOffset = getOffset(offset);
    int usedLimit = getLimit(limit);
    Date changedSinceDate = parseSince(changedSince, ARG_CHANGED_SINCE, zone);
    Date completedSinceDate = parseSince(completedSince, ARG_COMPLETED_SINCE, zone);
    Date since = changedSinceDate == null ? completedSinceDate : changedSinceDate;
    checkNotOverdueAndCompleted(overdueOnly, completedSinceDate);
    boolean filtered = Boolean.TRUE.equals(overdueOnly) || since != null;
    TaskQuery taskQuery = taskService.getAssignedTasksQuery(getCurrentUserName(),
                                                            new TaskChangeFilter(changedSinceDate,
                                                                                 completedSinceDate,
                                                                                 null,
                                                                                 null,
                                                                                 !filtered));
    List<TaskDto> tasks;
    if (Boolean.TRUE.equals(overdueOnly)) {
      setOverdueOnly(taskQuery, zone);
      tasks = findOverdueTasks(taskQuery, usedOffset, usedLimit);
    } else {
      tasks = taskService.findLastUpdatedTasks(taskQuery, usedOffset, usedLimit);
    }
    return toTaskCollection(toTaskListItems(tasks, since, zone, Boolean.TRUE.equals(includeDescription)), usedOffset, usedLimit, countTasks(taskQuery));
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
   * <p>
   * Each task comes as a list item, without its description or coworkers,
   * which {@link #getTaskById(Long)} returns; with {@code includeDescription},
   * with its description as plain text, cut at
   * {@value TaskDescriptionText#MAX_LENGTH} characters; with
   * {@code includeChangeLog}, each comes in full, with its change log.
   *
   * @param projectId the project, null for every accessible task
   * @param hideCompletedTasks whether the completed tasks are left out
   * @param includeChangeLog whether each task comes in full, with its change
   *          log
   * @param overdueOnly whether only the overdue tasks are listed
   * @param offset the first task returned
   * @param limit the most tasks returned, {@value #MAX_LIMIT} at most
   * @param title a part of the title of the tasks listed, blank for any
   * @param changedSince a date or date-time, blank for no condition
   * @param completedSince a date or date-time, blank for no condition
   * @param assignee the username of the assignee, blank for any
   * @param statusId the status of the tasks listed, null for any
   * @param includeDescription whether each list item carries its description
   *          as cut plain text
   * @return the tasks, with their count and whether more remain
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
                                       Long statusId,
                                       Boolean includeDescription) throws IllegalAccessException, ObjectNotFoundException {
    ZoneId zone = getCurrentUserZone();
    LocalDate today = getToday(zone);
    Date changedSinceDate = parseSince(changedSince, ARG_CHANGED_SINCE, zone);
    Date completedSinceDate = parseSince(completedSince, ARG_COMPLETED_SINCE, zone);
    checkNotOverdueAndCompleted(overdueOnly, completedSinceDate);
    TaskQuery taskQuery = getTaskQuery(projectId, false, Boolean.TRUE.equals(overdueOnly), zone);
    if (StringUtils.isNotBlank(title)) {
      // TaskQuery#setTitle compares the lower-cased title with the value as given
      taskQuery.setTitle(title.trim().toLowerCase(Locale.ROOT));
    }
    taskService.addChangeFilter(taskQuery,
                                new TaskChangeFilter(changedSinceDate,
                                                     completedSinceDate,
                                                     assignee,
                                                     statusId,
                                                     Boolean.TRUE.equals(hideCompletedTasks)));
    int usedOffset = getOffset(offset);
    int usedLimit = getLimit(limit);
    List<TaskDto> tasks = Boolean.TRUE.equals(overdueOnly) ? findOverdueTasks(taskQuery, usedOffset, usedLimit) :
                                                           taskService.findLastUpdatedTasks(taskQuery, usedOffset, usedLimit);
    Date since = changedSinceDate == null ? completedSinceDate : changedSinceDate;
    List<?> taskModels;
    if (!Boolean.TRUE.equals(includeChangeLog)) {
      taskModels = toTaskListItems(tasks, since, zone, Boolean.TRUE.equals(includeDescription));
    } else if (since != null) {
      taskModels = toTaskWithChangesModels(tasks, since, true, zone);
    } else {
      taskModels = tasks.stream().map(t -> toTaskWithChangeLogModel(t, null, zone, today)).toList();
    }
    return toTaskCollection(taskModels, usedOffset, usedLimit, countTasks(taskQuery));
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

  /**
   * Lists the comments of a task, each followed by its replies.
   *
   * @param taskId the task
   * @param offset the first comment returned
   * @param limit the most comments returned, replies aside,
   *          {@value #MAX_LIMIT} at most
   * @return the comments, with their count
   * @throws ObjectNotFoundException when the task doesn't exist
   * @throws IllegalAccessException when the user can't view the task
   */
  public TaskCommentCollectionModel listTaskCommentsById(Long taskId,
                                                         Integer offset,
                                                         Integer limit) throws ObjectNotFoundException, IllegalAccessException {
    TaskDto task = getTaskDetailById(taskId);
    int usedOffset = getOffset(offset);
    int usedLimit = getLimit(limit);
    List<TaskCommentModel> taskComments = getTaskCommentModels(task, usedOffset, usedLimit);
    int count = countTaskComments(task.getId());
    return new TaskCommentCollectionModel(taskComments, usedOffset, usedLimit, count);
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

  /**
   * Lists the open tasks of a project that have neither a start nor a due
   * date, among its {@value #MAX_TO_LOAD} most recently active open tasks.
   * Each task comes as a list item, with its description as cut plain text
   * with {@code includeDescription}. The tool takes no offset: {@code has_more}
   * says more unscheduled tasks were found than the limit let through, and
   * no {@code next_offset} is given.
   *
   * @param projectId the project
   * @param limit the most tasks returned, {@value #MAX_LIMIT} at most
   * @param includeDescription whether each task carries its description as
   *          cut plain text
   * @return the tasks, with the count of those returned and whether more
   *         were found
   * @throws IllegalAccessException when the user can't view the project
   * @throws ObjectNotFoundException when the project doesn't exist
   */
  public TaskCollectionModel listUnscheduledTasks(Long projectId,
                                                  Integer limit,
                                                  Boolean includeDescription) throws IllegalAccessException, ObjectNotFoundException {
    int max = getLimit(limit);
    ZoneId zone = getCurrentUserZone();
    List<TaskDto> unscheduled = getTasks(projectId, 0, MAX_TO_LOAD, true).stream()
                                                                        .filter(t -> t.getStartDate() == null
                                                                                     && t.getDueDate() == null)
                                                                        .limit(max + 1L)
                                                                        .toList();
    boolean hasMore = unscheduled.size() > max;
    List<TaskListItemModel> page = toTaskListItems(hasMore ? unscheduled.subList(0, max) : unscheduled,
                                                    null,
                                                    zone,
                                                    Boolean.TRUE.equals(includeDescription));
    return new TaskCollectionModel(page, 0, max, page.size(), hasMore, null);
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

  /**
   * @param memberships the memberships whose projects are listed
   * @param keyword words of the name of the projects, null for any
   * @param offset the first project returned
   * @param limit the most projects returned
   * @return the projects of the page, the most recently modified first, as
   *         list items
   */
  private List<ProjectListItemModel> getProjectListItems(List<String> memberships, String keyword, int offset, int limit) {
    List<ProjectDto> projects = projectService.findProjects(memberships,
                                                            keyword,
                                                            new OrderBy.DESC("lastModifiedDate"),
                                                            offset,
                                                            limit);
    return toProjectListItems(projects);
  }

  /**
   * @param projects projects, possibly null
   * @return their list items, in the same order
   */
  private List<ProjectListItemModel> toProjectListItems(List<ProjectDto> projects) {
    if (CollectionUtils.isEmpty(projects)) {
      return Collections.emptyList();
    } else {
      return projects.stream()
                     .map(this::toProjectListItem)
                     .toList();
    }
  }

  /**
   * @param project a project
   * @return its list item: its identifier, name, space and link, without
   *         reading its statuses or labels
   */
  private ProjectListItemModel toProjectListItem(ProjectDto project) {
    Space space = getProjectSpace(project);
    return new ProjectListItemModel(project.getId(),
                                    project.getName(),
                                    space == null ? null : space.getSpaceId(),
                                    getUrl(project));
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
   * @param since the date a listing with {@code changed_since} or
   *          {@code completed_since} lists the changes from, null otherwise
   * @param zone the user's time zone
   * @param includeDescription whether each item carries its description as
   *          cut plain text
   * @return their list items, each overdue one with its days overdue, each
   *         with its changes since that date when one is given, each
   *         commented one with its comments count and latest comment, all read
   *         in two queries, whatever the number of tasks
   */
  private List<TaskListItemModel> toTaskListItems(List<TaskDto> tasks,
                                                  Date since,
                                                  ZoneId zone,
                                                  boolean includeDescription) {
    if (CollectionUtils.isEmpty(tasks)) {
      return Collections.emptyList();
    }
    LocalDate today = getToday(zone);
    Identity aclIdentity = getCurrentUserAclIdentity();
    Map<Long, CommentsSummaryDto> commentsSummaries = commentService.getCommentsSummaries(tasks.stream()
                                                                                               .map(TaskDto::getId)
                                                                                               .toList());
    return tasks.stream().map(task -> {
      TaskListItemModel item = toTaskListItem(task, since, zone, today, aclIdentity);
      CommentsSummaryDto commentsSummary = commentsSummaries.get(task.getId());
      if (commentsSummary != null) {
        item.setCommentsCount((int) commentsSummary.getCount());
        item.setLastComment(formatDate(commentsSummary.getLastCommentDate()),
                            getTaskAssignee(commentsSummary.getLastCommentAuthor()));
      }
      if (includeDescription) {
        // the stored HTML read as text, without the rendering that resolves links and mentions
        item.setPlainTextDescription(TaskDescriptionText.toPlainText(task.getDescription()));
      }
      return item;
    }).toList();
  }

  /**
   * Maps a task to its list item: what tells it apart on its board, as a
   * search result, with its assignee, due date, creation date and priority,
   * without its comments, which {@link #toTaskListItems} reads for the whole
   * page. It reads neither the description, nor the coworkers, nor the last
   * update date that the full model holds.
   *
   * @param task a task
   * @param since the date the changes are listed from, null for none
   * @param zone the user's time zone
   * @param today today in that zone
   * @param aclIdentity the current user, who reads the labels
   * @return its list item
   */
  @SneakyThrows
  private TaskListItemModel toTaskListItem(TaskDto task, Date since, ZoneId zone, LocalDate today, Identity aclIdentity) {
    StatusDto status = task.getStatus();
    ProjectDto project = status == null ? null : status.getProject();
    Long projectId = project == null ? null : project.getId();
    List<TaskChange> changes = since == null ? null :
                                             CollectionUtils.emptyIfNull(taskService.getTaskChanges(task, since, false)
                                                                                    .getChanges())
                                                            .stream()
                                                            .map(this::toTaskChange)
                                                            .toList();
    return new TaskListItemModel(task.getId(),
                                 task.getTitle(),
                                 getUrl(task),
                                 task.isCompleted(),
                                 projectId,
                                 project == null ? null : project.getName(),
                                 getTaskStatus(status),
                                 project == null ? null : getTaskLabels(task, projectId, aclIdentity),
                                 getTaskAssignee(task.getAssignee()),
                                 formatDate(task.getDueDate()),
                                 formatDate(task.getCreatedTime()),
                                 getDaysOverdue(task, zone, today),
                                 task.getPriority(),
                                 0,
                                 changes);
  }

  /**
   * @param username the username of an assignee, possibly blank
   * @return the assignee's username and display name, null for none; the
   *         username alone when the user has no profile
   */
  private TaskAssigneeModel getTaskAssignee(String username) {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    org.exoplatform.social.core.identity.model.Identity identity = identityManager.getOrCreateUserIdentity(username);
    String displayName = identity == null || identity.getProfile() == null ? null : identity.getProfile().getFullName();
    return new TaskAssigneeModel(username, displayName);
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
                            transformHtmlContent(project.getDescription(), aclIdentity, locale),
                            getUrl(project),
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
   * @return its change log and comments, replies included, oldest first when
   *         it has comments
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
      // the count bounds the top-level comments, each read with its replies
      List<TaskChangeLog> commentTaskLogs = commentService.getCommentsWithSubs(task.getId(), 0, commentsCount)
                                                          .stream()
                                                          .flatMap(c -> Stream.concat(Stream.of(c),
                                                                                      CollectionUtils.emptyIfNull(c.getSubComments())
                                                                                                     .stream()))
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
      // an undated entry (recorded before 7.2 on an upgraded PostgreSQL, Oracle or SQL Server database) is the oldest
      taskLogs.sort(Comparator.comparing(TaskChangeLog::getActionDate, Comparator.nullsFirst(Comparator.naturalOrder())));
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
   * @return its model, with its changes since that date, as the task service
   *         rebuilds them, and, on demand, its newest change log entries
   */
  @SneakyThrows
  private TaskWithChangesModel toTaskWithChangesModel(TaskDto task,
                                                      Date since,
                                                      boolean includeChangeLog,
                                                      ZoneId zone,
                                                      LocalDate today) {
    TaskChanges taskChanges = taskService.getTaskChanges(task, since, includeChangeLog);
    List<TaskChangeLog> changeLog = taskChanges.getChangeLog() == null ? null : getChangeLogs(task, taskChanges.getChangeLog(), null);
    List<TaskChange> changes = CollectionUtils.emptyIfNull(taskChanges.getChanges()).stream().map(this::toTaskChange).toList();
    return new TaskWithChangesModel(toTaskModel(task, zone, today), changeLog, changes);
  }

  /**
   * @param change a change the task service rebuilt
   * @return its model: a status in the user's language for a default status,
   *         a date in the user's time zone, the author as a user
   */
  TaskChange toTaskChange(TaskFieldChange change) {
    boolean status = TaskFieldChange.FIELD_STATUS.equals(change.getField());
    String newValue = TaskFieldChange.FIELD_DUE_DATE.equals(change.getField()) ? formatDate(change.getNewDate()) : change.getNewValue();
    return new TaskChange(change.getField(),
                          status ? getStatusLabel(change.getOldValue()) : change.getOldValue(),
                          status ? getStatusLabel(newValue) : newValue,
                          formatDate(change.getCreatedTime()),
                          getUserModel(change.getAuthor()));
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
    int commentsCount = commentService.countCommentsWithSubs(task.getId());
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

  /**
   * @param changeLog a change log entry
   * @return its model; its target is a user for an assignment entry only,
   *         the other entries naming a status or a project there, never a
   *         user
   */
  private TaskChangeLog getTaskChangeLog(ChangeLogEntry changeLog) {
    return new TaskChangeLog(changeLog.getActionName(),
                             getActionLabel(changeLog.getActionName()),
                             getUserModel(changeLog.getAuthor()),
                             TaskService.USER_TARGET_ACTIONS.contains(changeLog.getActionName()) ? getUserModel(changeLog.getTarget()) : null,
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

  /**
   * The date of a task's latest activity: its newest dated change log entry or
   * its last comment, else its creation. The entries are read newest first,
   * the undated ones last, and an undated entry (0, recorded before 7.2 on an
   * upgraded PostgreSQL, Oracle or SQL Server database) dates nothing.
   *
   * @param task the task
   * @return the date of its latest activity
   */
  @SneakyThrows
  private Date getLastUpdateDate(TaskDto task) {
    Date lastUpdateDate = task.getCreatedTime();
    List<ChangeLogEntry> taskLogs = taskService.getTaskLogs(task.getId(), 0, 1);
    if (CollectionUtils.isNotEmpty(taskLogs) && taskLogs.get(0).getCreatedTime() > 0) {
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
