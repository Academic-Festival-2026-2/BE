package com.binteum.domain.user.controller;

import com.binteum.domain.user.dto.LoginRequest;
import com.binteum.domain.user.dto.SignUpRequest;
import com.binteum.domain.user.dto.TokenResponse;
import com.binteum.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
    // TODO ⑤: authService.signUp(request) 호출
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success("회원가입이 완료되었습니다.", null));
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(new TokenResponse("sample-access-token", "sample-refresh-token"));
  }
}