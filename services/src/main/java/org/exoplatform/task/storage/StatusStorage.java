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
package org.exoplatform.task.storage;

import java.util.List;

import org.exoplatform.task.domain.Status;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.NotAllowedOperationOnEntityException;

public interface StatusStorage {

  /**
   * Return the <code>Status</code> with given <code>statusId</code>.
   *
   * @param statusId the given status id.
   * @return the status of the given statusId.
   */
  StatusDto getStatus(long statusId);

  /**
   * Return the default status of the project which is ideally the first step in
   * the project workflow.
   *
   * @param projectId the given project id.
   * @return the default status of the given project.
   */
  StatusDto getDefaultStatus(long projectId);

  /**
   * Return the list of statuses from a project with given <code>projectId</code>.
   *
   * @param projectId the given project id.
   * @return the status of the given project.
   */
  List<StatusDto> getStatuses(long projectId);

  StatusDto createStatus(ProjectDto project, String status);

  StatusDto createStatus(ProjectDto project, String status, int rank) throws NotAllowedOperationOnEntityException;

  void removeStatus(long statusId) throws Exception;

  StatusDto updateStatus(long statusId, String statusName) throws EntityNotFoundException, NotAllowedOperationOnEntityException;

  StatusDto updateStatus(StatusDto statusDto) throws EntityNotFoundException,
                                                                  NotAllowedOperationOnEntityException;


}
