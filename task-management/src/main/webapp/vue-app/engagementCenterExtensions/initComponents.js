import TaskActionValue from './components/TaskActionValue.vue';
const components = {
  'task-action-value': TaskActionValue,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

