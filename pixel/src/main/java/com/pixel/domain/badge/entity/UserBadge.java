package com.pixel.domain.badge.entity;

import com.pixel.domain.badge.enums.BadgeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 회원이 받은 배지. 한 번 받으면 사진을 지워도 남는다.
// 저장은 UserBadgeRepository.insertIfAbsent(중복이면 그대로 둠)로만 하고, 엔티티는 조회에만 쓴다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_badges",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_badges_user_type",
        columnNames = {"user_id", "badge_type"}))
public class UserBadge {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_badge_id")
  private Long userBadgeId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "badge_type", length = 20, nullable = false)
  private BadgeType badgeType;

  // 배지를 받게 한 사진. 사진을 지우면 NULL (FK ON DELETE SET NULL)
  @Column(name = "photo_id")
  private Long photoId;

  // DONG_FIRST·DONG_TOP10만 채움. 화면에 "○○동" 표시용
  @Column(name = "dong_code", columnDefinition = "CHAR(10)")
  private String dongCode;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;
}
