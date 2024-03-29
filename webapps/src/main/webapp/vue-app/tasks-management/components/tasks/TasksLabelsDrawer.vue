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
  <div
    ref="filterLabelsTasksDrawer"
    class="filterLabelsTasksDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
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
        <template #selection="{ attrs, item, parent, selected }">
          <v-chip
            v-if="item === Object(item)"
            v-bind="attrs"
            :color="`${item.color} lighten-3`"
            :input-value="selected"
            class="pe-1"
            label
            small>
            <span class="pe-2">
              {{ item.text }}
            </span>
            <v-icon
              x-small
              class="pe-0"
              @click="parent.selectItem(item)">
              close
            </v-icon>
          </v-chip>
        </template>
      </v-combobox>
    </div>
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
    projectId: {
      type: Number,
      default: 0
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
    };
  },
  watch: {
    model(val, prev) {
      if (val&&prev){
        if (val.length === prev.length) {
          this.search = null;
          return;
        }
        this.model = val.map(v => {
          if (typeof v === 'string') {
            v = {
              text: v,
              name: v,
            };
            this.items.push(v);
            this.nonce++;
          }
          return v;
        });
      }

      this.$root.$emit('filter-task-labels',this.model);
    },

  },
  created() {
    const urlPath = document.location.pathname;
    if (urlPath.includes('projectDetail')){
      let projectId = urlPath.split('projectDetail/')[1].split(/[^0-9]/)[0];
      projectId = projectId && Number(projectId) || 0;
      this.getProjectLabels(projectId);
      this.projectId=projectId;
    } else {
      this.getMyAllLabels();
    }
    document.addEventListener('closeLabelsList',()=> {
      setTimeout(() => {
        if (typeof this.$refs.selectLabel !== 'undefined') {
          this.$refs.selectLabel.isMenuActive = false;
        }
      }, 100);
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
    document.addEventListener('loadFilterProjectLabels', event => {
      if (event && event.detail) {
        this.projectId=event.detail;
        this.getProjectLabels(this.projectId);
      }
    });
    this.$root.$on('reset-filter-task-group-sort',() =>{
      this.model = null;
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
    getTaskLabels() {
      this.$taskDrawerApi.getTaskLabels(this.task.id).then((labels) => {
        this.model = labels.map(function (el) {
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
    openLabelsList() {
      this.$emit('labelsListOpened');
    }
  }
};
</script>
