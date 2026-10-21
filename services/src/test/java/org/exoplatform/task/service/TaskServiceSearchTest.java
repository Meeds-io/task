/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.model.TaskSearchFilter;
import org.exoplatform.task.model.TaskTextSearchResult;
import org.exoplatform.task.service.impl.TaskServiceImpl;
import org.exoplatform.task.storage.TaskStorage;
import org.exoplatform.task.util.TaskUtil;

import io.meeds.task.plugin.TaskAclPlugin;

/**
 * Pins the text search of the tasks (EXO-91903), the rules behind the
 * search_tasks tool: the words the index is searched with, the filter handed
 * to the search of the Tasks application, the project it is limited to, the
 * order of the open and the completed tasks, the view permission every hit is
 * checked against, and when more tasks may match.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskServiceSearchTest {

  private static final String                 USER          = "root";

  private static final long                   PROJECT_ID    = 8319L;

  private static final long                   OTHER_PROJECT = 77L;

  @Mock
  private TaskStorage                         taskStorage;

  @Mock
  private DAOHandler                          daoHandler;

  @Mock
  private ListenerService                     listenerService;

  @Mock
  private ProjectService                      projectService;

  @Mock
  private UserACL                             userAcl;

  @Mock
  private Identity                            identity;

  @Mock
  private ProjectDto                          project;

  @Mock
  private ProjectDto                          otherProject;

  private MockedStatic<ExoContainerContext>   containerContext;

  private TaskService                         taskService;

  /**
   * A user, {@value #USER}, member of one space, who can view the project
   * {@value #PROJECT_ID} and every task the tests do not refuse; the project
   * service and the ACL are the container's.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Before
  public void setUp() throws Exception {// NOSONAR
    lenient().when(identity.getUserId()).thenReturn(USER);
    lenient().when(identity.getMemberships()).thenReturn(List.of(new MembershipEntry("/spaces/sales", "member")));
    lenient().when(project.getId()).thenReturn(PROJECT_ID);
    lenient().when(project.canView(identity)).thenReturn(true);
    lenient().when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    lenient().when(otherProject.getId()).thenReturn(OTHER_PROJECT);
    lenient().when(userAcl.hasAccessPermission(eq(TaskAclPlugin.OBJECT_TYPE), anyString(), eq(USER))).thenReturn(true);
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(() -> ExoContainerContext.getService(ProjectService.class)).thenReturn(projectService);
    containerContext.when(() -> ExoContainerContext.getService(UserACL.class)).thenReturn(userAcl);
    taskService = new TaskServiceImpl(taskStorage, daoHandler, listenerService);
  }

  /**
   * Releases the container's static lookup.
   */
  @After
  public void tearDown() {
    containerContext.close();
  }

  /**
   * The words reach the index as words only: no field query, escape, range,
   * wildcard or operator of the query syntax, and without accents.
   */
  @Test
  public void theTextReachesTheIndexAsWordsOnly() {
    assertEquals("grand dax x and y or not z", TaskUtil.toSearchWords(" Grand:Dax \\ <x> AND y=OR NOT* z~ "));
    assertEquals("etude cafe 2026", TaskUtil.toSearchWords("Étude (café) \"2026\""));
    assertEquals("", TaskUtil.toSearchWords(" :*\\ "));
    assertEquals("", TaskUtil.toSearchWords(null));
  }

  /**
   * A text holding no word with letters is refused with its code before any
   * search runs: a blank one, one of symbols only, and a number alone, which
   * the index's search analyzer, keeping letters only, would silently find
   * nothing for.
   */
  @Test
  public void aTextWithoutAWordWithLettersIsRefused() {
    for (String text : new String[] { null, "  ", " :*? ", "2026" }) {
      IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                                                      () -> taskService.searchTasks(text, null, false, 10, identity));
      assertEquals(TaskService.TEXT_SEARCH_NO_WORD, refusal.getMessage());
    }
    verify(taskStorage, never()).findTasks(any(TaskSearchFilter.class));
  }

  /**
   * A project the user cannot view is refused, and nothing is searched: its
   * tasks are never returned, not even through the index.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void aProjectTheUserCannotViewIsRefusedBeforeAnySearch() throws Exception {// NOSONAR
    when(project.canView(identity)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> taskService.searchTasks("Grand Dax", PROJECT_ID, false, 10, identity));
    verify(taskStorage, never()).findTasks(any(TaskSearchFilter.class));
  }

  /**
   * A project that does not exist is not found, and nothing is searched:
   * whether the project service throws, as it does for a missing project, or
   * answers none.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void aMissingProjectIsNotFound() throws Exception {// NOSONAR
    when(projectService.getProject(PROJECT_ID)).thenThrow(new EntityNotFoundException(PROJECT_ID, ProjectDto.class));
    assertThrows(ObjectNotFoundException.class, () -> taskService.searchTasks("Grand Dax", PROJECT_ID, false, 10, identity));

    when(projectService.getProject(OTHER_PROJECT)).thenReturn(null);
    assertThrows(ObjectNotFoundException.class, () -> taskService.searchTasks("Grand Dax", OTHER_PROJECT, false, 10, identity));
    verify(taskStorage, never()).findTasks(any(TaskSearchFilter.class));
  }

  /**
   * The search is the Tasks application's: the user, the words of the text
   * without their accents (the index folds accents on the tasks it indexes,
   * not on the text searched), the user and their memberships as
   * permissions, the project searched, and the 100 most relevant hits.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void theSearchIsTheTasksApplicationsWithTheTextFoldedAndTheProject() throws Exception {// NOSONAR
    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenReturn(Collections.emptyList());

    TaskTextSearchResult result = taskService.searchTasks("  Grând Dàx ", PROJECT_ID, false, 10, identity);

    ArgumentCaptor<TaskSearchFilter> filter = ArgumentCaptor.forClass(TaskSearchFilter.class);
    verify(taskStorage).findTasks(filter.capture());
    assertEquals(USER, filter.getValue().getUserId());
    assertEquals("grand dax", filter.getValue().getTerm());
    assertTrue(filter.getValue().getPermissions().contains(USER));
    assertTrue(filter.getValue().getPermissions().contains("member:/spaces/sales"));
    assertEquals(List.of(PROJECT_ID), filter.getValue().getProjectIds());
    assertEquals(0, filter.getValue().getOffset());
    assertEquals(100, filter.getValue().getLimit());
    assertTrue(result.getTasks().isEmpty());
    assertEquals(10, result.getLimit());
    assertFalse(result.isHasMore());
  }

  /**
   * Without a project, the search names none: every task the user can view.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void withoutAProjectTheSearchNamesNone() throws Exception {// NOSONAR
    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenReturn(Collections.emptyList());

    taskService.searchTasks("Grand Dax", null, false, 10, identity);

    ArgumentCaptor<TaskSearchFilter> filter = ArgumentCaptor.forClass(TaskSearchFilter.class);
    verify(taskStorage).findTasks(filter.capture());
    assertTrue(filter.getValue().getProjectIds() == null || filter.getValue().getProjectIds().isEmpty());
    verify(projectService, never()).getProject(any());
  }

  /**
   * The completed tasks are left out by default; asked for, they come after
   * every open task, each group in the order of relevance.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void theOpenTasksComeFirstAndTheCompletedOnesOnlyOnRequest() throws Exception {// NOSONAR
    TaskDto completed = task(1L, true, project);
    TaskDto open = task(2L, false, project);
    TaskDto openToo = task(3L, false, project);
    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenReturn(List.of(completed, open, openToo));

    assertEquals(List.of(2L, 3L), ids(taskService.searchTasks("Grand Dax", PROJECT_ID, false, 10, identity)));
    assertEquals(List.of(2L, 3L, 1L), ids(taskService.searchTasks("Grand Dax", PROJECT_ID, true, 10, identity)));
  }

  /**
   * A hit the user can no longer view, or a hit of another project than the
   * one searched, is never returned, whatever the index says.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void aHitTheUserCannotViewOrOfAnotherProjectIsNeverReturned() throws Exception {// NOSONAR
    TaskDto hidden = task(1L, false, project);
    TaskDto elsewhere = task(2L, false, otherProject);
    TaskDto visible = task(3L, false, project);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, "1", USER)).thenReturn(false);
    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenReturn(List.of(hidden, elsewhere, visible));

    assertEquals(List.of(3L), ids(taskService.searchTasks("Grand Dax", PROJECT_ID, true, 10, identity)));
    assertEquals(List.of(2L, 3L), ids(taskService.searchTasks("Grand Dax", null, true, 10, identity)));
  }

  /**
   * No more tasks than the limit are returned, and the result says when more
   * matched; a limit of exactly the tasks found says there are no more. The
   * limit is bounded between 1 and the window of hits.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void theLimitBoundsTheTasksAndHasMoreSaysWhenMoreMatched() throws Exception {// NOSONAR
    List<TaskDto> hits = new ArrayList<>();
    for (long id = 1; id <= 3; id++) {
      hits.add(task(id, false, project));
    }
    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenReturn(hits);

    TaskTextSearchResult limited = taskService.searchTasks("Grand Dax", PROJECT_ID, false, 2, identity);
    assertEquals(List.of(1L, 2L), ids(limited));
    assertEquals(2, limited.getLimit());
    assertTrue(limited.isHasMore());

    TaskTextSearchResult whole = taskService.searchTasks("Grand Dax", PROJECT_ID, false, 3, identity);
    assertEquals(List.of(1L, 2L, 3L), ids(whole));
    assertFalse(whole.isHasMore());

    assertEquals(1, taskService.searchTasks("Grand Dax", PROJECT_ID, false, 0, identity).getLimit());
    assertEquals(100, taskService.searchTasks("Grand Dax", PROJECT_ID, false, 500, identity).getLimit());
  }

  /**
   * A search that read the whole window of 100 hits says more may match, even
   * when the filters kept fewer tasks than the limit: matches ranked below
   * the window were never read.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void aFullWindowOfHitsSaysMoreMayMatch() throws Exception {// NOSONAR
    List<TaskDto> hits = new ArrayList<>();
    for (long id = 1; id <= 99; id++) {
      hits.add(task(id, true, project));
    }
    hits.add(task(100L, false, project));
    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenReturn(hits);

    TaskTextSearchResult result = taskService.searchTasks("Grand Dax", PROJECT_ID, false, 10, identity);
    assertEquals(List.of(100L), ids(result));
    assertTrue(result.isHasMore());

    when(taskStorage.findTasks(any(TaskSearchFilter.class))).thenReturn(hits.subList(1, 100));
    assertFalse(taskService.searchTasks("Grand Dax", PROJECT_ID, false, 10, identity).isHasMore());
  }

  /**
   * @param id the task identifier
   * @param completed whether the task is completed
   * @param taskProject the project of the task's status
   * @return a task in a status of that project
   */
  private TaskDto task(long id, boolean completed, ProjectDto taskProject) {
    TaskDto task = mock(TaskDto.class);
    StatusDto status = mock(StatusDto.class);
    lenient().when(task.getId()).thenReturn(id);
    lenient().when(task.isCompleted()).thenReturn(completed);
    lenient().when(task.getStatus()).thenReturn(status);
    lenient().when(status.getProject()).thenReturn(taskProject);
    return task;
  }

  /**
   * @param result a search result
   * @return the identifiers of its tasks, in order
   */
  private static List<Long> ids(TaskTextSearchResult result) {
    return result.getTasks().stream().map(TaskDto::getId).toList();
  }

}
