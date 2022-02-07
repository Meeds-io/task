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

import lombok.Data;
import org.exoplatform.task.domain.Project;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


@Data
public class UserSettingDto implements Serializable {
    private String username;

    private boolean showHiddenProject = false;

    private boolean showHiddenLabel = false;

    private Set<Project> hiddenProjects;

    public UserSettingDto clone() {
        UserSettingDto setting = new UserSettingDto();
        setting.setShowHiddenProject(isShowHiddenProject());
        Set<Project> hiddenProjects = new HashSet<Project>();
        if (getHiddenProjects() != null) {
            for (Project p : getHiddenProjects()) {
                hiddenProjects.add(p.clone(false));
            }
        }
        setting.setHiddenProjects(hiddenProjects);
        return setting;
    }

}
