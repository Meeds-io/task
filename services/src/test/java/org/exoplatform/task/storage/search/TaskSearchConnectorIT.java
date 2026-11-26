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
 * The task keyword search requests run on a real Elasticsearch (run with
 * {@code -Prun-its}): the index is created with the task mapping
 * ({@link TaskIndexingServiceConnector#getMapping()}) and the platform default
 * index settings, and the connector requests are sent as built, a refused
 * request failing the test.
 */
public class TaskSearchConnectorIT {

  private static final String           INDEX = "task_it";

  private static final HttpClient       HTTP  = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private static ElasticsearchContainer elasticsearch;

  private static String                 baseUrl;

  private static TaskSearchConnector    connector;

  /**
   * Starts Elasticsearch, creates the task index and indexes three tasks:
   * 1 « Renew Northwind support » and 2 « Upsell to Enterprise » for user1,
   * 3 « Secret Northwind deal » for user2 only.
   *
   * @throws Exception when Elasticsearch does not answer
   */
  @BeforeClass
  public static void startElasticsearch() throws Exception {
    elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.3.4").withEnv("xpack.security.enabled",
                                                                                                              "false")
                                                                                                     .withEnv("ES_JAVA_OPTS",
                                                                                                              "-Xms512m -Xmx512m")
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
   * The reserved characters of a keyword are ignored: the engine accepts the
   * request and searches the remaining words.
   */
  @Test
  public void aKeywordWithReservedCharactersIsAccepted() {
    assertEquals(Set.of(1L), ids("Renew\\"));
    assertEquals(Set.of(1L), ids("Northwind\\ support"));
  }

  /**
   * The operator words AND, OR and NOT of a keyword are not read as
   * operators: the engine accepts the request in every position and searches
   * the remaining words.
   */
  @Test
  public void aKeywordWithOperatorWordsIsAccepted() {
    for (String keyword : new String[] { "AND", "OR", "NOT" }) {
      assertEquals(keyword, Set.of(), ids(keyword));
    }
    for (String keyword : new String[] { "Renew AND", "Renew OR", "OR Renew", "Renew NOT", "NOT Renew", "Renew AND support" }) {
      assertEquals(keyword, Set.of(1L), ids(keyword));
    }
    assertEquals(Set.of(1L), ids("Northwind AND support"));
    assertEquals(Set.of(1L), ids("Renew\u3000AND"));
    assertEquals(Set.of(1L), ids("AND\u3000Renew"));
    assertEquals(Set.of(1L), ids("Renew AND\u3000"));
  }

  /**
   * The keyword is written as JSON text: quotes and control characters do not
   * break the request.
   */
  @Test
  public void aKeywordIsWrittenAsJsonText() {
    assertEquals(Set.of(1L), ids("\"Renew\" support"));
    assertEquals(Set.of(1L), ids("Renew\tsupport"));
    assertEquals(Set.of(1L), ids("Renew\nsupport"));
  }

  /**
   * The keyword is never read as part of the request template: it is searched
   * as text.
   */
  @Test
  public void aKeywordIsNeverReadAsTemplate() {
    assertEquals(Set.of(), ids("@permission_query@"));
    assertEquals(Set.of(), ids("@permission_query@ Northwind"));
  }

  /**
   * A sort by date is accepted whatever the direction: absent, ascending or
   * other text.
   */
  @Test
  public void aSortByDateIsAcceptedWhateverTheDirection() {
    for (String direction : new String[] { null, "asc", "desc", "sideways" }) {
      TaskSearchFilter filter = filter("Northwind");
      filter.setSortField("date");
      filter.setSortDirection(direction);
      assertEquals(direction, Set.of(1L), new HashSet<>(connector.search(filter)));
    }
  }

  private static Set<Long> ids(String keyword) {
    return new HashSet<>(connector.search(filter(keyword)));
  }

  /**
   * @param term the keyword
   * @return a search filter of user1, 10 results
   */
  private static TaskSearchFilter filter(String term) {
    return new TaskSearchFilter("user1", term, List.of("user1"), null, null, 0, 10);
  }

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
   * @return the answer body
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
      throw new IOException(method + " " + path + " answered " + response.statusCode() + ": " + response.body()
          + "\nrequest: " + body);
    }
    return response.body();
  }

  private static String readResource(String name) throws IOException {
    try (InputStream stream = TaskSearchConnectorIT.class.getClassLoader().getResourceAsStream(name)) {
      assertNotNull("the platform default index settings are on the test classpath (commons-search)", stream);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

}
