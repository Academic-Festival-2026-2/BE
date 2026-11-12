package com.pixel.domain.dong.repository;

import com.pixel.domain.dong.dto.DongBoundaryView;
import com.pixel.domain.dong.entity.Dong;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DongRepository extends JpaRepository<Dong, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT d FROM Dong d WHERE d.dongCode = :dongCode")
  Optional<Dong> findForUpdate(@Param("dongCode") String dongCode);

  @Query(
      value =
          "SELECT dong_code             AS dongCode, "
              + "       name                  AS name, "
              + "       gu_name               AS guName, "
              + "       ST_AsGeoJSON(boundary, 5) AS geometry "
              + "FROM dongs "
              + "WHERE region_code = :regionCode "
              + "ORDER BY dong_code ",
      nativeQuery = true)
  List<DongBoundaryView> findAllBoundaries(@Param("regionCode") String regionCode);

  @Query("SELECT MAX(d.boundaryVersion) FROM Dong d WHERE d.region.regionCode = :regionCode")
  String findBoundaryVersion(@Param("regionCode") String regionCode);

  @Query(
      value =
          """
          SELECT dong_code
          FROM dongs
          WHERE ST_Contains(
              boundary,
              ST_GeomFromText(
                  CONCAT('POINT(', :longitude, ' ', :latitude, ')'),
                  4326,
                  'axis-order=long-lat'
              )
          )
          ORDER BY dong_code
          LIMIT 1
          """,
      nativeQuery = true)
  Optional<String> findDongCodeByCoordinates(
      @Param("latitude") double latitude, @Param("longitude") double longitude);

  List<Dong> findByRegionRegionCodeOrderByDongCode(String regionCode);
}
