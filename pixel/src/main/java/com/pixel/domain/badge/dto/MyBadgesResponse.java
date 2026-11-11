package com.pixel.domain.badge.dto;

import com.pixel.domain.badge.enums.BadgeType;
import java.time.LocalDateTime;
import java.util.List;

// 전체 배지 목록. 받지 않은 배지도 earned=false로 함께 내려 화면에서 잠금 상태로 보여준다.
public record MyBadgesResponse(List<Item> badges, int earnedCount, int totalCount) {

  public record Item(BadgeType type, boolean earned, LocalDateTime earnedAt, String dongName) {}
}
