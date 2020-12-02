/* 
* Copyright (C) 2003-2015 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.task.util;


import java.text.*;
import java.util.*;

import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.legacy.service.ProjectService;
import org.exoplatform.task.legacy.service.StatusService;
import org.exoplatform.task.legacy.service.TaskService;
import org.exoplatform.task.legacy.service.UserService;
import org.exoplatform.task.rest.model.TaskEntity;
import org.exoplatform.task.service.LabelService;
import org.gatein.common.text.EntityEncoder;

import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.*;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.*;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.ParameterEntityException;
import org.exoplatform.task.model.*;
import org.exoplatform.task.legacy.service.*;
import org.exoplatform.web.controller.router.Router;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 6/3/15
 */
public final class TaskUtil {
  public static final String URL_TASK_DETAIL = "/taskDetail/";
  
  private static final Log LOG = ExoLogger.getExoLogger(TaskUtil.class.getName());

  public static final String TITLE = "title";  
  public static final String PRIORITY = "priority";
  public static final String DUEDATE = "dueDate";
  public static final String CREATED_TIME = "createdTime";
  public static final String NONE = "none";
  public static final String STATUS = "status";
  public static final String ASSIGNEE = "assignee";
  public static final String PROJECT = "project";
  public static final String LABEL = "label";
  public static final String COMPLETED = "completed";
  public static final String RANK = "rank";
  public static final String MEMBERSHIP = "membership";
  public static final String CREATED_BY = "createdBy";
  public static final String COWORKER = "coworker";
  
  public static final ListAccess<Task> EMPTY_TASK_LIST = new ListAccess<Task>() {

    @Override
    public Task[] load(int index, int length) throws Exception, IllegalArgumentException {
      return new Task[0];
    }

    @Override
    public int getSize() throws Exception {
      return 0;
    }
  };
  
  public static enum DUE {
    OVERDUE, TODAY, TOMORROW, UPCOMING
  }
  
  private TaskUtil() {
  }

  public static Map<String, String> getDefOrders(ResourceBundle bundle) {
    return resolve(Arrays.asList(TITLE, PRIORITY, DUEDATE, CREATED_TIME), bundle);
  }
  
  public static Map<String, String> getDefGroupBys(long currentProject, ResourceBundle bundle) {
    if (currentProject == -1) {
      return resolve(Arrays.asList(NONE, ASSIGNEE, LABEL, DUEDATE), bundle);
    } else {
      return resolve(Arrays.asList(NONE, ASSIGNEE, LABEL, DUEDATE, STATUS), bundle);
    }
  }
  
  public static Map<String, String> resolve(List<String> keys, ResourceBundle bundle) {    
    Map<String, String> labels = new LinkedHashMap<String, String>();
    for (String k : keys) {
      if (k.isEmpty()) {
        k = NONE;
      }
      String label;
      try {
        label = bundle.getString("label." + k);
      } catch (Exception e) {
        LOG.warn("Error while resolving label 'label." + k + "' with locale " + bundle.getLocale(), e);
        label = k;
      }
      labels.put(k, label);
    }
    return labels;
  }
  
  public static TaskQuery buildTaskQuery(TaskQuery query, String keyword,
                                   List<Long> searchLabelIds,
                                   Status status,
                                   DUE dueDate,
                                   Priority priority,
                                   List<String> searchAssignee,
                                   Boolean showCompleted, TimeZone timezone) {
    if (keyword != null && !keyword.trim().isEmpty()) {
      query.setKeyword(keyword);
    }
    if (searchLabelIds != null && !searchLabelIds.isEmpty()) {
      query.setLabelIds(searchLabelIds);
    }
    if (status != null) {
      query.setStatus(status);
    }
    if (dueDate != null) {
      Date[] due = TaskUtil.convertDueDate(dueDate.name(), timezone);
      query.setDueDateFrom(due[0]);
      query.setDueDateTo(due[1]);
    }
    if (priority != null) {
      query.setPriority(priority);      
    }
    if (searchAssignee != null && !searchAssignee.isEmpty()) {      
      query.setAssignee(searchAssignee);      
    }
    if (showCompleted != null && !showCompleted) {
      query.setCompleted(showCompleted);
    }
    
    return query;
  }

  public static TaskQuery buildTaskQuery(TaskQuery query, String keyword,
                                         List<Long> searchLabelIds,
                                         StatusDto status,
                                         DUE dueDate,
                                         Priority priority,
                                         List<String> searchAssignee,
                                         Boolean showCompleted, TimeZone timezone) {
    if (keyword != null && !keyword.trim().isEmpty()) {
      query.setKeyword(keyword);
    }
    if (searchLabelIds != null && !searchLabelIds.isEmpty()) {
      query.setLabelIds(searchLabelIds);
    }
    if (status != null) {
      query.setStatus(status);
    }
    if (dueDate != null) {
      Date[] due = TaskUtil.convertDueDate(dueDate.name(), timezone);
      query.setDueDateFrom(due[0]);
      query.setDueDateTo(due[1]);
    }
    if (priority != null) {
      query.setPriority(priority);
    }
    if (searchAssignee != null && !searchAssignee.isEmpty()) {
      query.setAssignee(searchAssignee);
    }
    if (showCompleted != null && !showCompleted) {
      query.setCompleted(showCompleted);
    }

    return query;
  }

