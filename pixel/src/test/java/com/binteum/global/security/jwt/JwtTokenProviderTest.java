package com.binteum.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  // 테스트 전용 더미 키 (실제 .env 키 아님)
  private static final String TEST_SECRET =
      "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdW5pdC10ZXN0LTEyMzQ1Njc4OTA=";

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(TEST_SECRET, 1800000L, 1209600000L);

  @Test
  @DisplayName("AccessToken 생성 후 userId를 추출한다")
  void createAccessToken_andExtractUserId() {
    String token = jwtTokenProvider.createAccessToken(1L);

    assertThat(jwtTokenProvider.resolveAccessUserId(token)).contains(1L);
  }

  @Test
  @DisplayName("RefreshToken은 AccessToken 검증을 통과하지 않는다")
  void refreshToken_failsAccessTokenValidation() {
    String refreshToken = jwtTokenProvider.createRefreshToken(1L);

    assertThat(jwtTokenProvider.resolveAccessUserId(refreshToken)).isEmpty();
    assertThat(jwtTokenProvider.validateRefreshToken(refreshToken)).isTrue();
  }

  @Test
  @DisplayName("만료된 토큰은 검증에 실패한다")
  void expiredToken_failsValidation() {
    JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1000L, -1000L);
    String token = expiredProvider.createAccessToken(1L);

    assertThat(jwtTokenProvider.resolveAccessUserId(token)).isEmpty();
  }

  @Test
  @DisplayName("변조된 토큰은 검증에 실패한다")
  void tamperedToken_failsValidation() {
    String token = jwtTokenProvider.createAccessToken(1L);

    assertThat(jwtTokenProvider.resolveAccessUserId(token + "x")).isEmpty();
  }

  @Test
  @DisplayName("같은 유저에게 연속 발급한 RefreshToken은 서로 다르다")
  void refreshTokens_issuedInSameSecond_areDifferent() {
    String first = jwtTokenProvider.createRefreshToken(1L);
    String second = jwtTokenProvider.createRefreshToken(1L);

    assertThat(first).isNotEqualTo(second);
  }
}