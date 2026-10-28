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
          v-for="(item, i) in comments"
          :key="i"
          class="pe-0 ps-0 TaskCommentItem">
          <task-comments
            :task="task"
            :comment="item"
            :comments="comments" />
        </div>
      </template>
      <div v-else class="taskCommentEmpty text-sub-title">
        {{ $t('comment.message.noComment') }}
      </div>
    </div>
    <div class="taskCommentsColumnComposer flex-shrink-0 pa-4">
      <div
        v-if="composerOpen"
        class="editorContent commentEditorContainer newCommentEditor">
        <task-comment-editor
          ref="commentEditor"
          :max-length="MESSAGE_MAX_LENGTH"
          :placeholder="$t('task.placeholder').replace('{0}', MESSAGE_MAX_LENGTH)"
          :task="task"
          :show-comment-editor="true"
          :id="editorId"
          class="subComment subCommentEditor"
          @addNewComment="addTaskComment" />
      </div>
      <div
        v-else
        :title="$t('comment.message.addYourComment')"
        class="taskCommentsColumnPlaceholder d-flex align-center text-sub-title px-3"
        role="button"
        tabindex="0"
        @click="openComposer"
        @keydown.enter="openComposer">
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
    editorId: 'commentContent-column',
    composerOpen: false,
    newComment: null,
  }),
  computed: {
    commentsCount() {
      return this.comments?.length || 0;
    },
  },
  created() {
    this.$root.$on('task-comment-created', this.closeComposer);
    this.$root.$on('showEditor', this.closeComposer);
    this.$root.$on('newCommentEditor', this.closeComposer);
  },
  beforeDestroy() {
    this.$root.$off('task-comment-created', this.closeComposer);
    this.$root.$off('showEditor', this.closeComposer);
    this.$root.$off('newCommentEditor', this.closeComposer);
  },
  methods: {
    /**
     * Opens the new comment editor at the bottom of the column, after closing
     * any reply editor open in the comments above, so that a single comment
     * editor is open at a time, as in the comments drawer.
     *
     * @returns {void}
     */
    openComposer() {
      this.$root.$emit('showEditor', null);
      this.$nextTick().then(() => this.composerOpen = true);
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
     * Posts the new comment and its attachments, the same way the comments
     * drawer does, then scrolls the comments list to the posted comment.
     *
     * @returns {void}
     */
    addTaskComment() {
      let comment = this.$refs.commentEditor.getMessage() || '';
      comment = comment.length && this.$taskDrawerApi.urlVerify(comment) || '';
      this.$taskDrawerApi.addTaskComments(this.task.id, comment)
        .then(comment => {
          this.newComment = comment;
          return this.$refs.commentEditor.saveAttachments(comment.comment.id);
        })
        .then(() => {
          this.comments.push(this.newComment);
          this.$root.$emit('update-task-comments', this.comments.length, this.task.id);
          this.$nextTick().then(() => this.scrollToBottom());
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
