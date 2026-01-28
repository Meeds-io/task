<%
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
%>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.exoplatform.services.resources.ResourceBundleService" %>
<%@ page import="org.exoplatform.container.PortalContainer" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<portlet:defineObjects />
<portlet:actionURL var="saveSettingsUrl" />
<%
  String itemsLimit = System.getProperty("exo.dw.page.snapshot.itemsLimit", "10");
    
  PortalContainer portalContainer = PortalContainer.getCurrentInstance(session.getServletContext());
  ResourceBundleService resourceBundleService = portalContainer.getComponentInstanceOfType(ResourceBundleService.class);
  ResourceBundle resourceBundle = resourceBundleService.getResourceBundle("locale.portlet.taskManagement", request.getLocale());

  boolean canEdit = (boolean) request.getAttribute("canEdit");
  Object settings = (String[]) request.getAttribute("settings");
  if (settings != null) {
    settings = ((String[]) settings)[0];
  }
  String portletId = (String) request.getAttribute("portletStorageId");
  String domId = "tasksApplication" + portletId;
  String valueDomId = "tasksSettingsValue" + portletId;
%>

<div class="VuetifyApp">
  <div id="<%=domId%>">
    <textarea id="<%=valueDomId%>" style="display:none;"><%=settings == null ? "{}" : settings%></textarea>
    <script>
      require(['SHARED/tasksBundle'], function(tasksApp) {
        tasksApp.init('<%=domId%>', '<%=itemsLimit%>', JSON.parse(document.getElementById('<%=valueDomId%>').value), '<%=saveSettingsUrl%>', <%=canEdit%>);
      });
    </script>
  </div>
</div>