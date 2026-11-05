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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The line a search result shows when it matched in a contributor's data:
 * an icon with its tooltip, a few « label: value » items, and how many more
 * matched. Every text is localized by the contributor; the host shows it as
 * given.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchFieldMatches {

  /** The icon class shown first on the line. */
  private String                 icon;

  /** The tooltip of the icon. */
  private String                 tooltip;

  /** The matched items shown, at most a few. */
  private List<SearchFieldMatch> items;

  /** The number of other matched items, not shown. */
  private int                    more;

  /** The text shown for the other items when {@link #more} is above 0. */
  private String                 moreLabel;

}
