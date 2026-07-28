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

import static org.exoplatform.task.util.TaskUtil.TASK_COMMENT_CREATED;
import static org.exoplatform.task.util.TaskUtil.TASK_COMMENT_DELETED;
import static org.exoplatform.task.util.TaskUtil.TASK_COMMENT_UPDATED;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;

import io.meeds.social.html.model.HtmlProcessorContext;
import io.meeds.social.html.utils.HtmlUtils;
import io.meeds.task.plugin.TaskCommentAclPlugin;

import jakarta.annotation.PostConstruct;

@Component
public class TaskCommentContentLinkListener extends Listener<TaskDto, CommentDto> {

  private static final List<String> SUPPORTED_EVENTS = Arrays.asList(TASK_COMMENT_CREATED,
                                                                     TASK_COMMENT_UPDATED,
                                                                     TASK_COMMENT_DELETED);

  @Autowired
  private PortalContainer           container;

  @PostConstruct
  public void init() {
    ListenerService listenerService = container.getComponentInstanceOfType(ListenerService.class);
    SUPPORTED_EVENTS.forEach(name -> listenerService.addListener(name, this));
  }

  @Override
  public void onEvent(Event<TaskDto, CommentDto> event) throws Exception {
    String content = switch (event.getEventName()) {
    case TASK_COMMENT_DELETED: {
      yield "";
    }
    default:
      yield event.getData().getComment();
    };
    HtmlUtils.process(content,
                      new HtmlProcessorContext(TaskCommentAclPlugin.OBJECT_TYPE,
                                               String.valueOf(event.getData().getId()),
                                               null));
  }

}
