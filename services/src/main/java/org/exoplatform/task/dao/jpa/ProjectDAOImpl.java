/* 
* Copyright (C) 2003-2015 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.task.dao.jpa;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.ProjectHandler;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.dao.condition.Conditions;
import org.exoplatform.task.dao.condition.SingleCondition;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Task;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 4/10/15
 */
public class ProjectDAOImpl extends CommonJPADAO<Project, Long> implements ProjectHandler {

  private static final Logger LOG = Logger.getLogger("ProjectDAOImpl");

  public ProjectDAOImpl() {
  }

  @Override
  public Project update(Project entity) {
    return cloneEntity(super.update(entity));
  }

  @Override
  public void delete(Project entity) {
    Project p = getEntityManager().find(Project.class, entity.getId());
    if (p != null) {           
      super.delete(p);
    }
  }

  @Override
  public Project removeProject(long projectId, boolean deleteChild) {
    Project p = getEntityManager().find(Project.class, projectId);
    if (p == null) {
      return null;
    }
    if (!deleteChild && p.getChildren() != null) {
      for(Project pj : p.getChildren()) {
        pj.setParent(p.getParent());
        getEntityManager().persist(pj);
      }
      p.getChildren().clear();
    }

    super.delete(p);
    return p;
  }

  @Override
  public ListAccess<Project> findSubProjects(Project project) {
    ProjectQuery query = new ProjectQuery();
    if (project != null) {
      query.setParent(project.getId());
    } else {
      query.setParent(null);
    }
    return findProjects(query);
  }

  @Override
  public ListAccess<Project> findAllByMembershipsAndKeyword(List<String> memberships, String keyword, OrderBy order) {
    ProjectQuery query = new ProjectQuery();
    query.setMembership(memberships);
    query.setKeyword(keyword);
    if (order != null) {
      query.setOrderBy(Arrays.asList(order));      
    }
    return findProjects(query);
  }

  @Override
  public ListAccess<Project> findProjects(ProjectQuery query) {
    return findEntities(query, Project.class);
  }
  
  protected Path buildPath(SingleCondition condition, Root<Project> root) {
    String field = condition.getField();
    
    if (Conditions.MANAGER.equals(field) || Conditions.PARTICIPATOR.equals(field)) {
      return root.join(field, JoinType.LEFT);     
    }
    return super.buildPath(condition, root);
  }
  
  public <T> List<T> selectProjectField(ProjectQuery query, String fieldName) {
    EntityManager em = getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery q = cb.createQuery();

    Root<Project> project = q.from(Project.class);
    Predicate predicate = this.buildQuery(query.getCondition(), project, cb, q);

    if(predicate != null) {
      q.where(predicate);
    }
    q.select(project.join(fieldName)).distinct(true);    

    final TypedQuery<T> selectQuery = em.createQuery(q);
    return cloneEntities(selectQuery.getResultList());
  }
}
