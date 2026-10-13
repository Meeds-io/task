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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import io.meeds.mcp.server.model.SimpleToolDefinition;
import io.meeds.mcp.server.model.SpaceScopeDeclaration;
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

  private static final Set<String> TASK_WRITES   = Set.of("update_task_status", "complete_task");

  private static final Set<String> DECLARED      = Set.of("list_projects",
                                                          "get_project_by_id",
                                                          "get_task_by_id",
                                                          "list_task_comments_by_id",
                                                          "update_task_status",
                                                          "complete_task");

  private static final JsonMapper  MAPPER        = JsonMapper.builder().build();

  /**
   * Exactly six task tools declare a space scope, each well formed for what
   * it is, a read or a write: the task tools by the task they reach and the
   * {@value TaskSpaceScopeResolver#NAME} resolver, the project lookup by the
   * project and the {@value ProjectSpaceScopeResolver#NAME} resolver, and the
   * project list by its space. A tool added, or one that stops declaring,
   * fails here, and every declared write keeps its approval.
   */
  @Test
  public void declaredToolsAreTheExpectedOnes() {
    Map<String, SimpleToolDefinition> declared = declaredTools();

    assertEquals(DECLARED, declared.keySet());
    declared.forEach((name, tool) -> {
      boolean write = TASK_WRITES.contains(name);
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
  }

  /**
   * The project list reads the call's space when it names none, takes its
   * space only through {@code space_id}, and may carry its paging, nothing
   * else.
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
    assertEquals(List.of("offset", "limit"), allowed(tool));
    JsonNode properties = MAPPER.readTree(tool.getInputSchema()).path("properties");
    assertTrue(properties.has("space_id"));
    allowed(tool).forEach(argument -> assertTrue(argument, properties.has(argument)));
  }

  /**
   * The project lookup takes no argument beside the project, and the project
   * is a schema property.
   */
  @Test
  public void projectLookupTakesTheProjectOnly() {
    SimpleToolDefinition tool = declaredTools().get("get_project_by_id");

    assertEquals(List.of(), allowed(tool));
    assertTrue(MAPPER.readTree(tool.getInputSchema()).path("properties").has("project_id"));
  }

  /**
   * The arguments a limited task call may carry are schema properties: the
   * comments' paging, and the status of the status update. The only
   * reference-shaped one is that status, which {@code TaskMcpTool} accepts
   * only when it belongs to the project of the task, the one the resolver
   * reads the space of.
   */
  @Test
  public void acceptedArgumentsAreTheExpectedSchemaProperties() {
    Map<String, SimpleToolDefinition> declared = declaredTools();

    assertEquals(List.of(), allowed(declared.get("get_task_by_id")));
    assertEquals(List.of(), allowed(declared.get("complete_task")));
    assertEquals(List.of("offset", "limit"), allowed(declared.get("list_task_comments_by_id")));
    assertEquals(List.of("status_id"), allowed(declared.get("update_task_status")));
    TASK_READS.forEach(name -> assertTaskArgumentsAreProperties(declared.get(name)));
    TASK_WRITES.forEach(name -> assertTaskArgumentsAreProperties(declared.get(name)));
    assertFalse(allowed(declared.get("get_task_by_id")).contains("status_id"));
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
