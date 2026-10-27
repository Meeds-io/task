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
package io.meeds.task.listener;

import jakarta.annotation.PostConstruct;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskPayload;
import org.exoplatform.task.service.TasksRemovedPayload;
import org.exoplatform.task.util.TaskUtil;
import org.exoplatform.task.storage.search.TaskIndexingServiceConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.exoplatform.task.util.TaskUtil.TASK_CREATED;
import static org.exoplatform.task.util.TaskUtil.TASKS_REMOVED;
import static org.exoplatform.task.util.TaskUtil.TASK_DELETED;
import static org.exoplatform.task.util.TaskUtil.TASK_UPDATED;

@Asynchronous
@Component
public class TaskIndexingListener extends Listener<Object, Object> {

  private String[]        LISTENERS = { TASK_CREATED, TASK_UPDATED, TASK_DELETED, TASKS_REMOVED };

  @Autowired
  private ListenerService listenerService;

  @Autowired
  private IndexingService indexingService;

  @PostConstruct
  public void init() {
    for (String listener : LISTENERS) {
      listenerService.addListener(listener, this);
    }
  }

  /**
   * Indexes, reindexes or unindexes the task of a task event, and unindexes
   * every task a project removal deleted.
   *
   * @param event a task event with a {@link TaskPayload}, or a
   *          {@link TaskUtil#TASKS_REMOVED} event with a
   *          {@link TasksRemovedPayload}
   */
  @Override
  public void onEvent(Event<Object, Object> event) throws Exception {
    if (TASKS_REMOVED.equals(event.getEventName())) {
      ((TasksRemovedPayload) event.getData()).taskIds()
                                             .forEach(taskId -> indexingService.unindex(TaskIndexingServiceConnector.TYPE,
                                                                                        String.valueOf(taskId)));
      return;
    }
    TaskPayload data = (TaskPayload) event.getData();
    TaskDto newTask = data.after();
    switch (event.getEventName()) {
    case TASK_CREATED -> indexingService.index(TaskIndexingServiceConnector.TYPE, String.valueOf(newTask.getId()));
    case TASK_UPDATED -> indexingService.reindex(TaskIndexingServiceConnector.TYPE, String.valueOf(newTask.getId()));
    case TASK_DELETED -> indexingService.unindex(TaskIndexingServiceConnector.TYPE, String.valueOf(newTask.getId()));
    default -> throw new IllegalArgumentException("Unexpected listener event name: " + event.getEventName());
    }
  }
}
