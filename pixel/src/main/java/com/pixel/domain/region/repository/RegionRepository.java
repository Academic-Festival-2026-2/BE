package com.pixel.domain.region.repository;

import com.pixel.domain.region.dto.RegionBoundsView;
import com.pixel.domain.region.entity.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RegionRepository extends JpaRepository<Region, String> {

  // 지역마다 동 경계 전체를 감싸는 사각형. 평면(SRID 0)으로 바꿔 x=경도, y=위도로 읽는다.
  // ST_Envelope 결과 사각형의 1번 점이 (최소 경도, 최소 위도), 3번 점이 (최대 경도, 최대 위도)다.
  @Query(
      value =
          """
          SELECT r.region_code AS regionCode,
                 r.name AS name,
                 COUNT(*) AS dongCount,
                 MIN(ST_X(ST_PointN(ST_ExteriorRing(ST_Envelope(
                           ST_SRID(d.boundary, 0))), 1))) AS minLng,
                 MIN(ST_Y(ST_PointN(ST_ExteriorRing(ST_Envelope(
                           ST_SRID(d.boundary, 0))), 1))) AS minLat,
                 MAX(ST_X(ST_PointN(ST_ExteriorRing(ST_Envelope(
                           ST_SRID(d.boundary, 0))), 3))) AS maxLng,
                 MAX(ST_Y(ST_PointN(ST_ExteriorRing(ST_Envelope(
                           ST_SRID(d.boundary, 0))), 3))) AS maxLat
          FROM regions r
          JOIN dongs d ON d.region_code = r.region_code
          GROUP BY r.region_code, r.name
          ORDER BY r.region_code
          """,
      nativeQuery = true)
  List<RegionBoundsView> findAllWithBounds();
}