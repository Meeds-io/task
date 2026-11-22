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
package org.exoplatform.task.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.task.TestDtoUtils;
import org.exoplatform.task.TestUtils;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.ProjectHandler;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.dao.StatusHandler;
import org.exoplatform.task.dao.TaskHandler;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.NotAllowedOperationOnEntityException;
import org.exoplatform.task.util.TaskUtil;
import org.exoplatform.task.service.impl.StatusServiceImpl;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.storage.StatusStorage;
import org.exoplatform.task.storage.TaskStorage;
import org.exoplatform.task.storage.impl.ProjectStorageImpl;
import org.exoplatform.task.storage.impl.StatusStorageImpl;


@RunWith(MockitoJUnitRunner.Silent.class)
public class StatusServiceTest {

    MockedStatic<ExoContainerContext> containerContext;

    StatusService statusService;

    StatusStorage statusStorage;

    ProjectStorage projectStorage;

    TaskStorage taskStorage;

    @Mock
    ListenerService listenerService;

    @Mock
    TaskHandler taskHandler;
    @Mock
    StatusHandler statusHandler;
    @Mock
    ProjectHandler projectHandler;
    @Mock
    DAOHandler daoHandler;

    @Captor
    ArgumentCaptor<Status> statusCaptor;

    @Captor
    ArgumentCaptor<Task> taskCaptor;

    PortalContainer                 container;

    @Before
    public void setUp() {
        // Make sure the container is started to prevent the ExoTransactional annotation to fail
      container = PortalContainer.getInstance();
      projectStorage = new ProjectStorageImpl(daoHandler);
      taskStorage = container.getComponentInstanceOfType(TaskStorage.class);
        statusStorage = new StatusStorageImpl(daoHandler, taskStorage, projectStorage);
        statusService = new StatusServiceImpl(daoHandler, statusStorage, projectStorage, listenerService);

        containerContext = mockStatic(ExoContainerContext.class);
        containerContext.when(() -> ExoContainerContext.getService(any())).thenAnswer(invocation -> {
          Class<?> clazz = invocation.getArgument(0, Class.class);
          if (clazz.equals(DAOHandler.class)) {
            return daoHandler;
          } else {
            return container.getComponentInstanceOfType(clazz);
          }
        });

        //Mock DAO handler to return Mocked DAO
        when(daoHandler.getTaskHandler()).thenReturn(taskHandler);
        when(daoHandler.getStatusHandler()).thenReturn(statusHandler);
        when(daoHandler.getProjectHandler()).thenReturn(projectHandler);

        //Mock some DAO methods
        when(daoHandler.getStatusHandler().findHighestRankStatusByProject(TestUtils.EXISTING_TASK_ID)).thenReturn(TestUtils.getDefaultStatus());

    }

    @After
    public void tearDown() {
        statusService = null;
        containerContext.close();
        containerContext = null;
    }

    @Test
    public void testCreateDefaultStatusesProject() {
        ProjectDto project = TestDtoUtils.getDefaultProject();
        statusService.createInitialStatuses(project);

        //Default project contains 4 default status so create(status) must be called 4 times
        verify(statusHandler, times(4)).create(any(Status.class));
    }

    @Test
    public void testCreateStatus() {
        ProjectDto project = TestDtoUtils.getDefaultProject();
        Status currStatus = new Status(1L, "oldStatus");
        //project.getStatus().add(currStatus);
        when(statusHandler.findHighestRankStatusByProject(project.getId())).thenReturn(currStatus);

        statusService.createStatus(project, "newStatus");
        //
        verify(statusHandler, times(1)).create(statusCaptor.capture());
        Status result = statusCaptor.getValue();
        int currRank = currStatus.getRank() != null ? currStatus.getRank() : -1;
        assertEquals(currRank + 1, result.getRank().intValue());
        assertEquals("newStatus", result.getName());
        //assertEquals(project, result.getProject());
    }

    @Test
    public void testDeleteLastStatus() throws EntityNotFoundException, NotAllowedOperationOnEntityException, Exception {
        Project project = TestUtils.getDefaultProject();

        Status s1 = TestUtils.getDefaultStatus();
        s1.setProject(project);

        Status s2 = TestUtils.getDefaultStatus();
        s2.setId(2L);
        s2.setProject(project);

        Task t = TestUtils.getDefaultTask();
        t.setStatus(s2);

        Set<String> participator = new HashSet<String>();
        participator.add("Tib");
        ProjectQuery query = new ProjectQuery();
        query.setId(project.getId());
        when(statusHandler.find(s2.getId())).thenReturn(s2);
        when(statusHandler.getStatuses(project.getId())).thenReturn(Arrays.asList(s1, s2)).thenReturn(Arrays.asList(s1));

        statusService.removeStatus(s2.getId());

        verify(statusHandler, times(1)).delete(statusCaptor.capture());
    }

