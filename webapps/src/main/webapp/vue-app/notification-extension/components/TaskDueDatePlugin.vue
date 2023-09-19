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
    message() {
      return this.projectName?.length && this.$t('Notification.message.TaskInProjectDueDatePlugin', {
        0: `<a class="user-name font-weight-bold">${this.profileFullName}</a>`,
        1: `<strong>${this.projectName}</strong>`,
      }) || this.$t('Notification.message.TaskNoProjectDueDatePlugin', {
        0: `<a class="user-name font-weight-bold">${this.profileFullName}</a>`,
      });
    },
  },
};
</script>