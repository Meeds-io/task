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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCollectionModel {

  private List<?> tasks;

  @JsonProperty("used_offset")
  private int     usedOffset;

  @JsonProperty("used_limit")
  private int     usedLimit;

  private long    count;

  /**
   * Whether more items than the ones returned remain after this page: call
   * again with {@code next_offset} as the offset to read them.
   */
  @JsonProperty("has_more")
  private boolean hasMore;

  /**
   * The offset of the next page, only when {@code has_more} is true.
   */
  @JsonProperty("next_offset")
  @JsonInclude(Include.NON_NULL)
  private Integer nextOffset;

  /**
   * With {@code group_by_project}, the boards holding the tasks the filters
   * select, the most first, {@value io.meeds.task.mcp.TaskMcpTool#MAX_PROJECT_COUNTS}
   * at most (EXO-94250).
   */
  @JsonInclude(Include.NON_NULL)
  private List<ProjectTaskCountModel> projects;

  /**
   * With {@code group_by_project}, the number of boards holding the tasks the
   * filters select, those left out of {@code projects} included.
   */
  @JsonProperty("project_count")
  @JsonInclude(Include.NON_NULL)
  private Integer projectCount;

  /**
   * In list_assigned_tasks, how many of the tasks counted by {@code count}
   * are assigned to the user, those the user is also a coworker of included.
   */
  @JsonProperty("assigned_count")
  @JsonInclude(Include.NON_NULL)
  private Long    assignedCount;

  /**
   * In list_assigned_tasks, how many of the tasks counted by {@code count}
   * the user is a coworker of without being their assignee: with
   * {@code assigned_count}, it makes {@code count}.
   */
  @JsonProperty("coworker_count")
  @JsonInclude(Include.NON_NULL)
  private Long    coworkerCount;

  /**
   * With {@code group_by_project}, the number of boards the user can access
   * that hold at least one open task (EXO-94360).
   */
  @JsonProperty("open_project_count")
  @JsonInclude(Include.NON_NULL)
  private Integer openProjectCount;

  /**
   * With {@code managed_only} on a grouped listing with no project, the
   * number of boards the user manages, those holding no task included: the
   * count list_projects gives with {@code managed_only} and no space
   * (EXO-94360).
   */
  @JsonProperty("managed_project_count")
  @JsonInclude(Include.NON_NULL)
  private Integer managedProjectCount;

  /**
   * With {@code group_by_project}, the boards holding selected tasks that
   * {@code projects} leaves out, past its cap (EXO-94360).
   */
  @JsonProperty("projects_not_listed")
  @JsonInclude(Include.NON_NULL)
  private Integer projectsNotListed;

  /**
   * With {@code group_by_project}, how many of the tasks counted by
   * {@code count} are personal tasks, on no board (EXO-94360).
   */
  @JsonProperty("personal_count")
  @JsonInclude(Include.NON_NULL)
  private Long    personalCount;

  /**
   * With {@code group_by_project}, one sentence stating the count, the
   * filters applied and the boards, built from the fields above (EXO-94360).
   */
  @JsonInclude(Include.NON_NULL)
  private String  summary;

  /**
   * With {@code group_by} assignee, status, label or due_period, the
   * grouping the answer counts by (EXO-94360).
   */
  @JsonProperty("group_by")
  @JsonInclude(Include.NON_NULL)
  private String  groupBy;

  /**
   * With {@code group_by} assignee, status, label or due_period, the groups
   * holding the tasks the filters select, the most first,
   * {@value io.meeds.task.mcp.TaskMcpTool#MAX_PROJECT_COUNTS} at most; the
   * five due periods always, in their order (EXO-94360).
   */
  @JsonInclude(Include.NON_NULL)
  private List<TaskGroupCountModel> groups;

  /**
   * With {@code group_by} assignee, status, label or due_period, the number
   * of groups holding the tasks the filters select, those left out of
   * {@code groups} included.
   */
  @JsonProperty("group_count")
  @JsonInclude(Include.NON_NULL)
  private Integer groupCount;

  /**
   * With {@code group_by} assignee, status, label or due_period, the groups
   * holding selected tasks that {@code groups} leaves out, past its cap.
   */
  @JsonProperty("groups_not_listed")
  @JsonInclude(Include.NON_NULL)
  private Integer groupsNotListed;

  /**
   * A page of tasks, with no count per board.
   *
   * @param tasks the tasks of the page
   * @param usedOffset the offset of the page
   * @param usedLimit the limit of the page
   * @param count the count of the tasks the listing selects
   * @param hasMore whether more tasks remain after the page
   * @param nextOffset the offset of the next page, null when none remains
   */
  public TaskCollectionModel(List<?> tasks, int usedOffset, int usedLimit, long count, boolean hasMore, Integer nextOffset) {
    this(tasks, usedOffset, usedLimit, count, hasMore, nextOffset, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

}
