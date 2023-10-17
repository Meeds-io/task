/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.integration;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.attachment.model.ObjectAttachmentId;
import org.exoplatform.task.service.TaskService;

public class TaskAttachmentLoggingListener extends Listener<String, ObjectAttachmentId> {

  public static final String ATTACHMENT_TASK_OBJECT_TYPE = "task";

  public static final String ATTACHMENT_CREATED_EVENT = "attachment.created";

  public static final String ATTACHMENT_DELETED_EVENT = "attachment.deleted";

  private final TaskService taskService;

  public TaskAttachmentLoggingListener(TaskService taskService) {
    this.taskService = taskService;
  }

  @Override
  public void onEvent(Event<String, ObjectAttachmentId> event) throws Exception {
    String username = event.getSource();
    ObjectAttachmentId objectAttachment = event.getData();

    if (objectAttachment != null && ATTACHMENT_TASK_OBJECT_TYPE.equals(objectAttachment.getObjectType())) {
      switch (event.getEventName()) {
      case ATTACHMENT_CREATED_EVENT: {
        taskService.addTaskLog(Long.parseLong(objectAttachment.getObjectId()), username, "attach_image", "");
        break;
      }
      case ATTACHMENT_DELETED_EVENT: {
        taskService.addTaskLog(Long.parseLong(objectAttachment.getObjectId()), username, "delete_image", "");
        break;
      }
      default:
        throw new IllegalArgumentException("Unexpected listener event name: " + event.getEventName());
      }
    }
  }
}
