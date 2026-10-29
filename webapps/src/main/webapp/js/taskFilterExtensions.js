/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2026 Meeds Association
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

/*
 * The filters other add-ons contribute to a project's views. They are kept per
 * project, not per view, so the board, the list and the Gantt apply the same
 * ones and they survive a view switch. This module is their only reader and
 * writer: the drawer writes them on Confirm, Reset and chip removal, and every
 * fetch of a project's tasks reads them (fetchProjectTasks).
 * Stored shape: { <filter id>: { payload: <JSON value>, count: <active conditions> } }.
 *
 * How an add-on contributes one:
 * - UI: extensionRegistry.registerComponent('TasksFilterDrawer', 'task-filter-section',
 *   {id, vueComponent, rank}). The component is shown in the drawer's Filter tab of a
 *   project, in every view, and is created again each time the drawer opens. It
 *   receives the props {projectId, filters, update}: filters is the stored object
 *   above; it calls update(<filter id>, <payload>, <count>) on each change, a count
 *   of 0 meaning the filter restricts nothing, which is then not stored.
 * - Server: a io.meeds.task.plugin.TaskFilterPlugin whose getId() is that filter
 *   id; it receives the payload as JSON text.
 * A stored filter the server refuses (a 400 naming a task.extensionFilters.* or
 * an add-on code) is dropped, the event 'task-filter-extensions-cleared' is
 * dispatched on the document with the project id, and the fetch runs again
 * without it, so the views never stay empty on a filter that no longer applies.
 */

const STORAGE_PREFIX = 'taskFilterExtensions';

function storageKey(projectId) {
  return `${STORAGE_PREFIX}${projectId}`;
}

/**
 * @param {Number|String} projectId a project id
 * @returns {Object} the project's filters, empty when none or not a project
 */
export function getFilterExtensions(projectId) {
  if (!(Number(projectId) > 0)) {
    return {};
  }
  try {
    const stored = JSON.parse(localStorage.getItem(storageKey(projectId)) || '{}');
    return stored && typeof stored === 'object' && !Array.isArray(stored) ? stored : {};
  } catch (e) {
    return {};
  }
}

/**
 * Stores the project's filters, dropping those that restrict nothing.
 *
 * @param {Number|String} projectId a project id
 * @param {Object} filters the filters by id
 */
export function saveFilterExtensions(projectId, filters) {
  if (!(Number(projectId) > 0)) {
    return;
  }
  const active = {};
  Object.keys(filters || {}).forEach(id => {
    if (filters[id] && filters[id].count > 0) {
      active[id] = {payload: filters[id].payload, count: filters[id].count};
    }
  });
  try {
    if (Object.keys(active).length) {
      localStorage.setItem(storageKey(projectId), JSON.stringify(active));
    } else {
      localStorage.removeItem(storageKey(projectId));
    }
  } catch (e) {
    // the filters then last for this page only
  }
}

/**
 * Fetches a project's tasks with the project's filters, dropping them when the
 * server refuses them.
 *
 * @param {String} url the fetch URL, without the filters
 * @param {Number|String} projectId the project id
 * @returns {Promise<Response>} the response of the fetch that applied
 */
export function fetchProjectTasks(url, projectId) {
  const parameter = filterExtensionsParameter(projectId);
  const options = {method: 'GET', credentials: 'include'};
  return fetch(`${url}${parameter}`, options).then(resp => {
    if (!parameter || !resp || resp.status !== 400) {
      return resp;
    }
    return resp.clone().text().then(code => {
      if (!/^(task\.extensionFilters\.|forms\.)/.test(code || '')) {
        return resp;
      }
      saveFilterExtensions(projectId, {});
      document.dispatchEvent(new CustomEvent('task-filter-extensions-cleared', {detail: projectId}));
      return fetch(url, options);
    });
  });
}

/**
 * @param {Number|String} projectId a project id
 * @returns {Number} the active conditions of the project's filters
 */
export function countFilterExtensions(projectId) {
  const filters = getFilterExtensions(projectId);
  return Object.keys(filters).reduce((count, id) => count + (Number(filters[id].count) || 0), 0);
}

/**
 * @param {Number|String} projectId a project id
 * @returns {String} the « extensionFilters » query parameter of the project's
 *          fetches, URL-encoded and with its leading &, or '' when none
 */
export function filterExtensionsParameter(projectId) {
  const filters = getFilterExtensions(projectId);
  const ids = Object.keys(filters);
  if (!ids.length) {
    return '';
  }
  const payloads = {};
  ids.forEach(id => payloads[id] = filters[id].payload);
  return `&extensionFilters=${encodeURIComponent(JSON.stringify(payloads))}`;
}
