/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.storage;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.exception.EntityNotFoundException;

import java.util.List;
import java.util.Set;

public interface ProjectStorage {

    /**
     * Return the project with given <code>projectId</code>.
     *
     * @param projectId the project id.
     * @return get the given project.
     * @throws EntityNotFoundException when user is not authorized to get project.
     */
    ProjectDto getProject(Long projectId) throws EntityNotFoundException;

    Set<String> getManager(long projectId);

    Set<String> getParticipator(long projectId);

    /**
     * Create a project with given <code>project</code> model object.
     *
     * @param project the given project.
     * @return create the given project.
     */
    ProjectDto createProject(ProjectDto project);

    /**
     * Create a sub-project with given <code>project</code> model object and parent project ID.
     *
     * @param project  the project metadata to create.
     * @param parentId parent project ID
     * @return Create a sub-project.
     * @throws EntityNotFoundException the project associated with <code>parentId</code> doesn't exist.
     */
    ProjectDto createProject(ProjectDto project, long parentId) throws EntityNotFoundException;


    ProjectDto updateProject(ProjectDto project);

    void updateProjectNoReturn(ProjectDto project);

    /**
     * Remove the project with given <code>projectId</code>,
     * and also its descendants if <code>deleteChild</code> is true.
     *
     * @param projectId   the given project id.
     * @param deleteChild delete Child.
     * @throws EntityNotFoundException when user is not authorized to remove project.
     */
    void removeProject(long projectId, boolean deleteChild) throws EntityNotFoundException;

    /**
     * Clone a project with given <code>projectId</code>. If <code>cloneTask</code> is true,
     * it will also clone all non-completed tasks from the project.
     *
     * @param projectId The id of a project which it copies from.
     * @param cloneTask If false, it will clone only project metadata.
     *                  Otherwise, it also clones all non-completed tasks from the project.
     * @return The cloned project.
     * @throws EntityNotFoundException when user is not authorized to clone project.
     */
    ProjectDto cloneProject(long projectId, boolean cloneTask) throws EntityNotFoundException;

    /**
     * Return a list of children of a parent project with given <code>parentId</code>.
     *
     * @param parentId The parent id of a project.
     * @param offset term to offset results.
     * @param limit term to limit results.
     * @return The list of children of a parent project.
     * @throws Exception when can't get sub projects.
     */
    List<ProjectDto> getSubProjects(long parentId,int offset ,int limit) throws Exception;

    List<ProjectDto> findProjects(ProjectQuery query,int offset ,int limit);

    int countProjects(ProjectQuery query);

    List<ProjectDto> findProjects(List<String> memberships, String keyword, OrderBy order,int offset ,int limit);

    List<ProjectDto> findCollaboratedProjects(String userName, String keyword,int offset ,int limit);

    List<ProjectDto> findNotEmptyProjects(List<String> memberships, String keyword,int offset ,int limit);

    int countCollaboratedProjects(String userName, String keyword);

    int countNotEmptyProjects(List<String> memberships, String keyword);

    int countProjects(List<String> memberships, String keyword);

}
