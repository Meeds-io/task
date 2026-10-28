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
  <div class="taskCommentsColumn d-flex flex-column">
    <div class="taskCommentsColumnTitle text-subtitle-1 flex-shrink-0 px-4 pt-4 pb-2">
      {{ $t('label.comments') }} ({{ commentsCount }})
    </div>
    <div
      ref="commentsList"
      class="taskCommentsColumnList flex-grow-1 overflow-y-auto px-4">
      <template v-if="commentsCount">
        <div
          v-for="item in comments"
          :key="item.comment.id"
          class="pe-0 ps-0 TaskCommentItem">
          <task-comments
            :task="task"
            :comment="item"
            :comments="comments"
            @confirmDialogOpened="$emit('confirmDialogOpened')"
            @confirmDialogClosed="$emit('confirmDialogClosed')" />
        </div>
      </template>
      <div v-else class="taskCommentEmpty text-sub-title">
        {{ $t('comment.message.noComment') }}
      </div>
    </div>
    <div class="taskCommentsColumnComposer flex-shrink-0 pa-4">
      <div
        v-show="composerOpen"
        class="editorContent commentEditorContainer newCommentEditor">
        <task-comment-editor
          ref="commentEditor"
          :max-length="MESSAGE_MAX_LENGTH"
          :placeholder="$t('task.placeholder').replace('{0}', MESSAGE_MAX_LENGTH)"
          :task="task"
          :id="`commentContent-${editorKey}`"
          class="subComment subCommentEditor"
          @addNewComment="addTaskComment" />
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
  </div>
</template>
<script>
export default {
  props: {
    task: {
      type: Object,
      default: null,
    },
    comments: {
      type: Array,
      default: () => [],
    },
  },
  data: () => ({
    MESSAGE_MAX_LENGTH: 1300,
    editorKey: 'column',
    composerOpen: false,
  }),
  computed: {
    commentsCount() {
      return this.comments?.length || 0;
    },
  },
  created() {
    this.$root.$on('task-comment-created', this.closeComposer);
    this.$root.$on('showEditor', this.onShowEditor);
    this.$root.$on('newCommentEditor', this.closeComposer);
  },
  beforeDestroy() {
    this.$root.$off('task-comment-created', this.closeComposer);
    this.$root.$off('showEditor', this.onShowEditor);
    this.$root.$off('newCommentEditor', this.closeComposer);
  },
  methods: {
    /**
     * Opens the new comment editor at the bottom of the column through the
     * comment editors' own event, which also closes any reply editor open in
     * the comments above, so that a single comment editor is open at a time.
     *
     * @returns {void}
     */
    openComposer() {
      this.$root.$emit('showEditor', this.editorKey);
    },
    /**
     * Shows the new comment editor when the opened editor is this column's
     * one, and the placeholder when a reply editor opens instead.
     *
     * @param {string} commentId the key of the comment editor that opens
     * @returns {void}
     */
    onShowEditor(commentId) {
      this.composerOpen = commentId === this.editorKey;
    },
    /**
     * Closes the new comment editor, when a comment was posted or when another
     * comment editor opens.
     *
     * @returns {void}
     */
    closeComposer() {
      this.composerOpen = false;
    },
    /**
     * Posts the new comment, shows it at the end of the comments list, then
     * saves its attachments, in the same order as a comment posted from a
     * comment thread.
     *
     * @returns {void}
     */
    addTaskComment() {
      let comment = this.$refs.commentEditor.getMessage() || '';
      comment = comment.length && this.$taskDrawerApi.urlVerify(comment) || '';
      this.$taskDrawerApi.addTaskComments(this.task.id, comment)
        .then(comment => {
          this.comments.push(comment);
          this.$root.$emit('update-task-comments', this.comments.length, this.task.id);
          this.$nextTick().then(() => this.scrollToBottom());
          return this.$refs.commentEditor.saveAttachments(comment.comment.id);
        })
        .finally(() => {
          this.$root.$emit('task-comment-created');
        });
    },
    /**
     * Scrolls the comments list down to its last comment.
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
