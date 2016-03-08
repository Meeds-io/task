/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.task.integration.notification;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.domain.Task;

import java.util.Set;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class TaskCommentPlugin extends AbstractNotificationPlugin {
  public static final String ID = "TaskCommentedPlugin";

  public TaskCommentPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public NotificationInfo makeNotification(NotificationContext ctx) {
    Task task = ctx.value(NotificationUtils.TASK);
    Comment comment = ctx.value(NotificationUtils.COMMENT);
    NotificationInfo info = super.makeNotification(ctx);
    info.with(NotificationUtils.TASKS, String.valueOf(task.getId()));
    info.with(NotificationUtils.COMMENT_TEXT, comment.getComment());

    // Override the activityId
    String projectId = "project.";
    if (task.getStatus() != null) {
      projectId += String.valueOf(task.getStatus().getProject().getId());
    }
    info.with(NotificationUtils.ACTIVITY_ID, projectId);

    return info;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    Set<String> receivers = (Set<String>)ctx.value(NotificationUtils.RECEIVERS);
    return receivers != null && receivers.size() > 0;
  }

  @Override
  protected Set<String> getReceiver(Task task, NotificationContext ctx) {
    return (Set<String>)ctx.value(NotificationUtils.RECEIVERS);
  }
}
