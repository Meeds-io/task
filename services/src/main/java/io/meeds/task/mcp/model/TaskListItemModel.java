/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A task in a list: what tells the card apart on its board, as a search
 * result does, with its assignee, due date, creation date, priority and
 * comments count, without its description, coworkers or other dates, which
 * get_task_by_id returns. The creation date tells the tasks created since a
 * date apart.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
public class TaskListItemModel {

  @JsonProperty("task_id")
  private long               id;

  private String             title;

  private String             link;

  private boolean            completed;

  @JsonProperty("project_id")
  private Long               projectId;

  @JsonProperty("project_name")
  private String             projectName;

  private ProjectStatus      status;

  private List<ProjectLabel> labels;

  private TaskAssigneeModel  assignee;

  @JsonProperty("due_date")
  private String             dueDate;

  /**
   * When the task was created, formatted as the full model's created_date.
   */
  @JsonProperty("created_date")
  private String             createdDate;

  /**
   * The days an open task is past its due date, in the user's time zone;
   * none for a task not overdue.
   */
  @JsonProperty("days_overdue")
  private Integer            daysOverdue;

  private Priority           priority;

  @JsonProperty("comments_count")
  private int                commentsCount;

  /**
   * The changes since the date of a listing with {@code changed_since} or
   * {@code completed_since}, oldest first; none otherwise.
   */
  private List<TaskChange>   changes;

}
