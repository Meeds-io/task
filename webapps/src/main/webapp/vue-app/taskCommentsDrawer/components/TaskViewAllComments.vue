<template>
  <div class="taskComments">
    <div v-if="comments && comments.length" class="taskCommentNumber d-flex pb-3">
      <span class="ViewAllCommentLabel" @click="$root.$emit('displayTaskComment')">{{ $t('comment.message.viewAllComment') }} ({{ displayedTotal }})</span>
      <div
        :title="$t('comment.message.addYourComment')"
        class="addCommentBtn ps-3"
        @click="openCommentDrawer">
        <i class="uiIcon uiIconTaskAddComment"></i>
      </div>
    </div>
    <div v-else class="taskCommentEmpty align-center pt-6 pb-3">
      <i class="uiIcon uiIconComment"></i>
      <span class="noCommentLabel">{{ $t('comment.message.noComment') }}</span>
      <span class="ViewAllCommentText" @click="$root.$emit('displayTaskComment')">{{ $t('comment.message.addYourComment') }}</span>
    </div>
    <div v-if="comments && comments.length" class="pe-0 ps-0 TaskCommentItem">
      <task-last-comment
        :task="task"
        :comment="comments[comments.length-1]" />
    </div>
  </div>
</template>

<script>
export default {
  props: {
    comments: {
      type: Array,
      default: () => {
        return [];
      }
    },
    task: {
      type: Object,
      default: null,
    },
    commentsTotal: {
      type: Number,
      default: 0
    }
  },
  computed: {
    displayedTotal() {
      // the comments are loaded by pages: the link counts all of them, not only those loaded
      return Math.max(this.commentsTotal, this.comments.length);
    }
  },
  methods: {
    openCommentDrawer() {
      this.$root.$emit('displayTaskComment', this.comments[this.comments.length - 1].comment.id, true);
    },
  }
};
</script>