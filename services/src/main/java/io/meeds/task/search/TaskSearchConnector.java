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
package io.meeds.task.search;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.model.TaskSearchFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TaskSearchConnector {

  /** The property of {@code constructor.params} holding the contributed ids' boost. */
  public static final String     CONTRIBUTED_BOOST_PROPERTY    = "contributedBoost";

  /** The score a contributed task gets when the property is unset or invalid. */
  public static final double     DEFAULT_CONTRIBUTED_BOOST     = 5.0d;

  /** The smallest boost accepted for contributed tasks. */
  public static final double     MIN_CONTRIBUTED_BOOST         = 0.1d;

  /** The largest boost accepted for contributed tasks. */
  public static final double     MAX_CONTRIBUTED_BOOST         = 100d;

  public static final String     DEFAULT_SORTING_QUERY         = """
    {
      "_score": {
        "order": "desc"
      }
    }
    """;

  public static final String     SORTING_QUERY                 = """
    {
      "@sortField@": {
        "order": "@sortOrder@"
      }
    },
    "_score"
    """;

  private static final Log       LOG = ExoLogger.getLogger(TaskSearchConnector.class);

  private static final String    SEARCH_QUERY                  = """
      {
        "from": @offset@,
        "size": @limit@,
        "query": {
          "bool": {
            "must": [
              @term_query@
              @project_query@
              @permission_query@
            ]
          }
        },
        "sort": [
          @sortQuery@
        ]
      }
      """;

  private static final String    SEARCH_QUERY_TERM             = """
      {
        "query_string": {
          "fields": ["title", "description"],
          "default_operator": "AND",
          "query": "@term@"
        }
      },
      """;

  /**
   * The term clause when another add-on contributed tasks for the term: a
   * task matches by its title or description, or by being contributed. The
   * clause stays in the {@code must}, beside the projects and the
   * permissions, so a contributed task passes them too.
   */
  private static final String    SEARCH_QUERY_TERM_CONTRIBUTED = """
      {
        "bool": {
          "should": [
            {
              "query_string": {
                "fields": ["title", "description"],
                "default_operator": "AND",
                "query": "@term@"
              }
            },
            {
              "ids": {
                "values": [@ids@],
                "boost": @boost@
              }
            }
          ],
          "minimum_should_match": 1
        }
      },
      """;

  private static final String    PERMISSIONS_QUERY             = """
      {
        "terms": {
          "permissions": [@permissions@]
        }
      }
      """;

  /**
   * The projects of the tasks found. A task document carries its project
   * identifier as a string in {@code projectId}, a field the index maps
   * dynamically (as text with a keyword sub-field): a number stays one token,
   * so a terms query on the field itself matches it whether the field is
   * mapped as text or as a keyword.
   */
  private static final String    PROJECTS_QUERY                = """
      {
        "terms": {
          "projectId": [@projectIds@]
        }
      },
      """;

  private static final String    COUNT_QUERY                   = """
      {
        "query": {
          "bool": {
            "must": [
              @term_query@
              @project_query@
              @permission_query@
            ]
          }
        }
      }
      """;

  private static final String    OFFSET_REPLACEMENT            = "@offset@";

  private static final String    LIMIT_REPLACEMENT             = "@limit@";

  private static final String    TERM_REPLACEMENT              = "@term@";

  private static final String    TERM_QUERY_REPLACEMENT        = "@term_query@";

  private static final String    IDS_REPLACEMENT               = "@ids@";

  private static final String    BOOST_REPLACEMENT             = "@boost@";

  private static final String    PERMISSIONS_QUERY_REPLACEMENT = "@permission_query@";

  private static final String    PERMISSIONS_REPLACEMENT       = "@permissions@";

  private static final String    PROJECTS_QUERY_REPLACEMENT    = "@project_query@";

  private static final String    PROJECTS_REPLACEMENT          = "@projectIds@";

  private static final String    SORT_REPLACEMENT              = "@sortQuery@";

  private ElasticSearchingClient client;

  private String                 index;

  private double                 contributedBoost;

  /**
   * The connector of the task index.
   *
   * @param client the Elasticsearch client
   * @param initParams {@code constructor.params}: {@code index}, and
   *          optionally {@code contributedBoost}
   */
  public TaskSearchConnector(ElasticSearchingClient client,
                             InitParams initParams) {
    this.client = client;
    PropertiesParam params = initParams.getPropertiesParam("constructor.params");
    this.index = params.getProperty("index");
    this.contributedBoost = parseContributedBoost(params.getProperty(CONTRIBUTED_BOOST_PROPERTY));
  }

  /**
   * Finds the ids of the tasks a filter matches, best first.
   *
   * @param filter the filter
   * @return the task ids, in the order of the search
   */
  public List<Long> search(TaskSearchFilter filter) {
    String esQuery = buildSearchQuery(SEARCH_QUERY, filter);
    String jsonResponse = client.sendRequest(esQuery, index);
    return buildResult(jsonResponse);
  }

  /**
   * Counts the tasks a filter matches. The filter's offset and limit are set
   * to 0.
   *
   * @param filter the filter
   * @return the number of tasks
   */
  public Long count(TaskSearchFilter filter) {
    filter.setLimit(0);
    filter.setOffset(0);
    String esQuery = buildSearchQuery(COUNT_QUERY, filter);
    String jsonResponse = client.countRequest(esQuery, index);
    return buildCount(jsonResponse);
  }

  /**
   * The boost a contributed task gets.
   *
   * @return the boost in use
   */
  public double getContributedBoost() {
    return contributedBoost;
  }

  /**
   * Builds the search or count request of a filter: its term in the title and
   * the description, or among the contributed tasks, its projects when it
   * names some, and the permissions that bound what the user can find. The
   * template is filled in one pass, so a value written into it, the typed
   * term included, is never searched for another placeholder.
   *
   * @param queryBase the search or the count template
   * @param filter the filter
   * @return the request
   */
  String buildSearchQuery(String queryBase, TaskSearchFilter filter) {
    String termQuery = buildTermQuery(filter);

    String projectQuery = "";
    if (CollectionUtils.isNotEmpty(filter.getProjectIds())) {
      String projectIds = filter.getProjectIds()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(projectId -> "\"" + projectId + "\"")
                                .collect(Collectors.joining(","));
      projectQuery = PROJECTS_QUERY.replace(PROJECTS_REPLACEMENT, projectIds);
    }

    String permissionQuery = "";
    if (CollectionUtils.isNotEmpty(filter.getPermissions())) {
      String permissions = filter.getPermissions()
                                 .stream()
                                 .map(permission -> "\"" + JSONValue.escape(permission) + "\"")
                                 .collect(Collectors.joining(","));
      permissionQuery = PERMISSIONS_QUERY.replace(PERMISSIONS_REPLACEMENT, permissions);
    }

    String sortQuery = StringUtils.isNotBlank(filter.getSortField()) ? buildSortQuery(filter) : DEFAULT_SORTING_QUERY;

    return StringUtils.replaceEach(queryBase,
                                   new String[] { OFFSET_REPLACEMENT, LIMIT_REPLACEMENT, TERM_QUERY_REPLACEMENT,
                                       PROJECTS_QUERY_REPLACEMENT, PERMISSIONS_QUERY_REPLACEMENT, SORT_REPLACEMENT },
                                   new String[] { String.valueOf(filter.getOffset()),
                                       String.valueOf(Math.max(filter.getLimit(), 10)), termQuery, projectQuery,
                                       permissionQuery, sortQuery });
  }

  /**
   * Builds the term clause: the term in the title and the description, and,
   * when another add-on contributed tasks, those tasks as an alternative,
   * scored by the contributed boost.
   *
   * @param filter the filter
   * @return the clause, followed by a comma, or empty when the filter has no
   *         term
   */
  private String buildTermQuery(TaskSearchFilter filter) {
    if (StringUtils.isBlank(filter.getTerm())) {
      return "";
    }
    String term = escapeTerm(filter.getTerm());
    List<Long> contributedIds = filter.getContributedIds() == null ? List.of()
                                                                    : filter.getContributedIds()
                                                                            .stream()
                                                                            .filter(Objects::nonNull)
                                                                            .distinct()
                                                                            .toList();
    if (contributedIds.isEmpty()) {
      return SEARCH_QUERY_TERM.replace(TERM_REPLACEMENT, term);
    }
    String ids = contributedIds.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
    return StringUtils.replaceEach(SEARCH_QUERY_TERM_CONTRIBUTED,
                                   new String[] { TERM_REPLACEMENT, IDS_REPLACEMENT, BOOST_REPLACEMENT },
                                   new String[] { term, ids, String.valueOf(contributedBoost) });
  }

  /**
   * Reads the ids of a search answer.
   *
   * @param jsonResponse the answer
   * @return the ids, in the order of the hits
   */
  @SuppressWarnings("rawtypes")
  private List<Long> buildResult(String jsonResponse) {
    JSONParser parser = new JSONParser();
    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject hitsObj = (JSONObject) json.get("hits");
    if (hitsObj == null) {
      return Collections.emptyList();
    }

    JSONArray hits = (JSONArray) hitsObj.get("hits");
    List<Long> result = new ArrayList<>();
    for (Object hit : hits) {
      try {
        JSONObject hitObj = (JSONObject) hit;
        result.add(parseLong(hitObj, "_id"));
      } catch (Exception e) {
        LOG.warn("Error parsing hit item: {}", hit, e);
      }
    }
    return result;
  }

  /**
   * Reads the count of a count answer.
   *
   * @param jsonResponse the answer
   * @return the count
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private long buildCount(String jsonResponse) {
    JSONParser parser = new JSONParser();
    try {
      Map json = (Map) parser.parse(jsonResponse);
      String countString = json.getOrDefault("count", "0").toString();
      return Long.parseLong(countString);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }
  }

  /**
   * Reads a hit's string value as a number.
   *
   * @param hitSource the hit
   * @param key the key
   * @return the number, null when blank
   */
  private Long parseLong(JSONObject hitSource, String key) {
    String value = (String) hitSource.get(key);
    return StringUtils.isBlank(value) ? null : Long.parseLong(value);
  }

  /**
   * Turns the typed term into the text of a {@code query_string} written in a
   * JSON string: the {@code query_string} reserved characters, the backslash
   * included (the parser reads a kept one as its own escape), become spaces,
   * then the rest is escaped for JSON (control characters such as a tab).
   *
   * @param text the typed term
   * @return the text to write between the quotes of {@code "query"}
   */
  String escapeTerm(String text) {
    String stripped = text.replaceAll("[\\Q+-!():^[]\"{}~*?|&/\\E\\\\]", " ").trim();
    return JSONValue.escape(stripped);
  }

  /**
   * Builds the sort of a filter that names a sort field: the date of last
   * update, in the filter's direction, ascending or descending only.
   *
   * @param filter the filter
   * @return the sort clause
   */
  private String buildSortQuery(TaskSearchFilter filter) {
    String sortFiled = filter.getSortField();
    String sortDirection = "asc".equalsIgnoreCase(filter.getSortDirection()) ? "asc" : "desc";
    return switch (sortFiled) {
    case "date" -> SORTING_QUERY.replace("@sortField@", "lastUpdatedDate").replace("@sortOrder@", sortDirection);
    default -> DEFAULT_SORTING_QUERY;
    };
  }

  /**
   * Reads the contributed tasks' boost: a number from
   * {@link #MIN_CONTRIBUTED_BOOST} to {@link #MAX_CONTRIBUTED_BOOST}; an unset
   * value gives the default, and an invalid one the default and a warning.
   *
   * @param value the property's value
   * @return the boost
   */
  static double parseContributedBoost(String value) {
    if (StringUtils.isBlank(value)) {
      return DEFAULT_CONTRIBUTED_BOOST;
    }
    try {
      double boost = Double.parseDouble(value.trim());
      if (boost >= MIN_CONTRIBUTED_BOOST && boost <= MAX_CONTRIBUTED_BOOST) {
        return boost;
      }
    } catch (NumberFormatException e) {
      // falls through to the warning
    }
    LOG.warn("Property task.search.contributed.boost '{}' is not a number from {} to {}, the default {} is used",
             value,
             MIN_CONTRIBUTED_BOOST,
             MAX_CONTRIBUTED_BOOST,
             DEFAULT_CONTRIBUTED_BOOST);
    return DEFAULT_CONTRIBUTED_BOOST;
  }
}
