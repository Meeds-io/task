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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Runs the task changelogs the way the platform does: each file registered in
 * {@code conf/portal/configuration.xml} by the
 * {@code TaskManagementChangeLogsPlugin}, one Liquibase run per file, in the
 * registered order ({@code LiquibaseDataInitializer#applyChangeLog} in portal).
 */
public final class TaskChangelogs {

  /** The kernel configuration that registers the task changelogs. */
  public static final String CONFIGURATION = "conf/portal/configuration.xml";

  /** The name of the plugin that lists the task changelogs. */
  public static final String PLUGIN_NAME   = "TaskManagementChangeLogsPlugin";

  /** Utility class: no instance. */
  private TaskChangelogs() {
  }

  /**
   * Reads the changelog paths the task configuration registers, in their order.
   *
   * @return the classpath paths of the changelog files
   * @throws Exception when the configuration cannot be read
   */
  public static List<String> registeredChangelogs() throws Exception {
    try (InputStream stream = TaskChangelogs.class.getClassLoader().getResourceAsStream(CONFIGURATION)) {
      if (stream == null) {
        throw new IllegalStateException(CONFIGURATION + " is not on the classpath");
      }
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      Document document = factory.newDocumentBuilder().parse(stream);
      NodeList plugins = document.getElementsByTagName("component-plugin");
      for (int i = 0; i < plugins.getLength(); i++) {
        Element plugin = (Element) plugins.item(i);
        if (PLUGIN_NAME.equals(text(plugin, "name"))) {
          NodeList values = plugin.getElementsByTagName("value");
          List<String> changelogs = new ArrayList<>();
          for (int j = 0; j < values.getLength(); j++) {
            changelogs.add(values.item(j).getTextContent().trim());
          }
          return changelogs;
        }
      }
      throw new IllegalStateException(PLUGIN_NAME + " is not declared in " + CONFIGURATION);
    }
  }

  /**
   * Applies every registered changelog, one Liquibase run per file.
   *
   * @param connection the database
   * @throws Exception when a changelog fails
   */
  public static void updateAll(Connection connection) throws Exception {
    for (String changelog : registeredChangelogs()) {
      update(connection, changelog);
    }
  }

  /**
   * Applies one changelog file.
   *
   * @param connection the database
   * @param changelog the classpath path of the file
   * @throws Exception when a changeset fails
   */
  public static void update(Connection connection, String changelog) throws Exception {
    liquibase(connection, changelog).update(new Contexts(), new LabelExpression());
    connection.setAutoCommit(true);
  }

  /**
   * Rolls back the last changesets of one changelog file.
   *
   * @param connection the database
   * @param changelog the classpath path of the file
   * @param count how many changesets to roll back
   * @throws Exception when a rollback fails
   */
  public static void rollback(Connection connection, String changelog, int count) throws Exception {
    liquibase(connection, changelog).rollback(count, new Contexts(), new LabelExpression());
    connection.setAutoCommit(true);
  }

  /**
   * Reads what Liquibase recorded for a changelog file.
   *
   * @param connection the database
   * @param changelog the classpath path of the file
   * @return the {@code EXECTYPE} of each recorded changeset, by id
   * @throws SQLException when the table cannot be read
   */
  public static Map<String, String> recorded(Connection connection, String changelog) throws SQLException {
    Map<String, String> recorded = new HashMap<>();
    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT ID, FILENAME, EXECTYPE FROM DATABASECHANGELOG")) {
      while (result.next()) {
        if (result.getString("FILENAME").endsWith(changelog)) {
          recorded.put(result.getString("ID"), result.getString("EXECTYPE"));
        }
      }
    }
    return recorded;
  }

  /**
   * Builds a Liquibase run on one file, as the platform does. Liquibase turns
   * the connection's auto-commit off, and a later run rolls back what is not
   * committed: so every run here ends by turning it on again, and a caller's
   * statements between two runs, a seed included, are committed as they run.
   *
   * @param connection the database
   * @param changelog the classpath path of the file
   * @return the Liquibase run
   * @throws Exception when the database is not recognised
   */
  private static Liquibase liquibase(Connection connection, String changelog) throws Exception {
    Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
    return new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
  }

  /**
   * Reads the text of an element's first child of a given name.
   *
   * @param parent the element
   * @param tag the child's name
   * @return its trimmed text, or {@code null} when absent
   */
  private static String text(Element parent, String tag) {
    NodeList children = parent.getElementsByTagName(tag);
    return children.getLength() == 0 ? null : children.item(0).getTextContent().trim();
  }
}
