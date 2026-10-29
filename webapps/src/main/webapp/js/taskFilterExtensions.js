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
 * Stored shape: { <filter id>: { payload: <JSON value>, count: <active conditions>,
 * state: <what the section shows again, never sent> } }.
 *
 * How an add-on contributes one:
 * - UI: extensionRegistry.registerComponent('TasksFilterDrawer', 'task-filter-section',
 *   {id, vueComponent, rank}). The component is shown in the drawer's Filter tab of a
 *   project, in every view, and is created again each time the drawer opens. It
 *   receives the props {projectId, filters, update}: filters is the stored object
 *   above; it calls update(<filter id>, <payload>, <count>, <state>) on each change,
 *   a count of 0 meaning the filter restricts nothing, which is then not stored.
 *   Only the payload is sent with the fetches; the optional state is kept for the
 *   section to show its rows again.
 * - UI, a card-state switch: the same registration with the type 'task-filter-switch'
 *   places the component among the drawer's switches (« Favorites only », « Show
 *   completed tasks »), after them and before the sections; it receives the same props.
 * - Server: a io.meeds.task.plugin.TaskFilterPlugin whose getId() is that filter
 *   id; it receives the payload as JSON text.
 * When the server refuses the stored filters (a 400 with a task.extensionFilters.*
 * code), the filter it names (task.extensionFilters.refused:<id> or .unknown:<id>)
 * is dropped, or all of them when it names none; the event
 * 'task-filter-extensions-cleared' is dispatched on the document with the project
 * id, and the fetch runs again with what is left, so the views never stay empty
 * on a filter that no longer applies, one refused filter after another. A
 * refusal of filters that were replaced while the fetch ran drops nothing: the
 * fetch runs again with what is stored now.
 */

const STORAGE_PREFIX = 'taskFilterExtensions';

/** How many times filters replaced while a fetch ran may start a new budget of attempts. */
const MAX_REPLACEMENTS = 2;

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
      active[id] = {payload: filters[id].payload, count: filters[id].count, state: filters[id].state};
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
  // The server names one refused filter per answer: one attempt per stored filter, plus the last one with
  // none, bounds the loop; filters replaced meanwhile get a budget of their own, a bounded number of times
  return fetchWithFilters(url, projectId, Object.keys(getFilterExtensions(projectId)).length + 1, MAX_REPLACEMENTS);
}

/**
 * One attempt of fetchProjectTasks: fetches with the stored filters and, on a
 * refusal of them, drops the refused one and tries again.
 *
 * @param {String} url the fetch URL, without the filters
 * @param {Number|String} projectId the project id
 * @param {Number} attempts the attempts left, this one included
 * @param {Number} replacements how many more times filters replaced while a
 *          fetch ran may start a new budget of attempts
 * @returns {Promise<Response>} the response of the fetch that applied
 */
function fetchWithFilters(url, projectId, attempts, replacements) {
  const parameter = filterExtensionsParameter(projectId);
  const options = {method: 'GET', credentials: 'include'};
  return fetch(`${url}${parameter}`, options).then(resp => {
    if (!parameter || !resp || resp.status !== 400 || attempts <= 1) {
      return resp;
    }
    return resp.clone().text().then(code => {
      const refusal = /^task\.extensionFilters\.[a-zA-Z]+(?::(.+))?$/.exec(code || '');
      if (!refusal) {
        return resp;
      }
      if (filterExtensionsParameter(projectId) !== parameter) {
        return replacements > 0 ? fetchWithFilters(url, projectId, Object.keys(getFilterExtensions(projectId)).length + 1, replacements - 1)
                                : resp;
      }
      const filters = getFilterExtensions(projectId);
      if (refusal[1]) {
        delete filters[refusal[1]];
      }
      saveFilterExtensions(projectId, refusal[1] ? filters : {});
      document.dispatchEvent(new CustomEvent('task-filter-extensions-cleared', {detail: projectId}));
      return fetchWithFilters(url, projectId, attempts - 1, replacements);
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
