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
  <v-app id="TasksManagementPortlet">
    <div class="application-body">
      <v-tabs
        v-if="showTabs"
        v-model="tab"
        slider-size="4"
        class="tasksMenuParent application-background-color application-background-image application-border application-border-radius-top">
        <v-tab href="#tab-2" @click="getMyProjects()">
          {{ $t('label.projects') }}
        </v-tab>
        <v-tab href="#tab-1" @click="getMyTasks()">
          {{ $t('label.tasks.header') }}
        </v-tab>
      </v-tabs>
      <v-tabs-items
        v-model="tab"
        :class="showTabs && 'application-border-radius-bottom' || 'application-border-radius'"
        class="application-background-color application-background-image application-border">
        <v-tab-item value="tab-1">
          <tasks-dashboard />
        </v-tab-item>
        <v-tab-item value="tab-2">
          <project-dashboard :space-name="spaceName" :display-details="displayDetails" />
        </v-tab-item>
      </v-tabs-items>
    </div>
    <add-project-drawer
      ref="addProjectDrawer" />

    <task-drawer
      ref="taskDrawer"
      :task="task" />

    <tasks-assignee-coworker-drawer />
  </v-app>
</template>
<script>
// The suffixes the app adds after the page holding it, each a whole path segment
const URL_SUFFIX_PATTERN = /\/(projectDetail|taskDetail|myTasks|myProjects)(\/|$)/;

