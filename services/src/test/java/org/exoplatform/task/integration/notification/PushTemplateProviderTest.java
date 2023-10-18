/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
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
 */
package org.exoplatform.task.integration.notification;

import junit.framework.TestCase;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.service.UserService;
import org.mockito.Mockito;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushTemplateProviderTest extends TestCase {

  private static final Log LOG = ExoLogger.getLogger(PushTemplateProviderTest.class);
  
  public void testMakeMessage() {
    NotificationInfo notificationInfo1 = createNotification();

    assertNotNull(notificationInfo1);

    InitParams initParams = new InitParams();
    ValueParam channel = new ValueParam();
    channel.setValue("push.channel");
    channel.setName("channel-id");
    initParams.addParam(channel);
    NotificationContext context = NotificationContextImpl.cloneInstance();
    Map<String, String> ownerParameter = new HashMap<>();

    PushTemplateProvider pushTemplateProvider = new PushTemplateProvider(initParams);
    notificationInfo1.setTo("root");
    notificationInfo1.setId(TaskMentionPlugin.ID);
    notificationInfo1.key(TaskMentionPlugin.ID);
    ownerParameter.put(NotificationUtils.TASK_CREATOR, "user1");
    ownerParameter.put(NotificationUtils.TASK_ASSIGNEE, "user2");
    ownerParameter.put(NotificationUtils.ADDED_COWORKER, "user3");
    ownerParameter.put(NotificationUtils.MENTIONED_USERS, "user4");
    notificationInfo1.setOwnerParameter(ownerParameter);
    context.setNotificationInfo(notificationInfo1);
    AbstractTemplateBuilder builder = pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(TaskMentionPlugin.ID));
    try {
      MessageInfo message = builder.buildMessage(context);
    } catch (StackOverflowError e) {
      LOG.error("Error when building message",e);
      fail("StackOverflowError when building notification message");
    }


  }

  private NotificationInfo createNotification() {
    try {
      return NotificationInfo.instance();
    } catch (Exception e) {
      LOG.error("Error getting notification", e);
      fail("Error getting notification instance: " + e.getMessage());
    }
    return null;
  }
}
