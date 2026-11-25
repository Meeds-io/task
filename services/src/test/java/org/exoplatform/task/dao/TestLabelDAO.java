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
package org.exoplatform.task.dao;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.domain.*;
import org.exoplatform.task.util.ListUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestLabelDAO extends AbstractTest {

  private LabelHandler lblDAO;
  private DAOHandler taskService;
  private final String username = "root";

  @Before
  public void setup() {
    PortalContainer container = PortalContainer.getInstance();
    
    taskService = container.getComponentInstanceOfType(DAOHandler.class);
    lblDAO = taskService.getLabelHandler();
  }

  @After
  public void tearDown() {
    deleteAll();
  }
  
  @Test
  public void testAddTask() throws Exception {
    Project project= new Project();
    project.setName("project");
    taskService.getProjectHandler().create(project);
    Label label = new Label("test label", username, project);
    label = lblDAO.create(label);
    Task task = new Task();
    task.setTitle("task1");
    taskService.getTaskHandler().create(task);
    //
    LabelTaskMapping map = new LabelTaskMapping(label, task);
    taskService.getLabelTaskMappingHandler().create(map);
    //find tasks that have label
    ListAccess<Task> tasks = taskService.getTaskHandler().findTasksByLabel(0, null, username, null);
    Assert.assertEquals(1, tasks.getSize());
  }
  
  @Test
  public void testRemoveTask() throws Exception {
    Project project= new Project();
    project.setName("project");
    taskService.getProjectHandler().create(project);
    Label label = new Label("test label", username, project);
    label = lblDAO.create(label);
    Task task = new Task();
    task.setTitle("test");
    taskService.getTaskHandler().create(task);
    //
    LabelTaskMapping map = new LabelTaskMapping(label, task);
    taskService.getLabelTaskMappingHandler().create(map);
    
    ListAccess<Task> tasks = taskService.getTaskHandler().findTasksByLabel(label.getId(), null, username, null);
    Assert.assertEquals(1, tasks.getSize());
    //
    endRequestLifecycle();
    initializeContainerAndStartRequestLifecycle();
    lblDAO.delete(lblDAO.find(label.getId()));
    tasks = taskService.getTaskHandler().findTasksByLabel(label.getId(), null, username, null);
    Assert.assertEquals(0, tasks.getSize());
  }
  
  @Test
  public void testUpdate() {
    Project project= new Project();
    project.setName("project");
    taskService.getProjectHandler().create(project);
    Label label = new Label("test label", username, project);
    label = lblDAO.create(label);
    Assert.assertNotNull(label);
    
    label = lblDAO.find(label.getId());
    Assert.assertNotNull(label);
    
    label.setColor("white");
    lblDAO.update(label);
    label = lblDAO.find(label.getId());
    Assert.assertEquals("white", label.getColor());
    
    label.setName("label2");
    lblDAO.update(label);
    label = lblDAO.find(label.getId());
    Assert.assertEquals("label2", label.getName());    
    
    Label parent = new Label("parent label", "root", project);
    lblDAO.create(parent);
    label.setParent(parent);
    lblDAO.update(label);
    label = lblDAO.find(label.getId());
    Assert.assertEquals(parent.getId(), label.getParent().getId());    
  }
  
  @Test
  public void testFindLabelByTask() throws Exception {
    Project project = new Project();
    project.setName("project1");
    taskService.getProjectHandler().create(project);

    Status status = new Status();
    status.setName("status1");
    status.setRank(1);
    status.setProject(project);
    taskService.getStatusHandler().create(status);

    Task task = new Task();
    task.setTitle("task1");
    task.setStatus(status);
    taskService.getTaskHandler().create(task);
    //
    Label label1 = new Label("test label1", "root", project);
    lblDAO.create(label1);
    //
    LabelTaskMapping mapping = new LabelTaskMapping();
    mapping.setLabel(label1);
    mapping.setTask(task);
    //
    taskService.getLabelTaskMappingHandler().create(mapping);
    
    Assert.assertEquals(1, lblDAO.findLabelsByTask(task.getId(), project.getId()).getSize());
  }
  
  @Test
  public void testRemoveLabel() throws Exception {
    Project project= new Project();
    project.setName("project");
    taskService.getProjectHandler().create(project);

    Task task = new Task();
    task.setTitle("task1");
    taskService.getTaskHandler().create(task);
    
    Task task2 = new Task();
    task2.setTitle("task2");
    taskService.getTaskHandler().create(task2);
    //
    Label label1 = new Label("test label1", "root",project);
    lblDAO.create(label1);    
    //
    LabelTaskMapping mapping = new LabelTaskMapping();
    mapping.setLabel(label1);
    mapping.setTask(task);
    
    LabelTaskMapping mapping2 = new LabelTaskMapping();
    mapping2.setLabel(label1);
    mapping2.setTask(task2);
    taskService.getLabelTaskMappingHandler().create(mapping);
    taskService.getLabelTaskMappingHandler().create(mapping2);
    
    endRequestLifecycle();
    initializeContainerAndStartRequestLifecycle();
    
    //
    Assert.assertNotNull(lblDAO.find(label1.getId()));
    ListAccess<Task> tasks = taskService.getTaskHandler().findTasksByLabel(label1.getId(), null, "root", null);
    Assert.assertEquals(2, tasks.getSize());

    lblDAO.delete(lblDAO.find(label1.getId()));    
    
    //
    Assert.assertNull(lblDAO.find(label1.getId()));
    tasks = taskService.getTaskHandler().findTasksByLabel(label1.getId(), null, "root", null);
    Assert.assertEquals(0, tasks.getSize());
  }
  
  @Test
  public void testLabelsPagesAreStableAndComplete() throws Exception {
    Project project = new Project();
    project.setName("pagedProject");
    taskService.getProjectHandler().create(project);
    for (int i = 0; i < 5; i++) {
      lblDAO.create(new Label("paged label " + i, "pager", project));
    }

    ListAccess<Label> byUser = lblDAO.findLabelsByUser("pager");
    Assert.assertEquals(5, byUser.getSize());
    List<String> paged = new java.util.ArrayList<>();
    for (int offset = 0; offset < 5; offset += 2) {
      for (Label l : byUser.load(offset, 2)) {
        paged.add(l.getName());
      }
    }
    Assert.assertEquals(List.of("paged label 0", "paged label 1", "paged label 2", "paged label 3", "paged label 4"), paged);

    ListAccess<Label> byProject = lblDAO.findLabelsByProject(project.getId());
    Assert.assertEquals(5, byProject.getSize());
    Assert.assertEquals("paged label 2", byProject.load(2, 2)[0].getName());
    Assert.assertEquals(0, byProject.load(5, 2).length);
  }

  @Test
  public void testSearchLabelsByNameIsCaseInsensitiveAndLiteral() throws Exception {
    Project project = new Project();
    project.setName("searchProject");
    taskService.getProjectHandler().create(project);
    for (String name : new String[] { "Bug fix", "bugzilla", "100% done", "a_b", "axb", "Other" }) {
      lblDAO.create(new Label(name, "searcher", project));
    }
    lblDAO.create(new Label("bug of someone else", "someone", project));

    Assert.assertEquals(2, lblDAO.findLabelsByUser("searcher", "BUG").getSize());
    Assert.assertEquals("Bug fix", lblDAO.findLabelsByUser("searcher", "bug").load(0, 1)[0].getName());
    // the user's own labels only
    Assert.assertEquals(3, lblDAO.findLabelsByProject(project.getId(), "bug").getSize());
    // % and _ are taken literally, not as wildcards
    Assert.assertEquals(1, lblDAO.findLabelsByUser("searcher", "%").getSize());
    Assert.assertEquals(1, lblDAO.findLabelsByUser("searcher", "a_b").getSize());
    Assert.assertEquals(0, lblDAO.findLabelsByUser("searcher", "zzz").getSize());
    // a blank keyword is no search
    Assert.assertEquals(6, lblDAO.findLabelsByUser("searcher", " ").getSize());
    // the search pages like the listing
    Assert.assertEquals(1, lblDAO.findLabelsByUser("searcher", "bug").load(1, 5).length);
  }

  @Test
  public void testQuery() throws Exception {
    Project project = new Project();
    project.setName("project1");
    taskService.getProjectHandler().create(project);

    Status status = new Status();
    status.setName("status1");
    status.setRank(1);
    status.setProject(project);
    taskService.getStatusHandler().create(status);

    Task task = new Task();
    task.setTitle("task1");
    task.setStatus(status);
    taskService.getTaskHandler().create(task);

    Label label1 = new Label("test label1", "root",project);
    Label label2 = new Label("test label2", "demo",project);
    lblDAO.create(label1);
    lblDAO.create(label2);
    
    //
    ListAccess<Label> labels = lblDAO.findLabelsByUser("root");
    List<Label> list = Arrays.asList(ListUtil.load(labels, 0, -1));
    Assert.assertEquals(1, labels.getSize());
    Assert.assertEquals(label1.getName(), list.get(0).getName());
    labels = lblDAO.findLabelsByUser("demo");
    list = Arrays.asList(ListUtil.load(labels, 0, -1));
    Assert.assertEquals(1, labels.getSize());
    Assert.assertEquals(label2.getName(), list.get(0).getName());

    LabelTaskMapping mapping = new LabelTaskMapping(label1, task);
    taskService.getLabelTaskMappingHandler().create(mapping);
    //
    labels = lblDAO.findLabelsByTask(task.getId(), project.getId());
    Assert.assertEquals(1, labels.getSize());
  }
}
