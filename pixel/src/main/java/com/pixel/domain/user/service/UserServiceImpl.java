package com.pixel.domain.user.service;

import com.pixel.domain.mymap.service.MyMapService;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.refreshtoken.repository.RefreshTokenRepository;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.user.dto.ChangePasswordRequest;
import com.pixel.domain.user.dto.MyInfoResponse;
import com.pixel.domain.user.dto.UpdateMyInfoRequest;
import com.pixel.domain.user.dto.WithdrawRequest;
import com.pixel.domain.user.entity.User;
import com.pixel.domain.user.enums.UserStatus;
import com.pixel.domain.user.exception.code.UserErrorCode;
import com.pixel.domain.user.repository.UserRepository;
import com.pixel.global.apiPayload.exception.GeneralException;
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
  private final PhotoRepository photoRepository;
  private final MyMapService myMapService;

  @Override
  @Transactional(readOnly = true)
  public MyInfoResponse getMyInfo(Long userId) {
    User user = getActiveUser(userId);
    long photoCount = photoRepository.countByUserUserId(userId);
    // 나의 지도와 같은 기준으로 세서 두 화면의 채운 칸 수가 항상 같게 한다.
    int filledCellCount = myMapService.countFilledCells(userId, SeasonType.AUTUMN);
    return MyInfoResponse.of(user, photoCount, filledCellCount);
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
    if (request.getProfileImage() != null) {
      user.changeProfileImage(request.getProfileImage());
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
