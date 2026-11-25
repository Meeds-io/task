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

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.LabelHandler;
import org.exoplatform.task.dao.LabelQuery;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.domain.Label;

public class LabelDAOImpl extends CommonJPADAO<Label, Long> implements LabelHandler {

  @Override
  @ExoTransactional
  public void delete(Label entity) {
    Query query = getEntityManager().createNamedQuery("LabelTaskMapping.removeLabelTaskMapping");
    query.setParameter("labelId", entity.getId());
    query.executeUpdate();
    List<Label> children = entity.getChildren();
    if (children != null) {
      for (Label child : children) {
        delete(child);
      }
    }
    super.delete(entity);
  }

  @Override
  public void deleteAll(List<Label> entities) {
    for (Label lb : entities) {
      delete(lb);
    }
  }

  @Override
  public void deleteAll() {
    deleteAll(findAll());
  }

  @Override
  public ListAccess<Label> findLabelsByUser(String username) {
    LabelQuery query = new LabelQuery();
    query.setUserName(username);
    // A stable order is required so that consecutive pages never overlap or skip labels
    query.setOrderBy(Collections.singletonList(new OrderBy.ASC("id")));
    return findLabels(query);
  }

  @Override
  public ListAccess<Label> findLabelsByProject(long projectId) {
    TypedQuery<Label> query = getEntityManager().createNamedQuery("Label.findLabelsByProject", Label.class);
    query.setParameter("projectId", projectId);

    TypedQuery<Long> count = getEntityManager().createNamedQuery("Label.findLabelsByProjectCount", Long.class);
    count.setParameter("projectId", projectId);
    return new JPAQueryListAccess<Label>(Label.class, count, query);
  }

  @Override
  public ListAccess<Label> findLabelsByUser(String username, String keyword) {
    if (StringUtils.isBlank(keyword)) {
      return findLabelsByUser(username);
    }
    TypedQuery<Label> query = getEntityManager().createNamedQuery("Label.searchLabelsByUser", Label.class);
    query.setParameter("username", username);
    query.setParameter("keyword", toLikePattern(keyword));
    TypedQuery<Long> count = getEntityManager().createNamedQuery("Label.searchLabelsByUserCount", Long.class);
    count.setParameter("username", username);
    count.setParameter("keyword", toLikePattern(keyword));
    return new JPAQueryListAccess<Label>(Label.class, count, query);
  }

  @Override
  public ListAccess<Label> findLabelsByProject(long projectId, String keyword) {
    if (StringUtils.isBlank(keyword)) {
      return findLabelsByProject(projectId);
    }
    TypedQuery<Label> query = getEntityManager().createNamedQuery("Label.searchLabelsByProject", Label.class);
    query.setParameter("projectId", projectId);
    query.setParameter("keyword", toLikePattern(keyword));
    TypedQuery<Long> count = getEntityManager().createNamedQuery("Label.searchLabelsByProjectCount", Long.class);
    count.setParameter("projectId", projectId);
    count.setParameter("keyword", toLikePattern(keyword));
    return new JPAQueryListAccess<Label>(Label.class, count, query);
  }

  /** The pattern of the names containing the keyword, with its wildcards taken literally. */
  private static String toLikePattern(String keyword) {
    return "%" + keyword.trim()
                        .toLowerCase()
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_") + "%";
  }

  @Override
  public ListAccess<Label> findLabelsByTask(long taskId, long projectId) {
    TypedQuery<Label> query = getEntityManager().createNamedQuery("Label.findLabelsByTask", Label.class);
    query.setParameter("projectId", projectId);
    query.setParameter("taskid", taskId);

    TypedQuery<Long> count = getEntityManager().createNamedQuery("Label.findLabelsByTaskCount", Long.class);
    count.setParameter("projectId", projectId);
    count.setParameter("taskid", taskId);
    return new JPAQueryListAccess<Label>(Label.class, count, query);
  }

  @Override
  public ListAccess<Label> findLabels(LabelQuery query) {
    return findEntities(query, Label.class);
  }
  
}

