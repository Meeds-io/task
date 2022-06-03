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
    :id="id"
    class="commentItem">
    <div class="commentHeader d-flex">
      <exo-user-avatar 
        :profile-id="comment.author.username"
        :key="comment.comment.id"
        :size="30"
        :extra-class="'position-relative overflow-hidden'"
        bold-title
        link-style
        popover />
      <div class="commentContent ps-3 d-flex align-center flex-grow-0 flex-shrink-0">
        <span :title="displayCommentDate(comment.comment.createdTime.time)" class="dateTime caption font-italic d-block text-sub-title">{{ relativeTime }}</span>
      </div>
    </div>
    <div class="commentBody ms-10 mt-1">
      <div
        class="taskContentComment"
        v-html="comment.formattedComment"></div>
      <v-btn
        id="reply_btn"
        depressed
        text
        small
        color="primary"
        @click="openCommentDrawer">
        {{ $t('comment.message.Reply') }}
      </v-btn>
    </div>
    <div v-if="comment.subComments && comment.subComments.length" class="py-0 TaskSubComments">
      <div
        v-for="(item, i) in comment.subComments"
        :key="i"
        class="TaskSubCommentItem pe-0 pb-2">
        <task-comment-item 
          :comment="item"
          :comments="comment.subComments"
          :avatar-size="30"
          :reply-last-comment="true"
          :last-comment-id="comment.comment.id" /> 
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    task: {
      type: Boolean,
      default: false
    },
    comment: {
      type: Object,
      default: () => {
        return {};
      }
    },
  },
  data () {
    return {
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
    relativeTime() {
      return this.getRelativeTime(this.comment.comment.createdTime.time);
    },
    id() {
      return `comment-${this.comment.comment.id}`;
    },
  },
  methods: {
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
    displayCommentDate( dateTimeValue ) {
      return dateTimeValue && this.$dateUtil.formatDateObjectToDisplay(new Date(dateTimeValue), this.dateTimeFormat, this.lang) || '';
    },
    openCommentDrawer() {
      this.$root.$emit('displayTaskComment', this.comment.comment.id );
    }
  }

};
</script>