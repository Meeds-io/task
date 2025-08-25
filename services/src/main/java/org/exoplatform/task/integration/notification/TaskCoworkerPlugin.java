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
package org.exoplatform.task.integration.notification;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.task.dto.TaskDto;

public class TaskCoworkerPlugin extends AbstractNotificationPlugin {
  
  public TaskCoworkerPlugin(InitParams initParams) {
    super(initParams);
  }

  public static final String ID = "TaskCoworkerPlugin";
  
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    Set<String> coworkers = getReceiver(null, ctx);
    return coworkers != null && coworkers.size() > 0;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Set<String> getReceiver(TaskDto task, NotificationContext ctx) {
    Set<String> receivers = null;
    if(ctx != null) {
      receivers = (Set<String>) ctx.value(NotificationUtils.COWORKER);
    }

    if(receivers == null) {
      receivers = new HashSet<>();
    } else if(ctx != null) {
      receivers.remove(ctx.value(NotificationUtils.CREATOR));
    }

    return receivers;
  }
}
