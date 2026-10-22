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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;

import io.meeds.mcp.server.constant.Origin;
import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.model.SimpleToolDefinition;
import io.meeds.mcp.server.model.SpaceScopeDeclaration;
import io.meeds.mcp.server.model.SpaceScopeRefusedException;
import io.meeds.mcp.server.plugin.SpaceScopeResolver;
import io.meeds.mcp.server.service.McpToolSpaceScopeService;
import io.meeds.mcp.server.util.McpToolUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the {@code space_scope} declarations the task add-on ships: which task
 * tools a call limited to one space may use, on which arguments.
 */
public class TaskToolDefinitionSpaceScopeTest {

  private static final Set<String> TASK_READS    = Set.of("get_task_by_id", "list_task_comments_by_id");

  private static final Set<String> TASK_WRITES   = Set.of("update_task_status",
                                                          "update_task_description",
                                                          "complete_task",
                                                          "add_task_comment",
                                                          "update_task_title",
                                                          "set_task_dates",
                                                          "set_task_priority",
                                                          "reopen_task",
                                                          "add_project_label_to_task",
                                                          "remove_project_label_from_task",
                                                          "assign_task",
                                                          "add_task_coworker");

  private static final Set<String> DECLARED      = Set.of("list_projects",
                                                          "get_project_by_id",
                                                          "list_tasks",
                                                          "list_project_statuses",
                                                          "create_task_in_project",
                                                          "get_task_by_id",
                                                          "list_task_comments_by_id",
                                                          "update_task_status",
                                                          "update_task_description",
                                                          "complete_task",
                                                          "add_task_comment",
                                                          "update_task_title",
                                                          "set_task_dates",
                                                          "set_task_priority",
                                                          "reopen_task",
                                                          "add_project_label_to_task",
                                                          "remove_project_label_from_task",
                                                          "assign_task",
                                                          "add_task_coworker");

  private static final JsonMapper  MAPPER        = JsonMapper.builder().build();

  /**
   * Exactly nineteen task tools declare a space scope, each well formed for what
   * it is, a read or a write: the task tools by the task they reach and the
   * {@value TaskSpaceScopeResolver#NAME} resolver, the project lookup, the
   * project's tasks and the project's statuses by the project and the
   * {@value ProjectSpaceScopeResolver#NAME} resolver, and the
   * project list by its space. A tool added, or one that stops declaring,
   * fails here, and every declared write keeps its approval.
   */
  @Test
  public void declaredToolsAreTheExpectedOnes() {
    Map<String, SimpleToolDefinition> declared = declaredTools();

    assertEquals(DECLARED, declared.keySet());
    declared.forEach((name, tool) -> {
      boolean write = TASK_WRITES.contains(name) || "create_task_in_project".equals(name);
      SpaceScopeDeclaration scope = tool.getSpaceScope();
      assertNotNull(name, tool.getAnnotations());
      assertEquals(name, !write, McpToolSpaceScopeService.isReadTool(tool));
      assertTrue(name, scope.isWellFormedFor(write));
      assertEquals(name, List.of(), scope.getFalseOnlyArgs() == null ? List.of() : scope.getFalseOnlyArgs());
      if (write) {
        assertTrue(name, tool.isRequireApproval());
      }
    });
    TASK_READS.forEach(name -> assertObjectKind(declared.get(name), "task_id", TaskSpaceScopeResolver.NAME));
    TASK_WRITES.forEach(name -> assertObjectKind(declared.get(name), "task_id", TaskSpaceScopeResolver.NAME));
    assertObjectKind(declared.get("get_project_by_id"), "project_id", ProjectSpaceScopeResolver.NAME);
    assertObjectKind(declared.get("list_project_statuses"), "project_id", ProjectSpaceScopeResolver.NAME);
    assertObjectKind(declared.get("list_tasks"), "project_id", ProjectSpaceScopeResolver.NAME);
    assertObjectKind(declared.get("create_task_in_project"), "project_id", ProjectSpaceScopeResolver.NAME);
  }

