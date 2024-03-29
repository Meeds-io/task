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
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class Conditions {
  public static String ID = "id";
  public static String TASK_TITLE = "title";
  public static String TASK_DES = "description";
  public static String TASK_PRIORITY = "priority";
  public static String TASK_ASSIGNEE = "assignee";
  public static String TASK_COWORKER = "coworker";
  public static String TASK_WATCHER = "watcher";
  public static String TASK_CREATOR = "createdBy";
  public static String TASK_STATUS = "status";
  public static String TASK_STATUS_NAME = "status.name";
  public static String TASK_DUEDATE = "dueDate";
  public static String TASK_PROJECT = "status.project.id";
  public static String TASK_LABEL_USERNAME = "lblMapping.label.username";
  public static String TASK_LABEL_ID = "lblMapping.label.id";
  public static String TASK_COMPLETED = "completed";
  public static String TASK_START_DATE = "startDate";
  public static String TASK_END_DATE = "endDate";
  public static String TASK_MANAGER = "status.project.manager";
  public static String TASK_PARTICIPATOR = "status.project.participator";
  public static String TASK_MENTIONED_USERS = "mentionedUsers";

  public static final String MANAGER = "manager";
  public static final String PARTICIPATOR = "participator";
  public static final String NAME = "name";
  public static final String PARENT = "parent";
  public static final String PARENT_ID = "parent.id";
  public static final String USERNAME = "username";  
  public static final String LABEL_TASK_ID = "lblMapping.task.id";

  public static <T> SingleCondition<T> eq(String fieldName, T value) {
    return new SingleCondition<T>(SingleCondition.EQ, fieldName, value);
  }
  public static <T> SingleCondition<T> like(String fieldName, T value) {
    return new SingleCondition<T>(SingleCondition.LIKE, fieldName, value);
  }
  public static <T> SingleCondition<T> gt(String fieldName, T value) {
    return new SingleCondition<T>(SingleCondition.GT, fieldName, value);
  }
  public static <T> SingleCondition<T> lt(String fieldName, T value) {
    return new SingleCondition<T>(SingleCondition.LT, fieldName, value);
  }
  public static <T> SingleCondition<T> gte(String fieldName, T value) {
    return new SingleCondition<T>(SingleCondition.GTE, fieldName, value);
  }
  public static <T> SingleCondition<T> lte(String fieldName, T value) {
    return new SingleCondition<T>(SingleCondition.LTE, fieldName, value);
  }
  public static <T> SingleCondition<T> isNull(String fieldName) {
    return new SingleCondition<T>(SingleCondition.IS_NULL, fieldName, null);
  }
  public static <T> SingleCondition<T> isEmpty(String fieldName) {
    return new SingleCondition<T>(SingleCondition.IS_EMPTY, fieldName, null);
  }
  public static <T> SingleCondition<T> notNull(String fieldName) {
    return new SingleCondition<T>(SingleCondition.NOT_NULL, fieldName, null);
  }
  public static <T> SingleCondition<List<T>> in(String fieldName, List<T> values) {
    return new SingleCondition<List<T>>(SingleCondition.IN, fieldName, values);
  }
  public static <T> SingleCondition<T> isTrue(String fieldName) {
    return new SingleCondition<T>(SingleCondition.IS_TRUE, fieldName, null);
  }
  public static <T> SingleCondition<T> isFalse(String fieldName) {
    return new SingleCondition<T>(SingleCondition.IS_FALSE, fieldName, null);
  }

  public static AggregateCondition and(Condition... cond) {
    return new AggregateCondition(AggregateCondition.AND, new ArrayList<Condition>(Arrays.asList(cond)));
  }
  public static AggregateCondition or(Condition... cond) {
    return new AggregateCondition(AggregateCondition.OR, new ArrayList<Condition>(Arrays.asList(cond)));
  }
}
