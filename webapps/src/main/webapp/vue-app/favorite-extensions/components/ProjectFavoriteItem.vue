<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
-->
<template>
  <v-list-item
    v-if="project"
    :href="projectUrl"
    @keydown.enter="setAsViewed"
    @auxclick="setAsViewed"
    @click="setAsViewed">
    <v-list-item-icon class="me-3 my-auto">
      <v-card
        :min-width="iconWidth"
        class="d-flex justify-center no-border-radius"
        color="transparent"
        flat>
        <v-icon :size="iconSize">fa-tasks</v-icon>
      </v-card>
    </v-list-item-icon>
    <v-list-item-content>
      <v-list-item-title class="text-truncate">{{ projectName }}</v-list-item-title>
    </v-list-item-content>
    <v-list-item-action>
      <favorite-button
        :id="id"
        :favorite="isFavorite"
        :top="top"
        :right="right"
        type="project"
        type-label="project"
        @removed="removed"
        @remove-error="removeError" />
    </v-list-item-action>
  </v-list-item>
</template>
<script>
export default {
  props: {
    id: {
      type: String,
      default: () => null,
    },
    clickCallback: {
      type: Function,
      default: null,
    },
    expanded: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['removed'],
  data: () => ({
    isFavorite: true,
    project: null,
    top: 0,
    right: 0,
  }),
  computed: {
    iconWidth() {
      return this.expanded ? 40 : 30;
    },
    iconSize() {
      return this.expanded ? 34 : 24;
    },
    projectName() {
      return this.project?.name || '';
    },
    projectUrl() {
      return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks/projectDetail/${this.id}`;
    },
  },
  async created() {
    try {
      this.project = await this.$projectService.getProject(this.id, false);
    } catch {
      this.$root.$emit('favorite-removed', 'project', this.id);
    }
  },
  methods: {
    removed() {
      this.isFavorite = false;
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyDeletedFavorite'));
      this.$emit('removed');
      this.$root.$emit('refresh-favorite-list');
    },
    removeError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorDeletingFavorite', 'project'), 'error');
    },
    displayAlert(message, type) {
      document.dispatchEvent(new CustomEvent('notification-alert', {detail: {
        message,
        type: type || 'success',
      }}));
    },
    setAsViewed(event) {
      if (event.which === 1 || event.which === 2) {
        this.clickCallback('project', this.id);
      }
    },
  },
};
</script>
