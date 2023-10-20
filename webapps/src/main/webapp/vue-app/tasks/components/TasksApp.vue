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
  <v-app
    id="tasks"
    class="VuetifyApp"
    flat>
    <v-container white pa-0>
      <v-layout
        row
        mx-3
        class="white">
        <v-flex
          d-flex
          xs12>
          <v-layout
            row
            mx-0
            align-center>
            <v-flex
              d-flex
              xs12>
              <v-flex class="d-flex my-2">
                <div class="d-flex align-center">
                  <a
                    class="body-1 text-uppercase color-title px-0"
                    @click="navigateTo('tasks/myTasks','ALL')">
                    {{ $t('label.tasks.header') }}
                  </a>
                </div>
                <v-spacer />
                <v-btn
                  :title="$t('label.addTask')"
                  icon
                  text
                  @click="openTaskDrawer">
                  <v-icon size="14">
                    fa-plus
                  </v-icon>
                </v-btn>
              </v-flex>
            </v-flex>
          </v-layout>
        </v-flex>
        <v-flex
          :class="tasks.length > 0"
          d-flex
          xs12>
          <v-layout
            row
            mx-0>
            <v-flex
              xs12>
              <div v-if="tasks.length > 0">
                <div v-if="tasksOverdueSize > 0">
                  <div class="nameGroup">
                    <span class="nameGroup">{{ $t('label.overdue') }}</span>
                    <div class="amount-item pointer" @click="navigateTo('tasks/myTasks','OVERDUE')">{{ tasksOverdueSize }}</div>
                    <hr
                      role="separator"
                      aria-orientation="horizontal"
                      class="v-divider theme&#45;&#45;light separator">
                  </div>
                  <div
                    v-for="taskItem in tasksOverdueList"
                    :key="taskItem.id"
                    class="list-item">
                    <task-details :task="taskItem" @removeTask="removeTask" />
                  </div>
                </div>
                <div v-if="tasksTodaySize > 0">
                  <div class="nameGroup">
                    <span class="nameGroup">{{ $t('label.today') }}</span>
                    <div class="amount-item pointer" @click="navigateTo('tasks/myTasks','TODAY')">{{ tasksTodaySize }}</div>
                    <hr
                      role="separator"
                      aria-orientation="horizontal"
                      class="v-divider theme&#45;&#45;light separator">
                  </div>

                  <div
                    v-for="taskItem in tasksTodayList"
                    :key="taskItem.id"
                    class="list-item">
                    <task-details :task="taskItem" @removeTask="removeTask" />
                  </div>
                </div>

                <div v-if="tasksTomorrowSize > 0">
                  <div class="nameGroup">
                    <span class="nameGroup">{{ $t('label.tomorrow') }}</span>
                    <div class="amount-item pointer" @click="navigateTo('tasks/myTasks','TOMORROW')">{{ tasksTomorrowSize }}</div>
                    <hr
                      role="separator"
                      aria-orientation="horizontal"
                      class="v-divider theme&#45;&#45;light separator">
                  </div>

                  <div
                    v-for="taskItem in tasksTomorrowList"
                    :key="taskItem.id"
                    class="list-item">
                    <task-details :task="taskItem" @removeTask="removeTask" />
                  </div>
                </div>

                <div v-if="tasksUpcomingSize > 0">
                  <div class="nameGroup">
                    <span class="nameGroup">{{ $t('label.upcoming') }}</span>
                    <div class="amount-item pointer" @click="navigateTo('tasks/myTasks','UPCOMING')">{{ tasksUpcomingSize }}</div>
                    <hr
                      role="separator"
                      aria-orientation="horizontal"
                      class="v-divider theme&#45;&#45;light separator">
                  </div>

                  <div
                    v-for="taskItem in tasksUpcomingList"
                    :key="taskItem.id"
                    class="list-item">
                    <task-details :task="taskItem" @removeTask="removeTask" />
                  </div>
                </div>

                <div class="text-center">
                  <button
                    type="button"
                    class="btn color-title btn-show"
                    @click="navigateTo('tasks/myTasks','ALL')">
                    {{ $t('label.tasks.btn.show') }}
                  </button>
                </div>
              </div>

              <div v-if="!loadingTasks && tasks.length===0" class="noTasks">
                <div class="noTasksContent">
                  <v-icon size="60" class="grey-color">fa-clipboard-list</v-icon>
                  <div class="noTasksTitle">{{ $t('label.noTask') }}</div>
                </div>
              </div>
            </v-flex>
          </v-layout>
        </v-flex>
      </v-layout>
      <task-drawer
        ref="taskDrawer"
        :task="task" />
    </v-container>
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
    tasksOverdueList(){
      if (this.tasksOverdue){
        if (this.tasksOverdueSize>10){
          return  this.tasksOverdue.slice(0, 10);
        } else {
          return this.tasksOverdue;
        }
      }
      else {return this.tasksOverdue;}
    },
    tasksTodayList(){
      if (this.tasksToday){
        if (this.tasksTodaySize && this.tasksOverdueSize<11){
          return  this.tasksToday.slice(0, 10-this.tasksOverdueSize);
        } else {
          return '';
        }
      } else {return this.tasksToday.slice(0, 0);}

    },
    tasksTomorrowList(){
      if (this.tasksTomorrow){
        if (this.tasksTomorrowSize && this.tasksOverdueSize+this.tasksTodaySize<11){
          return  this.tasksTomorrow.slice(0, 10-this.tasksOverdueSize-this.tasksTodaySize);
        } else {
          return '';
        }
      } else {return this.tasksTomorrow.slice(0, 0);}
    },
    tasksUpcomingList(){
      if (this.tasksUpcoming){
        if (this.tasksUpcomingSize && this.tasksOverdueSize+this.tasksTodaySize+this.tasksTomorrowSize<11){
          return  this.tasksUpcoming.slice(0, 10-this.tasksOverdueSize-this.tasksTodaySize-this.tasksTomorrowSize);
        }
      } else {return this.tasksUpcoming.slice(0, 0);}
      return '';
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
