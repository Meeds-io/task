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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.task.dao.StatusHandler;
import org.exoplatform.task.domain.Status;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 4/10/15
 */
public class StatusDAOImpl extends CommonJPADAO<Status, Long> implements StatusHandler {

  public StatusDAOImpl() {
  }

  @Override
  public Status findLowestRankStatusByProject(Long projectId) {
    EntityManager em = getEntityManager();
    Query query = em.createNamedQuery("Status.findLowestRankStatusByProject", Status.class);
    query.setParameter("projectId", projectId);
    return cloneEntity((Status)query.getSingleResult());
  }
  
  @Override
  public Status findHighestRankStatusByProject(long projectId) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("projectId", projectId);
    List<Status> sts = findByNamedQuery("Status.findHighestRankStatusByProject", params, 1);
    if (!sts.isEmpty()) {
      return sts.get(0);
    } else {
      return null;
    }
  }
  
  @Override
  public Status findByName(String name, long projectID) {
    if (!StringUtils.isNotEmpty(name)) {
      return null;
    }
    
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("name", name);
    params.put("projectID", projectID);
    List<Status> sts = findByNamedQuery("Status.findByName", params, 1);
    if (!sts.isEmpty()) {
      return sts.get(0);
    } else {
      return null;
    }
  }

  @Override
  public List<Status> getStatuses(long projectId) {
    Map<String, Object> param = new HashMap<String, Object>();
    param.put("projectId", projectId);
    return findByNamedQuery("Status.findStatusByProject", param, -1);
  }


  public List<Status> findByNamedQuery(String query, Map<String, Object> params, int limit) {
    EntityManager em = getEntityManager();
    TypedQuery<Status> q = em.createNamedQuery(query, Status.class);
    if (params != null) {
      for (Map.Entry<String, Object> p : params.entrySet()) {
        q.setParameter(p.getKey(), p.getValue());      
      }      
    }
    if (limit > 0) {
      q.setMaxResults(limit);
    }
    return cloneEntities(q.getResultList());
  }
}

