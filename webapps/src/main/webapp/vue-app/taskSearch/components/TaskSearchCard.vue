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
  <v-card 
    class="d-flex flex-column border-radius box-shadow" 
    flat
    min-height="227">
    <a
      v-sanitized-html="taskTitle"
      :title="taskTitleText"
      class="px-3 pt-2 pb-1 text-left text-truncate"
      @click="openTaskDrawer">
    </a>
    <div class="mx-auto d-flex flex-grow-1 px-3 py-0">
      <div
        ref="excerptNode"
        :title="excerptText"
        class="text-wrap text-break caption flex-grow-1">
      </div>
    </div>
    <div v-if="projectName" class="mx-3">
      <v-chip
        :title="projectName"
        :color="projectColor"
        :href="projectLink"
        class="border-radius width-auto mb-3 mt-2 py-2"
        outlined>
        <div class="text-truncate">{{ projectName }}</div>
      </v-chip>
    </div>
    <v-list class="light-grey-background flex-grow-0 border-top-color no-border-radius pa-0">
      <v-list-item class="px-0 pt-1 pb-2" @click="openTaskDrawer">
        <v-list-item-icon class="mx-0 my-auto">
          <span class="uiIconCalTask tertiary--text ps-1 pe-2 display-1"></span>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title :title="taskDetails">
            {{ taskDetails }}
          </v-list-item-title>
          <v-list-item-subtitle>
            <template v-if="taskDueDate">
              {{ $t('label.dueDate') }}:
              <date-format :value="taskDueDate" class="d-inline" />
            </template>
            <template v-else>
              {{ $t('label.noDueDate') }}
            </template>
          </v-list-item-subtitle>
        </v-list-item-content>
      </v-list-item>
    </v-list>
  </v-card>
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
    lineHeight: 22,
  }),
  computed: {
    projectName() {
      return this.result && this.result.status && this.result.status.project && this.result.status.project.name || '';
    },
    projectColor() {
      return this.result && this.result.status && this.result.status.project && this.result.status.project.color || '';
    },
    projectLink() {
      return this.result && this.result.status && this.result.status.project && `${eXo.env.portal.context}/${eXo.env.portal.portalName}/tasks/projectDetail/${this.result.status.project.id}` || '';
    },
    maxEllipsisHeight() {
      return this.result && this.result.status && 68 || 120;
    },
    excerptHtml() {
      return this.result && this.result.descriptionExcerpt || this.result.description || '';
    },
    excerptText() {
      return $('<div />').html(this.excerptHtml).text();
    },
    taskDueDate() {
      return this.result && this.result.dueDate && this.result.dueDate.time;
    },
    taskTitle() {
      return this.result && this.result.titleExcerpt || '';
    },
    taskTitleText() {
      return $('<div />').html(this.taskTitle).text();
    },
    taskDetails() {
      if (!this.result) {
        return '';
      }
      const commentCount = this.result.commentCount || 0;
      return this.$t('Search.task.commentsCount', {0: commentCount});
    },
  },
  mounted() {
    this.computeEllipsis();
  },
  methods: {
    openTaskDrawer() {
      document.dispatchEvent(new CustomEvent('openTaskDetail', {'detail': this.result}));
    },
    computeEllipsis() {
      if (!this.excerptHtml || this.excerptHtml.length === 0) {
        return;
      }
      const stNode = this.$refs.excerptNode;
      if (!stNode) {
        return;
      }
      stNode.innerHTML = this.excerptHtml;

      let stNodeHeight = stNode.getBoundingClientRect().height || this.lineHeight;
      if (stNodeHeight > this.maxEllipsisHeight) {
        while (stNodeHeight > this.maxEllipsisHeight) {
          const newHtml = this.deleteLastChars(stNode.innerHTML.replace(/&[a-z]*;/, ''), 10);
          if (newHtml.length === stNode.innerHTML.length) {
            break;
          }
          stNode.innerHTML = newHtml;
          stNodeHeight = stNode.getBoundingClientRect().height || this.lineHeight;
        }

        stNode.innerHTML = this.deleteLastChars(stNode.innerHTML, 4);
        stNode.innerHTML = `${stNode.innerHTML}...`;
      }
    },
    deleteLastChars(html, charsToDelete) {
      if (html.slice(-1) === '>') {
        // Replace empty tags
        html = html.replace(/<[a-zA-Z 0-9 "'=]*><\/[a-zA-Z 0-9]*>$/g, '');
      }
      html = html.replace(/<br>(\.*)$/g, '');

      charsToDelete = charsToDelete || 1;

      let newHtml = '';
      if (html.slice(-1) === '>') {
        // Delete last inner html char
        html = html.replace(/(<br>)*$/g, '');
        newHtml = html.replace(new RegExp(`([^>]{${charsToDelete}})(</)([a-zA-Z 0-9]*)(>)$`), '$2$3');
        newHtml = $('<div />').html(newHtml).html().replace(/&[a-z]*;/, '');
        if (newHtml.length === html.length) {
          newHtml = html.replace(new RegExp('([^>]*)(</)([a-zA-Z 0-9]*)(>)$'), '$2$3');
        }
      } else {
        newHtml = html.substring(0, html.trimRight().length - charsToDelete);
      }
      return newHtml;
    },
  }
};
</script>
