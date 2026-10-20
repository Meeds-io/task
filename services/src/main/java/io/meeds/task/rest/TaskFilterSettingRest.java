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
package io.meeds.task.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.task.model.TaskFilterSetting;
import io.meeds.task.service.TaskFilterSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/filter/settings")
@Tag(name = "/filter/settings", description = "Managing the tasks filter of the authenticated user")
public class TaskFilterSettingRest {

  private final TaskFilterSettingService taskFilterSettingService;

  @Autowired
  public TaskFilterSettingRest(TaskFilterSettingService taskFilterSettingService) {
    this.taskFilterSettingService = taskFilterSettingService;
  }

  @Secured("users")
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(method = "GET",
             summary = "Gets the tasks filter of the authenticated user",
             description = "This returns the group and sort options stored for a project and a tab view")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Project not found") })
  public ResponseEntity<Object> getSetting(HttpServletRequest request,
                                           @Parameter(description = "Project id, 0 for the tasks of the authenticated user")
                                           @RequestParam(name = "projectId", defaultValue = "0")
                                           long projectId,
                                           @Parameter(description = "Tab view: list, board or gantt")
                                           @RequestParam(name = "tabView", required = false)
                                           String tabView) {
    try {
      return answer(taskFilterSettingService.getSetting(projectId, tabView, request.getRemoteUser()));
    } catch (ObjectNotFoundException e) {
      return refusal(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      return refusal(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      return refusal(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @Secured("users")
  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(method = "PUT",
             summary = "Saves the tasks filter of the authenticated user",
             description = "This saves the group and sort options for a project and a tab view")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Project not found") })
  public ResponseEntity<Object> saveSetting(HttpServletRequest request,
                                            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Tasks filter to save", required = true)
                                            @RequestBody
                                            TaskFilterSetting setting) {
    try {
      return answer(taskFilterSettingService.saveSetting(setting, request.getRemoteUser()));
    } catch (ObjectNotFoundException e) {
      return refusal(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      return refusal(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      return refusal(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  private ResponseEntity<Object> answer(TaskFilterSetting setting) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(setting);
  }

  private ResponseEntity<Object> refusal(HttpStatus status, String messageCode) {
    return ResponseEntity.status(status)
                         .cacheControl(CacheControl.noStore().cachePrivate())
                         .body(Map.of("message", messageCode));
  }

}
