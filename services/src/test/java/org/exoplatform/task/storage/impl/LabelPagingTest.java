/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.task.storage.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.task.dao.CommentHandler;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.dao.LabelHandler;
import org.exoplatform.task.domain.Label;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.dto.LabelDto;
import org.exoplatform.task.service.impl.CommentServiceImpl;
import org.exoplatform.task.service.impl.LabelServiceImpl;
import org.exoplatform.task.storage.CommentStorage;
import org.exoplatform.task.storage.LabelStorage;
import org.exoplatform.task.storage.ProjectStorage;

/**
 * The paging, the search and the counting of labels, and the counting of the
 * comments of several tasks, from the service down to the DAO handlers.
 */
public class LabelPagingTest {

  private LabelHandler     labelHandler;

  private CommentHandler   commentHandler;

  private LabelStorageImpl labelStorage;

  private Project          project;

  @Before
  public void init() {
    DAOHandler daoHandler = mock(DAOHandler.class);
    labelHandler = mock(LabelHandler.class);
    commentHandler = mock(CommentHandler.class);
    when(daoHandler.getLabelHandler()).thenReturn(labelHandler);
    when(daoHandler.getCommentHandler()).thenReturn(commentHandler);
    labelStorage = new LabelStorageImpl(daoHandler);
    project = new Project();
    project.setId(5L);
    project.setName("project");
  }

  @SuppressWarnings("unchecked")
  private ListAccess<Label> access(int size, Label... page) throws Exception {
    ListAccess<Label> access = mock(ListAccess.class);
    when(access.load(anyInt(), anyInt())).thenReturn(page);
    when(access.getSize()).thenReturn(size);
    return access;
  }

  @Test
  public void searchesTheLabelsOfAUserByKeyword() throws Exception {
    ListAccess<Label> searched = access(7, new Label("Bug", "root", project));
    ListAccess<Label> all = access(3, new Label("a", "root", project), new Label("b", "root", project));
    when(labelHandler.findLabelsByUser("root", "bug")).thenReturn(searched);
    when(labelHandler.findLabelsByUser("root")).thenReturn(all);

    List<LabelDto> hits = labelStorage.findLabelsByUser("root", "bug", 0, 20);

    assertEquals(1, hits.size());
    assertEquals("Bug", hits.get(0).getName());
    assertEquals(7, labelStorage.countLabelsByUser("root", "bug"));
    assertEquals(3, labelStorage.countLabelsByUser("root"));
    assertEquals(2, labelStorage.findLabelsByUser("root", 0, 20).size());
  }

  @Test
  public void searchesTheLabelsOfAProjectByKeyword() throws Exception {
    ListAccess<Label> searched = access(2, new Label("Bug", "root", project));
    ListAccess<Label> all = access(9);
    when(labelHandler.findLabelsByProject(5L, "bug")).thenReturn(searched);
    when(labelHandler.findLabelsByProject(5L)).thenReturn(all);

    List<LabelDto> hits = labelStorage.findLabelsByProject(5L, new Identity("root"), mock(ProjectStorage.class), "bug", 0, 20);

    assertEquals(1, hits.size());
    assertEquals(2, labelStorage.countLabelsByProject(5L, "bug"));
    assertEquals(9, labelStorage.countLabelsByProject(5L));
  }

  @Test
  public void aFailingSearchOrCountGivesNothingInsteadOfAnError() {
    when(labelHandler.findLabelsByUser("root", "x")).thenThrow(new IllegalStateException("db"));
    when(labelHandler.findLabelsByProject(5L, "x")).thenThrow(new IllegalStateException("db"));
    when(labelHandler.findLabelsByUser("root")).thenThrow(new IllegalStateException("db"));
    when(labelHandler.findLabelsByProject(5L)).thenThrow(new IllegalStateException("db"));

    assertTrue(labelStorage.findLabelsByUser("root", "x", 0, 20).isEmpty());
    assertTrue(labelStorage.findLabelsByProject(5L, new Identity("root"), mock(ProjectStorage.class), "x", 0, 20).isEmpty());
    assertEquals(0, labelStorage.countLabelsByUser("root", "x"));
    assertEquals(0, labelStorage.countLabelsByProject(5L, "x"));
    assertEquals(0, labelStorage.countLabelsByUser("root"));
    assertEquals(0, labelStorage.countLabelsByProject(5L));
  }

  @Test
  public void theLabelServiceDelegatesTheSearchAndTheCounts() {
    LabelStorage storage = mock(LabelStorage.class);
    ProjectStorage projectStorage = mock(ProjectStorage.class);
    LabelServiceImpl service = new LabelServiceImpl(storage, mock(DAOHandler.class), projectStorage, mock(ListenerService.class));
    Identity root = new Identity("root");
    LabelDto label = new LabelDto();
    when(storage.findLabelsByUser("root", "bug", 20, 20)).thenReturn(List.of(label));
    when(storage.findLabelsByProject(5L, root, projectStorage, "bug", 0, 20)).thenReturn(List.of(label));
    when(storage.countLabelsByUser("root", "bug")).thenReturn(4);
    when(storage.countLabelsByProject(5L, "bug")).thenReturn(6);
    when(storage.countLabelsByUser("root")).thenReturn(8);
    when(storage.countLabelsByProject(5L)).thenReturn(10);

    assertEquals(List.of(label), service.findLabelsByUser("root", "bug", 20, 20));
    assertEquals(List.of(label), service.findLabelsByProject(5L, root, "bug", 0, 20));
    assertEquals(4, service.countLabelsByUser("root", "bug"));
    assertEquals(6, service.countLabelsByProject(5L, "bug"));
    assertEquals(8, service.countLabelsByUser("root"));
    assertEquals(10, service.countLabelsByProject(5L));
  }

  @Test
  public void theCommentsOfSeveralTasksAreCountedThroughTheServiceAndTheStorage() {
    when(commentHandler.countCommentsByTasks(List.of(1L, 2L))).thenReturn(Map.of(1L, 3));
    ProjectStorage projectStorage = mock(ProjectStorage.class);
    DAOHandler daoHandler = mock(DAOHandler.class);
    when(daoHandler.getCommentHandler()).thenReturn(commentHandler);
    CommentStorage storage = new CommentStorageImpl(daoHandler, projectStorage);
    CommentServiceImpl service = new CommentServiceImpl(storage, mock(ListenerService.class));

    assertEquals(Map.of(1L, 3), storage.countCommentsByTasks(List.of(1L, 2L)));
    assertEquals(Map.of(1L, 3), service.countCommentsByTasks(List.of(1L, 2L)));
    verify(commentHandler, times(2)).countCommentsByTasks(List.of(1L, 2L));
  }
}
