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
  <v-list-item
    v-if="objectId"
    :disabled="loading"
    class="menu-list"
    @click="toggleFavorite">
    <v-list-item-title class="subtitle-2">
      <v-icon
        size="16"
        class="pe-1"
        :class="isFavorite && 'warning--text'">
        {{ isFavorite && 'fas fa-star' || 'far fa-star' }}
      </v-icon>
      <span>{{ label }}</span>
    </v-list-item-title>
  </v-list-item>
</template>
<script>
export default {
  props: {
    task: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    isFavorite: false,
    loading: false,
  }),
  computed: {
    objectId() {
      return this.task && this.task.id && String(this.task.id);
    },
    spaceId() {
      return eXo.env.portal.spaceId && String(eXo.env.portal.spaceId);
    },
    label() {
      return this.isFavorite && this.$t('label.favorite.remove') || this.$t('label.favorite.add');
    },
  },
  created() {
    // Depending on how the drawer was opened, the task object may not carry the
    // favorite flag (it is only enriched by GET /tasks/{id}); resolve it lazily.
    if (this.task && this.task.id && typeof this.task.favorite !== 'boolean') {
      this.$tasksService.getTaskById(this.task.id)
        .then(task => this.isFavorite = !!(task && task.favorite))
        .catch(() => this.isFavorite = false);
    } else {
      this.isFavorite = !!(this.task && this.task.favorite);
    }
    document.addEventListener('metadata.favorite.updated', this.favoriteUpdated);
  },
  beforeDestroy() {
    document.removeEventListener('metadata.favorite.updated', this.favoriteUpdated);
  },
  methods: {
    favoriteUpdated(event) {
      const metadata = event && event.detail;
      if (metadata && metadata.objectType === 'task' && metadata.objectId === this.objectId) {
        this.isFavorite = metadata.favorite;
      }
    },
    toggleFavorite() {
      if (this.loading || !this.objectId) {
        return;
      }
      this.loading = true;
      const wasFavorite = this.isFavorite;
      const promise = wasFavorite
        ? this.$favoriteService.removeFavorite('task', this.objectId)
        : this.$favoriteService.addFavorite('task', this.objectId, null, this.spaceId);
      promise.then(() => {
        this.isFavorite = !wasFavorite;
        if (this.task) {
          this.task.favorite = this.isFavorite;
        }
        document.dispatchEvent(new CustomEvent(this.isFavorite && 'favorite-added' || 'favorite-removed', {
          detail: {
            type: 'task',
            typeLabel: 'task',
            id: this.objectId,
            spaceId: this.spaceId,
          },
        }));
        document.dispatchEvent(new CustomEvent('metadata.favorite.updated', {
          detail: {
            objectType: 'task',
            objectId: this.objectId,
            favorite: this.isFavorite,
          },
        }));
        this.displayAlert(this.$t(this.isFavorite
          && 'Favorite.tooltip.SuccessfullyAddedAsFavorite'
          || 'Favorite.tooltip.SuccessfullyDeletedFavorite'));
      })
        .catch(() => this.displayAlert(this.$t('Favorite.tooltip.ErrorAddingAsFavorite', 'task'), 'error'))
        .finally(() => this.loading = false);
    },
    displayAlert(message, type) {
      document.dispatchEvent(new CustomEvent('notification-alert', {
        detail: {
          message,
          type: type || 'success',
        },
      }));
    },
  },
};
</script>
