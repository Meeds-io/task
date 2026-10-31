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
package org.exoplatform.task.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.*;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.model.TaskChangeFilter;
import org.exoplatform.task.model.TaskChanges;
import org.exoplatform.task.model.TaskFieldChange;
import org.exoplatform.task.model.TaskListFilter;
import org.exoplatform.task.model.TaskSearchFilter;
import org.exoplatform.task.model.TaskTextSearchResult;
import org.exoplatform.task.service.TaskPayload;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.storage.TaskStorage;
import org.exoplatform.task.util.ProjectUtil;
import org.exoplatform.task.util.StorageUtil;
import org.exoplatform.task.util.TaskUtil;
import org.exoplatform.task.util.UserUtil;

import io.meeds.task.plugin.TaskAclPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

import static org.exoplatform.task.util.TaskUtil.*;

@Singleton
public class TaskServiceImpl implements TaskService {

    /** The most change log entries a task's changes come with (EXO-91902). */
    private static final int CHANGE_LOG_LIMIT = 100;

    private static final Log LOG = ExoLogger.getExoLogger(TaskServiceImpl.class);

    @Inject
    private TaskStorage taskStorage;

    @Inject
    private DAOHandler daoHandler;

    private ListenerService listenerService;

    private ProjectService projectService;

    public TaskServiceImpl(TaskStorage taskStorage, DAOHandler daoHandler, ListenerService listenerService) {
        this.taskStorage = taskStorage;
        this.daoHandler = daoHandler;
        this.listenerService = listenerService;
        this.projectService = projectService;
    }

    @Override
    public TaskDto createTask(TaskDto task) {
      TaskDto result = taskStorage.createTask(task);
      RequestLifeCycle.restartTransaction();
      TaskPayload event = new TaskPayload(null, result);
      broadcastEvent(listenerService, TASK_CREATED, null, event);
      if (result.getStatus() != null && result.getStatus().getProject() != null) {
        broadcastEvent(listenerService, "exo.project.projectModified", null, result.getStatus().getProject());
      }
      return result;
    }

    @Override
    public TaskDto updateTask(TaskDto task) {
      if (task == null) {
        throw new IllegalArgumentException("TaskDto must not be NULL");
      }
      TaskDto oldTaskEntity = taskStorage.getTaskWithCoworkers(task.getId());
      TaskDto newTaskEntity = taskStorage.update(task);
      TaskPayload event = new TaskPayload(oldTaskEntity, newTaskEntity);
      broadcastEvent(listenerService, TASK_UPDATED, null, event);
      if (task.getStatus() != null && task.getStatus().getProject() != null) {
        broadcastEvent(listenerService, "exo.project.projectModified", null, task.getStatus().getProject());
      }
      return newTaskEntity;
    }

    @Override
    public TaskDto updateTask(long taskId, TaskDto task, Identity identity) throws ObjectNotFoundException,
                                                                            IllegalAccessException {
      if (task == null) {
        throw new IllegalArgumentException("TaskDto must not be NULL");
      }
      TaskDto storedTask = taskStorage.getTaskById(taskId);
      if (storedTask == null) {
        throw new ObjectNotFoundException("Task " + taskId + " not found");
      }
      if (!TaskUtil.hasEditPermission(this, storedTask, identity)) {
        throw new IllegalAccessException("User " + (identity == null ? null : identity.getUserId()) + " cannot edit task "
            + taskId);
      }
      task.setId(taskId);
      // The authorship is an edit permission input: never taken from the update
      task.setCreatedBy(storedTask.getCreatedBy());
      task.setCreatedTime(storedTask.getCreatedTime());
      if (task.getStatus() != null) {
        // The storage resolves the status by its id alone: authorize a move on
        // the project of the loaded status, never on the project the task carries
        StatusDto status = task.getStatus().getId() == null ? null
                                                            : ExoContainerContext.getService(StatusService.class)
                                                                                 .getStatus(task.getStatus().getId());
        if (status == null || status.getProject() == null) {
          throw new IllegalArgumentException("task.status.notFound");
        }
        long currentProjectId = storedTask.getStatus() == null || storedTask.getStatus().getProject() == null ? 0
                                                                                                              : storedTask.getStatus()
                                                                                                                          .getProject()
                                                                                                                          .getId();
        if (status.getProject().getId() != currentProjectId && !canViewProject(status.getProject().getId(), identity)) {
          throw new IllegalAccessException("User " + identity.getUserId() + " cannot move task " + taskId + " to project "
              + status.getProject().getId());
        }
        task.setStatus(status);
      }
      return updateTask(task);
    }

