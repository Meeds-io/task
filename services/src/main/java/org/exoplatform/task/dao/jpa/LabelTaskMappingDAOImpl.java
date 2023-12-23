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
package org.exoplatform.task.dao.jpa;

import java.io.Serializable;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.dao.LabelTaskMappingHandler;
import org.exoplatform.task.domain.Label;
import org.exoplatform.task.domain.LabelTaskMapping;
import org.exoplatform.task.domain.Task;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

public class LabelTaskMappingDAOImpl extends CommonJPADAO<LabelTaskMapping, Serializable> implements LabelTaskMappingHandler {

    private static final Log log = ExoLogger.getExoLogger(LabelTaskMappingDAOImpl.class);

    @Override
    public LabelTaskMapping findLabelTaskMapping(long labelId, long taskId) {
        TypedQuery<LabelTaskMapping> query = getEntityManager().createNamedQuery("LabelTaskMapping.findLabelMapping", LabelTaskMapping.class);
        query.setParameter("labelId", labelId);
        query.setParameter("taskId", taskId);
        try {
            return cloneEntity((LabelTaskMapping)query.getSingleResult());
        } catch (PersistenceException e) {
            log.error("Error when fetching label mapping", e);
            return null;
        }
    }
}

