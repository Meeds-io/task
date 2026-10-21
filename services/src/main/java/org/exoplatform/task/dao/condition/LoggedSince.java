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
package org.exoplatform.task.dao.condition;

import java.util.Date;
import java.util.List;

/**
 * The value of a {@link SingleCondition#LOGGED_SINCE} condition: a task has
 * one change log entry whose action is one of {@code actionNames} and whose
 * date is at or after {@code since}.
 *
 * @param actionNames the change log action names, such as
 *          {@code edit_status}
 * @param since the earliest date of the entry, included
 */
public record LoggedSince(List<String> actionNames, Date since) {
}
