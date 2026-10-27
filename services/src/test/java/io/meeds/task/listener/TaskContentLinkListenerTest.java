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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.service.TasksRemovedPayload;
import org.exoplatform.task.util.TaskUtil;

import io.meeds.social.html.model.HtmlProcessorContext;
import io.meeds.social.html.utils.HtmlUtils;
import io.meeds.task.plugin.TaskContentLinkPlugin;

/**
 * Checks that the tasks a project removal deleted lose their content links.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskContentLinkListenerTest {

  @Mock
  private PortalContainer         container;

  @Mock
  private ListenerService         listenerService;

  @InjectMocks
  private TaskContentLinkListener listener;

  /**
   * The listener is bound to {@link TaskUtil#TASKS_REMOVED}: without that
   * binding it never hears of a project removal.
   */
  @Test
  public void testListensToTasksRemoved() {
    when(container.getComponentInstanceOfType(ListenerService.class)).thenReturn(listenerService);

    listener.init();

    verify(listenerService).addListener(TaskUtil.TASKS_REMOVED, listener);
  }

  /**
   * The content links of each task of a project removal are cleared.
   */
  @Test
  public void testClearsTheContentLinksOfEveryRemovedTask() throws Exception {
    try (MockedStatic<HtmlUtils> htmlUtils = mockStatic(HtmlUtils.class)) {
      listener.onEvent(new Event<>(TaskUtil.TASKS_REMOVED, 3L, new TasksRemovedPayload(List.of(3L), List.of(11L, 12L))));

      ArgumentCaptor<HtmlProcessorContext> context = ArgumentCaptor.forClass(HtmlProcessorContext.class);
      htmlUtils.verify(() -> HtmlUtils.process(eq(""), context.capture()), times(2));
      assertEquals(List.of("11", "12"), context.getAllValues().stream().map(HtmlProcessorContext::getObjectId).toList());
      context.getAllValues().forEach(value -> assertEquals(TaskContentLinkPlugin.OBJECT_TYPE, value.getObjectType()));
    }
  }

}
