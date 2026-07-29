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
package org.exoplatform.task.service;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.TestUtils;
import org.exoplatform.task.dao.*;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.UserSettingDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.impl.CommentServiceImpl;
import org.exoplatform.task.storage.CommentStorage;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.storage.StatusStorage;
import org.exoplatform.task.storage.TaskStorage;
import org.exoplatform.task.storage.impl.CommentStorageImpl;
import org.exoplatform.task.util.StorageUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CommentServiceTest {

  MockedStatic<ExoContainerContext> containerContext;

  StatusService           statusService;

  CommentService          commentService;

  ProjectStorage          projectStorage;

  @Mock
  CommentStorage          commentStorage;

  TaskStorage             taskStorage;

  ListenerService         listenerService;

  @Mock
  TaskHandler             taskHandler;

  @Mock
  ProjectHandler          projectHandler;

  @Mock
  StatusHandler           statusHandler;

  @Mock
  LabelHandler            labelHandler;

  @Mock
  UserService             userService;

  @Mock
  CommentHandler          commentHandler;

  @Mock
  DAOHandler              daoHandler;

  @Mock
  StatusStorage           statusStorage;

  @Captor
  ArgumentCaptor<Comment> commentCaptor;

  PortalContainer                   container;

  @Before
  public void setUp() {
    // Make sure the container is started to prevent the ExoTransactional annotation
    // to fail
    container = PortalContainer.getInstance();
    commentStorage = new CommentStorageImpl(daoHandler,projectStorage);
    commentService = new CommentServiceImpl(commentStorage, listenerService);

    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(() -> ExoContainerContext.getService(any())).thenAnswer(invocation -> {
      Class<?> clazz = invocation.getArgument(0, Class.class);
      if (clazz.equals(DAOHandler.class)) {
        return daoHandler;
      } else {
        return container.getComponentInstanceOfType(clazz);
      }
    });

    // Mock DAO handler to return Mocked DAO
    when(daoHandler.getCommentHandler()).thenReturn(commentHandler);
    when(daoHandler.getTaskHandler()).thenReturn(taskHandler);
    when(daoHandler.getStatusHandler()).thenReturn(statusHandler);
    when(daoHandler.getProjectHandler()).thenReturn(projectHandler);

    // Mock some DAO methods
    when(commentHandler.find(TestUtils.EXISTING_COMMENT_ID)).thenReturn(TestUtils.getDefaultComment());
    when(taskHandler.find(TestUtils.EXISTING_TASK_ID)).thenReturn(TestUtils.getDefaultTask());
    lenient().when(commentHandler.create(any())).thenAnswer(invocation -> {
      long id = (long) (Math.random() * Long.MAX_VALUE); // NOSONAR
      Comment c = invocation.getArgument(0, Comment.class);
      c.setId(id);
      when(commentHandler.find(id)).thenReturn(c);
      return c;
    });
  }

  @After
  public void tearDown() {
    commentService = null;
    containerContext.close();
    containerContext = null;
  }

  @Test
  public void testAddComment() throws EntityNotFoundException {
    Comment comment = TestUtils.getDefaultComment();
    commentService.addComment(StorageUtil.taskToDto(comment.getTask(), projectStorage),
                              comment.getAuthor(),
                              comment.getComment());
    verify(commentHandler, times(1)).create(commentCaptor.capture());
    Comment result = commentCaptor.getValue();
    assertEquals("Bla bla", result.getComment());
  }

  @Test
  public void testRemoveComment() throws EntityNotFoundException {
    Comment comment = TestUtils.getDefaultComment();

    when(daoHandler.getCommentHandler().find(TestUtils.EXISTING_COMMENT_ID)).thenReturn(comment);
    commentService.removeComment(TestUtils.EXISTING_COMMENT_ID);
    verify(commentHandler, times(1)).delete(commentCaptor.capture());

    assertEquals(TestUtils.EXISTING_COMMENT_ID, commentCaptor.getValue().getId().longValue());

  }

  @Test
  public void testUpdateComment() throws EntityNotFoundException {
    Comment comment = TestUtils.getDefaultComment();

    when(daoHandler.getCommentHandler().find(TestUtils.EXISTING_COMMENT_ID)).thenReturn(comment);
    when(commentHandler.update(any())).thenAnswer(invocation -> invocation.getArgument(0, Comment.class));

    CommentDto updatedComment = commentService.updateComment(TestUtils.EXISTING_COMMENT_ID, "Updated content");

    verify(commentHandler, times(1)).update(commentCaptor.capture());
    assertEquals("Updated content", commentCaptor.getValue().getComment());
    assertEquals("Updated content", updatedComment.getComment());
    assertNotNull("The comment update time should be stamped", updatedComment.getUpdatedTime());
  }

  @Test
  public void testCloneCommentKeepsUpdateTime() {
    CommentDto comment = TestUtils.getDefaultCommentDto();
    comment.setTask(TestUtils.getDefaultTaskDto());
    comment.setUpdatedTime(new Date());

    CommentDto clone = comment.clone();

    assertEquals(comment.getComment(), clone.getComment());
    assertEquals(comment.getUpdatedTime(), clone.getUpdatedTime());
  }

  @Test(expected = EntityNotFoundException.class)
  public void testUpdateCommentNotFound() throws EntityNotFoundException {
    when(daoHandler.getCommentHandler().find(TestUtils.UNEXISTING_COMMENT_ID)).thenReturn(null);

    commentService.updateComment(TestUtils.UNEXISTING_COMMENT_ID, "Updated content");
  }

  @Test
  public void testAddCommentsByTask() throws EntityNotFoundException {
    String username = "Tib";
    CommentDto newComment = TestUtils.getDefaultCommentDto();
    commentService.addComment(TestUtils.getDefaultTaskDto(), newComment.getAuthor(), newComment.getComment());
    commentService.getComments(TestUtils.EXISTING_TASK_ID, 0, 1);
    commentService.countComments(TestUtils.EXISTING_TASK_ID);
    verify(commentHandler, times(1)).create(commentCaptor.capture());
    assertEquals(TestUtils.EXISTING_TASK_ID, commentCaptor.getValue().getTask().getId().longValue());
    assertEquals(username, commentCaptor.getValue().getAuthor());
    assertEquals(newComment.getComment(), commentCaptor.getValue().getComment());
  }

  @Test
  public void testAddSubComments() throws EntityNotFoundException {
    String username = "Tib";
    String comment = "Bla bla bla bla bla";
    String authorSubComment = "Tib2";
    String subCommentContent = "Bla bla bla bla bla sub comment";
    CommentDto newComment = new CommentDto();
    newComment.setTask(TestUtils.getDefaultTaskDto());
    newComment.setAuthor(username);
    newComment.setComment(comment);
    newComment.setCreatedTime(new Date());
    UserSettingDto userSettingDto = TestUtils.getDefaultUserSettingDto();
    assertEquals("user", userSettingDto.getUsername());
    assertEquals(true, userSettingDto.isShowHiddenLabel());
    assertEquals(true, userSettingDto.isShowHiddenProject());
    commentService.addComment(StorageUtil.taskToDto(TestUtils.getDefaultTask(), projectStorage), username, comment);
    verify(commentHandler, times(1)).create(commentCaptor.capture());
    Comment parentComment = commentCaptor.getValue();
    assertEquals(TestUtils.EXISTING_TASK_ID, parentComment.getTask().getId().longValue());
    assertEquals(username, parentComment.getAuthor());
    assertEquals(comment, parentComment.getComment());
    long parentCommentId = parentComment.getId();
    commentService.addComment(StorageUtil.taskToDto(TestUtils.getDefaultTask(), projectStorage),
                              parentCommentId,
                              authorSubComment,
                              subCommentContent);
    verify(commentHandler, times(2)).create(commentCaptor.capture());
    Comment subComment = commentCaptor.getValue();
    assertEquals(TestUtils.EXISTING_TASK_ID, subComment.getTask().getId().longValue());
    assertEquals(authorSubComment, subComment.getAuthor());
    assertEquals(subCommentContent, subComment.getComment());
  }

}
