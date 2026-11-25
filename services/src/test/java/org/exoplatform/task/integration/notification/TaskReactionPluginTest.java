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
package org.exoplatform.task.integration.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.util.ProjectUtil;
import org.exoplatform.task.util.TaskUtil;
import org.exoplatform.web.WebAppController;

public class TaskReactionPluginTest {

  private final TaskReactionPlugin plugin = new TaskReactionPlugin(new InitParams());

  @Test
  public void idIsStable() {
    assertEquals("TaskReactionPlugin", plugin.getId());
  }

  @Test
  public void isValidOnlyWithReceivers() {
    NotificationContext ctx = mock(NotificationContext.class);
    assertFalse(plugin.isValid(ctx));

    when(ctx.value(NotificationUtils.RECEIVERS)).thenReturn(Set.of());
    assertFalse(plugin.isValid(ctx));

    when(ctx.value(NotificationUtils.RECEIVERS)).thenReturn(Set.of("author"));
    assertTrue(plugin.isValid(ctx));
  }

  @Test
  public void receiversAreTheOnesOfTheContext() {
    NotificationContext ctx = mock(NotificationContext.class);
    when(ctx.value(NotificationUtils.RECEIVERS)).thenReturn(Set.of("author"));

    assertTrue(plugin.getReceiver(null, ctx).contains("author"));
  }

  @Test
  public void aMissingEmojiIsNotTheWordNull() {
    TaskDto task = new TaskDto();
    task.setId(9L);
    task.setTitle("A task");
    CommentDto comment = new CommentDto();
    comment.setComment("hello");
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(NotificationUtils.TASK, task)
       .append(NotificationUtils.COMMENT, comment)
       .append(NotificationUtils.CREATOR, "john")
       .append(NotificationUtils.RECEIVERS, Set.of("author"));
    NotificationInfo info = makeNotification(ctx);

    assertEquals("", info.getValueOwnerParameter(NotificationUtils.REACTION_EMOJI));
  }

  @Test
  public void makeNotificationCarriesTheReactionTheCommentAndTheReceivers() {
    TaskDto task = new TaskDto();
    task.setId(9L);
    task.setTitle("A task");
    task.setCreatedBy("creator");
    CommentDto comment = new CommentDto();
    comment.setComment("hello");
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(NotificationUtils.TASK, task)
       .append(NotificationUtils.COMMENT, comment)
       .append(NotificationUtils.CREATOR, "john")
       .append(NotificationUtils.RECEIVERS, Set.of("author"))
       .append(NotificationUtils.REACTION_EMOJI_ARG, "\u2764\uFE0F");
    NotificationInfo info = makeNotification(ctx);

    assertEquals("TaskReactionPlugin", info.getKey().getId());
    assertEquals(Set.of("author"), Set.copyOf(info.getSendToUserIds()));
    assertEquals("9", info.getValueOwnerParameter(NotificationUtils.TASKS));
    assertEquals("\u2764\uFE0F", info.getValueOwnerParameter(NotificationUtils.REACTION_EMOJI));
    assertEquals("hello", info.getValueOwnerParameter(NotificationUtils.COMMENT_TEXT));
    assertEquals("john", info.getValueOwnerParameter(NotificationUtils.CREATOR.getKey()));
    assertEquals("http://localhost/tasks/9", info.getValueOwnerParameter(NotificationUtils.TASK_URL));
  }

  private NotificationInfo makeNotification(NotificationContext ctx) {
    PortalContainer container = mock(PortalContainer.class);
    RootContainer rootContainer = mock(RootContainer.class);
    when(rootContainer.getPortalContainer(any())).thenReturn(container);
    when(container.getComponentInstanceOfType(WebAppController.class)).thenReturn(mock(WebAppController.class));
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    when(portalConfigService.getMetaPortal()).thenReturn("meta");

    try (MockedStatic<RootContainer> root = mockStatic(RootContainer.class);
        MockedStatic<PortalContainer> portal = mockStatic(PortalContainer.class);
        MockedStatic<RequestLifeCycle> lifeCycle = mockStatic(RequestLifeCycle.class);
        MockedStatic<CommonsUtils> commons = mockStatic(CommonsUtils.class);
        MockedStatic<TaskUtil> taskUtil = mockStatic(TaskUtil.class);
        MockedStatic<ProjectUtil> projectUtil = mockStatic(ProjectUtil.class);
        MockedStatic<MentionUtils> mentions = mockStatic(MentionUtils.class)) {
      root.when(RootContainer::getInstance).thenReturn(rootContainer);
      portal.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
      commons.when(CommonsUtils::getCurrentDomain).thenReturn("http://localhost");
      commons.when(() -> CommonsUtils.getService(UserPortalConfigService.class)).thenReturn(portalConfigService);
      taskUtil.when(() -> TaskUtil.buildTaskURL(any(), any(), any(), any())).thenReturn("/tasks/9");
      projectUtil.when(() -> ProjectUtil.buildProjectURL(any(), any(), any(), any())).thenReturn("");
      mentions.when(() -> MentionUtils.substituteUsernames("meta", "hello")).thenReturn("hello");
      return plugin.makeNotification(ctx);
    }
  }

}
