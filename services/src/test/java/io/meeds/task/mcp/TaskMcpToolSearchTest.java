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
package io.meeds.task.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.model.TaskSearchFilter;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.LabelService;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.service.TaskService;

import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.task.mcp.model.TaskSearchCollectionModel;
import io.meeds.task.mcp.model.TaskSearchResultModel;
import io.meeds.task.plugin.TaskAclPlugin;

/**
 * Pins the search_tasks tool (EXO-91903): the filter it hands to the search of
 * the Tasks application, the project it is limited to, the order of the open
 * and the completed tasks, and the view permission every hit is checked
 * against.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskMcpToolSearchTest {

  private static final String     USER          = "root";

  private static final long       PROJECT_ID    = 8319L;

  private static final long       OTHER_PROJECT = 77L;

  @Mock
  private ProjectService          projectService;

  @Mock
  private StatusService           statusService;

  @Mock
  private TaskService             taskService;

  @Mock
  private CommentService          commentService;

  @Mock
  private LabelService            labelService;

  @Mock
  private SpaceService            spaceService;

  @Mock
  private TranslationService      translationService;

  @Mock
  private ResourceBundleService   resourceBundleService;

  @Mock
  private IdentityManager         identityManager;

  @Mock
  private ProfilePropertyService  profilePropertyService;

  @Mock
  private UserACL                 userAcl;

  @Mock
  private UserPortalConfigService portalConfigService;

  @Mock
  private PermanentLinkService    permanentLinkService;

  @Mock
  private FavoriteService         favoriteService;

  @Mock
  private TaskSpaceScopeResolver  spaceScopeResolver;

  @Mock
  private Identity                currentIdentity;

  @Mock
  private ProjectDto              project;

  @Mock
  private ProjectDto              otherProject;

  private TaskMcpTool             tool;

  /**
   * A tool acting as {@value #USER}, member of one space, who can view the
   * project {@value #PROJECT_ID} and every task the tests do not refuse.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Before
  public void setUp() throws Exception {// NOSONAR
    lenient().when(currentIdentity.getUserId()).thenReturn(USER);
    lenient().when(currentIdentity.getMemberships())
             .thenReturn(List.of(new MembershipEntry("/spaces/sales", "member")));
    ResourceBundle emptyBundle = new ListResourceBundle() {
      @Override
      protected Object[][] getContents() {
        return new Object[0][0];
      }
    };
    lenient().when(resourceBundleService.getResourceBundle(anyString(), any(Locale.class))).thenReturn(emptyBundle);
    lenient().when(permanentLinkService.getLink(any())).thenReturn("/task-link");
    lenient().when(project.getId()).thenReturn(PROJECT_ID);
    lenient().when(project.getName()).thenReturn("Communication follow-up");
    lenient().when(project.canView(currentIdentity)).thenReturn(true);
    lenient().when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    lenient().when(otherProject.getId()).thenReturn(OTHER_PROJECT);
    lenient().when(userAcl.hasAccessPermission(eq(TaskAclPlugin.OBJECT_TYPE), anyString(), eq(USER))).thenReturn(true);
    lenient().when(labelService.findLabelsByTask(any(), anyLong(), eq(currentIdentity), eq(0), anyInt()))
             .thenReturn(Collections.emptyList());
    tool = new TaskMcpTool(projectService,
                           statusService,
                           taskService,
                           commentService,
                           labelService,
                           spaceService,
                           translationService,
                           resourceBundleService,
                           identityManager,
                           profilePropertyService,
                           userAcl,
                           portalConfigService,
                           permanentLinkService,
                           favoriteService,
                           spaceScopeResolver) {
      @Override
      public Identity getCurrentUserAclIdentity() {
        return currentIdentity;
      }

      @Override
      public String getCurrentUserName() {
        return USER;
      }

      @Override
      public Locale getCurrentUserLocale() {
        return Locale.ENGLISH;
      }
    };
  }

  /**
   * A search needs a text: a blank one is refused before any search runs.
   */
  @Test
  public void aBlankQueryIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> tool.searchTasks("  ", null, null, null));
    verify(taskService, never()).findTasks(any(TaskSearchFilter.class));
  }

  /**
   * A project the user cannot view is refused, and nothing is searched: its
   * tasks are never returned, not even through the index.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void aProjectTheUserCannotViewIsRefusedBeforeAnySearch() throws Exception {// NOSONAR
    when(project.canView(currentIdentity)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> tool.searchTasks("Grand Dax", PROJECT_ID, null, null));
    verify(taskService, never()).findTasks(any(TaskSearchFilter.class));
  }

  /**
   * A project that does not exist is not found, and nothing is searched.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void aMissingProjectIsNotFound() throws Exception {// NOSONAR
    when(projectService.getProject(PROJECT_ID)).thenReturn(null);

    assertThrows(ObjectNotFoundException.class, () -> tool.searchTasks("Grand Dax", PROJECT_ID, null, null));
    verify(taskService, never()).findTasks(any(TaskSearchFilter.class));
  }

  /**
   * The search is the Tasks application's: the user, the words of the text
   * without their accents (the index folds accents on the tasks it indexes,
   * not on the text searched), the user and their memberships as permissions, the project
   * searched, and the 100 most relevant hits.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void theSearchIsTheTasksApplicationsWithTheTextFoldedAndTheProject() throws Exception {// NOSONAR
    when(taskService.findTasks(any(TaskSearchFilter.class))).thenReturn(Collections.emptyList());

    TaskSearchCollectionModel result = tool.searchTasks("  Grând Dàx ", PROJECT_ID, null, null);

    ArgumentCaptor<TaskSearchFilter> filter = ArgumentCaptor.forClass(TaskSearchFilter.class);
    verify(taskService).findTasks(filter.capture());
    assertEquals(USER, filter.getValue().getUserId());
    assertEquals("grand dax", filter.getValue().getTerm());
    assertTrue(filter.getValue().getPermissions().contains(USER));
    assertTrue(filter.getValue().getPermissions().contains("member:/spaces/sales"));
    assertEquals(List.of(PROJECT_ID), filter.getValue().getProjectIds());
    assertEquals(0, filter.getValue().getOffset());
    assertEquals(100, filter.getValue().getLimit());
    assertTrue(result.getTasks().isEmpty());
    assertEquals(10, result.getUsedLimit());
    assertFalse(result.isHasMore());
  }

  /**
   * The words reach the index as words only: no field query, escape, range,
   * wildcard or operator of the query syntax, and a text holding no word with
   * letters is refused like a blank one.
   */
  @Test
  public void theTextReachesTheIndexAsWordsOnly() {
    assertEquals("grand dax x and y or not z", TaskMcpTool.toSearchWords(" Grand:Dax \\ <x> AND y=OR NOT* z~ "));
    assertEquals("etude cafe 2026", TaskMcpTool.toSearchWords("Étude (café) \"2026\""));
    assertEquals("", TaskMcpTool.toSearchWords(" :*\\ "));
    assertEquals("", TaskMcpTool.toSearchWords(null));
    assertThrows(IllegalArgumentException.class, () -> tool.searchTasks(" :*? ", null, null, null));
    // the index's search analyzer keeps letters only: a number alone would
    // silently find nothing
    assertThrows(IllegalArgumentException.class, () -> tool.searchTasks("2026", null, null, null));
    verify(taskService, never()).findTasks(any(TaskSearchFilter.class));
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
      hits.add(task(id, "Grand Dax done " + id, true, project));
    }
    hits.add(task(100L, "Grand Dax", false, project));
    when(taskService.findTasks(any(TaskSearchFilter.class))).thenReturn(hits);

    TaskSearchCollectionModel result = tool.searchTasks("Grand Dax", PROJECT_ID, null, null);
    assertEquals(List.of(100L), ids(result));
    assertTrue(result.isHasMore());

    when(taskService.findTasks(any(TaskSearchFilter.class))).thenReturn(hits.subList(1, 100));
    assertFalse(tool.searchTasks("Grand Dax", PROJECT_ID, null, null).isHasMore());
  }

  /**
   * Without a project, the search names none: every task the user can view.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void withoutAProjectTheSearchNamesNone() throws Exception {// NOSONAR
    when(taskService.findTasks(any(TaskSearchFilter.class))).thenReturn(Collections.emptyList());

    tool.searchTasks("Grand Dax", null, null, null);

    ArgumentCaptor<TaskSearchFilter> filter = ArgumentCaptor.forClass(TaskSearchFilter.class);
    verify(taskService).findTasks(filter.capture());
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
    TaskDto completed = task(1L, "Grand Dax 2025", true, project);
    TaskDto open = task(2L, "Grand Dax", false, project);
    TaskDto openToo = task(3L, "Grand Dax follow-up", false, project);
    when(taskService.findTasks(any(TaskSearchFilter.class))).thenReturn(List.of(completed, open, openToo));

    assertEquals(List.of(2L, 3L), ids(tool.searchTasks("Grand Dax", PROJECT_ID, null, null)));
    assertEquals(List.of(2L, 3L), ids(tool.searchTasks("Grand Dax", PROJECT_ID, false, null)));
    assertEquals(List.of(2L, 3L, 1L), ids(tool.searchTasks("Grand Dax", PROJECT_ID, true, null)));
  }

  /**
   * A hit the user can no longer view, or a hit of another project than the
   * one searched, is never returned, whatever the index says.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void aHitTheUserCannotViewOrOfAnotherProjectIsNeverReturned() throws Exception {// NOSONAR
    TaskDto hidden = task(1L, "Grand Dax hidden", false, project);
    TaskDto elsewhere = task(2L, "Grand Dax elsewhere", false, otherProject);
    TaskDto visible = task(3L, "Grand Dax", false, project);
    when(userAcl.hasAccessPermission(TaskAclPlugin.OBJECT_TYPE, "1", USER)).thenReturn(false);
    when(taskService.findTasks(any(TaskSearchFilter.class))).thenReturn(List.of(hidden, elsewhere, visible));

    assertEquals(List.of(3L), ids(tool.searchTasks("Grand Dax", PROJECT_ID, true, null)));
    assertEquals(List.of(2L, 3L), ids(tool.searchTasks("Grand Dax", null, true, null)));
  }

  /**
   * A hit carries what tells the card apart on its board: its identifier,
   * title, link, project, status column and labels.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void aHitCarriesItsProjectStatusAndLabels() throws Exception {// NOSONAR
    TaskDto card = task(5L, "Grand Dax", false, project);
    LabelDto label = new LabelDto();
    label.setId(9L);
    label.setName("Customer");
    when(labelService.findLabelsByTask(eq(card), eq(PROJECT_ID), eq(currentIdentity), eq(0), anyInt())).thenReturn(List.of(label));
    when(taskService.findTasks(any(TaskSearchFilter.class))).thenReturn(List.of(card));

    TaskSearchResultModel hit = tool.searchTasks("Grand Dax", PROJECT_ID, null, null).getTasks().get(0);

    assertEquals(5L, hit.getId());
    assertEquals("Grand Dax", hit.getTitle());
    assertTrue(hit.getLink().endsWith("/task-link"));
    assertFalse(hit.isCompleted());
    assertEquals(Long.valueOf(PROJECT_ID), hit.getProjectId());
    assertEquals("Communication follow-up", hit.getProjectName());
    assertEquals("In progress", hit.getStatus().getName());
    assertEquals(2, hit.getStatus().getColumnPosition());
    assertEquals(1, hit.getLabels().size());
    assertEquals("Customer", hit.getLabels().get(0).getName());
  }

  /**
   * No more tasks than the limit are returned, and has_more says when more
   * matched; a limit of exactly the tasks found says there are no more.
   *
   * @throws Exception never, the project lookup declares it
   */
  @Test
  public void theLimitBoundsTheTasksAndHasMoreSaysWhenMoreMatched() throws Exception {// NOSONAR
    List<TaskDto> hits = new ArrayList<>();
    for (long id = 1; id <= 3; id++) {
      hits.add(task(id, "Grand Dax " + id, false, project));
    }
    when(taskService.findTasks(any(TaskSearchFilter.class))).thenReturn(hits);

    TaskSearchCollectionModel limited = tool.searchTasks("Grand Dax", PROJECT_ID, null, 2);
    assertEquals(List.of(1L, 2L), ids(limited));
    assertEquals(2, limited.getUsedLimit());
    assertTrue(limited.isHasMore());

    TaskSearchCollectionModel whole = tool.searchTasks("Grand Dax", PROJECT_ID, null, 3);
    assertEquals(List.of(1L, 2L, 3L), ids(whole));
    assertFalse(whole.isHasMore());
  }

  /**
   * @param id the task identifier
   * @param title the task title
   * @param completed whether the task is completed
   * @param taskProject the project of the task's status
   * @return a task in the "In progress" column of that project
   */
  private TaskDto task(long id, String title, boolean completed, ProjectDto taskProject) {
    TaskDto task = mock(TaskDto.class);
    StatusDto status = mock(StatusDto.class);
    lenient().when(task.getId()).thenReturn(id);
    lenient().when(task.getTitle()).thenReturn(title);
    lenient().when(task.isCompleted()).thenReturn(completed);
    lenient().when(task.getStatus()).thenReturn(status);
    lenient().when(status.getId()).thenReturn(30L);
    lenient().when(status.getName()).thenReturn("In progress");
    lenient().when(status.getRank()).thenReturn(2);
    lenient().when(status.getProject()).thenReturn(taskProject);
    return task;
  }

  /**
   * @param result a search result
   * @return the identifiers of its tasks, in order
   */
  private static List<Long> ids(TaskSearchCollectionModel result) {
    return result.getTasks().stream().map(TaskSearchResultModel::getId).toList();
  }

}
