/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.task.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.ParameterEntityException;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.web.controller.router.Router;
import org.gatein.common.text.EntityEncoder;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 6/3/15
 */
public final class ProjectUtil {
  private static final Log LOG = ExoLogger.getExoLogger(ProjectUtil.class);

  public static final String URL_PROJECT_DETAIL = "/projectDetail/";
  public static final String URL_PROJECT_ALL = "/projects";
  public static final String URL_PROJECT_MY_TASK = "/my-task";
  public static final String URL_LABEL_ALL = "/labels";
  public static final String URL_LABEL_DETAIL = "/label/";
  public static final long INCOMING_PROJECT_ID = -1;
  public static final long TODO_PROJECT_ID = -2;
  public static final long LABEL_PROJECT_ID = -5;

  public static final String NAME = "name";

  public static final String DUE_DATE = "dueDate";

  private ProjectUtil() {
  }
  
  public static List<ProjectDto> getProjectTree(String space_group_id, ProjectService projectService) {
    List<String> memberships = new LinkedList<String>();
    ConversationState state = ConversationState.getCurrent();
    Identity identity = state.getIdentity();
    if (space_group_id == null) {
      memberships.addAll(UserUtil.getMemberships(identity));      
    } else {
      memberships.addAll(UserUtil.getSpaceMemberships(space_group_id));
    }

//    ListAccess<Project> projects = projectService.findProjects(memberships, null, null);
    ProjectQuery manQ = new ProjectQuery();
    manQ.setManager(memberships);
    List<ProjectDto> editPrj = projectService.findProjects(manQ,0,-1);
    //
    ProjectQuery parQ = new ProjectQuery();
    parQ.setParticipator(memberships);
    List<ProjectDto> viewPrj = projectService.findProjects(parQ,0,-1);
    //
    Set<ProjectDto> tmp = new HashSet<ProjectDto>();
    try {
      tmp.addAll(buildProxy(editPrj, identity, true));
      tmp.addAll(buildProxy(viewPrj, identity, false));
    } catch (Exception ex) {
      LOG.error("Can't load project list", ex);
    }
    return ProjectUtil.buildRootProjects(new LinkedList<ProjectDto>(tmp));
  }

  public static List<ProjectDto> getProjectTree(String space_group_id, ProjectService projectService,int offset, int limit) {
    List<String> memberships = new LinkedList<String>();
    ConversationState state = ConversationState.getCurrent();
    Identity identity = state.getIdentity();
    if (space_group_id == null) {
      memberships.addAll(UserUtil.getMemberships(identity));
    } else {
      memberships.addAll(UserUtil.getSpaceMemberships(space_group_id));
    }

//    ListAccess<Project> projects = projectService.findProjects(memberships, null, null);
    ProjectQuery manQ = new ProjectQuery();
    manQ.setManager(memberships);
    List<ProjectDto> editPrj = projectService.findProjects(manQ,offset,limit);
    //
    ProjectQuery parQ = new ProjectQuery();
    parQ.setParticipator(memberships);
    List<ProjectDto> viewPrj = projectService.findProjects(parQ,offset,limit);
    //
    Set<ProjectDto> tmp = new HashSet<ProjectDto>();
    try {
      tmp.addAll(buildProxyDto(editPrj, identity, true));
      tmp.addAll(buildProxyDto(viewPrj, identity, false));
    } catch (Exception ex) {
      LOG.error("Can't load project list", ex);
    }
    return new LinkedList<ProjectDto>(tmp);
  }

