package com.pixel.domain.colorcell.repository;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.season.entity.Season;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ColorCellRepository extends JpaRepository<ColorCell, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM ColorCell c WHERE c.dong = :dong AND c.season = :season"
      + " AND c.timeBand = :timeBand")
  Optional<ColorCell> findForUpdate(@Param("dong") Dong dong,
      @Param("season") Season season, @Param("timeBand") TimeBand timeBand);

  @Query("""
      SELECT c FROM ColorCell c
      JOIN FETCH c.dong
      WHERE c.season = :season
        AND c.timeBand = :timeBand
        AND c.dong.region.regionCode = :regionCode
      ORDER BY c.dong.dongCode
      """)
  List<ColorCell> findForMap(@Param("season") Season season,
      @Param("timeBand") TimeBand timeBand, @Param("regionCode") String regionCode);

  List<ColorCell> findByDongAndSeason(Dong dong, Season season);

  // 나의 지도 미리보기 색: 요청한 지역에서 색이 정해진 칸만 읽는다.
  @Query("""
      SELECT c FROM ColorCell c
      WHERE c.season = :season
        AND c.dong.region.regionCode = :regionCode
        AND c.dominantColor IS NOT NULL
      """)
  List<ColorCell> findColoredInRegion(@Param("season") Season season,
      @Param("regionCode") String regionCode);
}
