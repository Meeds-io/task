/* 
* Copyright (C) 2003-2015 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.task.test.dao;

import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.task.dao.ProjectHandler;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.service.DAOHandler;
import org.exoplatform.task.test.AbstractTest;

/**
 * @author <a href="trongtt@exoplatform.com">Trong Tran</a>
 * @version $Revision$
 */
public class TestProjectDAO extends AbstractTest {

  private ProjectHandler pDAO;
  private DAOHandler taskService;

  @Before
  public void setup() {
    PortalContainer container = PortalContainer.getInstance();
    
    taskService = (DAOHandler) container.getComponentInstanceOfType(DAOHandler.class);
    pDAO = taskService.getProjectHandler();
  }

  @After
  public void tearDown() {
    pDAO.deleteAll();
  }

  @Test
  public void testProjectCreation() {
    List<Project> all;

    Project p1 = new Project("Test project 1", null, null, null, null);
    pDAO.create(p1);
    all = pDAO.findAll();
    Assert.assertEquals(1, all.size());
    Assert.assertEquals("Test project 1", p1.getName());

    all = pDAO.findAll();
    Assert.assertEquals(1, all.size());
    Assert.assertEquals("Test project 1", p1.getName());
    Project p2 = new Project("Test project 2", null, null, null, null);
    pDAO.create(p2);

    all = pDAO.findAll();
    Assert.assertEquals(2, all.size());
    Assert.assertEquals(p1.getId() + 1, p2.getId());
  }

  @Test
  public void testFindSubProject() {
    Project parent = new Project("Parent project", null, null, null, null);
    pDAO.create(parent);

    Project child = new Project("Child project", null, null, null, null);
    child.setParent(parent);
    pDAO.create(child);

    List<Project> projects = pDAO.findSubProjects(parent);
    Assert.assertEquals(1, projects.size());

    projects = pDAO.findSubProjects(null);
    Assert.assertTrue(projects.size() >= 1);
  }
}

