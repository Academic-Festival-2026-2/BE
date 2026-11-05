package com.pixel.domain.season.service;

import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;

public interface SeasonService {

  Season getCurrentSeason();

  Season getSeason(SeasonType season);
}
