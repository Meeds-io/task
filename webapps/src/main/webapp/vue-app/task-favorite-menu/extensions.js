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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

// Add the favorite toggle inside the project card 3-dots menu
// (consumed by ProjectCardFront.vue via extension-registry-components).
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
