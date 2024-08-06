/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package org.exoplatform.task.storage;

import java.util.Date;
import java.util.Set;

import org.exoplatform.task.dao.CommentHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.TaskHandler;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;

public class CommentStorageTest extends AbstractTest {

  private static final String USERNAME = "root";

  private TaskService         taskService;

  private CommentStorage      commentStorage;

  private TaskHandler    taskDAO;
  private CommentHandler commentDAO;

  @Before
  public void init() {
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);

    taskService = container.getComponentInstanceOfType(TaskService.class);
    commentStorage = container.getComponentInstanceOfType(CommentStorage.class);
    DAOHandler daoHandler = container.getComponentInstanceOfType(DAOHandler.class);
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
  public void testMentionedUsers() throws EntityNotFoundException {
    TaskDto task = newDefaultSimpleTask();
    task = taskService.createTask(task);

    commentStorage.addComment(task, USERNAME, "test1 @john test2 @mary test3 @ root");

    Set<String> mentionedUsers = taskService.getMentionedUsers(task.getId());
    Assert.assertEquals(2, mentionedUsers.size());
    Assert.assertTrue(mentionedUsers.contains("john"));
    Assert.assertTrue(mentionedUsers.contains("mary"));

    CommentDto comment2 = commentStorage.addComment(task, USERNAME, "@john @demo ");

    mentionedUsers = taskService.getMentionedUsers(task.getId());
    Assert.assertEquals(3, mentionedUsers.size());

    commentStorage.removeComment(comment2.getId());

    mentionedUsers = taskService.getMentionedUsers(task.getId());
    Assert.assertEquals(2, mentionedUsers.size());
  }

  @Test
  public void testFindTaskByMentionedUser() throws Exception {
    TaskDto task = newDefaultSimpleTask();
    task = taskService.createTask(task);

    commentStorage.addComment(task, USERNAME, "test comment @mary test");

    TaskQuery query = new TaskQuery();
    query.setIsTodoOf("mary");
    ListAccess<Task> tasks = taskDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());

    query = new TaskQuery();
    query.setIsIncomingOf("mary");
    tasks = taskDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());

    TaskDto task2 = taskService.createTask(newDefaultSimpleTask());
    task2.setAssignee("mary");
    taskService.updateTask(task2);

    query = new TaskQuery();
    query.setIsTodoOf("mary");
    tasks = taskDAO.findTasks(query);
    Assert.assertEquals(2, tasks.getSize());

    query = new TaskQuery();
    query.setIsIncomingOf("mary");
    tasks = taskDAO.findTasks(query);
    Assert.assertEquals(2, tasks.getSize());

    query = new TaskQuery();
    query.setIsTodoOf(USERNAME);
    tasks = taskDAO.findTasks(query);
    Assert.assertEquals(1, tasks.getSize());
  }

  private TaskDto newDefaultSimpleTask() {
    TaskDto task = new TaskDto();
    task.setTitle("Default task");
    task.setAssignee("root");
    task.setCreatedBy("root");
    task.setCreatedTime(new Date());
    return task;
  }

}
