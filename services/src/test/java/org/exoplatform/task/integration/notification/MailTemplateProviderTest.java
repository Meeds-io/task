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

import org.mockito.Mockito;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.task.service.UserService;

import junit.framework.TestCase;

public class MailTemplateProviderTest extends TestCase {

  public void testGetExcerptPreserveHtmlWithMentions() throws Exception {
    UserService userService = Mockito.mock(UserService.class);
    InitParams initParams = new InitParams();
    MailTemplateProvider provider = new MailTemplateProvider(initParams, userService);

    java.lang.reflect.Method method =
            MailTemplateProvider.class.getDeclaredMethod("getExcerptPreserveHtml", String.class, int.class);
    method.setAccessible(true);

    String htmlComment =
            "Hello <a class=\"mention\" data-user=\"userX\">@userX</a>, " +
                    "this is a <b>test</b> comment for email notification.";

    String excerpt = (String) method.invoke(provider, htmlComment, 18);

    assertNotNull(excerpt);

    assertTrue(excerpt.contains("Hello"));
    assertTrue(excerpt.contains("<a class=\"mention\" data-user=\"userX\">@userX</a>"));

    assertTrue(excerpt.endsWith("..."));
  }
}
