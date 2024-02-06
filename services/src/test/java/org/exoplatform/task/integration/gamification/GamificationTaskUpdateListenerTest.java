/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
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
package org.exoplatform.task.integration.gamification;

import static io.meeds.gamification.constant.GamificationConstant.EVENT_NAME;
import static io.meeds.gamification.constant.GamificationConstant.SENDER_ID;
import static io.meeds.gamification.listener.GamificationGenericListener.GENERIC_EVENT_NAME;
import static org.exoplatform.task.util.TaskUtil.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskPayload;
import org.exoplatform.task.service.TaskService;

@RunWith(MockitoJUnitRunner.class)
public class GamificationTaskUpdateListenerTest {

  private static final long   TASK_ID   = 2L;

  private static final String USERNAME  = "test";

  private static final String COWORKER1 = "cotest1";

  private static final String COWORKER2 = "cotest2";

  private static final String COWORKER3 = "cotest3";

  @Mock
  private TaskService         taskService;

  @Mock
  private IdentityManager     identityManager;

  @Mock
  private ListenerService     listenerService;

  @Mock
  private TaskDto             oldTask;

  @Mock
  private TaskDto             task;

  @Before
  public void setUp() {
    when(task.getId()).thenReturn(TASK_ID);
    ConversationState.setCurrent(new ConversationState(new org.exoplatform.services.security.Identity(USERNAME)));
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  @Test
  public void testCreateTask() throws Exception {
    GamificationTaskUpdateListener gamificationTaskUpdateListener = new GamificationTaskUpdateListener(taskService,
                                                                                                       identityManager,
                                                                                                       listenerService);
    Identity userIdentity = mock(Identity.class);
    when(userIdentity.getId()).thenReturn("1");
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(userIdentity);
    Event<TaskService, TaskPayload> event = new Event<TaskService, TaskPayload>(TASK_CREATED, null, new TaskPayload(null, task));
    gamificationTaskUpdateListener.onEvent(event);

    verify(listenerService,
           times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                               argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                             .equals(GAMIFICATION_TASK_ADDON_CREATE_TASK)),
                               eq("2"));

  }

  @Test
  public void testUpdateTask() throws Exception {
    mockIdentity("1", USERNAME);
    mockIdentity("2", COWORKER1);
    mockIdentity("3", COWORKER2);
    mockIdentity("4", COWORKER3);

    when(taskService.getTaskLogs(eq(TASK_ID), anyInt(), anyInt())).thenReturn(Arrays.asList(newChangeLogEntry("assign", USERNAME),
                                                                                            newChangeLogEntry("assignCoworker",
                                                                                                              COWORKER1),
                                                                                            newChangeLogEntry("assignCoworker",
                                                                                                              COWORKER2),
                                                                                            newChangeLogEntry("assignCoworker",
                                                                                                              COWORKER3)));

    new GamificationTaskUpdateListener(taskService,
                                       identityManager,
                                       listenerService).onEvent(new Event<TaskService, TaskPayload>(TASK_UPDATED, null, new TaskPayload(oldTask, task)));

    verify(listenerService,
           times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                               argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                             .equals(GAMIFICATION_TASK_ADDON_UPDATE_TASK)),
                               eq("2"));
    verify(listenerService,
           never()).broadcast(eq(GENERIC_EVENT_NAME),
                              argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                            .equals(GAMIFICATION_TASK_ADDON_COMPLETED_TASK)),
                              eq(TASK_ID));

    when(task.isCompleted()).thenReturn(true);
    new GamificationTaskUpdateListener(taskService,
                                       identityManager,
                                       listenerService).onEvent(new Event<TaskService, TaskPayload>(TASK_UPDATED, null, new TaskPayload(oldTask, task)));

    verify(listenerService,
           times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                               argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                             .equals(GAMIFICATION_TASK_ADDON_UPDATE_TASK)),
                               eq("2"));

    verify(listenerService,
           times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                               argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                             .equals(GAMIFICATION_TASK_ADDON_COMPLETED_TASK_ASSIGNED)),
                               eq("2"));
    verify(listenerService,
           times(3)).broadcast(eq(GENERIC_EVENT_NAME),
                               argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                             .equals(GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER)),
                               eq("2"));
    verify(listenerService,
           times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                               argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                             .equals(GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER)
                                   && source.get(SENDER_ID).equals("2")),
                               eq("2"));
    verify(listenerService,
           times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                               argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                             .equals(GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER)
                                   && source.get(SENDER_ID).equals("3")),
                               eq("2"));
    verify(listenerService,
           times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                               argThat((Map<String, String> source) -> source.get(EVENT_NAME)
                                                                             .equals(GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER)
                                   && source.get(SENDER_ID).equals("4")),
                               eq("2"));
  }

  private ChangeLogEntry newChangeLogEntry(String logName, String target) {
    ChangeLogEntry changeLogEntry = new ChangeLogEntry();
    changeLogEntry.setTarget(target);
    changeLogEntry.setActionName(logName);
    return changeLogEntry;
  }

  private void mockIdentity(String identityId, String username) {
    Identity userIdentity = mock(Identity.class);
    when(userIdentity.getId()).thenReturn(identityId);
    when(userIdentity.isEnable()).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(username)).thenReturn(userIdentity);
  }

}
