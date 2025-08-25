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

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.task.dto.TaskDto;

public class TaskCompletedPlugin extends AbstractNotificationPlugin {
  
  public TaskCompletedPlugin(InitParams initParams) {
    super(initParams);
  }

  public static final String ID = "TaskCompletedPlugin";
  
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    TaskDto task = ctx.value(NotificationUtils.TASK);
    return task.isCompleted() && ((task.getAssignee() != null && !task.getAssignee().isEmpty()) ||
        (task.getCoworker() != null && task.getCoworker().size() > 0) || (task.getWatcher() != null && task.getWatcher().size() > 0));
  }
}