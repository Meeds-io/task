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

import com.fasterxml.jackson.annotation.JsonProperty;

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

}
