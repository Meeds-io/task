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
package org.exoplatform.task.dao.jpa;

import org.exoplatform.task.dao.UserSettingHandler;
import org.exoplatform.task.domain.UserSetting;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class UserSettingDAO extends CommonJPADAO<UserSetting, String> implements UserSettingHandler {

  public UserSettingDAO() {
  }

  @Override
  public UserSetting getOrCreate(String username) {
    UserSetting setting = this.find(username);
    if (setting == null) {
      setting = new UserSetting(username);
      setting = this.create(setting);
      synchronized (this) {
        if (this.find(username) == null) {
          setting = new UserSetting(username);
          this.create(setting);
          getEntityManager().flush();
        }
      }
    }
    return cloneEntity(setting);
  }
}
