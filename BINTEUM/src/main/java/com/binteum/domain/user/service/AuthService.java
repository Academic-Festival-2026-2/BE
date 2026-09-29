package com.binteum.domain.user.service;

import com.binteum.domain.user.dto.SignUpRequest;

public interface AuthService {

  void signUp(SignUpRequest request);
}