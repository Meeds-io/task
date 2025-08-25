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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "TaskLabelTaskMapping")
@Table(name = "TASK_LABEL_TASK")
@NamedQuery(name = "LabelTaskMapping.countLabelMappingsOfTask",
  query = "SELECT count(m) FROM TaskLabelTaskMapping m WHERE m.task.id = :taskId")
@NamedQuery(name = "LabelTaskMapping.findLabelMappingsOfTask",
  query = "SELECT m FROM TaskLabelTaskMapping m WHERE m.task.id = :taskId")
@NamedQuery(name = "LabelTaskMapping.removeLabelTaskMapping",
    query = "DELETE FROM TaskLabelTaskMapping m WHERE m.label.id = :labelId")
@NamedQuery(name = "LabelTaskMapping.findLabelMapping",
     query = "SELECT m FROM TaskLabelTaskMapping m  WHERE m.label.id = :labelId and  m.task.id = :taskId")
@Data
public class LabelTaskMapping implements Serializable {

  private static final long serialVersionUID = -8180443225233907972L;

  @Id
  @ManyToOne
  @JoinColumn(name = "LABEL_ID")
  private Label             label;

  @Id
  @ManyToOne
  @JoinColumn(name = "TASK_ID")
  @EqualsAndHashCode.Exclude
  private Task              task;

  public LabelTaskMapping() {
  }

  public LabelTaskMapping(Label label, Task task) {
    super();
    this.label = label;
    this.task = task;
  }
}
