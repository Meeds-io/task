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
import org.exoplatform.task.domain.Label;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class LabelDto implements Serializable {
    private long      id;

    private String username;

    private String    name;

    private String    color;

    private boolean hidden;

    private boolean canEdit;

    private LabelDto parent;

    private ProjectDto project;

    private List<LabelDto> children;


    public LabelDto getParent() {
        return parent;
    }

    public void setParent(LabelDto parent) {
        if(this.equals(parent)){
          this.parent = null;
        } else {
            this.parent = parent;
        }
    }
}
