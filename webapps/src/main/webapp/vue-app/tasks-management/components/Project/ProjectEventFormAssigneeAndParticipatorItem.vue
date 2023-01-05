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
  <v-chip
    :close="canRemoveAttendee"
    class="identitySuggesterItem me-2 mt-2"
    @click:close="$emit('remove-attendee', attendee)">
    <v-avatar left>
      <v-img :src="avatarUrl" />
    </v-avatar>
    <span class="text-truncate">
      {{ displayName }}
    </span>
  </v-chip>
</template>

<script>

export default {
  props: {
    attendee: {
      type: Object,
      default: () => ({}),
    },
    project: {
      type: Object,
      default: () => ({}),
    },
    removable: {
      type: Boolean,
      default: true,
    }
  },
  data () {
    return {
      isExternal: false,
    };
  },
  computed: {
    canRemoveAttendee() {
      if (this.removable===false){
        return false;
      }
      if (this.project.spaceDetails && this.project.spaceDetails.prettyName) {
        return this.attendee.id !== `space:${this.project.spaceDetails.prettyName}`;
      } else {
        return true;
      }
    },
    avatarUrl() {
      const profile = this.attendee && (this.attendee.profile || this.attendee.space);
      return profile && (profile.avatarUrl || profile.avatar);
    },
    displayName() {
      const profile = this.attendee && (this.attendee.profile || this.attendee.space);
      const fullName = profile && (profile.displayName || profile.fullname || profile.fullName);
      return this.isExternal && fullName.indexOf(this.$t('label.external')) < 0 ? `${fullName} (${this.$t('label.external')})` : fullName;
    },
  },
  created() {
    if (this.attendee.providerId === 'organization'){
      this.$userService.getUser(this.attendee.remoteId)
        .then(user => {
          this.isExternal = user.external === 'true';
        });
    }
  },
};
</script>
