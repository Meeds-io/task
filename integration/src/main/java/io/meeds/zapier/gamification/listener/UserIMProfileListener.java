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
package io.meeds.zapier.gamification.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.profile.ProfileLifeCycleEvent;
import org.exoplatform.social.core.profile.ProfileListenerPlugin;
import org.exoplatform.social.core.profile.settings.IMType;
import org.exoplatform.social.core.profile.settings.UserProfileSettingsService;

import io.meeds.zapier.gamification.service.ZapierIntegrationService;

public class UserIMProfileListener extends ProfileListenerPlugin {

  private static final Log             LOG = ExoLogger.getLogger(UserIMProfileListener.class);

  protected ZapierIntegrationService   zapierIntegrationService;

  protected UserProfileSettingsService profileSettingsService;

  public UserIMProfileListener(UserProfileSettingsService profileSettingsService,
                               ZapierIntegrationService zapierIntegrationService) {
    this.profileSettingsService = profileSettingsService;
    this.zapierIntegrationService = zapierIntegrationService;
  }

  @Override
  public void avatarUpdated(ProfileLifeCycleEvent event) {
    // Not used
  }

  @Override
  public void bannerUpdated(ProfileLifeCycleEvent event) {
    // Not used
  }

  @Override
  public void basicInfoUpdated(ProfileLifeCycleEvent event) {
    // Not used
  }

  @Override
  public void contactSectionUpdated(ProfileLifeCycleEvent event) {
    String username = event.getUsername();
    Collection<IMType> imTypes = profileSettingsService.getIMTypes();
    for (IMType imType : imTypes) {
      String imTypeId = imType.getId();
      String imUserId = getIMUserId(imTypeId, event.getProfile());
      try {
        zapierIntegrationService.updateImOfUser(username, imTypeId, imUserId);
      } catch (Exception e) {
        LOG.warn("Can't update twitter account ", e);
      }
    }
  }

  @Override
  public void experienceSectionUpdated(ProfileLifeCycleEvent event) {
    // Not used
  }

  @Override
  public void headerSectionUpdated(ProfileLifeCycleEvent event) {
    // Not used
  }

  @Override
  public void createProfile(ProfileLifeCycleEvent event) {
    // Not used
  }

  @Override
  public void aboutMeUpdated(ProfileLifeCycleEvent event) {
    // Not used
  }

  private String getIMUserId(String imTypeId, Profile profile) {
    @SuppressWarnings("unchecked")
    List<HashMap<String, String>> ims = (List<HashMap<String, String>>) profile.getProperty("ims");
    for (HashMap<String, String> map : ims) {
      if (map.get("key").equals(imTypeId)) {
        return map.get("value");
      }
    }
    return null;
  }

}
