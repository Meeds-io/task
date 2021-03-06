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
package org.exoplatform.task.service;

import org.exoplatform.task.domain.Priority;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class TaskBuilder {
  private String title;
  private String description;

  private Priority priority = Priority.NORMAL;
  private String context;
  private String assignee;
  private Set<String> coworker;
  private Set<String> watcher;

  private StatusDto status;

  private String createdBy;
  private Date createdTime = new Date();

  private Date endDate;
  private Date startDate;
  private Date dueDate;

  public TaskDto build() {
    TaskDto task = new TaskDto();

    task.setTitle(title);
    task.setDescription(description);
    task.setPriority(priority);
    task.setContext(context);
    task.setAssignee(assignee);
    task.setCoworker(coworker);
    task.setWatcher(watcher);
    task.setStatus(status);
    task.setCreatedBy(createdBy);
    task.setCreatedTime(createdTime);
    task.setEndDate(endDate);
    task.setStartDate(startDate);
    task.setDueDate(dueDate);

    return task;
  }

  public TaskBuilder withTitle(String title) {
    this.title = title;
    return this;
  }
  public TaskBuilder withDescription(String description) {
    this.description = description;
    return this;
  }
  public TaskBuilder withPriority(Priority priority) {
    this.priority = priority;
    return this;
  }

  public TaskBuilder withAssignee(String assignee) {
    this.assignee = assignee;
    return this;
  }

  public TaskBuilder withWatcher(Set<String> watcher) {
    this.watcher = watcher;
    return this;
  }

  public TaskBuilder withCoworker(Set<String> coworker) {
    this.coworker = coworker;
    return this;
  }

  public TaskBuilder addCoworker(String coworker) {
    if (this.coworker == null) {
      this.coworker = new HashSet<String>();
    }
    this.coworker.add(coworker);
    return this;
  }

  public TaskBuilder withContext(String context) {
    this.context = context;
    return this;
  }

  public TaskBuilder withDueDate(Date date) {
    this.dueDate = date;
    return this;
  }

  public TaskBuilder withStatus(StatusDto status) {
    this.status = status;
    return this;
  }

  public TaskBuilder withCreatedBy(String username) {
    this.createdBy = username;
    return this;
  }

  public TaskBuilder withEndDate(Date endDate) {
    this.endDate = endDate;
    return this;
  }

  public TaskBuilder withStartDate(Date date) {
    this.startDate = date;
    return this;
  }
}
