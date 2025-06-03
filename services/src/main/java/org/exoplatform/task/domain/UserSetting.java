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
package org.exoplatform.task.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@Entity(name = "TaskUserSetting")
@Table(name = "TASK_USER_SETTINGS")
@EqualsAndHashCode
public class UserSetting implements Serializable {

  private static final long serialVersionUID  = 731610709234552969L;

  @Id
  @Column(name = "USERNAME")
  private String username;

  @Column(name = "SHOW_HIDDEN_PROJECT")
  private boolean showHiddenProject = false;
  
  @Column(name = "SHOW_HIDDEN_LABEL")
  private boolean showHiddenLabel = false;

  @ManyToMany
  @JoinTable(
    name = "TASK_HIDDEN_PROJECTS",
    joinColumns = {
      @JoinColumn(name = "USERNAME", referencedColumnName = "USERNAME")
    },
    inverseJoinColumns = {
      @JoinColumn(name = "PROJECT_ID", referencedColumnName = "PROJECT_ID")
    }
  )
  private Set<Project> hiddenProjects = new HashSet<>();

  public UserSetting() {
  }

  public UserSetting(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public boolean isShowHiddenProject() {
    return showHiddenProject;
  }

  public void setShowHiddenProject(boolean showHiddenProject) {
    this.showHiddenProject = showHiddenProject;
  }

  public boolean isShowHiddenLabel() {
    return showHiddenLabel;
  }

  public void setShowHiddenLabel(boolean showHiddenLabel) {
    this.showHiddenLabel = showHiddenLabel;
  }

  public Set<Project> getHiddenProjects() {
    return hiddenProjects;
  }

  public void setHiddenProjects(Set<Project> hiddenProjects) {
    this.hiddenProjects = hiddenProjects;
  }

  @Override
  public UserSetting clone() { // NOSONAR
    UserSetting setting = new UserSetting(getUsername());
    setting.setShowHiddenProject(isShowHiddenProject());
    return setting;
  }
}
