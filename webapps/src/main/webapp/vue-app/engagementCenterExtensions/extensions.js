const taskUserActions = ['commentTask', 'completeTask', 'completeTaskAssigned', 'completeTaskCoworker', 'createNewTask', 'updateTask'];
export function init() {
  extensionRegistry.registerExtension('engagementCenterActions', 'user-actions', {
    type: 'task',
    options: {
      rank: 50,
      icon: 'fas fa-tasks',
      match: (actionLabel) => taskUserActions.includes(actionLabel),
      getLink: (realization) => {
        Vue.prototype.$set(realization, 'link', `${eXo.env.portal.context}/${eXo.env.portal.portalName}/tasks/taskDetail/${realization?.objectId}`);
      }
    },
  });
}