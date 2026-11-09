package com.pixel.domain.user.dto;

import com.pixel.domain.user.entity.User;
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
  // 내가 올린 사진 전체 개수 (내 사진 목록과 같은 기준)
  private long photoCount;
  // 나의 지도(가장 최근 AUTUMN)의 filledCellCount와 같은 값
  private int filledCellCount;

  public static MyInfoResponse of(User user, long photoCount, int filledCellCount) {
    return new MyInfoResponse(user.getUserId(), user.getEmail(), user.getNickname(),
        user.getLocationAgreedAt() != null, user.getCreatedAt(), photoCount, filledCellCount);
  }
}
