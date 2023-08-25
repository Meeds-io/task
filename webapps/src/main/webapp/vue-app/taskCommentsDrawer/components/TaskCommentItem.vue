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
          <span :title="displayCommentDate(comment.comment.createdTime.time)" class="dateTime caption font-italic d-block">{{ relativeTime }}</span>
        </div>
        <div class="removeCommentBtn position-relative">
          <v-btn
            v-show="showDeleteButtom"
            :title="$t('label.remove')"
            :size="32"
            class="deleteComment position-absolute white"
            icon
            small
            @click="$emit('openConfirmDeleteDialog', comment.comment.id)"
            v-on="on">
            <v-icon class="icon-default-color" size="16">fa-trash</v-icon>
          </v-btn>
        </div>
      </div>
      <div class="commentBody d-block overflow-hidden ms-10 mt-1">
        <div
          class="taskContentComment"
          v-html="comment.formattedComment"></div>
         <attachments-image-items
            :object-id="comment.comment.id"
            object-type="taskComment"
            :preview-width="250"
            :preview-height="250" />
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
          :avatar-size="30"
          @openCommentEditor="$emit('openCommentEditor',comment.comment.id)"
          @openConfirmDeleteDialog="$emit('openConfirmDeleteDialog',item.comment.id,true)" />
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
  },
  data() {
    return {
      hover: false,
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
    showDeleteButtom() {
      return this.hover && !this.replyLastComment && eXo.env.portal.userName === this.comment.author.username;
    },
    relativeTime() {
      return this.getRelativeTime(this.comment.comment.createdTime.time);
    },
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
        return this.displayCommentDate(this.comment.comment.createdTime.time);
      }
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

