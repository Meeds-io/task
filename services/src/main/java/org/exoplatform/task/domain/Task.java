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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "TaskTask")
@Table(name = "TASK_TASKS")
@NamedQuery(
    name = "Task.findByMemberships",
    query = """
  SELECT ta FROM TaskTask ta
  LEFT JOIN ta.coworker coworkers
  WHERE ta.assignee = :userName
  OR ta.createdBy = :userName
  OR coworkers = :userName
  OR ta.status.id IN (
    SELECT st.id FROM TaskStatus st
    INNER JOIN st.project project
    WHERE project.id IN (
      SELECT pr1.id FROM TaskProject pr1
      LEFT JOIN pr1.manager managers
      WHERE managers IN :memberships
    )
    OR project.id IN (
      SELECT pr2.id FROM TaskProject pr2
      LEFT JOIN pr2.participator participators
      WHERE participators IN :memberships
    )
  )
""")
@NamedQuery(name = "Task.findTaskByProject",
    query = "SELECT t FROM TaskTask t WHERE t.status.project.id = :projectId")
@NamedQuery(name = "Task.findTaskByActivityId",
    query = "SELECT t FROM TaskTask t WHERE t.activityId = :activityId")
@NamedQuery(name = "Task.getCoworker",
    query = "SELECT c FROM TaskTask t inner join t.coworker c WHERE t.id = :taskid")
@NamedQuery(name = "Task.getWatcher",
    query = "SELECT w FROM TaskTask t inner join t.watcher w WHERE t.id = :taskid")
@NamedQuery(name = "Task.updateStatus",
    query = "UPDATE TaskTask t SET t.status = :status_new WHERE t.status = :status_old")
@NamedQuery(name = "Task.getTaskWithCoworkers",
    query = "SELECT t FROM TaskTask t LEFT JOIN FETCH t.coworker c WHERE t.id = :taskid")
@NamedQuery(name = "Task.getUncompletedTasks",
    query = "SELECT ta FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND (ta.assignee = :userName " +
            "OR :userName in (select co FROM ta.coworker co) " +
            ")")
@NamedQuery(name = "Task.countUncompletedTasks",
    query = "SELECT COUNT(ta) FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND (ta.assignee = :userName " +
            "OR :userName in (select co FROM ta.coworker co) " +
            ")")
@NamedQuery(name = "Task.getCollaboratedTasks",
    query = "SELECT ta FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND :userName in (select co FROM ta.coworker co) ")
@NamedQuery(name = "Task.countCollaboratedTasks",
    query = "SELECT COUNT(ta) FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND :userName in (select co FROM ta.coworker co) ")

 @NamedQuery(name = "Task.getAssignedTasks",
    query = "SELECT ta FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND ta.assignee = :userName ")

 @NamedQuery(name = "Task.countAssignedTasks",
    query = "SELECT COUNT(ta)  FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND ta.assignee = :userName ")

 @NamedQuery(name = "Task.getWatchedTasks",
    query = "SELECT ta FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND :userName in (select wa FROM ta.watcher wa) ")

 @NamedQuery(name = "Task.countWatchedTasks",
    query = "SELECT COUNT(ta)  FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND :userName in (select wa FROM ta.watcher wa) ")

@NamedQuery(name = "Task.getOverdueTasks",
    query = "SELECT ta FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND ta.dueDate < CURRENT_DATE " +
            "AND (ta.assignee = :userName " +
            "OR :userName in (select co FROM ta.coworker co) " +
            ")")
@NamedQuery(name = "Task.countOverdueTasks",
    query = "SELECT COUNT(ta) FROM TaskTask ta " +
            "WHERE ta.completed = false " +
            "AND ta.dueDate < CURRENT_DATE " +
            "AND (ta.assignee = :userName " +
            "OR :userName in (select co FROM ta.coworker co) " +
            ")")
@NamedQuery(
    name = "Task.findTasks",
    query = "SELECT ta FROM TaskTask ta " +
        "WHERE (ta.assignee = :userName OR ta.createdBy = :userName OR :userName in (select co FROM ta.coworker co) OR (SELECT participator FROM TaskProject p LEFT JOIN p.participator participator where p.id = ta.status.project.id) IN (:memberships) ) " +
        "ORDER BY ta.createdTime DESC"
)
@NamedQuery(
    name = "Task.countTasks",
    query = "SELECT COUNT(ta) FROM TaskTask ta " +
        "WHERE (ta.assignee = :userName OR ta.createdBy = :userName OR :userName in (select co FROM ta.coworker co)) "
)
@NamedQuery(name = "Task.countTaskStatusByProject",
        query = "SELECT m.status.name AS name, COUNT(m) AS total FROM TaskTask AS m where m.status.project.id = :projectId and m.completed = false GROUP BY m.status.name ORDER BY m.status.name ASC")

