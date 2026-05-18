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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "TaskComment")
@Table(name = "TASK_COMMENTS")
@NamedQuery(name = "Comment.countCommentOfTask",
    query = "SELECT count(c) FROM TaskComment c WHERE c.task.id = :taskId AND c.parentComment IS NULL")
@NamedQuery(name = "Comment.countCommentsWithSubs",
    query = "SELECT count(c) FROM TaskComment c WHERE c.task.id = :taskId")
@NamedQuery(name = "Comment.findCommentsOfTask",
    query = "SELECT c FROM TaskComment c WHERE c.task.id = :taskId AND c.parentComment IS NULL ORDER BY c.createdTime DESC")
@NamedQuery(name = "Comment.findLastCommentOfTask",
    query = "SELECT c FROM TaskComment c WHERE c.task.id = :taskId ORDER BY c.createdTime DESC")
@NamedQuery(name = "Comment.findSubCommentsOfComments",
    query = "SELECT c FROM TaskComment c WHERE c.parentComment IN (:comments) ORDER BY c.createdTime ASC")
@NamedQuery(name = "Comment.deleteCommentOfTask",
    query = "DELETE FROM TaskComment c WHERE c.task.id = :taskId")
@NamedQuery(name = "Comment.findMentionedUsersOfTask",
    query = "SELECT m FROM TaskComment c INNER JOIN c.mentionedUsers m WHERE c.task.id = :taskId")
@Data
public class Comment {

  @Id
  @PortableSequence(name = "SEQ_TASK_COMMENTS_COMMENT_ID")
  @Column(name = "COMMENT_ID")
  private Long                 id;

  @Column(name = "AUTHOR")
  private String               author;

  @Column(name = "CMT")
  private String               comment; // NOSONAR

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "CREATED_TIME")
  private Date                 createdTime = Calendar.getInstance().getTime();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TASK_ID")
  @EqualsAndHashCode.Exclude
  private Task                 task;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "PARENT_COMMENT_ID")
  @EqualsAndHashCode.Exclude
  private Comment              parentComment;

  @OneToMany(mappedBy = "parentComment", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @EqualsAndHashCode.Exclude
  private List<Comment>        subComments    = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "TASK_COMMENT_MENTIONED_USERS", joinColumns = @JoinColumn(name = "COMMENT_ID"))
  @Column(name = "MENTIONED_USERS")
  @EqualsAndHashCode.Exclude
  private Set<String>          mentionedUsers = new HashSet<>();

  public void setParentComment(Comment parentComment) {
    this.parentComment = parentComment;
    this.parentComment.addSubComment(this);
  }

  public void addSubComment(Comment subComment) {
    this.subComments.add(subComment);
  }

  @Override
  public Comment clone() { // NOSONAR
    Comment c = new Comment();
    c.setId(getId());
    c.setAuthor(getAuthor());
    c.setComment(getComment());
    c.setMentionedUsers(new HashSet<>(getMentionedUsers()));
    c.setCreatedTime(getCreatedTime());
    c.setTask(getTask().clone());
    return c;
  }
}
