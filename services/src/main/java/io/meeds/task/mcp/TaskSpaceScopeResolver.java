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
package io.meeds.task.mcp;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;

import io.meeds.mcp.server.plugin.SpaceScopeResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * Tells which space a task belongs to, for the space scope of the task tools:
 * the space of its project, when every manager of the project is a role of
 * one and the same space group ({@code <role>:/spaces/<name>}). A personal
 * task (no status or no project), a project with any user manager, any group
 * outside the spaces or a second space among its managers, a space that no
 * longer exists and a missing task belong to no space. Participants are not
 * considered. Read as the system, for the space scope decision only.
 */
@Service("taskSpaceScopeResolver")
@Slf4j
public class TaskSpaceScopeResolver implements SpaceScopeResolver {

  /** The name the task tools' {@code space_scope.resolver} gives. */
  public static final String NAME           = "task";

  /** The prefix of the group id of a space. */
  private static final String SPACES_PREFIX = "/spaces/";

  /** Reads the task and its project. */
  @Autowired
  private TaskService         taskService;

  /** Reads the space of a group. */
  @Autowired
  private SpaceService        spaceService;

  /**
   * @return {@value #NAME}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * @param objectId the task id
   * @return the id of the space of the task's project, or null for a personal
   *         task, a project that isn't managed through exactly one space, a
   *         missing task or space, an id that isn't one, or a lookup that
   *         fails
   */
  @Override
  public Long spaceOf(String objectId) {
    if (!StringUtils.isNumeric(objectId)) {
      return null;
    }
    try {
      TaskDto task = taskService.getTask(Long.parseLong(objectId));
      StatusDto status = task == null ? null : task.getStatus();
      ProjectDto project = status == null ? null : status.getProject();
      return project == null ? null : spaceOfProject(project);
    } catch (EntityNotFoundException e) {
      return null;
    } catch (RuntimeException e) {
      log.debug("The space of task '{}' couldn't be read", objectId, e);
      return null;
    }
  }

  /**
   * @param project a task project
   * @return the id of the space whose group all its managers are roles of,
   *         else null: any manager that is a user, a group outside the
   *         spaces or a role of another group gives none
   */
  private Long spaceOfProject(ProjectDto project) {
    Set<String> groupIds = project.getManager() == null ? Set.of() :
                                                        project.getManager()
                                                               .stream()
                                                               .map(TaskSpaceScopeResolver::groupIdOf)
                                                               .collect(Collectors.toSet());
    if (groupIds.size() != 1) {
      return null;
    }
    String groupId = groupIds.iterator().next();
    if (!groupId.startsWith(SPACES_PREFIX)) {
      return null;
    }
    Space space = spaceService.getSpaceByGroupId(groupId);
    return space != null && StringUtils.isNumeric(space.getId()) ? Long.valueOf(space.getId()) : null;
  }

  /**
   * @param permission a project manager permission, {@code <role>:<group>}
   *                   or a username
   * @return the group of the permission, empty when it names none
   */
  private static String groupIdOf(String permission) {
    int index = permission == null ? -1 : permission.indexOf(':');
    return index < 0 ? "" : permission.substring(index + 1);
  }

}