    @Test
    public void testUpdateStatus() throws EntityNotFoundException, NotAllowedOperationOnEntityException {
        Project project = TestUtils.getDefaultProject();
        Status s1 = TestUtils.getDefaultStatus();
        Status s2 = TestUtils.getDefaultStatus();
        Status s3 = TestUtils.getDefaultStatus();
        s2.setId(2L);
        s2.setName("s3");
        s2.setProject(project);
        s3.setId(2L);
        s3.setName("s3");
        s3.setProject(project);
        List<Status> sts=new ArrayList<>();
        sts.add(s1);
        sts.add(s2);
        when(statusHandler.find(s2.getId())).thenReturn(s2);
        when(statusHandler.getStatuses(project.getId())).thenReturn(sts);
        try {
            statusService.updateStatus(s2.getId(), s1.getName());
            Assert.fail("should raise exception for duplicating name");
        } catch (NotAllowedOperationOnEntityException ex) {
        }
        when(daoHandler.getStatusHandler().update(s2)).thenReturn(s2);
        statusService.updateStatus(s2.getId(), "s3");
        verify(statusHandler, times(1)).update(statusCaptor.capture());
        assertEquals(s2.getId(), statusCaptor.getValue().getId());
        assertEquals("s3", statusCaptor.getValue().getName());
    }


    /**
     * A status renamed broadcasts its tasks' ids, read without loading them,
     * so that an index showing its name builds them again (EXO-94917).
     * Mutants: the renaming event left out, or sent with no task id, fail it.
     */
    @Test
    public void testRenameBroadcastsTheStatusTasks() throws Exception {
        Status renamed = projectStatus(2L, "Old");
        when(statusHandler.find(2L)).thenReturn(renamed);
        when(statusHandler.getStatuses(TestUtils.EXISTING_PROJECT_ID)).thenReturn(Arrays.asList(projectStatus(TestUtils.EXISTING_STATUS_ID, "TODO"), renamed));
        when(statusHandler.update(any(Status.class))).thenAnswer(call -> call.getArgument(0));
        when(taskHandler.selectTaskField(any(TaskQuery.class), eq("id"))).thenReturn(new ArrayList<>(List.of(31L, 32L)));

        statusService.updateStatus(2L, " New ");

        verify(listenerService).broadcast(TaskUtil.STATUS_RENAMED, 2L, List.of(31L, 32L));
    }

    /**
     * A status moved, its rank changed and its name kept, trimmed or not, is
     * no renaming: no renaming event (EXO-94917). Mutants: the name check
     * left out, or inverted, fail it.
     */
    @Test
    public void testMoveBroadcastsNoRenaming() throws Exception {
        Status moved = projectStatus(2L, "Old");
        // the update maps the status back onto this entity and reads its project from the project handler
        when(projectHandler.find(TestUtils.EXISTING_PROJECT_ID)).thenReturn(TestUtils.getDefaultProject());
        when(statusHandler.find(2L)).thenReturn(moved);
        when(statusHandler.getStatuses(TestUtils.EXISTING_PROJECT_ID)).thenReturn(Arrays.asList(projectStatus(TestUtils.EXISTING_STATUS_ID, "TODO"), moved));
        when(statusHandler.update(any(Status.class))).thenAnswer(call -> call.getArgument(0));
        when(taskHandler.selectTaskField(any(TaskQuery.class), eq("id"))).thenReturn(new ArrayList<>(List.of(31L)));
        StatusDto move = TestUtils.getDefaultStatusDto();
        move.setId(2L);
        move.setName("Old");
        move.setRank(5);
        move.setProject(TestDtoUtils.getDefaultProject());

        statusService.updateStatus(move);
        statusService.updateStatus(2L, "  Old ");

        verify(listenerService, never()).broadcast(eq(TaskUtil.STATUS_RENAMED), any(), any());
        verify(statusHandler, times(2)).update(any(Status.class));
    }

    /**
     * A status removed broadcasts, after the removal, the ids of the tasks it
     * moved to another status, read before the removal (EXO-94917). Mutants:
     * the event broadcast before the removal, or the ids read after it (none
     * left), fail it.
     */
    @Test
    public void testRemovalBroadcastsTheMovedTasksAfterTheRemoval() throws Exception {
        Status removed = projectStatus(2L, "Doing");
        when(statusHandler.find(2L)).thenReturn(removed);
        when(statusHandler.getStatuses(TestUtils.EXISTING_PROJECT_ID)).thenReturn(Arrays.asList(projectStatus(TestUtils.EXISTING_STATUS_ID, "TODO"), removed));
        AtomicBoolean deleted = new AtomicBoolean();
        doAnswer(call -> {
            deleted.set(true);
            return null;
        }).when(statusHandler).delete(any(Status.class));
        when(taskHandler.selectTaskField(any(TaskQuery.class), eq("id"))).thenAnswer(call -> deleted.get() ? new ArrayList<>() : new ArrayList<>(List.of(33L)));

        statusService.removeStatus(2L);

        InOrder order = inOrder(statusHandler, listenerService);
        order.verify(statusHandler).delete(any(Status.class));
        order.verify(listenerService).broadcast(TaskUtil.STATUS_REMOVED, 2L, List.of(33L));
    }

