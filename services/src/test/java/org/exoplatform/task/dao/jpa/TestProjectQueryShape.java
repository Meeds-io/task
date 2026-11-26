/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.task.AbstractTest;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.domain.Project;

public class TestProjectQueryShape extends AbstractTest {

  @After
  public void tearDown() {
    deleteAll();
  }

  @Test
  public void testMembershipConditionsDoNotJoinTheCollections() {
    DAOHandler daoHandler = PortalContainer.getInstance().getComponentInstanceOfType(DAOHandler.class);
    ProjectDAOImpl dao = (ProjectDAOImpl) daoHandler.getProjectHandler();
    ProjectQuery query = new ProjectQuery();
    query.setMembership(Arrays.asList("manager:/spaces/space1", "member:/spaces/space1"));
    query.setMembership(Arrays.asList("john", "member:/spaces/space1"));

    EntityManager em = PortalContainer.getInstance().getComponentInstanceOfType(EntityManagerService.class).getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
    Root<Project> root = criteria.from(Project.class);
    criteria.where(dao.buildQuery(query.getCondition(), root, cb, criteria));
    criteria.select(cb.countDistinct(root));

    // a join per condition would multiply the rows of a project by the size of its collections
    Assert.assertTrue(root.getJoins().toString(), root.getJoins().isEmpty());
    Assert.assertNotNull(em.createQuery(criteria));
  }
}
