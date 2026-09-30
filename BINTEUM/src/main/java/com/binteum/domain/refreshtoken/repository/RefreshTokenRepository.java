package com.binteum.domain.refreshtoken.repository;

import com.binteum.domain.refreshtoken.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByUserUserId(Long userId);

  Optional<RefreshToken> findByRefreshToken(String refreshToken);
}