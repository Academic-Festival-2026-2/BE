package com.pixel.domain.photo.repository;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.photo.dto.DongPointCountView;
import com.pixel.domain.photo.dto.PhotoPointView;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.season.entity.Season;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

  // 잠금 읽기로 MySQL REPEATABLE READ의 이전 스냅샷 대신 최신 커밋을 확인한다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT p FROM Photo p
      WHERE p.user.userId = :userId AND p.shotDate = :shotDate
        AND p.dong.dongCode = :dongCode AND p.season.seasonId = :seasonId
        AND p.timeBand = :timeBand AND p.exifVerified = true
        AND p.aiVerdict = com.pixel.domain.photo.enums.AiVerdict.PUBLIC AND p.photoId <> :photoId
      """)
  List<Photo> findPreviousShot(@Param("userId") Long userId,
      @Param("shotDate") LocalDate shotDate, @Param("dongCode") String dongCode,
      @Param("seasonId") Short seasonId, @Param("timeBand") TimeBand timeBand,
      @Param("photoId") Long photoId, Pageable pageable);

  @Query("SELECT p.dong.dongCode FROM Photo p WHERE p.photoId = :photoId")
  Optional<String> findDongCodeByPhotoId(@Param("photoId") Long photoId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Photo p WHERE p.photoId = :photoId")
  Optional<Photo> findForUpdate(@Param("photoId") Long photoId);

  // 좋아요용 공유 잠금(MySQL FOR SHARE). 삭제·공개 변경(findForUpdate)은 기다리고, 좋아요끼리는 동시에 읽는다.
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("SELECT p FROM Photo p WHERE p.photoId = :photoId")
  Optional<Photo> findForShare(@Param("photoId") Long photoId);

  @EntityGraph(attributePaths = {"dong", "user"})
  Page<Photo> findByUserUserId(Long userId, Pageable pageable);

  // 칸 색(대표색 표 수) 계산용. 동 잠금(lockForUpload) 뒤에 불러서 업로드·삭제와 순서가 겹치지 않고,
  // 잠금 읽기라 REPEATABLE READ의 이전 스냅샷 대신 최신 커밋을 읽는다.
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("SELECT p FROM Photo p WHERE p.dong.dongCode = :dongCode"
      + " AND p.season.seasonId = :seasonId AND p.timeBand = :timeBand"
      + " AND p.exifVerified = true AND p.aiVerdict = com.pixel.domain.photo.enums.AiVerdict.PUBLIC"
      + " ORDER BY p.photoId")
  List<Photo> findCountedInCell(@Param("dongCode") String dongCode,
      @Param("seasonId") Short seasonId, @Param("timeBand") TimeBand timeBand);

  boolean existsByImageHash(String imageHash);

  long countByUserUserId(Long userId);

  // 배지 조건용. 나의 지도 기준이라 공개 여부·검수 결과와 상관없이 회원의 모든 사진을 센다.
  @Query("SELECT COUNT(DISTINCT p.dong.dongCode) FROM Photo p WHERE p.user.userId = :userId")
  long countDistinctDongsByUserId(@Param("userId") Long userId);

  @Query("SELECT COUNT(DISTINCT p.timeBand) FROM Photo p WHERE p.user.userId = :userId")
  long countDistinctTimeBandsByUserId(@Param("userId") Long userId);

  // 같은 동·계절의 모두의 지도에 이 사진보다 먼저 들어간 사진 수 (0이면 첫 사진).
  // Photo.countsForPublicMap과 같은 기준. 업로드의 동 잠금(lockForUpload) 뒤에 불러야 순서가 겹치지 않는다.
  @Query("""
      SELECT COUNT(p) FROM Photo p
      WHERE p.dong.dongCode = :dongCode AND p.season.seasonId = :seasonId
        AND p.exifVerified = true AND p.aiVerdict = com.pixel.domain.photo.enums.AiVerdict.PUBLIC
        AND p.photoId < :photoId
      """)
  long countEarlierOnPublicMap(@Param("dongCode") String dongCode,
      @Param("seasonId") Short seasonId, @Param("photoId") Long photoId);

  List<Photo> findAllByUserUserIdOrderByCreatedAtDesc(Long userId);

  @Modifying
  @Query(
      value =
          """
              UPDATE photos
              SET location = ST_GeomFromText(
                      CONCAT('POINT(', :longitude, ' ', :latitude, ')'),
                      4326,
                      'axis-order=long-lat'
                  ),
                  exif_verified = :verified
              WHERE photo_id = :photoId
              """,
      nativeQuery = true)
  int updateLocationAndVerification(
      @Param("photoId") Long photoId,
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("verified") boolean verified);

  // 점 조건은 countPhotoPoints(지도 API 점 개수)와 같아야 함. 한쪽을 바꾸면 다른 쪽도 같이 바꿀 것
  @Query(
      value =
          """
              SELECT p.photo_id AS photoId,
                    ROUND(ST_Latitude(p.location), 3) AS lat,
                    ROUND(ST_Longitude(p.location), 3) AS lng,
                    JSON_UNQUOTE(JSON_EXTRACT(p.palette, '$[0]')) AS color,
                    p.time_band AS timeBand
              FROM photos p
              WHERE p.dong_code = :dongCode
                AND p.season_id = :seasonId
                AND p.exif_verified = TRUE
                AND p.ai_verdict = 'PUBLIC'
                AND p.is_public = TRUE
                AND p.location IS NOT NULL
                AND (:timeBand IS NULL OR p.time_band = :timeBand)
              ORDER BY p.created_at DESC
              LIMIT 300
              """,
      nativeQuery = true)
  List<PhotoPointView> findPhotoPoints(
      @Param("dongCode") String dongCode,
      @Param("seasonId") Short seasonId,
      @Param("timeBand") String timeBand);

  // 지도 API: 지역 전체 동별 점 개수 (동 상세에 점으로 보이는 사진 수)
  // 조건은 findPhotoPoints와 같아야 함. 한쪽을 바꾸면 다른 쪽도 같이 바꿀 것 (숫자가 어긋남)
  @Query(
      value =
          """
              SELECT p.dong_code AS dongCode, COUNT(*) AS pointCount
              FROM photos p
              JOIN dongs d ON d.dong_code = p.dong_code
              WHERE d.region_code = :regionCode
                AND p.season_id = :seasonId
                AND p.time_band = :timeBand
                AND p.exif_verified = TRUE
                AND p.ai_verdict = 'PUBLIC'
                AND p.is_public = TRUE
                AND p.location IS NOT NULL
              GROUP BY p.dong_code
              """,
      nativeQuery = true)
  List<DongPointCountView> countPhotoPoints(
      @Param("regionCode") String regionCode,
      @Param("seasonId") Short seasonId,
      @Param("timeBand") String timeBand);

  @Query(
      value =
          """
              SELECT p.photo_id AS photoId,
                    ST_Latitude(p.location) AS lat,
                    ST_Longitude(p.location) AS lng,
                    JSON_UNQUOTE(JSON_EXTRACT(p.palette, '$[0]')) AS color,
                    p.time_band AS timeBand
              FROM photos p
              WHERE p.user_id = :userId
                AND p.dong_code = :dongCode
                AND p.season_id = :seasonId
                AND (:timeBand IS NULL OR p.time_band = :timeBand)
              ORDER BY p.created_at DESC
              """,
      nativeQuery = true)
  List<PhotoPointView> findMyPhotoPoints(
      @Param("userId") Long userId,
      @Param("dongCode") String dongCode,
      @Param("seasonId") Short seasonId,
      @Param("timeBand") String timeBand);

  @Query("""
        SELECT p FROM Photo p
        WHERE p.user.userId = :userId
          AND p.season = :season
      """)
  List<Photo> findMyMapPhotos(@Param("userId") Long userId, @Param("season") Season season);
}
