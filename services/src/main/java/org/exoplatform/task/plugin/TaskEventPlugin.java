/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package org.exoplatform.task.plugin;

import static org.exoplatform.task.util.TaskUtil.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.meeds.gamification.plugin.EventPlugin;
import org.apache.commons.lang3.StringUtils;

public class TaskEventPlugin extends EventPlugin {

  public static final String EVENT_TYPE = "task";

  @Override
  public String getEventType() {
    return EVENT_TYPE;
  }

  public List<String> getTriggers() {
    return List.of(GAMIFICATION_TASK_ADDON_CREATE_TASK,
                   GAMIFICATION_TASK_ADDON_COMPLETED_TASK_ASSIGNED,
                   GAMIFICATION_TASK_ADDON_COMPLETED_TASK_COWORKER,
                   GAMIFICATION_TASK_ADDON_COMPLETED_TASK,
                   GAMIFICATION_TASK_ADDON_UPDATE_TASK,
                   GAMIFICATION_TASK_ADDON_COMMENT_TASK);
  }

  @Override
  public boolean isValidEvent(Map<String, String> eventProperties, String triggerDetails) {
    String desiredProjectId = eventProperties.get(PROJECT_ID);
    return desiredProjectId != null && desiredProjectId.equals(getProjectId(triggerDetails));
  }

  private static String getProjectId(String mapAsString) {
    if (StringUtils.isNotBlank(mapAsString)) {
      Map<String, String> map = new HashMap<>();
      mapAsString = mapAsString.substring(1, mapAsString.length() - 1);
      String[] pairs = mapAsString.split(", ");
      for (String pair : pairs) {
        String[] keyValue = pair.split(": ");
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();
        map.put(key, value);
      }
      return map.get(PROJECT_ID);
    }
    return null;
  }
}
