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
import com.pixel.domain.mymap.dto.MyMapShareResponse;
import com.pixel.domain.mymap.dto.SharedMapResponse;
import com.pixel.domain.mymap.exception.code.MyMapErrorCode;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.service.SeasonService;
import com.pixel.domain.user.entity.User;
import com.pixel.domain.user.enums.UserStatus;
import com.pixel.domain.user.exception.code.UserErrorCode;
import com.pixel.domain.user.repository.UserRepository;
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
  private final UserRepository userRepository;

  @Override
  public MyMapResponse getMyMap(Long userId, SeasonType seasonType) {
    return buildMap(userId, seasonType, false);
  }

  // 공유 지도는 publicOnly = true로 비공개 사진을 뺀다.
  private MyMapResponse buildMap(Long userId, SeasonType seasonType, boolean publicOnly) {
    Season season = seasonService.getSeason(seasonType);

    List<Dong> dongs = dongRepository.findAll(Sort.by("dongCode"));

    Map<CellKey, List<Photo>> myPhotos = groupMyPhotosByCell(userId, season, publicOnly);

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

  @Override
  @Transactional
  public MyMapShareResponse shareMyMap(Long userId) {
    User user = findUser(userId);
    user.shareMyMap();
    return new MyMapShareResponse(user.getShareKey());
  }

  @Override
  @Transactional
  public void stopSharingMyMap(Long userId) {
    findUser(userId).stopSharingMyMap();
  }

  private User findUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new GeneralException(UserErrorCode._NOT_FOUND));
  }

  @Override
  public SharedMapResponse getSharedMap(String shareKey, SeasonType seasonType) {
    User user = userRepository.findByShareKeyAndStatus(shareKey, UserStatus.ACTIVE)
        .orElseThrow(() -> new GeneralException(MyMapErrorCode._SHARED_MAP_NOT_FOUND));

    return new SharedMapResponse(user.getNickname(), buildMap(user.getUserId(), seasonType, true));
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
        .map(season -> groupMyPhotosByCell(userId, season, false).size())
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

  // 나의 지도 조회와 채운 칸 수 계산이 같은 기준을 쓰도록 한 곳에서만 사진을 고른다.
  private Map<CellKey, List<Photo>> groupMyPhotosByCell(Long userId, Season season,
      boolean publicOnly) {
    return photoRepository.findMyMapPhotos(userId, season)
        .stream()
        .filter(photo -> !publicOnly || photo.isPublic())
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
