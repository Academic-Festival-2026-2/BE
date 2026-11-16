package com.pixel.domain.photo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원이 누른 사진 좋아요. 회원당 사진 하나에 한 행만 둔다.
// 저장은 PhotoLikeRepository.insertIfAbsent(이미 눌렀으면 그대로 둠)로만 하고, 엔티티는 조회에만 쓴다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "photo_likes",
    uniqueConstraints = @UniqueConstraint(name = "uk_photo_likes_user_photo",
        columnNames = {"user_id", "photo_id"}))
public class PhotoLike {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "photo_like_id")
  private Long photoLikeId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  // 사진을 지우면 함께 지운다 (FK ON DELETE CASCADE, 서비스에서도 먼저 지움)
  @Column(name = "photo_id", nullable = false)
  private Long photoId;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;
}
