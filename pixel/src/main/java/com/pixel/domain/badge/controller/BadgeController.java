package com.pixel.domain.badge.controller;

import com.pixel.domain.badge.dto.MyBadgesResponse;
import com.pixel.domain.badge.exception.code.BadgeSuccessCode;
import com.pixel.domain.badge.service.BadgeService;
import com.pixel.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BadgeController {

  private final BadgeService badgeService;

  @GetMapping("/api/users/me/badges")
  public ResponseEntity<ApiResponse<MyBadgesResponse>> getMyBadges(
      @AuthenticationPrincipal Long userId) {
    return ResponseEntity.ok(
        ApiResponse.of(BadgeSuccessCode._MY_BADGES_FOUND, badgeService.getMyBadges(userId)));
  }
}
