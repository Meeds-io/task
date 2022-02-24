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

import TaskCommentsDrawer from './components/TaskCommentsDrawer.vue';
import TaskComments from './components/TaskComments.vue';
import TaskCommentEditor from './components/TaskCommentEditor.vue';
import TaskCommentItem from './components/TaskCommentItem.vue';
import TaskLastComment from './components/TaskLastComment.vue';
import TaskViewAllComments from './components/TaskViewAllComments.vue';

import * as tasksService from '../../js/tasksService.js';
import * as taskDrawerApi from '../../js/taskDrawerApi.js';


const components = {
  'task-comment-editor': TaskCommentEditor,
  'task-comments': TaskComments,
  'task-comments-drawer': TaskCommentsDrawer,
  'task-comment-item': TaskCommentItem,
  'task-last-comment': TaskLastComment,
  'task-view-all-comments': TaskViewAllComments,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

if (!Vue.prototype.$tasksService) {
  window.Object.defineProperty(Vue.prototype, '$tasksService', {
    value: tasksService,
  });
}

if (!Vue.prototype.$taskDrawerApi) {
  window.Object.defineProperty(Vue.prototype, '$taskDrawerApi', {
    value: taskDrawerApi,
  });
}