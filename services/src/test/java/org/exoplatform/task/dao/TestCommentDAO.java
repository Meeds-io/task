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
package org.exoplatform.task.dao;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.util.ListUtil;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class TestCommentDAO extends AbstractTest {

  private CommentHandler commentDAO;
  private TaskHandler taskDAO;

  private final String username = "root";
  private DAOHandler daoHandler;

  @Before
  public void initDAOs() {
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    
    daoHandler = (DAOHandler) container.getComponentInstanceOfType(DAOHandler.class);
    taskDAO = daoHandler.getTaskHandler();
    commentDAO = daoHandler.getCommentHandler();
  }

  @After
  public void cleanData() {
    endRequestLifecycle();
    commentDAO.deleteAll();
    taskDAO.deleteAll();
  }

  @Test
  public void testCreateComment() {
    Task task = newDefaultSimpleTask();
    taskDAO.create(task);

    List<Task> tasks = taskDAO.findAll();
    task = tasks.get(0);

    Comment comment = newDefaultSimpleComment(task);
    commentDAO.create(comment);

    ListAccess<Comment> listComments = commentDAO.findComments(task.getId());
    Comment[] comments = ListUtil.load(listComments, 0, -1);
    Assert.assertEquals(1, comments.length);
    comment = comments[0];
    Assert.assertEquals(username, comment.getAuthor());

    //Assert.assertEquals(task.getId(), comment.getTask().getId());
  }

  @Test
  public void testCreateSubComment() {
    Task task = newDefaultSimpleTask();
    task = taskDAO.create(task);

    Comment comment = newDefaultSimpleComment(task);
    comment = commentDAO.create(comment);

    Comment subComment = newDefaultSimpleComment(task);
    subComment.setParentComment(comment);
    subComment = commentDAO.create(subComment);

    ListAccess<Comment> listComments = commentDAO.findComments(task.getId());
    Comment[] comments = ListUtil.load(listComments, 0, -1);
    Assert.assertEquals(1, comments.length);

    List<Comment> subComments = commentDAO.getSubComments(Arrays.asList(comments));

    Assert.assertEquals(1, subComments.size());
    subComment = subComments.get(0);
    Assert.assertEquals(username, comment.getAuthor());
  }

  @Test
  public void testDeleteSubComment() {
    Task task = newDefaultSimpleTask();
    taskDAO.create(task);

    List<Task> tasks = taskDAO.findAll();
    task = tasks.get(0);

    Comment comment = newDefaultSimpleComment(task);
    comment = commentDAO.create(comment);

    Comment subComment1 = newDefaultSimpleComment(task);
    subComment1.setParentComment(comment);
    subComment1 = commentDAO.create(subComment1);

    Comment subComment2 = newDefaultSimpleComment(task);
    subComment2.setParentComment(comment);
    subComment2 = commentDAO.create(subComment2);

    ListAccess<Comment> listComments = commentDAO.findComments(task.getId());
    Comment[] comments = ListUtil.load(listComments, 0, -1);
    List<Comment> subComments = commentDAO.getSubComments(Arrays.asList(comments));

    Assert.assertEquals(2, subComments.size());
    subComment2 = subComments.get(0);
    commentDAO.delete(subComment2);

    Assert.assertEquals(2, commentDAO.findAll().size());
    Assert.assertNotNull(subComments.get(0).getParentComment());
    Assert.assertNotNull(subComments.get(1).getParentComment());

    subComments = commentDAO.getSubComments(Arrays.asList(comments));
    Assert.assertEquals(1, subComments.size());

    Assert.assertNotNull(subComments.get(0).getParentComment());

    comment = commentDAO.find(comment.getId());
    Assert.assertNotNull(comment);

    commentDAO.delete(comment);
    Assert.assertEquals(0, commentDAO.findAll().size());
  }

  @Test
  public void testUpdateComment() {
    // Create Task
    Task task = newDefaultSimpleTask();
    taskDAO.create(task);

    List<Task> tasks = taskDAO.findAll();
    task = tasks.get(0);

    // Create Comment
    Comment comment = newDefaultSimpleComment(task);
    commentDAO.create(comment);

    // Load comment of task
    ListAccess<Comment> listComments = commentDAO.findComments(task.getId());
    Comment[] comments = ListUtil.load(listComments, 0, -1);
    Assert.assertEquals(1, comments.length);
    comment = comments[0];

    // Update comment
    long id = comment.getId();
    comment.setComment("New comment content");
    commentDAO.update(comment);

    comment = commentDAO.find(id);
    Assert.assertEquals("New comment content", comment.getComment());
  }

  @Test
  public void testDeleteComment() {
    // Create Task
    Task task = newDefaultSimpleTask();
    taskDAO.create(task);

    List<Task> tasks = taskDAO.findAll();
    task = tasks.get(0);

    // Create Comment
    Comment comment = newDefaultSimpleComment(task);
    commentDAO.create(comment);

    // Load comment of task
    ListAccess<Comment> listComments = commentDAO.findComments(task.getId());
    Comment[] comments = ListUtil.load(listComments, 0, -1);
    Assert.assertEquals(1, comments.length);
    comment = comments[0];

    // Delete comment
    long id = comment.getId();
    commentDAO.delete(comment);

    comment = commentDAO.find(id);
    Assert.assertNull(comment);
  }

  @Test
  public void testCountComments() {
    // Create Task
    Task task = newDefaultSimpleTask();
    taskDAO.create(task);

    List<Task> tasks = taskDAO.findAll();
    task = tasks.get(0);

    // Create Comment
    final int number = 10;
    for(int i = 0; i < number; i++) {
      Comment comment = newDefaultSimpleComment(task);
      comment.setComment("Comment number " + i);
      commentDAO.create(comment);
    }

    // Assure have 10 comment in task
    ListAccess<Comment> listComments = commentDAO.findComments(task.getId());
    long count = ListUtil.getSize(listComments);

    Assert.assertEquals(number, count);
  }

  @Test
  public void testLoadCommentWithLimit() {
    // Create Task
    Task task = newDefaultSimpleTask();
    taskDAO.create(task);

    List<Task> tasks = taskDAO.findAll();
    task = tasks.get(0);

    // Create Comment
    final int number = 10;
    for(int i = 0; i < number; i++) {
      Comment comment = newDefaultSimpleComment(task);
      comment.setComment("Comment number " + i);
      commentDAO.create(comment);
    }

    ListAccess<Comment> listComments = commentDAO.findComments(task.getId());

    Comment[] comments = ListUtil.load(listComments, 0, 5); //commentDAO.findCommentsOfTask(task, 0, 5);
    Assert.assertEquals(5, comments.length);

    comments = ListUtil.load(listComments, 2, 5); //commentDAO.findCommentsOfTask(task, 2, 5);
    Assert.assertEquals(5, comments.length);

    //. If does not pass limit number, start will be ignored
    comments = ListUtil.load(listComments, 2, -1); //commentDAO.findCommentsOfTask(task, 2, 0);
    Assert.assertEquals(10, comments.length);

    comments = ListUtil.load(listComments, 8, 5); //commentDAO.findCommentsOfTask(task, 8, 5);
    Assert.assertEquals(2, comments.length);
  }

  @Test
  public void testMentionedUsers() {
    Task task = newDefaultSimpleTask();
    taskDAO.create(task);

    List<Task> tasks = taskDAO.findAll();
    task = tasks.get(0);

    Comment comment1 = newDefaultSimpleComment(task);
    comment1.setComment("test1 @userA test2 @userB test3 @ test4");
    commentDAO.create(comment1);

    Set<String> mentionedUsers = commentDAO.findMentionedUsersOfTask(task.getId());
    Assert.assertEquals(2, mentionedUsers.size());
    Assert.assertTrue(mentionedUsers.contains("userA"));
    Assert.assertTrue(mentionedUsers.contains("userB"));

    Comment comment2 = newDefaultSimpleComment(task);
    comment2.setComment("@userA @userC ");
    commentDAO.create(comment2);
    //
    mentionedUsers = commentDAO.findMentionedUsersOfTask(task.getId());
    Assert.assertEquals(3, mentionedUsers.size());

    commentDAO.delete(comment2);
    //
    mentionedUsers = commentDAO.findMentionedUsersOfTask(task.getId());
    Assert.assertEquals(2, mentionedUsers.size());

    comment1.setComment("@userD");
    commentDAO.update(comment1);
    //
    mentionedUsers = commentDAO.findMentionedUsersOfTask(task.getId());
    Assert.assertEquals(1, mentionedUsers.size());
  }

  private Task newDefaultSimpleTask() {
    Task task = new Task();
    task.setTitle("Default task");
    task.setAssignee("root");
    task.setCreatedBy("root");
    task.setCreatedTime(new Date());
    return task;
  }

  private Comment newDefaultSimpleComment(Task task) {
    Comment comment = new Comment();
    comment.setAuthor(username);
    comment.setComment("Default comment");
    comment.setCreatedTime(new Date());
    comment.setTask(task);
    return comment;
  }
}