  /**
   * A task is created in a project of the call's space only, with the
   * arguments that stay inside it: its text, dates, priority and a status,
   * which {@code TaskMcpTool} takes only from the project's own statuses.
   * The assignee and the coworkers are arbitrary users, who could be outside
   * the space, so they are not accepted.
   * Mutant: the assignee or the coworkers are allowed, or the declaration is
   * dropped.
   */
  @Test
  public void taskCreationStaysInsideTheProject() {
    SimpleToolDefinition tool = declaredTools().get("create_task_in_project");

    assertTrue(tool.isRequireApproval());
    assertEquals(List.of("title", "description", "start_date", "due_date", "priority", "status_id"), allowed(tool));
    assertFalse(allowed(tool).contains("assignee"));
    assertFalse(allowed(tool).contains("coworkers"));
    JsonNode properties = MAPPER.readTree(tool.getInputSchema()).path("properties");
    allowed(tool).forEach(argument -> assertTrue(argument, properties.has(argument)));
    assertTrue(properties.has("project_id"));
  }

  /**
   * The project list reads the call's space when it names none, takes its
   * space only through {@code space_id}, and may carry its paging and the
   * name filter, which narrows the projects of that space, nothing else.
   * Mutant: {@code when_absent} dropped, so an absent space is refused; or
   * the space argument renamed.
   */
  @Test
  public void projectListUsesTheScopeSpaceWhenAbsent() {
    SimpleToolDefinition tool = declaredTools().get("list_projects");
    SpaceScopeDeclaration scope = tool.getSpaceScope();

    assertTrue(scope.isSpaceKind());
    assertEquals("space_id", scope.getSpaceArg());
    assertTrue(scope.isUseScopeWhenAbsent());
    assertEquals(List.of("offset", "limit", "name"), allowed(tool));
    assertTrue(tool.getDescription().contains("In a run limited to a space, only the projects managed through that space"));
    JsonNode properties = MAPPER.readTree(tool.getInputSchema()).path("properties");
    assertTrue(properties.has("space_id"));
    allowed(tool).forEach(argument -> assertTrue(argument, properties.has(argument)));
  }

  /**
   * The project lookup and the project's statuses take no argument beside the
   * project, and the project is a schema property.
   */
  @Test
  public void projectLookupTakesTheProjectOnly() {
    for (String name : List.of("get_project_by_id", "list_project_statuses")) {
      SimpleToolDefinition tool = declaredTools().get(name);

      assertEquals(name, List.of(), allowed(tool));
      assertTrue(name, MAPPER.readTree(tool.getInputSchema()).path("properties").has("project_id"));
    }
  }

  /**
   * The arguments a limited task call may carry are schema properties: the
   * comments' paging, the status of the status update, the new text of the
   * description update, a comment's text, a new title, the start and due
   * dates, the priority, a label and a person; reopening a task takes nothing
   * else. The only reference-shaped ones are that status and that label,
   * which {@code TaskMcpTool} accepts only when they belong to the project of
   * the task, the one the resolver reads the space of; the person, the
   * assignee or a coworker, it accepts in a limited call only when they are
   * a member of the call's space.
   */
  @Test
  public void acceptedArgumentsAreTheExpectedSchemaProperties() {
    Map<String, SimpleToolDefinition> declared = declaredTools();

    assertEquals(List.of(), allowed(declared.get("get_task_by_id")));
    assertEquals(List.of(), allowed(declared.get("complete_task")));
    assertEquals(List.of("offset", "limit"), allowed(declared.get("list_task_comments_by_id")));
    assertEquals(List.of("status_id"), allowed(declared.get("update_task_status")));
    assertEquals(List.of("description"), allowed(declared.get("update_task_description")));
    assertEquals(List.of("text"), allowed(declared.get("add_task_comment")));
    assertEquals(List.of("title"), allowed(declared.get("update_task_title")));
    assertEquals(List.of("start_date", "due_date"), allowed(declared.get("set_task_dates")));
    assertEquals(List.of("priority"), allowed(declared.get("set_task_priority")));
    assertEquals(List.of(), allowed(declared.get("reopen_task")));
    assertEquals(List.of("label_id"), allowed(declared.get("add_project_label_to_task")));
    assertEquals(List.of("label_id"), allowed(declared.get("remove_project_label_from_task")));
    assertEquals(List.of("username"), allowed(declared.get("assign_task")));
    assertEquals(List.of("username"), allowed(declared.get("add_task_coworker")));
    TASK_READS.forEach(name -> assertTaskArgumentsAreProperties(declared.get(name)));
    TASK_WRITES.forEach(name -> assertTaskArgumentsAreProperties(declared.get(name)));
    assertFalse(allowed(declared.get("get_task_by_id")).contains("status_id"));
  }

