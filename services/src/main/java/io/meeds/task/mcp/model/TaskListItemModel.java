/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.task.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.exoplatform.task.domain.Priority;

import io.meeds.task.mcp.util.TaskDescriptionText;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A task in a list: what tells the card apart on its board, as a search
 * result does, with its assignee, due date, creation date, priority and
 * comments count, without its description, coworkers or other dates, which
 * get_task_by_id returns. The creation date tells apart the tasks created
 * since a date; the date of the latest comment tells apart those nobody has
 * commented on since. On request, the description as cut plain text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
public class TaskListItemModel {

  @JsonProperty("task_id")
  private long               id;

  private String             title;

  private String             link;

  private boolean            completed;

  @JsonProperty("project_id")
  private Long               projectId;

  @JsonProperty("project_name")
  private String             projectName;

  private ProjectStatus      status;

  private List<ProjectLabel> labels;

  private TaskAssigneeModel  assignee;

  @JsonProperty("due_date")
  private String             dueDate;

  /**
   * When the task was created, formatted as the full model's created_date.
   */
  @JsonProperty("created_date")
  private String             createdDate;

  /**
   * The days an open task is past its due date, in the user's time zone;
   * none for a task not overdue.
   */
  @JsonProperty("days_overdue")
  private Integer            daysOverdue;

  private Priority           priority;

  /**
   * The number of its comments, replies included.
   */
  @JsonProperty("comments_count")
  private int                commentsCount;

  /**
   * When its latest comment, a reply or not, was written, formatted as
   * created_date; none for a task without comments.
   */
  @JsonProperty("last_comment_date")
  private String             lastCommentDate;

  /**
   * Who wrote its latest comment; none for a task without comments.
   */
  @JsonProperty("last_comment_author")
  private TaskAssigneeModel  lastCommentAuthor;

  /**
   * The changes since the date of a listing with {@code changed_since} or
   * {@code completed_since}, oldest first; none otherwise.
   */
  private List<TaskChange>   changes;

  /**
   * The description as plain text, cut at
   * {@link io.meeds.task.mcp.util.TaskDescriptionText#MAX_LENGTH}
   * characters, in a listing with {@code include_description}; none
   * otherwise, nor for a task without a description.
   */
  private String             description;

  /**
   * True when the description was cut; none otherwise, so that a listing
   * without {@code include_description} keeps its keys.
   */
  @JsonProperty("description_truncated")
  private Boolean            descriptionTruncated;

  /**
   * A list item without its latest comment, which
   * {@link #setLastComment(String, TaskAssigneeModel)} adds, nor its
   * description, which {@link #setPlainTextDescription(String)} adds on
   * request.
   *
   * @param id the task
   * @param title its title
   * @param link its link
   * @param completed whether it is completed
   * @param projectId its project, null for a personal task
   * @param projectName the name of its project
   * @param status its status
   * @param labels its labels
   * @param assignee its assignee
   * @param dueDate its due date
   * @param createdDate its creation date
   * @param daysOverdue its days overdue, null when not overdue
   * @param priority its priority
   * @param commentsCount its comments count, replies included
   * @param changes its changes since a date, null for none
   */
  public TaskListItemModel(long id, // NOSONAR the item's fields, each one its own
                           String title,
                           String link,
                           boolean completed,
                           Long projectId,
                           String projectName,
                           ProjectStatus status,
                           List<ProjectLabel> labels,
                           TaskAssigneeModel assignee,
                           String dueDate,
                           String createdDate,
                           Integer daysOverdue,
                           Priority priority,
                           int commentsCount,
                           List<TaskChange> changes) {
    this(id,
         title,
         link,
         completed,
         projectId,
         projectName,
         status,
         labels,
         assignee,
         dueDate,
         createdDate,
         daysOverdue,
         priority,
         commentsCount,
         null,
         null,
         changes,
         null,
         null);
  }

  /**
   * Sets the description from its plain text, cut at
   * {@link io.meeds.task.mcp.util.TaskDescriptionText#MAX_LENGTH}
   * characters, with {@code description_truncated} when it was cut.
   *
   * @param plainText the description as plain text, null for none
   */
  public void setPlainTextDescription(String plainText) {
    String cut = TaskDescriptionText.cut(plainText);
    this.description = cut;
    this.descriptionTruncated = cut != null && cut.length() < plainText.length() ? Boolean.TRUE : null;
  }

  /**
   * Sets when and by whom its latest comment was written.
   *
   * @param date the date of its latest comment, formatted as created_date
   * @param author its author
   */
  public void setLastComment(String date, TaskAssigneeModel author) {
    this.lastCommentDate = date;
    this.lastCommentAuthor = author;
  }

}
