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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.dao.condition.Conditions;
import org.exoplatform.task.dao.condition.PredicateCondition;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.domain.*;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.service.UserService;
import org.exoplatform.task.service.impl.TaskServiceImpl;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.storage.TaskStorage;
import org.exoplatform.task.storage.impl.TaskStorageImpl;
import io.meeds.task.search.TaskSearchConnector;
import org.exoplatform.task.util.ListUtil;
import org.exoplatform.task.util.TaskUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.*;


import java.util.*;

public class TestTaskDAO extends AbstractTest {

  private TaskHandler         tDAO;

  private CommentHandler      cDAO;

  private DAOHandler          daoHandler;

  private final String        username = "root";

  private LabelHandler        labelHandler;

  private ListenerService     listenerService;

  private ProjectStorage      projectStorage;

  private UserService         userService;

  private TaskStorage         taskStorage;

  private TaskService         taskService;

  private TaskSearchConnector taskSearchConnector;

  @Before
  public void setup() {
    PortalContainer container = PortalContainer.getInstance();

    daoHandler = container.getComponentInstanceOfType(DAOHandler.class);
    tDAO = daoHandler.getTaskHandler();
    cDAO = daoHandler.getCommentHandler();
    labelHandler = daoHandler.getLabelHandler();
    taskSearchConnector = container.getComponentInstanceOfType(TaskSearchConnector.class);
    projectStorage = container.getComponentInstanceOfType(ProjectStorage.class);
    userService = container.getComponentInstanceOfType(UserService.class);
    taskStorage = new TaskStorageImpl(daoHandler, userService, projectStorage, taskSearchConnector);
    taskService = new TaskServiceImpl(taskStorage, daoHandler, listenerService);
  }

  @After
  public void tearDown() {
    deleteAll();
  }

  @Test
  public void testTaskCreation() {
    Task task = new Task();
    task.setTitle("Testing task creation");
    tDAO.create(task);

    List<Task> list = tDAO.findAll();
    Assert.assertEquals(1, list.size());

    //
    task = new Task();
    task.setTitle("There is an important meeting tomorrow !high");
    tDAO.create(task);
    list = tDAO.findAll();
    Assert.assertEquals(2, list.size());

    //
    task = tDAO.find(task.getId());
    Assert.assertNotNull(task);
    Assert.assertEquals("There is an important meeting tomorrow !high", task.getTitle());
    Assert.assertEquals(Priority.NORMAL, task.getPriority());
  }

