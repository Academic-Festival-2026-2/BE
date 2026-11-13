package com.pixel.domain.map.service;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.map.dto.BoundaryResponse;
import com.pixel.domain.map.dto.MapDongsResponse;
import com.pixel.domain.map.dto.RegionListResponse;
import com.pixel.domain.region.entity.Region;
import com.pixel.domain.region.repository.RegionRepository;
import com.pixel.domain.region.service.RegionService;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.service.SeasonService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapServiceImpl implements MapService {

  private static final int TOP_DONG_LIMIT = 3;
  private static final int COLOR_RATIO_LIMIT = 5;

  private final DongRepository dongRepository;
  private final ColorCellRepository colorCellRepository;
  private final SeasonService seasonService;
  private final RegionService regionService;
  private final RegionRepository regionRepository;

  @Override
  public BoundaryResponse getBoundaries(String regionCode) {
    Region region = regionService.getRegion(regionCode);

    List<BoundaryResponse.Feature> features =
        dongRepository.findAllBoundaries(region.getRegionCode())
            .stream()
            .map(view -> new BoundaryResponse.Feature(
                new BoundaryResponse.Properties(view.getDongCode(), view.getName(), view.getGuName()),
                view.getGeometry()))
            .toList();

    return new BoundaryResponse(dongRepository.findBoundaryVersion(region.getRegionCode()),
        region.getRegionCode(), region.getName(), features);
  }

  @Override
  public MapDongsResponse getDongs(TimeBand timeBand, String regionCode) {
    Season season = seasonService.getCurrentSeason();
    Region region = regionService.getRegion(regionCode);

    List<ColorCell> cells =
        colorCellRepository.findForMap(season, timeBand, region.getRegionCode());

    List<MapDongsResponse.DongColor> dongs = cells
        .stream()
        .map(cell -> new MapDongsResponse.DongColor(
            cell.getDong().getDongCode(),
            cell.getDong().getName(),
            cell.getDominantColor(),
            cell.getPhotoCount(),
            cell.getStatus(),
            ColorBins.topRatios(cell.getBinsSum(), COLOR_RATIO_LIMIT)))
        .toList();

    List<MapDongsResponse.TopDong> topDongs = cells.stream()
        .filter(cell -> cell.getPhotoCount() > 0)
        .sorted(Comparator.comparingInt(ColorCell::getPhotoCount).reversed()
            .thenComparing(cell -> cell.getDong().getDongCode()))
        .limit(TOP_DONG_LIMIT)
        .map(cell -> new MapDongsResponse.TopDong(
            cell.getDong().getDongCode(),
            cell.getDong().getName(),
            cell.getDong().getGuName(),
            cell.getPhotoCount(),
            ColorBins.topRatios(cell.getBinsSum(), COLOR_RATIO_LIMIT)))
        .toList();

    return new MapDongsResponse(season.getSeason(), timeBand, dongs, topDongs);
  }

  // 지역 목록과 bbox는 동 경계를 새로 넣을 때만 바뀐다. 처음 계산하면 서버 메모리에 보관하고,
  // 경계를 넣은 뒤에는 서버를 재시작해야 반영된다. 빈 목록(경계를 아직 안 넣음)은 캐시하지 않는다
  @Cacheable(value = "regions", unless = "#result.regions().isEmpty()")
  @Override
  public RegionListResponse getRegions() {
    List<RegionListResponse.Item> regions = regionRepository.findAllWithBounds()
        .stream()
        .map(view -> new RegionListResponse.Item(
            view.getRegionCode(),
            view.getName(),
            view.getDongCount(),
            List.of(view.getMinLng(), view.getMinLat(), view.getMaxLng(), view.getMaxLat())))
        .toList();

    return new RegionListResponse(Region.DEFAULT_CODE, regions);
  }
}
