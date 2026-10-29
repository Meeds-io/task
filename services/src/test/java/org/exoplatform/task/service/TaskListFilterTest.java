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
package org.exoplatform.task.service;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dao.condition.PredicateCondition;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.dto.TasksList;
import org.exoplatform.task.model.TaskListFilter;
import org.exoplatform.task.service.impl.TaskServiceImpl;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.storage.impl.TaskStorageImpl;

import io.meeds.task.search.TaskSearchConnector;

/**
 * The extension conditions of a {@link TaskListFilter} run inside the project
 * board's task query, through the real DAO: combined with AND with the project
 * and the usual filters, in the page and in the count alike.
 */
public class TaskListFilterTest extends AbstractTest {

  private static final String ROOT = "root";

  private DAOHandler          daoHandler;

  private TaskService         taskService;

  private Project             alpha;

  private Task                matchByRoot;

  private Task                matchByJohn;

  private Task                otherByRoot;

  @Before
  public void setUp() {
    PortalContainer container = PortalContainer.getInstance();
    daoHandler = container.getComponentInstanceOfType(DAOHandler.class);
    UserService userService = container.getComponentInstanceOfType(UserService.class);
    ProjectStorage projectStorage = container.getComponentInstanceOfType(ProjectStorage.class);
    TaskSearchConnector taskSearchConnector = container.getComponentInstanceOfType(TaskSearchConnector.class);
    taskService = new TaskServiceImpl(new TaskStorageImpl(daoHandler, userService, projectStorage, taskSearchConnector),
                                      daoHandler,
                                      container.getComponentInstanceOfType(ListenerService.class));

    alpha = newProject("Alpha");
    Project beta = newProject("Beta");
    Status alphaStatus = newStatus(alpha);
    Status betaStatus = newStatus(beta);
    matchByRoot = newTask("match one", ROOT, alphaStatus);
    matchByJohn = newTask("match two", "john", alphaStatus);
    otherByRoot = newTask("other", ROOT, alphaStatus);
    newTask("match in beta", ROOT, betaStatus);
  }

  @After
  public void tearDown() {
    deleteAll();
  }

  /**
   * An extension condition narrows the project's tasks; beside the assignee
   * filter, only the tasks both keep remain. Without the extension
   * conditions being added to the query, the first assertions see the whole
   * project; added outside the project restriction, they see beta's task.
   */
  @Test
  public void testExtensionConditionCombinesWithProjectAndUsualFilters() throws Exception {
    TasksList extensionOnly = taskService.filterTasks(filter(null, List.of(titleContains("match"))));
    assertEquals(List.of(matchByRoot.getId(), matchByJohn.getId()), ids(extensionOnly));
    assertEquals(2, extensionOnly.getTasksSize());

    TasksList assigneeOnly = taskService.filterTasks(filter(List.of(ROOT), List.of()));
    assertEquals(List.of(matchByRoot.getId(), otherByRoot.getId()), ids(assigneeOnly));

    TasksList both = taskService.filterTasks(filter(List.of(ROOT), List.of(titleContains("match"))));
    assertEquals(List.of(matchByRoot.getId()), ids(both));
    assertEquals(1, both.getTasksSize());
  }

  /**
   * The count carries the extension condition as the page does: a one-row
   * page reports every matching task.
   */
  @Test
  public void testExtensionConditionRestrictsTheCount() throws Exception {
    TaskListFilter firstRow = filter(null, List.of(titleContains("match")));
    firstRow.setLimit(1);
    TasksList page = taskService.filterTasks(firstRow);
    assertEquals(1, page.getListTasks().size());
    assertEquals(2, page.getTasksSize());
  }

  /**
   * The criteria-by-criteria method filters as before, delegating to the
   * object filter with no extension condition.
   */
  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedMethodFiltersAsBefore() throws Exception {
    TasksList tasks = taskService.filterTasks(null,
                                              alpha.getId(),
                                              null,
                                              null,
                                              null,
                                              null,
                                              List.of(ROOT),
                                              null,
                                              null,
                                              null,
                                              null,
                                              new Identity(ROOT),
                                              "",
                                              null,
                                              TimeZone.getDefault(),
                                              false,
                                              true,
                                              false,
                                              false,
                                              null,
                                              null,
                                              0,
                                              -1);
    assertEquals(List.of(matchByRoot.getId(), otherByRoot.getId()), ids(tasks));
  }

  private TaskListFilter filter(List<String> assignees, List<Condition> conditions) {
    TaskListFilter filter = new TaskListFilter();
    filter.setProjectId(alpha.getId());
    filter.setAssignees(assignees);
    filter.setCurrIdentity(new Identity(ROOT));
    filter.setDueCategory("");
    filter.setUserTimezone(TimeZone.getDefault());
    filter.setAdvanceSearch(true);
    filter.setOffset(0);
    filter.setLimit(-1);
    filter.setExtensionConditions(conditions);
    return filter;
  }

  private static Condition titleContains(String text) {
    return new PredicateCondition<Task>((cb, query, root) -> cb.like(root.get("title"), "%" + text + "%"));
  }

  private static List<Long> ids(TasksList tasks) {
    return tasks.getListTasks().stream().map(TaskDto::getId).sorted(Comparator.naturalOrder()).toList();
  }

  private Project newProject(String name) {
    Project project = new Project();
    project.setName(name);
    return daoHandler.getProjectHandler().create(project);
  }

  private Status newStatus(Project project) {
    Status status = new Status();
    status.setName("To do");
    status.setRank(1);
    status.setProject(project);
    return daoHandler.getStatusHandler().create(status);
  }

  private Task newTask(String title, String assignee, Status status) {
    Task task = new Task();
    task.setTitle(title);
    task.setAssignee(assignee);
    task.setCreatedBy(ROOT);
    task.setCreatedTime(new Date());
    task.setStatus(status);
    return daoHandler.getTaskHandler().create(task);
  }

}
