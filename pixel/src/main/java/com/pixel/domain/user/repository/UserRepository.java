package com.pixel.domain.user.repository;

import com.pixel.domain.user.entity.User;
import com.pixel.domain.user.enums.UserStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

  boolean existsByUserIdAndStatus(Long userId, UserStatus status);

  Optional<User> findByShareKeyAndStatus(String shareKey, UserStatus status);

  // 탈퇴용 쓰기 잠금(FOR UPDATE). 같은 회원의 좋아요(findForShare)는 탈퇴가 끝날 때까지 기다린다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.userId = :userId")
  Optional<User> findForUpdate(@Param("userId") Long userId);

  // 좋아요용 공유 잠금(FOR SHARE). 탈퇴와 겹치면 탈퇴가 끝난 뒤의 상태(DELETED)를 읽는다.
  // 좋아요끼리는 서로 막지 않는다.
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("SELECT u FROM User u WHERE u.userId = :userId")
  Optional<User> findForShare(@Param("userId") Long userId);
}
