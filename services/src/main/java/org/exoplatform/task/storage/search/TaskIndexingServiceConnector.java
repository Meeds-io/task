/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.task.storage.search;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.search.DocumentWithMetadata;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.TaskService;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TaskIndexingServiceConnector extends ElasticIndexingServiceConnector {

  public static final String TYPE = "task";

  private static final Log   LOG  = ExoLogger.getLogger(TaskIndexingServiceConnector.class);

  private final TaskService  taskService;

  public TaskIndexingServiceConnector(InitParams initParams, TaskService taskService) {
    super(initParams);
    this.taskService = taskService;
  }

  @Override
  public String getConnectorName() {
    return TYPE;
  }

  @Override
  public Document create(String s) {
    return getDocument(s);
  }

  @Override
  public Document update(String s) {
    return getDocument(s);
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    // TODO
    return new ArrayList<>();
  }

  @Override
  public String getMapping() {
    return """
        {
          "properties": {
            "id": {"type": "keyword"},
            "title": {
              "type": "text",
              "analyzer": "ngram_analyzer",
              "search_analyzer": "ngram_analyzer_search",
              "index_options": "offsets",
              "fields": {
                "raw": {"type": "keyword"}
              }
            },
            "description": {
              "type": "text",
              "analyzer": "ngram_analyzer",
              "search_analyzer": "ngram_analyzer_search",
              "index_options": "offsets",
              "fields": {
                "raw": {"type": "keyword"}
              }
            },
            "status": {"type": "keyword"},
            "priority": {"type": "keyword"},
            "project": {"type": "keyword"},
            "creator": {"type": "keyword"},
            "lastUpdatedDate": {"type": "date", "format": "epoch_millis"},
            "permissions": {"type": "keyword"}
          }
        }
        """;
  }

  @SneakyThrows
  private Document getDocument(String id) {

    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id is mandatory");
    }
    LOG.debug("Index document for task with id={}", id);

    Long taskId = Long.valueOf(id);
    TaskDto task = taskService.getTask(taskId);
    Map<String, String> fields = new HashMap<>();
    fields.put("id", Long.toString(taskId));
    fields.put("title", task.getTitle());
    fields.put("description", htmlToText(task.getDescription()));
    DocumentWithMetadata doc = new DocumentWithMetadata();
    doc.setFields(fields);
    doc.setId(String.valueOf(taskId));
    doc.setLastUpdatedDate(new Date());
    Set<String> permissions = new HashSet<>();
    permissions.add(task.getCreatedBy());
    if (task.getAssignee() != null) {
      permissions.add(task.getAssignee());
    }
    if (task.getCoworker() != null) {
      permissions.addAll(task.getCoworker());
    }
    if (task.getStatus() != null && task.getStatus().getProject() != null) {
      fields.put("projectId", String.valueOf(task.getStatus().getProject().getId()));
      fields.put("statusId", String.valueOf(task.getStatus().getId()));
      permissions.addAll(task.getStatus().getProject().getParticipator());
      permissions.addAll(task.getStatus().getProject().getManager());
    }
    doc.setPermissions(permissions);
    return doc;
  }

  private String htmlToText(String source) {
    return source == null ? "" : Jsoup.parse(source).text();
  }

}
