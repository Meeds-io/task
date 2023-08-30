package org.exoplatform.task.integration;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.util.TaskUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskAttachmentPluginTest {

  private static MockedStatic<TaskUtil> TASK_UTIL;

  @Mock
  private TaskService                   taskService;

  @Mock
  private ProjectService                projectService;

  @Mock
  private SpaceService                  spaceService;

  private TaskAttachmentPlugin          taskAttachmentPlugin;

  @BeforeClass
  public static void initClassContext() {
    TASK_UTIL = mockStatic(TaskUtil.class);
  }

  @AfterClass
  public static void cleanClassContext() {
    TASK_UTIL.close();
  }

  @Before
  public void setUp() throws Exception {
    taskAttachmentPlugin = new TaskAttachmentPlugin(taskService, projectService, spaceService);
  }

  @Test
  public void testHasAccessPermission() throws EntityNotFoundException, ObjectNotFoundException {
    long taskId = 1l;
    String userId = "2";
    Identity userIdentity = Mockito.mock(org.exoplatform.services.security.Identity.class);
    TaskDto task = Mockito.mock(TaskDto.class);
    when(taskService.getTask(taskId)).thenReturn(task);
    TASK_UTIL.when(() -> TaskUtil.hasViewPermission(taskService, task, userIdentity)).thenReturn(true);
    assertTrue(taskAttachmentPlugin.hasAccessPermission(userIdentity, String.valueOf(taskId)));
  }

  @Test
  public void testHasEditPermission() throws EntityNotFoundException, ObjectNotFoundException {
    long taskId = 1l;
    Identity userIdentity = Mockito.mock(org.exoplatform.services.security.Identity.class);
    TaskDto task = Mockito.mock(TaskDto.class);
    when(taskService.getTask(taskId)).thenReturn(task);
    TASK_UTIL.when(() -> TaskUtil.hasEditPermission(taskService, task, userIdentity)).thenReturn(true);
    assertTrue(taskAttachmentPlugin.hasEditPermission(userIdentity, String.valueOf(taskId)));
  }

  @Test
  public void testGetSpaceId() throws EntityNotFoundException, ObjectNotFoundException {
    long taskId = 1l;
    long projectId = 12l;
    Set<String> permissions = new HashSet<>();
    String groupId = "space_test";
    permissions.add("spaces:space_test");
    long spaceId = 5l;

    TaskDto task = Mockito.mock(TaskDto.class);
    when(taskService.getTask(taskId)).thenReturn(task);

    StatusDto taskStatus = Mockito.mock(StatusDto.class);
    when(task.getStatus()).thenReturn(taskStatus);

    ProjectDto project = Mockito.mock(ProjectDto.class);
    when(taskStatus.getProject()).thenReturn(project);
    when(project.getId()).thenReturn(projectId);

    Space space = Mockito.mock(Space.class);

    when(projectService.getManager(projectId)).thenReturn(permissions);
    when(spaceService.getSpaceByGroupId(groupId)).thenReturn(space);
    assertNotNull(space);
    when(space.getId()).thenReturn(String.valueOf(spaceId));
    assertEquals(taskAttachmentPlugin.getSpaceId(String.valueOf(taskId)), spaceId);
  }

}
