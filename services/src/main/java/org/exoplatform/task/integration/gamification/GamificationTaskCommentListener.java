package org.exoplatform.task.integration.gamification;

import org.exoplatform.addons.gamification.service.configuration.RuleService;
import org.exoplatform.addons.gamification.service.effective.GamificationService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;

import static org.exoplatform.task.util.TaskUtil.TASK_OBJECT_TYPE;

public class GamificationTaskCommentListener extends Listener<TaskService, CommentDto> {

  private static final String   GAMIFICATION_TASK_ADDON_COMMENT_TASK = "commentTask";

  protected RuleService         ruleService;

  protected IdentityManager     identityManager;

  protected GamificationService gamificationService;

  public GamificationTaskCommentListener(RuleService ruleService,
                                         IdentityManager identityManager,
                                         GamificationService gamificationService) {
    this.ruleService = ruleService;
    this.identityManager = identityManager;
    this.gamificationService = gamificationService;
  }

  @Override
  public void onEvent(Event<TaskService, CommentDto> event) {
    String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();
    TaskDto task = event.getData().getTask();
    // Compute user id
    String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();

    gamificationService.createHistory(GAMIFICATION_TASK_ADDON_COMMENT_TASK,
                                      actorId,
                                      actorId,
                                      String.valueOf(task.getId()),
                                      TASK_OBJECT_TYPE);
  }
}
