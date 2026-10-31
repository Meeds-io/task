/*
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
package org.exoplatform.task.db;

import static org.exoplatform.task.db.TaskChangelogTest.CHANGELOG_7_3_0;
import static org.exoplatform.task.db.TaskChangelogTest.COWORKERS_INDEX;
import static org.exoplatform.task.db.TaskChangelogTest.LABELS_INDEX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManager;

import org.exoplatform.task.domain.ChangeLog;
import org.hibernate.dialect.PostgreSQLDialect;

import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import liquibase.resource.DirectoryResourceAccessor;

/**
 * The task changelogs on real PostgreSQL 16, MySQL 8.0 and MariaDB 11.4 (run with
 * {@code -Prun-its}). On PostgreSQL, the plan of a card's coworker and label
 * lookups is read before and after the TASK_ID indexes, on a seeded volume;
 * on MySQL and MariaDB, the foreign-key index that makes them unnecessary is
 * checked. Oracle and SQL Server are not run: {@link TaskChangelogTest} checks
 * the index statements they would receive, not how they plan the lookups;
 * nothing checks their fill of the pre-7.2 change dates (7.2.0-exo-92620-01).
 */
public class TaskChangelogIT {

  /** The changelog that moves change dates to TIMESTAMP. */
  private static final String            CHANGELOG_7_2_0 = "db/changelog/task.db.changelog-7.2.0.xml";

  /** The changeset that fills the change dates on PostgreSQL, MariaDB, Oracle and SQL Server (EXO-92620). */
  private static final String            FILL_DATES      = "7.2.0-exo-92620-01";

  /** A card's changes before 7.2, oldest first, and their times in milliseconds. */
  private static final Map<String, Long> CHANGE_TIMES    = changeTimesBefore72();

  /** Cards seeded on PostgreSQL, enough for a sequential scan to cost more than an index lookup. */
  private static final int    CARDS                 = 20000;

  /** A card's coworkers, read by its id. */
  private static final String COWORKERS_OF_A_CARD   = "SELECT COWORKER FROM TASK_TASK_COWORKERS WHERE TASK_ID = 12345";

  /** A card's labels, read by its id. */
  private static final String LABELS_OF_A_CARD      = "SELECT LABEL_ID FROM TASK_LABEL_TASK WHERE TASK_ID = 12345";

  /**
   * On PostgreSQL the indexes are created, a card's coworkers and labels are
   * then read through them instead of a sequential scan, and rolling the
   * changesets back brings the scans back.
   *
   * @throws Exception when the database cannot be reached
   */
  @Test
  public void testTaskIdIndexesOnPostgreSql() throws Exception {
    try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")) {
      postgres.start();
      try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        TaskChangelogs.updateAll(connection);
        Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_3_0);
        assertEquals(recorded.toString(), "EXECUTED", recorded.get(COWORKERS_INDEX));
        assertEquals(recorded.toString(), "EXECUTED", recorded.get(LABELS_INDEX));

        seed(connection);
        assertIndexed(connection);

        TaskChangelogs.rollback(connection, CHANGELOG_7_3_0, 2);
        assertEquals("the seed outlives the rollback", 2 * CARDS, count(connection, "SELECT count(*) FROM TASK_TASK_COWORKERS"));
        assertEquals("the seed outlives the rollback", CARDS, count(connection, "SELECT count(*) FROM TASK_LABEL_TASK"));
        execute(connection, "ANALYZE");
        assertNotIndexed(connection);

