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

import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.core.jpa.storage.entity.IdentityEntity;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.ProjectHandler;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.dao.condition.Conditions;
import org.exoplatform.task.dao.condition.SingleCondition;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.UserSetting;

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

    if (p.getHiddenOn() != null) {
      for(UserSetting u : p.getHiddenOn()) {
        u.getHiddenProjects().remove(p);
        getEntityManager().persist(u);
      }
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
    if(order == null && (StringUtils.isBlank(keyword) || !keyword.trim().contains(" "))) {
        // order == null && keyword is Empty
        TypedQuery<Project> selectQuery = null;
        TypedQuery<Long> countQuery = null;
        if((memberships == null || memberships.isEmpty()) && StringUtils.isBlank(keyword)) {
          // memberships == null, keywork == null, order == null
          // => return all projects
          selectQuery = getEntityManager().createNamedQuery("TaskProject.findAllProjects", Project.class);
          countQuery = getEntityManager().createNamedQuery("TaskProject.countAllProjects", Long.class);
        } else if(memberships == null || memberships.isEmpty()) {
          // memberships == null, keywork != null, order == null
          // => search projects by keyword
          selectQuery = getEntityManager().createNamedQuery("TaskProject.findProjectsByKeyword", Project.class);
          selectQuery.setParameter("name", keyword);

          countQuery = getEntityManager().createNamedQuery("TaskProject.countProjectsByKeyword", Long.class);
          countQuery.setParameter("name", keyword);
        } else if(StringUtils.isBlank(keyword)) {
          HashSet<String> membershipsSet = new HashSet<String>(memberships);

          selectQuery = getEntityManager().createNamedQuery("TaskProject.findProjectsByMemberships", Project.class);
          selectQuery.setParameter("memberships", membershipsSet);

          countQuery = getEntityManager().createNamedQuery("TaskProject.countProjectsByMemberships", Long.class);
          countQuery.setParameter("memberships", membershipsSet);
        } else {
          HashSet<String> membershipsSet = new HashSet<String>(memberships);

          selectQuery = getEntityManager().createNamedQuery("TaskProject.findProjectsByMembershipsByKeyword", Project.class);
          selectQuery.setParameter("memberships", membershipsSet);
          selectQuery.setParameter("name", keyword);

          countQuery = getEntityManager().createNamedQuery("TaskProject.countProjectsByMembershipsByKeyword", Long.class);
          countQuery.setParameter("memberships", membershipsSet);
          countQuery.setParameter("name", keyword);
        }

        return new JPAQueryListAccess<Project>(Project.class, countQuery, selectQuery);
    }
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

  /** The pattern of a project permission that names a space group. */
  private static final String SPACE_PERMISSION = "'%:/spaces/%'";

  /** The prefix of the group id of a space. */
  private static final String SPACE_GROUP_PREFIX = "/spaces/";

  /** The escape character of the patterns a space group is matched with. */
  private static final char   LIKE_ESCAPE       = '!';

  /**
   * Builds the condition a mention adds to the collaborated projects, by the
   * rule of {@code TaskUtil#hasViewPermission}: the task's project names no
   * space group among its managers and participators, or names one of the
   * space groups the reader is a member of (EXO-92405). The groups are bound
   * as escaped LIKE patterns, {@code :spaceGroup0} and on, one per group.
   *
   * @param spaceGroups the reader's space groups
   * @return the SQL condition, which starts with AND
   */
  private static String mentionInsideSpaces(List<String> spaceGroups) {
    StringBuilder sql = new StringBuilder("AND (NOT EXISTS (SELECT 1 FROM TASK_PROJECT_MANAGERS AS sman\n")
        .append("WHERE sman.PROJECT_ID = ts.PROJECT_ID AND sman.MANAGER LIKE ").append(SPACE_PERMISSION).append(")\n")
        .append("AND NOT EXISTS (SELECT 1 FROM TASK_PROJECT_PARTICIPATORS AS spart\n")
        .append("WHERE spart.PROJECT_ID = ts.PROJECT_ID AND spart.PARTICIPATOR LIKE ").append(SPACE_PERMISSION).append(")\n");
    if (!spaceGroups.isEmpty()) {
      sql.append("OR EXISTS (SELECT 1 FROM TASK_PROJECT_MANAGERS AS gman\n")
         .append("WHERE gman.PROJECT_ID = ts.PROJECT_ID AND (").append(groupPatterns("gman.MANAGER", spaceGroups.size())).append("))\n")
         .append("OR EXISTS (SELECT 1 FROM TASK_PROJECT_PARTICIPATORS AS gpart\n")
         .append("WHERE gpart.PROJECT_ID = ts.PROJECT_ID AND (").append(groupPatterns("gpart.PARTICIPATOR", spaceGroups.size())).append("))\n");
    }
    return sql.append(")\n").toString();
  }

  /**
   * Builds the OR of the LIKE comparisons of a permission column with the
   * space group patterns.
   *
   * @param column the permission column
   * @param count how many groups
   * @return the SQL disjunction
   */
  private static String groupPatterns(String column, int count) {
    StringJoiner patterns = new StringJoiner(" OR ");
    for (int i = 0; i < count; i++) {
      patterns.add(column + " LIKE :spaceGroup" + i + " ESCAPE '" + LIKE_ESCAPE + "'");
    }
    return patterns.toString();
  }

  /**
   * Binds the reader's space groups, each as the pattern of a permission of
   * any role on exactly that group.
   *
   * @param query the query
   * @param spaceGroups the reader's space groups
   */
  private static void bindSpaceGroups(Query query, List<String> spaceGroups) {
    for (int i = 0; i < spaceGroups.size(); i++) {
      String group = spaceGroups.get(i);
      StringBuilder escaped = new StringBuilder("%:");
      for (char c : group.toCharArray()) {
        if (c == LIKE_ESCAPE || c == '%' || c == '_') {
          escaped.append(LIKE_ESCAPE);
        }
        escaped.append(c);
      }
      query.setParameter("spaceGroup" + i, escaped.toString());
    }
  }

  /**
   * Reads the space groups a reader is a member of, with any role.
   *
   * @param user the reader, with their memberships
   * @return the distinct space group ids
   */
  private static List<String> spaceGroupsOf(Identity user) {
    return user.getMemberships()
               .stream()
               .map(MembershipEntry::getGroup)
               .filter(group -> group != null && group.startsWith(SPACE_GROUP_PREFIX))
               .distinct()
               .toList();
  }

 @Override
  public List<Project> findCollaboratedProjects(Identity user, String keyword,int offset ,int limit) {
   String userName = user.getUserId();
   List<String> spaceGroups = spaceGroupsOf(user);
   Query q = getEntityManager().createNativeQuery(
           "SELECT DISTINCT(p.PROJECT_ID) FROM TASK_PROJECTS p \n" +
                   "where \n" +
                   "lower(p.NAME) LIKE lower(concat('%', :keyword, '%'))\n" +
                   "AND\n" +
                   "(p.PROJECT_ID in (\n" +
                   "SELECT distinct(ts.PROJECT_ID)\n" +
                   "FROM TASK_STATUS AS ts\n" +
                   "WHERE ts.STATUS_ID IN\n" +
                   "   ( \n" +
                   "SELECT task.STATUS_ID\n" +
                   "FROM TASK_TASKS AS task\n" +
                   "WHERE task.COMPLETED = false\n" +
                   "AND (task.ASSIGNEE = :userName\n" +
                   "  OR task.TASK_ID IN (\n" +
                   "SELECT com.TASK_ID\n" +
                   "FROM TASK_COMMENTS AS com\n" +
                   "WHERE com.TASK_ID = task.TASK_ID\n" +
                   "AND (com.AUTHOR = :userName\n" +
                   " OR com.COMMENT_ID IN (\n" +
                   "SELECT cmention.COMMENT_ID\n" +
                   "FROM TASK_COMMENT_MENTIONED_USERS AS cmention\n" +
                   "WHERE com.COMMENT_ID = cmention.COMMENT_ID\n" +
                   "AND cmention.MENTIONED_USERS = :userName\n" +
                   mentionInsideSpaces(spaceGroups) +
                   ")\n" +
                   ")\n" +
                   " )\n" +
                   "         OR task.TASK_ID IN (\n" +
                   "SELECT coworker.TASK_ID\n" +
                   "            FROM TASK_TASK_COWORKERS AS coworker\n" +
                   "            WHERE coworker.TASK_ID = task.TASK_ID\n" +
                   " AND coworker.COWORKER = :userName\n" +
                   "         )\n" +
                   "    )\n" +
                   " )\n" +
                   "))");
     q.setParameter("keyword", keyword);
     q.setParameter("userName", userName);
     bindSpaceGroups(q, spaceGroups);
     if (limit > 0) {
         q.setFirstResult(offset).setMaxResults(limit);
     }
   List<Object> ids = q.getResultList();
   if(ids.isEmpty()) {
     return new ArrayList<Project>();
   }
   List<Long> idsLong = ids.stream().map(i -> Long.parseLong(i.toString())).collect(Collectors.toList());
   return findIdentitiesByIDs(idsLong);
 }

    @Override
    public List<Project> findNotEmptyProjects(List<String> memberships, String keyword,int offset ,int limit) {
   Query q = getEntityManager().createNativeQuery(
           "SELECT DISTINCT(p.PROJECT_ID) FROM TASK_PROJECTS p \n" +
                   "where \n" +
                   "lower(p.NAME) LIKE lower(concat('%', :keyword , '%'))\n" +
                   "AND\n" +
                   "(p.PROJECT_ID in (\n" +
                   "SELECT man.PROJECT_ID\n" +
                   "            FROM TASK_PROJECT_MANAGERS AS man\n" +
                   "            WHERE man.MANAGER IN (:memberships) \t\t    \n" +
                   ") or \n" +
                   "p.PROJECT_ID in (\n" +
                   "SELECT part.PROJECT_ID\n" +
                   "            FROM TASK_PROJECT_PARTICIPATORS AS part\n" +
                   "            WHERE part.PARTICIPATOR IN (:memberships) \n" +
                   ") ) AND p.PROJECT_ID in (\n" +
                   "SELECT distinct(ts.PROJECT_ID)\n" +
                   "FROM TASK_STATUS AS ts\n" +
                   "WHERE ts.STATUS_ID IN(\n" +
                   "SELECT tcol.STATUS_ID FROM  \n" +
                   "(SELECT task.STATUS_ID AS STATUS_ID , COUNT(*) \n" +
                   "FROM TASK_TASKS AS task \n" +
                   "WHERE task.COMPLETED = false\n" +
                   "GROUP BY task.STATUS_ID\n" +
                   "HAVING COUNT(*)> 0) AS tcol\n" +
                   ") \n" +
                   ")\n");
   q.setParameter("keyword", keyword);
   q.setParameter("memberships", memberships);
   if (limit > 0) {
       q.setFirstResult(offset).setMaxResults(limit);
   }
   List<Object> ids = q.getResultList();
   if(ids.isEmpty()) {
     return new ArrayList<Project>();
   }
   List<Long> idsLong = ids.stream().map(i -> Long.parseLong(i.toString())).collect(Collectors.toList());
   return findIdentitiesByIDs(idsLong);
  }

 @Override
  public int countCollaboratedProjects(Identity user, String keyword) {
   String userName = user.getUserId();
   List<String> spaceGroups = spaceGroupsOf(user);
   Query q = getEntityManager().createNativeQuery(
           "SELECT DISTINCT(p.PROJECT_ID) FROM TASK_PROJECTS p \n" +
                   "where \n" +
                   "lower(p.NAME) LIKE lower(concat('%', :keyword, '%'))\n" +
                   "AND\n" +
                   "(p.PROJECT_ID in (\n" +
                   "SELECT distinct(ts.PROJECT_ID)\n" +
                   "FROM TASK_STATUS AS ts\n" +
                   "WHERE ts.STATUS_ID IN\n" +
                   "   ( \n" +
                   "SELECT task.STATUS_ID\n" +
                   "FROM TASK_TASKS AS task\n" +
                   "WHERE task.COMPLETED = false\n" +
                   "AND (task.ASSIGNEE = :userName\n" +
                   "  OR task.TASK_ID IN (\n" +
                   "SELECT com.TASK_ID\n" +
                   "FROM TASK_COMMENTS AS com\n" +
                   "WHERE com.TASK_ID = task.TASK_ID\n" +
                   "AND (com.AUTHOR = :userName\n" +
                   " OR com.COMMENT_ID IN (\n" +
                   "SELECT cmention.COMMENT_ID\n" +
                   "FROM TASK_COMMENT_MENTIONED_USERS AS cmention\n" +
                   "WHERE com.COMMENT_ID = cmention.COMMENT_ID\n" +
                   "AND cmention.MENTIONED_USERS = :userName\n" +
                   mentionInsideSpaces(spaceGroups) +
                   ")\n" +
                   ")\n" +
                   " )\n" +
                   "         OR task.TASK_ID IN (\n" +
                   "SELECT coworker.TASK_ID\n" +
                   "            FROM TASK_TASK_COWORKERS AS coworker\n" +
                   "            WHERE coworker.TASK_ID = task.TASK_ID\n" +
                   " AND coworker.COWORKER = :userName\n" +
                   "         )\n" +
                   "    )\n" +
                   " )\n" +
                   "))");
     q.setParameter("keyword", keyword);
     q.setParameter("userName", userName);
     bindSpaceGroups(q, spaceGroups);
     int result = Integer.parseInt(q.getSingleResult().toString());
      return result ;
 }

    @Override
    public int countNotEmptyProjects(List<String> memberships, String keyword) {
   Query q = getEntityManager().createNativeQuery(
           "SELECT DISTINCT(p.PROJECT_ID) FROM TASK_PROJECTS p \n" +
                   "where \n" +
                   "lower(p.NAME) LIKE lower(concat('%', :keyword , '%'))\n" +
                   "AND\n" +
                   "(p.PROJECT_ID in (\n" +
                   "SELECT man.PROJECT_ID\n" +
                   "            FROM TASK_PROJECT_MANAGERS AS man\n" +
                   "            WHERE man.MANAGER IN (:memberships) \t\t    \n" +
                   ") or \n" +
                   "p.PROJECT_ID in (\n" +
                   "SELECT part.PROJECT_ID\n" +
                   "            FROM TASK_PROJECT_PARTICIPATORS AS part\n" +
                   "            WHERE part.PARTICIPATOR IN (:memberships) \n" +
                   ") ) AND p.PROJECT_ID in (\n" +
                   "SELECT distinct(ts.PROJECT_ID)\n" +
                   "FROM TASK_STATUS AS ts\n" +
                   "WHERE ts.STATUS_ID IN(\n" +
                   "SELECT tcol.STATUS_ID FROM  \n" +
                   "(SELECT task.STATUS_ID AS STATUS_ID , COUNT(*) \n" +
                   "FROM TASK_TASKS AS task \n" +
                   "WHERE task.COMPLETED = false\n" +
                   "GROUP BY task.STATUS_ID\n" +
                   "HAVING COUNT(*)> 0) AS tcol\n" +
                   ") \n" +
                   ")\n");
   q.setParameter("keyword", keyword);
   q.setParameter("memberships", memberships);
        int result = Integer.parseInt(q.getSingleResult().toString());
        return result ;
  }

  protected List<Project> findIdentitiesByIDs(List<Long> ids) {
    TypedQuery<Project> query = getEntityManager().createNamedQuery("TaskProject.findProjectsByIDs", Project.class);
      query.setParameter("ids", ids);
      return query.getResultList();
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
