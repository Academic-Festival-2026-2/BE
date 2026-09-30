package com.binteum.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  // 테스트 전용 더미 키 (실제 .env 키 아님)
  private static final String TEST_SECRET =
      "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdW5pdC10ZXN0LTEyMzQ1Njc4OTA=";

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(TEST_SECRET, 1800000L, 1209600000L);

  @Test
  void accessToken_생성_후_userId_추출() {
    String token = jwtTokenProvider.createAccessToken(1L);

    assertThat(jwtTokenProvider.validateAccessToken(token)).isTrue();
    assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L);
  }

  @Test
  void refreshToken은_accessToken으로_통과하지_않음() {
    String refreshToken = jwtTokenProvider.createRefreshToken(1L);

    assertThat(jwtTokenProvider.validateAccessToken(refreshToken)).isFalse();
    assertThat(jwtTokenProvider.validateRefreshToken(refreshToken)).isTrue();
  }

  @Test
  void 만료된_토큰은_검증_실패() {
    JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1000L, -1000L);
    String token = expiredProvider.createAccessToken(1L);

    assertThat(jwtTokenProvider.validateAccessToken(token)).isFalse();
  }

  @Test
  void 변조된_토큰은_검증_실패() {
    String token = jwtTokenProvider.createAccessToken(1L);

    assertThat(jwtTokenProvider.validateAccessToken(token + "x")).isFalse();
  }
}