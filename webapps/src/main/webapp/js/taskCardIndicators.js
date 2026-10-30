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

/**
 * Small markers other add-ons put on the cards of a board, its list rows and
 * its Gantt bars. A provider registers with
 * extensionRegistry.registerExtension(EXTENSION_APP, EXTENSION_TYPE, {id, rank, load})
 * where load(projectId, taskIds) resolves to an array of
 * {taskId, label, title, color}: label is shown on the card, title says why,
 * color is a Vuetify color, 'error' by default. An indicator may instead
 * name a globally registered Vue component as `component`: the card and the
 * list row render it with the props {indicator, taskId, dense}, the whole
 * indicator passed as is, and label stays its text where no component
 * renders, the Gantt bar's popup. The board asks every
 * provider once for the cards it shows, never once per card; a provider
 * dispatches CHANGED_EVENT on document, with {detail: {taskId}}, when a
 * card's markers change, and the board asks again for all its cards.
 */
export const EXTENSION_APP = 'TaskCardIndicators';

export const EXTENSION_TYPE = 'provider';

export const CHANGED_EVENT = 'task-card-indicators-changed';

export const UPDATED_EVENT = `extension-${EXTENSION_APP}-${EXTENSION_TYPE}-updated`;

/**
 * @param {Array} items the board's items: tasks, or arrays of tasks when
 *          grouped, each a task or an item holding it as `task`
 * @returns {Array<Number>} the ids of the tasks, each once
 */
export function getTaskIds(items) {
  const ids = new Set();
  (items || []).flat().forEach(item => {
    const id = item && (item.task && item.task.id || item.id);
    if (id) {
      ids.add(Number(id));
    }
  });
  return [...ids];
}

/**
 * @returns {Array<String>} the ids of the registered providers, in rank order
 */
export function getProviderIds() {
  return (extensionRegistry.loadExtensions(EXTENSION_APP, EXTENSION_TYPE) || []).map(provider => provider.id);
}

/**
 * Asks every registered provider, once each, for the markers of some cards
 * of a board. A provider that fails is left out; the others still answer.
 *
 * @param {Number} projectId the board
 * @param {Array<Number>} taskIds the cards shown
 * @returns {Promise<Object>} {byTask, failed}: task id to its markers, in
 *          provider rank order, and whether a provider failed to answer
 */
export function loadIndicators(projectId, taskIds) {
  const providers = extensionRegistry.loadExtensions(EXTENSION_APP, EXTENSION_TYPE) || [];
  if (!projectId || !taskIds.length || !providers.length) {
    return Promise.resolve({byTask: {}, failed: false});
  }
  let failed = false;
  return Promise.all(providers.map(provider => Promise.resolve()
    .then(() => provider.load(projectId, taskIds))
    .catch(e => {
      console.error(`Error reading the card markers of ${provider.id}`, e);
      failed = true;
      return [];
    })))
    .then(answers => {
      const byTask = {};
      answers.flat().forEach(indicator => {
        if (indicator && indicator.taskId) {
          const taskId = Number(indicator.taskId);
          byTask[taskId] = byTask[taskId] || [];
          byTask[taskId].push(indicator);
        }
      });
      return {byTask, failed};
    });
}
