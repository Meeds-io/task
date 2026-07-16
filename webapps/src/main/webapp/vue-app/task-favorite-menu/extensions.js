/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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

// Favorite as an inline star injected in the project board header
// (TasksViewDashboard.vue renders the task-board-header extension point inline:
// the favorite star + the AI icon when AI is enabled). Symmetric with AI's
// project-ask-ai-board-header-action.
extensionRegistry.registerComponent('TaskProjectBoard', 'task-board-header', {
  id: 'project-favorite',
  rank: 5,
  vueComponent: Vue.options.components['project-favorite-board-header-action'],
});

// Favorite toggle as a menu row, used in the project card 3-dots menu
// (ProjectCardFront.vue) and the board header overflow 3-dots when collapsed.
extensionRegistry.registerComponent('TaskProjectMenu', 'task-project-menu', {
  id: 'project-favorite',
  rank: 5,
  vueComponent: Vue.options.components['project-favorite-menu-action'],
});

// Add the favorite toggle inside the task drawer 3-dots menu
// (consumed by TaskDrawer.vue via loadExtensions('Task', 'task-menu')).
extensionRegistry.registerExtension('Task', 'task-menu', {
  id: 'task-favorite',
  rank: 5,
  enabled: true,
  vueComponent: 'task-favorite-menu-item',
});
