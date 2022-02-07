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
package org.exoplatform.task.dao;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class OrderBy implements Cloneable {
  private final String fieldName;
  private final boolean ascending;

  public OrderBy(String fieldName, boolean ascending) {
    this.fieldName = fieldName;
    this.ascending = ascending;
  }

  public String getFieldName() {
    return fieldName;
  }

  public boolean isAscending() {
    return this.ascending;
  }

  public static class DESC extends OrderBy {
    public DESC(String propertyName) {
      super(propertyName, false);
    }
  }

  public static class ASC extends OrderBy {
    public ASC(String fieldName) {
      super(fieldName, true);
    }
  }

}
