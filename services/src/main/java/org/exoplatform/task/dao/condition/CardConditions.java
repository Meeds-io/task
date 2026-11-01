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
package org.exoplatform.task.dao.condition;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.domain.LabelTaskMapping;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;

/**
 * Builds the card criteria of a task query as {@link PredicateCondition}s:
 * the column by its name, the assignee and the coworkers, a date range on the
 * due or the start date, and the labels by their name (EXO-92467).
 * <p>
 * Every criterion over a collection (the coworkers, the labels) or another
 * entity (the column) is a subquery rather than a join on the query's root.
 * The task DAO selects and counts distinct tasks, so a join would not repeat
 * a card; the subquery keeps each criterion independent of the query's other
 * joins, lets two label criteria match two different labels of the same
 * card, and states the negations (« none of », no coworker) directly. The
 * column, the labels and « no coworker » are correlated EXISTS or NOT EXISTS
 * subqueries, which PostgreSQL turns into a semi-join or an anti-join. The
 * coworkers of « one of these users » are an uncorrelated IN over the cards'
 * ids instead: beside the assignee, under an OR, PostgreSQL cannot turn a
 * correlated EXISTS into a semi-join and runs it once per card, and
 * {@code TASK_TASK_COWORKERS} has no index on its task there, so each run read
 * the whole table (15.8 s at the forms design bound, EXO-92467); the
 * uncorrelated set is read once, through the index on the coworker. That set
 * stays small, the cards of at most a few users, where the set of every card
 * with a coworker would not: past the memory PostgreSQL hashes a NOT IN's set
 * in, it reads that set again for each card. Each condition only narrows the
 * query it is added to, and is never placed under an OR.
 */
public final class CardConditions {

  /** The prefix of the group id of a space. */
  private static final String SPACE_GROUP_PREFIX = "/spaces/";

  /** Matches a project permission that names a space group, whatever its role. */
  private static final String SPACE_PERMISSION   = "%:" + SPACE_GROUP_PREFIX + "%";

  private CardConditions() {
  }

  /**
   * Selects the cards a reader may see because a comment mentions them: by
   * the rule of {@code TaskUtil#hasViewPermission}, a mention counts on a card
   * with no project, on a card whose project names no space group among its
   * managers and participators, and on a card whose project names a space
   * group the reader is a member of, with any role. A mention never opens a
   * card to someone outside its space. Unlike the other criteria of this
   * class, it is meant to sit under the OR of a reader's accessible cards.
   *
   * @param reader the reader, with their memberships
   * @return the condition
   */
  public static PredicateCondition<Task> mentionedReader(Identity reader) {
    if (reader == null || reader.getUserId() == null) {
      throw new IllegalArgumentException("A mention criterion needs its reader");
    }
    String username = reader.getUserId();
    List<String> spaceGroups = reader.getMemberships()
                                     .stream()
                                     .map(MembershipEntry::getGroup)
                                     .filter(group -> group != null && group.startsWith(SPACE_GROUP_PREFIX))
                                     .distinct()
                                     .toList();
    return new PredicateCondition<>((cb, query, root) -> {
      Subquery<Long> mentions = query.subquery(Long.class);
      Root<Comment> comment = mentions.from(Comment.class);
      Join<Comment, String> mentioned = comment.join("mentionedUsers");
      mentions.select(comment.get("id"))
              .where(cb.equal(comment.get("task").get("id"), root.get("id")), cb.equal(mentioned, username));

      Path<Object> status = root.get("status");
      Predicate inSpace = cb.or(cb.isNull(status),
                                cb.not(cb.or(cb.exists(projectPermissions(cb, query, status, "manager", null)),
                                             cb.exists(projectPermissions(cb, query, status, "participator", null)))));
      if (!spaceGroups.isEmpty()) {
        inSpace = cb.or(inSpace,
                        cb.exists(projectPermissions(cb, query, status, "manager", spaceGroups)),
                        cb.exists(projectPermissions(cb, query, status, "participator", spaceGroups)));
      }
      return cb.and(cb.exists(mentions), inSpace);
    });
  }

