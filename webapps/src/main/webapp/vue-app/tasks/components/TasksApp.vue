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
    <widget-wrapper :loading="loadingTasks">
      <template #title>
        <a 
          class="widget-text-header text-truncate" 
          @click="navigateTo('tasks/myTasks','ALL')">{{ $t('label.tasks.header') }}</a>
      </template>
      <template v-if="tasksSize" #action>
        <v-btn
          :title="$t('label.addTask')"
          icon
          text
          @click="openTaskDrawer">
          <v-icon>mdi-plus</v-icon>
        </v-btn>
      </template>
      <div v-if="tasksSize">
        <div v-if="tasksOverdueList.length > 0">
          <div class="d-flex align-center">
            <span class="me-2 subtitle-1">{{ $t('label.overdue') }}</span>
            <v-divider />
          </div>
          <task-details
            v-for="taskItem in tasksOverdueList"
            :key="taskItem.id"
            :task="taskItem"
            class="px-0"
            is-outdated
            @removeTask="removeTask" />
        </div>
        <div v-if="tasksTodayList.length > 0" class="mt-5">
          <div class="d-flex align-center">
            <span class="me-2 subtitle-1">{{ $t('label.today') }}</span>
            <v-divider />
          </div>
          <task-details
            v-for="taskItem in tasksTodayList"
            :key="taskItem.id"
            :task="taskItem"
            class="px-0"
            @removeTask="removeTask" />
        </div>
        <div v-if="tasksTomorrowList.length > 0" class="mt-5">
          <div class="d-flex align-center">
            <span class="me-2 subtitle-1">{{ $t('label.tomorrow') }}</span>
            <v-divider />
          </div>
          <task-details
            v-for="taskItem in tasksTomorrowList"
            :key="taskItem.id"
            :task="taskItem"
            class="px-0"
            @removeTask="removeTask" />
        </div>
        <div v-if="tasksUpcomingList.length > 0" class="mt-5">
          <div class="d-flex align-center">
            <span class="me-2 subtitle-1">{{ $t('label.upcoming') }}</span>
            <v-divider />
          </div>
          <task-details
            v-for="taskItem in tasksUpcomingList"
            :key="taskItem.id"
            :task="taskItem"
            class="px-0"
            @removeTask="removeTask" />
        </div>
      </div>
      <v-card 
        v-else 
        min-height="188"
        flat>
        <task-empty-row v-if="!loadingTasks" @open-task-drawer="openTaskDrawer" />
      </v-card>
    </widget-wrapper>
    <task-drawer
      ref="taskDrawer"
      :task="task" />
  </v-app>
</template>

<script>
import {filterTasksList} from '../../../js/tasksService';
import '../../taskCommentsDrawer/initComponents';

