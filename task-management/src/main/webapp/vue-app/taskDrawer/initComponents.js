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
import TaskDrawer from '../taskDrawer/components/TaskDrawer.vue';
import TaskLabels from './components/TaskDrawerComponents/TaskLabels.vue';
import TaskProjects from './components/TaskDrawerComponents/TaskProject.vue';
import TaskAssignment from './components/TaskDrawerComponents/TaskAssignment.vue';
import TaskDescriptionEditor from './components/TaskDrawerComponents/TaskDescriptionEditor.vue';
import TaskPriority from './components/TaskDrawerComponents/TaskPriority.vue';
import TaskStatus from './components/TaskDrawerComponents/TasksStatus.vue';
import TaskFormDatePickers from './components/TaskDrawerComponents/TaskFormDatePickers.vue';
import TaskChangesDrawer from './components/TaskDrawerComponents/TaskChangesDrawer.vue';

const components = {
  'task-drawer': TaskDrawer,
  'task-labels': TaskLabels,
  'task-projects': TaskProjects,
  'task-assignment': TaskAssignment,
  'task-description-editor': TaskDescriptionEditor,
  'task-priority': TaskPriority,
  'task-status': TaskStatus,
  'task-form-date-pickers': TaskFormDatePickers,
  'task-changes-drawer': TaskChangesDrawer
};

for (const key in components) {
  Vue.component(key, components[key]);
}