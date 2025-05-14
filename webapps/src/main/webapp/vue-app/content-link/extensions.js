/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
const appId = 'task-content-link-drawer';

export function init() {
  if (!document.querySelector(`#${appId}`)) {
    document.addEventListener('content-link-task-drawer', openTaskDrawer);
  }
}

function openTaskDrawer(event) {
  const taskId = event.detail;
  window.require(['SHARED/eXoVueI18n', 'SHARED/taskDrawer', 'SHARED/taskCommentsDrawer'], exoi18n => initTaskDrawer(exoi18n, taskId));
}

async function initTaskDrawer(exoi18n, taskId) {
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await Promise.all([
      initTaskDrawerApp(appId, exoi18n),
      Vue.prototype.$utils.importSkin('portal', 'tasksDrawer'),
      Vue.prototype.$utils.importSkin('portal', 'taskCommentsDrawer'),
      Vue.prototype.$utils.importSkin('portal', 'tasks')
    ]);
  }
  document.dispatchEvent(new CustomEvent('content-link-task-drawer-open', {detail: taskId}));
}

function initTaskDrawerApp(appId, exoi18n) {
  const lang = eXo.env.portal.language;
  const url = `/task-management/i18n/locale.portlet.taskManagement?lang=${lang}`;
  return new Promise(resolve => exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => Vue.createApp({
      template: `
        <task-drawer
          id="${appId}"
          ref="drawer" />
      `,
      computed: {
        isMobile() {
          return this.$vuetify?.breakpoint?.mobile;
        },
      },
      created() {
        document.addEventListener('content-link-task-drawer-open', this.openDrawer);
      },
      mounted() {
        resolve();
      },
      beforeDestroy() {
        document.removeEventListener('content-link-task-drawer-open', this.openDrawer);
      },
      methods: {
        async openDrawer(event) {
          const taskId = event.detail;
          const resp = await fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/tasks/${taskId}`, {
            method: 'GET',
            credentials: 'include',
          });
          if (resp?.ok) {
            const task = await resp.json();
            this.$refs.drawer.open(task);
          } else {
            throw new Error(`Can't get Task with id ${taskId}`);
          }
        },
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Task Content Link Drawer')));
}