@NamedQuery(name = "Task.getByStatus",
        query = "SELECT t FROM TaskTask t  WHERE t.status.id = :statusid")

@NamedQuery(name = "Task.getAllIds",
        query = "SELECT t.id FROM TaskTask t ORDER BY id ASC LIMIT :limit OFFSET :offset")
@Data
public class Task implements Serializable {

  private static final long     serialVersionUID = 1945617316471028822L;

  public static final String    PREFIX_CLONE     = "Copy of ";

  @Id
  @SequenceGenerator(name = "SEQ_TASK_TASKS_TASK_ID", sequenceName = "SEQ_TASK_TASKS_TASK_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_TASK_TASKS_TASK_ID")
  @Column(name = "TASK_ID")
  private Long                  id;

  private String                title;

  private String                description;

  @Enumerated(EnumType.ORDINAL)
  private Priority              priority;

  private String                context;

  private String                assignee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STATUS_ID")
  @EqualsAndHashCode.Exclude
  private Status                status;

  @Column(name = "TASK_RANK")
  private int                   rank;

  private boolean               completed        = false;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "TASK_TASK_COWORKERS", joinColumns = @JoinColumn(name = "TASK_ID"))
  @EqualsAndHashCode.Exclude
  private Set<String> coworker;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "TASK_TASK_WATCHERS", joinColumns = @JoinColumn(name = "TASK_ID"))
  @EqualsAndHashCode.Exclude
  private Set<String> watcher;

  @Column(name = "CREATED_BY")
  private String      createdBy;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "CREATED_TIME")
  private Date        createdTime = Calendar.getInstance().getTime();

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "START_DATE")
  private Date        startDate;
  
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "END_DATE")
  private Date        endDate;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "DUE_DATE")
  private Date        dueDate;

  //This field is only used for remove cascade
  @OneToMany(mappedBy = "task", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @EqualsAndHashCode.Exclude
  private List<Comment> comments; // NOSONAR

  //This field is only used for remove cascade
  @OneToMany(mappedBy = "task", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @EqualsAndHashCode.Exclude
  private List<ChangeLog> logs; // NOSONAR

  @OneToMany(mappedBy = "task", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @EqualsAndHashCode.Exclude
  private List<LabelTaskMapping> labels;

  @Column(name = "ACTIVITY_ID")
  private String activityId;

  public Task() {
    this.priority = Priority.NORMAL;
  }

  public Task(String title, // NOSONAR
              String description,
              Priority priority,
              String context,
              String assignee,
              Set<String> coworker,
              Set<String> watcher,
              Status status,
              String createdBy,
              Date endDate,
              Date startDate,
              Date dueDate) {
    this.title = title;
    this.assignee = assignee;
    this.coworker = coworker;
    this.watcher = watcher;
    this.context = context;
    this.createdBy = createdBy;
    this.description = description;
    this.priority = priority;
    this.startDate = startDate;
    this.endDate = endDate;
    this.dueDate = dueDate;
    this.status = status;
  }

  @Override
  public Task clone() { //NOSONAR
    Set<String> coworkerClone = new HashSet<>();
    Set<String> watcherClone = new HashSet<>();
    if (getCoworker() != null) {
      coworkerClone = new HashSet<>(getCoworker());
    }
    if (getWatcher() != null) {
      watcherClone = new HashSet<>(getWatcher());
    }
    Task newTask = new Task(this.getTitle(), this.getDescription(), this.getPriority(), this.getContext(), this.getAssignee(), coworkerClone, watcherClone, this.getStatus() != null ? this.getStatus().clone() : null, this.getCreatedBy() , this.getEndDate(), this.getStartDate(), this.getDueDate());

    newTask.setCreatedTime(getCreatedTime());
    newTask.setActivityId(getActivityId());
    newTask.setCompleted(isCompleted());
    newTask.setRank(getRank());
    newTask.setId(getId());

    return newTask;
  }
}
