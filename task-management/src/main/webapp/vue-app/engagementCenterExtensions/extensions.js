import './initComponents.js';
const taskUserActions = ['commentTask', 'completeTaskAssigned', 'completeTaskCoworker', 'createNewTask', 'updateTask'];
export function init() {
  extensionRegistry.registerExtension('engagementCenterActions', 'user-actions', {
    type: 'task',
    options: {
      rank: 50,
      vueComponent: Vue.options.components['task-action-value'],
      match: (actionLabel) => taskUserActions.includes(actionLabel),
    },
  });
}