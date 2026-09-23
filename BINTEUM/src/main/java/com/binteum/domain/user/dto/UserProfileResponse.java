package com.binteum.domain.user.dto;

import com.binteum.domain.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
  private Long userId;
  private String studentId;
  private String name;
  private String nickname;
  private String email;
  private String department;
  private UserStatus status;
}


