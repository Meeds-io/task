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
package io.meeds.task.service;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.util.TaskUtil;

import io.meeds.task.model.TaskFilterSetting;
import io.meeds.task.storage.TaskFilterSettingStorage;

@Service
public class TaskFilterSettingService {

  private static final String            LIST_TAB_VIEW   = "list";

  private static final Set<String>       TAB_VIEWS       = Set.of(LIST_TAB_VIEW, "board", "gantt");

  private static final Set<String>       GROUP_BY_VALUES = Set.of(TaskUtil.NONE,
                                                                  TaskUtil.PROJECT,
                                                                  TaskUtil.STATUS,
                                                                  TaskUtil.ASSIGNEE,
                                                                  TaskUtil.LABEL,
                                                                  TaskUtil.DUEDATE,
                                                                  TaskUtil.COMPLETED);

  private static final Set<String>       ORDER_BY_VALUES = Set.of(TaskUtil.STATUS,
                                                                  TaskUtil.TITLE,
                                                                  TaskUtil.DUEDATE,
                                                                  TaskUtil.PRIORITY,
                                                                  TaskUtil.RANK,
                                                                  TaskUtil.CREATED_TIME);

  private final TaskFilterSettingStorage taskFilterSettingStorage;

  private final ProjectService           projectService;

  private final UserACL                  userAcl;

  @Autowired
  public TaskFilterSettingService(TaskFilterSettingStorage taskFilterSettingStorage,
                                  ProjectService projectService,
                                  UserACL userAcl) {
    this.taskFilterSettingStorage = taskFilterSettingStorage;
    this.projectService = projectService;
    this.userAcl = userAcl;
  }

  public TaskFilterSetting getSetting(long projectId,
                                      String tabView,
                                      String username) throws ObjectNotFoundException, IllegalAccessException {
    checkProjectAccess(projectId, username);
    String view = checkTabView(tabView);
    TaskFilterSetting stored = taskFilterSettingStorage.getSetting(projectId, view, username);
    if (stored == null) {
      return new TaskFilterSetting(projectId, view, "", "");
    }
    return new TaskFilterSetting(projectId,
                                 view,
                                 GROUP_BY_VALUES.contains(stored.groupBy()) ? stored.groupBy() : "",
                                 ORDER_BY_VALUES.contains(stored.orderBy()) ? stored.orderBy() : "");
  }

  public TaskFilterSetting saveSetting(TaskFilterSetting setting,
                                       String username) throws ObjectNotFoundException, IllegalAccessException {
    if (setting == null) {
      throw new IllegalArgumentException("task.filter.setting.mandatory");
    }
    checkProjectAccess(setting.projectId(), username);
    String view = checkTabView(setting.tabView());
    String groupBy = checkValue(setting.groupBy(), GROUP_BY_VALUES, "task.filter.groupBy.invalid");
    String orderBy = checkValue(setting.orderBy(), ORDER_BY_VALUES, "task.filter.orderBy.invalid");
    TaskFilterSetting saved = new TaskFilterSetting(setting.projectId(), view, groupBy, orderBy);
    taskFilterSettingStorage.saveSetting(saved, username);
    return saved;
  }

  private void checkProjectAccess(long projectId, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (projectId < 0) {
      throw new IllegalArgumentException("task.filter.projectId.invalid");
    } else if (projectId == 0) {
      return;
    }
    ProjectDto project;
    try {
      project = projectService.getProject(projectId);
    } catch (EntityNotFoundException e) {
      throw new ObjectNotFoundException("task.filter.project.notFound");
    }
    Identity identity = userAcl.getUserIdentity(username);
    if (identity == null || !project.canView(identity)) {
      throw new IllegalAccessException("task.filter.project.notAccessible");
    }
  }

  private String checkTabView(String tabView) {
    if (StringUtils.isBlank(tabView)) {
      return LIST_TAB_VIEW;
    } else if (!TAB_VIEWS.contains(tabView)) {
      throw new IllegalArgumentException("task.filter.tabView.invalid");
    }
    return tabView;
  }

  private String checkValue(String value, Set<String> allowedValues, String errorCode) {
    if (StringUtils.isBlank(value)) {
      return "";
    } else if (!allowedValues.contains(value)) {
      throw new IllegalArgumentException(errorCode);
    }
    return value;
  }

}