  /**
   * Builds the subquery of the space permissions of the project of a card's
   * column, correlated on that column: the permissions of one kind that name
   * a space group, or, when groups are given, one of those groups.
   *
   * @param cb the criteria builder
   * @param query the query the subquery belongs to
   * @param status the column of the query's card
   * @param kind {@code manager} or {@code participator}
   * @param groups the space groups to look for, null for any space group
   * @return the subquery, one row per matching permission
   */
  private static Subquery<Long> projectPermissions(CriteriaBuilder cb,
                                                   CriteriaQuery<?> query,
                                                   Path<Object> status,
                                                   String kind,
                                                   List<String> groups) {
    Subquery<Long> permissions = query.subquery(Long.class);
    Root<Status> column = permissions.from(Status.class);
    Join<Status, Project> project = column.join("project");
    Join<Project, String> permission = project.join(kind);
    Predicate named;
    if (groups == null) {
      named = cb.like(permission, SPACE_PERMISSION);
    } else {
      Expression<String> group = cb.substring(permission, cb.sum(cb.locate(permission, ":"), 1));
      named = cb.and(cb.like(permission, SPACE_PERMISSION), group.in(groups));
    }
    permissions.select(column.get("id")).where(cb.equal(column.get("id"), status.get("id")), named);
    return permissions;
  }

  /**
   * Selects the cards whose column has one of the given names, or none of them.
   * Names are compared trimmed and ignoring case, so a name matches the column
   * of that name on every board of the query. A card on a board without such a
   * column is excluded by the first form and kept by the second, as is a card
   * with no column.
   *
   * @param names the column names, at least one, none blank
   * @param excluded true to keep the cards whose column has none of the names
   * @return the condition
   */
  public static PredicateCondition<Task> statusNames(Collection<String> names, boolean excluded) {
    List<String> normalized = normalizeNames(names, "status");
    return new PredicateCondition<>((cb, query, root) -> {
      Subquery<Long> statuses = query.subquery(Long.class);
      Root<Status> status = statuses.from(Status.class);
      statuses.select(status.get("id"))
              .where(cb.equal(status.get("id"), root.get("status").get("id")),
                     normalizedName(cb, status.get("name")).in(normalized));
      Predicate inColumn = cb.exists(statuses);
      return excluded ? cb.not(inColumn) : inColumn;
    });
  }

  /**
   * Selects the cards assigned to one of the given users, or, when coworkers
   * count, the cards on which one of them is the assignee or a coworker.
   *
   * @param usernames the users, at least one
   * @param includeCoworkers true to also select the cards one of them works on
   *          as a coworker
   * @return the condition
   */
  public static PredicateCondition<Task> assignedTo(Collection<String> usernames, boolean includeCoworkers) {
    if (usernames == null || usernames.isEmpty() || usernames.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("An assignee criterion needs at least one user");
    }
    List<String> users = List.copyOf(usernames);
    return new PredicateCondition<>((cb, query, root) -> {
      Predicate assignee = root.get("assignee").in(users);
      return includeCoworkers ? cb.or(assignee, root.get("id").in(coworkedCards(query, users))) : assignee;
    });
  }

  /**
   * Selects the cards with no assignee, and, when coworkers count, no coworker
   * either.
   *
   * @param includeCoworkers true to also require that the card has no coworker
   * @return the condition
   */
  public static PredicateCondition<Task> unassigned(boolean includeCoworkers) {
    return new PredicateCondition<>((cb, query, root) -> {
      Path<String> assignee = root.get("assignee");
      Predicate noAssignee = cb.or(cb.isNull(assignee), cb.equal(assignee, ""));
      return includeCoworkers ? cb.and(noAssignee, cb.not(cb.exists(coworkers(cb, query, root)))) : noAssignee;
    });
  }

  /**
   * Selects the cards whose date falls in a range, the start included and the
   * end excluded; a missing bound leaves that side open, and a card with no
   * date never matches.
   *
   * @param field the date field, {@code dueDate} or {@code startDate}
   * @param from the first instant of the range, included; null for no start
   * @param to the instant after the range, excluded; null for no end
   * @return the condition
   */
  public static PredicateCondition<Task> dateBetween(String field, Date from, Date to) {
    checkDateField(field);
    if (from == null && to == null) {
      throw new IllegalArgumentException("A date range needs at least one bound");
    }
    return new PredicateCondition<>((cb, query, root) -> {
      Path<Date> date = root.get(field);
      if (from == null) {
        return cb.lessThan(date, to);
      } else if (to == null) {
        return cb.greaterThanOrEqualTo(date, from);
      } else {
        return cb.and(cb.greaterThanOrEqualTo(date, from), cb.lessThan(date, to));
      }
    });
  }

  /**
   * Selects the cards with no value in a date field.
   *
   * @param field the date field, {@code dueDate} or {@code startDate}
   * @return the condition
   */
  public static PredicateCondition<Task> noDate(String field) {
    checkDateField(field);
    return new PredicateCondition<>((cb, query, root) -> cb.isNull(root.get(field)));
  }

