/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.task.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.UserSetting;

import java.io.Serializable;
import java.util.*;


@Data
@NoArgsConstructor
public class ProjectDto implements Serializable {
    private static final Log LOG           = ExoLogger.getLogger(ProjectDto.class);

    private static final String SPACES_GROUP_PREFIX = "/spaces/";


    private long      id;

    private String    name;

    private String    description;

    private String    color;

    private Set<StatusDto> status ;

    private Set<String> manager;

    private Set<String> participator;

    private Date dueDate;

    private Long lastModifiedDate;;

    private ProjectDto parent;

    private List<ProjectDto> children;

    private Set<UserSetting> hiddenOn;

    private String spaceName;

    public ProjectDto(String name, String description, HashSet<StatusDto> statuses, Set<String> managers, Set<String> participators) {
        this.name=name;
        this.description=description;
        this.status=statuses;
        this.manager=managers;
        this.participator=participators;
    }


    public ProjectDto clone(boolean cloneTask) {
        ProjectDto project = new ProjectDto();
        project.setId(getId());
        project.setName(this.getName());
        project.setDescription(this.getDescription());
        project.setColor(this.getColor());
        project.setDueDate(this.getDueDate());
        if (this.getParent() != null) {
            project.setParent(getParent().clone(false));
        }
        project.status = new HashSet<StatusDto>();
        project.children = new LinkedList<ProjectDto>();

        return project;
    }

    public boolean canView(Identity user) {
        Set<String> permissions = new HashSet<String>();
        Set<String> Participants = getParticipator();
        Set<String> managers = getManager();
        if(Participants!=null){
            permissions.addAll(Participants);
        }
        if(managers!=null){
            permissions.addAll(managers);
        }
        return hasPermission(user, permissions);
    }

    public boolean canEdit(Identity user) {
        return hasPermission(user, getManager());
    }

    private boolean hasPermission(Identity user, Set<String> permissions) {
        if (permissions == null) {
            return false;
        }
        if (permissions.contains(user.getUserId()) && isMemberOfProjectSpaces(user)) {
            return true;
        } else {
            Set<MembershipEntry> memberships = new HashSet<MembershipEntry>();
            for (String per : permissions) {
                MembershipEntry entry = MembershipEntry.parse(per);
                if (entry != null) {
                    memberships.add(entry);
                }
            }

            for (MembershipEntry entry :  user.getMemberships()) {
                if (memberships.contains(entry)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * A project belongs to a space when its manager entries
     * reference a space group (manager entries only). A user named by id on such a project keeps the
     * right only while member of one of those spaces. A project outside any
     * space is not concerned.
     */
    private boolean isMemberOfProjectSpaces(Identity user) {
        Set<String> spaceGroups = new HashSet<String>();
        collectSpaceGroups(getManager(), spaceGroups);
        if (spaceGroups.isEmpty()) {
            return true;
        }
        if (user.getMemberships() != null) {
            for (MembershipEntry entry : user.getMemberships()) {
                if (spaceGroups.contains(entry.getGroup())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void collectSpaceGroups(Set<String> permissions, Set<String> spaceGroups) {
        if (permissions == null) {
            return;
        }
        for (String per : permissions) {
            MembershipEntry entry = MembershipEntry.parse(per);
            if (entry != null && entry.getGroup() != null && entry.getGroup().startsWith(SPACES_GROUP_PREFIX)) {
                spaceGroups.add(entry.getGroup());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ProjectDto))
            return false;
        ProjectDto other = (ProjectDto) obj;
        if (getId() != other.getId())
            return false;
        return true;
    }

}
