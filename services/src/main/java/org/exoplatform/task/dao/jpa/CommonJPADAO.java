/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.dao.jpa;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.Query;
import org.exoplatform.task.dao.condition.AggregateCondition;
import org.exoplatform.task.dao.condition.Condition;
import org.exoplatform.task.dao.condition.SingleCondition;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public abstract class CommonJPADAO<E, K extends Serializable> extends GenericDAOJPAImpl<E, K> {  
  
  public Map<String, Class> clz = new ConcurrentHashMap<String, Class>();
  
  protected <E> List<E> cloneEntities(List<E> list) {
    if (list == null || list.isEmpty()) return list;

    List<E> result = new ArrayList<E>(list.size());
    for (E e : list) {
      E cloned = cloneEntity(e);
      result.add(cloned);
    }
    return result;
  }

  protected <E> E cloneEntity(E e) {
    return DAOHandlerJPAImpl.clone(e);
  }

  @Override
  public E find(K id) {
    return cloneEntity(super.find(id));
  }

  @Override
  public E create(E entity) {
    return cloneEntity(super.create(entity));
  }

  protected ListAccess<E> findEntities(Query query, Class<E> clazz) {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(new ClassLoader(cl) {
        @SuppressWarnings("rawtypes")
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
          Class c = getCache().get(name);
          if (c == null) {
            try {
              c = cl.loadClass(name);
              getCache().put(name, c);
            } catch (Exception e) {
              getCache().put(name, CommentDAOImpl.class);
            }
          }
          if (c.getName().equals(CommentDAOImpl.class.getName())) {
            return null;
          }
          return c;
        }
      });
      EntityManager em = getEntityManager();
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery q = cb.createQuery();
      q.distinct(true);
      Root<E> root = q.from(clazz);
      
      Predicate predicate = buildQuery(query.getCondition(), root, cb, q);
      if (predicate != null) {
        q.where(predicate);
      }
      
      //
      q.select(cb.countDistinct(root));
      final TypedQuery<Long> countQuery = em.createQuery(q);

      // Some RDBMS only allow sort by selected field
      // So we need multi-selection here to add the selectCase into selection in case we need the null value at last of results
      List<Selection> selections = new ArrayList<>();
      selections.add(root);
      
      List<OrderBy> orderby = query.getOrderBy();
      if(orderby != null && !orderby.isEmpty()) {
        List<Order> orders = new ArrayList<Order>(orderby.size());
        for(OrderBy orderBy : orderby) {
          Expression p = root.get(orderBy.getFieldName());
          Order order;
          if (orderBy.isAscending()) {
            // NULL value should be at last when order by
            Expression nullCase = cb.selectCase().when(p.isNull(), 1).otherwise(0);
            selections.add(nullCase);
            orders.add(cb.asc(nullCase));

            order = cb.asc(p);
          } else {
            order = cb.desc(p);
          }
          orders.add(order);
        }
        q.orderBy(orders);
      }

      //
      q.multiselect(selections).distinct(true);
      
      TypedQuery<E> selectQuery = em.createQuery(q);      
      return new JPAQueryListAccess<E>(clazz, countQuery, selectQuery);
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }
  
  protected Predicate buildQuery(Condition condition, Root<E> root, CriteriaBuilder cb, CriteriaQuery query) {
    if (condition == null) {
      return null;
    }
    if (condition instanceof SingleCondition) {
      return buildSingleCondition((SingleCondition)condition, root, cb, query);
    } else if (condition instanceof AggregateCondition) {
      AggregateCondition agg = (AggregateCondition)condition;
      String type = agg.getType();
      List<Condition> cds = agg.getConditions();
      Predicate[] ps = new Predicate[cds.size()];
      for (int i = 0; i < ps.length; i++) {
        ps[i] = buildQuery(cds.get(i), root, cb, query);
      }

      if (ps.length == 1) {
        return ps[0];
      }

      if (AggregateCondition.AND.equals(type)) {
        return cb.and(ps);
      } else if (AggregateCondition.OR.equals(type)) {
        return cb.or(ps);
      }
    }
    return null;
  }

  protected <T> Predicate buildSingleCondition(SingleCondition<T> condition, Root<E> root, CriteriaBuilder cb, CriteriaQuery query) {
    String type = condition.getType();
    T value = condition.getValue();

    Path path = buildPath(condition, root);

    if (SingleCondition.EQ.equals(condition.getType())) {
      return cb.equal(path, value);
    } else if (SingleCondition.LT.equals(condition.getType())) {
      return cb.lessThan((Path<Comparable>) path, (Comparable) value);
    } else if (SingleCondition.GT.equals(condition.getType())) {
      return cb.greaterThan((Path<Comparable>) path, (Comparable) value);
    } else if (SingleCondition.LTE.equals(condition.getType())) {
      return cb.lessThanOrEqualTo((Path<Comparable>)path, (Comparable)value);
    } else if (SingleCondition.GTE.equals(condition.getType())) {
      return cb.greaterThanOrEqualTo((Path<Comparable>)path, (Comparable) value);
    } else if (SingleCondition.IS_NULL.equals(type)) {
      return path.isNull();
    } else if (SingleCondition.NOT_NULL.equals(type)) {
      return path.isNotNull();
    } else if (SingleCondition.IS_EMPTY.equals(type)) {
        return cb.isEmpty(path);
    } else if (SingleCondition.LIKE.equals(type)) {
      return cb.like(cb.lower(path), String.valueOf(value));
    } else if (SingleCondition.IN.equals(type)) {
      return path.in((Collection) value);
    } else if (SingleCondition.IS_TRUE.equals(type)) {
      return cb.isTrue(path);
    } else if (SingleCondition.IS_FALSE.equals(type)) {
      return cb.isFalse(path);
    }

    throw new RuntimeException("Condition type " + type + " is not supported");
  }

  protected Path buildPath(SingleCondition condition, Root<E> root) {
    String field = condition.getField();
    
    Join join = null;
    if (field.indexOf('.') > 0) {
      String[] arr = field.split("\\.");
      for (int i = 0; i < arr.length - 1; i++) {
        String s = arr[i];
        if (join == null) {
          join = root.join(s, JoinType.INNER);
        } else {
          join = join.join(s, JoinType.INNER);
        }
      }
      field = arr[arr.length - 1];
    }    
    
    return join == null ? root.get(field) : join.get(field);
  }
  
  public Map<String, Class> getCache() {
    return clz;
  }     
}
