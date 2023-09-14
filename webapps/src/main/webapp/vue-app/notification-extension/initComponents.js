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

import TaskAssignPlugin from './components/TaskAssignPlugin.vue';
import TaskCommentedPlugin from './components/TaskCommentedPlugin.vue';
import TaskCompletedPlugin from './components/TaskCompletedPlugin.vue';
import TaskCoworkerPlugin from './components/TaskCoworkerPlugin.vue';
import TaskDueDatePlugin from './components/TaskDueDatePlugin.vue';
import TaskEditionPlugin from './components/TaskEditionPlugin.vue';
import TaskMentionedPlugin from './components/TaskMentionedPlugin.vue';

const components = {
  'user-notification-task-comment': TaskCommentedPlugin,
  'user-notification-task-mention': TaskMentionedPlugin,
  'user-notification-task-edit': TaskEditionPlugin,
  'user-notification-task-archive': TaskCompletedPlugin,
  'user-notification-task-assign': TaskAssignPlugin,
  'user-notification-task-assign-coworker': TaskCoworkerPlugin,
  'user-notification-task-due-date': TaskDueDatePlugin,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
