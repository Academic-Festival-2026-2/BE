package com.binteum.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  private static final String TOKEN_TYPE_CLAIM = "type";
  private static final String ACCESS_TYPE = "access";
  private static final String REFRESH_TYPE = "refresh";

  private final SecretKey secretKey;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
      @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
    this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  public String createAccessToken(Long userId) {
    return createToken(userId, ACCESS_TYPE, accessTokenExpiration);
  }

  public String createRefreshToken(Long userId) {
    return createToken(userId, REFRESH_TYPE, refreshTokenExpiration);
  }

  // 토큰을 한 번만 파싱해서 검증과 userId 추출을 함께 처리 (유효하지 않으면 empty)
  public Optional<Long> resolveAccessUserId(String token) {
    return parseValidClaims(token, ACCESS_TYPE)
        .map(claims -> Long.valueOf(claims.getSubject()));
  }

  public boolean validateRefreshToken(String token) {
    return parseValidClaims(token, REFRESH_TYPE).isPresent();
  }

  public long getRefreshTokenExpiration() {
    return refreshTokenExpiration;
  }

  private String createToken(Long userId, String type, long expiration) {
    Date now = new Date();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim(TOKEN_TYPE_CLAIM, type)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expiration))
        // 같은 초에 발급돼도 토큰이 겹치지 않도록 고유 ID 부여 (Rotation 보장)
        .id(UUID.randomUUID().toString())
        .signWith(secretKey)
        .compact();
  }

  private Optional<Claims> parseValidClaims(String token, String expectedType) {
    try {
      Claims claims = parseClaims(token);
      if (!expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
        return Optional.empty();
      }
      return Optional.of(claims);
    } catch (ExpiredJwtException e) {
      log.debug("만료된 JWT입니다: {}", e.getMessage());
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("유효하지 않은 JWT입니다: {}", e.getMessage());
    }
    return Optional.empty();
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}