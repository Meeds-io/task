/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.task.service.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.UserSetting;
import org.exoplatform.task.exception.NotAllowedOperationOnEntityException;
import org.exoplatform.task.exception.ProjectNotFoundException;
import org.exoplatform.task.model.User;
import org.exoplatform.task.service.DAOHandler;
import org.exoplatform.task.service.UserService;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@Singleton
public class UserServiceImpl implements UserService {
  private static final User GUEST = new User("guest", "Guest", LinkProvider.PROFILE_DEFAULT_AVATAR_URL);

  private static final Log LOG = ExoLogger.getExoLogger(UserServiceImpl.class);

  @Inject
  private OrganizationService orgService;

  @Inject
  private IdentityManager identityManager;

  @Inject
  private DAOHandler daoHandler;

  public UserServiceImpl(OrganizationService orgService, IdentityManager idMgr, DAOHandler handler) {
    this.orgService = orgService;
    this.identityManager = idMgr;
    this.daoHandler = handler;
  }

  @Override
  public User loadUser(String username) {
    if (username == null) {
      return GUEST;
    }
    try {
      org.exoplatform.services.organization.User u = orgService.getUserHandler().findUserByName(username);
      if (u == null) {
        return GUEST;
      }
      Profile profile = identityManager
              .getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, false)
              .getProfile();
      String avatarURL = profile.getAvatarUrl();
      if (avatarURL == null) {
        avatarURL = LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
      }
      return new User(username, profile.getFullName(), avatarURL);

    } catch (Exception ex) {// NOSONAR Throw by orgService
      LOG.debug("User not find, return GUEST", ex);
      return GUEST;
    }
  }

  @Override
  public UserSetting getUserSetting(String username) {
    return daoHandler.getUserSettingHandler().getOrCreate(username);
  }

  @Override
  public void hideProject(Identity identity, Long projectId, boolean hide) throws ProjectNotFoundException, NotAllowedOperationOnEntityException {
    Project project = daoHandler.getProjectHandler().find(projectId);
    if (project == null) {
      throw new ProjectNotFoundException(projectId);
    }

    if (!project.canView(identity)) {
      throw new NotAllowedOperationOnEntityException(projectId, "Project", "hide");
    }

    UserSetting setting = daoHandler.getUserSettingHandler().getOrCreate(identity.getUserId());
    if (hide) {
      setting.getHiddenProjects().add(project);
    } else {
      setting.getHiddenProjects().remove(project);
    }

    daoHandler.getUserSettingHandler().update(setting);
  }

  @Override
  public void showHiddenProject(String username, boolean show) {
    UserSetting setting = daoHandler.getUserSettingHandler().getOrCreate(username);
    setting.setShowHiddenProject(show);
    daoHandler.getUserSettingHandler().update(setting);
  }
}
