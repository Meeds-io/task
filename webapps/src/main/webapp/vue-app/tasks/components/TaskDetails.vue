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
  <v-list class="py-0 my-2">
    <v-list-item 
      :style="taskBorderColor" 
      class="py-0 ps-2 pe-0 border-radius"
      :class="isHidden && 'hidden' || ''"
      @click="openTaskDrawer">
      <v-list-item-content class="py-0">
        <v-list-item-title 
          class="d-flex flex justify-between align-center subtitle-2 mb-0 clickable">
          <span class="text-truncate">{{ title }}</span>
          <v-spacer />
          <span :class="isOutdated && 'red--text' || ''">{{ dueDate }}</span>
        </v-list-item-title>
        <v-list-item-subtitle v-if="commentCount || labels" class="d-flex align-center mt-1">
          <div v-if="commentCount">
            <v-icon 
              size="14">
              far fa-comment
            </v-icon>
            <span>{{ commentCount }}</span>
          </div>
          <div v-if="labels" :class="commentCount && 'ps-3' || ''">
            <v-chip
              v-if="labels == 1"
              :color="labelColor"
              :title="labelName"
              class="mx-1"
              label
              x-small>
              <span class="text-truncate">
                {{ labelName }}
              </span>
            </v-chip>
            <div v-else>
              <v-icon 
                size="14">
                fas fa-tag
              </v-icon>
              <span>{{ labels }}</span>
            </div>
          </div>
        </v-list-item-subtitle>  
      </v-list-item-content>
    </v-list-item>
  </v-list>
</template>
<script>
export default {
  props: {
    task: {
      type: Object,
      default: () => {
        return {};
      }
    },
    isOutdated: {
      type: Boolean,
      default: false,
    }
  },
  data() {
    return {
      drawer: false,
      currentTask: null,
      isCompleted: false
    };
  },
  computed: {
    title() {
      return this.currentTask?.task?.title;
    },
    dueDate() {
      return this.currentTask?.dueDate && this.dateFormatter(this.currentTask?.dueDate) || '';
    },
    commentCount() {
      return this.currentTask?.commentCount || 0;
    },
    taskBorderColor() {
      return this.currentTask?.task?.priority && `border-left: 5px solid ${this.getTaskPriorityColor(this.currentTask.task.priority)}` || '';
    },
    labels() {
      return this.currentTask?.labels?.length;
    },
    labelName() {
      return this.labels && this.currentTask.labels[0].name;
    },
    labelColor() {
      return this.labels && this.currentTask.labels[0].color;
    },
    isHidden() {
      return this.isCompleted;
    }
  },
  created() {
    this.currentTask = this.task && JSON.parse(JSON.stringify(this.task)) || {};
    this.$root.$on('task-updated', task => {
      if (this.currentTask?.task?.id === task?.id) {
        this.currentTask.task = task;
      }
    });
    this.$root.$on('update-task-comments',(value,id)=>{
      this.updateTaskComments(value,id);
    });
    this.$root.$on('update-task-labels',(value,id)=>{
      this.updateTaskLabels(value,id);
    });
    this.$root.$on('update-completed-task', (value, id) => {
      if (this.task.id === id) {
        this.currentTask.task.completed = value;
        if (this.currentTask.task.completed === true) {
          this.isCompleted = true;
          this.$emit('update-task-completed', this.currentTask.task);
        }
      }
    });
  },
  methods: {
    dateFormatter(dueDate) {
      if (dueDate) {
        const date = new Date(dueDate.time);
        const day = date.getDate();
        const month = date.getMonth()+1;
        const year = date.getFullYear();
        const formattedTime = `${day  }-${  month  }-${  year}`;
        return formattedTime;
      }
    },
    getTaskPriorityColor(priority) {
      switch (priority) {
      case 'HIGH':
        return '#bc4343';
      case 'NORMAL':
        return '#ffb441';
      case 'LOW':
        return '#2eb58c';
      case 'NONE':
        return '#476A9C';
      }
    },
    openTaskDrawer() {
      this.$root.$emit('open-task-drawer', this.currentTask.task);
    },
    updateTaskComments(value,id){
      if (this.currentTask.id === id){
        this.currentTask.commentCount=value;
      }
    },
    updateTaskLabels(value,id){
      if (this.currentTask.id === id){
        this.currentTask.labels.push(value);
      }
    },
  }
};
</script>
