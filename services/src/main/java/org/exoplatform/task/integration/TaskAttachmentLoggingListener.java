package org.exoplatform.task.integration;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.attachment.model.ObjectAttachmentId;

public class TaskAttachmentLoggingListener extends Listener<String, ObjectAttachmentId> {

  public static final String ATTACHMENT_TASK_OBJECT_TYPE = "task";

  private org.exoplatform.task.service.TaskService taskService;

  public TaskAttachmentLoggingListener(org.exoplatform.task.service.TaskService taskService) {
    this.taskService = taskService;
  }
  @Override
  public void onEvent(Event<String, ObjectAttachmentId> event) throws Exception {
    String username = event.getSource();
    ObjectAttachmentId objectAttachment = event.getData();

    if (objectAttachment == null) {
      return;
    }
    if (!objectAttachment.getObjectType().equals(ATTACHMENT_TASK_OBJECT_TYPE)) {
      return;
    }
    taskService.addTaskLog(Long.parseLong(objectAttachment.getObjectId()), username, "attach_image", "");
  }
}
