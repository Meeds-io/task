/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.exoplatform.task.integration;

import org.exoplatform.portal.config.GroupVisibilityPlugin;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

import java.util.Collection;

/**
 * The user can see a group only when:
 * * the given user is the super user
 * * the given user is a platform administrator
 * * the given user is a manager of the group
 */
public class ProjectGroupVisibilityPlugin extends GroupVisibilityPlugin {

  private UserACL userACL;

  public ProjectGroupVisibilityPlugin(UserACL userACL) {
    this.userACL = userACL;
  }

  public boolean hasPermission(Identity userIdentity, Group group) {
    Collection<MembershipEntry> userMemberships = userIdentity.getMemberships();
    return userACL.getSuperUser().equals(userIdentity.getUserId())
        || userMemberships.stream()
                          .anyMatch(userMembership -> userMembership.getGroup().equals(userACL.getAdminGroups())
                              || ((userMembership.getGroup().equals(group.getId())
                                  || userMembership.getGroup().startsWith(group.getId() + "/"))
                                  && (userMembership.getMembershipType().equals("*")
                                      || userMembership.getMembershipType().equals("manager"))));
  }
}
