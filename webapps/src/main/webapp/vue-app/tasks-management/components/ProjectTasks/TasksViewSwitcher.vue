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
<!-- Compact view selector (Board/List/Plan) styled like agenda's AgendaSwitchView.
     The dropdown is moved to document.body and positioned with fixed coordinates
     so it always renders above the kanban board (the board content otherwise
     paints over an in-place dropdown regardless of z-index). -->
<template>
  <div class="tasksViewSwitcher d-flex align-center">
    <v-btn
      :title="$t('label.viewOptions')"
      small
      min-height="36"
      elevation="0"
      class="px-0"
      @click="toggle">
      <v-icon
        v-if="selectedOption"
        :class="selectedOption.icon"
        class="text-light-color"
        size="20" />
      <v-icon class="ps-2 text-light-color" size="12">fa-chevron-down</v-icon>
    </v-btn>
    <v-card
      ref="menuCard"
      v-show="menu"
      class="tasksViewSwitcherMenu"
      elevation="4">
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
    </v-card>
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
  data: () => ({
    menu: false,
    cardEl: null,
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
  mounted() {
    // Relocate the dropdown high in the Vuetify app root so it escapes the
    // board's stacking context but stays inside .v-application (keeping the
    // theme background; a plain <body> child would render transparent).
    this.cardEl = this.$refs.menuCard && this.$refs.menuCard.$el;
    if (this.cardEl) {
      this.cardEl.style.position = 'fixed';
      this.cardEl.style.zIndex = '9999';
      this.cardEl.style.minWidth = '160px';
      const appRoot = document.querySelector('.v-application') || document.body;
      appRoot.appendChild(this.cardEl);
    }
    document.addEventListener('click', this.onDocumentClick, true);
    window.addEventListener('scroll', this.onScroll, true);
  },
  beforeDestroy() {
    document.removeEventListener('click', this.onDocumentClick, true);
    window.removeEventListener('scroll', this.onScroll, true);
    if (this.cardEl && this.cardEl.parentNode) {
      this.cardEl.parentNode.removeChild(this.cardEl);
    }
  },
  methods: {
    toggle() {
      this.menu = !this.menu;
      if (this.menu) {
        this.$nextTick(this.positionMenu);
      }
    },
    positionMenu() {
      if (!this.cardEl) {
        return;
      }
      const rect = this.$el.getBoundingClientRect();
      const width = this.cardEl.offsetWidth || 160;
      this.cardEl.style.top = `${Math.round(rect.bottom + 4)}px`;
      this.cardEl.style.left = `${Math.round(rect.right - width)}px`;
    },
    onScroll() {
      if (this.menu) {
        this.menu = false;
      }
    },
    onDocumentClick(event) {
      if (this.menu
          && !this.$el.contains(event.target)
          && !(this.cardEl && this.cardEl.contains(event.target))) {
        this.menu = false;
      }
    },
    select(item) {
      this.menu = false;
      if (item.value !== this.view) {
        this.$emit('change', item.value);
      }
    },
  },
};
</script>
