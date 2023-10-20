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
    id="taskCommentDrawer"
    class="taskCommentDrawer"
    ref="taskCommentDrawer"
    go-back-button
    right
    @closed="closeDrawer">
    <template #title>
      {{ $t('label.comments') }}
    </template>
    <template #titleIcons>
      <v-btn
        :disabled="isEditorActive"
        :title="$t('comment.message.addYourComment')"
        icon
        small
        class="addCommentBtn transparent my-auto"
        @click="openEditorToBottom(commentId)">
        <v-icon size="18" class="primary--text">fa fa-comment-medical</v-icon>
      </v-btn>
    </template>
    <template #content>
      <v-flex id="commentDrawerContent" class="drawerContent flex-grow-1 overflow-auto border-box-sizing">
        <div
          v-if="comments?.length"
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
        <div v-else>
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
      test: false,
      MESSAGE_MAX_LENGTH: 1300,
      newComment: null,
      newCommentId: null
    };
  },
  computed: {
    isEditorActive() {
      return !this.comments?.length;
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
          this.$root.$emit('update-task-comments',this.comments?.length ,this.task.id);
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
      if (this.comments?.length) {
        this.$root.$emit('showNewCommentEditor');
      }
      window.setTimeout(() => {
        const drawerContentElement = document.querySelector('#taskCommentDrawer .drawerContent');
        drawerContentElement?.scrollTo?.({
          top: drawerContentElement.scrollHeight,
          behavior: 'smooth',
          block: 'start',
        });
      }, 200);
    },
  }
};
</script>
