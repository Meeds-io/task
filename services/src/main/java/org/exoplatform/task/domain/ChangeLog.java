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

import java.util.Calendar;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Entity(name = "TaskChangeLog")
@Table(name = "TASK_CHANGE_LOGS")
@NamedQuery(name = "TaskChangeLog.findChangeLogByTaskId",
            query = "SELECT log FROM TaskChangeLog log WHERE log.task.id = :taskId ORDER BY log.createdTime DESC")
@NamedQuery(name = "TaskChangeLog.countChangeLogByTaskId",
        query = "SELECT count(log) FROM TaskChangeLog log WHERE log.task.id = :taskId")
@NamedQuery(name = "TaskChangeLog.removeChangeLogByTaskId",
        query = "DELETE FROM TaskChangeLog log WHERE log.task.id = :taskId")
@Data
public class ChangeLog implements Comparable<ChangeLog> {

  @Id
  @SequenceGenerator(name="SEQ_TASK_CHANGE_LOG_ID", sequenceName="SEQ_TASK_CHANGE_LOG_ID", allocationSize = 1)
  @GeneratedValue(strategy= GenerationType.AUTO, generator="SEQ_TASK_CHANGE_LOG_ID")
  @Column(name = "CHANGE_LOG_ID")
  private long id;

  @ManyToOne
  @JoinColumn(name = "TASK_ID")
  private Task task;
  
  private String author;

  @Column(name="ACTION_NAME")
  private String actionName;

  private String target;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "CREATED_TIME")
  private Date   createdTime = Calendar.getInstance().getTime();

  @Override
  public int compareTo(ChangeLog o) {
    return getCreatedTime().compareTo(o.getCreatedTime());
  }

  @Override
  public ChangeLog clone() { // NOSONAR
    ChangeLog log = new ChangeLog();
    log.setId(getId());
    log.setTask(getTask().clone());
    log.setAuthor(getAuthor());
    log.setActionName(getActionName());
    log.setCreatedTime(getCreatedTime());
    log.setTarget(getTarget());

    return log;
  }
}
