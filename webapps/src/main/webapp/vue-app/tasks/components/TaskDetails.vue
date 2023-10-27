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
  <v-list class="border-color border-radius border-light-color py-0 my-2">
    <v-list-item 
      :style="taskBorderColor" 
      class="py-0 px-1 border-radius"
      @click="openTaskDrawer">
      <v-list-item-content class="py-1">
        <v-list-item-title 
          class="d-flex flex justify-between align-center subtitle-2 pb-1 clickable">
          <span class="text-truncate">{{ title }}</span>
          <v-spacer></v-spacer>
          <span :class="isOutdated && 'red--text' || ''">{{ dueDate }}</span>
        </v-list-item-title>
        <v-list-item-subtitle class="d-flex align-center">
          <div v-if="commentCount">
            <v-icon 
              size="14">far fa-comment</v-icon>
            <span>{{ commentCount }}</span>
          </div>
          <div v-if="labels" class="ps-3">
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
                size="14">fas fa-tag</v-icon>
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
    };
  },
  computed: {
    title() {
      return this.task?.task?.title;
    },
    dueDate() {
      return this.task?.dueDate && this.dateFormatter(this.task?.dueDate) || '';
    },
    commentCount() {
      return this.task?.commentCount || 0;
    },
    taskBorderColor() {
      return this.task?.task?.priority && `border-left: 5px solid ${this.getTaskPriorityColor(this.task.task.priority)}` || '';
    },
    labels() {
      return this.task?.labels?.length;
    },
    labelName() {
      return this.labels && this.task.labels[0].name;
    },
    labelColor() {
      return this.labels && this.task.labels[0].color;
    }
  },
  created() {
    this.$root.$on('task-updated', task => {
      if (this.task?.task?.id === task?.id) {
        this.task.task = task;
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
      this.$root.$emit('open-task-drawer', this.task.task);
    },
  }
};
</script>
