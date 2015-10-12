/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.task.integration;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.RequestNavigationData;
import org.exoplatform.portal.application.state.AbstractContextualPropertyProviderPlugin;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.social.common.router.ExoRouter;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import javax.xml.namespace.QName;
import java.util.Map;

public class SpacePropertiesPlugin extends AbstractContextualPropertyProviderPlugin {

    private final QName spaceIdQName;
    private final QName spaceGroupIdQName;

    public SpacePropertiesPlugin(InitParams params) throws Exception {
        super(params);
        //
        this.spaceIdQName = new QName(namespaceURI, "space_id");
        this.spaceGroupIdQName = new QName(namespaceURI, "space_group_id");
    }

    @Override
    public void getProperties(UIPortlet portletWindow, Map<QName, String[]> properties) {
        try {
            Space space = getSpace();

            if (space != null) {
              addProperty(properties, spaceIdQName, space.getId());
              addProperty(properties, spaceGroupIdQName, space.getGroupId());
            }
        } catch (Exception ex) {
            log.error("Could not obtain contextual properties for portlet " + portletWindow, ex);
        }
    }

    private Space getSpace() {
      PortalRequestContext pContext = Util.getPortalRequestContext();
      String requestPath = pContext.getControllerContext().getParameter(RequestNavigationData.REQUEST_PATH);
      ExoRouter.Route er = ExoRouter.route(requestPath);

      if (er != null && er.localArgs != null) {
        String spacePrettyName = er.localArgs.get("spacePrettyName");
        SpaceService sService = (SpaceService) PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);

        if (spacePrettyName != null && !spacePrettyName.isEmpty()) {
          Space space = sService.getSpaceByPrettyName(spacePrettyName);
          return space;
        }
      }

      return null;
    }
}
