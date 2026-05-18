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
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import io.meeds.common.persistence.PortableSequence;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "TaskStatus")
@Table(name = "TASK_STATUS")
@NamedQuery(
    name = "Status.findLowestRankStatusByProject",
    query = "SELECT s FROM TaskStatus s WHERE"
        + " s.project.id = :projectId"
        + " AND s.rank = (SELECT MIN(s2.rank) FROM TaskStatus s2 WHERE s2.project.id = :projectId)"
)
@NamedQuery(
    name = "Status.findHighestRankStatusByProject",
    query = "SELECT s FROM TaskStatus s WHERE"
        + " s.project.id = :projectId"
        + " AND s.rank = (SELECT MAX(s2.rank) FROM TaskStatus s2 WHERE s2.project.id = :projectId)"
)
@NamedQuery(name = "Status.findByName",
            query = "SELECT s FROM TaskStatus s WHERE s.name = :name AND s.project.id = :projectID")
@NamedQuery(name = "Status.findStatusByProject",
            query = "SELECT s FROM TaskStatus s WHERE s.project.id = :projectId ORDER BY s.rank ASC")
@Data
public class Status implements Comparable<Status>, Serializable {

  private static final long serialVersionUID = -3079376553215147896L;

  @Id
  @PortableSequence(name = "SEQ_TASK_STATUS_STATUS_ID")
  @Column(name = "STATUS_ID")
  private long              id;

  private String            name;

  @Column(name = "STATUS_RANK")
  private Integer           rank;

  // This field only used for cascade remove
  @EqualsAndHashCode.Exclude
  @OneToMany(mappedBy = "status", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private List<Task>        tasks;

  @ManyToOne
  @EqualsAndHashCode.Exclude
  @JoinColumn(name = "PROJECT_ID")
  private Project           project;

  public Status() {
  }

  public Status(long id, String name) {
    this.id = id;
    this.name = name;
  }

  public Status(long id, String name, Integer rank, Project project) {
    this.id = id;
    this.name = name;
    this.rank = rank;
    this.project = project;
  }

  public Status(String name, Integer rank, Project project) {
    this.name = name;
    this.rank = rank;
    this.project = project;
  }

  @Override
  public Status clone() { // NOSONAR
    return new Status(getId(), getName(), getRank(), getProject().clone());
  }

  @Override
  public int compareTo(Status o) {
    if(getRank() == null) {
      return o.getRank() == null ? 0 : -1;
    } else if(o.getRank() == null) {
      return 1;
    }

    return getRank().compareTo(o.getRank());
  }
}