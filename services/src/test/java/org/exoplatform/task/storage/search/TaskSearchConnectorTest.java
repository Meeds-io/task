/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.task.storage.search;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.task.model.TaskSearchFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(MockitoJUnitRunner.class)
public class TaskSearchConnectorTest {

  private static final String    ES_INDEX = "task_alias";

  @Mock
  private ElasticSearchingClient client;

  @InjectMocks
  private TaskSearchConnector    taskSearchConnector;

  @Before
  public void setUp() {
    openMocks(this);
    ReflectionTestUtils.setField(taskSearchConnector, "index", ES_INDEX);
    ReflectionTestUtils.setField(taskSearchConnector, "searchType", "task");
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

}
