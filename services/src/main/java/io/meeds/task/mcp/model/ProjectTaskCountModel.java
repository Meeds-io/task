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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One board of a {@code list_tasks} answer grouped by project: how many of
 * the tasks the filters select are on it, and how many of its open tasks
 * the user can access (EXO-94250).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
// the board's own counts before its split by status: the default mapper would
// write the keys in alphabetical order, by_status and its counts first
@JsonPropertyOrder({ "project_id", "project_name", "count", "selected_open_count", "selected_completed_count", "open_count",
    "overdue_count", "by_status" })
public class ProjectTaskCountModel {

  @JsonProperty("project_id")
  private long   projectId;

  @JsonProperty("project_name")
  private String projectName;

  /** The tasks of the board that the listing's filters select. */
  private long   count;

  /** Every open task of the board the user can access. */
  @JsonProperty("open_count")
  private long   openCount;

  /**
   * The board's selected tasks status by status, in the board's column order,
   * the statuses holding none left out (EXO-94360).
   */
  @JsonProperty("by_status")
  @JsonInclude(Include.NON_NULL)
  private List<StatusTaskCountModel> byStatus;

  /**
   * The board's overdue tasks the user can access: open, due before today in
   * the user's time zone, within the listing's project and managed boards,
   * whatever the listing's other filters (EXO-94360); none when a filter
   * other than project_id, managed_only, boards_only or hide_completed_tasks
   * narrows the listing, nor with overdue_only, since it would then count
   * other tasks than the selected ones (EXO-94935).
   */
  @JsonProperty("overdue_count")
  @JsonInclude(Include.NON_NULL)
  private Long                       overdueCount;

  /**
   * The board's selected tasks that are open, when the listing keeps the open
   * and the completed tasks both (EXO-94935).
   */
  @JsonProperty("selected_open_count")
  @JsonInclude(Include.NON_NULL)
  private Long                       selectedOpenCount;

  /**
   * The board's selected tasks that are completed, when the listing keeps the
   * open and the completed tasks both (EXO-94935).
   */
  @JsonProperty("selected_completed_count")
  @JsonInclude(Include.NON_NULL)
  private Long                       selectedCompletedCount;

  /**
   * A board's counts with no split by status.
   *
   * @param projectId the board
   * @param projectName the name of the board
   * @param count the tasks of the board that the listing's filters select
   * @param openCount every open task of the board the user can access
   */
  public ProjectTaskCountModel(long projectId, String projectName, long count, long openCount) {
    this(projectId, projectName, count, openCount, null, null);
  }

  /**
   * A board's counts with its split by status and its overdue tasks.
   *
   * @param projectId the board
   * @param projectName the name of the board
   * @param count the tasks of the board that the listing's filters select
   * @param openCount every open task of the board the user can access
   * @param byStatus the board's selected tasks status by status
   * @param overdueCount the board's overdue tasks, or null when not counted
   */
  public ProjectTaskCountModel(long projectId,
                               String projectName,
                               long count,
                               long openCount,
                               List<StatusTaskCountModel> byStatus,
                               Long overdueCount) {
    this(projectId, projectName, count, openCount, byStatus, overdueCount, null, null);
  }

}