  /**
   * The task list of a run limited to a space lists the tasks of one project
   * of that space only, with the filters that narrow that project's tasks and
   * the paging: {@code TaskMcpTool#listTasks} reads every task the user can
   * access when no project is given, so the project is the scope, and every
   * other argument either narrows it or pages it.
   * Mutant: the declaration dropped, the resolver or the object argument
   * changed, or an argument added to the accepted ones.
   */
  @Test
  public void taskListTakesAProjectAndItsFilters() {
    SimpleToolDefinition tool = declaredTools().get("list_tasks");

    assertTrue(McpToolSpaceScopeService.isReadTool(tool));
    assertEquals(List.of("hide_completed_tasks",
                         "include_change_log",
                         "overdue_only",
                         "offset",
                         "limit",
                         "title",
                         "changed_since",
                         "completed_since",
                         "assignee",
                         "status_id",
                         "include_description"),
                 allowed(tool));
    JsonNode properties = MAPPER.readTree(tool.getInputSchema()).path("properties");
    assertTrue(properties.has("project_id"));
    allowed(tool).forEach(argument -> assertTrue(argument, properties.has(argument)));
    assertTrue(tool.getDescription().contains("In a run limited to a space, project_id is required"));
  }

  /**
   * Through the real space scope check and the real
   * {@value ProjectSpaceScopeResolver#NAME} resolver, a run limited to space
   * 12 lists the tasks of a project managed through space 12, with its
   * filters, paging and title; it can't list those of a project of space 13, of a
   * project that doesn't exist, of a project id that isn't one (zero or
   * negative, which {@code TaskMcpTool#listTasks} would read as "every
   * accessible task"), or with no project at all; and an argument the
   * declaration doesn't accept is refused even beside a project of the space.
   * A title alone, with no project, is refused like any call without one.
   * Mutants: the declaration dropped, its resolver renamed, its object
   * argument changed, or an argument added to the accepted ones.
   *
   * @throws EntityNotFoundException never, the project service is a mock
   */
  @Test
  @SuppressWarnings("unchecked")
  public void taskListIsEnforcedUnderASpaceScope() throws EntityNotFoundException {
    ProjectService projectService = mock(ProjectService.class);
    SpaceService spaceService = mock(SpaceService.class);
    TaskSpaceScopeResolver taskResolver = new TaskSpaceScopeResolver();
    ReflectionTestUtils.setField(taskResolver, "spaceService", spaceService);
    ProjectSpaceScopeResolver projectResolver = new ProjectSpaceScopeResolver();
    ReflectionTestUtils.setField(projectResolver, "projectService", projectService);
    ReflectionTestUtils.setField(projectResolver, "taskSpaceScopeResolver", taskResolver);
    Space alpha = mock(Space.class);
    when(alpha.getId()).thenReturn("12");
    Space beta = mock(Space.class);
    when(beta.getId()).thenReturn("13");
    when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenReturn(alpha);
    when(spaceService.getSpaceByGroupId("/spaces/beta")).thenReturn(beta);
    when(projectService.getProject(7L)).thenReturn(project(7L, "manager:/spaces/alpha"));
    when(projectService.getProject(8L)).thenReturn(project(8L, "manager:/spaces/beta"));
    when(projectService.getProject(9L)).thenThrow(new EntityNotFoundException(9L, ProjectDto.class));
    ObjectProvider<SpaceScopeResolver> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenAnswer(invocation -> Stream.of(projectResolver, taskResolver));
    McpToolSpaceScopeService service = new McpToolSpaceScopeService(provider);
    ActingIdentity read = ActingIdentity.agent("support-desk", "SUPPORT_DESK", Origin.MENTION, 12L);
    SimpleToolDefinition tool = declaredTools().get("list_tasks");

    String inSpace = "{\"project_id\":7,\"hide_completed_tasks\":true,\"overdue_only\":false,"
        + "\"include_change_log\":false,\"offset\":0,\"limit\":20}";
    assertEquals(inSpace, service.enforce("list_tasks", tool, inSpace, read));
    String byTitle = "{\"project_id\":7,\"title\":\"Live check 2\"}";
    assertEquals(byTitle, service.enforce("list_tasks", tool, byTitle, read));
    String inSpaceAsString = "{\"project_id\":\"7\"}";
    assertEquals(inSpaceAsString, service.enforce("list_tasks", tool, inSpaceAsString, read));
    for (String outside : List.of("{\"project_id\":8}",
                                  "{\"project_id\":9}",
                                  "{\"project_id\":0}",
                                  "{\"project_id\":-7}",
                                  "{\"project_id\":\"07\"}",
                                  "{\"project_id\":null}",
                                  "{\"hide_completed_tasks\":true}",
                                  "{\"title\":\"Live check 2\"}",
                                  "{\"project_id\":8,\"title\":\"Live check 2\"}",
                                  "{}")) {
      assertThrows(outside, SpaceScopeRefusedException.class, () -> service.enforce("list_tasks", tool, outside, read));
    }
    for (String excluded : List.of("{\"project_id\":7,\"space_id\":13}",
                                   "{\"project_id\":7,\"query\":\"Live check 2\"}",
                                   "{\"project_id\":7,\"projectId\":8}",
                                   "{\"project_id\":7,\"project_id\":8}")) {
      assertThrows(excluded, SpaceScopeRefusedException.class, () -> service.enforce("list_tasks", tool, excluded, read));
    }
  }

