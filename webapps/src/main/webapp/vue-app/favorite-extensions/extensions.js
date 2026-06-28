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

// Register the Project and Task types in the global top-bar Favorites drawer.
// The drawer resolves the section label from i18n key
// `UITopBarFavoritesPortlet.types.<id>` and falls back to `name` when absent.
extensionRegistry.registerExtension('favorite', 'favorite-type', {
  rank: 60,
  id: 'project',
  name: 'Projects',
  icon: 'fa-tasks',
});
extensionRegistry.registerComponent('favorite-project', 'favorite-drawer-item', {
  id: 'project',
  vueComponent: Vue.options.components['project-favorite-item'],
});

extensionRegistry.registerExtension('favorite', 'favorite-type', {
  rank: 61,
  id: 'task',
  name: 'Tasks',
  icon: 'fa-check-square',
});
extensionRegistry.registerComponent('favorite-task', 'favorite-drawer-item', {
  id: 'task',
  vueComponent: Vue.options.components['task-favorite-item'],
});
