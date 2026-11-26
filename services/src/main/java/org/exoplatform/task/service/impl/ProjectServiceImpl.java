/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.task.service.impl;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.StatusService;
import org.exoplatform.task.service.TaskPayload;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.storage.ProjectStorage;
import org.exoplatform.task.util.StorageUtil;
import org.exoplatform.task.util.TaskUtil;

@Singleton
public class ProjectServiceImpl implements ProjectService {

  private static final Log   LOG          = ExoLogger.getExoLogger(ProjectServiceImpl.class);

  public static final String PREFIX_CLONE = "Copy of ";

  private static final int  PAGE_SIZE = 200;

  @Inject
  private ProjectStorage     projectStorage;


  @Inject
  StatusService              statusService;

  @Inject
  TaskService                taskService;

  @Inject
  DAOHandler                 daoHandler;

  private ListenerService listenerService;

  public ProjectServiceImpl() {
  }

  // For testing purpose only
  public ProjectServiceImpl(StatusService statusService,
                            TaskService taskService,
                            DAOHandler daoHandler,
                            ProjectStorage projectStorage,
                            ListenerService listenerService) {
    this.daoHandler = daoHandler;
    this.statusService = statusService;
    this.taskService = taskService;
    this.projectStorage = projectStorage;
    this.listenerService = listenerService;
  }

  @Override
  public ProjectDto createProject(ProjectDto project) {
    ProjectDto proj = projectStorage.createProject(project);
    return proj;
  }

  @Override
  public ProjectDto createProject(ProjectDto project, long parentId) throws EntityNotFoundException {
    ProjectDto parentProject = projectStorage.getProject(parentId);
    if (parentProject != null) {
      project.setParent(parentProject);
      // If parent, list of members/participators of parents override the list of
      // members/participators in parameter
      project.setParticipator(new HashSet<String>(parentProject.getParticipator()));
      // If parent, list of manager of parents override the list of managers in
      // parameter
      project.setManager(new HashSet<String>(parentProject.getManager()));

      // persist project
      project.setLastModifiedDate(System.currentTimeMillis());
      project = createProject(project);
      // inherit status from parent
      List<StatusDto> prSt = statusService.getStatuses(parentProject.getId());
      for (StatusDto st : prSt) {
        statusService.createStatus(project, st.getName());
      }
      return project;
    } else {
      LOG.info("Can not find project for parent with ID: " + parentId);
      throw new EntityNotFoundException(parentId, ProjectDto.class);
    }
  }

  @Override
  public ProjectDto updateProject(ProjectDto proj) {
    proj.setLastModifiedDate(System.currentTimeMillis());
    return projectStorage.updateProject(proj);
  }

  @Override
  public void updateProjectNoReturn(ProjectDto proj) {
    proj.setLastModifiedDate(System.currentTimeMillis());
    projectStorage.updateProjectNoReturn(proj);
  }

  @Override
  public void removeProject(long id, boolean deleteChild) throws EntityNotFoundException {
    ProjectDto project = getProject(id);
    if (project == null)
      throw new EntityNotFoundException(id, ProjectDto.class);
    List<TaskDto> tasks = getTasksOfProjects(getProjectIdsToDelete(id, deleteChild));
    try {
      projectStorage.removeProject(id, deleteChild);
    } catch (RuntimeException | EntityNotFoundException e) {
      // The storage deletes the tasks one by one, each in its own transaction: when it fails midway, the listeners
      // still learn about the tasks that are gone
      announceDeletedTasks(tasks.stream().filter(this::isDeleted).toList());
      throw e;
    }
    announceDeletedTasks(tasks);
  }

  /**
   * The tasks of a project are deleted with it by the storage, which sends no
   * event of its own: the listeners of a deleted task (search index, content
   * links, reactions of the comments, gamification) learn it here.
   */
  private void announceDeletedTasks(List<TaskDto> tasks) {
    tasks.forEach(task -> TaskUtil.broadcastEvent(listenerService, TaskUtil.TASK_DELETED, null, new TaskPayload(null, task)));
  }

  private boolean isDeleted(TaskDto task) {
    try {
      taskService.getTask(task.getId());
      return false;
    } catch (EntityNotFoundException e) {
      return true;
    } catch (RuntimeException e) {
      LOG.warn("Unable to tell whether the task {} is deleted: its listeners won't be told", task.getId(), e);
      return false;
    }
  }

  private List<Long> getProjectIdsToDelete(long projectId, boolean deleteChild) {
    List<Long> projectIds = new ArrayList<>(List.of(projectId));
    if (deleteChild) {
      // The sub-projects, their statuses and their tasks go with the project
      try {
        for (int i = 0; i < projectIds.size(); i++) {
          int offset = 0;
          List<ProjectDto> children;
          do {
            children = projectStorage.getSubProjects(projectIds.get(i), offset, PAGE_SIZE);
            children.forEach(child -> projectIds.add(child.getId()));
            offset += children.size();
          } while (children.size() == PAGE_SIZE);
        }
      } catch (Exception e) {
        LOG.warn("Unable to list the sub-projects of the project {} before its deletion: the listeners of a deleted task "
                 + "won't be told of the tasks of those not listed",
                 projectId,
                 e);
      }
    }
    return projectIds;
  }

