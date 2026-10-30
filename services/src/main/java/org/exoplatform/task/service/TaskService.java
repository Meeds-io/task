/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.exoplatform.task.service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.dto.TasksList;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.model.TaskChangeFilter;
import org.exoplatform.task.model.TaskChanges;
import org.exoplatform.task.model.TaskListFilter;
import org.exoplatform.task.model.TaskSearchFilter;
import org.exoplatform.task.model.TaskTextSearchResult;
import org.exoplatform.task.util.TaskUtil;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;


public interface TaskService {

    /** The most change log entries one page of {@link #getTaskLogsBefore} reads. */
    int MAX_TASK_LOGS_PAGE = 100;

    /** The most hits a text search reads, the most relevant first. */
    int    TEXT_SEARCH_WINDOW  = 100;

    /** The code of the refusal of a text search holding no word with letters. */
    String TEXT_SEARCH_NO_WORD = "task.search.noWord";

    /**
     * The change log actions whose target is a user, an assignee or a
     * coworker; the other actions name a status, a project or nothing there
     * (EXO-91902).
     */
    List<String> USER_TARGET_ACTIONS = List.of("assign", "unassign", "assignCoworker", "unassignCoworker");


    /**
     * Create a new task.
     * @param task the given task.
     * @return the created task.
     */
    TaskDto createTask(TaskDto task);

    /**
     * Update the task.
     *
     * @param task the given task.
     * @return TaskDto the updated task.
     */
    TaskDto updateTask(TaskDto task);

    /**
     * Update a task on behalf of a user. The permission is checked on the
     * stored task, and the update applies to that task whatever id the given
     * task carries. A given status is loaded by its id, and moving the task to
     * a status of another project requires the user to view that project.
     *
     * @param taskId the id of the task to update.
     * @param task the updated task.
     * @param identity the user updating the task.
     * @return TaskDto the updated task.
     * @throws ObjectNotFoundException when the task is not found.
     * @throws IllegalAccessException when the user cannot edit the task, or
     *           cannot view the project of the given status.
     * @throws IllegalArgumentException when the given status is not found.
     */
    TaskDto updateTask(long taskId, TaskDto task, Identity identity) throws ObjectNotFoundException, IllegalAccessException;

    void updateTaskOrder(long currentTaskId, Status newStatus, long[] orders);

    /**
     * Remove the task with given <code>taskId</code>
     *
     * @param taskId the given task id.
     * @throws EntityNotFoundException when task is not found.
     */
    void removeTask(long taskId) throws EntityNotFoundException;

    /**
     * Clone the task from a task with given <code>taskId</code>.
     * @param taskId the task Id
     * @return the cloned task.
     * @throws EntityNotFoundException when task is not found.
     */
    TaskDto cloneTask(long taskId) throws EntityNotFoundException;

    /**
     * Return the task with given <code>taskId</code>.
     *
     * @param taskId the task Id
     * @return the given task.
     * @throws EntityNotFoundException when task is not found.
     */
    TaskDto getTask(long taskId) throws EntityNotFoundException;


    List<TaskDto> getWatchedTasks(String user, int limit);

    Long countWatchedTasks(String user);

    List<TaskDto> getAssignedTasks(String user, int limit);

    Long countAssignedTasks(String user);

    List<TaskDto> getCollaboratedTasks(String user, int limit);

    Long countCollaboratedTasks(String user);

    List<TaskDto> findTasksByMemberShips(String user, List<String> memberships, String query, int limit);

    List<TaskDto> findTasks(String user, String query, int limit);


    int countTasks(TaskQuery query) throws Exception;

    public <T> List<T> selectTaskField(TaskQuery query, String fieldName);

    List<TaskDto> findTasksByLabel(LabelDto label, List<Long> projectIds, String username, OrderBy orderBy, int offset, int limit) throws Exception;

    int countTasksByLabel(LabelDto label, List<Long> projectIds, String username, OrderBy orderBy) throws Exception;

