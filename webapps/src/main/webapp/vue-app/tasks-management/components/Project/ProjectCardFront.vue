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
  <v-card
    :id="`project-${project.id}`"
    class="projectCardItem d-flex flex-column"
    flat
    outlined
    hover>
    <!-- Left accent bar carrying the project color -->
    <div
      :class="project.color || 'projectMonogramDefault'"
      class="projectCardColorBar"></div>
    <!-- Header: project name/space + favorite & actions (identity carried by the left color bar) -->
    <div class="d-flex align-start px-3 pt-3">
      <div
        class="projectCardHeading flex-grow-1 text-truncate pointer"
        @click="showProjectTasksDetails(project)">
        <div class="projectCardTitle text-truncate">{{ project.name }}</div>
        <div
          v-if="!isInSpace && project.space && projectSpace && projectSpace.displayName"
          class="d-flex align-center text-truncate mt-1">
          <exo-space-avatar
            :space="space"
            :size="18"
            link-style />
        </div>
      </div>
      <div class="d-flex align-center flex-shrink-0">
        <favorite-button
          v-if="objectId"
          :id="objectId"
          :favorite="isFavorite"
          :space-id="favoriteSpaceId"
          type="project"
          type-label="project" />
        <v-btn
          v-if="project.canManage"
          class="px-0 ms-1"
          icon
          small
          @click="displayActionMenu = true">
          <v-icon size="18" class="text-light-color">fa-ellipsis-v</v-icon>
        </v-btn>
        <v-menu
          v-model="displayActionMenu"
          :attach="`#project-${project.id}`"
          transition="slide-x-reverse-transition"
          content-class="projectActionMenu"
          offset-y>
          <v-list class="pa-0" dense>
            <v-list-item class="menu-list" @click="openEditDrawer()">
              <v-list-item-title class="subtitle-2">
                <i class="uiIcon uiIconEdit pe-1"></i>
                <span>{{ $t('label.edit') }}</span>
              </v-list-item-title>
            </v-list-item>
            <extension-registry-components
              :params="{
                project,
              }"
              name="TaskProjectMenu"
              type="task-project-menu"
              parent-element="div"
              element="div" />
            <v-list-item class="draftButton" @click="confirmDeleteProject()">
              <v-list-item-title class="subtitle-2">
                <i class="uiIcon uiIconTrash pe-1"></i>
                <span>{{ $t('label.delete') }}</span>
              </v-list-item-title>
            </v-list-item>
            <v-list-item class="clone" @click="confirmCloneProject()">
              <v-list-item-title class="subtitle-2">
                <i class="uiIcon uiIconCloneNode pe-1"></i>
                <span>{{ $t('label.clone') }}</span>
              </v-list-item-title>
            </v-list-item>
            <v-list-item class="px-2 noColorLabel">
              <v-list-item-title class="noColorLabel caption text-center text--secondary">
                <span @click="changeColorProject(project,'')">{{ $t('label.noColor') }}</span>
              </v-list-item-title>
            </v-list-item>
            <v-list-item>
              <v-list-item-title class="subtitle-2 row projectColorPicker mx-auto my-2">
                <span
                  v-for="(color, i) in projectColors"
                  :key="i"
                  :class="[ color.class , color.class === project.color ? 'isSelected' : '']"
                  class="projectColorCell"
                  @click="changeColorProject(project,color.class)"></span>
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </div>
    </div>

    <!-- Description -->
    <div
      class="px-3 mt-3 projectCardDescription pointer"
      @click="showProjectTasksDetails(project)">
      <span v-if="hasDescription">{{ getDescription() }}</span>
      <span v-else class="text-light-color font-italic">{{ $t('label.noDescription') }}</span>
    </div>

    <v-spacer />

    <!-- Footer: member avatars + task count -->
    <div class="px-3 pt-3 pb-2 mt-3 d-flex align-center projectCardFooter">
      <exo-user-avatars-list
        v-if="avatarToDisplay.length"
        :users="avatarToDisplay"
        :max="4"
        :icon-size="28"
        :margin-left="avatarToDisplay.length > 1 && 'ml-n3' || ''"
        :compact="avatarToDisplay.length > 1"
        retrieve-extra-information
        @open-detail="$root.$emit('displayProjectManagers', avatarToDisplay)" />
      <span v-else class="caption text-light-color">{{ $t('message.noManager') }}</span>
      <v-spacer />
      <div
        v-if="tasksCount"
        class="projectCardProgress d-flex align-center flex-shrink-0">
        <span class="caption text-light-color me-2">{{ tasksCount }}</span>
        <div class="projectCardProgressBar d-flex">
          <v-tooltip
            v-for="segment in statusSegments"
            :key="segment.name"
            bottom>
            <template #activator="{ on, attrs }">
              <div
                class="projectCardProgressSegment"
                :style="{ width: `${segment.pct}%`, backgroundColor: segment.color }"
                v-bind="attrs"
                v-on="on"></div>
            </template>
            <span>{{ segment.value }} - {{ segment.name }}</span>
          </v-tooltip>
        </div>
      </div>
      <span
        v-else-if="tasksCount === 0"
        class="caption text-light-color flex-shrink-0">
        {{ $t('label.project.card.tasksCount', {0: 0}) }}
      </span>
    </div>

    <exo-confirm-dialog
      ref="CancelSavingChangesDialog"
      :message="$t('popup.msg.delete', {0: project.name})"
      :title="$t('popup.confirm')"
      :ok-label="$t('popup.delete')"
      :cancel-label="$t('popup.cancel')"
      @ok="deleteProject()" />
    <exo-confirm-dialog
      ref="CancelSavingChangesCloneDialog"
      :message="$t('popup.msg.clone', {0: project.name})"
      :title="$t('popup.confirm')"
      :ok-label="$t('popup.clone')"
      :cancel-label="$t('popup.cancel')"
      @ok="cloneProject()" />
  </v-card>