  public static List<ProjectDto> getProjectTree(List<String> memberships,String query, Identity identity , org.exoplatform.task.service.ProjectService projectService,int offset, int limit) {

    List<ProjectDto> projects = projectService.findProjects(memberships,query,null,offset, limit);
    /* ProjectQuery manQ = new ProjectQuery();
    manQ.setManager(memberships);
    List<ProjectDto> editPrj = projectService.findProjects(manQ,offset,limit);
    //
    ProjectQuery parQ = new ProjectQuery();
    parQ.setParticipator(memberships);
    List<ProjectDto> viewPrj = projectService.findProjects(parQ,offset,limit);
    //*/
    Set<ProjectDto> tmp = new HashSet<ProjectDto>();
    try {
    //  tmp.addAll(buildProxyDto(editPrj, identity, true));
      tmp.addAll(buildProxyDto(projects, identity, false));
    } catch (Exception ex) {
      LOG.error("Can't load project ", ex);
    }
    return new LinkedList<ProjectDto>(tmp);
  }

  public static List<ProjectDto> getProjectTree(List<ProjectDto> projects, Identity identity) {

    Set<ProjectDto> tmp = new HashSet<ProjectDto>();
    try {
      tmp.addAll(buildProxyDto(projects, identity, false));
    } catch (Exception ex) {
      LOG.error("Can't load project ", ex);
    }
    return new LinkedList<ProjectDto>(tmp);
  }

  private static Collection<? extends ProjectDto> buildProxy(List<ProjectDto> projects, Identity user, boolean editable) {
    List<ProjectDto> tmp = new LinkedList<ProjectDto>();
    for (ProjectDto p : projects) {
      tmp.add(new ProjectProxy(p, user, editable));
    }
    return tmp;
  }

  private static Collection<? extends ProjectDto> buildProxyDto(List<ProjectDto> projects, Identity user, boolean editable) {
    List<ProjectDto> tmp = new LinkedList<ProjectDto>();
    for (ProjectDto p : projects) {
      tmp.add(new ProjectProxyDto(p, user, editable));
    }
    return tmp;
  }

  public static List<ProjectDto> buildRootProjects(List<ProjectDto> projects) {
    if (projects == null) return projects;

    Map<Long, ProjectDto> maps = new HashMap<Long, ProjectDto>();
    Set<ProjectDto> rootPRJs = new LinkedHashSet<ProjectDto>();
    //Set<Project> childs = new LinkedHashSet<Project>();
    for (ProjectDto p : projects) {
      while(true) {
        if (!maps.containsKey(p.getId())) {
          maps.put(p.getId(), p);
        }
        ProjectDto parent = p.getParent();
        if (parent == null) {
          rootPRJs.add(p);
          break;
        } else {
          if (maps.containsKey(parent.getId())) {
            parent = maps.get(parent.getId());
          }
          if (!parent.getChildren().contains(p)) {
            parent.getChildren().add(p);
          }
          p = parent;
        }
      }
    }
    return new LinkedList<ProjectDto>(rootPRJs);
  }

  public static List<ProjectDto> flattenTree(List<ProjectDto> projectTree, ProjectService projectService) {
    if (projectTree == null) {
      return null;
    }

    Set<ProjectDto> projects = new HashSet<ProjectDto>();
    for (ProjectDto p : projectTree) {
      projects.add(p);
      List<ProjectDto> tmp = projectService.getSubProjects(p.getId(),0,-1);
      List<ProjectDto> children = new LinkedList<ProjectDto>();
      try {
        children = tmp;
      } catch (Exception ex) {
        LOG.error("Can't load project list", ex);
      }
      if (children != null && !children.isEmpty()) {
        projects.addAll(flattenTree(children, projectService));
      }
    }    
    return new ArrayList<ProjectDto>(projects);
  }

  public static List<ProjectDto> flattenTree(List<ProjectDto> projectTree, org.exoplatform.task.service.ProjectService projectService,int offset, int limit) {
    if (projectTree == null) {
      return null;
    }

    Set<ProjectDto> projects = new HashSet<ProjectDto>();
    for (ProjectDto p : projectTree) {
      projects.add(p);
      List<ProjectDto> tmp = projectService.getSubProjects(p.getId(),offset,limit);
      List<ProjectDto> children = new LinkedList<ProjectDto>();

      if (children != null && !children.isEmpty()) {
        projects.addAll(flattenTree(children, projectService,offset,limit));
      }
    }
    return new ArrayList<ProjectDto>(projects);
  }

