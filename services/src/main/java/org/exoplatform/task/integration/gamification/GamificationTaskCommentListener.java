package org.exoplatform.task.integration.gamification;

import static io.meeds.gamification.constant.GamificationConstant.*;
import static io.meeds.gamification.listener.GamificationGenericListener.DELETE_EVENT_NAME;
import static io.meeds.gamification.listener.GamificationGenericListener.GENERIC_EVENT_NAME;
import static org.exoplatform.task.util.TaskUtil.*;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

public class GamificationTaskCommentListener extends Listener<TaskService, CommentDto> {

  protected IdentityManager identityManager;

  protected ListenerService listenerService;

  public GamificationTaskCommentListener(IdentityManager identityManager, ListenerService listenerService) {
    this.identityManager = identityManager;
    this.listenerService = listenerService;
  }

  @Override
  public void onEvent(Event<TaskService, CommentDto> event) throws Exception {
    String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();
    TaskDto task = event.getData().getTask();
    // Compute user id
    String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();
    long projectId = task.getStatus() == null || task.getStatus().getProject() == null ? 0 : task.getStatus().getProject().getId();
    Map<String, String> gam = new HashMap<>();
    gam.put(EVENT_NAME, GAMIFICATION_TASK_ADDON_COMMENT_TASK);
    gam.put(OBJECT_ID_PARAM, String.valueOf(task.getId()));
    gam.put(OBJECT_TYPE_PARAM, TASK_OBJECT_TYPE);
    gam.put(SENDER_ID, actorId);
    gam.put(RECEIVER_ID, actorId);
    if (projectId > 0) {
      gam.put(EVENT_DETAILS_PARAM, "{" + PROJECT_ID + ": " + projectId + "}");
    }

    String gamificationEventName = event.getEventName().equals(TASK_COMMENT_DELETED) ? DELETE_EVENT_NAME : GENERIC_EVENT_NAME;
    listenerService.broadcast(gamificationEventName, gam, String.valueOf(task.getId()));

  }
}
