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
  <v-app
    id="projectListApplication"
    class="projectAndTasksContainer pa-4 pb-2 transparent"
    flat>
    <div v-show="!displayDetails">
      <project-list-toolbar
        :keyword="keyword"
        :project-filter-selected="projectFilterSelected"
        :space-name="spaceName"
        @keyword-changed="keyword = $event"
        @filter-changed="projectFilterSelected = $event" />
      <project-card-list
        :keyword="keyword"
        :space-name="spaceName"
        :project-filter-selected="projectFilterSelected"
        :loading-projects="loadingProjects" />
    </div>
    <div v-show="displayDetails">
      <tasks-view-dashboard :project="project" />
    </div>
    <project-manager-drawer />
  </v-app>
</template>
<script>
export default {
  props: {
    spaceName: {
      type: String,
      default: '',
    },
    displayDetails: {
      type: Boolean,
      default: false,
    }
  },
  data () {
    return {
      project: '',
      keyword: null,
      loadingProjects: false,
      projectFilterSelected: 'ALL',
    };
  },
  created() {
    document.addEventListener('showProjectTasks', (event) => {
      if (event && event.detail) {
        this.project =  event.detail;
        window.setTimeout(() => {
          this.$root.$emit('set-url', {type: 'project',id: this.project.id});
        }, 200);
      }
    });
    document.addEventListener('hideProjectTasks', () => {
      this.displayDetails = false;
    });
  },
};
</script>