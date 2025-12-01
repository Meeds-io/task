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
package org.exoplatform.task.integration;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.storage.ProjectStorage;

import io.meeds.common.ContainerTransactional;

@Asynchronous
public class ProjectModifiedListener extends Listener<ProjectService, ProjectDto> {

  @Override
  @ContainerTransactional
  public void onEvent(Event<ProjectService, ProjectDto> event) throws Exception {
    ProjectStorage storage = CommonsUtils.getService(ProjectStorage.class);
    ProjectDto data = event.getData();
    data.setLastModifiedDate(System.currentTimeMillis());
    storage.updateProjectNoReturn(data);
  }

}
