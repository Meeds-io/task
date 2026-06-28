<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <favorite-button
    v-if="objectId"
    :id="objectId"
    :favorite="isFavorite"
    :space-id="spaceId"
    :small="false"
    type="task"
    type-label="task"
    display-label
    @added="added"
    @removed="removed"
    @add-error="addError"
    @remove-error="removeError" />
</template>
<script>
export default {
  props: {
    task: {
      type: Object,
      default: null,
    },
  },
  created() {
    // Depending on how the drawer was opened, the task object may not carry the
    // favorite flag (it is only enriched by GET /tasks/{id}); resolve it lazily
    // so the toggle reflects the real state. favorite-button reacts to the prop.
    if (this.task && this.task.id && typeof this.task.favorite !== 'boolean') {
      this.$tasksService.getTaskById(this.task.id)
        .then(task => this.$set(this.task, 'favorite', !!(task && task.favorite)))
        .catch(() => this.$set(this.task, 'favorite', false));
    }
  },
  computed: {
    objectId() {
      return this.task && this.task.id && String(this.task.id);
    },
    isFavorite() {
      return !!(this.task && this.task.favorite);
    },
    spaceId() {
      return eXo.env.portal.spaceId && String(eXo.env.portal.spaceId);
    },
  },
  methods: {
    added() {
      if (this.task) {
        this.task.favorite = true;
      }
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyAddedAsFavorite'));
    },
    removed() {
      if (this.task) {
        this.task.favorite = false;
      }
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyDeletedFavorite'));
    },
    addError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorAddingAsFavorite', 'task'), 'error');
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
  },
};
</script>