  private List<TaskDto> getTasksOfProjects(List<Long> projectIds) {
    TaskQuery query = new TaskQuery();
    query.setProjectIds(projectIds);
    // A stable order, so the pages neither repeat nor skip a task
    query.setOrderBy(List.of(new OrderBy("id", true)));
    List<TaskDto> tasks = new ArrayList<>();
    try {
      List<TaskDto> page;
      do {
        page = taskService.findTasks(query, tasks.size(), PAGE_SIZE);
        tasks.addAll(page);
      } while (page.size() == PAGE_SIZE);
    } catch (Exception e) {
      LOG.warn("Unable to list the tasks of the projects {} before their deletion: the listeners of a deleted task won't be told",
               projectIds,
               e);
    }
    return tasks;
  }

  @Override
  public ProjectDto cloneProject(long id, boolean cloneTask) throws Exception {

    ProjectDto project = getProject(id); // Can throw ProjectNotFoundException

    ProjectDto newProject = project.clone(cloneTask);
    Set<String> managers = getManager(id);
    if (managers != null && !managers.isEmpty()) {
      newProject.setManager(managers);
    }
    Set<String> participators = getParticipator(id);
    if (participators != null && !participators.isEmpty()) {
      newProject.setParticipator(participators);
    }
    newProject.setId(0);
    newProject.setName(PREFIX_CLONE + newProject.getName());
    newProject = createProject(newProject);
    // . Get all Status of project
    List<StatusDto> statuses = statusService.getStatuses(id);
    ListAccess<TaskDto> tasks;
    TaskQuery taskQuery;
    if (statuses != null) {
      for (StatusDto st : statuses) {
        StatusDto s = statusService.createStatus(newProject, st.getName());
        if (cloneTask) {
          taskQuery = new TaskQuery();
          taskQuery.setStatus(StorageUtil.getStatusEntityById(st.getId()));
          for (TaskDto t : taskService.findTasks(taskQuery, 0, -1)) {
            TaskDto newTask = t.clone();
            newTask.setId(0);
            newTask.setStatus(s);
            newTask.setCoworker(taskService.getCoworker(t.getId()));
            newTask.setTitle(PREFIX_CLONE + newTask.getTitle());
            taskService.createTask(newTask);
          }

        }
      }
    }

    return newProject;

  }

  @Override
  public ProjectDto getProject(Long id) throws EntityNotFoundException {

    ProjectDto project = projectStorage.getProject(id);
    if (project == null)
      throw new EntityNotFoundException(id, ProjectDto.class);

    return project;

  }

  @Override
  public List<ProjectDto> getSubProjects(long parentId, int offset, int limit) {
    try {
      return projectStorage.getSubProjects(parentId, offset, limit);
    } catch (Exception ex) {
      return new ArrayList<ProjectDto>();
    }
  }

  @Override
  public List<ProjectDto> findProjects(ProjectQuery query, int offset, int limit) {
    return projectStorage.findProjects(query, offset, limit);
  }

  @Override
  public int countProjects(ProjectQuery query){
    return projectStorage.countProjects(query);
  }

  @Override
  public List<ProjectDto> findProjects(List<String> memberships, String keyword, OrderBy order, int offset, int limit) {
    return projectStorage.findProjects(memberships, keyword, order, offset, limit);
  }

  @Override
  public List<ProjectDto> findCollaboratedProjects(String userName, String keyword, int offset, int limit) {
    return projectStorage.findCollaboratedProjects(userName,keyword, offset, limit);
  }

  @Override
  public List<ProjectDto> findNotEmptyProjects(List<String> memberships, String keyword, int offset, int limit)  {
    return projectStorage.findNotEmptyProjects(memberships,keyword, offset, limit);
  }

  @Override
  public int countCollaboratedProjects(String userName, String keyword) {
    return projectStorage.countCollaboratedProjects(userName,keyword);
  }

  @Override
  public int countNotEmptyProjects(List<String> memberships, String keyword)  {
    return projectStorage.countNotEmptyProjects(memberships,keyword);
  }

  @Override
  public int countProjects(List<String> memberships, String keyword) {
    return projectStorage.countProjects(memberships, keyword);
  }

  @Override
  public Set<String> getManager(long projectId) {
    return projectStorage.getManager(projectId);
  }

  @Override
  public Set<String> getParticipator(long projectId) {
    return projectStorage.getParticipator(projectId);
  }

}
