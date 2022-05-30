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
  <div class="taskStatus">
    <div v-if="task.status && task.status.project">
      <v-select
        id="selectStatus"
        ref="selectStatus"
        v-model="taskStatus"
        :items="projectStatuses"
        item-value="key"
        item-text="value"
        class="pt-0 selectFont"
        attach
        dense
        solo
        @change="updateTaskStatus()"
        @click="$emit('statusListOpened')" />
    </div>
  </div>
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
  },
  data () {
    return {
      taskStatus: 'ToDo',
      projectStatuses: [],
    };
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.$refs.selectStatus && this.$refs.selectStatus.isMenuActive) {
        window.setTimeout(() => {
          this.$refs.selectStatus.isMenuActive = false;
        }, 200);
      }
    });
    document.addEventListener('closeStatus',()=> {
      if (this.$refs.selectStatus && this.$refs.selectStatus.isMenuActive) {
        window.setTimeout(() => {
          this.$refs.selectStatus.isMenuActive = false;
        }, 100);
      }
    });
    document.addEventListener('loadProjectStatus', this.loadProjectStatus);
  },
  destroyed() {
    document.removeEventListener('loadProjectStatus', this.loadProjectStatus);
  },
  methods: {
    getI18nStatus(status) {
      return this.$te(`tasks.status.${status}`) ? this.$t(`tasks.status.${status}`) : this.$te(`tasks.status.${status.toLowerCase()}`)
                                                && this.$t(`tasks.status.${status.toLowerCase()}`)
                                                || status;
    },
    updateTaskStatus() {
      if (this.taskStatus != null) {
        this.$taskDrawerApi.getStatusesByProjectId(this.task.status.project.id).then(
          (projectStatuses) => {
            const status = projectStatuses.find(s => s.name === this.taskStatus);
            this.$emit('updateTaskStatus',status);
          });
      }
    },
    getStatusesByProjectId(task) {
      this.$taskDrawerApi.getStatusesByProjectId(task.status.project.id).then(
        (data) => {
          this.projectStatuses = data;
          for (let i = 0; i < data.length; i++) {
            this.projectStatuses[i] = {key: data[i].name, value: this.getI18nStatus(data[i].name)};
          }
        });
    },
    loadProjectStatus(event) {
      if (event && event.detail) {
        const task = event.detail;
        if (task.status != null && task.status.project) {
          this.getStatusesByProjectId(task);
          if (task.status.name) {
            this.taskStatus = task.status.name;
          }
        } else {
          this.projectStatuses.push({key: 'ToDo', value: this.$t('exo.tasks.status.todo')});
          this.projectStatuses.push({key: 'InProgress', value: this.$t('exo.tasks.status.inprogress')});
          this.projectStatuses.push({key: 'WaitingOn', value: this.$t('exo.tasks.status.waitingon')});
          this.projectStatuses.push({key: 'Done', value: this.$t('exo.tasks.status.done')});
          this.taskStatus = 'ToDo';
        }
      }
    },
  }
};
</script>