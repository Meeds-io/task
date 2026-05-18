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
package org.exoplatform.task.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import io.meeds.common.persistence.PortableSequence;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "TaskProject")
@Table(name = "TASK_PROJECTS")
@NamedQuery(
        name = "TaskProject.findAllProjects",
        query = "SELECT DISTINCT p FROM TaskProject p ORDER BY p.lastModifiedDate DESC"
)
@NamedQuery(
            name = "TaskProject.countAllProjects",
            query = "SELECT count(p) FROM TaskProject p"
)
@NamedQuery(
            name = "TaskProject.findProjectsByKeyword",
            query = "SELECT DISTINCT p FROM TaskProject p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) order by p.lastModifiedDate desc"
)
@NamedQuery(
            name = "TaskProject.findProjectsByIDs",
            query = "SELECT DISTINCT p FROM TaskProject p WHERE p.id in (:ids) order by p.lastModifiedDate desc"
)
@NamedQuery(
            name = "TaskProject.countProjectsByKeyword",
            query = "SELECT count(p) FROM TaskProject p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))"
)
@NamedQuery(
            name = "TaskProject.findProjectsByMemberships",
            query = "SELECT DISTINCT p FROM TaskProject p"
                + " LEFT JOIN p.manager manager "
                + " LEFT JOIN p.participator participator "
                + " WHERE manager IN (:memberships) OR participator IN (:memberships)"
                + " order by p.lastModifiedDate desc"
)
@NamedQuery(
            name = "TaskProject.countProjectsByMemberships",
            query = "SELECT count(p) FROM TaskProject p"
                + " LEFT JOIN p.manager manager "
                + " LEFT JOIN p.participator participator "
                + " WHERE manager IN (:memberships) OR participator IN (:memberships)"
)
@NamedQuery(
            name = "TaskProject.findProjectsByMembershipsByKeyword",
            query = "SELECT DISTINCT p FROM TaskProject p "
                + " LEFT JOIN p.manager manager "
                + " LEFT JOIN p.participator participator "
                + " WHERE (manager IN (:memberships) OR participator IN (:memberships)) AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))"
                + " ORDER BY p.lastModifiedDate DESC"
)
@NamedQuery(
            name = "TaskProject.countProjectsByMembershipsByKeyword",
            query = "SELECT count(p) FROM TaskProject p "
                + " LEFT JOIN p.manager manager "
                + " LEFT JOIN p.participator participator "
                + " WHERE (manager IN (:memberships) OR participator IN (:memberships)) AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))"
)
@Data
public class Project implements Serializable {

  private static final long  serialVersionUID = -5595949787344061794L;

  public static final String PREFIX_CLONE     = "Copy of ";

  @Id
  @PortableSequence(name = "SEQ_TASK_PROJECTS_PROJECT_ID")
  @Column(name = "PROJECT_ID")
  private long               id;

  private String             name;

  private String             description;

  private String             color;

  @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @EqualsAndHashCode.Exclude
  private Set<Status>        status;

  @ElementCollection
  @CollectionTable(name = "TASK_PROJECT_MANAGERS", joinColumns = @JoinColumn(name = "PROJECT_ID"))
  private Set<String>        manager;

  @ElementCollection
  @CollectionTable(name = "TASK_PROJECT_PARTICIPATORS", joinColumns = @JoinColumn(name = "PROJECT_ID"))
  private Set<String>        participator;

  @Temporal(TemporalType.DATE)
  @Column(name = "DUE_DATE")
  private Date               dueDate;

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @EqualsAndHashCode.Exclude
  @JoinColumn(name = "PARENT_PROJECT_ID", nullable = true)
  private Project            parent;

  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @EqualsAndHashCode.Exclude
  private List<Project>      children;

  // This field is used for remove cascade
  @ManyToMany(mappedBy = "hiddenProjects", fetch = FetchType.LAZY)
  @EqualsAndHashCode.Exclude
  private Set<UserSetting>   hiddenOn;

  // This field is used for remove cascade
  @ManyToMany(mappedBy = "project", fetch = FetchType.LAZY)
  @EqualsAndHashCode.Exclude
  private Set<Label>         labels;

  @Column(name = "LAST_MODIFIED_DATE")
  private Long lastModifiedDate;

  public Project() {
  }

  public Project(String name, String description, Set<Status> status, Set<String> manager, Set<String> participator) {
    this.name = name;
    this.description = description;
    this.status = status;
    this.manager = manager;
    this.participator = participator;
  }

  @Override
  public Project clone() { // NOSONAR
    Project project = new Project(this.getName(),
                                  this.getDescription(),
                                  new HashSet<>(),
                                  null,
                                  null);
    project.setId(getId());
    project.setColor(this.getColor());
    project.setDueDate(this.getDueDate());
    if (this.getParent() != null) {
      project.setParent(getParent().clone());
    }
    project.status = new HashSet<>();
    project.children = new LinkedList<>();
    return project;
  }

  public boolean canView(Identity user) {
    Set<String> permissions = new HashSet<>(getParticipator());
    permissions.addAll(getManager());

    return hasPermission(user, permissions);
  }

  public boolean canEdit(Identity user) {
    return hasPermission(user, getManager());
  }

  private boolean hasPermission(Identity user, Set<String> permissions) {
    if (permissions.contains(user.getUserId())) {
      return true;
    } else {
      Set<MembershipEntry> memberships = new HashSet<>();
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

}
