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
  <v-app class="tasksList">
    <v-card
      class="tasksView tasksListItem tasksViewList pt-5"
      flat>
      <v-item-group>
        <div class="ma-0 border-box-sizing">
          <div v-if="filterByStatus">
            <div
              v-for="(status, index) in statusList"
              :key="index"
              class="pt-5 px-3 projectTaskItem">
              <tasks-view-list-column
                :status="status"
                :project="project"
                :tasks-list="getTasksByStatus(tasksList,status.name)"
                :show-completed-tasks="showCompletedTasks"
                :filter-by-status="filterByStatus"
                @updateTaskStatus="updateTaskStatus" />
            </div>
          </div>

          <div v-else class="pt-0 px-3 projectTaskItem">
            <tasks-view-list-column
              :tasks-list="tasksList"
              :show-completed-tasks="showCompletedTasks"
              :filter-by-status="filterByStatus"
              @updateTaskStatus="updateTaskStatus" />
          </div>
        </div>
      </v-item-group>
    </v-card>
  </v-app>
</template>
<script>
export default {

  props: {
    statusList: {
      type: Array,
      default: () => []
    },
    tasksList: {
      type: Array,
      default: () => []
    },
    showCompletedTasks: {
      type: Boolean,
      default: false
    },
    project: {
      type: Number,
      default: 0
    },
    filterByStatus: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      tasksStatsStartValue: 0
    };
  },
  created(){
    this.$root.$on('update-task-project', task =>{
      this.updateTaskProject(task);
    });
  },
  methods: {
    updateTaskProject(task){
      this.tasksList = this.tasksList.filter(item => item.id !== task.id);
    },
    getTasksByStatus(items ,statusName) {
      const tasksByStatus = [];
      items.forEach((item) => {
        if (item && item.task && item.task.status && item.task.status.name === statusName) {
          tasksByStatus.push(item);
        }
      });
      return tasksByStatus;
    },
    updateTaskStatus(task,newStatus){
      const status = this.statusList.find(s => s.id.toString() === newStatus || s.name === newStatus);
      if (status){
        task.status = status;
        this.updateTask(task);
      } 
    },
    updateTask(task) {
      if (task.id!=null){
        this.$taskDrawerApi.updateTask(task.id,task).then( () => {
          this.$root.$emit('show-alert', { type: 'success', message: this.$t('alert.success.task.status') });
        }).catch(e => {
          console.error('Error when updating task\'s status', e);
          this.$root.$emit('show-alert',{type: 'error',message: this.$t('alert.error')} );
        });
      }
    },
  }
};
</script>
