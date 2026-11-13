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

  // 정렬(ORDER BY)은 경계가 큰 동(안산 대부동 369KB)에서 정렬 메모리를 넘겨 MIN으로 대신한다.
  // SRID 4326 판정은 일부 동에서 틀려서, 인덱스로 후보만 고르고(MBRContains) 판정은 평면(SRID 0)으로 한다.
  // 경계는 GeoJSON [경도, 위도]로 넣어서 평면(SRID 0)에서도 x=경도, y=위도다. 점도 POINT(경도 위도)로 만든다.
  @Query(
      value =
          """
          SELECT MIN(dong_code)
          FROM dongs
          WHERE MBRContains(
                  boundary,
                  ST_GeomFromText(
                    CONCAT('POINT(', :longitude, ' ', :latitude, ')'),
                    4326,
                    'axis-order=long-lat')
                )
            AND ST_Contains(
                  ST_SRID(boundary, 0),
                  ST_GeomFromText(CONCAT('POINT(', :longitude, ' ', :latitude, ')'), 0)
                )
          """,
      nativeQuery = true)
  Optional<String> findDongCodeByCoordinates(
      @Param("latitude") double latitude, @Param("longitude") double longitude);

  List<Dong> findByRegionRegionCodeOrderByDongCode(String regionCode);
}
