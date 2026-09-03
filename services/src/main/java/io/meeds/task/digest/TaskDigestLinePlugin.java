/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.task.digest;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;

import io.meeds.commons.digest.model.DigestItem;
import io.meeds.commons.digest.model.DigestLine;
import io.meeds.commons.digest.plugin.DigestLineContext;
import io.meeds.commons.digest.plugin.DigestLinePlugin;

/**
 * The digest email lines of the task notifications: assigned, added as
 * coworker, mentioned. The task and its project are read fresh from the
 * stored task id; a deleted task gives no line.
 */
public class TaskDigestLinePlugin extends DigestLinePlugin {

  public static final String  TASK_ASSIGN_PLUGIN    = "TaskAssignPlugin";

  public static final String  TASK_COWORKER_PLUGIN  = "TaskCoworkerPlugin";

  public static final String  TASK_MENTIONED_PLUGIN = "TaskMentionedPlugin";

  /** The stored parameters, the same names as in the notifications */
  static final String         TASK_ID_PARAM         = "taskId";

  static final String         CREATOR_PARAM         = "creator";

  static final String         TASK_URL_PARAM        = "taskUrl";

  private static final String LINE_KEY_PREFIX       = "digest.line.";

  private TaskService         taskService;

  private IdentityManager     identityManager;

  public TaskDigestLinePlugin(InitParams params) {
    super(params);
  }

  TaskDigestLinePlugin(InitParams params, TaskService taskService, IdentityManager identityManager) {
    super(params);
    this.taskService = taskService;
    this.identityManager = identityManager;
  }

  @Override
  public DigestLine buildLine(DigestItem item, DigestLineContext context) {
    TaskDto task = findTask(item.getParam(TASK_ID_PARAM));
    if (task == null) {
      return null;
    }
    String key = LINE_KEY_PREFIX + item.getPluginId();
    String project = task.getStatus() == null || task.getStatus().getProject() == null ? ""
                                                                                        : task.getStatus().getProject().getName();
    String url = url(item, task);
    return switch (item.getPluginId()) {
      case TASK_ASSIGN_PLUGIN -> DigestLine.of(key, task.getTitle(), project).withUrl(url);
      case TASK_COWORKER_PLUGIN, TASK_MENTIONED_PLUGIN ->
        DigestLine.of(key, fullName(item.getParam(CREATOR_PARAM)), task.getTitle(), project).withUrl(url);
      default -> null;
    };
  }

  private TaskDto findTask(String taskId) {
    if (StringUtils.isBlank(taskId)) {
      return null;
    }
    try {
      return getTaskService().getTask(Long.parseLong(taskId));
    } catch (EntityNotFoundException | NumberFormatException e) {
      return null;
    }
  }

  /**
   * The link the instant email used when it was stored, otherwise the task
   * detail page of the platform
   */
  protected String url(DigestItem item, TaskDto task) {
    String stored = item.getParam(TASK_URL_PARAM);
    if (StringUtils.startsWith(stored, "http")) {
      return stored;
    }
    return CommonsUtils.getCurrentDomain() + "/" + LinkProvider.getPortalName(null) + "/" + CommonsUtils.getCurrentPortalOwner()
        + "/tasks/taskDetail/" + task.getId();
  }

  private String fullName(String username) {
    if (StringUtils.isBlank(username)) {
      return "";
    }
    Identity identity = getIdentityManager().getOrCreateUserIdentity(username);
    String fullName = identity == null || identity.getProfile() == null ? null : identity.getProfile().getFullName();
    return StringUtils.isBlank(fullName) ? username : fullName;
  }

  private TaskService getTaskService() {
    if (taskService == null) {
      taskService = ExoContainerContext.getService(TaskService.class);
    }
    return taskService;
  }

  private IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager = ExoContainerContext.getService(IdentityManager.class);
    }
    return identityManager;
  }

}
