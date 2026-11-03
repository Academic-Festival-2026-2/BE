package com.pixel.domain.colorcell.repository;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.season.entity.Season;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ColorCellRepository extends JpaRepository<ColorCell, Long> {

  @Query("""
      SELECT c FROM ColorCell c
      JOIN FETCH c.dong
      WHERE c.season = :season
        AND c.timeBand = :timeBand
      ORDER BY c.dong.dongCode
      """)
  List<ColorCell> findForMap(@Param("season") Season season,
      @Param("timeBand") TimeBand timeBand);

  List<ColorCell> findByDongAndSeason(Dong dong, Season season);
}
