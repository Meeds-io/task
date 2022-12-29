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
package org.exoplatform.task.service;

import java.util.TimeZone;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.domain.UserSetting;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.NotAllowedOperationOnEntityException;
import org.exoplatform.task.model.User;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public interface UserService {

  User loadUser(String username);

  /**
   * For now, this method is used only for search user in assignee, permission or mention.
   * These function use username, fullName and avatar, so some other infos will be null to avoid recall organizationService
   * @param keyword name of the user
   * @param excludeExternal if true, exclude external users from search
   * @return List of users
   */
  ListAccess<User> findUserByName(String keyword, boolean excludeExternal);

  UserSetting getUserSetting(String username);

  void showHiddenProject(String username, boolean show);
  
  void showHiddenLabel(String username, boolean show);

  void hideProject(Identity identity, Long projectId, boolean hide) throws EntityNotFoundException, NotAllowedOperationOnEntityException;

  TimeZone getUserTimezone(String username);
}