    @Override
    public void updateTaskOrder(long currentTaskId, Status newStatus, long[] orders) {
        taskStorage.updateTaskOrder(currentTaskId, newStatus, orders);
    }

    @Override
    public void removeTask(long id) throws EntityNotFoundException {
        TaskDto task = getTask(id);// Can throw TaskNotFoundException
        taskStorage.delete(task);
        broadcastEvent(listenerService, TASK_DELETED, null, new TaskPayload(null, task));
    }

    @Override
    public TaskDto cloneTask(long id) throws EntityNotFoundException {
        TaskDto task = getTask(id);// Can throw TaskNotFoundException
        TaskDto newTask = task.clone();
        newTask.setId(0L);
        newTask.setCoworker(getCoworker(id));
        newTask.setTitle(Task.PREFIX_CLONE + newTask.getTitle());
        return createTask(newTask);
    }

    @Override
    public TaskDto getTask(long id) throws EntityNotFoundException {
        TaskDto task = taskStorage.getTaskById(id);
        if (task == null) {
            LOG.info("Can not find task with ID: " + id);
            throw new org.exoplatform.task.exception.EntityNotFoundException(id, Task.class);
        }
        return task;
    }

    @Override
    public Set<String> getMentionedUsers(long taskId) {
        return daoHandler.getCommentHandler().findMentionedUsersOfTask(taskId);
    }

    @Override
    public ChangeLogEntry addTaskLog(long id, String username, String actionName, String target) throws EntityNotFoundException {
        ChangeLogEntry log = new ChangeLogEntry();
        log.setTask(StorageUtil.taskToEntity(getTask(id)));
        log.setAuthor(username);
        log.setActionName(actionName);
        log.setTarget(target);
        return taskStorage.addTaskLog(log);
    }

