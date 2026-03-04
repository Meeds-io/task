package org.exoplatform.task.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.TaskUtil;
import org.exoplatform.web.filter.Filter;

import java.io.IOException;

public class TaskRedirectHandler implements Filter {

  @SneakyThrows
  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain) {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String uri = httpRequest.getRequestURI();

    if (uri == null) {
      chain.doFilter(request, response);
      return;
    }

    Identity identity = ConversationState.getCurrent().getIdentity();
    if (uri.contains("/tasks/taskDetail/")) {
      Long taskId = extractId(uri);
      TaskService taskService = ExoContainerContext
        .getCurrentContainer()
        .getComponentInstanceOfType(TaskService.class);
      TaskDto task = taskService.getTask(taskId);

      if (task != null && !TaskUtil.hasViewPermission(taskService, task)) {
        redirectToRestricted(httpRequest, httpResponse);
        return;
      }
    }

    if (uri.contains("/tasks/projectDetail/")) {
      Long projectId = extractId(uri);
      ProjectService projectService = ExoContainerContext
        .getCurrentContainer()
        .getComponentInstanceOfType(ProjectService.class);
      ProjectDto project = projectService.getProject(projectId);

      if (project != null && !project.canView(identity)) {
        redirectToRestricted(httpRequest, httpResponse);
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private Long extractId(String uri) {
    return Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
  }

  private void redirectToRestricted(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.sendRedirect(request.getContextPath() + "/dw/restricted-project");
  }
}