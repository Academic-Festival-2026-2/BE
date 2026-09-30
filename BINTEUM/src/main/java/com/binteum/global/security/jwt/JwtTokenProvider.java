package com.binteum.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
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

  public boolean validateAccessToken(String token) {
    return validateToken(token, ACCESS_TYPE);
  }

  public boolean validateRefreshToken(String token) {
    return validateToken(token, REFRESH_TYPE);
  }

  public Long getUserId(String token) {
    return Long.valueOf(parseClaims(token).getSubject());
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
        .signWith(secretKey)
        .compact();
  }

  private boolean validateToken(String token, String expectedType) {
    try {
      Claims claims = parseClaims(token);
      return expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    } catch (ExpiredJwtException e) {
      log.debug("만료된 JWT입니다: {}", e.getMessage());
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("유효하지 않은 JWT입니다: {}", e.getMessage());
    }
    return false;
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}