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
  <div
    :id="'task-'+viewType+'-'+status.id"
    class="tasksViewHeaderStatus mr-2 d-flex justify-space-between align-center">
    <div
      class="d-flex align-center assigneeFilter pointer">
      <a
        class="toggle-collapse-group d-flex"
        href="#"
        @click="showDetailsTask(viewType,status.id)">
        <i
          :id="'uiIconMiniArrowDown'+viewType+status.id"
          class="uiIcon uiIconMiniArrowDown"
          style="display: block">
        </i>
        <i
          :id="'uiIconMiniArrowRight'+viewType+status.id"
          class="uiIcon  uiIconMiniArrowRight"
          style="display: none">
        </i>
      </a>
      <input
        v-if="editStatus || status.edit"
        ref="autoFocusInput1"
        v-model="status.name"
        placeholder="Status Name"
        type="text"
        class="text-truncate subtitle-2 text-color my-auto"
        required
        autofocus
        @keyup="checkImput($event,index)">
      <div
        v-else
        class="text-truncate subtitle-2 text-color my-auto">
        {{ taskStatusLabel }} <span class="text-truncate subtitle-2 text-color my-auto">({{ tasksNumber }})</span>
      </div>
    </div>
    <v-divider class="mx-1" />
    <div class="taskNumberAndActions d-flex align-center mb-1">
      <!-- <span v-if="tasksNumber < maxTasksToShow" class="caption">{{ tasksNumber }}</span>
      <div v-else class="showTasksPagination">
        <span class="caption">
          {{ limitTasksToshow }} - {{ initialTasksToShow }} of {{ tasksNumber }}
        </span>
        <v-btn
          :disabled="disableBtnLoadMore"
          icon
          small
          @click="loadNextTasks">
          <i class="uiIcon uiIconArrowNext text-color"></i>
        </v-btn>

      </div> -->
      <i
        icon
        small
        class="uiIconSocSimplePlus d-flex"
        @click="openTaskDrawer()">
      </i>
      <!--      <i
        icon
        small
        class="uiIconVerticalDots taskInfoIcon d-flex"
        @click="displayActionMenu = true">
      </i>
      <v-menu
        v-model="displayActionMenu"
        :attach="`#task-${viewType}-${status.id}`"
        transition="slide-x-reverse-transition"
        content-class="taskStatusActionMenu"
        offset-y>
        <v-list class="pa-0" dense>
          <v-list-item
            v-if="!project.canManage"
            class="menu-list"
            @click="openTaskDrawer()">
            <v-list-item-title class="subtitle-2">
              <i class="uiIcon uiIconTask pe-1"></i>
              <span>{{ $t('label.addTask') }}</span>
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>-->
    </div>
  </div>
</template>
<script>
export default {
  props: {
    status: {
      type: Object,
      default: null
    },
    viewType: {
      type: String,
      default: ''
    },
    tasksNumber: {
      type: Number,
      default: 0
    },
    maxTasksToShow: {
      type: Number,
      default: 0
    },
    index: {
      type: Number,
      default: 0
    },
    project: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      displayActionMenu: false,
      editStatus: false,
      tasksStatsStartValue: 1,
      originalTasksToShow: this.maxTasksToShow,
      disableBtnLoadMore: false,
    };
  },
  computed: {
    initialTasksToShow() {
      return this.originalTasksToShow;
    },
    limitTasksToshow() {
      return this.tasksStatsStartValue;
    },
    taskStatusLabel() {
      const key = `exo.tasks.status.${this.status?.name.toLowerCase()}`;
      const translatedLabel = this.$t(key);
      return translatedLabel === key && this.status?.name || translatedLabel;
    }
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.displayActionMenu) {
        window.setTimeout(() => {
          this.displayActionMenu = false;
        }, 200);
      }
      /*         if (this.editStatus || this.status.edit) {
          window.setTimeout(() => {
            this.cancelAddColumn(this.index);
          }, 200);
        } */
    });
  },
  methods: {
    loadNextTasks() {
      if (this.tasksNumber - this.originalTasksToShow >= this.maxTasksToShow) {
        this.tasksStatsStartValue = this.originalTasksToShow+1;
        this.originalTasksToShow += this.maxTasksToShow;
      } else {
        this.tasksStatsStartValue = this.originalTasksToShow;
        this.originalTasksToShow = this.tasksNumber;
        this.disableBtnLoadMore = true;
      }
    },
    openTaskDrawer() {
      const defaultTask= {id: null,
        status: this.status,
        priority: 'NONE',
        description: '',
        title: ''};
      this.$root.$emit('open-task-drawer', defaultTask);
    },
    checkImput: function(e,index) {
      if (e.keyCode === 13) {
        this.saveStatus(index);
      } 
      if (e.keyCode === 27) {
        this.cancelAddColumn(index);
      }      
    },
    
    deleteStatus() {
      this.$emit('delete-status', this.status);
    },
    saveStatus(index) {
      if (this.status.id){
        this.$emit('update-status', this.status);
        this.editStatus=false;
      } else {
        this.status.rank=index;
        this.$emit('add-status');
        this.editStatus=false;
      }
    },
    addColumn(index) {       
      this.$emit('add-column',index);    
    },
    cancelAddColumn(index) {
      if (this.status.id){
        this.editStatus=false;
        this.status.edit=false;
      } else {
        this.$emit('cancel-add-column',index);
      }
    },
    showDetailsTask(viewType,id){
      const uiIconMiniArrowDown = document.querySelector(`#${`uiIconMiniArrowDown${viewType}${id}`}`);
      const uiIconMiniArrowRight = document.querySelector(`#${`uiIconMiniArrowRight${viewType}${id}`}`);

      const detailsTask = document.querySelector(`#${`taskView${id}`}`);
      if (detailsTask.style.display !== 'none') {
        detailsTask.style.display = 'none';
        uiIconMiniArrowDown.style.display = 'none';
        uiIconMiniArrowRight.style.display = 'block';
      }
      else {detailsTask.style.display = 'block';
        uiIconMiniArrowDown.style.display = 'block';
        uiIconMiniArrowRight.style.display = 'none';}
    },
  }
};
</script>