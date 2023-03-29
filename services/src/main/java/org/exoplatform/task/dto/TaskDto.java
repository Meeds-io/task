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

import lombok.Getter;
import lombok.Setter;
import org.exoplatform.task.domain.*;
import java.io.Serializable;
import java.util.*;
import lombok.Data;


@Data
@Getter
@Setter
public class TaskDto  implements Serializable {
    private long        id;
    private String      title;
    private String      description;
    private Priority priority;
    private String      context;
    private String      assignee;
    private StatusDto status;
    private int         rank;
    private boolean completed;
    private Set<String> coworker;
    private Set<String> watcher;
    private String      createdBy;
    private Date createdTime;
    private Date        startDate;
    private Date        endDate;
    private Date        dueDate;
    private String activityId;

    public TaskDto clone() {
        Set<String> coworkerClone = new HashSet<String>();
        Set<String> watcherClone = new HashSet<String>();
        if (getCoworker() != null) {
            coworkerClone = new HashSet<String>(getCoworker());
        }
        if (getWatcher() != null) {
            watcherClone = new HashSet<String>(getWatcher());
        }
        TaskDto newTask = new TaskDto();
        newTask.setTitle(this.getTitle());
        newTask.setDescription(this.getDescription());
        newTask.setPriority(this.getPriority());
        newTask.setContext(this.getContext());
        newTask.setAssignee(this.getAssignee());
        newTask.setCoworker(coworkerClone);
        newTask.setWatcher(watcherClone);
        newTask.setStatus(this.getStatus() != null ? this.getStatus().clone() : null);
        newTask.setRank(this.getRank());
        newTask.setActivityId(this.getActivityId());
        newTask.setCompleted(this.isCompleted());
        newTask.setCreatedBy(this.getCreatedBy());
        newTask.setCreatedTime(this.getCreatedTime());
        newTask.setEndDate(this.getEndDate());
        newTask.setStartDate(this.getStartDate());
        newTask.setDueDate(this.getDueDate());
        newTask.setCreatedTime(getCreatedTime());
        newTask.setActivityId(getActivityId());
        newTask.setCompleted(isCompleted());
        newTask.setRank(getRank());
        newTask.setId(getId());

        return newTask;
    }

}
