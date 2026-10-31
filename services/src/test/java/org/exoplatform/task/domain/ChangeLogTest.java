/*
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
package org.exoplatform.task.domain;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;

/**
 * The natural order of change log entries.
 */
public class ChangeLogTest {

  /**
   * Entries sort by date, an undated one (recorded before 7.2 on an upgraded
   * PostgreSQL, Oracle or SQL Server database) first. Mutant: the dates
   * compared with no rule for a missing one, which throws.
   */
  @Test
  public void testUndatedEntryIsTheOldest() {
    ChangeLog newer = entry("newer", new Date(2000));
    ChangeLog undated = entry("undated", null);
    ChangeLog older = entry("older", new Date(1000));
    List<ChangeLog> entries = new ArrayList<>(List.of(newer, undated, older));

    Collections.sort(entries);

    assertEquals(List.of("undated", "older", "newer"), entries.stream().map(ChangeLog::getActionName).toList());
  }

  /**
   * @param actionName the entry's action
   * @param createdTime its date, null for none
   * @return the entry
   */
  private ChangeLog entry(String actionName, Date createdTime) {
    ChangeLog entry = new ChangeLog();
    entry.setActionName(actionName);
    entry.setCreatedTime(createdTime);
    return entry;
  }
}
