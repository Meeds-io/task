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
  <exo-drawer
    @closed="closeDrawer"
    id="taskCommentDrawer"
    v-model="drawer"
    class="taskCommentDrawer"
    ref="taskCommentDrawer"
    go-back-button
    right>
    <template slot="title">
      <v-flex
        class="mx-0 drawerHeader flex-grow-0 width-full">
        <v-list-item class="px-0">
          <v-list-item-content class="drawerTitle d-flex">
            <span class="text-title">
              <span class="ps-2 text-truncate">{{ $t('label.comments') }}</span>
            </span>
          </v-list-item-content>
          <v-list-item-action class="drawerIcons align-end d-flex flex-row">
            <v-btn
              v-if="!readOnly"
              :disabled="isEditorActive"
              icon
              :title="$t('comment.message.addYourComment')"
              class="addCommentBtn float-right"
              @click="openEditorToBottom(commentId)">
              <i class="uiIcon uiIconTaskAddComment"></i>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-flex>
    </template>
    <template slot="content">
      <task-comments-panel
        id="commentDrawerContent"
        :task="task"
        :comments="comments"
        :last-comment="commentId"
        :show-new-comment-editor="showNewCommentEditor"
        :active="drawer"
        class="drawerContent flex-grow-1 overflow-auto border-box-sizing"
        @confirmDialogOpened="$emit('confirmDialogOpened')"
        @confirmDialogClosed="$emit('confirmDialogClosed')" />
    </template>
  </exo-drawer>
</template>
<script>
export default {
  props: {
    comment: {
      type: Object,
      default: () => {
        return {};
      }
    },
    comments: {
      type: Object,
      default: () => {
        return {};
      }
    },
    task: {
      type: Boolean,
      default: false
    },
    showTaskCommentDrawer: {
      type: Boolean,
      default: false
    },
  },
  data() {
    return {
      commentId: '',
      showNewCommentEditor: false,
      drawer: false,
      test: false,
    };
  },
  computed: {
    /**
     * Whether the task is shown to a reader who may view it but not edit it,
     * as the card read says: such a reader reads the comments and adds none.
     *
     * @returns {boolean} true for a read-only reader
     */
    readOnly() {
      return !!this.task && this.task.editable === false;
    },
    isEditorActive() {
      return !this.comments.length;
    }
  },
  mounted() {
    this.$root.$on('displayTaskComment', (commentId, isNewComment) => {
      this.openDrawer();
      this.commentId = commentId;
      this.showNewCommentEditor = isNewComment;
      if (isNewComment) {
        this.$nextTick().then(() => this.openEditorToBottom(commentId));
      }
    });
    this.$root.$on('hideTaskComment', () => {
      this.closeDrawer();
    });
    if (this.$root.autoReply) {
      window.setTimeout(() => {
        this.$root.$emit('displayTaskComment', this.$root.autoReplyCommentId || null, true);
      }, 300);
      this.$root.autoReply = false;
    }
  },
  methods: {
    closeDrawer() {
      if (this.$refs.taskCommentDrawer) {
        this.$refs.taskCommentDrawer.close();
      }
      document.dispatchEvent(new CustomEvent('Task-comments-drawer-closed'));
    },
    openDrawer() {
      if (this.$refs.taskCommentDrawer) {
        this.$refs.taskCommentDrawer.open();
      }
    },
    openEditorToBottom() {
      if (this.comments.length > 0) {
        this.$root.$emit('showNewCommentEditor');
      }
      window.setTimeout(() => {
        const drawerContentElement = document.querySelector('#taskCommentDrawer .drawerContent');
        drawerContentElement.scrollTo({
          top: drawerContentElement.scrollHeight,
          behavior: 'smooth',
          block: 'start',
        });
      }, 200);
    },
  }
};
</script>
