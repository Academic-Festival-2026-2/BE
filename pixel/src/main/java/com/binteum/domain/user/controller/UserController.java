package com.binteum.domain.user.controller;

import com.binteum.domain.user.dto.ChangePasswordRequest;
import com.binteum.domain.user.dto.MyInfoResponse;
import com.binteum.domain.user.dto.UpdateMyInfoRequest;
import com.binteum.domain.user.dto.WithdrawRequest;
import com.binteum.domain.user.exception.code.UserSuccessCode;
import com.binteum.domain.user.service.UserService;
import com.binteum.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
      @AuthenticationPrincipal Long userId) {
    return ResponseEntity.ok(ApiResponse.of(UserSuccessCode._FOUND,
        userService.getMyInfo(userId)));
  }

  @PatchMapping
  public ResponseEntity<ApiResponse<Void>> updateMyInfo(@AuthenticationPrincipal Long userId,
      @Valid @RequestBody UpdateMyInfoRequest request) {
    userService.updateMyInfo(userId, request);
    return ResponseEntity.ok(ApiResponse.of(UserSuccessCode._UPDATED, null));
  }

  @PatchMapping("/password")
  public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal Long userId,
      @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(userId, request);
    return ResponseEntity.ok(ApiResponse.of(UserSuccessCode._PASSWORD_CHANGED, null));
  }

  @DeleteMapping
  public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal Long userId,
      @Valid @RequestBody WithdrawRequest request) {
    userService.withdraw(userId, request);
    return ResponseEntity.ok(ApiResponse.of(UserSuccessCode._WITHDRAWN, null));
  }
}