  //TODO: should move this method to web module
  public static String buildBreadcumbs(Long id, ProjectService projectService, ResourceBundle bundle) {
    ProjectDto project = null;
    if (id > 0) {
      try {
        project = projectService.getProject(id);
      } catch (EntityNotFoundException e) {
        LOG.warn("project {} not found", id);
      }
    }

    EntityEncoder encoder = HTMLEntityEncoder.getInstance();
    StringBuilder builder = new StringBuilder();
    if (project != null) {
      ProjectDto tmp = project;
      while (tmp != null) {
        StringBuilder el = new StringBuilder();
        if (builder.length() == 0) {
          el.append("<li class=\"active\">").append(encoder.encode(tmp.getName())).append("</li>");
        } else {
          el.append("<li>")
                  .append("<a class=\"Selected\" title=\"\" data-placement=\"bottom\" rel=\"tooltip\" href=\"#\" data-original-title=\"")
                  .append(encoder.encode(tmp.getName()))
                  .append("\">")
                  .append(encoder.encode(tmp.getName()))
                  .append("</a>")
                  .append("<span class=\"uiIconMiniArrowRight\"></span>")
                  .append("</li>");
        }
        builder.insert(0, el.toString());
        tmp = tmp.getParent();
      }
    }

    String label = bundle.getString("label.projects");
    StringBuilder el = new StringBuilder();
    if (builder.length() == 0) {
      el.append("<li class=\"active\">").append(label).append("</li>");
    } else {
      el.append("<li>")
              .append("<a class=\"Selected\" title=\"\" data-placement=\"bottom\" rel=\"tooltip\" href=\"#\" data-original-title=\"")
              .append(label)
              .append("\">")
              .append(label)
              .append("</a>")
              .append("<span class=\"uiIconMiniArrowRight\"></span>")
              .append("</li>");
    }
    builder.insert(0, el.toString());

    return builder.toString();
  }

  public static String permalink(Long projectId, String filter, Long labelId) {
    if (labelId == null && projectId == null) {
      return "#";
    }

    StringBuilder url = new StringBuilder(ResourceUtil.buildBaseURL());
    if (url.length() <= 1) {
      return url.toString();
    }

    if (labelId != null && labelId >= 0) {
      if (labelId == 0){
        url.append(URL_LABEL_ALL);
      } else {
        url.append(URL_LABEL_DETAIL).append(labelId);
      }

    } else if (projectId == 0){
      url.append(URL_PROJECT_ALL);

    } else if (INCOMING_PROJECT_ID == projectId) {
      // Don't need to append because default URL will point to incoming
    } else if (TODO_PROJECT_ID == projectId) {
      url.append(URL_PROJECT_MY_TASK);
      if (filter != null && !filter.isEmpty()) {
        url.append("/").append(filter);
      }
    } else {
      url.append(URL_PROJECT_DETAIL).append(projectId);
    }
    return url.toString();
  }