    /**
     * Create a log associated with a task with given <code>taskId</code>.
     *
     * @param taskId     the given task id.
     * @param username   the given username.
     * @param actionName the given action name.
     * @param target     the given target.
     * @return add task log.
     * @throws EntityNotFoundException when task is not found.
     */
    ChangeLogEntry addTaskLog(long taskId, String username, String actionName, String target) throws EntityNotFoundException;


    List<ChangeLogEntry> getTaskLogs(long taskId, int offset, int limit) throws Exception;

    /**
     * Reads one page of a task's change log, newest first, after the last
     * entry already read: entries added while a reader pages are never read
     * twice nor skipped.
     *
     * @param taskId the task id
     * @param beforeTime the date, in milliseconds, of the last entry already
     *          read, or null to read from the newest
     * @param beforeId the id of the last entry already read, required with
     *          beforeTime and refused without it
     * @param limit the maximum number of entries, from 1 to
     *          {@link #MAX_TASK_LOGS_PAGE}
     * @return the entries older than the last one read, or dated alike with a
     *         lower id, newest first and, on the same date, highest id first
     * @throws IllegalArgumentException with the code
     *           {@code task.logs.invalidCursor} when only one of beforeTime and
     *           beforeId is given, {@code task.logs.invalidLimit} when the
     *           limit is out of bounds
     */
    List<ChangeLogEntry> getTaskLogsBefore(long taskId, Long beforeTime, Long beforeId, int limit);

    /**
     * Narrows a task query with the conditions of a change filter
     * (EXO-91902): the tasks whose status, project, due date, assignee or
     * completion changed since a date, an assignee change counting only on a
     * task created before it; the tasks completed since a date; those
     * assigned to one user; those in one status; the completed tasks left out
     * on demand, unless the completed ones are asked for.
     *
     * @param query the query to narrow, already scoped to what the user can see
     * @param filter the change filter, possibly null
     */
    void addChangeFilter(TaskQuery query, TaskChangeFilter filter);

    /**
     * Builds the query of the tasks a user is assigned to or a coworker of,
     * narrowed by a change filter (EXO-91902).
     *
     * @param username the user
     * @param filter the change filter, possibly null
     * @return the query
     */
    TaskQuery getAssignedTasksQuery(String username, TaskChangeFilter filter);

    /**
     * Rebuilds the changes of a task since a date from its whole change log
     * (EXO-91902), oldest first: its status, project, due date, assignee and
     * completion changes, each with its old and new value when the log tells
     * it. On demand, also its newest change log entries, at most a hundred.
     *
     * @param task the task
     * @param since the earliest date of the changes, included; required
     * @param includeChangeLog whether its newest change log entries come too
     * @return its changes
     * @throws IllegalArgumentException when since is null
     * @throws Exception when its change log can't be read
     */
    TaskChanges getTaskChanges(TaskDto task, Date since, boolean includeChangeLog) throws Exception;

    //TODO: should use via #findTasks(TaskQuery)?
    TaskDto findTaskByActivityId(String activityId);

    Set<String> getCoworker(long taskId);


    Set<String> getMentionedUsers(long taskId);

    List<TaskDto> getUncompletedTasks(String user, int limit);

    Long countUncompletedTasks(String user);

    List<TaskDto> getIncomingTasks(String user, int offset, int limit) throws Exception;

    int countIncomingTasks(String user) throws Exception;

    List<TaskDto> getOverdueTasks(String user, int limit);

    Long countOverdueTasks(String user);

    Set<String> getWatchersOfTask(TaskDto task);

    void addWatcherToTask(String username, TaskDto task) throws Exception;

    void deleteWatcherOfTask(String username, TaskDto task) throws Exception;

    /**
     * Find tasks assigned to a user using a term to find in title or description
     * of the task
     *
     * @param query term to search in title or description
     * @param offset term to offset results.
     * @param limit term to limit results.
     * @return {@link List} of {@link TaskDto}
     * @throws Exception when therer is an exception on get tasks.
     */
    List<TaskDto> findTasks(TaskQuery query, int offset, int limit) throws Exception;

