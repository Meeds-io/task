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
import java.util.Set;

import org.exoplatform.addons.gamification.service.configuration.RuleService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskPayload;
import org.exoplatform.task.service.TaskService;

public class GamificationTaskUpdateListener extends Listener<TaskService, TaskPayload> {

  private static final String GAMIFICATION_TASK_ADDON_CREATE_TASK             = "createNewTask";

  private static final String GAMIFICATION_TASK_ADDON_COMPLETED_TASK_ASSIGNED = "completeTaskAssigned";

  private static final String GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER = "completeTaskCoworker";

  private static final String GAMIFICATION_TASK_ADDON_UPDATE_TASK             = "updateTask";

  protected RuleService       ruleService;

  protected IdentityManager   identityManager;

  protected ListenerService   listenerService;

  public GamificationTaskUpdateListener(RuleService ruleService,
                                        IdentityManager identityManager,
                                        ListenerService listenerService) {
    this.ruleService = ruleService;
    this.identityManager = identityManager;
    this.listenerService = listenerService;
  }

  @Override
  public void onEvent(Event<TaskService, TaskPayload> event) throws Exception {

    TaskPayload data = event.getData();

    TaskDto oldTask = data.before();
    TaskDto newTask = data.after();

    // New Task has been created
    if (oldTask == null && newTask != null) {
      createTask(newTask);
    }

    // Update Task
    if (oldTask != null && newTask != null) {
      updateTask(oldTask, newTask);
    }

  }

  protected void createTask(TaskDto task) {
    String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();

    // Compute user id
    String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();
    createGamificationRealization(actorId,
                                  actorId,
                                  GAMIFICATION_TASK_ADDON_CREATE_TASK,
                                  String.valueOf(task.getId()));
  }

  protected void updateTask(TaskDto before, TaskDto after) {
    String actorId;
    // Task completed
    if (isDiff(before.isCompleted(), after.isCompleted())) {
      // Get task assigned property
      if (after.getAssignee() != null && after.getAssignee().length() != 0) {
        // Compute user id
        actorId = identityManager.getOrCreateUserIdentity(after.getAssignee()).getId();
        createGamificationRealization(actorId,
                                      actorId,
                                      GAMIFICATION_TASK_ADDON_COMPLETED_TASK_ASSIGNED,
                                      String.valueOf(after.getId()));
      }
      // Manage coworker
      Set<String> coworkers = after.getCoworker();
      // Get task assigned property
      if (coworkers == null) {
        return;
      }
      for (String coworker : coworkers) {
        // Compute user id
        actorId = identityManager.getOrCreateUserIdentity(coworker).getId();
        createGamificationRealization(actorId,
                                      actorId,
                                      GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER,
                                      String.valueOf(after.getId()));
      }
    } else { // Update a task regardless le action made by a user

      // Get connected user
      String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();

      // Compute user id
      actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();

      createGamificationRealization(actorId,
                                    actorId,
                                    GAMIFICATION_TASK_ADDON_UPDATE_TASK,
                                    String.valueOf(after.getId()));
    }
  }

  /**
   * Check whether a task has been updated
   *
   * @param  before : Task data before modification
   * @param  after  : Task data after modification
   * @return        a boolean if task has been changed false else
   */
  protected boolean isDiff(Object before, Object after) {
    if (before == after) {
      return false;
    }
    if (before != null) {
      return !before.equals(after);
    } else {
      return true;
    }
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
