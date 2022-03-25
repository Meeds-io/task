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

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.util.ProjectUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*" })
@PrepareForTest({CommonsUtils.class, ProjectUtil.class})
public class AbstractNotificationPluginTest {

  @Mock
  private OrganizationService organizationService;

  public class DummyNotificationPlugin extends AbstractNotificationPlugin {

    public DummyNotificationPlugin(InitParams initParams) {
      super(initParams);
    }

    @Override
    public String getId() {
      return "DummyNotificationPlugin";
    }

    @Override
    public boolean isValid(NotificationContext notificationContext) {
      return true;
    }
  }

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(CommonsUtils.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    when(CommonsUtils.getOrganizationService()).thenReturn(organizationService);
  }

  @Test
  public void shouldReturnAssigneeInReceivers() throws Exception {
    // Given
    TaskDto task = new TaskDto();
    task.setAssignee("user1");
    ProjectDto projectDto = new ProjectDto();
    StatusDto statusDto = new StatusDto();
    statusDto.setProject(new ProjectDto());
    task.setStatus(statusDto);
    AbstractNotificationPlugin notificationPlugin = new DummyNotificationPlugin(new InitParams());

    // When
    when(ProjectUtil.isProjectParticipant(organizationService,"user1", projectDto)).thenReturn(true);
    Set<String> receivers = notificationPlugin.getReceiver(task, null);

    // Then
    assertNotNull(receivers);
    assertEquals(1, receivers.size());
    assertEquals("user1", receivers.iterator().next());
  }

  @Test
  public void shouldReturnCoworkersInReceivers() throws Exception {
    // Given
    TaskDto task = new TaskDto();
    ProjectDto projectDto = new ProjectDto();
    StatusDto statusDto = new StatusDto();
    statusDto.setProject(new ProjectDto());
    task.setStatus(statusDto);
    task.setCoworker(new HashSet<>(Arrays.asList("user1", "user2")));

    AbstractNotificationPlugin notificationPlugin = new DummyNotificationPlugin(new InitParams());

    // When
    when(ProjectUtil.isProjectParticipant(organizationService,"user1", projectDto)).thenReturn(true);
    when(ProjectUtil.isProjectParticipant(organizationService,"user2", projectDto)).thenReturn(true);

    Set<String> receivers = notificationPlugin.getReceiver(task, null);

    // Then
    assertNotNull(receivers);
    assertEquals(2, receivers.size());
    assertTrue(receivers.contains("user1"));
    assertTrue(receivers.contains("user2"));
  }

  @Test
  public void shouldReturnWatchersInReceivers() throws Exception {
    // Given
    TaskDto task = new TaskDto();
    ProjectDto projectDto = new ProjectDto();
    StatusDto statusDto = new StatusDto();
    statusDto.setProject(new ProjectDto());
    task.setStatus(statusDto);
    task.setWatcher(new HashSet<>(Arrays.asList("user1", "user2")));

    AbstractNotificationPlugin notificationPlugin = new DummyNotificationPlugin(new InitParams());

    // When
    when(ProjectUtil.isProjectParticipant(organizationService,"user1", projectDto)).thenReturn(true);
    when(ProjectUtil.isProjectParticipant(organizationService,"user2", projectDto)).thenReturn(true);
    Set<String> receivers = notificationPlugin.getReceiver(task, null);

    // Then
    assertNotNull(receivers);
    assertEquals(2, receivers.size());
    assertTrue(receivers.contains("user1"));
    assertTrue(receivers.contains("user2"));
  }

  @Test
  public void shouldReturnCreatorInReceivers() throws Exception {
    // Given
    TaskDto task = new TaskDto();
    task.setAssignee("user1");
    task.setCreatedBy("user2");
    ProjectDto projectDto = new ProjectDto();
    StatusDto statusDto = new StatusDto();
    statusDto.setProject(new ProjectDto());
    task.setStatus(statusDto);
    task.setCoworker(new HashSet<>(Arrays.asList("user2", "user3")));

    NotificationContext ctx = NotificationContextImpl.cloneInstance();

    AbstractNotificationPlugin notificationPlugin = new DummyNotificationPlugin(new InitParams());

    // When
    when(ProjectUtil.isProjectParticipant(organizationService,"user1", projectDto)).thenReturn(true);
    when(ProjectUtil.isProjectParticipant(organizationService,"user2", projectDto)).thenReturn(true);
    when(ProjectUtil.isProjectParticipant(organizationService,"user3", projectDto)).thenReturn(true);

    Set<String> receivers = notificationPlugin.getReceiver(task, ctx);

    // Then
    assertNotNull(receivers);
    assertEquals(3, receivers.size());
    assertTrue(receivers.contains("user1"));
    assertTrue(receivers.contains("user2"));
    assertTrue(receivers.contains("user3"));
  }

  @Test
  public void shouldNotReturnUserInReceiversIfNotProjectParticipant() throws Exception {
    // Given
    TaskDto task = new TaskDto();
    task.setCreatedBy("user2");
    task.setAssignee("user1");
    ProjectDto projectDto = new ProjectDto();
    StatusDto statusDto = new StatusDto();
    statusDto.setProject(new ProjectDto());
    task.setStatus(statusDto);

    NotificationContext ctx = NotificationContextImpl.cloneInstance();

    AbstractNotificationPlugin notificationPlugin = new DummyNotificationPlugin(new InitParams());

    // When
    when(ProjectUtil.isProjectParticipant(organizationService, "user1", projectDto)).thenReturn(false);
    when(ProjectUtil.isProjectParticipant(organizationService, "user2", projectDto)).thenReturn(true);

    Set<String> receivers = notificationPlugin.getReceiver(task, ctx);

    // Then
    assertNotNull(receivers);
    assertEquals(1, receivers.size());
    assertFalse(receivers.contains("user1"));
    assertTrue(receivers.contains("user2"));
  }
}
