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
package org.exoplatform.task.integration.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.container.xml.InitParams;

public class TaskReactionPluginTest {

  private final TaskReactionPlugin plugin = new TaskReactionPlugin(new InitParams());

  @Test
  public void idIsStable() {
    assertEquals("TaskReactionPlugin", plugin.getId());
  }

  @Test
  public void isValidOnlyWithReceivers() {
    NotificationContext ctx = mock(NotificationContext.class);
    assertFalse(plugin.isValid(ctx));

    when(ctx.value(NotificationUtils.RECEIVERS)).thenReturn(Set.of());
    assertFalse(plugin.isValid(ctx));

    when(ctx.value(NotificationUtils.RECEIVERS)).thenReturn(Set.of("author"));
    assertTrue(plugin.isValid(ctx));
  }

  @Test
  public void receiversAreTheOnesOfTheContext() {
    NotificationContext ctx = mock(NotificationContext.class);
    when(ctx.value(NotificationUtils.RECEIVERS)).thenReturn(Set.of("author"));

    assertTrue(plugin.getReceiver(null, ctx).contains("author"));
  }

}
