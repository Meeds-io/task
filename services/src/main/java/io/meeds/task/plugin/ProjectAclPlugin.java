/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.task.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;

import io.meeds.portal.plugin.AclPlugin;

import jakarta.annotation.PostConstruct;

@Component
public class ProjectAclPlugin implements AclPlugin {

  public static final String OBJECT_TYPE = ProjectPermanentLinkPlugin.OBJECT_TYPE;

  @Autowired
  private PortalContainer    container;

  @Autowired
  private ProjectService     projectService;

  @PostConstruct
  public void init() {
    container.getComponentInstanceOfType(UserACL.class).addAclPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasPermission(String objectId, String permissionType, Identity identity) {
    ProjectDto project;
    try {
      project = projectService.getProject(Long.parseLong(objectId));
    } catch (EntityNotFoundException e) {
      return false;
    }
    if (project == null) {
      return false;
    } else {
      return switch (permissionType) {
      case VIEW_PERMISSION_TYPE: {
        yield project.canView(identity);
      }
      case EDIT_PERMISSION_TYPE, DELETE_PERMISSION_TYPE: {
        yield project.canEdit(identity);
      }
      default:
        yield false;
      };
    }
  }

}
