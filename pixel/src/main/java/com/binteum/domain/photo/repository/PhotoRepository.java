package com.binteum.domain.photo.repository;

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
               ㅡㅋ       'axis-order=long-lat'
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
}
