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
package io.meeds.task.search.plugin;

import io.meeds.task.search.model.TaskSearchMatchRequest;
import io.meeds.task.search.model.TaskSearchMatches;

/**
 * A contribution to the keyword search of tasks in unified search: another
 * add-on that holds searchable data about tasks (for instance values typed in
 * a form attached to a task) names the tasks matching the keyword, and what
 * matched, so that they appear among the Tasks results.
 * <p>
 * The host adds the contributed ids to its own query, inside the clause that
 * holds the viewer's permissions and every other filter: a contributed task
 * the viewer may not see by the host's own rule is never returned. The matched
 * fields are attached only to the tasks the host returns.
 * <p>
 * A contributor is a Spring {@code @Service} with an explicit bean name,
 * looked up by type when a search runs; it carries no business logic and
 * delegates to its add-on's service layer.
 */
public interface TaskSearchMatchPlugin {

  /**
   * Finds the tasks matching a keyword in the contributor's own data, for one
   * viewer. Called once per keyword search, on the request thread: the
   * contributor bounds its own time and answers an empty result, never an
   * exception, when its backend is unavailable.
   *
   * @param request the viewer, the keyword, the spaces the search is
   *          restricted to, the number of results wanted and the viewer's
   *          locale
   * @return the matching task ids, best first, and what matched for each,
   *         never null
   */
  TaskSearchMatches search(TaskSearchMatchRequest request);

}
