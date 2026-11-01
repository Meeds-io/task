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
  <div class="taskPriority">
    <v-select
      ref="selectPriority"
      v-model="priority"
      :items="priorities"
      item-value="key"
      item-text="value"
      :readonly="readOnly"
      :append-icon="readOnly ? '' : '$dropdown'"
      attach
      solo
      dense
      class="pt-0 selectFont"
      @change="updateTaskPriority()"
      @click="$emit('PriorityListOpened')">
      <template #selection="{ item }">
        <span v-if="item.key === 'NONE'" class="primary--text text-body-2">{{ readOnly ? '' : $t('taskDrawer.field.add') }}</span>
        <span v-else class="d-flex align-center text-body-2">
          <v-icon
            :class="priorityDefaultColor"
            class="me-1"
            size="18">
            mdi-flag-variant
          </v-icon>
          {{ item.value }}
        </span>
      </template>
      <template slot="item" slot-scope="data">
        <v-list-avatar class="me-2">
          <v-icon :class="getTaskPriorityColor(data.item.key)" size="20">mdi-flag-variant</v-icon>
        </v-list-avatar>
        <v-list-tile-content>
          <v-list-tile-title class="body-2">
            {{ data.item.value }}
          </v-list-tile-title>
        </v-list-tile-content>
      </template>
    </v-select>
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
    readOnly: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      priority: '',
      priorityDefaultColor: 'highPriorityColor',
      priorities: [
        {key: 'HIGH',value: this.$t('label.priority.high')},
        {key: 'NORMAL',value: this.$t('label.priority.normal')},
        {key: 'LOW',value: this.$t('label.priority.low')},
        {key: 'NONE',value: this.$t('label.priority.none')}
      ],
    };
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.$refs.selectPriority && this.$refs.selectPriority.isMenuActive) {
        window.setTimeout(() => {
          this.$refs.selectPriority.isMenuActive = false;
        }, 200);
      }
    });
    document.addEventListener('closePriority',()=> {
      if (this.$refs.selectPriority && this.$refs.selectPriority.isMenuActive) {
        window.setTimeout(() => {
          this.$refs.selectPriority.isMenuActive = false;
        }, 100);
      }
    });
  },
  methods: {
    /**
     * Shows the priority of the task its drawer opens. Only the drawer that
     * holds this field calls it, and it saves nothing: the priority is saved
     * only when the user chooses one in the list.
     *
     * @param {object} task the task the drawer opens
     * @returns {void}
     */
    load(task) {
      this.priority = task?.id != null ? task.priority : 'NONE';
      this.priorityDefaultColor = this.getTaskPriorityColor(this.priority);
    },
    /**
     * @param {string} priority a priority key: HIGH, NORMAL, LOW or NONE
     * @returns {string} the class that colors the priority's flag
     */
    getTaskPriorityColor(priority) {
      switch (priority) {
      case 'HIGH':
        return 'highPriorityColor';
      case 'NORMAL':
        return 'normalPriorityColor';
      case 'LOW':
        return 'lowPriorityColor';
      case 'NONE':
        return 'nonePriorityColor';
      }
    },
    /**
     * Asks the drawer to save the priority the user chose in the list.
     *
     * @returns {void}
     */
    updateTaskPriority() {
      this.priorityDefaultColor = this.getTaskPriorityColor(this.priority);
      const value = {
        'priority': this.priority,
        'taskId': this.task.id,
      };
      this.$emit('updateTaskPriority',value);
    },
  }
};
</script>
