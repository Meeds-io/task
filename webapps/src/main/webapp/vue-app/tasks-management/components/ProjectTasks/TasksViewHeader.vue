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
    :id="`task-${viewType}-${status.id}`"
    class="tasksViewHeader d-flex justify-space-between align-center">
    <div
      class="d-flex tasksViewHeaderLeft">
      <v-btn
        v-if="viewType === 'list'"
        icon
        small
        outlined
        @click="showDetailsTask(viewType,status.id)">
        <v-icon
          :id="`arrowDown${viewType}${status.id}`"
          size="13"
          class="mx-2 mt-1 my-auto"
          style="display: inline-flex">
          fa-angle-down
        </v-icon>
        <v-icon
          :id="`arrowRight${viewType}${status.id}`"
          size="13"
          class="mx-2 mt-1 my-auto"
          style="display: none">
          fa-angle-right
        </v-icon>
      </v-btn>
      <div v-if="(editStatus || status.edit) && project.canManage">
        <v-text-field
          v-if="editStatus || status.edit"
          ref="autoFocusInput1"
          v-model="status.name"
          :placeholder="$t('label.tapStatus.name')"
          type="text"
          :rules="nameRules"
          @focus="editStatusMode(project.canManage)"
          @blur="cancelAddColumn(status.name)"
          class="taskStatusNameEdit font-weight-bold text-color mb-1"
          required
          autofocus
          outlined
          dense
          @keyup="checkInput($event,index)">
          <template #append>
            <v-btn
              icon
              small
              class="mt-0"
              @click="checkInput(13,status.name)">
              <v-icon
                small
                class="success--text">
                fa fa-check
              </v-icon>
            </v-btn>
            <v-btn
              icon
              small
              class="mt-0"
              @click="cancelAddColumn(status.name)">
              <v-icon
                small
                class="error--text">
                fa fa-times
              </v-icon>
            </v-btn>
          </template>
        </v-text-field>
      </div>

      <div
        class="d-flex taskStatusName  font-weight-bold text-color mb-1"
        v-else
        @click="editStatusMode(project.canManage)">
        <div
          class="statusName text-truncate"
          :title="getI18N(status.name)">
          {{ getI18N(status.name) }}
        </div>
        <div class="uiTaskNumber">{{ tasksNumber }}</div>
      </div>
    </div>
    <div
      class="taskNumberAndActions d-flex align-center mb-1"
      @click="editStatus = false">
      <v-btn
        :title="$t('label.addTask')"
        icon
        small
        @click="openTaskDrawer()">
        <v-icon size="16">fa-plus</v-icon>
      </v-btn>
      <v-btn
        v-if="project.canManage"
        :title="$t('label.addTask')"
        icon
        small
        @click="displayActionMenu = true">
        <v-icon size="16">fa-ellipsis-v</v-icon>
      </v-btn>
      <v-menu
        v-model="displayActionMenu"
        v-if="project.canManage"
        :attach="`#task-${viewType}-${status.id}`"
        transition="slide-x-reverse-transition"
        content-class="taskStatusActionMenu"
        offset-y>
        <v-list class="pa-0" dense>
          <v-list-item
            v-if="index < statusListLength-1"
            class="menu-list"
            @click="moveAfterColumn(index)">
            <v-list-item-title class="subtitle-2">
              <v-icon size="13" class="pe-1 icon-default-color">fa fa-angle-right</v-icon>
              <span> {{ $t('label.status.move.after') }}</span>
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            v-if="index>0"
            class="menu-list"
            @click="moveBeforeColumn(index)">
            <v-list-item-title class="subtitle-2">
              <v-icon size="13" class="pe-1 icon-default-color">fa fa-angle-left</v-icon>
              <span>{{ $t('label.status.move.before') }}</span>
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            class="menu-list"
            @click="addColumn(index)">
            <v-list-item-title class="subtitle-2">
              <v-icon size="13" class="pe-1 icon-default-color">fa fa-undo</v-icon>
              <span>{{ $t('label.status.before') }}</span>
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            class="menu-list"
            @click="addColumn(index+1)">
            <v-list-item-title class="subtitle-2">
              <!-- ps-1 instead of pe-1 due to flip rotation -->
              <v-icon size="13" class="ps-1 icon-default-color">fa fa-undo fa-flip-horizontal</v-icon>
              <span> {{ $t('label.status.after') }} </span>
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            class="menu-list"
            @click="deleteStatus()">
            <v-list-item-title class="subtitle-2">
              <v-icon size="13" class="pe-1 icon-default-color">fa-trash-alt</v-icon>
              <span>{{ $t('label.delete') }}</span>
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
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
    statusListLength: {
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
      disableBtnLoadMore: false,rules: [],
      nameRules: []
    };
  },
  computed: {
    initialTasksToShow() {
      return this.originalTasksToShow;
    },
    limitTasksToshow() {
      return this.tasksStatsStartValue;
    },
    limitStatusLabel() {
      return this.$t('label.status.name.rules');
    },
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.displayActionMenu) {
        window.setTimeout(() => {
          this.displayActionMenu = false;
        }, 200);
      }
    });
    this.nameRules = [v => !v || (v.length > 2 && v.length < 51) || this.limitStatusLabel];
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
    checkInput: function(e,index) {
      if (e.keyCode === 13 || e === 13) {
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
      if (this.status.name.length>=3 && this.status.name.length<=50){
        if (this.status.id){
          this.$emit('update-status', this.status);
          this.editStatus=false;
        } else {
          this.status.rank=index;
          this.$emit('add-status');
          this.editStatus=false;
        }
      }
    },
    openQuickAdd() {
      this.$emit('open-quick-add');
    },
    addColumn(index) {
      this.$emit('add-column',index);
    },
    moveBeforeColumn(index) {
      this.$emit('move-column',index,index-1);
    },
    moveAfterColumn(index) {
      this.$emit('move-column',index,index+1);
    },
    cancelAddColumn(index) {
      if (this.status.id){
        this.editStatus=false;
        this.status.edit=false;
        this.$emit('update-status', null);
      } else {
        this.editStatus=false;
        this.status.edit=false;
        this.$emit('cancel-add-column',index);
        this.$emit('update-status', null);

      }
    },
    editStatusMode(canManage){
      if (canManage){
        this.editStatus=true;
      }

    },
    getI18N(label){
      const fieldLabelI18NKey = `tasks.status.${label}`;
      const fieldLabelI18NValue = this.$t(fieldLabelI18NKey);
      return  fieldLabelI18NValue === fieldLabelI18NKey ? label : fieldLabelI18NValue;
    },
    showDetailsTask(viewType,id){
      const arrowDown = document.querySelector(`#${`arrowDown${viewType}${id}`}`);
      const arrowRight = document.querySelector(`#${`arrowRight${viewType}${id}`}`);

      const detailsTask = document.querySelector(`#${`taskView${id}`}`);
      if (detailsTask.style.display !== 'none') {
        detailsTask.style.display = 'none';
        arrowDown.style.display = 'none';
        arrowRight.style.display = 'inline-flex';
      } else {
        detailsTask.style.display = 'block';
        arrowDown.style.display = 'inline-flex';
        arrowRight.style.display = 'none';
      }
    },
  }
};
</script>