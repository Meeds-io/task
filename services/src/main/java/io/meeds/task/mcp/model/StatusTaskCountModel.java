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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One status of a board in a {@code list_tasks} answer grouped by project:
 * how many of the tasks the filters select are in it (EXO-94360).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
// the status before its count: the default mapper would write the keys in
// alphabetical order, the count first
@JsonPropertyOrder({ "status_id", "status", "count" })
public class StatusTaskCountModel {

  @JsonProperty("status_id")
  private long   statusId;

  /** The name of the status, the board's column. */
  private String status;

  /** The tasks of the status that the listing's filters select. */
  private long   count;

}
