/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.task.integration.notification;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.util.ProjectUtil;
import org.exoplatform.task.util.TaskUtil;
import org.exoplatform.web.WebAppController;

public abstract class AbstractNotificationPlugin extends BaseNotificationPlugin {

  AbstractNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public NotificationInfo makeNotification(NotificationContext ctx) {
    TaskDto task = ctx.value(NotificationUtils.TASK);
    String notificationCreator = ctx.value(NotificationUtils.CREATOR);
    Set coworker = ctx.value(NotificationUtils.COWORKER);
    Set mentioned = ctx.value(NotificationUtils.MENTIONED);
    String actionName=ctx.value(NotificationUtils.ACTION_NAME);
    //
    String projectName = "";
    String projectId = "";
    long spaceId = 0l;
    ProjectDto project = null;
    if (task.getStatus() != null && task.getStatus().getProject() != null) {
      project = task.getStatus().getProject();
      projectName = project.getName();
      projectId = String.valueOf(project.getId());
      Space space = getProjectSpace(project);
      if (space != null) {
        spaceId = Long.parseLong(space.getId());
      }
    }
    StringBuilder activityId = new StringBuilder(projectId);
    activityId.append("-").append(notificationCreator);
    
    ExoContainer container = getContainer();
    WebAppController controller = container.getComponentInstanceOfType(WebAppController.class);
    RequestLifeCycle.begin(container);
    String taskUrl = buildTaskUrl(task, container, controller);
    String projectUrl = buildProjectUrl(project, container, controller);
    String assignee = task.getAssignee();
    Set<String> listOfCoworker = task.getCoworker();
    Set<String> listOfWatcher = task.getWatcher();
    Set<String> receivers = getReceiver(task, ctx);
    String taskCreator= task.getCreatedBy();
    RequestLifeCycle.end();

    return NotificationInfo.instance()
                           .to(new LinkedList<>(receivers))
                           .setSpaceId(spaceId)
                           .with(NotificationUtils.TASK_ID, String.valueOf(task.getId()))
                           .with(NotificationUtils.TASK_TITLE, task.getTitle())
                           .with(NotificationUtils.TASK_DESCRIPTION, task.getDescription())
                           .with(NotificationUtils.CREATOR.getKey(), notificationCreator)
                           .with(NotificationUtils.PROJECT_NAME, projectName)
                           .with(NotificationUtils.ACTIVITY_ID, activityId.toString())
                           .with(NotificationUtils.TASK_URL, taskUrl)
                           .with(NotificationUtils.PROJECT_URL, projectUrl)
                           .with(NotificationUtils.TASK_ASSIGNEE, assignee)
                           .with(NotificationUtils.RECEIVERS.getKey(), String.valueOf(receivers))
                           .with(NotificationUtils.TASK_COWORKERS, String.valueOf(listOfCoworker))
                           .with(NotificationUtils.TASK_WATCHERS, String.valueOf(listOfWatcher))
                           .with(NotificationUtils.ADDED_COWORKER, String.valueOf(coworker))
                           .with(NotificationUtils.MENTIONED_USERS, String.valueOf(mentioned))
                           .with(NotificationUtils.ACTION_NAME.getKey(),String.valueOf(actionName))
                           .with(NotificationUtils.TASK_CREATOR, taskCreator)
                           .key(getKey()).end();
  }

  private ExoContainer getContainer() {
    String containerName = PortalContainer.getCurrentPortalContainerName();
    ExoContainer container = RootContainer.getInstance().getPortalContainer(containerName);
    return container;
  }

  private String buildProjectUrl(ProjectDto project, ExoContainer container, WebAppController controller) {
    return CommonsUtils.getCurrentDomain() + ProjectUtil.buildProjectURL(project, CommonsUtils.getCurrentSite(), container, controller.getRouter());
  }

  protected Set<String> getReceiver(TaskDto task, NotificationContext ctx) {
    OrganizationService organizationService = CommonsUtils.getOrganizationService();
    Set<String> receivers = new HashSet<String>();
    if (task.getAssignee() != null && !task.getAssignee().isEmpty()) {
      receivers.add(task.getAssignee());
    }
    if (task.getCreatedBy() != null && !task.getCreatedBy().isEmpty()) {
      receivers.add(task.getCreatedBy());
    }
    if (task.getCoworker() != null && task.getCoworker().size() > 0) {
      receivers.addAll(task.getCoworker());
    }
    if (task.getWatcher() != null && task.getWatcher().size() > 0) {
      receivers.addAll(task.getWatcher());
    }
    if (ctx != null) {
      receivers.remove(ctx.value(NotificationUtils.CREATOR));
    }
    if (task.getStatus() != null && task.getStatus().getProject() != null) {
      ProjectDto project = task.getStatus().getProject();
      receivers.removeIf(user -> !ProjectUtil.isProjectParticipant(organizationService, user, project));
    }
    return receivers;
  }

  protected String getPortalOwner() {
    return CommonsUtils.getService(UserPortalConfigService.class).getDefaultPortal();
  }

  private String buildTaskUrl(TaskDto t, ExoContainer container, WebAppController controller) {
    return CommonsUtils.getCurrentDomain() + TaskUtil.buildTaskURL(t, CommonsUtils.getCurrentSite(), container, controller.getRouter());
  }

  private Space getProjectSpace(ProjectDto project) {
    SpaceService spaceService = getContainer().getComponentInstanceOfType(SpaceService.class);
    Space space = null;
    for (String permission : project.getManager()) {
      int index = permission.indexOf(':');
      if (index > -1) {
        String groupId = permission.substring(index + 1);
        space = spaceService.getSpaceByGroupId(groupId);
      }
    }
    return space;
  }
}