  private static Long parseLong(String l) {
    try {
      return l != null ? Long.parseLong(l) : null;
    } catch (NumberFormatException ex) {
      return null;
    }
  }
  public static Object[] parsePermalinkURL(String requestPath) {
    if (requestPath == null || requestPath.isEmpty()) {
      return null;
    }

    if (requestPath.charAt(requestPath.length() - 1) == '/') {
      requestPath = requestPath.substring(0, requestPath.length() - 1);
    }

    Long projectId = null;
    String filter = null;
    Long labelId = null;

    int index;
    int lastSlashIndex;
    String id;

    if (requestPath.endsWith(URL_PROJECT_ALL)) {
      projectId = 0L;

    } else if ((index = requestPath.indexOf(URL_PROJECT_MY_TASK)) >= 0) {
      lastSlashIndex = requestPath.indexOf('/', index + 1);
      String path = lastSlashIndex == -1 ? requestPath.substring(index) : requestPath.substring(index, lastSlashIndex);
      if (URL_PROJECT_MY_TASK.equals(path)) {
        // Valid my-task URL
        projectId = new Long(TODO_PROJECT_ID);
        if (lastSlashIndex != -1) {
          int lastIndex = requestPath.indexOf('/', lastSlashIndex + 1);
          String s;
          if (lastIndex == -1) {
            s = requestPath.substring(lastSlashIndex + 1);
          } else {
            s = requestPath.substring(lastSlashIndex + 1, lastIndex);
          }
          try {
            TaskUtil.DUE.valueOf(s.toUpperCase());
            filter = s;
          } catch (IllegalArgumentException ex) {
            // Not valid filter
            filter = null;
          }
        }
      }
    } if ((index = requestPath.indexOf(URL_PROJECT_DETAIL)) >= 0) {
      index = index + URL_PROJECT_DETAIL.length();
      lastSlashIndex = requestPath.indexOf('/', index);
      if (lastSlashIndex > 0) {
        id = requestPath.substring(index, lastSlashIndex);
      } else {
        id = requestPath.substring(index);
      }
      projectId = parseLong(id);

    } else if (requestPath.endsWith(URL_LABEL_ALL)) {
      labelId = 0L;

    } else if ((index = requestPath.indexOf(URL_LABEL_DETAIL)) >= 0) {
      index = index + URL_LABEL_DETAIL.length();
      lastSlashIndex = requestPath.indexOf('/', index);
      if (lastSlashIndex > 0) {
        id = requestPath.substring(index, lastSlashIndex);
      } else {
        id = requestPath.substring(index);
      }
      labelId = parseLong(id);
    }

    if (projectId != null || labelId != null) {
      return new Object[] {projectId, filter, labelId};
    } else {
      return null;
    }
  }

  public static String buildProjectURL(ProjectDto project, SiteKey siteKey, ExoContainer container, Router router) {
    long projectId = ProjectUtil.TODO_PROJECT_ID;
    if (project != null) {
      projectId = project.getId();
    }

    StringBuilder urlBuilder = new StringBuilder(ResourceUtil.buildBaseURL(siteKey, container, router));
    if (urlBuilder.length() <= 1) {
      return urlBuilder.toString();
    } else {
      return urlBuilder.append(URL_PROJECT_DETAIL)
          .append(projectId)
          .toString();
    }
  }

  public static long getProjectIdFromURI(String requestPath) {
    long projectId = -1;
    int index = requestPath.indexOf(URL_PROJECT_DETAIL);
    if (index >= 0) {
      index = index + URL_PROJECT_DETAIL.length();
      int lastIndex = requestPath.indexOf('/', index);
      String id;
      if (lastIndex > 0) {
        id = requestPath.substring(index, lastIndex);
      } else {
        id = requestPath.substring(index);
      }
      try {
        projectId = Long.parseLong(id);
      } catch (NumberFormatException ex) {
        projectId = -100;
      }
    }
    return projectId;
  }

  public static Project newProjectInstance(String name, String description, String username) {
    Set<String> managers = new HashSet<String>();
    managers.add(username);
    return newProjectInstance(name, description, managers, Collections.<String>emptySet());
  }

  public static ProjectDto newProjectInstanceDto(String name, String description, String username) {
    Set<String> managers = new HashSet<String>();
    managers.add(username);
    return newProjectInstanceDto(name, description, managers, Collections.<String>emptySet());
  }

  public static Project newProjectInstance(String name, String description, Set<String> managers, Set<String> participators) {
    //CKEditor encode user input already, but we still need to remove malicious code
    //here in case user inject request using curl TA-387
    description = StringUtil.encodeInjectedHtmlTag(description);
    Project p = new Project(name, description, new HashSet<Status>(), managers, participators);
    return p;
  }

