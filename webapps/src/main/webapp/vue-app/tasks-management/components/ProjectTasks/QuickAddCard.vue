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
  <v-card
    v-if="quickAddTask"
    class="addTaskCard pa-3"
    flat>
    <v-text-field
      v-model="taskTitle"
      :placeholder="$t('label.tapTask.name')"
      :autofocus="quickAddTask"
      type="text"
      class="ps-0 pt-0 task-name"
      auto-grow
      rows="1"
      row-height="13"
      required 
      @keyup="checkImput($event)" />
    <div class="d-md-none">
      <v-spacer />
      <v-btn
        class="btn me-2"
        @click="closeForm">
        {{ $t('popup.cancel') }}
      </v-btn>
      <v-btn
        :disabled="disableSaveButton"
        class="btn btn-primary"
        @click="addTask">
        {{ $t('label.save') }}
      </v-btn>
    </div>
  </v-card>
</template>
<script>
export default {
  props: {
    status: {
      type: Object,
      default: null
    },
    quickAddTask: {
      type: Boolean,
      default: false
    },
    taskTitle: {
      type: String,
      default: ''
    },
  },
  data() {
    return {
      newTask: {title: '',
        priority: 'NONE'}
    };
  },
  computed: {
    taskTitleValid() {
      return this.taskTitle && this.taskTitle.trim() && this.taskTitle.trim().length >= 3 && this.taskTitle.length < 1024;
    },
    disableSaveButton() {
      return !this.taskTitleValid;
    },
  },

  methods: {
    addTask() {
      this.newTask.title=this.taskTitle;
      this.newTask.status=this.status;
      this.$taskDrawerApi.addTask(this.newTask).then( addedTask => {
        this.closeForm();
        this.$root.$emit('task-added', addedTask);
        this.$root.$emit('show-alert', {
          type: 'success',
          message: this.$t('alert.success.task.created')
        });
      }).catch(e => {
        console.error('Error when adding task title', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('alert.error')
        });
      });
    },
    checkImput: function(e) {
      if (e.keyCode === 13) {
        if (this.taskTitle && this.taskTitle.trim() && this.taskTitle.trim().length >= 3 && this.taskTitle.length < 1024){
          this.addTask() ;
        }       
      } 
      if (e.keyCode === 27) {
        this.closeForm();
      }      
    },
    openForm(){
      this.quickAddTask=true;
    },
    closeForm(){
      this.quickAddTask=false;
      this.taskTitle='';
      this.$emit('close-quick-form');
      this.$root.$emit('close-quick-task-form');
    }
  },
};
</script>