package org.exoplatform.task.integration.gamification;

import static io.meeds.gamification.constant.GamificationConstant.EVENT_NAME;
import static io.meeds.gamification.constant.GamificationConstant.OBJECT_ID_PARAM;
import static io.meeds.gamification.constant.GamificationConstant.OBJECT_TYPE_PARAM;
import static io.meeds.gamification.constant.GamificationConstant.RECEIVER_ID;
import static io.meeds.gamification.constant.GamificationConstant.SENDER_ID;
import static io.meeds.gamification.listener.GamificationGenericListener.GENERIC_EVENT_NAME;
import static org.exoplatform.task.util.TaskUtil.TASK_OBJECT_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskPayload;
import org.exoplatform.task.service.TaskService;

public class GamificationTaskUpdateListener extends Listener<TaskService, TaskPayload> {

  public static final String GAMIFICATION_TASK_ADDON_CREATE_TASK             = "createNewTask";

  public static final String GAMIFICATION_TASK_ADDON_COMPLETED_TASK_ASSIGNED = "completeTaskAssigned";

  public static final String GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER = "completeTaskCoworker";

  public static final String GAMIFICATION_TASK_ADDON_COMPLETED_TASK          = "completeTask";

  public static final String GAMIFICATION_TASK_ADDON_UPDATE_TASK             = "updateTask";

  protected TaskService      taskService;

  protected IdentityManager  identityManager;

  protected ListenerService  listenerService;

  public GamificationTaskUpdateListener(TaskService taskService,
                                        IdentityManager identityManager,
                                        ListenerService listenerService) {
    this.taskService = taskService;
    this.identityManager = identityManager;
    this.listenerService = listenerService;
  }

  @Override
  public void onEvent(Event<TaskService, TaskPayload> event) throws Exception {

    TaskPayload data = event.getData();

    TaskDto oldTask = data.before();
    TaskDto newTask = data.after();

    if (oldTask == null && newTask != null) {
      createTask(newTask);
    } else if (oldTask != null && newTask != null) {
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

  protected void updateTask(TaskDto before, TaskDto after) throws Exception {
    String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();
    String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();
    long taskId = after.getId();

    if (!before.isCompleted() && after.isCompleted()) {// Task marked as
                                                       // completed
      List<ChangeLogEntry> taskLogs = taskService.getTaskLogs(taskId, 0, -1);
      createGamificationRealization(taskId, GAMIFICATION_TASK_ADDON_COMPLETED_TASK_ASSIGNED, taskLogs, "assign");
      createGamificationRealization(taskId, GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER, taskLogs, "assignCoworker");
      createGamificationRealization(actorId,
                                    actorId,
                                    GAMIFICATION_TASK_ADDON_COMPLETED_TASK,
                                    String.valueOf(taskId));
    } else {
      createGamificationRealization(actorId,
                                    actorId,
                                    GAMIFICATION_TASK_ADDON_UPDATE_TASK,
                                    String.valueOf(taskId));
    }
  }

  private void createGamificationRealization(long taskId, String eventName, List<ChangeLogEntry> taskLogs, String taskLogName) {
    Set<String> usernames = taskLogs.stream()
                                    .filter(taskLog -> StringUtils.equals(taskLogName, taskLog.getActionName()))
                                    .map(ChangeLogEntry::getTarget)
                                    .collect(Collectors.toSet());
    createGamificationRealization(taskId, usernames, eventName);
  }

  private void createGamificationRealization(long taskId, Set<String> usernames, String eventName) {
    usernames.forEach(username -> {
      Identity identity = identityManager.getOrCreateUserIdentity(username);
      if (identity == null || !identity.isEnable() || identity.isDeleted()) {
        return;
      }
      createGamificationRealization(identity.getId(),
                                    identity.getId(),
                                    eventName,
                                    String.valueOf(taskId));
    });
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
