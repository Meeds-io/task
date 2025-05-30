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
  <v-hover v-slot="{ hover }">
    <v-card
      flat
      class="pa-0"
      @click="openTaskDrawer">
      <v-list class="pa-0" :class="hover && 'light-grey-background-color no-border-radius' || ''">
        <v-list-item>
          <v-list-item-icon class="me-2">
            <v-icon size="32" class="icon-default-color mt-1 pt-2px">fas fa-tasks</v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <v-list-item-title class="d-flex flex-row full-width align-center">
              <p
                :title="taskTitleText"
                class="flex-grow-1 title pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate"
                v-sanitized-html="taskTitle"></p>
            </v-list-item-title>

            <v-list-item-subtitle class="d-flex flex-column">
              <span class="d-flex flex-row align-center mx-auto full-width">
                <span class="d-flex flex-row align-center" v-if="projectName">
                  <v-icon size="18" class="icon-default-color me-1">fas fa-clipboard</v-icon>
                  {{ projectName }}
                  <v-icon
                    size="3"
                    class="icon-default-color mx-3">fas fa-circle</v-icon>
                </span>
                <span class="d-flex flex-row align-center" v-if="taskAssign && !isMobile">
                  <exo-user-avatar
                    :profile-id="taskAssign"
                    :size="18"
                    :popover="false"
                    small-font-size />
                  <v-icon
                      size="3"
                      class="icon-default-color mx-3">fas fa-circle</v-icon>
                </span>
                <span class="d-flex flex-row align-center" v-if="taskDueDate && !isMobile">
                  <v-icon
                    size="18"
                    class="icon-default-color">fas fa-calendar</v-icon>
                  <date-format class="ms-1 my-auto" :value="taskDueDate" />
                  <v-icon
                      size="3"
                      class="icon-default-color mx-3">fas fa-circle</v-icon>
                </span>
                <span class="d-flex flex-row align-center" v-if="!isMobile">
                  <v-icon :class="taskPriorityClass" size="18">mdi-flag-variant</v-icon>
                  {{ taskPriorityLabel }}
                </span>
              </span>
              <div
                class="pt-2 text-wrap text-body text-break"
                :title="excerptText"
                :class="isMobile && 'text-truncate-2' || 'text-truncate-3'"
                v-sanitized-html="excerptHtml"></div>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <div v-if="taskDrawer">
        <task-drawer ref="taskDrawer" />
      </div>
    </v-card>
  </v-hover>
</template>

<script>
export default {
  props: {
    term: {
      type: String,
      default: null,
    },
    result: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    taskDrawer: false
  }),
  computed: {
    projectName() {
      return this.result && this.result.status && this.result.status.project && this.result.status.project.name || '';
    },
    projectLink() {
      return this.result && this.result.status && this.result.status.project && `${eXo.env.portal.context}/${eXo.env.portal.portalName}/tasks/projectDetail/${this.result.status.project.id}` || '';
    },
    excerptHtml() {
      return this.result && this.result.descriptionExcerpt || this.result.description || '';
    },
    excerptText() {
      return $('<div />').html(this.excerptHtml).text();
    },
    taskDueDate() {
      return this.result?.dueDate?.time;
    },
    taskTitle() {
      return this.result?.titleExcerpt || '';
    },
    taskTitleText() {
      return $('<div />').html(this.taskTitle).text();
    },
    taskAssign() {
      return this.result?.assignee;
    },
    taskPriorityClass() {
      return this.result?.priority.toLowerCase().concat('PriorityColor') || 'highPriorityColor';
    },
    taskPriorityLabel() {
      return this.$t(`label.priority.${this.result?.priority.toLowerCase()}`);
    },
    isMobile() {
      return this.$vuetify?.breakpoint?.smAndDown;
    }
  },
  methods: {
    async openTaskDrawer() {
      this.taskDrawer = true;
      await this.$nextTick();
      this.$refs.taskDrawer.open(this.result);
    },
  }
};
</script>
