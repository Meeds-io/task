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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.model.CommentModel;
import org.exoplatform.task.model.User;
import org.exoplatform.task.util.UserUtil;

@Data
public class TaskEntity {
  long Id;
  Date dueDate;
  private StatusDto status;
  private TaskDto task;
  private boolean readOnly;
  private boolean isWatched;
  private String breadcumbs;
  private User assignee;
  private User createdBy;
  private List<User> coworker;
  private List<User> watcher;
  private int numberCoworkers;
  private long commentCount;
  private List<CommentModel> comments;
  private User currentUser;
  private SpaceEntity space;
  private List<LabelDto> labels = new ArrayList<LabelDto>();
  private boolean completed;

  public TaskEntity() {
  }

  public TaskEntity(TaskDto task, long commentCount) {
    this.Id=task.getId();
    this.dueDate=task.getDueDate();
    this.status=task.getStatus();
    this.task = task;
    this.completed = task.isCompleted();
    this.commentCount = commentCount;
    this.assignee = UserUtil.getUser(task.getAssignee());
    this.createdBy = UserUtil.getUser(task.getCreatedBy());
    if(task.getCoworker()!=null) this.coworker = task.getCoworker().stream().map(UserUtil::getUser).collect(Collectors.toList());
    if(task.getWatcher()!=null) this.watcher = task.getWatcher().stream().map(UserUtil::getUser).collect(Collectors.toList());
  }

}
