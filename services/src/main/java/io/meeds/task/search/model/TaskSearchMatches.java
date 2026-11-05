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
package io.meeds.task.search.model;

import java.util.List;
import java.util.Map;

/**
 * What a task search contributor answers.
 *
 * @param taskIds the matching task ids, best first
 * @param matches what matched, per task id; a task may have none
 */
public record TaskSearchMatches(List<Long> taskIds, Map<Long, SearchFieldMatches> matches) {

  /** The answer of a contributor that found nothing or could not search. */
  public static final TaskSearchMatches EMPTY = new TaskSearchMatches(List.of(), Map.of());

}
