/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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

package org.exoplatform.task.dao.jpa;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.task.dao.CommentHandler;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.domain.Task;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class CommentDAOImpl extends GenericDAOJPAImpl<Comment, Long> implements CommentHandler {

  private EntityManagerService entityService;

  public CommentDAOImpl(EntityManagerService entityService) {
    this.entityService = entityService;
  }
  
  @Override
  public EntityManager getEntityManager() {
    return entityService.getEntityManager();
  }

  @Override
  public long count(Task task) {
    Long count = getEntityManager().createNamedQuery("Comment.countCommentOfTask", Long.class)
            .setParameter("taskId", task.getId())
            .getSingleResult();
    return count;
  }

  @Override
  public List<Comment> findCommentsOfTask(Task task, int start, int limit) {
    Query query = getEntityManager().createNamedQuery("Comment.findCommentsOfTask", Comment.class);
    query.setParameter("taskId", task.getId());
    if(limit > 0) {
      query.setFirstResult(start);
      query.setMaxResults(limit);
    }
    List<Comment> comments = query.getResultList();

    //TODO: should re-order by java code or by query
    Collections.reverse(comments);

    return comments;
  }
}
