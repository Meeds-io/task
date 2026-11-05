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
import java.util.Map;

import org.exoplatform.task.dto.TaskDto;

import io.meeds.task.search.model.SearchFieldMatches;

/**
 * A page of a keyword search of tasks for unified search.
 *
 * @param tasks the tasks of the page
 * @param size the number of tasks the search matches
 * @param matches what matched in other add-ons' data, per task id
 */
public record TaskKeywordSearchPage(List<TaskDto> tasks, long size, Map<Long, SearchFieldMatches> matches) {
}