        TaskChangelogs.update(connection, CHANGELOG_7_3_0);
        assertIndexed(connection);
      }
    }
  }

  /**
   * An index an administrator already created on TASK_ID is kept, and the
   * changeset is marked as run instead of adding a second one.
   *
   * @throws Exception when the database cannot be reached
   */
  @Test
  public void testExistingIndexIsKeptOnPostgreSql() throws Exception {
    try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")) {
      postgres.start();
      try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        for (String changelog : TaskChangelogs.registeredChangelogs()) {
          if (!changelog.equals(CHANGELOG_7_3_0)) {
            TaskChangelogs.update(connection, changelog);
          }
        }
        execute(connection, "CREATE INDEX ADMIN_COWORKERS_TASK ON TASK_TASK_COWORKERS(TASK_ID)");
        TaskChangelogs.update(connection, CHANGELOG_7_3_0);

        Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_3_0);
        assertEquals(recorded.toString(), "MARK_RAN", recorded.get(COWORKERS_INDEX));
        assertEquals(recorded.toString(), "EXECUTED", recorded.get(LABELS_INDEX));
        assertEquals(1, count(connection, "SELECT count(*) FROM pg_indexes WHERE tablename = 'task_task_coworkers'"
            + " AND indexdef LIKE '%(task_id)'"));
      }
    }
  }

  /**
   * On MySQL the changesets do not run: InnoDB already holds one index leading
   * on TASK_ID for each table's foreign key.
   *
   * @throws Exception when the database cannot be reached
   */
  @Test
  public void testForeignKeyIndexesOnMySql() throws Exception {
    try (MySQLContainer mysql = new MySQLContainer("mysql:8.0")) {
      mysql.start();
      try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
        TaskChangelogs.updateAll(connection);
        Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_3_0);
        assertFalse(recorded.toString(), recorded.containsKey(COWORKERS_INDEX));
        assertFalse(recorded.toString(), recorded.containsKey(LABELS_INDEX));
        assertEquals(1, mysqlIndexesLeadingOnTaskId(connection, "TASK_TASK_COWORKERS"));
        assertEquals(1, mysqlIndexesLeadingOnTaskId(connection, "TASK_LABEL_TASK"));
      }
    }
  }

  /**
   * A PostgreSQL database not yet on 7.2 gets every change's date when it
   * upgrades: 7.2.0-exo-92620-01 fills them before the old column is dropped,
   * to the millisecond. 7.2.0-02, declared for "postgres", still never runs.
   * Mutants: the changeset removed, or moved after 7.2.0-03 (the upgrade then
   * fails on the dropped column).
   *
   * @throws Exception when the database cannot be reached
   */
  @Test
  public void testDatesFilledWhenUpgradingTo72OnPostgreSql() throws Exception {
    try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")) {
      postgres.start();
      try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        TaskChangelogs.updateBefore(connection, CHANGELOG_7_2_0);
        seedChangesBefore72(connection);

        TaskChangelogs.update(connection, CHANGELOG_7_2_0);

        Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_2_0);
        assertEquals(recorded.toString(), "EXECUTED", recorded.get(FILL_DATES));
        assertFalse(recorded.toString(), recorded.containsKey("7.2.0-02"));
        assertEquals(CHANGE_TIMES, changeTimes(connection));
        TaskChangelogs.update(connection, CHANGELOG_7_3_0);
      }
    }
  }

  /**
   * A PostgreSQL database that ran 7.2.0 as shipped, with no date on its old
   * changes, upgrades without error: 7.2.0-exo-92620-01 finds no source and is
   * marked as run, the existing changesets keep their checksums, the old
   * changes stay undated. A task's change log then reads its dated entries
   * newest first and the undated ones last. Mutants: the precondition
   * removed (the upgrade fails); the named query's NULL rule removed (the
   * undated entries come first).
   *
   * @throws Exception when the database cannot be reached
   */
  @Test
  public void testAlreadyUpgradedPostgreSqlKeepsItsUndatedChanges() throws Exception {
    try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")) {
      postgres.start();
      try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        TaskChangelogs.updateBefore(connection, CHANGELOG_7_2_0);
        seedChangesBefore72(connection);
        TaskChangelogs.update(connection, CHANGELOG_7_2_0, new DirectoryResourceAccessor(Path.of(getClass().getResource("/before-exo-92620").toURI())));
        assertEquals(CHANGE_TIMES.size(), count(connection, "SELECT count(*) FROM TASK_CHANGE_LOGS WHERE CREATED_TIME IS NULL"));
        execute(connection, "INSERT INTO TASK_CHANGE_LOGS (CHANGE_LOG_ID, TASK_ID, AUTHOR, ACTION_NAME, CREATED_TIME)"
            + " VALUES (10, 1, 'root', 'after 7.2', TIMESTAMP '2026-10-01 10:00:00')");

        TaskChangelogs.update(connection, CHANGELOG_7_2_0);
        TaskChangelogs.update(connection, CHANGELOG_7_3_0);

        Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_2_0);
        assertEquals(recorded.toString(), "MARK_RAN", recorded.get(FILL_DATES));
        assertEquals(CHANGE_TIMES.size(), count(connection, "SELECT count(*) FROM TASK_CHANGE_LOGS WHERE CREATED_TIME IS NULL"));
        assertEquals(List.of("after 7.2", "edit_title", "assign", "created"), taskLogActions(postgres, 1L));
      }
    }
  }

  /**
   * A MariaDB database not yet on 7.2 gets every change's date when it
   * upgrades: 7.2.0-02, declared for "mysql", does not match MariaDB, and
   * 7.2.0-exo-92620-01 fills the dates there, to the second (the column keeps
   * no fraction). The dates are compared in the database, where
   * UNIX_TIMESTAMP reverses FROM_UNIXTIME in the session's time zone.
   * Mutant: the MariaDB changeset removed.
   *
   * @throws Exception when the database cannot be reached
   */
  @Test
  public void testDatesFilledWhenUpgradingTo72OnMariaDb() throws Exception {
    try (MariaDBContainer mariadb = new MariaDBContainer("mariadb:11.4")) {
      mariadb.start();
      try (Connection connection = DriverManager.getConnection(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword())) {
        TaskChangelogs.updateBefore(connection, CHANGELOG_7_2_0);
        seedChangesBefore72(connection);

        TaskChangelogs.update(connection, CHANGELOG_7_2_0);

        Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_2_0);
        assertFalse(recorded.toString(), recorded.containsKey("7.2.0-02"));
        assertEquals(recorded.toString(), "EXECUTED", recorded.get(FILL_DATES));
        try (Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT ACTION_NAME, UNIX_TIMESTAMP(CREATED_TIME) FROM TASK_CHANGE_LOGS")) {
          int rows = 0;
          while (result.next()) {
            rows++;
            assertEquals(result.getString(1), CHANGE_TIMES.get(result.getString(1)) / 1000, result.getLong(2));
          }
          assertEquals(CHANGE_TIMES.size(), rows);
        }
      }
    }
  }

  /**
   * MySQL already had its dates from 7.2.0-02: 7.2.0-exo-92620-01 does not
   * run there.
   *
   * @throws Exception when the database cannot be reached
   */
  @Test
  public void testDatesUnchangedOnMySql() throws Exception {
    try (MySQLContainer mysql = new MySQLContainer("mysql:8.0")) {
      mysql.start();
      try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
        TaskChangelogs.updateBefore(connection, CHANGELOG_7_2_0);
        seedChangesBefore72(connection);

        TaskChangelogs.update(connection, CHANGELOG_7_2_0);

        Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_2_0);
        assertEquals(recorded.toString(), "EXECUTED", recorded.get("7.2.0-02"));
        assertFalse(recorded.toString(), recorded.containsKey(FILL_DATES));
        assertEquals(0, count(connection, "SELECT count(*) FROM TASK_CHANGE_LOGS WHERE CREATED_TIME IS NULL"));
      }
    }
  }

  /**
   * Seeds a board, a card and its changes as a database before 7.2 holds
   * them: CREATED_TIME in milliseconds.
   *
   * @param connection the database
   * @throws SQLException when a statement fails
   */
  private void seedChangesBefore72(Connection connection) throws SQLException {
    execute(connection, "INSERT INTO TASK_PROJECTS (PROJECT_ID, NAME) VALUES (1, 'Board')");
    execute(connection, "INSERT INTO TASK_STATUS (STATUS_ID, NAME, STATUS_RANK, PROJECT_ID) VALUES (1, 'To do', 0, 1)");
    execute(connection, "INSERT INTO TASK_TASKS (TASK_ID, TITLE, STATUS_ID, COMPLETED) VALUES (1, 'Card', 1, false)");
    long id = 1;
    for (Map.Entry<String, Long> change : CHANGE_TIMES.entrySet()) {
      execute(connection, "INSERT INTO TASK_CHANGE_LOGS (CHANGE_LOG_ID, TASK_ID, AUTHOR, ACTION_NAME, CREATED_TIME) VALUES ("
          + id++ + ", 1, 'root', '" + change.getKey() + "', " + change.getValue() + ")");
    }
  }

  /**
   * Reads every change's date, in milliseconds, as the JVM reads the
   * TIMESTAMP column (the PostgreSQL driver sets the session's time zone to
   * the JVM's).
   *
   * @param connection the database
   * @return the dates by action, null for an undated change
   * @throws SQLException when the table cannot be read
   */
  private Map<String, Long> changeTimes(Connection connection) throws SQLException {
    Map<String, Long> times = new LinkedHashMap<>();
    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT ACTION_NAME, CREATED_TIME FROM TASK_CHANGE_LOGS ORDER BY CHANGE_LOG_ID")) {
      while (result.next()) {
        Timestamp time = result.getTimestamp("CREATED_TIME");
        times.put(result.getString("ACTION_NAME"), time == null ? null : time.getTime());
      }
    }
    return times;
  }

  /**
   * Reads a task's change log through the named query the task service uses,
   * on Hibernate with the PostgreSQL dialect.
   *
   * @param container the PostgreSQL database
   * @param taskId the task
   * @return the actions of its entries, in the query's order
   */
  private List<String> taskLogActions(JdbcDatabaseContainer<?> container, long taskId) {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(new DriverManagerDataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword()));
    factory.setPackagesToScan("org.exoplatform.task.domain");
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    Properties properties = new Properties();
    properties.setProperty("hibernate.dialect", PostgreSQLDialect.class.getName());
    properties.setProperty("hibernate.hbm2ddl.auto", "none");
    factory.setJpaProperties(properties);
    factory.afterPropertiesSet();
    try {
      EntityManager entityManager = factory.getObject().createEntityManager();
      try {
        return entityManager.createNamedQuery("TaskChangeLog.findChangeLogByTaskId", ChangeLog.class)
                            .setParameter("taskId", taskId)
                            .getResultList()
                            .stream()
                            .map(ChangeLog::getActionName)
                            .toList();
      } finally {
        entityManager.close();
      }
    } finally {
      factory.destroy();
    }
  }

  /**
   * On MariaDB, which Liquibase names apart from MySQL, the changesets do not
   * run either: InnoDB holds one index leading on TASK_ID for each table's
   * foreign key there too.
   *
   * @throws Exception when the database cannot be reached
   */
  @Test
  public void testForeignKeyIndexesOnMariaDb() throws Exception {
    try (MariaDBContainer mariadb = new MariaDBContainer("mariadb:11.4")) {
      mariadb.start();
      try (Connection connection = DriverManager.getConnection(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword())) {
        TaskChangelogs.updateAll(connection);
        Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_3_0);
        assertFalse(recorded.toString(), recorded.containsKey(COWORKERS_INDEX));
        assertFalse(recorded.toString(), recorded.containsKey(LABELS_INDEX));
        assertEquals(1, mysqlIndexesLeadingOnTaskId(connection, "TASK_TASK_COWORKERS"));
        assertEquals(1, mysqlIndexesLeadingOnTaskId(connection, "TASK_LABEL_TASK"));
      }
    }
  }

  /**
   * Seeds one board, ten labels and {@link #CARDS} cards, each with two of
   * fifty coworkers and one label, then refreshes the planner's statistics.
   *
   * @param connection the PostgreSQL database
   * @throws SQLException when a statement fails
   */
  private void seed(Connection connection) throws SQLException {
    execute(connection, "INSERT INTO TASK_PROJECTS (PROJECT_ID, NAME) VALUES (1, 'Board')");
    execute(connection, "INSERT INTO TASK_STATUS (STATUS_ID, NAME, STATUS_RANK, PROJECT_ID) VALUES (1, 'To do', 0, 1)");
    execute(connection, "INSERT INTO TASK_LABELS (LABEL_ID, NAME, USERNAME, PROJECT_ID)"
        + " SELECT n, 'label' || n, 'user1', 1 FROM generate_series(1, 10) n");
    execute(connection, "INSERT INTO TASK_TASKS (TASK_ID, TITLE, ASSIGNEE, STATUS_ID, COMPLETED)"
        + " SELECT n, 'Card ' || n, 'user' || (n % 50), 1, false FROM generate_series(1, " + CARDS + ") n");
    execute(connection, "INSERT INTO TASK_TASK_COWORKERS (TASK_ID, COWORKER)"
        + " SELECT n, 'user' || (n % 50) FROM generate_series(1, " + CARDS + ") n"
        + " UNION ALL SELECT n, 'user' || ((n + 7) % 50) FROM generate_series(1, " + CARDS + ") n");
    execute(connection, "INSERT INTO TASK_LABEL_TASK (LABEL_ID, TASK_ID)"
        + " SELECT 1 + (n % 10), n FROM generate_series(1, " + CARDS + ") n");
    execute(connection, "ANALYZE");
  }

  /**
   * Asserts that a card's coworkers and labels are read through the TASK_ID
   * indexes, with no sequential scan. A condition on the coworkers that
   * PostgreSQL can hash (an EXISTS correlated by TASK_ID equality under an
   * OR) is planned once through the COWORKER index and needs neither; the
   * lookups below are those made card by card.
   *
   * @param connection the PostgreSQL database
   * @throws SQLException when a plan cannot be read
   */
  private void assertIndexed(Connection connection) throws SQLException {
    assertPlan(connection, COWORKERS_OF_A_CARD, "idx_task_task_coworkers_02", "Seq Scan on task_task_coworkers");
    assertPlan(connection, LABELS_OF_A_CARD, "idx_task_label_task_02", "Seq Scan on task_label_task");
  }

  /**
   * Asserts that, without the indexes, the card lookups scan the tables: what
   * the indexes remove.
   *
   * @param connection the PostgreSQL database
   * @throws SQLException when a plan cannot be read
   */
  private void assertNotIndexed(Connection connection) throws SQLException {
    String coworkers = plan(connection, COWORKERS_OF_A_CARD);
    assertTrue(coworkers, coworkers.contains("Seq Scan on task_task_coworkers"));
    String labels = plan(connection, LABELS_OF_A_CARD);
    assertFalse(labels, labels.contains("idx_task_label_task_02"));
  }

  /**
   * Asserts what a statement's plan holds and does not hold.
   *
   * @param connection the PostgreSQL database
   * @param sql the statement
   * @param expected a fragment the plan must contain
   * @param forbidden a fragment it must not contain
   * @throws SQLException when the plan cannot be read
   */
  private void assertPlan(Connection connection, String sql, String expected, String forbidden) throws SQLException {
    String plan = plan(connection, sql);
    assertTrue(plan, plan.contains(expected));
    assertFalse(plan, plan.contains(forbidden));
  }

  /**
   * Reads a statement's plan as PostgreSQL chooses it.
   *
   * @param connection the PostgreSQL database
   * @param sql the statement
   * @return the plan, one node per line
   * @throws SQLException when the plan cannot be read
   */
  private String plan(Connection connection, String sql) throws SQLException {
    StringBuilder plan = new StringBuilder();
    try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("EXPLAIN " + sql)) {
      while (result.next()) {
        plan.append(result.getString(1)).append('\n');
      }
    }
    return plan.toString();
  }

  /**
   * Counts a table's MySQL or MariaDB indexes whose first column is TASK_ID.
   *
   * @param connection the MySQL database
   * @param table the table
   * @return how many there are
   * @throws SQLException when the indexes cannot be read
   */
  private int mysqlIndexesLeadingOnTaskId(Connection connection, String table) throws SQLException {
    return count(connection, "SELECT count(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()"
        + " AND TABLE_NAME = '" + table + "' AND COLUMN_NAME = 'TASK_ID' AND SEQ_IN_INDEX = 1");
  }

  /**
   * Runs a count query.
   *
   * @param connection the database
   * @param sql a statement returning one number
   * @return that number
   * @throws SQLException when the statement fails
   */
  private int count(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
      result.next();
      return result.getInt(1);
    }
  }

  /**
   * Runs a statement.
   *
   * @param connection the database
   * @param sql the statement
   * @throws SQLException when it fails
   */
  private void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  /**
   * @return the changes of the card seeded before 7.2, each with its time in
   *         milliseconds, whole seconds and a millisecond part
   */
  private static Map<String, Long> changeTimesBefore72() {
    Map<String, Long> times = new LinkedHashMap<>();
    times.put("created", 1500000000123L);
    times.put("assign", 1600000000456L);
    times.put("edit_title", 1700000000789L);
    return times;
  }
}
