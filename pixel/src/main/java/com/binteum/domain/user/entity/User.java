package com.binteum.domain.user.entity;

import com.binteum.domain.user.enums.UserStatus;
import com.binteum.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "email", length = 255, nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", length = 100, nullable = false)
  private String passwordHash;

  @Column(name = "nickname", length = 16, nullable = false, unique = true)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "status", length = 10, nullable = false)
  private UserStatus status = UserStatus.ACTIVE;

  @Column(name = "terms_agreed_at", nullable = false)
  private LocalDateTime termsAgreedAt;

  @Column(name = "privacy_agreed_at", nullable = false)
  private LocalDateTime privacyAgreedAt;

  @Column(name = "location_agreed_at")
  private LocalDateTime locationAgreedAt;

  @Builder
  private User(String email, String passwordHash, String nickname, LocalDateTime termsAgreedAt,
      LocalDateTime privacyAgreedAt, LocalDateTime locationAgreedAt) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.nickname = nickname;
    this.termsAgreedAt = termsAgreedAt;
    this.privacyAgreedAt = privacyAgreedAt;
    this.locationAgreedAt = locationAgreedAt;
  }
}
