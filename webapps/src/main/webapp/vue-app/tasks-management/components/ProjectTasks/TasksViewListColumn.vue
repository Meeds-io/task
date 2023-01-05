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
  <div :id="status.id">
    <div v-if="filterByStatus===true">
      <tasks-view-header-status
        :status="status"
        :project="project"
        :view-type="'list'"
        :max-tasks-to-show="maxTasksToShow"
        :tasks-number="tasksList.length" />
    </div>
    <div :id="'taskView'+status.id" :class="filterByStatus===true ? 'pt-5 ms-7 me-2' : ''">
      <div :id="status.id">
        <draggable
          v-model="tasksList"
          :move="checkMove"
          :animation="200"
          :key="draggableKey"
          class="draggable-palceholder"
          ghost-class="ghost-card"
          :options="{group:'status'}"
          @start="drag=true"
          @end="drag=false">
          <task-view-list-item
            v-for="taskListItem in tasksList"
            :key="taskListItem.task.id"
            :task="taskListItem"
            :show-completed-tasks="showCompletedTasks"
            @update-task-completed="updateTaskCompleted" />
        </draggable>
      </div>
    </div>
  </div>
</template>
<script>

 
export default {
  props: {
    tasksList: {
      type: Array,
      default: () => []
    },
    status: {
      type: String,
      default: ''
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
  data() {
    return {
      maxTasksToShow: 6,
      drag: false,
      task: null,
      newStatus: null,
      draggableKey: 1,
    };
  },
  watch: {
    drag(val) {
      if (!val&&this.task&&this.newStatus&&this.task.status.id.toString() !== this.newStatus){
        this.$emit('updateTaskStatus', this.task,this.newStatus);
        Array.from(document.getElementsByClassName('draggable-palceholder')).forEach(element => element.style.backgroundColor= '#FFFFFF');
      }
    },
  },
  methods: {
    updateTaskCompleted(task) {
      if (!this.showCompletedTasks && task.completed) {
        setTimeout(() => {
          this.$root.$emit('task-isCompleted-updated', task);
        }, 500);
      }
    },
    checkMove(evt){
      if (evt){
        Array.from(document.getElementsByClassName('draggable-palceholder')).forEach(element => element.style.backgroundColor= '#FFFFFF');
        Array.from(evt.to.parentElement.getElementsByClassName('draggable-palceholder')).forEach(element => element.style.backgroundColor= '#f2f2f2');
        this.task = evt.draggedContext.element.task;
        this.newStatus = evt.to.parentElement.id;
      }

    },
    reRenderTasks() {
      this.draggableKey += 1;
    },
  }
};
</script>
