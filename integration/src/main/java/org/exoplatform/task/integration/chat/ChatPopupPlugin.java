/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.task.integration.chat;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;

import org.exoplatform.commons.api.ui.*;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.dao.ProjectQuery;
import org.exoplatform.task.domain.*;
import org.exoplatform.task.integration.ActivityTaskProcessor;
import org.exoplatform.task.model.User;
import org.exoplatform.task.service.*;
import org.exoplatform.task.util.*;

@SuppressWarnings("unchecked")
public class ChatPopupPlugin extends BaseUIPlugin {

  private static final String PREFIX = "++";

  private static Log          log                       =
                                  ExoLogger.getExoLogger(ChatPopupPlugin.class);

  private static final String TYPE                      = "type";

  private static final String CREATE_TASK_ACTION        = "createTask";

  private static final String CREATE_TASK_INLINE_ACTION = "createTaskInline";

  private ProjectService      projectService;

  private StatusService       statusService;

  private SpaceService        spaceService;

  private TaskService         taskService;

  private UserService         userService;

  private TaskParser          taskParser;

  private String              pluginType                = "chat_extension_popup";
  
  private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");

  public ChatPopupPlugin(InitParams params,
                         ProjectService projectService,
                         StatusService statusService,
                         SpaceService spaceService,
                         TaskService taskService,
                         UserService userService,
                         TaskParser taskParser) {
    this.projectService = projectService;
    this.statusService = statusService;
    this.taskService = taskService;
    this.spaceService = spaceService;
    this.userService = userService;
    this.taskParser = taskParser;

    ValueParam param = params.getValueParam(TYPE);
    if (param != null) {
      pluginType = param.getValue();
    }
  }

  @Override
  public Response render(RenderContext renderContext) {
    return new Response(new byte[0], MediaType.TEXT_PLAIN);
  }

  @Override
  public Response processAction(ActionContext context) {
    Map<String, List<String>> params = context.getParams();
    String actionName = context.getName();
    String creator = ConversationState.getCurrent().getIdentity().getUserId();
    String taskInput = getParam("text", params);
    String dueDateInput = getParam("dueDate", params);

    Status status = getStatus(params);
        
    if (CREATE_TASK_ACTION.equals(actionName)) {
      String username = getParam("username", params);
      username = StringUtils.isEmpty(username) ? creator : username;

      sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
      Date dueDate = null;
      if(StringUtils.isNotBlank(dueDateInput)) {
        try {
          dueDate = sdf.parse(dueDateInput + " 23:59");
        } catch (Exception ex) {
          log.error("Cannot parse due date " + dueDateInput + " : " + ex.getMessage(), ex);
        }
      }

      ObjectMapper mapper = new ObjectMapper();
      byte[] result = null;
      for (String name : username.split(",")) {
        Task task = new Task();
        task.setAssignee(name);
        task.setTitle(taskInput);
        task.setDueDate(dueDate);
        task.setCreatedBy(creator);
        task.setCreatedTime(new Date());
        task.setStatus(status);
        task = taskService.createTask(task);
        try {
          result = mapper.writeValueAsBytes(buildJSON(task));
        } catch (Exception e) {
          log.error("Problem when adding task in chat", e);
          return null;
        }
      }
      return new Response(result, MediaType.APPLICATION_JSON);
    } else if (CREATE_TASK_INLINE_ACTION.equals(actionName)) {
      ParserContext parserCtx = new ParserContext(userService.getUserTimezone(creator));
      Task task = parseText(taskInput, parserCtx);
      if (task != null) {
        task.setCreatedBy(creator);
        task.setCreatedTime(new Date());
        task.setStatus(status);
        taskService.createTask(task);
        return new Response(buildJSON(task).toJSONString().getBytes(), MediaType.APPLICATION_JSON);
      } else {
        return null;
      }
    } else {
      return null;      
    }
  }

  private JSONObject buildJSON(Task task) {
    JSONObject json = new JSONObject();
    json.put("title", task.getTitle());
    json.put("url", TaskUtil.buildTaskURL(task));
    if (task.getAssignee() != null) {
      User user = userService.loadUser(task.getAssignee());
      if (user != null) {
        json.put("assignee", user.getUsername());
        json.put("fullName", String.format("%s %s", user.getFirstName(), user.getLastName()));
      }
    }
    if (task.getDueDate() != null) {
      json.put("dueDate", sdf.format(task.getDueDate()));      
    }
    return json;
  }

  private Status getStatus(Map<String, List<String>> params) {
    String creator = ConversationState.getCurrent().getIdentity().getUserId();
    String roomName = getParam("roomName", params);
    String isSpace = getParam("isSpace", params);
    String isTeam = getParam("isTeam", params);
    String participants = getParam("participants", params);

    Project project = null;
    
    // find default project of space
    if ("true".equals(isSpace)) {
      log.debug("creating task in space {}", roomName);
      
      if (StringUtils.isNotEmpty(roomName)) {
        Space space = spaceService.getSpaceByPrettyName(roomName);
        if (space != null) {
          List<Project> projects = ProjectUtil.getProjectTree(space.getGroupId(), projectService);
          if (projects != null && projects.size() > 0) {
            project = projects.get(0);
          } else {
            log.warn("no project found for space {}, task will be created as incoming", space.getId());
          }
        } else {
          log.warn("space {} is null, can't add task to space's default project", roomName);
        }
      }
    } else if ("true".equals(isTeam)) {
      log.debug("creating task in team project {}", roomName);
      
      ProjectQuery query = new ProjectQuery();
      query.setKeyword(roomName);
      ListAccess<Project> projects = projectService.findProjects(query);
      try {
        if (projects.getSize() > 0) {
          project = projects.load(0, 1)[0];
        } else {
          Set<String> mans = new HashSet<String>(Arrays.asList(creator));
          Set<String> pars = new HashSet<String>(Arrays.asList(participants.split(",")));
          
          project = ProjectUtil.newProjectInstance(roomName, "", mans, pars);
          projectService.createProject(project);
          statusService.createInitialStatuses(project);
        }
      } catch (Exception ex) {
        log.error(ex.getMessage(), ex);
      }
    }

    if (project != null) {
      return statusService.getDefaultStatus(project.getId());      
    } else {
      return null;
    }
  }

  private String getParam(String name, Map<String, List<String>> params) {
    if (params.get(name) != null && params.get(name).size() > 0) {
      return params.get(name).get(0);
    }
    return null;
  }

  @Override
  public String getType() {
    return pluginType;
  }

  private Task parseText(String txt, ParserContext context) {
    if (txt != null && !txt.isEmpty()) {
      int idx = txt.indexOf(PREFIX);
      //
      if (idx >= 0 && idx + 2 < txt.length() - 1) {
        txt = ActivityTaskProcessor.decode(txt);
        txt = StringEscapeUtils.unescapeHtml(txt);
        String text = txt.substring(idx + 2);
        text = text.replaceFirst("<br(\\s*\\/?)>", "\n");

        String title, description;
        int index = text.indexOf("\n");
        if (index > 1) {
          title = text.substring(0, index);
          description = text.substring(index).trim();
        } else {
          title = text.trim();
          description = "";
        }
        Task task = taskParser.parse(title, context);
        //we need to remove malicious code here in case user inject request using curl TA-387
        task.setDescription(StringUtil.encodeInjectedHtmlTag(description));
        return task;
      }
    }
    return null;
  }
}
