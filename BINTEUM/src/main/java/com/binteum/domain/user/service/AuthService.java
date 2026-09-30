package com.binteum.domain.user.service;

import com.binteum.domain.user.dto.LoginRequest;
import com.binteum.domain.user.dto.SignUpRequest;
import com.binteum.domain.user.dto.TokenResponse;

public interface AuthService {

  void signUp(SignUpRequest request);

  TokenResponse login(LoginRequest request);
}