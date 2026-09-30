package com.binteum.domain.user.service;

import com.binteum.domain.refreshtoken.entity.RefreshToken;
import com.binteum.domain.refreshtoken.repository.RefreshTokenRepository;
import com.binteum.domain.user.dto.LoginRequest;
import com.binteum.domain.user.dto.ReissueRequest;
import com.binteum.domain.user.dto.SignUpRequest;
import com.binteum.domain.user.dto.TokenResponse;
import com.binteum.domain.user.entity.User;
import com.binteum.domain.user.enums.UserStatus;
import com.binteum.domain.user.repository.UserRepository;
import com.binteum.global.code.ErrorCode;
import com.binteum.global.exception.GeneralException;
import com.binteum.global.security.jwt.JwtTokenProvider;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

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

  @Override
  public TokenResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new GeneralException(ErrorCode.INVALID_CREDENTIALS));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new GeneralException(ErrorCode.INVALID_CREDENTIALS);
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new GeneralException(ErrorCode.INACTIVE_USER);
    }

    return issueTokens(user);
  }

  @Override
  public TokenResponse reissue(ReissueRequest request) {
    String refreshToken = request.getRefreshToken();

    if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
      throw new GeneralException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    RefreshToken savedToken = refreshTokenRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new GeneralException(ErrorCode.INVALID_REFRESH_TOKEN));

    User user = savedToken.getUser();
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new GeneralException(ErrorCode.INACTIVE_USER);
    }

    return issueTokens(user);
  }

  private TokenResponse issueTokens(User user) {
    String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
    LocalDateTime expiredAt = LocalDateTime.now()
        .plus(jwtTokenProvider.getRefreshTokenExpiration(), ChronoUnit.MILLIS);

    refreshTokenRepository.findByUserUserId(user.getUserId())
        .ifPresentOrElse(
            token -> token.updateToken(refreshToken, expiredAt),
            () -> refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiredAt(expiredAt)
                .build()));

    return new TokenResponse(accessToken, refreshToken);
  }
}