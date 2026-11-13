package com.pixel.domain.badge.repository;

import com.pixel.domain.badge.dto.EarnedBadge;
import com.pixel.domain.badge.entity.UserBadge;
import com.pixel.domain.badge.enums.BadgeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

  @Query("SELECT b.badgeType FROM UserBadge b WHERE b.userId = :userId")
  List<BadgeType> findBadgeTypesByUserId(@Param("userId") Long userId);

  @Query("""
      SELECT new com.pixel.domain.badge.dto.EarnedBadge(b.badgeType, b.createdAt, d.name)
      FROM UserBadge b LEFT JOIN Dong d ON d.dongCode = b.dongCode
      WHERE b.userId = :userId
      """)
  List<EarnedBadge> findEarnedByUserId(@Param("userId") Long userId);

  // 이미 받은 배지면 기존 행을 그대로 둔다.
  // 같은 회원의 업로드가 동시에 들어와도 UNIQUE(user_id, badge_type) 충돌로 업로드가 실패하지 않게 한다.
  // INSERT IGNORE와 달리 중복만 넘어가고, CHECK·FK·NOT NULL 위반은 오류로 드러난다.
  // 반환값으로는 새로 들어갔는지 알 수 없다(드라이버가 중복에도 1을 돌려줄 수 있음). 판단은 BadgeService가 한다.
  // 받은 시각은 앱(한국 시간)에서 넘긴다. DB의 CURRENT_TIMESTAMP는 RDS 기본 시간대(UTC)라 날짜가 어긋난다.
  @Modifying
  @Query(value = """
      INSERT INTO user_badges (user_id, badge_type, photo_id, dong_code, created_at)
      VALUES (:userId, :badgeType, :photoId, :dongCode, :earnedAt)
      ON DUPLICATE KEY UPDATE user_id = user_id
      """, nativeQuery = true)
  void insertIfAbsent(@Param("userId") Long userId, @Param("badgeType") String badgeType,
      @Param("photoId") Long photoId, @Param("dongCode") String dongCode,
      @Param("earnedAt") LocalDateTime earnedAt);
}
