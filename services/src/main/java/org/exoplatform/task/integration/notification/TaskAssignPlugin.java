/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.task.integration.notification;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.task.dto.TaskDto;

public class TaskAssignPlugin extends AbstractNotificationPlugin {
  
  public TaskAssignPlugin(InitParams initParams) {
    super(initParams);
  }

  public static final String ID = "TaskAssignPlugin";
  
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    TaskDto task = ctx.value(NotificationUtils.TASK);
    return task.getAssignee() != null && !task.getAssignee().isEmpty();
  }

  @Override
  protected Set<String> getReceiver(TaskDto task, NotificationContext ctx) {
    Set<String> receivers = new HashSet<String>();
    if (task.getAssignee() != null) {
      receivers.add(task.getAssignee());
    }
    if(ctx != null) {
      receivers.remove(ctx.value(NotificationUtils.CREATOR));
    }
    return receivers;
  }
  
}
