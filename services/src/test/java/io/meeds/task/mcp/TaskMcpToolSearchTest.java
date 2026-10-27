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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.model.TaskTextSearchResult;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.service.LabelService;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.service.TaskService;

import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.task.mcp.model.TaskSearchCollectionModel;
import io.meeds.task.mcp.model.TaskSearchResultModel;

/**
 * Pins the search_tasks tool (EXO-91903) as the adapter of
 * {@link TaskService#searchTasks(String, Long, boolean, int, Identity)}, which
 * holds the rules ({@code TaskServiceSearchTest}): the arguments it hands to
 * the service, the messages of the service's refusals, and what each task
 * found returns.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskMcpToolSearchTest {

  private static final String     USER       = "root";

  private static final long       PROJECT_ID = 8319L;

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

  private TaskMcpTool             tool;

  /**
   * A tool acting as {@value #USER}, whose tasks found are in the project
   * {@value #PROJECT_ID}.
   *
   * @throws Exception never, the label lookup declares it
   */
  @Before
  public void setUp() throws Exception {// NOSONAR
    lenient().when(currentIdentity.getUserId()).thenReturn(USER);
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
   * The tool hands the service the text as given, the project, the completed
   * tasks only when asked for, the limit, 10 by default, and the current
   * user's identity; it returns the limit and has_more the service used.
   *
   * @throws Exception never, the service declares it
   */
  @Test
  public void theArgumentsReachTheServiceAndItsLimitAndHasMoreComeBack() throws Exception {// NOSONAR
    when(taskService.searchTasks(any(), any(), anyBoolean(), anyInt(), any())).thenReturn(new TaskTextSearchResult(List.of(), 7, true));

    TaskSearchCollectionModel result = tool.searchTasks(" Grând Dàx ", PROJECT_ID, null, null);
    verify(taskService).searchTasks(" Grând Dàx ", PROJECT_ID, false, 10, currentIdentity);
    assertEquals(7, result.getUsedLimit());
    assertTrue(result.isHasMore());

    tool.searchTasks("Grand Dax", null, true, 3);
    verify(taskService).searchTasks("Grand Dax", null, true, 3, currentIdentity);
    tool.searchTasks("Grand Dax", null, false, 4);
    verify(taskService).searchTasks("Grand Dax", null, false, 4, currentIdentity);
  }

  /**
   * A text the service refuses for holding no word with letters is refused
   * with the tool's message, which points to get_task_by_id; any other
   * refusal passes as it is.
   *
   * @throws Exception never, the service declares it
   */
  @Test
  public void aTextWithoutAWordIsRefusedWithTheToolsMessage() throws Exception {// NOSONAR
    when(taskService.searchTasks(eq("2026"), any(), anyBoolean(), anyInt(), any()))
                                                                                .thenThrow(new IllegalArgumentException(TaskService.TEXT_SEARCH_NO_WORD));
    when(taskService.searchTasks(eq("other"), any(), anyBoolean(), anyInt(), any())).thenThrow(new IllegalArgumentException("other"));

    IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class, () -> tool.searchTasks("2026", null, null, null));
    assertEquals("A text with a word to search is mandatory: numbers are not searched, a task id goes to get_task_by_id",
                 refusal.getMessage());
    assertEquals("other", assertThrows(IllegalArgumentException.class, () -> tool.searchTasks("other", null, null, null)).getMessage());
  }

  /**
   * A project the service does not find, or that the user cannot view, is
   * refused with the tool's messages, those of the other project tools.
   *
   * @throws Exception never, the service declares it
   */
  @Test
  public void aProjectRefusalHasTheToolsMessages() throws Exception {// NOSONAR
    when(taskService.searchTasks(any(), eq(PROJECT_ID), anyBoolean(), anyInt(), any())).thenThrow(new ObjectNotFoundException("x"));
    assertEquals("Task project with identifier '8319' is not found",
                 assertThrows(ObjectNotFoundException.class, () -> tool.searchTasks("Grand Dax", PROJECT_ID, null, null)).getMessage());

    when(taskService.searchTasks(any(), eq(9L), anyBoolean(), anyInt(), any())).thenThrow(new IllegalAccessException("x"));
    assertEquals("Task project with identifier '9' is not accessible for the user 'root'",
                 assertThrows(IllegalAccessException.class, () -> tool.searchTasks("Grand Dax", 9L, null, null)).getMessage());
  }

  /**
   * A task found carries what tells the card apart on its board: its
   * identifier, title, link, completion, project, status column and labels;
   * a personal task carries no project and no labels.
   *
   * @throws Exception never, the service declares it
   */
  @Test
  public void aHitCarriesItsProjectStatusAndLabels() throws Exception {// NOSONAR
    TaskDto card = task(5L, "Grand Dax", project);
    TaskDto personal = mock(TaskDto.class);
    lenient().when(personal.getId()).thenReturn(6L);
    lenient().when(personal.getTitle()).thenReturn("Grand Dax notes");
    lenient().when(personal.isCompleted()).thenReturn(true);
    LabelDto label = new LabelDto();
    label.setId(9L);
    label.setName("Customer");
    when(labelService.findLabelsByTask(eq(card), eq(PROJECT_ID), eq(currentIdentity), eq(0), anyInt())).thenReturn(List.of(label));
    when(taskService.searchTasks(any(), any(), anyBoolean(), anyInt(), any())).thenReturn(new TaskTextSearchResult(List.of(card, personal),
                                                                                                                   10,
                                                                                                                   false));

    List<TaskSearchResultModel> hits = tool.searchTasks("Grand Dax", PROJECT_ID, true, null).getTasks();

    TaskSearchResultModel hit = hits.get(0);
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
    TaskSearchResultModel personalHit = hits.get(1);
    assertEquals(6L, personalHit.getId());
    assertTrue(personalHit.isCompleted());
    assertNull(personalHit.getProjectId());
    assertNull(personalHit.getStatus());
    assertNull(personalHit.getLabels());
  }

  /**
   * @param id the task identifier
   * @param title the task title
   * @param taskProject the project of the task's status
   * @return an open task in the "In progress" column of that project
   */
  private TaskDto task(long id, String title, ProjectDto taskProject) {
    TaskDto task = mock(TaskDto.class);
    StatusDto status = mock(StatusDto.class);
    lenient().when(task.getId()).thenReturn(id);
    lenient().when(task.getTitle()).thenReturn(title);
    lenient().when(task.getStatus()).thenReturn(status);
    lenient().when(status.getId()).thenReturn(30L);
    lenient().when(status.getName()).thenReturn("In progress");
    lenient().when(status.getRank()).thenReturn(2);
    lenient().when(status.getProject()).thenReturn(taskProject);
    return task;
  }

}
