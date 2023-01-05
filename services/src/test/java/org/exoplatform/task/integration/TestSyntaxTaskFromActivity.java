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
package org.exoplatform.task.integration;

import org.exoplatform.task.dto.TaskDto;
import org.junit.Assert;
import org.junit.Test;

public class TestSyntaxTaskFromActivity {

  @Test
  public void testExtractTaskInfo() {
    ActivityTaskCreationListener processor = new ActivityTaskCreationListener(null, null, null, null, null, null, null, null);

    String html = "we need to ++download new documentation from cloud<br>" +
        "It helps you understand better what's happening";
    TaskDto taskInfo = processor.extractTaskInfo(html);
    Assert.assertEquals("download new documentation from cloud", taskInfo.getTitle());
    Assert.assertEquals("It helps you understand better what's happening", taskInfo.getDescription());

    html = "we need to ++download new <strong>documentation from <i>cloud<br>" +
        "It helps you</i> understand</strong> better what's happening";
    taskInfo = processor.extractTaskInfo(html);
    Assert.assertEquals("download new documentation from cloud", taskInfo.getTitle());
    Assert.assertEquals("It helps you</i> understand</strong> better what's happening", taskInfo.getDescription());

    html = "we need to ++download new <strong>documentation from cloud<br>" +
        "It helps you understand</strong> better what's happening";
    taskInfo = processor.extractTaskInfo(html);
    Assert.assertEquals("download new documentation from cloud", taskInfo.getTitle());
    Assert.assertEquals("It helps you understand</strong> better what's happening", taskInfo.getDescription());
  }

  @Test
  public void testSubstituteTask() {
    ActivityTaskProcessor processor = new ActivityTaskProcessor(null, null, null, null);
    String taskURL = "/link/to/task";

    String html = "we need to ++download new documentation from cloud<br>" +
        "It helps you understand better what's happening";
    String expected = "we need to <a href=\"/link/to/task\">++download new documentation from cloud</a><br>" +
        "It helps you understand better what's happening";
    Assert.assertEquals(expected, processor.substituteTask(taskURL, html));

    html = "we need to ++download new <strong>documentation from cloud<br>" +
        "It helps you understand</strong> better what's happening";
    expected = "we need to <a href=\"/link/to/task\">++download new </a><strong>documentation from cloud<br>" +
        "It helps you understand</strong> better what's happening";
    Assert.assertEquals(expected, processor.substituteTask(taskURL, html));
  }
}