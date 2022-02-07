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
package org.exoplatform.task.storage;

import java.util.List;

import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;

public interface CommentStorage {

  CommentDto getComment(long commentId);

  List<CommentDto> getCommentsWithSubs(long taskId, int offset, int limit);

  List<CommentDto> getComments(long taskId, int offset, int limit);

  int countComments(long taskId);

  CommentDto addComment(TaskDto task, String username, String commentText) throws EntityNotFoundException;

  CommentDto addComment(TaskDto task, long parentCommentId, String username, String comment) throws EntityNotFoundException;

  void removeComment(long commentId) throws EntityNotFoundException;

  /**
   * Fetch sub comments of designed comments
   *
   * @param listComments the given list of comments.
   * @return List of SubComments
   */
  List<CommentDto> loadSubComments(List<CommentDto> listComments);

}
