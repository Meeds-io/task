/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.service;

import java.util.List;

import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;

public interface CommentService {

  CommentDto getComment(long commentId);

  CommentDto addComment(TaskDto task, String username, String commentText) throws EntityNotFoundException;

  CommentDto addComment(TaskDto task, long parentCommentId, String username, String comment) throws EntityNotFoundException;

  void removeComment(long commentId) throws EntityNotFoundException;

  List<CommentDto> getComments(long taskId, int offset, int limit);

  List<CommentDto> getCommentsWithSubs(long taskId, int offset, int limit);

  int countComments(long taskId);

  /**
   * Fetch sub comments of designed comments
   *
   * @param listComments the given list of comments.
   * @return List of sub comments
   */
  List<CommentDto> loadSubComments(List<CommentDto> listComments);
}
