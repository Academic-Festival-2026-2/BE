package com.pixel.domain.photo.repository;

import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.entity.PhotoLike;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoLikeRepository extends JpaRepository<PhotoLike, Long> {

  long countByPhotoId(Long photoId);

  boolean existsByUserIdAndPhotoId(Long userId, Long photoId);

  // 이미 누른 좋아요면 기존 행을 그대로 둔다. 같은 회원이 빠르게 두 번 눌러도 UNIQUE 충돌로 실패하지 않게 한다.
  // 누른 시각은 앱(한국 시간)에서 넘긴다. DB의 CURRENT_TIMESTAMP는 RDS 기본 시간대(UTC)라 어긋난다.
  @Modifying
  @Query(value = """
      INSERT INTO photo_likes (user_id, photo_id, created_at)
      VALUES (:userId, :photoId, :likedAt)
      ON DUPLICATE KEY UPDATE user_id = user_id
      """, nativeQuery = true)
  void insertIfAbsent(@Param("userId") Long userId, @Param("photoId") Long photoId,
      @Param("likedAt") LocalDateTime likedAt);

  @Modifying
  @Query("DELETE FROM PhotoLike l WHERE l.userId = :userId AND l.photoId = :photoId")
  void deleteByUserIdAndPhotoId(@Param("userId") Long userId, @Param("photoId") Long photoId);

  @Modifying
  @Query("DELETE FROM PhotoLike l WHERE l.photoId = :photoId")
  void deleteAllByPhotoId(@Param("photoId") Long photoId);

  // 좋아요한 사진 중 지금 볼 수 있는 사진만 최근에 누른 순으로 돌려준다.
  // 남의 사진이 나중에 비공개로 바뀌면 목록에서 빠지고, 다시 공개되면 돌아온다.
  @Query(value = """
      SELECT p FROM PhotoLike l JOIN Photo p ON p.photoId = l.photoId
      JOIN FETCH p.dong JOIN FETCH p.user
      WHERE l.userId = :userId AND (p.isPublic = true OR p.user.userId = :userId)
      ORDER BY l.createdAt DESC, l.photoLikeId DESC
      """,
      countQuery = """
          SELECT COUNT(l) FROM PhotoLike l JOIN Photo p ON p.photoId = l.photoId
          WHERE l.userId = :userId AND (p.isPublic = true OR p.user.userId = :userId)
          """)
  Page<Photo> findLikedPhotos(@Param("userId") Long userId, Pageable pageable);
}
