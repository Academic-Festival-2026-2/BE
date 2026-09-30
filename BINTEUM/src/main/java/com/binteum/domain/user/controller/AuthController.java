package com.binteum.domain.user.controller;

import com.binteum.domain.user.dto.LoginRequest;
import com.binteum.domain.user.dto.ReissueRequest;
import com.binteum.domain.user.dto.SignUpRequest;
import com.binteum.domain.user.dto.TokenResponse;
import com.binteum.domain.user.service.AuthService;
import com.binteum.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
    authService.signUp(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success("회원가입이 완료되었습니다.", null));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    TokenResponse tokenResponse = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", tokenResponse));
  }

  @PostMapping("/reissue")
  public ResponseEntity<ApiResponse<TokenResponse>> reissue(
      @Valid @RequestBody ReissueRequest request) {
    TokenResponse tokenResponse = authService.reissue(request);
    return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", tokenResponse));
  }
}