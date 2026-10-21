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
package org.exoplatform.task.model;

import java.util.List;

import org.exoplatform.task.dto.TaskDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The tasks a text search found for a user, the open ones first.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTextSearchResult {

  /** The tasks found, at most the limit used. */
  private List<TaskDto> tasks;

  /** The most tasks the search returns, once bounded. */
  private int           limit;

  /**
   * Whether more tasks may match than the ones returned: either the limit
   * was reached, or the search read its whole window of hits, so that matches
   * ranked below it were never read.
   */
  private boolean       hasMore;

}
