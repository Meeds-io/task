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
package org.exoplatform.task.util;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.domain.*;
import org.exoplatform.task.dto.*;
import org.exoplatform.task.service.UserService;
import org.exoplatform.task.storage.ProjectStorage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class StorageUtil{

    private static final Log LOG = ExoLogger.getExoLogger(StorageUtil.class);

    public static ChangeLog changeLogToEntity(ChangeLogEntry changeLogEntry, UserService userService) {
        ChangeLog changeLog = new ChangeLog();
        changeLog.setId(changeLogEntry.getId());
        changeLog.setTask(changeLogEntry.getTask());
        changeLog.setAuthor(changeLogEntry.getAuthor());
        changeLog.setActionName(changeLogEntry.getActionName());
        changeLog.setCreatedTime(changeLogEntry.getCreatedTime());
        changeLog.setTarget(changeLogEntry.getTarget());
        return changeLog;
    }

    public static ChangeLogEntry changeLogToDto(ChangeLog changeLog, UserService userService) {
        ChangeLogEntry changeLogEntry = new ChangeLogEntry();
        changeLogEntry.setId(changeLog.getId());
        changeLogEntry.setTask(changeLog.getTask());
        changeLogEntry.setAuthor(changeLog.getAuthor());
        changeLogEntry.setActionName(changeLog.getActionName());
        changeLogEntry.setCreatedTime(changeLog.getCreatedTime());
        changeLogEntry.setTarget(changeLog.getTarget());
        changeLogEntry.setAuthorFullName(userService.loadUser(changeLog.getAuthor()).getDisplayName());
        changeLogEntry.setAuthorAvatarUrl(userService.loadUser(changeLog.getAuthor()).getAvatar());
        changeLogEntry.setExternal(userService.loadUser(changeLog.getAuthor()).isExternal());
        if (changeLog.getActionName().equals("assign") || changeLog.getActionName().equals("unassign")
            || changeLog.getActionName().equals("assignCoworker") || changeLog.getActionName().equals("unassignCoworker")) {
            changeLogEntry.setTargetFullName(userService.loadUser(changeLog.getTarget()).getDisplayName());
            changeLogEntry.setIsTargetFullNameExternal(CommentUtil.isExternal(userService.loadUser(changeLog.getTarget()).getUsername()));
        }
        return changeLogEntry;
    }

    public static Task taskToEntity(TaskDto taskDto) {
        if (taskDto == null) {
          return null;
        }
        Task taskEntity = taskDto.getId() == 0 ? null : getTaskEntityById(taskDto.getId());
        if (taskEntity == null) {
          taskEntity = new Task();
        }
        taskEntity.setTitle(taskDto.getTitle());
        taskEntity.setDescription(taskDto.getDescription());
        taskEntity.setPriority(taskDto.getPriority());
        taskEntity.setContext(taskDto.getContext());
        taskEntity.setAssignee(taskDto.getAssignee());
        taskEntity.setCoworker(taskDto.getCoworker());
        taskEntity.setWatcher(taskDto.getWatcher());
        if (taskDto.getStatus() != null) {
          taskEntity.setStatus(getStatusEntityById(taskDto.getStatus().getId()));
        }
        taskEntity.setRank(taskDto.getRank());
        taskEntity.setActivityId(taskDto.getActivityId());
        taskEntity.setCompleted(taskDto.isCompleted());
        taskEntity.setCreatedBy(taskDto.getCreatedBy());
        taskEntity.setCreatedTime(taskDto.getCreatedTime());
        taskEntity.setEndDate(taskDto.getEndDate());
        taskEntity.setStartDate(taskDto.getStartDate());
        taskEntity.setDueDate(taskDto.getDueDate());
        return taskEntity;
    }

    public static TaskDto taskToDto(Task taskEntity, ProjectStorage projectStorage) {
        if(taskEntity==null){
            return null;
        }
        TaskDto task = new TaskDto();
        task.setId(taskEntity.getId() == null ? 0l : taskEntity.getId());
        task.setTitle(taskEntity.getTitle());
        if(taskEntity.getDescription()!=null) {
            try {
                task.setDescription(HTMLSanitizer.sanitize(taskEntity.getDescription()));
            } catch (Exception e) {
                LOG.warn("Task description cannot be sanitized",e);
            }
        }
        task.setPriority(taskEntity.getPriority());
        task.setContext(taskEntity.getContext());
        task.setAssignee(taskEntity.getAssignee());
        task.setCoworker(taskEntity.getCoworker());
        task.setWatcher(taskEntity.getWatcher());
        task.setStatus(statusToDTO(taskEntity.getStatus(),projectStorage));
        task.setRank(taskEntity.getRank());
        task.setActivityId(taskEntity.getActivityId());
        task.setCompleted(taskEntity.isCompleted());
        task.setCreatedBy(taskEntity.getCreatedBy());
        task.setCreatedTime(taskEntity.getCreatedTime());
        task.setEndDate(taskEntity.getEndDate());
        task.setStartDate(taskEntity.getStartDate());
        task.setDueDate(taskEntity.getDueDate());
        return task;
    }

    public static Status getStatusEntityById(Long statusId) {
      return ExoContainerContext.getService(DAOHandler.class).getStatusHandler().find(statusId);
    }

    public static Project getProjectEntityById(Long projectId) {
      return ExoContainerContext.getService(DAOHandler.class).getProjectHandler().find(projectId);
    }

    public static Label getLabelEntityById(Long labelId) {
      return ExoContainerContext.getService(DAOHandler.class).getLabelHandler().find(labelId);
    }

    public static Comment getCommentEntityById(Long commentId) {
      return ExoContainerContext.getService(DAOHandler.class).getCommentHandler().find(commentId);
    }

    public static Task getTaskEntityById(Long taskId) {
      return ExoContainerContext.getService(DAOHandler.class).getTaskHandler().find(taskId);
    }

    public static Status statusToEntity(StatusDto statusDto) {
        if(statusDto==null){
            return null;
        }
        Status status = statusDto.getId() == 0 ? null : getStatusEntityById(statusDto.getId());
        if (status == null) {
          status = new Status();
        }
        status.setName(statusDto.getName());
        status.setRank(statusDto.getRank());
        if (statusDto.getProject() != null) {
          status.setProject(getProjectEntityById(statusDto.getProject().getId()));
        }
        return status;
    }

    public static StatusDto statusToDTO(Status status, ProjectStorage projectStorage) {
        if(status==null){
            return null;
        }
        StatusDto statusDto = new StatusDto();
        statusDto.setId(status.getId());
        statusDto.setName(status.getName());
        statusDto.setRank(status.getRank());
        statusDto.setProject(projectToDto(status.getProject(),projectStorage));
        return statusDto;
    }

    public static List<StatusDto> listStatusToDTOs(List<Status> status, ProjectStorage projectStorage) {
        return status.stream()
                .map((Status status1) -> statusToDTO(status1,projectStorage))
                .collect(Collectors.toList());
    }

    public static Project projectToEntity(ProjectDto projectDto) {
        if(projectDto==null){
            return null;
        }
        long projectId = projectDto.getId();
        Project project = projectId == 0 ? null : getProjectEntityById(projectDto.getId());
        if (project == null) {
          project = new Project();
        } else {
          List<Status> statuses = ExoContainerContext.getService(DAOHandler.class).getStatusHandler().getStatuses(project.getId());
          project.setStatus(new HashSet<>(statuses));
        }
        project.setName(projectDto.getName());
        project.setDescription(projectDto.getDescription());
        project.setColor(projectDto.getColor());
        project.setDueDate(projectDto.getDueDate());
        project.setLastModifiedDate(projectDto.getLastModifiedDate());
        project.setParticipator(projectDto.getParticipator());
        project.setManager(projectDto.getManager());
        if (projectDto.getParent() != null) {
          project.setParent(getProjectEntityById(projectDto.getParent().getId()));
        }
        return project;
    }

    public static ProjectDto projectToDto(Project project, ProjectStorage projectStorage) {
        if(project==null){
            return null;
        }
        ProjectDto projectDto = new ProjectDto();
        projectDto.setId(project.getId());
        projectDto.setName(HTMLSanitizer.sanitize(project.getName()));
        projectDto.setDescription(project.getDescription());
        projectDto.setColor(project.getColor());
        projectDto.setDueDate(project.getDueDate());
        projectDto.setLastModifiedDate(project.getLastModifiedDate());
        projectDto.setParticipator(projectStorage.getParticipator(project.getId()));
        projectDto.setManager(projectStorage.getManager(project.getId()));
        projectDto.setParent(projectToDto(project.getParent(),projectStorage));
        return projectDto;
    }


    public static ProjectDto projectToDto(Project project) {
        if(project==null){
            return null;
        }
        ProjectDto projectDto = new ProjectDto();
        projectDto.setId(project.getId());
        projectDto.setName(project.getName());
        projectDto.setDescription(project.getDescription());
        projectDto.setColor(project.getColor());
        projectDto.setDueDate(project.getDueDate());
        projectDto.setLastModifiedDate(project.getLastModifiedDate());
        return projectDto;
    }


    public static Label labelToEntity(LabelDto labelDto) {
        if (labelDto == null) {
          return null;
        }
        Label label = labelDto.getId() == 0 ? null : getLabelEntityById(labelDto.getId());
        if (label == null) {
          label = new Label();
        }
        label.setUsername(labelDto.getUsername());
        if (labelDto.getProject() != null) {
          label.setProject(getProjectEntityById(labelDto.getProject().getId()));
        }
        label.setName(labelDto.getName());
        label.setColor(labelDto.getColor());
        label.setHidden(labelDto.isHidden());
        label.setParent(labelDto.getParent() == null ? null : getLabelEntityById(labelDto.getParent().getId()));
        return label;
    }

    public static LabelDto labelToDto(Label label, Identity currentUser,ProjectStorage projectStorage) {
        if(label==null){
            return null;
        }
        LabelDto labelDto = new LabelDto();
        labelDto.setId(label.getId());
        labelDto.setUsername(label.getUsername());
        labelDto.setName(label.getName());
        labelDto.setProject(projectToDto(label.getProject(),projectStorage));
        labelDto.setColor(label.getColor());
        labelDto.setHidden(label.isHidden());
        labelDto.setCanEdit(labelDto.getProject().canEdit(currentUser)||label.getUsername().equals(currentUser.getUserId()));
        labelDto.setParent(labelToDto(label.getParent()));
        return labelDto;
    }

    public static LabelDto labelToDto(Label label, TaskDto task, Identity currentUser,ProjectStorage projectStorage) {
        if(label==null){
            return null;
        }
        LabelDto labelDto = new LabelDto();
        labelDto.setId(label.getId());
        labelDto.setUsername(label.getUsername());
        labelDto.setName(label.getName());
        labelDto.setProject(projectToDto(label.getProject(),projectStorage));
        labelDto.setColor(label.getColor());
        labelDto.setHidden(label.isHidden());
        labelDto.setCanEdit(labelDto.getProject().canEdit(currentUser)||task.getCreatedBy().equals(currentUser.getUserId()));
        labelDto.setParent(labelToDto(label.getParent()));
        return labelDto;
    }


    public static LabelDto labelToDto(Label label) {
        if(label==null){
            return null;
        }
        LabelDto labelDto = new LabelDto();
        labelDto.setId(label.getId());
        labelDto.setUsername(label.getUsername());
        labelDto.setName(label.getName());
        labelDto.setProject(projectToDto(label.getProject()));
        labelDto.setColor(label.getColor());
        labelDto.setHidden(label.isHidden());
        labelDto.setParent(labelToDto(label.getParent()));
        return labelDto;
    }



    public static Label mappingLabelToEntity(LabelDto labelDto) {
        if(labelDto==null){
            return null;
        }
        Label label = labelDto.getId() == 0 ? null : getLabelEntityById(labelDto.getId());
        if (label == null) {
          label = new Label();
        }
        label.setUsername(labelDto.getUsername());
        label.setName(labelDto.getName());
        label.setColor(labelDto.getColor());
        label.setHidden(labelDto.isHidden());
        label.setParent(labelDto.getParent() == null ? null : getLabelEntityById(labelDto.getParent().getId()));
        if (labelDto.getProject() != null) {
          label.setProject(getProjectEntityById(labelDto.getProject().getId()));
        }
        return label;
    }

    public static Comment commentToEntity(CommentDto commentDto) {
        if(commentDto==null){
            return null;
        }
        Comment commentEntity = commentDto.getId() == 0 ? null : getCommentEntityById(commentDto.getId());
        if (commentEntity == null) {
          commentEntity = new Comment();
        }
        commentEntity.setAuthor(commentDto.getAuthor());
        commentEntity.setComment(commentDto.getComment());
        if (commentDto.getParentComment() != null) {
          commentEntity.setParentComment(getCommentEntityById(commentDto.getParentComment().getId()));
        }
        commentEntity.setCreatedTime(commentDto.getCreatedTime());

        commentEntity.setTask(getTaskEntityById(commentDto.getTask().getId()));
        commentEntity.setMentionedUsers(processMentions(commentDto.getTask(), commentDto.getComment()));
        return commentEntity;
    }

    public static CommentDto commentToDto(Comment comment, ProjectStorage projectStorage) {
        if (comment == null) {
            return null;
        }
        CommentDto commentDto = new CommentDto();
        commentDto.setId(comment.getId() == null ? 0 :comment.getId());
        commentDto.setAuthor(comment.getAuthor());
        commentDto.setComment(comment.getComment());
        if (comment.getParentComment() != null) {
            commentDto.setParentComment(commentToDto(comment.getParentComment(), projectStorage));
        }
        commentDto.setCreatedTime(comment.getCreatedTime());
        commentDto.setTask(taskToDto(comment.getTask(), projectStorage));
        commentDto.setMentionedUsers(comment.getMentionedUsers());
        return commentDto;
    }

    public static List<CommentDto> listCommentsToDtos(List<Comment> comments, ProjectStorage projectStorage) {
        return comments.stream()
                .filter(Objects::nonNull)
                .map((Comment comment) -> commentToDto(comment,projectStorage))
                .collect(Collectors.toList());
    }

    public static List<Comment> listCommentsToEntitys(List<CommentDto> commentDtos) {
        return commentDtos.stream()
                .filter(Objects::nonNull)
                .map(StorageUtil::commentToEntity)
                .toList();
    }

    private static Set<String> processMentions(TaskDto task, String message) {
      Set<String> mentions = new HashSet<>();
      mentions.addAll(MentionUtils.getMentionedUsernames(message));

      if (task != null
          && task.getStatus() != null
          && task.getStatus().getProject() != null) {
        Space space = getProjectSpace(task.getStatus().getProject());
        if (space != null) {
          org.exoplatform.social.core.identity.model.Identity spaceIdentity =
                                                                            ExoContainerContext.getService(IdentityManager.class)
                                                                                               .getOrCreateSpaceIdentity(space.getPrettyName());
          String spaceIdentityId = spaceIdentity == null ? null : spaceIdentity.getId();
          Set<String> mentionedRoles = MentionUtils.getMentionedRoles(message, spaceIdentityId);
          mentionedRoles.forEach(role -> {
            if (StringUtils.equals("member", role) && space.getMembers() != null) {
              mentions.addAll(Arrays.asList(space.getMembers()));
            } else if (StringUtils.equals("manager", role) && space.getManagers() != null) {
              mentions.addAll(Arrays.asList(space.getManagers()));
            } else if (StringUtils.equals("redactor", role) && space.getRedactors() != null) {
              mentions.addAll(Arrays.asList(space.getRedactors()));
            } else if (StringUtils.equals("publisher", role) && space.getPublishers() != null) {
              mentions.addAll(Arrays.asList(space.getPublishers()));
            }
          });
        }
      }
      return mentions;
    }

    private static Space getProjectSpace(ProjectDto project) {
      SpaceService spaceService = ExoContainerContext.getService(SpaceService.class);
      Space space = null;
      for (String permission : project.getManager()) {
        int index = permission.indexOf(':');
        if (index > -1) {
          String groupId = permission.substring(index + 1);
          space = spaceService.getSpaceByGroupId(groupId);
        }
      }
      return space;
    }
}
