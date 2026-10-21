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
