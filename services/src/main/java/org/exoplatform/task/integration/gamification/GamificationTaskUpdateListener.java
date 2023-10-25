package org.exoplatform.task.integration.gamification;

import static io.meeds.gamification.constant.GamificationConstant.EVENT_NAME;
import static io.meeds.gamification.constant.GamificationConstant.OBJECT_ID_PARAM;
import static io.meeds.gamification.constant.GamificationConstant.OBJECT_TYPE_PARAM;
import static io.meeds.gamification.constant.GamificationConstant.RECEIVER_ID;
import static io.meeds.gamification.constant.GamificationConstant.SENDER_ID;
import static io.meeds.gamification.listener.GamificationGenericListener.*;
import static org.exoplatform.task.util.TaskUtil.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.task.dto.ChangeLogEntry;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskPayload;
import org.exoplatform.task.service.TaskService;

public class GamificationTaskUpdateListener extends Listener<TaskService, TaskPayload> {

  private static final Log LOG                = ExoLogger.getLogger(GamificationTaskUpdateListener.class);

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
    switch (event.getEventName()) {
    case TASK_CREATED, TASK_DELETED -> createTask(newTask, event.getEventName());
    case TASK_UPDATED -> updateTask(oldTask, newTask, event.getEventName());
    default -> throw new IllegalArgumentException("Unexpected listener event name: " + event.getEventName());
    }
  }

  protected void createTask(TaskDto task, String eventName) throws Exception {
    String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();

    // Compute user id
    String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();
    Map<String, String> gam = buildGamificationEventDetails(GAMIFICATION_TASK_ADDON_CREATE_TASK,
                                                            actorId,
                                                            actorId,
                                                            String.valueOf(task.getId()));
    listenerService.broadcast(getGamificationEventName(eventName), gam, String.valueOf(task.getId()));
  }

  protected void updateTask(TaskDto before, TaskDto after, String eventName) throws Exception {
    String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();
    String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();
    long taskId = after.getId();
    List<ChangeLogEntry> taskLogs = taskService.getTaskLogs(taskId, 0, -1);

    if (!before.isCompleted() && after.isCompleted()) {// Task marked as
                                                       // completed
      createGamificationRealization(GENERIC_EVENT_NAME,
                                    taskId,
                                    GAMIFICATION_TASK_ADDON_COMPLETED_TASK_ASSIGNED,
                                    taskLogs,
                                    "assign");
      createGamificationRealization(GENERIC_EVENT_NAME,
                                    taskId,
                                    GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER,
                                    taskLogs,
                                    "assignCoworker");
      Map<String, String> gam = buildGamificationEventDetails(GAMIFICATION_TASK_ADDON_COMPLETED_TASK,
                                                              actorId,
                                                              actorId,
                                                              String.valueOf(taskId));
      listenerService.broadcast(GENERIC_EVENT_NAME, gam, String.valueOf(taskId));
    } else if (before.isCompleted() && !after.isCompleted()) {
      createGamificationRealization(CANCEL_EVENT_NAME,
                                    taskId,
                                    GAMIFICATION_TASK_ADDON_COMPLETED_TASK_ASSIGNED,
                                    taskLogs,
                                    "assign");
      createGamificationRealization(CANCEL_EVENT_NAME,
                                    taskId,
                                    GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER,
                                    taskLogs,
                                    "assignCoworker");
      Map<String, String> gam = buildGamificationEventDetails(GAMIFICATION_TASK_ADDON_COMPLETED_TASK,
                                                              actorId,
                                                              actorId,
                                                              String.valueOf(taskId));
      listenerService.broadcast(CANCEL_EVENT_NAME, gam, String.valueOf(taskId));
    } else {
      Map<String, String> gam = buildGamificationEventDetails(GAMIFICATION_TASK_ADDON_UPDATE_TASK,
                                                              actorId,
                                                              actorId,
                                                              String.valueOf(taskId));
      listenerService.broadcast(getGamificationEventName(eventName), gam, String.valueOf(taskId));
    }
  }

  private void createGamificationRealization(String gamificationEventName,
                                             long taskId,
                                             String eventName,
                                             List<ChangeLogEntry> taskLogs,
                                             String taskLogName) {
    Set<String> usernames = taskLogs.stream()
                                    .filter(taskLog -> StringUtils.equals(taskLogName, taskLog.getActionName()))
                                    .map(ChangeLogEntry::getTarget)
                                    .collect(Collectors.toSet());
    createGamificationRealization(gamificationEventName, taskId, usernames, eventName);
  }

  private void createGamificationRealization(String gamificationEventName, long taskId, Set<String> usernames, String eventName) {
    usernames.forEach(username -> {
      Identity identity = identityManager.getOrCreateUserIdentity(username);
      if (identity == null || !identity.isEnable() || identity.isDeleted()) {
        return;
      }
      Map<String, String> gam = buildGamificationEventDetails(eventName,
                                                              identity.getId(),
                                                              identity.getId(),
                                                              String.valueOf(taskId));
      try {
        listenerService.broadcast(gamificationEventName, gam, String.valueOf(taskId));
      } catch (Exception e) {
        LOG.warn("An error occurred while broadcasting event {}", eventName, e);
      }
    });
  }

  private String getGamificationEventName(String eventName) {
    if (eventName.equals(TASK_DELETED)) {
      return DELETE_EVENT_NAME;
    }
    return GENERIC_EVENT_NAME;
  }

  private Map<String, String> buildGamificationEventDetails(String gamificationEventName,
                                                            String earnerId,
                                                            String receiverId,
                                                            String objectId) {
    Map<String, String> gam = new HashMap<>();
    gam.put(EVENT_NAME, gamificationEventName);
    gam.put(OBJECT_ID_PARAM, objectId);
    gam.put(OBJECT_TYPE_PARAM, TASK_OBJECT_TYPE);
    gam.put(SENDER_ID, earnerId);
    gam.put(RECEIVER_ID, receiverId);
    return gam;
  }
}