  public static ProjectDto newProjectInstanceDto(String name, String description, Set<String> managers, Set<String> participators) {
    //CKEditor encode user input already, but we still need to remove malicious code
    //here in case user inject request using curl TA-387
    description = StringUtil.encodeInjectedHtmlTag(description);
    ProjectDto p = new ProjectDto(name, description, new HashSet<StatusDto>(), managers, participators);
    return p;
  }

  public static ProjectDto saveProjectField(ProjectService projService, long projectId, Map<String, String[]> fields)
      throws EntityNotFoundException, ParameterEntityException {

    ProjectDto project = projService.getProject(projectId);

    // Load 'manager' and 'participator'
    project.setManager(projService.getManager(projectId));
    project.setParticipator(projService.getParticipator(projectId));

    //
    for (Map.Entry<String, String[]> field : fields.entrySet()) {
      String fieldName = field.getKey();
      String[] values = field.getValue();
      
      String val = values != null && values.length > 0 ? values[0] : null;
      
      if("name".equalsIgnoreCase(fieldName)) {
        if(val == null || val.isEmpty()) {
          LOG.info("Name of project must not empty");
          throw new ParameterEntityException(projectId, Project.class, fieldName, val, "must not be empty", null);
        }
        project.setName(val);
      } else if("manager".equalsIgnoreCase(fieldName)) {
        Set<String> manager = new HashSet<String>();
        if(values != null) {
          for (String v : values) {
            manager.add(v);
          }
        }
        project.setManager(manager);
      } else if("participator".equalsIgnoreCase(fieldName)) {
        Set<String> participator = new HashSet<String>();
        if(values != null) {
          for (String v : values) {
            participator.add(v);
          }
        }
        project.setParticipator(participator);
      } else if("dueDate".equalsIgnoreCase(fieldName)) {
        if(val == null || val.isEmpty()) {
          project.setDueDate(null);
        } else {
          DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
          try {
            Date date = df.parse(val);
            project.setDueDate(date);
          } catch (ParseException e) {
            LOG.info("can not parse date string: " + val);
            throw new ParameterEntityException(projectId, Project.class, fieldName, val, "cannot be parse to date", e);
          }
        }
      } else if("description".equalsIgnoreCase(fieldName)) {
        //CKEditor encode user input already, but we still need to remove malicious code
        //here in case user inject request using curl TA-387
        val = StringUtil.encodeInjectedHtmlTag(val);
        project.setDescription(val);
      } else if ("color".equalsIgnoreCase(fieldName)) {
        project.setColor(val);
      } else if ("parent".equalsIgnoreCase(fieldName)) {
        try {
          long pId = Long.parseLong(val);
          if (pId == 0) {
            project.setParent(null);
          } else if (pId == project.getId()) {
            throw new ParameterEntityException(pId, Project.class, fieldName, val, "project can not be child of itself", null);
          } else {
            ProjectDto parent = projService.getProject(pId);
            project.setParent(parent);
          }
        } catch (NumberFormatException ex) {
          LOG.info("can not parse date string: " + val);
          throw new ParameterEntityException(projectId, Project.class, fieldName, val, "cannot be parse to Long", ex);
        }
      } else {
        LOG.info("Field name: " + fieldName + " is not supported for entity Project");
        throw new ParameterEntityException(projectId, Project.class, fieldName, val, "is not supported for the entity Project", null);
      }      
    }

    return project;
  }
/*
  public static ProjectDto saveProjectField(org.exoplatform.task.service.ProjectService projService, long projectId, Map<String, String[]> fields)
          throws EntityNotFoundException, ParameterEntityException {

    ProjectDto project = projService.getProject(projectId);

    // Load 'manager' and 'participator'
    project.setManager(projService.getManager(projectId));
    project.setParticipator(projService.getParticipator(projectId));

    //
    for (Map.Entry<String, String[]> field : fields.entrySet()) {
      String fieldName = field.getKey();
      String[] values = field.getValue();

      String val = values != null && values.length > 0 ? values[0] : null;

      if("name".equalsIgnoreCase(fieldName)) {
        if(val == null || val.isEmpty()) {
          LOG.info("Name of project must not empty");
          throw new ParameterEntityException(projectId, Project.class, fieldName, val, "must not be empty", null);
        }
        project.setName(val);
      } else if("manager".equalsIgnoreCase(fieldName)) {
        Set<String> manager = new HashSet<String>();
        if(values != null) {
          for (String v : values) {
            manager.add(v);
          }
        }
        project.setManager(manager);
      } else if("participator".equalsIgnoreCase(fieldName)) {
        Set<String> participator = new HashSet<String>();
        if(values != null) {
          for (String v : values) {
            participator.add(v);
          }
        }
        project.setParticipator(participator);
      } else if("dueDate".equalsIgnoreCase(fieldName)) {
        if(val == null || val.isEmpty()) {
          project.setDueDate(null);
        } else {
          DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
          try {
            Date date = df.parse(val);
            project.setDueDate(date);
          } catch (ParseException e) {
            LOG.info("can not parse date string: " + val);
            throw new ParameterEntityException(projectId, Project.class, fieldName, val, "cannot be parse to date", e);
          }
        }
      } else if("description".equalsIgnoreCase(fieldName)) {
        //CKEditor encode user input already, but we still need to remove malicious code
        //here in case user inject request using curl TA-387
        val = StringUtil.encodeInjectedHtmlTag(val);
        project.setDescription(val);
      } else if ("color".equalsIgnoreCase(fieldName)) {
        project.setColor(val);
      } else if ("parent".equalsIgnoreCase(fieldName)) {
        try {
          long pId = Long.parseLong(val);
          if (pId == 0) {
            project.setParent(null);
          } else if (pId == project.getId()) {
            throw new ParameterEntityException(pId, Project.class, fieldName, val, "project can not be child of itself", null);
          } else {
            ProjectDto parent = projService.getProject(pId);
           // project.setParent(parent);
          }
        } catch (NumberFormatException ex) {
          LOG.info("can not parse date string: " + val);
          throw new ParameterEntityException(projectId, Project.class, fieldName, val, "cannot be parse to Long", ex);
        }
      } else {
        LOG.info("Field name: " + fieldName + " is not supported for entity Project");
        throw new ParameterEntityException(projectId, Project.class, fieldName, val, "is not supported for the entity Project", null);
      }
    }

    return project;
  }
  */
  public static Set<String> getParticipator(long projectId) {
    ProjectService service = getProjectService();
    return service.getParticipator(projectId);
  }

