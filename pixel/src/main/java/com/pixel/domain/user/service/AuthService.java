package com.pixel.domain.user.service;

import com.pixel.domain.user.dto.LoginRequest;
import com.pixel.domain.user.dto.ReissueRequest;
import com.pixel.domain.user.dto.SignUpRequest;
import com.pixel.domain.user.dto.TokenResponse;

public interface AuthService {

  void signUp(SignUpRequest request);

  TokenResponse login(LoginRequest request);

  TokenResponse reissue(ReissueRequest request);

  void logout(Long userId);
}