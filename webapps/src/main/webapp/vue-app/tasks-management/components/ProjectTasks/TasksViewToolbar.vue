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
  <v-app>
    <v-toolbar
      id="TasksDashboardToolbar"
      flat
      class="tasksToolbar">
      <div class="taskDisplay">
        <v-tabs
          class="projectTasksViewTabs">
          <v-tab
            :href="taskCardTabView"
            class="taskTabBoard"
            @change="changeTaskViewTab('board')">
            <i class="uiIcon uiIconBoard"></i>
            <span>{{ $t('label.boardView') }}</span>
          </v-tab>
          <v-tab
            :href="taskListTabView"
            class="taskTabList"
            @change="changeTaskViewTab('list')">
            <i class="uiIcon uiIconList"></i>
            <span>{{ $t('label.listView') }}</span>
          </v-tab>
          <v-tab
            :href="taskGanttTabView"
            class="taskTabGantt"
            @change="changeTaskViewTab('gantt')">
            <i class="uiIcon uiIconGantt"></i>
            <span>{{ $t('label.ganttView') }}</span>
          </v-tab>
        </v-tabs>
      </div>
      <v-spacer />
      <v-scale-transition>
        <v-text-field
          v-if="taskViewTabName != 'gantt'"
          v-model="keyword"
          :placeholder=" $t('label.filterTask') "
          prepend-inner-icon="fa-filter"
          class="inputTasksFilter pa-0 me-3 my-auto"
          clearable />
      </v-scale-transition>
      <v-scale-transition>
        <v-btn
          class="btn px-2 btn-primary filterTasksSetting"
          outlined
          @click="openDrawer">
          <i class="uiIcon uiIconFilterSetting pe-3"></i>
          <span class="d-none font-weight-regular caption d-sm-inline">
            {{ $t('label.filter') }} {{ getFilterNum() }}
          </span>
        </v-btn>
      </v-scale-transition>
    </v-toolbar>
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
  </v-app>
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
    taskCardTabView: {
      type: String,
      default: ''
    },
    taskListTabView: {
      type: String,
      default: ''
    },
    taskGanttTabView: {
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
      keyword: null,
      awaitingSearch: false,
      filterNumber: 0,
      searchonkeyChange: true,
      taskViewTabName: '',
    };
  },
  watch: {        
    keyword() {  
      if (!this.awaitingSearch) {
        const searchonkeyChange = this.searchonkeyChange;
        setTimeout(() => {
          this.$emit('keyword-changed', this.keyword,searchonkeyChange);
          this.awaitingSearch = false;
        }, 1000);
      }
      this.awaitingSearch = true;
      if (this.searchonkeyChange){
        this.resetFields('query'); }      
      this.searchonkeyChange= true;
    },
  },
  created() {
    this.$root.$on('hide-tasks-project', () => {
      $('a.v-tab').removeClass('v-tab--active');
      $('a.taskTabBoard').addClass('v-tab--active');
      this.keyword = null ;
    });
  },
  methods: {
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
      this.searchonkeyChange=false;
      this.keyword='';
      this.searchonkeyChange=true;
      this.$emit('reset-filter-task-dashboard');
    },
    filterTasks(e) {
      this.searchonkeyChange = false;
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
    getFilterNum(){
      if (this.filterNumber>0){
        return `(${this.filterNumber})`;
      } return '';
    },
    changeTaskViewTab(view){
      $('a.v-tab').removeClass('v-tab--active');
      if ( view === 'list') {
        $('a.taskTabList').addClass('v-tab--active');
      }

      if ( view === 'board') {
        $('a.taskTabBoard').addClass('v-tab--active');
      }

      if ( view === 'gantt') {
        $('a.taskTabGantt').addClass('v-tab--active');
      }
      this.taskViewTabName=view;
      this.$emit('taskViewChangeTab', view);
    }
  }
};
</script>
