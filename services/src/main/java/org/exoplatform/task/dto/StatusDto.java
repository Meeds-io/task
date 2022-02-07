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
import lombok.NoArgsConstructor;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;

import java.io.Serializable;
import java.util.*;


@Data
@NoArgsConstructor
public class StatusDto implements Serializable {
    private Long id;

    private String name;

    private Integer rank;

    private List<Task> tasks;

    private ProjectDto project;

    public StatusDto(StatusDto status) {
        this.id=status.getId();
        this.name=status.getName();
        this.rank=status.getRank();
        this.project=status.getProject();
    }

    public StatusDto(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public StatusDto(long id, String name, Integer rank, ProjectDto project) {
        this.id = id;
        this.name = name;
        this.rank = rank;
        this.project = project;
    }

    public StatusDto clone() {
        StatusDto status = new StatusDto(getId(), getName(), getRank(), getProject().clone(false));

        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatusDto status = (StatusDto) o;

        if (id != status.getId()) return false;
        if (name != null ? !name.equals(status.getName()) : status.getName() != null) return false;
        if (project != null ? !project.equals(status.getProject()) : status.getProject() != null) return false;
        if (rank != null ? !rank.equals(status.getRank()) : status.getRank() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, rank, project);
    }

}
