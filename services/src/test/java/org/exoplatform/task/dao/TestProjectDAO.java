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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.AbstractTest;

public class TestProjectDAO extends AbstractTest {

  private ProjectHandler pDAO;
  private DAOHandler daoHandler;

  @Before
  public void setup() {
    PortalContainer container = PortalContainer.getInstance();
    
    daoHandler = (DAOHandler) container.getComponentInstanceOfType(DAOHandler.class);
    pDAO = daoHandler.getProjectHandler();
  }

  @After
  public void tearDown() {
    deleteAll();
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
  public void testFindSubProject() throws Exception {
    Project parent = new Project("Parent project", null, null, null, null);
    pDAO.create(parent);

    Project child = new Project("Child project", null, null, null, null);
    child.setParent(parent);
    pDAO.create(child);

    ListAccess<Project> projects = pDAO.findSubProjects(parent);
    Assert.assertEquals(1, projects.getSize());

    projects = pDAO.findSubProjects(null);
    Assert.assertTrue(projects.getSize() >= 1);
  }
  
  @Test
  public void testFindProjectByKeywordWithDifferentCase() throws Exception {
    Set<String> manager = new HashSet<String>();
    manager.add("root");

    ListAccess<Project> projects = pDAO.findAllByMembershipsAndKeyword(Arrays.asList("root"), "test parenT ProJecT", null);
    int initialSize = projects.getSize();

    Project parent = new Project("Test Parent project", null, null, manager, null);
    pDAO.create(parent);

    projects = pDAO.findAllByMembershipsAndKeyword(Arrays.asList("root"), "test parenT ProJecT", null);
    Assert.assertEquals(initialSize + 1, projects.getSize());
  }

  @Test
  public void testProjectQueryCountsEachProjectOnce() throws Exception {
    // one project matching through its manager and both its participators, and a second project
    pDAO.create(new Project("Campaign plan",
                            null,
                            null,
                            new HashSet<>(Arrays.asList("manager:/spaces/space1")),
                            new HashSet<>(Arrays.asList("member:/spaces/space1", "john"))));
    pDAO.create(new Project("Campaign budget", null, null, new HashSet<>(Arrays.asList("manager:/spaces/space1")), null));

    for (String keyword : Arrays.asList(null, "campaign")) {
      ProjectQuery query = new ProjectQuery();
      query.setMembership(Arrays.asList("manager:/spaces/space1", "member:/spaces/space1"));
      query.setKeyword(keyword);
      query.setOrderBy(Arrays.asList(new OrderBy.DESC("lastModifiedDate")));

      ListAccess<Project> projects = pDAO.findProjects(query);

      Assert.assertEquals(2, projects.getSize());
      Assert.assertEquals(2, projects.load(0, 10).length);
    }
  }

  @Test
  public void testSpaceProjectsAreListedOverTheSpaceAndTheUsersMemberships() throws Exception {
    Set<String> spaceMemberships = new HashSet<>(Arrays.asList("manager:/spaces/space1", "member:/spaces/space1"));
    // naming the user by id, who is not a member of the space: matched through two participators and a manager
    pDAO.create(new Project("Space named", null, null,
                            new HashSet<>(Arrays.asList("manager:/spaces/space1", "john")),
                            new HashSet<>(Arrays.asList("member:/spaces/space1", "john"))));
    // shared with the group the user holds
    pDAO.create(new Project("Space member", null, null,
                            new HashSet<>(Arrays.asList("manager:/spaces/space1")),
                            new HashSet<>(Arrays.asList("member:/spaces/space1"))));
    // a space project the user cannot see
    pDAO.create(new Project("Space hidden", null, null,
                            new HashSet<>(Arrays.asList("manager:/spaces/space1")),
                            new HashSet<>(Arrays.asList("mary"))));
    // naming the user among its managers, next to the space's manager role, with no participator the user holds
    pDAO.create(new Project("Space manager named", null, null,
                            new HashSet<>(Arrays.asList("manager:/spaces/space1", "john")),
                            new HashSet<>(Arrays.asList("mary"))));
    // a project the user can see, outside the space
    pDAO.create(new Project("Other named", null, null, new HashSet<>(Arrays.asList("john")), null));

    for (String keyword : Arrays.asList(null, "space")) {
      ProjectQuery query = new ProjectQuery();
      query.setMembership(new LinkedList<>(spaceMemberships));
      query.setMembership(Arrays.asList("john", "*:/spaces/other", "member:/spaces/space1"));
      query.setKeyword(keyword);
      query.setOrderBy(Arrays.asList(new OrderBy.DESC("lastModifiedDate")));

      ListAccess<Project> projects = pDAO.findProjects(query);
      Assert.assertEquals(3, projects.getSize());
      Set<String> names = new HashSet<>();
      for (Project project : projects.load(0, 10)) {
        names.add(project.getName());
      }
      Assert.assertEquals(new HashSet<>(Arrays.asList("Space named", "Space member", "Space manager named")), names);
    }

    ProjectQuery nonMember = new ProjectQuery();
    nonMember.setMembership(new LinkedList<>(spaceMemberships));
    nonMember.setMembership(Arrays.asList("john"));
    ListAccess<Project> projects = pDAO.findProjects(nonMember);
    // each condition has its own joins: the space's manager role and the user's id are two rows of the managers
    Assert.assertEquals(2, projects.getSize());
    Set<String> nonMemberNames = new HashSet<>();
    for (Project project : projects.load(0, 10)) {
      nonMemberNames.add(project.getName());
    }
    Assert.assertEquals(new HashSet<>(Arrays.asList("Space named", "Space manager named")), nonMemberNames);
  }

  @Test
  public void testFindProjectByKeywordPartiallyMatching() throws Exception {
    Set<String> manager = new HashSet<String>();
    manager.add("root");
    Project parent = new Project("Marketing", null, null, manager, null);
    pDAO.create(parent);

    ListAccess<Project> projects = pDAO.findAllByMembershipsAndKeyword(Arrays.asList("root"), "Mark", null);
    Assert.assertEquals(1, projects.getSize());

    projects = pDAO.findAllByMembershipsAndKeyword(Arrays.asList("root"), "arKet", null);
    Assert.assertEquals(1, projects.getSize());

    projects = pDAO.findAllByMembershipsAndKeyword(Arrays.asList("root"), "keTING", null);
    Assert.assertEquals(1, projects.getSize());
  }
}

