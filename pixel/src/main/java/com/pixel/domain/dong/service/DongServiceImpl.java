package com.pixel.domain.dong.service;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.CellStatus;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.dong.dto.DongDetailResponse;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.exception.code.DongErrorCode;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.map.exception.code.MapErrorCode;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.repository.SeasonRepository;
import com.pixel.global.apiPayload.exception.GeneralException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DongServiceImpl implements DongService {

  private static final int COLOR_RATIO_LIMIT = 5;

  private final DongRepository dongRepository;
  private final SeasonRepository seasonRepository;
  private final ColorCellRepository colorCellRepository;
  private final PhotoRepository photoRepository;

  @Override
  public DongDetailResponse getDetail(String dongCode, TimeBand timeBand) {
    Dong dong = dongRepository.findById(dongCode)
        .orElseThrow(() -> new GeneralException(DongErrorCode._DONG_NOT_FOUND));

    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    Season season = seasonRepository.findCurrent(today)
        .orElseThrow(() -> new GeneralException(MapErrorCode._SEASON_NOT_FOUND));

    Map<TimeBand, ColorCell> cells = colorCellRepository.findByDongAndSeason(dong, season)
        .stream()
        .collect(Collectors.toMap(ColorCell::getTimeBand, Function.identity()));

    List<DongDetailResponse.TimeBandSummary> timeBands = Arrays.stream(TimeBand.values())
        .map(band -> toSummary(band, cells.get(band)))
        .toList();

    String pointBand = timeBand == null ? null : timeBand.name();
    List<DongDetailResponse.Point> points = photoRepository
        .findPhotoPoints(dongCode, season.getSeasonId(), pointBand)
        .stream()
        .map(view -> new DongDetailResponse.Point(
            view.getPhotoId(), view.getLat(),
            view.getLng(), view.getColor(), TimeBand.valueOf(view.getTimeBand())))
        .toList();

    return new DongDetailResponse(
        dong.getDongCode(), dong.getName(), dong.getGuName(),
        season.getSeason(), timeBands, points);
  }

  private DongDetailResponse.TimeBandSummary toSummary(TimeBand band, ColorCell cell) {
    if (cell == null) {
      return new DongDetailResponse.TimeBandSummary(band, null, 0, CellStatus.EMPTY, List.of());
    }
    return new DongDetailResponse.TimeBandSummary(
        band,
        cell.getDominantColor(),
        cell.getPhotoCount(),
        cell.getStatus(),
        ColorBins.topRatios(cell.getBinsSum(), COLOR_RATIO_LIMIT));
  }
}
