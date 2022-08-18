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
package org.exoplatform.task.rest;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;

@Path("/status")
@Tag(name = "/status", description = "Managing status")
@RolesAllowed("users")
public class StatusRestService implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(StatusRestService.class);

  private ProjectService   projectService;

  private StatusService    statusService;

  public StatusRestService(ProjectService projectService, StatusService statusService) {
    this.projectService = projectService;
    this.statusService = statusService;
  }


  @GET
  @Path("{statusId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "get Status by Id", method = "GET", description = "This get status by Id")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getStatus(@Parameter(description = "statusId", required = true) @PathParam("statusId") long statusId) {
    try {
      StatusDto statusDto = statusService.getStatus(statusId);
      if (statusDto == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      return Response.ok(statusDto).build();
    }
    catch (Exception e) {
      LOG.error("Can't get Status {}", statusId,e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }


  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Create Status", method = "POST", description = "This create new status")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response addStatus(@RequestBody(description = "Status object to be created", required = true) List<StatusDto> status){
    try {
    Identity identity = ConversationState.getCurrent().getIdentity();
    ProjectDto projectDto = projectService.getProject(status.get(0).getProject().getId());
    if (projectDto.getParent() != null && projectDto.getParent().getId()!=0) {
      Long parentId = projectDto.getParent().getId();
      try {
        if (projectService.getProject(parentId)!=null && !projectService.getProject(parentId).canEdit(identity)) {
          return Response.status(Response.Status.UNAUTHORIZED).build();
        }
      } catch (EntityNotFoundException ex) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    }
    if (!projectDto.canEdit(identity)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    for(StatusDto statusDto : status){
      if(statusDto.getId()==null){
        statusService.createStatus(projectDto, statusDto.getName(),statusDto.getRank());
      }else{
        StatusDto oldStatus = statusService.getStatus(statusDto.getId());
        if(oldStatus!=null && oldStatus.getRank()!=statusDto.getRank()){
          statusService.updateStatus(statusDto);
        }
      }
    }

    return Response.ok(Response.Status.OK).build();
  }
    catch (Exception e) {
    LOG.error("Can't Create Status",e);
    return Response.serverError().entity(e.getMessage()).build();
  }
  }

  @PUT
  @Path("{statusId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Update StatusId", method = "PUT", description = "This Update status name")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response updateStatus(@Parameter(description = "statusId", required = true) @PathParam("statusId") long statusId,
                               @RequestBody(description = "status object to be updated", required = true) StatusDto statusDto) {
    try {  
  if (statusService.getStatus(statusId) == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    Identity identity = ConversationState.getCurrent().getIdentity();
    ProjectDto projectDto = projectService.getProject(statusDto.getProject().getId());
    if (projectDto.getParent() != null && !projectDto.getParent().toString().isEmpty()) {
      Long parentId = projectDto.getParent().getId();
      try {
        if (projectService.getProject(parentId)!=null && !projectService.getProject(parentId).canEdit(identity)) {
          return Response.status(Response.Status.UNAUTHORIZED).build();
        }
      } catch (EntityNotFoundException ex) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    }
    if (!projectDto.canEdit(identity)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    statusService.updateStatus(statusDto.getId(), statusDto.getName());
    return Response.ok(Response.Status.OK).build();
  }
    catch (Exception e) {
      LOG.error("Can't update Status {}", statusId,e);
      return Response.serverError().entity(e.getMessage()).build();
   }
  }


  @PUT
  @Path("move")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "move Status", method = "PUT", description = "This moves status")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response moveStatus(@RequestBody(description = "Status to be moved", required = true) List<StatusDto> status){
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      ProjectDto projectDto = projectService.getProject(status.get(0).getProject().getId());
      if (projectDto.getParent() != null && projectDto.getParent().getId()!=0) {
        Long parentId = projectDto.getParent().getId();
        try {
          if (projectService.getProject(parentId)!=null && !projectService.getProject(parentId).canEdit(identity)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
          }
        } catch (EntityNotFoundException ex) {
          return Response.status(Response.Status.NOT_FOUND).build();
        }
      }
      if (!projectDto.canEdit(identity)) {
        return Response.status(Response.Status.UNAUTHORIZED).build();
      }
      for(StatusDto statusDto : status){

          StatusDto oldStatus = statusService.getStatus(statusDto.getId());
          if(oldStatus!=null && oldStatus.getRank()!=statusDto.getRank()){
            statusService.updateStatus(statusDto);
          }

      }

      return Response.ok(Response.Status.OK).build();
    }
    catch (Exception e) {
      LOG.error("Can't move Status",e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Path("{statusId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Delete status", method = "DELETE",  description = "This deletes the status")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Project deleted"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "User not authorized to delete the Project"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteStatus(@Parameter(description = "status Id", required = true) @PathParam("statusId") Long statusId) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      StatusDto statusDto = statusService.getStatus(statusId);
      if (!statusDto.getProject().canEdit(identity)) {
        return Response.status(Response.Status.UNAUTHORIZED).build();
      }
      statusService.removeStatus(statusDto.getId());
      return Response.ok(Response.Status.OK).entity("Deleted").build();
    } catch (Exception e) {
      LOG.error("Can't delete Status {}", statusId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

}
