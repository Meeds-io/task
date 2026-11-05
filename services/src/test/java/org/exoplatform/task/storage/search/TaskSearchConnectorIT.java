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
package org.exoplatform.task.storage.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.task.model.TaskSearchFilter;

import io.meeds.task.search.TaskSearchConnector;

/**
 * The task keyword search's requests run on a real Elasticsearch (run with
 * {@code -Prun-its}): the index is created with the task mapping
 * ({@link TaskIndexingServiceConnector#getMapping()}) and the platform's
 * default index settings (commons {@code es-default-index-settings.json}),
 * and the connector's requests are sent as built, a refused request failing
 * the test. A mock cannot tell a query the engine refuses, nor whether a
 * contributed id outside the permissions comes back.
 */
public class TaskSearchConnectorIT {

  private static final String           INDEX = "task_it";

  private static final HttpClient       HTTP  = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private static ElasticsearchContainer elasticsearch;

  private static String                 baseUrl;

  private static TaskSearchConnector    connector;

  /**
   * Starts Elasticsearch, creates the task index and indexes four tasks:
   * 1 « Renew Northwind support » and 2 « Upsell to Enterprise » for user1,
   * 3 « Secret Northwind deal » for user2 only, 4 « Northwind renewal call »
   * for the sales space's members.
   *
   * @throws Exception when Elasticsearch does not answer
   */
  @BeforeClass
  public static void startElasticsearch() throws Exception {
    elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.4.5").withEnv("xpack.security.enabled", "false")
                                                                                                     .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                                                                                                     .withStartupTimeout(Duration.ofMinutes(3));
    elasticsearch.start();
    baseUrl = "http://" + elasticsearch.getHttpHostAddress();

    String settings = readResource("es-default-index-settings.json").replace("shard.number", "1")
                                                                     .replace("replica.number", "0")
                                                                     .replace("max_regex.length", "1000");
    String mapping = mock(TaskIndexingServiceConnector.class, CALLS_REAL_METHODS).getMapping();
    send("PUT", "/" + INDEX, "{\"settings\":" + settings + ",\"mappings\":" + mapping + "}");
    index(1, "Renew Northwind support", "user1");
    index(2, "Upsell to Enterprise", "user1");
    index(3, "Secret Northwind deal", "user2");
    index(4, "Northwind renewal call", "member:/spaces/sales");
    send("POST", "/" + INDEX + "/_refresh", "");

    ElasticSearchingClient client = mock(ElasticSearchingClient.class);
    when(client.sendRequest(anyString(), anyString())).thenAnswer(call -> send("POST",
                                                                                 "/" + INDEX + "/_search",
                                                                                 call.getArgument(0)));
    when(client.countRequest(anyString(), anyString())).thenAnswer(call -> send("POST",
                                                                                  "/" + INDEX + "/_count",
                                                                                  call.getArgument(0)));
    InitParams initParams = mock(InitParams.class);
    PropertiesParam params = mock(PropertiesParam.class);
    when(initParams.getPropertiesParam("constructor.params")).thenReturn(params);
    when(params.getProperty("index")).thenReturn(INDEX);
    connector = new TaskSearchConnector(client, initParams);
  }

  /**
   * Stops Elasticsearch.
   */
  @AfterClass
  public static void stopElasticsearch() {
    if (elasticsearch != null) {
      elasticsearch.stop();
    }
  }

  /**
   * Without contributed ids, the term finds the viewer's task only.
   */
  @Test
  public void testTermOnly() {
    assertEquals(Set.of(1L), new HashSet<>(connector.search(filter("Northwind", List.of("user1"), List.of()))));
  }

  /**
   * Contributed ids add the viewer's own tasks to the term's, and never a
   * task outside the permissions (3 is user2's), in the page and the count.
   * Mutant: the ids clause outside the {@code must}.
   */
  @Test
  public void testTermAndContributedIdsWithinThePermissions() {
    assertEquals(Set.of(1L, 2L), new HashSet<>(connector.search(filter("Northwind", List.of("user1"), List.of(2L, 3L)))));
    assertEquals(Long.valueOf(2), connector.count(filter("Northwind", List.of("user1"), List.of(2L, 3L))));
  }