  /**
   * Through the real space scope check and the real
   * {@value TaskSpaceScopeResolver#NAME} resolver, an approved proposal of a
   * run limited to space 12 comments on, renames, dates, prioritises,
   * reopens, labels, unlabels, assigns a task of a project managed through
   * space 12, or adds a coworker to it, with the arguments each tool accepts; the same call on a task of a project of space 13, on a
   * personal task or on a task that doesn't exist is refused, and so is any
   * argument a declaration doesn't accept, such as a username, or an assignee
   * for the tools that take a username. A write the
   * run makes itself, not as an approved proposal, is refused too.
   * Mutants: a declaration dropped, its resolver renamed, its object argument
   * changed, or an argument added to the accepted ones.
   *
   * @throws EntityNotFoundException never, the task service is a mock
   */
  @Test
  @SuppressWarnings("unchecked")
  public void taskWritesAreEnforcedUnderASpaceScope() throws EntityNotFoundException {
    TaskService taskService = mock(TaskService.class);
    SpaceService spaceService = mock(SpaceService.class);
    TaskSpaceScopeResolver taskResolver = new TaskSpaceScopeResolver();
    ReflectionTestUtils.setField(taskResolver, "taskService", taskService);
    ReflectionTestUtils.setField(taskResolver, "spaceService", spaceService);
    Space alpha = mock(Space.class);
    when(alpha.getId()).thenReturn("12");
    Space beta = mock(Space.class);
    when(beta.getId()).thenReturn("13");
    when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenReturn(alpha);
    when(spaceService.getSpaceByGroupId("/spaces/beta")).thenReturn(beta);
    when(taskService.getTask(5L)).thenReturn(task(5L, project(7L, "manager:/spaces/alpha")));
    when(taskService.getTask(6L)).thenReturn(task(6L, project(8L, "manager:/spaces/beta")));
    when(taskService.getTask(4L)).thenReturn(task(4L, null));
    when(taskService.getTask(9L)).thenThrow(new EntityNotFoundException(9L, TaskDto.class));
    ObjectProvider<SpaceScopeResolver> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenAnswer(invocation -> Stream.of(taskResolver));
    McpToolSpaceScopeService service = new McpToolSpaceScopeService(provider);
    ActingIdentity proposal = ActingIdentity.agent("support-desk", "SUPPORT_DESK", Origin.PROPOSAL, 12L);
    ActingIdentity run = ActingIdentity.agent("support-desk", "SUPPORT_DESK", Origin.MENTION, 12L);
    Map<String, SimpleToolDefinition> declared = declaredTools();
    Map<String, String> arguments = Map.of("add_task_comment", ",\"text\":\"reminder\"",
                                           "update_task_title", ",\"title\":\"Renewals\"",
                                           "set_task_dates", ",\"start_date\":\"2026-10-20\",\"due_date\":\"2026-10-23\"",
                                           "set_task_priority", ",\"priority\":\"HIGH\"",
                                           "reopen_task", "",
                                           "add_project_label_to_task", ",\"label_id\":99",
                                           "remove_project_label_from_task", ",\"label_id\":99",
                                           "assign_task", ",\"username\":\"john\"",
                                           "add_task_coworker", ",\"username\":\"john\"");

    arguments.forEach((name, rest) -> {
      SimpleToolDefinition tool = declared.get(name);
      String unaccepted = rest.contains("username") ? "\"assignee\":\"mary\"" : "\"username\":\"mary\"";
      String inSpace = "{\"task_id\":5" + rest + "}";
      assertEquals(name, inSpace, service.enforce(name, tool, inSpace, proposal));
      assertThrows(name, SpaceScopeRefusedException.class, () -> service.enforce(name, tool, inSpace, run));
      for (String outside : List.of("{\"task_id\":6" + rest + "}",
                                    "{\"task_id\":4" + rest + "}",
                                    "{\"task_id\":9" + rest + "}",
                                    "{\"task_id\":0" + rest + "}",
                                    "{\"task_id\":5" + rest + "," + unaccepted + "}",
                                    "{\"task_id\":5" + rest + ",\"project_id\":7}")) {
        assertThrows(name + " " + outside,
                     SpaceScopeRefusedException.class,
                     () -> service.enforce(name, tool, outside, proposal));
      }
    });
  }