export default {
  data () {
    return {
      tab: '',
      displayDetails: false,
      showTabs: false,
      spaceName: '',
      projectId: '',
      task: {
        type: Object,
        default: () => ({}),
      }
    };
  },
  created(){
    this.$root.$on('show-alert', this.displayMessage);
    this.$root.$on('open-project-drawer', project => {
      this.$refs.addProjectDrawer.open(project);
    });
    this.$root.$on('show-project-details-tasks',project =>{
      document.dispatchEvent(new CustomEvent('showProjectTasks', {detail: project}));
      this.tab='tab-2';
    });
    this.$root.$on('set-url',context =>{

      if (context.type==='task'){
        this.setTaskUrl(context.id);
      }
      if (context.type==='project') {
        if (this.isTaskDisplayed()) {
          this.showProjectDetails(context.id);
        } else {
          this.setProjectUrl(context.id);
        }
      }
      if (context.type==='myProjects'){
        this.getMyProjects();
      }
    });
    this.$root.$on('open-task-drawer', task => {
      this.task=task;
      if (task.id){
        this.setTaskUrl(task.id);
      }
      this.$refs.taskDrawer.open(task);
    });
    this.$root.$on('task-drawer-closed', task => {
      if (this.tab==='tab-1'){
        this.getMyTasks();
      }

      else if (task && task.status && task.status.project) {
        this.setProjectUrl(task.status.project.id);
      } else {
        this.tab='tab-1';
      }
    });
    // The suffix is read where getPageUrl() cuts it, so the address is read as it was built
    const urlSuffix = this.getUrlSuffix();
    const [, suffixName, suffixId] = urlSuffix.match(/^\/(projectDetail|taskDetail|myTasks|myProjects)(?:\/(\d+))?/) || [];
    const isMyTasksUrl = suffixName === 'myTasks';
    if (isMyTasksUrl){
      this.tab='tab-1';
      this.projectId='';
      this.showTabs=true;
    } else if (suffixName === 'taskDetail'){
      const taskId = Number(suffixId) || 0;
      if (taskId) {
        this.$tasksService.getTaskById(taskId).then(data => {
          this.task = data;
          if (this.task.status && this.task.status.project){
            this.displayDetails=true;
            this.projectId=this.task.status.project.id;
            this.showTabs=false;
            window.setTimeout(() => {
              document.dispatchEvent(new CustomEvent('showProjectTasks', {detail: this.task.status.project}));
            }, 20);
            this.tab='tab-2';
          } else {
            this.tab='tab-1';
            this.projectId='';
            this.showTabs=true;
          }
          window.setTimeout(() => {
            this.$refs.taskDrawer.open(this.task);
          }, 200);
        })
          .catch(() => this.showDefaultView('popup.msg.taskNotAccessible'))
          .finally(() => this.$root.$applicationLoaded());
      } else {
        this.showDefaultView('popup.msg.taskNotAccessible');
      }
    } else if (suffixName === 'projectDetail'){
      const projectId = Number(suffixId) || 0;
      if (projectId) {
        this.projectId=projectId;
        this.showTabs=false;
        this.$projectService.getProject(projectId).then(data => {
          document.dispatchEvent(new CustomEvent('showProjectTasks', {detail: data}));
        })
          .catch(() => this.showDefaultView('popup.msg.projectNotAccessible'))
          .finally(() => this.$root.$applicationLoaded());
        this.displayDetails=true;
        this.tab='tab-2';
      } else {
        this.showDefaultView('popup.msg.projectNotAccessible');
      }
    } else {
      this.showTabs=true;
      this.tab='tab-2';
    }
    if (eXo.env.portal.spaceName){
      this.spaceName = eXo.env.portal.spaceName;
      if (!isMyTasksUrl) {
        this.tab='tab-2';
      }
    }
  },
  methods: {
    getMyTasks(){
      this.showTabs=true;
      this.displayDetails=false;
      window.history.pushState('mytasks', 'My Tasks', `${this.getPageUrl()}/myTasks`);
    },
    getMyProjects(){
      this.projectId='';
      this.showTabs=true;
      this.displayDetails=false;
      if (eXo.env.portal.spaceName){
        window.history.pushState('task', 'Task details', this.getPageUrl());
      } else {
        window.history.pushState('myprojects', 'My Projects', `${this.getPageUrl()}/myProjects`);
      }
    },
    setTaskUrl(id){
      // this.projectId='';
      window.history.pushState('task', 'Task details', `${this.getPageUrl()}/taskDetail/${id}`);
    },
    setProjectUrl(id){
      this.showProjectDetails(id);
      window.history.pushState('task', 'Task details', `${this.getPageUrl()}/projectDetail/${id}`);
    },
    getPageUrl() {
      // The URL of the page holding the app, whatever its node is named, unless a node of the
      // path is itself named after a suffix the app adds: the current path without that suffix
      // (/projectDetail/<id>, /taskDetail/<id>, /myTasks, /myProjects). The portal serves that
      // page for any deeper path, and the app reads the suffix back when the page is reloaded.
      const urlPath = document.location.pathname;
      const suffixIndex = urlPath.search(URL_SUFFIX_PATTERN);
      return (suffixIndex >= 0 ? urlPath.substring(0, suffixIndex) : urlPath).replace(/\/+$/, '');
    },
    getUrlSuffix() {
      const urlPath = document.location.pathname;
      const suffixIndex = urlPath.search(URL_SUFFIX_PATTERN);
      return suffixIndex >= 0 ? urlPath.substring(suffixIndex) : '';
    },
    showDefaultView(messageKey) {
      // The project or task of the address cannot be loaded (refused or deleted): the project
      // list of the page, under the page's own address, with the reason in an alert.
      this.projectId='';
      this.displayDetails=false;
      this.showTabs=true;
      this.tab='tab-2';
      window.history.replaceState('task', 'Task details', this.getPageUrl());
      this.displayMessage({type: 'error', message: this.$t(messageKey)});
    },
    showProjectDetails(id){
      this.projectId=id;
      this.showTabs=false;
      this.displayDetails=true;
    },
    isTaskDisplayed() {
      return !!(this.$refs.taskDrawer?.drawer && this.$refs.taskDrawer?.taskId);
    },
    displayMessage(message) {
      this.$root.$emit('alert-message', message?.message, message?.type || 'success');
    }
  }
};
</script>