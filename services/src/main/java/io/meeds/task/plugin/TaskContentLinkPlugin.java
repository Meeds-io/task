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

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.UserUtil;

import io.meeds.social.cms.model.ContentLinkExtension;
import io.meeds.social.cms.model.ContentLinkSearchResult;
import io.meeds.social.cms.plugin.ContentLinkPlugin;
import io.meeds.social.cms.service.ContentLinkPluginService;

import jakarta.annotation.PostConstruct;

@Component
public class TaskContentLinkPlugin implements ContentLinkPlugin {

  public static final String                OBJECT_TYPE = TaskPermanentLinkPlugin.OBJECT_TYPE;

  private static final String               TITLE_KEY   = "contentLink.task";

  private static final String               ICON        = "fa fa-tasks";

  private static final String               COMMAND     = "task";

  private static final ContentLinkExtension EXTENSION   = new ContentLinkExtension(OBJECT_TYPE,
                                                                                   TITLE_KEY,
                                                                                   ICON,
                                                                                   COMMAND);

  @Autowired
  private TaskService                       taskService;

  @Autowired
  private PortalContainer                   container;

  @PostConstruct
  public void init() {
    container.getComponentInstanceOfType(ContentLinkPluginService.class).addPlugin(this);
  }

  @Override
  public ContentLinkExtension getExtension() {
    return EXTENSION;
  }

  @Override
  public List<ContentLinkSearchResult> search(String keyword,
                                              Identity identity,
                                              Locale locale,
                                              int offset,
                                              int limit) {
    String currentUser = identity.getUserId();
    List<String> memberships = new LinkedList<>();
    memberships.addAll(UserUtil.getMemberships(identity));
    return taskService.findTasksByMemberShips(currentUser,
                                              memberships,
                                              keyword,
                                              limit)
                      .stream()
                      .map(this::toContentLink)
                      .toList();
  }

  @Override
  public String getContentTitle(String objectId, Locale locale) {
    try {
      TaskDto task = taskService.getTask(Long.parseLong(objectId));
      return task == null ? StringUtils.EMPTY : task.getTitle();
    } catch (EntityNotFoundException e) {
      return StringUtils.EMPTY;
    }
  }

  private ContentLinkSearchResult toContentLink(TaskDto task) {
    return new ContentLinkSearchResult(OBJECT_TYPE,
                                       String.valueOf(task.getId()),
                                       task.getTitle(),
                                       EXTENSION.getIcon());
  }

}
