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
  <div @click.stop>
    <v-chip
        class="ma-1 white--text"
        :class="{ 'active-chip' : (!(labelItem && labelItem.project) && displayActionMenu) }"
        label
        color="`lighten-3`"
        primary
        small
        @click="resetLabelItem(true)"
        style="background: #3f8487">
      <span class="pr-2">
        {{ $t('label.addLabel') }}
      </span>
      <i class="fas fa-plus"></i>
    </v-chip>
    <editable-labels
      ref="childLabel"
      :items="items"
      @remove-label="removeLabel"
      @open-edit-label-action-menu="resetLabelItem"/>
    <v-list class="pa-0 labelsActionMenu" dense v-if="displayActionMenu">
      <v-list-item>
        <v-list-item-content>
          <v-text-field
              ref="autoFocusInput1"
              v-model="labelItem.text"
              type="text"
              class="font-weight-bold pa-0 text-color mb-1 labels-edit-name"
              :placeholder="$t('label.tapLabel.name')"
              autofocus
              outlined
              dense
              @keyup.enter="saveLabel">
            <i
                dark
                class="uiIconTick success--text clickable label-btn ma-1"
                slot="append"
                @click="saveLabel">
            </i>
            <i
                dark
                class="uiIconClose error--text clickable label-btn ma-1"
                slot="append"
                @click="resetLabelItem(false)">
            </i>
          </v-text-field>
        </v-list-item-content>
      </v-list-item>

      <v-list-item class="noColorLabel" style="padding-left: 55px !important;padding-right: 55px !important;">
        <v-list-item-title class="pa-1 noColorLabel caption text-center text&#45;&#45;secondary">
          <span @click="labelItem.color='';saveLabel()">{{ $t('label.noColor') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-list-item>
        <v-list-item-title class="subtitle-2 row projectColorPicker mx-auto my-2">
            <span
                v-for="(color, i) in labelColors"
                :key="i"
                :class="[ color.class , color.class === labelItem.color ? 'isSelected' : '']"
                class="projectColorCell"
                @click="labelItem.color=color.class;saveLabel()"></span>
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </div>
</template>

<script>
export default {
  props: {
    project: {
      type: Object,
      default: () => {
        return {};
      }
    },
  },
  data() {
    return {
      items: [],
      displayActionMenu: false,
      labelItem: null,
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
  created() {
    document.addEventListener('loadAllProjectLabels', event => {
      this.items = [];
      this.resetLabelItem(false);
      if (event && event.detail) {
        const project = event.detail;
        this.items = [];
        this.getProjectLabels(project.id);
      }
    });
  },
  methods: {
    getProjectLabels(projectId) {
      this.$taskDrawerApi.getProjectLabels(projectId).then((labels) => {
        this.items = labels.map(function (el) {
          const o = Object.assign({}, el);
          o.text = o.name;
          o.editMenu=false;
          return o;
        });
      });
    },
    addLabel(label) {
      if ( this.project.id!= null ) {
        label.project=this.project;
        this.$taskDrawerApi.addLabel(label).then( () => {
          this.$root.$emit('show-alert', {
            type: 'success',
            message: this.$t('alert.success.label.created')
          });
          this.resetLabelItem(false);
          this.getProjectLabels(this.project.id);
        }).catch(e => {
          console.error('Error when adding labels', e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
        });
      } else {
        this.items.push(label);
        this.$emit('set-labels', this.items);
        this.resetLabelItem(true);
      }
    },
    editLabel(label) {
      label.name=label.text;
      this.$taskDrawerApi.editLabel(label).then( (editedLabel) => {
        label=editedLabel;
        label.text = label.name;
        this.$root.$emit('show-alert', {
          type: 'success',
          message: this.$t('alert.success.label.updated')
        });
        this.resetLabelItem(false);
      }).catch(e => {
        console.error('Error when editing label', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('alert.error')
        });
      });
    },
    removeLabel(item, index) {
      if (item.project) {
        this.$taskDrawerApi.removeLabel(item.id).then(() => {
          this.$root.$emit('show-alert', {
            type: 'success',
            message: this.$t('alert.success.label.deleted')
          });

        }).catch(e => {
          console.error('Error when removing label', e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
        });
      }
      if (index != null && index > -1) {
        this.items.splice(index, 1);
      }
      this.resetLabelItem(false);
    },
    saveLabel() {
      if ( !this.labelItem.text) {
        return;
      }
      if (this.labelItem.project) {
        this.editLabel(this.labelItem);
      } else {
        this.labelItem.name = this.labelItem.text;
        this.addLabel(this.labelItem);
      }
    },
    resetLabelItem(displayActionMenu, item) {
      if (item) {
        this.labelItem = item;
      } else {
        this.labelItem  = {
          text: '',
        };
      }

      this.displayActionMenu = displayActionMenu;

      if (displayActionMenu) {
        setTimeout(() => {
          const el = this.$el.querySelector('.labelsActionMenu .v-text-field input');
          el.setSelectionRange(el.value.length, el.value.length);
          el.focus();
          this.$el.querySelector('.projectColorPicker').scrollIntoView({ behavior: 'smooth' });
        }, 100);
      }

      this.$refs.childLabel.resetActiveItem(item);
    }
  }
};
</script>