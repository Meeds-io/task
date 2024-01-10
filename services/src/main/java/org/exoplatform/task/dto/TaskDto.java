/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.task.domain.Priority;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto implements Serializable {

  private static final long serialVersionUID = -1137676834189711097L;

  private long        id;

  private String      title;

  private String      description;

  private Priority    priority;

  private String      context;

  private String      assignee;

  private StatusDto   status;

  private int         rank;

  private boolean     completed;

  private Set<String> coworker;

  private Set<String> watcher;

  private String      createdBy;

  private Date        createdTime;

  private Date        startDate;

  private Date        endDate;

  private Date        dueDate;

  private String      activityId;

  public TaskDto clone() { // NOSONAR
    return new TaskDto(id,
                       title,
                       description,
                       priority,
                       context,
                       assignee,
                       status,
                       rank,
                       completed,
                       coworker == null ? null : new HashSet<>(coworker),
                       watcher == null ? null : new HashSet<>(watcher),
                       createdBy,
                       createdTime,
                       startDate,
                       endDate,
                       dueDate,
                       activityId);
  }

}
