package org.exoplatform.task.integration.gamification;

import static io.meeds.gamification.constant.GamificationConstant.EVENT_NAME;
import static io.meeds.gamification.constant.GamificationConstant.OBJECT_ID_PARAM;
import static io.meeds.gamification.constant.GamificationConstant.OBJECT_TYPE_PARAM;
import static io.meeds.gamification.constant.GamificationConstant.RECEIVER_ID;
import static io.meeds.gamification.constant.GamificationConstant.SENDER_ID;
import static io.meeds.gamification.listener.GamificationGenericListener.GENERIC_EVENT_NAME;
import static org.exoplatform.task.util.TaskUtil.TASK_OBJECT_TYPE;

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

  private static final String GAMIFICATION_TASK_ADDON_COMMENT_TASK = "commentTask";

  protected IdentityManager   identityManager;

  protected ListenerService   listenerService;

  public GamificationTaskCommentListener(IdentityManager identityManager,
                                         ListenerService listenerService) {
    this.identityManager = identityManager;
    this.listenerService = listenerService;
  }

  @Override
  public void onEvent(Event<TaskService, CommentDto> event) {
    String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();
    TaskDto task = event.getData().getTask();
    // Compute user id
    String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();

    createGamificationRealization(actorId,
                                  actorId,
                                  GAMIFICATION_TASK_ADDON_COMMENT_TASK,
                                  String.valueOf(task.getId()));
  }

  private void createGamificationRealization(String earnerIdentityId,
                                             String receiverId,
                                             String gamificationEventName,
                                             String taskId) {
    Map<String, String> gam = new HashMap<>();
    try {
      gam.put(EVENT_NAME, gamificationEventName);
      gam.put(OBJECT_ID_PARAM, taskId);
      gam.put(OBJECT_TYPE_PARAM, TASK_OBJECT_TYPE);
      gam.put(SENDER_ID, earnerIdentityId);
      gam.put(RECEIVER_ID, receiverId);
      listenerService.broadcast(GENERIC_EVENT_NAME, gam, null);
    } catch (Exception e) {
      throw new IllegalStateException("Error triggering Gamification Listener Event: " + gam, e);
    }
  }

}
