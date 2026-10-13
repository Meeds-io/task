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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;

import io.meeds.mcp.server.plugin.SpaceScopeResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * Tells which space a task project belongs to, for the space scope of the
 * task tools that reach a project by its id: the space whose group all the
 * managers of the project are roles of, as {@link TaskSpaceScopeResolver}
 * reads it for the tasks of that project. A project with no space, a missing
 * project, an id that isn't one and any failure belong to no space. Read as
 * the system, for the space scope decision only.
 */
@Service("projectSpaceScopeResolver")
@Profile("mcp-server")
@Slf4j
public class ProjectSpaceScopeResolver implements SpaceScopeResolver {

  /** The name the task tools' {@code space_scope.resolver} gives. */
  public static final String NAME = "project";

  /** Reads the project. */
  @Autowired
  private ProjectService          projectService;

  /** Tells the space of a project. */
  @Autowired
  private TaskSpaceScopeResolver  taskSpaceScopeResolver;

  /**
   * @return {@value #NAME}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * @param objectId the project id
   * @return the id of the space of the project, or null for a project that
   *         isn't managed through exactly one space, a missing project or
   *         space, an id that isn't one, or a lookup that fails
   */
  @Override
  public Long spaceOf(String objectId) {
    if (!StringUtils.isNumeric(objectId)) {
      return null;
    }
    try {
      ProjectDto project = projectService.getProject(Long.parseLong(objectId));
      return project == null ? null : taskSpaceScopeResolver.spaceOfProject(project);
    } catch (EntityNotFoundException e) {
      return null;
    } catch (RuntimeException e) {
      log.debug("The space of project '{}' couldn't be read", objectId, e);
      return null;
    }
  }

}
