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
  <div
    v-if="indicators.length"
    class="taskCardIndicators d-flex flex-wrap align-center">
    <span
      v-for="(indicator, index) in indicators"
      :key="index"
      :title="indicator.title"
      :aria-label="indicator.title || indicator.label"
      :class="`${indicator.color || 'error'}--text`"
      class="taskCardIndicator d-flex align-center text-caption text-no-wrap me-2"
      tabindex="0"
      role="note">
      <v-icon
        :class="`${indicator.color || 'error'}--text`"
        size="6"
        class="me-1"
        aria-hidden="true">
        fa-circle
      </v-icon>
      {{ indicator.label }}
    </span>
  </div>
</template>
<script>
export default {
  inject: {
    taskCardIndicators: {
      default: null,
    },
  },
  props: {
    taskId: {
      type: [Number, String],
      default: null,
    },
  },
  computed: {
    indicators() {
      return this.taskCardIndicators && this.taskId && this.taskCardIndicators.byTask[Number(this.taskId)] || [];
    },
  },
};
</script>
