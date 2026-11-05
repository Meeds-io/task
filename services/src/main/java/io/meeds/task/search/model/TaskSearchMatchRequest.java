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
import java.util.Locale;

import org.exoplatform.services.security.Identity;

/**
 * What a task search contributor is asked for.
 *
 * @param viewer the user who searches, with their memberships
 * @param term the keyword as the user typed it
 * @param spaceIds the spaces the search is restricted to, empty for all
 * @param limit the number of results the search shows
 * @param locale the viewer's language, for the labels of what matched
 */
public record TaskSearchMatchRequest(Identity viewer, String term, List<Long> spaceIds, int limit, Locale locale) {
}
