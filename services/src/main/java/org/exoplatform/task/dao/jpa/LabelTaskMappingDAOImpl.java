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
package org.exoplatform.task.dao.jpa;

import java.io.Serializable;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.dao.LabelTaskMappingHandler;
import org.exoplatform.task.domain.LabelTaskMapping;

import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

public class LabelTaskMappingDAOImpl extends CommonJPADAO<LabelTaskMapping, Serializable> implements LabelTaskMappingHandler {

  private static final String LABEL_ID = "labelId";

  private static final String TASK_ID  = "taskId";

  private static final Log    log      = ExoLogger.getExoLogger(LabelTaskMappingDAOImpl.class);

  @Override
  public ListAccess<LabelTaskMapping> findLabelMappings(long taskId) {
    TypedQuery<LabelTaskMapping> query = getEntityManager().createNamedQuery("LabelTaskMapping.findLabelMappingsOfTask", LabelTaskMapping.class);
    TypedQuery<Long> count = getEntityManager().createNamedQuery("LabelTaskMapping.countLabelMappingsOfTask", Long.class);
    query.setParameter(TASK_ID, taskId);
    count.setParameter(TASK_ID, taskId);
    return new JPAQueryListAccess<>(LabelTaskMapping.class, count, query);
  }

  @Override
  public LabelTaskMapping findLabelTaskMapping(long labelId, long taskId) {
    TypedQuery<LabelTaskMapping> query = getEntityManager().createNamedQuery("LabelTaskMapping.findLabelMapping",
                                                                             LabelTaskMapping.class);
    query.setParameter(LABEL_ID, labelId);
    query.setParameter(TASK_ID, taskId);
    try {
      return cloneEntity(query.getSingleResult());
    } catch (NoResultException e) {
      return null;
    } catch (PersistenceException e) {
      log.warn("Error when fetching label mapping. Return null Task label", e);
      return null;
    }
  }

}
