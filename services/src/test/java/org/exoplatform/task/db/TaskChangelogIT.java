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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.junit.Test;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The task changelogs on real PostgreSQL 16 and MySQL 8.0 (run with
 * {@code -Prun-its}). On PostgreSQL, the plan of a card's coworker and label
 * lookups is read before and after the TASK_ID indexes, on a seeded volume;
 * on MySQL, the foreign-key index that makes them unnecessary is checked.
 * Oracle and SQL Server are not run: {@link TaskChangelogTest} checks the SQL
 * they would receive.
 */
public class TaskChangelogIT {

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
   * Counts a table's MySQL indexes whose first column is TASK_ID.
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
}
