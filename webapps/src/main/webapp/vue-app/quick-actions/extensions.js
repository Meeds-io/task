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

extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'task-project',
  icon: 'fa-clipboard-list',
  name: 'quickActions.taskProject.name',
  description: 'quickActions.taskProject.description',
  click: () => new Promise(resolve => {
    window.require(['SHARED/eXoVueI18n', 'PORTLET/task-management/TasksManagement'], exoi18n => initProjectDrawer(exoi18n, resolve));
  }),
});

extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'task',
  icon: 'fa-tasks',
  name: 'quickActions.task.name',
  description: 'quickActions.task.description',
  click: () => new Promise(resolve => {
    window.require(['SHARED/eXoVueI18n', 'SHARED/taskDrawer'], exoi18n => initTaskDrawer(exoi18n, resolve));
  }),
});

async function initProjectDrawer(exoi18n, callback) {
  const appId = 'task-project-quick-actions';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initProjectDrawerApp(appId, exoi18n);
    await Vue.prototype.$utils.importSkin('portal', 'tasks');
    await Vue.prototype.$utils.importSkin('portal', 'tasksDrawer');
    await Vue.prototype.$utils.importSkin('portal', 'taskCommentsDrawer');
    await Vue.prototype.$utils.importSkin('portal', 'ImageCropper');
  }
  document.dispatchEvent(new CustomEvent('quick-action-task-project-drawer'));
  callback();
}

async function initTaskDrawer(exoi18n, callback) {
  const appId = 'task-quick-actions';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initTaskDrawerApp(appId, exoi18n);
    await Vue.prototype.$utils.importSkin('portal', 'tasksDrawer');
  }
  document.dispatchEvent(new CustomEvent('quick-action-task-drawer'));
  callback();
}

function initProjectDrawerApp(appId, exoi18n) {
  const lang = eXo.env.portal.language;
  const url = `/task-management/i18n/locale.portlet.taskManagement?lang=${lang}`;
  return new Promise(resolve => exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => Vue.createApp({
      template: `
        <add-project-drawer
          id="${appId}"
          ref="drawer" />
      `,
      computed: {
        isMobile() {
          return this.$vuetify?.breakpoint?.mobile;
        },
      },
      created() {
        document.addEventListener('quick-action-task-project-drawer', this.openDrawer);
      },
      mounted() {
        resolve();
      },
      beforeDestroy() {
        document.removeEventListener('quick-action-task-project-drawer', this.openDrawer);
      },
      methods: {
        openDrawer() {
          this.$refs.drawer.open({
            id: null,
            status: {
              project: null
            },
            priority: 'NONE',
            description: '',
            title: ''
          });
        },
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Task Project Quick Action')));
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
        document.addEventListener('quick-action-task-drawer', this.openDrawer);
      },
      mounted() {
        resolve();
      },
      beforeDestroy() {
        document.removeEventListener('quick-action-task-drawer', this.openDrawer);
      },
      methods: {
        openDrawer() {
          this.$refs.drawer.open({
            id: null,
            status: {
              project: null
            },
            priority: 'NONE',
            description: '',
            title: ''
          });
        },
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Task Quick Action')));
}
