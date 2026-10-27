package com.binteum.domain.dong.repository;

import com.binteum.domain.dong.dto.DongBoundaryView;
import com.binteum.domain.dong.entity.Dong;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DongRepository extends JpaRepository<Dong, String> {

  @Query(value = "SELECT dong_code             AS dongCode, "
      + "       name                  AS name, "
      + "       gu_name               AS guName, "
      + "       ST_AsGeoJSON(boundary, 5) AS geometry "
      + "FROM dongs "
      + "ORDER BY dong_code ",
      nativeQuery = true)
  List<DongBoundaryView> findAllBoundaries();

  @Query("SELECT MAX(d.boundaryVersion) FROM Dong d")
  String findBoundaryVersion();
}