</template>
<script>
export default {
  props: {
    project: {
      type: Object,
      default: null,
    }
  },
  data () {
    return {
      displayActionMenu: false,
      waitTimeUntilCloseMenu: 200,
      tasksCount: null,
      statusStats: [],
      statusRanks: {},
      // Palette for the per-status progress segments, indexed by the status
      // column rank so a given status keeps the same colour on every card:
      // ToDo (slate) -> InProgress (blue) -> WaitingOn (amber) -> Done (green).
      statusPalette: ['#94a3b8', '#60a5fa', '#fbbf24', '#34d399', '#a78bfa', '#f87171'],
      projectColors: [
        { class: 'asparagus' },
        { class: 'munsell_blue' },
        { class: 'navy_blue' },
        { class: 'purple' },
        { class: 'red' },
        { class: 'brown' },
        { class: 'laurel_green' },
        { class: 'sky_blue' },
        { class: 'blue_gray' },
        { class: 'light_purple' },
        { class: 'hot_pink' },
        { class: 'light_brown' },
        { class: 'moss_green' },
        { class: 'powder_blue' },
        { class: 'light_blue' },
        { class: 'pink' },
        { class: 'Orange' },
        { class: 'gray' },
        { class: 'green' },
        { class: 'baby_blue' },
        { class: 'light_gray' },
        { class: 'beige' },
        { class: 'yellow' },
        { class: 'plum' },
      ],
      managerIdentities: this.project && this.project.managerIdentities,
      projectSpace: {},
    };
  },
  computed: {
    hasDescription() {
      return !!(this.project && this.project.description && this.getDescription().trim().length);
    },
    objectId() {
      return this.project && this.project.id && String(this.project.id);
    },
    isFavorite() {
      return !!(this.project && this.project.favorite);
    },
    favoriteSpaceId() {
      return this.project && this.project.spaceDetails && this.project.spaceDetails.id
        && String(this.project.spaceDetails.id);
    },
    avatarToDisplay () {
      const projectManagersList = [];
      if ( this.managerIdentities && this.managerIdentities.length ) {
        this.managerIdentities.forEach((manager) => {
          manager.ariaLabel=`${this.$t('project.card.managerAvatar.ariaLabel')}`;
          projectManagersList.push({'userName': manager.username, 'ariaLabel': manager.ariaLabel});
        });
      }
      return projectManagersList;
    },
    space() {
      return this.projectSpace;
    },
    isInSpace() {
      // When the portlet is displayed inside a space, every project belongs to
      // that space, so the per-card space chip is redundant.
      return !!(eXo.env.portal.spaceId);
    },
    statusSegments() {
      if (!this.tasksCount) {
        return [];
      }
      return this.statusStats
        .filter(stat => stat.value > 0)
        .map(stat => {
          const rank = this.statusRanks[stat.name] || 0;
          return {
            name: stat.name,
            value: stat.value,
            rank,
            pct: Math.round((stat.value / this.tasksCount) * 100),
            color: this.statusPalette[rank % this.statusPalette.length],
          };
        })
        .sort((a, b) => a.rank - b.rank);
    },
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.displayActionMenu) {
        window.setTimeout(() => {
          this.displayActionMenu = false;
        }, this.waitTimeUntilCloseMenu);
      }
    });
    this.$root.$on('update-projects-list-avatar',managerIdentities =>{
      this.project.managerIdentities=managerIdentities;
    });
    if (!this.isInSpace && this.project && this.project.space && this.project.spaceDetails ) {
      this.retrieveSpaceInformation(this.project.spaceDetails.id);
    }
  },
  mounted() {
    if (this.project && this.project.id && this.$projectService && this.$projectService.getProjectStats) {
      // Order the status segments by their column rank so the bar mirrors the
      // board's left-to-right workflow; counts come from the statistics endpoint.
      // Both are awaited together so the ranks are known on the first render of
      // the segments (otherwise re-sorting after a late ranks fetch wouldn't
      // reorder the keyed v-tooltip nodes already in the DOM).
      const statusesPromise = this.$tasksService && this.$tasksService.getStatusesByProjectId
        ? this.$tasksService.getStatusesByProjectId(this.project.id).catch(() => [])
        : Promise.resolve([]);
      Promise.all([statusesPromise, this.$projectService.getProjectStats(this.project.id)])
        .then(([statuses, data]) => {
          const ranks = {};
          (statuses || []).forEach(status => ranks[status.name] = status.rank);
          this.statusRanks = ranks;
          this.tasksCount = data && data.totalNumberTasks || 0;
          this.statusStats = data && data.statusStats || [];
        })
        .catch(() => this.tasksCount = null);
    }
  },
  methods: {
    showProjectTasksDetails(project) {
      document.dispatchEvent(new CustomEvent('showProjectTasks', {detail: project}));
    },
    openEditDrawer() {
      this.$emit('openDrawer');
    },
    onCloseDrawer: function (drawer) {
      this.drawer = drawer;
    },
    confirmDeleteProject: function () {
      this.$refs.CancelSavingChangesDialog.open();
    },
    confirmCloneProject: function () {
      this.$refs.CancelSavingChangesCloneDialog.open();
    },
    deleteProject() {
      this.$projectService.deleteProjectInfo(this.project)
        .then(() => this.$emit('refreshProjects'))
        .then(this.$root.$emit('show-alert',{type: 'success',message: this.$t('alert.success.project.deleted')} ))
        .catch(e => {
          console.error('Error updating project', e);
          this.$root.$emit('show-alert',{type: 'error',message: this.$t('alert.error')} );
        });
    },
    cloneProject() {
      this.$projectService.cloneProject(this.project)
        .then(() => this.$emit('refreshProjects'))
        .then(this.$root.$emit('show-alert',{type: 'success',message: this.$t('alert.success.project.cloned')} ))
        .catch(e => {
          console.error('Error updating project', e);
          this.$root.$emit('show-alert',{type: 'error',message: this.$t('alert.error')} );
        });
    },
    changeColorProject(project,color) {
      this.$projectService.updateProjectColor(project, color)
        .then(() => this.$emit('projectChangeColor'))
        .then(this.project.color = color);
    },
    getDescription(){
      let text=this.project.description;
      const div = document.createElement('div');
      div.innerHTML = text;
      text = div.textContent || div.innerText || '';
      return text;
    },
    retrieveSpaceInformation(spaceId) {
      this.$spaceService.getSpaceById(spaceId, 'favorite').then(space => {
        this.projectSpace = space;
      });
    }
  },
};
</script>
