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
package org.exoplatform.task.service.impl;

import java.util.ServiceLoader;

import org.exoplatform.task.domain.Task;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.ParserContext;
import org.exoplatform.task.service.TaskBuilder;
import org.exoplatform.task.service.TaskParserPlugin;
import org.exoplatform.task.service.TaskParser;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class TaskParserImpl implements TaskParser {
  ServiceLoader<TaskParserPlugin> parsers = ServiceLoader.load(TaskParserPlugin.class,
                                                               this.getClass().getClassLoader());

  @Override
  public TaskDto parse(String input, ParserContext context) {
    if(input == null) {
      return null;
    }

    TaskBuilder builder = new TaskBuilder();
    String in = input;
    for(TaskParserPlugin parser : parsers) {
      in = parser.parse(in, context, builder);
    }
    builder.withTitle(in);

    return builder.build();
  }
}
