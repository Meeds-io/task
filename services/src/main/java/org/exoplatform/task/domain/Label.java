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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

@Entity(name = "TaskLabel")
@Table(name = "TASK_LABELS")
@NamedQuery(name = "Label.findLabelsByTask",
  query = "SELECT lbl FROM TaskLabel lbl inner join lbl.tasks m WHERE lbl.project.id = :projectId AND m.task.id = :taskid ORDER BY lbl.id")
@NamedQuery(name = "Label.findLabelsByTaskCount",
  query = "SELECT count(*) FROM TaskLabel lbl inner join lbl.tasks m WHERE lbl.project.id = :projectId AND m.task.id = :taskid")
@NamedQuery(name = "Label.findLabelsByProject",
  query = "SELECT lbl FROM TaskLabel lbl WHERE lbl.project.id = :projectId ORDER BY lbl.id")
@NamedQuery(name = "Label.findLabelsByProjectCount",
  query = "SELECT count(*) FROM TaskLabel lbl WHERE lbl.project.id = :projectId ORDER BY lbl.id")
@Data
public class Label implements Serializable {

  private static final long serialVersionUID = 806731692361018042L;

  @Id
  @PortableSequence(name = "SEQ_TASK_LABELS_LABEL_ID")
  @Column(name = "LABEL_ID")
  private long              id;

  @Column(name = "USERNAME", nullable = false)
  private String            username;

  private String            name;

  private String            color;

  private boolean           hidden;

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_LABEL_ID", nullable = true)
  @EqualsAndHashCode.Exclude
  private Label parent;

  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
  @EqualsAndHashCode.Exclude
  private List<Label> children = new LinkedList<>();
  
  //Still use for named query
  @OneToMany(mappedBy = "label", fetch=FetchType.LAZY)
  @EqualsAndHashCode.Exclude
  private Set<LabelTaskMapping> tasks = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "PROJECT_ID")
  @EqualsAndHashCode.Exclude
  private Project project;

  public Label() {
  }

  public Label(String name, String username, Project project) {
    this.name = name;
    this.username = username;
    this.project = project;
  }

}