  /**
   * Selects the cards that carry a label of the given name. Only the labels of
   * the card's own board and the viewer's personal labels count: another
   * user's personal label never matches, so a find cannot reveal it.
   *
   * @param name the label name, compared trimmed and ignoring case
   * @param viewer the user making the find
   * @return the condition
   */
  public static PredicateCondition<Task> hasLabel(String name, String viewer) {
    return hasAnyLabel(List.of(name), viewer, false);
  }

  /**
   * Selects the cards that carry a label with one of the given names, or none
   * of them. Only the labels of the card's own board and the viewer's personal
   * labels count, as for {@link #hasLabel(String, String)}.
   *
   * @param names the label names, at least one, none blank
   * @param viewer the user making the find
   * @param excluded true to keep the cards that carry none of these labels
   * @return the condition
   */
  public static PredicateCondition<Task> hasAnyLabel(Collection<String> names, String viewer, boolean excluded) {
    List<String> normalized = normalizeNames(names, "label");
    if (viewer == null || viewer.isBlank()) {
      throw new IllegalArgumentException("A label criterion needs the user making the find");
    }
    return new PredicateCondition<>((cb, query, root) -> {
      Subquery<Long> labels = query.subquery(Long.class);
      Root<LabelTaskMapping> mapping = labels.from(LabelTaskMapping.class);
      Join<?, ?> label = mapping.join("label");
      Path<Object> labelProject = label.get("project");
      Predicate boardLabel = cb.equal(labelProject.get("id"), mapping.get("task").get("status").get("project").get("id"));
      Predicate ownLabel = cb.and(cb.isNull(labelProject), cb.equal(label.get("username"), viewer));
      labels.select(label.get("id"))
            .where(cb.equal(mapping.get("task").get("id"), root.get("id")),
                   normalizedName(cb, label.get("name")).in(normalized),
                   cb.or(boardLabel, ownLabel));
      Predicate labelled = cb.exists(labels);
      return excluded ? cb.not(labelled) : labelled;
    });
  }

  /**
   * Builds the subquery of the ids of the cards some users work on as
   * coworkers. It is not correlated with the query's card, so the database
   * reads it once, through the index on the coworker.
   *
   * @param query the query the subquery belongs to
   * @param users the coworkers to look for
   * @return the subquery of the cards' ids
   */
  private static Subquery<Long> coworkedCards(CriteriaQuery<?> query, List<String> users) {
    Subquery<Long> cards = query.subquery(Long.class);
    Root<Task> card = cards.from(Task.class);
    Join<Task, String> coworker = card.join("coworker");
    cards.select(card.get("id")).where(coworker.in(users));
    return cards;
  }

  /**
   * Builds the subquery of a card's coworkers, correlated on the query's card,
   * for a NOT EXISTS the database runs as an anti-join.
   *
   * @param cb the criteria builder
   * @param query the query the subquery belongs to
   * @param root the card of that query
   * @return the subquery, one row per coworker of the card
   */
  private static Subquery<Long> coworkers(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Task> root) {
    Subquery<Long> coworkers = query.subquery(Long.class);
    Root<Task> card = coworkers.from(Task.class);
    card.join("coworker");
    coworkers.select(card.get("id")).where(cb.equal(card.get("id"), root.get("id")));
    return coworkers;
  }

  /**
   * Normalizes a name column the way {@link #normalizeNames} normalizes the
   * names compared with it.
   *
   * @param cb the criteria builder
   * @param name the name column
   * @return the trimmed, lower-case name
   */
  private static Expression<String> normalizedName(CriteriaBuilder cb, Path<String> name) {
    return cb.lower(cb.trim(name));
  }

  /**
   * Trims the names and turns them to lower case, refusing an empty list or a
   * blank name.
   *
   * @param names the names
   * @param what what the names name, for the message
   * @return the normalized names
   */
  private static List<String> normalizeNames(Collection<String> names, String what) {
    if (names == null || names.isEmpty() || names.stream().anyMatch(name -> name == null || name.isBlank())) {
      throw new IllegalArgumentException("A " + what + " criterion needs at least one name, none blank");
    }
    return names.stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).distinct().toList();
  }

  /**
   * Refuses a field that is not a card date a user sees: the due date or the
   * start date, never the legacy end date.
   *
   * @param field the field
   */
  private static void checkDateField(String field) {
    if (!Conditions.TASK_DUEDATE.equals(field) && !Conditions.TASK_START_DATE.equals(field)) {
      throw new IllegalArgumentException("A card date criterion applies to the due date or the start date, not " + field);
    }
  }

}
