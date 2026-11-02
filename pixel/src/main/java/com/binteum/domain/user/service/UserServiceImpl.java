package com.binteum.domain.user.service;

import com.binteum.domain.refreshtoken.repository.RefreshTokenRepository;
import com.binteum.domain.user.dto.ChangePasswordRequest;
import com.binteum.domain.user.dto.MyInfoResponse;
import com.binteum.domain.user.dto.UpdateMyInfoRequest;
import com.binteum.domain.user.dto.WithdrawRequest;
import com.binteum.domain.user.entity.User;
import com.binteum.domain.user.enums.UserStatus;
import com.binteum.domain.user.exception.code.UserErrorCode;
import com.binteum.domain.user.repository.UserRepository;
import com.binteum.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional(readOnly = true)
  public MyInfoResponse getMyInfo(Long userId) {
    return MyInfoResponse.from(getActiveUser(userId));
  }

  @Override
  public void updateMyInfo(Long userId, UpdateMyInfoRequest request) {
    User user = getActiveUser(userId);

    String nickname = request.getNickname();
    if (nickname != null && !nickname.equals(user.getNickname())) {
      if (userRepository.existsByNickname(nickname)) {
        throw new GeneralException(UserErrorCode.DUPLICATE_NICKNAME);
      }
      user.changeNickname(nickname);
    }
    if (request.getLocationAgreed() != null) {
      user.changeLocationAgreement(request.getLocationAgreed());
    }
  }

  @Override
  public void changePassword(Long userId, ChangePasswordRequest request) {
    User user = getActiveUser(userId);
    checkPassword(user, request.getCurrentPassword());
    if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
      throw new GeneralException(UserErrorCode.SAME_PASSWORD);
    }
    user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    deleteRefreshToken(userId);
  }

  @Override
  public void withdraw(Long userId, WithdrawRequest request) {
    User user = getActiveUser(userId);
    checkPassword(user, request.getPassword());
    deleteRefreshToken(userId);
    user.withdraw();
  }

  // 탈퇴 후에도 access token은 만료(30분) 전까지 유효하므로 여기서 상태를 확인
  private User getActiveUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new GeneralException(UserErrorCode._NOT_FOUND));
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new GeneralException(UserErrorCode.INACTIVE_USER);
    }
    return user;
  }

  private void checkPassword(User user, String rawPassword) {
    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      throw new GeneralException(UserErrorCode.PASSWORD_MISMATCH);
    }
  }

  private void deleteRefreshToken(Long userId) {
    refreshTokenRepository.findByUserUserId(userId).ifPresent(refreshTokenRepository::delete);
  }
}
