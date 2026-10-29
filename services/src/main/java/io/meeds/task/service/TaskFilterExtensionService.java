/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;

import io.meeds.task.plugin.TaskFilterPlugin;

/**
 * Registry of the {@link TaskFilterPlugin}s other add-ons contribute, and
 * resolver of the filters a client sends for a project's tasks into the
 * conditions of its task query.
 */
@Service
public class TaskFilterExtensionService {

  /** The most filters one request may carry. */
  public static final int                     MAX_FILTERS                = 10;

  /** The message code of a filters parameter that is not a JSON object. */
  public static final String                  INVALID_FILTERS_CODE       = "task.extensionFilters.invalid";

  /** The message code of more filters than {@link #MAX_FILTERS}. */
  public static final String                  TOO_MANY_FILTERS_CODE      = "task.extensionFilters.tooMany";

  /** The message code of a filter no plugin is registered for. */
  public static final String                  UNKNOWN_FILTER_CODE        = "task.extensionFilters.unknown";

  /** The message code of filters sent without a project. */
  public static final String                  PROJECT_REQUIRED_CODE      = "task.extensionFilters.projectRequired";

  private final Map<String, TaskFilterPlugin> plugins                    = new ConcurrentHashMap<>();

  private final ProjectService                projectService;

  /**
   * Creates the service.
   *
   * @param projectService reads the project whose view permission is checked
   */
  @Autowired
  public TaskFilterExtensionService(ProjectService projectService) {
    this.projectService = projectService;
  }

  /**
   * Registers a plugin under its id.
   *
   * @param plugin the plugin to register
   * @throws IllegalArgumentException when another plugin holds the same id
   */
  public void addPlugin(TaskFilterPlugin plugin) {
    if (plugin == null || StringUtils.isBlank(plugin.getId())) {
      throw new IllegalArgumentException("A task filter plugin needs an id");
    }
    TaskFilterPlugin previous = plugins.putIfAbsent(plugin.getId(), plugin);
    if (previous != null && previous != plugin) {
      throw new IllegalArgumentException("A task filter plugin is already registered with id " + plugin.getId());
    }
  }

  /**
   * Turns the filters a client sent for a project's tasks into the conditions
   * of the project's task query, after checking that the viewer views the
   * project.
   *
   * @param projectId the project whose tasks are filtered
   * @param filters a JSON object mapping a plugin id to that plugin's payload,
   *          or a blank value for none
   * @param viewer the user who filters
   * @return the conditions, empty when the filters restrict nothing
   * @throws IllegalArgumentException with a message code when the filters are
   *           not a JSON object, are too many, name an unknown plugin, come
   *           without a project, or when a plugin refuses its payload
   * @throws ObjectNotFoundException when the project does not exist
   * @throws IllegalAccessException when the viewer does not view the project
   */
  public List<Condition> resolve(long projectId,
                                 String filters,
                                 Identity viewer) throws ObjectNotFoundException, IllegalAccessException {
    if (StringUtils.isBlank(filters)) {
      return List.of();
    }
    if (projectId <= 0) {
      throw new IllegalArgumentException(PROJECT_REQUIRED_CODE);
    }
    JSONObject entries = parse(filters);
    if (entries.length() > MAX_FILTERS) {
      throw new IllegalArgumentException(TOO_MANY_FILTERS_CODE);
    }
    checkProjectView(projectId, viewer);
    List<Condition> conditions = new ArrayList<>();
    for (String id : entries.keySet()) {
      TaskFilterPlugin plugin = plugins.get(id);
      if (plugin == null) {
        throw new IllegalArgumentException(UNKNOWN_FILTER_CODE);
      }
      Condition condition = plugin.toCondition(projectId, String.valueOf(entries.get(id)), viewer);
      if (condition != null) {
        conditions.add(condition);
      }
    }
    return conditions;
  }

  /**
   * Parses the filters parameter.
   *
   * @param filters the parameter's value
   * @return the JSON object it holds
   * @throws IllegalArgumentException when it holds no JSON object
   */
  private JSONObject parse(String filters) {
    try {
      return new JSONObject(filters);
    } catch (JSONException e) {
      throw new IllegalArgumentException(INVALID_FILTERS_CODE);
    }
  }

  /**
   * Checks that the viewer views the project.
   *
   * @param projectId the project
   * @param viewer the user who filters
   * @throws ObjectNotFoundException when the project does not exist
   * @throws IllegalAccessException when the viewer does not view it
   */
  private void checkProjectView(long projectId, Identity viewer) throws ObjectNotFoundException, IllegalAccessException {
    ProjectDto project;
    try {
      project = projectService.getProject(projectId);
    } catch (EntityNotFoundException e) {
      project = null;
    }
    if (project == null) {
      throw new ObjectNotFoundException("Project " + projectId + " not found");
    }
    if (viewer == null || !project.canView(viewer)) {
      throw new IllegalAccessException("The user may not view project " + projectId);
    }
  }

}
