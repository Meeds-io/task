<template>
  <user-notification-template
    :notification="notification"
    :avatar-url="profileAvatarUrl"
    :message="message"
    :loading="loading"
    :url="taskUrl">
    <template #actions>
      <div class="text-truncate">
        <v-icon size="14" class="me-1">fa-tasks</v-icon>
        {{ taskName }}
      </div>
      <div class="text-truncate my-2">
        <v-icon size="14" class="me-1">fa-comments</v-icon>
        {{ commentText }}
      </div>
    </template>
  </user-notification-template>
</template>
<script>
export default {
  props: {
    notification: {
      type: Object,
      default: null,
    },
  },
  computed: {
    taskUrl() {
      return this.notification?.parameters?.taskUrl;
    },
    profileFullName() {
      return this.notification?.from?.fullname;
    },
    profileAvatarUrl() {
      return this.notification?.from?.avatar;
    },
    projectName() {
      return this.notification?.parameters?.projectName;
    },
    taskName() {
      return this.notification?.parameters?.taskName;
    },
    commentText() {
      return this.$utils.htmlToText(this.notification?.parameters?.commentText);
    },
    reactionEmoji() {
      return this.notification?.parameters?.reactionEmoji;
    },
    message() {
      const user = `<a class="user-name font-weight-bold">${this.profileFullName}</a>`;
      return this.projectName?.length && this.$t('Notification.message.TaskInProjectReactionPlugin', {
        0: user,
        1: `<strong>${this.projectName}</strong>`,
        2: this.reactionEmoji,
      }) || this.$t('Notification.message.TaskNoProjectReactionPlugin', {
        0: user,
        1: this.reactionEmoji,
      });
    },
  },
};
</script>