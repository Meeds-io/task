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
package io.meeds.task.storage;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;

import io.meeds.task.model.TaskFilterSetting;

@Component
public class TaskFilterSettingStorage {

  private static final String  SETTING_KEY      = "filterTask";

  private static final String  NO_PROJECT       = "None";

  private static final String  GROUP_BY_FIELD   = "groupBy";

  private static final String  ORDER_BY_FIELD   = "orderBy";

  private static final String  PROJECT_ID_FIELD = "projectId";

  private static final String  TAB_VIEW_FIELD   = "tabView";

  private final SettingService settingService;

  @Autowired
  public TaskFilterSettingStorage(SettingService settingService) {
    this.settingService = settingService;
  }

  public TaskFilterSetting getSetting(long projectId, String tabView, String username) {
    SettingValue<?> value = settingService.get(Context.USER.id(username), scope(projectId, tabView), SETTING_KEY);
    if (value == null || value.getValue() == null) {
      return null;
    }
    try {
      JSONObject json = new JSONObject(value.getValue().toString());
      return new TaskFilterSetting(projectId,
                                   tabView,
                                   json.optString(GROUP_BY_FIELD, ""),
                                   json.optString(ORDER_BY_FIELD, ""));
    } catch (JSONException e) {
      return null;
    }
  }

  public void saveSetting(TaskFilterSetting setting, String username) {
    settingService.set(Context.USER.id(username),
                       scope(setting.projectId(), setting.tabView()),
                       SETTING_KEY,
                       SettingValue.create(serialize(setting)));
  }

  private String serialize(TaskFilterSetting setting) {
    JSONObject json = new JSONObject();
    json.put(GROUP_BY_FIELD, setting.groupBy());
    json.put(ORDER_BY_FIELD, setting.orderBy());
    json.put(PROJECT_ID_FIELD, setting.projectId() > 0 ? String.valueOf(setting.projectId()) : NO_PROJECT);
    json.put(TAB_VIEW_FIELD, setting.tabView());
    return json.toString();
  }

  private Scope scope(long projectId, String tabView) {
    String projectKey = projectId > 0 ? String.valueOf(projectId) : NO_PROJECT;
    return Scope.APPLICATION.id("task-" + projectKey + "-" + tabView);
  }

}
