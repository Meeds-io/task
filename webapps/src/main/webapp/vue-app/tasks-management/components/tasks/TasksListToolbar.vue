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
  <div id="TasksToolbar">
    <application-toolbar
      id="tasksListToolbar"
      :left-button="{
        icon: 'fa-plus',
        text: $t('label.addTask'),
      }"
      :right-text-filter="{
        minCharacters: 1,
        placeholder: $t('label.filterTask'),
        tooltip: $t('label.filterTask'),
      }"
      :right-select-box="{
        selected: primaryFilterSelected,
        items: primaryFilterItems,
      }"
      :right-filter-button="{
        text: $t('label.filter'),
        displayText: !$root.isMobile,
      }"
      :filters-count="filterNumber"
      @left-button-click="openTaskDrawer"
      @filter-text-input-end-typing="onKeyword"
      @filter-select-change="onPrimaryFilterChange"
      @filter-button-click="openDrawer" />
    <tasks-filter-drawer
      ref="filterTasksDrawer"
      :query="keyword"
      :show-completed-tasks="showCompletedTasks"
      @filter-num-changed="filterNumChanged"
      @filter-task="filterTasks"
      @reset-filter-task="resetFilterTask"
      @filter-task-query="filterTaskquery" />
  </div>
</template>
<script>
export default {
  props: {
    taskCardTab: {
      type: String,
      default: ''
    },
    taskListTab: {
      type: String,
      default: ''
    },
    showCompletedTasks: {
      type: Boolean,
      default: false
    },
  },
  data () {
    return {
      tasks: null,
      keyword: null,
      filterNumber: 0,
      primaryFilterSelected: 'ALL',
      primaryFilter: [
        {name: 'ALL'},{name: 'ASSIGNED'},{name: 'COLLABORATED'},{name: 'OVERDUE'},{name: 'TODAY'},{name: 'TOMORROW'},{name: 'UPCOMING'}
      ],
    };
  },
  computed: {
    primaryFilterItems() {
      return this.primaryFilter.map(item => ({
        value: item.name,
        text: this.$t(`label.dueDate.${item.name.toLowerCase()}`),
      }));
    },
  },
  watch: {
    filterNumber() {
      this.$emit('filter-count-changed', this.filterNumber);
    },
  },
  created() {
    this.primaryFilterSelected = localStorage.getItem('primary-filter-tasks') || 'ALL';
    localStorage.setItem('primary-filter-tasks', 'ALL');
  },
  mounted() {
    this.$emit('primary-filter-task', this.primaryFilterSelected);
  },
  methods: {
    onKeyword(term) {
      this.keyword = term;
      this.$emit('keyword-changed', term, true);
    },
    onPrimaryFilterChange(value) {
      this.primaryFilterSelected = value;
      this.keyword = '';
      this.$emit('primary-filter-task', value);
    },
    resetFilterTask(){
      this.$emit('reset-filter-task-dashboard');
    },
    filterTaskquery(e, filterGroupSort, filterLabels) {
      this.keyword = e.query;
      this.$emit('filter-task-query', e, filterGroupSort, filterLabels);
    },
    filterTasks(e) {
      this.tasks = e.tasks.tasks;
      this.$emit('filter-task-dashboard', {tasks: this.tasks, showCompletedTasks: this.showCompletedTasks});
    },
    openDrawer() {
      this.$refs.filterTasksDrawer.open();
    },
    openTaskDrawer() {
      const defaultTask = {
        id: null,
        status: {project: this.project},
        priority: 'NONE',
        description: '',
        title: ''
      };
      this.$root.$emit('open-task-drawer', defaultTask);
    },
    resetFields(activeField){
      this.keyword='';
      this.$refs.filterTasksDrawer.resetFields(activeField);
    },
    filterNumChanged(filtersnumber){
      this.filterNumber=filtersnumber;
    },
  }
};
</script>
