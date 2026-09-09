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
package org.exoplatform.task.integration.notification;

import java.util.*;

import org.gatein.common.text.EntityEncoder;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.plugin.SocialNotificationUtils;
import org.exoplatform.task.service.UserService;
import org.exoplatform.task.util.CommentUtil;
import org.exoplatform.task.util.TaskUtil;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = TaskAssignPlugin.ID, template = "war:/notification/templates/mail/TaskAssignPlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskCoworkerPlugin.ID, template = "war:/notification/templates/mail/TaskCoworkerPlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskDueDatePlugin.ID, template = "war:/notification/templates/mail/TaskDueDatePlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskCompletedPlugin.ID, template = "war:/notification/templates/mail/TaskCompletedPlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskCommentPlugin.ID, template = "war:/notification/templates/mail/TaskCommentPlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskMentionPlugin.ID, template = "war:/notification/templates/mail/TaskMentionPlugin.gtmpl"),
    @TemplateConfig(pluginId = TaskEditionPlugin.ID, template = "war:/notification/templates/mail/TaskEditionPlugin.gtmpl") })
public class MailTemplateProvider extends TemplateProvider {

  private UserService          userService;

  public MailTemplateProvider(InitParams initParams, UserService userService) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(TaskAssignPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskCoworkerPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskDueDatePlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskCompletedPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskCommentPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskMentionPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(TaskEditionPlugin.ID), new TemplateBuilder());
    this.userService = userService;
  }

  private class TemplateBuilder extends AbstractTemplateBuilder {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      EntityEncoder encoder = HTMLEntityEncoder.getInstance();

      NotificationInfo notification = ctx.getNotificationInfo();
      String language = getLanguage(notification);
      String notificationCreator = notification.getValueOwnerParameter(NotificationUtils.CREATOR.getKey());
      String projectName = notification.getValueOwnerParameter(NotificationUtils.PROJECT_NAME);
      String taskTitle = notification.getValueOwnerParameter(NotificationUtils.TASK_TITLE);
      String taskDesc = notification.getValueOwnerParameter(NotificationUtils.TASK_DESCRIPTION);
      String commentText = notification.getValueOwnerParameter(NotificationUtils.COMMENT_TEXT);
      String taskUrl = notification.getValueOwnerParameter(NotificationUtils.TASK_URL);
      String projectUrl = notification.getValueOwnerParameter(NotificationUtils.PROJECT_URL);
      String assignee = notification.getValueOwnerParameter(NotificationUtils.TASK_ASSIGNEE);
      String listOfCoworker = notification.getValueOwnerParameter(NotificationUtils.TASK_COWORKERS);
      commentText = CommentUtil.formatMention(getExcerptPreserveHtml(commentText, 130), language);
      String coworker = notification.getValueOwnerParameter(NotificationUtils.ADDED_COWORKER);
      String usersMentioned = notification.getValueOwnerParameter(NotificationUtils.MENTIONED_USERS);
      String actionName = notification.getValueOwnerParameter(NotificationUtils.ACTION_NAME.getKey());

      TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
      Identity author = CommonsUtils.getService(IdentityManager.class)
                                    .getOrCreateIdentity(OrganizationIdentityProvider.NAME, notificationCreator, true);
      Profile profile = author.getProfile();
      // creator
      String fullName = profile.getFullName();
      if(CommentUtil.isExternal(author.getRemoteId())) {
        fullName += " " + "(" + TaskUtil.getResourceBundleLabel(new Locale(TaskUtil.getUserLanguage(author.getRemoteId())), "external.label.tag") + ")";
      }
      templateContext.put("USER", encoder.encode(fullName));
      templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(profile));
      templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", author.getRemoteId()));
      // receiver
      Identity receiver = CommonsUtils.getService(IdentityManager.class)
                                      .getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getTo(), true);
      templateContext.put("FIRST_NAME", encoder.encode(receiver.getProfile().getProperty(Profile.FIRST_NAME).toString()));
      //
      templateContext.put("PROJECT_NAME", projectName == null ? "" : encoder.encode(projectName));
      templateContext.put("TASK_TITLE", encoder.encode(taskTitle));
      templateContext.put("TASK_DESCRIPTION", encoder.encode(getExcerpt(taskDesc, 130)));
      templateContext.put("COMMENT_TEXT", commentText == null ? "" : commentText);
      templateContext.put("DUE_DATE", getDueDate(notification));
      templateContext.put("TASK_URL", taskUrl);
      templateContext.put("PROJECT_URL", projectUrl);
      templateContext.put("AssignTask", assignee);
      templateContext.put("listOfCoworker", listOfCoworker);
      templateContext.put("coworkerTask", coworker);
      templateContext.put("usersMentioned", usersMentioned);
      //
      templateContext.put("ACTION_NAME", encoder.encode(actionName.toString()));

      SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);
      String subject = TemplateUtils.processSubject(templateContext);

      String body = TemplateUtils.processGroovy(templateContext);
      // binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.subject(subject).body(body).end();
    }

    protected String getDueDate(NotificationInfo notification) {
      String dueDate = notification.getValueOwnerParameter(NotificationUtils.DUE_DATE);
      if (dueDate != null) {
        Date date = new Date(Long.parseLong(dueDate));
        UserService userService = CommonsUtils.getService(UserService.class);
        return org.exoplatform.task.integration.notification.TemplateUtils.format(date,
                                                                                  userService.getUserTimezone(notification.getTo()));
      } else {
        return "";
      }
    }
  }

  public static String getExcerpt(String str, int len) {
    if (str == null) {
      return "";
    } else if (str.length() > len) {
      str = str.substring(0, len);
      int lastSpace = str.lastIndexOf(" ");
      return ((lastSpace > 0) ? str.substring(0, lastSpace) : str) + "...";
    } else {
      return str;
    }
  }

  private static String getExcerptPreserveHtml(String html, int maxTextLen) {
    if (html == null || maxTextLen <= 0) {
      return "";
    }

    StringBuilder out = new StringBuilder();
    Deque<String> openTags = new ArrayDeque<>();

    int textCount = 0;
    boolean inTag = false;
    boolean wasTruncated = false;

    StringBuilder currentTag = new StringBuilder();
    for (int i = 0; i < html.length() && !wasTruncated; i++) {
      char c = html.charAt(i);

      if (inTag) {
        currentTag.append(c);
        out.append(c);

        if (c == '>') {
          inTag = false;
          String tag = currentTag.toString();

          if (isOpeningTag(tag)) {
            String name = extractTagName(tag);
            openTags.push(name);
          } else if (isClosingTag(tag) && !openTags.isEmpty()) {
            openTags.pop();
          }
        }
      } else if (c == '<') {
        inTag = true;
        currentTag.setLength(0);
        currentTag.append(c);
        out.append(c);
      } else {
        out.append(c);
        textCount++;
        if (textCount >= maxTextLen) {
          wasTruncated = true;
        }
      }
    }

    while (!openTags.isEmpty()) {
      out.append("</").append(openTags.pop()).append(">");
    }

    String cut = out.toString();
    if (wasTruncated) {
      int lastSpace = cut.lastIndexOf(" ");
      if (lastSpace > 0) {
        cut = cut.substring(0, lastSpace);
      }
      cut = cut + "...";
    }
    return cut;
  }

  private static boolean isOpeningTag(String tag) {
    if (tag == null) return false;

    tag = tag.trim();
    if (tag.length() < 3 || tag.charAt(0) != '<' || tag.charAt(tag.length() - 1) != '>') {
      return false;
    }

    String inner = tag.substring(1, tag.length() - 1).trim();
    if (inner.isEmpty() || inner.charAt(0) == '/' || inner.charAt(0) == '!') {
      return false;
    }

    return !inner.endsWith("/");
  }

  private static boolean isClosingTag(String tag) {
    if (tag == null) {
      return false;
    }
    tag = tag.trim();
    return tag.startsWith("</") && tag.endsWith(">") && tag.length() > 3;
  }

  private static String extractTagName(String tag) {
    String inner = tag.substring(1, tag.length() - 1).trim();

    int spaceIndex = inner.indexOf(' ');
    int slashIndex = inner.indexOf('/');

    int endIndex = inner.length();
    if (spaceIndex > 0) {
      endIndex = Math.min(endIndex, spaceIndex);
    }
    if (slashIndex > 0) {
      endIndex = Math.min(endIndex, slashIndex);
    }

    return inner.substring(0, endIndex);
  }
}
