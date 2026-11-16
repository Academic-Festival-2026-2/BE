package com.pixel.domain.map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pixel.domain.colorcell.dto.ColorRatio;
import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.map.dto.MapDongsResponse;
import com.pixel.domain.region.entity.Region;
import com.pixel.domain.region.service.RegionService;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.service.SeasonService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapServiceImplTest {

  @Mock
  private DongRepository dongRepository;

  @Mock
  private SeasonService seasonService;

  @Mock
  private ColorCellRepository colorCellRepository;

  @Mock
  private RegionService regionService;

  @InjectMocks
  private MapServiceImpl mapService;

  private final Season season = mock(Season.class);

  private final Region region = mock(Region.class);

  @BeforeEach
  void setUp() {

    given(seasonService.getCurrentSeason()).willReturn(season);

    given(regionService.getRegion("3105")).willReturn(region);
    given(region.getRegionCode()).willReturn("3105");
  }

  @Test
  void excludesDongWithoutPhoto() {
    givenCells(cell("A", 5), cell("B", 0), cell("C", 3));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY, "3105");

    assertThat(response.getTopDongs())
        .extracting(MapDongsResponse.TopDong::getDongCode)
        .containsExactly("A", "C");
  }

  @Test
  void returnsTop3ByPhotoCountDesc() {
    givenCells(cell("A", 1), cell("B", 4), cell("C", 2), cell("D", 3));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY, "3105");

    assertThat(response.getTopDongs())
        .extracting(MapDongsResponse.TopDong::getDongCode)
        .containsExactly("B", "D", "C");
  }

  @Test
  void sortsTiesByDongCode() {
    givenCells(cell("B", 2), cell("A", 2), cell("C", 9));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY, "3105");

    assertThat(response.getTopDongs())
        .extracting(MapDongsResponse.TopDong::getDongCode)
        .containsExactly("C", "A", "B");
  }

  @Test
  void emptyWhenNoPhoto() {
    givenCells(cell("A", 0), cell("B", 0));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY, "3105");

    assertThat(response.getTopDongs()).isEmpty();
  }

  @Test
  void includesColorRatioForEachDong() {
    givenCells(cellWithBins("A", 5, bins(Map.of(0, 0.6, 12, 0.4))),
        cellWithBins("B", 0, bins(Map.of())));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY, "3105");

    MapDongsResponse.DongColor dongA = response.getDongs().get(0);
    assertThat(dongA.getColorRatio())
        .extracting(ColorRatio::hex)
        .containsExactly(ColorBins.HEX.get(0), ColorBins.HEX.get(12));

    assertThat(dongA.getColorRatio())
        .extracting(ColorRatio::ratio)
        .containsExactly(0.6, 0.4);

    assertThat(response.getDongs().get(1).getColorRatio()).isEmpty();

    assertThat(response.getTopDongs().get(0).getColorRatio())
        .extracting(ColorRatio::hex)
        .containsExactly(ColorBins.HEX.get(0),ColorBins.HEX.get(12));
  }

  @Test
  void colorRatioStartsWithCellColorEvenWhenItIsRareInBins() {
    // 분포는 나무·흙(26)·나무 초록(14) 위주지만 표로 정한 칸 색은 단풍 빨강(19)
    ColorCell cell = cellWithBins("A", 3,
        bins(Map.of(26, 0.5, 14, 0.2, 13, 0.1, 15, 0.1, 16, 0.07, 19, 0.03)));
    given(cell.getDominantColor()).willReturn(ColorBins.HEX.get(19));
    givenCells(cell);

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY, "3105");

    for (List<ColorRatio> ratios : List.of(response.getDongs().get(0).getColorRatio(),
        response.getTopDongs().get(0).getColorRatio())) {
      assertThat(ratios).hasSize(5);
      assertThat(ratios.get(0)).isEqualTo(new ColorRatio(ColorBins.HEX.get(19),
          ColorBins.FIRST_MIN_RATIO));
      assertThat(ratios).extracting(ColorRatio::hex).containsExactly(ColorBins.HEX.get(19),
          ColorBins.HEX.get(26), ColorBins.HEX.get(14), ColorBins.HEX.get(13),
          ColorBins.HEX.get(15));
    }
  }

  private void givenCells(ColorCell... cells) {
    given(colorCellRepository.findForMap(season, TimeBand.DAY, "3105"))
        .willReturn(List.of(cells));
  }

  private ColorCell cell(String dongCode, int photoCount) {
    Dong dong = mock(Dong.class);
    given(dong.getDongCode()).willReturn(dongCode);

    ColorCell cell = mock(ColorCell.class);
    given(cell.getDong()).willReturn(dong);
    given(cell.getPhotoCount()).willReturn(photoCount);
    return cell;
  }

  private ColorCell cellWithBins(String dongCode, int photoCount, List<Double> binsSum) {
    ColorCell cell = cell(dongCode, photoCount);
    given(cell.getBinsSum()).willReturn(binsSum);
    return cell;
  }

  private List<Double> bins(Map<Integer, Double> values) {
    List<Double> bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    values.forEach(bins::set);
    return bins;
  }
}
