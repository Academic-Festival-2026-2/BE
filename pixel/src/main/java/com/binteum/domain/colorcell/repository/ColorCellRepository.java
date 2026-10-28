package com.binteum.domain.colorcell.repository;

import com.binteum.domain.colorcell.entity.ColorCell;
import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.season.entity.Season;
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
}