  @Test
  public void testFindTaskByQuery() throws Exception {
    Task task = newTaskInstance("Test find task by query", "description of find task by query", "root");
    tDAO.create(task);

    TaskQuery query = new TaskQuery();
    query.setTitle("task");
    ListAccess<Task> tasks = tDAO.findTasks(query);
    Assert.assertTrue(tasks.getSize() > 0);

    query = new TaskQuery();
    query.setTitle("testFindTaskByQuery0123456789");
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(0, tasks.getSize());
    Assert.assertEquals(0, ListUtil.load(tasks, 0, -1).length);

    query = new TaskQuery();
    query.setDescription("description of find task by query");
    tasks = tDAO.findTasks(query);
    Assert.assertTrue(tasks.getSize() > 0);
    Assert.assertTrue(ListUtil.load(tasks, 0, -1).length > 0);

    query = new TaskQuery();
    query.setDescription("testFindTaskByQuery0123456789");
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(0, tasks.getSize());
    Assert.assertEquals(0, ListUtil.load(tasks, 0, -1).length);

    query = new TaskQuery();
    query.setAssignee(Arrays.asList("root"));
    tasks = tDAO.findTasks(query);
    Assert.assertTrue(tasks.getSize() > 0);
    Assert.assertTrue(ListUtil.load(tasks, 0, -1).length > 0);

    query = new TaskQuery();
    query.setAssignee(Arrays.asList("testFindTaskByQuery0123456789"));
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(0, tasks.getSize());
    Assert.assertEquals(0, ListUtil.load(tasks, 0, -1).length);

    query = new TaskQuery();
    query.setKeyword("find task by query");
    tasks = tDAO.findTasks(query);
    Assert.assertTrue(tasks.getSize() > 0);
    Assert.assertTrue(ListUtil.load(tasks, 0, -1).length > 0);


    query = new TaskQuery();
    query.setKeyword("testFindTaskByQuery0123456789");
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(0, tasks.getSize());
    Assert.assertEquals(0, ListUtil.load(tasks, 0, -1).length);
    
    query = new TaskQuery();
    query.setKeyword(" Find  QUERY");
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());
    Assert.assertEquals(1, ListUtil.load(tasks, 0, -1).length);
  }  

  /**
   * The title match the MCP task list builds: {@code TaskQuery#setTitle}
   * compares the lower-cased task title with the value as given, so a
   * lower-cased value matches whatever the title's case, a value with upper
   * case matches nothing, and, beside a project condition, it only selects
   * the tasks of that project.
   */
  @Test
  public void testFindTaskByTitleWithinAProject() throws Exception {
    Project alpha = new Project();
    alpha.setName("Alpha");
    alpha = daoHandler.getProjectHandler().create(alpha);
    Project beta = new Project();
    beta.setName("Beta");
    beta = daoHandler.getProjectHandler().create(beta);
    Status alphaStatus = newStatusInstance("To do", 1);
    alphaStatus.setProject(alpha);
    daoHandler.getStatusHandler().create(alphaStatus);
    Status betaStatus = newStatusInstance("To do", 1);
    betaStatus.setProject(beta);
    daoHandler.getStatusHandler().create(betaStatus);
    Task inAlpha = newTaskInstance("Live check 2", "", null);
    inAlpha.setStatus(alphaStatus);
    tDAO.create(inAlpha);
    Task otherInAlpha = newTaskInstance("Release notes", "", null);
    otherInAlpha.setStatus(alphaStatus);
    tDAO.create(otherInAlpha);
    Task inBeta = newTaskInstance("Live check 2", "", null);
    inBeta.setStatus(betaStatus);
    tDAO.create(inBeta);

    TaskQuery query = new TaskQuery();
    query.setProjectIds(Arrays.asList(alpha.getId()));
    query.setTitle("live check");
    Task[] found = ListUtil.load(tDAO.findTasks(query), 0, -1);
    assertEquals(1, found.length);
    assertEquals(inAlpha.getId(), found[0].getId());

    query = new TaskQuery();
    query.setProjectIds(Arrays.asList(alpha.getId()));
    query.setTitle("Live check");
    assertEquals(0, tDAO.findTasks(query).getSize());
  }

  /**
   * A {@link PredicateCondition} restricts the page and the count of a task
   * query alike, combined with the project restriction: a correlated EXISTS
   * subquery keeps the commented tasks of the project, a NOT EXISTS one its
   * uncommented tasks, through the DAO and through the task service. Without
   * the {@link PredicateCondition} branch of {@code CommonJPADAO#buildQuery},
   * a query whose only condition is the predicate silently returns every task,
   * which the first assertions catch; beside another condition the query fails
   * to build. A builder that returns no predicate is refused, for the page and
   * the count, rather than run the query unfiltered.
   */
  @Test
  public void testFindTasksByPredicateCondition() throws Exception {
    Project alpha = new Project();
    alpha.setName("Alpha");
    alpha = daoHandler.getProjectHandler().create(alpha);
    Project beta = new Project();
    beta.setName("Beta");
    beta = daoHandler.getProjectHandler().create(beta);
    Status alphaStatus = newStatusInstance("To do", 1);
    alphaStatus.setProject(alpha);
    daoHandler.getStatusHandler().create(alphaStatus);
    Status betaStatus = newStatusInstance("To do", 1);
    betaStatus.setProject(beta);
    daoHandler.getStatusHandler().create(betaStatus);
    Task firstCommented = newTaskInstance("First commented", "", null);
    firstCommented.setStatus(alphaStatus);
    tDAO.create(firstCommented);
    Task uncommented = newTaskInstance("Uncommented", "", null);
    uncommented.setStatus(alphaStatus);
    tDAO.create(uncommented);
    Task secondCommented = newTaskInstance("Second commented", "", null);
    secondCommented.setStatus(alphaStatus);
    tDAO.create(secondCommented);
    Task commentedInBeta = newTaskInstance("Commented in beta", "", null);
    commentedInBeta.setStatus(betaStatus);
    tDAO.create(commentedInBeta);
    for (Task task : Arrays.asList(firstCommented, secondCommented, commentedInBeta)) {
      Comment comment = new Comment();
      comment.setAuthor(username);
      comment.setComment("a comment");
      comment.setCreatedTime(new Date());
      comment.setTask(task);
      cDAO.create(comment);
    }

    TaskQuery onlyPredicateQuery = new TaskQuery();
    onlyPredicateQuery.add(new PredicateCondition<Task>((cb, query, root) -> cb.exists(commentsOf(cb, query, root))));
    ListAccess<Task> commentedEverywhere = tDAO.findTasks(onlyPredicateQuery);
    assertEquals(3, commentedEverywhere.getSize());
    assertEquals(3, ListUtil.load(commentedEverywhere, 0, -1).length);

    TaskQuery commentedQuery = new TaskQuery();
    commentedQuery.setProjectIds(Arrays.asList(alpha.getId()));
    commentedQuery.add(new PredicateCondition<Task>((cb, query, root) -> cb.exists(commentsOf(cb, query, root))));
    commentedQuery.setOrderBy(Arrays.asList(new OrderBy.DESC("id")));
    ListAccess<Task> commented = tDAO.findTasks(commentedQuery);
    assertEquals(2, commented.getSize());
    Task[] firstPage = ListUtil.load(commented, 0, 1);
    assertEquals(1, firstPage.length);
    assertEquals(secondCommented.getId(), firstPage[0].getId());
    Task[] secondPage = ListUtil.load(commented, 1, 1);
    assertEquals(1, secondPage.length);
    assertEquals(firstCommented.getId(), secondPage[0].getId());
    assertEquals(2, taskService.countTasks(commentedQuery));
    assertEquals(Arrays.asList(secondCommented.getId(), firstCommented.getId()),
                 taskService.findTasks(commentedQuery, 0, 10).stream().map(TaskDto::getId).toList());

    TaskQuery uncommentedQuery = new TaskQuery();
    uncommentedQuery.setProjectIds(Arrays.asList(alpha.getId()));
    uncommentedQuery.add(new PredicateCondition<Task>((cb, query, root) -> cb.not(cb.exists(commentsOf(cb, query, root)))));
    ListAccess<Task> uncommentedTasks = tDAO.findTasks(uncommentedQuery);
    assertEquals(1, uncommentedTasks.getSize());
    Task[] found = ListUtil.load(uncommentedTasks, 0, -1);
    assertEquals(1, found.length);
    assertEquals(uncommented.getId(), found[0].getId());
    assertEquals(1, taskService.countTasks(uncommentedQuery));

    TaskQuery nullPredicateQuery = new TaskQuery();
    nullPredicateQuery.add(new PredicateCondition<Task>((cb, query, root) -> null));
    assertThrows(IllegalStateException.class, () -> tDAO.findTasks(nullPredicateQuery));
    assertThrows(IllegalStateException.class, () -> taskService.countTasks(nullPredicateQuery));
  }

  @Test
  public void testFindTaskOrderByDueDate() throws Exception {
    Calendar calendar = Calendar.getInstance();

    Task task = newTaskInstance("task 1", "description of task 1", "root");
    task.setDueDate(calendar.getTime());
    tDAO.create(task);

    task = newTaskInstance("task 2", "description of task 2", "root");
    calendar.add(Calendar.DATE, 1);
    task.setDueDate(calendar.getTime());
    tDAO.create(task);

    task = newTaskInstance("task 3", "description of task 3", "root");
    calendar.add(Calendar.DATE, -2);
    task.setDueDate(calendar.getTime());
    tDAO.create(task);


    task = newTaskInstance("task 4", "description of task 4", "root");
    calendar.add(Calendar.DATE, 3);
    task.setDueDate(calendar.getTime());
    tDAO.create(task);


    task = newTaskInstance("task 5", "description of task 5", "root");
    tDAO.create(task);


    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setAssignee(Arrays.asList("root"));
    OrderBy order = new OrderBy.ASC(Conditions.TASK_DUEDATE);
    taskQuery.setOrderBy(Arrays.asList(order));

    ListAccess<Task> list = tDAO.findTasks(taskQuery);

    Assert.assertEquals(5, list.getSize());

    Task[] tasks = list.load(0, -1);
    Assert.assertEquals("task 3", tasks[0].getTitle());
    Assert.assertEquals("task 1", tasks[1].getTitle());
    Assert.assertEquals("task 2", tasks[2].getTitle());
    Assert.assertEquals("task 4", tasks[3].getTitle());
    Assert.assertEquals("task 5", tasks[4].getTitle());
  }

  @Test
  public void testFindLastUpdatedTasksOrdersByLastActivity() throws Exception {
    Calendar calendar = Calendar.getInstance();

    calendar.add(Calendar.MINUTE, -40);
    Task commentedTask = newTaskInstance("commented task", "task commented lately", "root");
    commentedTask.setCreatedTime(calendar.getTime());
    tDAO.create(commentedTask);

    calendar = Calendar.getInstance();
    // older than the untouched task on purpose: only its change log can lift it above
    calendar.add(Calendar.MINUTE, -35);
    Task loggedTask = newTaskInstance("logged task", "task changed lately", "root");
    loggedTask.setCreatedTime(calendar.getTime());
    tDAO.create(loggedTask);

    calendar = Calendar.getInstance();
    calendar.add(Calendar.MINUTE, -30);
    Task untouchedTask = newTaskInstance("untouched task", "task with no comment nor change log", "root");
    untouchedTask.setCreatedTime(calendar.getTime());
    tDAO.create(untouchedTask);

    calendar = Calendar.getInstance();
    calendar.add(Calendar.MINUTE, -5);
    Comment comment = new Comment();
    comment.setAuthor(username);
    comment.setComment("a comment made 5 minutes ago");
    comment.setCreatedTime(calendar.getTime());
    comment.setTask(commentedTask);
    cDAO.create(comment);

    calendar = Calendar.getInstance();
    calendar.add(Calendar.MINUTE, -10);
    ChangeLog changeLog = new ChangeLog();
    changeLog.setTask(loggedTask);
    changeLog.setAuthor(username);
    changeLog.setActionName("edit");
    changeLog.setTarget("title");
    changeLog.setCreatedTime(calendar.getTime());
    daoHandler.getTaskLogHandler().create(changeLog);

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setAssignee(Arrays.asList("root"));

    ListAccess<Task> list = tDAO.findLastUpdatedTasks(taskQuery);

    Assert.assertEquals(3, list.getSize());

    Task[] tasks = list.load(0, -1);
    Assert.assertEquals("commented task", tasks[0].getTitle());
    Assert.assertEquals("logged task", tasks[1].getTitle());
    Assert.assertEquals("untouched task", tasks[2].getTitle());
  }

  /**
   * The overdue query (EXO-91122) runs through the engine: open tasks the
   * user is assigned to or a coworker of, due strictly before the start of
   * today in their time zone. A task due at that instant, a completed task,
   * a task with no due date and another user's task are left out. Mutant:
   * lte instead of lt.
   *
   * @throws Exception never
   */
  @Test
  public void testFindOverdueTasksBeforeTheStartOfTheUsersDay() throws Exception {
    ZoneId zone = ZoneId.of("Europe/Paris");
    ZonedDateTime startOfToday = LocalDate.now(zone).atStartOfDay(zone);
    Task dueYesterday = newTaskInstance("due yesterday", "", username);
    dueYesterday.setDueDate(Date.from(startOfToday.minusDays(1).toInstant()));
    tDAO.create(dueYesterday);
    Task dueToday = newTaskInstance("due today", "", username);
    dueToday.setDueDate(Date.from(startOfToday.toInstant()));
    tDAO.create(dueToday);
    Task doneLate = newTaskInstance("done late", "", username);
    doneLate.setDueDate(Date.from(startOfToday.minusDays(3).toInstant()));
    doneLate.setCompleted(true);
    tDAO.create(doneLate);
    Task noDueDate = newTaskInstance("no due date", "", username);
    tDAO.create(noDueDate);
    Task coworkerLate = newTaskInstance("coworker late", "", "john");
    coworkerLate.setCoworker(new HashSet<>(Arrays.asList(username)));
    coworkerLate.setDueDate(Date.from(startOfToday.minusSeconds(1).toInstant()));
    tDAO.create(coworkerLate);
    Task someoneElsesLate = newTaskInstance("someone else's late", "", "john");
    someoneElsesLate.setDueDate(Date.from(startOfToday.minusDays(2).toInstant()));
    tDAO.create(someoneElsesLate);

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setAssigneeOrCoworker(Arrays.asList(username));
    taskQuery.setOverdueAt(Date.from(startOfToday.toInstant()));
    ListAccess<Task> list = tDAO.findLastUpdatedTasks(taskQuery);

    Assert.assertEquals(2, list.getSize());
    Set<String> titles = Arrays.stream(list.load(0, -1)).map(Task::getTitle).collect(Collectors.toSet());
    Assert.assertEquals(new HashSet<>(Arrays.asList("due yesterday", "coworker late")), titles);
    Assert.assertEquals(2, tDAO.findTasks(taskQuery).getSize());
  }

  /**
   * The overdue listing is ordered by due date ascending, ties on the
   * identifier, and a page keeps the most overdue tasks: of sixteen overdue
   * tasks (two due the same day), created from the least overdue to the most
   * overdue so that their identifiers run against their due dates, the first
   * page of ten is the ten oldest due dates, and the next page the six
   * others, the tie in identifier order. Mutants: descending due dates; no
   * order, or the identifier or creation order instead of the due date.
   *
   * @throws Exception never
   */
  @Test
  public void testFindOverdueTasksMostOverdueFirstAcrossPages() throws Exception {
    ZoneId zone = ZoneId.of("Europe/Paris");
    ZonedDateTime startOfToday = LocalDate.now(zone).atStartOfDay(zone);
    // created first, so its identifier is the lowest: it comes before its twin
    Task tie = newTaskInstance("late 1 bis", "", username);
    tie.setDueDate(Date.from(startOfToday.minusDays(1).toInstant()));
    tDAO.create(tie);
    for (int days = 1; days <= 15; days++) {
      // the least overdue task is created first: the identifier order is the opposite of the due date order
      Task task = newTaskInstance("late " + days, "", username);
      task.setDueDate(Date.from(startOfToday.minusDays(days).toInstant()));
      tDAO.create(task);
    }

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setAssigneeOrCoworker(Arrays.asList(username));
    taskQuery.setOverdueAt(Date.from(startOfToday.toInstant()));
    taskQuery.setOrderBy(Arrays.asList(new OrderBy.ASC("dueDate"), new OrderBy.ASC("id")));
    ListAccess<Task> list = tDAO.findTasks(taskQuery);

    Assert.assertEquals(16, list.getSize());
    List<String> firstPage = Arrays.stream(list.load(0, 10)).map(Task::getTitle).toList();
    Assert.assertEquals(Arrays.asList("late 15", "late 14", "late 13", "late 12", "late 11", "late 10", "late 9", "late 8",
                                      "late 7", "late 6"),
                        firstPage);
    List<String> secondPage = Arrays.stream(list.load(10, 10)).map(Task::getTitle).toList();
    Assert.assertEquals(Arrays.asList("late 5", "late 4", "late 3", "late 2", "late 1 bis", "late 1"), secondPage);
  }

  /**
   * changed_since selects the tasks with a status, due date, assignee or
   * completion entry in their change log at or after the date, and only
   * when one entry meets both: a task whose status changed before the date
   * and whose title changed after it is left out. An entry exactly at the
   * date counts. An assignee change counts on a task created before the
   * date, not on a task created since, whose creation logs its assignment.
   * The count runs the same subquery, and the listing the tool reads, by
   * last activity, loads the same tasks within a project. Mutants: the date
   * compared on another entry than the action (two joins), the date
   * excluded or the comparison inverted, the action list ignored, the
   * creation date ignored or inverted for an assignee change.
   *
   * @throws Exception never
   */
  @Test
  public void testFindTasksChangedSince() throws Exception {
    Date since = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
    Task moved = createTaskWithLog("moved", "edit_status", new Date());
    Task rescheduled = createTaskWithLog("rescheduled", "edit_duedate", since);
    Task completed = createTaskWithLog("completed", "mark_done", new Date());
    Task reassigned = newTaskInstance("reassigned", "", username);
    reassigned.setCreatedTime(Date.from(since.toInstant().minus(10, ChronoUnit.DAYS)));
    tDAO.create(reassigned);
    addLog(reassigned, "unassign", new Date());
    createTaskWithLog("created with an assignee", "assign", new Date());
    Task movedLongAgo = createTaskWithLog("moved long ago", "edit_status", Date.from(since.toInstant().minusSeconds(1)));
    addLog(movedLongAgo, "edit_title", new Date());
    createTaskWithLog("renamed", "edit_title", new Date());
    tDAO.create(newTaskInstance("untouched", "", username));

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setAssignee(Arrays.asList(username));
    taskQuery.setChangedSince(since);
    ListAccess<Task> list = tDAO.findTasks(taskQuery);

    Set<String> titles = Arrays.stream(list.load(0, -1)).map(Task::getTitle).collect(Collectors.toSet());
    Assert.assertEquals(new HashSet<>(Arrays.asList(moved.getTitle(),
                                                    rescheduled.getTitle(),
                                                    completed.getTitle(),
                                                    reassigned.getTitle())),
                        titles);
    Assert.assertEquals(4, list.getSize());
    Assert.assertEquals(4, tDAO.findLastUpdatedTasks(taskQuery).getSize());
    Set<String> lastUpdated = Arrays.stream(tDAO.findLastUpdatedTasks(taskQuery).load(0, -1))
                                    .map(Task::getTitle)
                                    .collect(Collectors.toSet());
    Assert.assertEquals(titles, lastUpdated);
  }

  /**
   * The acceptance call: changed_since on a project's tasks, through the
   * listing by last activity the tool reads, loads the project's tasks that
   * moved and not those of another project, nor the project's task that did
   * not move. Mutant: the change condition dropped.
   *
   * @throws Exception never
   */
  @Test
  public void testFindLastUpdatedTasksOfAProjectChangedSince() throws Exception {
    Date since = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
    Project project = new Project();
    project.setName("Moving");
    project = daoHandler.getProjectHandler().create(project);
    Project other = new Project();
    other.setName("Other");
    other = daoHandler.getProjectHandler().create(other);
    Status todo = newStatusInstance("To do", 1);
    todo.setProject(project);
    daoHandler.getStatusHandler().create(todo);
    Status otherTodo = newStatusInstance("To do", 1);
    otherTodo.setProject(other);
    daoHandler.getStatusHandler().create(otherTodo);
    for (String title : Arrays.asList("moved", "completed", "still")) {
      Task task = newTaskInstance(title, "", null);
      task.setStatus(todo);
      tDAO.create(task);
      if ("moved".equals(title)) {
        addLog(task, "edit_status", new Date());
      } else if ("completed".equals(title)) {
        addLog(task, "mark_done", new Date());
      }
    }
    Task elsewhere = newTaskInstance("moved elsewhere", "", null);
    elsewhere.setStatus(otherTodo);
    tDAO.create(elsewhere);
    addLog(elsewhere, "edit_status", new Date());

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setProjectIds(Arrays.asList(project.getId()));
    taskQuery.setChangedSince(since);
    Task[] found = tDAO.findLastUpdatedTasks(taskQuery).load(0, -1);

    Assert.assertEquals(new HashSet<>(Arrays.asList("moved", "completed")),
                        Arrays.stream(found).map(Task::getTitle).collect(Collectors.toSet()));
    Assert.assertEquals(2, found.length);
  }

  /**
   * completed_since selects the completed tasks whose completion entry is at
   * or after the date: not a task completed before it, nor a task reopened
   * after it, whose entry has the same action. Mutants: the completed
   * condition dropped; the date ignored.
   *
   * @throws Exception never
   */
  @Test
  public void testFindTasksCompletedSince() throws Exception {
    Date since = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
    Task completedThisWeek = newTaskInstance("completed this week", "", username);
    completedThisWeek.setCompleted(true);
    tDAO.create(completedThisWeek);
    addLog(completedThisWeek, "mark_done", new Date());
    Task completedLongAgo = newTaskInstance("completed long ago", "", username);
    completedLongAgo.setCompleted(true);
    tDAO.create(completedLongAgo);
    addLog(completedLongAgo, "mark_done", Date.from(since.toInstant().minus(3, ChronoUnit.DAYS)));
    addLog(completedLongAgo, "edit_status", new Date());
    createTaskWithLog("reopened this week", "mark_done", new Date());

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setAssignee(Arrays.asList(username));
    taskQuery.setCompletedSince(since);
    ListAccess<Task> list = tDAO.findTasks(taskQuery);

    Assert.assertEquals(1, list.getSize());
    Assert.assertEquals("completed this week", list.load(0, -1)[0].getTitle());
  }

  /**
   * The status filter selects the tasks of that column only, among the
   * tasks of the project. Mutant: the status condition dropped.
   *
   * @throws Exception never
   */
  @Test
  public void testFindTasksByStatusId() throws Exception {
    Project project = new Project();
    project.setName("Board");
    project = daoHandler.getProjectHandler().create(project);
    Status todo = newStatusInstance("To do", 1);
    todo.setProject(project);
    daoHandler.getStatusHandler().create(todo);
    Status done = newStatusInstance("Done", 2);
    done.setProject(project);
    daoHandler.getStatusHandler().create(done);
    Task inTodo = newTaskInstance("in to do", "", null);
    inTodo.setStatus(todo);
    tDAO.create(inTodo);
    Task inDone = newTaskInstance("in done", "", null);
    inDone.setStatus(done);
    tDAO.create(inDone);

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setProjectIds(Arrays.asList(project.getId()));
    taskQuery.setStatusId(done.getId());
    Task[] found = ListUtil.load(tDAO.findTasks(taskQuery), 0, -1);

    Assert.assertEquals(1, found.length);
    Assert.assertEquals("in done", found[0].getTitle());
  }

  /**
   * @param title the task title
   * @param actionName the action of its change log entry
   * @param createdTime the date of the entry
   * @return the open task assigned to the test user, with that entry
   */
  private Task createTaskWithLog(String title, String actionName, Date createdTime) {
    Task task = newTaskInstance(title, "", username);
    tDAO.create(task);
    addLog(task, actionName, createdTime);
    return task;
  }

  /**
   * @param task a task
   * @param actionName the action of the change log entry added to it
   * @param createdTime the date of the entry
   */
  private void addLog(Task task, String actionName, Date createdTime) {
    ChangeLog log = new ChangeLog();
    log.setTask(task);
    log.setAuthor(username);
    log.setActionName(actionName);
    log.setCreatedTime(createdTime);
    daoHandler.getTaskLogHandler().create(log);
  }

  @Test
  public void testFindLastUpdatedTasksBreaksTiesOnId() throws Exception {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.MINUTE, -15);
    Date sharedCreationDate = calendar.getTime();

    // no comment, no change log: the three tasks share the very same last activity date
    Task firstTask = newTaskInstance("first task", "created first", "root");
    firstTask.setCreatedTime(sharedCreationDate);
    tDAO.create(firstTask);

    Task secondTask = newTaskInstance("second task", "created second", "root");
    secondTask.setCreatedTime(sharedCreationDate);
    tDAO.create(secondTask);

    Task thirdTask = newTaskInstance("third task", "created third", "root");
    thirdTask.setCreatedTime(sharedCreationDate);
    tDAO.create(thirdTask);

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setAssignee(Arrays.asList("root"));

    ListAccess<Task> list = tDAO.findLastUpdatedTasks(taskQuery);
    Assert.assertEquals(3, list.getSize());

    // ties are broken on the identifier, newest first
    Task[] tasks = list.load(0, -1);
    Assert.assertEquals("third task", tasks[0].getTitle());
    Assert.assertEquals("second task", tasks[1].getTitle());
    Assert.assertEquals("first task", tasks[2].getTitle());

    // and that order is stable across pages: no row repeated, none skipped
    List<String> paged = new ArrayList<>();
    for (int offset = 0; offset < 3; offset++) {
      Task[] page = list.load(offset, 1);
      Assert.assertEquals(1, page.length);
      paged.add(page[0].getTitle());
    }
    Assert.assertEquals(Arrays.asList("third task", "second task", "first task"), paged);
  }

  @Test
  public void testFindTaskByQueryAdvance() throws Exception {
    Project project = new Project();
    project.setName("Project1");
    project.setParticipator(new HashSet<String>(Arrays.asList("root")));
    project = daoHandler.getProjectHandler().create(project);

    Task task = newTaskInstance("testTask", "task with label", username);
    tDAO.create(task);

    TaskQuery query = new TaskQuery();
    query.setEmptyField("labels");
    ListAccess<Task> tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());
    
    //Find by label
    Label label = new Label("testLabel", username, project);
    labelHandler.create(label);
    LabelTaskMapping mapping = new LabelTaskMapping(label, task);
    daoHandler.getLabelTaskMappingHandler().create(mapping);
    
    //
    query = new TaskQuery();
    query.setLabelIds(Arrays.asList(label.getId()));
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());
    
    //Find by status

    Status status = newStatusInstance("TO DO", 1);
    status.setProject(project);
    status = daoHandler.getStatusHandler().create(status);
    task.setStatus(status);
    tDAO.update(task);
    //
    query = new TaskQuery();
    query.setStatus(status);
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());
    
    //Find by assigneeOrProjectId
    query = new TaskQuery();
    query.setAssigneeOrCoworkerOrInProject(username, Arrays.asList(-100L));
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());
    //
    query = new TaskQuery();
    query.setAssigneeOrCoworkerOrInProject("test", Arrays.asList(project.getId()));
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());
    
    //Find by duedate
    Date date = new Date();
    task.setDueDate(date);
    tDAO.update(task);
    //
    query = new TaskQuery();
    query.setDueDateFrom(date);
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());

    //Find by priority
    task.setPriority(Priority.HIGH);
    tDAO.update(task);
    //
    query = new TaskQuery();
    query.setPriority(Priority.HIGH);
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());
    
    //Find by assignee
    query = new TaskQuery();
    query.setAssignee(Arrays.asList(username));
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());

    //Find completed
    task.setCompleted(true);
    tDAO.update(task);
    //
    query = new TaskQuery();
    query.setCompleted(true);
    tasks = tDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());    
  }

  @Test
  public void testFindTaskByMembership() {
    Project project = new Project();
    project.setName("Project1");
    project.setParticipator(new HashSet<String>(Arrays.asList("root")));
    project = daoHandler.getProjectHandler().create(project);

    Status status = newStatusInstance("TO DO", 1);
    status.setProject(project);
    daoHandler.getStatusHandler().create(status);
    
    Task task1 = newTaskInstance("Task 1", "", username);
    task1.setStatus(status);
    tDAO.create(task1);
    
    TaskQuery query = new TaskQuery();
    query.setMemberships(Arrays.asList("root"));
    ListAccess<Task> listTasks = tDAO.findTasks(query);
    Assert.assertEquals(1, ListUtil.getSize(listTasks));
    
    //
    query = new TaskQuery();
    Identity user = new Identity(username);
    query.setAccessible(user);
    query.setKeyword("Task");
    listTasks = tDAO.findTasks(query);
    Assert.assertEquals(1, ListUtil.getSize(listTasks));
    
    //The only thing related to "root" of task2 is he is creator
    //This is for the case user create task in label then search by unified search
    Task task2 = newTaskInstance("task2", "", null);
    tDAO.create(task2);
    Label label = new Label("label1", username ,project);
    labelHandler.create(label);
    LabelTaskMapping mapping = new LabelTaskMapping(label, task2);
    daoHandler.getLabelTaskMappingHandler().create(mapping);
    //
    
    query = new TaskQuery();
    query.setAccessible(user);
    query.setKeyword("Task");
    listTasks = tDAO.findTasks(query);
    Assert.assertEquals(2, ListUtil.getSize(listTasks));
  }
  
  @Test
  public void testFindTasksByLabel() throws Exception {
    Project project = new Project();
    project.setName("Project1");
    daoHandler.getProjectHandler().create(project);

    Status status = newStatusInstance("TO DO", 1);
    status.setProject(project);
    daoHandler.getStatusHandler().create(status);
    
    Task task = newTaskInstance("task1", "", username);
    task.setStatus(status);
    tDAO.create(task);
    Label label = new Label("label1", username ,project);
    labelHandler.create(label);
    //
    LabelTaskMapping mapping = new LabelTaskMapping(label, task);
    daoHandler.getLabelTaskMappingHandler().create(mapping);

    ListAccess<Task> tasks = tDAO.findTasksByLabel(label.getId(), Arrays.asList(project.getId()), username, null);
    Assert.assertEquals(1, tasks.getSize());
    Assert.assertEquals(task.getId(), tasks.load(0, -1)[0].getId());   
    
    daoHandler.getLabelTaskMappingHandler().delete(mapping);
    tasks = tDAO.findTasksByLabel(label.getId(), Arrays.asList(project.getId()), username, null);
    Assert.assertEquals(0, tasks.getSize());
  }
  
  @Test
  public void testRemoveTaskLabel() throws Exception {
    Project project = new Project();
    project.setName("project1");
    daoHandler.getProjectHandler().create(project);

    Status status = new Status();
    status.setName("status1");
    status.setRank(1);
    status.setProject(project);
    daoHandler.getStatusHandler().create(status);
    Task task = newTaskInstance("task1", "", username);
    task.setStatus(status);
    tDAO.create(task);
    Label label = new Label("label1", username ,project);
    label.setProject(project);
    labelHandler.create(label);
    //
    LabelTaskMapping mapping = new LabelTaskMapping(label, task);
    daoHandler.getLabelTaskMappingHandler().create(mapping);
    
    ListAccess<Label> labels = labelHandler.findLabelsByTask(task.getId(), project.getId());
    Assert.assertEquals(1, labels.getSize());
    //
    endRequestLifecycle();
    initializeContainerAndStartRequestLifecycle();
    taskStorage.delete(taskStorage.getTaskById(task.getId()));
    labels = labelHandler.findLabelsByTask(task.getId(), project.getId());
    Assert.assertEquals(0, labels.getSize());
  }

  @Test
  public void testGetIncomingTask() {
    Project project = new Project();
    project.setName("Project1");

    project = daoHandler.getProjectHandler().create(project);

    Status status = newStatusInstance("TO DO", 1);
    status.setProject(project);
    daoHandler.getStatusHandler().create(status);

    Task task1 = newTaskInstance("Task 1", "", username);
    tDAO.create(task1);

    Task task2 = newTaskInstance("Task 2", "", username);
    task2.setStatus(status);
    tDAO.create(task2);

    TaskQuery query = new TaskQuery();
    //query.setIsIncoming(Boolean.TRUE);
    //query.setUsername(username);
    query.setIsIncomingOf(username);
    ListAccess<Task> listTasks = tDAO.findTasks(query); //tDAO.getIncomingTasks(username, null);
    List<Task> tasks = Arrays.asList(ListUtil.load(listTasks, 0, -1)); //tDAO.getIncomingTask(username, null);
    assertContain(tasks, task1.getId());
    assertNotContain(tasks, task2.getId());

  }

  @Test
  public void testGetTodoTask() {
    Project project = new Project();
    project.setName("Project1");
    daoHandler.getProjectHandler().create(project);

    Status status = newStatusInstance("TO DO", 1);
    status.setProject(project);
    daoHandler.getStatusHandler().create(status);

    Task task1 = newTaskInstance("Task 1", "", null);
    tDAO.create(task1);

    Task task2 = newTaskInstance("Task 2", "", null);
    task2.setStatus(status);
    tDAO.create(task2);

    Task task3 = newTaskInstance("Task 3", "", username);
    task3.setDueDate(new Date());
    tDAO.create(task3);

    Task task4 = newTaskInstance("Task 4", "", username);
    task4.setDueDate(new Date());
    task4.setStatus(status);
    tDAO.create(task4);

    Task task5 = newTaskInstance("Task 5", "", username);
    task5.setStatus(status);
    task5.setCompleted(true);
    tDAO.create(task5);

    TaskQuery query = new TaskQuery();
    //query.setIsTodo(Boolean.TRUE);
    //query.setUsername(username);
    query.setIsTodoOf(username);
    query.setCompleted(false);
    ListAccess<Task> listTasks = tDAO.findTasks(query); //tDAO.getToDoTasks(username, null, null, null, null);
    List<Task> tasks = Arrays.asList(ListUtil.load(listTasks, 0, -1)); //tDAO.getToDoTask(username, null, null, null, null);

    assertContain(tasks, task3.getId());
    assertContain(tasks, task4.getId());
    assertNotContain(tasks, task1.getId());
    assertNotContain(tasks, task2.getId());
    assertNotContain(tasks, task5.getId());
  }
  
  @Test
  public void testTaskLog() throws EntityNotFoundException {
    Task task = newTaskInstance("Task 1", "", null);
    tDAO.create(task);

    TaskLogHandler logHandler = daoHandler.getTaskLogHandler();

    ListAccess<ChangeLog> logs = logHandler.findTaskLogs(task.getId());

    Assert.assertEquals(0, ListUtil.getSize(logs));
    
    ChangeLog log = new ChangeLog();
    log.setTask(task);
    log.setAuthor("root");
    log.setActionName("has created task");

    logHandler.create(log);
    
    //
    logs = logHandler.findTaskLogs(task.getId());
    Assert.assertEquals(1,  ListUtil.getSize(logs));
  }
  
  /**
   * The change log reads page after page by the last entry read: newest
   * first, entries of the same date by highest id first, nothing read twice or
   * skipped across a page boundary that splits entries of the same date, and
   * an entry added between two pages neither repeats nor hides an older one.
   * Another task's entries never show.
   */
  @Test
  public void testFindTaskLogsBeforePagesByDateThenId() {
    Task task = newTaskInstance("logged", "", username);
    tDAO.create(task);
    Task other = newTaskInstance("other", "", username);
    tDAO.create(other);
    Date oldest = new Date(1_700_000_000_000L);
    Date tied = new Date(1_700_000_060_000L);
    Date newest = new Date(1_700_000_120_000L);
    ChangeLog first = addLogEntry(task, oldest);
    ChangeLog tiedLow = addLogEntry(task, tied);
    ChangeLog tiedMiddle = addLogEntry(task, tied);
    addLogEntry(other, tied);
    ChangeLog tiedHigh = addLogEntry(task, tied);
    ChangeLog last = addLogEntry(task, newest);
    TaskLogHandler logHandler = daoHandler.getTaskLogHandler();

    List<ChangeLog> page1 = logHandler.findTaskLogsBefore(task.getId(), null, null, 2);
    assertEquals(List.of(last.getId(), tiedHigh.getId()), ids(page1));

    addLogEntry(task, new Date(1_700_000_180_000L));
    List<ChangeLog> page2 = logHandler.findTaskLogsBefore(task.getId(), tiedHigh.getCreatedTime(), tiedHigh.getId(), 2);
    assertEquals(List.of(tiedMiddle.getId(), tiedLow.getId()), ids(page2));

    List<ChangeLog> page3 = logHandler.findTaskLogsBefore(task.getId(), tiedLow.getCreatedTime(), tiedLow.getId(), 2);
    assertEquals(List.of(first.getId()), ids(page3));

    assertEquals(List.of(), logHandler.findTaskLogsBefore(task.getId(), first.getCreatedTime(), first.getId(), 2));
  }

  /**
   * Entries an upgrade left without a date read after the dated ones, as if
   * dated 0, highest id first, page after page from a cursor dated 0, whether
   * a page boundary falls on a dated or an undated entry; the dated ones are
   * never hidden behind them.
   */
  @Test
  public void testFindTaskLogsBeforeReadsUndatedEntriesLast() {
    Task task = newTaskInstance("upgraded", "", username);
    tDAO.create(task);
    ChangeLog undatedLow = addLogEntry(task, null);
    ChangeLog older = addLogEntry(task, new Date(1_700_000_000_000L));
    ChangeLog undatedMiddle = addLogEntry(task, null);
    ChangeLog undatedHigh = addLogEntry(task, null);
    ChangeLog newer = addLogEntry(task, new Date(1_700_000_060_000L));
    TaskLogHandler logHandler = daoHandler.getTaskLogHandler();

    List<ChangeLog> page1 = logHandler.findTaskLogsBefore(task.getId(), null, null, 3);
    assertEquals(List.of(newer.getId(), older.getId(), undatedHigh.getId()), ids(page1));
    List<ChangeLog> page2 = logHandler.findTaskLogsBefore(task.getId(), new Date(0), undatedHigh.getId(), 3);
    assertEquals(List.of(undatedMiddle.getId(), undatedLow.getId()), ids(page2));

    List<ChangeLog> dated = logHandler.findTaskLogsBefore(task.getId(), null, null, 2);
    assertEquals(List.of(newer.getId(), older.getId()), ids(dated));
    List<ChangeLog> next = logHandler.findTaskLogsBefore(task.getId(), older.getCreatedTime(), older.getId(), 2);
    assertEquals(List.of(undatedHigh.getId(), undatedMiddle.getId()), ids(next));
  }

  /**
   * The service reads a page of the change log in milliseconds for whoever
   * may view the task, its assignee, a user mentioned in one of its comments,
   * a member or a manager of its project, or a coworker here, and refuses, in
   * this order, a task
   * that does not exist, a reader who may not view it, its creator alone
   * included as on the card, then a cursor sent half and a limit out of 1 to
   * its bound.
   */
  @Test
  public void testGetTaskLogsBeforeChecksItsCursorAndLimit() throws Exception {
    Task task = newTaskInstance("logged", "", username);
    tDAO.create(task);
    ChangeLog older = addLogEntry(task, new Date(1_700_000_000_000L));
    ChangeLog newer = addLogEntry(task, new Date(1_700_000_060_000L));
    Identity reader = new Identity(username);
    Identity stranger = new Identity("stranger");
    Task created = newTaskInstance("created only", "", null);
    tDAO.create(created);
    Comment mention = new Comment();
    mention.setAuthor(username);
    mention.setComment("a comment mentioning a reader");
    mention.setCreatedTime(new Date());
    mention.setTask(created);
    mention.getMentionedUsers().add("mentioned");
    cDAO.create(mention);

    List<ChangeLogEntry> page = taskService.getTaskLogsBefore(task.getId(), newer.getCreatedTime().getTime(), newer.getId(), 1, reader);
    assertEquals(1, page.size());
    assertEquals(older.getId(), page.get(0).getId());
    assertEquals(2, taskService.getTaskLogsBefore(task.getId(), null, null, TaskService.MAX_TASK_LOGS_PAGE, reader).size());

    assertThrows(EntityNotFoundException.class, () -> taskService.getTaskLogsBefore(task.getId() + 1000, null, null, 1, reader));
    IllegalAccessException denied = assertThrows(IllegalAccessException.class,
                                                 () -> taskService.getTaskLogsBefore(task.getId(), null, null, 1, stranger));
    assertEquals("task.logs.notAccessible", denied.getMessage());
    assertThrows(IllegalAccessException.class, () -> taskService.getTaskLogsBefore(task.getId(), 1L, null, 1, stranger));
    assertThrows(IllegalAccessException.class, () -> taskService.getTaskLogsBefore(task.getId(), null, null, 1, null));
    assertThrows(IllegalAccessException.class, () -> taskService.getTaskLogsBefore(created.getId(), null, null, 1, reader));
    assertEquals(List.of(), taskService.getTaskLogsBefore(created.getId(), null, null, 1, new Identity("mentioned")));

    Project board = new Project("board", "", null, new HashSet<>(Set.of("manager")), new HashSet<>(Set.of("member")));
    board = daoHandler.getProjectHandler().create(board);
    Status column = newStatusInstance("To do", 1);
    column.setProject(board);
    column = daoHandler.getStatusHandler().create(column);
    Task onBoard = newTaskInstance("on the board", "", null);
    onBoard.setStatus(column);
    onBoard.setCoworker(new HashSet<>(Set.of("coworker")));
    tDAO.create(onBoard);
    for (String viewer : List.of("member", "manager", "coworker")) {
      assertEquals(viewer, List.of(), taskService.getTaskLogsBefore(onBoard.getId(), null, null, 1, new Identity(viewer)));
    }
    assertThrows(IllegalAccessException.class, () -> taskService.getTaskLogsBefore(onBoard.getId(), null, null, 1, stranger));
    ConversationState signedIn = ConversationState.getCurrent();
    try {
      ConversationState.setCurrent(new ConversationState(reader));
      assertThrows(IllegalAccessException.class, () -> taskService.getTaskLogsBefore(task.getId(), null, null, 1, stranger));
      ConversationState.setCurrent(new ConversationState(stranger));
      assertEquals(2, taskService.getTaskLogsBefore(task.getId(), null, null, 2, reader).size());
    } finally {
      ConversationState.setCurrent(signedIn);
    }

    assertRefused("task.logs.invalidCursor", () -> taskService.getTaskLogsBefore(task.getId(), 1L, null, 1, reader));
    assertRefused("task.logs.invalidCursor", () -> taskService.getTaskLogsBefore(task.getId(), null, 1L, 1, reader));
    assertRefused("task.logs.invalidLimit", () -> taskService.getTaskLogsBefore(task.getId(), null, null, 0, reader));
    assertRefused("task.logs.invalidLimit",
                  () -> taskService.getTaskLogsBefore(task.getId(), null, null, TaskService.MAX_TASK_LOGS_PAGE + 1, reader));
  }

  /**
   * @param code the code the call must be refused with
   * @param call the call
   */
  private void assertRefused(String code, ThrowingRunnable call) {
    IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class, call);
    assertEquals(code, refusal.getMessage());
  }

  /**
   * @param task a task
   * @param createdTime the date of the entry
   * @return the change log entry added to the task at that date
   */
  private ChangeLog addLogEntry(Task task, Date createdTime) {
    ChangeLog log = new ChangeLog();
    log.setTask(task);
    log.setAuthor(username);
    log.setActionName("edit_title");
    log.setCreatedTime(createdTime);
    return daoHandler.getTaskLogHandler().create(log);
  }

  /**
   * @param logs change log entries
   * @return their ids, in order
   */
  private List<Long> ids(List<ChangeLog> logs) {
    return logs.stream().map(ChangeLog::getId).toList();
  }

  @Test
  public void testTaskField() throws Exception {
    Task task = newTaskInstance("Task 1", "", null);

    Set<String> coworkers = new HashSet<String>();
    coworkers.add("worker1");
    task.setCoworker(coworkers);
    //
    tDAO.create(task);

    Assert.assertEquals(1, tDAO.getCoworker(task.getId()).size());    
  }

  @Test
  public void testGetTaskWithCoworkers() throws Exception {
    Task task = newTaskInstance("Task 1", "", null);

    Set<String> coworkers = new HashSet<String>();
    coworkers.add("worker1");
    task.setCoworker(coworkers);
    tDAO.create(task);
    Assert.assertEquals(1, tDAO.getTaskWithCoworkers(task.getId()).getCoworker().size());

    coworkers.add("worker2");
    coworkers.add("worker3");
    task.setCoworker(coworkers);
    Assert.assertEquals(3, tDAO.getTaskWithCoworkers(task.getId()).getCoworker().size());

    coworkers.remove("worker3");
    task.setCoworker(coworkers);
    Assert.assertEquals(2, tDAO.getTaskWithCoworkers(task.getId()).getCoworker().size());
  }

  @Test
  public void testTaskPermission() throws Exception {
    Task task = newTaskInstance("Task 1", "", "worker");
    Identity worker = new Identity("worker");
    Identity worker1 = new Identity("worker1");
    Identity worker2 = new Identity("worker2");
    Identity worker3 = new Identity("worker3");

    Set<String> coworkers = new HashSet<String>();
    coworkers.add("worker1");
    task.setCoworker(coworkers);
    task.setCreatedBy("worker3");
    tDAO.create(task);
    //worker1 is a coworker, so he has permission
    ConversationState.setCurrent(new ConversationState(worker1));
    Assert.assertEquals(true, TaskUtil.hasEditPermission(taskService,task));

    //worker is the assignee, so he has permission
    ConversationState.setCurrent(new ConversationState(worker));
    Assert.assertEquals(true, TaskUtil.hasEditPermission(taskService,task));

    //worker2 is not the assignee, neither coworker nor the creator, so he has not permission
    ConversationState.setCurrent(new ConversationState(worker2));
    Assert.assertEquals(false, TaskUtil.hasEditPermission(taskService,task));

    //worker3 is the creator, so he has permission
    ConversationState.setCurrent(new ConversationState(worker3));
    Assert.assertEquals(true, TaskUtil.hasEditPermission(taskService,task));

    coworkers.add("worker2");
    task.setCoworker(coworkers);
    tDAO.update(task);

    //worker2 is now a coworker, so he has permission
    ConversationState.setCurrent(new ConversationState(worker2));
    Assert.assertEquals(true, TaskUtil.hasEditPermission(taskService, task));
  }

  @Test
  public void testSelectFieldInTask() throws Exception {
    Project project = new Project();
    project.setName("Project1");

    project = daoHandler.getProjectHandler().create(project);

    Status status = newStatusInstance("TO DO", 1);
    status.setProject(project);
    Status status1 = newStatusInstance("IN Progress", 2);
    status1.setProject(project);

    daoHandler.getStatusHandler().create(status);
    daoHandler.getStatusHandler().create(status1);

    Task task1 = newTaskInstance("Task 1", "", null);
    tDAO.create(task1);

    Task task2 = newTaskInstance("Task 2", "", null);
    task2.setStatus(status);
    tDAO.create(task2);

    Task task3 = newTaskInstance("Task 3", "", username);
    task3.setDueDate(new Date());
    tDAO.create(task3);

    Task task4 = newTaskInstance("Task 4", "", username);
    task4.setDueDate(new Date());
    task4.setStatus(status1);
    tDAO.create(task4);

    Task task5 = newTaskInstance("Task 4", "", username);
    task5.setStatus(status1);
    task5.setCompleted(true);
    tDAO.create(task5);

    TaskQuery taskQuery = new TaskQuery();
    taskQuery.setProjectIds(Arrays.asList(project.getId()));
    
    ListAccess<Task> tasks = tDAO.findTasks(taskQuery);

//    List<Status> statuses = tDAO.selectTaskField(taskQuery, "status");
//    Assert.assertEquals(2, statuses.size());
//
//    taskQuery = new TaskQuery();
//    taskQuery.setAssignee(username);
//
//    List<Project> projects = tDAO.selectTaskField(taskQuery, "status.project");
//    Assert.assertEquals(1, projects.size());
  }

  @Test
  public void testGetUncompletedTasks() {
    Task task1 = newTaskInstance("Task 1", "", username);
    task1.setCompleted(false);
    tDAO.create(task1);

    Task task2 = newTaskInstance("Task 2", "", username);
    task2.setCompleted(false);
    tDAO.create(task2);

    Task task3 = newTaskInstance("Task 3", "", null);
    Set<String> coworker = new HashSet<>();
    coworker.add(username);
    task3.setCoworker(coworker);
    task3.setCompleted(false);
    tDAO.create(task3);

    Task task4 = newTaskInstance("Task 4", "", username);
    task4.setCompleted(true);
    tDAO.create(task4);

    Task task5 = newTaskInstance("Task 5", "", username);
    task5.setCompleted(true);
    tDAO.create(task5);

    assertNotNull(tDAO.getUncompletedTasks(username, -1));
    // test countUncompletedTasks
    assertEquals("should be 3 uncompleted tasks", Long.valueOf(3L), tDAO.countUncompletedTasks(username));
  }

  @Test
  public void testGetOverdueTasks() {

    Calendar calendar = Calendar.getInstance();

    Task task1 = newTaskInstance("task 1", "description of task 1", username);
    calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, -5);
    task1.setDueDate(calendar.getTime());
    tDAO.create(task1);

    Task task2 = newTaskInstance("task 2", "description of task 2", username);
    calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, +2);
    task2.setDueDate(calendar.getTime());
    tDAO.create(task2);

    Task task5 = newTaskInstance("task 5", "description of task 5", null);
    Set<String> coworker = new HashSet<>();
    coworker.add(username);
    task5.setCoworker(coworker);
    calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, -2);
    task5.setDueDate(calendar.getTime());
    tDAO.create(task5);

    assertNotNull(tDAO.getOverdueTasks(username, -1));
    // test countOverdueTasks
    assertEquals("should be 2 overdue tasks", Long.valueOf(2L), tDAO.countOverdueTasks(username));
  }

  @Test
  public void testAddWatchersToTasks() throws Exception {
    // Test Add watchers to task
    String user="john";
    Task task = newTaskInstance("task ", "description of task ", username);
    tDAO.create(task);
    Set<String> watchers = new HashSet<>();
    tDAO.addWatcherToTask(user,task);
    watchers=tDAO.getWatchersOfTask(task);
    assertEquals(1, watchers.size());
    assertTrue(watchers.contains(user));
  }

  @Test
  public void testDeleteWatchersToTasks() throws Exception {
    // Test Delete watcher From task
    String user="john";
    Task task = newTaskInstance("task ", "description of task", username);
    tDAO.create(task);
    //Add watcher to task
    tDAO.addWatcherToTask(user,task);
    Set<String> watchers=tDAO.getWatchersOfTask(task);
    assertEquals(1, watchers.size());
    // Delete watcher from task
    tDAO.deleteWatcherOfTask(user,task);
    watchers=tDAO.getWatchersOfTask(task);
    assertEquals(0, watchers.size());
  }

  @Test
  public void testGetWatchersOfTask() throws Exception{
    // Test GetWatchers of task
    Task task = newTaskInstance("Task ", "", null);
    Set<String> watchers = new HashSet<String>();
    task = tDAO.create(task);
    tDAO.addWatcherToTask("john",task);
    //Add watcher to task
    tDAO.addWatcherToTask("marry",task);

    //Then Test getWatchersOfTask after adding a watcher
    Assert.assertEquals(2, tDAO.getWatchersOfTask(task).size());
    assertTrue(tDAO.getWatchersOfTask(task).contains("marry"));

    //Delete watcher from task
    tDAO.deleteWatcherOfTask("marry",task);
    tDAO.update(task);
    //Then Test getWatchersOfTask after deleting a watcher
    Assert.assertEquals(1, tDAO.getWatchersOfTask(task).size());
    assertFalse(tDAO.getWatchersOfTask(task).contains("marry"));
  }

  @Test
  public void testFindTasks() throws Exception {
    String user="john";
    List<String> memberships = Arrays.asList("manager:/spaces/space1", "member:/spaces/space1");
    Task task = newTaskInstance("What is Lorem Ipsum?", "Lorem Ipsum is simply dummy text of the printing", user);
    tDAO.create(task);
    ConversationState.setCurrent(new ConversationState(new Identity(user)));

    List<Task> tasks = tDAO.findTasks(user,memberships, 10);
    assertEquals(1, tasks.size());

    tasks = tDAO.findTasks(user,memberships, 10);
    assertEquals(1, tasks.size());

    tasks = tDAO.findTasks(user,memberships,10);
    assertEquals(1, tasks.size());

    tasks = tDAO.findTasks("root",memberships,10);
    assertEquals(1, tasks.size());

    tasks = tDAO.findTasks("mary",memberships,10);
    assertEquals(0, tasks.size());
  }

  @Test
  public void testCountTasks() throws Exception {
    String user="john";
    Task task = newTaskInstance("What is Lorem Ipsum?", "Lorem Ipsum is simply dummy text of the printing", user);
    tDAO.create(task);

    assertEquals(1, tDAO.countTasks(user));
    assertEquals(1, tDAO.countTasks(user));
    assertEquals(1, tDAO.countTasks(user));
    assertEquals(1, tDAO.countTasks("root"));
    assertEquals(0, tDAO.countTasks("mary"));
  }

  private Task newTaskInstance(String taskTitle, String description, String assignee) {
    Task task = new Task();
    task.setTitle(taskTitle);
    task.setDescription(description);
    task.setAssignee(assignee);
    task.setCreatedBy(username);
    task.setCreatedTime(new Date());
    return task;
  }

  /**
   * A column criterion matches the column of that name on every board, trimmed
   * and ignoring case: « lost » finds the « Lost » card of alpha and the
   * « LOST » card of beta, page and count alike. Excluded, it keeps every other
   * card, including those of gamma, a board with no such column. Removing the
   * lower-casing or the trimming loses beta's card; inverting the exclusion
   * swaps the two results.
   */
  @Test
  public void testFindTasksByStatusNamesAcrossBoards() throws Exception {
    Project alpha = newBoard("Alpha");
    Project beta = newBoard("Beta");
    Project gamma = newBoard("Gamma");
    Status alphaLost = newColumn(alpha, "Lost", 1);
    Status alphaOpen = newColumn(alpha, "Open", 2);
    Status betaLost = newColumn(beta, " LOST ", 1);
    Status betaWon = newColumn(beta, "Won", 2);
    Status gammaOpen = newColumn(gamma, "Open", 1);
    Task lostInAlpha = newCard("lost in alpha", alphaLost, null);
    Task openInAlpha = newCard("open in alpha", alphaOpen, null);
    Task lostInBeta = newCard("lost in beta", betaLost, null);
    Task wonInBeta = newCard("won in beta", betaWon, null);
    Task openInGamma = newCard("open in gamma", gammaOpen, null);
    List<Long> boards = Arrays.asList(alpha.getId(), beta.getId(), gamma.getId());

    TaskQuery lost = boardsQuery(boards);
    lost.setStatusNames(List.of("lost"), false);
    assertCards(lost, lostInBeta, lostInAlpha);

    TaskQuery lostOrWon = boardsQuery(boards);
    lostOrWon.setStatusNames(List.of(" won", "Lost"), false);
    assertCards(lostOrWon, wonInBeta, lostInBeta, lostInAlpha);

    TaskQuery notLost = boardsQuery(boards);
    notLost.setStatusNames(List.of("LOST"), true);
    assertCards(notLost, openInGamma, wonInBeta, openInAlpha);

    assertThrows(IllegalArgumentException.class, () -> new TaskQuery().setStatusNames(List.of(), false));
    assertThrows(IllegalArgumentException.class, () -> new TaskQuery().setStatusNames(List.of(" "), false));
  }

  /**
   * An assignee criterion selects the cards assigned to the given users, and,
   * when coworkers count, the cards they work on as coworkers: a card with two
   * matching coworkers comes out once, in the page and in the count, and a
   * card only someone else works on never comes out. An
   * unassigned card has no assignee, null or empty, and, when coworkers
   * count, no coworker either.
   */
  @Test
  public void testFindTasksByAssigneeAndCoworkers() throws Exception {
    Project alpha = newBoard("Alpha");
    Status todo = newColumn(alpha, "To do", 1);
    Task rootsWithTwoCoworkers = newCard("root's, john and mary help", todo, "root", "john", "mary");
    Task johns = newCard("john's", todo, "john");
    Task maryHelpsAlone = newCard("mary helps, no assignee", todo, null, "mary");
    Task nobodys = newCard("nobody's", todo, null);
    Task emptyAssignee = newCard("empty assignee", todo, "");
    Task paulHelpsAlone = newCard("paul helps, no assignee", todo, null, "paul");
    List<Long> boards = Arrays.asList(alpha.getId());

    TaskQuery roots = boardsQuery(boards);
    roots.setAssignedTo(List.of("root"), false);
    assertCards(roots, rootsWithTwoCoworkers);

    TaskQuery johnsOrMarys = boardsQuery(boards);
    johnsOrMarys.setAssignedTo(List.of("john", "mary"), false);
    assertCards(johnsOrMarys, johns);

    TaskQuery johnOrMaryWorksOn = boardsQuery(boards);
    johnOrMaryWorksOn.setAssignedTo(List.of("john", "mary"), true);
    assertCards(johnOrMaryWorksOn, maryHelpsAlone, johns, rootsWithTwoCoworkers);

    TaskQuery unassigned = boardsQuery(boards);
    unassigned.setUnassigned(false);
    assertCards(unassigned, paulHelpsAlone, emptyAssignee, nobodys, maryHelpsAlone);

    TaskQuery nobodyOn = boardsQuery(boards);
    nobodyOn.setUnassigned(true);
    assertCards(nobodyOn, emptyAssignee, nobodys);

    assertThrows(IllegalArgumentException.class, () -> new TaskQuery().setAssignedTo(List.of(), true));
  }

  /**
   * A date criterion compares the field it names, the start of the range
   * included and its end excluded; a card with no date never falls in a range,
   * and only the cards with no date match « is empty ». The start date is the
   * start date, never the legacy end date, and the legacy end date is refused
   * as a criterion.
   */
  @Test
  public void testFindTasksByDueAndStartDates() throws Exception {
    Project alpha = newBoard("Alpha");
    Status todo = newColumn(alpha, "To do", 1);
    Date day = Date.from(Instant.parse("2026-11-10T00:00:00Z"));
    Date nextDay = Date.from(Instant.parse("2026-11-11T00:00:00Z"));
    Task dueBefore = newCard("due before", todo, null);
    dueBefore.setDueDate(Date.from(Instant.parse("2026-11-09T23:59:59Z")));
    tDAO.update(dueBefore);
    Task dueThatDay = newCard("due that day", todo, null);
    dueThatDay.setDueDate(day);
    tDAO.update(dueThatDay);
    Task dueNextDay = newCard("due the next day", todo, null);
    dueNextDay.setDueDate(nextDay);
    tDAO.update(dueNextDay);
    Task startsThatDay = newCard("starts that day, ends later", todo, null);
    startsThatDay.setStartDate(day);
    startsThatDay.setEndDate(nextDay);
    tDAO.update(startsThatDay);
    Task endsThatDay = newCard("only ends that day", todo, null);
    endsThatDay.setEndDate(day);
    tDAO.update(endsThatDay);
    List<Long> boards = Arrays.asList(alpha.getId());

    TaskQuery dueOnTheDay = boardsQuery(boards);
    dueOnTheDay.setDateBetween(Conditions.TASK_DUEDATE, day, nextDay);
    assertCards(dueOnTheDay, dueThatDay);

    TaskQuery dueFromTheDay = boardsQuery(boards);
    dueFromTheDay.setDateBetween(Conditions.TASK_DUEDATE, day, null);
    assertCards(dueFromTheDay, dueNextDay, dueThatDay);

    TaskQuery dueBeforeTheDay = boardsQuery(boards);
    dueBeforeTheDay.setDateBetween(Conditions.TASK_DUEDATE, null, day);
    assertCards(dueBeforeTheDay, dueBefore);

    TaskQuery startsOnTheDay = boardsQuery(boards);
    startsOnTheDay.setDateBetween(Conditions.TASK_START_DATE, day, nextDay);
    assertCards(startsOnTheDay, startsThatDay);

    TaskQuery noDueDate = boardsQuery(boards);
    noDueDate.setNoDate(Conditions.TASK_DUEDATE);
    assertCards(noDueDate, endsThatDay, startsThatDay);

    TaskQuery noStartDate = boardsQuery(boards);
    noStartDate.setNoDate(Conditions.TASK_START_DATE);
    assertCards(noStartDate, endsThatDay, dueNextDay, dueThatDay, dueBefore);

    assertThrows(IllegalArgumentException.class, () -> new TaskQuery().setDateBetween(Conditions.TASK_END_DATE, day, nextDay));
    assertThrows(IllegalArgumentException.class, () -> new TaskQuery().setNoDate(Conditions.TASK_END_DATE));
    assertThrows(IllegalArgumentException.class, () -> new TaskQuery().setDateBetween(Conditions.TASK_DUEDATE, null, null));
  }

  /**
   * A label criterion matches a label by its name, trimmed and ignoring case,
   * among the labels of the card's own board and the viewer's personal
   * labels: another user's personal label and another board's label never
   * match. A card with two matching labels comes out once. « has » with
   * several names needs each of them; « has none of » keeps the cards with
   * none of the names.
   */
  @Test
  public void testFindTasksByLabelNames() throws Exception {
    Project alpha = newBoard("Alpha");
    Project beta = newBoard("Beta");
    Status alphaTodo = newColumn(alpha, "To do", 1);
    Status betaTodo = newColumn(beta, "To do", 1);
    Label alphaHot = labelHandler.create(new Label("Hot", "john", alpha));
    Label alphaCold = labelHandler.create(new Label("Cold", "john", alpha));
    Label betaHot = labelHandler.create(new Label(" hot ", "john", beta));
    Label johnsHot = labelHandler.create(new Label("Hot", "john", null));
    Label rootsHot = labelHandler.create(new Label("HOT", username, null));
    Task hotInAlpha = newLabelledCard("hot in alpha", alphaTodo, null, alphaHot);
    Task hotInBeta = newLabelledCard("hot in beta", betaTodo, null, betaHot);
    Task hotForJohnOnly = newLabelledCard("hot for john only", alphaTodo, null, johnsHot);
    Task hotForRoot = newLabelledCard("hot for root", alphaTodo, null, rootsHot);
    Task carriesBetasLabel = newLabelledCard("carries beta's label", alphaTodo, null, betaHot);
    Task hotAndCold = newLabelledCard("hot and cold", alphaTodo, null, alphaHot, alphaCold);
    Task unlabelled = newLabelledCard("unlabelled", betaTodo, null);
    List<Long> boards = Arrays.asList(alpha.getId(), beta.getId());

    TaskQuery hot = boardsQuery(boards);
    hot.setLabelNamesAll(List.of("hot"), username);
    assertCards(hot, hotAndCold, hotForRoot, hotInBeta, hotInAlpha);

    TaskQuery hotOrCold = boardsQuery(boards);
    hotOrCold.setLabelNamesAny(List.of("HOT", "cold "), username, false);
    assertCards(hotOrCold, hotAndCold, hotForRoot, hotInBeta, hotInAlpha);

    TaskQuery hotAndColdToo = boardsQuery(boards);
    hotAndColdToo.setLabelNamesAll(List.of("Hot", "Cold"), username);
    assertCards(hotAndColdToo, hotAndCold);

    TaskQuery notHot = boardsQuery(boards);
    notHot.setLabelNamesAny(List.of("hot"), username, true);
    assertCards(notHot, unlabelled, carriesBetasLabel, hotForJohnOnly);

    TaskQuery johnsView = boardsQuery(boards);
    johnsView.setLabelNamesAll(List.of("hot"), "john");
    assertCards(johnsView, hotAndCold, hotForJohnOnly, hotInBeta, hotInAlpha);

    assertThrows(IllegalArgumentException.class, () -> new TaskQuery().setLabelNamesAny(List.of("hot"), null, false));
    assertThrows(IllegalArgumentException.class, () -> new TaskQuery().setLabelNamesAll(List.of(), username));
  }

  /**
   * Asserts that a query returns exactly the given cards, in that order, page
   * and count alike, through the DAO and through the task service.
   *
   * @param query the query, ordered by id descending
   * @param expected the cards, newest first
   */
  private void assertCards(TaskQuery query, Task... expected) throws Exception {
    List<Long> expectedIds = Arrays.stream(expected).map(Task::getId).toList();
    ListAccess<Task> found = tDAO.findTasks(query);
    assertEquals(expectedIds.size(), found.getSize());
    assertEquals(expectedIds, Arrays.stream(ListUtil.load(found, 0, -1)).map(Task::getId).toList());
    assertEquals(expectedIds.size(), taskService.countTasks(query));
    assertEquals(expectedIds, taskService.findTasks(query, 0, 50).stream().map(TaskDto::getId).toList());
  }

  /**
   * Builds a query over the given boards, newest card first.
   *
   * @param boards the board ids
   * @return the query
   */
  private TaskQuery boardsQuery(List<Long> boards) {
    TaskQuery query = new TaskQuery();
    query.setProjectIds(boards);
    query.setOrderBy(Arrays.asList(new OrderBy.DESC("id")));
    return query;
  }

  /**
   * Creates a board.
   *
   * @param name its name
   * @return the stored board
   */
  private Project newBoard(String name) {
    Project project = new Project();
    project.setName(name);
    return daoHandler.getProjectHandler().create(project);
  }

  /**
   * Creates a column of a board.
   *
   * @param board the board
   * @param name the column name
   * @param rank the column rank
   * @return the stored column
   */
  private Status newColumn(Project board, String name, int rank) {
    Status status = newStatusInstance(name, rank);
    status.setProject(board);
    return daoHandler.getStatusHandler().create(status);
  }

  /**
   * Creates a card in a column, with its coworkers.
   *
   * @param title its title
   * @param column its column
   * @param assignee its assignee, null for none
   * @param coworkers its coworkers
   * @return the stored card
   */
  private Task newCard(String title, Status column, String assignee, String... coworkers) {
    Task task = newTaskInstance(title, "", assignee);
    task.setStatus(column);
    task.setCoworker(new HashSet<>(Arrays.asList(coworkers)));
    return tDAO.create(task);
  }

  /**
   * Creates a card in a column, carrying labels.
   *
   * @param title its title
   * @param column its column
   * @param assignee its assignee, null for none
   * @param labels its labels
   * @return the stored card
   */
  private Task newLabelledCard(String title, Status column, String assignee, Label... labels) {
    Task task = newCard(title, column, assignee, new String[0]);
    for (Label label : labels) {
      daoHandler.getLabelTaskMappingHandler().create(new LabelTaskMapping(label, task));
    }
    return task;
  }

  /**
   * Builds the subquery of the comments of the task at the root of a query.
   *
   * @param cb the criteria builder
   * @param query the query the subquery is correlated with
   * @param root the task root of that query
   * @return the subquery selecting the comments of that task
   */
  private Subquery<Long> commentsOf(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Task> root) {
    Subquery<Long> comments = query.subquery(Long.class);
    Root<Comment> comment = comments.from(Comment.class);
    comments.select(comment.get("id")).where(cb.equal(comment.get("task"), root));
    return comments;
  }

  private Status newStatusInstance(String name, int rank) {
    Status status = new Status();
    status.setName(name);
    status.setRank(rank);
    return status;
  }

  private void assertContain(List<Task> tasks, Long taskId) {
    for(Task t : tasks) {
      if(t.getId() == taskId) {
        return;
      }
    }
    Assert.fail("Task with ID " + taskId  + " should exist on the list");
  }

  private void assertNotContain(List<Task> tasks, Long taskId) {
    for(Task t : tasks) {
      if(t.getId() == taskId) {
        Assert.fail("Task with ID " + taskId  + " should not exist on the list");
      }
    }
  }
}