    /**
     * Count tasks assigned to a user using a search term to find in title or
     * description of the task
     *
     * @param user  username
     * @param query term to search in title or description
     * @return tasks count
     */
    long countTasks(String user, String query, List<String> memberships) throws Exception;

    List<Object[]> countTaskStatusByProject(long projectId);

    /* @deprecated use {@link #filterTasks(TaskListFilter)}, since 7.3, not for removal */
    /**
     * Filters a task list from its criteria given one by one.
     *
     * @deprecated use {@link #filterTasks(TaskListFilter)}, which carries the
     *             same criteria in one object, plus the conditions other
     *             add-ons contribute
     */
    @Deprecated(since = "7.3", forRemoval = false)
    public TasksList filterTasks(String query, long projectId, String keyword, List<Long> labels, TaskUtil.DUE dueDate, Priority priority, List<String> assignees, List<String> coworkers, List<String> watchers, Long labelId, Long statusId, Identity currIdentity, String dueCategory, String space_group_id , TimeZone userTimezone, boolean isShowCompleted, boolean advanceSearch, boolean noProjPermission, boolean noLblPermission, String orderBy, String groupBy, int offset, int limit) throws Exception;

    /**
     * Filters a task list: one of the personal lists or a project's tasks,
     * with the usual criteria and the conditions other add-ons contribute,
     * all combined with AND, grouped, ordered and paginated.
     *
     * @param filter the list, its criteria and its page
     * @return the page of tasks and the number of tasks the criteria match
     * @throws Exception when the tasks cannot be read
     */
    TasksList filterTasks(TaskListFilter filter) throws Exception;
    
    public boolean isExternal(String userId);

    /**
     * Retrieves a paginated list of all task IDs.
     *
     * @param offset the starting position of the first result
     * @param limit  the maximum number of results to retrieve
     * @return a list of task IDs
     */
    public List<Long> getAllIds(int offset, int limit);

    /**
     * Find tasks assigned to a user using a term to find in title or description of
     * the task
     *
     * @param taskFilter the task search filter
     * @return a list of task IDs
     */
    public List<TaskDto> findTasks(TaskSearchFilter taskFilter);

    /**
     * Searches the tasks a user can view by a text in their title or
     * description, ignoring case and accents, through the same index and the
     * same permissions as {@link #findTasks(TaskSearchFilter)}: the user and
     * their memberships. The text is reduced to its words, lower-cased letters
     * and digits without accents, and must hold a word with letters, since the
     * index does not search numbers. With a project, only the tasks of that
     * project, which the user must be able to view. The open tasks come first,
     * in the order of relevance; the completed ones are left out unless asked
     * for, and then come after every open one. Each hit is checked again
     * against the task's own view permission and, with a project, against the
     * task's project, so a task the index still lists for a user who lost
     * access to it is never returned. The search reads the
     * {@value #TEXT_SEARCH_WINDOW} most relevant hits at most.
     *
     * @param text the text searched: every word in the title, or every word in
     *          the description
     * @param projectId the project, null for every task the user can view
     * @param includeCompleted whether the completed tasks are returned, after
     *          the open ones
     * @param limit the most tasks returned, bounded between 1 and
     *          {@value #TEXT_SEARCH_WINDOW}
     * @param identity the user who searches
     * @return the tasks found, the limit used, and whether more may match
     * @throws ObjectNotFoundException when the project doesn't exist
     * @throws IllegalAccessException when the user can't view the project
     * @throws IllegalArgumentException with the code
     *           {@value #TEXT_SEARCH_NO_WORD} when the text holds no word with
     *           letters
     */
    TaskTextSearchResult searchTasks(String text,
                                     Long projectId,
                                     boolean includeCompleted,
                                     int limit,
                                     Identity identity) throws ObjectNotFoundException, IllegalAccessException;

    /**
     * Find tasks switch last update date computed from Task Logs and Task
     * Comments after applying the {@link TaskQuery} filtering
     * 
     * @param query {@link TaskQuery}
     * @param offset result offset
     * @param limit result limit
     * @return {@link List} of corresponding Tasks
     */
    List<TaskDto> findLastUpdatedTasks(TaskQuery query, int offset, int limit);
}
