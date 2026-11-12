package com.pixel.domain.user.dto;

import com.pixel.domain.user.entity.User;
import com.pixel.domain.user.enums.ProfileImage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyInfoResponse {

  private Long userId;
  private String email;
  private String nickname;
  // COLOR_* 단색 / 그 외 캐릭터 (FE가 키로 이미지를 고름)
  private ProfileImage profileImage;
  private boolean locationAgreed;
  private LocalDateTime createdAt;
  // 내가 올린 사진 전체 개수 (내 사진 목록과 같은 기준)
  private long photoCount;
  // 나의 지도(가장 최근 AUTUMN)와 같은 기준, 모든 지역 합계
  private int filledCellCount;

  public static MyInfoResponse of(User user, long photoCount, int filledCellCount) {
    return new MyInfoResponse(user.getUserId(), user.getEmail(), user.getNickname(),
        user.getProfileImage(), user.getLocationAgreedAt() != null, user.getCreatedAt(),
        photoCount, filledCellCount);
  }
}
