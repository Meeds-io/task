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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.model.TaskKeywordSearchPage;
import org.exoplatform.task.model.TaskSearchFilter;
import org.exoplatform.task.storage.TaskStorage;

import io.meeds.task.search.model.SearchFieldMatch;
import io.meeds.task.search.model.SearchFieldMatches;
import io.meeds.task.search.model.TaskSearchMatchRequest;
import io.meeds.task.search.model.TaskSearchMatches;
import io.meeds.task.search.plugin.TaskSearchMatchPlugin;

/**
 * Unified search's keyword search of tasks with contributors
 * ({@link TaskServiceImpl#searchTasksByKeyword}).
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskKeywordSearchTest {

  @Mock
  private TaskStorage     taskStorage;

  @Mock
  private DAOHandler      daoHandler;

  @Mock
  private ListenerService listenerService;

  private TaskServiceImpl taskService;

  /**
   * A service on a mocked storage, its contributors set per test.
   */
  @Before
  public void setUp() {
    taskService = spy(new TaskServiceImpl(taskStorage, daoHandler, listenerService));
  }

  /**
   * Each contributor is asked once per search, with the viewer, the term, the
   * spaces, the offset plus the limit and the locale; the page and its count
   * run on the same contributed ids, and the matches come back per task id.
   * Mutants: the count re-asking the contributor, the ids not set before the
   * find, the count run before the find (it resets the offset and limit).
   */
  @Test
  public void testContributorAskedOnceAndPageAndCountUseItsIds() {
    Identity viewer = new Identity("user1");
    SearchFieldMatches line = new SearchFieldMatches("icon", "tip", List.of(new SearchFieldMatch("Account", "<mark>Northwind</mark>")), 1, "+1 more");
    TaskSearchMatchPlugin plugin = mock(TaskSearchMatchPlugin.class);
    when(plugin.search(any())).thenReturn(new TaskSearchMatches(List.of(7L, 8L), Map.of(7L, line)));
    doReturn(List.of(plugin)).when(taskService).getSearchMatchPlugins();
    TaskDto task = new TaskDto();
    task.setId(7L);
    List<List<Long>> idsAtFind = new ArrayList<>();
    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenAnswer(invocation -> {
      idsAtFind.add(((TaskSearchFilter) invocation.getArgument(0)).getContributedIds());
      return List.of(task);
    });
    when(taskStorage.countTasks(any(TaskSearchFilter.class))).thenReturn(5L);

    TaskSearchFilter filter = new TaskSearchFilter("user1", "Northwind", List.of("user1"), null, null, 10, 20);
    TaskKeywordSearchPage page = taskService.searchTasksByKeyword(filter, viewer, List.of(3L), Locale.FRENCH);

    ArgumentCaptor<TaskSearchMatchRequest> request = ArgumentCaptor.forClass(TaskSearchMatchRequest.class);
    verify(plugin, times(1)).search(request.capture());
    assertSame(viewer, request.getValue().viewer());
    assertEquals("Northwind", request.getValue().term());
    assertEquals(List.of(3L), request.getValue().spaceIds());
    assertEquals(30, request.getValue().limit());
    assertEquals(Locale.FRENCH, request.getValue().locale());
    assertEquals(List.of(List.of(7L, 8L)), idsAtFind);
    InOrder order = inOrder(taskStorage);
    order.verify(taskStorage).findTasks(filter);
    order.verify(taskStorage).countTasks(filter);
    assertEquals(List.of(7L, 8L), filter.getContributedIds());
    assertEquals(List.of(task), page.tasks());
    assertEquals(5L, page.size());
    assertSame(line, page.matches().get(7L));
    assertNull(page.matches().get(8L));
  }

  /**
   * A contributor that throws is skipped: the search answers with the other
   * contributors, or with none. Mutant: the catch removed.
   */
  @Test
  public void testFailingContributorIsSkipped() {
    TaskSearchMatchPlugin failing = mock(TaskSearchMatchPlugin.class);
    when(failing.search(any())).thenThrow(new IllegalStateException("index down"));
    TaskSearchMatchPlugin working = mock(TaskSearchMatchPlugin.class);
    when(working.search(any())).thenReturn(new TaskSearchMatches(List.of(9L), Map.of()));

    TaskSearchMatches merged = taskService.collectContributions(List.of(failing, working), request());

    assertEquals(List.of(9L), merged.taskIds());
    assertTrue(taskService.collectContributions(List.of(failing), request()).taskIds().isEmpty());
  }

  /**
   * Answers are merged in the contributors' order: an id once, its first
   * contributor's line kept, a null answer or id ignored, and at most
   * {@value TaskServiceImpl#MAX_CONTRIBUTED_TASKS} ids in all. Mutants: the
   * cap removed, the first line replaced by a later one.
   */
  @Test
  public void testContributionsMergedInOrderAndCapped() {
    SearchFieldMatches first = new SearchFieldMatches("a", "a", List.of(), 0, "");
    SearchFieldMatches second = new SearchFieldMatches("b", "b", List.of(), 0, "");
    TaskSearchMatchPlugin one = mock(TaskSearchMatchPlugin.class);
    when(one.search(any())).thenReturn(new TaskSearchMatches(java.util.Arrays.asList(1L, null, 2L), Map.of(1L, first)));
    TaskSearchMatchPlugin nothing = mock(TaskSearchMatchPlugin.class);
    when(nothing.search(any())).thenReturn(null);
    TaskSearchMatchPlugin two = mock(TaskSearchMatchPlugin.class);
    when(two.search(any())).thenReturn(new TaskSearchMatches(List.of(2L, 1L, 3L), Map.of(1L, second, 3L, second)));

    TaskSearchMatches merged = taskService.collectContributions(List.of(one, nothing, two), request());

    assertEquals(List.of(1L, 2L, 3L), merged.taskIds());
    assertSame(first, merged.matches().get(1L));
    assertSame(second, merged.matches().get(3L));

    TaskSearchMatchPlugin many = mock(TaskSearchMatchPlugin.class);
    when(many.search(any())).thenReturn(new TaskSearchMatches(LongStream.rangeClosed(1, TaskServiceImpl.MAX_CONTRIBUTED_TASKS + 5L)
                                                                        .boxed()
                                                                        .toList(),
                                                              Map.of()));
    assertEquals(TaskServiceImpl.MAX_CONTRIBUTED_TASKS, taskService.collectContributions(List.of(many), request()).taskIds().size());
  }

  /**
   * With no contributor, the search runs with no contributed id.
   */
  @Test
  public void testNoContributor() {
    doReturn(List.of()).when(taskService).getSearchMatchPlugins();
    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenReturn(List.of());
    TaskSearchFilter filter = new TaskSearchFilter("user1", "x", List.of("user1"), null, null, 0, 10);

    TaskKeywordSearchPage page = taskService.searchTasksByKeyword(filter, new Identity("user1"), null, Locale.ENGLISH);

    assertTrue(filter.getContributedIds().isEmpty());
    assertTrue(page.matches().isEmpty());
  }

  /**
   * @return a request for the merge tests
   */
  private static TaskSearchMatchRequest request() {
    return new TaskSearchMatchRequest(new Identity("user1"), "x", List.of(), 10, Locale.ENGLISH);
  }

}
