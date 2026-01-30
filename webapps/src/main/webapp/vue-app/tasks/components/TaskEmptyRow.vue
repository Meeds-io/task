<template>
  <div class="mx-auto d-flex flex-column align-center justify-center full-width full-height">
    <v-icon
      color="tertiary"
      size="60">
      fas fa-tasks
    </v-icon>
    <span class="mt-5">{{ $t('label.start.adding.tasks') }}</span>
    <v-btn
      id="addNewTaskButton"
      :small="$root.isMobile"
      color="primary"
      class="my-4"
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