  /**
   * A contributed task is found by its id alone, the term matching nothing,
   * through a membership too.
   */
  @Test
  public void testContributedIdsAlone() {
    assertEquals(List.of(2L), connector.search(filter("Zzyzx", List.of("user1"), List.of(2L, 3L))));
    assertEquals(List.of(4L),
                 connector.search(filter("Zzyzx", List.of("user1", "member:/spaces/sales"), List.of(4L, 3L))));
  }

  /**
   * Terms holding a backslash (inside or last), a double quote, a tab, a new
   * line or a template placeholder give requests the engine accepts, which
   * still never return a task outside the permissions. Mutants: the backslash
   * kept (« foo\ » fails to parse), the JSON escape removed (the tab breaks
   * the body).
   */
  @Test
  public void testTermsTheEngineMustAccept() {
    for (String term : List.of("foo\\", "a\\ b", "say \"Northwind\"", "a\tNorthwind", "ligne\nNorthwind", "@permission_query@ Northwind")) {
      List<Long> found = connector.search(filter(term, List.of("user1"), List.of(3L)));
      assertTrue(term, !found.contains(3L));
    }
    assertEquals(Set.of(1L), new HashSet<>(connector.search(filter("\"Northwind\"", List.of("user1"), List.of()))));
  }

  /**
   * A sort by date with contributed ids is accepted, whatever direction the
   * client sends.
   */
  @Test
  public void testSortByDateWithContributedIds() {
    TaskSearchFilter filter = filter("Northwind", List.of("user1"), List.of(2L));
    filter.setSortField("date");
    filter.setSortDirection("asc\"}},{\"x\":\"");
    assertEquals(Set.of(1L, 2L), new HashSet<>(connector.search(filter)));
  }

  /**
   * @param term the term
   * @param permissions the viewer's user and memberships
   * @param contributedIds the contributed task ids
   * @return a search filter of 10 results
   */
  private static TaskSearchFilter filter(String term, List<String> permissions, List<Long> contributedIds) {
    TaskSearchFilter filter = new TaskSearchFilter("user1", term, permissions, null, null, 0, 10);
    filter.setContributedIds(contributedIds);
    return filter;
  }

  /**
   * Indexes one task document as the task indexing connector shapes it.
   *
   * @param id the task id
   * @param title its title
   * @param permission the one permission it carries
   * @throws IOException when Elasticsearch refuses it
   * @throws InterruptedException when interrupted
   */
  private static void index(long id, String title, String permission) throws IOException, InterruptedException {
    send("PUT",
         "/" + INDEX + "/_doc/" + id,
         "{\"id\":\"" + id + "\",\"title\":\"" + title + "\",\"description\":\"\",\"permissions\":[\"" + permission
             + "\"],\"lastUpdatedDate\":" + (1_700_000_000_000L + id) + "}");
  }

  /**
   * Sends a request to Elasticsearch.
   *
   * @param method the HTTP method
   * @param path the path
   * @param body the body, empty for none
   * @return the answer's body
   * @throws IOException when the engine answers anything but 2xx, its answer
   *           in the message
   * @throws InterruptedException when interrupted
   */
  private static String send(String method, String path, String body) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                                     .timeout(Duration.ofSeconds(30))
                                     .header("Content-Type", "application/json")
                                     .method(method,
                                             body.isEmpty() ? HttpRequest.BodyPublishers.noBody()
                                                            : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                     .build();
    HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() / 100 != 2) {
      throw new IOException(method + " " + path + " answered " + response.statusCode() + ": " + response.body() + "\nrequest: " + body);
    }
    return response.body();
  }

  /**
   * @param name a classpath resource
   * @return its text
   * @throws IOException when it cannot be read
   */
  private static String readResource(String name) throws IOException {
    try (InputStream stream = TaskSearchConnectorIT.class.getClassLoader().getResourceAsStream(name)) {
      assertNotNull("the platform's default index settings are on the test classpath (commons-search)", stream);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

}
