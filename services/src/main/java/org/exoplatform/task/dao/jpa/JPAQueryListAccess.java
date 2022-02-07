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

import java.lang.reflect.Array;
import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.commons.utils.ListAccess;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class JPAQueryListAccess<E> implements ListAccess<E> {
  private final Class<E> clazz;
  private final TypedQuery<Long> countQuery;
  private final TypedQuery<E> selectQuery;

  private long size = -1;

  public JPAQueryListAccess(Class<E> clazz, TypedQuery<Long> countQuery, TypedQuery<E> selectQuery) {
    this.clazz = clazz;
    this.countQuery = countQuery;
    this.selectQuery = selectQuery;
  }

  @Override
  public E[] load(int index, int length) throws Exception, IllegalArgumentException {
    if (length > 0) {
      selectQuery.setFirstResult(index).setMaxResults(length);
    } else {
      // Load all
      selectQuery.setFirstResult(0).setMaxResults(Integer.MAX_VALUE);
    }
    List<E> list = selectQuery.getResultList();

    E[] e = (E[])Array.newInstance(clazz, list.size());
    for (int i = 0; i < e.length; i++) {
      Object obj = list.get(i);
      E entity = null;
      if (clazz.isInstance(obj)) {
        entity = (E)obj;
      } else if (obj instanceof Object[]){
        for (Object o : (Object[])obj) {
          if (clazz.isInstance(o)) {
            entity = (E)o;
            break;
          }
        }
      }

      E clone = DAOHandlerJPAImpl.clone(entity);
      e[i] = clone != null ? clone : entity;
    }
    return e;
  }

  @Override
  public int getSize() throws Exception {
    if (size == -1) {
      size = countQuery.getSingleResult();
    }
    return (int)size;
  }
}
