package io.meeds.zapier.gamification.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamificationAnnouncement {

  private Long         id;

  private Long         activityId;

  private String       challengeTitle;

  private String       comment;

  private UserIdentity assignee;

}
