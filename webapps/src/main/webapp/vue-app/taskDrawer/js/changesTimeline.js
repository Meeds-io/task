/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
 * Merges the changes of several sources into one list, newest first, each
 * source read a page at a time. A source is an object with:
 * - id: its id;
 * - rank: its order among sources whose changes share a date, lowest first;
 * - load({before, limit}): a promise of {entries, next}, entries being the
 *   source's changes older than the cursor before (none for its first page),
 *   newest first, each as {time, key}: time in milliseconds, key ordering the
 *   source's changes of a same time, highest first; next being the cursor of
 *   its following page, or null once it has no more.
 *
 * A change is given out only while every source not yet exhausted has one
 * read ahead, so that no older change of a source is given out before a newer
 * one of another source not read yet. A source whose read fails is dropped:
 * the changes of the others still come, and the timeline tells it failed.
 *
 * @param {Array<object>} sources the sources
 * @param {number} pageSize the most changes each read of a source asks for
 * @returns {object} the timeline: next(count) gives the following changes,
 *          each as {sourceId, entry}; hasMore() tells whether any is left;
 *          hasFailed() whether a source could not be read
 */
export function createTimeline(sources, pageSize) {
  const states = sources.map(source => ({
    source,
    buffer: [],
    cursor: null,
    exhausted: false,
    failed: false,
  }));

  /**
   * @param {object} state a source's state
   * @returns {Promise} settled once the source's next page is buffered, or
   *          the source dropped
   */
  function fill(state) {
    return Promise.resolve()
      .then(() => state.source.load({before: state.cursor, limit: pageSize}))
      .then(page => {
        const entries = page && Array.isArray(page.entries) && page.entries || [];
        state.buffer.push(...entries);
        state.cursor = page && page.next || null;
        state.exhausted = !state.cursor || !entries.length;
      })
      .catch(() => {
        state.buffer = [];
        state.exhausted = true;
        state.failed = true;
      });
  }

  /**
   * @param {object} state1 a source's state, with a change buffered
   * @param {object} state2 another source's state, with a change buffered
   * @returns {number} negative when the first's next change comes first
   */
  function compareHeads(state1, state2) {
    const entry1 = state1.buffer[0];
    const entry2 = state2.buffer[0];
    if (entry1.time !== entry2.time) {
      return entry2.time - entry1.time;
    }
    const rank1 = state1.source.rank || 0;
    const rank2 = state2.source.rank || 0;
    if (rank1 !== rank2) {
      return rank1 - rank2;
    }
    return compareKeys(entry2.key, entry1.key);
  }

  /**
   * @param {number} count the most changes to give out
   * @returns {Promise<Array<object>>} the following changes, newest first,
   *          fewer than count only once no source has any left
   */
  function next(count) {
    const merged = [];

    /**
     * Refills every emptied buffer, then gives out the newest change read.
     *
     * @returns {Promise<Array<object>>} the changes given out, once count of
     *          them are or no source has any left
     */
    function step() {
      if (merged.length >= count) {
        return Promise.resolve(merged);
      }
      return Promise.all(states.filter(state => !state.buffer.length && !state.exhausted).map(fill))
        .then(() => {
          const ready = states.filter(state => state.buffer.length);
          if (!ready.length) {
            return merged;
          }
          const first = ready.reduce((best, state) => {
            return compareHeads(state, best) < 0 ? state : best;
          });
          merged.push({sourceId: first.source.id, entry: first.buffer.shift()});
          return step();
        });
    }

    return step();
  }

  /**
   * @returns {boolean} whether any change may be left to give out
   */
  function hasMore() {
    return states.some(state => state.buffer.length || !state.exhausted);
  }

  /**
   * @returns {boolean} whether a source could not be read, so that some of
   *          its changes may be missing
   */
  function hasFailed() {
    return states.some(state => state.failed);
  }

  return {next, hasMore, hasFailed};
}

/**
 * @param {number|string} key1 a change's key
 * @param {number|string} key2 another change's key of the same source
 * @returns {number} negative when key1 is lower
 */
function compareKeys(key1, key2) {
  if (typeof key1 === 'number' && typeof key2 === 'number') {
    return key1 - key2;
  }
  return String(key1).localeCompare(String(key2), [], {numeric: true});
}
