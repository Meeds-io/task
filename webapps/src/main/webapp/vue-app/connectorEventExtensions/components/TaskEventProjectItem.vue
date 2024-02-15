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

<script>
export default {
  props: {
    project: {
      type: Object,
      default: null
    },
  },
  computed: {
    projectId() {
      return this.project?.id;
    },
    projectLink() {
      return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks/projectDetail/${this.projectId}`;
    },
    projectName() {
      return this.project?.name;
    },
  },
};
</script>
