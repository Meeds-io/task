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
package org.exoplatform.task.storage.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.domain.Label;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.storage.LabelStorage;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.util.StorageUtil;

import io.meeds.task.domain.LabelField;

public class LabelStorageImpl implements LabelStorage {

  private static final Log     LOG     = ExoLogger.getExoLogger(LabelStorageImpl.class);

  private static final Pattern pattern = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");

  @Inject
  private final DAOHandler     daoHandler;

  public LabelStorageImpl(DAOHandler daoHandler) {
    this.daoHandler = daoHandler;
  }

  @Override
  public List<LabelDto> findLabelsByUser(String username, int offset, int limit) {
    try {
      return Arrays.asList(daoHandler.getLabelHandler().findLabelsByUser(username).load(offset, limit))
                   .stream()
                   .map(StorageUtil::labelToDto)
                   .collect(Collectors.toList());
    } catch (Exception e) {
      return new ArrayList<LabelDto>();
    }
  }

  @Override
  public List<LabelDto> findLabelsByUser(String username, String keyword, int offset, int limit) {
    try {
      return Arrays.stream(daoHandler.getLabelHandler().findLabelsByUser(username, keyword).load(offset, limit))
                   .map(StorageUtil::labelToDto)
                   .toList();
    } catch (Exception e) {
      LOG.warn("Error searching labels of user {}", username, e);
      return new ArrayList<>();
    }
  }

  @Override
  public List<LabelDto> findLabelsByProject(long projectId,
                                            Identity currentUser,
                                            ProjectStorage projectStorage,
                                            String keyword,
                                            int offset,
                                            int limit) {
    try {
      return Arrays.stream(daoHandler.getLabelHandler().findLabelsByProject(projectId, keyword).load(offset, limit))
                   .map((Label label) -> StorageUtil.labelToDto(label, currentUser, projectStorage))
                   .toList();
    } catch (Exception e) {
      LOG.warn("Error searching labels of project {}", projectId, e);
      return new ArrayList<>();
    }
  }

  @Override
  public int countLabelsByUser(String username, String keyword) {
    try {
      return daoHandler.getLabelHandler().findLabelsByUser(username, keyword).getSize();
    } catch (Exception e) {
      LOG.warn("Error counting labels of user {}", username, e);
      return 0;
    }
  }

  @Override
  public int countLabelsByProject(long projectId, String keyword) {
    try {
      return daoHandler.getLabelHandler().findLabelsByProject(projectId, keyword).getSize();
    } catch (Exception e) {
      LOG.warn("Error counting labels of project {}", projectId, e);
      return 0;
    }
  }

  @Override
  public int countLabelsByUser(String username) {
    try {
      return daoHandler.getLabelHandler().findLabelsByUser(username).getSize();
    } catch (Exception e) {
      LOG.warn("Error counting labels of user {}", username, e);
      return 0;
    }
  }

  @Override
  public int countLabelsByProject(long projectId) {
    try {
      return daoHandler.getLabelHandler().findLabelsByProject(projectId).getSize();
    } catch (Exception e) {
      LOG.warn("Error counting labels of project {}", projectId, e);
      return 0;
    }
  }

  @Override
  public List<LabelDto> findLabelsByProject(long projectId, Identity currentUser, ProjectStorage projectStorage, int offset, int limit) {
    try {
      return Arrays.asList(daoHandler.getLabelHandler().findLabelsByProject(projectId).load(offset, limit))
                   .stream()
                   .map((Label label) -> StorageUtil.labelToDto(label,currentUser, projectStorage))
                   .collect(Collectors.toList());
    } catch (Exception e) {
      return new ArrayList<LabelDto>();
    }
  }
  @Override
  public List<LabelDto> findLabelsByTask(TaskDto task, long projectId, Identity currentUser, ProjectStorage projectStorage, int offset, int limit) {
    try {
      return Arrays.asList(daoHandler.getLabelHandler().findLabelsByTask(task.getId(), projectId).load(offset, limit))
                   .stream()
                   .map((Label label) -> StorageUtil.labelToDto(label,task,currentUser,projectStorage))
                   .collect(Collectors.toList());
    } catch (Exception e) {
      return new ArrayList<LabelDto>();
    }
  }

  @Override
  public LabelDto getLabel(long labelId) {
    return StorageUtil.labelToDto(daoHandler.getLabelHandler().find(labelId));
  }

  @Override
  public LabelDto createLabel(LabelDto label) {
    return StorageUtil.labelToDto(daoHandler.getLabelHandler().create(StorageUtil.labelToEntity(label)));
  }

  @Override
  public LabelDto updateLabel(LabelDto label, List<LabelField> fields) throws EntityNotFoundException {
    return null;
  }

  @Override
  public void removeLabel(long labelId) {
    daoHandler.getLabelHandler().delete(StorageUtil.labelToEntity(getLabel(labelId)));
  }

  @Override
  public void addTaskToLabel(TaskDto task, Long labelId) throws EntityNotFoundException {
  }

  @Override
  public void removeTaskFromLabel(TaskDto task, Long labelId) throws EntityNotFoundException {

  }

}
