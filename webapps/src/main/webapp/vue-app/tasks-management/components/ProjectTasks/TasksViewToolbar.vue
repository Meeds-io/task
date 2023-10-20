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
    <application-toolbar
      :center-button-toggle="{
        selected: taskViewTabName,
        hide: false,
        buttons: [{
          value: 'board',
          text: $t('label.boardView'),
          icon: 'far fa-clipboard',
        }, {
          value: 'list',
          text: $t('label.listView'),
          icon: 'fa-list',
        }, {
          value: 'gantt',
          text: $t('label.ganttView'),
          icon: 'fa-stream',
        }]
      }"
      :right-text-filter="taskViewTabName != 'gantt' && {
        minCharacters: 3,
        placeholder: $t('label.filterTask'),
        tooltip: $t('label.filterTask'),
      }"
      :right-filter-button="{
        text: $t('label.filter'),
      }"
      :filters-count="filterNumber"
      @toggle-select="changeTaskViewTab($event)"
      @filter-text-input-end-typing="keyword = $event"
      @filter-button-click="openDrawer">
      >
      <v-btn
        id="applicationToolbarLeftButton"
        slot="left"
        :class="isMobile && 'px-0'"
        class="text-truncate"
        outlined
        @click="$emit('close-project')">
        <v-icon
          :class="!isMobile && 'me-2'"
          class="icon-default-color"
          size="18">
          {{ $vuetify.rtl && 'fa fa-arrow-right' || 'fa fa-arrow-left' }}
        </v-icon>
        <span
          v-if="!isMobile"
          class="text-truncate text-none text-subtitle-1">
          {{ project.name }}
        </span>
      </v-btn>
    </application-toolbar>
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
  computed: {
    isMobile() {
      return this.$vuetify.breakpoint.mobile;
    },
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
      this.taskViewTabName = view;
      this.$emit('taskViewChangeTab', view);
    }
  }
};
</script>
