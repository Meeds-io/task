package org.exoplatform.task.test.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.task.test.AbstractTest;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class TestEntityManager extends AbstractTest {

  private EntityManagerFactory entityManagerFactory;
  private EntityManager entityManager;

  @Before
  public void initEntityManager() throws Exception {
    entityManagerFactory = Persistence.createEntityManagerFactory("org.exoplatform.task");
    entityManager = entityManagerFactory.createEntityManager();
    // Start transaction
    entityManager.getTransaction().begin();
  }


  @After
  public void closeEntityManager() throws Exception {
    // Close Transaction
    entityManager.close();
    entityManagerFactory.close();
  }

  @Test
  public void testEntityManagerFactoryNotNull() {
    Assert.assertNotNull(entityManagerFactory);
  }
}