export default {

  data() {
    return {
      placeholder: '',
      tasks: [],
      tasksOverdue: [],
      tasksToday: [],
      tasksTomorrow: [],
      tasksUpcoming: [],
      tasksOverdueSize: '',
      tasksTodaySize: '',
      tasksTomorrowSize: '',
      tasksUpcomingSize: '',
      primaryFilterSelected: 'ALL',
      loadingTasks: true,
      TasksWithoutUpcomingSize: '',
      TasksSize: '',
      task: {
        id: null,
        status: {project: this.project},
        priority: 'NONE',
        description: '',
        title: ''
      },
      priorityStatus: ['High', 'In Normal', 'Low', 'None', null],
    };
  },computed: {
    tasksSize() {
      return this.tasks?.length;
    },
    tasksOverdueList(){
      if (this.tasksOverdue){
        if (!this.tasksToday && !this.tasksTomorrow && !this.tasksUpcoming) {
          if (this.tasksOverdueSize>6){
            return  this.tasksOverdue.slice(0, 6);
          } else {
            return this.tasksOverdue;
          }
        } else {
          return this.tasksOverdue.slice(0, 2);
        }
      }
      else {return [];}
    },
    tasksTodayList(){
      if (this.tasksToday){
        if (!this.tasksOverdue && !this.tasksTomorrow && !this.tasksUpcoming) {
          if (this.tasksTodaySize>6){
            return  this.tasksOverdue.slice(0, 6);
          } else {
            return this.tasksToday;
          }
        } else {
          return this.tasksToday.slice(0, 2);
        }
      } else {return [];}

    },
    tasksTomorrowList(){
      if (this.tasksTomorrow){
        if (!this.tasksOverdue && !this.tasksToday && !this.tasksUpcoming) {
          if (this.tasksTomorrowSize>6){
            return  this.tasksTomorrow.slice(0, 6);
          } else {
            return this.tasksTomorrow;
          }
        } else {
          return this.tasksTomorrow.slice(0, 2);
        }
      } else {return [];}
    },
    tasksUpcomingList(){
      if (this.tasksUpcoming){
        if (!this.tasksOverdue && !this.tasksToday && !this.tasksTomorrow) {
          if (this.tasksUpcomingSize>6){
            return  this.tasksUpcoming.slice(0, 6);
          } else {
            return this.tasksUpcoming;
          }
        } else {
          if (this.tasksOverdueSize > 0 && this.tasksTodaySize > 0  && this.tasksTomorrowSize > 0 ) {
            return [];
          } else {
            return this.tasksUpcoming.slice(0, 2);
          }
        }
      } else {return [];}
    },
  },
  created(){
    this.$root.$on('task-updated',task => {
      this.task=task;
    });
    
    this.$root.$on('update-task-widget-list', task => {
      this.task=task;
      this.getMyOverDueTasks();
      this.getMyTodayTasks();
      this.getMyTomorrowTasks();
      this.getMyUpcomingTasks();
    });
    this.$root.$on('show-alert', this.displayMessage);
    this.$root.$on('open-task-drawer', task => {
      this.task=task;
      this.$refs.taskDrawer.open(this.task);
    });
    this.itemsLimit = this.$parent.$data.itemsLimit;
    Promise.all([
      this.getMyOverDueTasks(),
      this.getMyTodayTasks(),
      this.getMyTomorrowTasks(),
      this.getMyUpcomingTasks()
    ]).finally(() => this.$root.$applicationLoaded());
  },
  methods: {
    getMyOverDueTasks() {
      const task = {
        dueCategory: 'overDue',
        offset: 0,
        limit: 0,
        showCompletedTasks: false,
      };
      return filterTasksList(task,'','priority','','-2').then(
        (data) => {
          this.tasksOverdue = data.tasks;
          this.tasksOverdue=this.tasksOverdue.sort((a, b) => {
            return this.priorityStatus.indexOf(a.task.priority) - this.priorityStatus.indexOf(b.task.priority);
          });
          this.tasksOverdueSize=data.tasksNumber;
          this.tasks = this.tasks.concat(this.tasksOverdue);
          this.loadingTasks = false;
        }
      );
    },
    getMyTodayTasks() {
      const task = {
        dueCategory: 'today',
        offset: 0,
        limit: 0,
        showCompletedTasks: false,
      };
      return filterTasksList(task,'','priority','','-2').then(
        (data) => {
          this.tasksToday = data.tasks;
          this.tasksToday=this.tasksToday.sort((a, b) => {
            return this.priorityStatus.indexOf(a.task.priority) - this.priorityStatus.indexOf(b.task.priority);
          });
          this.tasksTodaySize=data.tasksNumber;
          this.tasks = this.tasks.concat(this.tasksToday);
          this.loadingTasks = false;
        }
      );
    },
    getMyTomorrowTasks() {
      const task = {
        dueCategory: 'tomorrow',
        offset: 0,
        limit: 0,
        showCompletedTasks: false,
      };
      return filterTasksList(task,'','priority','','-2').then(
        (data) => {
          this.tasksTomorrow = data.tasks;
          this.tasksTomorrow=this.tasksTomorrow.sort((a, b) => {
            return this.priorityStatus.indexOf(a.task.priority) - this.priorityStatus.indexOf(b.task.priority);
          });
          this.tasksTomorrowSize=data.tasksNumber;
          this.tasks = this.tasks.concat(this.tasksTomorrow);
          this.loadingTasks = false;
        }
      );
    },
    getMyUpcomingTasks() {
      const task = {
        dueCategory: 'upcoming',
        offset: 0,
        showCompletedTasks: false,
      };
      return filterTasksList(task,'','priority','','-2').then(
        (data) => {
          this.tasksUpcoming = data.tasks;
          this.tasksUpcoming=this.tasksUpcoming.sort((a, b) => {
            return this.priorityStatus.indexOf(a.task.priority) - this.priorityStatus.indexOf(b.task.priority);
          });
          this.tasksUpcomingSize=data.tasksNumber;
          this.tasks = this.tasks.concat(this.tasksUpcoming);
          this.loadingTasks = false;
        }
      );
    },
    openTaskDrawer() {
      const defaultTask={
        id: null,
        status: {project: this.project},
        priority: 'NONE',
        description: '',
        title: ''
      };
      this.$root.$emit('open-task-drawer', defaultTask);
    },
    removeTask(value) {
      this.tasks.splice(this.tasks.findIndex(function(i){
        return i.id === value;
      }), 1);
      document.body.style.overflow = 'auto';
    },
    navigateTo(pagelink,primaryFilterSelected) {
      this.primaryFilterSelected=primaryFilterSelected;
      localStorage.setItem('primary-filter-tasks', this.primaryFilterSelected);
      location.href=`${ eXo.env.portal.context }/${ eXo.env.portal.portalName }/${ pagelink }` ;

    },
    dateFormatter(dueDate) {
      if (dueDate) {
        const date = new Date(dueDate.time);
        const day = date.getDate();
        const month = date.getMonth()+1;
        const year = date.getFullYear();
        const formattedTime = `${  year}-${  month  }-${day  }`;
        return formattedTime;
      }
    },
    retrieveTask(task){
      if (task.dueDate){
        const Today = new Date();
        const formattedTimeToday = `${  Today.getFullYear()}-${  Today.getMonth()+1  }-${Today.getDate()  }`;
        const formattedTimeTomorrow = `${  Today.getFullYear()}-${  Today.getMonth()+1  }-${Today.getDate()+1  }`;
        const date = this.dateFormatter(task.dueDate);
        if (date===formattedTimeToday){
          return this.getMyTodayTasks();
        } else if (new Date(task.dueDate.time).getTime () < Today.getTime()){
          return  this.getMyOverDueTasks();
        } else if (date===formattedTimeTomorrow){
          return this.getMyTomorrowTasks();
        } else {
          return this.getMyUpcomingTasks();
        }
      }
    },
    displayMessage(message) {
      this.$root.$emit('alert-message', message?.message, message?.type || 'success');
    }
  }
};
</script>
