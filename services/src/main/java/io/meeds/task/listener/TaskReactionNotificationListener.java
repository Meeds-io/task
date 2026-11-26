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
package io.meeds.task.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.social.reaction.model.Reaction;
import io.meeds.social.reaction.service.ReactionService;
import io.meeds.task.service.TaskReactionNotificationService;

import jakarta.annotation.PostConstruct;

/**
 * Glue between the creation of a reaction and the notification of the author
 * of a task comment, which {@link TaskReactionNotificationService} decides.
 */
@Component
public class TaskReactionNotificationListener extends Listener<Reaction, String> {

  private final PortalContainer                 container;

  private final TaskReactionNotificationService notificationService;

  @Autowired
  public TaskReactionNotificationListener(PortalContainer container,
                                          TaskReactionNotificationService notificationService) {
    this.container = container;
    this.notificationService = notificationService;
  }

  @PostConstruct
  public void init() {
    container.getComponentInstanceOfType(ListenerService.class)
             .addListener(ReactionService.REACTION_CREATED_EVENT_NAME, this);
  }

  @Override
  public void onEvent(Event<Reaction, String> event) throws Exception {
    notificationService.notifyReaction(event.getSource(), event.getData());
  }

}
