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
package org.exoplatform.task.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.favorite.model.Favorite;
import org.exoplatform.social.metadata.model.MetadataItem;

public final class FavoriteUtil {

  private FavoriteUtil() {
    // Static utility class
  }

  /**
   * Checks whether the current authenticated user has marked the given
   * object as favorite.
   *
   * @param favoriteService {@link FavoriteService}
   * @param identityManager {@link IdentityManager}
   * @param objectType favorite object type (e.g. project/task)
   * @param objectId object technical identifier
   * @return true if the current user marked the object as favorite
   */
  public static boolean isFavorite(FavoriteService favoriteService, IdentityManager identityManager, String objectType, long objectId) {
    Long currentUserIdentityId = getCurrentUserIdentityId(identityManager);
    if (currentUserIdentityId == null) {
      return false;
    }
    return favoriteService.isFavorite(new Favorite(objectType, String.valueOf(objectId), null, currentUserIdentityId));
  }

  /**
   * Retrieves, in a single call, the ids of all the objects of the given
   * type that the current authenticated user marked as favorite. Meant to
   * avoid one {@link FavoriteService#isFavorite(Favorite)} call per row when
   * enriching a list of objects with their favorite status.
   *
   * @param favoriteService {@link FavoriteService}
   * @param identityManager {@link IdentityManager}
   * @param objectType favorite object type (e.g. project/task)
   * @return {@link Set} of favorite object ids (as {@link String}) for the current user
   */
  public static Set<String> getFavoriteObjectIds(FavoriteService favoriteService, IdentityManager identityManager, String objectType) {
    Long currentUserIdentityId = getCurrentUserIdentityId(identityManager);
    if (currentUserIdentityId == null) {
      return Collections.emptySet();
    }
    List<MetadataItem> favoriteItems = favoriteService.getFavoriteItemsByCreatorAndType(objectType, currentUserIdentityId, 0, -1);
    Set<String> favoriteObjectIds = new HashSet<>();
    for (MetadataItem favoriteItem : favoriteItems) {
      favoriteObjectIds.add(favoriteItem.getObjectId());
    }
    return favoriteObjectIds;
  }

  private static Long getCurrentUserIdentityId(IdentityManager identityManager) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    if (identity == null) {
      return null;
    }
    org.exoplatform.social.core.identity.model.Identity userIdentity = identityManager.getOrCreateUserIdentity(identity.getUserId());
    if (userIdentity == null) {
      return null;
    }
    return Long.parseLong(userIdentity.getId());
  }

}
