package com.pixel.domain.user.entity;

import com.pixel.domain.user.enums.ProfileImage;
import com.pixel.domain.user.enums.UserStatus;
import com.pixel.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
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
  @Column(name = "profile_image", length = 30, nullable = false)
  private ProfileImage profileImage;

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

  @Column(name = "share_key", length = 32, unique = true)
  private String shareKey;

  @Builder
  private User(String email, String passwordHash, String nickname, ProfileImage profileImage,
      LocalDateTime termsAgreedAt, LocalDateTime privacyAgreedAt, LocalDateTime locationAgreedAt) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.nickname = nickname;
    // 지정하지 않으면(회원가입) 캐릭터 중 랜덤으로 배정한다
    this.profileImage = profileImage != null ? profileImage : ProfileImage.random();
    this.termsAgreedAt = termsAgreedAt;
    this.privacyAgreedAt = privacyAgreedAt;
    this.locationAgreedAt = locationAgreedAt;
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }

  public void changeProfileImage(ProfileImage profileImage) {
    this.profileImage = profileImage;
  }

  // 이미 동의한 상태면 처음 동의한 시각을 유지한다
  public void changeLocationAgreement(boolean agreed) {
    if (!agreed) {
      this.locationAgreedAt = null;
    } else if (this.locationAgreedAt == null) {
      this.locationAgreedAt = LocalDateTime.now();
    }
  }

  // 이미 공유 중일 땐 기존 키 유지
  public void shareMyMap() {
    if (this.shareKey == null) {
      this.shareKey = UUID.randomUUID().toString().replace("-", "");
    }
  }

  public void stopSharingMyMap() {
    this.shareKey = null;
  }

  public void changePassword(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  // 같은 이메일로 재가입할 수 있게 이메일·닉네임을 익명 값으로 바꾼다
  // 이메일은 @Email 검증을 통과하지 못하는 형식, 닉네임 "탈퇴회원"은 가입·수정에서 막아 선점을 방지
  public void withdraw() {
    this.status = UserStatus.DELETED;
    this.email = "deleted:" + userId;
    this.nickname = "탈퇴회원" + userId;
    stopSharingMyMap();
    markDeleted();
  }
}
