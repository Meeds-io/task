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
  <application-toolbar
    id="projectListToolbar"
    compact
    :right-text-filter="{
      minCharacters: 1,
      placeholder: $t('label.filterProject'),
      tooltip: $t('label.filterProject'),
    }"
    :right-select-box="{
      hide: !!spaceName,
      selected: projectFilterSelected,
      items: projectFilterItems,
    }"
    @filter-text-input-end-typing="onKeyword"
    @filter-select-change="onFilterChange">
    <template #left>
      <!-- The slot itself is always declared (ApplicationToolbar reads $slots.left
           non-reactively); the create button is gated inside so it appears once the
           async space permission resolves, e.g. when displayed inside a space. -->
      <v-btn
        v-if="enableCreateButton"
        class="btn btn-primary addNewProjectButton text-none"
        @click="openDrawer">
        <v-icon size="16" dark class="me-2">fa-plus</v-icon>
        <span>{{ $t('label.addProject') }}</span>
      </v-btn>
    </template>
  </application-toolbar>
</template>
<script>
export default {
  props: {
    keyword: {
      type: String,
      default: null,
    },
    spaceName: {
      type: String,
      default: '',
    },
    projectFilterSelected: {
      type: String,
      default: 'ALL',
    },
  },
  data () {
    return {
      projectFilter: [
        {name: 'ALL'},{name: 'MANAGED'},{name: 'COLLABORATED'},{name: 'WITH_TASKS'},{name: 'FAVORITES'}
      ],
      currentSpace: false,
    };
  },
  computed: {
    enableCreateButton() {
      return eXo.env.portal.spaceId ? this.currentSpace && this.currentSpace.canEdit : true;
    },
    projectFilterItems() {
      return this.projectFilter.map(item => ({
        value: item.name,
        text: this.$t(`label.project.filter.${item.name.toLowerCase()}`),
      }));
    },
  },
  created() {
    this.retrieveCurrentSpace();
  },
  methods: {
    retrieveCurrentSpace() {
      if (eXo.env.portal.spaceId) {
        return this.$spaceService.getSpaceById(eXo.env.portal.spaceId).then(space => {
          this.currentSpace = space;
        });
      }
    },
    openDrawer() {
      this.$root.$emit('open-project-drawer', {});
    },
    onKeyword(term) {
      this.$emit('keyword-changed', term);
    },
    onFilterChange(value) {
      this.$emit('filter-changed', value);
    }
  }
};
</script>
