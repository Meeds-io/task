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
    ref="filterGroupTasksDrawer"
    class="filterGroupTasksDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
    <div v-if="taskViewTabName === 'gantt'">
      <form ref="form1" class="mt-4">
        <v-label for="name">
          <span class="font-weight-bold body-2">{{ $t('label.gantt.scale') }} </span>
        </v-label>
      </form>
      <v-radio-group
        v-model="scaleBy"
        mandatory>
        <v-radio
          :label="$t('label.gantt.scale.day')"
          value="Day" />
        <v-radio
          :label="$t('label.gantt.scale.week')"
          value="Week" />
        <v-radio
          :label="$t('label.gantt.scale.month')"
          value="Month" />
      </v-radio-group>
    </div>
    <div v-else>
      <form ref="form1" class="mt-4">
        <v-label for="name">
          <span class="font-weight-bold body-2">{{ $t('label.task.groupBy') }}</span>
        </v-label>
      </form>
      <v-radio-group
        v-model="groupBy"
        mandatory>
        <v-radio
          v-if="taskViewTabName !== 'list'"
          :label="$t('label.task.none')"
          value="none" />
        <v-radio
          v-if="taskViewTabName === 'list'"
          :label="$t('label.task.status')"
          value="status" />
        <v-radio
          :label="$t('label.task.assignee')"
          value="assignee" />
        <v-radio
          :label="$t('label.task.labels')"
          value="label" />
        <v-radio
          :label="$t('label.task.dueDate')"
          value="dueDate" />
        <v-radio
          :label="$t('label.task.completed')"
          value="completed" />
      </v-radio-group>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    value: {
      type: String,
      default: ''
    },
    taskViewTabName: {
      type: String,
      default: ''
    },
  },
  data() {
    return {
      groupBy: this.value,
      scaleBy: 'Week'

    };
  },
  watch: {
    groupBy(val) {
      this.$emit('input', val);
    },
    scaleBy(val) {
      this.$emit('scale-changed', val);
    }
  },created() {
    this.$root.$on('reset-filter-task-group-sort',groupBy =>{
      this.groupBy = groupBy;
    });
  }
};
</script>

