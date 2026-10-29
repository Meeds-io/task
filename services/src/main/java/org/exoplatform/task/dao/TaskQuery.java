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
package org.exoplatform.task.dao;

import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.condition.AggregateCondition;
import org.exoplatform.task.dao.condition.CardConditions;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dao.condition.Conditions;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.util.UserUtil;

import java.util.*;
import java.util.stream.Stream;

import static org.exoplatform.task.dao.condition.Conditions.*;

public class TaskQuery extends Query implements Cloneable {

  /**
   * The change log actions of a status change: a move to another column, to
   * another project, or out of any project.
   */
  public static final List<String> STATUS_CHANGE_ACTIONS   = List.of("edit_status", "edit_project", "remove_project");

  /** The change log action of a due date change. */
  public static final String       DUE_DATE_CHANGE_ACTION  = "edit_duedate";

  /** The change log actions of an assignee change. */
  public static final List<String> ASSIGNEE_CHANGE_ACTIONS = List.of("assign", "unassign");

  /**
   * The change log action of a completion change, logged when a task is
   * completed and when it is reopened.
   */
  public static final String       COMPLETION_ACTION       = "mark_done";

  /**
   * The change log actions {@link #setChangedSince(Date)} looks for: a
   * status, due date, assignee or completion change; an assignee change only
   * on a task created before the date.
   */
  public static final List<String> CHANGE_ACTIONS          = Stream.of(STATUS_CHANGE_ACTIONS,
                                                                       List.of(DUE_DATE_CHANGE_ACTION),
                                                                       ASSIGNEE_CHANGE_ACTIONS,
                                                                       List.of(COMPLETION_ACTION))
                                                                   .flatMap(List::stream)
                                                                   .toList();

  //TODO: how to remove these two field
  private List<Long> projectIds = null;
  private List<String> assignee = null;
  private List<String> coworker = null;
  private List<String> watchers = null;

  public TaskQuery() {

  }

  TaskQuery(AggregateCondition condition, List<OrderBy> orderBies, List<Long> projectIds, List<String> assignee, List<String> coworker, List<String> watchers) {
    super(condition, orderBies);
    this.projectIds = projectIds;
    this.assignee = assignee;
    this.coworker= coworker;
    this.watchers= watchers;
  }

  public static TaskQuery or(TaskQuery... queries) {
    List<Condition> cond = new ArrayList<Condition>();
    for(TaskQuery q : queries) {
      if (q.getCondition() != null) {
        cond.add(q.getCondition());
      }
    }
    Condition c = Conditions.or(cond.toArray(new Condition[cond.size()]));
    TaskQuery q = new TaskQuery();
    q.add(c);
    return q;
  }

  public TaskQuery add(TaskQuery taskQuery) {
    this.add(taskQuery.getCondition());
    return this;
  }

  public void setTitle(String title) {
    this.add(like(TASK_TITLE, '%' + title + '%'));
  }

  public void setDescription(String description) {
    this.add(like(TASK_DES, '%' + description + '%'));
  }

  public List<Long> getProjectIds() {
    return projectIds;
  }

  public void setProjectIds(List<Long> projectIds) {
    this.projectIds = projectIds;
    this.add(in(TASK_PROJECT, projectIds));
  }

  public List<String> getAssignee() {
    return assignee;
  }

  public void setAssignee(List<String> assignee) {
    if (assignee != null) {
      this.add(in(TASK_ASSIGNEE, assignee));
    }
    this.assignee = assignee;
  }

  public List<String> getWatchers() {
    return watchers;
  }

  public void setWatchers(List<String> watchers) {
    if (watchers != null) {
      this.add(in(TASK_WATCHER, watchers));
    }
    this.watchers = watchers;
  }

  public void setCoworker(List<String> coworker) {
    if (coworker != null) {
      this.add(in(TASK_COWORKER, coworker));
    }
    this.coworker = coworker;
  }

  public void setAssigneeOrCoworker(List<String> assignee) {
    if (assignee != null && assignee.size() > 0) {
      this.add(Conditions.or(in(TASK_ASSIGNEE, assignee), in(TASK_COWORKER, assignee)));
    }
    this.assignee = assignee;
  }

  public void setKeyword(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) return;

