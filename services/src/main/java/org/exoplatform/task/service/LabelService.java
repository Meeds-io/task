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
package org.exoplatform.task.service;

import java.util.List;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.domain.Label;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;

public interface LabelService {
  List<LabelDto> findLabelsByUser(String username, int offset, int limit);

  List<LabelDto> findLabelsByProject(long projectId, Identity currentUser, int offset, int limit);

  List<LabelDto> findLabelsByTask(TaskDto task, long projectId, Identity currentUser, int offset, int limit);

  LabelDto getLabel(long labelId);

  LabelDto createLabel(LabelDto label);

  @ExoTransactional
  LabelDto updateLabel(LabelDto label, List<Label.FIELDS> fields) throws EntityNotFoundException;

  LabelDto updateLabel(LabelDto label) throws EntityNotFoundException;

  void removeLabel(long labelId);

  void addTaskToLabel(TaskDto task, Long labelId) throws EntityNotFoundException;

  void removeTaskFromLabel(TaskDto task, Long labelId) throws EntityNotFoundException;
}
