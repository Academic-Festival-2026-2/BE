package com.binteum.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.binteum.domain.user.enums.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserTest {

  private static final LocalDateTime AGREED_AT = LocalDateTime.of(2026, 10, 1, 10, 0);

  @Test
  void 이미_동의한_상태에서_다시_동의해도_처음_동의_시각을_유지한다() {
    User user = user(AGREED_AT);

    user.changeLocationAgreement(true);

    assertThat(user.getLocationAgreedAt()).isEqualTo(AGREED_AT);
  }

  @Test
  void 동의하지_않은_상태에서_동의하면_시각을_기록한다() {
    User user = user(null);

    user.changeLocationAgreement(true);

    assertThat(user.getLocationAgreedAt()).isNotNull();
  }

  @Test
  void 동의를_철회하면_시각이_지워진다() {
    User user = user(AGREED_AT);

    user.changeLocationAgreement(false);

    assertThat(user.getLocationAgreedAt()).isNull();
  }

  @Test
  void 탈퇴하면_상태가_바뀌고_이메일과_닉네임이_익명화된다() {
    User user = user(AGREED_AT);

    user.withdraw();

    assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
    assertThat(user.getEmail()).isEqualTo("deleted:1");
    assertThat(user.getNickname()).isEqualTo("탈퇴회원1");
    assertThat(user.getDeletedAt()).isNotNull();
  }

  private User user(LocalDateTime locationAgreedAt) {
    User user = User.builder()
        .email("tester@example.com")
        .passwordHash("hash")
        .nickname("tester")
        .termsAgreedAt(AGREED_AT)
        .privacyAgreedAt(AGREED_AT)
        .locationAgreedAt(locationAgreedAt)
        .build();
    ReflectionTestUtils.setField(user, "userId", 1L);
    return user;
  }
}
