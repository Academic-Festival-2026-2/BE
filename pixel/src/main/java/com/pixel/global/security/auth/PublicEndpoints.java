package com.pixel.global.security.auth;

import java.util.List;
import org.springframework.util.AntPathMatcher;

/**
 * 로그인 없이 호출할 수 있는 경로 목록. SecurityConfig와 Swagger 자물쇠 표시가 같은 목록을 쓴다.
 */
public final class PublicEndpoints {

  /** 공개 경로 안에 있지만 로그인이 필요한 경로. 공개 경로보다 먼저 검사한다. */
  public static final List<String> AUTHENTICATED = List.of("/api/auth/logout");

  /** 모든 HTTP 메서드가 공개인 경로. */
  public static final List<String> ALL_METHODS = List.of(
      "/health", "/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/error");

  /** GET만 공개인 경로. */
  public static final List<String> GET_ONLY = List.of(
      "/api/map/boundaries", "/api/map/dongs", "/api/dongs/*", "/api/photos/*",
      "/api/shared-maps/*", "/api/map/regions");

  private static final AntPathMatcher MATCHER = new AntPathMatcher();

  private PublicEndpoints() {
  }

  /**
   * 로그인 없이 호출할 수 있는지 판단한다. path는 {@code /api/photos/{photoId}} 같은 템플릿도 된다.
   */
  public static boolean isPublic(String method, String path) {
    if (matchesAny(AUTHENTICATED, path)) {
      return false;
    }
    return matchesAny(ALL_METHODS, path)
        || ("GET".equalsIgnoreCase(method) && matchesAny(GET_ONLY, path));
  }

  private static boolean matchesAny(List<String> patterns, String path) {
    return patterns.stream().anyMatch(pattern -> MATCHER.match(pattern, path));
  }
}
