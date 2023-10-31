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
  <v-app>
    <exo-drawer
      @closed="closeDrawer"
      id="taskCommentDrawer"
      v-model="drawer"
      class="taskCommentDrawer"
      ref="taskCommentDrawer"
      right>
      <template slot="title">
        <v-flex
          class="mx-0 drawerHeader flex-grow-0 width-full">
          <v-list-item class="px-0">
            <v-list-item-content class="drawerTitle d-flex text-header-title text-truncate">
              <i class="uiIcon uiArrowBAckIcon" @click="closeDrawer"></i>
              <span class="ps-2">{{ $t('label.comments') }}</span>
            </v-list-item-content>
            <v-list-item-action class="drawerIcons align-end d-flex flex-row">
              <v-btn
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
        <v-flex id="commentDrawerContent" class="drawerContent flex-grow-1 overflow-auto border-box-sizing">
          <div
            v-if="this.comments && this.comments.length"
            class="TaskCommentContent">
            <div
              v-for="(item, i) in comments"
              :key="i"
              class="pe-0 ps-0 TaskCommentItem">
              <task-comments
                :task="task"
                :comment="item"
                :comments="comments"
                :last-comment="commentId"
                :show-new-comment-editor="showNewCommentEditor"
                @confirmDialogOpened="$emit('confirmDialogOpened')"
                @confirmDialogClosed="$emit('confirmDialogClosed')" />
            </div>
          </div>
          <div v-else-if="drawer">
            <div class="editorContent commentEditorContainer newCommentEditor">
              <task-comment-editor
                ref="commentEditor"
                :max-length="MESSAGE_MAX_LENGTH"
                :placeholder="$t('task.placeholder').replace('{0}', MESSAGE_MAX_LENGTH)"
                :task="task"
                :show-comment-editor="true"
                :id="'commentContent-editor'"
                class="subComment subCommentEditor"
                @addNewComment="addTaskComment($event)" />
            </div>
          </div>
        </v-flex>
      </template>
    </exo-drawer>
  </v-app>
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
      MESSAGE_MAX_LENGTH: 1300,
      newComment: null,
      newCommentId: null
    };
  },
  computed: {
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
    addTaskComment() {
      let comment = this.$refs.commentEditor.getMessage() || '';
      comment = comment.length && this.urlVerify(comment) || '';
      this.$taskDrawerApi.addTaskComments(this.task.id,comment)
        .then(comment => {
          this.newComment = comment;
          return this.$refs.commentEditor.saveAttachments(comment.comment.id);
        })
        .then(() => {
          this.comments.push(this.newComment);
          this.$root.$emit('update-task-comments',this.comments.length,this.task.id);
        })
        .finally(() => {
          this.$root.$emit('task-comment-created');
        });
    },
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
    urlVerify(text) {
      return this.$taskDrawerApi.urlVerify(text);
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
