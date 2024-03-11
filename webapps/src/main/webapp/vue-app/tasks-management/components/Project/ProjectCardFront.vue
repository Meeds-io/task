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
    :class="project.color && project.color+'_border_bottom' || ''"
    class="tasksCardItem"
    flat
    outlined
    hover>
    <div class="taskItemToolbar d-flex px-2 align-center font-weight-bold">
      <i
        :class="project.color && 'white--text' || 'toolbarNoColor'"
        icon
        small
        class="uiIconInformation taskInfoIcon d-flex"
        @click="$emit('flip')">
      </i>
      <div class="spacer d-none d-sm-inline"></div>
      <span
        :class="project.color && 'white--text' || 'toolbarNoColor'"
        class="projectCardTitle py-3"
        @click="showProjectTasksDetails(project)">
        {{ project.name }}
      </span>
      <v-spacer />
      <i
        v-if="project.canManage"
        :class="project.color && 'white--text' || 'toolbarNoColor'"
        icon
        small
        class="uiIconVerticalDots taskInfoIcon d-flex"
        @click="displayActionMenu = true">
      </i>
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
            <v-list-item-title class="noColorLabel caption text-center text&#45;&#45;secondary">
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
    <div class="taskItemInfo pa-3">
      <div
        :class="getClassDescription()"
        class="taskItemDescription"
        @click="showProjectTasksDetails(project)">
        <p
          v-if="project.description">
          {{ getDescription() }}
        </p>
        <div v-else>
          <span class="noProjectDescription">{{ $t('label.noDescription') }}</span>
        </div>
      </div>
      <div class="ProjectSpace">
        <div v-if="project.space">
          <exo-space-avatar 
            :space="space" 
            :size="28"
            link-style
            popover />
        </div>
      </div>
    </div>
    <div class="SpaceAdmin">
      <v-divider />
      <div class="spaceAdminContainer">
        <div class="spaceAdminWrapper">
          <exo-user-avatar 
            v-if="avatarToDisplay && avatarToDisplay.length === 1"
            :profile-id="avatarToDisplay[0].userName"
            :size="28"
            :extra-class="'my-2'"
            link-style
            popover />
          <div
            v-else
            :class="showAllAvatarList && 'AllManagerAvatar'"
            :style="`min-height:${avatarSize}px`"
            class="managerAvatarsList d-flex flex-nowrap my-2">
            <exo-user-avatars-list
              :users="avatarToDisplay"
              :max="5"
              :icon-size="avatarSize"
              :margin-left="avatarToDisplay.length > 1 && 'ml-n4' || ''"
              :compact="avatarToDisplay.length > 1"
              extra-class="position-absolute"
              clickable="'false'"
              avatar-overlay-position
              retrieve-extra-information
              @open-detail="$root.$emit('displayProjectManagers', avatarToDisplay)" />
          </div>
        </div>
      </div>
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
      avatarSize: 28
    };
  },
  computed: {
    avatarToDisplay () {
      const projectManagersList = [];
      if ( this.managerIdentities && this.managerIdentities.length ) {
        this.managerIdentities.forEach((manager) => {
          projectManagersList.push({'userName': manager.username});
        });
      }
      return projectManagersList;
    },
    space() {
      return this.projectSpace;
    }
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
    if (this.project && this.project.space && this.project.spaceDetails ) {
      this.retrieveSpaceInformation(this.project.spaceDetails.id);
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
    getClassDescription(){
      if (this.project && !this.project.space){
        return 'largeDescriptionArea';
      } else if (this.project && this.project.space){
        return 'largeDescriptionAreaSpace';
      }
    },
    retrieveSpaceInformation(spaceId) {
      this.$spaceService.getSpaceById(spaceId).then(space => {
        this.projectSpace = space;
      });
    }
  },

};
</script>
