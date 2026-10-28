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
    :class="[`taskDrawerField-${name}`, editable && 'taskDrawerFieldEditable']"
    class="taskDrawerField d-flex align-start">
    <div
      :style="labelColumnStyle"
      class="taskDrawerFieldLabel d-flex align-center flex-shrink-0 pe-3">
      <span class="taskDrawerFieldIcon d-inline-flex align-center justify-center flex-shrink-0">
        <v-icon
          size="18"
          class="icon-default-color"
          aria-hidden="true">
          {{ icon }}
        </v-icon>
      </span>
      <span class="text-body-2 text-light-color text-truncate">{{ label }}</span>
    </div>
    <div class="taskDrawerFieldValue d-flex align-center flex-grow-1">
      <div class="taskDrawerFieldContent flex-grow-1">
        <slot></slot>
      </div>
      <v-icon
        v-if="editable"
        size="12"
        class="taskDrawerFieldPen text-light-color flex-shrink-0 mx-2"
        aria-hidden="true">
        fa-pen
      </v-icon>
    </div>
  </div>
</template>
<script>
/**
 * The width of the label column, as the form cards' rows: a third of the row
 * plus the icon and its gap. A row spanning the two columns of the full-screen
 * list keeps the label column of one column: a third of half its width, less
 * the 24px end padding each column has, plus the icon and its gap. Set inline, as the form cards'
 * rows do: the portal's CSS minifier removes the spaces around the « + » of a
 * calc() in a stylesheet, which makes the declaration invalid.
 */
const LABEL_COLUMN_WIDTH = 'calc(100% / 3 + 31px)';
const WIDE_LABEL_COLUMN_WIDTH = 'calc(100% / 6 + 27px)';

/**
 * One row of the task drawer's field list: the field's icon and label in the
 * label column, its value in the value column, laid out as the rows of the
 * form cards the drawer shows below, so that both read as one list.
 */
export default {
  props: {
    name: {
      type: String,
      default: null,
    },
    icon: {
      type: String,
      default: null,
    },
    label: {
      type: String,
      default: null,
    },
    editable: {
      type: Boolean,
      default: false,
    },
    wide: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    /**
     * The style that sizes the label column.
     *
     * @returns {string} the column's flex basis and maximum width
     */
    labelColumnStyle() {
      const width = this.wide ? WIDE_LABEL_COLUMN_WIDTH : LABEL_COLUMN_WIDTH;
      return `flex: 0 0 ${width}; max-width: ${width}`;
    },
  },
};
</script>
