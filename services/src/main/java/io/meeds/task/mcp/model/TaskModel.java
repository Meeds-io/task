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
package io.meeds.task.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.exoplatform.task.domain.Priority;

import io.meeds.mcp.server.tool.model.UserModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
public class TaskModel {

  @JsonProperty("task_id")
  private long               id;

  private String             title;

  private String             description;

  private String             link;

  private UserModel          assignee;

  private List<UserModel>    coworkers;

  @JsonProperty("start_date")
  private String             startDate;

  @JsonProperty("end_date")
  private String             endDate;

  @JsonProperty("due_date")
  private String             dueDate;

  @JsonProperty("created_date")
  private String             createdDate;

  @JsonProperty("updated_date")
  private String             updatedDate;

  private boolean            completed;

  private Priority           priority;

  @JsonProperty("workload_in_days")
  private Integer            workloadInDays;

  @JsonProperty("comments_count")
  private int                commentsCount;

  @JsonProperty("project_id")
  private Long               projectId;

  @JsonProperty("project_name")
  private String             projectName;

  private ProjectStatus      status;

  private List<ProjectLabel> labels;

  @JsonProperty("space_id")
  private Long               spaceId;

  @JsonProperty("list_task_comments_tool")
  private final String       retrieveTaskCommentsTool = "list_task_comments_by_id"; // NOSONAR

  @JsonProperty("list_allowed_project_labels")
  private final String       retrieveAllowedLabels    = "list_project_labels";      // NOSONAR

  public TaskModel(TaskModel model) {
    this(model.id,
         model.title,
         model.description,
         model.link,
         model.assignee,
         model.coworkers,
         model.startDate,
         model.endDate,
         model.dueDate,
         model.createdDate,
         model.updatedDate,
         model.completed,
         model.priority,
         model.workloadInDays,
         model.commentsCount,
         model.projectId,
         model.projectName,
         model.status,
         model.labels,
         model.spaceId);
  }

}
