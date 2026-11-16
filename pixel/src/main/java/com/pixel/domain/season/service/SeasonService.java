package com.pixel.domain.season.service;

import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import java.util.Optional;

public interface SeasonService {

  Season getCurrentSeason();

  /** 현재 시즌이 아직 DB에 등록되지 않았다면 Optional.empty()를 반환한다. */
  Optional<Season> findCurrentSeason();

  Season getSeason(SeasonType season);

  /** 같은 종류 중 가장 최근 시즌. 없으면 예외 대신 빈 값을 돌려준다. */
  Optional<Season> findSeason(SeasonType season);
}
