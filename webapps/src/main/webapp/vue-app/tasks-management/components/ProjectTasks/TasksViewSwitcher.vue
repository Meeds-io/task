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
<!-- Compact view selector (Board/List/Plan), styled and structured like agenda's
     AgendaSwitchView: a plain <v-menu> escapes the kanban board's stacking
     context (3D-transformed cards) on its own, no manual DOM relocation needed. -->
<template>
  <div class="tasksViewSwitcher d-flex align-center">
    <v-menu
      v-model="menu"
      content-class="tasksViewSwitcherMenu"
      offset-y
      close-on-click>
      <template #activator="{ on, attrs }">
        <v-btn
          :title="$t('label.viewOptions')"
          small
          min-height="36"
          elevation="0"
          class="px-0"
          v-bind="attrs"
          v-on="on">
          <v-icon
            v-if="selectedOption"
            :class="selectedOption.icon"
            class="text-light-color"
            size="20" />
          <v-icon class="ps-2 text-light-color" size="12">fa-chevron-down</v-icon>
        </v-btn>
      </template>
      <v-list class="pa-0" dense>
        <v-list-item
          v-for="item in viewOptions"
          :key="item.value"
          :class="item.value === view && 'background-grey-primary'"
          dense
          @click="select(item)">
          <v-list-item-icon class="me-2 my-0 align-self-center">
            <v-icon
              :class="[item.icon, 'text-light-color']"
              size="16" />
          </v-list-item-icon>
          <v-list-item-title>{{ item.label }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
</template>
<script>
export default {
  props: {
    view: {
      type: String,
      default: 'board',
    },
  },
  emits: ['change'],
  data: () => ({
    menu: false,
  }),
  computed: {
    viewOptions() {
      return [
        { value: 'board', icon: 'far fa-clipboard', label: this.$t('label.boardView') },
        { value: 'list', icon: 'fas fa-list', label: this.$t('label.listView') },
        { value: 'gantt', icon: 'fas fa-stream', label: this.$t('label.ganttView') },
      ];
    },
    selectedOption() {
      return this.viewOptions.find(o => o.value === this.view) || this.viewOptions[0];
    },
  },
  methods: {
    select(item) {
      this.menu = false;
      if (item.value !== this.view) {
        this.$emit('change', item.value);
      }
    },
  },
};
</script>
