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

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.util.TaskUtil;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The filter of a task list, as the Tasks views send it: a project or one of
 * the personal lists, the usual criteria, the grouping, the order and the
 * page, and the conditions other add-ons contribute through
 * {@link io.meeds.task.plugin.TaskFilterPlugin}.
 */
@Data
@NoArgsConstructor
public class TaskListFilter {

  private String          query;

  private long            projectId;

  private String          keyword;

  private List<Long>      labels;

  private TaskUtil.DUE    dueDate;

  private Priority        priority;

  private List<String>    assignees;

  private List<String>    coworkers;

  private List<String>    watchers;

  private Long            labelId;

  private Long            statusId;

  private Identity        currIdentity;

  private String          dueCategory;

  private String          spaceGroupId;

  private TimeZone        userTimezone;

  private boolean         showCompleted;

  private boolean         advanceSearch;

  private boolean         noProjPermission;

  private boolean         noLblPermission;

  private String          orderBy;

  private String          groupBy;

  private int             offset;

  private int             limit;

  /** Combined with AND with the other criteria, never null. */
  private List<Condition> extensionConditions = new ArrayList<>();

}
