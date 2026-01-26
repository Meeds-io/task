<template>
  <div class="mx-auto mt-n5 d-flex flex-column">
    <v-icon
      color="secondary"
      size="60"
      class="mb-2">
      fas fa-tasks
    </v-icon>
    <p class="mt-2 mb-4">{{ $t('label.start.adding.tasks') }}</p>
    <v-btn
    id="addNewTaskButton"
    :small="$root.isMobile"
    color="primary"
    outlined
    @click="openTaskDrawer">
    <span>
      {{ $t('label.addTask') }}
    </span>
  </v-btn> 
  </div>
</template>
<script>
export default {
  props: {
    project: {
      type: String,
      default: ''
    },
  },  
  methods: {
    openTaskDrawer() {
      const defaultTask={
        id: null,
        status: {project: this.project},
        priority: 'NONE',
        description: '',
        title: ''
      };
      this.$root.$emit('open-task-drawer', defaultTask);
    },
  }
};
</script>