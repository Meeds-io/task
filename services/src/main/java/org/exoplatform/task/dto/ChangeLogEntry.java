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

import org.exoplatform.task.domain.ChangeLog;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.service.UserService;

import java.io.Serializable;

public class ChangeLogEntry implements Serializable {

    private long id;

    private Task task;

    private String author;
    
    private String authorFullName;

    private String authorAvatarUrl;

    private String actionName;

    private String target;
    
    private String targetFullName;

    private boolean external;

    private boolean isTargetFullNameExternal;

    private long createdTime = System.currentTimeMillis();

    public ChangeLogEntry() {
    }

    public ChangeLogEntry(ChangeLog changeLog,UserService userService) {
        
        this.id = changeLog.getId();
        
        this.task = changeLog.getTask();
        
        this.author = changeLog.getAuthor();
        
        this.authorFullName = userService.loadUser(changeLog.getAuthor()).getDisplayName();

        this.authorAvatarUrl = userService.loadUser(changeLog.getAuthor()).getAvatar();
        
        this.actionName = changeLog.getActionName();
        
        this.target = changeLog.getTarget();
        
        this.targetFullName = userService.loadUser(changeLog.getTarget()).getDisplayName();
        
        this.createdTime = changeLog.getCreatedTime();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorFullName() {
        return authorFullName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorFullName(String authorFullName) {
        this.authorFullName = authorFullName;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTargetFullName() {
        return targetFullName;
    }

    public void setTargetFullName(String targetFullName) {
        this.targetFullName = targetFullName;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public boolean getExternal() { return external; }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public boolean getIsTargetFullNameExternal() { return isTargetFullNameExternal; }

    public void setIsTargetFullNameExternal(boolean isTargetFullNameExternal) {
        this.isTargetFullNameExternal = isTargetFullNameExternal;
    }
}
