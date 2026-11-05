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
package org.exoplatform.task.storage.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.mockito.ArgumentCaptor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.task.model.TaskSearchFilter;

import io.meeds.task.search.TaskSearchConnector;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RunWith(MockitoJUnitRunner.class)
public class TaskSearchConnectorTest {

  private static final String    ES_INDEX = "task_alias";

  private static final JsonMapper MAPPER   = JsonMapper.builder().build();

  @Mock
  private ElasticSearchingClient client;

  @Mock
  private InitParams             initParams;

  @Mock
  private PropertiesParam        propertiesParam;

  private TaskSearchConnector taskSearchConnector;

  @Before
  public void setUp() {
    when(initParams.getPropertiesParam("constructor.params")).thenReturn(propertiesParam);
    when(propertiesParam.getProperty("index")).thenReturn(ES_INDEX);
    taskSearchConnector = new TaskSearchConnector(client, initParams);
  }

  @Test
  public void testSearchReturnsIds() {
    TaskSearchFilter filter = new TaskSearchFilter();
    filter.setLimit(5);
    filter.setOffset(0);
    filter.setTerm("test");
    filter.setPermissions(List.of("user1"));

    String jsonResponse = """
        {
          "hits": {
            "hits": [
              { "_id": "1" },
              { "_id": "2" }
            ]
          }
        }
        """;

    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn(jsonResponse);

    List<Long> result = taskSearchConnector.search(filter);

    assertEquals(List.of(1L, 2L), result);
  }

  @Test
  public void testCount() {
    TaskSearchFilter filter = new TaskSearchFilter();
    filter.setLimit(5);
    filter.setOffset(0);
    filter.setTerm("test");
    filter.setPermissions(List.of("user1"));

    String countResponse = """
        {
          "count": 42
        }
        """;

    when(client.countRequest(anyString(), eq(ES_INDEX))).thenReturn(countResponse);

    long count = taskSearchConnector.count(filter);
    assertEquals(42, count);
  }

  /**
   * A filter naming projects adds a terms clause on the projects to the
   * search and to the count, next to the term and the permissions, and the
   * request stays valid JSON. Checked against Elasticsearch 9.4.5 by hand
   * (EXO-91903): on an index with the task mapping and settings, a terms
   * query on the dynamically mapped projectId matches the documents of that
   * project only; no Elasticsearch runs in this suite.
   *
   */
  @Test
  public void testSearchAndCountByProject() {
    TaskSearchFilter filter = new TaskSearchFilter("user1", "Grand Dax", List.of("user1", "member:/spaces/sales"), null, null, 0, 100);
    filter.setProjectIds(List.of(8319L));
    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"hits\":{\"hits\":[]}}");
    when(client.countRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"count\":0}");

