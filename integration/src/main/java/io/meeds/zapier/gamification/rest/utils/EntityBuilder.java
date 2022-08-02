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
package io.meeds.zapier.gamification.rest.utils;

import java.util.Locale;
import java.util.ResourceBundle;

import org.exoplatform.addons.gamification.service.dto.configuration.RuleDTO;
import org.exoplatform.services.resources.ResourceBundleService;

import io.meeds.zapier.gamification.model.GamificationRule;

public class EntityBuilder {

  private static ResourceBundle gamificationResourceBundle;

  private EntityBuilder() {
    // Utils class
  }

  public static GamificationRule toGamificationRuleAction(ResourceBundleService resourceBundleService, RuleDTO rule) {
    String title = rule.getTitle();
    String event = rule.getEvent();
    ResourceBundle resourceBundle = getGamificationResourceBundle(resourceBundleService);
    if (resourceBundle != null) {
      if (resourceBundle.containsKey("exoplatform.gamification.gamificationinformation.rule.title." + event)) {
        title = resourceBundle.getString("exoplatform.gamification.gamificationinformation.rule.title." + event);
      } else if (resourceBundle.containsKey("exoplatform.gamification.gamificationinformation.rule.title.def_" + event)) {
        title = resourceBundle.getString("exoplatform.gamification.gamificationinformation.rule.title.def_" + event);
      }
    }
    return new GamificationRule(rule.getId(),
                                event,
                                title,
                                rule.getDescription());
  }

  private static ResourceBundle getGamificationResourceBundle(ResourceBundleService resourceBundleService) {
    if (gamificationResourceBundle == null) {
      gamificationResourceBundle = resourceBundleService.getResourceBundle("locale.addon.Gamification", Locale.ENGLISH);
    }
    return gamificationResourceBundle;
  }
}
