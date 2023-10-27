<template>
  <div class="mx-auto text-center">
    <v-icon class="icon-default-color mb-3" large>fas fa-tasks</v-icon>
    <p class="mb-5 subtitle-1 text-sub-title">{{ $t('tasks.label.allSet') }}</p>
    <v-btn class="btn btn-primary d-inline" @click="$emit('open-task-drawer')">{{ $t('label.addTask') }}</v-btn>
  </div> 
</template>
