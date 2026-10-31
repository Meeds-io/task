<!--
  This file is part of the Meeds project (https://meeds.io/).
  Copyright (C) 2026 Meeds Association
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
  <v-tabs
    :value="selectedIndex"
    class="taskChangesTabs"
    @change="$emit('change', $event)">
    <v-tab class="text-none">{{ $t('label.changes.tab.changes') }}</v-tab>
    <v-tab
      v-for="tab in tabs"
      :key="tab.id"
      class="text-none">
      {{ tabLabel(tab) }}
    </v-tab>
  </v-tabs>
</template>
<script>
export default {
  props: {
    tabs: {
      type: Array,
      default: () => [],
    },
    selectedIndex: {
      type: Number,
      default: 0,
    },
  },
  methods: {
    /**
     * @param {object} tab the options a tab was registered with
     * @returns {string} its label, translated when it gives a key
     */
    tabLabel(tab) {
      return tab.labelKey && this.$te(tab.labelKey) && this.$t(tab.labelKey) || tab.label || tab.id;
    },
  },
};
</script>
