package com.pixel.domain.mymap.service;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.dong.dto.DongDetailResponse;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.exception.code.DongErrorCode;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.mymap.dto.MyDongPhotosResponse;
import com.pixel.domain.mymap.dto.MyMapResponse;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.service.SeasonService;
import com.pixel.global.apiPayload.exception.GeneralException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyMapServiceImpl implements MyMapService {

  private static final int BIN_COUNT = ColorBins.HEX.size();

  private final SeasonService seasonService;
  private final DongRepository dongRepository;
  private final PhotoRepository photoRepository;
  private final ColorCellRepository colorCellRepository;

  @Override
  public MyMapResponse getMyMap(Long userId, SeasonType seasonType) {
    Season season = seasonService.getSeason(seasonType);

    List<Dong> dongs = dongRepository.findAll(Sort.by("dongCode"));

    Map<CellKey, List<Photo>> myPhotos = groupMyPhotosByCell(userId, season);

    Map<CellKey, String> previewColors = colorCellRepository.findBySeason(season)
        .stream()
        .filter(cell -> cell.getDominantColor() != null)
        .collect(Collectors.toMap(
            cell -> new CellKey(cell.getDong().getDongCode(), cell.getTimeBand()),
            ColorCell::getDominantColor));

    List<MyMapResponse.Cell> cells = dongs.stream()
        .flatMap(dong -> Arrays.stream(TimeBand.values())
            .map(timeBand -> {
              CellKey key = new CellKey(dong.getDongCode(), timeBand);
              return toCell(dong, timeBand, myPhotos.getOrDefault(key, List.of()),
                  previewColors.get(key));
            }))
        .toList();

    int filledCellCount = (int) cells.stream()
        .filter(MyMapResponse.Cell::isFilled)
        .count();

    return new MyMapResponse(season.getSeason(), cells.size(), filledCellCount, cells);
  }

  private MyMapResponse.Cell toCell(Dong dong, TimeBand timeBand, List<Photo> photos,
      String previewColor) {
    boolean filled = !photos.isEmpty();
    String myColor = filled ? ColorBins.dominantColor(sumBins(photos)) : null;

    return new MyMapResponse.Cell(dong.getDongCode(), dong.getName(), timeBand, filled,
        myColor, photos.size(), previewColor);
  }

  @Override
  public int countFilledCells(Long userId, SeasonType seasonType) {
    return seasonService.findSeason(seasonType)
        .map(season -> groupMyPhotosByCell(userId, season).size())
        .orElse(0);
  }

  @Override
  public MyDongPhotosResponse getMyDongPhotos(Long userId, String dongCode, TimeBand timeBand,
      SeasonType seasonType) {
    if (!dongRepository.existsById(dongCode)) {
      throw new GeneralException(DongErrorCode._DONG_NOT_FOUND);
    }
    Season season = seasonService.getSeason(seasonType);
    String band = timeBand == null ? null : timeBand.name();

    List<DongDetailResponse.Point> points = photoRepository
        .findMyPhotoPoints(userId, dongCode, season.getSeasonId(), band)
        .stream()
        .map(view -> new DongDetailResponse.Point(view.getPhotoId(), view.getLat(),
            view.getLng(), view.getColor(), TimeBand.valueOf(view.getTimeBand())))
        .toList();

    return new MyDongPhotosResponse(dongCode, points);
  }

  /** 나의 지도의 filledCellCount와 같은 기준으로 채운 칸 수만 센다. 시즌이 없으면 0. */
  private Map<CellKey, List<Photo>> groupMyPhotosByCell(Long userId, Season season) {
    return photoRepository.findMyMapPhotos(userId, season)
        .stream()
        .collect(Collectors.groupingBy(
            photo -> new CellKey(photo.getDong().getDongCode(), photo.getTimeBand())));
  }

  private List<Double> sumBins(List<Photo> photos) {
    List<Double> sum = new ArrayList<>(Collections.nCopies(BIN_COUNT, 0.0));
    for (Photo photo : photos) {
      for (int i = 0; i < BIN_COUNT; i++) {
        sum.set(i, sum.get(i) + photo.getBins().get(i) * photo.getWeight());
      }
    }
    return sum;
  }

  private record CellKey(String dongCode, TimeBand timeBand) {}
}
