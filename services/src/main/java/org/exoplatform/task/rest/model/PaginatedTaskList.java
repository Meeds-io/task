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
package org.exoplatform.task.rest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.model.CommentModel;
import org.exoplatform.task.model.User;
import org.exoplatform.task.util.UserUtil;

import lombok.Data;

@Data
public class PaginatedTaskList {
  private List<TaskEntity> tasks = new ArrayList<TaskEntity>();
  private long tasksNumber;

  public PaginatedTaskList() {
  }

  public PaginatedTaskList(List<TaskEntity> tasks, long tasksNumber) {
    this.tasks = tasks;
    this.tasksNumber = tasksNumber;
  }

}