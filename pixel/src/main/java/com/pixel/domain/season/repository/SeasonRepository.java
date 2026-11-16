package com.pixel.domain.season.repository;

import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Short> {

  Optional<Season> findBySeason(SeasonType season);

  /** 촬영 날짜의 연도와 무관하게 네 개의 고정 시즌 중 하나를 조회한다. */
  default Optional<Season> findCurrent(LocalDate date) {
    return findBySeason(SeasonType.fromDate(date));
  }
}