    List<Condition> conditions = new ArrayList<Condition>();
    for(String k : keyword.split(" ")) {
      if (!k.trim().isEmpty()) {
        k = "%" + k.trim().toLowerCase() + "%";
        conditions.add(like(TASK_TITLE, k));
        conditions.add(like(TASK_DES, k));
        conditions.add(like(TASK_ASSIGNEE, k));
      }
    }
    add(Conditions.or(conditions.toArray(new Condition[conditions.size()])));
  }
  
  public void setCompleted(Boolean completed) {
    if (completed) {
      add(isTrue(TASK_COMPLETED));
    } else {
      add(isFalse(TASK_COMPLETED));
    }
  }

  public void setStartDate(Date startDate) {
    add(gte(TASK_END_DATE, startDate));
  }

  public void setEndDate(Date endDate) {
    add(lte(TASK_START_DATE, endDate));
  }

  public void setCreatedBy(String creator) {
    add(eq(TASK_CREATOR, creator));
  }

  public void setMemberships(List<String> permissions) {
    add(Conditions.or(in(TASK_PARTICIPATOR, permissions), in(TASK_MANAGER, permissions)));
  }

  @Deprecated
  public void setAssigneeOrMembership(String username, List<String> memberships) {
    this.assignee = Arrays.asList(username);
    this.add(Conditions.or(eq(TASK_ASSIGNEE, username), in(TASK_MANAGER, memberships), in(TASK_PARTICIPATOR, memberships)));
  }
  
  public void setAccessible(Identity user) {
    this.assignee = Arrays.asList(user.getUserId());
    List<String> memberships = UserUtil.getMemberships(user);
    this.add(Conditions.or(eq(TASK_ASSIGNEE, user.getUserId()), in(TASK_COWORKER, assignee), eq(TASK_CREATOR, user.getUserId()),
                           in(TASK_MANAGER, memberships), in(TASK_PARTICIPATOR, memberships),
                           in(TASK_MENTIONED_USERS, this.assignee)));
  }

  public void setAssigneeOrCoworkerOrInProject(String username, List<Long> projectIds) {
    this.assignee = Arrays.asList(username);
    this.projectIds = projectIds;
    this.add(Conditions.or(eq(TASK_ASSIGNEE, username), in(TASK_COWORKER, Collections.singletonList(username)), in(TASK_PROJECT, projectIds)));
  }

  public void setStatusName(String statusName) {
    add(eq(TASK_STATUS_NAME, statusName));
  }

  public void setStatus(Status status) {
    add(eq(TASK_STATUS, status));
  }

  public void setStatus(StatusDto status) {
    add(eq(TASK_STATUS, status));
  }

  public void setDueDateFrom(Date dueDateFrom) {
    if (dueDateFrom != null) {
      add(gte(TASK_DUEDATE, dueDateFrom));
    }
  }

  public void setDueDateTo(Date dueDateTo) {
    if (dueDateTo != null) {
      add(lte(TASK_DUEDATE, dueDateTo));
    }
  }

  /**
   * Selects the overdue tasks: open, with a due date strictly before the
   * given instant, the start of the user's day (EXO-91122). A task due at
   * that instant, today, is not overdue; a task with no due date never is.
   *
   * @param startOfToday the start of today in the user's time zone
   */
  public void setOverdueAt(Date startOfToday) {
    if (startOfToday != null) {
      setCompleted(false);
      add(lt(TASK_DUEDATE, startOfToday));
    }
  }

  /**
   * Selects the tasks whose status, due date, assignee or completion changed
   * at or after the given date, as their change log records it (EXO-91902).
   * An assignee change counts only on a task created before the date: a task
   * created with an assignee logs that assignment at its creation, which is
   * no change.
   *
   * @param since the earliest date of the change, included; null for no
   *          condition
   */
  public void setChangedSince(Date since) {
    if (since != null) {
      List<String> otherActions = CHANGE_ACTIONS.stream().filter(action -> !ASSIGNEE_CHANGE_ACTIONS.contains(action)).toList();
      add(Conditions.or(loggedSince(otherActions, since),
                        and(loggedSince(ASSIGNEE_CHANGE_ACTIONS, since), lt(TASK_CREATED_TIME, since))));
    }
  }

  /**
   * Selects the completed tasks that were completed at or after the given
   * date, as their change log records it (EXO-91902).
   *
   * @param since the earliest date of the completion, included; null for no
   *          condition
   */
  public void setCompletedSince(Date since) {
    if (since != null) {
      setCompleted(true);
      add(loggedSince(List.of(COMPLETION_ACTION), since));
    }
  }

  /**
   * Selects the tasks in the given status, a column of a project
   * (EXO-91902).
   *
   * @param statusId the status identifier; null for no condition
   */
  public void setStatusId(Long statusId) {
    if (statusId != null) {
      add(eq(TASK_STATUS_ID, statusId));
    }
  }

  /**
   * Selects the tasks whose column has one of the given names, compared
   * trimmed and ignoring case, or none of them (EXO-92467).
   *
   * @param names the column names, at least one
   * @param excluded true to keep the tasks whose column has none of the names
   */
  public void setStatusNames(Collection<String> names, boolean excluded) {
    add(CardConditions.statusNames(names, excluded));
  }

  /**
   * Selects the tasks assigned to one of the given users, or with one of them
   * as assignee or coworker (EXO-92467).
   *
   * @param usernames the users, at least one
   * @param includeCoworkers true to also select the tasks one of them works on
   *          as a coworker
   */
  public void setAssignedTo(Collection<String> usernames, boolean includeCoworkers) {
    add(CardConditions.assignedTo(usernames, includeCoworkers));
  }

  /**
   * Selects the tasks with no assignee, and no coworker when coworkers count
   * (EXO-92467).
   *
   * @param includeCoworkers true to also require that the task has no coworker
   */
  public void setUnassigned(boolean includeCoworkers) {
    add(CardConditions.unassigned(includeCoworkers));
  }

  /**
   * Selects the tasks whose due date or start date falls in a range, the start
   * included and the end excluded (EXO-92467). Unlike {@link #setStartDate} and
   * {@link #setEndDate}, it compares the field it names.
   *
   * @param field {@link Conditions#TASK_DUEDATE} or
   *          {@link Conditions#TASK_START_DATE}
   * @param from the first instant, included; null for no start
   * @param to the instant after the range, excluded; null for no end
   */
  public void setDateBetween(String field, Date from, Date to) {
    add(CardConditions.dateBetween(field, from, to));
  }

  /**
   * Selects the tasks with no due date or no start date (EXO-92467).
   *
   * @param field {@link Conditions#TASK_DUEDATE} or
   *          {@link Conditions#TASK_START_DATE}
   */
  public void setNoDate(String field) {
    add(CardConditions.noDate(field));
  }

  /**
   * Selects the tasks that carry a label of each given name, among the labels
   * of the task's board and the viewer's personal labels (EXO-92467).
   *
   * @param names the label names, at least one
   * @param viewer the user making the query
   */
  public void setLabelNamesAll(Collection<String> names, String viewer) {
    if (names == null || names.isEmpty()) {
      throw new IllegalArgumentException("A label criterion needs at least one name");
    }
    names.forEach(name -> add(CardConditions.hasLabel(name, viewer)));
  }

  /**
   * Selects the tasks that carry a label with one of the given names, or none
   * of them, among the labels of the task's board and the viewer's personal
   * labels (EXO-92467).
   *
   * @param names the label names, at least one
   * @param viewer the user making the query
   * @param excluded true to keep the tasks that carry none of these labels
   */
  public void setLabelNamesAny(Collection<String> names, String viewer, boolean excluded) {
    add(CardConditions.hasAnyLabel(names, viewer, excluded));
  }

  public void setIsIncomingOf(String username) {
    add(and(Conditions.or(
            eq(TASK_ASSIGNEE, username), in(TASK_COWORKER, Collections.singletonList(username)), eq(TASK_CREATOR, username),
            in(TASK_MENTIONED_USERS, Collections.singletonList(username))),
            isNull(TASK_STATUS)));
  }

  public void setIsTodoOf(String username) {
    //setAssignee(Arrays.asList(username));
    //add(eq(TASK_ASSIGNEE, username));
    this.add(Conditions.or(eq(TASK_ASSIGNEE, username), in(TASK_COWORKER, Collections.singletonList(username)), in(TASK_MENTIONED_USERS, Arrays.asList(username))));
    this.assignee = Arrays.asList(username);
  }

  public void setAssigneeIsTodoOf(String username) {
    this.add(Conditions.or(eq(TASK_ASSIGNEE, username), in(TASK_COWORKER, Collections.singletonList(username))));
  }

  public void setLabelIds(List<Long> labelIds) {
    if (labelIds != null) {
      List<Condition> cond = new LinkedList<Condition>();
      for (Long id : labelIds) {
        cond.add(eq(TASK_LABEL_ID, id));
      }
      this.add(Conditions.and(cond.toArray(new Condition[cond.size()])));
    }
  }
  
  public void setIsLabelOf(String username) {
    this.add(eq(TASK_LABEL_USERNAME, username));
  }

  public void setPriority(Priority priority) {
    this.add(eq(TASK_PRIORITY, priority));
  }

  public void setNullField(String nullField) {
    add(isNull(nullField));
  }

  public void setEmptyField(String emptyField) {
    add(isEmpty(emptyField));
  }

  public TaskQuery clone() {
    Condition condition = getCondition();
    return new TaskQuery(condition != null ? (AggregateCondition)condition.clone() : null, getOrderBy(), projectIds != null ? new ArrayList<Long>(projectIds) : null, assignee,coworker,watchers);
  }

}
