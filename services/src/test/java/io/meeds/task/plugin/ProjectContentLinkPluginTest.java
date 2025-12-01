package io.meeds.task.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.util.UserUtil;

import io.meeds.social.cms.model.ContentLinkExtension;
import io.meeds.social.cms.model.ContentLinkSearchResult;
import io.meeds.social.cms.service.ContentLinkPluginService;

import lombok.SneakyThrows;

@RunWith(MockitoJUnitRunner.class)
public class ProjectContentLinkPluginTest {

  @Mock
  private ProjectService           projectService;

  @Mock
  private PortalContainer          container;

  @Mock
  private ContentLinkPluginService contentLinkPluginService;

  @Mock
  private Identity                 identity;

  @InjectMocks
  private ProjectContentLinkPlugin plugin;

  @Test
  public void testInitShouldRegisterPlugin() {
    when(container.getComponentInstanceOfType(ContentLinkPluginService.class)).thenReturn(contentLinkPluginService);
    plugin.init();
    verify(contentLinkPluginService).addPlugin(plugin);
  }

  @Test
  public void testGetExtension() {
    assertEquals(new ContentLinkExtension("project",
                                          "contentLink.project",
                                          "fa fa-clipboard-list",
                                          "project"),
                 plugin.getExtension());
  }

  @Test
  public void testSearchShouldDelegateToProjectServiceAndMapResults() {
    String keyword = "test";
    int offset = 0;
    int limit = 10;
    Locale locale = Locale.ENGLISH;

    List<String> memberships = Arrays.asList("member:/platform/users", "manager:/platform/administrators");

    ProjectDto project1 = mock(ProjectDto.class);
    when(project1.getId()).thenReturn(1L);
    when(project1.getName()).thenReturn("Project 1");

    ProjectDto project2 = mock(ProjectDto.class);
    when(project2.getId()).thenReturn(2L);
    when(project2.getName()).thenReturn("Project 2");

    List<ProjectDto> projects = Arrays.asList(project1, project2);

    when(projectService.findProjects(anyList(), eq(keyword), any(), eq(offset), eq(limit)))
                                                                                           .thenReturn(projects);

    try (MockedStatic<UserUtil> mocked = mockStatic(UserUtil.class)) {
      mocked.when(() -> UserUtil.getMemberships(identity)).thenReturn(memberships);

      List<ContentLinkSearchResult> results = plugin.search(keyword, identity, locale, offset, limit);

      assertNotNull(results);
      assertEquals(2, results.size());

      ContentLinkSearchResult r1 = results.get(0);
      ContentLinkSearchResult r2 = results.get(1);

      assertEquals(ProjectContentLinkPlugin.OBJECT_TYPE, r1.getObjectType());
      assertEquals("1", r1.getObjectId());
      assertEquals("Project 1", r1.getTitle());
      assertEquals(plugin.getExtension().getIcon(), r1.getIcon());
      assertEquals(plugin.getExtension().isDrawer(), r1.isDrawer());

      assertEquals(ProjectContentLinkPlugin.OBJECT_TYPE, r2.getObjectType());
      assertEquals("2", r2.getObjectId());
      assertEquals("Project 2", r2.getTitle());
      assertEquals(plugin.getExtension().getIcon(), r2.getIcon());
      assertEquals(plugin.getExtension().isDrawer(), r2.isDrawer());
    }
  }

  @Test
  public void testSearchWhenNoProjects() {
    String keyword = "nothing";
    int offset = 0;
    int limit = 5;
    Locale locale = Locale.ENGLISH;
    List<String> memberships = Collections.singletonList("member:/platform/users");
    when(projectService.findProjects(anyList(), eq(keyword), any(), eq(offset), eq(limit))).thenReturn(Collections.emptyList());
    try (MockedStatic<UserUtil> mocked = mockStatic(UserUtil.class)) {
      mocked.when(() -> UserUtil.getMemberships(identity)).thenReturn(memberships);

      List<ContentLinkSearchResult> results = plugin.search(keyword, identity, locale, offset, limit);
      assertNotNull(results);
      assertTrue(results.isEmpty());
    }
  }

  @Test
  @SneakyThrows
  public void testGetContentTitleWhenProjectExists() {
    String objectId = "123";
    long projectId = 123L;
    Locale locale = Locale.ENGLISH;

    ProjectDto project = mock(ProjectDto.class);
    when(project.getName()).thenReturn("My Project");
    when(projectService.getProject(projectId)).thenReturn(project);
    String title = plugin.getContentTitle(objectId, locale);
    assertEquals("My Project", title);
  }

  @Test
  @SneakyThrows
  public void testGetContentTitleWhenProjectIsNull() {
    String objectId = "456";
    long projectId = 456L;
    Locale locale = Locale.ENGLISH;

    when(projectService.getProject(projectId)).thenReturn(null);

    // when
    String title = plugin.getContentTitle(objectId, locale);

    // then
    assertEquals(StringUtils.EMPTY, title);
    verify(projectService).getProject(projectId);
  }

  @Test
  @SneakyThrows
  public void testGetContentTitleWhenEntityNotFound() {
    String objectId = "789";
    long projectId = 789L;
    Locale locale = Locale.ENGLISH;
    when(projectService.getProject(projectId)).thenThrow(new EntityNotFoundException(projectId, ProjectDto.class));
    String title = plugin.getContentTitle(objectId, locale);
    assertEquals(StringUtils.EMPTY, title);
    verify(projectService).getProject(projectId);
  }

}
