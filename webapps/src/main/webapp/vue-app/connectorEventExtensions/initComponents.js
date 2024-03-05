/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
import TaskEvent from './components/TaskEvent.vue';
import TaskEventForm from './components/TaskEventForm.vue';
import TaskEventDisplay from './components/TaskEventDisplay.vue';
import TaskEventProjectItem from './components/TaskEventProjectItem.vue';
import ProjectSuggester from '../tasks-management/components/Project/ProjectSuggester.vue';

const components = {
  'gamification-task-event': TaskEvent,
  'gamification-task-event-form': TaskEventForm,
  'gamification-task-event-display': TaskEventDisplay,
  'gamification-task-event-project-item': TaskEventProjectItem,
  'project-suggester': ProjectSuggester,
};

for (const key in components) {
  Vue.component(key, components[key]);
}