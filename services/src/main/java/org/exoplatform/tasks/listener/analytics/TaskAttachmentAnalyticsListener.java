/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.tasks.listener.analytics;

import java.util.List;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.ObjectAttachmentId;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.analytics.listener.social.BaseAttachmentAnalyticsListener;

import lombok.Getter;

@Asynchronous
public class TaskAttachmentAnalyticsListener extends BaseAttachmentAnalyticsListener {

  @Getter
  private AttachmentService attachmentService;

  @Getter
  private SpaceService      spaceService;

  @Getter
  private List<String>      supportedObjectType;

  public TaskAttachmentAnalyticsListener(AttachmentService attachmentService,
                                         SpaceService spaceService,
                                         InitParams initParam) {
    this.attachmentService = attachmentService;
    this.spaceService = spaceService;
    this.supportedObjectType = initParam.getValuesParam("supported-type").getValues();
  }

  @Override
  protected String getModule(ObjectAttachmentId objectAttachment) {
    return "tasks";
  }

  @Override
  protected String getSubModule(ObjectAttachmentId objectAttachment) {
    return switch (objectAttachment.getObjectType()) {
    case "task" -> "taskDescription";
    case "taskComment" -> "taskComment";
    default -> "task";
    };
  }
}