    taskSearchConnector.search(filter);
    taskSearchConnector.count(filter);

    ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(search.capture(), eq(ES_INDEX));
    ArgumentCaptor<String> count = ArgumentCaptor.forClass(String.class);
    verify(client).countRequest(count.capture(), eq(ES_INDEX));
    for (String request : List.of(search.getValue(), count.getValue())) {
      JsonNode must = must(request);
      assertEquals(3, must.size());
      assertEquals(List.of("8319"), terms(must, "projectId"));
      assertEquals(List.of("user1", "member:/spaces/sales"), terms(must, "permissions"));
      assertTrue(must.get(0).has("query_string"));
    }
  }

  /**
   * A filter naming no project adds no project clause.
   *
   */
  @Test
  public void testSearchWithoutProject() {
    TaskSearchFilter filter = new TaskSearchFilter("user1", "Grand Dax", List.of("user1"), null, null, 0, 100);
    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"hits\":{\"hits\":[]}}");

    taskSearchConnector.search(filter);

    ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(search.capture(), eq(ES_INDEX));
    JsonNode must = must(search.getValue());
    assertEquals(2, must.size());
    assertFalse(search.getValue().contains("projectId"));
  }

  /**
   * Tasks another add-on contributed for the term are an alternative to the
   * term inside the {@code must}, beside the permissions: a contributed task
   * still has to pass the permissions, in the search and in the count. Each id
   * is written once, as a string, with the configured boost. Mutants: the ids
   * clause moved out of the {@code must} (into a top-level {@code should}), or
   * written without the term alternative.
   */
  @Test
  public void testContributedIdsAreAnAlternativeToTheTermInsideTheMust() {
    TaskSearchFilter filter = new TaskSearchFilter("user1", "Northwind", List.of("user1", "member:/spaces/sales"), null, null, 0, 10);
    filter.setContributedIds(java.util.Arrays.asList(7L, 8L, 7L, null));
    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"hits\":{\"hits\":[]}}");
    when(client.countRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"count\":0}");

    taskSearchConnector.search(filter);
    taskSearchConnector.count(filter);

    ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(search.capture(), eq(ES_INDEX));
    ArgumentCaptor<String> count = ArgumentCaptor.forClass(String.class);
    verify(client).countRequest(count.capture(), eq(ES_INDEX));
    for (String request : List.of(search.getValue(), count.getValue())) {
      JsonNode bool = MAPPER.readTree(request).path("query").path("bool");
      assertFalse(bool.has("should"));
      JsonNode must = bool.path("must");
      assertEquals(2, must.size());
      assertEquals(List.of("user1", "member:/spaces/sales"), terms(must, "permissions"));
      JsonNode alternative = must.get(0).path("bool");
      assertEquals(1, alternative.path("minimum_should_match").asInt());
      JsonNode should = alternative.path("should");
      assertEquals(2, should.size());
      assertEquals("Northwind", should.get(0).path("query_string").path("query").asString());
      JsonNode ids = should.get(1).path("ids");
      List<String> values = new ArrayList<>();
      ids.path("values").forEach(value -> values.add(value.asString()));
      assertEquals(List.of("7", "8"), values);
      assertEquals(TaskSearchConnector.DEFAULT_CONTRIBUTED_BOOST, ids.path("boost").asDouble(), 0d);
    }
  }

  /**
   * With no contributed task, the request is the one the connector always
   * built: the term, then the permissions, sorted by score.
   */
  @Test
  public void testNoContributedIdsKeepsTheRequest() {
    TaskSearchFilter filter = new TaskSearchFilter("user1", "Grand Dax", List.of("user1"), null, null, 0, 20);
    filter.setContributedIds(List.of());
    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"hits\":{\"hits\":[]}}");

    taskSearchConnector.search(filter);

    ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(search.capture(), eq(ES_INDEX));
    JsonNode expected = MAPPER.readTree("""
        {"from":0,"size":20,"query":{"bool":{"must":[
          {"query_string":{"fields":["title","description"],"default_operator":"AND","query":"Grand Dax"}},
          {"terms":{"permissions":["user1"]}}]}},
         "sort":[{"_score":{"order":"desc"}}]}
        """);
    assertEquals(expected, MAPPER.readTree(search.getValue()));
  }

  /**
   * A typed term that spells a placeholder of the request template stays the
   * text searched: the template is filled in one pass. Mutant: the chained
   * replacements, where {@code @permission_query@} typed in the term becomes
   * a second permissions clause.
   */
  @Test
  public void testTermSpellingAPlaceholderStaysText() {
    TaskSearchFilter filter = new TaskSearchFilter("user1",
                                                   "@permission_query@ @project_query@ @sortQuery@ x",
                                                   List.of("user1"),
                                                   null,
                                                   null,
                                                   0,
                                                   10);
    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"hits\":{\"hits\":[]}}");

    taskSearchConnector.search(filter);

    ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(search.capture(), eq(ES_INDEX));
    JsonNode must = must(search.getValue());
    assertEquals(2, must.size());
    assertEquals("@permission_query@ @project_query@ @sortQuery@ x",
                 must.get(0).path("query_string").path("query").asString());
  }

  /**
   * A term holding a backslash (inside or at the end), a double quote or a
   * control character still gives one valid request: the backslash and the
   * quote are query_string syntax and become spaces, and what is left is
   * escaped for JSON. Mutants: the backslash kept in the stripped characters
   * (a trailing one escapes the closing quote, the request is not JSON), and
   * the JSON escape removed (a tab breaks the string).
   */
  @Test
  public void testTermWithBackslashQuoteOrTabGivesAValidRequest() {
    java.util.Map<String, String> expected = new java.util.LinkedHashMap<>();
    expected.put("foo\\", "foo");
    expected.put("a\\ b", "a  b");
    expected.put("say \"x\"", "say  x");
    expected.put("a\tb", "a\tb");
    expected.put("ligne\nsuivante", "ligne\nsuivante");
    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"hits\":{\"hits\":[]}}");
    for (java.util.Map.Entry<String, String> entry : expected.entrySet()) {
      TaskSearchFilter filter = new TaskSearchFilter("user1", entry.getKey(), List.of("user1"), null, null, 0, 10);
      taskSearchConnector.search(filter);
    }

    ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
    verify(client, org.mockito.Mockito.times(expected.size())).sendRequest(search.capture(), eq(ES_INDEX));
    List<String> requests = search.getAllValues();
    int i = 0;
    for (String value : expected.values()) {
      JsonNode must = must(requests.get(i++));
      assertEquals(value, must.get(0).path("query_string").path("query").asString());
    }
  }

  /**
   * The sort direction is ascending or descending, whatever the client sends:
   * it is written into the request. Mutant: the direction written as sent.
   */
  @Test
  public void testSortDirectionIsAscendingOrDescendingOnly() {
    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn("{\"hits\":{\"hits\":[]}}");
    taskSearchConnector.search(new TaskSearchFilter("user1", "x", List.of("user1"), "date", "asc\"}},{\"x\":\"", 0, 10));
    taskSearchConnector.search(new TaskSearchFilter("user1", "x", List.of("user1"), "date", "ASC", 0, 10));

    ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
    verify(client, org.mockito.Mockito.times(2)).sendRequest(search.capture(), eq(ES_INDEX));
    assertEquals("desc",
                 MAPPER.readTree(search.getAllValues().get(0)).path("sort").get(0).path("lastUpdatedDate").path("order").asString());
    assertEquals("asc",
                 MAPPER.readTree(search.getAllValues().get(1)).path("sort").get(0).path("lastUpdatedDate").path("order").asString());
  }

  /**
   * The contributed tasks' boost is read from the configuration, from 0.1 to
   * 100; anything else keeps the default. Mutants: the range check removed,
   * its bounds inverted.
   */
  @Test
  public void testContributedBoostIsReadWithinItsRange() {
    java.util.Map<String, Double> expected = new java.util.LinkedHashMap<>();
    expected.put(null, 5.0d);
    expected.put("5", 5.0d);
    expected.put("0.1", 0.1d);
    expected.put("100", 100d);
    expected.put("0", 5.0d);
    expected.put("-3", 5.0d);
    expected.put("100.5", 5.0d);
    expected.put("NaN", 5.0d);
    expected.put("abc", 5.0d);
    for (java.util.Map.Entry<String, Double> entry : expected.entrySet()) {
      when(propertiesParam.getProperty("contributedBoost")).thenReturn(entry.getKey());
      assertEquals(entry.getKey(),
                   entry.getValue(),
                   new TaskSearchConnector(client, initParams).getContributedBoost(),
                   0d);
    }
  }

  /**
   * Reads a request with a strict parser: a trailing comma, which
   * Elasticsearch refuses, fails here too.
   *
   * @param request a search or count request
   * @return the clauses its boolean query must match
   */
  private static JsonNode must(String request) {
    return MAPPER.readTree(request).path("query").path("bool").path("must");
  }

  /**
   * @param must the clauses of a boolean query
   * @param field a field
   * @return the values of the terms clause on that field, null for none
   */
  private static List<String> terms(JsonNode must, String field) {
    for (JsonNode clause : must) {
      JsonNode values = clause.path("terms").get(field);
      if (values != null) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.asString()));
        return result;
      }
    }
    return null;
  }

}
