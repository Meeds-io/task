/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.mail.MailPalette;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.task.service.UserService;

import groovy.text.GStringTemplateEngine;

/**
 * The task mails are written on the recipient's palette: the templates
 * compile with the engine of the mail channel, keep no palette colour as a
 * literal and print the bound palette, and the builder binds the palette of
 * the recipient and writes the comment, user content, for the recipient's
 * display mode. The templates live in the <code>webapps</code> module and are
 * read from the source tree of the reactor.
 */
public class TaskMailPaletteTest {

  private static final Path    TEMPLATES       = Paths.get("../webapps/src/main/webapp/WEB-INF/notification/templates/mail");

  // the light literals the palette replaces; white on a coloured button stays as written, and the footer label's link
  // colour is replaced where the label is printed
  private static final Pattern PALETTE_LITERAL = Pattern.compile("(?i)(bgcolor=\"|background-color:\\s*)#(ffffff|f9f9f9|f1f1f1|567ab6)"
      + "|(?<![a-z-])(?<!replace\\(\"color: )color:\\s*#(333333|999999)|(?<!replace\\(\"color: )#2f5e92|border[a-z-]*:\\s*\\d+px solid\\s*#(d8d8d8|224886)");

  @Test
  public void everyMailTemplateCompilesAndReadsThePalette() throws Exception {
    List<Path> templates = templates();
    assertEquals("the seven task mails", 7, templates.size());
    for (Path template : templates) {
      String text = Files.readString(template, StandardCharsets.UTF_8);
      new GStringTemplateEngine().createTemplate(text);
      assertFalse(template + " keeps a palette colour as a literal", PALETTE_LITERAL.matcher(text).find());
      assertTrue(template + " reads the palette", text.contains("MailPalette.of(_templateContext)"));
    }
  }

  @Test
  public void theBoundPaletteReachesEveryRenderedMail() throws Exception {
    Map<String, String> palette = new HashMap<>();
    palette.put("surface", "#1E2328");
    palette.put("background", "#14181B");
    palette.put("text", "#E6EAEE");
    palette.put("secondaryText", "#9AA4AD");
    palette.put("link", "#5FA9AC");
    palette.put("border", "#3A4148");
    palette.put("primary", "#3F8487");
    for (Path template : templates()) {
      String html = render(template, palette);
      assertTrue(template + ": " + html, html.contains("bgcolor=\"#1E2328\""));
      assertTrue(template + " link colour of the style block", html.contains("color: #5FA9AC;"));
      assertFalse(template + " keeps the light link colour", html.contains("#2f5e92"));
      assertFalse(template + " keeps a white surface", html.contains("bgcolor=\"#ffffff\""));
    }
  }

  @Test
  public void theBuilderBindsTheRecipientsPaletteAndColoursTheComment() throws Exception {
    TemplateContext templateContext = buildCommentMail("dark");
    assertEquals("dark", MailPalette.of(templateContext).get(MailPalette.MODE));
    assertEquals("#1E2328", MailPalette.of(templateContext).get("surface"));
    assertEquals("a black text is left to the palette's text colour",
                 "<p>black</p>",
                 templateContext.get("COMMENT_TEXT"));
  }

  @Test
  public void theCommentIsUntouchedForALightRecipient() throws Exception {
    TemplateContext templateContext = buildCommentMail("light");
    assertEquals("<p style=\"color:#000000\">black</p>", templateContext.get("COMMENT_TEXT"));
  }

