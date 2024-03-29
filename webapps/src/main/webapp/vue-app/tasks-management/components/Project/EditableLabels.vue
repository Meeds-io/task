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
  <div :id="`label-${item.id}`" @click.stop>
    <v-menu
      :attach="`#label-${item.id}`"
      v-model="displayActionMenu"
      transition="slide-x-reverse-transition"
      content-class="labelsActionMenu"
      :close-on-content-click="false"
      :close-on-click="true"
      offset-y>
      <template #activator="{ on }">
        <v-chip
          v-if="item === Object(item)"
          :id="`label-${item.id}`"
          v-bind="attrs"
          :input-value="selected"
          class="pr-1 font-weight-bold"
          label
          :color="`${item.color} lighten-3`"
          :outlined="!item.color"
          primary
          small>
          <span class="pr-2">
            {{ item.text }}
          </span>
          <v-icon
            x-small
            class="pr-0"
            v-on="on"
            @click="closeLabelsList">
            edit
          </v-icon>
          <v-icon
            x-small
            class="pr-0"
            @click="parent.selectItem(item);removeLabel()">
            close
          </v-icon>
        </v-chip>
      </template>
      <v-list class="pa-0" dense>
        <v-list-item class="px-2 ">
          <v-list-item-content>
            <v-text-field
              ref="autoFocusInput1"
              v-model="item.text"
              type="text"
              class="font-weight-bold text-color mb-1 labels-edit-name"
              @blur="cancel($event)"
              autofocus
              outlined
              dense>
              <i
                dark
                class="uiIconTick success--text clickable label-btn ma-1"
                slot="append"
                @click="editLabel()">
              </i>
              <i
                dark
                class="uiIconClose error--text clickable label-btn ma-1"
                slot="append"
                @click="cancel($event)">
              </i>
            </v-text-field>
          </v-list-item-content>
        </v-list-item>

        <v-list-item class="px-2 noColorLabel">
          <v-list-item-title class="noColorLabel caption text-center text&#45;&#45;secondary">
            <span @click="item.color='';editLabel()">{{ $t('label.noColor') }}</span>
          </v-list-item-title>
        </v-list-item>
        <v-list-item>
          <v-list-item-title class="subtitle-2 row projectColorPicker mx-auto my-2">
            <span
              v-for="(color, i) in labelColors"
              :key="i"
              :class="[ color.class , color.class === item.color ? 'isSelected' : '']"
              class="projectColorCell"
              @click="item.color=color.class;editLabel()"></span>
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
</template>

<script>
export default {
  props: {
    item: {
      type: Object,
      default: () => {
        return {};
      }
    },
    parent: {
      type: Object,
      default: () => {
        return {};
      }
    },
  },
  data() {
    return {
      displayActionMenu: false,
      labelColors: [
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
    };
  },
  methods: {
    editLabel() {
      if ( this.item.project ) {
        this.item.name=this.item.text;
        this.$taskDrawerApi.editLabel(this.item).then( (editedLabel) => {
          this.item=editedLabel;
          this.item.text = this.item.name;
          this.displayActionMenu= false;
          this.$root.$emit('show-alert', {
            type: 'success',
            message: this.$t('alert.success.label.updated')
          });
        }).catch(e => {
          console.error('Error when adding labels', e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
        });
      } else {
        this.$emit('edit-label-on-create', this.item);
        this.item.name=this.item.text;
        this.displayActionMenu= false;
      }
    },
    cancel(event) {
      window.setTimeout(() => {
        this.item.text = this.item.name;
        this.displayActionMenu=false;
        event.stopPropagation();
      }, 100);
    },
    removeLabel() {
      this.displayActionMenu= false;
      this.$emit('remove-label', this.item);
    },
    closeLabelsList() {
      document.dispatchEvent(new CustomEvent('closeLabelsList'));
    }
  }
};
</script>