<!--
  This file is part of the Meeds project (https://meeds.io/).
  Copyright (C) 2026 Meeds Association
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
  <v-flex :class="column && 'taskCommentsColumn d-flex flex-column'">
    <div
      v-if="column"
      class="taskCommentsColumnTitle text-subtitle-1 flex-shrink-0 px-4 pt-4 pb-2">
      {{ $t('label.comments') }} ({{ commentsCount }})
    </div>
    <div
      ref="commentsList"
      :class="column && 'taskCommentsColumnList flex-grow-1 overflow-y-auto px-4'">
      <div
        v-if="commentsCount"
        class="TaskCommentContent">
        <div
          v-for="(item, i) in comments"
          :key="column ? item.comment.id : i"
          class="pe-0 ps-0 TaskCommentItem">
          <task-comments
            :task="task"
            :comment="item"
            :comments="comments"
            :last-comment="lastComment"
            :show-new-comment-editor="showNewCommentEditor"
            @add-comment="addTaskComment"
            @confirmDialogOpened="$emit('confirmDialogOpened')"
            @confirmDialogClosed="$emit('confirmDialogClosed')" />
        </div>
      </div>
      <div v-else-if="column" class="taskCommentEmpty text-sub-title">
        {{ $t('comment.message.noComment') }}
      </div>
      <div v-else-if="active">
        <div class="editorContent commentEditorContainer newCommentEditor">
          <task-comment-editor
            ref="commentEditor"
            :max-length="MESSAGE_MAX_LENGTH"
            :placeholder="$t('task.placeholder').replace('{0}', MESSAGE_MAX_LENGTH)"
            :task="task"
            :show-comment-editor="true"
            :id="'commentContent-editor'"
            class="subComment subCommentEditor"
            @addNewComment="addTaskComment($refs.commentEditor)" />
        </div>
      </div>
    </div>
    <div
      v-if="column"
      class="taskCommentsColumnComposer flex-shrink-0 pa-4">
      <div
        v-show="composerOpen"
        class="editorContent commentEditorContainer newCommentEditor">
        <task-comment-editor
          ref="commentEditor"
          :max-length="MESSAGE_MAX_LENGTH"
          :placeholder="$t('task.placeholder').replace('{0}', MESSAGE_MAX_LENGTH)"
          :task="task"
          :id="`commentContent-${columnEditorKey}`"
          class="subComment subCommentEditor"
          @addNewComment="addTaskComment($refs.commentEditor)" />
      </div>
      <div
        v-if="!composerOpen"
        :title="$t('comment.message.addYourComment')"
        class="taskCommentsColumnPlaceholder d-flex align-center text-sub-title px-3"
        role="button"
        tabindex="0"
        @click="openComposer"
        @keydown.enter="openComposer"
        @keydown.space.prevent="openComposer">
        {{ $t('comment.message.addYourComment') }}
      </div>
    </div>
  </v-flex>
</template>
<script>
export default {
  props: {
    task: {
      type: Object,
      default: null,
    },
    comments: {
      type: [Array, Object],
      default: () => [],
    },
    lastComment: {
      type: String,
      default: '',
    },
    showNewCommentEditor: {
      type: Boolean,
      default: false,
    },
    active: {
      type: Boolean,
      default: false,
    },
    column: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    MESSAGE_MAX_LENGTH: 1300,
    columnEditorKey: 'column',
    composerOpen: false,
  }),
  computed: {
    commentsCount() {
      return this.comments?.length || 0;
    },
  },
  created() {
    if (this.column) {
      this.$root.$on('task-comment-created', this.closeComposer);
      this.$root.$on('showEditor', this.onShowEditor);
      this.$root.$on('newCommentEditor', this.closeComposer);
    }
  },
  beforeDestroy() {
    this.$root.$off('task-comment-created', this.closeComposer);
    this.$root.$off('showEditor', this.onShowEditor);
    this.$root.$off('newCommentEditor', this.closeComposer);
  },
  methods: {
    /**
     * Opens the new comment editor pinned at the bottom of the column through
     * the comment editors' own event, which also closes any reply editor open
     * in the comments above, so that a single comment editor is open at a
     * time.
     *
     * @returns {void}
     */
    openComposer() {
      this.$root.$emit('showEditor', this.columnEditorKey);
    },
    /**
     * Shows the column's new comment editor when the opened editor is this
     * one, and its placeholder when a reply editor opens instead.
     *
     * @param {string} commentId the key of the comment editor that opens
     * @returns {void}
     */
    onShowEditor(commentId) {
      this.composerOpen = commentId === this.columnEditorKey;
    },
    /**
     * Closes the column's new comment editor, when a comment was posted or
     * when another comment editor opens.
     *
     * @returns {void}
     */
    closeComposer() {
      this.composerOpen = false;
    },
    /**
     * Posts a new comment of the task, whichever new comment editor it was
     * written in: the one under the last comment, the one of a task with no
     * comment yet, or the one at the bottom of the column. When that editor
     * stays shown, the comment shows at the end of the comments list at once
     * and its attachments are saved next. The first comment of a task in the
     * drawer removes the editor it was written in, so its attachments are
     * saved before it shows. The task's comments count is updated once the
     * attachments are saved.
     *
     * @param {object} commentEditor the task-comment-editor the comment was written in
     * @returns {void}
     */
    addTaskComment(commentEditor) {
      let comment = commentEditor.getMessage() || '';
      comment = comment.length && this.$taskDrawerApi.urlVerify(comment) || '';
      const editorStaysShown = this.column || this.commentsCount > 0;
      this.$taskDrawerApi.addTaskComments(this.task.id, comment)
        .then(comment => {
          if (editorStaysShown) {
            this.showComment(comment);
            return commentEditor.saveAttachments(comment.comment.id);
          } else {
            return commentEditor.saveAttachments(comment.comment.id)
              .then(() => this.showComment(comment));
          }
        })
        .then(() => {
          this.$root.$emit('update-task-comments', this.comments.length, this.task.id);
        })
        .finally(() => {
          this.$root.$emit('task-comment-created');
        });
    },
    /**
     * Adds a posted comment at the end of the comments list, and scrolls the
     * column's list down to it.
     *
     * @param {object} comment the posted comment
     * @returns {void}
     */
    showComment(comment) {
      this.comments.push(comment);
      if (this.column) {
        this.$nextTick().then(() => this.scrollToBottom());
      }
    },
    /**
     * Scrolls the column's comments list down to its last comment.
     *
     * @returns {void}
     */
    scrollToBottom() {
      const list = this.$refs.commentsList;
      if (list) {
        list.scrollTo({
          top: list.scrollHeight,
          behavior: 'smooth',
        });
      }
    },
  },
};
</script>