  /**
   * @param id a task id
   * @param project the task's project, null for a personal task
   * @return a task with that id, in a status of that project
   */
  private static TaskDto task(long id, ProjectDto project) {
    TaskDto task = new TaskDto();
    task.setId(id);
    if (project != null) {
      StatusDto status = new StatusDto();
      status.setProject(project);
      task.setStatus(status);
    }
    return task;
  }

  /**
   * @param id a project id
   * @param managers the project's managers
   * @return a project with that id, managed by those
   */
  private static ProjectDto project(long id, String... managers) {
    ProjectDto project = new ProjectDto();
    project.setId(id);
    project.setManager(Set.of(managers));
    return project;
  }

  /**
   * @param tool a task tool declaring a space scope
   */
  private static void assertTaskArgumentsAreProperties(SimpleToolDefinition tool) {
    JsonNode properties = MAPPER.readTree(tool.getInputSchema()).path("properties");
    allowed(tool).forEach(argument -> assertTrue(tool.getName() + " takes " + argument, properties.has(argument)));
    assertTrue(tool.getName(), properties.has("task_id"));
  }

  /**
   * @param tool a tool declaring a space scope
   * @param argument the argument that must hold the object id
   * @param resolver the resolver that must tell the object's space
   */
  private static void assertObjectKind(SimpleToolDefinition tool, String argument, String resolver) {
    SpaceScopeDeclaration scope = tool.getSpaceScope();
    assertTrue(tool.getName(), scope.isObjectKind());
    assertEquals(tool.getName(), argument, scope.getObjectArg());
    assertEquals(tool.getName(), resolver, scope.getResolver());
  }

  /**
   * @param tool a tool declaring a space scope
   * @return its allowed arguments, empty when it names none
   */
  private static List<String> allowed(SimpleToolDefinition tool) {
    List<String> allowed = tool.getSpaceScope().getAllowedArgs();
    return allowed == null ? List.of() : allowed;
  }

  /**
   * @return the shipped task tools that declare a space scope, by name
   */
  private static Map<String, SimpleToolDefinition> declaredTools() {
    try {
      URL classes = TaskMcpTool.class.getProtectionDomain().getCodeSource().getLocation();
      URL url = new File(new File(classes.toURI()), "ai-tool-definitions.json").toURI().toURL();
      return McpToolUtils.parseToolDefinitions(url)
                         .stream()
                         .filter(tool -> tool.getSpaceScope() != null)
                         .collect(Collectors.toMap(SimpleToolDefinition::getName, tool -> tool));
    } catch (Exception e) {
      throw new IllegalStateException("The task tool definitions couldn't be read", e);
    }
  }

}
