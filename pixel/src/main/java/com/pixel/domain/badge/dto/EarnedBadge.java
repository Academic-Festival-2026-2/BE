package com.pixel.domain.badge.dto;

import com.pixel.domain.badge.enums.BadgeType;
import java.time.LocalDateTime;

// 받은 배지 한 개 (조회용). dongName은 DONG_FIRST·DONG_TOP10만 있음
public record EarnedBadge(BadgeType type, LocalDateTime earnedAt, String dongName) {}
