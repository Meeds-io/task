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
package io.meeds.zapier.gamification.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.addons.gamification.listener.generic.GamificationGenericListener;
import org.exoplatform.addons.gamification.service.AnnouncementService;
import org.exoplatform.addons.gamification.service.dto.configuration.Announcement;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.zapier.gamification.model.GamificationAction;
import io.meeds.zapier.gamification.model.GamificationAnnouncement;
import io.meeds.zapier.gamification.model.UserIdentity;

public class ZapierIntegrationService {

  private static final Context ZAPIER_CONTEXT = Context.GLOBAL;

  private static final Scope   ZAPIER_SCOPE   = Scope.APPLICATION.id("zapier");

  private static final Log     LOG            = ExoLogger.getLogger(ZapierIntegrationService.class);

  private IdentityManager      identityManager;

  private SettingService       settingService;

  private AnnouncementService  announcementService;

  private ListenerService      listenerService;

  public ZapierIntegrationService(IdentityManager identityManager,
                                  AnnouncementService announcementService,
                                  SettingService settingService,
                                  ListenerService listenerService) {
    this.identityManager = identityManager;
    this.announcementService = announcementService;
    this.settingService = settingService;
    this.listenerService = listenerService;
  }

  public void createGamificationPoints(GamificationAction gamificationAction) throws Exception {
    String imType = gamificationAction.getImType();
    String imUser = gamificationAction.getImUser();
    String ruleEvent = gamificationAction.getRuleTitle();
    String url = gamificationAction.getUrl();

    Identity userIdentity = getIdentityByImTypeAndUser(imType, imUser);
    if (userIdentity == null) {
      LOG.debug("Gamification action ignored from zapier for user '{}' using Social network '{}' on rule '{}'",
                imUser,
                imType,
                ruleEvent);
    } else {
      Map<String, String> gam = new HashMap<>();
      gam.put("ruleTitle", ruleEvent);
      gam.put("senderType", OrganizationIdentityProvider.NAME);
      gam.put("senderId", userIdentity.getRemoteId());
      gam.put("receiverId", userIdentity.getId());
      gam.put("object", url);
      listenerService.broadcast(GamificationGenericListener.EVENT_NAME, gam, "");
      LOG.info("Gamification action triggered from zapier for user '{}' using Social network '{}' on rule '{}'",
               userIdentity.getRemoteId(),
               imType,
               ruleEvent);
    }
  }

  public List<GamificationAnnouncement> getAnnouncements(long challengeId, int offset, int limit) throws IllegalAccessException,
                                                                                                  ObjectNotFoundException {
    List<Announcement> announcements = announcementService.findAllAnnouncementByChallenge(challengeId, offset, limit);
    return announcements.stream().map(announcement -> {
      Identity identity = identityManager.getIdentity(String.valueOf(announcement.getAssignee()));
      UserIdentity userIdentity = new UserIdentity(identity.getId(),
                                                   identity.getRemoteId(),
                                                   identity.getProfile().getFullName(),
                                                   identity.getProfile().getAvatarUrl());
      return new GamificationAnnouncement(announcement.getId(),
                                          announcement.getActivityId(),
                                          announcement.getChallengeTitle(),
                                          announcement.getComment(),
                                          userIdentity);
    }).collect(Collectors.toList());
  }

  public void updateImOfUser(String username, String imType, String imUser) {
    String oldImUser = getImUserByImTypeAndInternalUsername(imType, username);
    if (StringUtils.isBlank(imUser) && StringUtils.isNotBlank(oldImUser)) {
      settingService.remove(ZAPIER_CONTEXT, ZAPIER_SCOPE, getImSettingKey(imType, oldImUser));
      settingService.remove(ZAPIER_CONTEXT, ZAPIER_SCOPE, getSettingKey(imType, username));
    } else if (!StringUtils.equals(imUser, oldImUser)) {
      settingService.remove(ZAPIER_CONTEXT, ZAPIER_SCOPE, getImSettingKey(imType, oldImUser));
      settingService.set(ZAPIER_CONTEXT, ZAPIER_SCOPE, getImSettingKey(imType, imUser), SettingValue.create(username));
      settingService.set(ZAPIER_CONTEXT, ZAPIER_SCOPE, getSettingKey(imType, username), SettingValue.create(imUser));
    }
  }

  private Identity getIdentityByImTypeAndUser(String imType, String imUser) {
    String settingKey = getImSettingKey(imType, imUser);
    SettingValue<?> settingValue = settingService.get(ZAPIER_CONTEXT, ZAPIER_SCOPE, settingKey);
    if (settingValue != null && settingValue.getValue() != null) {
      String username = settingValue.getValue().toString();
      return identityManager.getOrCreateUserIdentity(username);
    }
    return null;
  }

  private String getImUserByImTypeAndInternalUsername(String imType, String username) {
    String settingKey = getSettingKey(imType, username);
    SettingValue<?> settingValue = settingService.get(ZAPIER_CONTEXT, ZAPIER_SCOPE, settingKey);
    String imUser = null;
    if (settingValue != null && settingValue.getValue() != null) {
      imUser = settingValue.getValue().toString();
    }
    return imUser;
  }

  private String getImSettingKey(String imType, String imUser) {
    return imType + "_" + imUser;
  }

  private String getSettingKey(String imType, String username) {
    return "internal_" + imType + "_" + username;
  }

}
