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
package org.exoplatform.task.service.impl;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.NotAllowedOperationOnEntityException;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.storage.StatusStorage;
import org.exoplatform.task.util.StorageUtil;
import org.exoplatform.task.util.TaskUtil;

import io.meeds.common.ContainerTransactional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


@Singleton
public class StatusServiceImpl implements StatusService {

    @Inject
    private DAOHandler daoHandler;

    @Inject
    private StatusStorage statusStorage;

    @Inject
    private ProjectStorage projectStorage;

    private ListenerService listenerService;

    private String[] DEFAULT_STATUS = {"ToDo", "InProgress", "WaitingOn", "Done"};

    private static Log LOG = ExoLogger.getExoLogger(StatusServiceImpl.class);

    public StatusServiceImpl(DAOHandler daoHandler, StatusStorage statusStorage, ProjectStorage projectStorage, ListenerService listenerService) {
        this.daoHandler = daoHandler;
        this.statusStorage = statusStorage;
        this.projectStorage = projectStorage;
        this.listenerService = listenerService;
    }

    public StatusServiceImpl(DAOHandler daoHandler) {
        String status = PropertyManager.getProperty("exo.tasks.default.workflow");
        if (status != null) {
            List<String> stList = new LinkedList<String>();
            for (String s : status.split(",")) {
                s = s.trim();
                if (!s.isEmpty()) {
                    stList.add(s);
                }
            }

            if (!stList.isEmpty()) {
                DEFAULT_STATUS = stList.toArray(new String[stList.size()]);
            }
        }
        this.daoHandler = daoHandler;
    }

    private List<String> getDefaultStatus() {
        return Arrays.asList(DEFAULT_STATUS);
    }

    @Override
    @ExoTransactional
    public void createInitialStatuses(ProjectDto proj) {
        for (String s : getDefaultStatus()) {
            createStatus(proj, s);
        }
    }

    @Override
    public StatusDto getStatus(long statusId) {
        return statusStorage.getStatus(statusId);
    }

    @Override
    public StatusDto getDefaultStatus(long projectId) {
        return statusStorage.getDefaultStatus(projectId);
    }

    @Override
    public List<StatusDto> getStatuses(long projectId) {
        return statusStorage.getStatuses(projectId);
    }

    @Override
    @ExoTransactional
    public StatusDto createStatus(ProjectDto project, String name) {
        if (name == null || (name = name.trim()).isEmpty() || project == null) {
            throw new IllegalArgumentException("project must be not null and status must not be null or empty");
        }
        StatusDto status = statusStorage.createStatus(project, name);
        try {
            listenerService.broadcast("exo.project.projectModified", null, project);
        } catch (Exception e) {
            LOG.error("Error while broadcasting status creation event", e);
        }
        return status;
    }

    @Override
    @ExoTransactional
    public StatusDto createStatus(ProjectDto project, String name, int rank) throws  NotAllowedOperationOnEntityException {
        if (name == null || (name = name.trim()).isEmpty() || project == null) {
            throw new IllegalArgumentException("project must be not null and status must not be null or empty");
        }
        try {
            listenerService.broadcast("exo.project.projectModified", null, project);
        } catch (Exception e) {
            LOG.error("Error while broadcasting status creation event", e);
        }
        return statusStorage.createStatus(project, name, rank);
    }

    /**
     * Removes a status, its tasks moved to another status of its project, then,
     * once the removal's transaction has committed, broadcasts
     * {@link TaskUtil#STATUS_REMOVED} with the ids of the tasks it moved, read
     * before the removal (EXO-94917). A removal refused, the project's last
     * status, broadcasts nothing. Called inside a transaction it did not begin,
     * the event goes out before that outer commit.
     *
     * @param statusID the status id
     * @throws Exception when the status does not exist or is the last one
     */
    @Override
    public void removeStatus(long statusID) throws Exception {
        List<Long> movedTaskIds = removeStatusAtomically(statusID);
        broadcastStatusEvent(TaskUtil.STATUS_REMOVED, statusID, movedTaskIds);
    }

    /**
     * One transaction for the removal, its event left to the caller, which
     * broadcasts it once this method returns.
     *
     * @param statusID the status id
     * @return the ids of the tasks the removal moved, read before it
     * @throws Exception when the status does not exist or is the last one
     */
    @ContainerTransactional(atomic = true)
    protected List<Long> removeStatusAtomically(long statusID) throws Exception {
        List<Long> movedTaskIds = statusTaskIds(statusID);
        statusStorage.removeStatus(statusID, false);
        return movedTaskIds;
    }

    /**
     * Renames a status; when its name changed, broadcasts
     * {@link TaskUtil#STATUS_RENAMED} with the ids of its tasks once the
     * renaming's transaction has committed (EXO-94917).
     *
     * @param id the status id
     * @param name its new name, trimmed
     * @return the status
     * @throws EntityNotFoundException when the status does not exist
     * @throws NotAllowedOperationOnEntityException when another status of its
     *           project has that name
     */
    @Override
    public StatusDto updateStatus(long id, String name) throws EntityNotFoundException, NotAllowedOperationOnEntityException {
        if (name == null || (name = name.trim()).isEmpty()) {
            throw new IllegalArgumentException("status name can't be null or empty");
        }
        Update update = renameStatusAtomically(id, name);
        broadcastProjectModified(null, update.status());
        broadcastRenamed(id, update);
        return update.status();
    }

