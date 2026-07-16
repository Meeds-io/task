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
<!-- Inline favorite star injected in the project board header (any viewer can use it). -->
<template>
  <favorite-button
    v-if="objectId && !$vuetify.breakpoint.smAndDown"
    :id="objectId"
    :favorite="isFavorite"
    :space-id="spaceId"
    type="project"
    type-label="project" />
</template>
<script>
export default {
  props: {
    project: {
      type: Object,
      default: null,
    },
  },
  computed: {
    objectId() {
      return this.project && this.project.id && String(this.project.id);
    },
    isFavorite() {
      return !!(this.project && this.project.favorite);
    },
    spaceId() {
      return this.project && this.project.spaceDetails && this.project.spaceDetails.id
        && String(this.project.spaceDetails.id);
    },
  },
};
</script>
