package org.exoplatform.task.integration.gamification;

import static org.exoplatform.addons.gamification.GamificationConstant.OBJECT_ID_PARAM;
import static org.exoplatform.addons.gamification.GamificationConstant.OBJECT_TYPE_PARAM;
import static org.exoplatform.addons.gamification.GamificationConstant.RECEIVER_ID;
import static org.exoplatform.addons.gamification.GamificationConstant.EVENT_NAME;
import static org.exoplatform.addons.gamification.GamificationConstant.SENDER_ID;
import static org.exoplatform.addons.gamification.listener.generic.GamificationGenericListener.GENERIC_EVENT_NAME;
import static org.exoplatform.task.util.TaskUtil.TASK_OBJECT_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.addons.gamification.service.configuration.RuleService;
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

  protected RuleService       ruleService;

  protected IdentityManager   identityManager;

  protected ListenerService   listenerService;

  public GamificationTaskCommentListener(RuleService ruleService,
                                         IdentityManager identityManager,
                                         ListenerService listenerService) {
    this.ruleService = ruleService;
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
