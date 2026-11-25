/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.task.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.CommentService;

import io.meeds.portal.plugin.AclPlugin;
import io.meeds.social.reaction.service.ReactionService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TaskCommentReactionObjectPluginTest {

  @Mock
  private ReactionService                reactionService;

  @Mock
  private CommentService                 commentService;

  @Mock
  private UserACL                        userAcl;

  @InjectMocks
  private TaskCommentReactionObjectPlugin plugin;

  @Test
  public void registersItselfAndTellsItsObjectType() {
    plugin.init();

    verify(reactionService).addObjectPlugin(plugin);
    assertEquals("taskComment", plugin.getObjectType());
  }

  @Test
  public void metadataObjectOfACommentHasItsTaskAsParent() {
    CommentDto comment = new CommentDto();
    TaskDto task = new TaskDto();
    task.setId(9L);
    comment.setTask(task);
    when(commentService.getComment(5L)).thenReturn(comment);

    assertEquals(new MetadataObject("taskComment", "5", "9"), plugin.getMetadataObject("5"));
  }

  @Test
  public void metadataObjectIsNullWhenTheCommentIsUnknownOrHasNoTask() {
    assertNull(plugin.getMetadataObject("5"));
    when(commentService.getComment(6L)).thenReturn(new CommentDto());
    assertNull(plugin.getMetadataObject("6"));
    assertNull(plugin.getMetadataObject("not-a-number"));
  }

  @Test
  public void viewPermissionIsTheOneOfTheCommentAcl() {
    Identity viewer = new Identity("john");
    when(userAcl.hasPermission("taskComment", "5", AclPlugin.VIEW_PERMISSION_TYPE, viewer)).thenReturn(true);

    assertTrue(plugin.hasViewPermission("5", viewer));
    assertFalse(plugin.hasViewPermission("6", viewer));
    assertFalse(plugin.hasViewPermission("5", mockOther()));
  }

  private Identity mockOther() {
    return new Identity("jane");
  }

}
