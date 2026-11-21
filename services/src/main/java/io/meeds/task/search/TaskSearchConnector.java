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

public class TaskSearchConnector {

  private static final Log       LOG = ExoLogger.getLogger(TaskSearchConnector.class);

  private static final String    SEARCH_QUERY                  = """
      {
        "from": @offset@,
        "size": @limit@,
        "query": {
          "bool": {
            "must": [
              @term_query@
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

  private static final String    PERMISSIONS_QUERY             = """
      {
        "terms": {
          "permissions": [@permissions@]
        }
      }
      """;

  private static final String    COUNT_QUERY                   = """
      {
        "query": {
          "bool": {
            "must": [
              @term_query@
              @permission_query@
            ]
          }
        }
      }
      """;

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

  private static final String    OFFSET_REPLACEMENT            = "@offset@";

  private static final String    LIMIT_REPLACEMENT             = "@limit@";

  private static final String    TERM_REPLACEMENT              = "@term@";

  private static final String    TERM_QUERY_REPLACEMENT        = "@term_query@";

  private static final String    PERMISSIONS_QUERY_REPLACEMENT = "@permission_query@";

  private static final String    PERMISSIONS_REPLACEMENT       = "@permissions@";

  private static final String    SORT_REPLACEMENT              = "@sortQuery@";

  private ElasticSearchingClient client;

  private String                 index;

  public TaskSearchConnector(ElasticSearchingClient client,
                             InitParams initParams) {
    this.client = client;
    this.index = initParams.getPropertiesParam("constructor.params").getProperty("index");
  }

  public List<Long> search(TaskSearchFilter filter) {
    String esQuery = buildSearchQuery(SEARCH_QUERY, filter);
    String jsonResponse = client.sendRequest(esQuery, index);
    return buildResult(jsonResponse);
  }

  public Long count(TaskSearchFilter filter) {
    filter.setLimit(0);
    filter.setOffset(0);
    String esQuery = buildSearchQuery(COUNT_QUERY, filter);
    String jsonResponse = client.countRequest(esQuery, index);
    return buildCount(jsonResponse);
  }

  private String buildSearchQuery(String queryBase, TaskSearchFilter filter) {
    String termQuery = "";
    if (StringUtils.isNotBlank(filter.getTerm())) {
      termQuery = SEARCH_QUERY_TERM.replace(TERM_REPLACEMENT, escapeTerm(filter.getTerm()));
    }

    String permissionQuery = "";
    if (CollectionUtils.isNotEmpty(filter.getPermissions())) {
      String permissions = "\"" + String.join("\",\"", filter.getPermissions()) + "\"";
      permissionQuery = PERMISSIONS_QUERY.replace(PERMISSIONS_REPLACEMENT, permissions);
    }

    String sortQuery = StringUtils.isNotBlank(filter.getSortField()) ? buildSortQuery(filter) : DEFAULT_SORTING_QUERY;

    // One pass: a value written into the template is never searched for another placeholder
    return StringUtils.replaceEach(queryBase,
                                   new String[] { OFFSET_REPLACEMENT, LIMIT_REPLACEMENT, TERM_QUERY_REPLACEMENT,
                                       PERMISSIONS_QUERY_REPLACEMENT, SORT_REPLACEMENT },
                                   new String[] { String.valueOf(filter.getOffset()),
                                       String.valueOf(Math.max(filter.getLimit(), 10)), termQuery, permissionQuery,
                                       sortQuery });
  }

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

  private Long parseLong(JSONObject hitSource, String key) {
    String value = (String) hitSource.get(key);
    return StringUtils.isBlank(value) ? null : Long.parseLong(value);
  }

  /**
   * Turns the typed term into the text of a query_string written in a JSON
   * string: the reserved characters, the backslash included, become spaces,
   * then the rest is escaped for JSON.
   *
   * @param text the typed term
   * @return the text to write between the quotes of "query"
   */
  private String escapeTerm(String text) {
    String stripped = text.replaceAll("[\\Q+-!():^[]\"{}~*?|&/\\E\\\\]", " ").trim();
    return JSONValue.escape(stripped);
  }

  private String buildSortQuery(TaskSearchFilter filter) {
    String sortFiled = filter.getSortField();
    String sortDirection = "asc".equalsIgnoreCase(filter.getSortDirection()) ? "asc" : "desc";
    return switch (sortFiled) {
    case "date" -> SORTING_QUERY.replace("@sortField@", "lastUpdatedDate").replace("@sortOrder@", sortDirection);
    default -> DEFAULT_SORTING_QUERY;
    };
  }
}
