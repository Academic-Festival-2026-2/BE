package com.binteum.domain.map.service;

import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.colorcell.repository.ColorCellRepository;
import com.binteum.domain.dong.repository.DongRepository;
import com.binteum.domain.map.dto.BoundaryResponse;
import com.binteum.domain.map.dto.MapDongsResponse;
import com.binteum.domain.map.exception.code.MapErrorCode;
import com.binteum.domain.season.entity.Season;
import com.binteum.domain.season.repository.SeasonRepository;
import com.binteum.global.apiPayload.exception.GeneralException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapServiceImpl implements MapService {

  private final DongRepository dongRepository;
  private final SeasonRepository seasonRepository;
  private final ColorCellRepository colorCellRepository;

  @Override
  public BoundaryResponse getBoundaries() {
    List<BoundaryResponse.Feature> features =
        dongRepository.findAllBoundaries().stream().map(view -> new BoundaryResponse.Feature(
            new BoundaryResponse.Properties(view.getDongCode(), view.getName(), view.getGuName()),
            view.getGeometry())).toList();

    return new BoundaryResponse(dongRepository.findBoundaryVersion(), features);
  }

  @Override
  public MapDongsResponse getDongs(TimeBand timeBand) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    Season season = seasonRepository.findCurrent(today)
        .orElseThrow(() -> new GeneralException(MapErrorCode._SEASON_NOT_FOUND));

    List<MapDongsResponse.DongColor> dongs = colorCellRepository.findForMap(season, timeBand)
        .stream()
        .map(cell -> new MapDongsResponse.DongColor(cell.getDong().getDongCode(),
            cell.getDong().getName(),
            cell.getDominantColor(),
            cell.getPhotoCount(),
            cell.getStatus()))
        .toList();

    return new MapDongsResponse(season.getSeason(), timeBand, dongs);
  }
}
