package com.pixel.domain.season.service;

import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import java.util.Optional;

public interface SeasonService {

  Season getCurrentSeason();

  Season getSeason(SeasonType season);

  /** 같은 종류 중 가장 최근 시즌. 없으면 예외 대신 빈 값을 돌려준다. */
  Optional<Season> findSeason(SeasonType season);
}
