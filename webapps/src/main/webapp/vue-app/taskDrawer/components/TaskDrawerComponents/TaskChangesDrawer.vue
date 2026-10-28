<!--
  This file is part of the Meeds project (https://meeds.io/).
  Copyright (C) 2022 Meeds Association
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
  <exo-drawer
    ref="taskChangesDrawer"
    v-model="showTaskChangesDrawer"
    :class="showTaskChangesDrawer && 'showTaskChangesDrawer' || ''"
    class="taskCommentDrawer"
    right
    go-back-button>
    <template #title>
      {{ $t('label.changes') }}
    </template>
    <template #content>
      <task-changes-tabs
        v-if="tabs.length"
        :tabs="tabs"
        :selected-index="selectedIndex"
        @change="chooseTab" />
      <v-progress-linear
        v-if="tabsLoading && requestedTab !== selectedTabId"
        indeterminate
        color="primary" />
      <component
        :is="selectedTab.vueComponent"
        v-else-if="selectedTab && showTaskChangesDrawer"
        :key="`${selectedTab.id}-${taskId}`"
        :task-id="taskId"
        :project-id="projectId" />
      <task-changes-log v-else :logs="logs" />
    </template>
  </exo-drawer>
</template>
<script>
/** The tab showing the task's own change log, always first. */
const TASK_TAB = 'task';
/** The type under which another add-on registers a tab, on the TaskDrawer extension app. */
const TAB_TYPE = 'task-changes-tab';

export default {
  props: {
    logs: {
      type: Object,
      default: () => ({}),
    },
    task: {
      type: Object,
      default: null
    },
  },
  data() {
    return {
      showTaskChangesDrawer: false,
      tabs: [],
      tabsKey: null,
      tabsRequest: 0,
      tabsLoading: false,
      requestedTab: TASK_TAB,
      selectedIndex: 0,
    };
  },
  computed: {
    taskId() {
      return this.task && this.task.id || null;
    },
    projectId() {
      return this.task && this.task.status && this.task.status.project && this.task.status.project.id || null;
    },
    selectedTab() {
      return this.selectedIndex > 0 && this.tabs[this.selectedIndex - 1] || null;
    },
    selectedTabId() {
      return this.selectedTab && this.selectedTab.id || TASK_TAB;
    },
  },
  created() {
    document.addEventListener(`component-TaskDrawer-${TAB_TYPE}-updated`, this.refreshOpenTabs);
  },
  mounted() {
    this.$root.$on('displayTaskChanges', this.open);
    this.$root.$on('hideTaskChanges', this.closeDrawer);
  },
  beforeDestroy() {
    document.removeEventListener(`component-TaskDrawer-${TAB_TYPE}-updated`, this.refreshOpenTabs);
    this.$root.$off('displayTaskChanges', this.open);
    this.$root.$off('hideTaskChanges', this.closeDrawer);
  },
  methods: {
    /**
     * Opens the drawer, on the tab of the given id once it is known to apply
     * to this task, on the task's own changes otherwise.
     *
     * @param {string} [tabId] the id of the tab to open; the task's own changes by default
     * @returns {Promise} settled once the tabs applying to the task are shown
     */
    open(tabId) {
      this.requestedTab = typeof tabId === 'string' && tabId || TASK_TAB;
      this.showTaskChangesDrawer = true;
      this.selectRequestedTab();
      return this.refreshTabs();
    },
    /**
     * Closes the drawer.
     *
     * @returns {void}
     */
    closeDrawer() {
      this.showTaskChangesDrawer = false;
    },
    /**
     * Re-reads the tabs when one is registered while the drawer is open; a
     * closed drawer reads them when it opens.
     *
     * @returns {Promise} settled once the tabs are shown
     */
    refreshOpenTabs() {
      return this.showTaskChangesDrawer && this.refreshTabs() || Promise.resolve();
    },
    /**
     * Reads the tabs other add-ons registered, keeps those that apply to the
     * task, the last one registered per id, ordered by rank, and selects the
     * requested one. The tabs of another task are dropped at once, and an
     * answer arriving after a newer read is ignored. While the read is
     * pending, a drawer asked to open on another tab than the task's own
     * changes shows that it is loading rather than those changes.
     *
     * @returns {Promise} settled once the tabs are shown or the answer dropped
     */
    refreshTabs() {
      const request = ++this.tabsRequest;
      const params = {taskId: this.taskId, projectId: this.projectId};
      const key = `${params.taskId}-${params.projectId}`;
      if (this.tabsKey !== key) {
        this.tabsKey = key;
        this.tabs = [];
        this.selectRequestedTab();
      }
      const byId = new Map();
      (this.taskId && extensionRegistry.loadComponents('TaskDrawer') || [])
        .filter(component => component.componentName === TAB_TYPE)
        .forEach(component => byId.set(component.componentOptions.id, component.componentOptions));
      const registered = [...byId.values()];
      this.tabsLoading = true;
      return Promise.all(registered.map(tab => this.isTabEnabled(tab, params)))
        .then(enabled => {
          if (request === this.tabsRequest) {
            this.tabs = registered.filter((tab, index) => enabled[index])
              .sort((tab1, tab2) => (tab1.rank || 0) - (tab2.rank || 0));
            this.selectRequestedTab();
            this.tabsLoading = false;
          }
        });
    },
    /**
     * @param {object} tab the options a tab was registered with
     * @param {object} params the task's and its project's ids
     * @returns {Promise<boolean>} whether the tab applies to the task; not when its check fails
     */
    isTabEnabled(tab, params) {
      if (!tab.isEnabled) {
        return Promise.resolve(true);
      }
      return Promise.resolve()
        .then(() => tab.isEnabled(params))
        .then(enabled => !!enabled)
        .catch(() => false);
    },
    /**
     * Selects the requested tab when it is shown, the task's own changes otherwise.
     *
     * @returns {void}
     */
    selectRequestedTab() {
      const index = this.tabs.findIndex(tab => tab.id === this.requestedTab);
      this.selectedIndex = index + 1;
    },
    /**
     * @param {number} index the index of the tab the user chose, the task's own changes being 0
     * @returns {void}
     */
    chooseTab(index) {
      this.selectedIndex = index;
      this.requestedTab = index > 0 && this.tabs[index - 1] && this.tabs[index - 1].id || TASK_TAB;
    },
  }
};
</script>