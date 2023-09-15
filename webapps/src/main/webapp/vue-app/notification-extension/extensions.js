/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

extensionRegistry.registerExtension('WebNotification', 'notification-group-extension', {
  rank: 50,
  name: 'task',
  plugins: [
    'TaskCommentedPlugin',
    'TaskMentionedPlugin',
    'TaskEditionPlugin',
    'TaskCompletedPlugin',
    'TaskAssignPlugin',
    'TaskCoworkerPlugin',
    'TaskDueDatePlugin'
  ],
  icon: 'fa-tasks',
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'TaskCommentedPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-task-comment'],
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'TaskMentionedPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-task-mention'],
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'TaskEditionPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-task-edit'],
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'TaskCompletedPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-task-archive'],
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'TaskAssignPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-task-assign'],
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'TaskCoworkerPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-task-assign-coworker'],
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'TaskDueDatePlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-task-due-date'],
});
