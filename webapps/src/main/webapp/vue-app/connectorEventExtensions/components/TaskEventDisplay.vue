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
    <template v-if="projectIds">
      <div class="subtitle-1 font-weight-bold mb-2">
        {{ $t('gamification.event.display.doItNow') }}
      </div>
      <v-progress-liner
        v-if="!initialized"
        color="primary"
        indeterminate />
      <gamification-task-event-project-item
        v-for="project in projects"
        :key="project.id"
        :project="project" />
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
      projects: [],
      initialized: false
    };
  },
  computed: {
    projectIds() {
      return this.properties?.projectIds;
    },
    tasksLink() {
      return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks`;
    },
  },
  created() {
    if (this.projectIds) {
      this.loadProjects();
    }
  },
  methods: {
    loadProjects() {
      this.projectIds.split(',').forEach((projectId, index) => {
        this.$projectService.getProject(projectId)
          .then((project) => {
            this.projects.push(project);
          }).finally(() => {
            if (index === this.projectIds.split(',').length - 1) {
              this.initialized = true;
            }
          });
      });
    },
  }
};
</script>
