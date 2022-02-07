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

Vue.use(Vuetify);

export function formatSearchResult(results, term) {
  if (results && results.tasks && results.tasks.length) {
    results = results.tasks.map(task => {
      const commentCount = task.commentCount;
      task = task.task;
      task.commentCount = commentCount;
      task.titleExcerpt = task.title.replace(new RegExp(`(${term})`, 'ig'), '<span class="searchMatchExcerpt">$1</span>');
      task.descriptionExcerpt = $('<div />').html(task.description).text().replace(new RegExp(`(${term})`, 'ig'), '<span class="searchMatchExcerpt">$1</span>');
      return task;
    });
  }
  return results;
}

$('.VuetifyApp .v-application').first().append('<div id="TaskSearchDrawers" />');

const appId = 'TaskSearchDrawers';

const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';
Vue.use(Vuetify);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

const urls = [`${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.portlet.taskManagement-${lang}.json`];
exoi18n.loadLanguageAsync(lang, urls).then(i18n => {
  new Vue({
    template: '<task-search-drawer />',
    vuetify,
    i18n
  }).$mount(`#${appId}`);
});
