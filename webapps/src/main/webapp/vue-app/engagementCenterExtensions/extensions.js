export function init() {
  extensionRegistry.registerExtension('engagementCenterActions', 'user-actions', {
    type: 'task',
    options: {
      rank: 50,
      icon: 'fas fa-tasks',
      match: (actionLabel) => [
        'commentTask',
        'completeTask',
        'completeTaskAssigned',
        'completeTaskCoworker',
        'createNewTask',
        'updateTask'
      ].includes(actionLabel),
      getLink: (realization) => {
        realization.link = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks/taskDetail/${realization?.objectId}`;
        return realization.link;
      },
      isExtensible: true
    },
  });
}