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
  <v-app>
    <template v-if="projectId">
      <div class="subtitle-1 font-weight-bold mb-2">
        {{ $t('gamification.event.display.doItNow') }}
      </div>
      <v-list-item class="clickable" :href="projectLink">
        <v-list-item-icon class="me-3 my-auto">
          <v-icon class="primary--text">fas fa-tasks</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title class="text-color body-2">
            <p
              class="ma-auto text-truncate"
              v-sanitized-html="projectName"></p>
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
    </template>
    <template v-else>
      <div class="subtitle-1 font-weight-bold mb-2">
        {{ $t('gamification.event.display.goThere') }}
      </div>
      <div class="d-flex justify-center">
        <v-btn
          :href="tasksLink"
          max-width="250"
          class="ignore-vuetify-classes text-capitalize btn btn-primary">
          {{ $t('gamification.event.display.seeProjects') }}
        </v-btn>
      </div>
    </template>
  </v-app>
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
      project: null,
    };
  },
  computed: {
    projectId() {
      return this.properties?.projectId;
    },
    projectLink() {
      return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks/projectDetail/${this.projectId}`;
    },
    tasksLink() {
      return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks`;
    },
    projectName() {
      return this.project?.name;
    },
  },
  created() {
    if (this.projectId) {
      this.loadProject();
    }
  },
  methods: {
    loadProject() {
      return this.$projectService.getProject(this.projectId)
        .then((project) => {
          this.project = project;
        });
    },
  }
};
</script>
