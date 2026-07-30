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
package org.exoplatform.task.service.impl;

import static org.exoplatform.task.util.TaskUtil.TASK_COMMENT_CREATED;
import static org.exoplatform.task.util.TaskUtil.TASK_COMMENT_DELETED;
import static org.exoplatform.task.util.TaskUtil.TASK_COMMENT_UPDATED;
import static org.exoplatform.task.util.TaskUtil.broadcastEvent;

import java.util.List;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.NotAllowedOperationOnEntityException;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.storage.CommentStorage;
import org.exoplatform.task.util.StorageUtil;
import org.exoplatform.task.util.TaskUtil;

public class CommentServiceImpl implements CommentService {
    private static final Log LOG = ExoLogger.getExoLogger(CommentServiceImpl.class);

    private CommentStorage commentStorage;

    private ListenerService listenerService;

    public CommentServiceImpl(CommentStorage commentStorage, ListenerService listenerService) {
      this.commentStorage = commentStorage;
      this.listenerService = listenerService;
    }

    @Override
    public CommentDto getComment(long commentId) {
        return commentStorage.getComment(commentId);
    }

    @Override
    public List<CommentDto> getComments(long taskId, int offset, int limit) {
        return commentStorage.getComments(taskId,offset,limit);
    }

    @Override
    public List<CommentDto> getCommentsWithSubs(long taskId, int offset, int limit){
      return commentStorage.getCommentsWithSubs(taskId,offset,limit);
    }

    @Override
    public int countCommentsWithSubs(long taskId) {
      return commentStorage.countCommentsWithSubs(taskId);
    }

    @Override
    public int countComments(long taskId) {
        return commentStorage.countComments(taskId);
    }

    @Override
    public CommentDto getLastComment(long taskId) {
      return commentStorage.getLastComment(taskId);
    }

    @Override
    public List<CommentDto> loadSubComments(List<CommentDto> listComments) {
        if (listComments == null || listComments.isEmpty()) {
            return null;
        }
        listComments.forEach(comment -> comment.setComment(MentionUtils.substituteUsernames(comment.getComment())));
        List<CommentDto> subComments = commentStorage.loadSubComments(listComments);
        for (CommentDto comment : listComments) {
            subComments.forEach(subComment -> subComment.setComment(MentionUtils.substituteUsernames(subComment.getComment())));
            comment.setSubComments(subComments.stream()
                    .filter(subComment -> subComment.getParentComment().getId() == comment.getId())
                    .toList());
        }
        return listComments;
    }

    @Override
    public CommentDto addComment(TaskDto task,
                                 long parentCommentId,
                                 String username,
                                 String comment) throws EntityNotFoundException {
      CommentDto commentDto = commentStorage.addComment(task, parentCommentId, username, comment);
      if (commentDto != null) {
        broadcastEvent(listenerService, TASK_COMMENT_CREATED, commentDto.getTask(), commentDto);
        if (commentDto.getTask() != null && commentDto.getTask().getStatus() != null && commentDto.getTask().getStatus().getProject() != null) {
          broadcastEvent(listenerService, "exo.project.projectModified", null, commentDto.getTask().getStatus().getProject());
        }
      }
      return commentDto;
    }

    @Override
    public CommentDto addComment(TaskDto task, String username, String comment) throws EntityNotFoundException {
        return addComment(task, 0, username, comment);
    }

    @Override
    public CommentDto updateComment(long commentId, String commentText, Identity identity) throws EntityNotFoundException,
                                                                                          NotAllowedOperationOnEntityException {

      CommentDto comment = commentStorage.getComment(commentId);

      if (comment == null) {
        LOG.info("Can not find comment with ID: " + commentId);
        throw new EntityNotFoundException(commentId, CommentDto.class);
      }
      if (!TaskUtil.canEditComment(identity, comment)) {
        throw new NotAllowedOperationOnEntityException(commentId, CommentDto.class, "edit");
      }
      CommentDto updatedComment = commentStorage.updateComment(commentId, commentText);
      if (updatedComment != null) {
        broadcastEvent(listenerService, TASK_COMMENT_UPDATED, updatedComment.getTask(), updatedComment);
      }
      return updatedComment;
    }

    @Override
    public void removeComment(long commentId) throws EntityNotFoundException {

      CommentDto comment = commentStorage.getComment(commentId);

      if (comment == null) {
        LOG.info("Can not find comment with ID: " + commentId);
        throw new EntityNotFoundException(commentId, CommentDto.class);
      }
      commentStorage.removeComment(commentId);
      broadcastEvent(listenerService, TASK_COMMENT_DELETED, comment.getTask(), comment);
    }

}
