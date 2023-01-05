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
    <v-combobox
      id="labelInput"
      ref="selectLabel"
      v-model="model"
      :filter="filter"
      :hide-no-data="!search"
      :items="items"
      :search-input.sync="search"
      :label="$t('label.tapLabel.name')"
      attach
      class="pt-0 inputTaskLabel"
      hide-selected
      multiple
      small-chips
      prepend-icon
      solo
      @click="openLabelsList()"
      @change="search = ''">
      <template #prepend>
        <i class="uiIconTag uiIconBlue"></i>
      </template>
      <template #no-data>
        <v-list-item>
          <span class="subheading">{{ $t('label.createLabel') }}</span>
          <v-chip
            label
            small>
            {{ search }}
          </v-chip>
        </v-list-item>
      </template>
      <template #selection="{ attrs, item, parent, selected }">
        <v-chip
          v-if="item === Object(item)"
          v-bind="attrs"
          :color="`${item.color} lighten-3`"
          :input-value="selected"
          class="pe-1 font-weight-bold"
          label
          :outlined="!item.color"
          small>
          <span class="pe-2">
            {{ item.text }}
          </span>
          <v-icon
            v-if="item.canEdit"
            x-small
            class="pe-0"
            @click="parent.selectItem(item);removeTaskFromLabel(item)">
            close
          </v-icon>
        </v-chip>
      </template>
      <template #item="{ item }">
        <v-list-item @click="addTaskToLabel(item)">
          <v-chip
            :color="`${item.color} lighten-3`"
            :outlined="!item.color"
            label
            small>
            {{ item.text }}
          </v-chip>
        </v-list-item>
      </template>
    </v-combobox>
  </div>
</template>

<script>
export default {
  props: {
    task: {
      type: Object,
      default: () => {
        return {};
      }
    },
  },
  data() {
    return {
      index: -1,
      items: [],
      nonce: 1,
      model: [],
      x: 0,
      search: null,
      y: 0,
      currentUserName: eXo.env.portal.userName,
    };
  },
  watch: {
    model(val, prev) {
      if (val.length === prev.length) {
        this.search = null;
        return;
      }
      this.model = val.map(v => {
        if (typeof v === 'string') {
          v = {
            text: v,
            name: v,
            canEdit: this.task.createdBy===this.currentUserName||this.task.status.project.canManage
          };
          this.items.push(v);
          this.nonce++;
          this.addTaskToLabel(v);
        }
        return v;
      });
    },

  },
  created() {
    $(document).on('mousedown', () => {
      if (this.$refs.selectLabel && this.$refs.selectLabel.isMenuActive) {
        window.setTimeout(() => {
          this.$refs.selectLabel.isMenuActive = false;
        }, 200);
      }
    });
    document.addEventListener('closeLabelsList',()=> {
      setTimeout(() => {
        if (typeof this.$refs.selectLabel !== 'undefined') {
          this.$refs.selectLabel.isMenuActive = false;
        }
      }, 100);
    });
    document.addEventListener('loadProjectLabels', event => {
      if (event && event.detail) {
        const task = event.detail;
        this.model = [];
        if (task.status.project!=null) {
          this.getProjectLabels(task.status.project.id);
        }
      }
    });
    document.addEventListener('loadTaskLabels', event => {
      if (event && event.detail) {
        const task = event.detail;
        this.model = [];
        if (task.id!=null) {
          this.getTaskLabels();
          this.$taskDrawerApi.getTaskLabels(task.id).then((labels) => {
            this.model = labels.map(function (el) {
              const o = Object.assign({}, el);
              o.text = o.name;
              return o;
            });
          });
        }
      }
    });
  },
  methods: {
    filter(item, queryText, itemText) {
      if (item.header) {
        return false;
      }
      const hasValue = function (val) {
        return val != null ? val : '';
      };
      const text = hasValue(itemText);
      const query = hasValue(queryText);
      return text.toString().toLowerCase().indexOf(query.toString().toLowerCase()) > -1;
    },
    getMyAllLabels() {
      this.$taskDrawerApi.getMyAllLabels().then((labels) => {
        this.items = labels.map(function (el) {
          const o = Object.assign({}, el);
          o.text = o.name;
          return o;
        });
      });
    },
    getProjectLabels(projectId) {
      this.$taskDrawerApi.getProjectLabels(projectId).then((labels) => {
        this.items = labels.map(function (el) {
          const o = Object.assign({}, el);
          o.text = o.name;
          return o;
        });
      });
    },
    getTaskLabels() {
      this.$taskDrawerApi.getTaskLabels(this.task.id).then((labels) => {
        this.model = labels.map(function (el) {
          const o = Object.assign({}, el);
          o.text = o.name;
          return o;
        });
      });
    },
    addTaskToLabel(label) {
      if ( this.task.id!= null ) {
        this.$taskDrawerApi.addTaskToLabel(this.task.id, label).then( () => {
          this.$root.$emit('show-alert', {
            type: 'success',
            message: this.$t('alert.success.task.label')
          });
          this.getTaskLabels();
        }).then( () => {
          this.$root.$emit('update-task-labels',label,this.task.id);
        }).catch(e => {
          console.error('Error when updating task\'s labels', e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
        });
      } else {
        document.dispatchEvent(new CustomEvent('labelListChanged', {detail: label}));
      }
      this.model.push(label);
      document.getElementById('labelInput').focus();
    },
    removeTaskFromLabel(item) {
      this.$taskDrawerApi.removeTaskFromLabel(this.task.id, item.id).then( () => {
        this.$root.$emit('show-alert', {
          type: 'success',
          message: this.$t('alert.success.task.label')
        });
      }).then( () => {
        this.$root.$emit('update-remove-task-labels',item.id,this.task.id);
      }).catch(e => {
        console.error('Error when updating task\'s labels', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('alert.error')
        });
      });
    },
    openLabelsList() {
      this.$emit('labelsListOpened');
    }
  }
};
</script>