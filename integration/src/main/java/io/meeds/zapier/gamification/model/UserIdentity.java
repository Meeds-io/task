package io.meeds.zapier.gamification.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity {

  private String id;

  private String username;

  private String fullName;

  private String avatar;

}