    /**
     * The renaming, removal and board events are broadcast once their
     * transactional step has returned, that is after its commit when it began
     * the transaction, never from inside it (EXO-94917). Mutant: any of them
     * broadcast from inside its transactional step fails it.
     */
    @Test
    public void testTheStatusEventsFollowTheirTransactionalStep() throws Exception {
        Status status = projectStatus(2L, "Old");
        when(projectHandler.find(TestUtils.EXISTING_PROJECT_ID)).thenReturn(TestUtils.getDefaultProject());
        when(statusHandler.find(2L)).thenReturn(status);
        when(statusHandler.getStatuses(TestUtils.EXISTING_PROJECT_ID)).thenReturn(Arrays.asList(projectStatus(TestUtils.EXISTING_STATUS_ID, "TODO"), status));
        when(statusHandler.update(any(Status.class))).thenAnswer(call -> call.getArgument(0));
        when(taskHandler.selectTaskField(any(TaskQuery.class), eq("id"))).thenReturn(new ArrayList<>(List.of(31L)));
        AtomicBoolean inStep = new AtomicBoolean();
        StatusServiceImpl service = new StatusServiceImpl(daoHandler, statusStorage, projectStorage, listenerService) {
            @Override
            protected List<Long> removeStatusAtomically(long statusID) throws Exception {
                inStep.set(true);
                try {
                    return super.removeStatusAtomically(statusID);
                } finally {
                    inStep.set(false);
                }
            }

            @Override
            protected Update renameStatusAtomically(long id, String name) throws EntityNotFoundException,
                                                                             NotAllowedOperationOnEntityException {
                inStep.set(true);
                try {
                    return super.renameStatusAtomically(id, name);
                } finally {
                    inStep.set(false);
                }
            }
        };
        List<Boolean> broadcastInStep = new ArrayList<>();
        doAnswer(call -> broadcastInStep.add(inStep.get())).when(listenerService).broadcast(eq(TaskUtil.STATUS_RENAMED), any(), any());
        doAnswer(call -> broadcastInStep.add(inStep.get())).when(listenerService).broadcast(eq(TaskUtil.STATUS_REMOVED), any(), any());
        doAnswer(call -> broadcastInStep.add(inStep.get())).when(listenerService).broadcast(eq("exo.project.projectModified"), any(), any());

        service.updateStatus(2L, "New");
        service.removeStatus(2L);

        assertEquals(List.of(false, false, false), broadcastInStep, "the board's, the renaming's and the removal's events");
    }

    /**
     * The removal of a project's last status is refused and broadcasts nothing
     * (EXO-94917). Mutant: the event broadcast before the removal fails it.
     */
    @Test
    public void testRefusedRemovalBroadcastsNothing() throws Exception {
        Status last = projectStatus(2L, "Only");
        when(statusHandler.find(2L)).thenReturn(last);
        when(statusHandler.getStatuses(TestUtils.EXISTING_PROJECT_ID)).thenReturn(Arrays.asList(last));
        when(taskHandler.selectTaskField(any(TaskQuery.class), eq("id"))).thenReturn(new ArrayList<>(List.of(34L)));

        Assert.assertThrows(NotAllowedOperationOnEntityException.class, () -> statusService.removeStatus(2L));

        verify(listenerService, never()).broadcast(eq(TaskUtil.STATUS_REMOVED), any(), any());
    }

    /**
     * A listener failing on a status event never fails the renaming nor the
     * removal (EXO-94917). Mutant: the try/catch left out fails it.
     */
    @Test
    public void testAFailingListenerLeavesTheStatusChangeDone() throws Exception {
        Status status = projectStatus(2L, "Old");
        // the update maps the status back onto this entity and reads its project from the project handler
        when(projectHandler.find(TestUtils.EXISTING_PROJECT_ID)).thenReturn(TestUtils.getDefaultProject());
        when(statusHandler.find(2L)).thenReturn(status);
        when(statusHandler.getStatuses(TestUtils.EXISTING_PROJECT_ID)).thenReturn(Arrays.asList(projectStatus(TestUtils.EXISTING_STATUS_ID, "TODO"), status));
        when(statusHandler.update(any(Status.class))).thenAnswer(call -> call.getArgument(0));
        doThrow(new IllegalStateException("listener down")).when(listenerService).broadcast(eq(TaskUtil.STATUS_RENAMED), any(), any());
        doThrow(new IllegalStateException("listener down")).when(listenerService).broadcast(eq(TaskUtil.STATUS_REMOVED), any(), any());

        StatusDto renamed = statusService.updateStatus(2L, "New");
        statusService.removeStatus(2L);

        assertEquals("New", renamed.getName());
        verify(statusHandler).delete(any(Status.class));
    }

    /**
     * @param id a status id
     * @param name its name
     * @return a status of the default project
     */
    private static Status projectStatus(long id, String name) {
        Status status = TestUtils.getDefaultStatus();
        status.setId(id);
        status.setName(name);
        status.setProject(TestUtils.getDefaultProject());
        return status;
    }

}
