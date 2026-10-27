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

/**
 * A condition given as a Criteria predicate rather than as a field, an
 * operator and a value, for criteria a {@link SingleCondition} cannot express,
 * such as a correlated EXISTS subquery over another add-on's entity.
 * <p>
 * Added through {@link org.exoplatform.task.dao.Query#add(Condition)}, it is
 * combined with AND with the query's other conditions, including the project
 * restriction of {@link org.exoplatform.task.dao.TaskQuery#setProjectIds}, so it
 * can only narrow the result. Placed under an OR
 * {@link AggregateCondition}, it could widen a query instead: the builder must
 * return a restriction, and the caller keeps the condition out of any OR.
 *
 * @param <E> the entity of the DAO that runs the query
 */
public class PredicateCondition<E> extends Condition {

  /** Builds the predicate of each query that carries the condition. */
  private final PredicateBuilder<E> builder;

  /**
   * Creates the condition.
   *
   * @param builder builds the predicate for each query that carries the condition
   */
  public PredicateCondition(PredicateBuilder<E> builder) {
    if (builder == null) {
      throw new IllegalArgumentException("A predicate condition needs a predicate builder");
    }
    this.builder = builder;
  }

  /**
   * Returns the builder of the predicate.
   *
   * @return the builder given at creation
   */
  public PredicateBuilder<E> getBuilder() {
    return builder;
  }

}