    /**
     * One transaction for the renaming, {@link #updateStatus(long, String)}'s
     * event left to the caller.
     *
     * @param id the status id
     * @param name its new name, trimmed
     * @return the status and, when its name changed, the ids of its tasks
     * @throws EntityNotFoundException when the status does not exist
     * @throws NotAllowedOperationOnEntityException when another status of its
     *           project has that name
     */
    @ContainerTransactional(atomic = true)
    protected Update renameStatusAtomically(long id, String name) throws EntityNotFoundException,
                                                                    NotAllowedOperationOnEntityException {
        String previousName = storedName(id);
        StatusDto statusDto = statusStorage.updateStatus(id, name);
        return new Update(statusDto, renamedTaskIds(id, previousName, name));
    }

    /**
     * Updates a status, its name or its rank; when its name changed, a move
     * changing its rank alone not being a renaming, broadcasts
     * {@link TaskUtil#STATUS_RENAMED} with the ids of its tasks once the
     * update's transaction has committed (EXO-94917).
     *
     * @param statusDto the status, its new name and rank
     * @return the status
     * @throws EntityNotFoundException when the status does not exist
     * @throws NotAllowedOperationOnEntityException when another status of its
     *           project has that name
     */
    @Override
    public StatusDto updateStatus(StatusDto statusDto) throws EntityNotFoundException, NotAllowedOperationOnEntityException {
        String name = statusDto.getName();
        if (name == null || (name = name.trim()).isEmpty()) {
            throw new IllegalArgumentException("status name can't be null or empty");
        }
        Update update = updateStatusAtomically(statusDto, name);
        broadcastProjectModified(this, update.status());
        broadcastRenamed(statusDto.getId(), update);
        return update.status();
    }

    /**
     * One transaction for the update, {@link #updateStatus(StatusDto)}'s event
     * left to the caller.
     *
     * @param statusDto the status, its new name and rank
     * @param name its new name, trimmed
     * @return the status and, when its name changed, the ids of its tasks
     * @throws EntityNotFoundException when the status does not exist
     * @throws NotAllowedOperationOnEntityException when another status of its
     *           project has that name
     */
    @ContainerTransactional(atomic = true)
    protected Update updateStatusAtomically(StatusDto statusDto, String name) throws EntityNotFoundException,
                                                                                NotAllowedOperationOnEntityException {
        long statusId = statusDto.getId();
        String previousName = storedName(statusId);
        StatusDto updated = statusStorage.updateStatus(statusDto);
        return new Update(updated, renamedTaskIds(statusId, previousName, name));
    }

    /**
     * A status updated and, when it was renamed, the ids of its tasks.
     *
     * @param status the status
     * @param renamedTaskIds the ids of its tasks, or null when its name is the
     *          one it had
     */
    protected record Update(StatusDto status, List<Long> renamedTaskIds) {
    }

    /**
     * Broadcasts {@code exo.project.projectModified} with the status's board,
     * once the update's transaction has returned, as
     * {@code TaskServiceImpl#updateTask} does. Its listener saves that board
     * whole (pitfall project-modified-event-saves-the-board-it-carries-whole):
     * the board is the one this request loaded with the status, so a board
     * edit another request committed after that load is undone. A read of
     * the board here would not close it: it answers the entity this
     * request's persistence context already holds.
     *
     * @param source the event's source
     * @param status the status updated
     */
    private void broadcastProjectModified(Object source, StatusDto status) {
        try {
            listenerService.broadcast("exo.project.projectModified", source, status.getProject());
        } catch (Exception e) {
            LOG.error("Error while broadcasting status update event", e);
        }
    }

    /**
     * Broadcasts {@link TaskUtil#STATUS_RENAMED} when the update renamed the
     * status.
     *
     * @param statusId the status id
     * @param update the update's result
     */
    private void broadcastRenamed(long statusId, Update update) {
        if (update.renamedTaskIds() != null) {
            broadcastStatusEvent(TaskUtil.STATUS_RENAMED, statusId, update.renamedTaskIds());
        }
    }

    /**
     * @param statusId a status id
     * @return its stored name, or null when it does not exist
     */
    private String storedName(long statusId) {
        StatusDto stored = statusStorage.getStatus(statusId);
        return stored == null ? null : stored.getName();
    }

    /**
     * @param statusId the status id
     * @param previousName its name before the update, or null
     * @param name its name now, trimmed
     * @return the ids of its tasks when its name, trimmed, is not the one it
     *         had, else null
     */
    private List<Long> renamedTaskIds(long statusId, String previousName, String name) {
        return previousName == null || !previousName.trim().equals(name) ? statusTaskIds(statusId) : null;
    }

    /**
     * @param statusId a status id
     * @return the ids of its tasks, read without loading them
     */
    private List<Long> statusTaskIds(long statusId) {
        TaskQuery query = new TaskQuery();
        query.setStatusId(statusId);
        List<Long> ids = daoHandler.getTaskHandler().<Long> selectTaskField(query, "id");
        return ids == null ? List.of() : List.copyOf(ids);
    }

    /**
     * Broadcasts a status event; a listener's failure is logged and never
     * fails the renaming or the removal.
     *
     * @param eventName {@link TaskUtil#STATUS_RENAMED} or
     *          {@link TaskUtil#STATUS_REMOVED}
     * @param statusId the status id, the event's source
     * @param taskIds the ids of its tasks, the event's data
     */
    private void broadcastStatusEvent(String eventName, long statusId, List<Long> taskIds) {
        if (listenerService == null) {
            return;
        }
        try {
            listenerService.broadcast(eventName, Long.valueOf(statusId), taskIds);
        } catch (Exception e) {
            LOG.warn("Error while broadcasting {} for status {}", eventName, statusId, e);
        }
    }

}
