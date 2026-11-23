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
package org.exoplatform.task.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.TestUtils;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.dao.condition.AggregateCondition;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dao.condition.Conditions;
import org.exoplatform.task.dao.condition.SingleCondition;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.model.User;
import org.exoplatform.task.rest.model.PaginatedTaskList;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.LabelService;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.service.UserService;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.storage.StatusStorage;
import org.mockito.ArgumentCaptor;

@RunWith(MockitoJUnitRunner.class)
public class TestProjectRestService {
  @Mock
  TaskService    taskService;

  @Mock
  ProjectService projectService;

  @Mock
  ProjectStorage projectStorage;

  @Mock
  StatusService  statusService;

  @Mock
  StatusStorage  statusStorage;

  @Mock
  UserService    userService;

  @Mock
  SpaceService   spaceService;

  @Mock
  CommentService commentService;

  @Mock
  LabelService   labelService;

  @Mock
  IdentityManager identityManager;

  @Mock
  FavoriteService favoriteService;

  @Before
  public void setup() {
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
  }

  @Test
  public void testGetTasks() throws Exception {
    // Given
    TaskRestService taskRestService = new TaskRestService(taskService,
                                                          commentService,
                                                          projectService,
                                                          statusService,
                                                          userService,
                                                          spaceService,
                                                          labelService,
                                                          favoriteService,
                                                          identityManager);
    Identity root = new Identity("root");
    ConversationState.setCurrent(new ConversationState(root));
    TaskDto task1 = new TaskDto();
    TaskDto task2 = new TaskDto();
    TaskDto task3 = new TaskDto();
    TaskDto task4 = new TaskDto();
    List<TaskDto> uncompletedTasks = new ArrayList<TaskDto>();
    task1.setCompleted(true);
    uncompletedTasks.add(task2);
    uncompletedTasks.add(task3);
    uncompletedTasks.add(task4);
    List<TaskDto> overdueTasks = new ArrayList<TaskDto>();
    overdueTasks.add(task1);
    overdueTasks.add(task2);
    List<TaskDto> incomingTasks = new ArrayList<TaskDto>();
    incomingTasks.add(task1);
    incomingTasks.add(task2);

    when(taskService.getUncompletedTasks("root", 20)).thenReturn(uncompletedTasks);
    when(taskService.countUncompletedTasks("root")).thenReturn(Long.valueOf(uncompletedTasks.size()));
    when(taskService.getOverdueTasks("root", 20)).thenReturn(overdueTasks);
    when(taskService.countOverdueTasks("root")).thenReturn(Long.valueOf(overdueTasks.size()));
    when(taskService.getIncomingTasks("root", 0, 20)).thenReturn(incomingTasks);
    when(taskService.countIncomingTasks("root")).thenReturn(incomingTasks.size());
    when(taskService.countTasks(eq("root"), eq("searchTerm"), anyList())).thenReturn(1L);

    // When
    Response response = taskRestService.getTasks("overdue", null, null, null, null, 0, 20, false, false);
    Response response1 = taskRestService.getTasks("incoming", null, null, null, null, 0, 20, false, false);
    Response response2 = taskRestService.getTasks("", null, null, null, null, 0, 20, false, false);
    Response response3 = taskRestService.getTasks("whatever", "searchTerm", null, null, null, 0, 20, true, false);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    PaginatedTaskList tasks = (PaginatedTaskList) response.getEntity();
    assertNotNull(tasks);
    assertEquals(2, tasks.getTasksNumber());

    assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
    PaginatedTaskList tasks1 = (PaginatedTaskList) response1.getEntity();
    assertNotNull(tasks1);
    assertEquals(2, tasks1.getTasksNumber());

    assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
    PaginatedTaskList tasks2 = (PaginatedTaskList) response2.getEntity();
    assertNotNull(tasks2);
    assertEquals(3, tasks2.getTasksNumber());

    assertEquals(Response.Status.OK.getStatusCode(), response3.getStatus());
    // JSONObject tasks3JsonObject = (JSONObject) response3.getEntity();
    // assertNotNull(tasks3JsonObject);
    // assertTrue(tasks3JsonObject.has("size"));
    // assertTrue(tasks3JsonObject.has("tasks"));
    // JSONArray tasks3 = (JSONArray) tasks3JsonObject.get("tasks");
    // assertNotNull(tasks3);
    // assertEquals(1, tasks3.length());
    // Long tasks3Size = (Long) tasks3JsonObject.get("size");
    // assertEquals(1L, tasks3Size.longValue());
  }

