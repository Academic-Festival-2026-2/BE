package com.pixel.domain.season.service;

import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.exception.code.SeasonErrorCode;
import com.pixel.domain.season.repository.SeasonRepository;
import com.pixel.global.apiPayload.exception.GeneralException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonServiceImpl implements SeasonService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private final SeasonRepository seasonRepository;

  @Override
  public Season getCurrentSeason() {
    LocalDate today = LocalDate.now(SEOUL);
    return seasonRepository.findCurrent(today)
        .orElseThrow(() -> new GeneralException(SeasonErrorCode._SEASON_NOT_FOUND));
  }

  @Override
  public Season getSeason(SeasonType season) {
    return findSeason(season)
        .orElseThrow(() -> new GeneralException(SeasonErrorCode._SEASON_TYPE_NOT_FOUND));
  }

  @Override
  public Optional<Season> findSeason(SeasonType season) {
    return seasonRepository.findFirstBySeasonOrderByYearDesc(season);
  }
}
