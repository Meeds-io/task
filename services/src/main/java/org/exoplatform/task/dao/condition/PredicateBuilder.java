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
package org.exoplatform.task.dao.condition;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Builds a JPA Criteria {@link Predicate} over the root of the query a task
 * DAO is running, for a {@link PredicateCondition}.
 *
 * @param <E> the entity of the DAO that runs the query
 */
@FunctionalInterface
public interface PredicateBuilder<E> {

  /**
   * Builds the predicate for one query. It is called once for the select query
   * and once for the count query of the same {@link org.exoplatform.task.dao.Query},
   * each time with that query's own root, so the predicate must be built from
   * the arguments only, never from a root or a query kept from an earlier call.
   *
   * @param cb the criteria builder of the entity manager running the query
   * @param query the criteria query being built, to create correlated subqueries
   * @param root the root of that query
   * @return the predicate, which restricts the entities the query returns
   */
  Predicate build(CriteriaBuilder cb, CriteriaQuery<?> query, Root<E> root);

}
