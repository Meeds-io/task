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

import org.exoplatform.portal.config.GroupVisibilityPlugin;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectGroupVisibilityPluginTest {
    @Test
    public void shouldHasPermissionWhenUserIsSuperUser() {
        // Given
        UserACL userACL = mock(UserACL.class);
        when(userACL.getSuperUser()).thenReturn("john");
        GroupVisibilityPlugin plugin = new ProjectGroupVisibilityPlugin(userACL);

        Identity userIdentity = new Identity("john", Arrays.asList(new MembershipEntry("/platform/users", "manager")));
        Group groupPlatform = new GroupImpl();
        groupPlatform.setId("/platform");
        Group groupPlatformUsers = new GroupImpl();
        groupPlatformUsers.setId("/platform/users");

        // When
        boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
        boolean hasPermissionOnPlatformUsers = plugin.hasPermission(userIdentity, groupPlatformUsers);

        // Then
        assertTrue(hasPermissionOnPlatform);
        assertTrue(hasPermissionOnPlatformUsers);
    }

    @Test
    public void shouldHasPermissionWhenUserIsPlatformAdministrator() {
        // Given
        UserACL userACL = mock(UserACL.class);
        when(userACL.getSuperUser()).thenReturn("root");
        when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
        GroupVisibilityPlugin plugin = new ProjectGroupVisibilityPlugin(userACL);

        Identity userIdentity = new Identity("john", Arrays.asList(new MembershipEntry("/platform/administrators", "manager")));
        Group groupPlatform = new GroupImpl();
        groupPlatform.setId("/platform");
        Group groupPlatformUsers = new GroupImpl();
        groupPlatformUsers.setId("/platform/users");

        // When
        boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
        boolean hasPermissionOnPlatformUsers = plugin.hasPermission(userIdentity, groupPlatformUsers);

        // Then
        assertTrue(hasPermissionOnPlatform);
        assertTrue(hasPermissionOnPlatformUsers);
    }

    @Test
    public void shouldHasPermissionWhenUserIsInGivenGroup() {
        // Given
        UserACL userACL = mock(UserACL.class);
        when(userACL.getSuperUser()).thenReturn("root");
        when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
        GroupVisibilityPlugin plugin = new ProjectGroupVisibilityPlugin(userACL);

        Identity userIdentity = new Identity("john",
                Arrays.asList(new MembershipEntry("/platform/developers", "manager"),
                        new MembershipEntry("/platform/testers", "member"),
                        new MembershipEntry("/spaces/marketing", "member"),
                        new MembershipEntry("/spaces/sales", "manager"),
                        new MembershipEntry("/organization/rh", "*")));
        Group groupPlatform = new GroupImpl();
        groupPlatform.setId("/platform");
        Group groupPlatformDevelopers = new GroupImpl();
        groupPlatformDevelopers.setId("/platform/developers");
        Group groupPlatformTesters = new GroupImpl();
        groupPlatformTesters.setId("/platform/testers");
        Group groupSpaces = new GroupImpl();
        groupSpaces.setId("/spaces");
        Group groupSpacesMarketing = new GroupImpl();
        groupSpacesMarketing.setId("/spaces/marketing");
        Group groupSpacesSales = new GroupImpl();
        groupSpacesSales.setId("/spaces/sales");
        Group groupSpacesEngineering = new GroupImpl();
        groupSpacesEngineering.setId("/spaces/engineering");
        Group groupOrganization = new GroupImpl();
        groupOrganization.setId("/organization");
        Group groupOrganizationRh = new GroupImpl();
        groupOrganizationRh.setId("/organization/rh");

        // When
        boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
        boolean hasPermissionOnPlatformDevelopers = plugin.hasPermission(userIdentity, groupPlatformDevelopers);
        boolean hasPermissionOnPlatformTesters = plugin.hasPermission(userIdentity, groupPlatformTesters);
        boolean hasPermissionOnSpaces = plugin.hasPermission(userIdentity, groupSpaces);
        boolean hasPermissionOnSpacesMarketing = plugin.hasPermission(userIdentity, groupSpacesMarketing);
        boolean hasPermissionOnSpacesSales = plugin.hasPermission(userIdentity, groupSpacesSales);
        boolean hasPermissionOnSpacesEngineering = plugin.hasPermission(userIdentity, groupSpacesEngineering);
        boolean hasPermissionOnOrganization = plugin.hasPermission(userIdentity, groupOrganization);
        boolean hasPermissionOnOrganizationRh = plugin.hasPermission(userIdentity, groupOrganizationRh);

        // Then
        assertTrue(hasPermissionOnPlatform);
        assertTrue(hasPermissionOnPlatformDevelopers);
        assertFalse(hasPermissionOnPlatformTesters);
        assertTrue(hasPermissionOnSpaces);
        assertFalse(hasPermissionOnSpacesMarketing);
        assertTrue(hasPermissionOnSpacesSales);
        assertFalse(hasPermissionOnSpacesEngineering);
        assertTrue(hasPermissionOnOrganization);
        assertTrue(hasPermissionOnOrganizationRh);
    }
}