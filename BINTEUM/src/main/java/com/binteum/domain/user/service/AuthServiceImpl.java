package com.binteum.domain.user.service;

import com.binteum.domain.user.dto.SignUpRequest;
import com.binteum.domain.user.entity.User;
import com.binteum.domain.user.repository.UserRepository;
import com.binteum.global.code.ErrorCode;
import com.binteum.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void signUp(SignUpRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new GeneralException(ErrorCode.DUPLICATE_EMAIL);
    }
    if (userRepository.existsByStudentId(request.getStudentId())) {
      throw new GeneralException(ErrorCode.DUPLICATE_STUDENT_ID);
    }

    User user = User.builder()
        .studentId(request.getStudentId())
        .name(request.getName())
        .nickname(request.getNickname())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .department(request.getDepartment())
        .build();

    userRepository.save(user);
  }
}