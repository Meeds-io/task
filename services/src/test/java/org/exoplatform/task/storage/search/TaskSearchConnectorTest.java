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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.task.model.TaskSearchFilter;

import io.meeds.task.search.TaskSearchConnector;

@RunWith(MockitoJUnitRunner.class)
public class TaskSearchConnectorTest {

  private static final String    ES_INDEX = "task_alias";

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

  @Test
  public void testKeywordReservedCharactersAreIgnored() throws Exception {
    JsonNode request = searchRequest(filterWithTerm("report\\"));

    assertEquals("report", termOf(request));
  }

  @Test
  public void testKeywordIsWrittenAsJsonText() throws Exception {
    JsonNode request = searchRequest(filterWithTerm("quarterly \\ \"report\" tab\there"));

    assertEquals("quarterly    report  tab\there", termOf(request));
  }

  @Test
  public void testKeywordOperatorWordsAreDropped() throws Exception {
    assertEquals("", termOf(searchRequest(filterWithTerm("AND"))));
  }

  @Test
  public void testKeywordOperatorWordsAreDroppedInEveryPosition() throws Exception {
    assertEquals("Renew", termOf(searchRequest(filterWithTerm("Renew AND"))));
  }

  @Test
  public void testKeywordLeadingAndTrailingOperatorWordsAreDropped() throws Exception {
    assertEquals("Renew", termOf(searchRequest(filterWithTerm("OR Renew"))));
  }

  @Test
  public void testKeywordOperatorWordsInsideOtherWordsAreKept() throws Exception {
    assertEquals("ANDROID Notes ORDER and or not", termOf(searchRequest(filterWithTerm("ANDROID Notes ORDER and or not"))));
  }

  @Test
  public void testKeywordIsNeverReadAsTemplate() throws Exception {
    TaskSearchFilter filter = filterWithTerm("@permission_query@ @sortQuery@ @limit@");
    filter.setSortField("date");
    filter.setSortDirection("asc");

    JsonNode request = searchRequest(filter);

    assertEquals("@permission_query@ @sortQuery@ @limit@", termOf(request));
    JsonNode must = request.get("query").get("bool").get("must");
    assertEquals(2, must.size());
    assertTrue(must.get(1).has("terms"));
    assertEquals(10, request.get("size").asInt());
  }

  @Test
  public void testSortDirectionAbsentIsDescending() throws Exception {
    assertEquals("desc", sortOrderOf(sortedFilter(null)));
  }

  @Test
  public void testSortDirectionOtherTextIsDescending() throws Exception {
    assertEquals("desc", sortOrderOf(sortedFilter("sideways")));
  }

  @Test
  public void testSortDirectionAscIsAscending() throws Exception {
    assertEquals("asc", sortOrderOf(sortedFilter("asc")));
  }

  private TaskSearchFilter filterWithTerm(String term) {
    TaskSearchFilter filter = new TaskSearchFilter();
    filter.setLimit(5);
    filter.setOffset(0);
    filter.setTerm(term);
    filter.setPermissions(List.of("user1"));
    return filter;
  }

  private TaskSearchFilter sortedFilter(String direction) {
    TaskSearchFilter filter = filterWithTerm("report");
    filter.setSortField("date");
    filter.setSortDirection(direction);
    return filter;
  }

  private JsonNode searchRequest(TaskSearchFilter filter) throws Exception {
    when(client.sendRequest(anyString(), eq(ES_INDEX))).thenReturn("{}");
    taskSearchConnector.search(filter);
    ArgumentCaptor<String> request = ArgumentCaptor.forClass(String.class);
    verify(client).sendRequest(request.capture(), eq(ES_INDEX));
    return new ObjectMapper().readTree(request.getValue());
  }

  private String termOf(JsonNode request) {
    return request.get("query").get("bool").get("must").get(0).get("query_string").get("query").asString();
  }

  private String sortOrderOf(TaskSearchFilter filter) throws Exception {
    JsonNode request = searchRequest(filter);
    return request.get("sort").get(0).get("lastUpdatedDate").get("order").asString();
  }

}
