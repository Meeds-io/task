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
package org.exoplatform.task.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The comments of a task at a glance: how many it has, replies included, and
 * when and by whom the latest one, a reply or not, was written.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsSummaryDto {

  /** The task. */
  private long   taskId;

  /** The number of its comments, replies included. */
  private long   count;

  /** The creation date of its latest comment, a reply or not. */
  private Date   lastCommentDate;

  /** The username of the author of its latest comment. */
  private String lastCommentAuthor;

  /**
   * The id of its latest comment, which tells apart two comments created at
   * the same time.
   */
  private long   lastCommentId;

}
