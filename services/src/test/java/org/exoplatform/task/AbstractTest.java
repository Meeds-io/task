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
package org.exoplatform.task;

import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;

/**
 * A base test class for all DAO tests which take responsibility to
 * initialize/clean up database.
 * 
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class AbstractTest {

  @Before
  public void initializeContainerAndStartRequestLifecycle() {
    PortalContainer container = PortalContainer.getInstance();

    //
    RequestLifeCycle.begin(container);
    
    EntityManagerService entityMgrService = (EntityManagerService) container.getComponentInstanceOfType(EntityManagerService.class);
    entityMgrService.getEntityManager().getTransaction().begin();
  }

  @After
  public void endRequestLifecycle() {
    PortalContainer container = PortalContainer.getInstance();

    //
    EntityManagerService entityMgrService = (EntityManagerService) container.getComponentInstanceOfType(EntityManagerService.class);
    if (entityMgrService.getEntityManager() != null && entityMgrService.getEntityManager().getTransaction() != null
        && entityMgrService.getEntityManager().getTransaction().isActive()) {
      entityMgrService.getEntityManager().getTransaction().commit();
      //
      RequestLifeCycle.end();
    }

  }
}
