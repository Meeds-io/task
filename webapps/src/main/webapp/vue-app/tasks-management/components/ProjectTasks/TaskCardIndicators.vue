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
    <template v-for="(indicator, index) in indicators">
      <component
        :is="indicator.component"
        v-if="isRendered(indicator)"
        :key="index"
        :indicator="indicator"
        :task-id="taskId"
        :dense="dense"
        class="taskCardIndicatorComponent me-2" />
      <v-tooltip
        v-else
        :key="index"
        :disabled="!indicator.title"
        bottom>
        <template #activator="{ on, attrs }">
          <span
            v-bind="attrs"
            :aria-label="indicator.title || indicator.label"
            :class="`${indicator.color || 'error'}--text`"
            class="taskCardIndicator d-flex align-center text-caption text-no-wrap me-2"
            tabindex="0"
            role="note"
            v-on="on">
            <v-icon
              :class="`${indicator.color || 'error'}--text`"
              size="6"
              class="me-1"
              aria-hidden="true">
              fa-circle
            </v-icon>
            {{ indicator.label }}
          </span>
        </template>
        <span>{{ indicator.title }}</span>
      </v-tooltip>
    </template>
  </div>
</template>
<script>
import { isComponentIndicator } from '../../../../js/taskCardIndicators.js';

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
    dense: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    indicators() {
      return this.taskCardIndicators && this.taskId && this.taskCardIndicators.byTask[Number(this.taskId)] || [];
    },
  },
  methods: {
    isRendered: isComponentIndicator,
  },
};
</script>
