<!--
This file is part of the Meeds project (https://meeds.io/).

Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
  <v-flex :id="id">
    <v-autocomplete
      ref="selectAutoComplete"
      v-model="value"
      :label="labels.label"
      :placeholder="labels.placeholder"
      :items="projects"
      :loading="loadingSuggestions"
      :multiple="multiple"
      :hide-no-data="hideNoData"
      append-icon=""
      menu-props="closeOnClick, closeOnContentClick, maxHeight = 100"
      class="identitySuggester identitySuggesterInputStyle"
      content-class="identitySuggesterContent"
      width="100%"
      max-width="100%"
      item-text="name"
      item-value="id"
      return-object
      persistent-hint
      hide-selected
      cache-items
      chips
      dense
      flat
      required
      @update:search-input="searchTerm = $event">
      <template slot="no-data">
        <v-list-item class="pa-0">
          <v-list-item-title
            v-if="displaySearchPlaceHolder"
            :style="menuItemStyle"
            class="px-2">
            {{ labels.searchPlaceholder }}
          </v-list-item-title>
          <v-list-item-title
            v-else-if="loadingSuggestions"
            :style="menuItemStyle"
            class="px-2">
            {{ $t('Search.label.inProgress') }}
          </v-list-item-title>
          <v-list-item-title
            v-else-if="labels.noDataLabel"
            :style="menuItemStyle"
            class="px-2">
            {{ labels.noDataLabel }}
          </v-list-item-title>
        </v-list-item>
      </template>
      <template slot="selection" slot-scope="{item, selected}">
        <v-chip
          :input-value="selected"
          :color="item.color ? item.color : 'primary'"
          close
          dark
          @click:close="remove(item)">
          <span class="text-truncate">
            {{ item.name }}
          </span>
        </v-chip>
      </template>
      <template slot="item" slot-scope="data">
        <v-list-item-title
          class="text-truncate identitySuggestionMenuItemText"
          v-text="data.item.name" />
      </template>
    </v-autocomplete>
  </v-flex>
</template>
<script>

export default {
  props: {
    multiple: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    value: {
      type: Object,
      default: null,
    },
    projects: {
      type: Array,
      default: function() {
        return [];
      },
    },
    labels: {
      type: Object,
      default: () => ({
        label: '',
        placeholder: '',
        searchPlaceholder: '',
        noDataLabel: '',
      }),
    },
  },
  data() {
    return {
      id: `ProjectSuggester${parseInt(Math.random() * 10000)}`,
      previousSearchTerm: null,
      searchTerm: null,
      searchStarted: false,
      loadingSuggestions: false,
      startSearchAfterInMilliseconds: 300,
      endTypingKeywordTimeout: 50,
      startTypingKeywordTimeout: 0,
      typing: false,
    };
  },
  computed: {
    displaySearchPlaceHolder() {
      return this.labels.searchPlaceholder && !this.searchStarted;
    },
    menuItemStyle() {
      return this.width && `width:${this.width}px;max-width:${this.width}px;min-width:${this.width}px;` || '';
    },
    hideNoData() {
      return !this.searchStarted && this.projects.length === 0;
    },
  },
  watch: {
    loadingSuggestions() {
      if (this.loadingSuggestions && !this.searchStarted) {
        this.searchStarted = true;
      }
    },
    searchTerm() {
      this.startTypingKeywordTimeout = Date.now() + this.startSearchAfterInMilliseconds;
      if (!this.typing) {
        this.typing = true;
        this.waitForEndTyping();
      }
    },
    value() {
      this.emitSelectedValue(this.value);
      this.init();
    },
    includeDeleted() {
      this.$refs.selectAutoComplete.cachedItems = [];
    },
  },
  created() {
    this.project = this.value;
    if (this.project) {
      this.projects = [this.project];
    }
  },
  mounted() {
    $(`#${this.id} input`).on('blur', () => {
      this.$refs.selectAutoComplete.isFocused = false;
    });
    if (this.autofocus) {
      window.setTimeout(() => {
        this.focus();
      }, 200);
    }
    this.init();
  },
  methods: {
    init() {
      if (this.value && this.value.length) {
        this.value.forEach(item => {
          if ( item.id ) {
            this.projects = this.value;
          }
        });

      } else if (this.value && this.value.id){
        this.projects = this.value;
      }
    },
    emitSelectedValue(value) {
      this.$emit('input', value);
    },
    remove(item) {
      if (this.value) {
        if (this.value.splice) {
          const index = this.value.findIndex(val => val.id === item.id);
          if (index >= 0){
            this.value.splice(index, 1);
          }
        } else {
          this.value = null;
        }
      }
      this.$emit('removeProject',item);
    },
    waitForEndTyping() {
      window.setTimeout(() => {
        if (Date.now() > this.startTypingKeywordTimeout) {
          this.typing = false;
          this.searchProjects();
        } else {
          this.waitForEndTyping();
        }
      }, this.endTypingKeywordTimeout);
    },
    focus() {
      this.$refs.selectAutoComplete.focus();
    },
    searchProjects() {
      if (this.searchTerm?.length) {
        this.focus();
        if (!this.previousSearchTerm || this.previousSearchTerm !== this.searchTerm) {
          this.loadingSuggestions = true;
          this.projects = [];
          this.$projectService.getProjectsList(null, this.searchTerm, null, 0, 10, null)
            .then(data => {
              this.projects = data.projects;
            })
            .finally(() => this.loadingSuggestions = false);
        }
        this.previousSearchTerm = this.searchTerm;
      } else {
        this.searchStarted = false;
      }
    },
    clear() {
      this.projects = [];
      this.value = null;
      this.loadingSuggestions = false;
      this.$refs.selectAutoComplete.cachedItems = [];
    },
  },
};
</script>