  @Test
  public void testGetProjects() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity root = new Identity("root");
    ConversationState.setCurrent(new ConversationState(root));

    ProjectDto project1 = new ProjectDto();
    project1.setName("project1");
    project1.setManager(java.util.Collections.singleton("root"));
    projectService.createProject(project1);
    ProjectDto project2 = new ProjectDto();
    project2.setName("project2");
    project2.setManager(java.util.Collections.singleton("root"));
    projectService.createProject(project2);
    ProjectDto project3 = new ProjectDto();
    project3.setName("project3");
    projectService.createProject(project3);

    // When
    Response response = projectRestService.getProjects(null,"", "ALL", -1, -1,false);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());

    // get projects by space name
    Space space = new Space();
    space.setDisplayName("space1");
    space.setPrettyName("space1");
    space.setGroupId(SpaceUtils.SPACE_GROUP + "/" + space.getPrettyName());
    when(spaceService.getSpaceByPrettyName("space1")).thenReturn(space);
    ConversationState.setCurrent(new ConversationState(new Identity("root",
                                                                    Arrays.asList(new MembershipEntry("/spaces/space1",
                                                                                                      "manager"),
                                                                                  new MembershipEntry("/spaces/space1",
                                                                                                      "member")))));

    List<ProjectDto> projects = Arrays.asList(project1, project2);

    when(projectService.findProjects(any(ProjectQuery.class), anyInt(), anyInt())).thenReturn(projects);
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(2);

    // When
    response = projectRestService.getProjects("", "space1", "ALL", -1, -1, false);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    JSONObject jsonObject = new JSONObject(response.getEntity().toString());
    int projectNumber = jsonObject.getInt("projectNumber");
    assertEquals(2, projectNumber);
  }

  /**
   * The list outside a space selects, with the memberships the user holds, only
   * the projects naming the user by id that still grant a right, and its
   * total is the query's: the page and the total stay consistent for a user who
   * left a space. Mutant: the user's id left among the memberships, or the
   * space project among the selected projects.
   */
  @Test
  public void testGetProjectsSelectsTheProjectsNamingTheUserOnlyWhileTheyKeepTheRight() throws Exception {
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity john = new Identity("john", Arrays.asList(new MembershipEntry("/platform/users", "member")));
    ConversationState.setCurrent(new ConversationState(john));
    ProjectDto spaceProject = new ProjectDto();
    spaceProject.setId(1);
    spaceProject.setManager(new HashSet<>(Arrays.asList("john", "manager:/spaces/team")));
    ProjectDto keptProject = new ProjectDto();
    keptProject.setId(2);
    keptProject.setParticipator(new HashSet<>(Arrays.asList("john")));
    when(projectService.findProjects(any(ProjectQuery.class), eq(0), eq(-1))).thenReturn(Arrays.asList(spaceProject, keptProject));
    ArgumentCaptor<ProjectQuery> query = ArgumentCaptor.forClass(ProjectQuery.class);
    when(projectService.findProjects(query.capture(), eq(0), eq(20))).thenReturn(new ArrayList<>());
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(25);

    Response response = projectRestService.getProjects("", null, "ALL", 0, 20, false);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals(25, new JSONObject(response.getEntity().toString()).getInt("projectNumber"));
    Condition condition = query.getAllValues().get(query.getAllValues().size() - 1).getCondition();
    assertEquals(Arrays.asList(2L), valuesOf(condition, Conditions.ID));
    assertTrue(valuesOf(condition, Conditions.MANAGER).contains("member:/platform/users"));
    assertFalse("the user's id is not a membership", valuesOf(condition, Conditions.MANAGER).contains("john"));
    assertFalse(valuesOf(condition, Conditions.PARTICIPATOR).contains("john"));
  }

  private static List<Object> valuesOf(Condition condition, String field) {
    List<Object> values = new ArrayList<>();
    if (condition instanceof AggregateCondition aggregate) {
      for (Condition child : aggregate.getConditions()) {
        values.addAll(valuesOf(child, field));
      }
    } else if (condition instanceof SingleCondition<?> single && field.equals(single.getField())) {
      if (single.getValue() instanceof Collection<?> collection) {
        values.addAll(collection);
      } else {
        values.add(single.getValue());
      }
    }
    return values;
  }

  @Test
  public void testGetProjectsOfASpaceMatchesTheSpaceMembershipsAndTheUsers() throws Exception {
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Space space = new Space();
    space.setPrettyName("space1");
    space.setGroupId(SpaceUtils.SPACE_GROUP + "/space1");
    when(spaceService.getSpaceByPrettyName("space1")).thenReturn(space);
    ConversationState.setCurrent(new ConversationState(new Identity("mary",
                                                                    Arrays.asList(new MembershipEntry("/spaces/space1",
                                                                                                      "member")))));
    when(projectService.countProjects(any(ProjectQuery.class))).thenReturn(1);

    Response response = projectRestService.getProjects("pro", "space1", "ALL", 0, 10, false);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals(1, new JSONObject(response.getEntity().toString()).getInt("projectNumber"));
    ArgumentCaptor<ProjectQuery> listQuery = ArgumentCaptor.forClass(ProjectQuery.class);
    ArgumentCaptor<ProjectQuery> countQuery = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).findProjects(listQuery.capture(), eq(0), eq(10));
    verify(projectService).countProjects(countQuery.capture());
    // the total is counted by the list's own query
    assertSame(listQuery.getValue(), countQuery.getValue());
    // the space's projects, and among them those the user's memberships match
    assertEquals(Arrays.asList(new HashSet<>(Arrays.asList("manager:/spaces/space1", "member:/spaces/space1")),
                               new HashSet<>(Arrays.asList("*:/spaces/space1", "member:/spaces/space1"))),
                 inConditions(listQuery.getValue().getCondition()));
    assertEquals(new HashSet<>(Arrays.asList("%pro%")), conditionValues(listQuery.getValue().getCondition(), SingleCondition.LIKE));
  }

  @Test
  public void testGetProjectsOfASpaceMatchesAMembershipOfAnyType() throws Exception {
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Space space = new Space();
    space.setPrettyName("space1");
    space.setGroupId(SpaceUtils.SPACE_GROUP + "/space1");
    when(spaceService.getSpaceByPrettyName("space1")).thenReturn(space);
    ConversationState.setCurrent(new ConversationState(new Identity("mary",
                                                                    Arrays.asList(new MembershipEntry("/spaces/space1")))));

    projectRestService.getProjects("", "space1", "ALL", 0, 10, false);

    ArgumentCaptor<ProjectQuery> listQuery = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).findProjects(listQuery.capture(), eq(0), eq(10));
    assertEquals(Arrays.asList(new HashSet<>(Arrays.asList("manager:/spaces/space1", "member:/spaces/space1")),
                               new HashSet<>(Arrays.asList("*:/spaces/space1",
                                                           "manager:/spaces/space1",
                                                           "member:/spaces/space1"))),
                 inConditions(listQuery.getValue().getCondition()));
  }

  @Test
  public void testGetProjectsOfASpaceTheUserIsNotPartOfDoesNotMatchTheProjectsNamingTheUser() throws Exception {
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Space space = new Space();
    space.setPrettyName("space1");
    space.setGroupId(SpaceUtils.SPACE_GROUP + "/space1");
    when(spaceService.getSpaceByPrettyName("space1")).thenReturn(space);
    ConversationState.setCurrent(new ConversationState(new Identity("mary",
                                                                    Arrays.asList(new MembershipEntry("/spaces/other",
                                                                                                      "member")))));

    projectRestService.getProjects("pro", "space1", "ALL", 0, 10, false);

    ArgumentCaptor<ProjectQuery> listQuery = ArgumentCaptor.forClass(ProjectQuery.class);
    verify(projectService).findProjects(listQuery.capture(), eq(0), eq(10));
    // a project naming the user grants no right outside the user's spaces: the
    // user's id is not one of the values the projects of the space are matched with
    assertEquals(Arrays.asList(new HashSet<>(Arrays.asList("manager:/spaces/space1", "member:/spaces/space1")),
                               new HashSet<>(Arrays.asList("*:/spaces/other", "member:/spaces/other"))),
                 inConditions(listQuery.getValue().getCondition()));
  }

  @Test
  public void testGetProjectsOfAnUnknownSpaceGivesNothing() throws Exception {
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    ConversationState.setCurrent(new ConversationState(new Identity("mary", Arrays.asList())));

    Response response = projectRestService.getProjects("pro", "unknown", "ALL", 0, 10, false);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    JSONObject result = new JSONObject(response.getEntity().toString());
    assertEquals(0, result.getInt("projectNumber"));
    assertEquals(0, result.getJSONArray("projects").length());
    verify(projectService, never()).findProjects(any(ProjectQuery.class), anyInt(), anyInt());
    verify(projectService, never()).countProjects(any(ProjectQuery.class));
  }

  // the distinct value sets the IN conditions of a query test, in order: one per condition ANDed
  private static List<Set<String>> inConditions(Condition condition) {
    List<Set<String>> sets = new ArrayList<>();
    if (condition instanceof AggregateCondition aggregate) {
      aggregate.getConditions().forEach(child -> inConditions(child).stream().filter(s -> !sets.contains(s)).forEach(sets::add));
    } else if (condition instanceof SingleCondition<?> single && SingleCondition.IN.equals(single.getType())
               && single.getValue() instanceof Collection<?> collection) {
      Set<String> values = new HashSet<>();
      collection.forEach(value -> values.add(String.valueOf(value)));
      if (!sets.contains(values)) {
        sets.add(values);
      }
    }
    return sets;
  }

  // the values a query's conditions of the given type test, at any depth
  private static Set<String> conditionValues(Condition condition, String type) {
    Set<String> values = new HashSet<>();
    if (condition instanceof AggregateCondition aggregate) {
      aggregate.getConditions().forEach(child -> values.addAll(conditionValues(child, type)));
    } else if (condition instanceof SingleCondition<?> single && type.equals(single.getType())) {
      if (single.getValue() instanceof Collection<?> collection) {
        collection.forEach(value -> values.add(String.valueOf(value)));
      } else {
        values.add(String.valueOf(single.getValue()));
      }
    }
    return values;
  }

  @Test
  public void testGetDefaultStatusByProjectId() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity john = new Identity("john");
    ConversationState.setCurrent(new ConversationState(john));
    ProjectDto project = new ProjectDto();
    project.setId(1);
    Set<String> manager = new HashSet<String>();
    manager.add("john");
    project.setManager(manager);
    StatusDto status = new StatusDto();
    status.setId(Long.valueOf(1));
    status.setName("status 1");

    // When
    Response response = projectRestService.getDefaultStatusByProjectId(3);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

  }

  @Test
  public void testGetProjectById() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity john = new Identity("john");
    ConversationState.setCurrent(new ConversationState(john));
    ProjectDto project = new ProjectDto();
    project.setId(1);
    Set<String> manager = new HashSet<String>();
    manager.add("john");
    project.setManager(manager);

    when(projectService.getProject(1L)).thenReturn(project);
    org.exoplatform.social.core.identity.model.Identity johnIdentity = mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(johnIdentity.getId()).thenReturn("1");
    when(identityManager.getOrCreateUserIdentity("john")).thenReturn(johnIdentity);
    when(favoriteService.isFavorite(any())).thenReturn(true);

    // When
    Response response = projectRestService.getProjectById(1,true);
    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(new JSONObject((String) response.getEntity()).getBoolean("favorite"));

    when(projectService.getManager(1)).thenReturn(new HashSet<>(Arrays.asList(null, "")));
    when(projectService.getParticipator(1)).thenReturn(new HashSet<>(Arrays.asList(null, "")));

    // When
    response = projectRestService.getProjectById(1,true);
    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());

  }

  @Test
  public void testCreateProject() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity john = new Identity("john");
    ConversationState.setCurrent(new ConversationState(john));
    Set<String> manager = new HashSet<String>();
    manager.add("john");
    ProjectDto projectDto = new ProjectDto();
    projectDto.setId(1);
    projectDto.setName("john");
    projectDto.setDescription("bla bla bla");
    projectDto.setManager(manager);

    projectService.createProject(projectDto);

    when(projectService.createProject(any())).thenReturn(projectDto);

    Response response = projectRestService.createProject(projectDto);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    projectDto.setName(null);
    projectService.createProject(projectDto);

    when(projectService.createProject(any())).thenReturn(projectDto);

    response = projectRestService.createProject(projectDto);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

    projectDto.setName("john");
    projectDto.setSpaceName("space");
    projectService.createProject(projectDto);

    when(projectService.createProject(any())).thenReturn(projectDto);

    response = projectRestService.createProject(projectDto);
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());


    projectService.createProject(projectDto);
    Space space = new Space();
    when(projectService.createProject(any())).thenReturn(projectDto);
    when(spaceService.getSpaceByPrettyName(any())).thenReturn(space);
    response = projectRestService.createProject(projectDto);
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

    projectService.createProject(projectDto);
    space = new Space();
    space.setGroupId("/space");
    when(projectService.createProject(any())).thenReturn(projectDto);
    when(spaceService.getSpaceByPrettyName(any())).thenReturn(space);
    when(spaceService.isMember(space,"john")).thenReturn(true);
    response = projectRestService.createProject(projectDto);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

  }

  @Test
  public void testUpdateProject() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity john = new Identity("john");
    Identity exo = new Identity("exo");
    ConversationState.setCurrent(new ConversationState(john));
    Set<String> manager = new HashSet<String>();
    manager.add("john");
    Set<String> participator = new HashSet<String>();
    participator.add("exo");
    ProjectDto projectDto = new ProjectDto();
    projectDto.setId(1);
    projectDto.setDescription("bla bla bla");
    projectDto.setManager(manager);
    projectDto.setParticipator(participator);
    projectService.updateProject(projectDto);

    when(projectService.updateProject(any())).thenReturn(projectDto);

    Response response = projectRestService.updateProject(projectDto.getId(), projectDto);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

    projectDto.setName("john");
    projectService.updateProject(projectDto);

    when(projectService.getProject(projectDto.getId())).thenReturn(projectDto);
    response = projectRestService.updateProject(projectDto.getId(), projectDto);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    Identity root = new Identity("root");
    ConversationState.setCurrent(new ConversationState(john));

    when(projectService.getProject(projectDto.getId())).thenReturn(projectDto);
    response = projectRestService.updateProject(projectDto.getId(), projectDto);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

  }

  @Test
  public void testGetSatusesByProjectId() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity root = new Identity("root");
    ConversationState.setCurrent(new ConversationState(root));

    ProjectDto project1 = new ProjectDto();
    project1.setName("project1");
    projectService.createProject(project1);
    ProjectDto project2 = new ProjectDto();
    project2.setName("project2");
    projectService.createProject(project2);
    ProjectDto project3 = new ProjectDto();
    project3.setName("project3");
    projectService.createProject(project3);

    StatusDto status1 = new StatusDto(3, "ToDo", 1, project1);
    StatusDto status2 = new StatusDto(4, "ToDo", 2, project1);

    List<StatusDto> statuses = new ArrayList<>();
    statuses.add(status1);
    statuses.add(status2);

    Set<StatusDto> foo = new HashSet<StatusDto>(statuses);
    project1.setStatus(foo);

    Set<String> participator = new HashSet<String>();
    participator.add("Tib");
    project1.setParticipator(participator);

    Set<String> managers = new HashSet<String>();
    managers.add("Tib");
    project1.setManager(managers);

    Set<String> participator1 = new HashSet<String>();
    participator1.add("root");
    project2.setParticipator(participator1);

    Set<String> managers1 = new HashSet<String>();
    managers1.add("root");
    project2.setManager(managers1);

    // When
    Response response1 = projectRestService.getStatusesByProjectId(project1.getId());

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response1.getStatus());

    // When
    when(projectService.getProject(project1.getId())).thenReturn(project1);
    Response response2 = projectRestService.getStatusesByProjectId(project1.getId());

    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response2.getStatus());

    // When
    when(projectService.getProject(project2.getId())).thenReturn(project2);
    Response response3 = projectRestService.getStatusesByProjectId(project2.getId());

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response3.getStatus());
    assertNotNull(response3.getEntity());

  }

  @Test
  public void testFindUsersToMention() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
            commentService,
            projectService,
            statusService,
            userService,
            spaceService,
            labelService,
            identityManager,
            favoriteService);

    Identity root = new Identity("root");
    ConversationState.setCurrent(new ConversationState(root));

    ProjectDto project1 = new ProjectDto();
    project1.setName("project1");


    final User user = TestUtils.getUser();
    ListAccess<User> lists = new ListAccess<User>() {
      @Override
      public User[] load(int i, int i1) throws Exception, IllegalArgumentException {
        return new User[] { user };
      }

      @Override
      public int getSize() throws Exception {
        return 1;
      }
    };

    when(userService.findUserByName("root", false)).thenReturn(lists);

    Response response = projectRestService.getUsersByQueryAndProjectName("root", "project1");
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testFindUsersToMentionInTasksAffectedToProject() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);

    Identity root = new Identity("root");
    final User userA = TestUtils.getUserA();
    org.exoplatform.social.core.identity.model.Identity userAIdentity = TestUtils.getUserAIdentity();

    ConversationState.setCurrent(new ConversationState(root));

    Set<String> projectParticipator = new HashSet<>();
    projectParticipator.add(root.getUserId());
    projectParticipator.add(userA.getUsername());

    ProjectDto project1 = new ProjectDto();
    project1.setName("project1");
    project1.setId(1L);
    project1.setParticipator(projectParticipator);


    //when
    Response response = projectRestService.getProjectParticipants(1L, "userA", false);

    //then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
  }

  @Test
  public void testDeleteProject() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity john = new Identity("john");
    ConversationState.setCurrent(new ConversationState(john));
    Set<String> manager = new HashSet<String>();
    manager.add("john");
    ProjectDto projectDto = new ProjectDto();
    projectDto.setId(1);
    projectDto.setName("john");
    projectDto.setDescription("bla bla bla");
    projectDto.setManager(manager);

    when(projectService.getProject(projectDto.getId())).thenReturn(projectDto);

    Response response1 = projectRestService.deleteProject(projectDto.getId(), false, 0, 0);
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response1.getStatus());
  }

  @Test
  public void testCloneProject() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity john = new Identity("john");
    ConversationState.setCurrent(new ConversationState(john));
    Set<String> manager = new HashSet<String>();
    manager.add("john");
    ProjectDto projectDto = new ProjectDto();
    projectDto.setId(1);
    projectDto.setName("john");
    projectDto.setDescription("bla bla bla");
    projectDto.setManager(manager);

    ProjectDto projectCloned = new ProjectDto();
    projectCloned.setId(2);
    projectCloned.setName("john");
    projectCloned.setDescription("bla bla bla");
    projectCloned.setManager(manager);

    when(projectService.cloneProject(projectDto.getId(), true)).thenReturn(projectCloned);
    when(projectService.getProject(projectDto.getId())).thenReturn(projectDto);

    Response response1 = projectRestService.cloneProject(projectDto);
    assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
  }

  @Test
  public void testChangeProjectColor() throws Exception {
    // Given
    ProjectRestService projectRestService = new ProjectRestService(taskService,
                                                                   commentService,
                                                                   projectService,
                                                                   statusService,
                                                                   userService,
                                                                   spaceService,
                                                                   labelService,
                                                                   identityManager,
                                                                   favoriteService);
    Identity john = new Identity("john");
    ConversationState.setCurrent(new ConversationState(john));
    Set<String> manager = new HashSet<String>();
    manager.add("john");
    ProjectDto projectDto = new ProjectDto();
    projectDto.setId(1);
    projectDto.setColor("");
    projectDto.setDescription("bla bla bla");
    projectDto.setManager(manager);

    ProjectDto projectDto1 = new ProjectDto();
    projectDto.setId(1);
    projectDto.setColor("red");
    projectDto.setDescription("bla bla bla");
    projectDto.setManager(manager);

    when(projectService.getProject(projectDto.getId())).thenReturn(projectDto);

    Response response1 = projectRestService.changeProjectColor(projectDto.getId(), "red");
    assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
  }
}
