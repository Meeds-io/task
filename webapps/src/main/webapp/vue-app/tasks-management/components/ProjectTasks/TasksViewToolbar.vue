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
  <div>
    <div class="projectBoardToolbar d-flex align-center">
      <!-- Search expanded: back arrow + full-width input (same scheme as Agenda/Documents) -->
      <template v-if="searchOpen">
        <v-btn
          class="px-0 me-2 flex-shrink-0"
          small
          icon
          @click="closeSearch">
          <v-icon size="22" class="icon-default-color">fa-arrow-left</v-icon>
        </v-btn>
        <v-text-field
          ref="searchField"
          v-model="keyword"
          :placeholder="$t('label.filterTask')"
          prepend-inner-icon="fa-filter"
          class="flex-grow-1 pa-0 my-auto"
          autocomplete="off"
          hide-details
          clearable
          @input="onKeyword"
          @keydown.esc="closeSearch" />
      </template>
      <!-- Default: breadcrumb on the left, all actions grouped on the right -->
      <template v-else>
        <div class="d-flex align-center text-truncate flex-grow-1">
          <slot name="left"></slot>
        </div>
        <extension-registry-components
          :params="{ project }"
          name="TaskProjectBoard"
          type="task-board-header"
          parent-element="div"
          element="div"
          class="boardHeaderExtensions d-flex align-center flex-shrink-0" />
        <tasks-view-switcher
          v-if="!$root.isMobile"
          :view="taskViewTabName"
          class="flex-shrink-0"
          @change="changeTaskViewTab" />
        <v-btn
          v-if="taskViewTabName !== 'gantt'"
          :title="$t('label.filterTask')"
          class="flex-shrink-0"
          max-width="36"
          max-height="36"
          icon
          @click="openSearch">
          <v-icon
            size="20"
            :class="keyword && keyword.length && 'primary--text' || 'text-light-color'">
            fa-filter
          </v-icon>
        </v-btn>
        <v-btn
          :title="$t('label.filter')"
          class="flex-shrink-0"
          max-width="36"
          max-height="36"
          icon
          @click="openDrawer">
          <v-badge
            :value="filterNumber > 0"
            :content="filterNumber"
            color="primary"
            overlap
            bordered>
            <v-icon
              size="20"
              :class="filterNumber > 0 && 'primary--text' || 'text-light-color'">
              fa-sliders-h
            </v-icon>
          </v-badge>
        </v-btn>
      </template>
    </div>
    <tasks-filter-drawer
      ref="filterTasksDrawer"
      :project="project.id"
      :query="keyword"
      :status-list="statusList"
      :task-view-tab-name="taskViewTabName"
      :show-completed-tasks="showCompletedTasks"
      @filter-num-changed="filterNumChanged"
      @filter-task="filterTasks"
      @reset-filter-task="resetFilterTask" />
  </div>
</template>
<script>
export default {
  props: {
    project: {
      type: Object,
      default: null
    },
    statusList: {
      type: Array,
      default: () => []
    },
    showCompletedTasks: {
      type: Boolean,
      default: false
    },
  },
  data () {
    return {
      keyword: null,
      filterNumber: 0,
      taskViewTabName: 'board',
      searchOpen: false,
      searchTimer: null,
    };
  },
  created() {
    this.filterNumber = Number(localStorage.getItem('filtersNumber')) || 0;
    this.$root.$on('hide-tasks-project', () => {
      this.taskViewTabName = 'board';
      this.keyword = null;
      this.searchOpen = false;
    });
  },
  methods: {
    openSearch() {
      this.searchOpen = true;
      this.$nextTick(() => this.$refs.searchField && this.$refs.searchField.focus());
    },
    closeSearch() {
      this.searchOpen = false;
    },
    onKeyword(term) {
      this.keyword = term;
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
      }
      this.searchTimer = setTimeout(() => {
        this.$emit('keyword-changed', this.keyword, true);
      }, 600);
    },
    openDrawer() {
      this.$refs.filterTasksDrawer.open();
    },
    openTaskDrawer() {
      const defaultTask= {id: null,
        status: {project: this.project},
        priority: 'NONE',
        description: '',
        title: ''};
      this.$root.$emit('open-task-drawer', defaultTask);
    },
    resetFilterTask(){
      this.keyword = '';
      this.$emit('reset-filter-task-dashboard');
    },
    filterTasks(e) {
      this.keyword = e.tasks.query;
      this.$emit('filter-task-dashboard', {
        tasks: e.tasks,
        filterLabels: e.filterLabels,
        showCompletedTasks: e.showCompletedTasks
      });
    },
    resetFields(activeField){
      this.$refs.filterTasksDrawer.resetFields(activeField);
    },
    filterNumChanged(filtersnumber){
      this.filterNumber=filtersnumber;
    },
    changeTaskViewTab(view){
      this.taskViewTabName=view;
      this.$emit('taskViewChangeTab', view);
    }
  }
};
</script>
