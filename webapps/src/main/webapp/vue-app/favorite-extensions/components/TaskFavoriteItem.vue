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
    v-if="task"
    :href="taskUrl"
    @keydown.enter="setAsViewed"
    @auxclick="setAsViewed"
    @click="setAsViewed">
    <v-list-item-icon class="me-3 my-auto">
      <v-card
        :min-width="iconWidth"
        class="d-flex justify-center no-border-radius"
        color="transparent"
        flat>
        <v-icon :size="iconSize">fa-check-square</v-icon>
      </v-card>
    </v-list-item-icon>
    <v-list-item-content>
      <v-list-item-title class="text-truncate">{{ taskTitle }}</v-list-item-title>
    </v-list-item-content>
    <v-list-item-action>
      <favorite-button
        :id="id"
        :favorite="isFavorite"
        :top="top"
        :right="right"
        type="task"
        type-label="task"
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
    task: null,
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
    taskTitle() {
      return this.task?.title || '';
    },
    taskUrl() {
      return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks/taskDetail/${this.id}`;
    },
  },
  async created() {
    try {
      this.task = await this.$tasksService.getTaskById(this.id);
    } catch {
      this.$root.$emit('favorite-removed', 'task', this.id);
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
      this.displayAlert(this.$t('Favorite.tooltip.ErrorDeletingFavorite', 'task'), 'error');
    },
    displayAlert(message, type) {
      document.dispatchEvent(new CustomEvent('notification-alert', {detail: {
        message,
        type: type || 'success',
      }}));
    },
    setAsViewed(event) {
      if (event.which === 1 || event.which === 2) {
        this.clickCallback('task', this.id);
      }
    },
  },
};
</script>
