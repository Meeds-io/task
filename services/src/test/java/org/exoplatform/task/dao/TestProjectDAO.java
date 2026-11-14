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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

  /**
   * A project matches the name words when its name holds every one of them,
   * each ignoring case and anywhere in the name; a project holding only some
   * of them, even as part of another word, does not match; nor does a project
   * holding them all that the memberships do not reach. The count is that of
   * the projects listed.
   *
   * @throws Exception never
   */
  @Test
  public void testFindProjectsHoldingAllNameWords() throws Exception {
    Set<String> manager = new HashSet<>();
    manager.add("root");
    Project board = pDAO.create(new Project("Seed Big Board 50", null, null, manager, null));
    pDAO.create(new Project("Seed Delta Onboarding 04", null, null, manager, null));
    Project orion = pDAO.create(new Project("Seed Orion 36", null, null, manager, null));
    pDAO.create(new Project("Seed Big Board 50", null, null, Set.of("john"), Set.of("mary")));

    assertNameWordsSelect(List.of(board.getId()), "Seed Big Board 50");
    assertNameWordsSelect(List.of(board.getId()), "big  board 50");
    assertNameWordsSelect(List.of(orion.getId()), "orion");
    assertNameWordsSelect(List.of(), "seed board 36");
  }

  /**
   * The any-word selection the MCP tool lists and counts, the memberships and
   * an order given, counts each project once although its managers and
   * participants make several rows of the joins, with or without name words;
   * the projects the memberships do not reach are not counted.
   *
   * @throws Exception never
   */
  @Test
  public void testCountProjectsOfMembershipsCountsEachProjectOnce() throws Exception {
    Project board = pDAO.create(new Project("Seed Big Board 50",
                                            null,
                                            null,
                                            Set.of("root"),
                                            Set.of("john", "mary", "demo")));
    pDAO.create(new Project("Seed Orion 36", null, null, Set.of("john"), null));

    for (String keyword : Arrays.asList(null, "board orbit")) {
      ProjectQuery query = new ProjectQuery();
      query.setMembership(Arrays.asList("root"));
      query.setKeyword(keyword);
      query.setOrderBy(List.of(new OrderBy.DESC("lastModifiedDate")));
      ListAccess<Project> projects = pDAO.findProjects(query);
      Assert.assertEquals(List.of(board.getId()), Arrays.stream(projects.load(0, 10)).map(Project::getId).toList());
      Assert.assertEquals("keyword " + keyword, 1, projects.getSize());
    }
  }

  /**
   * The selection list_projects reads with managed_only: the memberships
   * against the managers only. A project naming the user among its managers
   * and one whose managers are the managers of a space the user manages are
   * selected, each counted once although the first matches two memberships;
   * a project the user only takes part in, and one the user is in neither
   * way, are not. The memberships against the managers and the participants
   * select the project the user takes part in too. Mutants: the participants
   * read for the managed selection; the count not distinct.
   *
   * @throws Exception never
   */
  @Test
  public void testManagedProjectsAreThoseWhoseManagersHoldTheMemberships() throws Exception {
    List<String> memberships = List.of("root", "*:/spaces/seed", "manager:/spaces/seed");
    Project named = pDAO.create(new Project("Seed Board Named", null, null, Set.of("root", "manager:/spaces/seed"), null));
    Project ofSpace = pDAO.create(new Project("Seed Board Space",
                                              null,
                                              null,
                                              Set.of("manager:/spaces/seed"),
                                              Set.of("member:/spaces/seed")));
    Project takesPart = pDAO.create(new Project("Seed Board Part", null, null, Set.of("mary"), Set.of("root")));
    pDAO.create(new Project("Seed Board Other", null, null, Set.of("mary"), Set.of("john")));

    for (String keyword : Arrays.asList(null, "board orbit")) {
      ProjectQuery managed = new ProjectQuery();
      managed.setManager(memberships);
      managed.setKeyword(keyword);
      managed.setOrderBy(List.of(new OrderBy.DESC("lastModifiedDate")));
      ListAccess<Project> managedProjects = pDAO.findProjects(managed);
      Assert.assertEquals("keyword " + keyword,
                          Set.of(named.getId(), ofSpace.getId()),
                          Arrays.stream(managedProjects.load(0, 10)).map(Project::getId).collect(Collectors.toSet()));
      Assert.assertEquals("keyword " + keyword, 2, managedProjects.getSize());

      ProjectQuery reached = new ProjectQuery();
      reached.setMembership(memberships);
      reached.setKeyword(keyword);
      reached.setOrderBy(List.of(new OrderBy.DESC("lastModifiedDate")));
      Assert.assertEquals("keyword " + keyword,
                          Set.of(named.getId(), ofSpace.getId(), takesPart.getId()),
                          Arrays.stream(pDAO.findProjects(reached).load(0, 10)).map(Project::getId).collect(Collectors.toSet()));
    }
  }

  /**
   * @param ids the ids of the projects expected, in their order
   * @param words words of a project name
   * @throws Exception never
   */
  private void assertNameWordsSelect(List<Long> ids, String words) throws Exception {
    ListAccess<Project> projects = findByNameWords(words);
    Assert.assertEquals(words, ids, Arrays.stream(projects.load(0, 10)).map(Project::getId).toList());
    Assert.assertEquals(words, ids.size(), projects.getSize());
  }

  /**
   * @param words words of a project name
   * @return the projects of the root membership whose name holds all of them
   */
  private ListAccess<Project> findByNameWords(String words) {
    ProjectQuery query = new ProjectQuery();
    query.setMembership(Arrays.asList("root"));
    query.setNameWords(words);
    return pDAO.findProjects(query);
  }
}
