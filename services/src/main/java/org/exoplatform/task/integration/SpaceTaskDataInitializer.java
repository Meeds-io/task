/**
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 **/

package org.exoplatform.task.integration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.util.ProjectUtil;
import org.exoplatform.task.util.UserUtil;

public class SpaceTaskDataInitializer extends SpaceListenerPlugin {

  private ProjectService   projectService;

  private StatusService    statusServ;

  public SpaceTaskDataInitializer(ProjectService pServ, StatusService statusServ) {
    this.projectService = pServ;
    this.statusServ = statusServ;
  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();

    List<String> memberships = UserUtil.getSpaceMemberships(space.getGroupId());
    Set<String> managers = new HashSet<>(Arrays.asList(memberships.get(0)));
    Set<String> participators = new HashSet<>(Arrays.asList(memberships.get(1)));

    ProjectDto project = ProjectUtil.newProjectInstanceDto(space.getDisplayName(), "", managers, participators);
    project = projectService.createProject(project);
    statusServ.createInitialStatuses(project);
  }

}
