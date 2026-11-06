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
      class="transparent pa-0">
      <v-list class="pa-0" :class="hover && 'light-grey-background-color no-border-radius' || ''">
        <v-list-item>
          <v-list-item-icon class="me-2">
            <v-icon size="32" class="icon-default-color mt-1 pt-2px">fas fa-tasks</v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <v-list-item-title class="d-flex flex-row full-width align-center">
              <h1 class="flex-grow-1 title primary--text pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate">
                <a
                  :href="taskUrl"
                  class="stretched-link"
                  v-sanitized-html="taskTitle"
                  @click.stop.prevent="openTaskDrawer"></a>
              </h1>
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
                <span class="stretched-link-above d-flex flex-row align-center" v-if="taskAssign && !isMobile">
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
                class="search-excerpt pt-2 text-wrap text-body-2 text-color text-break text-truncate-2"
                v-sanitized-html="excerptHtml"></div>
              <div
                v-if="fieldMatchItems.length"
                class="search-field-matches d-flex flex-row flex-nowrap align-center overflow-hidden pt-1 text-body-2 text-color">
                <v-icon
                  :title="fieldMatches.tooltip"
                  size="14"
                  class="icon-default-color flex-shrink-0 me-2">
                  {{ fieldMatches.icon }}
                </v-icon>
                <span
                  v-for="(item, index) in fieldMatchItems"
                  :key="index"
                  class="search-field-match text-truncate">
                  <span v-if="index" class="mx-1">·</span>{{ item.label }}: <span v-sanitized-html="item.valueHtml"></span>
                </span>
                <v-chip
                  v-if="fieldMatches.more > 0"
                  class="search-field-more flex-shrink-0 ms-2"
                  x-small
                  outlined>
                  {{ fieldMatches.moreLabel }}
                </v-chip>
              </div>
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
    fieldMatches() {
      return this.result?.fieldMatches || null;
    },
    fieldMatchItems() {
      return this.fieldMatches?.items || [];
    },
    excerptHtml() {
      return this.joinExcerptLines(this.result?.descriptionExcerpt || this.result?.description || '');
    },
    taskDueDate() {
      return this.result?.dueDate?.time;
    },
    taskTitle() {
      return this.result?.title || '';
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
    },
    taskUrl() {
      return `/${eXo.env.portal.containerName}/${eXo.env.portal.metaPortalName}/tasks/taskDetail/${this.result.id}`;
    }
  },
  methods: {
    /**
     * Joins the lines and paragraphs of an excerpt with « · », so that the
     * excerpt reads as one run of fields within its two lines. A blank line,
     * written as a space entity, adds no field.
     *
     * @param {string} html the excerpt, as HTML or text
     * @returns {string} the excerpt on one line
     */
    joinExcerptLines(html) {
      return (html || '')
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/<\/(p|div|li|h[1-6]|ul|ol|blockquote)>/gi, '\n')
        .replace(/<(p|div|li|h[1-6]|ul|ol|blockquote)(\s[^>]*)?>/gi, '')
        .split(/\s*\n+\s*/)
        .filter(line => line.replace(/&nbsp;/gi, '').trim().length)
        .join(' · ');
    },
    async openTaskDrawer() {
      this.taskDrawer = true;
      // The task skins are filtered portal skins, loaded only with the Tasks
      // portlets: the search page has none of them until the drawer asks.
      await Promise.all([
        this.$utils.importSkin('portal', 'tasksDrawer'),
        this.$utils.importSkin('portal', 'taskCommentsDrawer'),
        this.$utils.importSkin('portal', 'tasks'),
      ]).catch(() => null);
      await this.$nextTick();
      this.$refs.taskDrawer.open(this.result);
    },
  }
};
</script>
