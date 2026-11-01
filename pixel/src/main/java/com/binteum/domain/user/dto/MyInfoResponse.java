package com.binteum.domain.user.dto;

import com.binteum.domain.user.entity.User;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyInfoResponse {

  private Long userId;
  private String email;
  private String nickname;
  private boolean locationAgreed;
  private LocalDateTime createdAt;

  public static MyInfoResponse from(User user) {
    return new MyInfoResponse(user.getUserId(), user.getEmail(), user.getNickname(),
        user.getLocationAgreedAt() != null, user.getCreatedAt());
  }
}