  public static Set<String> getManager(long projectId) {
    ProjectService service = getProjectService();
    return service.getManager(projectId);
  }

  public static boolean isCurrentUserHasNoProject() {
    ProjectService projectService = getProjectService();
    Identity identity = ConversationState.getCurrent().getIdentity();
    List<String> memberships = new LinkedList<String>();
    memberships.addAll(UserUtil.getMemberships(identity));

    List<ProjectDto> list = projectService.findProjects(memberships,"",null, 0, -1);
    return (list.size() == 0);
  }

  private static ProjectService getProjectService() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    return container.getComponentInstanceOfType(ProjectService.class);
  }

  private static class ProjectProxy extends ProjectDto {
    private ProjectDto project;
    private boolean editable;
    private Identity identity;

    public ProjectProxy(ProjectDto project, Identity identity, boolean editable) {
      this.project = project;
      this.editable = editable;
      this.identity = identity;
    }

    public long getId() {
      return project.getId();
    }

    public void setId(long id) {
      project.setId(id);
    }

    public String getName() {
      return project.getName();
    }

    public void setName(String name) {
      project.setName(name);
    }

    public Set<StatusDto> getStatus() {
      return project.getStatus();
    }

    public void setStatus(Set<StatusDto> status) {
      project.setStatus(status);
    }

    public Set<String> getParticipator() {
      return project.getParticipator();
    }

    public void setParticipator(Set<String> participator) {
      project.setParticipator(participator);
    }

    public Set<String> getManager() {
      return project.getManager();
    }

    public void setManager(Set<String> manager) {
      project.setManager(manager);
    }

    public String getDescription() {
      return project.getDescription();
    }

    public void setDescription(String description) {
      project.setDescription(description);
    }

    public Date getDueDate() {
      return project.getDueDate();
    }

    public void setDueDate(Date dueDate) {
      project.setDueDate(dueDate);
    }

    public String getColor() {
      return project.getColor();
    }

    public void setColor(String color) {
      project.setColor(color);
    }

    public ProjectDto getParent() {
      return project.getParent();
    }

    public void setParent(ProjectDto parent) {
      project.setParent(parent);
    }

    public List<ProjectDto> getChildren() {
      return project.getChildren();
    }
    public void setChildren(List<ProjectDto> children) {
      project.setChildren(children);
    }

    public ProjectDto clone(boolean cloneTask) {
      return project.clone(cloneTask);
    }

    public boolean canView(Identity user) {
      if (user != null && user.getUserId().equals(identity.getUserId())) {
        return true;
      } else {
        return project.canView(user);
      }
    }

    public boolean canEdit(Identity user) {
      if (user != null && user.getUserId().equals(identity.getUserId())) {
        return editable;
      } else {
        return project.canEdit(user);
      }
    }

    public int hashCode() {
      return project.hashCode();
    }

    public boolean equals(Object obj) {
      return project.equals(obj);
    }

    public String toString() {
      return project.toString();
    }    
  }

  private static class ProjectProxyDto extends ProjectDto {
    private ProjectDto project;
    private boolean editable;
    private Identity identity;


    public ProjectProxyDto(ProjectDto project, Identity identity, boolean editable) {
      this.project = project;
      this.editable = editable;
      this.identity = identity;
    }


    public long getId() {
      return project.getId();
    }

    public void setId(long id) {
      project.setId(id);
    }

    public String getName() {
      return project.getName();
    }

    public void setName(String name) {
      project.setName(name);
    }

    public Set<StatusDto> getStatus() {
      return project.getStatus();
    }

    public void setStatus(Set<StatusDto> status) {
      project.setStatus(status);
    }

    public Set<String> getParticipator() {
      return project.getParticipator();
    }

    public void setParticipator(Set<String> participator) {
      project.setParticipator(participator);
    }

    public Set<String> getManager() {
      return project.getManager();
    }

    public void setManager(Set<String> manager) {
      project.setManager(manager);
    }

    public String getDescription() {
      return project.getDescription();
    }

    public void setDescription(String description) {
      project.setDescription(description);
    }

    public Date getDueDate() {
      return project.getDueDate();
    }

    public void setDueDate(Date dueDate) {
      project.setDueDate(dueDate);
    }

    public String getColor() {
      return project.getColor();
    }

    public void setColor(String color) {
      project.setColor(color);
    }

    public ProjectDto getParent() {
      return project.getParent();
    }

    public void setParent(ProjectDto parent) {
      project.setParent(parent);
    }

    public List<ProjectDto> getChildren() {
      return project.getChildren();
    }

    public void setChildren(List<ProjectDto> children) {
      project.setChildren(children);
    }

    public ProjectDto clone(boolean cloneTask) {
      return project.clone(cloneTask);
    }

    public boolean canView(Identity user) {
        return project.canView(user);
    }

    public boolean canEdit(Identity user) {
        return project.canEdit(user);
    }

    public int hashCode() {
      return project.hashCode();
    }

    public boolean equals(Object obj) {
      return project.equals(obj);
    }

    public String toString() {
      return project.toString();
    }
  }

}
