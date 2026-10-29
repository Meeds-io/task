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
package io.meeds.task.plugin;

import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.condition.Condition;

/**
 * A filter another add-on contributes to the tasks of a project: given the
 * payload the client sent under the plugin's id, it returns a condition that
 * the task query of the project's views carries, combined with AND with the
 * project restriction and the usual filters. The page, the count and the
 * project's view permission therefore stay the task query's own.
 * <p>
 * A plugin registers itself through
 * {@link io.meeds.task.service.TaskFilterExtensionService#addPlugin(TaskFilterPlugin)}.
 * It holds no business logic: it delegates the parsing and the predicate to a
 * Service of its add-on.
 */
public interface TaskFilterPlugin {

  /**
   * Returns the id the client names the filter by, unique among the plugins.
   *
   * @return the plugin id, such as {@code forms.fields}
   */
  String getId();

  /**
   * Builds the condition of a payload. It is called only once the viewer is
   * known to view the project. The condition must only narrow the query, as a
   * {@link org.exoplatform.task.dao.condition.SingleCondition} or a
   * {@link org.exoplatform.task.dao.condition.PredicateCondition} does, never
   * place a restriction under an OR.
   *
   * @param projectId the project whose tasks are filtered
   * @param payload the JSON value the client sent under the plugin's id, as
   *          JSON text: a string arrives quoted
   * @param viewer the user who filters
   * @return the condition, or {@code null} when the payload restricts nothing
   * @throws IllegalArgumentException with a message code when the payload is
   *           invalid
   */
  Condition toCondition(long projectId, String payload, Identity viewer);

}
