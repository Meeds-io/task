<template>
  <div class="taskComments">
    <div v-if="comments && comments.length" class="taskCommentNumber d-flex pb-3">
      <span class="ViewAllCommentLabel" @click="$root.$emit('displayTaskComment')">{{ $t('comment.message.viewAllComment') }} ({{ comments.length }})</span>
      <v-btn
        :title="$t('comment.message.addYourComment')"
        icon
        x-small
        class="addCommentBtn ms-3 transparent my-auto"
        @click="openCommentDrawer">
        <v-icon size="18" class="primary--text">fa fa-comment-medical</v-icon>
      </v-btn>
    </div>
    <div v-else class="taskCommentEmpty align-center pt-6 pb-3">
      <v-icon size="34" class="mb-4 grey-text">fa fa-comments</v-icon>
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
    }
  },
  methods: {
    openCommentDrawer() {
      this.$root.$emit('displayTaskComment', this.comments[this.comments.length - 1].comment.id, true);
    },
  }
};
</script>