    @Override
    public List<ChangeLogEntry> getTaskLogs(long taskId, int offset, int limit) throws Exception {
        return taskStorage.getTaskLogs(taskId,  offset,  limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChangeLogEntry> getTaskLogsBefore(long taskId,
                                                  Long beforeTime,
                                                  Long beforeId,
                                                  int limit,
                                                  Identity identity) throws EntityNotFoundException, IllegalAccessException {
        TaskDto task = getTask(taskId);
        if (!canView(task, identity)) {
            throw new IllegalAccessException("task.logs.notAccessible");
        }
        if ((beforeTime == null) != (beforeId == null)) {
            throw new IllegalArgumentException("task.logs.invalidCursor");
        }
        if (limit < 1 || limit > MAX_TASK_LOGS_PAGE) {
            throw new IllegalArgumentException("task.logs.invalidLimit");
        }
        return taskStorage.getTaskLogsBefore(taskId, beforeTime, beforeId, limit);
    }

    /**
     * @param task a task
     * @param identity a reader, whoever is signed in
     * @return whether the reader is mentioned in the task or may edit it, read
     *         for that reader alone, never for the user of the current
     *         conversation
     */
    private boolean canView(TaskDto task, Identity identity) {
        return identity != null
            && (getMentionedUsers(task.getId()).contains(identity.getUserId())
                || TaskUtil.hasEditPermission(this, task, identity));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addChangeFilter(TaskQuery query, TaskChangeFilter filter) {
        if (filter == null) {
            return;
        }
        if (filter.isHideCompleted() && filter.getCompletedSince() == null) {
            query.setCompleted(false);
        }
        query.setChangedSince(filter.getChangedSince());
        query.setCompletedSince(filter.getCompletedSince());
        if (StringUtils.isNotBlank(filter.getAssignee())) {
            query.setAssignee(Collections.singletonList(filter.getAssignee().trim()));
        }
        query.setStatusId(filter.getStatusId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskQuery getAssignedTasksQuery(String username, TaskChangeFilter filter) {
        TaskQuery query = new TaskQuery();
        query.setAssigneeOrCoworker(Collections.singletonList(username));
        addChangeFilter(query, filter);
        return query;
    }

    /**
     * {@inheritDoc} The whole change log is read: the task may have been
     * selected on an entry the newest ones do not hold, and an old value is
     * the new value of an earlier entry.
     */
    @Override
    public TaskChanges getTaskChanges(TaskDto task, Date since, boolean includeChangeLog) throws Exception {
        if (since == null) {
            throw new IllegalArgumentException("task.changes.noDate");
        }
        List<ChangeLogEntry> logs = getTaskLogs(task.getId(), 0, -1);
        List<ChangeLogEntry> changeLog = includeChangeLog ? logs.subList(0, Math.min(logs.size(), CHANGE_LOG_LIMIT)) : null;
        return new TaskChanges(getTaskChanges(task, logs, since), changeLog);
    }


    @Override
    public List<TaskDto> findTasks(TaskQuery query, int offset, int limit) throws Exception {
        return taskStorage.findTasks(query, offset, limit);
    }

    @Override
    public List<TaskDto> findLastUpdatedTasks(TaskQuery query, int offset, int limit) {
        return taskStorage.findLastUpdatedTasks(query, offset, limit);
    }

    @Override
    public int countTasks(TaskQuery query) throws Exception {
        return taskStorage.countTasks(query);
    }

    @Override
    public <T> List<T> selectTaskField(TaskQuery query, String fieldName) {
        return taskStorage.selectTaskField(query, fieldName);
    }

    @Override
    public List<TaskDto> findTasksByLabel(LabelDto label, List<Long> projectIds, String username, OrderBy orderBy, int offset, int limit) throws Exception {

        return taskStorage.findTasksByLabel(label, projectIds, username, orderBy, offset, limit);
    }

    @Override
    public int countTasksByLabel(LabelDto label, List<Long> projectIds, String username, OrderBy orderBy) throws Exception {
        return taskStorage.countTasksByLabel(label, projectIds, username, orderBy);
    }

    public TaskDto findTaskByActivityId(String id) {
        return taskStorage.findTaskByActivityId(id);
    }

    @Override
    public Set<String> getCoworker(long taskId) {
        return taskStorage.getCoworker(taskId);
    }

    @Override
    public List<TaskDto> getUncompletedTasks(String user, int limit) {
        return taskStorage.getUncompletedTasks(user, limit);
    }

    @Override
    public Long countUncompletedTasks(String user) {
        return taskStorage.countUncompletedTasks(user);
    }

    @Override
    public List<TaskDto> getWatchedTasks(String user, int limit) {
        return taskStorage.getWatchedTasks(user, limit);
    }

    @Override
    public Long countWatchedTasks(String user) {
        return taskStorage.countWatchedTasks(user);
    }


    @Override
    public List<TaskDto> getAssignedTasks(String user, int limit) {
        return taskStorage.getAssignedTasks(user, limit);
    }

    @Override
    public Long countAssignedTasks(String user) {
        return taskStorage.countWatchedTasks(user);
    }

    @Override
    public List<TaskDto> getCollaboratedTasks(String user, int limit) {
        return taskStorage.getCollaboratedTasks(user, limit);
    }

    @Override
    public Long countCollaboratedTasks(String user) {
        return taskStorage.countCollaboratedTasks(user);
    }


    @Override
    public List<TaskDto> findTasksByMemberShips(String user, List<String> memberships, String query, int limit) {
        if (StringUtils.isBlank(user)) {
            throw new IllegalArgumentException("user parameter is mandatory");
        }
        if (StringUtils.isBlank(query)) {
            throw new IllegalArgumentException("query parameter is mandatory");
        }
        return taskStorage.findTasks(user, memberships, query, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TaskTextSearchResult searchTasks(String text,
                                            Long projectId,
                                            boolean includeCompleted,
                                            int limit,
                                            Identity identity) throws ObjectNotFoundException, IllegalAccessException {
      String words = TaskUtil.toSearchWords(text);
      if (!words.matches(".*\\p{L}.*")) {
        throw new IllegalArgumentException(TEXT_SEARCH_NO_WORD);
      }
      if (projectId != null) {
        checkCanViewProject(projectId, identity);
      }
      int max = Math.clamp(limit, 1, TEXT_SEARCH_WINDOW);
      TaskSearchFilter filter = new TaskSearchFilter(identity.getUserId(),
                                                     words,
                                                     UserUtil.getMemberships(identity),
                                                     null,
                                                     null,
                                                     0,
                                                     TEXT_SEARCH_WINDOW);
      if (projectId != null) {
        filter.setProjectIds(Collections.singletonList(projectId));
      }
      List<TaskDto> hits = findTasks(filter);
      List<TaskDto> found = new ArrayList<>();
      collectSearchHits(hits, projectId, false, identity.getUserId(), found, max + 1);
      if (includeCompleted) {
        collectSearchHits(hits, projectId, true, identity.getUserId(), found, max + 1);
      }
      // a full window may hide matches ranked below it, whatever the filters kept
      boolean hasMore = found.size() > max || hits.size() >= TEXT_SEARCH_WINDOW;
      return new TaskTextSearchResult(found.size() > max ? new ArrayList<>(found.subList(0, max)) : found, max, hasMore);
    }

    @Override
    public List<TaskDto> findTasks(TaskSearchFilter filter) {
      if (StringUtils.isBlank(filter.getUserId())) {
        throw new IllegalArgumentException("user parameter is mandatory");
      }
      if (StringUtils.isBlank(filter.getTerm())) {
        throw new IllegalArgumentException("query parameter is mandatory");
      }
      return taskStorage.findTasks(filter);
    }

    @Override
    public List<TaskDto> findTasks(String user, String query, int limit) {
        return findTasksByMemberShips(user,new ArrayList<>(), query, limit);
    }

    @Override
    public long countTasks(String user, String query, List<String> memberships) {
        if (StringUtils.isBlank(user)) {
            throw new IllegalArgumentException("user parameter is mandatory");
        }
        if (StringUtils.isBlank(query)) {
            throw new IllegalArgumentException("query parameter is mandatory");
        }
        return taskStorage.countTasks(user, query, memberships);
    }

    @Override
    public List<TaskDto> getIncomingTasks(String user, int offset, int limit) throws Exception {
        return taskStorage.getIncomingTasks(user, offset, limit);
    }

    @Override
    public int countIncomingTasks(String user) throws Exception {
        return taskStorage.countIncomingTasks(user);
    }

    @Override
    public List<TaskDto> getOverdueTasks(String user, int limit) {
        return taskStorage.getOverdueTasks(user, limit);
    }

    @Override
    public Long countOverdueTasks(String user) {
        return taskStorage.countOverdueTasks(user);
    }

    @Override
    public void addWatcherToTask(String user, TaskDto task) throws Exception {
        taskStorage.addWatcherToTask(user, task);
    }

    @Override
    public void deleteWatcherOfTask(String user, TaskDto task) throws Exception {
        taskStorage.deleteWatcherOfTask(user, task);
    }

    public Set<String> getWatchersOfTask(TaskDto task) {
        return taskStorage.getWatchersOfTask(task);
    }

    @Override
    public List<Object[]> countTaskStatusByProject(long projectId) {
        return taskStorage.countTaskStatusByProject(projectId);
    }

    /**
     * Filters a task list from its criteria given one by one, through
     * {@link #filterTasks(TaskListFilter)}.
     */
    @Override
    @Deprecated(since = "7.3", forRemoval = false)
    public TasksList filterTasks(String query, long projectId, String keyword, List<Long> labels, TaskUtil.DUE dueDate, Priority priority, List<String> assignees, List<String> coworker, List<String> watchers, Long labelId, Long statusId, Identity currIdentity, String dueCategory, String space_group_id , TimeZone userTimezone, boolean isShowCompleted, boolean advanceSearch, boolean noProjPermission, boolean noLblPermission, String orderBy, String groupBy, int offset, int limit) throws Exception {
        TaskListFilter filter = new TaskListFilter();
        filter.setQuery(query);
        filter.setProjectId(projectId);
        filter.setKeyword(keyword);
        filter.setLabels(labels);
        filter.setDueDate(dueDate);
        filter.setPriority(priority);
        filter.setAssignees(assignees);
        filter.setCoworkers(coworker);
        filter.setWatchers(watchers);
        filter.setLabelId(labelId);
        filter.setStatusId(statusId);
        filter.setCurrIdentity(currIdentity);
        filter.setDueCategory(dueCategory);
        filter.setSpaceGroupId(space_group_id);
        filter.setUserTimezone(userTimezone);
        filter.setShowCompleted(isShowCompleted);
        filter.setAdvanceSearch(advanceSearch);
        filter.setNoProjPermission(noProjPermission);
        filter.setNoLblPermission(noLblPermission);
        filter.setOrderBy(orderBy);
        filter.setGroupBy(groupBy);
        filter.setOffset(offset);
        filter.setLimit(limit);
        return filterTasks(filter);
    }

    /**
     * Filters a task list. The extension conditions are added to the task
     * query after its own criteria, so they are combined with them with AND,
     * in the page and in the count alike.
     */
    @Override
    public TasksList filterTasks(TaskListFilter filter) throws Exception {
        String query = filter.getQuery();
        long projectId = filter.getProjectId();
        String keyword = filter.getKeyword();
        List<Long> labels = filter.getLabels();
        TaskUtil.DUE dueDate = filter.getDueDate();
        Priority priority = filter.getPriority();
        List<String> assignees = filter.getAssignees();
        List<String> coworker = filter.getCoworkers();
        List<String> watchers = filter.getWatchers();
        Long labelId = filter.getLabelId();
        Long statusId = filter.getStatusId();
        Identity currIdentity = filter.getCurrIdentity();
        String dueCategory = filter.getDueCategory();
        String space_group_id = filter.getSpaceGroupId();
        TimeZone userTimezone = filter.getUserTimezone();
        boolean isShowCompleted = filter.isShowCompleted();
        boolean advanceSearch = filter.isAdvanceSearch();
        boolean noProjPermission = filter.isNoProjPermission();
        boolean noLblPermission = filter.isNoLblPermission();
        String orderBy = filter.getOrderBy();
        String groupBy = filter.getGroupBy();
        int offset = filter.getOffset();
        int limit = filter.getLimit();

        List<String> defOrders;
        List<String> defGroupBys;

        List<Long> spaceProjectIds = null;
        if (space_group_id != null) {
            spaceProjectIds = new LinkedList<Long>();
            List<ProjectDto> projects = ProjectUtil.flattenTree(ProjectUtil.getProjectTree(space_group_id, projectService,offset,limit), projectService,offset,limit);
            for (ProjectDto p : projects) {
                if (p.canView(currIdentity)) {
                    spaceProjectIds.add(p.getId());
                }
            }
        }
        List<Long> allProjectIds = null;
        if (projectId == 0) {
            allProjectIds = new LinkedList<Long>();
            List<ProjectDto> projects = ProjectUtil.flattenTree(ProjectUtil.getProjectTree(null, projectService,offset,limit), projectService,offset,limit);
            for (ProjectDto p : projects) {
                if (p.canView(currIdentity)) {
                    allProjectIds.add(p.getId());
                }
            }
        }

        OrderBy order = null;
        if(orderBy != null && !orderBy.trim().isEmpty()) {
            order = TaskUtil.TITLE.equals(orderBy) || TaskUtil.DUEDATE.equals(orderBy) ? new OrderBy.ASC(orderBy) : new OrderBy.DESC(orderBy);
        }

        TaskQuery taskQuery = new TaskQuery();
        if (advanceSearch) {
            Status status = statusId != null ? daoHandler.getStatusHandler().find(statusId) : null;
            TaskUtil.buildTaskQuery(taskQuery, keyword, labels, status, dueDate, priority, assignees, coworker, watchers, isShowCompleted, userTimezone);
        } else {
            taskQuery.setKeyword(query);
            taskQuery.setCompleted(false);
        }

        //Get Tasks in good order
        if(projectId == ProjectUtil.INCOMING_PROJECT_ID) {
            //. Default order by CreatedDate
            if (orderBy == null || orderBy.isEmpty()) {
                orderBy = TaskUtil.CREATED_TIME;
            }
            order = new OrderBy.DESC(orderBy);

            taskQuery.setIsIncomingOf(currIdentity.getUserId());
            taskQuery.setOrderBy(Arrays.asList(order));
        } else if (projectId == ProjectUtil.TODO_PROJECT_ID) {
            defGroupBys =  Arrays.asList(TaskUtil.NONE, TaskUtil.PROJECT, TaskUtil.LABEL, TaskUtil.DUEDATE,TaskUtil.ASSIGNEE);
            defOrders =  Arrays.asList(TaskUtil.TITLE, TaskUtil.STATUS, TaskUtil.DUEDATE, TaskUtil.PRIORITY, TaskUtil.RANK);
            if (dueDate != null || !dueCategory.equals("")){
                taskQuery.setAssigneeIsTodoOf(currIdentity.getUserId());
            } else {
                taskQuery.setIsTodoOf(currIdentity.getUserId());
            }

            //TODO: process fiter here
            if ("overDue".equalsIgnoreCase(dueCategory)) {
                defGroupBys = Arrays.asList(TaskUtil.NONE, TaskUtil.PROJECT, TaskUtil.LABEL);
                defOrders = Arrays.asList(TaskUtil.TITLE, TaskUtil.PRIORITY, TaskUtil.DUEDATE);
                groupBy = groupBy == null || !defGroupBys.contains(groupBy) ? TaskUtil.PROJECT : groupBy;
            } else if ("today".equalsIgnoreCase(dueCategory)) {
                defGroupBys =  Arrays.asList(TaskUtil.NONE, TaskUtil.PROJECT, TaskUtil.LABEL);
                defOrders =  Arrays.asList(TaskUtil.TITLE, TaskUtil.PRIORITY, TaskUtil.RANK);
                if (orderBy == null) {
                    order = new OrderBy.DESC(TaskUtil.PRIORITY);
                    orderBy = TaskUtil.PRIORITY;
                }
                groupBy = groupBy == null || !defGroupBys.contains(groupBy) ? TaskUtil.NONE : groupBy;
            } else if ("tomorrow".equalsIgnoreCase(dueCategory)) {
                defGroupBys = Arrays.asList(TaskUtil.NONE, TaskUtil.PROJECT, TaskUtil.LABEL);
                defOrders = Arrays.asList(TaskUtil.TITLE, TaskUtil.PRIORITY, TaskUtil.RANK);
                if (orderBy == null || !defOrders.contains(orderBy)) {
                    order = new OrderBy.DESC(TaskUtil.PRIORITY);
                    orderBy = TaskUtil.PRIORITY;
                }
                groupBy = groupBy == null || !defGroupBys.contains(groupBy) ? TaskUtil.NONE : groupBy;
            } else if ("upcoming".equalsIgnoreCase(dueCategory)) {
                defGroupBys = Arrays.asList(TaskUtil.NONE, TaskUtil.PROJECT, TaskUtil.LABEL);
                defOrders =  Arrays.asList(TaskUtil.TITLE, TaskUtil.PRIORITY, TaskUtil.DUEDATE, TaskUtil.RANK);
                groupBy = groupBy == null || !defGroupBys.contains(groupBy) ? TaskUtil.NONE : groupBy;
            }

            if (orderBy == null || !defOrders.contains(orderBy)) {
                orderBy = TaskUtil.DUEDATE;
                order = new OrderBy.ASC(TaskUtil.DUEDATE);
            }
            if (groupBy == null || !defGroupBys.contains(groupBy)) {
                groupBy = TaskUtil.DUEDATE;
            }

            Date[] filterDate = TaskUtil.convertDueDate(dueCategory, userTimezone);
            taskQuery.setDueDateFrom(filterDate[0]);
            taskQuery.setDueDateTo(filterDate[1]);
            taskQuery.setOrderBy(Arrays.asList(order));

        } else if (projectId >= 0) {
            if (projectId == 0) {
                defGroupBys = Arrays.asList(TaskUtil.NONE, TaskUtil.ASSIGNEE, TaskUtil.PROJECT, TaskUtil.LABEL, TaskUtil.DUEDATE, TaskUtil.STATUS);

                if (orderBy == null || orderBy.isEmpty()) {
                    orderBy = TaskUtil.DUEDATE;
                    order = new OrderBy.ASC(orderBy);
                }

                if (spaceProjectIds != null) {
                    taskQuery.setProjectIds(spaceProjectIds);
                } else {
                    taskQuery.setProjectIds(allProjectIds);
                }
            } else {
                //. Default order by CreatedDate
                if (orderBy == null || orderBy.isEmpty()) {
                    orderBy = TaskUtil.DUEDATE;
                    order = new OrderBy.ASC(orderBy);
                }
                if (!noProjPermission) {
                    taskQuery.setProjectIds(Arrays.asList(projectId));
                }
            }
            taskQuery.setOrderBy(Arrays.asList(order));

        } else if (labelId != null && labelId >= 0) {
            defOrders =  Arrays.asList(TaskUtil.TITLE, TaskUtil.CREATED_TIME, TaskUtil.DUEDATE, TaskUtil.PRIORITY);
            if (orderBy == null || orderBy.isEmpty() || !defOrders.contains(orderBy)) {
                orderBy = TaskUtil.DUEDATE;
                order = new OrderBy.ASC(TaskUtil.DUEDATE);
            }

            if (labelId > 0) {
                defGroupBys = Arrays.asList(TaskUtil.NONE, TaskUtil.PROJECT, TaskUtil.DUEDATE, TaskUtil.STATUS);
                if (groupBy == null || groupBy.isEmpty() || !defGroupBys.contains(groupBy)) {
                    groupBy = TaskUtil.NONE;
                }
                if (!noLblPermission) {
                    taskQuery.setLabelIds(Arrays.asList(labelId));
                }
            } else {
                defGroupBys = Arrays.asList(TaskUtil.NONE, TaskUtil.PROJECT, TaskUtil.LABEL, TaskUtil.DUEDATE, TaskUtil.STATUS);
                if (groupBy == null || groupBy.isEmpty() || !defGroupBys.contains(groupBy)) {
                    groupBy = TaskUtil.LABEL;
                }
                taskQuery.setIsLabelOf(currIdentity.getUserId());
            }

            if (spaceProjectIds != null) {
                taskQuery.setProjectIds(spaceProjectIds);
            }
            taskQuery.setOrderBy(Arrays.asList(order));
        }


        if (filter.getExtensionConditions() != null) {
            filter.getExtensionConditions().forEach(taskQuery::add);
        }

        List<TaskDto> listTasks = null;
        long tasksSize = 0;
        if ((spaceProjectIds != null  && spaceProjectIds.isEmpty()) || ( projectId == 0 && allProjectIds.isEmpty()) ||
                (noLblPermission && labelId != null && labelId > 0) || (noProjPermission &&  projectId > 0)) {
            listTasks = new ArrayList<>();
        } else {
            listTasks = findTasks(taskQuery,offset,limit);
            tasksSize = countTasks(taskQuery);
        }
       return new TasksList(listTasks,tasksSize);
    }

    @Override
    public boolean isExternal(String userId) {
        IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
        org.exoplatform.social.core.identity.model.Identity userIdentity =  identityManager.getOrCreateIdentity(
            OrganizationIdentityProvider.NAME, userId);
        return userIdentity.getProfile() != null && userIdentity.getProfile().getProperty(Profile.EXTERNAL) != null && userIdentity.getProfile().getProperty(Profile.EXTERNAL).equals("true");
    }

    @Override
    public List<Long> getAllIds(int offset, int limit) {
      return taskStorage.getAllIds(offset, limit);
    }

    /**
     * Tells the status, project, due date, assignee and completion changes of
     * a task at or after a date, oldest first, from its change log. The log
     * records the new status, project and assignee of each change, so the old
     * value is the new value of the previous change of the same field. The
     * creation entry records the status the task was created in, so a first
     * move tells the status it left; a creation entry logged before that has
     * none, and neither does a move after a project change, whose new status
     * is not logged: the old value is then none. The log records no due date:
     * the latest due date change carries the current due date, the earlier
     * ones none. A completion entry is logged on each completion and each
     * reopening, so the latest one carries the current completion and the
     * earlier ones alternate. An assignee change of a task created at or after
     * the date is left out, as {@link TaskQuery#setChangedSince(Date)} leaves
     * it out: a task created with an assignee logs that assignment at its
     * creation.
     *
     * @param task the task
     * @param logs its change log entries, in any order
     * @param since the earliest date of the changes, included
     * @return the changes
     */
    List<TaskFieldChange> getTaskChanges(TaskDto task, List<ChangeLogEntry> logs, Date since) {
        if (logs == null || logs.isEmpty()) {
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
        List<TaskFieldChange> changes = new ArrayList<>();
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
            Date newDate = null;
            if ("created".equals(action)) {
                // the status a task is created in, logged since EXO-91902; none before
                status = target;
            } else if ("edit_status".equals(action)) {
                field = TaskFieldChange.FIELD_STATUS;
                oldValue = status;
                newValue = target;
                status = target;
            } else if ("edit_project".equals(action) || "remove_project".equals(action)) {
                field = TaskFieldChange.FIELD_PROJECT;
                oldValue = project;
                newValue = "edit_project".equals(action) ? target : null;
                project = newValue;
                status = null;
            } else if (("assign".equals(action) || "unassign".equals(action)) && !createdBefore) {
                assignee = "assign".equals(action) ? target : null;
            } else if ("assign".equals(action) || "unassign".equals(action)) {
                field = TaskFieldChange.FIELD_ASSIGNEE;
                oldValue = "assign".equals(action) ? assignee : target;
                newValue = "assign".equals(action) ? target : null;
                assignee = newValue;
            } else if (TaskQuery.DUE_DATE_CHANGE_ACTION.equals(action)) {
                field = TaskFieldChange.FIELD_DUE_DATE;
                newDate = log == lastDueDateChange ? task.getDueDate() : null;
            } else if (TaskQuery.COMPLETION_ACTION.equals(action)) {
                completionsLeft--;
                // the latest entry is the current completion, and each earlier one its opposite
                boolean completed = task.isCompleted() == (completionsLeft % 2 == 0);
                field = TaskFieldChange.FIELD_COMPLETED;
                oldValue = String.valueOf(!completed);
                newValue = String.valueOf(completed);
            }
            if (field != null && listed) {
                changes.add(new TaskFieldChange(field, oldValue, newValue, newDate, log.getCreatedTime(), log.getAuthor()));
            }
        }
        return changes;
    }

    /**
     * Checks that a project exists and that a user can view it.
     *
     * @param projectId the project
     * @param identity the user
     * @throws ObjectNotFoundException when the project doesn't exist
     * @throws IllegalAccessException when the user can't view the project
     */
    private void checkCanViewProject(long projectId, Identity identity) throws ObjectNotFoundException, IllegalAccessException {
      ProjectDto project;
      try {
        project = ExoContainerContext.getService(ProjectService.class).getProject(projectId);
      } catch (EntityNotFoundException e) {
        project = null;
      }
      if (project == null) {
        throw new ObjectNotFoundException("Project " + projectId + " not found");
      } else if (!project.canView(identity)) {
        throw new IllegalAccessException("User " + identity.getUserId() + " cannot view project " + projectId);
      }
    }

    /**
     * Adds to the tasks found the search hits of one completion state, in their
     * order of relevance, that belong to the project searched, when there is
     * one, and that the user can view, until there are enough.
     *
     * @param hits the search hits, the most relevant first
     * @param projectId the project searched, null for any
     * @param completed the completion state of the hits added
     * @param username the user who searches
     * @param found the tasks found so far, completed by this call
     * @param enough how many tasks found are enough
     */
    private void collectSearchHits(List<TaskDto> hits,
                                   Long projectId,
                                   boolean completed,
                                   String username,
                                   List<TaskDto> found,
                                   int enough) {
      UserACL userAcl = ExoContainerContext.getService(UserACL.class);
      for (TaskDto task : hits) {
        if (found.size() >= enough) {
          return;
        }
        if (task.isCompleted() == completed
            && (projectId == null || projectId.equals(getProjectIdOrNull(task)))
            && userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, String.valueOf(task.getId()), username)) {
          found.add(task);
        }
      }
    }

    /**
     * @param task a task
     * @return the identifier of its project, null for a personal task
     */
    private Long getProjectIdOrNull(TaskDto task) {
      StatusDto status = task.getStatus();
      return status == null || status.getProject() == null ? null : status.getProject().getId();
    }

    private boolean canViewProject(long projectId, Identity identity) {
      try {
        ProjectDto project = ExoContainerContext.getService(ProjectService.class).getProject(projectId);
        return project != null && project.canView(identity);
      } catch (EntityNotFoundException e) {
        return false;
      }
    }
}
