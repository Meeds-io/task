/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.dao.condition.AggregateCondition;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dao.condition.LoggedSince;
import org.exoplatform.task.dao.condition.SingleCondition;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.model.TaskChangeFilter;
import org.exoplatform.task.model.TaskChanges;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.model.TaskFieldChange;
import org.exoplatform.task.storage.TaskStorage;

/**
 * The rules of a task's changes since a date (EXO-91902): the conditions a
 * change filter adds to a task query, and the changes rebuilt from a task's
 * change log.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskServiceChangesTest {

  private static final String USER       = "root";

  private static final String OTHER_USER = "john";

  private static final long   TASK_ID    = 12L;

  private static final Date   SINCE      = Date.from(Instant.parse("2026-10-01T00:00:00Z"));

  @Mock
  private TaskStorage         taskStorage;

  private TaskServiceImpl     taskService;

  /**
   * Builds the service on a mocked storage.
   */
  @Before
  public void setUp() {
    taskService = new TaskServiceImpl(taskStorage, null, null);
  }

  /**
   * changed_since adds a status, project, due date or completion entry at or
   * after the date, or an assignee entry at or after it on a task created
   * before it, beside the query's own conditions. Mutants: another action
   * list; the creation date condition dropped, inverted or on another date;
   * the two branches joined by a conjunction; the filter not applied.
   */
  @Test
  public void changedSinceSelectsTheTasksWithAChangeSinceTheDate() {
    TaskQuery query = new TaskQuery();
    query.setProjectIds(List.of(45L));

    taskService.addChangeFilter(query, new TaskChangeFilter(SINCE, null, null, null, false));

    assertChangedSince(query, SINCE);
    assertTrue(conjunction(query).stream().anyMatch(c -> isSingle(c, SingleCondition.IN, "status.project.id", List.of(45L))));
    assertFalse(selectsCompleted(query, false));
  }

  /**
   * completed_since selects the completed tasks with a completion entry at
   * or after the date, and lifts hide_completed_tasks, which would
   * otherwise select the open tasks only and so nothing; without it,
   * hide_completed_tasks leaves the completed tasks out. Mutants: the
   * completed condition dropped; hide_completed_tasks applied with
   * completed_since, or never.
   */
  @Test
  public void completedSinceSelectsTheTasksCompletedSinceThatDate() {
    TaskQuery completed = new TaskQuery();
    taskService.addChangeFilter(completed, new TaskChangeFilter(null, SINCE, null, null, true));
    TaskQuery hidden = new TaskQuery();
    taskService.addChangeFilter(hidden, new TaskChangeFilter(SINCE, null, null, null, true));

    assertEquals(new LoggedSince(List.of("mark_done"), SINCE), findLoggedSince(completed));
    assertTrue(selectsCompleted(completed, true));
    assertFalse(selectsCompleted(completed, false));
    assertTrue(selectsCompleted(hidden, false));
  }

  /**
   * assignee and status_id narrow the query, the assignee trimmed; a blank
   * assignee and a null status add no condition, nor does a null filter.
   * Mutants: either filter dropped, the assignee untrimmed, a blank assignee
   * matching nobody.
   */
  @Test
  public void assigneeAndStatusNarrowTheQuery() {
    TaskQuery filtered = new TaskQuery();
    taskService.addChangeFilter(filtered, new TaskChangeFilter(null, null, " john ", 8L, false));
    TaskQuery blank = new TaskQuery();
    taskService.addChangeFilter(blank, new TaskChangeFilter(null, null, " ", null, false));
    TaskQuery none = new TaskQuery();
    taskService.addChangeFilter(none, null);

    assertTrue(conjunction(filtered).stream().anyMatch(c -> isSingle(c, SingleCondition.IN, "assignee", List.of(OTHER_USER))));
    assertTrue(conjunction(filtered).stream().anyMatch(c -> isSingle(c, SingleCondition.EQ, "status.id", 8L)));
    assertNull(blank.getCondition());
    assertNull(none.getCondition());
  }

  /**
   * The tasks assigned to a user are those they are assigned to or a
   * coworker of, narrowed by the filter, completed ones included. Mutants:
   * the user scope dropped; the filter not applied.
   */
  @Test
  public void theAssignedTasksQueryIsTheUsersTasksNarrowedByTheFilter() {
    TaskQuery query = taskService.getAssignedTasksQuery(USER, new TaskChangeFilter(SINCE, null, null, null, false));

    AggregateCondition scope = conjunction(query).stream()
                                                 .filter(c -> c instanceof AggregateCondition aggregate
                                                              && aggregate.getConditions()
                                                                          .stream()
                                                                          .anyMatch(m -> isSingle(m, SingleCondition.IN, "assignee", List.of(USER))))
                                                 .map(AggregateCondition.class::cast)
                                                 .findFirst()
                                                 .orElseThrow();
    assertTrue(scope.getConditions().stream().anyMatch(m -> isSingle(m, SingleCondition.IN, "coworker", List.of(USER))));
    assertChangedSince(query, SINCE);
    assertFalse(selectsCompleted(query, false));
  }

  /**
   * The changes are read from the whole change log, the task having been
   * selected on an entry the newest ones may not hold, while the change log
   * returned holds the newest hundred entries, and none when not asked for.
   * Mutants: a bounded read; the whole log returned; a log returned when
   * not asked for.
   *
   * @throws Exception never
   */
  @Test
  public void theChangesReadTheWholeLogAndTheChangeLogTheNewestHundred() throws Exception {
    TaskDto task = mock(TaskDto.class);
    when(task.getId()).thenReturn(TASK_ID);
    when(task.getCreatedTime()).thenReturn(Date.from(Instant.parse("2026-09-01T00:00:00Z")));
    List<ChangeLogEntry> logs = new ArrayList<>();
    for (int i = 120; i > 1; i--) {
      logs.add(log(i, "edit_title", "", Instant.parse("2026-10-02T00:00:00Z").plusSeconds(i).toString()));
    }
    logs.add(log(1, "edit_status", "Done", "2026-10-01T10:00:00Z"));
    when(taskStorage.getTaskLogs(TASK_ID, 0, -1)).thenReturn(logs);

    TaskChanges withLog = taskService.getTaskChanges(task, SINCE, true);
    TaskChanges withoutLog = taskService.getTaskChanges(task, SINCE, false);

    assertEquals(1, withLog.getChanges().size());
    assertEquals("Done", withLog.getChanges().get(0).getNewValue());
    assertEquals(100, withLog.getChangeLog().size());
    assertEquals(120L, withLog.getChangeLog().get(0).getId());
    assertNull(withoutLog.getChangeLog());
    verify(taskStorage, org.mockito.Mockito.times(2)).getTaskLogs(TASK_ID, 0, -1);
  }

  /**
   * The date of the changes is required: a null one is refused as a bad
   * argument, before the change log is read. Mutant: the check dropped,
   * which fails with a null pointer instead.
   */
  @Test
  public void theChangesNeedADate() {
    TaskDto task = mock(TaskDto.class);

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> taskService.getTaskChanges(task, null, false));

    assertEquals("task.changes.noDate", error.getMessage());
    verifyNoInteractions(taskStorage);
  }

  /**
   * The changes of a task are its status, project, assignee, due date and
   * completion entries at or after the date, oldest first, whatever order
   * the log comes in. A change's old value is the new value of the previous
   * change of the same field, also when that one is before the date; a
   * project move forgets the status. Only the latest due date change
   * carries the due date. Completion entries alternate back from the current
   * completion. An entry exactly at the date counts; a title change is no
   * change. Mutants: the old value taken from the listed changes only; the
   * date excluded; the completion read forward from the first entry; every
   * due date change carrying the current due date; a project move keeping
   * the status.
   */
  @Test
  public void theChangesTellTheOldAndNewValueOfEachChangeSinceTheDate() {
    TaskDto task = mock(TaskDto.class);
    when(task.isCompleted()).thenReturn(false);
    Date dueDate = Date.from(Instant.parse("2026-10-20T00:00:00Z"));
    when(task.getDueDate()).thenReturn(dueDate);
    when(task.getCreatedTime()).thenReturn(Date.from(Instant.parse("2026-09-20T08:00:00Z")));
    List<ChangeLogEntry> logs = List.of(log(11, "mark_done", "", "2026-10-07T12:00:00Z"),
                                        log(10, "edit_title", "", "2026-10-07T11:00:00Z"),
                                        log(9, "edit_duedate", "", "2026-10-07T10:00:00Z"),
                                        log(8, "mark_done", "", "2026-10-06T12:00:00Z"),
                                        log(7, "edit_duedate", "", "2026-10-06T10:00:00Z"),
                                        log(6, "unassign", "mary", "2026-10-05T12:00:00Z"),
                                        log(5, "assign", "mary", "2026-10-05T10:00:00Z"),
                                        log(4, "edit_status", "Done", "2026-10-04T12:00:00Z"),
                                        log(3, "edit_project", "Beta", "2026-10-04T10:00:00Z"),
                                        log(2, "edit_status", "InProgress", "2026-10-01T00:00:00Z"),
                                        log(1, "assign", OTHER_USER, "2026-09-20T10:00:00Z"),
                                        log(0, "edit_status", "ToDo", "2026-09-20T09:00:00Z"));

    List<TaskFieldChange> changes = taskService.getTaskChanges(task, logs, SINCE);

    assertEquals(List.of("status:ToDo>InProgress@2026-10-01T00:00:00Z",
                         "project:null>Beta@2026-10-04T10:00:00Z",
                         "status:null>Done@2026-10-04T12:00:00Z",
                         "assignee:john>mary@2026-10-05T10:00:00Z",
                         "assignee:mary>null@2026-10-05T12:00:00Z",
                         "due_date:null>null@2026-10-06T10:00:00Z",
                         "completed:false>true@2026-10-06T12:00:00Z",
                         "due_date:null>2026-10-20T00:00:00Z@2026-10-07T10:00:00Z",
                         "completed:true>false@2026-10-07T12:00:00Z"),
                 changes.stream().map(TaskServiceChangesTest::render).toList());
    assertTrue(changes.stream().allMatch(c -> USER.equals(c.getAuthor())));
  }

  /**
   * The latest completion entry carries the current completion, also when
   * the entries read hold an even count for a completed task: the earlier
   * entry is then a reopening. Mutant: the completion read forward from the
   * first entry.
   */
  @Test
  public void theLatestCompletionEntryCarriesTheCurrentCompletion() {
    TaskDto task = mock(TaskDto.class);
    when(task.isCompleted()).thenReturn(true);

    List<TaskFieldChange> changes = taskService.getTaskChanges(task,
                                                               List.of(log(2, "mark_done", "", "2026-10-07T10:00:00Z"),
                                                                       log(1, "mark_done", "", "2026-10-06T10:00:00Z")),
                                                               SINCE);

    assertEquals(List.of("false", "true"), changes.stream().map(TaskFieldChange::getNewValue).toList());
  }

  /**
   * A task created since the date has no assignee change: its creation logs
   * its assignment, which is no change, and the task query leaves it out as
   * well; its status changes are kept. Mutant: the creation date ignored.
   */
  @Test
  public void aTaskCreatedSinceTheDateHasNoAssigneeChange() {
    TaskDto task = mock(TaskDto.class);
    when(task.getCreatedTime()).thenReturn(Date.from(Instant.parse("2026-10-03T08:00:00Z")));

    List<TaskFieldChange> changes = taskService.getTaskChanges(task,
                                                               List.of(log(4, "edit_status", "Done", "2026-10-06T10:00:00Z"),
                                                                       log(3, "assign", "mary", "2026-10-04T10:00:00Z"),
                                                                       log(2, "edit_status", "ToDo", "2026-10-03T08:00:01Z"),
                                                                       log(1, "assign", OTHER_USER, "2026-10-03T08:00:00Z")),
                                                               SINCE);

    assertEquals(List.of("status:null>ToDo", "status:ToDo>Done"),
                 changes.stream().map(c -> c.getField() + ":" + c.getOldValue() + ">" + c.getNewValue()).toList());
  }

  /**
   * The creation entry records the status a task is created in: its first
   * move tells the status it left. A creation entry logged before, with no
   * target, and no creation entry at all, leave it unrecorded. Mutant: the
   * creation entry ignored.
   */
  @Test
  public void aFirstMoveTellsTheStatusTheTaskWasCreatedIn() {
    TaskDto task = mock(TaskDto.class);
    ChangeLogEntry move = log(2, "edit_status", "InProgress", "2026-10-06T10:00:00Z");

    List<TaskFieldChange> recorded = taskService.getTaskChanges(task, List.of(move, log(1, "created", "ToDo", "2026-09-20T10:00:00Z")), SINCE);
    List<TaskFieldChange> older = taskService.getTaskChanges(task, List.of(move, log(1, "created", "", "2026-09-20T10:00:00Z")), SINCE);
    List<TaskFieldChange> noCreation = taskService.getTaskChanges(task, List.of(move), SINCE);

    assertEquals("ToDo", recorded.get(0).getOldValue());
    assertEquals("InProgress", recorded.get(0).getNewValue());
    assertEquals(1, older.size());
    assertNull(older.get(0).getOldValue());
    assertNull(noCreation.get(0).getOldValue());
    assertEquals("InProgress", noCreation.get(0).getNewValue());
  }

  /**
   * @param change a change
   * @return it, as field:old&gt;new@date
   */
  private static String render(TaskFieldChange change) {
    String newValue = TaskFieldChange.FIELD_DUE_DATE.equals(change.getField()) && change.getNewDate() != null ?
                                                                                                         change.getNewDate()
                                                                                                               .toInstant()
                                                                                                               .toString() :
                                                                                                         change.getNewValue();
    return change.getField() + ":" + change.getOldValue() + ">" + newValue + "@" + Instant.ofEpochMilli(change.getCreatedTime());
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
   * or after it on a task created before it.
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
    assertEquals(new LoggedSince(List.of("edit_status", "edit_project", "remove_project", "edit_duedate", "mark_done"), since),
                 ((SingleCondition<?>) changed.getConditions().get(0)).getValue());
    AggregateCondition assignee = (AggregateCondition) changed.getConditions().get(1);
    assertEquals(AggregateCondition.AND, assignee.getType());
    assertEquals(new LoggedSince(List.of("assign", "unassign"), since), ((SingleCondition<?>) assignee.getConditions().get(0)).getValue());
    assertTrue(isSingle(assignee.getConditions().get(1), SingleCondition.LT, "createdTime", since));
  }

  /**
   * @param query a task query
   * @return the value of its change log condition
   */
  private static LoggedSince findLoggedSince(TaskQuery query) {
    return all(query).stream()
                     .filter(c -> c instanceof SingleCondition<?> single && SingleCondition.LOGGED_SINCE.equals(single.getType()))
                     .map(c -> (LoggedSince) ((SingleCondition<?>) c).getValue())
                     .findFirst()
                     .orElseThrow();
  }

  /**
   * @param query a task query
   * @param completed true for the completed tasks condition, false for the
   *          open tasks one
   * @return whether the query holds it
   */
  private static boolean selectsCompleted(TaskQuery query, boolean completed) {
    String type = completed ? SingleCondition.IS_TRUE : SingleCondition.IS_FALSE;
    return all(query).stream()
                     .anyMatch(c -> c instanceof SingleCondition<?> single && type.equals(single.getType())
                                    && "completed".equals(single.getField()));
  }

  /**
   * @param query a task query
   * @return every condition of its tree
   */
  private static List<Condition> all(TaskQuery query) {
    List<Condition> conditions = new ArrayList<>();
    if (query.getCondition() != null) {
      conditions.add(query.getCondition());
    }
    for (int i = 0; i < conditions.size(); i++) {
      if (conditions.get(i) instanceof AggregateCondition aggregate) {
        conditions.addAll(aggregate.getConditions());
      }
    }
    return conditions;
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

}
