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
  <div>
    <v-card-text class="px-0 pb-0 dark-grey-color font-weight-bold">
      {{ $t('gamification.event.label.project') }}
    </v-card-text>
    <v-radio-group v-model="project" @change="changeSelection">
      <v-radio
        value="ANY"
        :label="$t('gamification.event.label.anyProject')" />
      <v-radio value="ANY_IN_PROJECT" :label="$t('gamification.event.label.onlyProjects')" />
      <project-suggester
        v-if="project === 'ANY_IN_PROJECT'"
        ref="projectsSuggester"
        v-model="selected"
        :labels="projectSuggesterLabels"
        :width="220"
        name="projectsSuggester"
        multiple />
    </v-radio-group>
  </div>
</template>

<script>

export default {
  props: {
    properties: {
      type: Object,
      default: null
    },
    trigger: {
      type: String,
      default: null
    },
  },
  data() {
    return {
      project: 'ANY',
      selected: [],
      startTypingKeywordTimeout: 0,
      startSearchAfterInMilliseconds: 300,
      endTypingKeywordTimeout: 50,
    };
  },
  computed: {
    projectSuggesterLabels() {
      return {
        placeholder: this.$t('gamification.event.label.project.placeholder'),
        noDataLabel: this.$t('gamification.event.label.project.noDataLabel'),
      };
    },
  },
  watch: {
    selected() {
      if (this.selected?.length) {
        const eventProperties = {
          projectIds: this.selected.map(project => project.id).toString(),
        };
        document.dispatchEvent(new CustomEvent('event-form-filled', {detail: eventProperties}));
      } else if (this.project === 'ANY_IN_PROJECT') {
        document.dispatchEvent(new CustomEvent('event-form-unfilled'));
      }
    },
    trigger() {
      this.project = 'ANY';
      document.dispatchEvent(new CustomEvent('event-form-filled'));
    },
  },
  created() {
    if (this.properties?.projectIds) {
      this.project = 'ANY_IN_PROJECT';
      this.properties?.projectIds.split(',').forEach(projectId => {
        this.$projectService.getProject(projectId)
          .then((project) => {
            this.selected.push(project);
          });
      });

    } else {
      document.dispatchEvent(new CustomEvent('event-form-filled'));
    }
  },
  methods: {
    changeSelection() {
      this.selected = null;
      if (this.project === 'ANY') {
        document.dispatchEvent(new CustomEvent('event-form-filled'));
      } else {
        document.dispatchEvent(new CustomEvent('event-form-unfilled'));
      }
    },
  }
};
</script>