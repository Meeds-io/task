/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.integration.notification;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.TaskDto;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 *
 */
public class TaskAssignPluginTest {

  @Test
  public void shouldReturnAssigneeInReceivers() throws Exception {
    // Given
    TaskDto task = new TaskDto();
    task.setAssignee("user1");

    AbstractNotificationPlugin notificationPlugin = new TaskAssignPlugin(new InitParams());

    // When
    Set<String> receivers = notificationPlugin.getReceiver(task, null);

    // Then
    assertNotNull(receivers);
    assertEquals(1, receivers.size());
    assertEquals("user1", receivers.iterator().next());
  }

  @Test
  public void shouldNotReturnAssigneeInReceiversWhenAssigneeIsCreator() throws Exception {
    // Given
    TaskDto task = new TaskDto();
    task.setAssignee("user1");

    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(NotificationUtils.CREATOR, "user1");

    AbstractNotificationPlugin notificationPlugin = new TaskAssignPlugin(new InitParams());

    // When
    Set<String> receivers = notificationPlugin.getReceiver(task, ctx);

    // Then
    assertNotNull(receivers);
    assertEquals(0, receivers.size());
  }
}