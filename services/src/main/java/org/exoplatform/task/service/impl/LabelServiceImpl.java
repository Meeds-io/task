/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.service.impl;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.domain.Label;
import org.exoplatform.task.domain.LabelTaskMapping;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.LabelService;
import org.exoplatform.task.storage.LabelStorage;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.util.StorageUtil;

import javax.inject.Inject;
import java.util.List;

public class LabelServiceImpl implements LabelService {

    private static final Log LOG = ExoLogger.getExoLogger(LabelServiceImpl.class);

    String                 LABEL_ADDED_TO_TASK     = "exo.task.labelAddedToTask";

    String                 LABEL_DELETED_FROM_TASK = "exo.task.labelDeletedFromTask";

    @Inject
    private DAOHandler daoHandler;

    @Inject
    private LabelStorage labelStorage;

    @Inject
    private ProjectStorage projectStorage;

    private ListenerService listenerService;

    public LabelServiceImpl(LabelStorage labelStorage,  DAOHandler daoHandler, ProjectStorage projectStorage, ListenerService listenerService) {
        this.labelStorage = labelStorage;
        this.daoHandler = daoHandler;
        this.projectStorage = projectStorage;
        this.listenerService = listenerService;
    }


    @Override
    public List<LabelDto> findLabelsByUser(String username, int offset, int limit) {
        return labelStorage.findLabelsByUser(username,offset,limit);
    }

    @Override
    public List<LabelDto> findLabelsByProject(long projectId, Identity currentUser, int offset, int limit) {
        return labelStorage.findLabelsByProject(projectId,  currentUser,projectStorage, offset,limit);
    }

    @Override
    public List<LabelDto> findLabelsByTask(TaskDto task, long projectId, Identity currentUser, int offset, int limit) {
        return labelStorage.findLabelsByTask(task, projectId,currentUser,projectStorage,offset,limit);
    }

    @Override
    public LabelDto getLabel(long labelId) {
        return StorageUtil.labelToDto(daoHandler.getLabelHandler().find(labelId));
    }

    @Override
    @ExoTransactional
    public LabelDto createLabel(LabelDto label) {
        return labelStorage.createLabel(label);
    }

    @Override
    @ExoTransactional
    public LabelDto updateLabel(LabelDto label, List<Label.FIELDS> fields) throws EntityNotFoundException {
        LabelDto lb = getLabel(label.getId());
        if (lb == null) {
            throw new EntityNotFoundException(label.getId(), LabelDto.class);
        }

        //Todo: validate input and throw exception if need
        for (Label.FIELDS field : fields) {
            switch (field) {
                case NAME:
                    lb.setName(label.getName());
                    break;
                case COLOR:
                    lb.setColor(label.getColor());
                    break;
                case PARENT:
                    lb.setParent(label.getParent());
                    break;
                case HIDDEN:
                    lb.setHidden(label.isHidden());
            }
        }
        return StorageUtil.labelToDto(daoHandler.getLabelHandler().update(StorageUtil.mappingLabelToEntity(lb)));
    }


    @Override
    @ExoTransactional
    public LabelDto updateLabel(LabelDto label) throws EntityNotFoundException {
        LabelDto lb = getLabel(label.getId());
        if (lb == null) {
            throw new EntityNotFoundException(label.getId(), LabelDto.class);
        }
        lb.setName(label.getName());
        lb.setColor(label.getColor());
        lb.setHidden(label.isHidden());
        return StorageUtil.labelToDto(daoHandler.getLabelHandler().update(StorageUtil.labelToEntity(lb)));
    }

    @Override
    @ExoTransactional
    public void removeLabel(long labelId) {
        daoHandler.getLabelHandler().delete(StorageUtil.labelToEntity(getLabel(labelId)));
    }


    @Override
    @ExoTransactional
    public void addTaskToLabel(TaskDto task, Long labelId) throws EntityNotFoundException {
        LabelTaskMapping mapping = new LabelTaskMapping();
        mapping.setLabel(StorageUtil.mappingLabelToEntity(getLabel(labelId)));
        mapping.setTask(StorageUtil.taskToEntity(task));
        daoHandler.getLabelTaskMappingHandler().create(mapping);
        broadcastEvent(LABEL_ADDED_TO_TASK, mapping);
    }

    @Override
    @ExoTransactional
    public void removeTaskFromLabel(TaskDto task, Long labelId) throws EntityNotFoundException {
      LabelTaskMapping mapping = daoHandler.getLabelTaskMappingHandler().findLabelTaskMapping(labelId, task.getId());
      daoHandler.getLabelTaskMappingHandler().delete(mapping);
      broadcastEvent(LABEL_DELETED_FROM_TASK, mapping);
    }
    
    private void broadcastEvent(String eventName, LabelTaskMapping data) {
      ConversationState conversationState = ConversationState.getCurrent();
      String currentUserName = conversationState.getIdentity().getUserId();
      try {
        if (data != null) {
          listenerService.broadcast(eventName, currentUserName, data);
        }
      } catch (Exception e) {
        LOG.warn("Error while broadcasting event {}", eventName, e);
      }
    }
}
