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
package org.exoplatform.task.storage.impl;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.storage.CommentStorage;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.util.StorageUtil;


public class CommentStorageImpl implements CommentStorage {

    private static final Log LOG = ExoLogger.getExoLogger(CommentStorageImpl.class);

    private static final Pattern pattern = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");


    @Inject
    private final DAOHandler daoHandler;

    @Inject
    private final ProjectStorage projectStorage;


    public CommentStorageImpl(DAOHandler daoHandler, ProjectStorage projectStorage) {
        this.daoHandler = daoHandler;
        this.projectStorage = projectStorage;
    }

    @Override
    public CommentDto getComment(long commentId) {
        return StorageUtil.commentToDto(daoHandler.getCommentHandler().find(commentId),projectStorage);
    }

    @Override
    public List<CommentDto> getCommentsWithSubs(long taskId, int offset, int limit) {
        try {
            List<CommentDto> commentsDto=new ArrayList<>();
            List<Comment>  comments =  Arrays.asList(daoHandler.getCommentHandler().findComments(taskId).load(offset, limit));
            if(comments.size()>0){
                List<Comment> subComments = daoHandler.getCommentHandler().getSubComments(comments);
                List<CommentDto> subCommentsDto = subComments.stream().map((Comment comment1) -> StorageUtil.commentToDto(comment1,projectStorage)).collect(Collectors.toList());
                commentsDto = comments.stream().map((Comment comment1) -> StorageUtil.commentToDto(comment1,projectStorage)).collect(Collectors.toList());
                for (CommentDto comment : commentsDto) {
                    comment.setSubComments(subCommentsDto.stream()
                            .filter(subComment -> subComment.getParentComment().getId() == comment.getId())
                            .collect(Collectors.toList()));
                }
            }

        return commentsDto;
        } catch (Exception e) {
            return new ArrayList<CommentDto>();
        }
    }

    @Override
    public List<CommentDto> getComments(long taskId, int offset, int limit) {
        try {
        return Arrays.asList(daoHandler.getCommentHandler().findComments(taskId).load(offset, limit)).stream().map((Comment comment) -> StorageUtil.commentToDto(comment,projectStorage)).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<CommentDto>();
        }
    }

    @Override
    public int countComments(long taskId) {
        try {
        return daoHandler.getCommentHandler().findComments(taskId).getSize();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public CommentDto addComment(TaskDto task, String username, String comment) throws EntityNotFoundException {
        CommentDto newComment = new CommentDto();
        newComment.setTask(task);
        newComment.setAuthor(username);
        newComment.setComment(comment);
        newComment.setCreatedTime(new Date());
        return  StorageUtil.commentToDto(daoHandler.getCommentHandler().create(StorageUtil.commentToEntity(newComment)),projectStorage);
    }

    @Override
    public CommentDto addComment(TaskDto task, long parentCommentId, String username, String comment) throws EntityNotFoundException {
        CommentDto newComment = new CommentDto();
        newComment.setTask(task);
        newComment.setAuthor(username);
        newComment.setComment(comment);
        newComment.setCreatedTime(new Date());
        if (parentCommentId > 0) {
            CommentDto parentComment = getComment(parentCommentId);
            if (parentComment.getParentComment() != null) {
                parentComment = parentComment.getParentComment();
            }
            newComment.setParentComment(parentComment);
        }
        return  StorageUtil.commentToDto(daoHandler.getCommentHandler().create(StorageUtil.commentToEntity(newComment)),projectStorage);
    }

    @Override
    public void removeComment(long commentId) throws EntityNotFoundException {
        daoHandler.getCommentHandler().delete(StorageUtil.commentToEntity(getComment(commentId)));
    }

    @Override
    public List<CommentDto> loadSubComments(List<CommentDto> listComments) {
        List<Comment> comments = new ArrayList<>();
        if(comments.size()>0) {
            comments = listComments.stream().map(StorageUtil::commentToEntity).collect(Collectors.toList());
            comments = daoHandler.getCommentHandler().getSubComments(comments);
            return comments.stream().map((Comment comment) -> StorageUtil.commentToDto(comment, projectStorage)).collect(Collectors.toList());
        }
        return new ArrayList<CommentDto>();
    }

}
