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
package org.exoplatform.task.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TaskSearchFilter {

  private String       userId;

  private String       term;

  private List<String> permissions;

  private String       sortField;

  private String       sortDirection;

  private int          offset;

  private int          limit;

  /**
   * The projects the tasks found belong to, none for any project.
   */
  private List<Long>   projectIds;

  /**
   * A filter on any project.
   *
   * @param userId the user who searches
   * @param term the text searched in the title and the description
   * @param permissions the user and memberships whose tasks are searched
   * @param sortField the field to sort by, blank for the relevance
   * @param sortDirection the sort direction
   * @param offset the first task returned
   * @param limit the most tasks returned
   */
  public TaskSearchFilter(String userId, // NOSONAR the filter's fields, each one its own
                          String term,
                          List<String> permissions,
                          String sortField,
                          String sortDirection,
                          int offset,
                          int limit) {
    this.userId = userId;
    this.term = term;
    this.permissions = permissions;
    this.sortField = sortField;
    this.sortDirection = sortDirection;
    this.offset = offset;
    this.limit = limit;
  }

}
