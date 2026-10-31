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
package io.meeds.task.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;

import io.meeds.task.rest.model.TaskChangeLogItem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * A task's change log, read page after page by the last entry read, for the
 * Changes timeline of the task drawer.
 */
@RestController
@RequestMapping("/tasks")
@Tag(name = "/tasks", description = "Reading the change log of a task")
public class TaskChangeLogRest {

  private final TaskService taskService;

  /**
   * @param taskService the service the change log is read through
   */
  @Autowired
  public TaskChangeLogRest(TaskService taskService) {
    this.taskService = taskService;
  }

  /**
   * Reads one page of a task's change log, newest first, after the last entry
   * the reader already read, so that the pages of a long log read one by one
   * with no entry read twice or skipped. No answer may be stored.
   *
   * @param taskId the task id
   * @param beforeTime the date, in milliseconds, of the last entry already
   *          read; none for the first page
   * @param beforeId the id of the last entry already read, sent with
   *          beforeTime
   * @param limit the maximum number of entries, 20 by default
   * @return the entries as the timeline shows them, never the task itself,
   *         newest first and, on the same date, highest id first, the undated
   *         ones last; 404 when the task does not exist,
   *         403 when the reader may not view it, 400 with the code of a
   *         cursor sent without one of its parts or of a limit out of range,
   *         and 400 with no code when a parameter is not a number
   */
  @Secured("users")
  @GetMapping(path = "{taskId}/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(method = "GET",
             summary = "Gets a page of the change log of a task",
             description = "Newest first, the entries older than the last one read")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid cursor or limit"),
      @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Task not found") })
  public ResponseEntity<Object> getTaskLogs(@Parameter(description = "Task id", required = true)
                                            @PathVariable("taskId")
                                            long taskId,
                                            @Parameter(description = "Date in milliseconds of the last entry read")
                                            @RequestParam(name = "beforeTime", required = false)
                                            Long beforeTime,
                                            @Parameter(description = "Id of the last entry read, sent with beforeTime")
                                            @RequestParam(name = "beforeId", required = false)
                                            Long beforeId,
                                            @Parameter(description = "Maximum number of entries")
                                            @RequestParam(name = "limit", defaultValue = "20")
                                            int limit) {
    try {
      List<TaskChangeLogItem> logs = taskService.getTaskLogsBefore(taskId, beforeTime, beforeId, limit, currentIdentity())
                                                .stream()
                                                .map(TaskChangeLogItem::of)
                                                .toList();
      return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(logs);
    } catch (EntityNotFoundException e) {
      return refusal(HttpStatus.NOT_FOUND, "task.logs.notFound");
    } catch (IllegalAccessException e) {
      return refusal(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      return refusal(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  /**
   * @return the identity of the user reading, none when no one is signed in
   */
  private Identity currentIdentity() {
    ConversationState state = ConversationState.getCurrent();
    return state == null ? null : state.getIdentity();
  }

  /**
   * @param status the status of the refusal
   * @param messageCode the code the client shows through its label
   * @return the refusal, never stored
   */
  private ResponseEntity<Object> refusal(HttpStatus status, String messageCode) {
    return ResponseEntity.status(status)
                         .cacheControl(CacheControl.noStore().cachePrivate())
                         .body(Map.of("message", messageCode));
  }

}
