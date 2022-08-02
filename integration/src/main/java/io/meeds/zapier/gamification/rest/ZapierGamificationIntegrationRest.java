/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.zapier.gamification.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.addons.gamification.service.configuration.RuleService;
import org.exoplatform.addons.gamification.service.dto.configuration.RuleDTO;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import io.meeds.zapier.gamification.model.GamificationAction;
import io.meeds.zapier.gamification.model.GamificationAnnouncement;
import io.meeds.zapier.gamification.model.GamificationRule;
import io.meeds.zapier.gamification.rest.utils.EntityBuilder;
import io.meeds.zapier.gamification.service.ZapierIntegrationService;

@Path("/zapier/gamification")
public class ZapierGamificationIntegrationRest implements ResourceContainer {

  private static final Log         LOG = ExoLogger.getLogger(ZapierGamificationIntegrationRest.class);

  private ZapierIntegrationService zapierIntegrationService;

  private RuleService              ruleService;

  private ResourceBundleService    resourceBundleService;

  public ZapierGamificationIntegrationRest(ZapierIntegrationService zapierIntegrationService,
                                           ResourceBundleService resourceBundleService,
                                           RuleService ruleService) {
    this.zapierIntegrationService = zapierIntegrationService;
    this.ruleService = ruleService;
    this.resourceBundleService = resourceBundleService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("zapier")
  public Response getCurrentUser() {
    // Used as empty endpoint to authenticate user only
    return Response.ok("{\"username\":\"" + ConversationState.getCurrent().getIdentity().getUserId() + "\"}").build();
  }

  @POST
  @Path("achievements")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("zapier")
  public Response createGamificationPoints(GamificationAction gamificationAction) {
    try {
      zapierIntegrationService.createGamificationPoints(gamificationAction);
      return Response.ok(gamificationAction).build();
    } catch (Exception e) {
      LOG.warn("Error while triggering action from zapier", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("rules")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("zapier")
  public Response getRules() {
    List<RuleDTO> rules = ruleService.getActiveRules();
    List<GamificationRule> ruleEntities = rules.stream()
                                               .map(rule -> EntityBuilder.toGamificationRuleAction(resourceBundleService, rule))
                                               .collect(Collectors.toList());
    return Response.ok(ruleEntities).build();
  }

  @GET
  @Path("challenges/{id}/announcements")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("zapier")
  public Response getAnnouncements(
                                   @PathParam("id")
                                   int challengeId,
                                   @QueryParam("offset")
                                   int offset,
                                   @QueryParam("limit")
                                   int limit) {
    try {
      List<GamificationAnnouncement> announcements = zapierIntegrationService.getAnnouncements(challengeId, offset, limit);
      return Response.ok(announcements).build();
    } catch (IllegalAccessException | ObjectNotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

}
