package com.binteum.domain.refreshtoken.repository;

import com.binteum.domain.refreshtoken.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByUserUserId(Long userId);

  // 재발급 동시 요청 시 같은 토큰으로 두 번 Rotation되지 않도록 행 잠금 (SELECT ... FOR UPDATE)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