  public static Date[] convertDueDate(String dueDate, TimeZone timezone) {
    Date[] due = new Date[] {null, null};

    if (dueDate != null && !dueDate.isEmpty()) {
      Calendar today = Calendar.getInstance(timezone);
      today.set(Calendar.HOUR_OF_DAY, 0);
      today.set(Calendar.MINUTE, 0);
      today.set(Calendar.SECOND, 0);
      today.set(Calendar.MILLISECOND, 0);

      switch (DUE.valueOf(dueDate.toUpperCase())) {
        case OVERDUE:
          today.add(Calendar.DATE, -1);
          due[1] = today.getTime();
          break;
        case TODAY:
          due[0] = today.getTime();
          today.add(Calendar.DATE, 1);
          due[1] = new Date(today.getTimeInMillis() - 1);
          break;
        case TOMORROW:
          today.add(Calendar.DATE, 1);
          due[0] = today.getTime();
          today.add(Calendar.DATE, 1);
          due[1] = new Date(today.getTimeInMillis() - 1);
          break;
        case UPCOMING:
          today.add(Calendar.DATE, 2);
          due[0] = today.getTime();
      }
    }
    return due;
  }

  public static TaskModel getTaskModel(Long id, boolean loadAllComment, ResourceBundle bundle, String username,
                                       TaskService taskService, OrganizationService orgService, UserService userService, ProjectService projectService) throws EntityNotFoundException {
    TaskModel taskModel = new TaskModel();
    
    Task task = taskService.getTask(id); //Can throw TaskNotFoundException
    Set<String> coworker = getCoworker(taskService, id);
    task.setCoworker(coworker);
    taskModel.setTask(task);

    taskModel.setReadOnly(hasViewOnlyPermission(task));

    //Verify if a task is watched
    Set<String> watchers = task.getWatcher();
    taskModel.setWatched(watchers.contains(username));

    org.exoplatform.task.model.User assignee = null;
    int numberCoworkers = coworker != null ? coworker.size() : 0;
    if (task.getAssignee() != null && !task.getAssignee().isEmpty()) {
      assignee = userService.loadUser(task.getAssignee());
    }
    taskModel.setAssignee(assignee);
    taskModel.setNumberCoworkers(numberCoworkers);

    ListAccess<Comment> listComments = taskService.getComments(task.getId());
    long commentCount = ListUtil.getSize(listComments);
    taskModel.setCommentCount(commentCount);

    int limitComment = loadAllComment ? -1 : 2;
    List<Comment> cmts = Arrays.asList(ListUtil.load(listComments, 0, limitComment));
    Collections.reverse(cmts);
    List<CommentModel> comments = new ArrayList<CommentModel>();
    taskService.loadSubComments(cmts);
    for (Comment c : cmts) {
      org.exoplatform.task.model.User u = userService.loadUser(c.getAuthor());
      CommentModel commentModel = new CommentModel(c, u, CommentUtil.formatMention(c.getComment(), userService));
      comments.add(commentModel);
      if (c.getSubComments() != null && !c.getSubComments().isEmpty()) {
        List<CommentModel> subComments = new ArrayList<>();
        for (Comment comment : c.getSubComments()) {
          org.exoplatform.task.model.User user = userService.loadUser(comment.getAuthor());
          subComments.add(new CommentModel(comment, user, CommentUtil.formatMention(comment.getComment(), userService)));
        }
        commentModel.setSubComments(subComments);
      }
    }
    taskModel.setComments(comments);

    org.exoplatform.task.model.User currentUser = userService.loadUser(username);
    taskModel.setCurrentUser(currentUser);
    
    EntityEncoder encoder = HTMLEntityEncoder.getInstance();
    String breadcumbs = "<li class=\"muted\" >" + bundle.getString("label.noProject") + "</li>";
    if (task.getStatus() != null) {
      Project p = task.getStatus().getProject();
      //breadcumbs = ProjectUtil.buildBreadcumbs(p.getId(), projectService, bundle);
      breadcumbs = "<li><a class=\"project-name\" href=\"javascript:void(0)\">" + encoder.encode(p.getName()) + "</a></li>";
    }
    taskModel.setBreadcumbs(breadcumbs);

    ListAccess<Label> labels = taskService.findLabelsByTask(id, username);
    if (labels != null) {
      taskModel.setLabels(Arrays.asList(ListUtil.load(labels, 0, -1)));
    }

    return taskModel;
  }

  public static Map<GroupKey, List<Task>> groupTasks(List<Task> tasks, String groupBy, String username, TimeZone userTimezone, ResourceBundle bundle, TaskService taskService, UserService userService) throws EntityNotFoundException {
    Map<GroupKey, List<Task>> maps = new TreeMap<GroupKey, List<Task>>();
    for(Task task : tasks) {
      for (GroupKey key : getGroupName(task, groupBy, username, userTimezone, bundle, taskService, userService)) {
        List<Task> list = maps.get(key);
        if(list == null) {
          list = new LinkedList<Task>();
          maps.put(key, list);
        }
        list.add(task);
      }
    }
    return maps;
  }

  public static Map<GroupKey, List<TaskEntity>> groupTasks(List<TaskEntity> tasks, String groupBy, String username, TimeZone userTimezone, LabelService labelService, UserService userService) throws EntityNotFoundException {
    Map<GroupKey, List<TaskEntity>> maps = new TreeMap<GroupKey, List<TaskEntity>>();
    for(TaskEntity task : tasks) {
      for (GroupKey key : getGroupName(task, groupBy, username, userTimezone, labelService, userService)) {
        List<TaskEntity> list = maps.get(key);
        if(list == null) {
          list = new LinkedList<TaskEntity>();
          maps.put(key, list);
        }
        list.add(task);
      }
    }
    return maps;
  }

  //TODO: Remove this method
  @Deprecated
  public static int countTasks(TaskService taskService, TaskQuery taskQuery) {
    ListAccess<Task> list = taskService.findTasks(taskQuery);
    return ListUtil.getSize(list);
  }

