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
  <div
    class="commentItem"
    :id="`commentItem-${comment.comment.id}`">
    <div
      class="commentWrapper"
      @mouseover="hover = true"
      @mouseleave="hover = false">
      <div class="commentHeader d-flex">
        <exo-user-avatar 
          :profile-id="comment.author.username"
          :extra-class="'position-relative overflow-hidden'"
          :size="30"
          bold-title
          link-style
          popover />
        <div class="commentContent ps-3 d-flex align-center flex-grow-0 flex-shrink-0">
          <span :title="displayCommentDate(commentDate)" class="dateTime caption font-italic d-block">{{ displayedTime }}</span>
        </div>
        <div class="removeCommentBtn position-relative z-index-modal">
          <v-menu
            v-model="displayActionMenu"
            :left="!$vuetify.rtl"
            :right="$vuetify.rtl"
            transition="slide-y-transition"
            content-class="commentActionMenu z-index-modal"
            offset-y
            attach>
            <template #activator="{ on, attrs }">
              <v-btn
                v-show="showCommentActions || displayActionMenu"
                :title="$t('comment.label.actions')"
                :size="32"
                class="commentActions position-absolute white"
                icon
                small
                v-bind="attrs"
                v-on="on">
                <v-icon class="icon-default-color" size="16">fa-ellipsis-v</v-icon>
              </v-btn>
            </template>
            <v-list class="pa-0 white" dense>
              <v-list-item class="editCommentButton" @click="editComment">
                <v-list-item-title class="subtitle-2">
                  <i class="uiIcon uiIconEdit icon-default-color pe-1"></i>
                  <span>{{ $t('label.edit') }}</span>
                </v-list-item-title>
              </v-list-item>
              <v-list-item class="deleteComment" @click="$emit('openConfirmDeleteDialog', comment.comment.id)">
                <v-list-item-title class="subtitle-2">
                  <i class="uiIcon uiIconTrash icon-default-color pe-1"></i>
                  <span>{{ $t('popup.delete') }}</span>
                </v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </div>
      </div>
      <div v-if="editing" class="editorContent commentEditorContainer">
        <task-comment-editor
          ref="commentEditionEditor"
          :value="commentText"
          :max-length="MESSAGE_MAX_LENGTH"
          :placeholder="$t('task.placeholder').replace('{0}', MESSAGE_MAX_LENGTH)"
          :task="task"
          :id="`commentEditContent-${comment.comment.id}`"
          :comment-id="String(comment.comment.id)"
          class="subComment subCommentEditor"
          edit-mode
          @updateComment="updateComment"
          @cancelEdit="cancelEdit" />
      </div>
      <div v-else class="commentBody d-block overflow-hidden ms-10 mt-1">
        <dynamic-html-element
          v-if="bodyElement"
          :html="bodyElement"
          class="taskContentComment reset-style-box rich-editor-content"
          dir="auto" />
        <attachments-image-items
          v-if="comment.comment.id"
          :object-id="comment.comment.id"
          :preview-width="250"
          :preview-height="250"
          object-type="taskComment" />
        <v-btn
          id="reply_btn"
          depressed
          text
          small
          color="primary"
          @click="replyComment">
          {{ $t('comment.message.Reply') }}
        </v-btn>
      </div>
    </div>
    <div v-if="comment && comment.subComments" class="py-0 TaskSubComments">
      <div
        v-for="(item, i) in comment.subComments"
        :key="i"
        class="TaskSubCommentItem pe-0 pb-2">
        <task-comment-item
          :comment="item"
          :comments="comment.subComments"
          :task="task"
          :avatar-size="30"
          @openCommentEditor="$emit('openCommentEditor',comment.comment.id)"
          @openConfirmDeleteDialog="$emit('openConfirmDeleteDialog',item.comment.id,true)"
          @commentUpdated="$emit('commentUpdated', $event)" />
      </div>
    </div>
  </div>
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
    replyLastComment: {
      type: Boolean,
      default: false
    }
    ,
    showOnly: {
      type: Boolean,
      default: true
    },
    lastCommentId: {
      type: String,
      default: ''
    },
    comments: {
      type: Object,
      default: () => {
        return {};
      }
    },
    task: {
      type: Object,
      default: () => null
    },
  },
  data() {
    return {
      hover: false,
      editing: false,
      displayActionMenu: false,
      MESSAGE_MAX_LENGTH: 1300,
      lang: eXo.env.portal.language,
      dateTimeFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      },
    };
  },
  computed: {
    showCommentActions() {
      return this.hover && !this.editing && !this.replyLastComment
        && eXo.env.portal.userName === this.comment.author.username;
    },
    updatedTime() {
      return this.comment.comment.updatedTime?.time || null;
    },
    commentDate() {
      return this.updatedTime || this.comment.comment.createdTime.time;
    },
    displayedTime() {
      const relativeTime = this.getRelativeTime(this.commentDate);
      return this.updatedTime && this.$t('comment.label.updatedTime').replace('{0}', relativeTime) || relativeTime;
    },
    bodyElement() {
      return this.comment?.formattedComment || '';
    },
    commentText() {
      return this.comment?.comment?.comment || '';
    },
  },
  mounted() {
    document.addEventListener('Task-comments-drawer-closed', this.cancelEdit);
    this.$root.$on('commentEditionOpened', this.closeConcurrentEdition);
    // opening a reply or a new comment editor closes the comment edition
    this.$root.$on('showEditor', this.cancelEdit);
    this.$root.$on('newCommentEditor', this.cancelEdit);
  },
  beforeDestroy() {
    document.removeEventListener('Task-comments-drawer-closed', this.cancelEdit);
    this.$root.$off('commentEditionOpened', this.closeConcurrentEdition);
    this.$root.$off('showEditor', this.cancelEdit);
    this.$root.$off('newCommentEditor', this.cancelEdit);
  },
  methods: {
    displayCommentDate( dateTimeValue ) {
      return dateTimeValue && this.$dateUtil.formatDateObjectToDisplay(new Date(dateTimeValue), this.dateTimeFormat, this.lang) || '';
    },
    getRelativeTime(previous) {
      const msPerMinute = 60 * 1000;
      const msPerHour = msPerMinute * 60;
      const msPerDay = msPerHour * 24;
      const msPerMaxDays = msPerDay * 2;
      const elapsed = new Date().getTime() - previous;

      if (elapsed < msPerMinute) {
        return this.$t('task.timeConvert.Less_Than_A_Minute');
      } else if (elapsed === msPerMinute) {
        return this.$t('task.timeConvert.About_A_Minute');
      } else if (elapsed < msPerHour) {
        return this.$t('task.timeConvert.About_?_Minutes').replace('{0}', Math.round(elapsed / msPerMinute));
      } else if (elapsed === msPerHour) {
        return this.$t('task.timeConvert.About_An_Hour');
      } else if (elapsed < msPerDay) {
        return this.$t('task.timeConvert.About_?_Hours').replace('{0}', Math.round(elapsed / msPerHour));
      } else if (elapsed === msPerDay) {
        return this.$t('task.timeConvert.About_A_Day');
      } else if (elapsed < msPerMaxDays) {
        return this.$t('task.timeConvert.About_?_Days').replace('{0}', Math.round(elapsed / msPerDay));
      } else {
        return this.displayCommentDate(previous);
      }
    },
    editComment() {
      this.displayActionMenu = false;
      // a single editor at a time: close the other editions, then the reply
      // editors (no id matches 'commentContent-null', so all of them close)
      this.$root.$emit('commentEditionOpened', this.comment.comment.id);
      this.$root.$emit('showEditor', null);
      this.editing = true;
    },
    closeConcurrentEdition(commentId) {
      if (this.editing && commentId !== this.comment.comment.id) {
        this.cancelEdit();
      }
    },
    cancelEdit() {
      this.editing = false;
    },
    updateComment() {
      const editor = this.$refs.commentEditionEditor;
      let commentText = editor.getMessage() || '';
      commentText = commentText.length && this.urlVerify(commentText) || '';
      this.$taskDrawerApi.updateTaskComment(this.comment.comment.id, commentText)
        .then(updatedComment => {
          this.$set(this.comment, 'formattedComment', updatedComment.formattedComment);
          this.$set(this.comment.comment, 'comment', updatedComment.comment.comment);
          this.$set(this.comment.comment, 'updatedTime', updatedComment.comment.updatedTime);
          return editor.saveAttachments(this.comment.comment.id);
        })
        .then(() => {
          this.editing = false;
          this.$emit('commentUpdated', this.comment);
        })
        .catch(() => {
          // keep the editor open so that the edited text is not lost
          this.editing = true;
        });
    },
    urlVerify(text) {
      return this.$taskDrawerApi.urlVerify(text);
    },
    replyComment()
    {
      if (this.replyLastComment)
      {
        this.$root.$emit('displayTaskComment', this.lastCommentId);
      }
      else {
        this.$emit('openCommentEditor',this.comment.comment.id);
      }
    }
  },
};
</script>

