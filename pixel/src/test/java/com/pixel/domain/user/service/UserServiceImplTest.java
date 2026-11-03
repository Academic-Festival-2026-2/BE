package com.pixel.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pixel.domain.refreshtoken.entity.RefreshToken;
import com.pixel.domain.refreshtoken.repository.RefreshTokenRepository;
import com.pixel.domain.user.dto.ChangePasswordRequest;
import com.pixel.domain.user.dto.UpdateMyInfoRequest;
import com.pixel.domain.user.dto.WithdrawRequest;
import com.pixel.domain.user.entity.User;
import com.pixel.domain.user.enums.UserStatus;
import com.pixel.domain.user.exception.code.UserErrorCode;
import com.pixel.domain.user.repository.UserRepository;
import com.pixel.global.apiPayload.code.BaseErrorCode;
import com.pixel.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  private static final Long USER_ID = 1L;
  private static final String PASSWORD = "password123";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Mock
  private UserRepository userRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  private UserServiceImpl userService;
  private User user;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(userRepository, refreshTokenRepository, passwordEncoder);
    user = User.builder()
        .email("tester@example.com")
        .passwordHash(passwordEncoder.encode(PASSWORD))
        .nickname("tester")
        .termsAgreedAt(LocalDateTime.now())
        .privacyAgreedAt(LocalDateTime.now())
        .build();
    ReflectionTestUtils.setField(user, "userId", USER_ID);
  }

  @Test
  void 없는_사용자면_실패한다() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

    assertErrorCode(() -> userService.getMyInfo(USER_ID), UserErrorCode._NOT_FOUND);
  }

  @Test
  void 다른_사람이_쓰는_닉네임으로_바꿀_수_없다() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(userRepository.existsByNickname("taken")).willReturn(true);

    assertErrorCode(() -> userService.updateMyInfo(USER_ID, updateRequest("taken")),
        UserErrorCode.DUPLICATE_NICKNAME);
    assertThat(user.getNickname()).isEqualTo("tester");
  }

  @Test
  void 지금_닉네임과_같으면_중복_검사를_하지_않는다() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    userService.updateMyInfo(USER_ID, updateRequest("tester"));

    verify(userRepository, never()).existsByNickname(any());
  }

  @Test
  void 현재_비밀번호가_틀리면_변경에_실패한다() throws Exception {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    assertErrorCode(
        () -> userService.changePassword(USER_ID, changePasswordRequest("wrong1234", "newpass123")),
        UserErrorCode.PASSWORD_MISMATCH);
  }

  @Test
  void 새_비밀번호가_현재와_같으면_실패한다() throws Exception {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    assertErrorCode(
        () -> userService.changePassword(USER_ID, changePasswordRequest(PASSWORD, PASSWORD)),
        UserErrorCode.SAME_PASSWORD);
  }

  @Test
  void 비밀번호를_바꾸면_새_비밀번호로_저장되고_refresh_token을_삭제한다() throws Exception {
    RefreshToken token = refreshToken();
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(refreshTokenRepository.findByUserUserId(USER_ID)).willReturn(Optional.of(token));

    userService.changePassword(USER_ID, changePasswordRequest(PASSWORD, "newpass123"));

    assertThat(passwordEncoder.matches("newpass123", user.getPasswordHash())).isTrue();
    verify(refreshTokenRepository).delete(token);
  }

  @Test
  void 비밀번호가_틀리면_탈퇴에_실패하고_계정은_그대로다() throws Exception {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    assertErrorCode(() -> userService.withdraw(USER_ID, withdrawRequest("wrong1234")),
        UserErrorCode.PASSWORD_MISMATCH);
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    verify(refreshTokenRepository, never()).delete(any());
  }

  @Test
  void 탈퇴하면_익명화되고_refresh_token을_삭제한다() throws Exception {
    RefreshToken token = refreshToken();
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(refreshTokenRepository.findByUserUserId(USER_ID)).willReturn(Optional.of(token));

    userService.withdraw(USER_ID, withdrawRequest(PASSWORD));

    assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
    assertThat(user.getEmail()).isEqualTo("deleted:1");
    verify(refreshTokenRepository).delete(token);
  }

  @Test
  void 탈퇴한_사용자는_마이페이지를_이용할_수_없다() {
    user.withdraw();
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    assertErrorCode(() -> userService.getMyInfo(USER_ID), UserErrorCode.INACTIVE_USER);
  }

  private void assertErrorCode(ThrowingCallable action, BaseErrorCode expected) {
    assertThatThrownBy(action)
        .isInstanceOf(GeneralException.class)
        .extracting("code")
        .isEqualTo(expected);
  }

  private UpdateMyInfoRequest updateRequest(String nickname) {
    UpdateMyInfoRequest request = new UpdateMyInfoRequest();
    request.setNickname(nickname);
    return request;
  }

  private ChangePasswordRequest changePasswordRequest(String current, String next)
      throws Exception {
    return objectMapper.readValue(
        "{\"currentPassword\": \"%s\", \"newPassword\": \"%s\"}".formatted(current, next),
        ChangePasswordRequest.class);
  }

  private WithdrawRequest withdrawRequest(String password) throws Exception {
    return objectMapper.readValue(
        "{\"password\": \"%s\"}".formatted(password), WithdrawRequest.class);
  }

  private RefreshToken refreshToken() {
    return RefreshToken.builder()
        .user(user)
        .refreshToken("refresh-token")
        .expiredAt(LocalDateTime.now().plusDays(14))
        .build();
  }
}
