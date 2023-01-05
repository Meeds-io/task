const taskUserActions = ['commentTask', 'completeTaskAssigned', 'completeTaskCoworker', 'createNewTask', 'updateTask'];
export function init() {
  extensionRegistry.registerExtension('engagementCenterActions', 'user-actions', {
    type: 'task',
    options: {
      rank: 50,
      icon: 'fas fa-tasks',
      match: (actionLabel) => taskUserActions.includes(actionLabel),
      getLabel: () => ''
    },
  });
}