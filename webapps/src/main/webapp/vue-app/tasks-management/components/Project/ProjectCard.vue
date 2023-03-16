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
  <v-app id="projectCard">
    <v-flex :class="flipCard && 'taskCardFlip taskCardFlipped' || 'taskCardFlip'">
      <div class="taskCardFront py-3 px-2">
        <project-card-front
          :project="project"
          @openDrawer="openEditDrawer"
          @closed="onCloseDrawer"
          @refreshProjects="refreshProjects"
          @flip="flipCard = true; flip()" />
      </div>
      <div class="tasksCardBack pa-3">
        <project-card-Reverse
          ref="reversCard"
          :project="project"
          @flip="flipCard = false; flip()" />
      </div>
    </v-flex>
  </v-app>
</template>
<script>
export default {
  props: {
    project: {
      type: Object,
      default: () => ({}),
    }
  },
  data () {
    return {
      flipCard: false,
    };
  },
  methods: {
    openEditDrawer() {
      this.$root.$emit('open-project-drawer', this.project);
    },
    onCloseDrawer: function(drawer){
      this.drawer = drawer;
    },
    refreshProjects: function(){
      this.$emit('refreshProjects');
    },
    flip: function(){
      if (this.flipCard){
        this.$refs.reversCard.getStats(this.project);
      }
    }
  }
};
</script>

