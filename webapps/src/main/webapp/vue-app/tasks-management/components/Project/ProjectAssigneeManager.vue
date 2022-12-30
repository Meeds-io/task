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
  <v-flex class="user-suggester">
    <div v-if="manager" class="identitySuggester no-border mt-0">
      <div v-if="spaces.length">
        <div class="mt-4">{{ $t('label.spaceManagers') }}</div>
        <project-event-form-assignee-and-participator-item
          v-for="attendee in spaces"
          :key="attendee.id"
          :attendee="attendee"
          :project="project"
          :removable="manager.length>1"
          @remove-attendee="removeAttendee" />
      </div>
      <div v-if="spaces.length && users.length" class="mt-2 font-weight-bold">{{ $t('label.and') }}</div>
      <div v-if="users.length">
        <project-event-form-assignee-and-participator-item
          v-for="attendee in users"
          :key="attendee.id"
          :attendee="attendee"
          :project="project"
          :removable="manager.length>1"
          @remove-attendee="removeAttendee" />
      </div>
    </div>
    <div
      v-show="!showManager && canEditManager"
      class="editManager"
      @click="showManager_(true)">
      <a href="#" class="editManager">
        <i class="fas fa-pencil-alt uiIconProject"></i>
        {{ $t('label.editManager') }}
      </a>
    </div>

    <exo-identity-suggester
      v-show="showManager"
      ref="invitedAttendeeAutoComplete"
      v-model="invitedAttendee"
      :labels="participantSuggesterLabels"
      :search-options="searchOptions"
      :ignore-items="ignoredMembers"
      :type-of-relations="relationsType"
      name="inviteAttendee"
      include-users />
  </v-flex>
</template>

<script>
export default {
  props: {
    manager: {
      type: Array,
      default: () => [],
    },
    project: {
      type: Object,
      default: () => ({}),
    },
    showManager: {
      type: Boolean,
      default: false,
    }
  },
  data() {
    return {
      currentUser: null,
      invitedAttendee: [],
      users: [],
      spaces: [],
    };
  },
  computed: {
    canEditManager() {
      return !this.project.spaceName;
    },
    searchOptions(){
      if (this.project.spaceDetails){
        return {
          spaceURL: this.project.spaceDetails.prettyName,
          currentUser: this.currentUser
        };
      }
      return this.currentUser;
    },
    relationsType(){
      if (this.project.spaceDetails){
        return 'member_of_space';
      }
      return '';
    },
    participantSuggesterLabels() {
      return {
        searchPlaceholder: this.$t('label.searchPlaceholder'),
        placeholder: this.$t('label.inviteManagers'),
        noDataLabel: this.$t('label.noDataLabel'),
      };
    },
    ignoredMembers() {
      if (this.manager){
        return this.manager.map(attendee => `${attendee.providerId}:${attendee.remoteId}`);
      }
      return '';
    },
  },
  watch: {
    currentUser() {
      this.reset();
    },
    project() {
      this.reset();
    },
    invitedAttendee() {
      if (!this.invitedAttendee) {
        this.$nextTick(this.$refs.invitedAttendeeAutoComplete.$refs.selectAutoComplete.deleteCurrentItem);
        return;
      }
      if (!this.manager) {
        this.manager = [];
      }

      const found = this.manager.find(attendee => {
        return attendee.remoteId === this.invitedAttendee.remoteId
                  && attendee.providerId === this.invitedAttendee.providerId;
      });
      if (!found) {
        this.manager.push(
          this.$suggesterService.convertSuggesterItemToIdentity(this.invitedAttendee),
        );
      }
      this.spaces = this.manager.filter(attendee => attendee.providerId === 'space');
      this.users = this.manager.filter(attendee => attendee.providerId === 'organization');
      this.$root.$emit('task-project-manager',this.manager);
      this.invitedAttendee = null;
    },
  },
  created() {
    this.reset();
  },
  mounted(){
    this.$userService.getUser(eXo.env.portal.userName).then(user => {
      this.currentUser = user;
    });
  },
  methods: {
    showManager_(e) {
      this.showManager=e;
      if (e){
        this.$emit('managerAssignmentsOpened');
      }
    },
    reset() {
      if (this.manager && !this.manager.length>0) { 
        // Add current user as default attendee
        if (eXo.env.portal.spaceName) {
          this.manager = [{
            id: `space:${eXo.env.portal.spaceName}`,
            providerId: 'space',
            remoteId: eXo.env.portal.spaceGroup,
            profile: {
              avatar: `/portal/rest/v1/social/spaces/${eXo.env.portal.spaceName}/avatar`,
              fullname: eXo.env.portal.spaceDisplayName,
              originalName: eXo.env.portal.spaceGroup,
            },
          }];
        } else {
          if (this.currentUser) {
            this.manager = [{
              id: `organization:${eXo.env.portal.userIdentityId}`,
              providerId: 'organization',
              remoteId: eXo.env.portal.userName,
              profile: {
                avatar: this.currentUser.avatar,
                fullname: this.currentUser.fullname,
              },
            }];
          } else {
            this.manager = [];
          }
        }
      }

      this.spaces = this.manager.filter(attendee => attendee.providerId === 'space');
      this.users = this.manager.filter(attendee => attendee.providerId === 'organization');
      if (this.$refs.invitedAttendeeAutoComplete) {
        this.$refs.invitedAttendeeAutoComplete.focus();
      }
      this.$root.$emit('task-project-manager',this.manager);
    },
    removeAttendee(attendee) {
      const index = this.manager.findIndex(addedAttendee => {
        return attendee.remoteId === addedAttendee.remoteId
                  && attendee.providerId === addedAttendee.providerId;
      });
      if (index >= 0) {
        this.manager.splice(index, 1);
      }
      this.spaces = this.manager.filter(attendee => attendee.providerId === 'space');
      this.users = this.manager.filter(attendee => attendee.providerId === 'organization');
      this.$root.$emit('task-project-manager',this.manager);
    },
  }
};
</script>
