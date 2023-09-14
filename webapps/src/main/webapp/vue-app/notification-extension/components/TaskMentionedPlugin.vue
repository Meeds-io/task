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
        {{ commentText }}
      </div>
      <div class="my-1">
        <v-btn
          :href="taskUrl"
          color="primary"
          elevation="0"
          small
          outlined>
          <v-icon size="14" class="me-1">far fa-comment</v-icon>
          <span class="text-none">
            {{ $t('Notification.label.Reply') }}
          </span>
        </v-btn>
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
    message() {
      return this.projectName?.length && this.$t('Notification.message.TaskInProjectMentionPlugin', {
        0: `<a class="user-name font-weight-bold">${this.profileFullName}</a>`,
        1: `<a class="space-name font-weight-bold">${this.projectName}</a>`,
      }) || this.$t('Notification.message.TaskNoProjectMentionPlugin', {
        0: `<a class="user-name font-weight-bold">${this.profileFullName}</a>`,
      });
    },
  },
};
</script>