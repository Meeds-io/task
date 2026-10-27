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
package io.meeds.task.listener;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.service.TasksRemovedPayload;
import org.exoplatform.task.storage.search.TaskIndexingServiceConnector;
import org.exoplatform.task.util.TaskUtil;

/**
 * Checks that the tasks a project removal deleted leave the search index.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskIndexingListenerTest {

  @Mock
  private ListenerService      listenerService;

  @Mock
  private IndexingService      indexingService;

  @InjectMocks
  private TaskIndexingListener listener;

  /**
   * The listener is bound to {@link TaskUtil#TASKS_REMOVED}: without that
   * binding it never hears of a project removal.
   */
  @Test
  public void testListensToTasksRemoved() {
    listener.init();

    verify(listenerService).addListener(TaskUtil.TASKS_REMOVED, listener);
  }

  /**
   * Each task of a project removal is unindexed.
   */
  @Test
  public void testUnindexesEveryRemovedTask() throws Exception {
    listener.onEvent(new Event<>(TaskUtil.TASKS_REMOVED, 3L, new TasksRemovedPayload(List.of(3L), List.of(11L, 12L))));

    verify(indexingService).unindex(TaskIndexingServiceConnector.TYPE, "11");
    verify(indexingService).unindex(TaskIndexingServiceConnector.TYPE, "12");
  }

}
