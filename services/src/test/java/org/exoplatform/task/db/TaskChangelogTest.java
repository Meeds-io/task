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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * The task changelogs on the engines the unit suite can reach: HSQLDB in
 * memory, and the SQL Liquibase generates offline for Oracle and SQL Server.
 * The offline output checks the grammar of what those engines would receive,
 * not how they plan a query; PostgreSQL and MySQL run for real in
 * {@link TaskChangelogIT}.
 */
public class TaskChangelogTest {

  /** The changelog that adds the TASK_ID indexes (EXO-92610). */
  public static final String CHANGELOG_7_3_0 = "db/changelog/task.db.changelog-7.3.0.xml";

  /** The changeset indexing the coworkers' TASK_ID. */
  public static final String COWORKERS_INDEX = "7.3.0-exo-92610-01";

  /** The changeset indexing the labels' TASK_ID. */
  public static final String LABELS_INDEX    = "7.3.0-exo-92610-02";

  /**
   * Every registered changelog exists, and the 7.3.0 one runs after 7.2.0, so
   * that the platform applies it at all.
   *
   * @throws Exception when the configuration cannot be read
   */
  @Test
  public void testChangelogsAreRegisteredInOrder() throws Exception {
    List<String> changelogs = TaskChangelogs.registeredChangelogs();
    for (String changelog : changelogs) {
      assertNotNull(changelog, getClass().getClassLoader().getResource(changelog));
    }
    assertEquals(CHANGELOG_7_3_0, changelogs.get(changelogs.size() - 1));
    assertEquals(changelogs.indexOf("db/changelog/task.db.changelog-7.2.0.xml") + 1, changelogs.indexOf(CHANGELOG_7_3_0));
  }

  /**
   * On HSQLDB the whole changelog applies, and the TASK_ID indexes are not
   * added: HSQLDB indexes a foreign key's column itself, and the changesets
   * name the engines that need them.
   *
   * @throws Exception when a changelog fails
   */
  @Test
  public void testIndexesAreNotAddedOnHsqldb() throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:taskChangelogTest;shutdown=true", "sa", "")) {
      TaskChangelogs.updateAll(connection);
      Map<String, String> recorded = TaskChangelogs.recorded(connection, CHANGELOG_7_3_0);
      assertFalse(recorded.toString(), recorded.containsKey(COWORKERS_INDEX));
      assertFalse(recorded.toString(), recorded.containsKey(LABELS_INDEX));
    }
  }

  /**
   * Oracle receives one index on each table's TASK_ID.
   *
   * @throws Exception when the SQL cannot be generated
   */
  @Test
  public void testIndexesSqlOnOracle() throws Exception {
    assertIndexesInSql(offlineSql("oracle"));
  }

  /**
   * SQL Server receives one index on each table's TASK_ID.
   *
   * @throws Exception when the SQL cannot be generated
   */
  @Test
  public void testIndexesSqlOnSqlServer() throws Exception {
    assertIndexesInSql(offlineSql("mssql"));
  }

  /**
   * MySQL receives no index: InnoDB already created one for each foreign key.
   *
   * @throws Exception when the SQL cannot be generated
   */
  @Test
  public void testNoIndexSqlOnMySql() throws Exception {
    String sql = offlineSql("mysql");
    assertFalse(sql, sql.contains("IDX_TASK_TASK_COWORKERS_02"));
    assertFalse(sql, sql.contains("IDX_TASK_LABEL_TASK_02"));
  }

  /**
   * Asserts the two {@code CREATE INDEX} statements.
   *
   * @param sql the generated SQL, normalised
   */
  private void assertIndexesInSql(String sql) {
    assertTrue(sql, sql.contains(" INDEX IDX_TASK_TASK_COWORKERS_02 ON TASK_TASK_COWORKERS(TASK_ID)"));
    assertTrue(sql, sql.contains(" INDEX IDX_TASK_LABEL_TASK_02 ON TASK_LABEL_TASK(TASK_ID)"));
  }

  /**
   * Generates the 7.3.0 changelog's SQL for an engine, without a database:
   * preconditions are not evaluated offline, so both statements are written.
   * The offline engine keeps the changesets it has written in a CSV history
   * (in the working directory by default), so each run gets an empty one of
   * its own: a shared one makes the second run write nothing.
   *
   * @param engine the Liquibase short name of the engine
   * @return the SQL, upper-cased, without quotes, brackets or extra spaces
   * @throws Exception when the SQL cannot be generated
   */
  private String offlineSql(String engine) throws Exception {
    Path history = Files.createTempFile("task-changelog-" + engine, ".csv");
    Files.delete(history);
    try {
      ClassLoaderResourceAccessor accessor = new ClassLoaderResourceAccessor();
      OfflineConnection connection = new OfflineConnection("offline:" + engine + "?changeLogFile=" + history, accessor);
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
      StringWriter output = new StringWriter();
      new Liquibase(CHANGELOG_7_3_0, accessor, database).update(new Contexts(), new LabelExpression(), output);
      return output.toString()
                   .toUpperCase(Locale.ROOT)
                   .replaceAll("[\"\\[\\]`]", "")
                   .replaceAll("\\s+", " ")
                   .replace(" (", "(");
    } finally {
      Files.deleteIfExists(history);
    }
  }
}
