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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.social.reaction.model.Reaction;
import io.meeds.social.reaction.service.ReactionService;
import io.meeds.task.service.TaskReactionNotificationService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TaskReactionNotificationListenerTest {

  @Mock
  private PortalContainer                  container;

  @Mock
  private TaskReactionNotificationService  notificationService;

  @InjectMocks
  private TaskReactionNotificationListener listener;

  @Test
  public void initRegistersTheReactionCreationEvent() {
    ListenerService listenerService = mock(ListenerService.class);
    when(container.getComponentInstanceOfType(ListenerService.class)).thenReturn(listenerService);

    listener.init();

    verify(listenerService).addListener(ReactionService.REACTION_CREATED_EVENT_NAME, listener);
  }

  @Test
  public void theCreatedReactionIsHandedToTheService() throws Exception {
    Reaction reaction = new Reaction(1L, "like", "taskComment", "5");

    listener.onEvent(new Event<>(ReactionService.REACTION_CREATED_EVENT_NAME, reaction, "john"));

    verify(notificationService).notifyReaction(reaction, "john");
  }

}
