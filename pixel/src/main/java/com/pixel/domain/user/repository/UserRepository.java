package com.pixel.domain.user.repository;

import com.pixel.domain.user.entity.User;
import com.pixel.domain.user.enums.UserStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

  boolean existsByUserIdAndStatus(Long userId, UserStatus status);

  Optional<User> findByShareKeyAndStatus(String shareKey, UserStatus status);
}


