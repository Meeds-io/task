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
  <v-app id="projectBoardToolbar">
    <v-toolbar
      id="ProjectListToolbar"
      :class="showMobileTaskFilter && 'toolbarLarge'"
      flat
      class="tasksToolbar pb-3 transparent">
      <v-toolbar-title v-if="enableCreateButton">
        <v-btn
          class="btn px-2 btn-primary addNewProjectButton"
          @click="openDrawer">
          <!--<i class="uiIcon uiIconPlus"></i>-->
          <v-icon dark class="d-block d-sm-none">mdi-plus</v-icon>
          <span class="d-none font-weight-regular d-sm-flex align-center">
            <i class="uiIcon uiIconPlus"></i>
            <span class="addProject">{{ $t('label.addProject') }}</span> 
          </span>
        </v-btn>
      </v-toolbar-title>
      <v-scale-transition>
        <v-icon
          size="20"
          class="taskFilterMobile"
          :class="showMobileTaskFilter && 'tasksFilterMobile'"
          @click="showMobileTaskFilter = !showMobileTaskFilter">
          fas fa-filter
        </v-icon>
      </v-scale-transition>
      <v-spacer />
      <v-scale-transition>
        <v-text-field
          v-model="keyword"
          :placeholder="$t('label.filterProject')"
          prepend-inner-icon="fa-filter"
          :append-outer-icon="showMobileTaskFilter && 'mdi-close'"
          class="inputTasksFilter inputProjectFilter pa-0 ms-3 me-3 my-auto"
          :class="showMobileTaskFilter && 'inputTasksFilterMobile'"
          @click:append-outer="clearMessage"
          :clearable="!showMobileTaskFilter" />
      </v-scale-transition>
      <v-scale-transition v-if="!spaceName">
        <select
          id="filterTaskSelect"
          v-model="projectFilterSelected"
          name="projectFilter"
          class="selectTasksFilter my-auto me-2 subtitle-1 ignore-vuetify-classes d-sm-inline"
          @change="changeProjectFilter">
          <option
            v-for="item in projectFilter"
            :key="item.name"
            :value="item.name">
            {{ $t('label.project.filter.'+item.name.toLowerCase()) }}
          </option>
        </select>
      </v-scale-transition>
      <v-scale-transition>
        <select id="widthTmpSelectTaskFilter">
          <option id="widthTmpSelectOption"></option>
        </select>
      </v-scale-transition>
    </v-toolbar>
  </v-app>
</template>
<script>
export default {
  props: {
    keyword: {
      type: String,
      default: null,
    },
    spaceName: {
      type: String,
      default: '',
    },
    projectFilterSelected: {
      type: String,
      default: 'ALL',
    },
  },
  data () {
    return {
      projectFilter: [
        {name: 'ALL'},{name: 'MANAGED'},{name: 'COLLABORATED'},{name: 'WITH_TASKS'}
      ],
      showMobileTaskFilter: false,
      currentSpace: false,
    };
  },
  watch: {
    keyword() {
      this.$emit('keyword-changed', this.keyword);
    },
    projectFilterSelected() {
      this.$emit('filter-changed', this.projectFilterSelected);
    }
  },
  created() {
    this.retrieveCurrentSpace();
  },
  computed: {
    enableCreateButton() {
      return eXo.env.portal.spaceId ? this.currentSpace && this.currentSpace.canEdit : true;
    }
  },
  mounted() {
    $('#widthTmpSelectOption').html($('#filterTaskSelect option:selected').text());
    const widthElem = $('#widthTmpSelectTaskFilter').width() + 40;
    $('#filterTaskSelect').width(widthElem);
  },
  methods: {
    retrieveCurrentSpace() {
      if (eXo.env.portal.spaceId) {
        return this.$spaceService.getSpaceById(eXo.env.portal.spaceId).then(space => {
          this.currentSpace = space;
        });
      }
    },
    openDrawer() {
      this.$root.$emit('open-project-drawer', {});
    },
    clearMessage() {
      this.keyword = '';
      this.showMobileTaskFilter = !this.showMobileTaskFilter;
    },
    changeProjectFilter() {
      $('#widthTmpSelectOption').html($('#filterTaskSelect option:selected').text());
      const widthElem = $('#widthTmpSelectTaskFilter').width() + 40;
      $('#filterTaskSelect').width(widthElem);   
    }
  }
};
</script>