  private TemplateContext buildCommentMail(String mode) {
    BrandingService brandingService = mock(BrandingService.class);
    when(brandingService.getDisplayMode("mary")).thenReturn(mode);
    when(brandingService.getMailPalette("dark".equals(mode))).thenReturn(new HashMap<>(Map.of("surface", "#1E2328")));
    IdentityManager identityManager = mock(IdentityManager.class);
    Identity identity = mock(Identity.class);
    Profile profile = mock(Profile.class);
    when(identityManager.getOrCreateIdentity(anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(identity);
    when(identityManager.getOrCreateIdentity(anyString(), anyString())).thenReturn(identity);
    when(identity.getProfile()).thenReturn(profile);
    when(identity.getRemoteId()).thenReturn("mary");
    when(profile.getFullName()).thenReturn("Mary Smith");
    when(profile.getProperty(Profile.FIRST_NAME)).thenReturn("Mary");

    NotificationInfo notification = NotificationInfo.instance().setTo("mary").key(PluginKey.key(TaskCommentPlugin.ID));
    notification.with(NotificationUtils.CREATOR.getKey(), "john")
                .with(NotificationUtils.TASK_TITLE, "Task")
                .with(NotificationUtils.COMMENT_TEXT, "<p style=\"color:#000000\">black</p>")
                .with(NotificationUtils.ACTION_NAME.getKey(), "comment");
    NotificationContext ctx = mock(NotificationContext.class);
    when(ctx.getNotificationInfo()).thenReturn(notification);
    AtomicReference<TemplateContext> rendered = new AtomicReference<>();

    try (MockedStatic<CommonsUtils> commonsUtils = mockStatic(CommonsUtils.class);
         MockedStatic<Utils> utils = mockStatic(Utils.class);
         MockedStatic<LinkProviderUtils> links = mockStatic(LinkProviderUtils.class);
         MockedStatic<NotificationPluginUtils> pluginUtils = mockStatic(NotificationPluginUtils.class);
         MockedStatic<MentionUtils> mentionUtils = mockStatic(MentionUtils.class);
         MockedStatic<TemplateUtils> templateUtils = mockStatic(TemplateUtils.class)) {
      commonsUtils.when(() -> CommonsUtils.getService(BrandingService.class)).thenReturn(brandingService);
      commonsUtils.when(() -> CommonsUtils.getService(IdentityManager.class)).thenReturn(identityManager);
      utils.when(Utils::getIdentityManager).thenReturn(identityManager);
      links.when(() -> LinkProviderUtils.getUserAvatarUrl(any())).thenReturn("/avatar");
      links.when(() -> LinkProviderUtils.getRedirectUrl(anyString(), anyString())).thenReturn("/redirect");
      links.when(LinkProviderUtils::getBaseUrl).thenReturn("http://host");
      pluginUtils.when(() -> NotificationPluginUtils.getLanguage("mary")).thenReturn("en");
      mentionUtils.when(() -> MentionUtils.substituteUsernames(anyString(), any(java.util.Locale.class))).thenAnswer(call -> call.getArgument(0));
      templateUtils.when(() -> TemplateUtils.processSubject(any(TemplateContext.class))).thenReturn("subject");
      templateUtils.when(() -> TemplateUtils.processGroovy(any(TemplateContext.class))).thenAnswer(call -> {
        rendered.set(call.getArgument(0));
        return "";
      });

      MailTemplateProvider provider = new MailTemplateProvider(new InitParams(), mock(UserService.class));
      MessageInfo message = provider.getTemplateBuilder().get(PluginKey.key(TaskCommentPlugin.ID)).buildMessage(ctx);
      assertNotNull(message);
      assertNull(ctx.getException());
    }
    return rendered.get();
  }

  private String render(Path template, Map<String, String> palette) throws Exception {
    StringWriter writer = new StringWriter();
    Map<String, Object> templateContext = new HashMap<>();
    templateContext.put(MailPalette.CONTEXT_KEY, palette);
    Map<String, Object> binding = new HashMap<>();
    binding.put("_ctx", new TemplateContextStub());
    binding.put("_templateContext", templateContext);
    binding.put("_writer", writer);
    for (String name : List.of("AVATAR", "FIRST_NAME", "TASK_DESCRIPTION", "TASK_TITLE", "TASK_URL", "USER", "PROFILE_URL",
                               "PROJECT_NAME", "PROJECT_URL", "COMMENT_TEXT", "DUE_DATE", "FOOTER_LINK", "ACTION_NAME",
                               "AssignTask", "listOfCoworker", "coworkerTask", "usersMentioned")) {
      binding.put(name, name.toLowerCase());
    }
    new GStringTemplateEngine().createTemplate(Files.readString(template, StandardCharsets.UTF_8)).make(binding).writeTo(writer);
    return writer.toString();
  }

  private List<Path> templates() throws IOException {
    try (Stream<Path> files = Files.list(TEMPLATES)) {
      return files.filter(path -> path.toString().endsWith("Plugin.gtmpl")).toList();
    }
  }

  /**
   * Stands for the template context the engine gives a template: the labels
   * are echoed, the shared header and footer are not part of what is pinned.
   */
  public static class TemplateContextStub {

    public String appRes(String key, Object... args) {
      return "[" + key + "]";
    }

    public String escapeHTML(String text) {
      return text;
    }

    public void include(String path, Object templateContext) {
      // the shared header and footer are pinned by the commons module
    }
  }

}
