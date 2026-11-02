package com.binteum.domain.photo.repository;

import com.binteum.domain.photo.dto.PhotoPointView;
import com.binteum.domain.photo.entity.Photo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

  boolean existsByImageHash(String imageHash);

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

  @Query(
      value =
          """
              SELECT p.photo_id AS photoId,
                    ROUND(ST_Latitude(p.location),3) AS lat,
                    ROUND(ST_Longitude(p.location), 3) AS lng,
                    JSON_UNQUOTE(JSON_EXTRACT(p.palette, '$[0]')) AS color,
                    p.time_band AS timeBand 
              FROM photos p
              WHERE p.dong_code = :dongCode
                AND p.season_id = :seasonId
                AND p.exif_verified = TRUE
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
}