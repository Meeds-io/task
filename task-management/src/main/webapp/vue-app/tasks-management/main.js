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
import './initComponents.js';
import '../taskDrawer/initComponents.js';
import { tasksConstants } from '../../js/tasksConstants.js';
import * as tasksService from '../../js/tasksService.js';
import * as projectService from '../../js/projectService.js';
import * as statusService from '../../js/statusService.js';
import * as taskDrawerApi from '../../js/taskDrawerApi.js';

Vue.use(Vuetify);
Vue.use(VueEllipsis);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

if (!Vue.prototype.$tasksService) {
  window.Object.defineProperty(Vue.prototype, '$tasksService', {
    value: tasksService,
  });
}

if (!Vue.prototype.$projectService) {
  window.Object.defineProperty(Vue.prototype, '$projectService', {
    value: projectService,
  });
}

if (!Vue.prototype.$statusService) {
  window.Object.defineProperty(Vue.prototype, '$statusService', {
    value: statusService,
  });
}

if (!Vue.prototype.$taskDrawerApi) {
  window.Object.defineProperty(Vue.prototype, '$taskDrawerApi', {
    value: taskDrawerApi,
  });
}


//getting language of user
const lang = eXo && tasksConstants.LANG || 'en';

//should expose the locale ressources as REST API
const url = `${tasksConstants.PORTAL}/${tasksConstants.PORTAL_REST}/i18n/bundle/locale.portlet.taskManagement-${lang}.json`;

export function init() {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    Vue.createApp({
      template: '<tasks-management></tasks-management>',
      i18n,
      vuetify,
    }, '#tasksManagement', 'Tasks');
  });
}