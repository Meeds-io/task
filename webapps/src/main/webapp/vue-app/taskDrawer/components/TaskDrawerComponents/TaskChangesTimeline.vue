<!--
  This file is part of the Meeds project (https://meeds.io/).
  Copyright (C) 2026 Meeds Association
  contact@meeds.io
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <div class="taskChangesTimeline">
    <v-chip-group
      v-if="sources.length > 1"
      :value="selected"
      class="taskChangesFilter px-4"
      mandatory
      @change="select">
      <v-chip
        :value="ALL"
        class="taskChangesFilterAll"
        active-class="primary--text"
        small
        outlined>
        {{ $t('label.changes.filter.all') }}
      </v-chip>
      <v-chip
        v-for="source in sources"
        :key="source.id"
        :value="source.id"
        :class="`taskChangesFilter-${source.id}`"
        active-class="primary--text"
        small
        outlined>
        {{ sourceLabel(source) }}
      </v-chip>
    </v-chip-group>
    <v-list class="taskChangesTimelineList py-0">
      <component
        :is="sourceComponent(line.sourceId)"
        v-for="line in lines"
        :key="`${line.sourceId}-${line.entry.key}`"
        :entry="line.entry"
        :task-id="taskId" />
    </v-list>
    <v-progress-linear
      v-if="loading"
      indeterminate
      color="primary" />
    <div
      v-else-if="failed"
      class="taskChangesTimelineError text-light-color text-body-2 pa-4">
      {{ $t('label.changes.error') }}
    </div>
    <div
      v-else-if="!lines.length && !hasMore"
      class="taskChangesTimelineEmpty text-light-color text-body-2 pa-4">
      {{ $t('label.changes.empty') }}
    </div>
    <div ref="end" class="taskChangesTimelineEnd"></div>
  </div>
</template>
<script>
import {createTimeline} from '../../js/changesTimeline.js';

/** The changes given out per scroll, and asked of each source per read. */
const PAGE_SIZE = 20;
/** The type under which another add-on feeds the timeline, on the TaskDrawer extension app. */
const SOURCE_TYPE = 'task-changes-source';
/** The id of the task's own changes, always shown. */
const TASK_SOURCE = 'task';

export default {
  props: {
    taskId: {
      type: [Number, String],
      default: null,
    },
    projectId: {
      type: [Number, String],
      default: null,
    },
  },
  data: () => ({
    ALL: 'all',
    sources: [],
    selected: 'all',
    lines: [],
    loading: false,
    hasMore: false,
    failed: false,
    timeline: null,
    generation: 0,
    observer: null,
  }),
  /**
   * Reads the sources, and again when an add-on registers one late.
   *
   * @returns {void}
   */
  created() {
    document.addEventListener(`component-TaskDrawer-${SOURCE_TYPE}-updated`, this.refreshSources);
    this.loadSources();
  },
  /**
   * Loads the following changes whenever the end of the list comes into view.
   *
   * @returns {void}
   */
  mounted() {
    this.observer = new IntersectionObserver(observed => {
      if (observed.some(entry => entry.isIntersecting)) {
        this.loadMore();
      }
    });
    this.observer.observe(this.$refs.end);
  },
  /**
   * Drops every answer still to come and stops watching the list and the registry.
   *
   * @returns {void}
   */
  beforeDestroy() {
    document.removeEventListener(`component-TaskDrawer-${SOURCE_TYPE}-updated`, this.refreshSources);
    this.generation++;
    if (this.observer) {
      this.observer.disconnect();
    }
  },
  methods: {
    /**
     * Collects the task's own changes and the sources other add-ons
     * registered, the last one registered per id, that apply to the task,
     * ordered by rank, then shows the changes of all of them.
     *
     * @returns {Promise} settled once the first changes are shown
     */
    loadSources() {
      const generation = ++this.generation;
      const params = {taskId: this.taskId, projectId: this.projectId};
      const byId = new Map();
      (extensionRegistry.loadComponents('TaskDrawer') || [])
        .filter(component => component.componentName === SOURCE_TYPE && component.componentOptions.id !== TASK_SOURCE)
        .forEach(component => byId.set(component.componentOptions.id, component.componentOptions));
      const registered = [...byId.values()];
      this.loading = true;
      return Promise.all(registered.map(source => this.isSourceEnabled(source, params)))
        .then(enabled => {
          if (generation === this.generation) {
            this.sources = [this.taskSource(), ...registered.filter((source, index) => enabled[index])]
              .sort((source1, source2) => (source1.rank || 0) - (source2.rank || 0));
            this.select(this.ALL);
          }
        });
    },
    /**
     * Reads the sources again when one is registered after the timeline
     * opened, while it shows no change yet, so that no line read is lost.
     *
     * @returns {Promise} settled once the first changes are shown, or at once
     *          when changes already show
     */
    refreshSources() {
      return this.lines.length && Promise.resolve() || this.loadSources();
    },
    /**
     * @param {object} source the options a source was registered with
     * @param {object} params the task's and its project's ids
     * @returns {Promise<boolean>} whether the source applies to the task; not when its check fails
     */
    isSourceEnabled(source, params) {
      if (!source.isEnabled) {
        return Promise.resolve(true);
      }
      return Promise.resolve()
        .then(() => source.isEnabled(params))
        .then(enabled => !!enabled)
        .catch(() => false);
    },
    /**
     * @returns {object} the source of the task's own changes, each keyed by
     *          its id and dated by its creation
     */
    taskSource() {
      return {
        id: TASK_SOURCE,
        labelKey: 'label.changes.tab.task',
        rank: 0,
        vueComponent: Vue.options.components['task-changes-timeline-task-line'],
        load: ({before, limit}) => this.$taskDrawerApi.getTaskLogsPage(this.taskId, before, limit)
          .then(logs => {
            const entries = (logs || []).map(log => ({time: log.createdTime, key: log.id, log}));
            const last = entries[entries.length - 1];
            return {
              entries,
              next: entries.length === limit && {time: last.time, id: last.key} || null,
            };
          }),
      };
    },
    /**
     * Shows, from the newest, the changes of the chosen source, of all of
     * them for « All ».
     *
     * @param {string} sourceId the id of the chosen source, or all
     * @returns {Promise} settled once the first changes are shown
     */
    select(sourceId) {
      this.selected = sourceId || this.ALL;
      const chosen = this.selected === this.ALL && this.sources
        || this.sources.filter(source => source.id === this.selected);
      this.generation++;
      this.timeline = createTimeline(chosen.map(source => ({
        id: source.id,
        rank: source.rank || 0,
        load: ({before, limit}) => source.load({taskId: this.taskId, projectId: this.projectId, before, limit}),
      })), PAGE_SIZE);
      this.lines = [];
      this.hasMore = true;
      this.failed = false;
      this.loading = false;
      return this.loadMore();
    },
    /**
     * Shows the following changes, then more while the end of the list stays
     * in view, since scrolling cannot reveal it again.
     *
     * @returns {Promise} settled once they are shown, or dropped when the
     *          choice changed meanwhile
     */
    loadMore() {
      if (this.loading || !this.hasMore || !this.timeline) {
        return Promise.resolve();
      }
      const generation = this.generation;
      const timeline = this.timeline;
      this.loading = true;
      return timeline.next(PAGE_SIZE).then(lines => {
        if (generation !== this.generation) {
          return;
        }
        this.lines.push(...lines);
        this.hasMore = timeline.hasMore();
        this.failed = timeline.hasFailed();
        this.loading = false;
        // the end marker is measured once the new lines are rendered
        return this.$nextTick().then(() => this.isEndInView() && this.loadMore());
      });
    },
    /**
     * @returns {boolean} whether the end of the list shows in the window
     */
    isEndInView() {
      const end = this.$refs.end;
      if (!end) {
        return false;
      }
      const bounds = end.getBoundingClientRect();
      return bounds.top < window.innerHeight && bounds.bottom >= 0;
    },
    /**
     * @param {object} source a source
     * @returns {string} its label, translated when it gives a key
     */
    sourceLabel(source) {
      return source.labelKey && this.$te(source.labelKey) && this.$t(source.labelKey) || source.label || source.id;
    },
    /**
     * @param {string} sourceId a source's id
     * @returns {object} the component showing one of its changes
     */
    sourceComponent(sourceId) {
      const source = this.sources.find(candidate => candidate.id === sourceId);
      return source && source.vueComponent;
    },
  },
};
</script>
