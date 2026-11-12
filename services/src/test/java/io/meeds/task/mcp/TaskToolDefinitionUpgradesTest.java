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
package io.meeds.task.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The task add-on's {@code ai-tool-definition-upgrades.json}: an instance that
 * stored a tool's definition at an earlier start keeps it unless this file
 * lists that stored text's digest, so every definition of a changed tool the
 * add-on ever shipped is listed, and the shipped one never is (EXO-94250).
 * The digest is the one mcp-server's {@code McpToolUtils#definitionDigest}
 * computes: the SHA-256 of the description and the input schema as one
 * compact JSON object whose keys are sorted at every level.
 */
public class TaskToolDefinitionUpgradesTest {

  private static final JsonMapper          MAPPER                   = JsonMapper.builder().build();

  /** How many earlier definitions of each changed tool the sweep of every ref found. */
  private static final Map<String, Integer> EARLIER_DEFINITIONS      = Map.of("list_tasks", 17, "list_assigned_tasks", 12);

  /**
   * The definitions feature/rtd shipped before EXO-94250, which the
   * integration rig of 12 November stored.
   */
  private static final Map<String, String>  DEFINITIONS_BEFORE_94250 =
                                                                    Map.of("list_tasks",
                                                                           "f5710ba530526048c337add3dd8e0fe6a930b5dd6b7a750b25be6ddf3d5cb9ff",
                                                                           "list_assigned_tasks",
                                                                           "d946c744a74d7798c5679c7c83517449738a45c8937a5572b8ed9fad398f3884");

  /**
   * Each changed tool lists every earlier definition, the one shipped just
   * before included, and not its shipped one, which an instance already
   * holding it keeps anyway. Mutants: a digest removed, the shipped digest
   * added, a tool's entry dropped.
   *
   * @throws Exception never
   */
  @Test
  public void eachChangedToolListsItsEarlierDefinitionsOnly() throws Exception { // NOSONAR
    Map<String, List<String>> replaced = replacedDefinitions();
    Map<String, String> shipped = shippedDigests();

    assertEquals(EARLIER_DEFINITIONS.keySet(), replaced.keySet());
    EARLIER_DEFINITIONS.forEach((name, count) -> {
      List<String> digests = replaced.get(name);
      assertEquals(name, count.intValue(), digests.size());
      assertEquals(name, count.intValue(), Set.copyOf(digests).size());
      assertTrue(name, digests.contains(DEFINITIONS_BEFORE_94250.get(name)));
      assertFalse(name + " lists its shipped definition", digests.contains(shipped.get(name)));
      digests.forEach(digest -> assertTrue(digest, digest.matches("[0-9a-f]{64}")));
    });
  }

  /**
   * The digest reads the definition, not its layout: the keys of the input
   * schema in another order give the same digest, as mcp-server's canonical
   * form does, and one more character of the description another. Mutant:
   * the keys left unsorted.
   *
   * @throws Exception never
   */
  @Test
  public void theDigestIsTheStoredDefinitionsOne() throws Exception { // NOSONAR
    String description = "List uncompleted tasks";
    JsonNode schema = MAPPER.readTree("{\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"},\"a\":{\"type\":\"integer\"}}}");
    JsonNode reordered = MAPPER.readTree("{\"properties\":{\"a\":{\"type\":\"integer\"},\"b\":{\"type\":\"string\"}},\"type\":\"object\"}");

    assertEquals(digest(description, schema), digest(description, reordered));
    assertFalse(digest(description, schema).equals(digest(description + ".", schema)));
  }

  /**
   * @return by tool name, the digests the upgrades file lists
   * @throws Exception when the file can't be read
   */
  private static Map<String, List<String>> replacedDefinitions() throws Exception {
    Map<String, List<String>> replaced = new HashMap<>();
    for (JsonNode tool : read("ai-tool-definition-upgrades.json").path("tools")) {
      List<String> digests = new ArrayList<>();
      tool.path("replaced_definitions").forEach(digest -> digests.add(digest.asString()));
      replaced.put(tool.path("name").asString(), digests);
    }
    return replaced;
  }

  /**
   * @return by tool name, the digest of the definition the add-on ships
   * @throws Exception when the file can't be read
   */
  private static Map<String, String> shippedDigests() throws Exception {
    Map<String, String> digests = new HashMap<>();
    for (JsonNode tool : read("ai-tool-definitions.json").path("tools")) {
      digests.put(tool.path("name").asString(), digest(tool.path("description").asString(), tool.path("input_schema")));
    }
    return digests;
  }

  /**
   * @param resource a resource of the add-on's classpath
   * @return its JSON tree
   * @throws Exception when it can't be read
   */
  private static JsonNode read(String resource) throws Exception {
    try (InputStream input = TaskMcpTool.class.getClassLoader().getResourceAsStream(resource)) {
      return MAPPER.readTree(input);
    }
  }

  /**
   * @param description a tool's description
   * @param inputSchema its input schema
   * @return the definition's digest, as mcp-server computes it
   * @throws Exception never
   */
  private static String digest(String description, JsonNode inputSchema) throws Exception {
    ObjectNode definition = MAPPER.createObjectNode();
    definition.put("description", description);
    definition.set("input_schema", inputSchema);
    byte[] hash = MessageDigest.getInstance("SHA-256")
                               .digest(MAPPER.writeValueAsString(withSortedKeys(definition)).getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash);
  }

  /**
   * @param node a JSON tree
   * @return a copy whose objects list their keys in sorted order at every
   *         level
   */
  private static JsonNode withSortedKeys(JsonNode node) {
    if (node.isObject()) {
      ObjectNode sorted = MAPPER.createObjectNode();
      List<Map.Entry<String, JsonNode>> properties = new ArrayList<>(node.properties());
      properties.sort(Map.Entry.comparingByKey());
      properties.forEach(property -> sorted.set(property.getKey(), withSortedKeys(property.getValue())));
      return sorted;
    } else if (node.isArray()) {
      ArrayNode array = MAPPER.createArrayNode();
      node.values().forEach(value -> array.add(withSortedKeys(value)));
      return array;
    } else {
      return node;
    }
  }

}
