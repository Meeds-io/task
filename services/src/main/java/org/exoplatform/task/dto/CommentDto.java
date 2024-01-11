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
package org.exoplatform.task.dto;

import lombok.Data;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.domain.Task;

import java.io.Serializable;
import java.util.*;

@Data
public class CommentDto implements Serializable {

    private static final long serialVersionUID = 8506789057343074059L;

    private long id;

    private String author;

    private String comment;

    private Date createdTime;

    private TaskDto task;

    private CommentDto parentComment;

    private List<CommentDto> subComments;

    private Set<String> mentionedUsers;

    public CommentDto clone() {
        CommentDto commentDto=new CommentDto();
        commentDto.setId(this.getId());
        commentDto.setAuthor(this.getAuthor());
        commentDto.setComment(this.getComment());
        commentDto.setSubComments(this.getSubComments());
        commentDto.setCreatedTime(this.getCreatedTime());
        commentDto.setTask(this.getTask().clone());

        return commentDto;
    }

}