  //TODO: this method is not used by paging
  @Deprecated
  public static Map<GroupKey, ListAccess<Task>> findTasks(TaskService taskService, TaskQuery query, String groupBy,
                                                          TimeZone userTimezone, UserService userService) {
    Map<GroupKey, ListAccess<Task>> maps = new TreeMap<GroupKey, ListAccess<Task>>();
    TaskQuery selectFieldQuery = query.clone();
    if (groupBy == null || groupBy.trim().isEmpty() || NONE.equalsIgnoreCase(groupBy)) {

      ListAccess<Task> tasks = taskService.findTasks(query);
      GroupKey key = new GroupKey("", null, 0);
      maps.put(key, tasks);
      return maps;

    } else if (DUEDATE.equalsIgnoreCase(groupBy)) {
      Calendar c = Calendar.getInstance(userTimezone);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);

      Date fromDueDate = null;
      Date toDueDate = null;
      TaskQuery q;

      GroupKey key;
      ListAccess<Task> tasks;

      // Overdue
      c.add(Calendar.MILLISECOND, -1);
      toDueDate = c.getTime();
      q = query.clone();
      q.setDueDateTo(toDueDate);
      key = new GroupKey("Overdue", toDueDate, 0);
      tasks = taskService.findTasks(q);
      if (ListUtil.getSize(tasks) > 0) {
          maps.put(key, tasks);
      }

      // Today
      c.add(Calendar.MILLISECOND, 1);
      fromDueDate = c.getTime();
      c.add(Calendar.HOUR_OF_DAY, 24);
      c.add(Calendar.MILLISECOND, -1);
      toDueDate = c.getTime();

      q = query.clone();
      q.setDueDateFrom(fromDueDate);
      q.setDueDateTo(toDueDate);

      key = new GroupKey("Today", fromDueDate, 1);
      tasks = taskService.findTasks(q);
      if (ListUtil.getSize(tasks) > 0) {
        maps.put(key, tasks);
      }

      // Tomorrow
      c.add(Calendar.MILLISECOND, 1);
      fromDueDate = c.getTime();
      c.add(Calendar.HOUR_OF_DAY, 24);
      c.add(Calendar.MILLISECOND, -1);
      toDueDate = c.getTime();

      q = query.clone();
      q.setDueDateFrom(fromDueDate);
      q.setDueDateTo(toDueDate);

      key = new GroupKey("Tomorrow", fromDueDate, 2);
      tasks = taskService.findTasks(q);
      if (ListUtil.getSize(tasks) > 0) {
        maps.put(key, tasks);
      }

      // Upcoming
      c.add(Calendar.MILLISECOND, 1);
      fromDueDate = c.getTime();
      toDueDate = null;

      q = query.clone();
      q.setDueDateFrom(fromDueDate);
      q.setDueDateTo(toDueDate);

      key = new GroupKey("Upcoming", fromDueDate, 3);
      tasks = taskService.findTasks(q);
      if (ListUtil.getSize(tasks) > 0) {
        maps.put(key, tasks);
      }


      // No Due date
      q = query.clone();
      q.setNullField(DUEDATE);
      key = new GroupKey("No Due date", null, 4);
      tasks = taskService.findTasks(q);
      if (ListUtil.getSize(tasks) > 0) {
        maps.put(key, tasks);
      }

    } else if (PROJECT.equalsIgnoreCase(groupBy)) {
      List<Project> projects = taskService.selectTaskField(selectFieldQuery, "status.project");
      for (int i = 0; i < projects.size(); i++) {
        Project p = projects.get(i);
        GroupKey key = new GroupKey(p.getName(), p, i);
        TaskQuery q = query.clone();
        q.setProjectIds(Arrays.asList(p.getId()));
        ListAccess<Task> tasks = taskService.findTasks(q);
        if (ListUtil.getSize(tasks) > 0) {
          maps.put(key, tasks);
        }
      }

      //
      if (query.getProjectIds() == null || query.getProjectIds().isEmpty()) {
        GroupKey key = new GroupKey("No Project", null, Integer.MAX_VALUE);
        TaskQuery q = query.clone();
        q.setNullField(STATUS);
        ListAccess<Task> tasks = taskService.findTasks(q);
        if (ListUtil.getSize(tasks) > 0) {
          maps.put(key, tasks);
        }
      }

    } else if (STATUS.equalsIgnoreCase(groupBy)) {
      List<Status> statuses = taskService.selectTaskField(selectFieldQuery, STATUS);
      TaskQuery q;
      GroupKey key;
      ListAccess<Task> tasks;

      for (Status st : statuses) {
        q = query.clone();
        q.setStatus(st);
        key = new GroupKey(st.getName(), st, st.getRank());
        tasks = taskService.findTasks(q);
        if (ListUtil.getSize(tasks) > 0) {
          maps.put(key, tasks);
        }
      }
      if (query.getProjectIds() == null || query.getProjectIds().isEmpty()) {
        q = query.clone();
        q.setNullField(STATUS);
        key = new GroupKey("No Status", null, Integer.MAX_VALUE);
        tasks = taskService.findTasks(q);
        if (ListUtil.getSize(tasks) > 0) {
          maps.put(key, tasks);
        }
      }

    } else if (ASSIGNEE.equalsIgnoreCase(groupBy)) {
      List<String> assignees = taskService.selectTaskField(selectFieldQuery, ASSIGNEE);
      GroupKey key;
      ListAccess<Task> tasks;
      TaskQuery q;

      for (int i = 0; i < assignees.size(); i++) {
        String assignee = assignees.get(i);
        if (assignee == null) continue;
        q = query.clone();
        q.setAssignee(Arrays.asList(assignee));
        org.exoplatform.task.model.User user = userService.loadUser(assignee);
        key = new GroupKey(user.getDisplayName(), user, i);
        tasks = taskService.findTasks(q);
        if (ListUtil.getSize(tasks) > 0) {
          maps.put(key, tasks);
        }
      }

      if (query.getAssignee() == null) {
        q = query.clone();
        q.setNullField(ASSIGNEE);
        key = new GroupKey("Unassigned", null, Integer.MAX_VALUE);
        tasks = taskService.findTasks(q);
        if (ListUtil.getSize(tasks) > 0) {
          maps.put(key, tasks);
        }
      }
    } else if (LABEL.equalsIgnoreCase(groupBy)) {
      List<Label> labels = taskService.selectTaskField(selectFieldQuery, "lblMapping.label");
      GroupKey key;
      ListAccess<Task> tasks;

      for (int i = 0; i < labels.size(); i++) {
        Label label = labels.get(i);
        if (label == null) continue;
        TaskQuery q = query.clone();
        q.setLabelIds(Arrays.asList(label.getId()));
        key = new GroupKey(label.getName(), label, i);
        tasks = taskService.findTasks(q);
        if (ListUtil.getSize(tasks) > 0) {
          maps.put(key, tasks);
        }
      }

      TaskQuery q = query.clone();
      q.setEmptyField("lblMapping");
      key = new GroupKey("No Label", null, Integer.MAX_VALUE);
      tasks = taskService.findTasks(q);
      if (ListUtil.getSize(tasks) > 0) {
        maps.put(key, tasks);
      }
    }
    return maps;
  }

  public static String getWorkPlan(Calendar start, Calendar end, ResourceBundle bundle) {
    if (start == null || end == null) {
      return null;
    }    
    
    //
    long duration = normalizeDate(end).getTimeInMillis() - normalizeDate(start).getTimeInMillis();
    long durationInMinute = duration / 60000; // 1 minute = 60 * 1000 milisecond
    long durationInHours = durationInMinute / 60;
    long durationInDays = durationInHours / 24;
    boolean hasHalfOfHour = (durationInMinute % 60 != 0);

    Calendar current = Calendar.getInstance(start.getTimeZone());

    boolean startTimeCurrentYear = start.get(Calendar.YEAR) == current.get(Calendar.YEAR);
    boolean endTimeCurrentYear = end.get(Calendar.YEAR) == current.get(Calendar.YEAR);
    boolean isSameYear = start.get(Calendar.YEAR) == end.get(Calendar.YEAR);
    boolean isSameMonth = isSameYear && start.get(Calendar.MONTH) == end.get(Calendar.MONTH);
    boolean isSameDay = isSameMonth && start.get(Calendar.DATE) == end.get(Calendar.DATE);

    String endDateFormat = endTimeCurrentYear ? "dd MMM" : "dd MMM YYYY";
    String startDateFormat;

    if (isSameMonth) {
      startDateFormat = "dd";
    } else if (isSameYear || startTimeCurrentYear) {
      startDateFormat = "dd MMM";
    } else {
      startDateFormat = "dd MMM YYYY";
    }

    Locale locale = bundle != null ? bundle.getLocale() : Locale.getDefault(Locale.Category.FORMAT);
    SimpleDateFormat startDF = new SimpleDateFormat(startDateFormat, locale);
    startDF.setTimeZone(start.getTimeZone());
    SimpleDateFormat endDF = new SimpleDateFormat(endDateFormat, locale);
    endDF.setTimeZone(start.getTimeZone());

    String fromDate = startDF.format(start.getTime());
    String toDate = endDF.format(end.getTime());

    String bundleKey;
    List<String> params;

    if (durationInHours == 0) {
      // 30 minutes
      bundleKey = "message.workPlaned.minutes";
      params = Arrays.asList(endDF.format(start.getTime()));
    } else if (isSameDay && durationInHours == 1) {
      params = Arrays.asList(toDate);
      bundleKey = hasHalfOfHour ? "message.workPlaned.hourMinutes" : "message.workPlaned.hour";
    } else if (isSameDay && durationInHours < 24) {
      params = Arrays.asList(toDate, String.valueOf(durationInHours));
      bundleKey = hasHalfOfHour ? "message.workPlaned.hoursMinutes" : "message.workPlaned.hours";
    } else if (isSameDay && durationInHours == 24) {
      params = Arrays.asList(toDate);
      bundleKey = "message.workPlaned.day";
    } else {
      params = Arrays.asList(fromDate, toDate, String.valueOf(durationInHours));
      if (durationInHours == 1) {
        bundleKey = hasHalfOfHour ? "message.workPlaned.daysHourMinutes" : "message.workPlaned.daysHour";
      } else {
        bundleKey = hasHalfOfHour ? "message.workPlaned.daysHoursMinutes" : "message.workPlaned.daysHours";
      }
    }

    return ResourceUtil.resolveMessage(bundle, bundleKey, params.toArray());
  }

  public static long getTaskIdFromURI(String requestPath) {
    long taskId = -1;
    int index = requestPath.indexOf(URL_TASK_DETAIL);
    if (index >= 0) {
      index = index + URL_TASK_DETAIL.length();
      int lastIndex = requestPath.indexOf('/', index);
      String id;
      if (lastIndex > 0) {
        id = requestPath.substring(index, lastIndex);
      } else {
        id = requestPath.substring(index);
      }
      try {
        taskId = Long.parseLong(id);
      } catch (NumberFormatException ex) {
        taskId = -1;
      }
    }
    return taskId;
  }

  public static String buildTaskURL(Task task) {
    if (task == null) {
      return "#";
    }

    StringBuilder urlBuilder = new StringBuilder(ResourceUtil.buildBaseURL());
    if (urlBuilder.length() <= 1) {
      return urlBuilder.toString();
    } else {
      return urlBuilder.append(URL_TASK_DETAIL)
              .append(task.getId())
              .toString();
    }
  }

  public static String buildTaskURL(Task task, SiteKey siteKey, ExoContainer container, Router router) {
    if (task == null) {
      return "#";
    }

    StringBuilder urlBuilder = new StringBuilder(ResourceUtil.buildBaseURL(siteKey, container, router));
    if (urlBuilder.length() <= 1) {
      return urlBuilder.toString();
    } else {
      return urlBuilder.append(URL_TASK_DETAIL)
          .append(task.getId())
          .toString();
    }
  }

  private static void buildHour(long duration, StringBuilder workplan, ResourceBundle bundle) {
    workplan.append(" (");        
    int halfHour = 30 * 60 * 1000;
    if (duration == halfHour) {
      workplan.append("30 ").append(bundle.getString("label.minutes")).append(")");
    } else if (duration == halfHour * 2) {
      workplan.append("1 ").append(bundle.getString("label.hour")).append(")");
    } else if (duration == halfHour * 48) {
      workplan.append(bundle.getString("label.allday")).append(")");
    } else {      
      long hour = duration / (halfHour*2);
      long day = hour / 24;
      if (day > 0) {
        hour = hour % 24;
      }
      long odd = duration % (halfHour * 2);
      
      boolean conjunction = false;
      if (day > 0) {
        workplan.append(day).append(" ").append(bundle.getString("label.days"));
        conjunction = true;
      }
      if (hour > 0) {
        if (conjunction) {
          workplan.append(" ");
        }
        workplan.append(hour).append(" ").append(bundle.getString("label.hours"));
        conjunction = true;
      }
      if (odd > 0) {
        if (conjunction) {
          workplan.append(" ");
        }
        workplan.append("30 ").append(bundle.getString("label.minutes"));
      }
      workplan.append(")");
    }
  }

  private static Calendar normalizeDate(Calendar date) {
    Calendar result = Calendar.getInstance();
    result.setTimeInMillis(date.getTimeInMillis());
    if (result.get(Calendar.MINUTE) == 59) {
      result.add(Calendar.MINUTE, 1);
    }
    return result;
  }

  private static GroupKey[] getGroupName(Task task, String groupBy, String username, TimeZone userTimezone, ResourceBundle bundle, TaskService taskService, UserService userService) throws EntityNotFoundException {
    if("project".equalsIgnoreCase(groupBy)) {
      Status s = task.getStatus();
      if(s == null) {
        return new GroupKey[] {new GroupKey(bundle.getString("label.noProject"), null, Integer.MAX_VALUE)};
      } else {
        return new GroupKey[] {new GroupKey(s.getProject().getName(), s.getProject(), (int)s.getProject().getId())};
      }
    } else if("status".equalsIgnoreCase(groupBy)) {
      Status s = task.getStatus();
      if(s == null) {
        return new GroupKey[] {new GroupKey(bundle.getString("label.todo"), null, Integer.MIN_VALUE)};
      } else {
        return new GroupKey[] {new GroupKey(ResourceUtil.resolveStatus(bundle, s.getName()), s, s.getRank())};
      }
    } else if ("assignee".equalsIgnoreCase(groupBy)) {
      String assignee = task.getAssignee();
      User user = userService.loadUser(assignee);
      return (assignee != null && user.getUsername() != "guest" && user.getUsername() != null) ? new GroupKey[]{new GroupKey(assignee, user, assignee.hashCode())} : new GroupKey[]{new GroupKey(bundle.getString("label.unassigned"), null, Integer.MAX_VALUE)};

    } else if ("dueDate".equalsIgnoreCase(groupBy)) {
      Date dueDate = task.getDueDate();
      Calendar calendar = null;
      if (dueDate != null) {
        calendar = Calendar.getInstance(userTimezone);
        calendar.setTime(dueDate);
        if (DateUtil.isOverdue(calendar)) {
          calendar.setTimeInMillis(System.currentTimeMillis());
          calendar.add(Calendar.DATE, -1);
        } else if (!DateUtil.isToday(calendar) && !DateUtil.isTomorrow(calendar)) {
          //. is upcoming task
          calendar.setTimeInMillis(System.currentTimeMillis());
          calendar.add(Calendar.DATE, 7);
        }
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //
        dueDate = calendar.getTime();
      }
      return new GroupKey[] {new GroupKey(DateUtil.getDueDateLabel(calendar, bundle), dueDate, calendar == null ? Integer.MAX_VALUE : (int)calendar.getTimeInMillis())};

    } else if (TaskUtil.LABEL.equalsIgnoreCase(groupBy)) {
      ListAccess<Label> tmp = taskService.findLabelsByTask(task.getId(), username);
      List<Label> labels = Arrays.asList(ListUtil.load(tmp, 0, -1));
      if (labels.isEmpty()) {
        return new GroupKey[] {new GroupKey(bundle.getString("label.noLabel"), null, Integer.MAX_VALUE)};
      } else {
        GroupKey[] keys = new GroupKey[labels.size()];
        for (int i = 0; i < keys.length; i++) {
          Label label = labels.get(i);
          keys[i] = new GroupKey(label.getName(), label, (int)label.getId());
        }
        return keys;
      }
    }
    return new GroupKey[0];
  }

  private static GroupKey[] getGroupName(TaskEntity task, String groupBy, String username, TimeZone userTimezone, LabelService labelService, UserService userService) throws EntityNotFoundException {
    if("project".equalsIgnoreCase(groupBy)) {
      Status s = task.getStatus();
      if(s == null) {
        return new GroupKey[] {new GroupKey("No project", null, Integer.MAX_VALUE)};
      } else {
        return new GroupKey[] {new GroupKey(s.getProject().getName(), s.getProject(), (int)s.getProject().getId())};
      }
    } else if("status".equalsIgnoreCase(groupBy)) {
      Status s = task.getStatus();
      if(s == null) {
        return new GroupKey[] {new GroupKey("To do", null, Integer.MIN_VALUE)};
      }
    } else if ("assignee".equalsIgnoreCase(groupBy)) {
      User user = null;
      if (task.getAssignee()!=null){
        user = task.getAssignee();
      }
      else {
        user=userService.loadUser(null);
      }

      return ( user.getUsername() != "guest" && user.getUsername() != null) ? new GroupKey[]{new GroupKey(user.getUsername(), user, user.getUsername().hashCode())} : new GroupKey[]{new GroupKey("Unassigned", null, Integer.MAX_VALUE)};

    } else if ("dueDate".equalsIgnoreCase(groupBy)) {
      Date dueDate = task.getDueDate();
      Calendar calendar = null;
      if (dueDate != null) {
        calendar = Calendar.getInstance(userTimezone);
        calendar.setTime(dueDate);
        if (DateUtil.isOverdue(calendar)) {
          calendar.setTimeInMillis(System.currentTimeMillis());
          calendar.add(Calendar.DATE, -1);
        } else if (!DateUtil.isToday(calendar) && !DateUtil.isTomorrow(calendar)) {
          //. is upcoming task
          calendar.setTimeInMillis(System.currentTimeMillis());
          calendar.add(Calendar.DATE, 7);
        }
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //
        dueDate = calendar.getTime();
      }
      return new GroupKey[] {new GroupKey(DateUtil.getDueDateLabel(calendar), dueDate, calendar == null ? Integer.MAX_VALUE : (int)calendar.getTimeInMillis())};

    } else if (TaskUtil.LABEL.equalsIgnoreCase(groupBy)) {
      List<LabelDto> labels = labelService.findLabelsByTask(task.getId(), username,0,-1);
      if (labels.isEmpty()) {
        return new GroupKey[] {new GroupKey("No Label", null, Integer.MAX_VALUE)};
      } else {
        GroupKey[] keys = new GroupKey[labels.size()];
        for (int i = 0; i < keys.length; i++) {
          LabelDto label = labels.get(i);
          keys[i] = new GroupKey(label.getName(), label, (int)label.getId());
        }
        return keys;
      }
    }
    else if (TaskUtil.COMPLETED.equalsIgnoreCase(groupBy)) {

      if(task.isCompleted() == false) {
        return new GroupKey[] {new GroupKey("Uncompleted", null, Integer.MAX_VALUE)};
      } else {
        return new GroupKey[] {new GroupKey("Completed", null, Integer.MIN_VALUE)};
      }
    }
    return new GroupKey[0];
  }

  public static boolean isCalendarEnabled() {
    return ExoContainer.hasProfile("calendar");
  }
  public static List<Label> buildRootLabels(List<Label> labels) {
    if (labels == null) return labels;

    Set<Label> rootLBLs = new LinkedHashSet<Label>();
    Set<Label> childs = new LinkedHashSet<Label>();
    for (Label p : labels) {
      while(true) {
        Label parent = p.getParent();
        if (parent == null) {
          rootLBLs.add(p);
          break;
        } else {
          childs.add(p);
          p = parent;
        }
      }
    }

    List<Label> parents = new LinkedList<Label>(rootLBLs);
    do {
      List<Label> tmpParents = new LinkedList<Label>();
      for (Label p : parents) {
        List<Label> tmp = new LinkedList<Label>();
        for (Label c : childs) {
          if (c.getParent().equals(p)) {
            tmp.add(c);
          }
        }
        p.setChildren(tmp);
        tmpParents.addAll(tmp);
        childs.removeAll(tmp);
      }
      parents = tmpParents;
    } while (!parents.isEmpty() || !childs.isEmpty());

    return new LinkedList<Label>(rootLBLs);
  }
  
  public static List<Label> filterLabelTree(List<Label> allLabels, Label lbl) {
    List<Label> tmp = new LinkedList<Label>();
    for (Label l : allLabels) {
      if (!TaskUtil.isLabelIn(l, lbl)) {
        tmp.add(l);
      }
    }
    return tmp;
  }

  public static boolean canDeleteComment(Identity identity, Comment comment) {
    if (comment == null || identity == null) {
      return false;
    }

    // Owner can delete his comment
    if (identity.getUserId().equals(comment.getAuthor())) {
      return true;
    }

    // Project manager can delete comment
    Task task = comment.getTask();
    if (task.getStatus() != null) {
      Project pj = task.getStatus().getProject();
      return pj.canEdit(identity);
    }

    return false;
  }

  public static boolean canDeleteComment(Identity identity, CommentDto comment) {
    if (comment == null || identity == null) {
      return false;
    }

    // Owner can delete his comment
    if (identity.getUserId().equals(comment.getAuthor())) {
      return true;
    }

    // Project manager can delete comment
    Task task = comment.getTask();
    if (task.getStatus() != null) {
      Project pj = task.getStatus().getProject();
      return pj.canEdit(identity);
    }

    return false;
  }
  
  private static boolean isLabelIn(Label child, Label parent) {
    Label pr = child;
    while (pr != null) {
      if (pr.getId() == parent.getId()) {
        return true;
      }
      pr = pr.getParent();
    }
    return false;
  }

  public static Task saveTaskField(Task task, String username, String param, String[] values, TimeZone timezone, TaskService taskService, StatusService statusService)
      throws EntityNotFoundException, ParameterEntityException {

    if (timezone == null) {
      timezone = TimeZone.getDefault();
    }
    
    // Load coworker to avoid they will be deleted when save task
    //This issue caused because we alway clone entity from DAO, but Coworker are loaded lazily
    //This is need for TA-421
    task.setCoworker(TaskUtil.getCoworker(taskService, task.getId()));
    task.setWatcher(TaskUtil.getWatcher(taskService, task));

    //
    if ("workPlan".equalsIgnoreCase(param)) {
      //
      if (values == null) {
        task.setStartDate(null);
        task.setEndDate(null);
      } else {
        if (values.length != 2) {
          LOG.error("workPlan updating lack of params");
        }

        try {
          SimpleDateFormat wpf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
          wpf.setTimeZone(timezone);
          Date startDate = wpf.parse(values[0]);
          Date endDate = wpf.parse(values[1]);
          
          task.setStartDate(startDate);
          task.setEndDate(endDate);
        } catch (ParseException ex) {
          LOG.info("Can parse date time value: "+values[0]+" or "+values[1]+" for Task with ID: " + task.getId());
          throw new ParameterEntityException(task.getId(), Task.class, param, values[0]+" or "+values[1],
              "cannot be parse to date", ex);
        }
      }
    } else {
      String value = values != null && values.length > 0 ? values[0] : null;

      if("title".equalsIgnoreCase(param)) {
        task.setTitle(value);
      } else if("dueDate".equalsIgnoreCase(param)) {
        //
        if(value == null || value.trim().isEmpty()) {
          task.setDueDate(null);
        } else {
          DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
          df.setTimeZone(timezone);
          try {
            Date date = df.parse(value);
            task.setDueDate(date);
          } catch (ParseException ex) {
            LOG.info("Can parse date time value: "+value+" for Task with ID: "+ task.getId());
            throw new ParameterEntityException(task.getId(), Task.class, param, value, "cannot be parse to date", ex);
          }
        }
      } else if("status".equalsIgnoreCase(param)) {
        //
        try {
          Long statusId = Long.parseLong(value.toUpperCase());
          Status status = statusService.getStatus(statusId);
          if(status == null) {
            LOG.info("Status does not exist with ID: " + value);
            throw new EntityNotFoundException(task.getId(), Status.class);
          }
          task.setStatus(status);
        } catch (NumberFormatException ex) {
          LOG.info("Status is unacceptable: "+value+" for Task with ID: "+task.getId());
          throw new ParameterEntityException(task.getId(), Task.class, param, value, "is unacceptable", ex);
        }
      } else if("description".equalsIgnoreCase(param)) {
        //CKEditor encode user input already, but we still need to remove malicious code
        //here in case user inject request using curl TA-387
        value = StringUtil.encodeInjectedHtmlTag(value);
        task.setDescription(value);
      } else if("completed".equalsIgnoreCase(param)) {
        task.setCompleted(Boolean.parseBoolean(value));
      } else if("assignee".equalsIgnoreCase(param)) {
        task.setAssignee(value);
      } else if("coworker".equalsIgnoreCase(param)) {
        Set<String> coworker = new HashSet<String>();
        if (values != null) {
          for (String v : values) {
            if (v != null && !v.isEmpty()) {
              coworker.add(v);
            }
          }
        }
        task.setCoworker(coworker);
      } else if("watcher".equalsIgnoreCase(param)) {
        Set<String> watcher = new HashSet<String>();
        if (values != null) {
          for (String v : values) {
            if (v != null && !v.isEmpty()) {
              watcher.add(v);
            }
          }
        }
        task.setWatcher(watcher);
      } else if ("priority".equalsIgnoreCase(param)) {
        Priority priority = Priority.valueOf(value);
        task.setPriority(priority);
      } else if ("project".equalsIgnoreCase(param)) {
        try {
          Long projectId = Long.parseLong(value);
          if (projectId > 0) {
            Status st = statusService.getDefaultStatus(projectId);
            if (st == null) {
              throw new ParameterEntityException(task.getId(), Task.class, param, value, "Status for project is not found", null);
            }
            task.setStatus(st);
          } else {
            task.setStatus(null);
          }
        } catch (NumberFormatException ex) {
          throw new ParameterEntityException(task.getId(), Task.class, param, value, "ProjectID must be long", ex);
        }
      } else if("labels".equalsIgnoreCase(param)) {
        List<Long> ids = new ArrayList<Long>(values.length);
        List<String> labelsToCreate = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
          try {
            if (values[i] == null || values[i].isEmpty()) {
              continue;
            }
            Long id = Long.parseLong(values[i]);
            Label label = taskService.getLabel(id);
            if (label != null) {
              ids.add(Long.parseLong(values[i]));
            } else {
              labelsToCreate.add(values[i]);
            }
          } catch (NumberFormatException ex) {
            //throw new ParameterEntityException(-1L, Label.class, param, values[i], "LabelID must be long", ex);
            labelsToCreate.add(values[i]);
          }
        }

        Set<Long> persisted = new HashSet<Long>();
        ListAccess<Label> tmp = taskService.findLabelsByTask(task.getId(), username);
        List<Label> labels = Arrays.asList(ListUtil.load(tmp, 0, -1));
        for(Label label : labels) {
          if (!ids.contains(label.getId())) {
            taskService.removeTaskFromLabel(task.getId(), label.getId());
          } else {
            persisted.add(label.getId());
          }
        }

        //
        for (String label : labelsToCreate) {
          Label l = new Label();
          l.setName(label);
          l.setUsername(username);
          l = taskService.createLabel(l);
          ids.add(l.getId());
        }
        
        for (Long labelId : ids) {
          if (!persisted.contains(labelId)) {
            taskService.addTaskToLabel(task.getId(), labelId);            
          }
        }
      } else if ("calendarIntegrated".equalsIgnoreCase(param)) {
        task.setCalendarIntegrated(Boolean.parseBoolean(value));
      } else {
        LOG.info("Field name: " + param + " is not supported for entity Task");
        throw new ParameterEntityException(task.getId(), Task.class, param, value, "is not supported for the entity Task", null);
      }
    }

    return taskService.updateTask(task);
  }
  
  public static boolean hasEditPermission(Task task) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    String userId = identity.getUserId();

    if ((task.getAssignee() != null && task.getAssignee().equals(identity.getUserId())) ||
        getCoworker(task.getId()).contains(userId) ||
        (task.getCreatedBy() != null && task.getCreatedBy().equals(userId))) {
      return true;
    }

    if (task.getStatus() != null) {
      Project project = task.getStatus().getProject();
      if (project.canView(identity)) {
        return true;
      }
    }

    return UserUtil.isPlatformAdmin(identity);
  }

  public static boolean hasEditPermission(TaskDto task) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    String userId = identity.getUserId();

    if ((task.getAssignee() != null && task.getAssignee().equals(identity.getUserId())) ||
        getCoworker(task.getId()).contains(userId) ||
        (task.getCreatedBy() != null && task.getCreatedBy().equals(userId))) {
      return true;
    }

    if (task.getStatus() != null) {
      Project project = task.getStatus().getProject();
      if (project.canView(identity)) {
        return true;
      }
    }

    return UserUtil.isPlatformAdmin(identity);
  }

  public static boolean hasViewPermission(Task task) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    String userId = identity.getUserId();

    return hasMentionedUser(task, userId) || hasEditPermission(task);
  }

  public static boolean hasViewPermission(TaskDto task) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    //String userId = identity.getUserId();

    return hasEditPermission(task);
  }

  private static boolean hasMentionedUser(Task task, String userId) {
    TaskService taskService = getTaskService();
    Set<String> mentionedUsers = taskService.getMentionedUsers(task.getId());
    return mentionedUsers.contains(userId);
  }

  private static boolean hasMentionedUser(TaskDto task, String userId) {
    TaskService taskService = getTaskService();
    Set<String> mentionedUsers = taskService.getMentionedUsers(task.getId());
    return mentionedUsers.contains(userId);
  }

  public static boolean hasViewOnlyPermission(Task task) {
    return hasViewPermission(task) && !hasEditPermission(task);
  }

  public static boolean hasDeletePermission(Task task) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    String userId = identity.getUserId();

    if (task.getCreatedBy() != null && task.getCreatedBy().equals(userId)) {
      return true;
    }

    if (task.getStatus() != null) {
      Project project = task.getStatus().getProject();
      if (project.canEdit(identity)) {
        return true;
      }
    }

    return UserUtil.isPlatformAdmin(identity);
  }

  public static boolean hasPermissionOnField(Task task, String name, String[] values, StatusService statusService, 
                                             TaskService taskService, ProjectService projectService) {
    String value = values != null && values.length > 0 ? values[0] : null;
    Identity identity = ConversationState.getCurrent().getIdentity();
    
    try {
      if("status".equalsIgnoreCase(name)) {
        Long statusId = Long.parseLong(value);
        Status status = statusService.getStatus(statusId);
        return status.getProject().canView(identity);
      } else if ("project".equalsIgnoreCase(name)) {
        Long projectId = Long.parseLong(value);
        if (projectId > 0) {
          Project project = projectService.getProject(projectId);
          return project.canView(identity);
        }
      } else if("labels".equalsIgnoreCase(name)) {
        boolean valid = true;
        for (int i = 0; i < values.length; i++) {
          if (values[i] == null || values[i].isEmpty()) {
            continue;
          }
          long labelId = 0;
          try {
            labelId = Long.parseLong(values[i]);
            if (labelId > 0) {
              Label label = taskService.getLabel(labelId);
              if (label != null) {
                valid = valid && label.getUsername().equals(identity.getUserId());
              }
            }
          } catch (NumberFormatException ex) {
            // Ignore, the label will be created later
          }
        }
        return valid;
      }
    } catch (Exception ex) {
      LOG.error("Can not check permission on task field", ex);
    }
    return true;
  }

  private static TaskService getTaskService() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    if (container instanceof RootContainer) {
      container = ((RootContainer)container).getPortalContainer(PortalContainer.getCurrentPortalContainerName());
    }
    return container.getComponentInstanceOfType(TaskService.class);
  }

  /**
   * 
   * Added for tests using a specific instance of taskService
   * 
   * @param taskService
   * @param taskId
   * @return
   */
  public static Set<String> getCoworker(TaskService taskService, long taskId) {
    return taskService.getCoworker(taskId);
  }

  public static Set<String> getCoworker(long taskId) {
    return getTaskService().getCoworker(taskId);
  }

  public static Set<String> getWatcher(TaskService taskService, Task task) {
    return taskService.getWatchersOfTask(task);
  }
  public static boolean isEligibleWatcher(Task task,String username){
    if(!task.getCoworker().contains(username)&& !username.equals(task.getAssignee())&& !username.equals(task.getCreatedBy())) {
      return true;
    } else {
      return false;
    }
  }
}

