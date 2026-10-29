/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.task.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.exoplatform.tasks.listener.analytics.TaskCommentListener;
import org.exoplatform.tasks.listener.analytics.TaskSavedListener;

import tools.jackson.databind.ObjectMapper;

/**
 * The collaborative activity declaration analytics reads from this jar: the
 * operations TaskSavedListener and TaskCommentListener record for a task's
 * creation, its field and status changes, its completion and its comments
 * count; taskRankChanged and the label operations do not. The computed names
 * are read from the listeners' constants, every TaskSavedListener operation
 * constant but the rank being expected, so that a new one is declared or the
 * test fails; taskCreated and taskUpdated are the values the two
 * registrations of TaskSavedListener receive in analytics-configuration.xml.
 * The label keys are matched by hand against the webapp's
 * Portlets_en.properties.
 */
public class CollaborativeActivityDeclarationTest {

  private static final String DECLARATION_FILE = "analytics-collaborative-activities.json";

  @Test
  @SuppressWarnings("unchecked")
  public void theDeclarationCountsEveryTaskActionButTheRankAndTheLabels() throws IOException, IllegalAccessException {
    Map<String, Object> declaration;
    try (InputStream content = getClass().getClassLoader().getResourceAsStream(DECLARATION_FILE)) {
      assertNotNull("the declaration ships at the root of task-services, which lands in /lib", content);
      declaration = new ObjectMapper().readValue(content, Map.class);
    }

    assertEquals(List.of("tasks"), declaration.get("modules"));
    Set<String> expectedOperations = new HashSet<>(Set.of("taskCreated", "taskUpdated", TaskCommentListener.TASK_COMMENTED_OPERATION));
    for (Field field : TaskSavedListener.class.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())
          && field.getType() == String.class && field.getName().endsWith("_OPERATION")) {
        expectedOperations.add((String) field.get(null));
      }
    }
    assertEquals("two configured names, eleven computed changes, one comment, before the rank is removed", 14, expectedOperations.size());
    expectedOperations.remove(TaskSavedListener.TASK_RANK_CHANGED_OPERATION);
    List<String> operations = (List<String>) declaration.get("operations");
    assertEquals(expectedOperations, new HashSet<>(operations));
    assertEquals("one entry per operation", expectedOperations.size(), operations.size());
    assertFalse("a rank change is organisation, not collaboration",
                operations.contains(TaskSavedListener.TASK_RANK_CHANGED_OPERATION));
    assertFalse(declaration.containsKey("legacyOperations"));
    assertFalse(declaration.containsKey("excludedActivityTypes"));
    assertFalse(declaration.containsKey("browserOperations"));

    Map<String, Object> labels = (Map<String, Object>) declaration.get("labels");
    assertEquals(List.of("task.collaborativeActivity.help.counted.createTask",
                         "task.collaborativeActivity.help.counted.updateTask",
                         "task.collaborativeActivity.help.counted.changeStatus",
                         "task.collaborativeActivity.help.counted.completeTask",
                         "task.collaborativeActivity.help.counted.commentTask"),
                 labels.get("counted"));
  }

}
