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
  <span>
    <v-chip
        v-for="(item,i) in items" :key="i"
        :id="`label-${item.id}`"
        :input-value="selected"
        class="ma-1 pr-1 font-weight-bold"
        :class="{ 'active-chip' : item.active }"
        label
        :color="`${item.color} lighten-3`"
        :outlined="!item.color"
        primary
        small>
          <span class="pr-2">
            {{ item.text }}
          </span>
      <v-icon
          v-if="item.project"
          x-small
          class="pr-0"
          v-on="on"
          @click="openEditActionMenu(item)">
        edit
      </v-icon>
      <v-icon
          x-small
          class="pr-0"
          @click="removeLabel(item, i)">
        close
      </v-icon>
    </v-chip>
  </span>
</template>

<script>
export default {
  props: {
    items: [],
  },
  methods: {
    removeLabel(item, index) {
      this.$emit('remove-label', item, index);
    },
    openEditActionMenu(item) {
      this.$emit('open-edit-label-action-menu', true, item);
    },
    resetActiveItem(item) {
      this.items.forEach(item => {
        item.active = false;
      });
      if (item) {
        item.active = true;
      }
      this.$forceUpdate();
    }
  },
};
</script>