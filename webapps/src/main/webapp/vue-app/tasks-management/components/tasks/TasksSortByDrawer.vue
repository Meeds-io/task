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
    ref="filterSortTasksDrawer"
    class="filterSortTasksDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
    <form ref="form1" class="mt-4">
      <v-label for="name">
        <span class="font-weight-bold body-2">{{ $t('label.task.sort') }}</span>
      </v-label>
    </form>
    <v-radio-group
      v-model="orderBy"
      mandatory>
      <v-radio
        :label="$t('label.task.status')"
        value="status" />
      <v-radio
        :label="$t('label.task.title')"
        value="title" />
      <v-radio
        :label="$t('label.task.dueDate')"
        value="dueDate" />
      <v-radio
        :label="$t('label.task.priority')"
        value="priority" />
      <v-radio
        :label="$t('label.task.rank')"
        value="rank" />
    </v-radio-group>
  </div>
</template>
<script>
export default {
  props: {
    value: {
      type: String,
      default: ''
    },
  },
  data() {
    return {
      orderBy: this.value,
    };
  },
  watch: {
    orderBy(val) {
      this.$emit('input', val);
    },
  },
  created() {
    this.$root.$on('reset-filter-task-group-sort', () =>{
      this.orderBy = '';
    });
    this.$root.$on('reset-filter-task-sort',orderBy =>{
      this.orderBy = orderBy;
    });
  }
};
</script>

