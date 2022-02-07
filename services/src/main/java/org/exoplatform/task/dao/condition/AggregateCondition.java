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
package org.exoplatform.task.dao.condition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class AggregateCondition extends Condition implements Cloneable {
  public static final String AND = "and";
  public static final String OR = "or";

  final String type;
  List<Condition> conditions;

  public AggregateCondition(String type, List<Condition> conditions) {
    this.type = type;
    this.conditions = conditions;
  }

  public String getType() {
    return type;
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public AggregateCondition add(Condition cond) {
    this.conditions.add(cond);
    return this;
  }

  public AggregateCondition clone() {
    List<Condition> c1 = new ArrayList<Condition>();
    for (Condition condition : conditions) {
      if (condition != null) {
        c1.add(condition.clone());
      }
    }
    return new AggregateCondition(type, c1);
  }
}
